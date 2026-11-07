#!/usr/bin/env python3
"""Static design and state wiring guards for T05, T08, T10.

They do NOT assert device behavior or downloaded file validity.
"""
from pathlib import Path

root = Path(__file__).resolve().parents[1]
main = (root / "android/app-telefon/src/main/java/com/eymen/browser/telefon/MainActivity.kt").read_text()
motor = (root / "android/core-motor/src/main/java/com/eymen/browser/motor/GeckoMotorDenemesi.kt").read_text()
shield = (root / "android/app-telefon/src/main/java/com/eymen/browser/telefon/ShieldPanel.kt").read_text()
downloads = (root / "android/app-telefon/src/main/java/com/eymen/browser/telefon/DownloadsScreen.kt").read_text()
download_engine = (root / "android/app-telefon/src/main/java/com/eymen/browser/telefon/AndroidDownloadDenemesi.kt").read_text()
download_coordinator = (root / "android/app-telefon/src/main/java/com/eymen/browser/telefon/DownloadCoordinator.kt").read_text()
registry = (root / "android/app-telefon/src/main/java/com/eymen/browser/telefon/DownloadRegistry.kt").read_text()
settings = (root / "android/app-telefon/src/main/java/com/eymen/browser/telefon/SettingsScreen.kt").read_text()

assert 'useTrackingProtection(trackingProtection)' in motor
assert 'ContentBlocking.AntiTracking.DEFAULT' in motor
assert 'setContentBlockingDelegate' in motor
assert 'onContentBlocked' in motor
assert 'tab.blockedPage += 1' in main
# The engine opens an internal about:blank when creating an empty tab;
# it must never hide the approved new-tab homepage.
assert 'if ((tab.home || tab.pendingRestoreUrl != null) &&' in main
assert 'url.equals("about:blank", ignoreCase = true)' in main
assert 'return@runOnUiThread' in main
assert main.index('if ((tab.home || tab.pendingRestoreUrl != null)') < main.index('tab.home = false')
assert 'tab.blockedTab += 1' in main
assert 'if (enabled) p.protection else warning' in shield
assert 'canToggle = tab != null' in main
assert 'getSharedPreferences("eymen_browser_shield_sites"' in main
assert 'private val privateShieldOverrides = mutableMapOf<String, Boolean>()' in main
assert 'if (current.privateMode)' in main
assert 'privateShieldOverrides[host] = enabled' in main
assert 'if (tab.privateMode && tabs.none { it.privateMode }) privateShieldOverrides.clear()' in main
assert 'tab.privateMode == current.privateMode' in main
assert 'tab.gecko.settings.setUseTrackingProtection(enabled)' in main
assert 'pageBlocked.toString()' in shield
assert 'tabBlocked.toString()' in shield
assert 'listOf(' in shield
assert 'shield_scope_explanation' in shield
# v1.0.9: an accurate GeckoView category count, not a synthetic ad counter.
assert 'ContentBlocking.AntiTracking.STRICT' in motor
assert 'ContentBlocking.AntiTracking.DEFAULT' in motor
assert 'ContentBlocking.EtpLevel.STRICT' in motor
assert 'fun configureTrackerProtection(context: Context, strict: Boolean)' in motor
assert 'if (event.isBlocking) onBlocked(event.antiTrackingCategory)' in motor
assert 'private val strictShieldWanted = mutableStateOf(false)' in main
assert 'strictShieldWanted.value = prefs.getBoolean("shield_strict_mode", false)' in main
assert 'GeckoMotorDenemesi.configureTrackerProtection(this, strictShieldWanted.value)' in main
assert 'GeckoMotorDenemesi.configureTrackerProtection(this, strict)' in main
assert 'setShieldForCurrentSite(enabled: Boolean)' in main
assert 'if (current.privateMode)' in main
assert 'category and ContentBlocking.AntiTracking.AD != 0' in main
assert 'category and ContentBlocking.AntiTracking.ANALYTIC != 0' in main
assert 'tab.adTrackerBlockedTab += 1' in main
assert 'tab.analyticsBlockedTab += 1' in main
assert 'tab.otherTrackerBlockedTab += 1' in main
assert 'onModeChange = ::setShieldMode' in main
assert 'onModeChange: (Boolean) -> Unit' in shield
assert 'onModeChange(isStrict)' in shield
assert 'shield_count_scope' in shield


