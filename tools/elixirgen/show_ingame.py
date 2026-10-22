"""
Shows the elixir sprites the way the game will actually present them: composited over the
backgrounds they will sit on. Transparency is what made these read as half empty, so judging
them on a flat checkerboard is misleading.

    python show_ingame.py
"""
import os

from PIL import Image, ImageDraw, ImageFont

from gen_elixir_sprites import SHAPES, ELIXIRS, render, OUT

# Approximations of blocks the item is likely to be held or displayed against.
BACKGROUNDS = [
    ('Cave / deepslate', [(58, 60, 66), (48, 50, 56)]),
    ('Grass / dirt', [(92, 132, 62), (122, 96, 68)]),
    ('Stone / bright', [(196, 196, 202), (172, 172, 178)]),
    ('Nether / dark', [(74, 42, 44), (48, 30, 32)]),
]

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


def panel(w, h, cols, seed=0):
    img = Image.new('RGB', (w, h))
    px = img.load()
    for y in range(h):
        for x in range(w):
            band = ((x // 7) + (y // 7)) % 2
            px[x, y] = cols[(band + seed) % len(cols)]
    return img


def main():
    shape = dict(SHAPES)['Potion']
    S = 7
    T = 16 * S
    gap = 16
    left = 150
    pad = 26
    head = 46

    # three scales per background: big, GUI-ish (2x), and true 1x
    W = left + 3 * (T + gap) + pad + 3 * (16 * 3 + gap)
    H = head + len(BACKGROUNDS) * (T + 44) + pad

    canvas = Image.new('RGBA', (W, H), BG)
    d = ImageDraw.Draw(canvas)
    f_title = font(21, True)
    f_head = font(15, True)
    f_sub = font(13)
    d.text((pad, 12), 'Filled bottles over real backgrounds  -  Potion silhouette',
           font=f_title, fill=FG)

    y = head
    for label, cols in BACKGROUNDS:
        d.text((pad, y + T // 2 - 8), label, font=f_head, fill=FG)
        x = left
        for j, (elabel, item, liquid) in enumerate(ELIXIRS):
            sp = render(shape, liquid)
            bgp = panel(T + 8, T + 8, cols, seed=j).convert('RGBA')
            canvas.alpha_composite(bgp, (x - 4, y - 4))
            canvas.alpha_composite(sp.resize((T, T), Image.NEAREST), (x, y))
            d.text((x + 2, y + T + 6), elabel, font=f_sub, fill=DIM)
            x += T + gap

        # size ladder on a mid background
        d.text((x + 4, y + 4), '1x / 2x / 3x', font=f_sub, fill=DIM)
        sx = x + 4
        bgp = panel(16 * 3 + 40, 16 * 3 + 40, cols).convert('RGBA')
        for z in (1, 2, 3):
            sp = render(shape, ELIXIRS[0][2])
            canvas.alpha_composite(sp.resize((16 * z, 16 * z), Image.NEAREST),
                                   (sx, y + (16 * 3 - 16 * z)))
            sx += 16 * z + gap
        y += T + 44

    p = os.path.join(OUT, 'INGAME_CHECK.png')
    canvas.convert('RGB').save(p)
    print('wrote', p, canvas.size)


if __name__ == '__main__':
    main()
