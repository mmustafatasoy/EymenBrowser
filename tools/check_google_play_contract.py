#!/usr/bin/env python3
"""Guardrail for the Play AAB build path; does not assert Play approval."""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
gradle = (ROOT / "android/app-telefon/build.gradle.kts").read_text()
updater = (ROOT / "android/app-telefon/src/main/java/com/eymen/browser/telefon/BrowserUpdateManager.kt").read_text()
manifest = (ROOT / "android/app-telefon/src/main/AndroidManifest.xml").read_text()
prep = (ROOT / "tools/prepare_google_play_manifest.py").read_text()
script = (ROOT / "tools/build_google_play_bundle.sh").read_text()
assert 'applicationId = "com.eymen.browser.telefon"' in gradle
assert 'val googlePlayBuild = providers.gradleProperty("eymenGooglePlay").orNull == "true"' in gradle
assert 'targetSdk = if (googlePlayBuild) 36 else 35' in gradle
assert 'releaseVersion == "1.0.0" && releaseCode == 1000000' in gradle
assert '"EYMEN_GOOGLE_PLAY"' in gradle
assert 'BuildConfig.EYMEN_GOOGLE_PLAY' in updater
assert 'R.string.update_managed_by_google_play' in updater
assert "android.permission.REQUEST_INSTALL_PACKAGES" in manifest
assert "content.count(TARGET) != 1" in prep
assert "MANIFEST.write_text(content.replace(TARGET, \"\", 1)" in prep
assert "trap restore_manifest EXIT" in script
assert "bundleRelease" in script and "-PeymenGooglePlay=true" in script
assert "jarsigner -verify -strict" in script
assert "EY_SIGNING_KEYSTORE_FILE" in script and "sha256sum" in script
for lang in ("tr", "en"):
    strings=json.loads((ROOT / "shared/i18n" / f"{lang}.json").read_text())
    assert strings.get("update_managed_by_google_play")
print("Google Play preparation contract passes; no signed AAB or Play release implied.")