assert 'bytes = bytes' in download_coordinator
assert 'savedAt = System.currentTimeMillis()' in download_coordinator
assert 'completed = SavedDownload(' in download_coordinator
assert 'registry.record(candidate)' in download_coordinator
# Activity's ContextWrapper has no base Context during MainActivity property initialization.
assert 'private val coordinator by lazy { DownloadCoordinator.get(activity.applicationContext) }' in download_engine
assert 'private val registry = DownloadRegistry(activity.applicationContext)' not in download_engine
assert 'if (!file.uri.startsWith("content://")' in registry
assert 'fun clearListOnly()' in registry
assert 'prefs.edit().remove("completed").apply()' in registry
assert 'visibleFiles.forEach { file ->' in downloads
assert 'files.sumOf { it.bytes }' in downloads
assert 'onOpen(file)' in downloads
assert 'onClear: () -> Unit' in downloads
assert 'setDataAndType(Uri.parse(file.uri), file.mimeType)' in main
assert 'Intent.FLAG_GRANT_READ_URI_PERMISSION' in main
assert 'downloadRegistry.clearListOnly()' in main
assert 'download_clear_confirm' in downloads

assert 'settingsRows.filter' in settings
assert 'SettingRow(R.string.settings_shield' in settings
assert 'settings_basic' in settings and 'SettingsActionRow' in settings
assert 'SettingRow(R.string.settings_about_update' in settings
assert 'SettingsAction.SHIELD ->' in main
assert 'SettingsAction.SEARCH_ENGINE -> showSearchEngineSelection()' in main
assert 'SettingsAction.DOWNLOADS ->' in main
assert 'MenuAction.SETTINGS -> showSettings()' in main
# Navigation-bar safety for API 35 edge-to-edge and older 3-button devices.
chrome = (root / "android/app-telefon/src/main/java/com/eymen/browser/telefon/BrowserChrome.kt").read_text()
assert 'Modifier.navigationBarsPadding()' not in chrome
assert 'bottomSystemInsetPx' in main
assert 'insets.getInsets(WindowInsets.Type.navigationBars()).bottom' in main
assert 'root.setPadding(left, top, right, bottom)' in main
assert 'T.telefon_liste_aracCubugu.toFloat()' in main
assert 'LightBrowserHome(' in main
assert 'LightBrowserToolbar(' in main
assert 'BrowserMenu(' in main
# Signed upgrades from <=v1.0.4 must not strand users on the saved dark default.
assert 'light_design_default_migrated_v105' in main
assert '.putBoolean("day_theme", true)' in main
assert main.index('light_design_default_migrated_v105') < main.index('dayMode.value = prefs.getBoolean("day_theme", true)')
assert 'SettingsAction.ABOUT -> updater.check(manual = true)' in main
assert 'updater.onResumed()' in main
assert 'updater.dispose()' in main

# S-002: flat list overflow and settings replace the old narrow/grid panels.
light_ui = (root / "android/app-telefon/src/main/java/com/eymen/browser/telefon/LightBrowserUi.kt").read_text()
menu = (root / "android/app-telefon/src/main/java/com/eymen/browser/telefon/BrowserMenu.kt").read_text()
assert '.width(width).heightIn(max=available)' in menu
assert 'ListPageHeader' in settings and 'SettingsRows' in settings
assert 'settings_personal_hint' not in settings
assert 'buildAnnotatedString' in light_ui
assert 'append("Eymen ")' in light_ui and 'append("Browser")' in light_ui
assert 'quadraticBezierTo(' in light_ui
assert 'LightBrowserToolbar(' in light_ui and 'onVoiceSearch' in light_ui
assert 'BrowserChromeMotion' in main and 'session.setCompositorScrollDelegate' in main
assert 'SOURCE_USER_INTERACTION' in main and 'update.scrollY*update.zoom' in main
motion = (root / "android/app-telefon/src/main/java/com/eymen/browser/telefon/BrowserChromeMotion.kt").read_text()
assert 'engine.setVerticalClipping(-(height-visible))' in motion
assert 'engine.setDynamicToolbarMaxHeight(height)' in motion
assert 'event.rawY' in motion and 'return super.dispatchTouchEvent(event)' in motion
visual_check = (root / 'tools/capture_android_visual.sh').read_text()
assert 'S-002 native list overflow' in visual_check

for label in ('T05-Kalkan','T08-Indirmeler','T10-Ayarlar'):
    assert (root / f'design/ekranlar/{label}.png').exists()

print('T05/T08/T10 static checks passed; physical-device acceptance remains open.')
