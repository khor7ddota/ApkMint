package com.apkmint.creator.engine

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

internal class ParsedStringPool(
    val flags: Int,
    val strings: MutableList<String>,
    val styleOffsets: IntArray,
    val styleBytes: ByteArray
) {
    val isUtf8: Boolean get() = (flags and 0x100) != 0
}

internal object StringPoolCodec {

    const val CHUNK_TYPE_STRING_POOL = 0x0001

    fun parse(data: ByteArray, offset: Int): Pair<ParsedStringPool, Int> {
        val buf = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)
        val type = buf.getShort(offset).toInt() and 0xFFFF
        require(type == CHUNK_TYPE_STRING_POOL) { "Expected StringPool chunk (0x0001), got 0x${type.toString(16)}" }

        val headerSize = buf.getShort(offset + 2).toInt() and 0xFFFF
        val chunkSize = buf.getInt(offset + 4)
        val stringCount = buf.getInt(offset + 8)
        val styleCount = buf.getInt(offset + 12)
        val flags = buf.getInt(offset + 16)
        val stringsStart = buf.getInt(offset + 20)
        val stylesStart = buf.getInt(offset + 24)
        val isUtf8 = (flags and 0x100) != 0

        val stringOffsets = IntArray(stringCount) { i ->
            buf.getInt(offset + headerSize + i * 4)
        }
        val styleOffsets = IntArray(styleCount) { i ->
            buf.getInt(offset + headerSize + stringCount * 4 + i * 4)
        }

        val stringsBase = offset + stringsStart
        val strings = MutableList(stringCount) { i ->
            val strPos = stringsBase + stringOffsets[i]
            if (isUtf8) readUtf8String(data, strPos) else readUtf16String(data, strPos)
        }

        val styleBytes = if (styleCount > 0 && stylesStart > 0) {
            val stylesAbsStart = offset + stylesStart
            val stylesLen = (offset + chunkSize) - stylesAbsStart
            data.copyOfRange(stylesAbsStart, stylesAbsStart + stylesLen)
        } else {
            ByteArray(0)
        }

        return ParsedStringPool(flags, strings, styleOffsets, styleBytes) to chunkSize
    }

    fun encode(pool: ParsedStringPool): ByteArray {
        val stringCount = pool.strings.size
        val styleCount = pool.styleOffsets.size
        val headerSize = 28

        val stringOffsets = IntArray(stringCount)
        val stringsOut = ByteArrayOutputStream()
        for (i in 0 until stringCount) {
            stringOffsets[i] = stringsOut.size()
            val encoded = if (pool.isUtf8) {
                encodeUtf8String(pool.strings[i])
            } else {
                encodeUtf16String(pool.strings[i])
            }
            stringsOut.write(encoded)
        }

        // Pad strings data to 4-byte boundary
        while ((stringsOut.size() % 4) != 0) {
            stringsOut.write(0)
        }
        val stringsData = stringsOut.toByteArray()

        val offsetsTableSize = (stringCount + styleCount) * 4
        val stringsStart = headerSize + offsetsTableSize
        val stylesStart = if (styleCount > 0) stringsStart + stringsData.size else 0

        var totalSize = stringsStart + stringsData.size + pool.styleBytes.size
        val padding = (4 - (totalSize % 4)) % 4
        totalSize += padding

        val out = ByteBuffer.allocate(totalSize).order(ByteOrder.LITTLE_ENDIAN)
        out.putShort(CHUNK_TYPE_STRING_POOL.toShort())
        out.putShort(headerSize.toShort())
        out.putInt(totalSize)
        out.putInt(stringCount)
        out.putInt(styleCount)
        // Clear SORTED_FLAG (bit 0) since string values may have changed
        out.putInt(pool.flags and 1.inv())
        out.putInt(stringsStart)
        out.putInt(stylesStart)

        for (off in stringOffsets) out.putInt(off)
        for (off in pool.styleOffsets) out.putInt(off)
        out.put(stringsData)
        out.put(pool.styleBytes)
        repeat(padding) { out.put(0) }

        return out.array()
    }

    private fun readUtf8String(data: ByteArray, pos: Int): String {
        var p = pos
        // Skip char length (1 or 2 bytes)
        val c1 = data[p++].toInt() and 0xFF
        if ((c1 and 0x80) != 0) p++
        // Read byte length (1 or 2 bytes)
        val b1 = data[p++].toInt() and 0xFF
        val byteLen = if ((b1 and 0x80) != 0) {
            val b2 = data[p++].toInt() and 0xFF
            ((b1 and 0x7F) shl 8) or b2
        } else {
            b1
        }
        return String(data, p, byteLen, Charsets.UTF_8)
    }

    private fun readUtf16String(data: ByteArray, pos: Int): String {
        val buf = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)
        var p = pos
        val s1 = buf.getShort(p).toInt() and 0xFFFF
        p += 2
        val charLen = if ((s1 and 0x8000) != 0) {
            val s2 = buf.getShort(p).toInt() and 0xFFFF
            p += 2
            ((s1 and 0x7FFF) shl 16) or s2
        } else {
            s1
        }
        return String(data, p, charLen * 2, Charsets.UTF_16LE)
    }

    private fun encodeUtf8String(value: String): ByteArray {
        val utf8 = value.toByteArray(Charsets.UTF_8)
        val charLen = value.length
        val byteLen = utf8.size
        val out = ByteArrayOutputStream(byteLen + 5)
        writeUtf8Len(out, charLen)
        writeUtf8Len(out, byteLen)
        out.write(utf8)
        out.write(0)
        return out.toByteArray()
    }

    private fun writeUtf8Len(out: ByteArrayOutputStream, len: Int) {
        if (len > 0x7F) {
            out.write(((len shr 8) and 0x7F) or 0x80)
            out.write(len and 0xFF)
        } else {
            out.write(len)
        }
    }

    private fun encodeUtf16String(value: String): ByteArray {
        val utf16 = value.toByteArray(Charsets.UTF_16LE)
        val charLen = value.length
        val out = ByteArrayOutputStream(utf16.size + 6)
        if (charLen > 0x7FFF) {
            val hi = ((charLen shr 16) and 0x7FFF) or 0x8000
            val lo = charLen and 0xFFFF
            out.write(hi and 0xFF)
            out.write((hi shr 8) and 0xFF)
            out.write(lo and 0xFF)
            out.write((lo shr 8) and 0xFF)
        } else {
            out.write(charLen and 0xFF)
            out.write((charLen shr 8) and 0xFF)
        }
        out.write(utf16)
        out.write(0)
        out.write(0)
        return out.toByteArray()
    }
}
