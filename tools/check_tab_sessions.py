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
        main, 'if (!privateMode && url.startsWith("https://", true)'
    ),
    "external links not silently routed to private mode": (
        main, "if (activeTab?.privateMode == true) createTab(false, url)"
    ),
    "tab close frees gecko session": (main, "tab.gecko.close()"),
    "all tabs closed on activity destroy": (main, "tabs.forEach { it.gecko.close() }"),
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
assert "WindowInsets.Type.statusBars() or WindowInsets.Type.displayCutout()" in main
assert "root.setOnApplyWindowInsetsListener" in main
assert "root.setPadding(left, top, right, 0)" in main
assert "applyBrowserSafeArea()" in main
assert "root.requestApplyInsets()" in main
assert "maxOf(insets.systemWindowInsetTop, cutout?.safeInsetTop ?: 0)" in main
assert "if (videoFullScreen) 0 else safeTopInsetPx" in main
assert ".windowInsetsPadding(" not in tabs, "T06 would apply status inset twice"
assert ".navigationBarsPadding()" in tabs, "Tab footer can overlap gesture navigation"
assert "Column(Modifier.fillMaxSize().background(background).padding(top = 24.dp))" not in tabs

assert (ROOT / "design/ekranlar/T06-Sekmeler.png").exists(), "T06 approved asset missing"
assert (ROOT / "design/ekranlar/T09-Gizli.png").exists(), "T09 approved asset missing"
print("T06/T09 static regression checks passed; real-device validation remains required.")
