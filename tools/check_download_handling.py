#!/usr/bin/env python3
"""Android phone Stage 1 download-stream regression checks (static only)."""
from pathlib import Path
import json

ROOT = Path(__file__).resolve().parents[1]
engine = (ROOT / "android/core-motor/src/main/java/com/eymen/browser/motor/GeckoMotorDenemesi.kt").read_text()
phone = (ROOT / "android/app-telefon/src/main/java/com/eymen/browser/telefon/MainActivity.kt").read_text()
handler = (ROOT / "android/app-telefon/src/main/java/com/eymen/browser/telefon/AndroidDownloadDenemesi.kt").read_text()

assert "override fun onExternalResponse" in engine
assert "onDownload(response)" in engine
assert "onDownload = downloads::handleResponse" in phone
assert "response.body" in handler
assert "source.copyTo(output" in handler
assert "MediaStore.Downloads.getContentUri" in handler
assert "MediaStore.MediaColumns.IS_PENDING, 1" in handler
assert "MediaStore.MediaColumns.IS_PENDING, 0" in handler
assert "resolver.delete(createdDownload" in handler
assert "Intent.ACTION_CREATE_DOCUMENT" in handler
assert "VERSION_CODES.Q" in handler
assert "DownloadManager" not in handler.split("internal class")[1]
assert "HttpURLConnection" not in handler
assert "URLDecoder.decode" in handler
assert ".setPositiveButton(R.string.download_confirm_allow)" in handler
assert ".setNegativeButton(R.string.download_confirm_deny)" in handler

tr = json.loads((ROOT / "shared/i18n/tr.json").read_text())
en = json.loads((ROOT / "shared/i18n/en.json").read_text())
for key in ("download_confirm_title","download_confirm_message","download_confirm_allow",
            "download_confirm_deny","download_saved","download_failed","download_cancelled",
            "download_busy","download_missing"):
    assert tr[key] and en[key], key
assert "%1$s" in tr["download_confirm_message"] and "%1$s" in en["download_confirm_message"]
assert tr.keys() == en.keys()
print("Android indirme akışı: GeckoView body, MediaStore, SAF, kullanıcı onayı ve yerelleştirme denetimi geçti.")
