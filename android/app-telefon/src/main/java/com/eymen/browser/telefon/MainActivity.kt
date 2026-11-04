package com.eymen.browser.telefon

import android.app.AlertDialog
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import com.eymen.browser.motor.GeckoMotorDenemesi
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoView
import java.net.URLEncoder
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Android Aşama 2 – T01/T02 ilk kurulum ve T03/T04/T11 gerçek GeckoView kabuğu.
 * Oturum, DRM izni, Android PDF indirme ve video tam ekranı korunur.
 * Sekme yöneticisi ve filtre motoru sonraki aşamalardır.
 */
class MainActivity : ComponentActivity() {
    private lateinit var root: FrameLayout
    private lateinit var view: GeckoView
    private lateinit var chrome: ComposeView
    private lateinit var homeLayer: ComposeView
    private lateinit var prefs: SharedPreferences
    private var session: GeckoSession? = null
    private val downloads = AndroidDownloadDenemesi(this)
    private val atHome = mutableStateOf(true)
    private val currentUrl = mutableStateOf("")
    private val dayMode = mutableStateOf(false)
    private val recent = mutableStateListOf<String>()
    private var addressBottom by mutableStateOf(true)
    private var searchEngine = "Google"

    private var videoFullScreen = false
    private var originalOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    private var originalSystemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = getSharedPreferences("eymen_browser_setup", MODE_PRIVATE)
        addressBottom = prefs.getBoolean("address_at_bottom", true)
        searchEngine = prefs.getString("search_engine", "Google") ?: "Google"
        dayMode.value = prefs.getBoolean("day_theme", false)
        val incomingUrl = GeckoMotorDenemesi.urlFromIntent(intent)
        currentUrl.value = incomingUrl ?: ""

