#!/usr/bin/env bash
# Build a signed, internal-review-only Android App Bundle, never publish it.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"
: "$EY_SIGNING_KEYSTORE_FILE" "$EY_SIGNING_STORE_PASSWORD" "$EY_SIGNING_KEY_ALIAS" "$EY_SIGNING_KEY_PASSWORD"
test -f "$EY_SIGNING_KEYSTORE_FILE"
test -z "$(git status --porcelain)" || { echo "Use a clean, disposable checkout" >&2; exit 1; }
python3 tools/generate.py
python3 tools/generate.py --check
python3 tools/verify.py
for check in tools/check_*.py; do python3 "$check"; done
node tools/test_cosmetic_extension.mjs
EXPECTED="$(tr -d '\r\n' < docs/ANDROID_RELEASE_CERT_SHA256.txt)"
ACTUAL="$(keytool -list -v -keystore "$EY_SIGNING_KEYSTORE_FILE" -storepass:env EY_SIGNING_STORE_PASSWORD -alias "$EY_SIGNING_KEY_ALIAS" | awk '/SHA256:/ {gsub(":","",$2);print toupper($2);exit}')"
test "$ACTUAL" = "$EXPECTED" || { echo "Production signing certificate mismatch" >&2; exit 1; }
MANIFEST="$ROOT/android/app-telefon/src/main/AndroidManifest.xml"
BACKUP="$(mktemp)"
cp "$MANIFEST" "$BACKUP"
restore_manifest() { cp "$BACKUP" "$MANIFEST"; rm -f "$BACKUP"; }
trap restore_manifest EXIT
python3 tools/prepare_google_play_manifest.py --apply
(
  cd android
  gradle --no-daemon :app-telefon:bundleRelease \
    -PeymenGooglePlay=true -PeymenReleaseVersion=1.0.0 -PeymenReleaseCode=1000000
)
BUNDLE="$ROOT/android/app-telefon/build/outputs/bundle/release/app-telefon-release.aab"
test -s "$BUNDLE" || { echo "AAB not produced" >&2; exit 1; }
unzip -tq "$BUNDLE" >/dev/null
jarsigner -verify -strict "$BUNDLE"
python3 - "$ROOT" <<'PY'
import pathlib, sys, xml.etree.ElementTree as ET
root=pathlib.Path(sys.argv[1])
folder=root/"android/app-telefon/build/intermediates"
manifests=list(folder.glob("**/release/**/AndroidManifest.xml"))
if not manifests:
    raise SystemExit("No merged release manifest found; cannot prove Play permission compliance")
name="{http://schemas.android.com/apk/res/android}name"
for path in manifests:
    try: parsed=ET.parse(path)
    except ET.ParseError: continue
    permissions={e.get(name) for e in parsed.getroot().findall("uses-permission")}
    if "android.permission.REQUEST_INSTALL_PACKAGES" in permissions:
        raise SystemExit(f"APK install permission leaked into Play manifest: {path}")
print(f"Checked {len(manifests)} merged release manifest candidates")
PY
sha256sum "$BUNDLE"
printf 'Signed Google Play review AAB: %s\n' "$BUNDLE"
