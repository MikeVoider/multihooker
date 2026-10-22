"""Mystery book item sprite - a flat, high-resolution 2D item like a vanilla item.

The book is still *designed* as a box - back cover, page block, front cover and
spine - with the details built as geometry rather than painted on:

  * design 6: four metal corner guards, raised off the front cover
  * design 8: a blind-tooled frame, raised off the front cover
  * the emblem: a bone "?", inlaid into the cover as real geometry

...but none of that geometry is shipped. It is rendered once, offline, from the
item's own three-quarter angle and baked down into a flat sprite, which is what
the game actually loads. So the item stays a single flat `item/generated` quad
and the depth lives in the pixels. Baking it from the design keeps the raised
frame, the page block and the inlaid "?" perfectly consistent with each other,
which is the part hand-drawn sprites usually get wrong.

Everything is written from this one script so it can be regenerated:

    textures/item/mystery_book_<tier>.png   the flat HD item sprite, 128x128
    models/item/mystery_book_<tier>.json    parent = minecraft:item/generated

Run with --preview to also render preview/book_item_sprite.png: the five sprites,
how they hold up once minified to inventory size, and alternative camera angles,
so the look can be judged without launching the game.
"""

import argparse
import json
import math
import os
import random
import sys

from PIL import Image, ImageChops, ImageDraw, ImageEnhance, ImageFilter, ImageFont

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import gen_book_textures as g  # noqa: E402

MOD = os.path.abspath(os.path.join(HERE, "..", ".."))
ASSETS = os.path.join(MOD, "src", "main", "resources", "assets", "multihooker")
TEXDIR = os.path.join(ASSETS, "textures", "item")
MODELDIR = os.path.join(ASSETS, "models", "item")
PREVDIR = os.path.join(HERE, "preview")

TIERS = ["common", "rare", "epic", "legendary", "mythical"]
PAL = g.PALETTES["classic"]

TEX_SIZE = 128

# ----------------------------------------------------------------------------
# the shipped sprite: 16x16 pixel art, the vanilla item resolution
#
# The books are drawn pixel by pixel the way every other Minecraft item is: a
# hard black outline around the silhouette, flat deliberate colours, no
# blurring and no gradient. The art lives in the backup PNGs at
# %TEMP%\\opencode\\book_tex_backup_user_art and is regenerated here with a
# 1px black outline added around the book silhouette. Whatever the item has to
# say, it fits in 256 pixels.
# ----------------------------------------------------------------------------
SPRITE_PX = 16                   # one sprite pixel: 16x16, like a vanilla item
OUTLINE = (0, 0, 0)              # pure black, as the user asked for
PAPER = (245, 245, 220)          # the page fore-edge (the ?)
COVER_HIGHLIGHT_OFFSET = 1       # the lighter left edge sits one pixel in

# ----------------------------------------------------------------------------
# geometry, in model units (0..16)
# ----------------------------------------------------------------------------
SPINE_X0, SPINE_X1 = 2.5, 3.6
X0, X1 = SPINE_X1, 13.5          # covers and pages run from the spine to here
Y0, Y1 = 1.5, 14.5

THICKNESS = 3.8                  # total depth of the closed book (this is the one to tune)
COVER_T = 0.45                   # how thick each cover board is
FRAME_RAISE = 0.22               # how far the frame stands off the cover
GUARD_RAISE = 0.40               # how far the guards stand off the cover

PAGE_INSET = 0.5
GUARD = 3.0
FRAME_X0, FRAME_X1 = 5.4, 11.7
FRAME_Y0, FRAME_Y1 = 3.3, 12.7
FRAME_W = 0.35

FACE_W_UNITS = X1 - X0           # the cover face is 9.9 units across...
FACE_H_UNITS = Y1 - Y0           # ...and 13 units high, both stretched over the sheet
PLAIN_UV = (0, 0, 4, 4)          # a corner of the cover sheet, clear of the emblem

# Which of the two detail designs to build as geometry.
WITH_GUARDS = False   # design 6: raised metal corner guards
WITH_FRAME = True     # design 8: raised tooled frame


