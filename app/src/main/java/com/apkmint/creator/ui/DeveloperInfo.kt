package com.apkmint.creator.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

object DeveloperInfo {
    const val NAME = "Khorshed Alom"
    const val GITHUB_URL = "https://github.com/khor7ddota"
    const val EMAIL = "khor7d.a@gmail.com"
}

@Composable
fun DeveloperCreditCard(
    onOpenDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        onClick = onOpenDialog,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color(0xFF0F766E), Color(0xFF10B981))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "KA",
                    color = Color.White,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Developed by ${DeveloperInfo.NAME}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${DeveloperInfo.EMAIL} • GitHub",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                contentDescription = "Developer Info",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
fun AboutDeveloperDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFF0F766E), Color(0xFF10B981))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text("KA", color = Color.White, fontWeight = FontWeight.Bold)
                }
                Column {
                    Text(DeveloperInfo.NAME, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        "Creator of ApkMint",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { openUrl(context, DeveloperInfo.GITHUB_URL) }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Default.Code, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Column(modifier = Modifier.weight(1f)) {
                            Text("GitHub", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            Text(DeveloperInfo.GITHUB_URL, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { openEmail(context, DeveloperInfo.EMAIL) }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Default.Email, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Email", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            Text(DeveloperInfo.EMAIL, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                Spacer(Modifier.height(2.dp))
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

private fun openUrl(context: Context, url: String) {
    runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }.onFailure {
        Toast.makeText(context, "Unable to open link", Toast.LENGTH_SHORT).show()
    }
}

private fun openEmail(context: Context, email: String) {
    runCatching {
        context.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$email")))
    }.onFailure {
        Toast.makeText(context, "No email app found", Toast.LENGTH_SHORT).show()
    }
}
