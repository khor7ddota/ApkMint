package com.apkmint.creator.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File

object ApkIntentHelper {

    fun installApk(context: Context, filePath: String) {
        val file = File(filePath)
        if (!file.exists()) {
            Toast.makeText(context, "APK file not found", Toast.LENGTH_SHORT).show()
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            !context.packageManager.canRequestPackageInstalls()
        ) {
            Toast.makeText(
                context,
                "Please allow 'Install unknown apps' for ApkMint, then tap Install again.",
                Toast.LENGTH_LONG
            ).show()
            val permIntent = Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${context.packageName}")
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(permIntent)
            return
        }

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching {
            context.startActivity(intent)
        }.onFailure {
            Toast.makeText(context, "Unable to launch installer: ${it.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun shareApk(context: Context, filePath: String, appName: String) {
        val file = File(filePath)
        if (!file.exists()) {
            Toast.makeText(context, "APK file not found", Toast.LENGTH_SHORT).show()
            return
        }
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/vnd.android.package-archive"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "$appName.apk")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share APK"))
    }
}
