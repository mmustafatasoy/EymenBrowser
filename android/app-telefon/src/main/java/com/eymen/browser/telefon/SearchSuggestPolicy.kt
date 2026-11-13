package com.eymen.browser.telefon

/** No network requests from private/child browsing, addresses, or non-Google providers. */
internal object SearchSuggestPolicy {
    fun eligible(raw:String,engine:String,privateMode:Boolean,childMode:Boolean,enabled:Boolean):String? {
        if(!enabled || privateMode || childMode || !engine.equals("Google",true))return null
        val query=TabSearchPolicy.query(raw)
        if(query.isEmpty() || query.length>100 || query.contains("://") ||
            (query.contains('.') && query.none{it.isWhitespace()}) ||
            query.any{it.isISOControl()})return null
        return query
    }
}
