"""Draw the five Mystery Enchantment Book item sprites (16x16) plus preview sheets.

Style: clean and punchy, so the books sit next to bold PvP resource packs without
looking soft. That means a crisp near-black outline, flat high-saturation fills
instead of soft gradients, and bright cream pages. The tier is carried by the
cover colour - silver, blue, purple, orange, red - so a book's tier is readable at
a glance without reading the tooltip.

The shape is Minecraft's own book: a closed book seen at an angle, the cover
wrapping the front with the binding down one edge and the page block showing
along the other. Its shading is then posterised into a few flat tones and
re-coloured, which is what gives the crisp, pack-friendly look while keeping the
silhouette everyone already recognises.

    python tools/bookgen/gen_book_textures.py                # write the default palette
    python tools/bookgen/gen_book_textures.py --palette jewel
    python tools/bookgen/gen_book_textures.py --dump         # text view of the sprites
    python tools/bookgen/gen_book_textures.py --vanilla <book.png|client.jar>

The vanilla texture is located automatically (or passed with --vanilla); if you
want the generator to keep working without the game installed, drop the vanilla
book.png at tools/bookgen/vanilla/book.png.
"""
import colorsys
import glob
import io
import os
import sys
import zipfile

from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.normpath(os.path.join(HERE, "..", ".."))
TEX_DIR = os.path.join(ROOT, "src", "main", "resources", "assets", "multihooker", "textures", "item")
PREVIEW_DIR = os.path.join(HERE, "preview")
VANILLA_CACHE = os.path.join(HERE, "vanilla", "book.png")

W = H = 16

TIERS = ["common", "rare", "epic", "legendary", "mythical"]

# Each palette is just the five cover colours, Common -> Mythical. Everything
# else - the lit face, the shaded face, the binding - is derived from the cover
# colour, so a palette only has to say "what colour is each tier".
PALETTES = {
    # Classic MMO rarity ladder: silver, blue, purple, orange, red.
    "classic": [
        (158, 163, 171),
        (60, 130, 232),
        (155, 77, 224),
        (240, 138, 34),
        (214, 28, 40),
    ],
    # Minecraft-flavoured ladder: silver, emerald, blue, purple, gold.
    "jewel": [
        (150, 156, 164),
        (47, 168, 90),
        (60, 130, 232),
        (155, 77, 224),
        (240, 180, 41),
    ],
    # Louder, cooler ladder: silver, cyan, violet, orange, hot pink.
    "neon": [
        (169, 176, 184),
        (41, 199, 240),
        (180, 76, 240),
        (255, 122, 26),
        (255, 46, 99),
    ],
}

DEFAULT_PALETTE = "classic"

SATURATION_BOOST = 1.18   # punch the cover colour up a notch before shading it

# The vanilla cover face - the brightest of the book's cover tones. Its shading
# is scaled relative to this and then bucketed into flat tones.
COVER_REF = (0x65, 0x4B, 0x17)
DARK_CUT = 38.0     # below this the pixel is outline/shadow -> ink
GREY_SPREAD = 14    # low colour spread = neutral = page block

INK = (17, 19, 25)          # crisp near-black outline
PAGE_LIGHT = (253, 250, 240)
PAGE_MID = (235, 227, 201)
PAGE_DARK = (200, 190, 155)

VANILLA_ENTRY = "assets/minecraft/textures/item/book.png"


def luma(c):
    return 0.299 * c[0] + 0.587 * c[1] + 0.114 * c[2]


def mix(a, b, t):
    return tuple(round(a[i] + (b[i] - a[i]) * t) for i in range(3))


def punch(base):
    """Boost saturation so the cover reads boldly next to resource packs."""
    h, s, v = colorsys.rgb_to_hsv(*(c / 255 for c in base))
    r, g, b = colorsys.hsv_to_rgb(h, min(1.0, s * SATURATION_BOOST), v)
    return (round(r * 255), round(g * 255), round(b * 255))


