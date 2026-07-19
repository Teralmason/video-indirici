package com.otomatik.indirici

import android.app.Activity
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast

// Bu Activity görünmezdir. Tile'a basınca açılır, panoyu okur, indirir, kapanır.
class TriggerActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onResume() {
        super.onResume()

        // Activity artık odakta, pano şimdi okunabilir
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = clipboard.primaryClip
        val link = if (clip != null && clip.itemCount > 0) clip.getItemAt(0).text?.toString() else null

        if (link.isNullOrBlank() || !link.startsWith("http")) {
            Toast.makeText(this, "Panoda geçerli bir link bulunamadı", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        Toast.makeText(this, "İndiriliyor...", Toast.LENGTH_SHORT).show()
        val mainHandler = Handler(Looper.getMainLooper())

        Thread {
            val success = try {
                VideoDownloader.downloadFromLink(applicationContext, link)
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
            mainHandler.post {
                val message = if (success) "Video galeriye kaydedildi ✅" else "Video bulunamadı / indirilemedi ❌"
                Toast.makeText(applicationContext, message, Toast.LENGTH_LONG).show()
                finish()
            }
        }.start()
    }
}
