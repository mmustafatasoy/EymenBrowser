package com.eymen.browser.telefon

/** Feature 40. Only the explicitly selected, built-in HTTPS resolver can be configured. */
internal enum class SecureDnsOption(val key:String,val endpoint:String?) {
    SYSTEM("system",null),
    CLOUDFLARE("cloudflare","https://cloudflare-dns.com/dns-query"),
    QUAD9("quad9","https://dns.quad9.net/dns-query");
    companion object {fun fromKey(key:String?):SecureDnsOption=entries.firstOrNull{it.key==key}?:SYSTEM}
}
