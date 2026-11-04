package com.eymen.browser.motor

import android.content.Context
import android.content.Intent
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoView
import java.net.URI
import java.util.Locale

/** Aşama 1: HTTPS motor testi, onaylı tasarım ekranlarının uygulaması değildir. */
object GeckoMotorDenemesi {
    private const val DEFAULT_URL = "https://example.org/"
    private var runtime: GeckoRuntime? = null

    @Synchronized
    private fun getRuntime(context: Context): GeckoRuntime =
        runtime ?: GeckoRuntime.create(context.applicationContext).also { runtime = it }

    /**
     * Only HTTPS URLs are accepted from untrusted Android share/open intents.
     * A shared message can contain a title followed by a link.
     * No URLs are printed to logs or stored as telemetry.
     */
    fun verifiedHttpsUrl(input: String?): String? {
        val raw = input?.trim()?.takeIf { it.isNotEmpty() && it.length <= 4096 } ?: return null
        val candidate = Regex("""https://[^\s<>]+""", RegexOption.IGNORE_CASE)
            .find(raw)?.value?.trimEnd('.', ',', ')') ?: return null
        return try {
            val url = URI(candidate)
            val host = url.host
            if (url.scheme?.lowercase(Locale.ROOT) == "https" &&
                !host.isNullOrBlank() &&
                url.userInfo == null &&
                host.length <= 253 &&
                url.port in -1..65535
            ) url.toASCIIString() else null
        } catch (_: Exception) {
            null
        }
    }

    fun urlFromIntent(intent: Intent?): String? {
        if (intent == null) return null
        val candidate = when (intent.action) {
            Intent.ACTION_VIEW -> intent.dataString
            Intent.ACTION_SEND -> if (intent.type == "text/plain")
                intent.getStringExtra(Intent.EXTRA_TEXT) else null
            Intent.ACTION_MAIN, null -> intent.getStringExtra("test_url")
            else -> null
        }
        return verifiedHttpsUrl(candidate)
    }

    fun attach(context: Context, view: GeckoView, url: String?): GeckoSession {
        val session = GeckoSession()
        session.contentDelegate = object : GeckoSession.ContentDelegate {}
        session.open(getRuntime(context))
        view.setSession(session)
        session.loadUri(verifiedHttpsUrl(url) ?: DEFAULT_URL)
        return session
    }

    fun detach(view: GeckoView, session: GeckoSession?) {
        if (session == null) return
        view.releaseSession()
        session.close()
    }
}
