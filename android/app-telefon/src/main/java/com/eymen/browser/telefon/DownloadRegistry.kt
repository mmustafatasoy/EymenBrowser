package com.eymen.browser.telefon

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Only user-approved completed downloads appear; no simulated progress or files. */
internal data class SavedDownload(
    val uri: String,
    val name: String,
    val mimeType: String,
    val bytes: Long,
    val savedAt: Long
)

internal class DownloadRegistry(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(
        "eymen_browser_completed_downloads_v1", Context.MODE_PRIVATE
    )
    @Synchronized
    fun items(): List<SavedDownload> {
        val json = prefs.getString("completed", "[]") ?: "[]"
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).mapNotNull { index ->
                val obj = arr.optJSONObject(index) ?: return@mapNotNull null
                val uri = obj.optString("uri")
                val name = obj.optString("name")
                if (!uri.startsWith("content://") || name.isBlank()) return@mapNotNull null
                SavedDownload(
                    uri, name, obj.optString("mime", "application/octet-stream"),
                    obj.optLong("bytes").coerceAtLeast(0),
                    obj.optLong("time").coerceAtLeast(0)
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    @Synchronized
    fun record(file: SavedDownload) {
        if (!file.uri.startsWith("content://") || file.name.isBlank()) return
        val merged = (listOf(file) + items().filter { it.uri != file.uri }).take(100)
        val array = JSONArray()
        merged.forEach { download ->
            array.put(JSONObject().apply {
                put("uri", download.uri)
                put("name", download.name)
                put("mime", download.mimeType)
                put("bytes", download.bytes)
                put("time", download.savedAt)
            })
        }
        prefs.edit().putString("completed", array.toString()).apply()
    }

    @Synchronized
    fun clearListOnly() {
        // Never delete the actual content:// file when clearing the history.
        prefs.edit().remove("completed").apply()
    }
}
