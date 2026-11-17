package com.apkmint.creator.ui

import android.annotation.SuppressLint
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.apkmint.creator.engine.ApkBuildConfig
import com.apkmint.creator.engine.SourceMode

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun LivePreviewDialog(
    config: ApkBuildConfig,
    onDismiss: () -> Unit
) {
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var menuExpanded by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Live Preview • ${config.appName.ifBlank { "App" }}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close Preview")
                    }
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { ctx ->
                            WebView(ctx).apply {
                                settings.javaScriptEnabled = config.javaScriptEnabled
                                settings.domStorageEnabled = config.domStorageEnabled
                                webViewClient = WebViewClient()
                                loadPreviewTarget(this, config)
                                webViewRef = this
                            }
                        }
                    )

                    if (config.showTopToolbar) {
                        Column(
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .padding(end = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AnimatedVisibility(
                                visible = menuExpanded,
                                enter = fadeIn() + scaleIn(),
                                exit = fadeOut() + scaleOut()
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(22.dp),
                                    color = Color(0xF01E293B),
                                    tonalElevation = 6.dp
                                ) {
                                    Column(
                                        modifier = Modifier.padding(6.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        IconButton(onClick = {
                                            webViewRef?.let { if (it.canGoBack()) it.goBack() }
                                            menuExpanded = false
                                        }) {
                                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                                        }
                                        IconButton(onClick = {
                                            webViewRef?.let { if (it.canGoForward()) it.goForward() }
                                            menuExpanded = false
                                        }) {
                                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Forward", tint = Color.White)
                                        }
                                        IconButton(onClick = {
                                            webViewRef?.reload()
                                            menuExpanded = false
                                        }) {
                                            Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = Color.White)
                                        }
                                        IconButton(onClick = {
                                            webViewRef?.let { loadPreviewTarget(it, config) }
                                            menuExpanded = false
                                        }) {
                                            Icon(Icons.Default.Home, contentDescription = "Home", tint = Color.White)
                                        }
                                    }
                                }
                            }

                            FloatingActionButton(
                                onClick = { menuExpanded = !menuExpanded },
                                shape = CircleShape,
                                containerColor = Color(0xFF0D9488),
                                contentColor = Color.White,
                                modifier = Modifier
                                    .size(48.dp)
                                    .border(2.dp, Color.White.copy(alpha = 0.8f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = if (menuExpanded) Icons.Default.Close else Icons.Default.Menu,
                                    contentDescription = "Floating Menu"
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun loadPreviewTarget(wv: WebView, config: ApkBuildConfig) {
    if (config.sourceMode == SourceMode.URL) {
        val raw = config.url.trim()
        val url = if (raw.startsWith("http://", true) || raw.startsWith("https://", true)) {
            raw
        } else {
            "https://$raw"
        }
        wv.loadUrl(url)
    } else {
        wv.loadDataWithBaseURL("https://apkmint.local/", config.htmlCode, "text/html", "UTF-8", null)
    }
}
