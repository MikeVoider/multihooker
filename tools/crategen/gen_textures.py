"""Generate 64x64 crate textures for every MultiHooker crate tier.

Overhauled look: a banded shipping crate. Riveted metal bands along the top and
bottom plus L-shaped metal corner brackets, wood plank slats between them, the
tier name ENGRAVED into the wood (carved groove + lit lip, no plaque), and heavy
sea encrustation (barnacles, shells, coral, seaweed) around the edges.

Style rules:
  * metal uses the tier's trim palette, wood the tier's wood palette.
  * light comes from the top-left: lit top edges, shadowed bottoms.
  * the side texture's top band doubles as the lid rim: the lid's 3px-tall
    sides sample just that strip (see build_crate.py), so they read as a clean
    metal band instead of a squashed smear.
  * the crate is UNLOCKED by right-clicking it with the AquaKey, so the
    front face carries no lock, keyhole, key art or handle - just the
    engraved tier name, the same as the other sides.
  * the lid top is a plain riveted metal rim.

Output: tools/crategen/out/<tier>_crate_{side,front,top}.png
Usage:  python gen_textures.py [tier]   (no arg = all tiers)

IMPORTANT: the crate block uses the "cutout" render type, so ANY transparent
pixel in a texture becomes a literal hole you can see through. Every texture
here is authored FULLY OPAQUE.
"""
import math
import os
import random
import sys

from PIL import Image

import tiers

# 64x64 art; every coordinate below is in this 64-space.
SIZE = 64
# Pixels per font bit for the engraved tier name, and the gap (in pixels)
# between glyphs. The active font is chosen just below the font tables; run
# tools/crategen/make_size_compare.py to see the alternatives side by side.
TEXT_SCALE = 2
GLYPH_GAP = 2
HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(HERE, "out")

# sea growth colours (shared across tiers)
BARN_L = (236, 232, 222)
BARN_M = (203, 197, 183)
BARN_D = (150, 142, 126)
BARN_HOLE = (84, 80, 70)
SHELL_L = (250, 236, 230)
SHELL_M = (232, 197, 192)
SHELL_D = (196, 150, 148)
CORAL_L = (250, 150, 150)
CORAL_M = (230, 110, 110)
CORAL_D = (186, 72, 78)
WEED_L = (152, 194, 100)
WEED_M = (104, 152, 74)
WEED_D = (62, 106, 52)

