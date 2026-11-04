#!/usr/bin/env python3
"""Static regression guard: Stage-1 rotation must preserve GeckoView session."""
from pathlib import Path
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
AND_NS = "{http://schemas.android.com/apk/res/android}"
required = set("orientation|screenSize|smallestScreenSize|screenLayout|keyboardHidden".split("|"))

engine = (ROOT / "android/core-motor/src/main/java/com/eymen/browser/motor/GeckoMotorDenemesi.kt").read_text(encoding="utf-8")
assert "override fun onFullScreen" in engine
assert "fun onConfigurationChanged(config: Configuration)" in engine

for module, package in (("app-telefon","telefon"), ("app-tv","tv")):
    manifest = ET.parse(ROOT / f"android/{module}/src/main/AndroidManifest.xml")
    activity = manifest.getroot().find("./application/activity")
    assert activity is not None
    actual = set((activity.attrib.get(AND_NS+"configChanges") or "").split("|"))
    assert required <= actual, f"{module}: rotation will recreate Activity"
    source = (ROOT / f"android/{module}/src/main/java/com/eymen/browser/{package}/MainActivity.kt").read_text(encoding="utf-8")
    assert "override fun onConfigurationChanged" in source
    assert "onFullScreen = " in source
    assert "GeckoMotorDenemesi.onConfigurationChanged(newConfig)" in source

print("Aşama 1: tüm Android motor Activity yapılandırmaları dönüşte oturumu korumaya hazır.")
