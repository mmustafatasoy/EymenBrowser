#!/usr/bin/env bash
set -euo pipefail
mkdir -p browser-ui-audit
trap 'adb logcat -d -v threadtime > browser-ui-audit/logcat.txt || true' EXIT
adb shell am force-stop com.android.launcher3 || true
adb install -r android/app-telefon/build/outputs/apk/debug/app-telefon-x86_64-debug.apk
adb install -r android/app-telefon/build/outputs/apk/androidTest/debug/app-telefon-debug-androidTest.apk
adb shell pm list instrumentation > browser-ui-audit/registered-runners.txt
python3 - <<'PY'
from pathlib import Path
assert 'com.eymen.browser.telefon.test/com.eymen.browser.telefon.BrowserUiInstrumentation' in Path('browser-ui-audit/registered-runners.txt').read_text()
PY
adb shell cmd locale set-app-locales com.eymen.browser.telefon --locales tr-TR
set +e
timeout 480 adb shell am instrument -w -r \
  com.eymen.browser.telefon.test/com.eymen.browser.telefon.BrowserUiInstrumentation \
  | tee browser-ui-audit/instrumentation.txt
TEST_EXIT=${PIPESTATUS[0]}
set -e
for NAME in address-search-ime.png navigation-restored.png settings-light.png appearance-night.png downloads-light.png privacy-settings.png extensions-installed.png child-mode.png language-english.png site-permissions.png history-light.png history-dark.png menu-native.png settings-native-dark.png default-browser-system.png scroll-hidden.png scroll-visible.png scroll-bottom.png failure.png hierarchy.txt chrome-diagnostics.txt; do
  adb exec-out run-as com.eymen.browser.telefon cat "cache/browser-ui-audit/$NAME" \
    > "browser-ui-audit/$NAME" || true
done
if [ "$TEST_EXIT" -ne 0 ]; then exit "$TEST_EXIT"; fi
python3 - <<'PY'
from pathlib import Path
text = Path('browser-ui-audit/instrumentation.txt').read_text()
assert 'INSTRUMENTATION_RESULT: eymen_browser_ui_audit=passed' in text, text
assert 'INSTRUMENTATION_CODE: -1' in text, text
for name in ('address-search-ime.png', 'navigation-restored.png', 'settings-light.png', 'appearance-night.png', 'downloads-light.png', 'history-light.png', 'history-dark.png', 'menu-native.png', 'scroll-hidden.png', 'scroll-visible.png', 'scroll-bottom.png', 'default-browser-system.png'):
    assert Path('browser-ui-audit',name).stat().st_size > 1000, name
print('Real touch/type and custom settings/download screens passed.')
PY