def cover_tones(base):
    base = punch(base)
    return {
        "base": base,                                  # the lit cover face
        "shade": mix(base, (0, 0, 0), 0.16),           # the next face across
        "dark": mix(base, (0, 0, 0), 0.38),            # the binding
        "edge": mix(base, (0, 0, 0), 0.55),            # deepest crease
    }


def restyle(base, tier_rgb):
    """Posterise the vanilla book's shading into flat tones, in the tier colour."""
    tones = cover_tones(tier_rgb)
    ref = luma(COVER_REF)
    out = Image.new("RGBA", base.size, (0, 0, 0, 0))
    for y in range(base.size[1]):
        for x in range(base.size[0]):
            r, g, b, a = base.getpixel((x, y))
            if a == 0:
                continue
            col = (r, g, b)
            level = luma(col)
            spread = max(r, g, b) - min(r, g, b)
            if level < DARK_CUT:
                flat = INK                                  # outline / under-shadow
            elif spread <= GREY_SPREAD:
                if level >= 200:
                    flat = PAGE_LIGHT                       # page block
                elif level >= 140:
                    flat = PAGE_MID
                else:
                    flat = PAGE_DARK
            else:
                ratio = level / ref                         # cover / binding
                if ratio >= 0.95:
                    flat = tones["base"]
                elif ratio >= 0.72:
                    flat = tones["shade"]
                elif ratio >= 0.55:
                    flat = tones["dark"]
                else:
                    flat = tones["edge"]
            out.putpixel((x, y), (*flat, 255))
    return out


def load_from_path(path):
    if path.lower().endswith((".jar", ".zip")):
        with zipfile.ZipFile(path) as z:
            if VANILLA_ENTRY not in z.namelist():
                raise SystemExit("no %s inside %s" % (VANILLA_ENTRY, path))
            return Image.open(io.BytesIO(z.read(VANILLA_ENTRY))).convert("RGBA")
    return Image.open(path).convert("RGBA")


def find_vanilla():
    if "--vanilla" in sys.argv:
        return load_from_path(sys.argv[sys.argv.index("--vanilla") + 1])
    if os.path.exists(VANILLA_CACHE):
        return Image.open(VANILLA_CACHE).convert("RGBA")
    pattern = os.path.expanduser(os.path.join("~", ".gradle", "caches", "neoformruntime",
                                              "artifacts", "minecraft_*_client.jar"))
    for jar in sorted(glob.glob(pattern)):
        try:
            with zipfile.ZipFile(jar) as z:
                if VANILLA_ENTRY in z.namelist():
                    print("using vanilla texture from", jar)
                    return Image.open(io.BytesIO(z.read(VANILLA_ENTRY))).convert("RGBA")
        except Exception:
            continue
    raise SystemExit(
        "could not find the vanilla book texture.\n"
        "pass --vanilla <book.png or client.jar>, or drop it at %s" % VANILLA_CACHE
    )


def dump(im, label):
    """Text view of the sprite so the shape can be checked without an image viewer."""
    key = {}
    letters = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"
    lines = []
    for y in range(im.size[1]):
        row = []
        for x in range(im.size[0]):
            r, g, b, a = im.getpixel((x, y))
            if a == 0:
                row.append("..")
                continue
            c = (r, g, b)
            if c not in key:
                key[c] = letters[len(key)]
            row.append(key[c] * 2)
        lines.append("".join(row))
    print("---", label, "---")
    print("\n".join(lines))
    for c, letter in key.items():
        print(f"  {letter} = #{c[0]:02x}{c[1]:02x}{c[2]:02x}")