def z_plan(thickness):
    """Depth positions, always centred on z=8.0 so the item sits right in the GUI."""
    zb0 = 8.0 - thickness / 2.0
    zb1 = zb0 + COVER_T
    zp1 = zb1 + (thickness - 2 * COVER_T)
    zf1 = zp1 + COVER_T
    return tuple(round(v, 4) for v in (zb0, zb1, zp1, zf1))

FACE_NAMES = ("north", "south", "east", "west", "up", "down")

# The display transforms the 3D model used. The sprite is baked flat, so the
# game uses item/generated's own transforms now; these are kept because the
# design geometry is still what the sprite is rendered from.
DISPLAY = {
    "thirdperson_righthand": {"rotation": [0, 0, 0], "translation": [0, 3, 1], "scale": [0.55, 0.55, 0.55]},
    "thirdperson_lefthand": {"rotation": [0, 0, 0], "translation": [0, 3, 1], "scale": [0.55, 0.55, 0.55]},
    "firstperson_righthand": {"rotation": [0, -90, 25], "translation": [1.13, 3.2, 1.13], "scale": [0.68, 0.68, 0.68]},
    "firstperson_lefthand": {"rotation": [0, 90, -25], "translation": [1.13, 3.2, 1.13], "scale": [0.68, 0.68, 0.68]},
    "ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [0.5, 0.5, 0.5]},
    "head": {"rotation": [0, 180, 0], "translation": [0, 13, 7], "scale": [1, 1, 1]},
    "fixed": {"rotation": [0, 180, 0], "translation": [0, 0, 0], "scale": [1, 1, 1]},
    "gui": {"rotation": [0, -22, 0], "translation": [0, 0, 0], "scale": [1, 1, 1]},
}


def cube(frm, to, tex, over=None, uv=None, uv_over=None):
    """A cuboid. `uv` is the default face UV in 0..16 units, `uv_over` a per-face UV."""
    over = over or {}
    uv_over = uv_over or {}
    default = uv or (0, 0, 16, 16)
    faces = {}
    for name in FACE_NAMES:
        faces[name] = {
            "uv": [float(v) for v in uv_over.get(name, default)],
            "texture": "#" + over.get(name, tex),
        }
    return {"from": [float(v) for v in frm], "to": [float(v) for v in to], "faces": faces}


def elements(thickness=THICKNESS):
    zb0, zb1, zp1, zf1 = z_plan(thickness)
    z_frame = round(zf1 + FRAME_RAISE, 4)
    z_guard = round(zf1 + GUARD_RAISE, 4)
    els = []
    # spine: the full height and depth on the left
    els.append(cube((SPINE_X0, Y0, zb0), (SPINE_X1, Y1, zf1), "cover", uv=PLAIN_UV))
    # back cover
    els.append(cube((X0, Y0, zb0), (X1, Y1, zb1), "cover", uv=PLAIN_UV))
    # page block, inset all round so the covers overhang it
    els.append(cube((X0, Y0 + PAGE_INSET, zb1), (X1 - 0.7, Y1 - PAGE_INSET, zp1), "pages_side",
                    {"up": "pages_top", "down": "pages_side"}))
    # front cover - only this face carries the emblem
    els.append(cube((X0, Y0, zp1), (X1, Y1, zf1), "cover", uv=PLAIN_UV,
                    uv_over={"south": (0, 0, 16, 16)}))
    # design 6: raised metal corner guards
    if WITH_GUARDS:
        for gx, gy in ((X0, Y0), (X1 - GUARD, Y0), (X0, Y1 - GUARD), (X1 - GUARD, Y1 - GUARD)):
            els.append(cube((gx, gy, zf1), (gx + GUARD, gy + GUARD, z_guard), "metal"))
    # design 8: raised tooled frame
    if WITH_FRAME:
        els.append(cube((FRAME_X0, FRAME_Y0, zf1), (FRAME_X1, FRAME_Y0 + FRAME_W, z_frame), "frame"))
        els.append(cube((FRAME_X0, FRAME_Y1 - FRAME_W, zf1), (FRAME_X1, FRAME_Y1, z_frame), "frame"))
        els.append(cube((FRAME_X0, FRAME_Y0 + FRAME_W, zf1), (FRAME_X0 + FRAME_W, FRAME_Y1 - FRAME_W, z_frame), "frame"))
        els.append(cube((FRAME_X1 - FRAME_W, FRAME_Y0 + FRAME_W, zf1), (FRAME_X1, FRAME_Y1 - FRAME_W, z_frame), "frame"))
    # design 9: the bone "?" inlaid into the middle of the front cover
    if WITH_EMBLEM:
        els.extend(glyph_elements(round(zf1 - EMBLEM_SINK, 4), round(zf1 + EMBLEM_RAISE, 4)))
    return els


