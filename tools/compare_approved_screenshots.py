#!/usr/bin/env python3
"""Compare a REAL Android screenshot with an approved design/ekranlar PNG.

Outputs are diagnostic (never auto-approve visual parity).
No user/private screenshots are uploaded by this tool.
"""
import argparse
import json
from pathlib import Path
from PIL import Image, ImageChops, ImageEnhance, ImageStat

ROOT = Path(__file__).resolve().parents[1]
APPROVED = {p.stem: p for p in (ROOT / "design" / "ekranlar").glob("T*.png")}

def compare(screen: str, capture: Path, destination: Path) -> dict:
    if screen not in APPROVED:
        raise ValueError(f"Unknown approved screen {screen!r}; allowed: {', '.join(sorted(APPROVED))}")
    with Image.open(APPROVED[screen]) as source, Image.open(capture) as actual:
        base = source.convert("RGB")
        observed = actual.convert("RGB")
    original_size = observed.size
    if observed.size != base.size:
        observed = observed.resize(base.size, Image.Resampling.LANCZOS)
    diff = ImageChops.difference(base, observed)
    stats = ImageStat.Stat(diff)
    mean_abs = sum(stats.mean) / len(stats.mean)
    pixel_diffs = [max(r, g, b) for r, g, b in diff.getdata()]
    fraction = sum(value >= 25 for value in pixel_diffs) / len(pixel_diffs)
    report = {
        "screen": screen,
        "approved_png": str(APPROVED[screen].relative_to(ROOT)),
        "capture": str(capture),
        "approved_pixels": list(base.size),
        "captured_pixels": list(original_size),
        "size_resampled_for_comparison": original_size != base.size,
        "mean_absolute_rgb_error_0_to_255": round(mean_abs, 2),
        "fraction_of_pixels_with_channel_error_at_least_25": round(fraction, 4),
        "pixel_exact_verified": mean_abs == 0.0 and original_size == base.size,
        "acceptance": "NOT AUTOMATICALLY ACCEPTED — human review and device checks required",
    }
    destination.mkdir(parents=True, exist_ok=True)
    panels = Image.new("RGB", (base.width * 3, base.height))
    panels.paste(base, (0, 0))
    panels.paste(observed, (base.width, 0))
    panels.paste(
        ImageEnhance.Contrast(diff).enhance(3.0),
        (base.width * 2, 0)
    )
    panels.save(destination / f"{screen}-side-by-side-diff.png", optimize=True)
    (destination / f"{screen}-metrics.json").write_text(
        json.dumps(report, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8"
    )
    return report

def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--screen", required=True, choices=sorted(APPROVED))
    ap.add_argument("--capture", required=True, type=Path)
    ap.add_argument("--output", required=True, type=Path)
    args = ap.parse_args()
    if not args.capture.is_file():
        ap.error(f"Capture is absent: {args.capture}")
    print(json.dumps(compare(args.screen, args.capture, args.output), indent=2))

if __name__ == "__main__":
    main()
