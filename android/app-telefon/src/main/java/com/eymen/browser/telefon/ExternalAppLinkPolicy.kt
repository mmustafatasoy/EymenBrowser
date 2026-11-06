package com.eymen.browser.telefon

import android.content.Intent
import android.net.Uri
import com.eymen.browser.motor.GeckoMotorDenemesi
import java.net.URI
import java.util.Locale

/**
 * Only hand off a deliberately clicked link from an HTTPS top-level document.
 * NEVER silently follow a web redirect into another Android app.
 *
 * Rebuild intents from URI and validated package; never forward browser-page
 * controlled Intent flags, selectors, components, grant flags or extras.
 */
internal object ExternalAppLinkPolicy {
    internal data class Handoff(
        val intent: Intent,
        val originHost: String,
        val destinationScheme: String,
        val packageName: String?
    )

    private val packageSyntax =
        Regex("""(?:[a-zA-Z][a-zA-Z0-9_]*\.)+[a-zA-Z][a-zA-Z0-9_]*""")
    private val schemeSyntax = Regex("""[a-z][a-z0-9+.-]{1,39}""")
    private val blockedSchemes = setOf(
        "http", "file", "content", "data", "javascript", "about",
        "blob", "chrome", "resource", "ftp", "market", "tel",
        "sms", "mailto", "intent", "android-app"
    )

    fun fromWebsite(
        rawLink: String,
        sourceUrl: String?,
        hasUserGesture: Boolean
    ): Handoff? {
        if (!hasUserGesture || rawLink.length !in 3..2048 ||
            rawLink.any { it.isWhitespace() || it.code < 32 || it.code == 127 }
        ) return null
        val source = GeckoMotorDenemesi.verifiedHttpsUrl(sourceUrl)
        if (source == null || source != sourceUrl) return null
        val host = try { URI(source).host?.lowercase(Locale.ROOT) } catch (_: Exception) { null }
            ?: return null
        return try {
            val parsed: Intent
            val dest: Uri
            val packageName: String?
            if (rawLink.startsWith("intent:", ignoreCase = true)) {
                parsed = Intent.parseUri(rawLink, Intent.URI_INTENT_SCHEME)
                packageName = parsed.`package`
                if (packageName == null || !packageSyntax.matches(packageName)) return null
                dest = parsed.data ?: return null
            } else {
                packageName = null
                dest = Uri.parse(rawLink)
                parsed = Intent(Intent.ACTION_VIEW, dest)
            }
            val scheme = dest.scheme?.lowercase(Locale.ROOT) ?: return null
            if (!schemeSyntax.matches(scheme) || scheme in blockedSchemes) return null
            val uri = URI(dest.toString())
            if (uri.userInfo != null) return null

            val safe = Intent(Intent.ACTION_VIEW, dest).apply {
                addCategory(Intent.CATEGORY_BROWSABLE)
                // Never inherit flags, component, selector or third-party extras.
                if (packageName != null) setPackage(packageName)
            }
            Handoff(safe, host, scheme, packageName)
        } catch (_: Exception) {
            null
        }
    }
}
