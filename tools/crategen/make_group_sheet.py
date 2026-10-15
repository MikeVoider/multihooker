"""Contact sheet grouping the crates by category (normal vs special), taken
straight from the "group" field in tiers.py. Writes preview/crate_groups.png.

Usage:  python make_group_sheet.py
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from PIL import Image, ImageDraw

import tiers

HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(HERE, "out")
PREVIEW = os.path.join(HERE, "preview")

ZOOM = 5
PAD = 14
LABEL = 18
HEAD = 32

ORDER = ["normal", "special"]
TITLES = {
    "normal": "NORMAL CRATES  (tag: multihooker:normal_crates)",
    "special": "SPECIAL CRATES  (tag: multihooker:special_crates)",
}


def main():
    os.makedirs(PREVIEW, exist_ok=True)
    groups = {g: [t for t in tiers.TIERS if t["group"] == g] for g in ORDER}
    cols = max(len(v) for v in groups.values())
    cw = 64 * ZOOM
    W = PAD + cols * (cw + PAD)
    row_h = HEAD + LABEL + cw + PAD
    H = 30 + len(ORDER) * row_h + PAD
    sheet = Image.new("RGBA", (W, H), (24, 24, 28, 255))
    d = ImageDraw.Draw(sheet)
    d.text((PAD, 10), "MultiHooker crates by category", fill=(245, 245, 245, 255))

    y = 30
    for group in ORDER:
        d.text((PAD, y), TITLES[group], fill=(255, 220, 140, 255))
        y += HEAD
        for i, tier in enumerate(groups[group]):
            x = PAD + i * (cw + PAD)
            d.text((x + 2, y), "%d. %s" % (tier["num"], tier["mc_name"]), fill=(235, 235, 235, 255))
            img = Image.open(os.path.join(OUT, tier["key"] + "_crate_front.png")).convert("RGBA")
            sheet.alpha_composite(img.resize((cw, cw), Image.NEAREST), (x, y + LABEL))
            d.rectangle([x - 1, y + LABEL - 1, x + cw, y + LABEL + cw], outline=(90, 90, 96, 255))
        y += LABEL + cw + PAD

    p = os.path.join(PREVIEW, "crate_groups.png")
    sheet.save(p)
    print("preview", p, sheet.size)


if __name__ == "__main__":
    main()
