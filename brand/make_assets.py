#!/usr/bin/env python3
"""Generates every binary brand asset blankTV ships from brand/logo_master.png.

Everything here is derived from the single AI-generated master mark plus a system font, so the
repository stays reproducible: re-running this script regenerates identical (or near-identical)
assets. Keep the master in git; the generated densities are also committed because Android needs
them at build time and CI must not run image tooling.
"""
from __future__ import annotations

import os
from PIL import Image, ImageDraw, ImageFilter, ImageFont

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
BRAND = os.path.join(ROOT, "brand")
RES = os.path.join(ROOT, "app", "src", "main", "res")

MASTER = os.path.join(BRAND, "logo_master.png")

# Launcher icon densities (dp -> px). Legacy square icon side per density.
DENSITIES = {
    "mdpi": 48,
    "hdpi": 72,
    "xhdpi": 96,
    "xxhdpi": 144,
    "xxxhdpi": 192,
}
# Adaptive icon foreground canvas side per density (108dp full-bleed).
ADAPTIVE = {
    "mdpi": 108,
    "hdpi": 162,
    "xhdpi": 216,
    "xxhdpi": 324,
    "xxxhdpi": 432,
}

BG = (10, 14, 20, 255)          # #0A0E14 — matches the master's canvas
CYAN = (34, 211, 238, 255)      # #22D3EE
VIOLET = (139, 92, 246, 255)    # #8B5CF6


def ensure(path: str) -> None:
    os.makedirs(path, exist_ok=True)


def bbox_of_mark(img: Image.Image) -> tuple[int, int, int, int]:
    """Bounding box of the bright (non-background) pixels."""
    grey = img.convert("RGB")
    px = grey.load()
    w, h = grey.size
    minx, miny, maxx, maxy = w, h, -1, -1
    for y in range(0, h, 2):
        for x in range(0, w, 2):
            r, g, b = px[x, y]
            if r + g + b > 120:  # background is near-black; anything brighter is the mark
                minx = min(minx, x)
                miny = min(miny, y)
                maxx = max(maxx, x)
                maxy = max(maxy, y)
    if maxx < 0:
        raise SystemExit("could not locate the mark inside logo_master.png")
    return minx, miny, maxx + 1, maxy + 1


def rounded_mask(size: int, radius_ratio: float) -> Image.Image:
    mask = Image.new("L", (size, size), 0)
    d = ImageDraw.Draw(mask)
    r = int(size * radius_ratio)
    d.rounded_rectangle((0, 0, size - 1, size - 1), radius=r, fill=255)
    return mask


def circle_mask(size: int) -> Image.Image:
    mask = Image.new("L", (size, size), 0)
    d = ImageDraw.Draw(mask)
    d.ellipse((0, 0, size - 1, size - 1), fill=255)
    return mask


def gradient(size: int) -> Image.Image:
    """Diagonal cyan -> violet gradient, used behind the wordmark on the TV banner."""
    grad = Image.new("RGB", (size, size))
    px = grad.load()
    for y in range(size):
        for x in range(size):
            t = (x + y) / (2 * (size - 1))
            px[x, y] = tuple(
                int(CYAN[i] * (1 - t) + VIOLET[i] * t) for i in range(3)
            )
    return grad.convert("RGBA")


def load_font(px: int) -> ImageFont.FreeTypeFont:
    candidates = [
        "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf",
        "/usr/share/fonts/dejavu/DejaVuSans-Bold.ttf",
        "/usr/share/fonts/TTF/DejaVuSans-Bold.ttf",
        "/usr/share/fonts/truetype/liberation/LiberationSans-Bold.ttf",
    ]
    for c in candidates:
        if os.path.isfile(c):
            return ImageFont.truetype(c, px)
    return ImageFont.load_default()


