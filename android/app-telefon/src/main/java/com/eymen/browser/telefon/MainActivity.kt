package com.eymen.browser.telefon

import android.app.AlertDialog
import android.app.DownloadManager
import android.content.ActivityNotFoundException
import android.speech.RecognizerIntent
import android.content.Intent
import android.net.Uri
import android.net.VpnService
import android.content.SharedPreferences
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Bundle
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.WindowInsets
import android.widget.FrameLayout
import android.widget.Toast
import android.widget.EditText
import java.util.concurrent.atomic.AtomicBoolean
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.WindowCompat
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.ComposeView
import com.eymen.browser.motor.GeckoMotorDenemesi
import org.mozilla.geckoview.ContentBlocking
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.WebExtension
import org.json.JSONObject
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
    var adTrackerBlockedTab by mutableStateOf(0)
    var analyticsBlockedTab by mutableStateOf(0)
    var otherTrackerBlockedTab by mutableStateOf(0)
    // Normal-only grouping; private tabs are always excluded.
    var groupId by mutableStateOf<Long?>(null)
    // Restored background tabs open their saved HTTPS URL only when selected.
    var pendingRestoreUrl: String? = null
    // Captured Gecko page pixels live only in RAM, never stored or synced.
    var thumbnail by mutableStateOf<Bitmap?>(null)
}

