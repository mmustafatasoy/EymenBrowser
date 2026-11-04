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
import androidx.activity.OnBackPressedCallback
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import com.eymen.browser.motor.GeckoMotorDenemesi
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoView
import java.net.URLEncoder
import java.util.Locale
import kotlin.math.roundToInt

private class BrowserTab(
    val id: Long,
    val gecko: GeckoSession,
    val privateMode: Boolean,
    initialUrl: String?
) {
    var url by mutableStateOf(initialUrl.orEmpty())
    var title by mutableStateOf("")
    var home by mutableStateOf(initialUrl == null)
    var fullScreen by mutableStateOf(false)
}

/**
 * Android phone: T01-T04, T06, T09 and T11 implementation stages.
 * GeckoView owns one displayed GeckoSession; background tab sessions stay open.
 * Private sessions are never added to regular history/recent pages.
 */
class MainActivity : ComponentActivity() {
    private lateinit var root: FrameLayout
    private lateinit var view: GeckoView
    private lateinit var chrome: ComposeView
    private lateinit var homeLayer: ComposeView
    private lateinit var tabsLayer: ComposeView
    private lateinit var prefs: SharedPreferences

    private val tabs = mutableStateListOf<BrowserTab>()
    private val activeId = mutableStateOf(-1L)
    private val atHome = mutableStateOf(true)
    private val currentUrl = mutableStateOf("")
    private val dayMode = mutableStateOf(false)
    private val recent = mutableStateListOf<String>()
    private val downloads = AndroidDownloadDenemesi(this)
    private var nextId = 1L
    private var addressBottom by mutableStateOf(true)
    private var searchEngine = "Google"
    private var overviewShowing = false

    private val activeTab: BrowserTab?
        get() = tabs.firstOrNull { it.id == activeId.value }

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

