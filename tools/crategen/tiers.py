"""Shared data for the MultiHooker crate tiers: names, plaque words, palettes.

Used by gen_textures.py (texture art), gen_resources.py (per-tier JSONs + the
crate category tags) and the preview/render-sheet scripts.
"""
# key: dict with
#   num      - ladder position (1 = weakest); the order shown in the mod
#   seed     - stable art seed for the random sea-growth scatter. NEVER reuse a
#              value: adding or renumbering tiers must not reshuffle the art of
#              the tiers that already exist.
#   group    - "normal" or "special". The special group (the five end-game
#              crates) is published as the item tag multihooker:special_crates;
#              the rest as multihooker:normal_crates.
#   words    - engraved name lines (see gen_textures.carve_text)
#   mc_name  - in-game block name
#   wood     - (light, mid, dark, seam) plank tones
#   trim     - (light, mid, dark) frame/band tones
#   gem      - (light, mid, dark) gem tones (data only, not drawn)
#   swatches - [(label, (r,g,b))] shown on the render sheet
#
# The ladder, weakest first, and the loot power order must always match:
#   NORMAL  (tag normal_crates) :
#       common < uncommon < rare < epic < legendary < godly < mythic < secret
#   SPECIAL (tag special_crates), a higher category - far better loot, and far
#   rarer to fish up than anything in the normal group :
#       divine < ultra_divine < unspoken < ghost_unspoken
# Mythic sits between Godly and Secret; Secret is the strongest NORMAL crate
# (above Mythic), and the four specials sit above every normal crate.
TIERS = [
    {
        "key": "common",
        "num": 1,
        "seed": 1,
        "group": "normal",
        "words": ["COMMON"],
        "mc_name": "Common Crate",
        "wood": ((182, 166, 148), (161, 146, 128), (137, 123, 106), (115, 103, 88)),
        "trim": ((212, 212, 215), (158, 158, 164), (108, 108, 116)),
        "gem": ((250, 222, 170), (236, 168, 84), (168, 108, 42)),
        "swatches": (("oak", (161, 146, 128)), ("iron", (158, 158, 164)), ("amber", (236, 168, 84)), ("sea", (230, 110, 110))),
    },
    {
        "key": "uncommon",
        "num": 2,
        "seed": 2,
        "group": "normal",
        "words": ["UN", "COMMON"],
        "mc_name": "Uncommon Crate",
        "wood": ((150, 162, 134), (131, 143, 115), (111, 123, 97), (92, 103, 81)),
        "trim": ((246, 182, 150), (204, 124, 92), (142, 82, 62)),
        "gem": ((176, 250, 192), (84, 208, 120), (44, 148, 82)),
        "swatches": (("greenwood", (131, 143, 115)), ("copper", (204, 124, 92)), ("emerald", (84, 208, 120)), ("sea", (230, 110, 110))),
    },
    {
        "key": "rare",
        "num": 3,
        "seed": 3,
        "group": "normal",
        "words": ["RARE"],
        "mc_name": "Rare Crate",
        "wood": ((92, 124, 164), (77, 106, 144), (63, 90, 122), (52, 75, 104)),
        "trim": ((234, 240, 250), (178, 188, 208), (128, 138, 158)),
        "gem": ((172, 212, 255), (82, 138, 238), (42, 88, 178)),
        "swatches": (("deepblue", (77, 106, 144)), ("silver", (178, 188, 208)), ("sapphire", (82, 138, 238)), ("sea", (230, 110, 110))),
    },
    {
        "key": "epic",
        "num": 4,
        "seed": 11,
        "group": "normal",
        "words": ["EPIC"],
        "mc_name": "Epic Crate",
        "wood": ((152, 58, 72), (128, 46, 60), (104, 36, 48), (82, 26, 38)),
        "trim": ((252, 208, 198), (216, 136, 124), (152, 76, 72)),
        "gem": ((255, 190, 214), (238, 104, 172), (168, 46, 110)),
        "swatches": (("crimson", (128, 46, 60)), ("rosegold", (216, 136, 124)), ("rosequartz", (238, 104, 172)), ("sea", (230, 110, 110))),
    },
    {
        "key": "legendary",
        "num": 5,
        "seed": 4,
        "group": "normal",
        "words": ["LEGEND"],
        "mc_name": "Legendary Crate",
        "wood": ((124, 94, 166), (106, 80, 146), (91, 66, 124), (74, 54, 104)),
        "trim": ((248, 182, 248), (208, 112, 208), (148, 70, 150)),
        "gem": ((228, 192, 255), (168, 112, 238), (112, 62, 178)),
        "swatches": (("royalpurple", (106, 80, 146)), ("magenta", (208, 112, 208)), ("amethyst", (168, 112, 238)), ("sea", (230, 110, 110))),
    },
    {
        "key": "godly",
        "num": 6,
        "seed": 5,
        "group": "normal",
        "words": ["GODLY"],
        "mc_name": "Godly Crate",
        "wood": ((202, 176, 118), (184, 158, 102), (160, 136, 88), (136, 114, 74)),
        "trim": ((255, 244, 196), (240, 200, 104), (178, 132, 52)),
        "gem": ((255, 238, 182), (238, 178, 72), (178, 118, 42)),
        "swatches": (("goldwood", (184, 158, 102)), ("brightgold", (240, 200, 104)), ("topaz", (238, 178, 72)), ("sea", (230, 110, 110))),
    },
    {
        "key": "mythic",
        "num": 7,
        "seed": 12,
        "group": "normal",
        "words": ["MYTHIC"],
        "mc_name": "Mythic Crate",
        "wood": ((44, 42, 88), (34, 32, 68), (25, 24, 50), (17, 16, 36)),
        "trim": ((255, 238, 190), (236, 182, 96), (168, 112, 46)),
        "gem": ((255, 222, 248), (236, 122, 214), (112, 56, 170)),
        "swatches": (("midnight", (34, 32, 68)), ("gold", (236, 182, 96)), ("prism", (236, 122, 214)), ("sea", (230, 110, 110))),
    },
    {
        "key": "secret",
        "num": 8,
        "seed": 10,
        "group": "normal",
        "words": ["SECRET"],
        "mc_name": "Secret Crate",
        "wood": ((118, 98, 84), (97, 80, 68), (75, 61, 51), (56, 45, 37)),
        "trim": ((222, 182, 116), (168, 124, 64), (112, 80, 38)),
        "gem": ((255, 176, 178), (226, 74, 86), (150, 34, 46)),
        "swatches": (("darkwalnut", (97, 80, 68)), ("bronze", (168, 124, 64)), ("ruby", (226, 74, 86)), ("sea", (230, 110, 110))),
    },
    {
        "key": "divine",
        "num": 9,
        "seed": 6,
        "group": "special",
        "words": ["DIVINE"],
        "mc_name": "Divine Crate",
        "wood": ((253, 252, 250), (240, 237, 232), (219, 215, 209), (191, 187, 179)),
        "trim": ((255, 244, 196), (240, 200, 104), (178, 132, 52)),
        "gem": ((238, 206, 255), (176, 112, 240), (120, 64, 180)),
        "swatches": (("ivory", (240, 237, 232)), ("gold", (240, 200, 104)), ("violet", (176, 112, 240)), ("sea", (230, 110, 110))),
    },
    {
        "key": "ultra_divine",
        "num": 10,
        "seed": 7,
        "group": "special",
        "words": ["ULTRA", "DIVINE"],
        "mc_name": "Ultra Divine Crate",
        "wood": ((255, 255, 255), (238, 243, 250), (210, 221, 240), (178, 194, 224)),
        "trim": ((255, 246, 200), (240, 205, 110), (180, 136, 56)),
        "gem": ((205, 255, 255), (90, 225, 255), (35, 160, 225)),
        "swatches": (("radiant", (238, 243, 250)), ("gold", (240, 205, 110)), ("cyan", (90, 225, 255)), ("sea", (230, 110, 110))),
    },
    {
        "key": "unspoken",
        "num": 11,
        "seed": 8,
        "group": "special",
        "words": ["UN", "SPOKEN"],
        "mc_name": "Unspoken Crate",
        "wood": ((42, 38, 56), (32, 29, 46), (23, 21, 35), (16, 14, 26)),
        "trim": ((152, 122, 202), (102, 74, 152), (64, 44, 104)),
        "gem": ((182, 152, 232), (122, 82, 182), (70, 44, 122)),
        "swatches": (("void", (32, 29, 46)), ("darkviolet", (102, 74, 152)), ("deepgem", (122, 82, 182)), ("sea", (230, 110, 110))),
    },
    {
        "key": "ghost_unspoken",
        "num": 12,
        "seed": 9,
        "group": "special",
        "words": ["GHOST", "SPOKEN"],
        "mc_name": "GhostUnspoken Crate",
        "wood": ((208, 234, 231), (184, 216, 212), (158, 194, 190), (134, 170, 166)),
        "trim": ((250, 255, 255), (210, 240, 255), (150, 200, 220)),
        "gem": ((255, 255, 255), (190, 240, 240), (120, 200, 210)),
        "swatches": (("ghostteal", (184, 216, 212)), ("pale", (210, 240, 255)), ("pearl", (190, 240, 240)), ("sea", (230, 110, 110))),
    },
]

BY_KEY = {t["key"]: t for t in TIERS}

# item tag that publishes each crate category (see gen_resources.write_crate_tags)
GROUPS = {"normal": "normal_crates", "special": "special_crates"}


def get(key):
    return BY_KEY[key]
