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
registry = (root / "android/app-telefon/src/main/java/com/eymen/browser/telefon/DownloadRegistry.kt").read_text()
settings = (root / "android/app-telefon/src/main/java/com/eymen/browser/telefon/SettingsScreen.kt").read_text()

assert 'useTrackingProtection(trackingProtection)' in motor
assert 'ContentBlocking.AntiTracking.DEFAULT' in motor
assert 'setContentBlockingDelegate' in motor
assert 'onContentBlocked' in motor
assert 'tab.blockedPage += 1' in main
assert 'tab.blockedTab += 1' in main
assert 'if (enabled) T.gece_koruma else T.gece_uyari' in shield
assert 'canToggle = tab != null' in main
assert 'getSharedPreferences("eymen_browser_shield_sites"' in main
assert 'tab.gecko.settings.setUseTrackingProtection(enabled)' in main
assert 'pageBlocked.toString()' in shield
assert 'tabBlocked.toString()' in shield
assert 'listOf(' in shield
assert 'shield_scope_explanation' in shield

assert 'bytes = bytes' in download_engine
assert 'savedAt = System.currentTimeMillis()' in download_engine
assert 'registry.record(SavedDownload(' in download_engine
assert 'if (!file.uri.startsWith("content://")' in registry
assert 'fun clearListOnly()' in registry
assert 'prefs.edit().remove("completed").apply()' in registry
assert 'visible.forEach { file ->' in downloads
assert 'files.sumOf { it.bytes }' in downloads
assert 'onOpen(file)' in downloads
assert 'onClear: () -> Unit' in downloads
assert 'setDataAndType(Uri.parse(file.uri), file.mimeType)' in main
assert 'Intent.FLAG_GRANT_READ_URI_PERMISSION' in main
assert 'downloadRegistry.clearListOnly()' in main
assert 'download_clear_confirm' in main

assert 'settingsRows.filter' in settings
assert 'SettingRow(R.string.settings_shield' in settings
assert 'settings_sync_off' in settings
assert 'SettingRow(R.string.settings_about_update' in settings
assert 'SettingsAction.SHIELD ->' in main
assert 'SettingsAction.SEARCH_ENGINE -> showSearchEngineSelection()' in main
assert 'SettingsAction.DOWNLOADS ->' in main
assert 'MenuAction.SETTINGS -> showSettings()' in main
assert 'getPackageInfo(packageName, 0).versionName' in main

for label in ('T05-Kalkan','T08-Indirmeler','T10-Ayarlar'):
    assert (root / f'design/ekranlar/{label}.png').exists()

print('T05/T08/T10 static checks passed; physical-device acceptance remains open.')
