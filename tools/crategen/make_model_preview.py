"""Offline preview of a generated crate block model.

Reads Minecraft block model JSONs (<tier>_crate.json for the shut crate,
<tier>_crate_body.json + <tier>_crate_lid.json for an empty one) plus their
textures and renders them with a plain orthographic camera and painter's-
algorithm depth sorting. No Minecraft and no Blender needed, so the crate can be
eyeballed before it ever loads in a world.

The models are built from axis-aligned boxes, so every face projects to a
parallelogram and its texture maps with one affine transform. To show an open
crate the lid model is drawn with a quarter turn about the crate's back rim -
the same rotation the game animates.

Usage:  python make_model_preview.py
Writes: preview/empty_crates.png, preview/open_vs_closed.png
"""
import json
import math
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from PIL import Image, ImageChops, ImageDraw

import tiers

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.normpath(os.path.join(HERE, "..", ".."))
RES = os.path.join(ROOT, "src", "main", "resources", "assets", "multihooker")
MODELS = os.path.join(RES, "models", "block")
TEXTURES = os.path.join(RES, "textures", "block")
PREVIEW = os.path.join(HERE, "preview")

SS = 2                        # supersample, then downscale for clean edges
GRID = 8                      # face subdivision for depth sorting
TILE = 340
YAW, PITCH = 35.0, 24.0       # hero angle: from the front-right, looking down a bit
RESAMPLE = getattr(Image, "Resampling", Image).LANCZOS

# The empty crate's lid, as the game swings it: a quarter turn about X through
# the back rim of the crate (the top of the body, the crate's back face).
OPEN_LID = (-90.0, 12.0, 0.5)

# Flat per-direction shading, so the six sides read as a solid box.
LIGHT = {"up": 1.0, "south": 0.85, "east": 0.75, "west": 0.70, "north": 0.62, "down": 0.5}
NORMAL = {
    "up": (0.0, 1.0, 0.0), "down": (0.0, -1.0, 0.0),
    "north": (0.0, 0.0, -1.0), "south": (0.0, 0.0, 1.0),
    "east": (1.0, 0.0, 0.0), "west": (-1.0, 0.0, 0.0),
}


def corners(face, x1, y1, z1, x2, y2, z2):
    """The face's 4 corners, each tagged with its (u,v) in the 0..1 face space,
    ordered top-left, top-right, bottom-right, bottom-left as seen from outside -
    exactly how Minecraft lays a face's texture out."""
    return {
        "up": [((x1, y2, z1), (0, 0)), ((x2, y2, z1), (1, 0)), ((x2, y2, z2), (1, 1)), ((x1, y2, z2), (0, 1))],
        "down": [((x1, y1, z1), (0, 1)), ((x2, y1, z1), (1, 1)), ((x2, y1, z2), (1, 0)), ((x1, y1, z2), (0, 0))],
        "north": [((x1, y2, z1), (1, 0)), ((x2, y2, z1), (0, 0)), ((x2, y1, z1), (0, 1)), ((x1, y1, z1), (1, 1))],
        "south": [((x1, y2, z2), (0, 0)), ((x2, y2, z2), (1, 0)), ((x2, y1, z2), (1, 1)), ((x1, y1, z2), (0, 1))],
        "west": [((x1, y2, z1), (0, 0)), ((x1, y2, z2), (1, 0)), ((x1, y1, z2), (1, 1)), ((x1, y1, z1), (0, 1))],
        "east": [((x2, y2, z1), (1, 0)), ((x2, y2, z2), (0, 0)), ((x2, y1, z2), (0, 1)), ((x2, y1, z1), (1, 1))],
    }[face]


def sub(a, b):
    return (a[0] - b[0], a[1] - b[1], a[2] - b[2])


def cross(a, b):
    return (a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0])


def dot(a, b):
    return a[0] * b[0] + a[1] * b[1] + a[2] * b[2]


def normalise(v):
    n = math.sqrt(dot(v, v)) or 1.0
    return (v[0] / n, v[1] / n, v[2] / n)


