package com.apkmint.creator.engine

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

object AxmlPatcher {

    private const val CHUNK_AXML_FILE = 0x0003
    private const val CHUNK_XML_START_ELEMENT = 0x0102
    private const val TYPE_STRING = 0x03
    private const val TYPE_INT_DEC = 0x10

    fun patchManifest(
        axmlBytes: ByteArray,
        newPackageName: String,
        newVersionCode: Int,
        newVersionName: String
    ): ByteArray {
        val buf = ByteBuffer.wrap(axmlBytes).order(ByteOrder.LITTLE_ENDIAN)
        val fileType = buf.getShort(0).toInt() and 0xFFFF
        val fileHeaderSize = buf.getShort(2).toInt() and 0xFFFF
        require(fileType == CHUNK_AXML_FILE) { "Invalid AXML header: 0x${fileType.toString(16)}" }

        val (pool, poolChunkSize) = StringPoolCodec.parse(axmlBytes, fileHeaderSize)
        val restOffset = fileHeaderSize + poolChunkSize
        val restBytes = axmlBytes.copyOfRange(restOffset, axmlBytes.size)
        val restBuf = ByteBuffer.wrap(restBytes).order(ByteOrder.LITTLE_ENDIAN)

        // Walk chunks in restBytes to find <manifest> START_ELEMENT
        var pos = 0
        while (pos + 8 <= restBytes.size) {
            val chunkType = restBuf.getShort(pos).toInt() and 0xFFFF
            val chunkHeaderSize = restBuf.getShort(pos + 2).toInt() and 0xFFFF
            val chunkSize = restBuf.getInt(pos + 4)
            if (chunkSize <= 0 || pos + chunkSize > restBytes.size) break

            if (chunkType == CHUNK_XML_START_ELEMENT) {
                val extOffset = pos + chunkHeaderSize
                val nameIdx = restBuf.getInt(extOffset + 4)
                val tagName = pool.strings.getOrNull(nameIdx)
                if (tagName == "manifest") {
                    val attrStart = restBuf.getShort(extOffset + 8).toInt() and 0xFFFF
                    val attrSize = restBuf.getShort(extOffset + 10).toInt() and 0xFFFF
                    val attrCount = restBuf.getShort(extOffset + 12).toInt() and 0xFFFF
                    val attrsBase = extOffset + attrStart

                    for (i in 0 until attrCount) {
                        val aPos = attrsBase + i * attrSize
                        val attrNameIdx = restBuf.getInt(aPos + 4)
                        val attrName = pool.strings.getOrNull(attrNameIdx)
                        val dataType = restBuf.get(aPos + 15).toInt() and 0xFF

                        when (attrName) {
                            "package" -> {
                                val newIdx = addString(pool, newPackageName)
                                restBuf.putInt(aPos + 8, newIdx) // rawValue
                                if (dataType == TYPE_STRING) {
                                    restBuf.putInt(aPos + 16, newIdx) // typedValue.data
                                }
                            }
                            "versionName" -> {
                                val newIdx = addString(pool, newVersionName)
                                restBuf.putInt(aPos + 8, newIdx)
                                if (dataType == TYPE_STRING) {
                                    restBuf.putInt(aPos + 16, newIdx)
                                }
                            }
                            "versionCode" -> {
                                if (dataType == TYPE_INT_DEC) {
                                    restBuf.putInt(aPos + 8, -1) // rawValue
                                    restBuf.putInt(aPos + 16, newVersionCode)
                                }
                            }
                        }
                    }
                    break
                }
            }
            pos += chunkSize
        }

        val encodedPool = run {
            val oldPrefix = "com.apkmint.template."
            val keepClass = "com.apkmint.template.TemplateMainActivity"
            for (i in pool.strings.indices) {
                val s = pool.strings[i]
                if (s != keepClass && s.startsWith(oldPrefix)) {
                    pool.strings[i] = newPackageName + "." + s.removePrefix(oldPrefix)
                }
            }
            StringPoolCodec.encode(pool)
        }
        val totalSize = fileHeaderSize + encodedPool.size + restBytes.size

        val out = ByteArrayOutputStream(totalSize)
        val header = ByteBuffer.allocate(fileHeaderSize).order(ByteOrder.LITTLE_ENDIAN)
        header.put(axmlBytes, 0, fileHeaderSize)
        header.putInt(4, totalSize)
        out.write(header.array())
        out.write(encodedPool)
        out.write(restBytes)
        return out.toByteArray()
    }

    private fun addString(pool: ParsedStringPool, value: String): Int {
        val existing = pool.strings.indexOf(value)
        if (existing >= 0) return existing
        pool.strings.add(value)
        return pool.strings.lastIndex
    }
}
