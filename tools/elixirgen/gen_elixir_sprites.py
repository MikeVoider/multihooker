"""
Elixir bottle silhouettes for 16x16 item sprites.

item/generated maps a texture onto a square quad whatever shape the image is, so these are
square and laid out on the pixel grid rather than scaled from a render.

Every silhouette keeps the same pixel legend, so the liquid colours the same way in all of them
and only the shape changes:

    .  transparent      O  dark outline     G  glass rim (translucent)
    H  glass highlight  C  cork             K  cork shadow
    M  metal cap        N  metal shadow
    s  liquid surface   l  liquid highlight L  liquid body   d  liquid shadow

    python gen_elixir_sprites.py            # writes PNGs + a preview grid
"""

import os

from PIL import Image, ImageDraw

SIZE = 16
HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(HERE, 'out', 'sprites16')

OUTLINE = (43, 47, 58, 255)
# Alpha 150, not lower. At 88 the rim all but vanished over dark cave blocks and the bottle read
# as half empty even when it was not - the glass has to survive being 1px wide.
GLASS = (215, 238, 250, 150)
GLASS_HI = (248, 253, 255, 205)
CORK = (150, 106, 62, 255)
CORK_DARK = (106, 72, 40, 255)
METAL = (158, 166, 182, 255)
METAL_DARK = (104, 110, 126, 255)

SHAPES = [
    # Filled to the brim, with no glass band. Both of those were deliberate:
    #   - the liquid runs up into the neck, because empty glass up there read as a half bottle
    #   - there is no 1px glass rim, because at that width a rim is either transparent (and the
    #     background shows through, so the sides look empty) or opaque (and it becomes a white
    #     stripe down the side). Neither works at 16px; letting the liquid meet the dark outline
    #     reads as full on every background.
    ('Potion', [
        '......CKCK......',
        '......CKKK......',
        '......OsLO......',   # meniscus sits right under the cork
        '......OLLO......',
        '.....OLLLdO.....',
        '....OLLLLddO....',
        '...OlLLLLdddO...',   # widest point
        '...OlLLLLdddO...',   # highlight on the liquid
        '...OlLLLLdddO...',
        '...OlLLLLdddO...',
        '...OlLLLLdddO...',
        '...OLLLLLdddO...',
        '...OLLLLLdddO...',
        '...OLLLLLdddO...',
        '....OLLLLddO....',   # body narrows into the base
        '.....OOOOOO.....',   # bottom edge
    ]),
    ('Slim vial', [
        '.......CK.......',
        '.......CK.......',
        '......OGGO......',
        '......OGGO......',
        '......OGGO......',
        '......OGGO......',
        '.....OGGGGO.....',
        '....OGsLLdGO....',
        '....OGlLLdGO....',
        '....OGlLLdGO....',
        '....OHlLLdGO....',
        '....OHlLLdGO....',
        '....OGlLLdGO....',
        '....OGlLLdGO....',
        '....OGLLddGO....',
        '.....OOOOOO.....',
    ]),
    ('Hex vial', [
        '......NMMN......',
        '......MMMM......',
        '.....OOOOOO.....',
        '....OGGGGGGO....',
        '...OGsLLLddGO...',
        '...OlLLLdddGO...',
        '...OlLLLdddGO...',
        '...OlLLLdddGO...',
        '...OlLLLdddGO...',
        '...OlLLLdddGO...',
        '...OlLLLdddGO...',
        '...OlLLLdddGO...',
        '...OlLLLdddGO...',
        '....OGLLddGO....',
        '....OGGGGGGO....',
        '.....OOOOOO.....',
    ]),
    ('Wide flask', [
        '......CKCK......',
        '......OGGO......',
        '.....OGGGGO.....',
        '....OGsLLdGO....',
        '...OGslLLddGO...',
        '...OlLLLdddGO...',
        '...OlLLLdddGO...',
        '...OlLLLdddGO...',
        '...OlLLLdddGO...',
        '...OlLLLdddGO...',
        '...OlLLLdddGO...',
        '...OlLLLdddGO...',
        '...OlLLLdddGO...',
        '...OlLLLdddGO...',
        '....OGLLddGO....',
        '.....OOOOOO.....',
    ]),
    ('Conical', [
        '.......GG.......',
        '......OGGO......',
        '......OGGO......',
        '.....OGsLdO.....',
        '....OGslLdGO....',
        '...OGslLLddGO...',
        '...OlLLLdddGO...',
        '...OlLLLdddGO...',
        '...OlLLLdddGO...',
        '...OlLLLdddGO...',
        '...OlLLLdddGO...',
        '...OlLLLdddGO...',
        '...OlLLLdddGO...',
        '...OlLLLdddGO...',
        '....OGLLddGO....',
        '.....OOOOOO.....',
    ]),
    ('Blocky jar', [
        '....NNNNNNNN....',
        '....MMMMMMMM....',
        '....OOOOOOOO....',
        '...OGGGGGGGGO...',
        '...OGsLLLddGO...',
        '...OlLLLdddGO...',
        '...OlLLLdddGO...',
        '...OlLLLdddGO...',
        '...OlLLLdddGO...',
        '...OlLLLdddGO...',
        '...OlLLLdddGO...',
        '...OlLLLdddGO...',
        '...OlLLLdddGO...',
        '...OlLLLdddGO...',
        '....OGLLddGO....',
        '.....OOOOOO.....',
    ]),
]

