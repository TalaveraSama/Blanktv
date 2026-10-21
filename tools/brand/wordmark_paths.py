#!/usr/bin/env python3
"""Rebuild the in-app wordmark vectors ("Blank" + "TV") from a font.

Upstream's own wordmark is traced artwork kept in ``tools/brand/pixel/wordmark.json`` — outlines for
the letters o, w, n, t, v at the design's optical size. This fork changes the word, and that file
cannot produce letters it does not contain, so the fork generates the outlines from the font the app
already ships instead.

Geometry keeps the design's numbers so the lockup is unaffected: a 988 x 182 canvas, the word on one
baseline with an x-height of 138 and a cap height of 179 — the heights of upstream's own traced
letters. Only the word changes, and the phrase is centred in the canvas.

    /tmp/venv/bin/python tools/brand/wordmark_paths.py \\
        --font app/src/main/res/font/plus_jakarta_sans_variable.ttf \\
        --out core/src/main/res/drawable

Requires fontTools. Writes ``owntv_wordmark_own.xml`` and ``owntv_wordmark_tv.xml`` — the file names
are kept because every call site and the lint config name them.
"""

from __future__ import annotations

import argparse
import sys
from pathlib import Path

from fontTools.pens.qu2cuPen import Qu2CuPen
from fontTools.pens.svgPathPen import SVGPathPen
from fontTools.pens.transformPen import TransformPen
from fontTools.misc.transform import Transform
from fontTools.ttLib import TTFont
from fontTools.varLib.instancer import instantiateVariableFont

# --- the canvas the app draws into ---------------------------------------------------------
# `Wordmark()` lays these two vectors into one box of width/height = 988/182 and tints each, so the
# numbers that matter are the canvas and the type sizes the design settled on. Upstream traced its
# word into the same canvas (its letters sit at x 271.7 -> 1258 in a group translated by -270.5);
# this fork draws straight into 0..988 and centres the phrase, which is the same picture without the
# translation to unwind.
VIEW_W, VIEW_H = 988.0, 182.0
BASELINE = 179.0          # leaves 3 units below for descenders/rounding
X_HEIGHT = 138.0          # the design's x-height: upstream's o/w/n are exactly this tall
CAP_HEIGHT = 179.0        # the design's "tv" is exactly this tall
GAP_UNITS = 14.0          # gap between the two words, in units (upstream's traced letters sat closer
                          # because tracing removes sidebearings; a real font keeps them)
TRACKING_EM = -0.025
WORD1 = "blank"
WORD2 = "TV"
WEIGHT = 800


def load_font(path: str) -> TTFont:
    font = TTFont(path)
    if "fvar" in font:
        try:
            font = instantiateVariableFont(font, {"wght": WEIGHT})
        except Exception as exc:  # a font without a wght axis is fine as-is
            print(f"note: could not instance weight {WEIGHT} ({exc}); using the default instance")
    return font


def measure(font: TTFont, text: str) -> tuple[float, float, float]:
    """(advance in font units including tracking, x-height, cap height)."""
    upem = font["head"].unitsPerEm
    advance = sum(font["hmtx"][font.getBestCmap()[ord(ch)]][0] for ch in text)
    advance += TRACKING_EM * upem * (len(text) - 1)
    os2 = font["OS/2"]
    x_height = getattr(os2, "sxHeight", 0) or upem * 0.52
    cap_height = getattr(os2, "sCapHeight", 0) or upem * 0.70
    return advance, x_height, cap_height


def path_for(font: TTFont, text: str, scale: float, origin_x: float) -> str:
    """One SVG path for `text`, baseline at BASELINE, first glyph starting at origin_x."""
    upem = font["head"].unitsPerEm
    cmap = font.getBestCmap()
    glyph_set = font.getGlyphSet()
    x = origin_x
    parts: list[str] = []
    for ch in text:
        name = cmap[ord(ch)]
        pen = SVGPathPen(glyph_set, ntos=lambda v: f"{v:.3f}".rstrip("0").rstrip("."))
        # Font units are y-up; the vector canvas is y-down, so flip and sit the glyph on the baseline.
        # TrueType outlines are quadratic and VectorDrawable does accept Q, but emitting cubics keeps
        # the emitted path in the same one-command-family shape upstream's traced wordmark used, and
        # means the file can be checked by any SVG path renderer.
        # fillType is nonZero, not evenOdd: real font outlines rely on winding, and a glyph whose
        # contours overlap (Jakarta's "a" and "k" both do) turns into a hole under even-odd. Upstream
        # traced its wordmark, so even-odd was the right rule for traced shapes — not for these.
        placed = TransformPen(pen, Transform(scale, 0, 0, -scale, x, BASELINE))
        glyph_set[name].draw(Qu2CuPen(placed, max_err=0.4, all_cubic=True))
        parts.append(pen.getCommands())
        x += font["hmtx"][name][0] * scale + TRACKING_EM * upem * scale
    return " ".join(p for p in parts if p)


def layout(font: TTFont) -> tuple[str, str, float, float]:
    """Both paths, scaled uniformly so the design's x-height holds, centred in the canvas."""
    adv1, xh, _ = measure(font, WORD1)
    adv2, _, cap = measure(font, WORD2)
    # One scale for both words: the x-height is the design's fixed point, and the cap height has to
    # fit the canvas above the same baseline. A font whose caps run taller than the design's ratio
    # takes the smaller of the two scales.
    scale = min(X_HEIGHT / xh, CAP_HEIGHT / cap)
    w1, w2 = adv1 * scale, adv2 * scale
    total = w1 + GAP_UNITS + w2
    x = (VIEW_W - total) / 2
    return (
        path_for(font, WORD1, scale, x),
        path_for(font, WORD2, scale, x + w1 + GAP_UNITS),
        scale,
        total,
    )


def write_vector(path: Path, body: str) -> None:
    path.write_text(
        '<?xml version="1.0" encoding="utf-8"?>\n'
        "<!-- Generated by tools/brand/wordmark_paths.py. Do not edit by hand. -->\n"
        '<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
        f'    android:width="{VIEW_W:.0f}dp" android:height="{VIEW_H:.0f}dp" '
        f'android:viewportWidth="{VIEW_W:.0f}" android:viewportHeight="{VIEW_H:.0f}">\n'
        f'    <path android:pathData="{body}" android:fillColor="#FFFFFFFF" '
        'android:fillType="nonZero"/>\n'
        "</vector>\n",
        encoding="utf-8",
    )


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    ap.add_argument("--font", required=True)
    ap.add_argument("--out", required=True)
    args = ap.parse_args()

    font = load_font(args.font)
    first, second, scale, total = layout(font)
    out = Path(args.out)
    write_vector(out / "owntv_wordmark_own.xml", first)
    write_vector(out / "owntv_wordmark_tv.xml", second)
    print(f"wrote {WORD1!r} + {WORD2!r} to {out} (scale {scale:.2f}, ink width {total:.0f} of {VIEW_W:.0f})")
    return 0


if __name__ == "__main__":
    sys.exit(main())
