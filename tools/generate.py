#!/usr/bin/env python3
"""Create platform themes, localized strings and SVG icons exclusively from shared sources."""
import argparse
import html
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
TOKENS = json.loads((ROOT / "design/tokens.json").read_text(encoding="utf-8"))
LANG = {locale: json.loads((ROOT / "shared/i18n" / f"{locale}.json").read_text(encoding="utf-8")) for locale in ("tr", "en")}

def colors():
    for theme, values in TOKENS["renk"].items():
        if not isinstance(values, dict):
            continue
        for key, value in values.items():
            if isinstance(value, str) and len(value) == 7 and value[0] == "#":
                yield theme, key, value

def metrics():
    for group in ("telefon", "bilgisayar", "tv"):
        for section, values in TOKENS[group].items():
            if not isinstance(values, dict):
                continue
            for key, value in values.items():
                if isinstance(value, (int, float)):
                    yield group, section, key, value

def produce():
    kt = ["// GENERATED from design/tokens.json - DO NOT EDIT",
          "package com.eymen.browser.tema", "",
          "import androidx.compose.ui.graphics.Color", "",
          "object EymenTokens {"]
    swift = ["// GENERATED from design/tokens.json - DO NOT EDIT", "import SwiftUI", "", "enum EymenTokens {"]
    css = ["/* GENERATED from design/tokens.json - DO NOT EDIT */", ":root {"]
    for theme, key, value in colors():
        name = f"{theme}_{key}"
        rgb = tuple(int(value[i:i+2], 16) for i in (1, 3, 5))
        kt.append(f"    val {name} = Color(0xFF{value[1:].upper()})")
        swift.append(f"    static let {name} = Color(red: {rgb[0]}/255.0, green: {rgb[1]}/255.0, blue: {rgb[2]}/255.0)")
        css.append(f"  --renk-{theme}-{key}: {value};")
    for group, section, key, value in metrics():
        name = f"{group}_{section}_{key}"
        kt.append(f"    const val {name} = {value}")
        swift.append(f"    static let {name}: CGFloat = {value}")
        css.append(f"  --{group}-{section}-{key}: {value}px;")
    kt.append("}")
    swift.append("}")
    css.append("}")

    results = {
        "android/core-tema/src/main/java/com/eymen/browser/tema/EymenTokens.kt": "\n".join(kt) + "\n",
        "ios/EymenBrowser/Generated/EymenTokens.swift": "\n".join(swift) + "\n",
        "desktop/src/generated/tokens.css": "\n".join(css) + "\n",
        "desktop/src/generated/i18n.ts": ("// GENERATED from shared/i18n - DO NOT EDIT\n"
            + "export const messages = " + json.dumps(LANG, ensure_ascii=False, indent=2) + " as const;\n"),
    }
    assert LANG["tr"].keys() == LANG["en"].keys(), "TR/EN anahtarları uyuşmuyor"
    for lang, data in LANG.items():
        resource = ['<?xml version="1.0" encoding="utf-8"?>', '<resources>']
        for key, text in data.items():
            escaped = html.escape(text, quote=True).replace("'", "\\'")
            resource.append(f'    <string name="{key}">{escaped}</string>')
        resource.append('</resources>')
        suffix = "values" if lang == "tr" else "values-en"
        for module in ("app-telefon", "app-tv"):
            results[f"android/{module}/src/main/res/{suffix}/strings.xml"] = "\n".join(resource) + "\n"
        ioslines = ["// GENERATED from shared/i18n - DO NOT EDIT"]
        for key, text in data.items():
            encoded = text.replace("\\", "\\\\").replace('"', '\\"').replace("\n", "\\n")
            ioslines.append(f'"{key}" = "{encoded}";')
        results[f"ios/EymenBrowser/{lang}.lproj/Localizable.strings"] = "\n".join(ioslines) + "\n"
    icon = ['<!-- GENERATED from design/tokens.json - DO NOT EDIT -->', '<svg xmlns="http://www.w3.org/2000/svg">']
    stroke = TOKENS["ikon"]["cizgi"]
    for name, path in TOKENS["ikon"]["yollar"].items():
        icon.append(f'  <symbol id="{html.escape(name)}" viewBox="0 0 24 24">'
                    + f'<path d="{html.escape(path, quote=True)}" fill="none" stroke="currentColor" stroke-width="{stroke}" stroke-linecap="round" stroke-linejoin="round"/></symbol>')
    icon.append('</svg>')
    results["shared/ikon/eymen-icons.svg"] = "\n".join(icon) + "\n"
    return results

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--check", action="store_true", help="Fail if generated outputs are missing or stale")
    args = parser.parse_args()
    mismatches = []
    for rel, content in produce().items():
        target = ROOT / rel
        if args.check:
            if not target.exists() or target.read_text(encoding="utf-8") != content:
                mismatches.append(rel)
        else:
            target.parent.mkdir(parents=True, exist_ok=True)
            target.write_text(content, encoding="utf-8")
            print("Oluşturuldu:", rel)
    if mismatches:
        raise SystemExit("Üretilmiş dosyalar güncel değil: " + ", ".join(mismatches))
    if args.check:
        print("Tüm tema, çeviri ve simge dosyaları güncel.")

if __name__ == "__main__":
    main()
