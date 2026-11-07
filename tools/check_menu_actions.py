#!/usr/bin/env python3
"""T07 menu integration static checks; on-device behavior still requires testing."""
from pathlib import Path

root = Path(__file__).resolve().parents[1]
main = (root / "android/app-telefon/src/main/java/com/eymen/browser/telefon/MainActivity.kt").read_text()
menu = (root / "android/app-telefon/src/main/java/com/eymen/browser/telefon/BrowserMenu.kt").read_text()
design = (root / "design/kaynak/T07-Menu.html").read_text()
tokens = (root / "design/tokens.json").read_text()

# S-002: the user's 7 Nov Chrome reference supersedes the old 4x3 sheet.
assert 'private val groups = listOf(' in menu
assert 'rows.forEach' in menu and '.verticalScroll(rememberScrollState())' in menu
assert 'heightIn(max=available)' in menu
assert 'MenuAction.HISTORY' in menu and 'MenuAction.CLEAR_DATA' in menu
assert 'canGoBack: Boolean' in menu and 'canGoForward: Boolean' in menu
assert 'EymenPaths.' in menu
assert '.clickable(enabled=allowed' in menu
assert 'MenuAction.TRANSLATE -> translateCurrentPage()' in main
assert 'MenuAction.READER -> toggleReaderMode()' in main
assert 'MenuAction.PDF' in menu
assert 'MenuAction.PDF -> saveCurrentPageAsPdf()' in main
assert 'tab.gecko.saveAsPdf().accept(' in main
assert 'downloads.savePdfStream(stream)' in main
assert 'private fun offerDownload(request: Pending)' in (root / "android/app-telefon/src/main/java/com/eymen/browser/telefon/AndroidDownloadDenemesi.kt").read_text()
assert 'MenuAction.SEND_TO_DEVICE -> shareCurrentPage()' in main
assert 'MenuAction.QR -> qrScanner.start()' in main
assert 'MenuZoomButton' in menu and 'onTextSize(textPercent + 10)' in menu
assert 'GeckoView' in main
assert 'BrowserMenu(' in main and 'menuLayer.visibility = View.GONE' in main
assert 'MenuAction.BOOKMARKS -> showBookmarks()' in main
assert 'getSharedPreferences("eymen_browser_bookmarks"' in main
assert 'MenuAction.HISTORY -> showRecentPages()' in main
assert 'MenuAction.DOWNLOADS -> showDownloads()' in main
assert 'DownloadsScreen(' in main
assert 'downloadRegistry.items()' in main
assert 'MenuAction.PRIVATE -> createTab(privateMode = true)' in main
assert 'tab.gecko.finder.find(term, 0)' in main
assert 'GeckoSessionSettings.USER_AGENT_MODE_DESKTOP' in main
assert 'GeckoSessionSettings.VIEWPORT_MODE_DESKTOP' in main
assert 'Intent.ACTION_SEND' in main
assert 'prefs.edit().putBoolean("day_theme", dayMode.value)' in main
assert 'menuShowing -> hideBrowserMenu()' in main
assert 'onDownload = downloads::handleResponse' in main
assert 'koruma' in tokens
print("T07 static regression checks passed; no real-device behavior has been inferred.")