# ----------------------------------------------------------------------------
# textures
# ----------------------------------------------------------------------------
def _clamp(v):
    return max(0, min(255, int(round(v))))


# ----------------------------------------------------------------------------
# the cover emblem - a bone "?" inlaid into the cover as real geometry
#
# Painted on, the glyph read as a sticker glued to a book that has real depth.
# So it is built the same way as the frame: extruded cuboids standing off the
# cover, with the lit face in bright bone and the cut edges in a darker bone,
# which is what makes the raise read. The bowl is a row of short bars laid
# along a circle, each given its own tilt, so it comes out as a smooth curve
# instead of a staircase.
# ----------------------------------------------------------------------------
EMBLEM_STROKE = 0.9               # how thick the stroke of the "?" is
EMBLEM_BOWL_X = 8.55              # centre of the bowl...
EMBLEM_BOWL_Y = 9.25
EMBLEM_BOWL_RX = 1.25             # ...and its radius, across and up
EMBLEM_BOWL_RY = 1.5
EMBLEM_A0, EMBLEM_A1 = 170.0, -35.0   # degrees: the bowl sweeps down the right
EMBLEM_STEPS = 14                 # bars used for the bowl
EMBLEM_STEM_TOP = 7.9             # where the tail meets the stem...
EMBLEM_STEM_BOT = 6.45            # ...and where the stem stops
EMBLEM_DOT_Y = 5.25
EMBLEM_RAISE = 0.2                # how far the inlay stands off the cover
EMBLEM_SINK = 0.08                # how far it is let into the cover
EMBLEM_SHADOW = (0.26, -0.30)     # offset of the baked drop shadow, in model units

INLAY_TOP = "emblem"              # the lit face of the inlay
INLAY_SIDE = "emblem_side"        # its cut edge

WITH_EMBLEM = True                # design 9: the bone "?" inlay


def emblem_point(angle_deg):
    """A point on the bowl's centre line, in cover coordinates (x right, y up)."""
    a = math.radians(angle_deg)
    return (EMBLEM_BOWL_X + EMBLEM_BOWL_RX * math.cos(a),
            EMBLEM_BOWL_Y + EMBLEM_BOWL_RY * math.sin(a))


def glyph_bars():
    """The "?" as a list of strokes (p, q), in cover coordinates."""
    bars = []
    span = EMBLEM_A1 - EMBLEM_A0
    pts = [emblem_point(EMBLEM_A0 + span * i / EMBLEM_STEPS) for i in range(EMBLEM_STEPS + 1)]
    bars.extend(zip(pts, pts[1:]))
    bars.append((emblem_point(EMBLEM_A1), (EMBLEM_BOWL_X, EMBLEM_STEM_TOP)))
    bars.append(((EMBLEM_BOWL_X, EMBLEM_STEM_TOP), (EMBLEM_BOWL_X, EMBLEM_STEM_BOT)))
    return bars


def _inlay_pieces():
    """Every piece of the inlay as a (centre, size) box in cover coordinates."""
    pieces = []
    for p, q in glyph_bars():
        dx, dy = q[0] - p[0], q[1] - p[1]
        span = math.hypot(dx, dy) + EMBLEM_STROKE * 0.5
        ang = math.atan2(dy, dx)
        pieces.append(((p[0] + q[0]) / 2.0, (p[1] + q[1]) / 2.0,
                       span, EMBLEM_STROKE, math.degrees(ang)))
    pieces.append((EMBLEM_BOWL_X, EMBLEM_DOT_Y, EMBLEM_STROKE, EMBLEM_STROKE, 0.0))
    return pieces


