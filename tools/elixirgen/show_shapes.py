"""
Big presentation sheet for the elixir bottle silhouettes.

    python show_shapes.py
"""
import os

from PIL import Image, ImageDraw, ImageFont

from gen_elixir_sprites import SHAPES, ELIXIRS, render, checker, OUT

BG = (24, 25, 32, 255)
FG = (240, 240, 246, 255)
DIM = (168, 178, 205, 255)


def font(size, bold=False):
    for name in (('segoeuib.ttf' if bold else 'segoeui.ttf'), 'arialbd.ttf' if bold else 'arial.ttf'):
        p = os.path.join('C:\\', 'Windows', 'Fonts', name)
        if os.path.exists(p):
            try:
                return ImageFont.truetype(p, size)
            except OSError:
                pass
    return ImageFont.load_default()


def main():
    S = 10
    T = 16 * S
    gap = 10
    cellw = 3 * T + 2 * gap
    cellh = T + 46
    cols, rows = 3, 2
    pad = 26

    strip_h = 150
    W = pad * 2 + cols * cellw + (cols - 1) * 34
    H = pad + 40 + rows * cellh + 26 + strip_h + pad

    canvas = Image.alpha_composite(Image.new('RGBA', (W, H), BG), checker(W, H, 16))
    d = ImageDraw.Draw(canvas)

    f_title = font(22, True)
    f_head = font(19, True)
    f_sub = font(14)
    f_small = font(12)

    d.text((pad, 14), 'Elixir bottles - 16x16 item sprites', font=f_title, fill=FG)

    y0 = pad + 40
    for i, (name, rowsdef) in enumerate(SHAPES):
        c, r = i % cols, i // cols
        x = pad + c * (cellw + 34)
        y = y0 + r * (cellh + 26)
        d.text((x + 2, y), name, font=f_head, fill=FG)
        for j, (label, item, liquid) in enumerate(ELIXIRS):
            vx = x + j * (T + gap)
            canvas.alpha_composite(render(rowsdef, liquid).resize((T, T), Image.NEAREST), (vx, y + 30))
            d.text((vx + 2, y + 34 + T), label, font=f_sub, fill=DIM)

    # readability strip: every shape at 1x, 2x and 4x, so small-size legibility can be judged
    sy = y0 + rows * (cellh + 26) + 6
    d.text((pad, sy), 'At true item size (1x), 2x and 4x - Vitality', font=f_head, fill=FG)
    sx = pad
    for name, rowsdef in SHAPES:
        sp = render(rowsdef, ELIXIRS[0][2])
        for z in (1, 2, 4):
            big = sp.resize((16 * z, 16 * z), Image.NEAREST)
            canvas.alpha_composite(big, (sx, sy + 34 + (64 - big.height)))
            sx += big.width + 8
        sx += 18
        d.text((sx - 14, sy + 34 + 70), name, font=f_small, fill=DIM)

    p = os.path.join(OUT, 'SHAPE_SHEET.png')
    canvas.convert('RGB').save(p)
    print('wrote', p, canvas.size)


if __name__ == '__main__':
    main()
