"""
Elixir item models: 3D cube models for the three MultiHooker draughts.

Why cubes and not a rendered sprite: Punchy animates whatever is in your hand,
and it rotates it in three dimensions. A flat `item/generated` sprite becomes a
card spinning in the air; a cube model is real geometry with real depth. These
are pure JSON + PNG, so they need no model loader mod in the pack.

Design: each draught gets its OWN reliquary vessel, dark tinted glass held in a
dark iron cage, with the liquid reading as a bright glowing core. Because cube
textures are opaque, the glow is carried by the BODY boxes being the vivid
liquid colour and the cage and glass being near-black, which is what reads as
"something lit, seen through dark glass" at inventory size.

Run:  python tools/elixirgen/gen_elixir_models.py
Writes models, textures and previews into out/.
     python tools/elixirgen/gen_elixir_models.py --install
Installs all three into the mod's assets.
"""

import json
import math
import os
import sys

from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(HERE, 'out')

# ---------------------------------------------------------------- palette
# Dark iron and dark tinted glass, so the vivid liquid is the only bright thing.
GLASS = (40, 44, 58)     # dark tinted glass
METAL = (78, 82, 96)     # iron fittings
BAND = (56, 60, 72)      # darker iron, for the cage straps

ELIXIRS = {
    'vitality': {'liquid': (214, 52, 46), 'accent': (255, 168, 150)},
    'fortune':  {'liquid': (243, 186, 46), 'accent': (255, 242, 186)},
    'potion':   {'liquid': (162, 78, 232), 'accent': (226, 184, 255)},
}

# Each box is (role, (x1,y1,z1), (x2,y2,z2)) in the usual 0-16 model space.
DESIGNS = {
    'vitality': {
        'title': 'Caged Orb',
        'note': 'a near-spherical core of red held in three iron straps',
        'boxes': [
            ('metal', (4, 0, 4), (12, 2, 12)),
            ('liquid', (5, 2, 5), (11, 4, 11)),
            ('band', (3, 4, 3), (13, 6, 13)),
            ('liquid', (4, 6, 4), (12, 9, 12)),
            ('liquid', (5, 9, 5), (11, 10, 11)),
            ('glass', (7, 10, 7), (9, 13, 9)),
            ('metal', (6, 13, 6), (10, 16, 10)),
        ],
    },
    'fortune': {
        'title': 'Caged Lantern',
        'note': 'a tall gold column between two iron hoops, lantern cap on top',
        'boxes': [
            ('metal', (4, 0, 4), (12, 2, 12)),
            ('liquid', (5, 2, 5), (11, 5, 11)),
            ('band', (4, 5, 4), (12, 7, 12)),
            ('liquid', (5, 7, 5), (11, 10, 11)),
            ('band', (4, 10, 4), (12, 11, 12)),
            ('liquid', (6, 11, 6), (10, 12, 10)),
            ('glass', (7, 12, 7), (9, 14, 9)),
            ('metal', (5, 14, 5), (11, 16, 11)),
        ],
    },
    'potion': {
        'title': 'Caged Reliquary',
        'note': 'a squat violet casket under an iron crown and finial',
        'boxes': [
            ('metal', (3, 0, 3), (13, 1, 13)),
            ('liquid', (4, 1, 4), (12, 5, 12)),
            ('band', (3, 5, 3), (13, 7, 13)),
            ('liquid', (4, 7, 4), (12, 10, 12)),
            ('liquid', (5, 10, 5), (11, 11, 11)),
            ('metal', (4, 11, 4), (12, 13, 12)),
            ('glass', (7, 13, 7), (9, 15, 9)),
            ('metal', (7, 15, 7), (9, 16, 9)),
        ],
    },
}

# Standard item display transforms. Punchy overrides these per item through
# its own tuning file, so these only need to look right on their own.
DISPLAY = {
    'gui':                  {'rotation': [0, 0, 0],     'translation': [0, 0, 0],    'scale': [0.625, 0.625, 0.625]},
    'ground':               {'rotation': [0, 0, 0],     'translation': [0, 0, 0],    'scale': [0.5, 0.5, 0.5]},
    'fixed':                {'rotation': [0, 0, 0],     'translation': [0, 0, 0],    'scale': [0.75, 0.75, 0.75]},
    'thirdperson_righthand': {'rotation': [0, 90, 0],    'translation': [0, 0.25, 0], 'scale': [0.55, 0.55, 0.55]},
    'thirdperson_lefthand':  {'rotation': [0, -90, 0],   'translation': [0, 0.25, 0], 'scale': [0.55, 0.55, 0.55]},
    'firstperson_righthand': {'rotation': [0, 90, 55],   'translation': [0, 0.25, 0], 'scale': [0.5, 0.5, 0.5]},
    'firstperson_lefthand':  {'rotation': [0, -90, -55], 'translation': [0, 0.25, 0], 'scale': [0.5, 0.5, 0.5]},
}

ITEMS = {
    'vitality': 'elixir_vitality',
    'fortune': 'elixir_fortune',
    'potion': 'crate_potion',
}