# Bitmap fonts, drawn at TEXT_SCALE x TEXT_SCALE pixels per bit with GLYPH_GAP
# pixels between glyphs. FONT_4X6 reads a little better; FONT_3X5 is narrower.
# FONT picks the active one; tools/crategen/make_size_compare.py renders the
# alternatives side by side.
FONT_4X6 = {
    "A": ["0110", "1001", "1001", "1111", "1001", "1001"],
    "B": ["1110", "1001", "1110", "1001", "1001", "1110"],
    "C": ["0111", "1000", "1000", "1000", "1000", "0111"],
    "D": ["1110", "1001", "1001", "1001", "1001", "1110"],
    "E": ["1111", "1000", "1110", "1000", "1000", "1111"],
    "F": ["1111", "1000", "1110", "1000", "1000", "1000"],
    "G": ["0111", "1000", "1000", "1011", "1001", "0111"],
    "H": ["1001", "1001", "1111", "1001", "1001", "1001"],
    "I": ["1110", "0100", "0100", "0100", "0100", "1110"],
    "J": ["0011", "0001", "0001", "0001", "1001", "0110"],
    "K": ["1001", "1010", "1100", "1100", "1010", "1001"],
    "L": ["1000", "1000", "1000", "1000", "1000", "1111"],
    "M": ["1001", "1111", "1111", "1001", "1001", "1001"],
    "N": ["1001", "1101", "1101", "1011", "1001", "1001"],
    "O": ["0110", "1001", "1001", "1001", "1001", "0110"],
    "P": ["1110", "1001", "1110", "1000", "1000", "1000"],
    "Q": ["0110", "1001", "1001", "1011", "0110", "0001"],
    "R": ["1110", "1001", "1110", "1100", "1010", "1001"],
    "S": ["0111", "1000", "0110", "0001", "0001", "1110"],
    "T": ["1111", "0100", "0100", "0100", "0100", "0100"],
    "U": ["1001", "1001", "1001", "1001", "1001", "0110"],
    "V": ["1001", "1001", "1001", "1001", "1001", "0110"],
    "W": ["1001", "1001", "1001", "1111", "1111", "1001"],
    "X": ["1001", "1001", "0110", "0110", "1001", "1001"],
    "Y": ["1001", "1001", "0110", "0100", "0100", "0100"],
    "Z": ["1111", "0001", "0110", "1000", "1000", "1111"],
    " ": ["0000", "0000", "0000", "0000", "0000", "0000"],
}
FONT_3X5 = {
    "A": ["010", "101", "111", "101", "101"],
    "B": ["110", "101", "110", "101", "110"],
    "C": ["011", "100", "100", "100", "011"],
    "D": ["110", "101", "101", "101", "110"],
    "E": ["111", "100", "110", "100", "111"],
    "F": ["111", "100", "110", "100", "100"],
    "G": ["011", "100", "101", "101", "011"],
    "H": ["101", "101", "111", "101", "101"],
    "I": ["111", "010", "010", "010", "111"],
    "J": ["001", "001", "001", "101", "010"],
    "K": ["101", "101", "110", "101", "101"],
    "L": ["100", "100", "100", "100", "111"],
    "M": ["101", "111", "111", "101", "101"],
    "N": ["111", "101", "101", "101", "101"],
    "O": ["010", "101", "101", "101", "010"],
    "P": ["110", "101", "110", "100", "100"],
    "Q": ["010", "101", "101", "111", "011"],
    "R": ["110", "101", "110", "101", "101"],
    "S": ["011", "100", "010", "001", "110"],
    "T": ["111", "010", "010", "010", "010"],
    "U": ["101", "101", "101", "101", "011"],
    "V": ["101", "101", "101", "101", "010"],
    "W": ["101", "101", "111", "111", "101"],
    "X": ["101", "101", "010", "101", "101"],
    "Y": ["101", "101", "010", "010", "010"],
    "Z": ["111", "001", "010", "100", "111"],
    " ": ["000", "000", "000", "000", "000"],
}
FONT = FONT_3X5


def clamp(v):
    return max(0, min(255, int(v)))


def shade(c, n):
    return (clamp(c[0] + n), clamp(c[1] + n), clamp(c[2] + n), 255)


def mix(a, b, t):
    return (clamp(a[0] + (b[0] - a[0]) * t), clamp(a[1] + (b[1] - a[1]) * t), clamp(a[2] + (b[2] - a[2]) * t), 255)


def lum(c):
    return 0.299 * c[0] + 0.587 * c[1] + 0.114 * c[2]


def put(img, x, y, c):
    if 0 <= x < SIZE and 0 <= y < SIZE:
        img.putpixel((int(x), int(y)), c)


def rect(img, x0, y0, x1, y1, c):
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            put(img, x, y, c)


def darken(img, x, y, n):
    """Push an existing pixel's brightness down (and warm it slightly) without
    changing its alpha. Used for ambient-occlusion / cast shadows so they read
    on top of whatever wood or metal is already there."""
    if 0 <= x < SIZE and 0 <= y < SIZE:
        r, g, b, a = img.getpixel((int(x), int(y)))
        img.putpixel((int(x), int(y)), (clamp(r + n), clamp(g + n), clamp(b + n), a))


def ao_rect(img, x0, y0, x1, y1, n):
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            darken(img, x, y, n)


def fbm_noise(img, rnd, amount=7, scale=5):
    """A little per-pixel weathering so large flat areas don't look like plastic.
    Deliberately gentle: the block faces are small, so loud noise just muddies."""
    for y in range(SIZE):
        for x in range(SIZE):
            if rnd.random() < 0.16:
                n = rnd.randint(-amount, amount)
                darken(img, x, y, n)


