package com.eymen.browser.motor

import android.content.Context
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoView
import java.net.URI

/** Aşama 1: HTTPS motor testi, onaylı tasarım ekranlarının uygulaması değildir. */
object GeckoMotorDenemesi {
    private const val DEFAULT_URL = "https://example.org/"
    private var runtime: GeckoRuntime? = null

    @Synchronized
    private fun getRuntime(context: Context): GeckoRuntime =
        runtime ?: GeckoRuntime.create(context.applicationContext).also { runtime = it }

    private fun validatedUrl(input: String?): String = try {
        val u = URI(input?.trim().orEmpty())
        if (u.scheme == "https" && !u.host.isNullOrEmpty() && u.userInfo == null) u.toString()
        else DEFAULT_URL
    } catch (_: Exception) { DEFAULT_URL }

    fun attach(context: Context, view: GeckoView, url: String?): GeckoSession {
        val session = GeckoSession()
        session.contentDelegate = object : GeckoSession.ContentDelegate {}
        session.open(getRuntime(context))
        view.setSession(session)
        session.loadUri(validatedUrl(url))
        return session
    }

    fun detach(view: GeckoView, session: GeckoSession?) {
        if (session == null) return
        view.releaseSession()
        session.close()
    }
}
