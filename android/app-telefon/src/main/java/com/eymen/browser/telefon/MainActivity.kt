package com.eymen.browser.telefon

import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Bundle
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
    private var videoFullScreen = false
    private var originalOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    private var originalSystemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        view = GeckoView(this)
        setContentView(view)
        session = GeckoMotorDenemesi.attach(
            this, view, GeckoMotorDenemesi.urlFromIntent(intent),
            onFullScreen = ::setVideoFullScreen
        )
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