def _to_sheet(x, y, size):
    """Cover coordinates -> sheet pixels. The square sheet is stretched over the face."""
    return ((x - X0) / FACE_W_UNITS * size, (Y1 - y) / FACE_H_UNITS * size)


def _shift(mask, dx, dy):
    out = Image.new("L", mask.size, 0)
    out.paste(mask, (dx, dy))
    return out


def emblem_shadow(size):
    """The drop shadow the raised inlay casts on the board beneath it.

    Drawn from the same strokes as the geometry, so the two always line up.
    """
    ss = 4
    big = size * ss
    mask = Image.new("L", (big, big), 0)
    d = ImageDraw.Draw(mask)
    for cx, cy, span, width, ang in _inlay_pieces():
        a = math.radians(ang)
        hx, hy = math.cos(a) * span / 2.0, math.sin(a) * span / 2.0
        p0 = _to_sheet(cx - hx, cy - hy, big)
        p1 = _to_sheet(cx + hx, cy + hy, big)
        wpx = max(3, int(round(width / FACE_W_UNITS * big)))
        d.line([p0, p1], fill=255, width=wpx)
        for px_, py_ in (p0, p1):                       # round off the stroke ends
            d.ellipse([px_ - wpx / 2.0, py_ - wpx / 2.0, px_ + wpx / 2.0, py_ + wpx / 2.0], fill=255)
    mask = mask.resize((size, size), Image.LANCZOS)
    dx = int(round(EMBLEM_SHADOW[0] / FACE_W_UNITS * size))
    dy = int(round(-EMBLEM_SHADOW[1] / FACE_H_UNITS * size))
    return _shift(mask, dx, dy).filter(ImageFilter.GaussianBlur(max(1.0, size / 90.0)))


def inlay_cube(frm, to):
    """One piece of the inlay: bright bone up top, darker bone down the cut edges."""
    over = {name: INLAY_SIDE for name in FACE_NAMES if name != "south"}
    return cube(frm, to, INLAY_TOP, over=over)


def bar_el(p0, p1, z0, z1, width=EMBLEM_STROKE):
    """One stroke of the inlay: a bar across p0->p1, tilted to match.

    A model element may only be rotated up to 45 degrees either way, so a stroke
    running mostly up or down is laid along y instead and given the rest of its
    angle - the same bar in the same place, with a legal rotation.
    """
    dx, dy = p1[0] - p0[0], p1[1] - p0[1]
    span = math.hypot(dx, dy) + width * 0.5        # the overlap closes the joints
    if span < 1e-6:
        raise ValueError("zero length stroke in the cover emblem")
    ang = math.degrees(math.atan2(dy, dx))
    if ang > 90.0:                                 # a bar may be laid either way
        ang -= 180.0
    elif ang <= -90.0:
        ang += 180.0
    cx, cy = (p0[0] + p1[0]) / 2.0, (p0[1] + p1[1]) / 2.0
    if ang > 45.0:
        ang -= 90.0
        frm, to = (cx - width / 2, cy - span / 2, z0), (cx + width / 2, cy + span / 2, z1)
    elif ang < -45.0:
        ang += 90.0
        frm, to = (cx - width / 2, cy - span / 2, z0), (cx + width / 2, cy + span / 2, z1)
    else:
        frm, to = (cx - span / 2, cy - width / 2, z0), (cx + span / 2, cy + width / 2, z1)
    el = inlay_cube(frm, to)
    el["rotation"] = {"origin": [round(cx, 4), round(cy, 4), round(z0, 4)],
                      "axis": "z", "angle": round(ang, 4)}
    return el


def glyph_elements(z0, z1):
    """The whole "?" as geometry: every stroke plus the dot."""
    els = [bar_el(p, q, z0, z1) for p, q in glyph_bars()]
    half = EMBLEM_STROKE / 2.0
    els.append(inlay_cube((EMBLEM_BOWL_X - half, EMBLEM_DOT_Y - half, z0),
                          (EMBLEM_BOWL_X + half, EMBLEM_DOT_Y + half, z1)))
    return els


