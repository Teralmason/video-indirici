package com.otomatik.indirici

import android.app.Activity
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast

class TriggerActivity : Activity() {

    private var handled = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (!hasFocus || handled) return
        handled = true
        readClipboardAndDownload()
    }

    private fun readClipboardAndDownload() {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = clipboard.primaryClip
        val link = if (clip != null && clip.itemCount > 0) clip.getItemAt(0).text?.toString() else null

        if (link.isNullOrBlank() || !link.startsWith("http")) {
            Toast.makeText(this, "Panoda geçerli bir link bulunamadı", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        Toast.makeText(this, "İndiriliyor: ${link.take(40)}...", Toast.LENGTH_SHORT).show()
        val mainHandler = Handler(Looper.getMainLooper())

        Thread {
            val result = try {
                VideoDownloader.downloadFromLink(applicationContext, link)
            } catch (e: Exception) {
                DownloadResult.Failure("Beklenmeyen hata: ${e.message}")
            }
            mainHandler.post {
                val message = when (result) {
                    is DownloadResult.Success -> "Video galeriye kaydedildi ✅"
                    is DownloadResult.Failure -> "❌ ${result.reason}"
                }
                Toast.makeText(applicationContext, message, Toast.LENGTH_LONG).show()
                finish()
            }
        }.start()
    }
}