private data class ClosedNormalTab(val safeUrl: String?, val groupId: Long?)

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
    private val tabGroups = mutableStateListOf<TabGroupPreview>()
    private var nextTabGroupId = 1L
    private val workspaceStore by lazy { TabWorkspaceStore(applicationContext) }
    private var restoringWorkspace = false
    // Bounded history of only normal closed tabs, never private URLs.
    private val recentlyClosedNormal = mutableStateListOf<ClosedNormalTab>()
    private val lastClosedNormalTab: ClosedNormalTab?
        get() = recentlyClosedNormal.lastOrNull()
    private val activeId = mutableStateOf(-1L)
    private val atHome = mutableStateOf(true)
    private val currentUrl = mutableStateOf("")
    private val dayMode = mutableStateOf(false)
    private val recent = mutableStateListOf<String>()
    private val savedFiles = mutableStateListOf<SavedDownload>()
    private var activeDownload by mutableStateOf<ActiveDownload?>(null)
    private val queuedDownloads = mutableStateListOf<QueuedDownload>()
    private val downloadRegistry by lazy { DownloadRegistry(this) }
    private val downloads = AndroidDownloadDenemesi(
        this,
        onProgress = { progress -> activeDownload = progress },
        onCompleted = { file ->
            savedFiles.removeAll { it.uri == file.uri }
            savedFiles.add(0, file)
            while (savedFiles.size > 100) savedFiles.removeAt(savedFiles.lastIndex)
        },
        onQueue = { queue -> queuedDownloads.clear(); queuedDownloads.addAll(queue) }
    )
    // Safe to create only when onCreate/onResume has attached the Activity Context.
    private val updater by lazy { BrowserUpdateManager(this) }
    private val vpn by lazy { EymenWireGuardVpn.get(applicationContext) }
    private val vpnImportLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) vpn.importConfig(uri)
    }
    private val vpnConsentLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            vpn.connectWithUserPermission()
        } else {
            Toast.makeText(this, "Android VPN izni verilmedi; bağlantı açılmadı.", Toast.LENGTH_LONG).show()
        }
    }
    private val voiceSearchLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val phrase = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()?.trim()
            if (!phrase.isNullOrEmpty()) openAddress(phrase)
        }
    }
    private var nextId = 1L
    private var addressBottom by mutableStateOf(true)
    private var searchEngine by mutableStateOf("Google")
    private var overviewShowing = false
    // Compose observes this so only the home logo/search reflow when popup opens.
    private var menuShowing by mutableStateOf(false)
    private var shieldShowing = false
    private var downloadsShowing by mutableStateOf(false)
    private var settingsShowing by mutableStateOf(false)
    private var settingsInitialPage by mutableStateOf(BrowserSettingsPage.MAIN)
    private var vpnScreenVisible by mutableStateOf(false)
    private val globalShieldWanted = mutableStateOf(true)
    private val strictShieldWanted = mutableStateOf(false)
    private val cosmeticFilterWanted = mutableStateOf(false)
    private val cosmeticFilterReady = mutableStateOf(false)
    private var cosmeticExtension: WebExtension? = null
    private val devicesRequested = mutableStateOf(false)
    // Never persist site names visited in private browsing mode.
    private val privateShieldOverrides = mutableMapOf<String, Boolean>()

    private val activeTab: BrowserTab?
        get() = tabs.firstOrNull { it.id == activeId.value }

    private var bottomSystemInsetPx = 0
    // The whole app viewport starts BELOW the device's camera/status bar,
    // not just one Compose header. Values come from Android WindowInsets.
    private var safeTopInsetPx = 0
    private var safeLeftInsetPx = 0
    private var safeRightInsetPx = 0
    private var videoFullScreen = false
    private var originalOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    private var originalSystemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Make the inset contract deterministic on Android 8–16.
        // Android 15/16 already enforce edge-to-edge for targetSdk 35;
        // Android 8–14 must use the SAME geometry so status-bar padding
        // is never applied on top of a decor-fitted window.
        WindowCompat.setDecorFitsSystemWindows(window, false)
        prefs = getSharedPreferences("eymen_browser_setup", MODE_PRIVATE)
        addressBottom = prefs.getBoolean("address_at_bottom", true)
        searchEngine = prefs.getString("search_engine", "Google") ?: "Google"
        // v1.0.4 changed the default to light but old installs retain the legacy
        // saved false value. Migrate that legacy default ONCE so upgrades actually
        // show the newly approved light UI; later user theme choices persist.
        if (!prefs.getBoolean("light_design_default_migrated_v105", false)) {
            prefs.edit()
                .putBoolean("day_theme", true)
                .putBoolean("light_design_default_migrated_v105", true)
                .apply()
        }
        dayMode.value = prefs.getBoolean("day_theme", true)
        globalShieldWanted.value = prefs.getBoolean("shield_wanted", true)
        strictShieldWanted.value = prefs.getBoolean("shield_strict_mode", false)
        cosmeticFilterWanted.value = prefs.getBoolean("cosmetic_filter_wanted", false)
        GeckoMotorDenemesi.configureTrackerProtection(this, strictShieldWanted.value)
        initializeCosmeticExtension()
        val incomingUrl = GeckoMotorDenemesi.urlFromIntent(intent)

        root = FrameLayout(this)
        updateSystemBarsAppearance()
        // Android 15+ forces edge-to-edge for targetSdk 35. The decor can
        // extend beneath the camera hole, clock and carrier indicators.
        // Reserve the DEVICE-REPORTED status/cutout insets for the ENTIRE
        // browser (GeckoView + home + overlays + toolbar), not a fixed
        // 24dp offset on only the tabs screen.
        //
        // Navigation + keyboard safe area is reserved HERE too. All screens
        // share the same viewport; Compose child layers must not pad it twice.
        // Fullscreen video is the intentional exception.
        root.setOnApplyWindowInsetsListener { _, insets ->
            @Suppress("DEPRECATION")
            val top: Int
            val left: Int
            val right: Int
            val bottom: Int
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val safe = insets.getInsets(
                    WindowInsets.Type.statusBars() or WindowInsets.Type.displayCutout()
                )
                top = safe.top
                left = safe.left
                right = safe.right
                bottom = maxOf(
                    insets.getInsets(WindowInsets.Type.navigationBars()).bottom,
                    insets.getInsets(WindowInsets.Type.ime()).bottom
                )
            } else {
                @Suppress("DEPRECATION")
                val cutout = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    insets.displayCutout
                } else null
                @Suppress("DEPRECATION")
                top = maxOf(insets.systemWindowInsetTop, cutout?.safeInsetTop ?: 0)
                @Suppress("DEPRECATION")
                left = maxOf(insets.systemWindowInsetLeft, cutout?.safeInsetLeft ?: 0)
                @Suppress("DEPRECATION")
                right = maxOf(insets.systemWindowInsetRight, cutout?.safeInsetRight ?: 0)
                @Suppress("DEPRECATION")
                bottom = insets.systemWindowInsetBottom
            }
            safeTopInsetPx = top
            safeLeftInsetPx = left
            safeRightInsetPx = right
            bottomSystemInsetPx = bottom
            applyBrowserSafeArea()
            insets
        }
        view = GeckoView(this)
        root.addView(view, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ))
        setContentView(root)

        // Restore only normal tab/group URLs. Private sessions never enter
        // TabWorkspaceStore; incoming external links always open a fresh normal tab.
        restoreNormalWorkspace(incomingUrl)

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
            } else if (dayMode.value) {
                LightBrowserHome(
                    menuOpen = menuShowing,
                    recentUrls = recent,
                    onSearch = ::openAddress,
                    onOpen = ::openUrl,
                    onVoiceSearch = ::startVoiceSearch
                )
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
            if (dayMode.value) {
                LightBrowserToolbar(
                    currentUrl = currentUrl.value,
                    atHome = atHome.value,
                    tabCount = tabs.count { it.privateMode == (activeTab?.privateMode == true) },
                    onGo = ::openAddress,
                    onHome = { showHome(true) },
                    onVoiceSearch = ::startVoiceSearch,
                    onNewTab = { createTab(privateMode = false) },
                    onTabs = ::showTabs,
                    onMenu = ::showBrowserMenu
                )
            } else {
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
                        selected = tab.id == activeId.value,
                        groupId = if (tab.privateMode) null else tab.groupId,
                        thumbnail = if (tab.privateMode) null
                            else tab.thumbnail?.asImageBitmap()
                    )
                },
                activePrivate = activeTab?.privateMode == true,
                openDevices = devicesRequested.value,
                groups = tabGroups.toList(),
                undoAvailable = lastClosedNormalTab != null,
                undoCount = recentlyClosedNormal.size,
                day = dayMode.value,
                onOpen = ::activateTab,
                onClose = ::closeTab,
                onCloseAll = ::closeAllOfType,
                onNew = { privateMode, groupId ->
                    val previousId = activeId.value
                    createTab(privateMode)
                    // If MAX_TABS prevented creation, do not accidentally
                    // reassign the previously active tab to another group.
                    if (!privateMode && activeId.value != previousId) {
                        activeTab?.groupId = groupId?.takeIf { id ->
                            tabGroups.any { it.id == id }
                        }
                        saveNormalWorkspace()
                    }
                },
                onNewGroup = ::promptNewTabGroup,
                onRenameGroup = ::promptRenameTabGroup,
                onRemoveGroup = ::confirmRemoveTabGroup,
                onAssignGroup = ::promptTabGroupSelection,
                onUndoClose = ::restoreLastClosedNormalTab,
                onMoveTab = ::moveTabWithinSection,
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
            if (dayMode.value) {
                LightBrowserMenu(
                    hasPage = !atHome.value &&
                        GeckoMotorDenemesi.verifiedHttpsUrl(activeTab?.url) != null,
                    onDismiss = ::hideBrowserMenu,
                    onBack = { activeTab?.gecko?.goBack() },
                    onForward = { activeTab?.gecko?.goForward() },
                    onReload = { activeTab?.gecko?.reload() },
                    onAction = ::handleMenuAction
                )
            } else {
                BrowserMenu(
                    hasPage = !atHome.value &&
                        GeckoMotorDenemesi.verifiedHttpsUrl(activeTab?.url) != null,
                    isDesktop = activeTab?.desktop == true,
                    dayMode = dayMode.value,
                    onDismiss = ::hideBrowserMenu,
                    onAction = ::handleMenuAction
                )
            }
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
                adTrackerBlocked = tab?.adTrackerBlockedTab ?: 0,
                analyticsBlocked = tab?.analyticsBlockedTab ?: 0,
                otherTrackerBlocked = tab?.otherTrackerBlockedTab ?: 0,
                strictMode = strictShieldWanted.value,
                globalEnabled = globalShieldWanted.value,
                cosmeticEnabled = cosmeticFilterWanted.value,
                cosmeticAvailable = cosmeticFilterReady.value,
                canToggle = tab != null && !tab.home && verifiedHost(tab.url) != null,
                onDismiss = ::hideShield,
                onToggle = ::setShieldForCurrentSite,
                onModeChange = ::setShieldMode,
                onGlobalToggle = ::setGlobalShield,
                onCosmeticToggle = ::setCosmeticFilterEnabled
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
                active = activeDownload,
                queued = queuedDownloads,
                dayMode = dayMode.value,
                visible = downloadsShowing,
                onBack = ::hideDownloads,
                onOpen = ::openDownloadedFile,
                onCancelActive = downloads::cancelActive,
                onCancelQueued = downloads::cancelQueued,
                onClear = ::clearDownloadedFileRecords
            )
        }
        downloadsLayer.visibility = View.GONE
        settingsLayer = ComposeView(this)
        root.addView(settingsLayer, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ))
        settingsLayer.setContent {
            if (vpnScreenVisible) {
                VpnSettingsScreen(
                    status = vpn.status,
                    dayMode = dayMode.value,
                    configPresent = vpn.configPresent,
                    importing = vpn.importing,
                    peerStats = vpn.peerStats,
                    message = vpn.message,
                    onBack = {
                        vpn.stopUiMonitoring()
                        vpnScreenVisible = false
                    },
                    onImport = {
                        vpnImportLauncher.launch(
                            arrayOf("text/plain", "application/octet-stream", "*/*")
                        )
                    },
                    onConnect = ::requestVpnConsent,
                    onDisconnect = vpn::disconnect,
                    onRefresh = vpn::refreshState,
                    onDelete = ::confirmDeleteVpnConfiguration
                )
            } else {
                SettingsScreen(
                    globalShield = globalShieldWanted.value,
                    searchEngine = searchEngine,
                    dayMode = dayMode.value,
                    addressBottom = addressBottom,
                    visible = settingsShowing,
                    initialPage = settingsInitialPage,
                    onSearchEngine = ::updateSearchEnginePreference,
                    onDayMode = ::updateDayThemePreference,
                    onAddressBottom = ::updateAddressPositionPreference,
                    onBack = ::hideSettings,
                    onAction = ::handleSettingsAction
                )
            }
        }
        settingsLayer.visibility = View.GONE
        updateChromeMargins()
        updateHomeVisibility()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                when {
                    settingsShowing && vpnScreenVisible -> {
                        vpn.stopUiMonitoring()
                        vpnScreenVisible = false
                    }
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
                        tab.shieldEnabled = protectionFor(tab.url, tab.privateMode)
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
        if (intent?.action == EymenDownloadService.OPEN_DOWNLOADS) {
            root.post { showDownloads() }
        }
    }

    /** Existing attached sessions are released, never closed on tab switches. */
    private fun createTab(
        privateMode: Boolean,
        initialUrl: String? = null,
        deferRestoreLoad: Boolean = false
    ) {
        if (::root.isInitialized) hideBrowserKeyboard()
        if (!privateMode && tabs.count { !it.privateMode } >= TabWorkspaceStore.MAX_TABS) {
            Toast.makeText(this, "En fazla 32 normal sekme açılabilir.", Toast.LENGTH_SHORT).show()
            return
        }
        if (tabs.isNotEmpty()) {
            view.releaseSession()
        }
        val id = nextId++
        val session = GeckoMotorDenemesi.attach(
            this, view, if (deferRestoreLoad) null else initialUrl,
            onFullScreen = { enabled ->
                tabs.firstOrNull { it.id == id }?.let { tab ->
                    tab.fullScreen = enabled
                    if (activeId.value == id) setVideoFullScreen(enabled)
                }
            },
            askDrmPermission = ::showDrmAccessPrompt,
            onDownload = downloads::handleResponse,
            privateMode = privateMode,
            initialLoad = initialUrl != null && !deferRestoreLoad,
            trackingProtection = protectionFor(initialUrl, privateMode),
            onBlocked = { category ->
                runOnUiThread {
                    tabs.firstOrNull { it.id == id }?.let { tab ->
                        tab.blockedPage += 1
                        tab.blockedTab += 1
                        when {
                            category and ContentBlocking.AntiTracking.AD != 0 ->
                                tab.adTrackerBlockedTab += 1
                            category and ContentBlocking.AntiTracking.ANALYTIC != 0 ->
                                tab.analyticsBlockedTab += 1
                            else -> tab.otherTrackerBlockedTab += 1
                        }
                    }
                }
            },
            onTitle = { title ->
                tabs.firstOrNull { it.id == id }?.title = title.orEmpty()
            },
            onExternalAppLink = { link, trigger, gesture ->
                showSecureExternalAppPrompt(id, link, trigger, gesture)
            },
            onUserOpenedWindow = { popupUri ->
                // Only an active HTTPS parent can request a new auth tab.
                // Never mix private popups into the normal workspace.
                val parent = tabs.firstOrNull { it.id == id }
                if (parent == null || activeId.value != id ||
                    parent.home ||
                    GeckoMotorDenemesi.verifiedHttpsUrl(parent.url) != parent.url ||
                    (popupUri != "about:blank" &&
                        GeckoMotorDenemesi.verifiedHttpsUrl(popupUri) != popupUri)
                ) {
                    null
                } else {
                    val oldId = activeId.value
                    createTab(privateMode = privateMode)
                    // createTab enforces the normal-tab capacity bound.
                    // No child session is returned if that limit was reached.
                    tabs.firstOrNull {
                        it.id == activeId.value && it.id != oldId
                    }?.gecko
                }
            }
        )
        val tab = BrowserTab(id, session, privateMode, initialUrl)
        installStandardWebPrompts(tab)
        tab.pendingRestoreUrl = if (deferRestoreLoad) initialUrl else null
        tab.shieldEnabled = protectionFor(initialUrl, privateMode)
        session.progressDelegate = object : GeckoSession.ProgressDelegate {
            override fun onPageStart(session: GeckoSession, url: String) {
                runOnUiThread {
                    // Gecko can signal its internal about:blank document on a new
                    // empty session. This is not a user navigation: keep T03 home.
                    if ((tab.home || tab.pendingRestoreUrl != null) &&
                        url.equals("about:blank", ignoreCase = true)
                    ) {
                        return@runOnUiThread
                    }
                    tab.url = url
                    tab.home = false
                    // Never show an obsolete screenshot after a new navigation.
                    tab.thumbnail = null
                    tab.blockedPage = 0
                    tab.shieldEnabled = protectionFor(url, privateMode)
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
                    if (!privateMode) saveNormalWorkspace()
                }
            }
        }
        tabs.add(tab)
        cosmeticExtension?.let { installCosmeticPolicy(tab, it) }
        activeId.value = id
        currentUrl.value = initialUrl.orEmpty()
        atHome.value = initialUrl == null
        if (::tabsLayer.isInitialized) hideTabs()
        updateHomeVisibility()
        if (!privateMode) saveNormalWorkspace()
    }

    private fun activateTab(id: Long) {
        if (::root.isInitialized) hideBrowserKeyboard()
        val tab = tabs.firstOrNull { it.id == id } ?: return
        if (activeId.value != id) {
            captureActiveTabThumbnail()
            if (videoFullScreen) setVideoFullScreen(false)
            view.releaseSession()
            view.setSession(tab.gecko)
            activeId.value = id
        }
        currentUrl.value = tab.url
        atHome.value = tab.home
        val delayed = tab.pendingRestoreUrl
        if (delayed != null) {
            tab.pendingRestoreUrl = null
            tab.gecko.loadUri(delayed)
        }
        setVideoFullScreen(tab.fullScreen)
        updateHomeVisibility()
        hideTabs()
        if (!tab.privateMode) saveNormalWorkspace()
    }

    private fun closeTab(id: Long) {
        val tab = tabs.firstOrNull { it.id == id } ?: return
        val wasOverviewOpen = overviewShowing
        val active = activeId.value == id
        if (active) {
            if (videoFullScreen) setVideoFullScreen(false)
            view.releaseSession()
        }
        // Only public, verified HTTPS URLs may enter the normal undo slot.
        // Restoring always creates a NEW session; closed GeckoSessions never
        // get reattached, which protects resources and session isolation.
        if (!tab.privateMode) {
            recentlyClosedNormal.add(
                ClosedNormalTab(
                    safeUrl = if (tab.home) null
                        else workspaceStore.safeHttpsUrl(tab.url),
                    groupId = tab.groupId
                )
            )
            while (recentlyClosedNormal.size > 10) recentlyClosedNormal.removeAt(0)
        }
        tab.gecko.close()
        tab.thumbnail = null
        tabs.remove(tab)
        if (tab.privateMode && tabs.none { it.privateMode }) privateShieldOverrides.clear()
        if (active) {
            if (tabs.isEmpty()) {
                // Always leave a usable normal tab rather than a blank GeckoView.
                createTab(privateMode = false)
            } else {
                activeId.value = -1L
                activateTab(tabs.last().id)
            }
        }
        // activateTab/createTab normally dismiss the overview. When closing
        // from a tab card, keep it visible so the undo row can be used at once.
        if (wasOverviewOpen && ::tabsLayer.isInitialized) {
            openTabOverview(false)
        }
        if (!tab.privateMode) saveNormalWorkspace()
    }

    private fun closeAllOfType(privateMode: Boolean) {
        tabs.filter { it.privateMode == privateMode }.map { it.id }.forEach(::closeTab)
        if (tabs.isEmpty()) createTab(privateMode = false)
    }

    private fun restoreLastClosedNormalTab() {
        val closed = lastClosedNormalTab ?: return
        recentlyClosedNormal.removeAt(recentlyClosedNormal.lastIndex)
        val previousId = activeId.value
        createTab(privateMode = false, initialUrl = closed.safeUrl)
        if (activeId.value == previousId) {
            // Reopen was blocked by the 32-tab cap: preserve undo history.
            recentlyClosedNormal.add(closed)
            return
        }
        // Group may have been deleted while the tab was closed.
        activeTab?.groupId = closed.groupId?.takeIf { id ->
            tabGroups.any { it.id == id }
        }
        saveNormalWorkspace()
    }

    private fun moveTabWithinSection(tabId: Long, direction: Int, selectedGroupId: Long?) {
        if (direction !in -1..1 || direction == 0) return
        val tab = tabs.firstOrNull { it.id == tabId } ?: return
        val candidates = tabs.filter {
            it.privateMode == tab.privateMode &&
                (tab.privateMode || selectedGroupId == null || it.groupId == selectedGroupId)
        }
        val index = candidates.indexOfFirst { it.id == tabId }
        val next = index + direction
        if (index < 0 || next !in candidates.indices) return
        val target = candidates[next]
        val sourcePosition = tabs.indexOf(tab)
        val destinationPosition = tabs.indexOf(target)
        tabs.removeAt(sourcePosition)
        tabs.add(destinationPosition, tab)
        if (!tab.privateMode) saveNormalWorkspace()
    }

    private fun promptNewTabGroup() {
        if (tabGroups.size >= 20) {
            notice(R.string.tab_group_limit)
            return
        }
        val input = EditText(this).apply {
            setSingleLine(true)
            setText(getString(R.string.tab_group_name_default) + " " + nextTabGroupId)
            selectAll()
        }
        AlertDialog.Builder(this)
            .setTitle(R.string.tab_group_new)
            .setView(input)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val title = input.text.toString().trim().take(32)
                if (title.isNotEmpty()) {
                    tabGroups.add(TabGroupPreview(nextTabGroupId++, title))
                    saveNormalWorkspace()
                }
            }
            .show()
    }

    private fun promptRenameTabGroup(groupId: Long) {
        val group = tabGroups.firstOrNull { it.id == groupId } ?: return
        val input = EditText(this).apply {
            setSingleLine(true)
            setText(group.title)
            selectAll()
        }
        AlertDialog.Builder(this)
            .setTitle(R.string.tab_group_rename)
            .setView(input)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val index = tabGroups.indexOfFirst { it.id == groupId }
                val title = input.text.toString().trim().take(32)
                if (index >= 0 && title.isNotEmpty()) {
                    tabGroups[index] = group.copy(title = title)
                    saveNormalWorkspace()
                }
            }
            .show()
    }

    private fun confirmRemoveTabGroup(groupId: Long) {
        if (tabGroups.none { it.id == groupId }) return
        AlertDialog.Builder(this)
            .setTitle(R.string.tab_group_remove)
            .setMessage(R.string.tab_group_remove_note)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                tabGroups.removeAll { it.id == groupId }
                tabs.forEach { if (!it.privateMode && it.groupId == groupId) it.groupId = null }
                saveNormalWorkspace()
            }
            .show()
    }

    private fun promptTabGroupSelection(tabId: Long) {
        val tab = tabs.firstOrNull { it.id == tabId && !it.privateMode } ?: return
        val options = listOf(getString(R.string.tab_group_ungrouped)) +
            tabGroups.map { it.title } +
            getString(R.string.tab_group_new)
        AlertDialog.Builder(this)
            .setTitle(R.string.tab_group_assign)
            .setItems(options.toTypedArray()) { _, which ->
                when {
                    which == 0 -> tab.groupId = null
                    which in 1..tabGroups.size -> tab.groupId = tabGroups[which - 1].id
                    which == options.lastIndex -> promptNewTabGroupForTab(tabId)
                }
                saveNormalWorkspace()
            }
            .show()
    }

    private fun promptNewTabGroupForTab(tabId: Long) {
        if (tabGroups.size >= 20) {
            notice(R.string.tab_group_limit)
            return
        }
        val input = EditText(this).apply {
            setSingleLine(true)
            setText(getString(R.string.tab_group_name_default) + " " + nextTabGroupId)
            selectAll()
        }
        AlertDialog.Builder(this)
            .setTitle(R.string.tab_group_new)
            .setView(input)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val title = input.text.toString().trim().take(32)
                if (title.isNotEmpty()) {
                    val id = nextTabGroupId++
                    tabGroups.add(TabGroupPreview(id, title))
                    tabs.firstOrNull { it.id == tabId && !it.privateMode }?.groupId = id
                    saveNormalWorkspace()
                }
            }
            .show()
    }

    private fun showTabs() = openTabOverview(false)

    private fun showDevices() = openTabOverview(true)

    /**
     * GeckoView returns real rendered web pixels. Keep only a small, capped
     * in-memory preview per normal tab; private mode and home are excluded.
     * No screenshots are written to disk or included in Android backups.
     */
    private fun captureActiveTabThumbnail() {
        val tab = activeTab ?: return
        if (tab.privateMode || tab.home || tab.pendingRestoreUrl != null ||
            videoFullScreen || !::view.isInitialized
        ) return
        val capturedUrl = tab.url
        try {
            view.capturePixels().accept(
                { pixels ->
                    if (pixels == null) return@accept
                    runOnUiThread {
                        if (tabs.none { it.id == tab.id } || tab.privateMode ||
                            tab.url != capturedUrl
                        ) return@runOnUiThread
                        try {
                            if (pixels.width < 1 || pixels.height < 1) return@runOnUiThread
                            val cropHeight = minOf(
                                pixels.height, (pixels.width * 1.32f).roundToInt()
                            )
                            val crop = Bitmap.createBitmap(
                                pixels, 0, 0, pixels.width, cropHeight
                            )
                            val targetHeight = (220f * cropHeight / pixels.width)
                                .roundToInt().coerceAtLeast(1)
                            tab.thumbnail = Bitmap.createScaledBitmap(
                                crop, 220, targetHeight, true
                            )
                        } catch (_: RuntimeException) {
                            // Compositor shutdown or allocation failure: placeholder.
                        }
                    }
                },
                { _ -> Unit }
            )
        } catch (_: RuntimeException) {
            // The compositor may not yet be ready: no fake preview.
        }
    }

    private fun openTabOverview(devices: Boolean) {
        if (::root.isInitialized) hideBrowserKeyboard()
        if (!::tabsLayer.isInitialized || videoFullScreen) return
        if (!devices) captureActiveTabThumbnail()
        devicesRequested.value = devices
        overviewShowing = true
        tabsLayer.visibility = View.VISIBLE
    }

    private fun hideTabs() {
        overviewShowing = false
        if (::tabsLayer.isInitialized) tabsLayer.visibility = View.GONE
    }

    /** Uses an installed speech recognizer without requesting microphone permission ourselves. */
    private fun startVoiceSearch() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Arama yapmak için konuşun")
        }
        try {
            voiceSearchLauncher.launch(intent)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(this, "Bu telefonda sesli arama hizmeti bulunamadı.",
                Toast.LENGTH_LONG).show()
        }
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
        if (::root.isInitialized) hideBrowserKeyboard()
        val verified = GeckoMotorDenemesi.verifiedHttpsUrl(url)
        if (verified == null) {
            notice(R.string.invalid_address)
            return
        }
        activeTab?.let { tab ->
            tab.url = verified
            tab.home = false
            tab.shieldEnabled = protectionFor(verified, tab.privateMode)
            tab.gecko.settings.setUseTrackingProtection(tab.shieldEnabled)
            currentUrl.value = verified
            atHome.value = false
            updateHomeVisibility()
            tab.gecko.loadUri(verified)
            if (!tab.privateMode) saveNormalWorkspace()
        }
    }

    /**
     * GeckoView has no built-in Android UI for JavaScript alert/confirm/prompt.
     * Some authentication flows rely on these. Browser-provided dialogs
     * display the actual current origin and never intercept/log credentials.
     */
    private fun installStandardWebPrompts(tab: BrowserTab) {
        tab.gecko.promptDelegate = object : GeckoSession.PromptDelegate {
            override fun onAlertPrompt(
                session: GeckoSession,
                prompt: GeckoSession.PromptDelegate.AlertPrompt
            ): GeckoResult<GeckoSession.PromptDelegate.PromptResponse> {
                val result = GeckoResult<GeckoSession.PromptDelegate.PromptResponse>()
                if (activeId.value != tab.id || tabs.none { it.id == tab.id }) {
                    result.complete(prompt.dismiss())
                    return result
                }
                val done = AtomicBoolean(false)
                fun finish() {
                    if (done.compareAndSet(false, true)) result.complete(prompt.dismiss())
                }
                AlertDialog.Builder(this@MainActivity)
                    .setTitle(verifiedHost(tab.url) ?: getString(R.string.bank_js_prompt_title))
                    .setMessage(prompt.message.orEmpty().take(3000))
                    .setPositiveButton(android.R.string.ok) { _, _ -> finish() }
                    .setOnCancelListener { finish() }
                    .show()
                return result
            }

            override fun onButtonPrompt(
                session: GeckoSession,
                prompt: GeckoSession.PromptDelegate.ButtonPrompt
            ): GeckoResult<GeckoSession.PromptDelegate.PromptResponse> {
                val result = GeckoResult<GeckoSession.PromptDelegate.PromptResponse>()
                if (activeId.value != tab.id || tabs.none { it.id == tab.id }) {
                    result.complete(prompt.dismiss())
                    return result
                }
                val done = AtomicBoolean(false)
                fun finish(accept: Boolean) {
                    if (done.compareAndSet(false, true)) {
                        result.complete(prompt.confirm(
                            if (accept) GeckoSession.PromptDelegate.ButtonPrompt.Type.POSITIVE
                            else GeckoSession.PromptDelegate.ButtonPrompt.Type.NEGATIVE
                        ))
                    }
                }
                AlertDialog.Builder(this@MainActivity)
                    .setTitle(verifiedHost(tab.url) ?: getString(R.string.bank_js_prompt_title))
                    .setMessage(prompt.message.orEmpty().take(3000))
                    .setNegativeButton(android.R.string.cancel) { _, _ -> finish(false) }
                    .setPositiveButton(android.R.string.ok) { _, _ -> finish(true) }
                    .setOnCancelListener { finish(false) }
                    .show()
                return result
            }

            override fun onTextPrompt(
                session: GeckoSession,
                prompt: GeckoSession.PromptDelegate.TextPrompt
            ): GeckoResult<GeckoSession.PromptDelegate.PromptResponse> {
                val result = GeckoResult<GeckoSession.PromptDelegate.PromptResponse>()
                if (activeId.value != tab.id || tabs.none { it.id == tab.id }) {
                    result.complete(prompt.dismiss())
                    return result
                }
                val edit = EditText(this@MainActivity).apply {
                    setSingleLine(true)
                    setText(prompt.defaultValue.orEmpty().take(2048))
                    setSelectAllOnFocus(true)
                }
                val done = AtomicBoolean(false)
                fun finish(accept: Boolean) {
                    if (done.compareAndSet(false, true)) {
                        if (accept) result.complete(prompt.confirm(edit.text.toString()))
                        else result.complete(prompt.dismiss())
                        edit.text.clear()
                    }
                }
                AlertDialog.Builder(this@MainActivity)
                    .setTitle(verifiedHost(tab.url) ?: getString(R.string.bank_js_prompt_title))
                    .setMessage(prompt.message.orEmpty().take(3000))
                    .setView(edit)
                    .setNegativeButton(android.R.string.cancel) { _, _ -> finish(false) }
                    .setPositiveButton(android.R.string.ok) { _, _ -> finish(true) }
                    .setOnCancelListener { finish(false) }
                    .show()
                return result
            }
        }
    }

    /**
     * Only opens a native-app deep link after a real HTTPS page user gesture
     * and a SECOND explicit Android dialog. Never relays page-supplied Intent
     * extras/components, JavaScript, files, implicit permissions or credentials.
     */
    private fun showSecureExternalAppPrompt(
        tabId: Long, link: String, trigger: String?, hasGesture: Boolean
    ): Boolean {
        val sourceTab = tabs.firstOrNull { it.id == tabId } ?: return false
        if (activeId.value != tabId) return false
        val origin = trigger ?: sourceTab.url
        val safe = ExternalAppLinkPolicy.fromWebsite(link, origin, hasGesture)
            ?: return false
        // No link or token-bearing query is printed to logs or a dialog.
        val description = getString(R.string.bank_external_prompt) + "\n\n" +
            getString(R.string.bank_external_origin) + ": " + safe.originHost + "\n" +
            getString(R.string.bank_external_target) + ": " +
            (safe.packageName ?: safe.destinationScheme) + "\n\n" +
            getString(R.string.bank_external_security_warning)
        AlertDialog.Builder(this)
            .setTitle(R.string.bank_external_title)
            .setMessage(description)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.bank_external_confirm) { _, _ ->
                try {
                    startActivity(safe.intent)
                } catch (_: Exception) {
                    notice(R.string.bank_external_missing)
                }
            }
            .show()
        return true
    }

    /** Explicit user command, never a background website redirect. */
    private fun openCurrentPageInOfficialApp() {
        val page = activeTab?.url
        val verified = GeckoMotorDenemesi.verifiedHttpsUrl(page)
            ?.takeIf { it == page && !atHome.value }
        if (verified == null) {
            notice(R.string.invalid_address)
            return
        }
        AlertDialog.Builder(this)
            .setTitle(R.string.menu_open_official_app)
            .setMessage(R.string.bank_https_handoff_prompt)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.bank_external_confirm) { _, _ ->
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(verified)).apply {
                    addCategory(Intent.CATEGORY_BROWSABLE)
                    if (Build.VERSION.SDK_INT >= 30) {
                        addFlags(Intent.FLAG_ACTIVITY_REQUIRE_NON_BROWSER)
                    }
                }
                try {
                    if (Build.VERSION.SDK_INT >= 30) {
                        startActivity(intent)
                    } else {
                        startActivity(Intent.createChooser(intent,
                            getString(R.string.menu_open_official_app)))
                    }
                } catch (_: Exception) {
                    notice(R.string.bank_external_missing)
                }
            }
            .show()
    }

    /**
     * Interoperability escape hatch for banks that support ordinary web login
     * in another full browser but reject this embedded GeckoView flow.
     * The Android system chooser is user-controlled: no credentials, cookies,
     * headers or session tokens are copied into the external browser.
     */
    private fun openCurrentPageInAnotherBrowser() {
        val page = activeTab?.url
        val verified = GeckoMotorDenemesi.verifiedHttpsUrl(page)
            ?.takeIf { it == page && !atHome.value }
        if (verified == null) {
            notice(R.string.invalid_address)
            return
        }
        AlertDialog.Builder(this)
            .setTitle(R.string.menu_open_other_browser)
            .setMessage(R.string.bank_browser_handoff_notice)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(verified)).apply {
                    addCategory(Intent.CATEGORY_BROWSABLE)
                }
                try {
                    startActivity(Intent.createChooser(intent,
                        getString(R.string.menu_open_other_browser)))
                } catch (_: Exception) {
                    notice(R.string.bank_external_missing)
                }
            }
            .show()
    }

    private fun showHome(enabled: Boolean) {
        if (::root.isInitialized) hideBrowserKeyboard()
        activeTab?.home = enabled
        atHome.value = enabled
        updateHomeVisibility()
        if (activeTab?.privateMode == false) saveNormalWorkspace()
    }

    private fun updateHomeVisibility() {
        if (::homeLayer.isInitialized) {
            homeLayer.visibility =
                if (atHome.value && !videoFullScreen) View.VISIBLE else View.GONE
        }
    }

    private fun updateSystemBarsAppearance() {
        // This background is visible only behind system bars and camera
        // cutouts; every real browser screen begins below their safe area.
        if (::root.isInitialized) {
            root.setBackgroundColor(
                if (dayMode.value) AndroidColor.rgb(245, 247, 251)
                else AndroidColor.rgb(10, 15, 28)
            )
        }
        WindowCompat.getInsetsController(window, window.decorView)
            .isAppearanceLightStatusBars = dayMode.value
    }

    private fun applyBrowserSafeArea() {
        if (!::root.isInitialized) return
        // Full-screen video intentionally covers the display; normal
        // browser content and all dialogs stay beneath the camera cutout.
        val top = if (videoFullScreen) 0 else safeTopInsetPx
        val left = if (videoFullScreen) 0 else safeLeftInsetPx
        val right = if (videoFullScreen) 0 else safeRightInsetPx
        val bottom = if (videoFullScreen) 0 else bottomSystemInsetPx
        if (root.paddingTop != top || root.paddingLeft != left ||
            root.paddingRight != right || root.paddingBottom != bottom
        ) {
            root.setPadding(left, top, right, bottom)
            // Actual root insets allow QA to detect regressions on old
            // 3-button, gesture, and camera-cutout phones.
            Log.i(
                "EymenSafeArea",
                "top=$top left=$left right=$right bottom=$bottom fullscreen=$videoFullScreen"
            )
        }
    }

    private fun updateChromeMargins() {
        if (!::chrome.isInitialized) return
        // Parent already reserves navigation/IME insets for ALL screens.
        // Adding them to the toolbar height would create a second gap.
        // Light approved UI has ONE compact top bar; dark legacy keeps its
        // original top/bottom placement. Never overlay live Gecko content.
        val lightTopBar = dayMode.value
        val bottomBar = addressBottom && !lightTopBar
        val barHeightDp = if (lightTopBar) 68f else 130f
        val barHeight = (barHeightDp * resources.displayMetrics.density).roundToInt()
        val body = view.layoutParams as FrameLayout.LayoutParams
        body.topMargin = if (videoFullScreen || bottomBar) 0 else barHeight
        body.bottomMargin = if (videoFullScreen || !bottomBar) 0 else barHeight
        view.layoutParams = body
        chrome.layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT,
            if (bottomBar) Gravity.BOTTOM else Gravity.TOP
        )
    }

    private fun verifiedHost(url: String?): String? {
        if (GeckoMotorDenemesi.verifiedHttpsUrl(url) != url) return null
        return try { URI(url).host?.lowercase(Locale.ROOT) } catch (_: Exception) { null }
    }

    /** Per-host exceptions are local, never encoded into the visited URL. */
    private fun protectionFor(url: String?, privateMode: Boolean = false): Boolean {
        val host = verifiedHost(url) ?: return globalShieldWanted.value
        if (privateMode) {
            return privateShieldOverrides[host] ?: globalShieldWanted.value
        }
        return getSharedPreferences("eymen_browser_shield_sites", MODE_PRIVATE)
            .getBoolean("host_$host", globalShieldWanted.value)
    }

    private fun setShieldForCurrentSite(enabled: Boolean) {
        val current = activeTab ?: return
        val host = verifiedHost(current.url) ?: return
        if (current.privateMode) {
            // Ephemeral: no browsing host survives last private tab closure.
            privateShieldOverrides[host] = enabled
        } else {
            getSharedPreferences("eymen_browser_shield_sites", MODE_PRIVATE).edit()
                .putBoolean("host_$host", enabled).apply()
        }
        tabs.forEach { tab ->
            if (tab.privateMode == current.privateMode && verifiedHost(tab.url) == host) {
                tab.shieldEnabled = enabled
                tab.gecko.settings.setUseTrackingProtection(enabled)
            }
        }
        current.gecko.reload()
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

    private fun showSettings() = openSettingsPage(BrowserSettingsPage.MAIN)

    private fun openSettingsPage(page: BrowserSettingsPage) {
        settingsInitialPage = page
        hideBrowserKeyboard()
        if (!::settingsLayer.isInitialized || videoFullScreen) return
        if (menuShowing) hideBrowserMenu()
        if (downloadsShowing) hideDownloads()
        if (shieldShowing) hideShield()
        if (overviewShowing) hideTabs()
        settingsShowing = true
        settingsLayer.visibility = View.VISIBLE
    }

    private fun hideSettings() {
        hideBrowserKeyboard()
        vpn.stopUiMonitoring()
        vpnScreenVisible = false
        settingsShowing = false
        if (::settingsLayer.isInitialized) settingsLayer.visibility = View.GONE
    }

    private fun handleSettingsAction(action: SettingsAction) {
        when (action) {
            SettingsAction.VPN -> {
                vpnScreenVisible = true
                vpn.startUiMonitoring()
            }
            SettingsAction.SHIELD -> {
                hideSettings()
                showShield()
            }
            SettingsAction.SEARCH_ENGINE -> showSearchEngineSelection()
            SettingsAction.APPEARANCE -> showAppearanceOptions()
            SettingsAction.DOWNLOADS -> {
                hideSettings()
                showDownloads()
            }
            SettingsAction.ABOUT -> updater.check(manual = true)
            else -> Unit
        }
    }

    private fun requestVpnConsent() {
        if (!vpn.configPresent) {
            Toast.makeText(this, "Önce kendi WireGuard .conf dosyanı içe aktar.", Toast.LENGTH_LONG).show()
            return
        }
        try {
            val request = VpnService.prepare(this)
            if (request == null) {
                vpn.connectWithUserPermission()
            } else {
                // Explicit system-managed user approval, never a fake dialog.
                vpnConsentLauncher.launch(request)
            }
        } catch (_: Exception) {
            Toast.makeText(this, "Android VPN izin penceresi açılamadı.", Toast.LENGTH_LONG).show()
        }
    }

    private fun confirmDeleteVpnConfiguration() {
        AlertDialog.Builder(this)
            .setTitle(R.string.vpn_remove)
            .setMessage(R.string.vpn_saved_crypt)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                vpn.deleteConfiguration()
            }
            .show()
    }

    private fun initializeCosmeticExtension() {
        GeckoMotorDenemesi.ensureCosmeticExtension(this) { installed ->
            runOnUiThread {
                cosmeticExtension = installed
                cosmeticFilterReady.value = installed != null
                if (installed != null) {
                    tabs.forEach { installCosmeticPolicy(it, installed) }
                }
            }
        }
    }

    /**
     * Every extension message is untrusted. Native policy is based exclusively
     * on the owning live GeckoSession and its native site protection setting.
     * Neither a page URL nor a private browsing record is sent to the add-on.
     */
    private fun installCosmeticPolicy(tab: BrowserTab, extension: WebExtension) {
        tab.gecko.webExtensionController.setMessageDelegate(
            extension,
            object : WebExtension.MessageDelegate {
                override fun onMessage(
                    nativeApp: String,
                    message: Any,
                    sender: WebExtension.MessageSender
                ): GeckoResult<Any>? {
                    val request = message as? JSONObject
                    val allowed = nativeApp == "eymen_cosmetic" &&
                        request?.optString("type") == "cosmetic-policy-v1" &&
                        sender.environmentType ==
                            WebExtension.MessageSender.ENV_TYPE_CONTENT_SCRIPT &&
                        sender.isTopLevel &&
                        sender.session === tab.gecko &&
                        sender.webExtension.id == extension.id &&
                        tabs.any { it.id == tab.id && it.gecko === tab.gecko } &&
                        verifiedHost(sender.url) != null &&
                        cosmeticFilterWanted.value &&
                        // Gecko supplies the real top-level frame URL. Using
                        // tab.url here races the first onPageStart callback.
                        // Native per-site policy uses this trusted frame URL
                        // and the owning GeckoSession's privacy mode.
                        protectionFor(sender.url, tab.privateMode)
                    return GeckoResult.fromValue(
                        JSONObject().put("enabled", allowed) as Any
                    )
                }
            },
            "eymen_cosmetic"
        )
    }

    private fun setCosmeticFilterEnabled(enabled: Boolean) {
        // No UI success state unless Gecko installed the bundled extension.
        if (!cosmeticFilterReady.value) return
        if (cosmeticFilterWanted.value == enabled) return
        cosmeticFilterWanted.value = enabled
        prefs.edit().putBoolean("cosmetic_filter_wanted", enabled).apply()
        activeTab?.takeIf { !it.home }?.gecko?.reload()
    }

    private fun setShieldMode(strict: Boolean) {
        if (strictShieldWanted.value == strict) return
        strictShieldWanted.value = strict
        prefs.edit().putBoolean("shield_strict_mode", strict).apply()
        GeckoMotorDenemesi.configureTrackerProtection(this, strict)
        // Gecko runtime list policy is global, while host exceptions remain
        // per-session; reload only the active enabled page after changing lists.
        activeTab?.takeIf { it.shieldEnabled && !it.home }?.gecko?.reload()
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
            val overridden = if (tab.privateMode) {
                host != null && privateShieldOverrides.containsKey(host)
            } else {
                host != null && exceptions.contains("host_$host")
            }
            if (!overridden) {
                tab.shieldEnabled = enabled
                tab.gecko.settings.setUseTrackingProtection(enabled)
            }
        }
        if (!atHome.value && previous != activeTab?.shieldEnabled) {
            activeTab?.gecko?.reload()
        }
    }

    private fun updateSearchEnginePreference(engine: String) {
        if (engine !in listOf("Google", "DuckDuckGo", "Bing", "Yandex")) return
        searchEngine = engine
        prefs.edit().putString("search_engine", engine).apply()
    }

    private fun updateDayThemePreference(day: Boolean) {
        dayMode.value = day
        prefs.edit().putBoolean("day_theme", day).apply()
        updateSystemBarsAppearance()
        updateChromeMargins()
    }

    private fun updateAddressPositionPreference(bottom: Boolean) {
        addressBottom = bottom
        prefs.edit().putBoolean("address_at_bottom", bottom).apply()
        updateChromeMargins()
    }

    private fun showSearchEngineSelection() = openSettingsPage(BrowserSettingsPage.SEARCH)
    private fun showAddressPositionSelection() = openSettingsPage(BrowserSettingsPage.APPEARANCE)
    private fun showAppearanceOptions() = openSettingsPage(BrowserSettingsPage.APPEARANCE)

    private fun hideBrowserKeyboard() {
        currentFocus?.clearFocus()
        (getSystemService(INPUT_METHOD_SERVICE) as? android.view.inputmethod.InputMethodManager)
            ?.hideSoftInputFromWindow(root.windowToken, 0)
    }

    private fun showDownloads() {
        hideBrowserKeyboard()
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
        hideBrowserKeyboard()
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

    private fun clearDownloadedFileRecords() {
        downloadRegistry.clearListOnly()
        savedFiles.clear()
        notice(R.string.download_clear_done)
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
            MenuAction.OPEN_EXTERNAL -> openCurrentPageInOfficialApp()
            MenuAction.OPEN_BROWSER -> openCurrentPageInAnotherBrowser()
            MenuAction.THEME -> {
                dayMode.value = !dayMode.value
                prefs.edit().putBoolean("day_theme", dayMode.value).apply()
                updateSystemBarsAppearance()
                updateChromeMargins()
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
        // Rotating a notched phone changes top vs side display cutout insets.
        if (::root.isInitialized) root.requestApplyInsets()
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
        // Entering/exiting immersive video must remove/restore the device
        // camera/status bar reservation without restarting Gecko sessions.
        applyBrowserSafeArea()
        root.requestApplyInsets()
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
        if (intent.action == EymenDownloadService.OPEN_DOWNLOADS) {
            showDownloads()
            return
        }
        GeckoMotorDenemesi.urlFromIntent(intent)?.let { url ->
            // Links from another app never silently enter an existing private tab.
            if (activeTab?.privateMode == true) createTab(false, url)
            else openUrl(url)
        }
    }

    /**
     * Private tabs are never passed to the persistence store. Saved pages
     * use fresh GeckoSessions, with inactive pages loaded only on selection.
     */
    private fun restoreNormalWorkspace(incomingUrl: String?) {
        restoringWorkspace = true
        try {
            val saved = workspaceStore.read()
            if (saved != null && saved.tabs.isNotEmpty()) {
                tabGroups.addAll(saved.groups)
                nextTabGroupId = (saved.groups.maxOfOrNull { it.id } ?: 0L) + 1L
                saved.tabs.forEach { item ->
                    val safeUrl = workspaceStore.safeHttpsUrl(item.url)
                    createTab(false, safeUrl, deferRestoreLoad = safeUrl != null)
                    activeTab?.groupId = item.groupId
                }
                val selectedPosition = saved.tabs.indexOfFirst { it.selected }
                    .takeIf { it >= 0 } ?: saved.tabs.lastIndex
                activateTab(tabs[selectedPosition].id)
                if (incomingUrl != null) createTab(false, incomingUrl)
            } else {
                if (saved != null) {
                    tabGroups.addAll(saved.groups)
                    nextTabGroupId = (saved.groups.maxOfOrNull { it.id } ?: 0L) + 1L
                }
                createTab(false, incomingUrl)
            }
        } finally {
            restoringWorkspace = false
            saveNormalWorkspace(durable = true)
        }
    }

    private fun saveNormalWorkspace(durable: Boolean = false) {
        if (restoringWorkspace) return
        val normal = tabs.filter { !it.privateMode }
        // When private browsing is active, keep a normal tab selected in the
        // persisted snapshot; never persist the private session or its URL.
        val selectedNormal = activeId.value.takeIf { active ->
            normal.any { it.id == active }
        } ?: normal.lastOrNull()?.id
        workspaceStore.write(
            StoredWorkspace(
                groups = tabGroups.toList(),
                tabs = normal.map { tab ->
                    StoredNormalTab(
                        url = if (tab.home) null
                            else workspaceStore.safeHttpsUrl(tab.url),
                        groupId = tab.groupId,
                        selected = tab.id == selectedNormal
                    )
                }
            ),
            durable = durable
        )
    }

    override fun onStop() {
        // UI status polling does not run while the Activity is hidden.
        vpn.stopUiMonitoring()
        downloads.pauseObserving()
        // Synchronous write at the lifecycle boundary for process/phone death.
        if (::view.isInitialized) saveNormalWorkspace(durable = true)
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
        downloads.observe()
        savedFiles.clear()
        savedFiles.addAll(downloadRegistry.items())
        if (::prefs.isInitialized) updater.onResumed()
        if (vpnScreenVisible) vpn.startUiMonitoring()
        else vpn.refreshState()
    }

    override fun onDestroy() {
        vpn.stopUiMonitoring()
        updater.dispose()
        downloads.dispose()
        if (::view.isInitialized) view.releaseSession()
        tabs.forEach { it.gecko.close() }
        tabs.clear()
        tabGroups.clear()
        recentlyClosedNormal.clear()
        privateShieldOverrides.clear()
        super.onDestroy()
    }
}
