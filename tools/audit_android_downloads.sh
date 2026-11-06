#!/usr/bin/env bash
set -euo pipefail
mkdir -p download-audit
trap 'adb logcat -d -v threadtime > download-audit/logcat.txt || true' EXIT
TARGET=android/app-telefon/build/outputs/apk/debug/app-telefon-x86_64-debug.apk
TEST=android/app-telefon/build/outputs/apk/androidTest/debug/app-telefon-debug-androidTest.apk
test -s "$TARGET"
test -s "$TEST"
adb install -r "$TARGET"
adb install -r "$TEST"
# A denied notification permission must not prevent an approved dataSync FGS.
adb shell pm revoke com.eymen.browser.telefon android.permission.POST_NOTIFICATIONS || true
timeout 150 adb shell am instrument -w -r \
    com.eymen.browser.telefon.test/com.eymen.browser.telefon.DownloadServiceInstrumentation \
    | tee download-audit/instrumentation.txt
python3 - <<'PY'
from pathlib import Path
text = Path('download-audit/instrumentation.txt').read_text()
assert 'INSTRUMENTATION_RESULT: eymen_download_audit=passed' in text, text
assert 'INSTRUMENTATION_CODE: -1' in text, text
assert 'background_FIFO,SHA256,idle_stop' in text, text
print('Real API35 background download service acceptance passed.')
PY