def rotate_x(point, degrees, pivot_y, pivot_z):
    """A point swung about the X axis through (pivot_y, pivot_z)."""
    rad = math.radians(degrees)
    cosine, sine = math.cos(rad), math.sin(rad)
    x, y, z = point
    dy, dz = y - pivot_y, z - pivot_z
    return (x, pivot_y + dy * cosine - dz * sine, pivot_z + dy * sine + dz * cosine)


def facing_of(normal):
    """The face name a (rotated) normal points along - exact for our 90 turns."""
    axis = max(range(3), key=lambda i: abs(normal[i]))
    name = ("east", "up", "south")[axis] if normal[axis] > 0 else ("west", "down", "north")[axis]
    return name


def camera_basis():
    """(forward, right, up): forward points from the crate towards the camera."""
    yr, pr = math.radians(YAW), math.radians(PITCH)
    forward = (math.sin(yr) * math.cos(pr), math.sin(pr), math.cos(yr) * math.cos(pr))
    right = normalise(cross((0.0, 1.0, 0.0), forward))
    return forward, right, cross(forward, right)


def gather_faces(model, rotation=None):
    """Every face of a model, ready to project.

    With a rotation `(degrees, pivot_y, pivot_z)`, each face's corners are swung
    about that pivot and the face is re-labelled by where its normal ends up, so
    culling and shading follow the lid once it is up. The UVs travel with the
    corners, so the texture turns with the lid exactly as it should.
    """
    faces = []
    for element in model["elements"]:
        x1, y1, z1 = element["from"]
        x2, y2, z2 = element["to"]
        for face, spec in element["faces"].items():
            texture = model["textures"][spec["texture"].lstrip("#")]
            quad = corners(face, x1, y1, z1, x2, y2, z2)
            turned = face
            if rotation is not None:
                degrees, pivot_y, pivot_z = rotation
                quad = [(rotate_x(point, degrees, pivot_y, pivot_z), uv) for point, uv in quad]
                turned = facing_of(rotate_x(NORMAL[face], degrees, 0.0, 0.0))
            faces.append(
                {
                    "face": turned,
                    "quad": quad,
                    "uv": spec.get("uv", [0, 0, 16, 16]),
                    "texture": os.path.join(TEXTURES, texture.split("/")[-1] + ".png"),
                }
            )
    return faces


def warp_face(source, uv, quad, shade):
    """Map the face's uv crop of source onto the projected parallelogram, masked
    to it. Returns (patch, x, y) or None."""
    u1, v1, u2, v2 = uv
    d0, d1, d3 = quad[0][0], quad[1][0], quad[3][0]
    e1x, e1y = d1[0] - d0[0], d1[1] - d0[1]      # the quad's "u" edge, in pixels
    e2x, e2y = d3[0] - d0[0], d3[1] - d0[1]      # the quad's "v" edge
    det = e1x * e2y - e2x * e1y
    if abs(det) < 1e-6:
        return None

    def source_at(px, py):
        """Where the face-space point (u,v) at this destination pixel lands."""
        rx, ry = px - d0[0], py - d0[1]
        u = (e2y * rx - e2x * ry) / det
        v = (-e1y * rx + e1x * ry) / det
        return (
            (u1 + u * (u2 - u1)) / 16.0 * source.width,
            (v1 + v * (v2 - v1)) / 16.0 * source.height,
        )

    p0 = source_at(d0[0], d0[1])
    p1 = source_at(d1[0], d1[1])
    p3 = source_at(d3[0], d3[1])
    # The linear part maps the quad's edges onto the matching source offsets, so
    # L = [dp1 dp3] * [e1 e2]^-1, and src = L*(dst - d0) + p0.
    dx1, dy1 = p1[0] - p0[0], p1[1] - p0[1]
    dx2, dy2 = p3[0] - p0[0], p3[1] - p0[1]
    a = (dx1 * e2y - dx2 * e1y) / det
    bb = (-dx1 * e2x + dx2 * e1x) / det
    c = p0[0] - a * d0[0] - bb * d0[1]
    d = (dy1 * e2y - dy2 * e1y) / det
    e = (-dy1 * e2x + dy2 * e1x) / det
    f = p0[1] - d * d0[0] - e * d0[1]

    xs = [p[0] for p, _ in quad]
    ys = [p[1] for p, _ in quad]
    x0, y0 = int(math.floor(min(xs))), int(math.floor(min(ys)))
    w = max(int(math.ceil(max(xs))) - x0, 1)
    h = max(int(math.ceil(max(ys))) - y0, 1)

    patch = source.transform(
        (w, h), Image.AFFINE, (a, bb, c + a * x0 + bb * y0, d, e, f + d * x0 + e * y0), resample=Image.NEAREST
    )
    mask = Image.new("L", (w, h), 0)
    outline = [(p[0] - x0, p[1] - y0) for p, _ in quad]
    # The outline is drawn too: neighbouring sub-quads then overlap by a pixel,
    # which hides the seams a face would otherwise show between its pieces.
    ImageDraw.Draw(mask).polygon(outline, fill=255, outline=255)
    patch.putalpha(ImageChops.multiply(patch.getchannel("A"), mask))

    if shade < 1.0:
        grey = int(round(shade * 255))
        patch = ImageChops.multiply(patch, Image.new("RGBA", (w, h), (grey, grey, grey, 255)))
    return patch, x0, y0


