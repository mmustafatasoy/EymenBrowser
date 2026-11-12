package com.eymen.browser.telefon

import java.text.Normalizer

/** Local matching only. The caller must first select the current tab/privacy/group scope. */
internal object TabSearchPolicy {
    const val MAX_QUERY_CHARS = 256

    fun boundedInput(raw: String): String {
        if (raw.length <= MAX_QUERY_CHARS) return raw
        val clipped = raw.take(MAX_QUERY_CHARS)
        return if (Character.isHighSurrogate(clipped.last())) clipped.dropLast(1) else clipped
    }

    fun query(raw: String): String = Normalizer.normalize(boundedInput(raw), Normalizer.Form.NFC).trim()

    fun matches(title: String, url: String, rawQuery: String): Boolean {
        val term = query(rawQuery)
        if (term.isEmpty()) return true
        return Normalizer.normalize(title, Normalizer.Form.NFC).contains(term, ignoreCase = true) ||
            Normalizer.normalize(url, Normalizer.Form.NFC).contains(term, ignoreCase = true)
    }
}
