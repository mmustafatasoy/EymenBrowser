package com.eymen.browser.telefon

import android.app.AlertDialog
import android.app.DownloadManager
import android.content.ActivityNotFoundException
import android.speech.RecognizerIntent
import android.content.Intent
import android.content.Context
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
import androidx.compose.ui.graphics.toArgb
import com.eymen.browser.tema.EymenTokens as T
import androidx.compose.ui.platform.ComposeView
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import com.eymen.browser.motor.GeckoMotorDenemesi
import org.mozilla.geckoview.ContentBlocking
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.WebExtension
import org.json.JSONObject
import org.mozilla.geckoview.GeckoSessionSettings
import org.mozilla.geckoview.GeckoView
import org.mozilla.geckoview.StorageController
import org.mozilla.geckoview.TranslationsController
import java.net.URI
import java.net.URLEncoder
import java.util.Locale
import kotlin.math.roundToInt

private class BrowserTab(
    val id: Long,
    val gecko: GeckoSession,
    val privateMode: Boolean,
    initialUrl: String?,
    val openerId: Long? = null
) {
    var url by mutableStateOf(initialUrl.orEmpty())
    var title by mutableStateOf("")
    var home by mutableStateOf(initialUrl == null)
    var fullScreen by mutableStateOf(false)
    var desktop by mutableStateOf(false)
    var shieldEnabled by mutableStateOf(false)
    var lastScrollY = 0
    var historyVisitAt = 0L
    var historyUrl: String? = null
    var canGoBack by mutableStateOf(false)
    var canGoForward by mutableStateOf(false)
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
    private lateinit var libraryLayer: ComposeView
    private lateinit var history: BrowserHistory
    private lateinit var toolbarMotion: BrowserChromeMotion
    private var libraryShowing by mutableStateOf(false)
    private var libraryMode by mutableStateOf(BrowserLibrary.HISTORY)
    private var bookmarkRevision by mutableStateOf(0)
    private var imeVisible = false
    private var homeButtonEnabled by mutableStateOf(true)
    private var defaultBrowser by mutableStateOf(false)
    private var notificationsEnabled by mutableStateOf(false)
    private lateinit var prefs: SharedPreferences
    private lateinit var settingsServices: BrowserSettingsServices
    private lateinit var childMode: BrowserChildMode
    private lateinit var addons: BrowserAddons
    private lateinit var qrScanner: BrowserQrScanner
    private var functionalSettingsPage by mutableStateOf<FunctionalSettingsPage?>(null)

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
    private val recent: List<String>
        get() = if (::history.isInitialized) history.entries.map { it.url }.distinct().take(2) else emptyList()
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
    private val browserRoleLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { refreshSystemPreferences() }

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
    private val webPrompts = mutableSetOf<AlertDialog>()

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

    override fun attachBaseContext(base: Context) {
        val language = base.getSharedPreferences("eymen_browser_setup", MODE_PRIVATE)
            .getString("app_language", "system")
        if (Build.VERSION.SDK_INT < 33 && language in setOf("tr", "en")) {
            val config = Configuration(base.resources.configuration)
            config.setLocale(Locale.forLanguageTag(language!!))
            super.attachBaseContext(base.createConfigurationContext(config))
        } else super.attachBaseContext(base)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Make the inset contract deterministic on Android 8–16.
        // Android 15/16 already enforce edge-to-edge for targetSdk 35;
        // Android 8–14 must use the SAME geometry so status-bar padding
        // is never applied on top of a decor-fitted window.
        WindowCompat.setDecorFitsSystemWindows(window, false)
        prefs = getSharedPreferences("eymen_browser_setup", MODE_PRIVATE)
        history = BrowserHistory.get(this)
        history.refresh()
        childMode = BrowserChildMode(this, ::resetTabsForChildMode)
        settingsServices = BrowserSettingsServices(this,
            active = { session -> activeTab?.gecko === session && !isFinishing && !isDestroyed },
            reload = {
                updateAutofill()
                activeTab?.takeIf { !it.home && it.gecko.isOpen }?.gecko?.reload()
            })
        settingsServices.apply()
        addons = BrowserAddons(this)
        qrScanner = BrowserQrScanner(this, ::openUrl, ::notice)
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
        homeButtonEnabled = prefs.getBoolean("home_button_enabled",true)
        // The old light toolbar always used the top despite its legacy saved
        // bottom value. Preserve its visible position once, then honor choices.
        if (!prefs.getBoolean("toolbar_position_migrated_v119",false)) {
            if (dayMode.value) { addressBottom=false;prefs.edit().putBoolean("address_at_bottom",false).apply() }
            prefs.edit().putBoolean("toolbar_position_migrated_v119",true).apply()
        }

        globalShieldWanted.value = prefs.getBoolean("shield_wanted", true)
        strictShieldWanted.value = prefs.getBoolean("shield_strict_mode", false)
        cosmeticFilterWanted.value = prefs.getBoolean("cosmetic_filter_wanted", false)
        GeckoMotorDenemesi.configureTrackerProtection(this, strictShieldWanted.value)
        initializeCosmeticExtension()
        // A locale recreation must restore the workspace rather than replay
        // the external URL which originally launched this Activity.
        val incomingUrl = if (savedInstanceState == null) GeckoMotorDenemesi.urlFromIntent(intent) else null

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
            imeVisible = if (Build.VERSION.SDK_INT >= 30) insets.isVisible(WindowInsets.Type.ime())
                else bottom > resources.displayMetrics.heightPixels / 5
            if (imeVisible && ::toolbarMotion.isInitialized) toolbarMotion.show()
            applyBrowserSafeArea()
            insets
        }
        view = BrowserGeckoView(this).also { engine ->
            engine.onBrowserTouch = { event -> if (::toolbarMotion.isInitialized) toolbarMotion.onTouch(event) }
        }
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
        toolbarMotion = BrowserChromeMotion(view, chrome) { canCollapseChrome() }
        chrome.setContent {
            // Compose owns a virtual accessibility tree beneath this native
            // view. Clear it as well while a modal surface owns interaction.
            val modal=menuShowing||overviewShowing||settingsShowing||downloadsShowing||shieldShowing||libraryShowing
            Box(if(modal)Modifier.clearAndSetSemantics {} else Modifier) {
            LightBrowserToolbar(
                currentUrl=currentUrl.value,atHome=atHome.value,
                tabCount=tabs.count { it.privateMode==(activeTab?.privateMode==true) },
                dayMode=dayMode.value,homeButtonEnabled=homeButtonEnabled,
                privateMode=activeTab?.privateMode==true,
                onGo=::openAddress,onHome={showHome(true)},onVoiceSearch=::startVoiceSearch,
                onNewTab={createTab(privateMode=false)},onTabs=::showTabs,onMenu=::showBrowserMenu)
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
                onShareTab = ::shareNormalTab,
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
            if (menuShowing) BrowserMenu(
                hasPage = !atHome.value && GeckoMotorDenemesi.verifiedHttpsUrl(activeTab?.url) != null,
                isDesktop = activeTab?.desktop == true,dayMode = dayMode.value,
                textPercent = settingsServices.textPercent,onTextSize = settingsServices::setTextSize,
                onDismiss = ::hideBrowserMenu,onAction = ::handleMenuAction,
                onBack = { hideBrowserKeyboard();activeTab?.gecko?.goBack() },
                onForward = { hideBrowserKeyboard();activeTab?.gecko?.goForward() },
                onReload = { hideBrowserKeyboard();activeTab?.gecko?.reload() },
                canGoBack = activeTab?.canGoBack == true,canGoForward = activeTab?.canGoForward == true,
                bookmarked = isCurrentBookmarked(),onBookmark = ::toggleCurrentBookmark)
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
                dayMode = dayMode.value,
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
                cookieReject = settingsServices.cookieReject,
                fingerprint = settingsServices.fingerprint,
                httpsOnly = settingsServices.httpsOnly,
                autoplay = settingsServices.autoplay,
                canToggle = tab != null && !tab.home && verifiedHost(tab.url) != null,
                onDismiss = ::hideShield,
                onToggle = ::setShieldForCurrentSite,
                onModeChange = ::setShieldMode,
                onGlobalToggle = ::setGlobalShield,
                onCosmeticToggle = ::setCosmeticFilterEnabled,
                onPrivacyToggle = settingsServices::set,
                onManagePrivacy = {
                    hideShield(); showSettings(); handleSettingsAction(SettingsAction.SHIELD)
                },
                onSitePermissions = {
                    hideShield(); showSettings(); handleSettingsAction(SettingsAction.SITE_PERMISSIONS)
                }
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
            } else if (functionalSettingsPage != null) {
                val back = { hideBrowserKeyboard(); functionalSettingsPage = null }
                when (functionalSettingsPage) {
                    FunctionalSettingsPage.ACCESSIBILITY -> AccessibilitySettingsScreen(settingsServices,dayMode.value,back)
                    FunctionalSettingsPage.DATA -> BrowsingDataSettingsScreen(dayMode.value,back,::confirmClearHistory,::clearBrowsingData)
                    FunctionalSettingsPage.SECURITY -> SecuritySettingsScreen(settingsServices,globalShieldWanted.value,dayMode.value,back,
                        { functionalSettingsPage=FunctionalSettingsPage.PRIVACY },{updater.check(manual=true)})
                    FunctionalSettingsPage.PRIVACY -> PrivacySettingsScreen(settingsServices, dayMode.value, back, ::clearBrowsingData)
                    FunctionalSettingsPage.PERMISSIONS -> PermissionsSettingsScreen(settingsServices, dayMode.value, back,
                        currentUrl.value, activeTab?.privateMode == true) {
                        try { startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))) }
                        catch (_: Exception) { notice(R.string.settings_system_unavailable) }
                    }
                    FunctionalSettingsPage.PASSWORDS -> PasswordSettingsScreen(settingsServices, dayMode.value, back)
                    FunctionalSettingsPage.EXTENSIONS -> AddonsSettingsScreen(addons, dayMode.value, back,
                        onBrowse = { hideSettings(); createTab(false, "https://addons.mozilla.org/android/") },
                        onOptions = ::openAddonOptions)
                    FunctionalSettingsPage.LANGUAGE -> LanguageSettingsScreen(settingsServices, dayMode.value, back, ::translateCurrentPage)
                    FunctionalSettingsPage.CHILD -> ChildSettingsScreen(childMode, dayMode.value, back)
                    FunctionalSettingsPage.TOOLS -> BrowserToolsScreen(settingsServices, dayMode.value,
                        !atHome.value && verifiedHost(currentUrl.value) != null, back) { action ->
                        hideSettings(); handleMenuAction(action)
                    }
                    null -> Unit
                }
            } else {
                SettingsScreen(
                    globalShield = globalShieldWanted.value,
                    searchEngine = searchEngine,
                    dayMode = dayMode.value,
                    addressBottom = addressBottom,
                    visible = settingsShowing,
                    initialPage = settingsInitialPage,
                    defaultBrowser = defaultBrowser,notificationsEnabled = notificationsEnabled,
                    homeButtonEnabled = homeButtonEnabled,onHomeButton = { enabled ->
                        homeButtonEnabled=enabled;prefs.edit().putBoolean("home_button_enabled",enabled).apply()
                    },
                    onSearchEngine = ::updateSearchEnginePreference,
                    onDayMode = ::updateDayThemePreference,
                    onAddressBottom = ::updateAddressPositionPreference,
                    onBack = ::hideSettings,
                    onAction = ::handleSettingsAction
                )
            }
        }
        settingsLayer.visibility = View.GONE
        libraryLayer = ComposeView(this)
        root.addView(libraryLayer, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
        libraryLayer.setContent {
            bookmarkRevision
            val items = when (libraryMode) {
                BrowserLibrary.HISTORY -> history.entries
                BrowserLibrary.BOOKMARKS -> bookmarkUrls().sorted().mapIndexed { index,url ->
                    HistoryEntry(index.toLong(),url,history.entries.firstOrNull { it.url == url }?.title
                        ?: HistoryPolicy.title("",url),0L)
                }
                BrowserLibrary.RECENT_TABS -> recentlyClosedNormal.asReversed().mapNotNull { it.safeUrl }
                    .distinct().mapIndexed { index,url -> HistoryEntry(index.toLong(),url,
                        history.entries.firstOrNull { it.url == url }?.title ?: HistoryPolicy.title("",url),0L) }
            }
            HistoryScreen(libraryShowing,libraryMode,items,history.loading,history.failed,dayMode.value,
                onBack = ::hideLibrary, onOpen = { url -> hideLibrary();openUrl(url) },
                onDelete = { item ->
                    when (libraryMode) {
                        BrowserLibrary.HISTORY -> history.delete(item.id)
                        BrowserLibrary.BOOKMARKS -> {
                            getSharedPreferences("eymen_browser_bookmarks",MODE_PRIVATE).edit()
                                .putStringSet("urls",bookmarkUrls()-item.url).apply();bookmarkRevision++
                        }
                        BrowserLibrary.RECENT_TABS -> recentlyClosedNormal.removeAll { it.safeUrl == item.url }
                    }
                },onClear = ::confirmClearHistory)
        }
        libraryLayer.visibility = View.GONE
        updateChromeMargins()
        updateHomeVisibility()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                when {
                    libraryShowing -> hideLibrary()
                    settingsShowing && functionalSettingsPage != null -> functionalSettingsPage = null
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
        if (savedInstanceState == null && intent?.action == EymenDownloadService.OPEN_DOWNLOADS) {
            root.post { showDownloads() }
        }
        if (savedInstanceState?.getBoolean("language_settings_visible", false) == true) {
            openSettingsPage(BrowserSettingsPage.MAIN)
            functionalSettingsPage = FunctionalSettingsPage.LANGUAGE
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean("language_settings_visible",
            settingsShowing && functionalSettingsPage == FunctionalSettingsPage.LANGUAGE)
        super.onSaveInstanceState(outState)
    }

    /** Existing attached sessions are released, never closed on tab switches. */
    private fun createTab(
        privateMode: Boolean,
        initialUrl: String? = null,
        deferRestoreLoad: Boolean = false,
        popupRequest: Boolean = false
    ): GeckoSession? {
        if (childMode.enabled && !privateMode)
            return createTab(true, initialUrl, deferRestoreLoad, popupRequest)
        if (initialUrl != null && !childMode.allows(initialUrl)) {
            notice(R.string.child_blocked); return null
        }
        if (::root.isInitialized) hideBrowserKeyboard()
        if (tabs.size >= TabWorkspaceStore.MAX_TABS) {
            notice(R.string.tab_capacity); return null
        }
        if (!privateMode && tabs.count { !it.privateMode } >= TabWorkspaceStore.MAX_TABS) {
            Toast.makeText(this, "En fazla 32 normal sekme açılabilir.", Toast.LENGTH_SHORT).show()
            return null
        }
        if (!popupRequest && tabs.isNotEmpty()) {
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
            openSession = !popupRequest,
            sitePermissions = settingsServices,
            allowNavigation = { url ->
                val allowed = childMode.allows(url)
                if (!allowed) notice(R.string.child_blocked)
                allowed
            },
            allowPopups = { tabs.firstOrNull { it.id == id }?.let {
                settingsServices.popupAllowed(it.url, it.privateMode)
            } ?: false },
            onPopupBlocked = { if (activeId.value == id) notice(R.string.popup_blocked) },
            onClose = {
                tabs.firstOrNull { it.id == id }?.takeIf { it.openerId != null }?.let { closeTab(id) }
            },
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
                tabs.firstOrNull { it.id == id }?.let { tab ->
                    tab.title = title.orEmpty()
                    if (!tab.privateMode && !childMode.enabled && tab.historyVisitAt > 0 && tab.historyUrl == tab.url)
                        history.updateTitle(tab.url,tab.title,tab.historyVisitAt)
                }
            },
            onCanGoBack = { value -> tabs.firstOrNull { it.id == id }?.canGoBack = value },
            onCanGoForward = { value -> tabs.firstOrNull { it.id == id }?.canGoForward = value },
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
                    // Keep the parent displayed and return an UNOPENED child.
                    // Gecko initializes the popup and its window.opener link.
                    createTab(privateMode = privateMode, initialUrl = popupUri,
                        popupRequest = true)
                }
            }
        )
        val tab = BrowserTab(id, session, privateMode, initialUrl, if (popupRequest) activeId.value else null)
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
                    tab.historyVisitAt = 0L;tab.historyUrl = null;tab.title = ""
                    tab.lastScrollY = 0
                    if (activeId.value == id && ::toolbarMotion.isInitialized) toolbarMotion.reset()
                    tab.home = false
                    if (activeId.value == id) updateAutofill()
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
                    if (!privateMode) saveNormalWorkspace()
                }
            }
            override fun onPageStop(session: GeckoSession, success: Boolean) {
                if (success && !tab.privateMode && !childMode.enabled && !tab.home &&
                    HistoryPolicy.safeUrl(tab.url,false) != null) {
                    tab.historyVisitAt = System.currentTimeMillis();tab.historyUrl = tab.url
                    history.record(tab.url,tab.title,false,tab.historyVisitAt)
                }
            }
        }
        session.setScrollDelegate(object : GeckoSession.ScrollDelegate {
            override fun onScrollChanged(session: GeckoSession, scrollX: Int, scrollY: Int) {
                tab.lastScrollY = scrollY.coerceAtLeast(0)
                if (activeId.value == id && ::toolbarMotion.isInitialized) toolbarMotion.onScroll(tab.lastScrollY)
            }
        })
        tabs.add(tab)
        cosmeticExtension?.let { installCosmeticPolicy(tab, it) }
        if (popupRequest) {
            val openerId = activeId.value
            awaitPopupOpen(tab, openerId)
            return session
        }
        activeId.value = id
        currentUrl.value = initialUrl.orEmpty()
        atHome.value = initialUrl == null
        updateAutofill()
        if (::tabsLayer.isInitialized) hideTabs()
        updateHomeVisibility()
        if (!privateMode) saveNormalWorkspace()
        return session
    }

    /** Wait for Gecko's own open() before attaching the popup to GeckoView. */
    private fun awaitPopupOpen(tab: BrowserTab, openerId: Long, attempts: Int = 0) {
        view.postDelayed({
            if (isFinishing || isDestroyed || tabs.none { it.id == tab.id }) return@postDelayed
            if (tab.gecko.isOpen) {
                if (activeId.value == openerId) activateTab(tab.id)
                if (!tab.privateMode) saveNormalWorkspace()
            } else if (attempts < 125) {
                awaitPopupOpen(tab, openerId, attempts + 1)
            } else {
                tabs.remove(tab)
                if (activeId.value == openerId) notice(R.string.bank_popup_failed)
            }
        }, 40L)
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
        if (::toolbarMotion.isInitialized) toolbarMotion.reset(tab.lastScrollY)
        val delayed = tab.pendingRestoreUrl
        if (delayed != null) {
            tab.pendingRestoreUrl = null
            tab.gecko.loadUri(delayed)
        }
        setVideoFullScreen(tab.fullScreen)
        updateAutofill()
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
        if (tab.gecko.isOpen) tab.gecko.close()
        tab.thumbnail = null
        tabs.remove(tab)
        if (tab.privateMode && tabs.none { it.privateMode }) privateShieldOverrides.clear()
        if (tab.privateMode && tabs.none { it.privateMode }) settingsServices.clearPrivatePermissions()
        if (active) {
            if (tabs.isEmpty()) {
                // Always leave a usable normal tab rather than a blank GeckoView.
                createTab(privateMode = false)
            } else {
                activeId.value = -1L
                activateTab(tab.openerId?.takeIf { parent -> tabs.any { it.id == parent } } ?: tabs.last().id)
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
        hideLibrary()
        if (::root.isInitialized) hideBrowserKeyboard()
        if (!::tabsLayer.isInitialized || videoFullScreen) return
        if (!devices) captureActiveTabThumbnail()
        devicesRequested.value = devices
        overviewShowing = true
        tabsLayer.visibility = View.VISIBLE
        updateOverlayAccessibility()
    }

    private fun hideTabs() {
        overviewShowing = false
        if (::tabsLayer.isInitialized) tabsLayer.visibility = View.GONE
        updateOverlayAccessibility()
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
        val direct = if (raw.startsWith("https://",true) || raw.startsWith("http://",true)) {
            GeckoMotorDenemesi.verifiedWebUrl(raw)
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
        if (!childMode.allows(url)) { notice(R.string.child_blocked); return }
        if (::root.isInitialized) hideBrowserKeyboard()
        val verified = GeckoMotorDenemesi.verifiedHttpsUrl(url)
            ?: HistoryPolicy.safeUrl(url,false)?.takeIf { it.startsWith("http://") }
        // Gecko still applies the user's HTTPS-only policy to HTTP history links.
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
                if (isFinishing || isDestroyed || activeId.value != tab.id || tabs.none { it.id == tab.id }) {
                    result.complete(prompt.dismiss())
                    return result
                }
                val done = AtomicBoolean(false)
                fun finish() {
                    if (done.compareAndSet(false, true)) result.complete(prompt.dismiss())
                }
                val builder = AlertDialog.Builder(this@MainActivity)
                    .setTitle(verifiedHost(tab.url) ?: getString(R.string.bank_js_prompt_title))
                    .setMessage(prompt.message.orEmpty().take(3000))
                    .setPositiveButton(android.R.string.ok) { _, _ -> finish() }
                    .setOnCancelListener { finish() }

                presentWebPrompt(builder) { finish() }
                return result
            }

            override fun onButtonPrompt(
                session: GeckoSession,
                prompt: GeckoSession.PromptDelegate.ButtonPrompt
            ): GeckoResult<GeckoSession.PromptDelegate.PromptResponse> {
                val result = GeckoResult<GeckoSession.PromptDelegate.PromptResponse>()
                if (isFinishing || isDestroyed || activeId.value != tab.id || tabs.none { it.id == tab.id }) {
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
                val builder = AlertDialog.Builder(this@MainActivity)
                    .setTitle(verifiedHost(tab.url) ?: getString(R.string.bank_js_prompt_title))
                    .setMessage(prompt.message.orEmpty().take(3000))
                    .setNegativeButton(android.R.string.cancel) { _, _ -> finish(false) }
                    .setPositiveButton(android.R.string.ok) { _, _ -> finish(true) }
                    .setOnCancelListener { finish(false) }

                presentWebPrompt(builder) { finish(false) }
                return result
            }

            override fun onTextPrompt(
                session: GeckoSession,
                prompt: GeckoSession.PromptDelegate.TextPrompt
            ): GeckoResult<GeckoSession.PromptDelegate.PromptResponse> {
                val result = GeckoResult<GeckoSession.PromptDelegate.PromptResponse>()
                if (isFinishing || isDestroyed || activeId.value != tab.id || tabs.none { it.id == tab.id }) {
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
                val builder = AlertDialog.Builder(this@MainActivity)
                    .setTitle(verifiedHost(tab.url) ?: getString(R.string.bank_js_prompt_title))
                    .setMessage(prompt.message.orEmpty().take(3000))
                    .setView(edit)
                    .setNegativeButton(android.R.string.cancel) { _, _ -> finish(false) }
                    .setPositiveButton(android.R.string.ok) { _, _ -> finish(true) }
                    .setOnCancelListener { finish(false) }

                presentWebPrompt(builder) { finish(false) }
                return result
            }
        }
    }

    private fun presentWebPrompt(builder: AlertDialog.Builder, dismissed: () -> Unit) {
        if (isFinishing || isDestroyed) { dismissed(); return }
        val dialog = builder.create()
        dialog.setOnDismissListener { webPrompts.remove(dialog); dismissed() }
        try { dialog.show(); webPrompts.add(dialog) }
        catch (_: RuntimeException) { dismissed() }
    }

    /**
     * Only opens a native-app deep link after a real HTTPS page user gesture
     * and a SECOND explicit Android dialog. Never relays page-supplied Intent
     * extras/components, JavaScript, files, implicit permissions or credentials.
     */
    private fun showSecureExternalAppPrompt(
        tabId: Long, link: String, trigger: String?, hasGesture: Boolean
    ): Boolean {
        if (childMode.enabled) return false
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
        if (childMode.enabled) { notice(R.string.child_blocked); return }
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
        if (childMode.enabled) { notice(R.string.child_blocked); return }
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
                else T.listeGece_zemin.toArgb()
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

    /** Overflow and full-page panels expose only their own accessibility tree. */
    private fun updateOverlayAccessibility() {
        if (!::root.isInitialized) return
        val owner = when {
            libraryShowing && ::libraryLayer.isInitialized -> libraryLayer
            settingsShowing && ::settingsLayer.isInitialized -> settingsLayer
            downloadsShowing && ::downloadsLayer.isInitialized -> downloadsLayer
            shieldShowing && ::shieldLayer.isInitialized -> shieldLayer
            overviewShowing && ::tabsLayer.isInitialized -> tabsLayer
            menuShowing && ::menuLayer.isInitialized -> menuLayer
            else -> null
        }
        for (index in 0 until root.childCount) {
            val child=root.getChildAt(index)
            child.importantForAccessibility=if(owner==null || child===owner)
                View.IMPORTANT_FOR_ACCESSIBILITY_AUTO else View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
        }
    }

    private fun canCollapseChrome(): Boolean {
        val accessibility = getSystemService(android.view.accessibility.AccessibilityManager::class.java)
        return !atHome.value && !videoFullScreen && !imeVisible && !menuShowing &&
            !overviewShowing && !settingsShowing && !downloadsShowing && !shieldShowing && !libraryShowing &&
            accessibility?.isTouchExplorationEnabled != true
    }

    private fun updateChromeMargins() {
        if (!::chrome.isInitialized || !::toolbarMotion.isInitialized) return
        val bottomBar = addressBottom
        val barHeightDp = T.telefon_liste_aracCubugu.toFloat()
        val barHeight = (barHeightDp * resources.displayMetrics.density).roundToInt()
        // The engine keeps its maximum measured height. Dynamic clipping moves
        // sticky site controls with the toolbar without repeatedly reflowing pages.
        view.layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,FrameLayout.LayoutParams.MATCH_PARENT)
        chrome.layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,barHeight,
            if (bottomBar) Gravity.BOTTOM else Gravity.TOP)
        toolbarMotion.configure(barHeight,bottomBar,videoFullScreen)
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
        hideBrowserKeyboard()
        if (!::shieldLayer.isInitialized || videoFullScreen) return
        if (overviewShowing) hideTabs()
        if (menuShowing) hideBrowserMenu()
        shieldShowing = true
        shieldLayer.visibility = View.VISIBLE
        updateOverlayAccessibility()
    }

    private fun hideShield() {
        shieldShowing = false
        if (::shieldLayer.isInitialized) shieldLayer.visibility = View.GONE
        updateOverlayAccessibility()
    }

    private fun showSettings() = openSettingsPage(BrowserSettingsPage.MAIN)

    private fun openSettingsPage(page: BrowserSettingsPage) {
        hideLibrary()
        settingsInitialPage = page
        hideBrowserKeyboard()
        if (!::settingsLayer.isInitialized || videoFullScreen) return
        if (menuShowing) hideBrowserMenu()
        if (downloadsShowing) hideDownloads()
        if (shieldShowing) hideShield()
        if (overviewShowing) hideTabs()
        settingsShowing = true
        settingsLayer.visibility = View.VISIBLE
        updateOverlayAccessibility()
    }

    private fun hideSettings() {
        hideBrowserKeyboard()
        functionalSettingsPage = null
        vpn.stopUiMonitoring()
        vpnScreenVisible = false
        settingsShowing = false
        if (::settingsLayer.isInitialized) settingsLayer.visibility = View.GONE
        updateOverlayAccessibility()
    }

    private fun handleSettingsAction(action: SettingsAction) {
        if (childMode.enabled && action != SettingsAction.CHILD_MODE) {
            childMode.parent { performSettingsAction(action) }
        } else performSettingsAction(action)
    }

    private fun performSettingsAction(action: SettingsAction) {
        when (action) {
            SettingsAction.ADDRESS -> openSettingsPage(BrowserSettingsPage.ADDRESS)
            SettingsAction.HOME -> openSettingsPage(BrowserSettingsPage.HOME)
            SettingsAction.DEFAULT_BROWSER -> requestDefaultBrowser()
            SettingsAction.NOTIFICATIONS -> openNotificationSettings()
            SettingsAction.SECURITY_CHECK -> { functionalSettingsPage=FunctionalSettingsPage.SECURITY }
            SettingsAction.ACCESSIBILITY -> { functionalSettingsPage=FunctionalSettingsPage.ACCESSIBILITY }
            SettingsAction.CLEAR_DATA -> { functionalSettingsPage=FunctionalSettingsPage.DATA }
            SettingsAction.TABS -> { hideSettings();showTabs() }
            SettingsAction.VPN -> {
                vpnScreenVisible = true
                vpn.startUiMonitoring()
            }
            SettingsAction.SHIELD -> {
                functionalSettingsPage = FunctionalSettingsPage.PRIVACY
            }
            SettingsAction.PASSWORDS -> functionalSettingsPage = FunctionalSettingsPage.PASSWORDS
            SettingsAction.SITE_PERMISSIONS -> functionalSettingsPage = FunctionalSettingsPage.PERMISSIONS
            SettingsAction.LANGUAGE -> functionalSettingsPage = FunctionalSettingsPage.LANGUAGE
            SettingsAction.EXTENSIONS -> functionalSettingsPage = FunctionalSettingsPage.EXTENSIONS
            SettingsAction.CHILD_MODE -> functionalSettingsPage = FunctionalSettingsPage.CHILD
            SettingsAction.TOOLS -> functionalSettingsPage = FunctionalSettingsPage.TOOLS
            SettingsAction.SEARCH_ENGINE -> showSearchEngineSelection()
            SettingsAction.APPEARANCE -> showAppearanceOptions()
            SettingsAction.DOWNLOADS -> {
                hideSettings()
                showDownloads()
            }
            SettingsAction.ABOUT -> updater.check(manual = true)
            SettingsAction.LICENSES -> {
                val text = assets.open("OPEN_SOURCE_NOTICES.txt").bufferedReader().use { it.readText() }
                AlertDialog.Builder(this).setTitle(R.string.about_licenses).setMessage(text)
                    .setPositiveButton(android.R.string.ok, null).show()
            }
        }
    }

    private fun refreshSystemPreferences() {
        defaultBrowser = if (Build.VERSION.SDK_INT>=29) {
            getSystemService(android.app.role.RoleManager::class.java)?.isRoleHeld(android.app.role.RoleManager.ROLE_BROWSER)==true
        } else try {
            packageManager.resolveActivity(Intent(Intent.ACTION_VIEW,Uri.parse("https://example.com/")),0)
                ?.activityInfo?.packageName==packageName
        } catch (_: Exception) { false }
        notificationsEnabled = getSystemService(android.app.NotificationManager::class.java)?.areNotificationsEnabled()==true
    }

    private fun requestDefaultBrowser() {
        try {
            val roles=if(Build.VERSION.SDK_INT>=29)getSystemService(android.app.role.RoleManager::class.java) else null
            if(Build.VERSION.SDK_INT>=29 && roles?.isRoleAvailable(android.app.role.RoleManager.ROLE_BROWSER)==true &&
                !roles.isRoleHeld(android.app.role.RoleManager.ROLE_BROWSER))
                browserRoleLauncher.launch(roles.createRequestRoleIntent(android.app.role.RoleManager.ROLE_BROWSER))
            else startActivity(Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS))
        } catch (_: Exception) { notice(R.string.settings_system_unavailable) }
    }

    private fun openNotificationSettings() {
        try { startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE,packageName)) }
        catch (_: Exception) { notice(R.string.settings_system_unavailable) }
    }

    private fun updateAutofill() {
        if (::view.isInitialized && ::settingsServices.isInitialized) {
            view.setAutofillEnabled(settingsServices.autofill && activeTab?.privateMode == false &&
                verifiedHost(activeTab?.url.orEmpty()) != null)
        }
    }

    private fun resetTabsForChildMode() {
        if (!::view.isInitialized) return
        view.releaseSession()
        tabs.forEach { if (it.gecko.isOpen) it.gecko.close() }
        tabs.clear(); recentlyClosedNormal.clear(); privateShieldOverrides.clear()
        activeId.value = -1L
        createTab(childMode.enabled)
        saveNormalWorkspace(durable = true)
    }

    private fun clearBrowsingData(cacheOnly: Boolean) {
        if (settingsServices.clearing) return
        val title = if (cacheOnly) R.string.cache_clear else R.string.cookie_clear
        AlertDialog.Builder(this).setTitle(title)
            .setMessage(if (cacheOnly) R.string.cache_clear_confirm else R.string.cookie_clear_confirm)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                if (settingsServices.clearing) return@setPositiveButton
                settingsServices.beginClear()
                view.releaseSession()
                tabs.forEach { if (it.gecko.isOpen) it.gecko.close() }
                tabs.clear(); activeId.value = -1L; privateShieldOverrides.clear()
                val flags = if (cacheOnly) StorageController.ClearFlags.ALL_CACHES else
                    StorageController.ClearFlags.COOKIES or StorageController.ClearFlags.DOM_STORAGES or StorageController.ClearFlags.AUTH_SESSIONS
                GeckoMotorDenemesi.getRuntime(this).storageController.clearData(flags).accept({ _ ->
                    if (!isFinishing && !isDestroyed) {
                        createTab(false)
                        settingsServices.finishClear(if (cacheOnly) R.string.cache_clear_success else R.string.cookie_clear_success)
                    }
                }, { _ ->
                    if (!isFinishing && !isDestroyed) { createTab(false); settingsServices.finishClear(R.string.settings_operation_failed) }
                })
            }.show()
    }

    private fun openAddonOptions(url: String) {
        if (childMode.enabled) { notice(R.string.child_blocked); return }
        if (try { URI(url).scheme != "moz-extension" } catch (_: Exception) { true }) return
        hideSettings()
        val session = createTab(false) ?: return
        activeTab?.let { tab -> tab.url = url; tab.home = false }
        atHome.value = false; currentUrl.value = url; updateHomeVisibility()
        session.loadUri(url)
    }

    private fun translateCurrentPage() {
        val tab = activeTab?.takeIf { !it.home && verifiedHost(it.url) != null }
        if (tab == null) { notice(R.string.invalid_address); return }
        val translation = tab.gecko.sessionTranslation
        if (translation == null) { notice(R.string.translation_failed); return }
        AlertDialog.Builder(this).setTitle(R.string.translation_title)
            .setItems(arrayOf(getString(R.string.translation_en_tr), getString(R.string.translation_tr_en), getString(R.string.translation_original))) { _, which ->
                if (which == 2) {
                    translation.restoreOriginalPage().accept({ _ -> Unit }, { _ -> notice(R.string.translation_failed) })
                } else {
                    AlertDialog.Builder(this).setTitle(R.string.translation_pair).setMessage(R.string.translation_download)
                        .setNegativeButton(android.R.string.cancel, null).setPositiveButton(android.R.string.ok) { _, _ ->
                            val from = if (which == 0) "en" else "tr"
                            val to = if (which == 0) "tr" else "en"
                            val options = TranslationsController.SessionTranslation.TranslationOptions.Builder().downloadModel(true).build()
                            translation.translate(from, to, options).accept(
                                { _ -> notice(R.string.translation_started) }, { _ -> notice(R.string.translation_failed) })
                        }.show()
                }
            }.show()
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
                        // Gecko supplies the real top-level frame URL. Using
                        // tab.url here races the first onPageStart callback.
                        // Native per-site policy uses this trusted frame URL
                        // and the owning GeckoSession's privacy mode.
                        protectionFor(sender.url, tab.privateMode)
                    return GeckoResult.fromValue(
                        // Gecko's event callback accepts primitive values, not
                        // org.json.JSONObject. Serialize this private policy
                        // reply across the native messaging boundary.
                        JSONObject().put("enabled", allowed && cosmeticFilterWanted.value)
                            .put("rejectCookies", allowed && settingsServices.cookieReject).toString() as Any
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
        if (::toolbarMotion.isInitialized) toolbarMotion.show()
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
        updateOverlayAccessibility()
    }

    private fun hideDownloads() {
        hideBrowserKeyboard()
        downloadsShowing = false
        if (::downloadsLayer.isInitialized) downloadsLayer.visibility = View.GONE
        updateOverlayAccessibility()
    }

    private fun openDownloadedFile(file: SavedDownload) {
        if (childMode.enabled) { notice(R.string.child_blocked); return }
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
        hideBrowserKeyboard()
        if (!::menuLayer.isInitialized || videoFullScreen) return
        menuShowing = true
        menuLayer.visibility = View.VISIBLE
        updateOverlayAccessibility()
    }

    private fun hideBrowserMenu() {
        menuShowing = false
        if (::menuLayer.isInitialized) menuLayer.visibility = View.GONE
        updateOverlayAccessibility()
    }

    private fun handleMenuAction(action: MenuAction) {
        if (childMode.enabled && action in setOf(MenuAction.QR, MenuAction.SHARE, MenuAction.SEND_TO_DEVICE)) {
            notice(R.string.child_blocked); return
        }
        hideBrowserMenu()
        when (action) {
            MenuAction.NEW_TAB -> createTab(privateMode = false)
            MenuAction.TAB_GROUP -> { hideSettings();showTabs() }
            MenuAction.RECENT_TABS -> showLibrary(BrowserLibrary.RECENT_TABS)
            MenuAction.CLEAR_DATA -> { showSettings();handleSettingsAction(SettingsAction.CLEAR_DATA) }
            MenuAction.SITE_CONTROLS -> showShield()
            MenuAction.HELP -> openSettingsPage(BrowserSettingsPage.ABOUT)
            MenuAction.BOOKMARKS -> showBookmarks()
            MenuAction.HISTORY -> showRecentPages()
            MenuAction.DOWNLOADS -> showDownloads()
            MenuAction.PRIVATE -> createTab(privateMode = true)
            MenuAction.FIND -> showFindDialog()
            MenuAction.TRANSLATE -> translateCurrentPage()
            MenuAction.READER -> toggleReaderMode()
            MenuAction.QR -> qrScanner.start()
            MenuAction.SEND_TO_DEVICE -> shareCurrentPage()
            MenuAction.EXTENSIONS -> { showSettings(); handleSettingsAction(SettingsAction.EXTENSIONS) }
            MenuAction.TOOLS -> { showSettings(); handleSettingsAction(SettingsAction.TOOLS) }
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
        }
    }

    private fun toggleReaderMode() {
        val tab = activeTab ?: return
        val url = tab.url
        if (url.startsWith("about:reader?")) {
            val original = Uri.parse(url).getQueryParameter("url")
            if (original != null) openUrl(original)
            return
        }
        val https = GeckoMotorDenemesi.verifiedHttpsUrl(url)?.takeIf { it == url && !tab.home }
        if (https == null) { notice(R.string.invalid_address); return }
        notice(R.string.reader_note)
        tab.gecko.loadUri("about:reader?url=" + URLEncoder.encode(https, "UTF-8"))
    }

    private fun bookmarkUrls(): Set<String> = getSharedPreferences("eymen_browser_bookmarks",MODE_PRIVATE)
        .getStringSet("urls",emptySet()).orEmpty().filter { GeckoMotorDenemesi.verifiedHttpsUrl(it) == it }.toSet()

    private fun isCurrentBookmarked(): Boolean {
        bookmarkRevision
        return activeTab?.url in bookmarkUrls()
    }

    private fun toggleCurrentBookmark() {
        val tab = activeTab ?: return
        val url = GeckoMotorDenemesi.verifiedHttpsUrl(tab.url)?.takeIf { it==tab.url && !tab.home } ?: return
        val old = bookmarkUrls()
        getSharedPreferences("eymen_browser_bookmarks",MODE_PRIVATE).edit()
            .putStringSet("urls",if (url in old) old-url else old+url).apply()
        bookmarkRevision++
    }

    private fun showBookmarks() = showLibrary(BrowserLibrary.BOOKMARKS)
    private fun showRecentPages() = showLibrary(BrowserLibrary.HISTORY)
    private fun showLibrary(mode: BrowserLibrary) {
        if (childMode.enabled) childMode.parent { openLibrary(mode) } else openLibrary(mode)
    }
    private fun openLibrary(mode: BrowserLibrary) {
        hideBrowserKeyboard();hideBrowserMenu();hideSettings();hideDownloads();hideShield();hideTabs()
        libraryMode = mode;history.refresh();libraryShowing = true;libraryLayer.visibility = View.VISIBLE
        updateOverlayAccessibility()
    }
    private fun hideLibrary() {
        hideBrowserKeyboard()
        libraryShowing = false
        if (::libraryLayer.isInitialized) libraryLayer.visibility = View.GONE
        updateOverlayAccessibility()
    }
    private fun confirmClearHistory() {
        AlertDialog.Builder(android.view.ContextThemeWrapper(this,if(dayMode.value)
            android.R.style.Theme_Material_Light_Dialog_Alert else android.R.style.Theme_Material_Dialog_Alert))
            .setTitle(R.string.history_clear).setMessage(R.string.history_clear_confirm)
            .setNegativeButton(android.R.string.cancel,null)
            .setPositiveButton(android.R.string.ok) { _,_ -> history.clear() }.show()
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
        if (childMode.enabled) { notice(R.string.child_blocked); return }
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
        val done = AtomicBoolean(false)
        val tabId = activeId.value
        fun finish(allowed: Boolean) {
            if (done.compareAndSet(false, true)) answer(allowed && activeId.value == tabId)
        }
        val builder = AlertDialog.Builder(this)
            .setTitle(getString(R.string.drm_permission_title))
            .setMessage(getString(R.string.drm_permission_message, host))
            .setPositiveButton(getString(R.string.drm_permission_allow)) { _, _ -> finish(true) }
            .setNegativeButton(getString(R.string.drm_permission_deny)) { _, _ -> finish(false) }
            .setOnCancelListener { finish(false) }
        presentWebPrompt(builder) { finish(false) }
    }

    private fun shareNormalTab(id: Long) {
        if (childMode.enabled) { notice(R.string.child_blocked); return }
        val tab = tabs.firstOrNull { it.id == id && !it.privateMode && !it.home } ?: return
        val url = GeckoMotorDenemesi.verifiedHttpsUrl(tab.url)?.takeIf { it == tab.url } ?: return
        try { startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"; putExtra(Intent.EXTRA_TEXT, url)
        }, getString(R.string.menu_send_device))) }
        catch (_: Exception) { notice(R.string.settings_operation_failed) }
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
        if (::toolbarMotion.isInitialized) toolbarMotion.show(false)
        webPrompts.toList().forEach { it.dismiss() }
        webPrompts.clear()
        if (::settingsServices.isInitialized) settingsServices.stop()
        if (::addons.isInitialized) addons.stop()
        if (::childMode.isInitialized) childMode.stop()
        // UI status polling does not run while the Activity is hidden.
        vpn.stopUiMonitoring()
        downloads.pauseObserving()
        // Synchronous write at the lifecycle boundary for process/phone death.
        if (::view.isInitialized) saveNormalWorkspace(durable = true)
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
        refreshSystemPreferences()
        updateAutofill()
        downloads.observe()
        savedFiles.clear()
        savedFiles.addAll(downloadRegistry.items())
        if (::prefs.isInitialized) updater.onResumed()
        if (vpnScreenVisible) vpn.startUiMonitoring()
        else vpn.refreshState()
    }

    override fun onDestroy() {
        if (::toolbarMotion.isInitialized) toolbarMotion.dispose()
        vpn.stopUiMonitoring()
        updater.dispose()
        downloads.dispose()
        if (::view.isInitialized) view.releaseSession()
        tabs.forEach { if (it.gecko.isOpen) it.gecko.close() }
        tabs.clear()
        tabGroups.clear()
        recentlyClosedNormal.clear()
        privateShieldOverrides.clear()
        super.onDestroy()
    }
}