def checker(d, x, y, w, h, size=8):
    for yy in range(0, h, size):
        for xx in range(0, w, size):
            shade = (62, 62, 70, 255) if ((xx // size + yy // size) % 2 == 0) else (46, 46, 52, 255)
            d.rectangle([x + xx, y + yy, x + xx + size - 1, y + yy + size - 1], fill=shade)


def build_images(base, palette):
    return [restyle(base, c) for c in PALETTES[palette]]


def main():
    os.makedirs(TEX_DIR, exist_ok=True)
    os.makedirs(PREVIEW_DIR, exist_ok=True)

    chosen = DEFAULT_PALETTE
    if "--palette" in sys.argv:
        chosen = sys.argv[sys.argv.index("--palette") + 1]
        if chosen not in PALETTES:
            raise SystemExit("unknown palette %r (have %s)" % (chosen, ", ".join(PALETTES)))

    vanilla = find_vanilla()
    if vanilla.size != (W, H):
        vanilla = vanilla.resize((W, H), Image.NEAREST)

    # 1. Write the chosen palette's sprites into the mod.
    imgs = build_images(vanilla, chosen)
    for tier, im in zip(TIERS, imgs):
        p = os.path.join(TEX_DIR, "mystery_book_" + tier + ".png")
        im.save(p)
        print("wrote", p)
    if "--dump" in sys.argv:
        dump(imgs[0], chosen + " / common")
        dump(imgs[-1], chosen + " / mythical")

    # 2. Zoom sheet: the vanilla book for reference, then the tiers, big and at
    #    actual hotbar size.
    order = [("vanilla", vanilla)] + list(zip(TIERS, imgs))
    big, small, pad, label = 12, 4, 12, 18
    cell_big = W * big
    cell_small = W * small
    col = cell_big + pad
    sheet = Image.new("RGBA", (pad + len(order) * col, pad + label + cell_big + pad + label + cell_small + pad),
                      (28, 28, 32, 255))
    d = ImageDraw.Draw(sheet)
    for i, (name, im) in enumerate(order):
        x = pad + i * col
        y = pad + label
        checker(d, x, y, cell_big, cell_big)
        sheet.alpha_composite(im.resize((cell_big, cell_big), Image.NEAREST), (x, y))
        d.text((x + 2, pad + 2), name.upper(), fill=(235, 235, 240, 255))
        y2 = y + cell_big + pad + label
        checker(d, x, y2, cell_small, cell_small, 4)
        sheet.alpha_composite(im.resize((cell_small, cell_small), Image.NEAREST), (x, y2))
        d.text((x + 2, y2 - label + 2), "in game", fill=(170, 170, 180, 255))
    out = os.path.join(PREVIEW_DIR, "book_textures_zoom.png")
    sheet.save(out)
    print("preview", out, sheet.size)

    # 3. Side-by-side of every palette so the colours can be compared at a glance.
    scale, pad2, rowlab = 10, 16, 120
    cell = W * scale
    rows = list(PALETTES)
    wide = pad2 + rowlab + len(TIERS) * (cell + pad2) + pad2
    tall = 22 + pad2 + len(rows) * (cell + 22 + pad2) + 22
    cmp_sheet = Image.new("RGBA", (wide, tall), (26, 26, 30, 255))
    d2 = ImageDraw.Draw(cmp_sheet)
    d2.text((pad2, 6), "TIER COLOUR OPTIONS  -  pick one", fill=(255, 240, 120, 255))
    for r, pal in enumerate(rows):
        y = 22 + pad2 + r * (cell + 22 + pad2)
        tag = pal.upper() + ("  (in the jar)" if pal == chosen else "")
        d2.text((pad2, y + cell // 2 - 6), tag, fill=(235, 235, 240, 255))
        for i, im in enumerate(build_images(vanilla, pal)):
            x = pad2 + rowlab + i * (cell + pad2)
            checker(d2, x, y, cell, cell)
            cmp_sheet.alpha_composite(im.resize((cell, cell), Image.NEAREST), (x, y))
            d2.text((x + 2, y + cell + 4), TIERS[i].upper(), fill=(220, 220, 230, 255))
    out2 = os.path.join(PREVIEW_DIR, "book_textures_variants.png")
    cmp_sheet.save(out2)
    print("preview", out2, cmp_sheet.size)


if __name__ == "__main__":
    main()
