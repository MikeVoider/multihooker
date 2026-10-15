"""Compose a zoomed contact sheet of the generated crate textures (grid layout).

Works for any texture resolution; wraps into rows so the sheet stays a sane
size even now that the art is 64x64.
"""
import os

from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(HERE, "out")
S = 4  # zoom factor
COLS = 6
PAD = 8
LABEL = 14


def main():
    names = sorted(n for n in os.listdir(OUT) if n.endswith(".png"))
    if not names:
        print("no textures in", OUT)
        return
    imgs = [Image.open(os.path.join(OUT, n)).convert("RGBA") for n in names]
    cw = max(i.width for i in imgs) * S
    ch = max(i.height for i in imgs) * S
    rows = (len(imgs) + COLS - 1) // COLS
    W = COLS * (cw + PAD) + PAD
    H = rows * (ch + LABEL + PAD) + PAD
    sheet = Image.new("RGBA", (W, H), (30, 30, 34, 255))
    d = ImageDraw.Draw(sheet)
    for i, (im, n) in enumerate(zip(imgs, names)):
        col = i % COLS
        row = i // COLS
        x = PAD + col * (cw + PAD)
        y = PAD + row * (ch + LABEL + PAD)
        for yy in range(0, cw, 8):
            for xx in range(0, ch, 8):
                c = (60, 60, 64, 255) if ((xx // 8 + yy // 8) % 2 == 0) else (44, 44, 48, 255)
                d.rectangle([x + xx, y + LABEL + yy, x + xx + 7, y + LABEL + yy + 7], fill=c)
        big = im.resize((im.width * S, im.height * S), Image.NEAREST)
        sheet.alpha_composite(big, (x, y + LABEL))
        d.text((x + 2, y + 1), n.replace(".png", ""), fill=(230, 230, 230, 255))
    os.makedirs(os.path.join(HERE, "preview"), exist_ok=True)
    p = os.path.join(HERE, "preview", "textures_zoom.png")
    sheet.save(p)
    print("preview", os.path.abspath(p), sheet.size)


if __name__ == "__main__":
    main()