# ---------------------------------------------------------------- wood ------
def wood_block(img, x0, y0, x1, y1, wood, rnd, plank_h=10):
    """Horizontal planks: lit top edge, shadowed bottom edge, seam above,
    a few vertical grain streaks."""
    l, m, d, seam = wood
    ys = list(range(y0, y1 + 1, plank_h))
    for i, py0 in enumerate(ys):
        py1 = min(y1, py0 + plank_h - 1)
        base = m if i % 2 == 0 else shade(m, -7)
        for yy in range(py0, py1 + 1):
            for xx in range(x0, x1 + 1):
                put(img, xx, yy, base)
        for xx in range(x0, x1 + 1):
            put(img, xx, py0, shade(l, 8))
            put(img, xx, py1, shade(d, -12))
        if py0 > y0:
            for xx in range(x0, x1 + 1):
                put(img, xx, py0 - 1, seam)
        for _ in range(max(2, (x1 - x0) // 14)):
            gx = rnd.randint(x0 + 1, x1 - 1)
            gy = rnd.randint(py0 + 1, max(py0 + 1, py1 - 1))
            for k in range(rnd.randint(2, 7)):
                if gy + k <= py1:
                    put(img, gx, gy + k, shade(base, -7))


def wood_post(img, x0, x1, wood, rnd):
    """A vertical corner post: a slightly darker wood tone with a lit left edge
    and a shadowed right edge (so it reads as standing proud of the slats),
    plus faint vertical grain. Drawn over the slats, before the metal bands."""
    _, m, d, _ = wood
    base = mix(m, d, 0.5)
    for y in range(SIZE):
        for x in range(x0, x1 + 1):
            put(img, x, y, base)
    for y in range(SIZE):
        put(img, x0, y, shade(base, 18))
        put(img, x0 + 1, y, shade(base, 8))
        put(img, x1, y, shade(base, -20))
        put(img, x1 - 1, y, shade(base, -10))
    for _ in range(9):
        gx = rnd.randint(x0 + 2, max(x0 + 2, x1 - 2))
        gy = rnd.randint(2, SIZE - 3)
        for k in range(rnd.randint(4, 14)):
            if gy + k < SIZE:
                put(img, gx, gy + k, shade(base, -6))


# --------------------------------------------------------------- metal ------
def metal_band(img, x0, y0, x1, y1, trim):
    """A band of metal with a vertical light-to-dark gradient, a bright specular
    line near the top, darker near the bottom, and lifted left/right edges."""
    if x1 < x0 or y1 < y0:
        return
    tl, tm, td = trim
    h = max(1, y1 - y0)
    for yy in range(y0, y1 + 1):
        t = (yy - y0) / h
        c = mix(tl, td, min(1.0, t * 0.9))
        if h >= 3 and abs(t - 0.22) < 1.0 / h:      # specular sheen
            c = shade(c, 20)
        for xx in range(x0, x1 + 1):
            put(img, xx, yy, c)
    for xx in range(x0, x1 + 1):
        put(img, xx, y0, shade(tl, 26))
        put(img, xx, y1, shade(td, -26))
    if h >= 3:
        for xx in range(x0, x1 + 1):
            put(img, xx, y1 - 1, shade(td, -12))
    for yy in range(y0, y1 + 1):
        put(img, x0, yy, shade(tl, 14))
        put(img, x1, yy, shade(td, -18))


def rivet(img, x, y, trim, r=2):
    tl, tm, td = trim
    for dy in range(-r, r + 1):
        for dx in range(-r, r + 1):
            d = math.hypot(dx, dy)
            if d <= r:
                put(img, x + dx, y + dy, tm if d < r - 0.5 else td)
    put(img, x - 1, y - 1, shade(tl, 28))
    put(img, x, y, shade(tl, 12))


# ---------------------------------------------------------- sea growth ------
def barnacle(img, cx, cy, r, seed=0):
    """A clean, readable barnacle: a shell of radial plates that brighten toward
    the centre, a small dark aperture set slightly off-centre, a rim light on the
    upper-left and a contact shadow on the wood to the lower-right."""
    rnd = random.Random(seed)
    plates = rnd.choice((5, 6, 7))
    phase = rnd.random() * math.tau
    step = math.tau / plates
    apx, apy = r * 0.12, r * 0.18           # aperture sits off-centre
    ap_r = max(1.0, r * 0.40)
    # 1) contact shadow cast onto the wood, opposite the light
    for dy in range(-r - 1, r + 3):
        for dx in range(-r - 1, r + 3):
            if r < math.hypot(dx - 1, dy - 1) <= r + 1.3:
                darken(img, cx + dx, cy + dy, -38)
    # 2) the shell
    for dy in range(-r, r + 1):
        for dx in range(-r, r + 1):
            d = math.hypot(dx, dy)
            if d > r + 0.35:
                continue
            w = ((math.atan2(dy, dx) + phase) % math.tau) / step
            base = mix(BARN_D, BARN_L, max(0.0, min(1.0, 1.0 - d / max(1.0, r))))
            c = shade(base, 11 if int(w) % 2 else -11)         # plate shading
            if min(w, plates - w) < 0.11:                      # groove between plates
                c = shade(c, -42)
            if d > r - 0.6:                                    # outer rim
                c = shade(c, -16)
            da = math.hypot(dx - apx, dy - apy)
            if da <= ap_r:                                     # the opening
                c = mix(BARN_HOLE, (0, 0, 0), 0.4 * (1.0 - da / ap_r))
            if dx + dy < -r * 0.45:                            # rim light / shade
                c = mix(c, (255, 255, 255), 0.22)
            elif dx + dy > r * 0.45:
                c = shade(c, -18)
            put(img, cx + dx, cy + dy, c)


def barnacle_cluster(img, cx, cy, r, seed):
    rnd = random.Random(seed)
    barnacle(img, cx, cy, r, seed)
    for _ in range(rnd.randint(1, 2)):
        ox = cx + rnd.randint(-r, r)
        oy = cy + rnd.randint(-r, r)
        barnacle(img, ox, oy, max(2, r - 1), rnd.randint(0, 99999))


def shell(img, cx, cy, r):
    for dy in range(-r, 1):
        for dx in range(-r, r + 1):
            d = math.hypot(dx, dy)
            if d <= r:
                c = SHELL_D if ((dx + dy) % 2 == 0) else SHELL_M
                if d >= r - 1:
                    c = SHELL_D
                if d <= 1:
                    c = SHELL_L
                put(img, cx + dx, cy + dy, c)
    for i in range(-r // 2, r // 2 + 1):
        put(img, cx + i, cy - r + 1, SHELL_D)
    put(img, cx, cy - r + 2, SHELL_L)


def coral(img, x, y, h, dirx):
    for i in range(h):
        put(img, x, y - i, CORAL_D)
        put(img, x + 1, y - i, CORAL_M)
        put(img, x + 2, y - i, CORAL_D)
    for i in range(2, h - 1, 4):
        put(img, x + dirx, y - i, CORAL_L)
        put(img, x + dirx * 2, y - i - 1, CORAL_M)
        put(img, x + dirx * 2, y - i - 2, CORAL_D)


def seaweed(img, x, y, h, dirx):
    for i in range(h):
        yy = y - i
        xx = x + (dirx if (i // 3) % 2 else 0)
        put(img, xx, yy, WEED_D)
        put(img, xx + 1, yy, WEED_M)
        if i % 3 == 2:
            put(img, xx - dirx, yy, WEED_L)


def prop_box(kind, x, y, par):
    """Conservative bounding box (x0, y0, x1, y1) for the growth about to be
    drawn at (x, y), so an entire prop can be kept clear of the tier name."""
    if kind == "barn":
        r = par
        return (x - 2 * r - 2, y - 2 * r - 2, x + 2 * r + 2, y + 2 * r + 2)
    if kind == "shell":
        r = par
        return (x - r, y - r, x + r, y + 1)
    if kind == "weed":
        h = par
        return (x - 2, y - h, x + 3, y + 1)
    h = par                                   # coral
    return (x - 3, y - h - 3, x + 4, y + 1)


def scatter_encrust(img, rnd, keepout, count):
    """Heavy, non-overlapping sea growth. A prop is placed only when its WHOLE
    footprint clears every keepout box (the engraved name), so the growth can
    never creep over or cover the tier name."""
    placed = []
    tries = 0
    while len(placed) < count and tries < 800:
        tries += 1
        x = rnd.randint(4, SIZE - 5)
        y = rnd.randint(3, SIZE - 4)
        kind = rnd.choices(("barn", "shell", "weed", "coral"), weights=(6, 2, 3, 2))[0]
        if kind == "barn":
            par = rnd.choice((2, 3, 4))
        elif kind == "shell":
            par = rnd.choice((3, 4))
        elif kind == "weed":
            par = rnd.randint(8, 14)
        else:
            par = rnd.randint(6, 10)
        bx0, by0, bx1, by1 = prop_box(kind, x, y, par)
        # 1px pad so growth never touches the carved grooves either
        if any(bx0 <= kx1 + 1 and bx1 >= kx0 - 1 and by0 <= ky1 + 1 and by1 >= ky0 - 1 for (kx0, ky0, kx1, ky1) in keepout):
            continue
        if any(abs(x - px) < 10 and abs(y - py) < 10 for px, py in placed):
            continue
        placed.append((x, y))
        if kind == "barn":
            barnacle_cluster(img, x, y, par, rnd.randint(0, 99999))
        elif kind == "shell":
            shell(img, x, y, par)
        elif kind == "weed":
            seaweed(img, x, y, par, rnd.choice((-1, 1)))
        else:
            coral(img, x, y, par, rnd.choice((-1, 1)))


# ---------------------------------------------------------------- text ------
def carve_text(img, text, cx, cy, groove, lip):
    """Carve text into wood: a dark groove with a lit lip along the bottom.
    Works with any fixed-width bitmap font (glyph width/height come from the
    font table). Returns the (x0, y0, x1, y1) box it occupies."""
    glyphs = [FONT.get(ch, FONT[" "]) for ch in text]
    gw = len(glyphs[0][0])
    gh = len(glyphs[0])
    w = gw * len(text) * TEXT_SCALE + GLYPH_GAP * (len(text) - 1)
    x0 = cx - w // 2
    y0 = cy - (gh * TEXT_SCALE) // 2
    pix = []
    for ci, glyph in enumerate(glyphs):
        pen = x0 + ci * (gw * TEXT_SCALE + GLYPH_GAP)
        for gy, rowstr in enumerate(glyph):
            for gx, bit in enumerate(rowstr):
                if bit == "1":
                    pix.append((pen + gx * TEXT_SCALE, y0 + gy * TEXT_SCALE))
    for px, py in pix:                       # lit lip sits one px below strokes
        rect(img, px, py + TEXT_SCALE, px + TEXT_SCALE - 1, py + TEXT_SCALE, lip)
    for px, py in pix:                       # dark groove on top
        rect(img, px, py, px + TEXT_SCALE - 1, py + TEXT_SCALE - 1, groove)
    return (x0, y0, x0 + w - 1, y0 + gh * TEXT_SCALE - 1)


def engrave_name(img, tier, wood):
    """Engrave the tier name into the wood; returns a list of keepout boxes,
    one per line, so sea growth can still fill the gaps beside short lines."""
    l, m, d, seam = wood
    if lum(m) > 110:
        groove = mix(m, (34, 26, 18), 0.72)
        lip = mix(m, (255, 250, 240), 0.35)
    else:
        groove = mix(m, (235, 238, 245), 0.70)
        lip = mix(m, (10, 8, 14), 0.55)
    words = tier["words"]
    if len(words) == 1:
        return [carve_text(img, words[0], 32, 32, groove, lip)]
    # two lines, spaced to grow/shrink with the glyph height
    off = 3 * TEXT_SCALE + 2
    return [
        carve_text(img, words[0], 32, 32 - off, groove, lip),
        carve_text(img, words[1], 32, 32 + off, groove, lip),
    ]


# --------------------------------------------------------------- faces ------
def crate_face(tier, seed, which):
    rnd = random.Random(seed)
    wood = tier["wood"]
    trim = tier["trim"]
    img = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 255))
    # thinner slats (plank_h=7) so the wood reads as a slatted crate
    wood_block(img, 0, 0, SIZE - 1, SIZE - 1, wood, rnd, plank_h=7)

    if which == "top":
        # plain riveted lid: full metal rim + rivets (the lock now lives on the
        # front face, so the lid top stays clean)
        fbm_noise(img, rnd)
        metal_band(img, 0, 0, SIZE - 1, 9, trim)
        metal_band(img, 0, SIZE - 10, SIZE - 1, SIZE - 1, trim)
        metal_band(img, 0, 0, 9, SIZE - 1, trim)
        metal_band(img, SIZE - 10, 0, SIZE - 1, SIZE - 1, trim)
        # AO in the recess between the outer rim and the inner panel
        ao_rect(img, 10, 10, SIZE - 11, 11, -30)
        ao_rect(img, 10, 10, 11, SIZE - 11, -30)
        ao_rect(img, 10, SIZE - 12, SIZE - 11, SIZE - 11, -14)
        ao_rect(img, SIZE - 12, 10, SIZE - 11, SIZE - 11, -14)
        for rx, ry in ((10, 5), (22, 5), (42, 5), (54, 5), (10, 58), (22, 58), (42, 58), (54, 58), (5, 20), (5, 44), (58, 20), (58, 44)):
            rivet(img, rx, ry, trim, 2)
        # keep the middle of the lid clear so the growth stays around the rim
        keepout = [(14, 14, 49, 49)]
    else:
        # Slatted crate: wooden corner posts down each edge, the thinner plank
        # slats between them, metal straps top and bottom, L corner brackets
        # capping the posts, and the engraved name.
        # posts are 6px wide so the engraved names (up to 53px) still sit fully
        # on the slats between them
        post = 6
        wood_post(img, 0, post - 1, wood, rnd)
        wood_post(img, SIZE - post, SIZE - 1, wood, rnd)
        fbm_noise(img, rnd)
        # soft cast shadows: the top band over the slats, and the posts onto
        # the slats beside them (top-lit, so the shadow falls below/right)
        ao_rect(img, post, 13, SIZE - post - 1, 16, -30)
        ao_rect(img, post, 13, post + 1, SIZE - 15, -26)
        ao_rect(img, SIZE - post - 2, 13, SIZE - post - 1, SIZE - 15, -16)
        ao_rect(img, 0, SIZE - 17, SIZE - 1, SIZE - 15, -18)
        metal_band(img, 0, 0, SIZE - 1, 13, trim)
        metal_band(img, 0, SIZE - 14, SIZE - 1, SIZE - 1, trim)
        leg, reach = 5, 20
        metal_band(img, 0, 0, leg - 1, reach - 1, trim)
        metal_band(img, 0, 0, reach - 1, leg - 1, trim)
        metal_band(img, SIZE - leg, 0, SIZE - 1, reach - 1, trim)
        metal_band(img, SIZE - reach, 0, SIZE - 1, leg - 1, trim)
        metal_band(img, 0, SIZE - reach, leg - 1, SIZE - 1, trim)
        metal_band(img, 0, SIZE - leg, reach - 1, SIZE - 1, trim)
        metal_band(img, SIZE - leg, SIZE - reach, SIZE - 1, SIZE - 1, trim)
        metal_band(img, SIZE - reach, SIZE - leg, SIZE - 1, SIZE - 1, trim)
        for rx, ry in ((10, 6), (22, 6), (42, 6), (54, 6), (10, 57), (22, 57), (42, 57), (54, 57), (2, 17), (61, 17), (2, 46), (61, 46)):
            rivet(img, rx, ry, trim, 2)
        # no lock/keyhole/handle: the crate is locked and opened with the
        # AquaKey, so the front just carries the engraved name like the sides.
        keepout = engrave_name(img, tier, wood)

    scatter_encrust(img, rnd, keepout, count=13)
    return img


def generate(tier_key):
    tier = tiers.get(tier_key)
    for name, fn in (
        ("side", lambda t, s: crate_face(t, s, "side")),
        ("front", lambda t, s: crate_face(t, s, "front")),
        ("top", lambda t, s: crate_face(t, s, "top")),
    ):
        img = fn(tier, 120 + tier["seed"] * 10 + len(name) + 3)
        path = os.path.join(OUT, f"{tier_key}_crate_{name}.png")
        img.save(path)
        print("wrote", path)


def main():
    os.makedirs(OUT, exist_ok=True)
    keys = sys.argv[1:] or [t["key"] for t in tiers.TIERS]
    for key in keys:
        generate(key)


if __name__ == "__main__":
    main()
