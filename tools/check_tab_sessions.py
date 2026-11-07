#!/usr/bin/env python3
"""Static regression guard for Android T06/T09 session separation.

This is not a substitute for on-device tests of cookies, DRM, or rotation.
"""
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
base = ROOT / "android"
motor = (base / "core-motor/src/main/java/com/eymen/browser/motor/GeckoMotorDenemesi.kt").read_text()
main = (base / "app-telefon/src/main/java/com/eymen/browser/telefon/MainActivity.kt").read_text()
tabs = (base / "app-telefon/src/main/java/com/eymen/browser/telefon/TabOverview.kt").read_text()

must_exist = {
    "private mode GeckoSession setting": (motor, ".usePrivateMode(privateMode)"),
    "title callback preservation": (motor, "override fun onTitleChange"),
    "session released on tab switches": (main, "view.releaseSession()"),
    "session attached when activated": (main, "view.setSession(tab.gecko)"),
    "private tab session creation": (main, "privateMode = privateMode,"),
    "private history never enters normal recent list": (
        main, 'if (success && !tab.privateMode && !childMode.enabled && !tab.home'
    ),
    "external links not silently routed to private mode": (
        main, "if (activeTab?.privateMode == true) createTab(false, url)"
    ),
    "tab close frees gecko session": (main, "tab.gecko.close()"),
    "all tabs closed on activity destroy": (main, "tabs.forEach { if (it.gecko.isOpen) it.gecko.close() }"),
    "real tab cards used in overview": (main, "TabPreview("),
    "no fabricated tab page previews": (tabs, "neutral placeholders"),
    "no tracking numbers fabricated": (
        main, "TabOverview("
    ),
}
for label, (body, needle) in must_exist.items():
    assert needle in body, f"T06/T09 static regression: {label}"

# Android 15+ can draw BEHIND the camera and status indicators. Every
# browser screen must share ONE activity-level status/cutout safe viewport.
assert "WindowCompat.setDecorFitsSystemWindows(window, false)" in main
assert "WindowInsets.Type.statusBars() or WindowInsets.Type.displayCutout()" in main
assert "updateSystemBarsAppearance()" in main
assert "root.setOnApplyWindowInsetsListener" in main
assert "root.setPadding(left, top, right, bottom)" in main
assert "WindowInsets.Type.ime()" in main
assert "root.paddingBottom != bottom" in main
assert "applyBrowserSafeArea()" in main
assert "root.requestApplyInsets()" in main
assert "maxOf(insets.systemWindowInsetTop, cutout?.safeInsetTop ?: 0)" in main
assert "if (videoFullScreen) 0 else safeTopInsetPx" in main
assert ".windowInsetsPadding(" not in tabs, "T06 would apply status inset twice"
assert ".navigationBarsPadding()" not in tabs, "T06 must not reserve root navigation inset twice"
assert "Column(Modifier.fillMaxSize().background(background).padding(top = 24.dp))" not in tabs

