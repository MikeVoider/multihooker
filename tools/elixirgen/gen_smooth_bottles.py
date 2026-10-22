"""
Smooth elixir vessels: lathed surfaces-of-revolution, as a design preview.

Shape prototype for the real implementation. The Java side will lathe the SAME
profiles through NeoForge's IModelBuilder, so what you approve here is what the
game shows. Cube models could not do curves; a lathe can.

A profile is a list of (radius, y) from the base upward. Lathe it around Y and
you get a genuinely round bottle: smooth shoulders, a real curve into the neck,
no voxel steps anywhere.

Renderer notes: quads are split into two triangles and each triangle is filled
with interpolated barycentric depth, and shading uses per-vertex normals taken
from the profile tangent, so the surface is genuinely smooth rather than faceted.
"""

import math
import os

from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(HERE, 'out', 'smooth')

SEGMENTS = 28
BG = (34, 36, 43, 255)

# (colour, glossy, alpha) - alpha < 1 means the pass is blended, not written
GLASS_PALE = ((196, 220, 240), True, 0.30)
CORK = ((146, 100, 56), False, 1.0)
IRON = ((92, 98, 114), False, 1.0)
BRASS = ((182, 148, 78), False, 1.0)
WAX = ((206, 200, 186), False, 1.0)

# how far outside the liquid the glass shell sits, and how far below the rim the
# liquid is filled. Together they give a real wall thickness you can see through.
SHELL = 0.22
FILL_DROP = 0.75
BASE_LIFT = 0.10

LIQUIDS = {
    'vitality': (232, 62, 52),
    'fortune': (250, 200, 60),
    'potion': (174, 88, 242),
}

# ------------------------------------------------------------------ profiles
DESIGNS = {
    'mc_bottle': {
        'title': 'Chunky Bottle',
        'note': 'a wide stubby potion bottle, eight flat sides, small cork',
        'segments': 8,
        'parts': [
            ('LIQ', [(0.0, 0.0), (3.6, 0.0), (3.9, 0.5), (3.9, 4.2), (3.2, 5.4),
                     (1.15, 6.2), (1.0, 7.6)]),
            ('GLASS_PALE', [(1.05, 7.45), (1.25, 8.0), (1.25, 9.0), (0.0, 9.1)]),
            ('CORK', [(1.22, 8.95), (1.45, 9.45), (1.45, 10.5), (0.0, 10.6)]),
        ],
    },
    'mc_vial': {
        'title': 'Hex Vial',
        'note': 'a squat hexagonal vial with a small flat iron cap',
        'segments': 6,
        'parts': [
            ('LIQ', [(0.0, 0.0), (2.9, 0.0), (3.15, 0.5), (3.15, 6.0), (2.6, 7.0),
                     (1.05, 7.6), (0.95, 8.3)]),
            ('IRON', [(0.92, 8.2), (1.25, 8.7), (1.25, 9.9), (0.0, 10.0)]),
        ],
    },
    'mc_flask': {
        'title': 'Hex Flask',
        'note': 'a broad hexagonal foot stepping up to a small neck',
        'segments': 6,
        'parts': [
            ('LIQ', [(0.0, 0.0), (4.0, 0.0), (4.2, 0.6), (3.3, 2.0), (2.4, 3.4),
                     (1.7, 4.8), (1.0, 6.0), (0.95, 7.2)]),
            ('GLASS_PALE', [(1.0, 7.1), (1.18, 7.6), (1.18, 8.6), (0.0, 8.7)]),
            ('CORK', [(1.15, 8.55), (1.38, 8.95), (1.38, 10.1), (0.0, 10.2)]),
        ],
    },
    'mc_prism': {
        'title': 'Square Prism',
        'note': 'a four-sided bottle, dead square in plan, small cork',
        'segments': 4,
        'parts': [
            ('LIQ', [(0.0, 0.0), (3.5, 0.0), (3.6, 0.5), (3.6, 5.6), (3.0, 6.6),
                     (1.25, 7.2), (1.2, 8.0)]),
            ('CORK', [(1.18, 7.95), (1.5, 8.4), (1.5, 9.9), (0.0, 10.0)]),
        ],
    },
    'mc_shard': {
        'title': 'Pentagon Shard',
        'note': 'a five-sided crystal drawn to a fine point',
        'segments': 5,
        'parts': [
            ('LIQ', [(0.0, 0.0), (1.6, 0.0), (3.0, 1.4), (3.3, 4.0), (2.1, 6.0),
                     (1.0, 7.4), (0.0, 9.4)]),
        ],
    },
    'mc_urn': {
        'title': 'Octagonal Urn',
        'note': 'a broad-shouldered urn capped with a small iron band',
        'segments': 8,
        'parts': [
            ('LIQ', [(0.0, 0.0), (2.6, 0.0), (2.9, 0.5), (2.7, 1.4), (3.5, 2.6),
                     (3.9, 4.4), (3.5, 6.2), (2.4, 7.4), (1.35, 7.9), (1.3, 8.6)]),
            ('IRON', [(1.28, 8.5), (1.6, 9.0), (1.6, 9.8), (0.0, 9.9)]),
        ],
    },
}


