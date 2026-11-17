package com.apkmint.creator.engine

import android.net.Uri

enum class SourceMode {
    URL,
    HTML
}

data class ApkBuildConfig(
    val sourceMode: SourceMode = SourceMode.URL,
    val url: String = "https://example.com",
    val htmlCode: String = DEFAULT_HTML_TEMPLATE,
    val htmlZipUri: Uri? = null,
    val appName: String = "My Web App",
    val packageName: String = "com.apkmint.myapp",
    val versionName: String = "1.0.0",
    val versionCode: Int = 1,
    val iconUri: Uri? = null,
    val statusBarColorHex: String = "#1565C0",
    val dynamicStatusBar: Boolean = true,
    val progressBarColorHex: String = "#10B981",
    val splashEnabled: Boolean = true,
    val splashDurationMs: Long = 1500L,
    val splashBgColorHex: String = "#FFFFFF",
    val splashTextColorHex: String = "#111827",
    val orientation: String = "auto",
    val javaScriptEnabled: Boolean = true,
    val domStorageEnabled: Boolean = true,
    val zoomEnabled: Boolean = true,
    val pullToRefresh: Boolean = true,
    val allowFileUpload: Boolean = true,
    val allowDownloads: Boolean = true,
    val openExternalLinksInBrowser: Boolean = true,
    val exitConfirmEnabled: Boolean = true,
    val showTopToolbar: Boolean = false,
    val swipeNavigation: Boolean = true,
    val desktopMode: Boolean = false,
    val keepScreenOn: Boolean = false,
    val blockScreenshots: Boolean = false,
    val allowCamera: Boolean = false,
    val allowMicrophone: Boolean = false,
    val allowLocation: Boolean = false,
    val customCss: String = "",
    val customJs: String = "",
    val userAgent: String = ""
) {
    companion object {
        const val DEFAULT_HTML_TEMPLATE = """<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
  <title>My App</title>
  <style>
    body { font-family: system-ui, sans-serif; margin: 0; padding: 24px; background: #f8fafc; color: #0f172a; }
    .card { background: #fff; border-radius: 16px; padding: 24px; box-shadow: 0 4px 20px rgba(0,0,0,0.06); }
    h1 { margin-top: 0; color: #1565c0; }
  </style>
</head>
<body>
  <div class="card">
    <h1>Hello from ApkMint!</h1>
    <p>Edit this HTML or import your own HTML/ZIP file to build your Android APK.</p>
  </div>
</body>
</html>"""
    }
}

data class BuiltApkInfo(
    val fileName: String,
    val filePath: String,
    val appName: String,
    val packageName: String,
    val versionName: String,
    val sizeBytes: Long,
    val createdAtMillis: Long
)
