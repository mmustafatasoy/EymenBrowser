package com.eymen.browser.motor

import android.content.Context
import android.content.res.Configuration
import android.content.Intent
import org.mozilla.geckoview.ContentBlocking
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoSessionSettings
import org.mozilla.geckoview.GeckoView
import org.mozilla.geckoview.WebResponse
import java.net.URI
import java.util.Locale

/** Aşama 1: HTTPS motor testi, onaylı tasarım ekranlarının uygulaması değildir. */
object GeckoMotorDenemesi {
    private const val DEFAULT_URL = "https://example.org/"
    private var runtime: GeckoRuntime? = null
    @Volatile private var strictTrackingMode: Boolean = false

    @Synchronized
    private fun getRuntime(context: Context): GeckoRuntime =
        runtime ?: GeckoRuntime.create(context.applicationContext).also { runtime = it }

    /**
     * Global Gecko tracking list policy. Individual website exceptions are
     * still enforced per GeckoSession via setUseTrackingProtection(enabled).
     * This blocks *known trackers*, not every cosmetic ad or sponsored item.
     */
    fun configureTrackerProtection(context: Context, strict: Boolean) {
        strictTrackingMode = strict
        val content = getRuntime(context).settings.contentBlocking
        content.setAntiTracking(
            if (strict) ContentBlocking.AntiTracking.STRICT
            else ContentBlocking.AntiTracking.DEFAULT
        )
        content.setEnhancedTrackingProtectionLevel(
            if (strict) ContentBlocking.EtpLevel.STRICT
            else ContentBlocking.EtpLevel.DEFAULT
        )
        content.setStrictSocialTrackingProtection(strict)
    }

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

    /**
     * Keep the same GeckoSession during an Activity orientation/configuration change.
     * Only the Activity should manage display flags; the engine remains the same.
     */
    fun onConfigurationChanged(config: Configuration) {
        runtime?.configurationChanged(config)
        runtime?.orientationChanged()
    }

    fun attach(
        context: Context,
        view: GeckoView,
        url: String?,
        onFullScreen: (Boolean) -> Unit,
        askDrmPermission: (String, (Boolean) -> Unit) -> Unit,
        onDownload: (WebResponse) -> Unit = { response -> response.body?.close(); Unit },
        privateMode: Boolean = false,
        initialLoad: Boolean = true,
        onTitle: (String?) -> Unit = {},
        trackingProtection: Boolean = false,
        onBlocked: (Int) -> Unit = { _ -> Unit }
    ): GeckoSession {
        val settings = GeckoSessionSettings.Builder()
            .usePrivateMode(privateMode)
            .useTrackingProtection(trackingProtection)
            .build()
        val session = GeckoSession(settings)
        session.setContentBlockingDelegate(object : ContentBlocking.Delegate {
            override fun onContentBlocked(session: GeckoSession, event: ContentBlocking.BlockEvent) {
                // Do not count "could have blocked" or allowed resources.
                if (event.isBlocking) onBlocked(event.antiTrackingCategory)
            }
        })
        session.contentDelegate = object : GeckoSession.ContentDelegate {
            override fun onTitleChange(session: GeckoSession, title: String?) {
                onTitle(title)
            }

            override fun onFullScreen(session: GeckoSession, fullScreen: Boolean) {
                onFullScreen(fullScreen)
            }

            override fun onExternalResponse(session: GeckoSession, response: WebResponse) {
                // GeckoView PDF viewer's Download button and regular file
                // downloads arrive here with an already authenticated body.
                // Do not perform a second unauthenticated HTTP request.
                onDownload(response)
            }
        }
        // Only the Widevine/DRM key-system site permission is handled here.
        // All other permissions keep GeckoView's normal default behavior.
        session.permissionDelegate = object : GeckoSession.PermissionDelegate {
            override fun onContentPermissionRequest(
                session: GeckoSession,
                perm: GeckoSession.PermissionDelegate.ContentPermission
            ): GeckoResult<Int>? {
                if (perm.permission !=
                    GeckoSession.PermissionDelegate.PERMISSION_MEDIA_KEY_SYSTEM_ACCESS
                ) return null

                // Reject non-HTTPS origins instead of silently granting DRM access.
                val origin = try {
                    val u = URI(perm.uri)
                    if (u.scheme?.lowercase(Locale.ROOT) == "https" &&
                        !u.host.isNullOrBlank() && u.userInfo == null
                    ) u.host else null
                } catch (_: Exception) {
                    null
                }
                if (origin == null) {
                    return GeckoResult.fromValue(
                        GeckoSession.PermissionDelegate.ContentPermission.VALUE_DENY
                    )
                }

                val pending = GeckoResult<Int>()
                try {
                    // The callback must display a user-controlled native permission dialog.
                    askDrmPermission(origin) { allowed ->
                        pending.complete(
                            if (allowed) GeckoSession.PermissionDelegate.ContentPermission.VALUE_ALLOW
                            else GeckoSession.PermissionDelegate.ContentPermission.VALUE_DENY
                        )
                    }
                    // Dialog.show() has returned, so the prompt is now visible.
                    perm.notifyShown()
                } catch (_: Exception) {
                    pending.complete(
                        GeckoSession.PermissionDelegate.ContentPermission.VALUE_DENY
                    )
                }
                return pending
            }
        }
        val geckoRuntime = getRuntime(context)
        // Gecko categorizes known trackers. Individual GeckoSessions opt in/out.
        geckoRuntime.settings.contentBlocking.setAntiTracking(
            if (strictTrackingMode) ContentBlocking.AntiTracking.STRICT
            else ContentBlocking.AntiTracking.DEFAULT
        )
        session.open(geckoRuntime)
        view.setSession(session)
        if (initialLoad || url != null) {
            session.loadUri(verifiedHttpsUrl(url) ?: DEFAULT_URL)
        }
        return session
    }

    fun detach(view: GeckoView, session: GeckoSession?) {
        if (session == null) return
        view.releaseSession()
        session.close()
    }
}