        root = FrameLayout(this)
        view = GeckoView(this)
        root.addView(view, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ))
        setContentView(root)

        // Attach the same engine as the verified motor tests. No WebView fallback.
        session = GeckoMotorDenemesi.attach(
            this, view, incomingUrl,
            onFullScreen = ::setVideoFullScreen,
            askDrmPermission = ::showDrmAccessPrompt,
            onDownload = downloads::handleResponse
        )
        session?.progressDelegate = object : GeckoSession.ProgressDelegate {
            override fun onPageStart(session: GeckoSession, url: String) {
                runOnUiThread {
                    currentUrl.value = url
                    if (!atHome.value && url.startsWith("https://", true) &&
                        url != "https://example.org/"
                    ) {
                        recent.remove(url)
                        recent.add(0, url)
                        while (recent.size > 2) recent.removeAt(recent.lastIndex)
                    }
                }
            }
        }

        homeLayer = ComposeView(this)
        root.addView(
            homeLayer,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )
        homeLayer.setContent {
            BrowserHome(
                day = dayMode.value,
                recentUrls = recent,
                addressBottom = addressBottom,
                onOpen = ::openUrl,
                onDevices = { notice(R.string.devices_not_ready) },
                onShield = { notice(R.string.shield_not_ready) }
            )
        }

        chrome = ComposeView(this)
        root.addView(
            chrome,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
        )
        chrome.setContent {
            BrowserToolbar(
                currentUrl = currentUrl.value,
                home = atHome.value,
                day = dayMode.value,
                addressBottom = addressBottom,
                onGo = ::openAddress,
                onBack = { session?.goBack() },
                onForward = { session?.goForward() },
                onHome = { showHome(true) },
                onReload = { session?.reload() },
                onTabs = { notice(R.string.tabs_not_ready) },
                onMenu = ::showBrowserMenu,
                onShield = { notice(R.string.shield_not_ready) }
            )
        }
        showHome(incomingUrl == null)
        updateChromeMargins()

        if (!prefs.getBoolean("setup_completed", false)) {
            val onboarding = ComposeView(this)
            root.addView(
                onboarding,
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
            )
            // First run overrides the chrome but does not destroy GeckoSession.
            onboarding.setContent {
                FirstRunOverlay { shield, engine, bottom, requestDefault ->
                    prefs.edit()
                        .putBoolean("shield_wanted", shield)
                        .putString("search_engine", engine)
                        .putBoolean("address_at_bottom", bottom)
                        .putBoolean("setup_completed", true)
                        .apply()
                    searchEngine = engine
                    addressBottom = bottom
                    root.removeView(onboarding)
                    // Rebuild only chrome layout when address placement changes.
                    chrome.setContent {
                        BrowserToolbar(
                            currentUrl = currentUrl.value,
                            home = atHome.value,
                            day = dayMode.value,
                            addressBottom = addressBottom,
                            onGo = ::openAddress,
                            onBack = { session?.goBack() },
                            onForward = { session?.goForward() },
                            onHome = { showHome(true) },
                            onReload = { session?.reload() },
                            onTabs = { notice(R.string.tabs_not_ready) },
                            onMenu = ::showBrowserMenu,
                            onShield = { notice(R.string.shield_not_ready) }
                        )
                    }
                    updateChromeMargins()
                    if (requestDefault) {
                        try {
                            startActivity(Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS))
                        } catch (_: Exception) {
                            // Some Android versions provide no such settings screen.
                        }
                    }
                }
            }
        }
    }

    /** Never allow javascript:, data:, cleartext http: or userinfo in the bar. */
    private fun openAddress(text: String) {
        val raw = text.trim()
        if (raw.isEmpty()) return
        val direct = if (raw.startsWith("https://", ignoreCase = true)) {
            GeckoMotorDenemesi.verifiedHttpsUrl(raw)
        } else if (raw.contains("://")) {
            null
        } else if ('.' in raw && raw.none { it.isWhitespace() }) {
            GeckoMotorDenemesi.verifiedHttpsUrl("https://$raw")
        } else null
        if (raw.contains("://") && direct == null) {
            notice(R.string.invalid_address)
            return
        }
        val url = direct ?: run {
            val encoded = URLEncoder.encode(raw, "UTF-8")
            when (searchEngine.lowercase(Locale.ROOT)) {
                "duckduckgo" -> "https://duckduckgo.com/?q=$encoded"
                "bing" -> "https://www.bing.com/search?q=$encoded"
                "yandex" -> "https://yandex.com/search/?text=$encoded"
                else -> "https://www.google.com/search?q=$encoded"
            }
        }
        openUrl(url)
    }

    private fun openUrl(url: String) {
        val verified = GeckoMotorDenemesi.verifiedHttpsUrl(url)
        if (verified == null) {
            notice(R.string.invalid_address)
            return
        }
        currentUrl.value = verified
        showHome(false)
        session?.loadUri(verified)
    }

    private fun showHome(enabled: Boolean) {
        atHome.value = enabled
        if (::homeLayer.isInitialized) {
            homeLayer.visibility = if (enabled && !videoFullScreen) View.VISIBLE else View.GONE
        }
    }

    private fun updateChromeMargins() {
        if (!::chrome.isInitialized) return
        val barHeight = (130f * resources.displayMetrics.density).roundToInt()
        val body = view.layoutParams as FrameLayout.LayoutParams
        body.topMargin = if (videoFullScreen || addressBottom) 0 else barHeight
        body.bottomMargin = if (videoFullScreen || !addressBottom) 0 else barHeight
        view.layoutParams = body
        chrome.layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT,
            if (addressBottom) Gravity.BOTTOM else Gravity.TOP
        )
    }

    private fun showBrowserMenu() {
        val choice = if (dayMode.value) R.string.theme_night else R.string.theme_day
        AlertDialog.Builder(this)
            .setTitle(R.string.browser_menu_title)
            .setItems(arrayOf(getString(choice), getString(R.string.menu_current_page))) { _, which ->
                when (which) {
                    0 -> {
                        dayMode.value = !dayMode.value
                        prefs.edit().putBoolean("day_theme", dayMode.value).apply()
                    }
                    1 -> if (!atHome.value) session?.reload()
                }
            }
            .show()
    }

    private fun notice(resId: Int) {
        Toast.makeText(this, getString(resId), Toast.LENGTH_SHORT).show()
    }

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
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        } else {
            requestedOrientation = originalOrientation
            window.decorView.systemUiVisibility = originalSystemUiVisibility
            window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        }
        if (::chrome.isInitialized) chrome.visibility =
            if (enabled) View.GONE else View.VISIBLE
        if (::homeLayer.isInitialized) homeLayer.visibility =
            if (enabled || !atHome.value) View.GONE else View.VISIBLE
        if (::chrome.isInitialized) updateChromeMargins()
    }

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
        GeckoMotorDenemesi.urlFromIntent(intent)?.let { url -> openUrl(url) }
    }

    override fun onDestroy() {
        downloads.dispose()
        GeckoMotorDenemesi.detach(view, session)
        session = null
        super.onDestroy()
    }
}
