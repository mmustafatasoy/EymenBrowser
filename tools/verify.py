#!/usr/bin/env python3
"""Stage-0 gates: size ceiling, central version pins, forbidden dependencies and tokens."""
import argparse
import json
from pathlib import Path
import re
import tomllib

ROOT = Path(__file__).resolve().parents[1]
V = tomllib.loads((ROOT / "versions.toml").read_text(encoding="utf-8"))
MAX = V["checks"]["max_installer_mb"] * 1024 * 1024

def verify():
    errors = []
    colors = json.loads((ROOT / "design/tokens.json").read_text(encoding="utf-8"))
    if not all(theme in colors["renk"] for theme in ("gece", "gunduz", "gizli")):
        errors.append("Tema renkleri eksik")
    tr = json.loads((ROOT / "shared/i18n/tr.json").read_text(encoding="utf-8"))
    en = json.loads((ROOT / "shared/i18n/en.json").read_text(encoding="utf-8"))
    if set(tr) != set(en):
        errors.append("TR ve EN metin anahtarları farklı")
    manifests = [*ROOT.glob("android/**/*.gradle.kts"), ROOT / "desktop/package.json",
                 ROOT / "ios/project.yml"]
    banned = re.compile(r"firebase[-.]analytics|google-mobile-ads|appcenter|"
                        r"amplitude[-_]|mixpanel|segment[-.]analytics|sentry[-.]|"
                        r"facebook[-.]business[-.]sdk", re.I)
    for file in manifests:
        if file.exists() and banned.search(file.read_text(encoding="utf-8")):
            errors.append(f"Yasak izleme bağımlılığı: {file.relative_to(ROOT)}")
    package = json.loads((ROOT / "desktop/package.json").read_text(encoding="utf-8"))
    expected = "github:castlabs/electron-releases#" + V["desktop"]["electron"]
    if package["devDependencies"].get("electron") != expected:
        errors.append("Electron sürümü versions.toml ile uyuşmuyor")
    if package["version"] != V["project"]["version"]:
        errors.append("Masaüstü sürümü versions.toml ile uyuşmuyor")
    for module in ("app-telefon", "app-tv"):
        gradle = (ROOT / f"android/{module}/build.gradle.kts").read_text(encoding="utf-8")
        if f'minSdk = {V["android"]["min_sdk"]}' not in gradle:
            errors.append(f"{module} Android alt sürüm farklı")
        if f'versionName = "{V["project"]["version"]}"' not in gradle:
            errors.append(f"{module} uygulama sürümü farklı")
    if errors:
        raise SystemExit("\n".join(errors))
    print("Şartname bağımlılıkları, sürüm kilitleri ve tasarım temel denetimi geçti.")

def check_artifacts(paths):
    for p in paths:
        f = Path(p)
        if not f.is_file():
            raise SystemExit(f"Kurulum dosyası bulunamadı: {f}")
        if f.stat().st_size > MAX:
            raise SystemExit(f"500 MB sınırı aşıldı: {f} ({f.stat().st_size} bayt)")
        if f.stat().st_size == 0:
            raise SystemExit(f"Boş kurulum dosyası: {f}")
        print(f"Boyut uygun: {f} ({f.stat().st_size} bayt)")

if __name__ == "__main__":
    ap = argparse.ArgumentParser()
    ap.add_argument("--artifact", action="append", default=[])
    args = ap.parse_args()
    verify()
    check_artifacts(args.artifact)
