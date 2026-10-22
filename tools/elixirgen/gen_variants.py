"""
Variant grid: four proportion variants of each of the six smooth vessels,
rendered into a single comparison sheet.

Variants are produced by transforming the same lathe profiles, so every cell is
a real surface of revolution and directly comparable to the others.
"""

import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from PIL import Image, ImageDraw

import gen_smooth_bottles as G

HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(HERE, 'out', 'smooth')

# (label, height scale, width scale, neck stretch, neck width scale)
VARIANTS = [
    ('Standard',    1.00, 1.00, 1.00, 1.00),
    ('Tall',        1.38, 0.84, 1.35, 0.94),
    ('Wide',        0.78, 1.20, 0.80, 1.08),
    ('Long neck',   1.06, 1.00, 2.30, 0.88),
]

DESIGN_ORDER = ['mc_bottle', 'mc_vial', 'mc_flask', 'mc_prism', 'mc_shard', 'mc_urn']


def transform(profile, h, r, neck, neck_r):
    """Scale the body, then stretch the thin neck region above the shoulder."""
    start = len(profile)
    for i, (rr, _) in enumerate(profile):
        if rr <= 1.75:
            start = i
            break
    base_y = profile[start][1] if start < len(profile) else 0.0
    out = []
    for i, (rr, y) in enumerate(profile):
        if i >= start and start < len(profile):
            out.append((rr * neck_r, base_y + (y - base_y) * neck))
        else:
            out.append((rr * r, y * h))
    return out


def variant_parts(design, v):
    _, h, r, neck, neck_r = v
    return [(mat, transform(profile, h, r, neck, neck_r))
            for mat, profile in G.DESIGNS[design]['parts']]


def main():
    elixir = 'vitality'
    originals = {d: list(G.DESIGNS[d]['parts']) for d in DESIGN_ORDER}

    tiles = {}
    for d in DESIGN_ORDER:
        row = []
        for v in VARIANTS:
            G.DESIGNS[d]['parts'] = variant_parts(d, v)
            row.append(G.render(elixir, d, 30, scale=13))
        tiles[d] = row
    for d in DESIGN_ORDER:
        G.DESIGNS[d]['parts'] = originals[d]

    cw = max(t.width for _, row in tiles.items() for t in row) + 12
    ch = max(t.height for _, row in tiles.items() for t in row) + 12
    left, top = 132, 62
    W = left + cw * len(DESIGN_ORDER) + 20
    H = top + ch * len(VARIANTS) + 40

    img = G.backdrop(W, H)
    d = ImageDraw.Draw(img)
    d.text((20, 14), 'Six vessels x four proportions - Elixir of Vitality, same lathe, four shapes',
           fill=(240, 242, 248, 255))

    x = left
    for d_ in DESIGN_ORDER:
        d.text((x + cw // 2, 40), G.DESIGNS[d_]['title'], fill=(190, 200, 218, 255))
        x += cw

    y = top
    for v in VARIANTS:
        d.text((20, y + ch // 2 - 6), v[0], fill=(190, 200, 218, 255))
        x = left
        for d_ in DESIGN_ORDER:
            t = tiles[d_][VARIANTS.index(v)]
            img.alpha_composite(t, (x + (cw - t.width) // 2, y + (ch - t.height) // 2))
            x += cw
        y += ch

    os.makedirs(OUT, exist_ok=True)
    path = os.path.join(OUT, 'VARIANT_GRID.png')
    img.save(path)
    print('wrote %s  %dx%d' % (path, img.width, img.height))


if __name__ == '__main__':
    main()