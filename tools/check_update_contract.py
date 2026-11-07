#!/usr/bin/env python3
"""Static no-false-claim checks for the EymenBrowser signed update channel.

Device installation and release signing are validated by separate gated CI.
"""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
app = (ROOT / "android/app-telefon")
updater = (app / "src/main/java/com/eymen/browser/telefon/BrowserUpdateManager.kt").read_text()
main = (app / "src/main/java/com/eymen/browser/telefon/MainActivity.kt").read_text()
settings = (app / "src/main/java/com/eymen/browser/telefon/SettingsScreen.kt").read_text()
gradle = (app / "build.gradle.kts").read_text()
manifest = (app / "src/main/AndroidManifest.xml").read_text()
paths = (app / "src/main/res/xml/browser_update_file_paths.xml").read_text()
release = (ROOT / ".github/workflows/android-stable-release.yml").read_text()

# Only a public, stable GitHub release is eligible. Never reinstall prereleases,
# debug artifacts, wrong processor architecture, or an older version code.
assert "releases/latest" in updater
assert 'payload.optBoolean("prerelease", true)' in updater
assert 'payload.optBoolean("draft", true)' in updater
assert 'if (expectedCode <= installedVersionCode()) return null' in updater
assert "for (abi in Build.SUPPORTED_ABIS)" in updater
assert '"EymenBrowser-Android-Phone-$abi.apk"' in updater
assert '"sha256:[a-fA-F0-9]{64}"' in updater
assert 'read > candidate.bytes || read > MAX_APK_BYTES' in updater
assert 'digest.digest().toHex()' in updater
assert 'verifyPackageAndSigner(partial, candidate.versionCode)' in updater
assert 'apk.packageName != activity.packageName' in updater
assert 'a.isEmpty() || a != b' in updater
assert 'MAX_APK_BYTES = 500L * 1024 * 1024' in updater
assert 'CHECK_INTERVAL_MS = 6L * 60L * 60L * 1000L' in updater
assert 'private val checking = AtomicBoolean(false)' in updater
assert 'checking.compareAndSet(false, true)' in updater
assert 'checking.set(false)' in updater
assert 'ApplicationInfo.FLAG_DEBUGGABLE' in updater
assert 'update_debug_build' in updater

# Android's normal APK installer always asks for user action and authority.
assert "android.permission.REQUEST_INSTALL_PACKAGES" in manifest
assert 'android:name="androidx.core.content.FileProvider"' in manifest
assert 'android:exported="false"' in manifest
assert 'android:grantUriPermissions="true"' in manifest
assert '<cache-path name="updates" path="updates/" />' in paths
assert "FileProvider.getUriForFile(" in updater
assert "Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES" in updater
assert "Intent.ACTION_VIEW" in updater
assert "FLAG_GRANT_READ_URI_PERMISSION" in updater
assert "PackageInstaller.Session" not in updater
assert "ACTION_INSTALL_PACKAGE" not in updater
assert 'SettingsAction.ABOUT -> updater.check(manual = true)' in main
assert 'updater.onResumed()' in main
assert "updater.dispose()" in main
assert 'SettingsActionRow(stringResource(R.string.update_manual_check)' in settings

# Signing is isolated in protected Actions secrets and pinned to a public digest.
assert 'signingConfigs {' in gradle
assert 'if (canSign)' in gradle
assert 'isDebuggable = false' in gradle
assert 'EYMEN_ANDROID_KEYSTORE_BASE64' in release
assert 'EYMEN_ANDROID_STORE_PASSWORD' in release
assert 'EYMEN_ANDROID_KEY_ALIAS' in release
assert 'EYMEN_ANDROID_KEY_PASSWORD' in release
assert 'docs/ANDROID_RELEASE_CERT_SHA256.txt' in release
assert 'apksigner" verify --verbose --print-certs' in release
assert 'gh release create "$RELEASE_TAG"' in release
# Emulator visual verification lives in a separate workflow so transient SDK
# download/bootstrap failures cannot block the signed release indefinitely.
visual_audit = (ROOT / ".github/workflows/android-visual-audit.yml").read_text()
assert 'script: bash tools/capture_android_visual.sh' in visual_audit
assert 'compare signed update compatibility' in release
assert 'PREVIOUS_CERT' in release
assert 'OLD_PACKAGE' in release and 'NEW_PACKAGE' in release
assert 'NEW_CODE' in release and 'OLD_CODE' in release
assert 'e ym en br ow ser' not in release
assert 'versionName = "0.0.1"' in gradle  # Default version is still locked.

fingerprint = (ROOT / "docs/ANDROID_RELEASE_CERT_SHA256.txt").read_text().strip()
assert len(fingerprint) == 64 and all(x in '0123456789ABCDEF' for x in fingerprint)

for lang in ("tr", "en"):
    tr = json.loads((ROOT / f"shared/i18n/{lang}.json").read_text())
    for text in (
        "update_checking", "update_no_release", "update_available_title",
        "update_available_message", "update_failed",
        "update_install_permission_title", "update_install_permission_message",
        "update_download_install", "update_manual_check",
    ):
        assert tr.get(text), f"Missing {lang} string: {text}"

for part in ROOT.rglob("*"):
    if part.is_file() and part.suffix.lower() in {".jks", ".p12", ".pfx", ".keystore"}:
        if "build" not in part.parts:
            raise AssertionError(f"Private signing material must not be committed: {part}")

print("Signed Android update/release static contract passed; no production release is implied.")
