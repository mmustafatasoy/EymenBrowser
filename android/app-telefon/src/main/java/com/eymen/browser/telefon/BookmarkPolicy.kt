package com.eymen.browser.telefon

import java.net.URI
import java.text.Normalizer

internal data class BookmarkEntry(val id: Long, val url: String, val title: String, val folderId: Long?)
internal data class BookmarkFolder(val id: Long, val name: String)

/** Feature 12. Bookmarks are an explicit user action, never harvested from private history. */
internal object BookmarkPolicy {
    const val MAX_ENTRIES = 2000
    const val MAX_FOLDERS = 100
    fun url(raw: String): String? = try {
        raw.trim().takeIf { it.length <= 16384 }?.let { value ->
            val uri = URI(value)
            value.takeIf { uri.scheme.equals("https", true) && !uri.host.isNullOrBlank() &&
                uri.rawUserInfo == null && value.none { it.isISOControl() } }
        }
    } catch (_: Exception) { null }
    fun name(raw: String): String {
        val bounded=Normalizer.normalize(raw, Normalizer.Form.NFC).filterNot {it.isISOControl()}.trim().take(120)
        return if(bounded.lastOrNull()?.isHighSurrogate()==true)bounded.dropLast(1) else bounded
    }
    fun title(raw: String, url: String): String = name(raw).ifEmpty {
        try { URI(url).host.orEmpty() } catch (_: Exception) { url.take(120) }
    }
}