def render(parts, size=TILE, grid=GRID, margin=0.90):
    """Draw one or more models from the same angle.

    `parts` is a model path, or a list of `(model_path, rotation)` pairs, where a
    rotation is `(degrees_about_x, pivot_y, pivot_z)`. That is how an empty crate
    is shown: no single model file contains the hinged lid swung open.
    """
    if isinstance(parts, str):
        parts = [parts]
    forward, right, up = camera_basis()
    side = size * SS

    pieces = []
    for part in parts:
        path, rotation = part if isinstance(part, tuple) else (part, None)
        with open(path, encoding="utf-8") as handle:
            model = json.load(handle)
        for piece in gather_faces(model, rotation):
            # Cull using the direction the face points after any rotation.
            if dot(NORMAL[piece["face"]], forward) <= 0.0:
                continue
            pts, depths = [], []
            for point, _ in piece["quad"]:
                pts.append((dot(point, right), -dot(point, up)))
                depths.append(dot(point, forward))
            piece["pts"], piece["depths"] = pts, depths
            pieces.append(piece)

    # Fit whatever was asked for, however tall the open lid stands.
    xs = [p[0] for piece in pieces for p in piece["pts"]]
    ys = [p[1] for piece in pieces for p in piece["pts"]]
    span = max(max(xs) - min(xs), max(ys) - min(ys)) or 1.0
    scale = side * margin / span
    ox = side / 2.0 - (min(xs) + max(xs)) / 2.0 * scale
    oy = side / 2.0 - (min(ys) + max(ys)) / 2.0 * scale
    for piece in pieces:
        piece["pts"] = [(x * scale + ox, y * scale + oy) for x, y in piece["pts"]]

    # Subdivide every face into a grid before sorting: a single mean depth per
    # face cannot order a face that pokes through another one, which is exactly
    # what the empty crate's upright lid does to the body's open top.
    quads = []
    for piece in pieces:
        d0, d1, _, d3 = piece["pts"]
        e1 = (d1[0] - d0[0], d1[1] - d0[1])
        e2 = (d3[0] - d0[0], d3[1] - d0[1])
        z0, z1, z2, z3 = piece["depths"]
        u1, v1, u2, v2 = piece["uv"]
        for i in range(grid):
            for j in range(grid):
                ua, ub = i / grid, (i + 1) / grid
                va, vb = j / grid, (j + 1) / grid
                corners = []
                depth_total = 0.0
                for (u, v) in ((ua, va), (ub, va), (ub, vb), (ua, vb)):
                    px = d0[0] + u * e1[0] + v * e2[0]
                    py = d0[1] + u * e1[1] + v * e2[1]
                    depth = z0 * (1 - u) * (1 - v) + z1 * u * (1 - v) + z2 * u * v + z3 * (1 - u) * v
                    corners.append(((px, py), depth))
                    depth_total += depth
                quads.append(
                    {
                        "quad": corners,
                        "uv": [u1 + ua * (u2 - u1), v1 + va * (v2 - v1), u1 + ub * (u2 - u1), v1 + vb * (v2 - v1)],
                        "texture": piece["texture"],
                        "shade": LIGHT[piece["face"]],
                        "depth": depth_total / 4.0,
                    }
                )


    canvas = Image.new("RGBA", (side, side), (0, 0, 0, 0))
    sources = {}
    for item in sorted(quads, key=lambda q: q["depth"]):
        source = sources.get(item["texture"])
        if source is None:
            source = sources[item["texture"]] = Image.open(item["texture"]).convert("RGBA")
        warped = warp_face(source, item["uv"], item["quad"], item["shade"])
        if warped is not None:
            patch, x0, y0 = warped
            canvas.alpha_composite(patch, (x0, y0))

    return canvas.resize((size, size), RESAMPLE)


