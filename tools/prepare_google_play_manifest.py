#!/usr/bin/env python3
"""Remove only the Google Play-disallowed self-update install permission.

The calling build script restores the original manifest at EXIT. AABs are
produced in a disposable checkout, not by modifying production source.
"""
import argparse
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
MANIFEST = ROOT / "android/app-telefon/src/main/AndroidManifest.xml"
TARGET = '    <uses-permission android:name="android.permission.REQUEST_INSTALL_PACKAGES" />\n'
NS = "{http://schemas.android.com/apk/res/android}"


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--apply", action="store_true")
    parser.add_argument("--verify", action="store_true")
    args = parser.parse_args()
    if args.apply == args.verify:
        parser.error("choose exactly one of --apply and --verify")
    content = MANIFEST.read_text(encoding="utf-8")
    if args.apply:
        if content.count(TARGET) != 1:
            raise SystemExit("Refusing to change an unexpected Android manifest")
        MANIFEST.write_text(content.replace(TARGET, "", 1), encoding="utf-8")
    tree = ET.parse(MANIFEST)
    perms = [
        e.get(NS + "name")
        for e in tree.getroot().findall("uses-permission")
    ]
    if "android.permission.REQUEST_INSTALL_PACKAGES" in perms:
        raise SystemExit("Google Play candidate still requests APK installation")
    print("Google Play manifest: self-update APK installation permission is absent")


if __name__ == "__main__":
    main()
