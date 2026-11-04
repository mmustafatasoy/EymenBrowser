package com.eymen.browser.tv

import android.app.AlertDialog
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import com.eymen.browser.motor.GeckoMotorDenemesi
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoView

/** Aşama 1: Android TV GeckoView motor denemesi (henüz V01–V03 değil). */
class MainActivity : ComponentActivity() {
    private lateinit var view: GeckoView
    private var session: GeckoSession? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        view = GeckoView(this)
        setContentView(view)
        session = GeckoMotorDenemesi.attach(
            this, view, GeckoMotorDenemesi.urlFromIntent(intent),
            onFullScreen = { enabled ->
                if (enabled) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
                }
            },
            askDrmPermission = ::showDrmAccessPrompt
        )
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        GeckoMotorDenemesi.onConfigurationChanged(newConfig)
    }

    /**
     * Temporary Stage-1 native site-permission prompt (not an approved UI screen).
     * The user always chooses: no DRM key-system access is auto-granted.
     */
    private fun showDrmAccessPrompt(host: String, answer: (Boolean) -> Unit) {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.drm_permission_title))
            .setMessage(getString(R.string.drm_permission_message, host))
            .setPositiveButton(getString(R.string.drm_permission_allow)) { _, _ -> answer(true) }
            .setNegativeButton(getString(R.string.drm_permission_deny)) { _, _ -> answer(false) }
            .setOnCancelListener { answer(false) }
            .show()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        GeckoMotorDenemesi.urlFromIntent(intent)?.let { url ->
            session?.loadUri(url)
        }
    }

    override fun onDestroy() {
        GeckoMotorDenemesi.detach(view, session)
        session = null
        super.onDestroy()
    }
}
