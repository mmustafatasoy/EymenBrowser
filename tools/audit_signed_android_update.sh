#!/usr/bin/env bash
set -euo pipefail
AUDIT_DIR=signed-update-audit
DRIVER=com.eymen.browser.updateaudit
BROWSER=com.eymen.browser.telefon
mkdir -p "$AUDIT_DIR"
trap 'timeout 20 adb logcat -d -v threadtime > "$AUDIT_DIR/logcat.txt" || true' EXIT

# This script must never operate on a physical device or an existing user app.
test "$(adb shell getprop ro.kernel.qemu | tr -d '\r\n')" = 1
test "$(adb shell getprop ro.product.cpu.abi | tr -d '\r\n')" = x86_64
if adb shell pm path "$BROWSER" | grep -q '^package:'; then
  echo 'Refusing to overwrite an existing browser installation on this emulator'
  exit 1
fi
SOURCE_URL=$(python3 -c 'import json; print(json.load(open("signed-update-audit/manifest.json"))["source"]["url"])')
curl --fail --silent --show-error --location --proto '=https' --proto-redir '=https' \
  --max-time 180 "$SOURCE_URL" --output "$AUDIT_DIR/source.apk"
python3 - <<'PY'
import hashlib,json
from pathlib import Path
data=json.loads(Path('signed-update-audit/manifest.json').read_text())['source']
p=Path('signed-update-audit/source.apk')
assert p.stat().st_size==data['bytes']
assert hashlib.sha256(p.read_bytes()).hexdigest()==data['sha256']
print('Published source APK size and SHA-256 verified')
PY
timeout 90 adb install "$AUDIT_DIR/source.apk"
rm "$AUDIT_DIR/source.apk"
timeout 90 adb install tools/signed-update-audit/build/outputs/apk/debug/signed-update-audit-debug.apk
timeout 20 adb shell cmd locale set-app-locales "$BROWSER" --locales en-US
timeout 20 adb shell am force-stop com.android.launcher3 || true
SOURCE_CODE=$(python3 -c 'import json; print(json.load(open("signed-update-audit/manifest.json"))["source"]["versionCode"])')
TARGET_CODE=$(python3 -c 'import json; print(json.load(open("signed-update-audit/manifest.json"))["target"]["versionCode"])')
TARGET_TAG=$(python3 -c 'import json; print(json.load(open("signed-update-audit/manifest.json"))["target"]["tag"])')
SIGNER=$(python3 -c 'import json; print(json.load(open("signed-update-audit/manifest.json"))["productionSignerSha256"])')
set +e
timeout 660 adb shell am instrument -w -r \
  -e source_code "$SOURCE_CODE" -e target_code "$TARGET_CODE" \
  -e target_tag "$TARGET_TAG" -e signer_sha256 "$SIGNER" \
  "$DRIVER/.SignedUpdateInstrumentation" | tee "$AUDIT_DIR/instrumentation.txt"
RESULT=${PIPESTATUS[0]}
set -e
for NAME in update-offer.png bookmarks-before.png preferences-before.png update-downloading.png \
  unknown-source-prompt.png unknown-source-settings.png unknown-source-granted.png \
  install-confirmation.png install-complete.png preferences-after.png bookmarks-after.png \
  current-version.png installed-metadata.json failure.png hierarchy.txt; do
  timeout 15 adb exec-out run-as "$DRIVER" cat "cache/signed-update-audit/$NAME" \
    > "$AUDIT_DIR/$NAME" || true
done
if [ "$RESULT" -ne 0 ]; then exit "$RESULT"; fi
python3 - <<'PY'
from pathlib import Path
s=Path('signed-update-audit/instrumentation.txt').read_text()
assert 'eymen_signed_update_audit=passed' in s,s
assert 'INSTRUMENTATION_CODE: -1' in s,s
for name in ('update-offer','bookmarks-before','preferences-before','update-downloading',
             'unknown-source-prompt','unknown-source-settings','unknown-source-granted',
             'install-confirmation','install-complete','preferences-after','bookmarks-after','current-version'):
 p=Path('signed-update-audit',name+'.png');b=p.read_bytes()
 assert len(b)>1000 and b.startswith(b'\x89PNG\r\n\x1a\n') and b.endswith(b'\x00\x00\x00\x00IEND\xaeB`\x82'),p
PY
# Pull only the installed, public APK code. No production private data is read.
INSTALLED_PATH=$(adb shell pm path "$BROWSER" | tr -d '\r' | sed -n 's/^package://p')
case "$INSTALLED_PATH" in /data/app/*/base.apk) ;; *) exit 1;; esac
timeout 90 adb pull "$INSTALLED_PATH" "$AUDIT_DIR/installed-target.apk"
python3 - <<'PY'
import hashlib,json
from pathlib import Path
base=Path('signed-update-audit')
target=json.loads((base/'manifest.json').read_text())['target']
p=base/'installed-target.apk'
actual=hashlib.sha256(p.read_bytes()).hexdigest()
assert p.stat().st_size==target['bytes']
assert actual==target['sha256']
(base/'installed-apk-sha256.txt').write_text(actual+'\n')
p.unlink()
print('Actual installed APK matches the published target byte-for-byte')
PY
