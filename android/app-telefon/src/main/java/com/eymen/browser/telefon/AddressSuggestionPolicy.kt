package com.eymen.browser.telefon

internal data class AddressSuggestion(val url: String, val title: String, val search: Boolean = false)

/** Feature 9. Local library suggestions; no keystroke leaves the device. */
internal object AddressSuggestionPolicy {
    fun find(raw: String, candidates: List<AddressSuggestion>, privateMode: Boolean): List<AddressSuggestion> {
        val query=TabSearchPolicy.query(raw)
        if(privateMode || query.length<2)return emptyList()
        return candidates.asSequence().filter {BookmarkPolicy.url(it.url)==it.url}
            .filter {TabSearchPolicy.matches(it.title,it.url,query)}.distinctBy {it.url}.take(5).toList()
    }
}