def checker(size, step=12):
    tile = Image.new("RGBA", (size, size), (40, 40, 44, 255))
    draw = ImageDraw.Draw(tile)
    for y in range(0, size, step):
        for x in range(0, size, step):
            if ((x // step) + (y // step)) % 2 == 0:
                draw.rectangle([x, y, x + step - 1, y + step - 1], fill=(54, 54, 58, 255))
    return tile


def cell(render_img, label, sub_label=None):
    img = checker(render_img.width)
    img.alpha_composite(render_img)
    out = Image.new("RGBA", (render_img.width, render_img.height + 18), (24, 24, 28, 255))
    out.alpha_composite(img, (0, 18))
    draw = ImageDraw.Draw(out)
    draw.text((2, 3), label, fill=(240, 240, 240, 255))
    if sub_label:
        draw.text((2 + 6 * len(label) + 8, 3), sub_label, fill=(150, 200, 150, 255))
    return out


def build_sheet(rows, path):
    pad = 10
    width = pad + max(len(r) for r in rows) * (TILE + pad)
    height = pad + len(rows) * (TILE + 18 + pad)
    sheet = Image.new("RGBA", (width, height), (18, 18, 22, 255))
    y = pad
    for row in rows:
        x = pad
        for img in row:
            sheet.alpha_composite(img, (x, y))
            x += TILE + pad
        y += TILE + 18 + pad
    sheet.save(path)
    print("preview", path, sheet.size)


def opened(key):
    """Body + hinged lid at a quarter turn: what a crate looks like mid-loot."""
    return [
        os.path.join(MODELS, f"{key}_crate_body.json"),
        (os.path.join(MODELS, f"{key}_crate_lid.json"), OPEN_LID),
    ]


def main():
    os.makedirs(PREVIEW, exist_ok=True)
    rows = []
    for group in ("normal", "special"):
        row = []
        for tier in [t for t in tiers.TIERS if t["group"] == group]:
            img = render(opened(tier["key"]))
            row.append(cell(img, f"{tier['num']}. {tier['mc_name']}", "(empty, lid up)"))
        rows.append(row)
    build_sheet(rows, os.path.join(PREVIEW, "empty_crates.png"))

    # A crate at rest, next to the same crate while its loot window is open. Both
    # the placeable block and the empty block bake the same shut silhouette; only
    # the lid moves.
    compare = []
    for key in ("common", "godly", "ghost_unspoken"):
        tier = tiers.get(key)
        shut = render(os.path.join(MODELS, f"{key}_crate.json"))
        empty = render(opened(key))
        compare.append([cell(shut, f"{tier['num']}. {tier['mc_name']}"), cell(empty, f"{tier['num']}. {tier['mc_name']}", "(opened)")])
    build_sheet(compare, os.path.join(PREVIEW, "open_vs_closed.png"))


if __name__ == "__main__":
    main()
