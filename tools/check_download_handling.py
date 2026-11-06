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
coordinator = (ROOT / "android/app-telefon/src/main/java/com/eymen/browser/telefon/DownloadCoordinator.kt").read_text()
copier = (ROOT / "android/app-telefon/src/main/java/com/eymen/browser/telefon/DownloadStreamCopy.kt").read_text()
service = (ROOT / "android/app-telefon/src/main/java/com/eymen/browser/telefon/EymenDownloadService.kt").read_text()
manifest = (ROOT / "android/app-telefon/src/main/AndroidManifest.xml").read_text()
assert "DownloadStreamCopy.copy(source, output, request.cancelled::get)" in coordinator
assert "source.read(buffer)" in copier and "output.write(buffer, 0, count)" in copier
assert "written += count" in copier
assert 'header(response, "content-length")' in handler
assert 'DownloadProgressMath.percent(' in (ROOT / "android/app-telefon/src/main/java/com/eymen/browser/telefon/DownloadsScreen.kt").read_text()
assert 'running.value.cancelled.compareAndSet(false, true)' in coordinator
assert 'DocumentsContract.deleteDocument(context.contentResolver, uri)' in coordinator
assert 'cleanup.execute { running.value.body.closeSafely() }' in coordinator
assert 'onProgress: (ActiveDownload?) -> Unit' in handler
assert 'onCompleted: (SavedDownload) -> Unit' in handler
assert 'onCompleted(candidate)' in coordinator
assert 'active = activeDownload' in phone
assert 'onCancelActive = downloads::cancelActive' in phone
assert 'onCancelQueued = downloads::cancelQueued' in phone
assert 'savedFiles.add(0, file)' in phone
assert 'downloads.pauseObserving()' in phone
assert 'DownloadCoordinator(context.applicationContext)' in coordinator
assert 'private val activity' not in coordinator
assert 'startForegroundService(' in coordinator
assert 'FOREGROUND_SERVICE_DATA_SYNC' in manifest
assert 'android:foregroundServiceType="dataSync"' in manifest
assert 'android:stopWithTask="false"' in manifest
assert 'android:exported="false"' in manifest
assert 'FOREGROUND_SERVICE_TYPE_DATA_SYNC' in service
assert 'override fun onTimeout(startId: Int, fgsType: Int)' in service
assert 'START_NOT_STICKY' in service
assert 'stopSelfResult(lastStartId)' in service
assert 'FLAG_IMMUTABLE' in service
assert 'VISIBILITY_PRIVATE' in service
assert 'worker.shutdownNow()' not in handler
jvm_tests = (ROOT / "android/app-telefon/src/test/java/com/eymen/browser/telefon/DownloadProgressMathTest.kt").read_text()
assert 'unknownSizeNeverClaimsPercentage' in jvm_tests
assert 'neverDisplaysMoreThanOneHundredPercent' in jvm_tests
assert 'avoidsOverflowForLargeFiles' in jvm_tests
assert "MediaStore.Downloads.getContentUri" in coordinator
assert "MediaStore.MediaColumns.IS_PENDING, 1" in coordinator
assert "MediaStore.MediaColumns.IS_PENDING, 0" in coordinator
assert "resolver.delete(it" in coordinator
assert "Intent.ACTION_CREATE_DOCUMENT" in handler
assert "VERSION_CODES.Q" in handler
assert "DownloadManager" not in coordinator
assert "HttpURLConnection" not in handler + coordinator
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
print("Android download source contracts passed: original Gecko stream, bounded queue, foreground service, cancellation, SAF and localization. Device behavior is tested separately.")