ELIXIRS = [('Vitality', 'elixir_vitality', (0xE8, 0x3E, 0x34)),
           ('Fortune', 'elixir_fortune', (0xFA, 0xC8, 0x3C)),
           ('Crate Potion', 'crate_potion', (0xAE, 0x58, 0xF2))]


def mix(rgb, target, t):
    return tuple(int(round(c + (g - c) * t)) for c, g in zip(rgb, target))


def palette(liquid):
    return {
        'O': OUTLINE,
        'G': GLASS,
        'H': GLASS_HI,
        'C': CORK,
        'K': CORK_DARK,
        'M': METAL,
        'N': METAL_DARK,
        's': mix(liquid, (255, 255, 255), 0.62) + (255,),
        'l': mix(liquid, (255, 255, 255), 0.45) + (255,),
        'L': liquid + (255,),
        'd': mix(liquid, (0, 0, 0), 0.32) + (255,),
    }


def render(rows, liquid):
    pal = palette(liquid)
    assert len(rows) == SIZE, 'expected %d rows, got %d' % (SIZE, len(rows))
    img = Image.new('RGBA', (SIZE, SIZE), (0, 0, 0, 0))
    px = img.load()
    for y, row in enumerate(rows):
        assert len(row) == SIZE, 'row %d is %d wide: %r' % (y, len(row), row)
        for x, ch in enumerate(row):
            if ch != '.':
                px[x, y] = pal[ch]
    return img


def checker(w, h, s=12):
    img = Image.new('RGBA', (w, h))
    d = ImageDraw.Draw(img)
    for y in range(h):
        for x in range(w):
            d.point((x, y), fill=(62, 64, 74, 255) if ((x // s + y // s) % 2 == 0)
                    else (46, 48, 56, 255))
    return img


def main():
    os.makedirs(OUT, exist_ok=True)
    S = 8                      # preview zoom
    T = SIZE * S
    pad = 14
    left = 116
    head = 30

    W = left + len(SHAPES) * (T + pad) + pad
    H = head + len(ELIXIRS) * (T + 34) + pad

    canvas = Image.alpha_composite(Image.new('RGBA', (W, H), (24, 25, 32, 255)),
                                   checker(W, H))
    d = ImageDraw.Draw(canvas)

    for c, (name, rows) in enumerate(SHAPES):
        d.text((left + c * (T + pad) + T // 2, 8), name, fill=(245, 245, 250, 255))

    for r, (label, _item, liquid) in enumerate(ELIXIRS):
        y = head + r * (T + 34)
        d.text((10, y + T // 2 - 5), label, fill=(200, 210, 235, 255))
        for c, (name, rows) in enumerate(SHAPES):
            x = left + c * (T + pad)
            sprite = render(rows, liquid)
            canvas.alpha_composite(sprite.resize((T, T), Image.NEAREST), (x, y))
            # also show it at roughly true item size, to check it still reads
            small = sprite.resize((SIZE * 3, SIZE * 3), Image.NEAREST)
            canvas.alpha_composite(small, (x + T - small.width, y + T - small.height))

    p = os.path.join(OUT, 'SHAPE_GRID.png')
    canvas.convert('RGB').save(p)
    print('wrote', p, canvas.size)

    chosen = os.path.join(OUT, 'chosen')
    os.makedirs(chosen, exist_ok=True)
    for label, item, liquid in ELIXIRS:
        for name, rows in SHAPES:
            render(rows, liquid).save(os.path.join(chosen, '%s_%s.png'
                                                  % (item, name.lower().replace(' ', '_'))))
    print('wrote individual sprites to', chosen)


if __name__ == '__main__':
    main()
