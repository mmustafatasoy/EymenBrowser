package com.eymen.browser.telefon

import java.net.URI
import java.text.Normalizer
import java.util.Locale

internal data class WebAppSpec(val name:String,val startUrl:String,val scope:String)

/** The installed entry is always a validated same-origin HTTPS app. */
internal object WebAppPolicy {
    private fun url(value:String):URI?=try {
        if(value.length !in 1..4096 || value.any{it.isWhitespace() || Character.isISOControl(it)})null
        else URI(value).normalize().takeIf{it.scheme.equals("https",true) && !it.host.isNullOrBlank() && it.userInfo==null && it.port in -1..65535 &&
            !Regex("(?i)%2f|%5c|%2e").containsMatchIn(it.rawPath.orEmpty())}
    }catch(_:Exception){null}
    private fun origin(uri:URI)="https://"+uri.host.lowercase(Locale.ROOT)+if(uri.port!=-1 && uri.port!=443)":"+uri.port else ""
    fun withinScope(scope:String,target:String):Boolean {
        val base=url(scope) ?: return false;val destination=url(target) ?: return false
        if(origin(base)!=origin(destination))return false
        val prefix=base.path.ifEmpty{"/"};val path=destination.path.ifEmpty{"/"}
        return path==prefix || path.startsWith(if(prefix.endsWith('/'))prefix else "$prefix/")
    }
    fun candidate(document:String,start:String,scope:String,name:String):WebAppSpec? {
        val doc=url(document) ?: return null
        val launch=url(try{doc.resolve(start.ifBlank{document}).toString()}catch(_:Exception){return null}) ?: return null
        val area=url(try{launch.resolve(scope.ifBlank{"."}).toString()}catch(_:Exception){return null}) ?: return null
        if(origin(doc)!=origin(launch) || origin(doc)!=origin(area) || !withinScope(area.toString(),launch.toString()))return null
        var title=Normalizer.normalize(name,Normalizer.Form.NFC).filter{!Character.isISOControl(it) && Character.getType(it)!=Character.FORMAT.toInt()}.trim().take(40)
        if(title.lastOrNull()?.isHighSurrogate()==true)title=title.dropLast(1)
        if(title.isBlank())title=launch.host
        return WebAppSpec(title,launch.toASCIIString(),area.toASCIIString())
    }
}
