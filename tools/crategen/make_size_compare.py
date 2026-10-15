"""Render the engraved crate name at several font sizes side by side, so the
text size can be compared before picking one.

Writes preview/size_compare.png: rows = size variants, columns = a few tiers.
Usage:  python make_size_compare.py
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from PIL import Image, ImageDraw

import gen_textures as g
import tiers

HERE = os.path.dirname(os.path.abspath(__file__))
PREVIEW = os.path.join(HERE, "preview")

SIZE = g.SIZE
ZOOM = 6
PAD = 20
LABEL_H = 22
NAME_COL = 215

# the config the textures are actually generated with, captured before we
# monkeypatch anything below
ACTIVE = (g.FONT is g.FONT_3X5, g.TEXT_SCALE, g.GLYPH_GAP, g.FONT is g.FONT_4X6)

# (label, font table, TEXT_SCALE, GLYPH_GAP)
VARIANTS = [
    ("4x6 @1  gap1", g.FONT_4X6, 1, 1),
    ("3x5 @2  gap1", g.FONT_3X5, 2, 1),
    ("3x5 @2  gap2", g.FONT_3X5, 2, 2),
    ("4x6 @2  gap1", g.FONT_4X6, 2, 1),
]
PICK = ["common", "uncommon", "ghost_unspoken"]


def use(font, scale, gap):
    g.FONT = font
    g.TEXT_SCALE = scale
    g.GLYPH_GAP = gap


def name_box(tier):
    scratch = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 255))
    boxes = g.engrave_name(scratch, tier, tier["wood"])
    w = max(b[2] - b[0] + 1 for b in boxes)
    h = max(b[3] - b[1] + 1 for b in boxes)
    return w, h


def main():
    os.makedirs(PREVIEW, exist_ok=True)
    tiers_by_key = {t["key"]: t for t in tiers.TIERS}
    picks = [tiers_by_key[k] for k in PICK]

    cells = {}
    for label, font, scale, gap in VARIANTS:
        use(font, scale, gap)
        for tier in picks:
            seed = 120 + tier["seed"] * 10 + len("front") + 3
            cells[(label, tier["key"])] = (g.crate_face(tier, seed, "front"), name_box(tier))

    cw = SIZE * ZOOM
    cols = len(picks)
    W = NAME_COL + PAD + cols * (cw + PAD)
    H = 44 + len(VARIANTS) * (LABEL_H + cw + PAD + 14)
    sheet = Image.new("RGBA", (W, H), (24, 24, 28, 255))
    d = ImageDraw.Draw(sheet)
    d.text((PAD, 16), "CRATE NAME SIZE  -  front faces at %dx" % ZOOM, fill=(245, 245, 245, 255))

    for c, tier in enumerate(picks):
        x = NAME_COL + PAD + c * (cw + PAD)
        d.text((x + 2, 32), "%s  [%s]" % (tier["mc_name"], " ".join(tier["words"])), fill=(235, 235, 235, 255))

    for r, (label, font, scale, gap) in enumerate(VARIANTS):
        active = (font is g.FONT_3X5, scale, gap, font is g.FONT_4X6) == ACTIVE
        tag = "  <== shipped" if active else ""
        y = 44 + r * (LABEL_H + cw + PAD + 14)
        d.text((PAD, y + cw // 2 + LABEL_H), label + tag, fill=(255, 220, 140, 255))
        for c, tier in enumerate(picks):
            img, (w, h) = cells[(label, tier["key"])]
            x = NAME_COL + PAD + c * (cw + PAD)
            big = img.resize((cw, cw), Image.NEAREST)
            sheet.alpha_composite(big, (x, y + LABEL_H))
            d.rectangle([x - 1, y + LABEL_H - 1, x + cw, y + LABEL_H + cw], outline=(90, 90, 96, 255))
            d.text((x + 2, y + LABEL_H + cw + 4), "name %d x %d px" % (w, h), fill=(190, 230, 190, 255))

    p = os.path.join(PREVIEW, "size_compare.png")
    sheet.save(p)
    print("preview", p, sheet.size)


if __name__ == "__main__":
    main()
