#!/usr/bin/env bash
set -euo pipefail
mkdir -p browser-ui-audit
trap 'adb logcat -d -v threadtime > browser-ui-audit/logcat.txt || true' EXIT
adb install -r android/app-telefon/build/outputs/apk/debug/app-telefon-x86_64-debug.apk
adb install -r android/app-telefon/build/outputs/apk/androidTest/debug/app-telefon-debug-androidTest.apk
adb shell cmd locale set-app-locales com.eymen.browser.telefon --locales tr-TR
set +e
timeout 180 adb shell am instrument -w -r \
  com.eymen.browser.telefon.test/com.eymen.browser.telefon.BrowserUiInstrumentation \
  | tee browser-ui-audit/instrumentation.txt
TEST_EXIT=${PIPESTATUS[0]}
set -e
for NAME in settings-light.png appearance-night.png downloads-light.png; do
  adb exec-out run-as com.eymen.browser.telefon cat "cache/browser-ui-audit/$NAME" \
    > "browser-ui-audit/$NAME" || true
done
if [ "$TEST_EXIT" -ne 0 ]; then exit "$TEST_EXIT"; fi
python3 - <<'PY'
from pathlib import Path
text = Path('browser-ui-audit/instrumentation.txt').read_text()
assert 'INSTRUMENTATION_RESULT: eymen_browser_ui_audit=passed' in text, text
assert 'INSTRUMENTATION_CODE: -1' in text, text
for name in ('settings-light.png', 'appearance-night.png', 'downloads-light.png'):
    assert Path('browser-ui-audit',name).stat().st_size > 1000, name
print('Real touch/type and custom settings/download screens passed.')
PY
