package com.eymen.browser.telefon

import android.app.AlertDialog
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Bundle
import android.provider.Settings
import android.widget.FrameLayout
import androidx.compose.ui.platform.ComposeView
import android.view.View
import android.view.WindowManager
import androidx.activity.ComponentActivity
import com.eymen.browser.motor.GeckoMotorDenemesi
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoView

/**
 * Aşama 1: GeckoView motor testi.
 * Bir HTTPS sayfasını Android Paylaş/Birlikte Aç üzerinden kabul eder.
 * Video tam ekran / yatay dönüşte GeckoSession yaşam döngüsü korunur.
 * Nihai T01–T11 arayüzü Kapı 1 sonrasına aittir.
 */
class MainActivity : ComponentActivity() {
    private lateinit var view: GeckoView
    private var session: GeckoSession? = null
    private val downloads = AndroidDownloadDenemesi(this)
    private var videoFullScreen = false
    private var originalOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    private var originalSystemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = FrameLayout(this)
        view = GeckoView(this)
        root.addView(view, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ))
        setContentView(root)
        session = GeckoMotorDenemesi.attach(
            this, view, GeckoMotorDenemesi.urlFromIntent(intent),
            onFullScreen = ::setVideoFullScreen,
            askDrmPermission = ::showDrmAccessPrompt,
            onDownload = downloads::handleResponse
        )

        // Aşama 2'nin ilk dilimi: yalnız ilk kurulumda T01/T02 üst katman.
        // GeckoSession aynen çalışır ve kullanıcı Başla dediğinde korunur.
        val preferences = getSharedPreferences("eymen_browser_setup", MODE_PRIVATE)
        if (!preferences.getBoolean("setup_completed", false)) {
            val onboarding = ComposeView(this)
            root.addView(onboarding, FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            ))
            onboarding.setContent {
                FirstRunOverlay { shield, searchEngine, addressBottom, requestDefault ->
                    preferences.edit()
                        .putBoolean("shield_wanted", shield)
                        .putString("search_engine", searchEngine)
                        .putBoolean("address_at_bottom", addressBottom)
                        .putBoolean("setup_completed", true)
                        .apply()
                    root.removeView(onboarding)
                    if (requestDefault) {
                        // Sistem seçimi: varsayılan tarayıcı olma garantisi verilmez.
                        try {
                            startActivity(Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS))
                        } catch (_: Exception) {
                            // Seçim tamamlandı; sistem ekranı olmayan cihazlarda
                            // kullanıcı normal tarayıcı oturumuna döner.
                        }
                    }
                }
            }
        }
    }

    // Rotation MUST NOT destroy the Activity/GeckoSession: manifest configChanges
    // owns screenSize, smallestScreenSize, screenLayout, orientation, keyboardHidden.
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        GeckoMotorDenemesi.onConfigurationChanged(newConfig)
    }

    @Suppress("DEPRECATION")
    private fun setVideoFullScreen(enabled: Boolean) {
        if (videoFullScreen == enabled) return
        videoFullScreen = enabled
        if (enabled) {
            originalOrientation = requestedOrientation
            originalSystemUiVisibility = window.decorView.systemUiVisibility
            window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
            window.decorView.systemUiVisibility = originalSystemUiVisibility or
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            // Natural landscape video, preserving the existing GeckoSession.
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        } else {
            requestedOrientation = originalOrientation
            window.decorView.systemUiVisibility = originalSystemUiVisibility
            window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        }
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
        downloads.dispose()
        GeckoMotorDenemesi.detach(view, session)
        session = null
        super.onDestroy()
    }
}