def emblem_texture(size=TEX_SIZE):
    """The lit face of the inlay: warm bone, a touch deeper towards the bottom."""
    im = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    px = im.load()
    for y in range(size):
        f = 1.0 - 0.12 * (y / float(size))
        for x in range(size):
            px[x, y] = (_clamp(247 * f), _clamp(240 * f), _clamp(222 * f), 255)
    return im


def emblem_side_texture(size=TEX_SIZE):
    """The cut edge of the inlay: the same bone gone dark - this is the depth."""
    im = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    px = im.load()
    for y in range(size):
        f = 0.84 - 0.42 * (y / float(size))
        for x in range(size):
            px[x, y] = (_clamp(179 * f), _clamp(171 * f), _clamp(148 * f), 255)
    return im


def cover_texture(base_rgb, size=TEX_SIZE):
    """A clean tier cover: a soft top-left lit gradient, a whisper of grain, and
    the shadow of the raised emblem so the inlay sits down into the board."""
    rnd = random.Random(20240516)
    im = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    px = im.load()
    for y in range(size):
        for x in range(size):
            t = (x / size) * 0.42 + (y / size) * 0.58
            f = 1.12 - 0.24 * t
            n = rnd.randint(-3, 3)
            px[x, y] = (_clamp(base_rgb[0] * f + n), _clamp(base_rgb[1] * f + n), _clamp(base_rgb[2] * f + n), 255)
    shadow = emblem_shadow(size)
    layer = Image.new("RGBA", (size, size), (26, 19, 13, 0))
    layer.putalpha(shadow.point(lambda v: int(v * 0.44)))
    im.alpha_composite(layer)
    return im


def pages_texture(vertical, size=TEX_SIZE):
    """A paper edge: eight soft, low-contrast strata - never fine stripes.

    The page block is only a few model units deep, so the sheet's pixels land
    roughly one per screen pixel and a line every few pixels turns into a
    shimmering moire. Wide, gentle strata read as a page stack at every size
    and average away cleanly when the texture is minified.
    """
    base = (250, 246, 233)
    line = (228, 219, 197)
    im = Image.new("RGBA", (size, size), base + (255,))
    px = im.load()
    rnd = random.Random(11)
    step = size / 8.0
    for a in range(size):
        t = ((a + 0.5) % step) / step
        f = (1.0 - min(1.0, abs(t - 0.5) * 2.0)) ** 2
        for b in range(size):
            n = rnd.randint(-2, 2)
            x, y = (a, b) if vertical else (b, a)
            px[x, y] = (
                _clamp(base[0] + (line[0] - base[0]) * f + n),
                _clamp(base[1] + (line[1] - base[1]) * f + n),
                _clamp(base[2] + (line[2] - base[2]) * f + n),
                255,
            )
    return im


def metal_texture(size=TEX_SIZE):
    """Brushed steel, lit diagonally, so the guards read as metal."""
    im = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    px = im.load()
    rnd = random.Random(7)
    for y in range(size):
        for x in range(size):
            t = (x + y) / (2 * size)
            f = 0.72 + 0.62 * t
            n = rnd.randint(-4, 4)
            px[x, y] = (_clamp(70 * f + n), _clamp(77 * f + n), _clamp(88 * f + n), 255)
    d = ImageDraw.Draw(im)
    d.line([(0, 2), (size, 2)], fill=(150, 160, 176, 255), width=2)
    d.line([(0, size - 3), (size, size - 3)], fill=(36, 39, 46, 255), width=2)
    return im


def frame_texture(size=TEX_SIZE):
    """Gilt bronze, brightest along the middle so a thin strip still reads."""
    im = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    px = im.load()
    for y in range(size):
        t = abs((y / size) - 0.5) * 2.0
        f = 1.35 - 0.75 * t
        for x in range(size):
            px[x, y] = (_clamp(168 * f), _clamp(126 * f), _clamp(58 * f), 255)
    return im


