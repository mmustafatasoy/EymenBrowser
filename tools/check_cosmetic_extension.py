#!/usr/bin/env python3
"""Regression guard for the opt-in built-in GeckoView cosmetic WebExtension.

Checks metadata and boundaries only. It does NOT pretend visual ad removal,
third-party network blocking, extension installation or real VPN succeeded.
"""
import json
from pathlib import Path

root = Path(__file__).resolve().parents[1]
assets = root / "android/app-telefon/src/main/assets/eymen-cosmetic"
manifest = json.loads((assets / "manifest.json").read_text(encoding="utf-8"))
js = (assets / "cosmetic.js").read_text(encoding="utf-8")
motor = (root / "android/core-motor/src/main/java/com/eymen/browser/motor/GeckoMotorDenemesi.kt").read_text()
main = (root / "android/app-telefon/src/main/java/com/eymen/browser/telefon/MainActivity.kt").read_text()
shield = (root / "android/app-telefon/src/main/java/com/eymen/browser/telefon/ShieldPanel.kt").read_text()

addon_id = "eymen-cosmetic@atasoyyazilim.com"
assert manifest["manifest_version"] == 2
assert manifest["browser_specific_settings"]["gecko"]["id"] == addon_id
assert manifest["content_scripts"] == [{
    "matches": ["https://*/*"],
    "js": ["cosmetic.js"],
    "run_at": "document_start",
    "all_frames": False,
}]
assert set(manifest["permissions"]) == {
    "nativeMessaging", "nativeMessagingFromContent", "geckoViewAddons"
}
assert "browser.runtime.sendNativeMessage(" in js
assert "cosmetic-policy-v1" in js and '"eymen_cosmetic"' in js
assert "reply.enabled === true" in js
assert "location.protocol !== \"https:\"" in js
assert "window.top !== window" in js
assert "ins.adsbygoogle" in js
assert "data-ad-client" in js
assert "display:none!important" in js
assert "DOMContentLoaded" in js
assert "fetch(" not in js
assert "webRequest" not in js
assert "browser.storage" not in js
assert "XMLHttpRequest" not in js
assert "eval(" not in js
assert "<all_urls>" not in (assets / "manifest.json").read_text()
assert "resource://android/assets/eymen-cosmetic/" in motor
assert addon_id in motor
assert "ensureBuiltIn(" in motor
assert "onReady(null)" in motor

assert 'prefs.getBoolean("cosmetic_filter_wanted", false)' in main
assert 'private val cosmeticFilterReady = mutableStateOf(false)' in main
assert 'if (!cosmeticFilterReady.value) return' in main
assert 'sender.environmentType ==' in main
assert 'WebExtension.MessageSender.ENV_TYPE_CONTENT_SCRIPT' in main
assert 'sender.isTopLevel' in main
assert 'sender.session === tab.gecko' in main
assert 'verifiedHost(sender.url) != null' in main
assert 'protectionFor(sender.url, tab.privateMode)' in main
assert 'allowed && cosmeticFilterWanted.value' in main
assert 'tabs.any { it.id == tab.id && it.gecko === tab.gecko }' in main
assert 'onCosmeticToggle = ::setCosmeticFilterEnabled' in main
assert 'cosmeticExtension?.let { installCosmeticPolicy(tab, it) }' in main
assert 'tabs.forEach { installCosmeticPolicy(it, installed) }' in main
assert 'onCosmeticToggle(!cosmeticEnabled)' in shield
assert '.clickable(enabled = cosmeticAvailable)' in shield
assert 'shield_cosmetic_scope' in shield

for lang in ("tr", "en"):
    locale = json.loads((root / f"shared/i18n/{lang}.json").read_text())
    for key in ("shield_cosmetic_title", "shield_cosmetic_beta",
                "shield_cosmetic_unavailable", "shield_cosmetic_scope"):
        assert locale[key] and isinstance(locale[key], str)
print("Cosmetic module packaging/policy/privacy static checks passed.")
