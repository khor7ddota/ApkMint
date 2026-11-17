package com.apkmint.creator.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.apkmint.creator.engine.BuiltApkInfo
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MyApksScreen(
    apks: List<BuiltApkInfo>,
    onEditRebuild: (BuiltApkInfo) -> Unit,
    onDelete: (BuiltApkInfo) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    if (apks.isEmpty()) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Android,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                modifier = Modifier.size(72.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text("No APKs Built Yet", style = MaterialTheme.typography.titleMedium)
            Text(
                "Create your first APK from a Website URL or HTML file in the Create tab.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(apks, key = { it.filePath }) { item ->
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Android,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.appName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            if (item.packageName.isNotBlank()) {
                                Text(
                                    text = item.packageName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = "v${item.versionName} • ${formatSize(item.sizeBytes)} • ${formatDate(item.createdAtMillis)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { ApkIntentHelper.installApk(context, item.filePath) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.InstallMobile, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Install")
                        }
                        IconButton(onClick = { onEditRebuild(item) }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit & Rebuild")
                        }
                        IconButton(onClick = { ApkIntentHelper.shareApk(context, item.filePath, item.appName) }) {
                            Icon(Icons.Default.Share, contentDescription = "Share")
                        }
                        IconButton(onClick = { onDelete(item) }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatDate(millis: Long): String {
    return SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()).format(Date(millis))
}
