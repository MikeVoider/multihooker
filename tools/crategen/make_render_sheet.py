"""Contact sheet + sanity stats for one tier's Blender crate renders."""
import os
import sys

from PIL import Image, ImageDraw

import tiers

HERE = os.path.dirname(os.path.abspath(__file__))
R = os.path.join(HERE, "preview", "render")
ORDER = ["hero", "front", "back", "top"]
LABEL = 22
PAD = 8
HEADER = 40


def main():
    key = sys.argv[1] if len(sys.argv) > 1 else "divine"
    t = tiers.get(key)
    name = t["mc_name"].upper()
    imgs = []
    for name0 in ORDER:
        p = os.path.join(R, f"{name0}.png")
        im = Image.open(p).convert("RGBA")
        px = im.load()
        opaque = 0
        rs = gs = bs = 0
        for y in range(im.height):
            for x in range(im.width):
                r, g, b, a = px[x, y]
                if a > 16:
                    opaque += 1
                    rs += r; gs += g; bs += b
        total = im.width * im.height
        mean = (rs // opaque, gs // opaque, bs // opaque) if opaque else (0, 0, 0)
        print(f"{name0}: {im.size} covered={opaque/total:.1%} meanRGB={mean}")
        imgs.append((name0, im))

    w = sum(i.width for _, i in imgs) + PAD * (len(imgs) + 1)
    h = max(i.height for _, i in imgs) + LABEL + HEADER + PAD * 2
    sheet = Image.new("RGBA", (w, h), (26, 26, 30, 255))
    d = ImageDraw.Draw(sheet)
    d.text((PAD, 6), f"{name}  -  tier {t['num']}", fill=(245, 245, 245, 255))
    sx = PAD
    for label, c in t["swatches"]:
        d.rectangle([sx, 22, sx + 14, 36], fill=c + (255,), outline=(20, 20, 20, 255))
        d.text((sx + 18, 23), label, fill=(220, 220, 220, 255))
        sx += 18 + 8 * len(label) + 12
    x = PAD
    for name0, im in imgs:
        for yy in range(0, im.height, 12):
            for xx in range(0, im.width, 12):
                c = (54, 54, 58, 255) if ((xx // 12 + yy // 12) % 2 == 0) else (40, 40, 44, 255)
                d.rectangle([x + xx, HEADER + LABEL + yy, x + xx + 11, HEADER + LABEL + yy + 11], fill=c)
        sheet.alpha_composite(im, (x, HEADER + LABEL))
        d.text((x + 2, HEADER + 4), name0, fill=(235, 235, 235, 255))
        x += im.width + PAD
    out = os.path.join(HERE, "preview", f"sheet_{key}.png")
    sheet.save(out)
    print("sheet", os.path.abspath(out), sheet.size)


if __name__ == "__main__":
    main()