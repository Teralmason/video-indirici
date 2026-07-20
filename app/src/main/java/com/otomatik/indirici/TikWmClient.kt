package com.otomatik.indirici

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

// TikTok'a özel, ücretsiz TikWM API istemcisi.
// Cobalt'tan önce TikTok linkleri için bu deneniyor.
object TikWmClient {

    private const val USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Safari/537.36"

    var lastError: String = ""
        private set

    fun resolveVideoUrl(tiktokLink: String): String? {
        var conn: HttpURLConnection? = null
        return try {
            val encoded = URLEncoder.encode(tiktokLink, "UTF-8")
            val apiUrl = "https://www.tikwm.com/api/?url=$encoded"

            conn = URL(apiUrl).openConnection() as HttpURLConnection
            conn.setRequestProperty("User-Agent", USER_AGENT)
            conn.connectTimeout = 15000
            conn.readTimeout = 20000

            val code = conn.responseCode
            if (code !in 200..299) {
                lastError = "TikWM HTTP $code"
                return null
            }

            val text = conn.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(text)

            if (json.optInt("code", -1) != 0) {
                lastError = "TikWM hata: ${json.optString("msg")}"
                return null
            }

            val data = json.optJSONObject("data")
            if (data == null) {
                lastError = "TikWM: data alanı yok"
                return null
            }

            // hdplay = HD & filigransız, play = normal & filigransız (öncelik hdplay'de)
            val videoPath = data.optString("hdplay").ifBlank { data.optString("play") }
            if (videoPath.isBlank()) {
                lastError = "TikWM: video linki boş döndü"
                return null
            }

            if (videoPath.startsWith("http")) videoPath else "https://www.tikwm.com$videoPath"
        } catch (e: Exception) {
            lastError = "TikWM istisna: ${e.javaClass.simpleName} - ${e.message}"
            null
        } finally {
            conn?.disconnect()
        }
    }
}
