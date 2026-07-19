package com.otomatik.indirici

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import java.net.HttpURLConnection
import java.net.URL
import java.util.regex.Pattern

object VideoDownloader {

    private const val USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Safari/537.36"

    fun downloadFromLink(context: Context, link: String): Boolean {
        val videoUrl = resolveVideoUrl(link) ?: return false
        val bytes = fetchBytes(videoUrl) ?: return false
        saveToGallery(context, bytes)
        return true
    }

    // Sayfanın HTML'ini indirip içindeki gerçek video linkini regex ile arıyoruz.
    // Instagram / Twitter genelde og:video meta etiketinde gerçek linki verir.
    // TikTok, sayfa içindeki JSON verisinde "playAddr" alanında tutar.
    private fun resolveVideoUrl(pageUrl: String): String? {
        val html = fetchHtml(pageUrl) ?: return null

        val patterns = listOf(
            "property=\"og:video:secure_url\" content=\"([^\"]+)\"",
            "property=\"og:video\" content=\"([^\"]+)\"",
            "\"playAddr\":\"([^\"]+)\"",
            "\"downloadAddr\":\"([^\"]+)\"",
            "\"video_url\":\"([^\"]+)\""
        )

        for (pattern in patterns) {
            val matcher = Pattern.compile(pattern).matcher(html)
            if (matcher.find()) {
                var url = matcher.group(1) ?: continue
                url = url.replace("\\u0026", "&").replace("\\/", "/")
                if (url.startsWith("http")) return url
            }
        }
        return null
    }

    private fun fetchHtml(pageUrl: String): String? {
        val conn = URL(pageUrl).openConnection() as HttpURLConnection
        conn.setRequestProperty("User-Agent", USER_AGENT)
        conn.connectTimeout = 15000
        conn.readTimeout = 15000
        conn.instanceFollowRedirects = true
        return try {
            conn.inputStream.bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            conn.disconnect()
        }
    }

    private fun fetchBytes(videoUrl: String): ByteArray? {
        val conn = URL(videoUrl).openConnection() as HttpURLConnection
        conn.setRequestProperty("User-Agent", USER_AGENT)
        conn.connectTimeout = 15000
        conn.readTimeout = 30000
        return try {
            conn.inputStream.use { it.readBytes() }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            conn.disconnect()
        }
    }

    private fun saveToGallery(context: Context, bytes: ByteArray) {
        val fileName = "video_${System.currentTimeMillis()}.mp4"
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, "Movies/VideoIndirici")
            }
        }

        val resolver = context.contentResolver
        val collection = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        val uri = resolver.insert(collection, values) ?: throw Exception("MediaStore insert başarısız")

        resolver.openOutputStream(uri)?.use { out ->
            out.write(bytes)
        }
    }
}
