package com.eymen.browser.telefon

import android.app.AlertDialog
import android.app.DownloadManager
import android.content.Intent
import android.net.Uri
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
import android.widget.EditText
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import com.eymen.browser.motor.GeckoMotorDenemesi
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoSessionSettings
import org.mozilla.geckoview.GeckoView
import java.net.URI
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
    var desktop by mutableStateOf(false)
    var shieldEnabled by mutableStateOf(false)
    var blockedPage by mutableStateOf(0)
    var blockedTab by mutableStateOf(0)
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
    private lateinit var menuLayer: ComposeView
    private lateinit var shieldLayer: ComposeView
    private lateinit var downloadsLayer: ComposeView
    private lateinit var settingsLayer: ComposeView
    private lateinit var prefs: SharedPreferences

    private val tabs = mutableStateListOf<BrowserTab>()
    private val activeId = mutableStateOf(-1L)
    private val atHome = mutableStateOf(true)
    private val currentUrl = mutableStateOf("")
    private val dayMode = mutableStateOf(false)
    private val recent = mutableStateListOf<String>()
    private val savedFiles = mutableStateListOf<SavedDownload>()
    private val downloadRegistry by lazy { DownloadRegistry(this) }
    private val downloads = AndroidDownloadDenemesi(this)
    private var nextId = 1L
    private var addressBottom by mutableStateOf(true)
    private var searchEngine by mutableStateOf("Google")
    private var overviewShowing = false
    private var menuShowing = false
    private var shieldShowing = false
    private var downloadsShowing = false
    private var settingsShowing = false
    private val globalShieldWanted = mutableStateOf(true)
    private val devicesRequested = mutableStateOf(false)

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
        globalShieldWanted.value = prefs.getBoolean("shield_wanted", true)
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
                    shieldEnabled = globalShieldWanted.value,
                    realBlockedCount = tabs.filter { !it.privateMode }.sumOf { it.blockedTab },
                    onOpen = ::openUrl,
                    onDevices = ::showDevices,
                    onShield = ::showShield
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
                onShield = ::showShield
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
                openDevices = devicesRequested.value,
                day = dayMode.value,
                onOpen = ::activateTab,
                onClose = ::closeTab,
                onCloseAll = ::closeAllOfType,
                onNew = { privateMode -> createTab(privateMode) },
                onDismiss = ::hideTabs
            )
        }
        tabsLayer.visibility = View.GONE
        menuLayer = ComposeView(this)
        root.addView(
            menuLayer,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )
        menuLayer.setContent {
            BrowserMenu(
                hasPage = !atHome.value && GeckoMotorDenemesi.verifiedHttpsUrl(activeTab?.url) != null,
                isDesktop = activeTab?.desktop == true,
                dayMode = dayMode.value,
                onDismiss = ::hideBrowserMenu,
                onAction = ::handleMenuAction
            )
        }
        menuLayer.visibility = View.GONE
        shieldLayer = ComposeView(this)
        root.addView(shieldLayer, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ))
        shieldLayer.setContent {
            val tab = activeTab
            ShieldPanel(
                host = verifiedHost(tab?.url),
                enabled = tab?.shieldEnabled ?: globalShieldWanted.value,
                pageBlocked = tab?.blockedPage ?: 0,
                tabBlocked = tab?.blockedTab ?: 0,
                canToggle = tab != null && !tab.home && verifiedHost(tab.url) != null,
                onDismiss = ::hideShield,
                onToggle = ::setShieldForCurrentSite
            )
        }
        shieldLayer.visibility = View.GONE
        downloadsLayer = ComposeView(this)
        root.addView(downloadsLayer, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ))
        downloadsLayer.setContent {
            DownloadsScreen(
                files = savedFiles,
                onBack = ::hideDownloads,
                onOpen = ::openDownloadedFile,
                onClear = ::confirmClearDownloadedFiles
            )
        }
        downloadsLayer.visibility = View.GONE
        settingsLayer = ComposeView(this)
        root.addView(settingsLayer, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ))
        settingsLayer.setContent {
            SettingsScreen(
                globalShield = globalShieldWanted.value,
                searchEngine = searchEngine,
                dayMode = dayMode.value,
                onBack = ::hideSettings,
                onAction = ::handleSettingsAction
            )
        }
        settingsLayer.visibility = View.GONE
        updateChromeMargins()
        updateHomeVisibility()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                when {
                    settingsShowing -> hideSettings()
                    downloadsShowing -> hideDownloads()
                    shieldShowing -> hideShield()
                    menuShowing -> hideBrowserMenu()
                    overviewShowing -> hideTabs()
                    videoFullScreen -> activeTab?.gecko?.exitFullScreen()
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
                    globalShieldWanted.value = shield
                    tabs.forEach { tab ->
                        tab.shieldEnabled = protectionFor(tab.url)
                        tab.gecko.settings.setUseTrackingProtection(tab.shieldEnabled)
                    }
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
            trackingProtection = protectionFor(initialUrl),
            onBlocked = {
                runOnUiThread {
                    tabs.firstOrNull { it.id == id }?.let { tab ->
                        tab.blockedPage += 1
                        tab.blockedTab += 1
                    }
                }
            },
            onTitle = { title ->
                tabs.firstOrNull { it.id == id }?.title = title.orEmpty()
            }
        )
        val tab = BrowserTab(id, session, privateMode, initialUrl)
        tab.shieldEnabled = protectionFor(initialUrl)
        session.progressDelegate = object : GeckoSession.ProgressDelegate {
            override fun onPageStart(session: GeckoSession, url: String) {
                runOnUiThread {
                    tab.url = url
                    tab.home = false
                    tab.blockedPage = 0
                    tab.shieldEnabled = protectionFor(url)
                    tab.gecko.settings.setUseTrackingProtection(tab.shieldEnabled)
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

    private fun showTabs() = openTabOverview(false)

    private fun showDevices() = openTabOverview(true)

    private fun openTabOverview(devices: Boolean) {
        if (!::tabsLayer.isInitialized || videoFullScreen) return
        devicesRequested.value = devices
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
            tab.shieldEnabled = protectionFor(verified)
            tab.gecko.settings.setUseTrackingProtection(tab.shieldEnabled)
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

    private fun verifiedHost(url: String?): String? {
        if (GeckoMotorDenemesi.verifiedHttpsUrl(url) != url) return null
        return try { URI(url).host?.lowercase(Locale.ROOT) } catch (_: Exception) { null }
    }

    /** Per-host exceptions are local, never encoded into the visited URL. */
    private fun protectionFor(url: String?): Boolean {
        val host = verifiedHost(url) ?: return globalShieldWanted.value
        return getSharedPreferences("eymen_browser_shield_sites", MODE_PRIVATE)
            .getBoolean("host_$host", globalShieldWanted.value)
    }

    private fun setShieldForCurrentSite(enabled: Boolean) {
        val host = verifiedHost(activeTab?.url) ?: return
        getSharedPreferences("eymen_browser_shield_sites", MODE_PRIVATE).edit()
            .putBoolean("host_$host", enabled).apply()
        tabs.forEach { tab ->
            if (verifiedHost(tab.url) == host) {
                tab.shieldEnabled = enabled
                tab.gecko.settings.setUseTrackingProtection(enabled)
            }
        }
        activeTab?.gecko?.reload()
    }

    private fun showShield() {
        if (!::shieldLayer.isInitialized || videoFullScreen) return
        if (overviewShowing) hideTabs()
        if (menuShowing) hideBrowserMenu()
        shieldShowing = true
        shieldLayer.visibility = View.VISIBLE
    }

    private fun hideShield() {
        shieldShowing = false
        if (::shieldLayer.isInitialized) shieldLayer.visibility = View.GONE
    }

    private fun showSettings() {
        if (!::settingsLayer.isInitialized || videoFullScreen) return
        if (menuShowing) hideBrowserMenu()
        if (downloadsShowing) hideDownloads()
        if (shieldShowing) hideShield()
        if (overviewShowing) hideTabs()
        settingsShowing = true
        settingsLayer.visibility = View.VISIBLE
    }

    private fun hideSettings() {
        settingsShowing = false
        if (::settingsLayer.isInitialized) settingsLayer.visibility = View.GONE
    }

    private fun handleSettingsAction(action: SettingsAction) {
        when (action) {
            SettingsAction.SHIELD -> {
                val currentlyOn = globalShieldWanted.value
                AlertDialog.Builder(this)
                    .setTitle(R.string.settings_shield)
                    .setMessage(R.string.settings_shield_global_msg)
                    .setPositiveButton(
                        if (currentlyOn) R.string.settings_shield_global_off
                        else R.string.settings_shield_global_on
                    ) { _, _ -> setGlobalShield(!currentlyOn) }
                    .setNegativeButton(android.R.string.cancel, null)
                    .show()
            }
            SettingsAction.SEARCH_ENGINE -> showSearchEngineSelection()
            SettingsAction.APPEARANCE -> showAppearanceOptions()
            SettingsAction.DOWNLOADS -> {
                hideSettings()
                showDownloads()
            }
            SettingsAction.ABOUT -> {
                val version = try {
                    packageManager.getPackageInfo(packageName, 0).versionName ?: "—"
                } catch (_: Exception) { "—" }
                AlertDialog.Builder(this)
                    .setTitle(R.string.settings_about_update)
                    .setMessage(getString(R.string.settings_about_text, version))
                    .setPositiveButton(android.R.string.ok, null)
                    .show()
            }
            else -> Unit
        }
    }

    private fun setGlobalShield(enabled: Boolean) {
        val previous = activeTab?.shieldEnabled
        globalShieldWanted.value = enabled
        prefs.edit().putBoolean("shield_wanted", enabled).apply()
        val exceptions = getSharedPreferences(
            "eymen_browser_shield_sites", MODE_PRIVATE
        )
        tabs.forEach { tab ->
            val host = verifiedHost(tab.url)
            if (host == null || !exceptions.contains("host_$host")) {
                tab.shieldEnabled = enabled
                tab.gecko.settings.setUseTrackingProtection(enabled)
            }
        }
        if (!atHome.value && previous != activeTab?.shieldEnabled) {
            activeTab?.gecko?.reload()
        }
    }

    private fun showSearchEngineSelection() {
        val engines = arrayOf("Google", "DuckDuckGo", "Bing", "Yandex")
        AlertDialog.Builder(this)
            .setTitle(R.string.menu_change_search)
            .setSingleChoiceItems(
                engines, engines.indexOf(searchEngine).coerceAtLeast(0)
            ) { dialog, index ->
                searchEngine = engines[index]
                prefs.edit().putString("search_engine", searchEngine).apply()
                dialog.dismiss()
            }.show()
    }

    private fun showAddressPositionSelection() {
        val options = arrayOf(
            getString(R.string.onboarding_bottom),
            getString(R.string.onboarding_top)
        )
        AlertDialog.Builder(this)
            .setTitle(R.string.menu_change_address)
            .setSingleChoiceItems(
                options, if (addressBottom) 0 else 1
            ) { dialog, index ->
                addressBottom = index == 0
                prefs.edit().putBoolean("address_at_bottom", addressBottom).apply()
                updateChromeMargins()
                dialog.dismiss()
            }.show()
    }

    private fun showAppearanceOptions() {
        AlertDialog.Builder(this)
            .setTitle(R.string.settings_appearance)
            .setItems(arrayOf(
                getString(R.string.theme_day),
                getString(R.string.theme_night),
                getString(R.string.menu_change_address)
            )) { _, index ->
                when (index) {
                    0, 1 -> {
                        dayMode.value = index == 0
                        prefs.edit().putBoolean("day_theme", dayMode.value).apply()
                    }
                    2 -> showAddressPositionSelection()
                }
            }.show()
    }

    private fun showDownloads() {
        if (!::downloadsLayer.isInitialized || videoFullScreen) return
        if (menuShowing) hideBrowserMenu()
        if (shieldShowing) hideShield()
        if (settingsShowing) hideSettings()
        if (overviewShowing) hideTabs()
        savedFiles.clear()
        savedFiles.addAll(downloadRegistry.items())
        downloadsShowing = true
        downloadsLayer.visibility = View.VISIBLE
    }

    private fun hideDownloads() {
        downloadsShowing = false
        if (::downloadsLayer.isInitialized) downloadsLayer.visibility = View.GONE
    }

    private fun openDownloadedFile(file: SavedDownload) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(Uri.parse(file.uri), file.mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(intent)
        } catch (_: Exception) {
            notice(R.string.download_open_error)
        }
    }

    private fun confirmClearDownloadedFiles() {
        AlertDialog.Builder(this)
            .setTitle(R.string.download_clear_records)
            .setMessage(R.string.download_clear_confirm)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                downloadRegistry.clearListOnly()
                savedFiles.clear()
                notice(R.string.download_clear_done)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun showBrowserMenu() {
        if (!::menuLayer.isInitialized || videoFullScreen) return
        menuShowing = true
        menuLayer.visibility = View.VISIBLE
    }

    private fun hideBrowserMenu() {
        menuShowing = false
        if (::menuLayer.isInitialized) menuLayer.visibility = View.GONE
    }

    private fun handleMenuAction(action: MenuAction) {
        hideBrowserMenu()
        when (action) {
            MenuAction.BOOKMARKS -> showBookmarks()
            MenuAction.HISTORY -> showRecentPages()
            MenuAction.DOWNLOADS -> showDownloads()
            MenuAction.PRIVATE -> createTab(privateMode = true)
            MenuAction.FIND -> showFindDialog()
            MenuAction.PDF -> saveCurrentPageAsPdf()
            MenuAction.DESKTOP -> toggleDesktopSite()
            MenuAction.SHARE -> shareCurrentPage()
            MenuAction.THEME -> {
                dayMode.value = !dayMode.value
                prefs.edit().putBoolean("day_theme", dayMode.value).apply()
            }
            MenuAction.SETTINGS -> showSettings()
            else -> Unit // T07 unavailable actions are non-clickable.
        }
    }

    private fun showBookmarks() {
        val store = getSharedPreferences("eymen_browser_bookmarks", MODE_PRIVATE)
        val links = store.getStringSet("urls", emptySet()).orEmpty()
            .filter { GeckoMotorDenemesi.verifiedHttpsUrl(it) == it }
            .sorted()
        val current = GeckoMotorDenemesi.verifiedHttpsUrl(activeTab?.url)
            ?.takeIf { it == activeTab?.url && !atHome.value }
        val dialog = AlertDialog.Builder(this)
            .setTitle(R.string.bookmarks)
            .setNegativeButton(android.R.string.cancel, null)
        if (links.isEmpty()) dialog.setMessage(R.string.menu_bookmark_empty)
        else dialog.setItems(links.toTypedArray()) { _, index -> openUrl(links[index]) }
        if (current != null) dialog.setPositiveButton(R.string.menu_bookmark_prompt) { _, _ ->
            if (links.contains(current)) notice(R.string.menu_bookmark_exists)
            else {
                store.edit().putStringSet("urls", (links + current).toSet()).apply()
                notice(R.string.menu_bookmark_saved)
            }
        }
        dialog.show()
    }

    private fun showRecentPages() {
        val pages = recent.toList()
        AlertDialog.Builder(this)
            .setTitle(R.string.menu_recent_history)
            .apply {
                if (pages.isEmpty()) setMessage(R.string.menu_recent_empty)
                else setItems(pages.toTypedArray()) { _, which ->
                    openUrl(pages[which])
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun showFindDialog() {
        val tab = activeTab ?: return
        if (tab.home) return
        val field = EditText(this).apply {
            hint = getString(R.string.menu_find_hint)
            isSingleLine = true
        }
        AlertDialog.Builder(this)
            .setTitle(R.string.menu_find)
            .setView(field)
            .setPositiveButton(R.string.menu_find_next) { _, _ ->
                val term = field.text.toString().trim()
                if (term.isNotBlank()) {
                    tab.gecko.finder.setDisplayFlags(
                        GeckoSession.FINDER_DISPLAY_HIGHLIGHT_ALL
                    )
                    tab.gecko.finder.find(term, 0)
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun saveCurrentPageAsPdf() {
        val tab = activeTab ?: return
        if (tab.home || GeckoMotorDenemesi.verifiedHttpsUrl(tab.url) == null) {
            notice(R.string.menu_pdf_error)
            return
        }
        tab.gecko.saveAsPdf().accept(
            { stream ->
                runOnUiThread {
                    if (stream == null) notice(R.string.menu_pdf_error)
                    else downloads.savePdfStream(stream)
                }
            },
            { _ ->
                runOnUiThread { notice(R.string.menu_pdf_error) }
            }
        )
    }

    private fun toggleDesktopSite() {
        val tab = activeTab ?: return
        if (tab.home) return
        tab.desktop = !tab.desktop
        val mode = if (tab.desktop) {
            GeckoSessionSettings.USER_AGENT_MODE_DESKTOP
        } else GeckoSessionSettings.USER_AGENT_MODE_MOBILE
        val viewport = if (tab.desktop) {
            GeckoSessionSettings.VIEWPORT_MODE_DESKTOP
        } else GeckoSessionSettings.VIEWPORT_MODE_MOBILE
        tab.gecko.settings.setUserAgentMode(mode)
        tab.gecko.settings.setViewportMode(viewport)
        tab.gecko.reload()
        notice(if (tab.desktop) R.string.menu_desktop_on else R.string.menu_desktop_off)
    }

    private fun shareCurrentPage() {
        val url = GeckoMotorDenemesi.verifiedHttpsUrl(activeTab?.url)
            ?.takeIf { it == activeTab?.url && !atHome.value }
        if (url == null) {
            notice(R.string.menu_share_error)
            return
        }
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, url)
        }
        try {
            startActivity(Intent.createChooser(send, getString(R.string.menu_share)))
        } catch (_: Exception) {
            notice(R.string.menu_share_error)
        }
    }

    private fun showStarterSettings() {
        val choices = arrayOf(
            getString(R.string.menu_settings_search),
            getString(R.string.menu_settings_address)
        )
        AlertDialog.Builder(this)
            .setTitle(R.string.menu_settings_limited)
            .setItems(choices) { _, selected ->
                if (selected == 0) {
                    val engines = arrayOf("Google", "DuckDuckGo", "Bing", "Yandex")
                    AlertDialog.Builder(this)
                        .setTitle(R.string.menu_change_search)
                        .setSingleChoiceItems(
                            engines, engines.indexOf(searchEngine).coerceAtLeast(0)
                        ) { dialog, index ->
                            searchEngine = engines[index]
                            prefs.edit().putString("search_engine", searchEngine).apply()
                            dialog.dismiss()
                        }.show()
                } else {
                    val positions = arrayOf(
                        getString(R.string.onboarding_bottom),
                        getString(R.string.onboarding_top)
                    )
                    AlertDialog.Builder(this)
                        .setTitle(R.string.menu_change_address)
                        .setSingleChoiceItems(
                            positions, if (addressBottom) 0 else 1
                        ) { dialog, index ->
                            addressBottom = index == 0
                            prefs.edit()
                                .putBoolean("address_at_bottom", addressBottom).apply()
                            updateChromeMargins()
                            dialog.dismiss()
                        }.show()
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
        if (enabled) {
            hideBrowserMenu()
            hideShield()
            hideDownloads()
            hideSettings()
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
