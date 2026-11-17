package com.apkmint.creator.engine

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object ApkBuilderEngine {

    private val ICON_DENSITY_SIZES = mapOf(
        "mdpi" to 48,
        "hdpi" to 72,
        "xhdpi" to 96,
        "xxhdpi" to 144,
        "xxxhdpi" to 192
    )

    suspend fun buildApk(
        context: Context,
        config: ApkBuildConfig,
        onProgress: (step: String, fraction: Float) -> Unit
    ): Result<BuiltApkInfo> = withContext(Dispatchers.IO) {
        runCatching {
            val workDir = File(context.cacheDir, "apkmint_work").apply {
                if (exists()) deleteRecursively()
                mkdirs()
            }
            val apksDir = File(context.filesDir, "apks").apply { mkdirs() }

            onProgress("Preparing template APK...", 0.10f)
            val templateFile = File(workDir, "template.apk")
            context.assets.open("template.apk").use { input ->
                templateFile.outputStream().use { output -> input.copyTo(output) }
            }

            onProgress("Preparing app icons...", 0.25f)
            val iconMap = buildIconBytesMap(context, config.iconUri)

            onProgress("Preparing web content & config...", 0.35f)
            val configJsonBytes = buildConfigJson(config).toByteArray(Charsets.UTF_8)
            val webEntries = buildWebEntries(context, config)

            onProgress("Patching Manifest & Resources...", 0.55f)
            val unsignedApk = File(workDir, "unsigned.apk")
            packagePatchedApk(
                templateFile = templateFile,
                outputFile = unsignedApk,
                config = config,
                iconMap = iconMap,
                configJsonBytes = configJsonBytes,
                webEntries = webEntries
            )

            onProgress("Signing APK (v1/v2/v3)...", 0.80f)
            val safeName = config.appName
                .replace(Regex("[^a-zA-Z0-9_-]"), "_")
                .trim('_')
                .ifEmpty { "ApkMintApp" }
            val timestamp = System.currentTimeMillis()
            val finalFileName = "${safeName}_v${config.versionName}_${timestamp % 10000}.apk"
            val signedApk = File(apksDir, finalFileName)

            ApkSignerHelper.signApk(context, unsignedApk, signedApk)

            onProgress("Saving to Downloads...", 0.92f)
            exportToDownloads(context, signedApk, finalFileName)

            val info = BuiltApkInfo(
                fileName = finalFileName,
                filePath = signedApk.absolutePath,
                appName = config.appName,
                packageName = config.packageName,
                versionName = config.versionName,
                sizeBytes = signedApk.length(),
                createdAtMillis = timestamp
            )
            saveMetaSidecar(apksDir, info)
            saveProjectConfig(context, apksDir, finalFileName, config)

            workDir.deleteRecursively()
            onProgress("Done!", 1.0f)
            info
        }
    }

    fun listBuiltApks(context: Context): List<BuiltApkInfo> {
        val apksDir = File(context.filesDir, "apks")
        if (!apksDir.exists()) return emptyList()
        return apksDir.listFiles { file -> file.extension.equals("apk", ignoreCase = true) }
            ?.map { apk ->
                val metaFile = File(apksDir, "${apk.name}.meta.json")
                if (metaFile.exists()) {
                    val json = runCatching { JSONObject(metaFile.readText()) }.getOrNull()
                    if (json != null) {
                        return@map BuiltApkInfo(
                            fileName = apk.name,
                            filePath = apk.absolutePath,
                            appName = json.optString("appName", apk.nameWithoutExtension),
                            packageName = json.optString("packageName", ""),
                            versionName = json.optString("versionName", "1.0"),
                            sizeBytes = apk.length(),
                            createdAtMillis = json.optLong("createdAtMillis", apk.lastModified())
                        )
                    }
                }
                BuiltApkInfo(
                    fileName = apk.name,
                    filePath = apk.absolutePath,
                    appName = apk.nameWithoutExtension,
                    packageName = "",
                    versionName = "1.0",
                    sizeBytes = apk.length(),
                    createdAtMillis = apk.lastModified()
                )
            }
            ?.sortedByDescending { it.createdAtMillis }
            ?: emptyList()
    }

    fun deleteBuiltApk(info: BuiltApkInfo) {
        val apkFile = File(info.filePath)
        val metaFile = File("${info.filePath}.meta.json")
        val cfgFile = File("${info.filePath}.config.json")
        val iconFile = File("${info.filePath}.icon.png")
        if (apkFile.exists()) apkFile.delete()
        if (metaFile.exists()) metaFile.delete()
        if (cfgFile.exists()) cfgFile.delete()
        if (iconFile.exists()) iconFile.delete()
    }

    fun loadSavedProject(info: BuiltApkInfo): ApkBuildConfig? {
        val cfgFile = File("${info.filePath}.config.json")
        if (!cfgFile.exists()) return null
        val json = runCatching { JSONObject(cfgFile.readText()) }.getOrNull() ?: return null
        val iconFile = File("${info.filePath}.icon.png")
        val iconUri = if (iconFile.exists()) Uri.fromFile(iconFile) else null
        return ApkBuildConfig(
            sourceMode = if (json.optString("sourceMode") == "HTML") SourceMode.HTML else SourceMode.URL,
            url = json.optString("url", "https://example.com"),
            htmlCode = json.optString("htmlCode", ApkBuildConfig.DEFAULT_HTML_TEMPLATE),
            appName = json.optString("appName", info.appName),
            packageName = json.optString("packageName", info.packageName),
            versionName = json.optString("versionName", info.versionName),
            versionCode = json.optInt("versionCode", 1) + 1,
            iconUri = iconUri,
            statusBarColorHex = json.optString("statusBarColorHex", "#1565C0"),
            dynamicStatusBar = json.optBoolean("dynamicStatusBar", true),
            progressBarColorHex = json.optString("progressBarColorHex", "#10B981"),
            splashEnabled = json.optBoolean("splashEnabled", true),
            splashDurationMs = json.optLong("splashDurationMs", 1500L),
            splashBgColorHex = json.optString("splashBgColorHex", "#FFFFFF"),
            splashTextColorHex = json.optString("splashTextColorHex", "#111827"),
            orientation = json.optString("orientation", "auto"),
            javaScriptEnabled = json.optBoolean("javaScriptEnabled", true),
            domStorageEnabled = json.optBoolean("domStorageEnabled", true),
            zoomEnabled = json.optBoolean("zoomEnabled", true),
            pullToRefresh = json.optBoolean("pullToRefresh", true),
            allowFileUpload = json.optBoolean("allowFileUpload", true),
            allowDownloads = json.optBoolean("allowDownloads", true),
            openExternalLinksInBrowser = json.optBoolean("openExternalLinksInBrowser", true),
            exitConfirmEnabled = json.optBoolean("exitConfirmEnabled", true),
            showTopToolbar = json.optBoolean("showTopToolbar", false),
            swipeNavigation = json.optBoolean("swipeNavigation", true),
            desktopMode = json.optBoolean("desktopMode", false),
            keepScreenOn = json.optBoolean("keepScreenOn", false),
            blockScreenshots = json.optBoolean("blockScreenshots", false),
            allowCamera = json.optBoolean("allowCamera", false),
            allowMicrophone = json.optBoolean("allowMicrophone", false),
            allowLocation = json.optBoolean("allowLocation", false),
            customCss = json.optString("customCss", ""),
            customJs = json.optString("customJs", ""),
            userAgent = json.optString("userAgent", "")
        )
    }

    private fun saveProjectConfig(
        context: Context,
        apksDir: File,
        apkFileName: String,
        config: ApkBuildConfig
    ) {
        runCatching {
            if (config.iconUri != null) {
                context.contentResolver.openInputStream(config.iconUri)?.use { input ->
                    File(apksDir, "$apkFileName.icon.png").outputStream().use { out ->
                        input.copyTo(out)
                    }
                }
            }
            val json = JSONObject().apply {
                put("sourceMode", config.sourceMode.name)
                put("url", config.url)
                put("htmlCode", config.htmlCode)
                put("appName", config.appName)
                put("packageName", config.packageName)
                put("versionName", config.versionName)
                put("versionCode", config.versionCode)
                put("statusBarColorHex", config.statusBarColorHex)
                put("dynamicStatusBar", config.dynamicStatusBar)
                put("progressBarColorHex", config.progressBarColorHex)
                put("splashEnabled", config.splashEnabled)
                put("splashDurationMs", config.splashDurationMs)
                put("splashBgColorHex", config.splashBgColorHex)
                put("splashTextColorHex", config.splashTextColorHex)
                put("orientation", config.orientation)
                put("javaScriptEnabled", config.javaScriptEnabled)
                put("domStorageEnabled", config.domStorageEnabled)
                put("zoomEnabled", config.zoomEnabled)
                put("pullToRefresh", config.pullToRefresh)
                put("allowFileUpload", config.allowFileUpload)
                put("allowDownloads", config.allowDownloads)
                put("openExternalLinksInBrowser", config.openExternalLinksInBrowser)
                put("exitConfirmEnabled", config.exitConfirmEnabled)
                put("showTopToolbar", config.showTopToolbar)
                put("swipeNavigation", config.swipeNavigation)
                put("desktopMode", config.desktopMode)
                put("keepScreenOn", config.keepScreenOn)
                put("blockScreenshots", config.blockScreenshots)
                put("allowCamera", config.allowCamera)
                put("allowMicrophone", config.allowMicrophone)
                put("allowLocation", config.allowLocation)
                put("customCss", config.customCss)
                put("customJs", config.customJs)
                put("userAgent", config.userAgent)
            }
            File(apksDir, "$apkFileName.config.json").writeText(json.toString())
        }
    }

    private fun saveMetaSidecar(apksDir: File, info: BuiltApkInfo) {
        val json = JSONObject().apply {
            put("appName", info.appName)
            put("packageName", info.packageName)
            put("versionName", info.versionName)
            put("createdAtMillis", info.createdAtMillis)
        }
        File(apksDir, "${info.fileName}.meta.json").writeText(json.toString())
    }

    private fun packagePatchedApk(
        templateFile: File,
        outputFile: File,
        config: ApkBuildConfig,
        iconMap: Map<String, ByteArray>,
        configJsonBytes: ByteArray,
        webEntries: Map<String, ByteArray>
    ) {
        var currentOffset = 0L
        ZipFile(templateFile).use { zipIn ->
            ZipOutputStream(FileOutputStream(outputFile)).use { zipOut ->
                val entries = zipIn.entries()
                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement()
                    val name = entry.name

                    if (name.startsWith("META-INF/") ||
                        name == "assets/apkmint_config.json" ||
                        name.startsWith("assets/web/")
                    ) {
                        continue
                    }

                    val rawBytes = zipIn.getInputStream(entry).use { it.readBytes() }
                    val finalBytes = when {
                        name == "AndroidManifest.xml" -> AxmlPatcher.patchManifest(
                            axmlBytes = rawBytes,
                            newPackageName = config.packageName,
                            newVersionCode = config.versionCode,
                            newVersionName = config.versionName
                        )
                        name == "resources.arsc" -> ArscPatcher.patchAppName(rawBytes, config.appName)
                        else -> matchIconReplacement(name, iconMap) ?: rawBytes
                    }

                    val storeUncompressed = name == "resources.arsc" || name.endsWith(".so")
                    val alignBytes = if (name == "resources.arsc" || name.endsWith(".so")) 4 else 0
                    currentOffset = writeZipEntry(
                        zipOut = zipOut,
                        name = name,
                        bytes = finalBytes,
                        stored = storeUncompressed,
                        alignTo = alignBytes,
                        currentOffset = currentOffset
                    )
                }

                currentOffset = writeZipEntry(
                    zipOut = zipOut,
                    name = "assets/apkmint_config.json",
                    bytes = configJsonBytes,
                    stored = false,
                    alignTo = 0,
                    currentOffset = currentOffset
                )

                for ((webPath, webBytes) in webEntries) {
                    currentOffset = writeZipEntry(
                        zipOut = zipOut,
                        name = "assets/web/$webPath",
                        bytes = webBytes,
                        stored = false,
                        alignTo = 0,
                        currentOffset = currentOffset
                    )
                }
            }
        }
    }

    private fun writeZipEntry(
        zipOut: ZipOutputStream,
        name: String,
        bytes: ByteArray,
        stored: Boolean,
        alignTo: Int,
        currentOffset: Long
    ): Long {
        val outEntry = ZipEntry(name)
        val nameLen = name.toByteArray(Charsets.UTF_8).size
        var extraLen = 0

        if (stored) {
            outEntry.method = ZipEntry.STORED
            outEntry.size = bytes.size.toLong()
            outEntry.compressedSize = bytes.size.toLong()
            val crc = CRC32().apply { update(bytes) }.value
            outEntry.crc = crc

            if (alignTo > 1) {
                val dataOffset = currentOffset + 30 + nameLen
                val pad = ((alignTo - (dataOffset % alignTo)) % alignTo).toInt()
                if (pad > 0) {
                    outEntry.extra = ByteArray(pad)
                    extraLen = pad
                }
            }
        } else {
            outEntry.method = ZipEntry.DEFLATED
        }

        zipOut.putNextEntry(outEntry)
        zipOut.write(bytes)
        zipOut.closeEntry()

        val writtenPayloadSize = if (stored) bytes.size.toLong() else outEntry.compressedSize.coerceAtLeast(0L)
        return currentOffset + 30 + nameLen + extraLen + writtenPayloadSize
    }

    private fun matchIconReplacement(entryName: String, iconMap: Map<String, ByteArray>): ByteArray? {
        if (iconMap.isEmpty()) return null
        val isTarget = entryName.endsWith("ic_launcher.png") ||
            entryName.endsWith("ic_launcher_round.png") ||
            entryName.endsWith("ic_launcher_fg.png")
        if (!isTarget) return null
        for ((density, pngBytes) in iconMap) {
            if (entryName.contains("mipmap-$density") || entryName.contains("drawable-$density")) {
                return pngBytes
            }
        }
        return null
    }

    private fun buildIconBytesMap(context: Context, iconUri: Uri?): Map<String, ByteArray> {
        if (iconUri == null) return emptyMap()
        val original = context.contentResolver.openInputStream(iconUri)?.use {
            BitmapFactory.decodeStream(it)
        } ?: return emptyMap()

        val result = mutableMapOf<String, ByteArray>()
        for ((density, size) in ICON_DENSITY_SIZES) {
            val scaled = Bitmap.createScaledBitmap(original, size, size, true)
            val baos = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.PNG, 100, baos)
            result[density] = baos.toByteArray()
            if (scaled !== original) scaled.recycle()
        }
        original.recycle()
        return result
    }

    private fun buildWebEntries(context: Context, config: ApkBuildConfig): Map<String, ByteArray> {
        if (config.sourceMode == SourceMode.URL) {
            return mapOf("index.html" to "<html><body></body></html>".toByteArray(Charsets.UTF_8))
        }
        val zipUri = config.htmlZipUri
        if (zipUri != null) {
            val entries = linkedMapOf<String, ByteArray>()
            context.contentResolver.openInputStream(zipUri)?.use { stream ->
                ZipInputStream(stream).use { zis ->
                    var entry = zis.nextEntry
                    while (entry != null) {
                        if (!entry.isDirectory) {
                            val cleanPath = entry.name.trimStart('/')
                            if (cleanPath.isNotEmpty() && !cleanPath.contains("..")) {
                                entries[cleanPath] = zis.readBytes()
                            }
                        }
                        entry = zis.nextEntry
                    }
                }
            }
            if (entries.isNotEmpty()) {
                return entries
            }
        }
        return mapOf("index.html" to config.htmlCode.toByteArray(Charsets.UTF_8))
    }

    private fun buildConfigJson(config: ApkBuildConfig): String {
        return JSONObject().apply {
            put("mode", if (config.sourceMode == SourceMode.URL) "url" else "html")
            put("url", config.url.trim())
            put("htmlEntry", "web/index.html")
            put("appName", config.appName.trim())
            put("splashEnabled", config.splashEnabled)
            put("splashDurationMs", config.splashDurationMs)
            put("splashBgColor", config.splashBgColorHex)
            put("splashTextColor", config.splashTextColorHex)
            put("statusBarColor", config.statusBarColorHex)
            put("dynamicStatusBar", config.dynamicStatusBar)
            put("progressBarColor", config.progressBarColorHex)
            put("orientation", config.orientation)
            put("javaScriptEnabled", config.javaScriptEnabled)
            put("domStorageEnabled", config.domStorageEnabled)
            put("zoomEnabled", config.zoomEnabled)
            put("pullToRefresh", config.pullToRefresh)
            put("allowFileUpload", config.allowFileUpload)
            put("allowDownloads", config.allowDownloads)
            put("openExternalLinksInBrowser", config.openExternalLinksInBrowser)
            put("exitConfirmEnabled", config.exitConfirmEnabled)
            put("showTopToolbar", config.showTopToolbar)
            put("swipeNavigation", config.swipeNavigation)
            put("desktopMode", config.desktopMode)
            put("keepScreenOn", config.keepScreenOn)
            put("blockScreenshots", config.blockScreenshots)
            put("allowCamera", config.allowCamera)
            put("allowMicrophone", config.allowMicrophone)
            put("allowLocation", config.allowLocation)
            put("customCss", config.customCss)
            put("customJs", config.customJs)
            put("userAgent", config.userAgent.trim())
        }.toString(2)
    }

    private fun exportToDownloads(context: Context, apkFile: File, fileName: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            runCatching {
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                    put(MediaStore.Downloads.MIME_TYPE, "application/vnd.android.package-archive")
                    put(MediaStore.Downloads.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/ApkMint")
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return
                resolver.openOutputStream(uri)?.use { out ->
                    apkFile.inputStream().use { input -> input.copyTo(out) }
                }
            }
        }
    }
}
