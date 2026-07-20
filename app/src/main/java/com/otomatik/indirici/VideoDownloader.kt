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
        val videoUrl = CobaltClient.resolveVideoUrl(link)
            ?: return DownloadResult.Failure("Video linki alınamadı. Detay: ${CobaltClient.lastError}")

        val bytes = fetchBytes(videoUrl)
            ?: return DownloadResult.Failure("Video linki bulundu ama dosya indirilemedi")

        return try {
            saveToGallery(context, bytes)
            DownloadResult.Success
        } catch (e: Exception) {
            DownloadResult.Failure("Galeriye kaydetme hatası: ${e.message}")
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