def write_textures():
    """16x16 pixel-art sprites: black cover, inner frame, centered bone ?,
    1px black outline. The ? is centered on the cover."""
    os.makedirs(TEXDIR, exist_ok=True)
    made = {}
    for tier in TIERS:
        img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
        px = img.load()
        BLACK = (25, 20, 30)
        BONE = (245, 245, 220)
        FRAME = (60, 55, 70)
        # black cover
        for y in range(2, 14):
            for x in range(3, 13):
                px[x, y] = BLACK + (255,)
        # frame (1px, inset 1)
        for y in range(3, 13):
            px[4, y] = FRAME + (255,)
            px[11, y] = FRAME + (255,)
        for x in range(4, 12):
            px[x, 3] = FRAME + (255,)
            px[x, 12] = FRAME + (255,)
        # ? medium (4x8), centered on cover
        Q = [".PP.", "P..P", "...P", "..P.", ".P..",
             "....", ".P..", ".P.."]
        for ry, row in enumerate(Q):
            for rx, ch in enumerate(row):
                if ch == "P":
                    px[6 + rx, 4 + ry] = BONE + (255,)
        # 1px black outline (using saved mask to prevent cascade)
        w, h = img.size
        mask = [[px[x, y][3] > 40 for x in range(w)] for y in range(h)]
        for y in range(h):
            for x in range(w):
                if not mask[y][x]:
                    for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                        nx, ny = x + dx, y + dy
                        if 0 <= nx < w and 0 <= ny < h and mask[ny][nx]:
                            px[x, y] = (0, 0, 0, 255)
                            break
        img.save(os.path.join(TEXDIR, "mystery_book_%s.png" % tier))
        made[tier] = img
    for stale in ("book_pages_top", "book_pages_side", "book_frame",
                  "book_emblem", "book_emblem_side", "book_metal"):
        path = os.path.join(TEXDIR, stale + ".png")
        if os.path.exists(path):
            os.remove(path)
    return made


def check_elements(els):
    """Refuse to work with a design the game would reject as a model: the loader
    throws on any element outside -16..32, on an inverted box, and on a rotation
    past 45 degrees, and one bad element takes the whole model down with it. The
    shipped item is a baked sprite, but the geometry below is still the source
    of that sprite, so it is worth proving it stays model-legal."""
    for e in els:
        frm, to = e["from"], e["to"]
        for v in list(frm) + list(to):
            if v < -16.0 or v > 32.0:
                raise ValueError("element outside the legal model space: %r %r" % (frm, to))
        if any(b < a for a, b in zip(frm, to)):
            raise ValueError("inverted element: %r %r" % (frm, to))
        rot = e.get("rotation")
        if rot and abs(rot["angle"]) > 45.0:
            raise ValueError("element rotation past the 45 degrees the game allows: %r" % (rot,))


def write_models():
    """One flat generated model per tier - a normal vanilla-style item quad."""
    os.makedirs(MODELDIR, exist_ok=True)
    stale = os.path.join(MODELDIR, "mystery_book_base.json")
    if os.path.exists(stale):
        os.remove(stale)
    for tier in TIERS:
        model = {
            "parent": "minecraft:item/generated",
            "textures": {"layer0": "multihooker:item/mystery_book_%s" % tier},
        }
        path = os.path.join(MODELDIR, "mystery_book_%s.json" % tier)
        with open(path, "w", encoding="utf-8", newline="\n") as f:
            json.dump(model, f, indent=2)
            f.write("\n")


# ----------------------------------------------------------------------------
# offline preview render
# ----------------------------------------------------------------------------
FACE_NORMAL = {
    "south": (0, 0, 1), "north": (0, 0, -1), "east": (1, 0, 0),
    "west": (-1, 0, 0), "up": (0, 1, 0), "down": (0, -1, 0),
}


def face_corners(name, frm, to):
    """Corners in texture order: top-left, top-right, bottom-right, bottom-left."""
    x0, y0, z0 = frm
    x1, y1, z1 = to
    if name == "south":
        return [(x0, y1, z1), (x1, y1, z1), (x1, y0, z1), (x0, y0, z1)]
    if name == "north":
        return [(x1, y1, z0), (x0, y1, z0), (x0, y0, z0), (x1, y0, z0)]
    if name == "west":
        return [(x0, y1, z0), (x0, y1, z1), (x0, y0, z1), (x0, y0, z0)]
    if name == "east":
        return [(x1, y1, z1), (x1, y1, z0), (x1, y0, z0), (x1, y0, z1)]
    if name == "up":
        return [(x0, y1, z0), (x1, y1, z0), (x1, y1, z1), (x0, y1, z1)]
    return [(x0, y0, z1), (x1, y0, z1), (x1, y0, z0), (x0, y0, z0)]


