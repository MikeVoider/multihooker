"""
Compares two ways of closing the sides of the bottle.

A 1px semi-transparent glass rim lets the background through, which is what makes the sides read
as not full even when the liquid fills the neck. Two fixes:

  A  opaque glass  - the rim stays, but is solid, so nothing shows through and it still reads
                     as glass
  B  liquid to edge - no glass band at all, the liquid runs straight into the dark outline

    python show_full.py
"""
import os

from PIL import Image, ImageDraw, ImageFont

from gen_elixir_sprites import SHAPES, ELIXIRS, render, palette, OUT

BG = (24, 25, 32, 255)
FG = (240, 240, 246, 255)
DIM = (168, 178, 205, 255)

# The same silhouette with the glass band replaced by liquid.
EDGE = [
    '......CKCK......',
    '......CKKK......',
    '......OsLO......',
    '......OLLO......',
    '.....OLLLdO.....',
    '....OLLLLddO....',
    '...OlLLLLdddO...',
    '...OlLLLLdddO...',
    '...OlLLLLdddO...',
    '...OlLLLLdddO...',
    '...OlLLLLdddO...',
    '...OLLLLLdddO...',
    '...OLLLLLdddO...',
    '...OLLLLLdddO...',
    '....OLLLLddO....',
    '.....OOOOOO.....',
]

BACKGROUNDS = [('Cave / deepslate', [(58, 60, 66), (48, 50, 56)]),
               ('Grass / dirt', [(92, 132, 62), (122, 96, 68)]),
               ('Stone / bright', [(196, 196, 202), (172, 172, 178)]),
               ('Nether / dark', [(74, 42, 44), (48, 30, 32)])]


def font(size, bold=False):
    for name in (('segoeuib.ttf' if bold else 'segoeui.ttf'), 'arialbd.ttf' if bold else 'arial.ttf'):
        p = os.path.join('C:\\', 'Windows', 'Fonts', name)
        if os.path.exists(p):
            try:
                return ImageFont.truetype(p, size)
            except OSError:
                pass
    return ImageFont.load_default()


def render_variant(rows, liquid, opaque_glass):
    pal = palette(liquid)
    if opaque_glass:
        pal = dict(pal)
        pal['G'] = (222, 240, 250, 255)
        pal['H'] = (250, 254, 255, 255)
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    px = img.load()
    for y, row in enumerate(rows):
        assert len(row) == 16, (y, len(row), row)
        for x, ch in enumerate(row):
            if ch != '.':
                px[x, y] = pal[ch]
    return img


def panel(w, h, cols, seed=0):
    img = Image.new('RGB', (w, h))
    px = img.load()
    for y in range(h):
        for x in range(w):
            px[x, y] = cols[(((x // 7) + (y // 7)) + seed) % len(cols)]
    return img


def main():
    base = dict(SHAPES)['Potion']
    for i, r in enumerate(EDGE):
        assert len(r) == 16, (i, len(r), r)

    S = 7
    T = 16 * S
    gap = 18
    left = 152
    pad = 26
    head = 78
    ladder = 3 * (16 * 4 + gap) + 40

    W = left + 2 * (T + gap) + pad + ladder
    H = head + len(BACKGROUNDS) * (T + 46) + pad

    canvas = Image.new('RGBA', (W, H), BG)
    d = ImageDraw.Draw(canvas)
    d.text((pad, 12), 'Closing the sides  -  Vitality, Potion silhouette', font=font(21, True),
           fill=FG)
    d.text((left, 46), 'A: opaque glass rim', font=font(15, True), fill=FG)
    d.text((left + T + gap, 46), 'B: liquid to the edge', font=font(15, True), fill=FG)

    y = head
    for label, cols in BACKGROUNDS:
        d.text((pad, y + T // 2 - 8), label, font=font(15, True), fill=FG)
        for k, (rows, opaque) in enumerate([(base, True), (EDGE, False)]):
            x = left + k * (T + gap)
            sp = render_variant(rows, ELIXIRS[0][2], opaque)
            canvas.alpha_composite(panel(T + 8, T + 8, cols, seed=k).convert('RGBA'),
                                   (x - 4, y - 4))
            canvas.alpha_composite(sp.resize((T, T), Image.NEAREST), (x, y))
            small = sp.resize((48, 48), Image.NEAREST)
            canvas.alpha_composite(small, (x + T - small.width + 6, y + T - small.height + 6))
        y += T + 46

    # side-by-side size ladders, on the cave background
    lx = left + 2 * (T + gap) + 10
    d.text((lx, head - 26), 'at 1x / 2x / 4x', font=font(13), fill=DIM)
    for k, (rows, opaque) in enumerate([(base, True), (EDGE, False)]):
        yy = head + 6
        d.text((lx, yy - 16), 'A' if k == 0 else 'B', font=font(13, True), fill=DIM)
        sp = render_variant(rows, ELIXIRS[0][2], opaque)
        canvas.alpha_composite(panel(16 * 4 + 24, 16 * 4 + 24, BACKGROUNDS[0][1]).convert('RGBA'),
                               (lx - 4, yy - 4))
        sx = lx
        for z in (1, 2, 4):
            big = sp.resize((16 * z, 16 * z), Image.NEAREST)
            canvas.alpha_composite(big, (sx, yy + (16 * 4 - big.height)))
            sx += big.width + 10
        yy += 16 * 4 + 40

    p = os.path.join(OUT, 'FULL_COMPARE.png')
    canvas.convert('RGB').save(p)
    print('wrote', p, canvas.size)


if __name__ == '__main__':
    main()