# ------------------------------------------------------------------ geometry
def lathe(profile, segments=SEGMENTS, twist=0.0, bend=0.0, bend_dir=0.0):
    """Surface of revolution -> (positions, normals, triangle indices).

    Normals come from the profile tangent, averaged at each ring, which is what
    makes the shading smooth instead of faceted.

    twist and bend break the symmetry of a plain surface of revolution, which is
    what lets a corkscrew or a bent retort arm exist at all: the ring is rotated
    and pushed sideways as it climbs the profile.
    """
    n2 = []
    for i in range(len(profile)):
        r0, y0 = profile[max(0, i - 1)]
        r1, y1 = profile[min(len(profile) - 1, i + 1)]
        dr, dy = r1 - r0, y1 - y0
        nr, ny = dy, -dr
        L = math.hypot(nr, ny)
        n2.append((nr / L, ny / L) if L > 1e-9 else (0.0, 1.0))

    pos, nor, tris = [], [], []
    y_lo = profile[0][1]
    span = max(1e-6, profile[-1][1] - y_lo)
    for idx, ((r, y), (nr, ny)) in enumerate(zip(profile, n2)):
        # drift is driven by HEIGHT, not by ring index, and held at zero for the
        # first 8% so the base disc and the wall above it stay aligned
        th = max(0.0, min(1.0, (y - y_lo) / span))
        t = max(0.0, (th - 0.08) / 0.92)
        ang = twist * t
        ca, sa = math.cos(ang), math.sin(ang)
        off = bend * t
        # The sideways drift follows the twist, so twist+bend together trace a
        # helix. Twist alone is invisible on a round cross-section.
        ox = off * math.cos(ang + bend_dir)
        oz = off * math.sin(ang + bend_dir)
        for i in range(segments):
            a = 2 * math.pi * i / segments + ang
            c, s = math.cos(a), math.sin(a)
            pos.append((r * c + ox, y, r * s + oz))
            nor.append((nr * c, ny, nr * s))

    for k in range(len(profile) - 1):
        r0 = profile[k][0]
        r1 = profile[k + 1][0]
        flat = abs(profile[k + 1][1] - profile[k][1]) < 1e-9
        a, b = k * segments, (k + 1) * segments
        for i in range(segments):
            j = (i + 1) % segments
            if flat:
                # A flat ring is a cap, not a wall: fan it from the axis. Only
                # fan when the ring actually touches the axis, otherwise it is
                # a flat annulus we do not want in a bottle at all.
                if r0 < 1e-6:
                    tris.append((a, b + i, b + j))
                elif r1 < 1e-6:
                    tris.append((a + i, b, a + j))
                continue
            if r0 < 1e-6:
                tris.append((a, b + i, b + j))
            elif r1 < 1e-6:
                tris.append((a + i, b, a + j))
            else:
                tris.append((a + i, b + i, b + j))
                tris.append((a + i, b + j, a + j))
    return pos, nor, tris


def offset(profile, d, top_drop=0.0, base_lift=0.0):
    """Push a profile out (or in) radially, keeping any pole on the axis.

    base_lift raises the flat base very slightly. Without it the liquid's base
    and the glass shell's base are both at y=0, which z-fights into a pinwheel.
    """
    last = len(profile) - 1
    out = []
    for i, (r, y) in enumerate(profile):
        rr = 0.0 if r < 1e-6 else r + d
        yy = y
        if i >= last - 1:
            yy -= top_drop
        if i == 0:
            yy += base_lift
        out.append((rr, yy))
    return out


