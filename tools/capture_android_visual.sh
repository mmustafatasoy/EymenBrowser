#!/usr/bin/env bash
set -euo pipefail

# Each command inside android-emulator-runner's multi-line script may run in
# its own shell; keep state, traps and errors inside ONE actual script process.
PKG="com.eymen.browser.telefon"
mkdir -p visual-captures visual-report
on_exit() {
  status=$?
  if [ "$status" -ne 0 ]; then
    adb logcat -d -s AndroidRuntime:E GeckoRuntime:E ActivityTaskManager:W > visual-captures/emulator-crash-log.txt 2>&1 || true
    adb shell dumpsys activity activities > visual-captures/foreground-failure.txt 2>&1 || true
    adb exec-out screencap -p > visual-captures/failure.png 2>/dev/null || true
  fi
}
trap on_exit EXIT

APK="$(find android/app-telefon/build/outputs/apk/debug -name '*x86_64*.apk' -print -quit)"
test -n "$APK" && test -f "$APK"
adb shell wm size 390x844
adb shell wm density 160
adb install -r "$APK"
adb shell pm clear "$PKG"
# The approved reference PNGs are Turkish; force the app locale on Android 35.
# This avoids comparing English UI text against Turkish artwork.
adb shell cmd locale set-app-locales "$PKG" --locales tr-TR
adb shell cmd locale get-app-locales "$PKG" | grep -q 'tr-TR' || {
  echo "Failed to switch the app locale to tr-TR; cannot compare references." >&2
  exit 19
}
adb logcat -c
adb shell am start -W -n "$PKG/.MainActivity"
sleep 7

check_browser_foreground() {
  # An installed APK and a successful 'am start' exit do NOT prove the app
  # launched: Android can report start success even when the activity crashes.
  adb shell pidof "$PKG"
  if ! adb shell dumpsys activity activities | grep -E 'topResumedActivity|mResumedActivity' | grep -q "$PKG"; then
    echo "EYMEN browser is not the resumed Android Activity; REFUSE visual comparison" >&2
    return 17
  fi
}
check_browser_foreground
adb exec-out screencap -p > visual-captures/T02-Ilk-kurulum.png
test -s visual-captures/T02-Ilk-kurulum.png

# The approved T02 button sits near the bottom of a 390x844 viewport.
adb shell input tap 190 734
sleep 4
check_browser_foreground
adb exec-out screencap -p > visual-captures/T03-Yeni-sekme.png
test -s visual-captures/T03-Yeni-sekme.png

echo "Browser process and resumed foreground activity checked for both screenshots."