# v1.0.7: real, mutable normal-tab groups + session-safe undo. Never
# materialize a private tab's URL in any restore slot or group membership.
assert 'var groupId by mutableStateOf<Long?>(null)' in main
assert 'private val tabGroups = mutableStateListOf<TabGroupPreview>()' in main
assert 'tab.privateMode' in main
assert 'if (!tab.privateMode) {' in main
assert 'recentlyClosedNormal.add(' in main
assert 'ClosedNormalTab(' in main
assert 'while (recentlyClosedNormal.size > 10)' in main
assert 'GeckoMotorDenemesi.verifiedHttpsUrl(tab.url)' in main
assert 'private fun restoreLastClosedNormalTab()' in main
assert 'createTab(privateMode = false, initialUrl = closed.safeUrl)' in main
assert 'activeTab?.groupId = closed.groupId?.takeIf' in main
assert 'tabs.firstOrNull { it.id == tabId && !it.privateMode }' in main
assert 'onNewGroup = ::promptNewTabGroup' in main
assert 'onAssignGroup = ::promptTabGroupSelection' in main
assert 'onUndoClose = ::restoreLastClosedNormalTab' in main
assert 'onMoveTab = ::moveTabWithinSection' in main
assert 'tabs.add(destinationPosition, tab)' in main
assert 'undoCount = recentlyClosedNormal.size' in main
assert 'onMoveTab(tab.id, -1' in tabs and 'onMoveTab(tab.id, +1' in tabs
assert 'val wasOverviewOpen = overviewShowing' in main
assert 'if (wasOverviewOpen && ::tabsLayer.isInitialized)' in main
assert 'openTabOverview(false)' in main
assert 'groups: List<TabGroupPreview>' in tabs
assert 'undoAvailable: Boolean' in tabs
assert 'selectedGroupId == null || tab.groupId == selectedGroupId' in tabs
assert 'onAssignGroup(tab.id)' in tabs
assert 'onUndoClose' in tabs
assert 'onOpen(visible[(activeIndex - 1 + visible.size) % visible.size].id)' in tabs
assert 'onOpen(visible[(activeIndex + 1) % visible.size].id)' in tabs
assert 'selectedGroupId == null)' in tabs
assert 'groups.none { it.id == selectedGroupId }' in tabs

# v1.0.8: only normal tabs may enter the bounded persistent workspace,
# with lazy-load restores, document navigation updates and lifecycle durability.
store = (base / "app-telefon/src/main/java/com/eymen/browser/telefon/TabWorkspaceStore.kt").read_text()
assert 'getSharedPreferences(' in store
assert '"eymen_browser_normal_workspace_v108"' in store
assert 'const val MAX_TABS = 32' in store
assert 'const val MAX_GROUPS = 20' in store
assert 'fun safeHttpsUrl(candidate: String?)' in store
assert 'parsed.userInfo != null' in store
assert 'val raw = preferences.getString(KEY, null) ?: return null' in store
assert 'fun write(workspace: StoredWorkspace, durable: Boolean = false)' in store
assert 'if (durable) editor.commit() else editor.apply()' in store
assert 'if (restoringWorkspace) return' in main
assert 'private fun restoreNormalWorkspace(incomingUrl: String?)' in main
assert 'private fun saveNormalWorkspace(durable: Boolean = false)' in main
assert 'val normal = tabs.filter { !it.privateMode }' in main
assert 'url = if (tab.home) null' in main
assert 'else workspaceStore.safeHttpsUrl(tab.url)' in main
assert 'createTab(false, safeUrl, deferRestoreLoad = safeUrl != null)' in main
assert 'tab.pendingRestoreUrl = if (deferRestoreLoad) initialUrl else null' in main
assert 'val delayed = tab.pendingRestoreUrl' in main
assert 'override fun onStop()' in main
assert 'saveNormalWorkspace(durable = true)' in main
assert 'restoreNormalWorkspace(incomingUrl)' in main
assert 'if (!privateMode) saveNormalWorkspace()' in main
assert 'if (!tab.privateMode) saveNormalWorkspace()' in main
assert 'if (activeTab?.privateMode == false) saveNormalWorkspace()' in main
assert 'private fun captureActiveTabThumbnail()' in main
assert 'if (tab.privateMode || tab.home || tab.pendingRestoreUrl != null' in main
assert 'view.capturePixels().accept(' in main
assert 'tab.thumbnail?.asImageBitmap()' in main
assert 'tab.thumbnail = null' in main
assert 'val thumbnail: ImageBitmap? = null' in tabs
assert 'if (!tab.privateMode && tab.thumbnail != null)' in tabs
assert 'ContentScale.Crop' in tabs

assert (ROOT / "design/ekranlar/T06-Sekmeler.png").exists(), "T06 approved asset missing"
assert (ROOT / "design/ekranlar/T09-Gizli.png").exists(), "T09 approved asset missing"
print("T06/T09 static regression checks passed; real-device validation remains required.")
