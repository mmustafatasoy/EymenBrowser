package com.eymen.browser.telefon

import java.net.URI
import java.time.Instant
import java.time.ZoneId

internal data class HistoryEntry(val id: Long, val url: String, val title: String, val visitedAt: Long)

/** Only completed regular web navigations may enter the local history database. */
internal object HistoryPolicy {
    const val MAX_ENTRIES = 10000
    const val RETENTION_DAYS = 90L
    fun safeUrl(raw: String, privateMode: Boolean): String? {
        if (privateMode || raw.length > 8192 || raw.any { it.isISOControl() }) return null
        return try {
            val uri = URI(raw)
            if (uri.scheme?.lowercase() !in setOf("https", "http") || uri.host.isNullOrBlank() ||
                uri.userInfo != null || uri.port !in -1..65535) null
            else raw.substringBefore('#')
        } catch (_: Exception) { null }
    }
    fun title(raw: String, url: String): String = raw.replace(Regex("[\\p{Cntrl}\\s]+"), " ")
        .trim().take(512).ifEmpty { try { URI(url).host.orEmpty() } catch (_: Exception) { url } }
    fun dayKey(at: Long, zone: ZoneId = ZoneId.systemDefault()): String =
        Instant.ofEpochMilli(at).atZone(zone).toLocalDate().toString()
    fun cutoff(now: Long): Long = now - RETENTION_DAYS * 24 * 60 * 60 * 1000
}
