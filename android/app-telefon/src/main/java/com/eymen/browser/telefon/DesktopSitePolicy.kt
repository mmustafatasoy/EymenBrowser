package com.eymen.browser.telefon

import java.net.URI

/** Standard desktop layout for a single known site. Never spoof account credentials or other origins. */
internal object DesktopSitePolicy {
    fun preferDesktop(url: String?): Boolean = try {
        val uri = URI(url ?: "")
        uri.scheme.equals("https", ignoreCase = true) &&
            uri.host.equals("web.whatsapp.com", ignoreCase = true) &&
            uri.userInfo == null
    } catch (_: Exception) { false }
}
