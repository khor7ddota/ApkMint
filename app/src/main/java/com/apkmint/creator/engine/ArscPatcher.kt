package com.apkmint.creator.engine

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

object ArscPatcher {

    private const val CHUNK_TABLE = 0x0002
    private const val TEMPLATE_APP_NAME = "ApkMintTemplateApp"

    fun patchAppName(arscBytes: ByteArray, newAppName: String): ByteArray {
        val buf = ByteBuffer.wrap(arscBytes).order(ByteOrder.LITTLE_ENDIAN)
        val tableType = buf.getShort(0).toInt() and 0xFFFF
        val tableHeaderSize = buf.getShort(2).toInt() and 0xFFFF
        require(tableType == CHUNK_TABLE) { "Invalid ARSC header: 0x${tableType.toString(16)}" }

        val (pool, poolChunkSize) = StringPoolCodec.parse(arscBytes, tableHeaderSize)
        var replaced = false
        for (i in pool.strings.indices) {
            if (pool.strings[i] == TEMPLATE_APP_NAME) {
                pool.strings[i] = newAppName
                replaced = true
            }
        }
        if (!replaced) {
            return arscBytes
        }

        val encodedPool = StringPoolCodec.encode(pool)
        val restOffset = tableHeaderSize + poolChunkSize
        val restBytes = arscBytes.copyOfRange(restOffset, arscBytes.size)
        val totalSize = tableHeaderSize + encodedPool.size + restBytes.size

        val out = ByteArrayOutputStream(totalSize)
        val header = ByteBuffer.allocate(tableHeaderSize).order(ByteOrder.LITTLE_ENDIAN)
        header.put(arscBytes, 0, tableHeaderSize)
        header.putInt(4, totalSize)
        out.write(header.array())
        out.write(encodedPool)
        out.write(restBytes)
        return out.toByteArray()
    }
}