ROLES = ('liquid', 'accent', 'glass', 'metal', 'band')

# Minecraft's fixed per-face shade for block models. The preview applies the
# same numbers, so what you see here is what the game lights.
FACE_SHADE = {
    'up': 1.00, 'down': 0.50,
    'north': 0.80, 'south': 0.80,
    'east': 0.60, 'west': 0.60,
}


def role_colour(key, role):
    e = ELIXIRS[key]
    return {'liquid': e['liquid'], 'accent': e['accent'],
            'glass': GLASS, 'metal': METAL, 'band': BAND}[role]


# ---------------------------------------------------------------- box uv
def box_uv(w, h, d, u, v):
    """Minecraft's box-UV layout: every face gets its own rectangle."""
    return {
        'down':  (u + d, v, u + d + w, v + d),
        'up':    (u + d + w, v, u + d + 2 * w, v + d),
        'east':  (u, v + d, u + d, v + d + h),
        'north': (u + d, v + d, u + d + w, v + d + h),
        'west':  (u + d + w, v + d, u + d + w + d, v + d + h),
        'south': (u + d + w + d, v + d, u + d + 2 * w + d, v + d + h),
    }


def pack(boxes):
    """Stack each box's UV rectangle into one atlas, column by column."""
    placed, u, v, max_w = [], 0, 0, 0
    for role, a, b in boxes:
        w, h, d = b[0] - a[0], b[1] - a[1], b[2] - a[2]
        rect_w = 2 * d + 2 * w
        placed.append((role, a, b, box_uv(w, h, d, u, v)))
        max_w = max(max_w, u + rect_w)
        v += d + h
    return placed, max_w, v


# ---------------------------------------------------------------- texture
def shade(c, f):
    return tuple(max(0, min(255, int(round(x * f)))) for x in c)