def rot_y(p, a):
    c, s = math.cos(a), math.sin(a)
    return (p[0] * c + p[2] * s, p[1], -p[0] * s + p[2] * c)


def rot_x(p, a):
    c, s = math.cos(a), math.sin(a)
    return (p[0], p[1] * c - p[2] * s, p[1] * s + p[2] * c)


def rot_about(p, origin, axis, angle_deg):
    """Turn a point about an element's own origin, the way the game does."""
    a = math.radians(angle_deg)
    c, s = math.cos(a), math.sin(a)
    x, y, z = p[0] - origin[0], p[1] - origin[1], p[2] - origin[2]
    if axis == "z":
        x, y = x * c - y * s, x * s + y * c
    elif axis == "y":
        x, z = x * c + z * s, -x * s + z * c
    else:                                        # "x"
        y, z = y * c - z * s, y * s + z * c
    return (x + origin[0], y + origin[1], z + origin[2])


def render_model(textures, yaw_deg, pitch_deg, size=256, scale=None, thickness=THICKNESS):
    """Offline render with a per-pixel depth buffer, so raised details sit correctly."""
    yaw = math.radians(yaw_deg)
    pitch = math.radians(pitch_deg)
    if scale is None:
        scale = size / 20.0
    cx = cy = size / 2.0
    ctr = (8.0, 8.0, 8.0)

    def view(p):
        return rot_x(rot_y((p[0] - ctr[0], p[1] - ctr[1], p[2] - ctr[2]), yaw), pitch)

    def screen(v):
        return (cx + v[0] * scale, cy - v[1] * scale)

    out = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    op = out.load()
    zbuf = [[-1e9] * size for _ in range(size)]

    for el in elements(thickness):
        frm, to = el["from"], el["to"]
        rot = el.get("rotation")

        def place(p, rot=rot):
            if rot is None:
                return p
            return rot_about(p, tuple(rot["origin"]), str(rot["axis"]).lower(), rot["angle"])

        def turn(n, rot=rot):
            if rot is None:
                return n
            return rot_about(n, (0.0, 0.0, 0.0), str(rot["axis"]).lower(), rot["angle"])

        for name in FACE_NAMES:
            texname = el["faces"][name]["texture"].lstrip("#")
            fu0, fv0, fu1, fv1 = [c / 16.0 for c in el["faces"][name].get("uv", (0, 0, 16, 16))]
            n = rot_x(rot_y(turn(FACE_NORMAL[name]), yaw), pitch)
            if n[2] <= 0.001:                       # facing away from the camera
                continue
            vs = [view(place(p)) for p in face_corners(name, frm, to)]
            ds = [v[2] for v in vs]
            p0 = screen(vs[0])
            p1 = screen(vs[1])
            p3 = screen(vs[3])
            e1 = (p1[0] - p0[0], p1[1] - p0[1])
            e2 = (p3[0] - p0[0], p3[1] - p0[1])
            det = e1[0] * e2[1] - e1[1] * e2[0]
            if abs(det) < 1e-9:
                continue
            inv = ((e2[1] / det, -e2[0] / det), (-e1[1] / det, e1[0] / det))
            # gui_light: front - faces pointing at the camera are lit
            tex = ImageEnhance.Brightness(textures[texname].convert("RGB")).enhance(0.45 + 0.55 * min(1.0, n[2]))
            tw, th = tex.size
            tb = tex.tobytes()
            corners = [p0, p1, (p0[0] + e1[0] + e2[0], p0[1] + e1[1] + e2[1]), p3]
            xs = [c[0] for c in corners]
            ys = [c[1] for c in corners]
            x0i = max(0, int(math.floor(min(xs))))
            x1i = min(size - 1, int(math.ceil(max(xs))))
            y0i = max(0, int(math.floor(min(ys))))
            y1i = min(size - 1, int(math.ceil(max(ys))))
            for py in range(y0i, y1i + 1):
                dyy = py + 0.5 - p0[1]
                zrow = zbuf[py]
                for pxx in range(x0i, x1i + 1):
                    dxx = pxx + 0.5 - p0[0]
                    u = inv[0][0] * dxx + inv[0][1] * dyy
                    if u < 0.0 or u > 1.0:
                        continue
                    v = inv[1][0] * dxx + inv[1][1] * dyy
                    if v < 0.0 or v > 1.0:
                        continue
                    d = ds[0] + (ds[1] - ds[0]) * u + (ds[3] - ds[0]) * v
                    if d <= zrow[pxx]:
                        continue
                    zrow[pxx] = d
                    ti = (min(th - 1, int((fv0 + v * (fv1 - fv0)) * th)) * tw
                          + min(tw - 1, int((fu0 + u * (fu1 - fu0)) * tw))) * 3
                    op[pxx, py] = (tb[ti], tb[ti + 1], tb[ti + 2], 255)
    return out