def build(elixir, design):
    """Returns (pos, nor, tris, colour, glossy, alpha) over every part."""
    pos, nor, tris, cols, gloss, alph = [], [], [], [], [], []
    # A low segment count is how a lathe turns into a Minecraft-shaped object:
    # same profile, far fewer sides. Smooth normals would hide that, so a
    # faceted design also gets one normal per triangle, which makes every face
    # shade as its own flat plane the way a voxel model does.
    seg = DESIGNS[design].get('segments', SEGMENTS)
    faceted = seg <= 12
    twist = DESIGNS[design].get('twist', 0.0)
    bend = DESIGNS[design].get('bend', 0.0)
    bend_dir = DESIGNS[design].get('bend_dir', 0.0)

    def add(profile, colour, g, a):
        base = len(pos)
        p, n, t = lathe(profile, seg, twist, bend, bend_dir)
        if faceted:
            flat = []
            for tri in t:
                acc = [0.0, 0.0, 0.0]
                for idx in tri:
                    for k in range(3):
                        acc[k] += p[idx][k]
                L = math.sqrt(acc[0] ** 2 + acc[1] ** 2 + acc[2] ** 2) or 1.0
                flat.append((acc[0] / L, acc[1] / L, acc[2] / L))
            n = flat
        pos.extend(p)
        nor.extend(n)
        tris.extend(tuple(i + base for i in f) for f in t)
        cols.extend([colour] * len(t))
        gloss.extend([g] * len(t))
        alph.extend([a] * len(t))

    for mat, profile in DESIGNS[design]['parts']:
        if mat == 'LIQ':
            # An opaque liquid would hide the glass, and glass with nothing
            # behind it looks like grey plastic. So each liquid volume gets a
            # glass shell around it and the liquid itself is inset, which is
            # what you actually see through the wall of a real bottle.
            add(offset(profile, -SHELL, top_drop=FILL_DROP, base_lift=BASE_LIFT),
                LIQUIDS[elixir], False, 1.0)
            add(offset(profile, +SHELL), GLASS_PALE[0], True, GLASS_PALE[2])
        else:
            colour, g, a = {'GLASS_PALE': GLASS_PALE, 'CORK': CORK,
                            'IRON': IRON, 'BRASS': BRASS, 'WAX': WAX}[mat]
            add(profile, colour, g, a)
    return pos, nor, tris, cols, gloss, alph


# ------------------------------------------------------------------ render
def render(elixir, design, yaw_deg, scale=34, pad=16):
    pos, nor, tris, cols, gloss, alph = build(elixir, design)
    yaw, pitch = math.radians(yaw_deg), math.radians(26.0)
    cy, sy = math.cos(yaw), math.sin(yaw)
    cp, sp = math.cos(pitch), math.sin(pitch)

    def cam(p):
        x, y, z = p[0], p[1] - 6.0, p[2]
        rx = x * cy + z * sy
        rz = -x * sy + z * cy
        return (rx, -y * cp + rz * sp, y * sp + rz * cp)

    P = [cam(p) for p in pos]
    # normals: rotate the same way, no translation
    N = []
    for n in nor:
        x, y, z = n
        rx = x * cy + z * sy
        rz = -x * sy + z * cy
        N.append((rx, -y * cp + rz * sp, y * sp + rz * cp))

    L = math.sqrt(0.40 ** 2 + 0.82 ** 2 + 0.45 ** 2)
    L = (0.40 / L, 0.82 / L, 0.45 / L)
    H = (L[0], L[1] + 1.0, L[2])
    hl = math.sqrt(sum(c * c for c in H))

    xs = [p[0] for p in P]
    ys = [p[1] for p in P]
    W = max(8, int((max(xs) - min(xs)) * scale) + pad * 2)
    Hh = max(8, int((max(ys) - min(ys)) * scale) + pad * 2)
    img = Image.new('RGBA', (W, Hh), (0, 0, 0, 0))
    px = img.load()
    zb = [[1e9] * W for _ in range(Hh)]

    cx, cyy = (max(xs) + min(xs)) / 2.0, (max(ys) + min(ys)) / 2.0
    scr = [((p[0] - cx) * scale + W / 2.0, (p[1] - cyy) * scale + Hh / 2.0, p[2])
           for p in P]

    opaque, glassy = [], []
    for t, colour, g, a in zip(tris, cols, gloss, alph):
        (opaque if a >= 0.999 else glassy).append((t, colour, g, a))
    for t, colour, g, a in opaque:
        _tri(img, zb, px, scr, N, t, colour, g, a, L, H, hl, W, Hh)
    # Blended glass, drawn far to near so the walls layer correctly.
    glassy.sort(key=lambda e: sum(scr[i][2] for i in e[0]) / 3.0, reverse=True)
    for t, colour, g, a in glassy:
        _tri(img, zb, px, scr, N, t, colour, g, a, L, H, hl, W, Hh, blend=True)
    return img


