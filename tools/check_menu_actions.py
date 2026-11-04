#!/usr/bin/env python3
"""T07 menu integration static checks; on-device behavior still requires testing."""
from pathlib import Path

root = Path(__file__).resolve().parents[1]
main = (root / "android/app-telefon/src/main/java/com/eymen/browser/telefon/MainActivity.kt").read_text()
menu = (root / "android/app-telefon/src/main/java/com/eymen/browser/telefon/BrowserMenu.kt").read_text()
design = (root / "design/kaynak/T07-Menu.html").read_text()
tokens = (root / "design/tokens.json").read_text()

assert 'grid-template-columns: repeat(4,' in design
assert 'height: 628px' in design
assert 'height: 196px' not in menu, "T07 grid must not reuse T06 card height"
assert 'tiles.chunked(4)' in menu, "menu must remain in four columns"
assert 'private val tiles = listOf(' in menu
assert 'EymenPaths.' in menu
assert 'tile.supported' in menu and '.clickable(enabled = allowed)' in menu
assert 'MenuAction.TRANSLATE, false' in menu
assert 'MenuAction.READER, false' in menu
assert 'MenuAction.PDF, false' in menu
assert 'MenuAction.SEND_TO_DEVICE, false' in menu
assert 'MenuAction.QR, false' in menu
assert 'MenuZoomPlaceholder' in menu
assert 'GeckoView' in main
assert 'BrowserMenu(' in main and 'menuLayer.visibility = View.GONE' in main
assert 'MenuAction.BOOKMARKS -> showBookmarks()' in main
assert 'getSharedPreferences("eymen_browser_bookmarks"' in main
assert 'MenuAction.HISTORY -> showRecentPages()' in main
assert 'DownloadManager.ACTION_VIEW_DOWNLOADS' in main
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