def _font(size):
    """A readable label font, falling back to PIL's tiny built-in."""
    for name in ("segoeui.ttf", "arial.ttf", "DejaVuSans.ttf"):
        try:
            return ImageFont.truetype(name, size)
        except Exception:
            pass
    return ImageFont.load_default()


def _checker(size):
    tile = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    d = ImageDraw.Draw(tile)
    step = max(4, size // 16)
    for yy in range(0, size, step):
        for xx in range(0, size, step):
            if ((xx // step) + (yy // step)) % 2 == 0:
                d.rectangle([xx, yy, xx + step - 1, yy + step - 1], fill=(70, 70, 78, 255))
            else:
                d.rectangle([xx, yy, xx + step - 1, yy + step - 1], fill=(54, 54, 61, 255))
    return tile


def write_preview(sprites):
    """Show the 16x16 sprites at a large zoom so the pixel art is readable."""
    os.makedirs(PREVDIR, exist_ok=True)
    pad, zoom = 12, 8
    cell = SPRITE_PX * zoom          # 16 * 8 = 128
    sheet = Image.new("RGBA", (pad + len(TIERS) * (cell + pad) + pad,
                              pad + cell + 60 + pad), (24, 24, 28, 255))
    d = ImageDraw.Draw(sheet)
    try:
        f_title = ImageFont.truetype("segoeui.ttf", 20)
        f_small = ImageFont.truetype("segoeui.ttf", 13)
    except Exception:
        f_title = ImageFont.load_default()
        f_small = f_title
    d.text((pad, 8), "MYSTERY ENCHANTMENT BOOK - 16x16 pixel art, black outline",
           font=f_title, fill=(255, 236, 120, 255))
    y = pad + 36
    for i, tier in enumerate(TIERS):
        x = pad + i * (cell + pad)
        # checkerboard behind the sprite
        for yy in range(0, cell, zoom):
            for xx in range(0, cell, zoom):
                shade = (58, 58, 66, 255) if ((xx // zoom + yy // zoom) % 2 == 0) else (44, 44, 50, 255)
                d.rectangle([x + xx, y + yy, x + xx + zoom - 1, y + yy + zoom - 1], fill=shade)
        sheet.alpha_composite(sprites[tier].resize((cell, cell), Image.NEAREST), (x, y))
        d.text((x + 2, y + cell + 6), tier.upper(), font=f_small, fill=(224, 224, 234, 255))
    out = os.path.join(PREVDIR, "book_item_sprite.png")
    sheet.save(out)
    return out


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--preview", action="store_true", help="also render preview/book_item_sprite.png")
    args = ap.parse_args()
    check_elements(elements())            # the design the sprite is baked from
    sprites = write_textures()
    write_models()
    print("wrote %d sprites and %d models" % (len(sprites), len(TIERS)))
    if args.preview:
        print("preview ->", write_preview(sprites))


if __name__ == "__main__":
    main()
