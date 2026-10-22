"""
Side-by-side of the three candidate palette ceilings, before any of them is applied.

    python preview_ceilings.py [ceiling ...]
"""
import importlib.util
import os
import sys

from PIL import Image, ImageDraw, ImageFont

HERE = os.path.dirname(os.path.abspath(__file__))
CUR = os.path.join(HERE, 'out')
SCRATCH = os.path.join(HERE, 'out', 'ceilings')
TIERS = ['rare', 'godly', 'divine', 'ultra_divine', 'ghost_unspoken']
FACES = ['top', 'side']


def lum(c):
    return 0.299 * c[0] + 0.587 * c[1] + 0.114 * c[2]


def cap(c, ceiling):
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


def gen_at(ceiling):
    out = os.path.join(SCRATCH, str(ceiling))
    os.makedirs(out, exist_ok=True)
    tiers = load('tiers')
    gen = load('gen_textures')
    gen.tiers = tiers
    gen.OUT = out
    for t in tiers.TIERS:
        t['trim'] = tuple(cap(c, ceiling) for c in t['trim'])
        t['wood'] = tuple(cap(c, ceiling) for c in t['wood'])
        if t.get('swatches'):
            t['swatches'] = tuple((n, cap(c, ceiling)) for n, c in t['swatches'])
    for t in tiers.TIERS:
        gen.generate(t['key'])
    return out


def main():
    ceilings = [int(x) for x in sys.argv[1:]] or [120, 150, 180]
    dirs = [(c, gen_at(c)) for c in ceilings]

    S = 2
    TW = 64 * S
    pad = 8
    labelw = 104
    cols = [('now', CUR)] + [(str(c), d) for c, d in dirs]
    W = labelw + len(FACES) * len(cols) * (TW + pad) + pad
    H = pad + 46 + len(TIERS) * (TW + 24) + pad
    out = Image.new('RGB', (W, H), (24, 25, 32))
    d = ImageDraw.Draw(out)
    try:
        f = ImageFont.truetype('C:\\Windows\\Fonts\\segoeui.ttf', 12)
        fb = ImageFont.truetype('C:\\Windows\\Fonts\\segoeuib.ttf', 16)
    except Exception:
        f = fb = ImageFont.load_default()

    d.text((pad, 8), 'Palette ceiling comparison  -  the top face is where the glare shows',
           fill=(240, 240, 246), font=fb)

    x = labelw
    for i, (label, _) in enumerate(cols):
        d.text((x + i * (TW + pad) + TW // 2 - 18, 30), label, fill=(160, 200, 255), font=f)
    for fi, face in enumerate(FACES):
        d.text((labelw + fi * len(cols) * (TW + pad) - 40, 30), face, fill=(150, 255, 170), font=f)

    y = 46 + pad
    for key in TIERS:
        d.text((pad, y + TW // 2), key, fill=(205, 215, 238), font=f)
        for fi, face in enumerate(FACES):
            for ci, (label, base) in enumerate(cols):
                p = os.path.join(base, '%s_crate_%s.png' % (key, face))
                if not os.path.exists(p):
                    continue
                im = Image.open(p).convert('RGBA').resize((TW, TW), Image.NEAREST)
                cell = Image.new('RGBA', (TW, TW), (58, 60, 70, 255))
                cell.alpha_composite(im)
                out.paste(cell.convert('RGB'),
                          (labelw + (fi * len(cols) + ci) * (TW + pad), y))
        y += TW + 24

    p = os.path.join(SCRATCH, 'CEILINGS.png')
    out.save(p)
    print('wrote', p, out.size)


if __name__ == '__main__':
    main()