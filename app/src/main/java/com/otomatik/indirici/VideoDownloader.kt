package com.otomatik.indirici

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import java.net.HttpURLConnection
import java.net.URL

sealed class DownloadResult {
    object Success : DownloadResult()
    data class Failure(val reason: String) : DownloadResult()
}

object VideoDownloader {

    private const val USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Safari/537.36"

    fun downloadFromLink(context: Context, link: String): DownloadResult {
        val videoUrl = if (link.contains("tiktok.com", ignoreCase = true)) {
            // TikTok linki ise önce TikWM'i dene, o başarısız olursa Cobalt'a düş
            TikWmClient.resolveVideoUrl(link) ?: CobaltClient.resolveVideoUrl(link)
        } else {
            CobaltClient.resolveVideoUrl(link)
        }

        if (videoUrl == null) {
            return DownloadResult.Failure(
                "Video linki alınamadı. TikWM: ${TikWmClient.lastError} | Cobalt: ${CobaltClient.lastError}"
            )
        }

        val bytes = fetchBytes(videoUrl)
            ?: return DownloadResult.Failure("İndirme başarısız. Detay: $lastFetchError")

        if (bytes.size < 20_000) {
            return DownloadResult.Failure("İndirilen dosya çok küçük (${bytes.size} byte) - muhtemelen video değil, hata sayfası")
        }

        return try {
            saveToGallery(context, bytes)
            DownloadResult.Success
        } catch (e: Exception) {
            DownloadResult.Failure("Galeriye kaydetme hatası: ${e.message}")
        }
    }

    var lastFetchError: String = ""
        private set

    private fun fetchBytes(videoUrl: String): ByteArray? {
        var conn: HttpURLConnection? = null
        return try {
            conn = URL(videoUrl).openConnection() as HttpURLConnection
            conn.setRequestProperty("User-Agent", USER_AGENT)
            if (videoUrl.contains("tiktok", ignoreCase = true) ||
                videoUrl.contains("byteoversea", ignoreCase = true) ||
                videoUrl.contains("bytecdn", ignoreCase = true) ||
                videoUrl.contains("muscdn", ignoreCase = true)
            ) {
                conn.setRequestProperty("Referer", "https://www.tiktok.com/")
            }
            conn.connectTimeout = 15000
            conn.readTimeout = 30000
            conn.instanceFollowRedirects = true

            val code = conn.responseCode
            if (code !in 200..299) {
                val errBody = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                lastFetchError = "HTTP $code döndü. URL: ${videoUrl.take(100)} Gövde: ${errBody.take(150)}"
                return null
            }
            conn.inputStream.use { it.readBytes() }
        } catch (e: Exception) {
            lastFetchError = "${e.javaClass.simpleName}: ${e.message} - URL: ${videoUrl.take(100)}"
            null
        } finally {
            conn?.disconnect()
        }
    }

    private fun saveToGallery(context: Context, bytes: ByteArray) {
        val fileName = "video_${System.currentTimeMillis()}.mp4"
        val resolver = context.contentResolver
        val collection = MediaStore.Video.Media.EXTERNAL_CONTENT_URI

        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
            put(MediaStore.MediaColumns.DATE_ADDED, System.currentTimeMillis() / 1000)
            put(MediaStore.MediaColumns.DATE_MODIFIED, System.currentTimeMillis() / 1000)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, "Movies/VideoIndirici")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
                put(MediaStore.MediaColumns.DATE_TAKEN, System.currentTimeMillis())
            }
        }

        val uri = resolver.insert(collection, values) ?: throw Exception("MediaStore insert başarısız")

        resolver.openOutputStream(uri)?.use { out ->
            out.write(bytes)
            out.flush()
        } ?: throw Exception("OutputStream açılamadı")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val doneValues = ContentValues().apply {
                put(MediaStore.MediaColumns.IS_PENDING, 0)
            }
            resolver.update(uri, doneValues, null, null)
        }
    }
}
