package com.apkmint.creator.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL

object WebIconFetcher {

    private val ICON_LINK_REGEX = Regex(
        """<link\b[^>]*rel\s*=\s*["']([^"']*icon[^"']*)["'][^>]*>""",
        RegexOption.IGNORE_CASE
    )
    private val HREF_REGEX = Regex(
        """href\s*=\s*["']([^"']+)["']""",
        RegexOption.IGNORE_CASE
    )

    suspend fun fetchWebsiteIcon(context: Context, rawUrl: String): Result<Pair<Uri, Bitmap>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val normalized = normalizeUrl(rawUrl)
                val baseUri = URI(normalized)
                val host = baseUri.host ?: error("Invalid website URL")

                val candidates = mutableListOf<String>()

                // 1. Try parsing <link rel="icon" / "apple-touch-icon"> from page HTML
                val html = downloadText(normalized)
                if (html != null) {
                    val foundIcons = mutableListOf<Pair<Int, String>>()
                    ICON_LINK_REGEX.findAll(html).forEach { match ->
                        val tag = match.value
                        val rel = match.groupValues[1].lowercase()
                        val href = HREF_REGEX.find(tag)?.groupValues?.get(1)?.trim()
                        if (!href.isNullOrEmpty() && !href.startsWith("data:")) {
                            val resolved = runCatching { baseUri.resolve(href).toString() }.getOrNull()
                            if (resolved != null) {
                                val priority = if ("apple-touch-icon" in rel) 0 else 1
                                foundIcons.add(priority to resolved)
                            }
                        }
                    }
                    foundIcons.sortedBy { it.first }.forEach { candidates.add(it.second) }
                }

                // 2. Standard root paths & high-res Google Favicon fallback
                val origin = "${baseUri.scheme}://${baseUri.authority}"
                candidates.add("$origin/apple-touch-icon.png")
                candidates.add("https://www.google.com/s2/favicons?domain=$host&sz=256")
                candidates.add("$origin/favicon.ico")

                var decodedBitmap: Bitmap? = null
                for (candidateUrl in candidates.distinct()) {
                    val bytes = downloadBytes(candidateUrl) ?: continue
                    val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    if (bmp != null && bmp.width >= 16 && bmp.height >= 16) {
                        decodedBitmap = bmp
                        break
                    }
                }

                val rawBitmap = decodedBitmap ?: error("Could not find a valid icon for $host")
                val framedBitmap = frameForAdaptiveIcon(rawBitmap)
                if (framedBitmap !== rawBitmap) rawBitmap.recycle()

                val outFile = File(context.cacheDir, "web_fetched_icon_${System.currentTimeMillis()}.png")
                FileOutputStream(outFile).use { fos ->
                    framedBitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
                }
                Uri.fromFile(outFile) to framedBitmap
            }
        }

    private fun frameForAdaptiveIcon(src: Bitmap): Bitmap {
        val size = 512
        val out = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)

        val corner = src.getPixel(0, 0)
        val bgColor = if (Color.alpha(corner) < 128) Color.WHITE else corner
        canvas.drawColor(bgColor)

        val inset = size * 0.27f
        val dst = RectF(inset, inset, size - inset, size - inset)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        canvas.drawBitmap(src, null, dst, paint)
        return out
    }

    private fun normalizeUrl(raw: String): String {
        val trimmed = raw.trim()
        return if (trimmed.startsWith("http://", true) || trimmed.startsWith("https://", true)) {
            trimmed
        } else {
            "https://$trimmed"
        }
    }

    private fun downloadText(urlStr: String): String? = runCatching {
        val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
            connectTimeout = 6000
            readTimeout = 6000
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "Mozilla/5.0 (Android) ApkMint/1.0")
        }
        conn.inputStream.bufferedReader().use { it.readText().take(200_000) }
    }.getOrNull()

    private fun downloadBytes(urlStr: String): ByteArray? = runCatching {
        val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
            connectTimeout = 6000
            readTimeout = 6000
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "Mozilla/5.0 (Android) ApkMint/1.0")
        }
        if (conn.responseCode in 200..299) {
            conn.inputStream.use { it.readBytes() }
        } else {
            null
        }
    }.getOrNull()
}
