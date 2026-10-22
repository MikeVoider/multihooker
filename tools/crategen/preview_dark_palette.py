"""
Preview a darker crate palette BEFORE wiring it into the mod.

The pale lid is not a lighting problem. Every tier's `trim` carries a light tone - the metal band,
the rivets and the lid rim - and most of those are near white (rare is (234,240,250), ultra divine
is (255,246,200)). Three tiers also have near-white bodies. A near-white surface facing up catches
the full sky light and reads as glare no matter what light value it is drawn with.

This regenerates the textures with every palette tone pulled down to a luminance ceiling, writing
to a scratch folder so nothing in the mod is touched, and lays the before/after out as one sheet.

    python preview_dark_palette.py [luminance_ceiling]
"""
import importlib.util
import os
import sys

from PIL import Image, ImageDraw, ImageFont

HERE = os.path.dirname(os.path.abspath(__file__))
SCRATCH = os.path.join(HERE, 'out', 'preview_dark')
CEILING = int(sys.argv[1]) if len(sys.argv) > 1 else 150


def lum(c):
    # Palettes are stored as RGB, but a few entries carry an alpha too - only the first three
    # channels carry colour, so those are what is measured.
    r, g, b = c[0], c[1], c[2]
    return 0.299 * r + 0.587 * g + 0.114 * b


def cap(c, ceiling):
    """Pull a colour down to `ceiling` luminance, keeping its hue and any alpha it has."""
    l = lum(c)
    if l <= ceiling or l == 0:
        return c
    f = ceiling / l
    return tuple(max(0, min(255, int(round(v * f)))) for v in c[:3]) + tuple(c[3:])


def load(name):
    spec = importlib.util.spec_from_file_location(name, os.path.join(HERE, name + '.py'))
    m = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(m)
    return m


def main():
    os.makedirs(SCRATCH, exist_ok=True)

    tiers = load('tiers')
    gen = load('gen_textures')
    gen.tiers = tiers
    gen.OUT = SCRATCH

    for t in tiers.TIERS:
        # trim and wood are plain RGB(A) tuples; swatches is a tuple of (name, colour) pairs.
        t['trim'] = tuple(cap(c, CEILING) for c in t['trim'])
        t['wood'] = tuple(cap(c, CEILING) for c in t['wood'])
        if t.get('swatches'):
            t['swatches'] = tuple((n, cap(c, CEILING)) for n, c in t['swatches'])

    for t in tiers.TIERS:
        gen.generate(t['key'])

    cur_dir = os.path.join(HERE, 'out')
    S = 2
    TW = 64 * S
    pad = 10
    labelw = 128
    faces = ['top', 'side']
    rows = [t['key'] for t in tiers.TIERS]
    W = labelw + len(faces) * 2 * (TW + pad)
    H = pad + len(rows) * (TW + 26) + 34
    out = Image.new('RGB', (W, H), (24, 25, 32))
    dr = ImageDraw.Draw(out)
    try:
        f = ImageFont.truetype('C:\\Windows\\Fonts\\segoeui.ttf', 13)
        fb = ImageFont.truetype('C:\\Windows\\Fonts\\segoeuib.ttf', 15)
    except Exception:
        f = fb = ImageFont.load_default()

    dr.text((pad, 8), 'Ceiling luminance %d   -   BEFORE (left)  vs  AFTER (right)' % CEILING, fill=(240, 240, 246), font=fb)
    for i, face in enumerate(faces):
        dr.text((labelw + i * 2 * (TW + pad) + pad, 26), face, fill=(150, 200, 255), font=f)
        dr.text((labelw + i * 2 * (TW + pad) + (TW + pad) + pad, 26), face, fill=(150, 255, 170), font=f)

    y = 34 + pad
    for key in rows:
        dr.text((pad, y + TW // 2), key, fill=(200, 210, 235), font=f)
        for i, face in enumerate(faces):
            for j, base in enumerate((cur_dir, SCRATCH)):
                p = os.path.join(base, '%s_crate_%s.png' % (key, face))
                if not os.path.exists(p):
                    continue
                im = Image.open(p).convert('RGBA').resize((TW, TW), Image.NEAREST)
                cell = Image.new('RGBA', (TW, TW), (58, 60, 70, 255))
                cell.alpha_composite(im)
                out.paste(cell.convert('RGB'), (labelw + (i * 2 + j) * (TW + pad) + pad, y))
        y += TW + 26

    p = os.path.join(SCRATCH, 'DARK_PREVIEW.png')
    out.save(p)
    print('wrote', p, out.size)
    print('scratch textures in', SCRATCH)


if __name__ == '__main__':
    main()