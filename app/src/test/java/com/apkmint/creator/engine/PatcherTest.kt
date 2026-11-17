package com.apkmint.creator.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.ZipFile

class PatcherTest {

    @Test
    fun stringPoolRoundTripUtf8AndUtf16() {
        val utf8Pool = ParsedStringPool(
            flags = 0x100,
            strings = mutableListOf("alpha", "ApkMintTemplateApp", "gamma"),
            styleOffsets = IntArray(0),
            styleBytes = ByteArray(0)
        )
        val encoded8 = StringPoolCodec.encode(utf8Pool)
        val (decoded8, size8) = StringPoolCodec.parse(encoded8, 0)
        assertEquals(encoded8.size, size8)
        assertEquals(utf8Pool.strings, decoded8.strings)

        val utf16Pool = ParsedStringPool(
            flags = 0,
            strings = mutableListOf("com.apkmint.template", "1.0.0", "বাংলা নাম"),
            styleOffsets = IntArray(0),
            styleBytes = ByteArray(0)
        )
        val encoded16 = StringPoolCodec.encode(utf16Pool)
        val (decoded16, size16) = StringPoolCodec.parse(encoded16, 0)
        assertEquals(encoded16.size, size16)
        assertEquals(utf16Pool.strings, decoded16.strings)
    }

    @Test
    fun patchTemplateApkManifestAndArscIfAvailable() {
        val candidatePaths = listOf(
            "src/main/assets/template.apk",
            "app/src/main/assets/template.apk"
        )
        val templateFile = candidatePaths.map(::File).firstOrNull { it.exists() } ?: return

        ZipFile(templateFile).use { zip ->
            val manifestBytes = zip.getInputStream(zip.getEntry("AndroidManifest.xml")).use { it.readBytes() }
            val patchedManifest = AxmlPatcher.patchManifest(
                axmlBytes = manifestBytes,
                newPackageName = "com.custom.testapp",
                newVersionCode = 42,
                newVersionName = "2.5.1"
            )
            val buf = ByteBuffer.wrap(patchedManifest).order(ByteOrder.LITTLE_ENDIAN)
            assertEquals(patchedManifest.size, buf.getInt(4))
            val (pool, _) = StringPoolCodec.parse(patchedManifest, 8)
            assertTrue(pool.strings.contains("com.custom.testapp"))
            assertTrue(pool.strings.contains("2.5.1"))
            assertTrue(pool.strings.contains("com.apkmint.template.TemplateMainActivity"))

            val arscBytes = zip.getInputStream(zip.getEntry("resources.arsc")).use { it.readBytes() }
            val patchedArsc = ArscPatcher.patchAppName(arscBytes, "Custom বাংলা App")
            val arscBuf = ByteBuffer.wrap(patchedArsc).order(ByteOrder.LITTLE_ENDIAN)
            assertEquals(patchedArsc.size, arscBuf.getInt(4))
            val (arscPool, _) = StringPoolCodec.parse(patchedArsc, 12)
            assertTrue(arscPool.strings.contains("Custom বাংলা App"))

            // Ensure mipmap and adaptive foreground PNGs are present with readable names
            val entries = zip.entries().toList().map { it.name }
            val mipmaps = entries.filter { it.contains("ic_launcher.png") }
            val fgPngs = entries.filter { it.contains("ic_launcher_fg.png") }
            assertTrue("Expected mipmap ic_launcher.png entries, found: $mipmaps", mipmaps.isNotEmpty())
            assertTrue("Expected drawable ic_launcher_fg.png entries, found: $fgPngs", fgPngs.isNotEmpty())
        }
    }
}
