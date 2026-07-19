package com.otomatik.indirici

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

// Cobalt (açık kaynak video indirme servisi) API istemcisi.
// Kendi scraping/regex mantığımız yerine bu servisi kullanıyoruz.
object CobaltClient {

    // Bilinen topluluk sunucuları - biri çalışmazsa sıradaki denenir
    private val INSTANCES = listOf(
        "co.wuk.sh",
        "cobalt-api.hyper.lol",
        "cobalt.api.timelessnesses.me",
        "api-dl.cgm.rs",
        "capi.oak.li",
        "co.tskau.team"
    )

    private const val USER_AGENT = "VideoIndirici/1.0"

    // Sırayla dener, ilk başarılı sonucu döndürür
    fun resolveVideoUrl(pageUrl: String): String? {
        for (instance in INSTANCES) {
            val result = tryInstance(instance, pageUrl)
            if (result != null) return result
        }
        return null
    }

    private fun tryInstance(instance: String, pageUrl: String): String? {
        val apiUrl = "https://$instance/"
        var conn: HttpURLConnection? = null
        return try {
            conn = URL(apiUrl).openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Accept", "application/json")
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("User-Agent", USER_AGENT)
            conn.doOutput = true
            conn.connectTimeout = 10000
            conn.readTimeout = 15000

            val body = JSONObject().apply { put("url", pageUrl) }.toString()
            conn.outputStream.use { it.write(body.toByteArray()) }

            if (conn.responseCode !in 200..299) return null

            val responseText = conn.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(responseText)

            when (json.optString("status")) {
                "tunnel", "redirect", "stream" -> {
                    val url = json.optString("url")
                    if (url.isNotBlank()) url else null
                }
                "picker" -> {
                    val picker = json.optJSONArray("picker")
                    if (picker != null && picker.length() > 0) {
                        picker.getJSONObject(0).optString("url").ifBlank { null }
                    } else null
                }
                else -> null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            conn?.disconnect()
        }
    }
}
