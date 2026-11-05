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
# Verify the ENTIRE FrameLayout uses the device-reported insets. A T06-only
# Compose padding could pass a header screenshot while GeckoView, Home,
# Settings, and other browser screens still overlap the camera/status bar.
adb logcat -d -s EymenSafeArea:I > visual-captures/actual-safe-insets.txt || true
python - <<'PY'
from pathlib import Path
import re
text = Path("visual-captures/actual-safe-insets.txt").read_text()
values = [
    tuple(map(int, match))
    for match in re.findall(
        r"EymenSafeArea:\s+top=(\d+) left=(\d+) right=(\d+) bottom=(\d+) fullscreen=false",
        text,
    )
]
print(f"Activity root safe-area measurements (top,left,right,bottom): {values}")
if not values or max(item[0] for item in values) <= 0 or max(item[3] for item in values) <= 0:
    raise SystemExit(
        "No positive top camera/status or bottom navigation inset on the app root; "
        "refusing to treat a T06-only header as a whole-app fix"
    )
PY
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

# Different Android system navigation modes move the primary start
# button up/down. Hard-coding y=734 can hit "Make default browser" instead,
# launching Android Settings and incorrectly reporting an app crash.
# Locate the REAL bright-blue "Start" pill in the T02 screenshot.
python - <<'PY'
from PIL import Image
from pathlib import Path
image = Image.open("visual-captures/T02-Ilk-kurulum.png").convert("RGB")
x = image.width // 2
blue_rows = []
for y in range(image.height // 2, image.height - 35):
    red, green, blue = image.getpixel((x, y))
    if 50 <= red <= 115 and 130 <= green <= 195 and blue >= 220:
        blue_rows.append(y)
runs = []
for y in blue_rows:
    if not runs or y > runs[-1][-1] + 1:
        runs.append([y])
    else:
        runs[-1].append(y)
primary = max(runs, key=len, default=[])
if len(primary) < 15:
    raise SystemExit("Primary onboarding Start button is clipped by system navigation")
tap_y = primary[len(primary) // 2]
Path("visual-captures/onboarding-start-tap.txt").write_text(str(tap_y))
print(f"T02 primary Start button visible at y={primary[0]}..{primary[-1]}; tapping y={tap_y}")
PY
adb shell input tap 195 "$(cat visual-captures/onboarding-start-tap.txt)"
sleep 4
check_browser_foreground
adb exec-out screencap -p > visual-captures/T03-Yeni-sekme.png
test -s visual-captures/T03-Yeni-sekme.png

# New approved T03 is LIGHT, branded with the actual blue Atasoy A
# logo. Never compare it to the obsolete DARK T03 PNG in design/ekranlar:
# such pixel metrics would be false evidence for this user-approved redesign.
python - <<'PY'
from PIL import Image
from pathlib import Path
with Image.open("visual-captures/T03-Yeni-sekme.png") as raw:
    image = raw.convert("RGB")
    if image.size != (390, 844):
        raise SystemExit("T03 unexpected emulator screen size")
    # Count bright points around the ACTUAL content area, not system bars.
    sample = image.crop((20, 105, 370, 680))
    sample.thumbnail((80, 120))
    pixels = list(sample.getdata())
    light = sum(min(rgb) >= 210 for rgb in pixels) / len(pixels)
    # Atasoy website A mark is blue and must appear near the center of the
    # actual homepage, otherwise this may be a blank Gecko white page.
    logo = image.crop((110, 130, 280, 440))
    blues = sum(
        r < 115 and 50 < g < 180 and b > 185
        for r, g, b in logo.getdata()
    )
    print(f"New light T03: light fraction={light:.3f}, official-blue logo pixels={blues}")
    if light < 0.45 or blues < 130:
        raise SystemExit("T03 is not the approved LIGHT home with the real blue A mark")
PY

# T07: the approved compact three-dot menu must really open at the top
# right and contain working Back/Forward actions (not a decorative image).
adb shell input tap 365 58
sleep 2
check_browser_foreground
adb exec-out screencap -p > visual-captures/T07-Acik-Menu.png
python - <<'PY'
from PIL import Image, ImageChops
with Image.open("visual-captures/T03-Yeni-sekme.png") as original:
    base = original.convert("RGB").crop((125, 90, 388, 585))
with Image.open("visual-captures/T07-Acik-Menu.png") as current:
    menu = current.convert("RGB").crop((125, 90, 388, 585))
diff = ImageChops.difference(base, menu)
fraction = sum(max(p) >= 24 for p in diff.getdata()) / (diff.width * diff.height)
print(f"Actual T07 top-right popup screen delta: {fraction:.3f}")
if fraction < 0.23:
    raise SystemExit("Three-dot popup did not open as a top-right menu")
PY
adb shell input keyevent 4
sleep 1
check_browser_foreground

# T06: real LIGHT TOP toolbar tab-count control: x≈324, y≈58 in
# 390x844/160dpi AOSP with a 24px status bar. The old bottom-toolbar
# click (268,754) would now click homepage content instead.
adb shell input tap 324 58
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
    # x=380 is outside the tab card grid and header, so it remains
    # the actual root background even when a real tab preview is present.
    background = image.getpixel((380, 270))
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
