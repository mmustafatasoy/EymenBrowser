#!/usr/bin/env python3
"""Aşama 1: DRM consent prompt regression guard; not a runtime DRM test."""
from pathlib import Path
import json
ROOT = Path(__file__).resolve().parents[1]
source = (ROOT / "android/core-motor/src/main/java/com/eymen/browser/motor/GeckoMotorDenemesi.kt").read_text()
assert "PERMISSION_MEDIA_KEY_SYSTEM_ACCESS" in source
assert "askDrmPermission(origin)" in source
assert "perm.notifyShown()" in source
assert "return null" in source  # unhandled permissions use GeckoView default
assert "VALUE_DENY" in source
for app, module in (("telefon","app-telefon"), ("tv","app-tv")):
    activity = (ROOT / f"android/{module}/src/main/java/com/eymen/browser/{app}/MainActivity.kt").read_text()
    assert "askDrmPermission = ::showDrmAccessPrompt" in activity
    assert ".setNegativeButton(" in activity
    assert ".setPositiveButton(" in activity
    assert ".show()" in activity
for locale in ("tr", "en"):
    texts = json.loads((ROOT / f"shared/i18n/{locale}.json").read_text())
    for key in ("drm_permission_title","drm_permission_message","drm_permission_allow","drm_permission_deny"):
        assert texts[key]
    assert "%1$s" in texts["drm_permission_message"]
print("DRM site-key permission requires explicit localized user choice.")
