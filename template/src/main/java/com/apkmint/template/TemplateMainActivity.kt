package com.apkmint.template

import android.Manifest
import android.annotation.SuppressLint
import android.app.Dialog
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.webkit.CookieManager
import android.webkit.GeolocationPermissions
import android.webkit.JavascriptInterface
import android.webkit.PermissionRequest
import android.webkit.URLUtil
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import org.json.JSONObject

data class TemplateConfig(
    val mode: String,
    val url: String,
    val htmlEntry: String,
    val appName: String,
    val splashEnabled: Boolean,
    val splashDurationMs: Long,
    val splashBgColor: Int,
    val splashTextColor: Int,
    val statusBarColor: Int,
    val dynamicStatusBar: Boolean,
    val progressBarColor: Int,
    val exitConfirmEnabled: Boolean,
    val orientation: String,
    val javaScriptEnabled: Boolean,
    val domStorageEnabled: Boolean,
    val zoomEnabled: Boolean,
    val pullToRefresh: Boolean,
    val allowFileUpload: Boolean,
    val allowDownloads: Boolean,
    val openExternalLinksInBrowser: Boolean,
    val showTopToolbar: Boolean,
    val swipeNavigation: Boolean,
    val desktopMode: Boolean,
    val keepScreenOn: Boolean,
    val blockScreenshots: Boolean,
    val allowCamera: Boolean,
    val allowMicrophone: Boolean,
    val allowLocation: Boolean,
    val customCss: String,
    val customJs: String,
    val userAgent: String
)

class TemplateMainActivity : ComponentActivity() {

    private lateinit var config: TemplateConfig
    private lateinit var rootContainer: FrameLayout
    private lateinit var webView: WebView
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var errorLayout: LinearLayout
    private lateinit var errorMessageView: TextView
    private var currentThemeColor: Int = Color.WHITE
    private var floatingToggleBtn: ImageView? = null
    private var floatingMenuCard: LinearLayout? = null
    private val floatingMenuIcons = mutableListOf<ImageView>()

    private var filePathCallback: ValueCallback<Array<Uri>>? = null
    private var pendingWebPermissionRequest: PermissionRequest? = null
    private var pendingGeoOrigin: String? = null
    private var pendingGeoCallback: GeolocationPermissions.Callback? = null
    private var initialHost: String? = null
    private var hasMainFrameError = false
    private var dynamicColorApplied = false

