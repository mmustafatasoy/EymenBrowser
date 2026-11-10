#!/usr/bin/env python3
"""Pin a real, published old/new APK pair for the external Android UI audit."""
import json
import os
from pathlib import Path
import re
import tomllib
import urllib.request

ROOT = Path(__file__).resolve().parents[1]
REPO = "mmustafatasoy/EymenBrowser"
OUTPUT = ROOT / "signed-update-audit"


def release_code(tag):
    if not re.fullmatch(r"v[1-9][0-9]*\.[0-9]{1,2}\.[0-9]{1,2}", tag):
        raise ValueError("Use a stable vMAJOR.MINOR.PATCH tag")
    major, minor, patch = map(int, tag[1:].split("."))
    value = major * 10000 + minor * 100 + patch
    if value > 2_000_000_000:
        raise ValueError("Version code is out of range")
    return value


def fetch_release(suffix):
    headers = {"Accept": "application/vnd.github+json", "User-Agent": "EymenSignedUpdateAudit"}
    token = os.environ.get("GH_TOKEN")
    if token:
        headers["Authorization"] = "Bearer " + token
    # Authentication goes only to this fixed official API origin.
    request = urllib.request.Request(f"https://api.github.com/repos/{REPO}/releases/{suffix}", headers=headers)
    with urllib.request.urlopen(request, timeout=30) as response:
        data = response.read(2 * 1024 * 1024 + 1)
    if len(data) > 2 * 1024 * 1024:
        raise ValueError("Release metadata exceeds size limit")
    release = json.loads(data)
    if release.get("draft", True) or release.get("prerelease", True):
        raise ValueError("Audit requires an already published stable release")
    return release


def asset(release, tag):
    if release["tag_name"] != tag:
        raise ValueError("Unexpected release tag")
    name = "EymenBrowser-Android-Phone-x86_64.apk"
    item = next(x for x in release["assets"] if x["name"] == name)
    digest = item.get("digest", "")
    expected_url = f"https://github.com/{REPO}/releases/download/{tag}/{name}"
    if (item.get("state") != "uploaded" or item.get("browser_download_url") != expected_url
            or not 0 < item["size"] <= 500 * 1024 * 1024
            or not re.fullmatch(r"sha256:[a-fA-F0-9]{64}", digest)):
        raise ValueError("Invalid canonical APK asset")
    return {"tag": tag, "versionCode": release_code(tag), "url": expected_url,
            "bytes": item["size"], "sha256": digest.split(":")[1].lower()}


def main():
    source_tag = os.environ.get("AUDIT_SOURCE_TAG", "v1.0.21")
    target_tag = os.environ.get("AUDIT_TARGET_TAG", "v1.0.22")
    if release_code(source_tag) >= release_code(target_tag):
        raise ValueError("Target must be newer than source")
    source = asset(fetch_release("tags/" + source_tag), source_tag)
    target = asset(fetch_release("tags/" + target_tag), target_tag)
    if fetch_release("latest")["tag_name"] != target_tag:
        raise ValueError("Target must be the real latest stable release used by the production updater")
    signer = (ROOT / "docs/ANDROID_RELEASE_CERT_SHA256.txt").read_text().strip().lower()
    if not re.fullmatch(r"[a-f0-9]{64}", signer):
        raise ValueError("Missing pinned public certificate fingerprint")
    OUTPUT.mkdir(exist_ok=True)
    (OUTPUT / "manifest.json").write_text(json.dumps({"source": source, "target": target,
        "productionSignerSha256": signer, "physicalDeviceAcceptance": False}, indent=2) + "\n")
    pins = tomllib.loads((ROOT / "versions.toml").read_text())["android"]
    # Build dependencies come exclusively from the existing central pins.
    with open(os.environ["GITHUB_ENV"], "a") as output:
        for key in ("agp", "gradle", "compile_sdk", "target_sdk", "min_sdk", "java"):
            value = str(pins[key])
            if not re.fullmatch(r"[0-9.]+", value):
                raise ValueError("Invalid central Android tool pin")
            output.write(f"EY_AUDIT_{key.upper()}={value}\n")
    print(f"Pinned published signed update: {source_tag} -> {target_tag}")


if __name__ == "__main__":
    main()