def main() -> None:
    master = Image.open(MASTER).convert("RGBA")
    mw, mh = master.size
    box = bbox_of_mark(master)
    mark = master.crop(box)

    # --- adaptive icon foreground: full-bleed master, mark sits inside the 66dp safe zone ---
    for dpi, side in ADAPTIVE.items():
        ensure(os.path.join(RES, f"drawable-{dpi}"))
        fg = master.resize((side, side), Image.LANCZOS)
        fg.save(os.path.join(RES, f"drawable-{dpi}", "ic_launcher_foreground.png"))

    # --- legacy square + round launcher icons: tight crop, padded, on a filled dark square ---
    pad_ratio = 0.14
    mw_px, mh_px = mark.size
    pad = int(max(mw_px, mh_px) * pad_ratio)
    side = max(mw_px, mh_px) + 2 * pad
    for dpi, px in DENSITIES.items():
        canvas = Image.new("RGBA", (side, side), BG)
        ox = (side - mw_px) // 2
        oy = (side - mh_px) // 2
        canvas.paste(mark, (ox, oy), mark)
        icon = canvas.resize((px, px), Image.LANCZOS)

        ensure(os.path.join(RES, f"mipmap-{dpi}"))
        icon.save(os.path.join(RES, f"mipmap-{dpi}", "ic_launcher.png"))
        round_icon = Image.new("RGBA", (px, px), (0, 0, 0, 0))
        round_icon.paste(icon, (0, 0), circle_mask(px))
        round_icon.save(os.path.join(RES, f"mipmap-{dpi}", "ic_launcher_round.png"))

    # --- Android TV banner (320x180 @xhdpi): mark left, wordmark right, on a soft gradient sweep ---
    bw, bh = 320, 180
    banner = Image.new("RGBA", (bw, bh), BG)
    sweep = gradient(bh).resize((bw, bh), Image.LANCZOS).filter(ImageFilter.GaussianBlur(30))
    fade = Image.new("L", (bw, bh), 0)
    fd = ImageDraw.Draw(fade)
    fd.ellipse((-140, -120, 260, 300), fill=42)
    fade = fade.filter(ImageFilter.GaussianBlur(40))
    banner.paste(sweep, (0, 0), fade)

    icon_side = 88
    icon_img = Image.new("RGBA", (icon_side, icon_side), (0, 0, 0, 0))
    tmp = Image.new("RGBA", (icon_side, icon_side), BG)
    m = mark.resize((int(icon_side * 0.74), int(icon_side * 0.74)), Image.LANCZOS)
    tmp.paste(m, ((icon_side - m.width) // 2, (icon_side - m.height) // 2), m)
    icon_img.paste(tmp, (0, 0), rounded_mask(icon_side, 0.22))
    banner.paste(icon_img, (20, (bh - icon_side) // 2), icon_img)

    font = load_font(40)
    d = ImageDraw.Draw(banner)
    text = "blankTV"
    tb = d.textbbox((0, 0), text, font=font)
    tw, th = tb[2] - tb[0], tb[3] - tb[1]
    tx = 20 + icon_side + 14
    ty = (bh - th) // 2 - tb[1]
    d.text((tx, ty), text, font=font, fill=(245, 247, 250, 255))
    ensure(os.path.join(RES, "drawable-xhdpi"))
    banner.convert("RGB").save(os.path.join(RES, "drawable-xhdpi", "tv_banner.png"))

    # --- in-app wordmark: mark + text on transparent background, for empty states / about ---
    wmw, wmh = 420, 120
    wm = Image.new("RGBA", (wmw, wmh), (0, 0, 0, 0))
    mi = mark.resize((96, 96), Image.LANCZOS)
    wm.paste(mi, (0, (wmh - 96) // 2), mi)
    wfont = load_font(58)
    wd = ImageDraw.Draw(wm)
    wtb = wd.textbbox((0, 0), text, font=wfont)
    wd.text((112, (wmh - (wtb[3] - wtb[1])) // 2 - wtb[1]), text, font=wfont, fill=(245, 247, 250, 255))
    ensure(os.path.join(RES, "drawable-nodpi"))
    wm.save(os.path.join(RES, "drawable-nodpi", "logo_wordmark.png"))

    # --- master copy of the banner/wordmark for docs & README ---
    banner.convert("RGB").save(os.path.join(BRAND, "tv_banner.png"))
    wm.save(os.path.join(BRAND, "logo_wordmark.png"))

    print("brand assets written")


if __name__ == "__main__":
    main()