def paint_face(img, rect, role, colour, accent):
    """One face rectangle: role colour, lighter at the top, dark at the rim."""
    x1, y1, x2, y2 = rect
    w, h = x2 - x1, y2 - y1
    if w <= 0 or h <= 0:
        return
    d = ImageDraw.Draw(img)
    for i in range(h):
        f = 0.72 + 0.38 * (i / max(1, h - 1))
        d.line([(x1, y1 + i), (x2, y1 + i)], fill=shade(colour, f))
    if role == 'liquid' and w > 3 and h > 3:
        d.rectangle([x1 + 1, y1 + 1, x1 + max(1, w // 3), y1 + max(1, h // 3)],
                    fill=shade(accent, 0.95))
    if role == 'metal' and w > 4 and h > 2:
        d.line([(x1, y1 + h - 2), (x2 - 1, y1 + h - 2)], fill=shade(colour, 1.35))
    d.rectangle([x1, y1, x2 - 1, y2 - 1], outline=shade(colour, 0.45))


def build_texture(placed, w, h, key):
    colours = {r: role_colour(key, r) for r in ROLES}
    img = Image.new('RGBA', (w, h), (0, 0, 0, 0))
    for role, a, b, rects in placed:
        for rect in rects.values():
            paint_face(img, rect, role, colours[role], colours['accent'])
    return img


# ---------------------------------------------------------------- preview
def corners(face, a, b):
    x1, y1, z1 = a
    x2, y2, z2 = b
    return {
        'up':    [(x1, y2, z1), (x1, y2, z2), (x2, y2, z2), (x2, y2, z1)],
        'down':  [(x1, y1, z1), (x2, y1, z1), (x2, y1, z2), (x1, y1, z2)],
        'east':  [(x2, y1, z1), (x2, y2, z1), (x2, y2, z2), (x2, y1, z2)],
        'west':  [(x1, y1, z1), (x1, y1, z2), (x1, y2, z2), (x1, y2, z1)],
        'south': [(x1, y1, z2), (x2, y1, z2), (x2, y2, z2), (x1, y2, z2)],
        'north': [(x1, y1, z1), (x1, y2, z1), (x2, y2, z1), (x2, y1, z1)],
    }[face]


def render(key, yaw_deg, scale=20):
    """Painter's-algorithm isometric render of the boxes, in the game's shading."""
    boxes = DESIGNS[key]['boxes']
    colours = {r: role_colour(key, r) for r in ROLES}
    yaw = math.radians(yaw_deg)
    cy, sy = math.cos(yaw), math.sin(yaw)
    pitch = math.radians(30.317)

    def project(p):
        x, y, z = p
        rx = x * cy + z * sy
        rz = -x * sy + z * cy
        return rx * scale, (-y * math.cos(pitch) + rz * math.sin(pitch)) * scale

    quads = []
    for role, a, b in boxes:
        for face in FACE_SHADE:
            cs = corners(face, a, b)
            quads.append((sum(c[2] for c in cs) / 4.0,
                          [project(c) for c in cs],
                          shade(colours[role], FACE_SHADE[face])))
    quads.sort(key=lambda q: q[0])

    xs = [p[0] for q in quads for p in q[1]]
    ys = [p[1] for q in quads for p in q[1]]
    pad = 14
    img = Image.new('RGBA', (int(max(xs) - min(xs)) + pad * 2,
                            int(max(ys) - min(ys)) + pad * 2), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    ox, oy = pad - min(xs), pad - min(ys)
    for depth, pts, colour in quads:
        d.polygon([(x + ox, y + oy) for x, y in pts], fill=colour + (255,),
                  outline=shade(colour, 0.55) + (255,))
    return img


# ---------------------------------------------------------------- sheets
BG = (38, 40, 48, 255)
GRID = (48, 51, 60, 255)
NAMES = {'vitality': 'Elixir of Vitality', 'fortune': 'Elixir of Fortune',
         'potion': 'Crate Potion'}


def backdrop(w, h):
    img = Image.new('RGBA', (w, h), BG)
    d = ImageDraw.Draw(img)
    for y in range(0, h, 16):
        for x in range(0, w, 16):
            if (x // 16 + y // 16) % 2 == 0:
                d.rectangle([x, y, x + 15, y + 15], fill=GRID)
    return img


def sheet(key):
    spec = DESIGNS[key]
    hero = render(key, 35, scale=22)
    spin = [render(key, y, scale=13) for y in (0, 90, 180, 270)]
    sw = max(i.width for i in spin) + 16
    W = hero.width + sw * 4 + 60
    H = max(hero.height, max(i.height for i in spin)) + 110
    img = backdrop(W, H)
    d = ImageDraw.Draw(img)
    d.text((20, 16), spec['title'], fill=(255, 255, 255, 255))
    d.text((20, 38), spec['note'], fill=(150, 158, 176, 255))
    img.alpha_composite(hero, (20, 64))
    x = 20 + hero.width + 26
    for i, y in enumerate((0, 90, 180, 270)):
        img.alpha_composite(spin[i], (x + (sw - spin[i].width) // 2, 64))
        d.text((x + sw // 2, 64 + spin[i].height + 8), '%d deg' % y,
               fill=(140, 148, 166, 255))
        x += sw
    d.text((20, H - 22), NAMES[key], fill=(220, 226, 236, 255))
    return img


def contact():
    tiles = [(k, sheet(k)) for k in ('vitality', 'fortune', 'potion')]
    W = max(t[1].width for t in tiles) + 40
    H = sum(t[1].height + 12 for t in tiles) + 50
    img = Image.new('RGBA', (W, H), (26, 28, 34, 255))
    d = ImageDraw.Draw(img)
    d.text((20, 14), 'MultiHooker elixirs - three bespoke reliquary vessels, dark glass, glowing liquid',
           fill=(236, 238, 244, 255))
    y = 40
    for k, im in tiles:
        img.alpha_composite(im, (20, y))
        y += im.height + 12
    return img


# ---------------------------------------------------------------- build
def build(key):
    boxes = DESIGNS[key]['boxes']
    placed, aw, ah = pack(boxes)
    aw, ah = max(16, aw), max(16, ah)
    build_texture(placed, aw, ah, key).save(
        os.path.join(OUT, 'textures', '%s.png' % ITEMS[key]))
    return aw, ah


def install(assets_root):
    model_dir = os.path.join(assets_root, 'models', 'item')
    tex_dir = os.path.join(assets_root, 'textures', 'item')
    os.makedirs(model_dir, exist_ok=True)
    os.makedirs(tex_dir, exist_ok=True)
    for key, item in ITEMS.items():
        boxes = DESIGNS[key]['boxes']
        placed, aw, ah = pack(boxes)
        elements = [{
            'from': [int(v) for v in a],
            'to': [int(v) for v in b],
            'faces': {f: {'uv': [int(v) for v in r], 'texture': '#bottle'}
                      for f, r in rects.items()},
        } for role, a, b, rects in placed]
        build_texture(placed, max(16, aw), max(16, ah), key).save(
            os.path.join(tex_dir, '%s.png' % item))
        with open(os.path.join(model_dir, '%s.json' % item), 'w') as fh:
            json.dump({'parent': 'minecraft:block/block',
                       'textures': {'bottle': 'multihooker:item/%s' % item},
                       'display': DISPLAY,
                       'elements': elements}, fh, indent=2)
        print('   %-16s <- %s' % (item, DESIGNS[key]['title']))


def main():
    os.makedirs(os.path.join(OUT, 'textures'), exist_ok=True)
    print('atlas sizes:')
    for k in ITEMS:
        aw, ah = build(k)
        im = sheet(k)
        im.save(os.path.join(OUT, 'preview_%s.png' % k))
        print('   %-9s %-18s atlas %dx%d  preview %dx%d'
              % (k, DESIGNS[k]['title'], aw, ah, im.width, im.height))
    contact().save(os.path.join(OUT, 'ALL_ELIXIRS.png'))
    print('\nwrote 3 bespoke vessels')

    if len(sys.argv) > 1 and sys.argv[1] == '--install':
        root = os.path.abspath(os.path.join(HERE, '..', '..', 'src', 'main', 'resources',
                                           'assets', 'multihooker'))
        print('\ninstalling into %s' % root)
        install(root)


if __name__ == '__main__':
    main()