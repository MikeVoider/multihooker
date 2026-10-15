"""One combined contact sheet showing every tier's hero render + swatches."""
import os

from PIL import Image, ImageDraw

import tiers

HERE = os.path.dirname(os.path.abspath(__file__))
TIERS_DIR = os.path.join(HERE, "preview", "tiers")
PAD = 10
LABEL = 20
SWATCH_H = 16
COLS = 3
HEADER = 36


def main():
    os.makedirs(TIERS_DIR, exist_ok=True)
    cells = []
    for t in tiers.TIERS:
        p = os.path.join(TIERS_DIR, f"{t['key']}_hero.png")
        if os.path.exists(p):
            cells.append((t, Image.open(p).convert("RGBA")))
        else:
            print("missing", p)
    if not cells:
        print("no hero renders found")
        return
    cw = max(im.width for _, im in cells)
    ch = max(im.height for _, im in cells)
    rows = (len(cells) + COLS - 1) // COLS
    W = COLS * (cw + PAD) + PAD
    H = HEADER + rows * (LABEL + ch + SWATCH_H + PAD) + PAD
    sheet = Image.new("RGBA", (W, H), (26, 26, 30, 255))
    d = ImageDraw.Draw(sheet)
    d.text((PAD, 10), "MULTIHOOKER CRATES - all 9 tiers", fill=(245, 245, 245, 255))
    for i, (t, im) in enumerate(cells):
        col = i % COLS
        row = i // COLS
        x = PAD + col * (cw + PAD)
        y = HEADER + row * (LABEL + ch + SWATCH_H + PAD)
        d.text((x + 2, y), f"{t['num']}. {t['mc_name']}", fill=(235, 235, 235, 255))
        for yy in range(0, im.height, 12):
            for xx in range(0, im.width, 12):
                c = (54, 54, 58, 255) if ((xx // 12 + yy // 12) % 2 == 0) else (40, 40, 44, 255)
                d.rectangle([x + xx, y + LABEL + yy, x + xx + 11, y + LABEL + yy + 11], fill=c)
        sheet.alpha_composite(im, (x, y + LABEL))
        sy = y + LABEL + ch + 2
        sx = x + 2
        for label, c in t["swatches"]:
            d.rectangle([sx, sy, sx + 12, sy + 12], fill=c + (255,), outline=(20, 20, 20, 255))
            d.text((sx + 16, sy - 1), label, fill=(210, 210, 210, 255))
            sx += 16 + 6 * len(label) + 8
    out = os.path.join(HERE, "preview", "all_tiers.png")
    sheet.save(out)
    print("all-tiers sheet", os.path.abspath(out), sheet.size)


if __name__ == "__main__":
    main()