def _tri(img, zb, px, scr, N, t, colour, g, alpha, L, H, hl, W, Hh, blend=False):
    a, b, c = (scr[t[0]], scr[t[1]], scr[t[2]])
    area = (b[0] - a[0]) * (c[1] - a[1]) - (b[1] - a[1]) * (c[0] - a[0])
    if area <= 1e-9:
        return          # back-facing
    x0 = max(0, int(min(a[0], b[0], c[0])))
    x1 = min(W - 1, int(max(a[0], b[0], c[0])) + 1)
    y0 = max(0, int(min(a[1], b[1], c[1])))
    y1 = min(Hh - 1, int(max(a[1], b[1], c[1])) + 1)
    if x1 < x0 or y1 < y0:
        return
    inv = 1.0 / area
    for y in range(y0, y1 + 1):
        py = y + 0.5
        for x in range(x0, x1 + 1):
            px_ = x + 0.5
            w1 = ((px_ - a[0]) * (c[1] - a[1]) - (py - a[1]) * (c[0] - a[0])) * inv
            w2 = ((b[0] - a[0]) * (py - a[1]) - (b[1] - a[1]) * (px_ - a[0])) * inv
            w0 = 1.0 - w1 - w2
            if w0 < -1e-6 or w1 < -1e-6 or w2 < -1e-6:
                continue
            z = w0 * a[2] + w1 * b[2] + w2 * c[2]
            if z >= zb[y][x]:
                continue
            nx = w0 * N[t[0]][0] + w1 * N[t[1]][0] + w2 * N[t[2]][0]
            ny = w0 * N[t[0]][1] + w1 * N[t[1]][1] + w2 * N[t[2]][1]
            nz = w0 * N[t[0]][2] + w1 * N[t[1]][2] + w2 * N[t[2]][2]
            ln = math.sqrt(nx * nx + ny * ny + nz * nz) or 1.0
            nx, ny, nz = nx / ln, ny / ln, nz / ln
            lam = 0.26 + 0.74 * max(0.0, nx * L[0] + ny * L[1] + nz * L[2])
            spec = 0.0
            if g:
                sp2 = max(0.0, (nx * H[0] + ny * H[1] + nz * H[2]) / hl)
                spec = sp2 ** 30 * 0.9
            out = tuple(min(255, int(colour[i] * lam + 255 * spec))
                        for i in range(3))
            if blend:
                # Fresnel: glass turns brighter and more opaque where it turns
                # away from you, which is what sells it as glass rather than film.
                f = 1.0 - abs(nz)
                av = min(1.0, (alpha + (1.0 - alpha) * (f ** 1.6))
                         * (0.55 + 0.45 * lam))
                dst = px[x, y]
                sa = dst[3] / 255.0
                oa = av + sa * (1.0 - av)
                if oa <= 1e-6:
                    px[x, y] = (0, 0, 0, 0)
                else:
                    px[x, y] = tuple(
                        int((out[i] * av + dst[i] * sa * (1.0 - av)) / oa)
                        for i in range(3)) + (int(oa * 255),)
            else:
                zb[y][x] = z
                px[x, y] = out + (255,)


# ------------------------------------------------------------------ sheets
def backdrop(w, h):
    img = Image.new('RGBA', (w, h), BG)
    d = ImageDraw.Draw(img)
    for y in range(0, h, 16):
        for x in range(0, w, 16):
            if (x // 16 + y // 16) % 2 == 0:
                d.rectangle([x, y, x + 15, y + 15], fill=(43, 46, 55, 255))
    return img


NAMES = {'vitality': 'Vitality', 'fortune': 'Fortune', 'potion': 'Crate Potion'}


def sheet(design):
    spec = DESIGNS[design]
    row = [render(e, design, 30, scale=30) for e in LIQUIDS]
    spin = [render('vitality', design, y, scale=17) for y in (0, 90, 180, 270)]
    cw = max(i.width for i in row) + 24
    sw = max(i.width for i in spin) + 14
    W = cw * 3 + sw * 4 + 60
    H = max(max(i.height for i in row), max(i.height for i in spin)) + 120
    img = backdrop(W, H)
    d = ImageDraw.Draw(img)
    d.text((20, 16), '%s  -  %s' % (spec['title'], spec['note']), fill=(240, 242, 248, 255))
    x = 20
    for i, e in enumerate(LIQUIDS):
        img.alpha_composite(row[i], (x + (cw - row[i].width) // 2, 62))
        d.text((x + cw // 2, H - 22), NAMES[e], fill=(178, 186, 202, 255))
        x += cw
    x = cw * 3 + 34
    for i, y in enumerate((0, 90, 180, 270)):
        img.alpha_composite(spin[i], (x + (sw - spin[i].width) // 2, 62))
        d.text((x + sw // 2, 62 + spin[i].height + 6), '%d deg' % y, fill=(150, 158, 176, 255))
        x += sw
    return img


def main():
    os.makedirs(OUT, exist_ok=True)
    for k, spec in DESIGNS.items():
        im = sheet(k)
        im.save(os.path.join(OUT, 'smooth_%s.png' % k))
        print('   %-8s %-16s %dx%d' % (k, spec['title'], im.width, im.height))
    print('\n6 smooth vessels -> out/smooth/')


if __name__ == '__main__':
    main()