        root = FrameLayout(this)
        view = GeckoView(this)
        root.addView(view, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ))
        setContentView(root)

        // Create the first normal tab; keep the verified DRM, download and
        // rotation callbacks for every new or reopened GeckoSession.
        createTab(privateMode = false, initialUrl = incomingUrl)

        homeLayer = ComposeView(this)
        root.addView(
            homeLayer,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )
        homeLayer.setContent {
            if (activeTab?.privateMode == true) {
                PrivateHome()
            } else {
                BrowserHome(
                    day = dayMode.value,
                    recentUrls = recent,
                    addressBottom = addressBottom,
                    onOpen = ::openUrl,
                    onDevices = ::showTabs,
                    onShield = { notice(R.string.shield_not_ready) }
                )
            }
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
                privateMode = activeTab?.privateMode == true,
                tabCount = tabs.count { it.privateMode == (activeTab?.privateMode == true) },
                onGo = ::openAddress,
                onBack = { activeTab?.gecko?.goBack() },
                onForward = { activeTab?.gecko?.goForward() },
                onHome = { showHome(true) },
                onReload = { activeTab?.gecko?.reload() },
                onTabs = ::showTabs,
                onMenu = ::showBrowserMenu,
                onShield = { notice(R.string.shield_not_ready) }
            )
        }

        tabsLayer = ComposeView(this)
        root.addView(
            tabsLayer,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )
        tabsLayer.setContent {
            TabOverview(
                tabs = tabs.map { tab ->
                    TabPreview(
                        id = tab.id,
                        title = tab.title.ifBlank {
                            if (tab.privateMode) getString(R.string.private_tab)
                            else if (tab.home) getString(R.string.new_tab)
                            else tab.url.removePrefix("https://").substringBefore('/')
                        },
                        url = tab.url,
                        privateMode = tab.privateMode,
                        selected = tab.id == activeId.value
                    )
                },
                activePrivate = activeTab?.privateMode == true,
                day = dayMode.value,
                onOpen = ::activateTab,
                onClose = ::closeTab,
                onCloseAll = ::closeAllOfType,
                onNew = { privateMode -> createTab(privateMode) },
                onDismiss = ::hideTabs
            )
        }
        tabsLayer.visibility = View.GONE
        updateChromeMargins()
        updateHomeVisibility()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                when {
                    overviewShowing -> hideTabs()
                    videoFullScreen -> activeTab?.gecko?.goBack()
                    !atHome.value -> activeTab?.gecko?.goBack()
                    else -> showTabs()
                }
            }
        })

        if (!prefs.getBoolean("setup_completed", false)) {
            val onboarding = ComposeView(this)
            root.addView(
                onboarding,
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
            )
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
                    updateChromeMargins()
                    if (requestDefault) {
                        try {
                            startActivity(Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS))
                        } catch (_: Exception) {
                            // Some Android versions have no default-app settings screen.
                        }
                    }
                }
            }
        }
    }

    /** Existing attached sessions are released, never closed on tab switches. */
    private fun createTab(privateMode: Boolean, initialUrl: String? = null) {
        if (tabs.isNotEmpty()) {
            view.releaseSession()
        }
        val id = nextId++
        val session = GeckoMotorDenemesi.attach(
            this, view, initialUrl,
            onFullScreen = { enabled ->
                tabs.firstOrNull { it.id == id }?.let { tab ->
                    tab.fullScreen = enabled
                    if (activeId.value == id) setVideoFullScreen(enabled)
                }
            },
            askDrmPermission = ::showDrmAccessPrompt,
            onDownload = downloads::handleResponse,
            privateMode = privateMode,
            initialLoad = initialUrl != null,
            onTitle = { title ->
                tabs.firstOrNull { it.id == id }?.title = title.orEmpty()
            }
        )
        val tab = BrowserTab(id, session, privateMode, initialUrl)
        session.progressDelegate = object : GeckoSession.ProgressDelegate {
            override fun onPageStart(session: GeckoSession, url: String) {
                runOnUiThread {
                    tab.url = url
                    tab.home = false
                    if (activeId.value == id) {
                        currentUrl.value = url
                        atHome.value = false
                        updateHomeVisibility()
                    }
                    if (!privateMode && url.startsWith("https://", true) &&
                        url != "https://example.org/"
                    ) {
                        recent.remove(url)
                        recent.add(0, url)
                        while (recent.size > 2) recent.removeAt(recent.lastIndex)
                    }
                }
            }
        }
        tabs.add(tab)
        activeId.value = id
        currentUrl.value = initialUrl.orEmpty()
        atHome.value = initialUrl == null
        if (::tabsLayer.isInitialized) hideTabs()
        updateHomeVisibility()
    }

    private fun activateTab(id: Long) {
        val tab = tabs.firstOrNull { it.id == id } ?: return
        if (activeId.value != id) {
            if (videoFullScreen) setVideoFullScreen(false)
            view.releaseSession()
            view.setSession(tab.gecko)
            activeId.value = id
        }
        currentUrl.value = tab.url
        atHome.value = tab.home
        setVideoFullScreen(tab.fullScreen)
        updateHomeVisibility()
        hideTabs()
    }

    private fun closeTab(id: Long) {
        val tab = tabs.firstOrNull { it.id == id } ?: return
        val active = activeId.value == id
        if (active) {
            if (videoFullScreen) setVideoFullScreen(false)
            view.releaseSession()
        }
        tab.gecko.close()
        tabs.remove(tab)
        if (active) {
            if (tabs.isEmpty()) {
                // Always leave a usable normal tab rather than a blank GeckoView.
                createTab(privateMode = false)
            } else {
                activeId.value = -1L
                activateTab(tabs.last().id)
            }
        }
    }

    private fun closeAllOfType(privateMode: Boolean) {
        tabs.filter { it.privateMode == privateMode }.map { it.id }.forEach(::closeTab)
        if (tabs.isEmpty()) createTab(privateMode = false)
    }

    private fun showTabs() {
        if (!::tabsLayer.isInitialized || videoFullScreen) return
        overviewShowing = true
        tabsLayer.visibility = View.VISIBLE
    }

    private fun hideTabs() {
        overviewShowing = false
        if (::tabsLayer.isInitialized) tabsLayer.visibility = View.GONE
    }

    /** Only validated HTTPS destinations are loaded into GeckoSession. */
    private fun openAddress(text: String) {
        val raw = text.trim()
        if (raw.isEmpty()) return
        val direct = if (raw.startsWith("https://", ignoreCase = true)) {
            GeckoMotorDenemesi.verifiedHttpsUrl(raw)
        } else if (raw.contains("://")) null
        else if ('.' in raw && raw.none { it.isWhitespace() }) {
            GeckoMotorDenemesi.verifiedHttpsUrl("https://$raw")
        } else null
        if (raw.contains("://") && direct == null) {
            notice(R.string.invalid_address)
            return
        }
        val destination = direct ?: run {
            val encoded = URLEncoder.encode(raw, "UTF-8")
            when (searchEngine.lowercase(Locale.ROOT)) {
                "duckduckgo" -> "https://duckduckgo.com/?q=$encoded"
                "bing" -> "https://www.bing.com/search?q=$encoded"
                "yandex" -> "https://yandex.com/search/?text=$encoded"
                else -> "https://www.google.com/search?q=$encoded"
            }
        }
        openUrl(destination)
    }

    private fun openUrl(url: String) {
        val verified = GeckoMotorDenemesi.verifiedHttpsUrl(url)
        if (verified == null) {
            notice(R.string.invalid_address)
            return
        }
        activeTab?.let { tab ->
            tab.url = verified
            tab.home = false
            currentUrl.value = verified
            atHome.value = false
            updateHomeVisibility()
            tab.gecko.loadUri(verified)
        }
    }

    private fun showHome(enabled: Boolean) {
        activeTab?.home = enabled
        atHome.value = enabled
        updateHomeVisibility()
    }

    private fun updateHomeVisibility() {
        if (::homeLayer.isInitialized) {
            homeLayer.visibility =
                if (atHome.value && !videoFullScreen) View.VISIBLE else View.GONE
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
        val themeChoice = if (dayMode.value) R.string.theme_night else R.string.theme_day
        AlertDialog.Builder(this)
            .setTitle(R.string.browser_menu_title)
            .setItems(arrayOf(
                getString(R.string.new_tab),
                getString(R.string.new_private_tab),
                getString(themeChoice),
                getString(R.string.menu_current_page)
            )) { _, choice ->
                when (choice) {
                    0 -> createTab(false)
                    1 -> createTab(true)
                    2 -> {
                        dayMode.value = !dayMode.value
                        prefs.edit().putBoolean("day_theme", dayMode.value).apply()
                    }
                    3 -> if (!atHome.value) activeTab?.gecko?.reload()
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
        if (::chrome.isInitialized) {
            chrome.visibility = if (enabled) View.GONE else View.VISIBLE
            updateChromeMargins()
        }
        updateHomeVisibility()
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
        GeckoMotorDenemesi.urlFromIntent(intent)?.let { url ->
            // Links from another app never silently enter an existing private tab.
            if (activeTab?.privateMode == true) createTab(false, url)
            else openUrl(url)
        }
    }

    override fun onDestroy() {
        downloads.dispose()
        if (::view.isInitialized) view.releaseSession()
        tabs.forEach { it.gecko.close() }
        tabs.clear()
        super.onDestroy()
    }
}
