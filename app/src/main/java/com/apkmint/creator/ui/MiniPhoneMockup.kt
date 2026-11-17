package com.apkmint.creator.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.apkmint.creator.engine.ApkBuildConfig

@Composable
fun MiniPhoneMockup(
    config: ApkBuildConfig,
    iconBitmap: Bitmap?,
    modifier: Modifier = Modifier
) {
    val statusColor = parseComposeColor(
        if (config.splashEnabled) config.splashBgColorHex else config.statusBarColorHex,
        Color(0xFF1565C0)
    )
    val statusContentColor = if (statusColor.luminance() > 0.5f) Color(0xFF111827) else Color.White
    val splashBg = parseComposeColor(config.splashBgColorHex, Color.White)
    val splashText = parseComposeColor(config.splashTextColorHex, Color(0xFF111827))
    val progressColor = parseComposeColor(config.progressBarColorHex, Color(0xFF10B981))

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .width(176.dp)
                .height(230.dp)
                .clip(RoundedCornerShape(22.dp))
                .border(3.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(22.dp))
                .background(if (config.splashEnabled) splashBg else Color.White)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Status Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(statusColor)
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("12:30", color = statusContentColor, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        Icon(Icons.Default.Wifi, contentDescription = null, tint = statusContentColor, modifier = Modifier.size(10.dp))
                        Icon(Icons.Default.BatteryFull, contentDescription = null, tint = statusContentColor, modifier = Modifier.size(10.dp))
                    }
                }

                // Loading Progress Bar Preview
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.65f)
                        .height(3.dp)
                        .background(progressColor)
                )

                // Body: Splash or Page Preview
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    if (config.splashEnabled) {
                        if (iconBitmap != null) {
                            Image(
                                bitmap = iconBitmap.asImageBitmap(),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Android,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = config.appName.ifBlank { "My App" },
                            color = splashText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        Text(
                            text = "Splash (${config.splashDurationMs / 1000f}s)",
                            color = splashText.copy(alpha = 0.6f),
                            fontSize = 9.sp
                        )
                    } else {
                        Text(
                            text = if (config.dynamicStatusBar) "Dynamic Status Bar Active" else "Web Content View",
                            color = Color(0xFF475569),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            if (config.showTopToolbar) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 6.dp)
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF0D9488))
                        .border(1.dp, Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
    }
}

internal fun parseComposeColor(hex: String, fallback: Color): Color {
    return runCatching { Color(android.graphics.Color.parseColor(hex.trim())) }.getOrDefault(fallback)
}
