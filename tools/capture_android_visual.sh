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

APK="${EY_APK_PATH:-$(find android/app-telefon/build/outputs/apk/debug -name '*x86_64*.apk' -print -quit)}"
test -n "$APK" && test -f "$APK"
adb shell wm size 390x844
adb shell wm density 160
# For stable release QA, install the previously published production APK
# first, then upgrade to the candidate signed APK. This tests Android's
# actual package-signature/version acceptance rather than only APK metadata.
if [[ -n "${EY_PREVIOUS_APK_PATH:-}" ]]; then
  test -f "$EY_PREVIOUS_APK_PATH"
  echo "Checking signed upgrade from previous stable APK"
  adb install -r "$EY_PREVIOUS_APK_PATH"
  adb shell dumpsys package "$PKG" | grep -m 1 'versionCode=' || true
fi
adb install -r "$APK"
if [[ -n "${EY_PREVIOUS_APK_PATH:-}" ]]; then
  EXPECTED_CODE="${RELEASE_CODE:?Signed release version code required for upgrade test}"
  adb shell dumpsys package "$PKG" | grep -m 1 "versionCode=$EXPECTED_CODE" || {
    echo "Stable in-place APK update did not install versionCode $EXPECTED_CODE" >&2
    exit 20
  }
fi
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

# Android can layer a system ANR dialog OVER the foreground app. Such a
# capture is not comparable to the approved dark T02 design.
python - <<'PY'
from PIL import Image, ImageChops
from pathlib import Path
reference = Path("design/ekranlar/T02-Ilk-kurulum.png")
capture = Path("visual-captures/T02-Ilk-kurulum.png")
with Image.open(reference) as image:
    expected = image.convert("RGB")
with Image.open(capture) as image:
    observed = image.convert("RGB").resize(expected.size, Image.Resampling.LANCZOS)
diff = ImageChops.difference(expected, observed)
fraction = sum(max(pixel) >= 25 for pixel in diff.getdata()) / (expected.width * expected.height)
print(f"T02 screen identity error fraction: {fraction:.3f}")
if fraction > 0.35:
    raise SystemExit("T02 capture does not resemble the approved page; possibly a system dialog")
PY

# The approved T02 button sits near the bottom of a 390x844 viewport.
adb shell input tap 190 734
sleep 4
check_browser_foreground
adb exec-out screencap -p > visual-captures/T03-Yeni-sekme.png
test -s visual-captures/T03-Yeni-sekme.png

# A white Gecko about:blank viewport is NOT the approved dark T03 homepage.
# Do not produce misleading pixel metrics if internal Gecko navigation hid it.
python - <<'PY'
from PIL import Image
from pathlib import Path

path = Path("visual-captures/T03-Yeni-sekme.png")
with Image.open(path) as image:
    sample = image.convert("RGB").crop((0, 55, image.width, image.height - 135))
    sample.thumbnail((90, 130))
    pixels = list(sample.getdata())
bright_fraction = sum(
    1 for red, green, blue in pixels if min(red, green, blue) >= 235
) / len(pixels)
print(f"T03 bright-background fraction: {bright_fraction:.3f}")
if bright_fraction > 0.35:
    raise SystemExit("T03 is predominantly white; refused to compare blank Gecko content as homepage")
with Image.open("design/ekranlar/T03-Yeni-sekme.png") as image:
    expected = image.convert("RGB")
with Image.open(path) as image:
    observed = image.convert("RGB").resize(expected.size, Image.Resampling.LANCZOS)
from PIL import ImageChops
diff = ImageChops.difference(expected, observed)
fraction = sum(max(pixel) >= 25 for pixel in diff.getdata()) / (expected.width * expected.height)
print(f"T03 screen identity error fraction: {fraction:.3f}")
if fraction > 0.20:
    raise SystemExit("T03 capture resembles a different page (e.g. onboarding); refused wrong-page metric")
PY

# T06: reproduce the real device path to the tab overview (4th toolbar
# button on the 390x844 reference emulator). Never audit just T02/T03 while
# an edge-to-edge regression makes the tabs cover the status bar.
adb shell input tap 268 754
sleep 3
check_browser_foreground
adb exec-out screencap -p > visual-captures/T06-Sekmeler.png
test -s visual-captures/T06-Sekmeler.png

python - <<'PY'
from PIL import Image
with Image.open("visual-captures/T06-Sekmeler.png") as raw:
    image = raw.convert("RGB")
    assert image.size == (390, 844), f"Unexpected emulator size: {image.size}"
    # The tab overview has a 48dp segmented header at x=160. Sample below
    # the status bar and assert the header actually appeared, not T03.
    background = image.getpixel((160, 270))
    first_header = None
    for y in range(5, 106):
        pixel = image.getpixel((160, y))
        if max(abs(a - b) for a, b in zip(pixel, background)) >= 8:
            first_header = y
            break
    print(f"T06 first segmented-header pixel: y={first_header}")
    if first_header is None:
        raise SystemExit("T06 tabs overview did not open; refusing misleading safe-area metrics")
    # 160 dpi emulator: the status bar occupies at least ~24px. A fixed
    # 24dp top inset collides with edge-to-edge status icons; the actual
    # safe area + interior spacing must keep the segment below 32px.
    if first_header < 32:
        raise SystemExit(f"T06 tabs header overlaps the OS status bar (y={first_header})")
PY

echo "Browser foreground, Turkish locale, T03 home and T06 device-safe tabs validated."