    private val runtimePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        pendingWebPermissionRequest?.let { req ->
            pendingWebPermissionRequest = null
            val allGranted = req.resources.all { res ->
                when (res) {
                    PermissionRequest.RESOURCE_VIDEO_CAPTURE -> grants[Manifest.permission.CAMERA] == true
                    PermissionRequest.RESOURCE_AUDIO_CAPTURE -> grants[Manifest.permission.RECORD_AUDIO] == true
                    else -> true
                }
            }
            if (allGranted) req.grant(req.resources) else req.deny()
        }
        pendingGeoCallback?.let { cb ->
            val origin = pendingGeoOrigin ?: ""
            pendingGeoCallback = null
            pendingGeoOrigin = null
            val locGranted = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
            cb.invoke(origin, locGranted, false)
        }
    }

    private val fileChooserLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val callback = filePathCallback ?: return@registerForActivityResult
        filePathCallback = null
        val data = result.data
        val uris: Array<Uri>? = when {
            result.resultCode != RESULT_OK || data == null -> null
            data.clipData != null -> {
                val clip = data.clipData!!
                Array(clip.itemCount) { i -> clip.getItemAt(i).uri }
            }
            data.data != null -> arrayOf(data.data!!)
            else -> null
        }
        callback.onReceiveValue(uris)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        config = loadConfig()
        applyOrientation(config.orientation)
        if (config.keepScreenOn) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        if (config.blockScreenshots) {
            window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }

        rootContainer = buildUi()
        setContentView(rootContainer)
        val initialStatusColor = if (config.splashEnabled) config.splashBgColor else config.statusBarColor
        applyStatusBar(initialStatusColor)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (::webView.isInitialized && webView.canGoBack()) {
                    webView.goBack()
                } else if (config.exitConfirmEnabled) {
                    showStyledExitDialog()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })

        loadTarget()
    }

    private fun loadConfig(): TemplateConfig {
        val raw = runCatching {
            assets.open("apkmint_config.json").bufferedReader().use { it.readText() }
        }.getOrDefault("{}")
        val json = runCatching { JSONObject(raw) }.getOrDefault(JSONObject())
        return TemplateConfig(
            mode = json.optString("mode", "url"),
            url = json.optString("url", "https://example.com"),
            htmlEntry = json.optString("htmlEntry", "web/index.html"),
            appName = json.optString("appName", getString(R.string.app_name)),
            splashEnabled = json.optBoolean("splashEnabled", true),
            splashDurationMs = json.optLong("splashDurationMs", 1500L).coerceIn(300L, 6000L),
            splashBgColor = parseColorSafe(json.optString("splashBgColor", "#FFFFFF"), Color.WHITE),
            splashTextColor = parseColorSafe(json.optString("splashTextColor", "#111827"), Color.DKGRAY),
            statusBarColor = parseColorSafe(json.optString("statusBarColor", "#1565C0"), Color.parseColor("#1565C0")),
            dynamicStatusBar = json.optBoolean("dynamicStatusBar", true),
            progressBarColor = parseColorSafe(json.optString("progressBarColor", "#10B981"), Color.parseColor("#10B981")),
            exitConfirmEnabled = json.optBoolean("exitConfirmEnabled", true),
            orientation = json.optString("orientation", "auto"),
            javaScriptEnabled = json.optBoolean("javaScriptEnabled", true),
            domStorageEnabled = json.optBoolean("domStorageEnabled", true),
            zoomEnabled = json.optBoolean("zoomEnabled", true),
            pullToRefresh = json.optBoolean("pullToRefresh", true),
            allowFileUpload = json.optBoolean("allowFileUpload", true),
            allowDownloads = json.optBoolean("allowDownloads", true),
            openExternalLinksInBrowser = json.optBoolean("openExternalLinksInBrowser", true),
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
            userAgent = json.optString("userAgent", "").trim()
        )
    }

    private fun parseColorSafe(hex: String, fallback: Int): Int {
        return runCatching { Color.parseColor(hex) }.getOrDefault(fallback)
    }

    private fun applyOrientation(orientation: String) {
        requestedOrientation = when (orientation.lowercase()) {
            "portrait" -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            "landscape" -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            else -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    @Suppress("DEPRECATION")
    private fun applyStatusBar(color: Int) {
        currentThemeColor = color
        window.statusBarColor = color
        if (::rootContainer.isInitialized) {
            rootContainer.setBackgroundColor(color)
        }
        val isLight = ColorUtils.calculateLuminance(color) > 0.5
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = isLight
        updateFloatingBubbleTheme(color, isLight)
    }

    private fun updateFloatingBubbleTheme(baseColor: Int, isLightBackground: Boolean) {
        val btn = floatingToggleBtn ?: return
        val card = floatingMenuCard ?: return

        val bubbleFill = if (isLightBackground) {
            Color.parseColor("#0D9488")
        } else {
            ColorUtils.blendARGB(baseColor, Color.parseColor("#0D9488"), 0.35f)
        }
        val borderColor = if (isLightBackground) {
            Color.parseColor("#CCFFFFFF")
        } else {
            Color.parseColor("#66FFFFFF")
        }
        btn.background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(bubbleFill)
            setStroke(dp(2), borderColor)
        }

        val cardBg = if (isLightBackground) Color.parseColor("#F5FFFFFF") else Color.parseColor("#F21E293B")
        val cardStroke = if (isLightBackground) Color.parseColor("#22000000") else Color.parseColor("#33FFFFFF")
        val itemCircleBg = if (isLightBackground) Color.parseColor("#140D9488") else Color.parseColor("#1FFFFFFF")
        val iconTint = if (isLightBackground) Color.parseColor("#0F172A") else Color.WHITE

        card.background = GradientDrawable().apply {
            setColor(cardBg)
            cornerRadius = dp(22).toFloat()
            setStroke(dp(1), cardStroke)
        }
        floatingMenuIcons.forEach { iv ->
            iv.setColorFilter(iconTint, PorterDuff.Mode.SRC_IN)
            iv.background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(itemCircleBg)
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun buildUi(): FrameLayout {
        val root = FrameLayout(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(if (config.splashEnabled) config.splashBgColor else config.statusBarColor)
        }
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, windowInsets ->
            val bars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            windowInsets
        }

        webView = WebView(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            configureWebView(this)
        }

        swipeRefresh = SwipeRefreshLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            isEnabled = config.pullToRefresh
            setColorSchemeColors(config.statusBarColor)
            addView(webView)
            setOnRefreshListener {
                webView.reload()
            }
        }
        root.addView(swipeRefresh)

        if (config.showTopToolbar) {
            attachFloatingBubble(root)
        }

        progressBar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(3)
            ).apply { gravity = Gravity.TOP }
            max = 100
            @Suppress("DEPRECATION")
            progressDrawable?.setColorFilter(config.progressBarColor, PorterDuff.Mode.SRC_IN)
            visibility = View.GONE
        }
        root.addView(progressBar)

        errorLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.WHITE)
            setPadding(dp(24), dp(24), dp(24), dp(24))
            visibility = View.GONE

            val title = TextView(this@TemplateMainActivity).apply {
                text = "Unable to load page"
                setTextColor(Color.parseColor("#111827"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
                setTypeface(null, Typeface.BOLD)
                gravity = Gravity.CENTER
            }
            addView(title)

            errorMessageView = TextView(this@TemplateMainActivity).apply {
                text = "Please check your connection and try again."
                setTextColor(Color.parseColor("#6B7280"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                gravity = Gravity.CENTER
                setPadding(0, dp(8), 0, dp(20))
            }
            addView(errorMessageView)

            val retryBtn = Button(this@TemplateMainActivity).apply {
                text = "Retry"
                setOnClickListener {
                    hasMainFrameError = false
                    visibility = View.GONE
                    errorLayout.visibility = View.GONE
                    webView.reload()
                }
            }
            addView(retryBtn)
        }
        root.addView(errorLayout)

        if (config.splashEnabled) {
            val splashOverlay = buildSplashOverlay()
            root.addView(splashOverlay)
            Handler(Looper.getMainLooper()).postDelayed({
                splashOverlay.animate()
                    .alpha(0f)
                    .setDuration(250L)
                    .withEndAction {
                        splashOverlay.visibility = View.GONE
                        root.removeView(splashOverlay)
                        if (!config.dynamicStatusBar && !dynamicColorApplied) {
                            applyStatusBar(config.statusBarColor)
                        }
                    }
                    .start()
            }, config.splashDurationMs)
        }

        return root
    }

    private fun buildSplashOverlay(): View {
        return LinearLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(config.splashBgColor)
            setPadding(dp(32), dp(32), dp(32), dp(32))

            val iconView = ImageView(this@TemplateMainActivity).apply {
                val size = dp(96)
                layoutParams = LinearLayout.LayoutParams(size, size)
                val appIcon = runCatching { packageManager.getApplicationIcon(packageName) }.getOrNull()
                if (appIcon != null) {
                    setImageDrawable(appIcon)
                } else {
                    setImageResource(R.mipmap.ic_launcher)
                }
            }
            addView(iconView)

            val nameView = TextView(this@TemplateMainActivity).apply {
                text = config.appName
                setTextColor(config.splashTextColor)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f)
                setTypeface(null, Typeface.BOLD)
                gravity = Gravity.CENTER
                setPadding(0, dp(16), 0, 0)
            }
            addView(nameView)
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun configureWebView(wv: WebView) {
        wv.settings.apply {
            javaScriptEnabled = config.javaScriptEnabled
            domStorageEnabled = config.domStorageEnabled
            databaseEnabled = config.domStorageEnabled
            setSupportZoom(config.zoomEnabled)
            builtInZoomControls = config.zoomEnabled
            displayZoomControls = false
            allowFileAccess = true
            allowContentAccess = true
            loadWithOverviewMode = true
            useWideViewPort = true
            mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            setGeolocationEnabled(config.allowLocation)
            if (config.userAgent.isNotEmpty()) {
                userAgentString = config.userAgent
            } else if (config.desktopMode) {
                userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
            }
        }

        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(wv, true)

        if (config.dynamicStatusBar && config.javaScriptEnabled) {
            wv.addJavascriptInterface(object {
                @JavascriptInterface
                fun onThemeColorChanged(cssColor: String) {
                    val parsed = parseCssColor(cssColor.trim()) ?: return
                    runOnUiThread {
                        dynamicColorApplied = true
                        applyStatusBar(parsed)
                    }
                }
            }, "ApkMintBridge")
        }

        wv.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val uri = request?.url ?: return false
                val scheme = uri.scheme?.lowercase() ?: return false
                if (scheme == "file" || scheme == "data" || scheme == "about") {
                    return false
                }
                if (scheme != "http" && scheme != "https") {
                    openExternal(uri)
                    return true
                }
                if (config.openExternalLinksInBrowser) {
                    val host = uri.host?.lowercase()
                    if (config.mode == "html" || (initialHost != null && host != null && host != initialHost && !host.endsWith(".$initialHost"))) {
                        openExternal(uri)
                        return true
                    }
                }
                return false
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                super.onPageStarted(view, url, favicon)
                hasMainFrameError = false
                errorLayout.visibility = View.GONE
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                swipeRefresh.isRefreshing = false
                if (!hasMainFrameError) {
                    errorLayout.visibility = View.GONE
                    injectCustomCssAndJs(view)
                    if (config.dynamicStatusBar) {
                        scheduleDynamicStatusBarChecks(view)
                    }
                }
            }

            override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
                super.doUpdateVisitedHistory(view, url, isReload)
                if (config.dynamicStatusBar && !hasMainFrameError) {
                    Handler(Looper.getMainLooper()).postDelayed({
                        detectAndApplyDynamicStatusBar(view)
                    }, 350L)
                }
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                super.onReceivedError(view, request, error)
                if (request?.isForMainFrame == true) {
                    hasMainFrameError = true
                    swipeRefresh.isRefreshing = false
                    errorMessageView.text = error?.description?.toString()
                        ?: "Please check your connection and try again."
                    errorLayout.visibility = View.VISIBLE
                }
            }
        }

        wv.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                progressBar.progress = newProgress
                progressBar.visibility = if (newProgress in 1..99) View.VISIBLE else View.GONE
            }

            override fun onPermissionRequest(request: PermissionRequest?) {
                val req = request ?: return
                val needed = mutableListOf<String>()
                for (res in req.resources) {
                    when (res) {
                        PermissionRequest.RESOURCE_VIDEO_CAPTURE -> {
                            if (!config.allowCamera) { req.deny(); return }
                            if (ContextCompat.checkSelfPermission(this@TemplateMainActivity, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                                needed.add(Manifest.permission.CAMERA)
                            }
                        }
                        PermissionRequest.RESOURCE_AUDIO_CAPTURE -> {
                            if (!config.allowMicrophone) { req.deny(); return }
                            if (ContextCompat.checkSelfPermission(this@TemplateMainActivity, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                                needed.add(Manifest.permission.RECORD_AUDIO)
                            }
                        }
                    }
                }
                if (needed.isEmpty()) {
                    req.grant(req.resources)
                } else {
                    pendingWebPermissionRequest = req
                    runtimePermissionLauncher.launch(needed.toTypedArray())
                }
            }

            override fun onGeolocationPermissionsShowPrompt(
                origin: String?,
                callback: GeolocationPermissions.Callback?
            ) {
                if (!config.allowLocation || callback == null) {
                    callback?.invoke(origin, false, false)
                    return
                }
                if (ContextCompat.checkSelfPermission(this@TemplateMainActivity, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    callback.invoke(origin, true, false)
                } else {
                    pendingGeoOrigin = origin
                    pendingGeoCallback = callback
                    runtimePermissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                }
            }

            override fun onShowFileChooser(
                webView: WebView?,
                filePathCallback: ValueCallback<Array<Uri>>?,
                fileChooserParams: FileChooserParams?
            ): Boolean {
                if (!config.allowFileUpload || filePathCallback == null) {
                    return false
                }
                this@TemplateMainActivity.filePathCallback?.onReceiveValue(null)
                this@TemplateMainActivity.filePathCallback = filePathCallback

                val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = fileChooserParams?.acceptTypes?.firstOrNull()?.takeIf { it.isNotBlank() } ?: "*/*"
                    putExtra(Intent.EXTRA_ALLOW_MULTIPLE, fileChooserParams?.mode == FileChooserParams.MODE_OPEN_MULTIPLE)
                }
                return runCatching {
                    fileChooserLauncher.launch(Intent.createChooser(intent, "Select File"))
                    true
                }.getOrElse {
                    this@TemplateMainActivity.filePathCallback = null
                    false
                }
            }
        }

        wv.setDownloadListener { url, userAgent, contentDisposition, mimeType, _ ->
            if (!config.allowDownloads) {
                Toast.makeText(this, "Downloads are disabled", Toast.LENGTH_SHORT).show()
                return@setDownloadListener
            }
            handleDownload(url, userAgent, contentDisposition, mimeType)
        }

        if (config.swipeNavigation) {
            attachEdgeSwipeNavigation(wv)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun attachEdgeSwipeNavigation(wv: WebView) {
        val edgeZonePx = dp(36).toFloat()
        val triggerDistancePx = dp(64).toFloat()
        var startX = 0f
        var startY = 0f
        var fromLeftEdge = false
        var fromRightEdge = false

        wv.setOnTouchListener { v, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    startX = event.x
                    startY = event.y
                    fromLeftEdge = startX <= edgeZonePx
                    fromRightEdge = startX >= (v.width - edgeZonePx)
                }
                MotionEvent.ACTION_UP -> {
                    if (fromLeftEdge || fromRightEdge) {
                        val dx = event.x - startX
                        val dy = event.y - startY
                        if (kotlin.math.abs(dx) > triggerDistancePx &&
                            kotlin.math.abs(dx) > kotlin.math.abs(dy) * 1.5f
                        ) {
                            if (fromLeftEdge && dx > 0 && wv.canGoBack()) {
                                wv.goBack()
                                return@setOnTouchListener true
                            } else if (fromRightEdge && dx < 0 && wv.canGoForward()) {
                                wv.goForward()
                                return@setOnTouchListener true
                            }
                        }
                    }
                    fromLeftEdge = false
                    fromRightEdge = false
                }
            }
            false
        }
    }

    private fun handleDownload(url: String, userAgent: String, contentDisposition: String, mimeType: String) {
        val fileName = URLUtil.guessFileName(url, contentDisposition, mimeType)
        runCatching {
            val request = DownloadManager.Request(Uri.parse(url)).apply {
                setMimeType(mimeType)
                val cookies = CookieManager.getInstance().getCookie(url)
                if (!cookies.isNullOrEmpty()) addRequestHeader("cookie", cookies)
                addRequestHeader("User-Agent", userAgent)
                setTitle(fileName)
                setDescription("Downloading file...")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
            }
            val dm = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            dm.enqueue(request)
            Toast.makeText(this, "Downloading $fileName", Toast.LENGTH_SHORT).show()
        }.onFailure {
            openExternal(Uri.parse(url))
        }
    }

    private fun openExternal(uri: Uri) {
        runCatching {
            startActivity(Intent(Intent.ACTION_VIEW, uri))
        }
    }

    private fun loadTarget() {
        if (config.mode == "html") {
            val entry = config.htmlEntry.trimStart('/')
            webView.loadUrl("file:///android_asset/$entry")
        } else {
            val normalizedUrl = normalizeUrl(config.url)
            initialHost = runCatching { Uri.parse(normalizedUrl).host?.lowercase() }.getOrNull()
            webView.loadUrl(normalizedUrl)
        }
    }

    private fun normalizeUrl(raw: String): String {
        val trimmed = raw.trim()
        return if (trimmed.startsWith("http://", true) || trimmed.startsWith("https://", true)) {
            trimmed
        } else {
            "https://$trimmed"
        }
    }

    private fun scheduleDynamicStatusBarChecks(view: WebView?) {
        injectDynamicThemeObserver(view)
        detectAndApplyDynamicStatusBar(view)
        val handler = Handler(Looper.getMainLooper())
        handler.postDelayed({ detectAndApplyDynamicStatusBar(view) }, 600L)
        handler.postDelayed({ detectAndApplyDynamicStatusBar(view) }, 1800L)
    }

    private fun injectDynamicThemeObserver(view: WebView?) {
        val wv = view ?: return
        if (!config.javaScriptEnabled) return
        val observerJs = """
            (function() {
              if (window.__apkMintObserverInstalled) return;
              window.__apkMintObserverInstalled = true;
              function isVisibleColor(c) {
                if (!c || c === 'transparent' || c === 'rgba(0, 0, 0, 0)') return false;
                var m = c.match(/rgba?\((\d+),\s*(\d+),\s*(\d+)(?:,\s*([\d.]+))?\)/);
                if (m && m[4] !== undefined && parseFloat(m[4]) < 0.2) return false;
                return true;
              }
              function pickTopColor() {
                try {
                  var topEl = document.elementFromPoint(Math.floor(window.innerWidth / 2), 12);
                  var cur = topEl;
                  while (cur && cur !== document) {
                    var bg = getComputedStyle(cur).backgroundColor;
                    if (isVisibleColor(bg)) return bg;
                    cur = cur.parentElement;
                  }
                  var sels = ['header', 'nav', '[role="banner"]', 'mat-toolbar'];
                  for (var i = 0; i < sels.length; i++) {
                    var el = document.querySelector(sels[i]);
                    if (el) {
                      var hbg = getComputedStyle(el).backgroundColor;
                      if (isVisibleColor(hbg)) return hbg;
                    }
                  }
                  var meta = document.querySelector('meta[name="theme-color"]');
                  if (meta && meta.content) return meta.content.trim();
                  var bodyBg = getComputedStyle(document.body).backgroundColor;
                  if (isVisibleColor(bodyBg)) return bodyBg;
                  var htmlBg = getComputedStyle(document.documentElement).backgroundColor;
                  if (isVisibleColor(htmlBg)) return htmlBg;
                } catch (e) {}
                return "";
              }
              var timer = null;
              function notifySoon(delay) {
                clearTimeout(timer);
                timer = setTimeout(function() {
                  var c = pickTopColor();
                  if (c && window.ApkMintBridge) {
                    window.ApkMintBridge.onThemeColorChanged(c);
                  }
                }, delay);
              }
              var obs = new MutationObserver(function() {
                notifySoon(60);
                setTimeout(function() {
                  var c = pickTopColor();
                  if (c && window.ApkMintBridge) window.ApkMintBridge.onThemeColorChanged(c);
                }, 350);
              });
              obs.observe(document.documentElement, {
                attributes: true,
                attributeFilter: ['class', 'data-theme', 'data-mode', 'style'],
                subtree: true
              });
              document.addEventListener('click', function() { notifySoon(120); }, true);
              if (window.matchMedia) {
                try {
                  window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', function() {
                    notifySoon(80);
                  });
                } catch (e) {}
              }
            })();
        """.trimIndent()
        wv.evaluateJavascript(observerJs, null)
    }

    private fun detectAndApplyDynamicStatusBar(view: WebView?) {
        val wv = view ?: return
        if (!config.javaScriptEnabled) return
        val js = """
            (function() {
              try {
                function isVisibleColor(c) {
                  if (!c || c === 'transparent' || c === 'rgba(0, 0, 0, 0)') return false;
                  var m = c.match(/rgba?\((\d+),\s*(\d+),\s*(\d+)(?:,\s*([\d.]+))?\)/);
                  if (m && m[4] !== undefined && parseFloat(m[4]) < 0.2) return false;
                  return true;
                }
                var topEl = document.elementFromPoint(Math.floor(window.innerWidth / 2), 12);
                var cur = topEl;
                while (cur && cur !== document) {
                  var bg = getComputedStyle(cur).backgroundColor;
                  if (isVisibleColor(bg)) return bg;
                  cur = cur.parentElement;
                }
                var headerSelectors = ['header', 'nav', '[role="banner"]', 'mat-toolbar'];
                for (var i = 0; i < headerSelectors.length; i++) {
                  var el = document.querySelector(headerSelectors[i]);
                  if (el) {
                    var hbg = getComputedStyle(el).backgroundColor;
                    if (isVisibleColor(hbg)) return hbg;
                  }
                }
                var meta = document.querySelector('meta[name="theme-color"]');
                if (meta && meta.content) return meta.content.trim();
                var bodyBg = getComputedStyle(document.body).backgroundColor;
                if (isVisibleColor(bodyBg)) return bodyBg;
                var htmlBg = getComputedStyle(document.documentElement).backgroundColor;
                if (isVisibleColor(htmlBg)) return htmlBg;
              } catch (e) {}
              return "";
            })();
        """.trimIndent()
        wv.evaluateJavascript(js) { rawResult ->
            val cleaned = rawResult?.trim()?.trim('"')?.trim() ?: return@evaluateJavascript
            if (cleaned.isEmpty() || cleaned == "null") return@evaluateJavascript
            val parsed = parseCssColor(cleaned) ?: return@evaluateJavascript
            dynamicColorApplied = true
            applyStatusBar(parsed)
        }
    }

    private fun parseCssColor(css: String): Int? {
        if (css.startsWith("#")) {
            return runCatching { Color.parseColor(css) }.getOrNull()
        }
        val match = Regex("""rgba?\(\s*(\d+)\s*,\s*(\d+)\s*,\s*(\d+)""").find(css) ?: return null
        val r = match.groupValues[1].toIntOrNull()?.coerceIn(0, 255) ?: return null
        val g = match.groupValues[2].toIntOrNull()?.coerceIn(0, 255) ?: return null
        val b = match.groupValues[3].toIntOrNull()?.coerceIn(0, 255) ?: return null
        return Color.rgb(r, g, b)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun attachFloatingBubble(parent: FrameLayout) {
        val btnSize = dp(48)
        val margin = dp(12)
        var isMenuOpen = false

        val menuCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            visibility = View.GONE
            alpha = 0f
            scaleX = 0.85f
            scaleY = 0.85f
            elevation = dp(10).toFloat()
            setPadding(dp(8), dp(8), dp(8), dp(8))
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#F21E293B"))
                cornerRadius = dp(22).toFloat()
                setStroke(dp(1), Color.parseColor("#33FFFFFF"))
            }
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
            }
        }

        val toggleBtn = ImageView(this).apply {
            setImageResource(R.drawable.ic_nav_menu)
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            setPadding(dp(12), dp(12), dp(12), dp(12))
            elevation = dp(8).toFloat()
            layoutParams = FrameLayout.LayoutParams(btnSize, btnSize).apply {
                gravity = Gravity.TOP or Gravity.START
            }
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(Color.parseColor("#0D9488"), Color.parseColor("#10B981"))
            ).apply {
                shape = GradientDrawable.OVAL
                setStroke(dp(2), Color.parseColor("#CCFFFFFF"))
            }
        }

        fun closeMenu() {
            if (!isMenuOpen) return
            isMenuOpen = false
            toggleBtn.animate()
                .rotation(0f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(180L)
                .withStartAction { toggleBtn.setImageResource(R.drawable.ic_nav_menu) }
                .start()
            menuCard.animate()
                .alpha(0f)
                .scaleX(0.85f)
                .scaleY(0.85f)
                .setDuration(160L)
                .withEndAction { menuCard.visibility = View.GONE }
                .start()
        }

        fun openMenu() {
            if (isMenuOpen) return
            isMenuOpen = true
            menuCard.measure(
                View.MeasureSpec.makeMeasureSpec(parent.width, View.MeasureSpec.AT_MOST),
                View.MeasureSpec.makeMeasureSpec(parent.height, View.MeasureSpec.AT_MOST)
            )
            val menuW = menuCard.measuredWidth.coerceAtLeast(dp(52))
            val menuH = menuCard.measuredHeight.coerceAtLeast(dp(200))

            val btnX = toggleBtn.translationX
            val btnY = toggleBtn.translationY
            val menuX = (btnX + (btnSize - menuW) / 2f)
                .coerceIn(margin.toFloat(), (parent.width - menuW - margin).toFloat().coerceAtLeast(margin.toFloat()))
            val showAbove = btnY > (parent.height / 2f)
            val rawMenuY = if (showAbove) btnY - menuH - dp(10) else btnY + btnSize + dp(10)
            val menuY = rawMenuY.coerceIn(
                margin.toFloat(),
                (parent.height - menuH - margin).toFloat().coerceAtLeast(margin.toFloat())
            )

            menuCard.translationX = menuX
            menuCard.translationY = menuY
            menuCard.pivotX = menuW / 2f
            menuCard.pivotY = if (showAbove) menuH.toFloat() else 0f
            menuCard.visibility = View.VISIBLE
            menuCard.alpha = 0f
            menuCard.scaleX = 0.8f
            menuCard.scaleY = 0.8f

            toggleBtn.setImageResource(R.drawable.ic_nav_close)
            toggleBtn.animate()
                .rotation(90f)
                .scaleX(1.06f)
                .scaleY(1.06f)
                .setDuration(220L)
                .setInterpolator(OvershootInterpolator(1.3f))
                .start()

            menuCard.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(220L)
                .setInterpolator(OvershootInterpolator(1.1f))
                .start()
        }

        fun menuActionBtn(iconRes: Int, onClick: () -> Unit): ImageView {
            return ImageView(this).apply {
                setImageResource(iconRes)
                scaleType = ImageView.ScaleType.CENTER_INSIDE
                setPadding(dp(9), dp(9), dp(9), dp(9))
                layoutParams = LinearLayout.LayoutParams(dp(40), dp(40)).apply {
                    topMargin = dp(3)
                    bottomMargin = dp(3)
                }
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(Color.parseColor("#1FFFFFFF"))
                }
                setOnClickListener {
                    onClick()
                    closeMenu()
                }
            }.also { floatingMenuIcons.add(it) }
        }

        menuCard.addView(menuActionBtn(R.drawable.ic_nav_back) { if (webView.canGoBack()) webView.goBack() })
        menuCard.addView(menuActionBtn(R.drawable.ic_nav_forward) { if (webView.canGoForward()) webView.goForward() })
        menuCard.addView(menuActionBtn(R.drawable.ic_nav_refresh) { webView.reload() })
        menuCard.addView(menuActionBtn(R.drawable.ic_nav_home) { loadTarget() })
        menuCard.addView(menuActionBtn(R.drawable.ic_nav_share) {
            val shareUrl = webView.url ?: config.url
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, shareUrl)
            }
            runCatching { startActivity(Intent.createChooser(intent, "Share")) }
        })

        parent.addView(menuCard)
        parent.addView(toggleBtn)
        floatingMenuCard = menuCard
        floatingToggleBtn = toggleBtn
        val initColor = if (config.splashEnabled) config.splashBgColor else config.statusBarColor
        updateFloatingBubbleTheme(initColor, ColorUtils.calculateLuminance(initColor) > 0.5)

        // Position at right-middle once parent is laid out
        parent.post {
            val pw = parent.width.takeIf { it > 0 } ?: resources.displayMetrics.widthPixels
            val ph = parent.height.takeIf { it > 0 } ?: resources.displayMetrics.heightPixels
            toggleBtn.translationX = (pw - btnSize - margin).toFloat()
            toggleBtn.translationY = ((ph - btnSize) / 2f)
        }

        var dX = 0f
        var dY = 0f
        var downRawX = 0f
        var downRawY = 0f
        var isDragging = false

        toggleBtn.setOnTouchListener { _, event ->
            val maxX = (parent.width - btnSize - margin).toFloat().coerceAtLeast(margin.toFloat())
            val maxY = (parent.height - btnSize - margin).toFloat().coerceAtLeast(margin.toFloat())
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    dX = toggleBtn.translationX - event.rawX
                    dY = toggleBtn.translationY - event.rawY
                    downRawX = event.rawX
                    downRawY = event.rawY
                    isDragging = false
                    toggleBtn.animate().scaleX(0.92f).scaleY(0.92f).setDuration(80L).start()
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val moved = kotlin.math.hypot(
                        (event.rawX - downRawX).toDouble(),
                        (event.rawY - downRawY).toDouble()
                    )
                    if (moved > dp(6)) {
                        isDragging = true
                        if (isMenuOpen) closeMenu()
                        toggleBtn.translationX = (event.rawX + dX).coerceIn(margin.toFloat(), maxX)
                        toggleBtn.translationY = (event.rawY + dY).coerceIn(margin.toFloat(), maxY)
                    }
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if (!isDragging) {
                        if (isMenuOpen) closeMenu() else openMenu()
                    } else {
                        // Snap smoothly to nearest left or right edge
                        val snapX = if (toggleBtn.translationX + btnSize / 2f < parent.width / 2f) {
                            margin.toFloat()
                        } else {
                            maxX
                        }
                        toggleBtn.animate()
                            .translationX(snapX)
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(240L)
                            .setInterpolator(DecelerateInterpolator())
                            .start()
                    }
                    true
                }
                else -> false
            }
        }
    }

    private fun injectCustomCssAndJs(view: WebView?) {
        val wv = view ?: return
        if (!config.javaScriptEnabled) return
        val css = config.customCss.trim()
        if (css.isNotEmpty()) {
            val escaped = css.replace("\\", "\\\\").replace("`", "\\`").replace("$", "\\$")
            wv.evaluateJavascript(
                "(function(){var s=document.getElementById('apkmint-css');if(!s){s=document.createElement('style');s.id='apkmint-css';document.head.appendChild(s);}s.textContent=`$escaped`;})();",
                null
            )
        }
        val js = config.customJs.trim()
        if (js.isNotEmpty()) {
            wv.evaluateJavascript(js, null)
        }
    }

    private fun showStyledExitDialog() {
        val isLight = ColorUtils.calculateLuminance(currentThemeColor) > 0.5
        val cardBg = if (isLight) Color.WHITE else Color.parseColor("#1E293B")
        val titleColor = if (isLight) Color.parseColor("#0F172A") else Color.WHITE
        val subtitleColor = if (isLight) Color.parseColor("#64748B") else Color.parseColor("#94A3B8")
        val stayBg = if (isLight) Color.parseColor("#F1F5F9") else Color.parseColor("#334155")
        val stayFg = if (isLight) Color.parseColor("#334155") else Color.WHITE

        val dialog = Dialog(this)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(24), dp(24), dp(24), dp(20))
            background = GradientDrawable().apply {
                setColor(cardBg)
                cornerRadius = dp(24).toFloat()
            }
            alpha = 0f
            scaleX = 0.88f
            scaleY = 0.88f
        }

        val iconHolder = FrameLayout(this).apply {
            val size = dp(60)
            layoutParams = LinearLayout.LayoutParams(size, size).apply { bottomMargin = dp(14) }
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#1AEF4444"))
            }
            val iv = ImageView(this@TemplateMainActivity).apply {
                layoutParams = FrameLayout.LayoutParams(dp(38), dp(38), Gravity.CENTER)
                val appIcon = runCatching { packageManager.getApplicationIcon(packageName) }.getOrNull()
                if (appIcon != null) setImageDrawable(appIcon) else setImageResource(R.mipmap.ic_launcher)
            }
            addView(iv)
        }
        card.addView(iconHolder)

        val titleView = TextView(this).apply {
            text = "Exit ${config.appName}?"
            setTextColor(titleColor)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 19f)
            setTypeface(null, Typeface.BOLD)
            gravity = Gravity.CENTER
        }
        card.addView(titleView)

        val msgView = TextView(this).apply {
            text = "Are you sure you want to close this app?"
            setTextColor(subtitleColor)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            gravity = Gravity.CENTER
            setPadding(0, dp(6), 0, dp(22))
        }
        card.addView(msgView)

        val btnRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        val stayBtn = TextView(this).apply {
            text = "Stay"
            setTextColor(stayFg)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            setTypeface(null, Typeface.BOLD)
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(0, dp(46), 1f).apply { marginEnd = dp(8) }
            background = GradientDrawable().apply {
                setColor(stayBg)
                cornerRadius = dp(14).toFloat()
            }
            setOnClickListener { dialog.dismiss() }
        }

        val exitBtn = TextView(this).apply {
            text = "Exit"
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            setTypeface(null, Typeface.BOLD)
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(0, dp(46), 1f).apply { marginStart = dp(8) }
            background = GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(Color.parseColor("#EF4444"), Color.parseColor("#DC2626"))
            ).apply {
                cornerRadius = dp(14).toFloat()
            }
            setOnClickListener {
                dialog.dismiss()
                finish()
            }
        }

        btnRow.addView(stayBtn)
        btnRow.addView(exitBtn)
        card.addView(btnRow)

        dialog.setContentView(card)
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.84).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog.show()

        card.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(220L)
            .setInterpolator(OvershootInterpolator(1.1f))
            .start()
    }

    private fun dp(value: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            value.toFloat(),
            resources.displayMetrics
        ).toInt()
    }
}
