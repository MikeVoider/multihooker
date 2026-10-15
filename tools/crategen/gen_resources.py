"""Generate the per-tier resource JSONs for all MultiHooker crates and copy
the art into src/main/resources. Run from tools/crategen:

    python gen_resources.py

Writes:
  assets/multihooker/blockstates/<key>_crate.json           (placeable, closed)
  assets/multihooker/blockstates/<key>_crate_empty.json     (the opened-up crate)
  assets/multihooker/models/item/<key>_crate.json
  assets/multihooker/models/item/<key>_crate_empty.json
  assets/multihooker/models/block/<key>_crate.json          (shut: body + flat lid, same geometry as build_crate.py)
  assets/multihooker/models/block/<key>_crate_body.json     (body alone, dark inside on top)
  assets/multihooker/models/block/<key>_crate_lid.json      (lid alone, dark inside underneath; drawn hinged by CrateLidRenderer)
  data/multihooker/loot_table/blocks/<key>_crate.json       (self-drop)
  data/multihooker/loot_table/blocks/<key>_crate_empty.json (drops the empty crate)
  data/multihooker/loot_table/crates/<key>.json             (open loot)
  data/multihooker/tags/item/{normal,special}_crates.json    (crate categories)
  assets/multihooker/textures/block/<key>_crate_*.png        (copied)
"""
import json
import os
import shutil

from PIL import Image

import tiers

HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(HERE, "out")
ROOT = os.path.normpath(os.path.join(HERE, "..", ".."))
RES = os.path.join(ROOT, "src", "main", "resources")

# loot spec per tier: rolls [min,max], entries
# entry: ("item", "minecraft:diamond", weight, min, max)
#        ("tag",  "c:gems",           weight)                 -> expand:true
#        ("book", None,               weight)                 -> enchanted_book + enchant_randomly
# Every tier also gets a second pool (bonus_pool) that rolls one item from the
# broad conventional tags - that is what puts OTHER MODS' loot in the crates.
LOOT = {
    "common": {
        "rolls": [1, 2],
        "entries": [
            ("item", "minecraft:oak_log", 20, 3, 8),
            ("item", "minecraft:coal", 18, 2, 6),
            ("item", "minecraft:iron_ingot", 18, 1, 3),
            ("item", "minecraft:stick", 15, 4, 10),
            ("item", "minecraft:bread", 15, 1, 3),
        ],
    },
    "uncommon": {
        "rolls": [1, 3],
        "entries": [
            ("item", "minecraft:iron_ingot", 20, 2, 5),
            ("item", "minecraft:gold_ingot", 16, 1, 3),
            ("item", "minecraft:copper_ingot", 15, 3, 8),
            ("item", "minecraft:coal", 15, 3, 8),
            ("item", "minecraft:string", 12, 2, 6),
            ("item", "minecraft:leather", 10, 1, 3),
        ],
    },
    "rare": {
        "rolls": [2, 3],
        "entries": [
            ("item", "minecraft:gold_ingot", 20, 2, 5),
            ("item", "minecraft:lapis_lazuli", 16, 4, 10),
            ("item", "minecraft:diamond", 14, 1, 2),
            ("item", "minecraft:emerald", 14, 2, 5),
            ("item", "minecraft:iron_ingot", 12, 4, 8),
            ("book", None, 10),
        ],
    },
    "epic": {
        "rolls": [2, 4],
        "entries": [
            ("item", "minecraft:gold_ingot", 18, 3, 6),
            ("item", "minecraft:emerald", 16, 3, 6),
            ("item", "minecraft:diamond", 14, 1, 3),
            ("item", "minecraft:lapis_lazuli", 12, 6, 12),
            ("item", "minecraft:experience_bottle", 12, 2, 4),
            ("item", "minecraft:golden_apple", 10, 1, 1),
            ("book", None, 10),
        ],
    },
    "legendary": {
        "rolls": [2, 4],
        "entries": [
            ("item", "minecraft:diamond", 18, 1, 3),
            ("item", "minecraft:emerald", 16, 4, 8),
            ("item", "minecraft:experience_bottle", 14, 2, 5),
            ("item", "minecraft:golden_apple", 12, 1, 1),
            ("item", "minecraft:netherite_scrap", 10, 1, 2),
            ("item", "minecraft:ender_pearl", 10, 1, 2),
            ("book", None, 10),
        ],
    },
    "godly": {
        "rolls": [3, 5],
        "entries": [
            ("tag", "c:gems", 18),
            ("item", "minecraft:diamond", 18, 2, 5),
            ("item", "minecraft:emerald", 14, 6, 12),
            ("item", "minecraft:experience_bottle", 12, 4, 8),
            ("item", "minecraft:golden_apple", 12, 1, 2),
            ("item", "minecraft:netherite_ingot", 8, 1, 1),
            ("item", "minecraft:heart_of_the_sea", 6, 1, 1),
            ("item", "minecraft:enchanted_golden_apple", 6, 1, 1),
            ("book", None, 8),
        ],
    },
    # Mythic sits between godly and secret: a clear step up from godly, and
    # secret (the strongest NORMAL crate) is still a step above it.
    "mythic": {
        "rolls": [3, 6],
        "entries": [
            ("tag", "c:gems", 18),
            ("item", "minecraft:diamond", 18, 3, 6),
            ("item", "minecraft:emerald", 14, 7, 14),
            ("item", "minecraft:experience_bottle", 12, 5, 9),
            ("item", "minecraft:golden_apple", 12, 1, 2),
            ("item", "minecraft:netherite_ingot", 9, 1, 2),
            ("item", "minecraft:heart_of_the_sea", 7, 1, 2),
            ("item", "minecraft:enchanted_golden_apple", 6, 1, 1),
            ("item", "minecraft:nautilus_shell", 6, 1, 3),
            ("book", None, 8),
        ],
    },
    # The strongest NORMAL crate: the secret sits directly above mythic and its
    # loot is strictly better. The four specials below sit above this.
    "secret": {
        "rolls": [3, 6],
        "entries": [
            ("tag", "c:gems", 18),
            ("item", "minecraft:diamond", 18, 3, 6),
            ("item", "minecraft:emerald", 14, 8, 16),
            ("item", "minecraft:experience_bottle", 12, 5, 10),
            ("item", "minecraft:golden_apple", 12, 2, 3),
            ("item", "minecraft:netherite_ingot", 10, 1, 2),
            ("item", "minecraft:heart_of_the_sea", 8, 1, 2),
            ("item", "minecraft:enchanted_golden_apple", 8, 1, 2),
            ("item", "minecraft:nautilus_shell", 8, 2, 4),
            ("book", None, 8),
        ],
    },
    # ---- SPECIAL crates: their own higher category, above every normal crate ----
    "divine": {
        "rolls": [4, 6],
        "entries": [
            ("tag", "c:gems", 18),
            ("tag", "c:ingots", 12),
            ("item", "minecraft:diamond", 16, 4, 8),
            ("item", "minecraft:emerald", 14, 10, 20),
            ("item", "minecraft:experience_bottle", 12, 6, 12),
            ("item", "minecraft:golden_apple", 12, 2, 4),
            ("item", "minecraft:netherite_ingot", 10, 2, 3),
            ("item", "minecraft:heart_of_the_sea", 8, 2, 3),
            ("item", "minecraft:enchanted_golden_apple", 8, 1, 3),
            ("item", "minecraft:totem_of_undying", 5, 1, 1),
            ("book", None, 8),
        ],
    },
    "ultra_divine": {
        "rolls": [4, 7],
        "entries": [
            ("tag", "c:gems", 18),
            ("tag", "c:ingots", 12),
            ("item", "minecraft:diamond", 16, 5, 10),
            ("item", "minecraft:emerald", 14, 12, 24),
            ("item", "minecraft:experience_bottle", 12, 8, 16),
            ("item", "minecraft:golden_apple", 12, 3, 5),
            ("item", "minecraft:netherite_ingot", 10, 2, 4),
            ("item", "minecraft:heart_of_the_sea", 8, 2, 4),
            ("item", "minecraft:enchanted_golden_apple", 8, 2, 3),
            ("item", "minecraft:totem_of_undying", 6, 1, 2),
            ("item", "minecraft:diamond_block", 6, 1, 1),
            ("book", None, 8),
        ],
    },
    "unspoken": {
        "rolls": [5, 8],
        "entries": [
            ("tag", "c:gems", 16),
            ("tag", "c:ingots", 12),
            ("item", "minecraft:diamond_block", 12, 2, 3),
            ("item", "minecraft:netherite_ingot", 12, 3, 5),
            ("item", "minecraft:experience_bottle", 12, 10, 20),
            ("item", "minecraft:enchanted_golden_apple", 10, 2, 4),
            ("item", "minecraft:totem_of_undying", 10, 2, 3),
            ("item", "minecraft:shulker_shell", 8, 2, 4),
            ("item", "minecraft:heart_of_the_sea", 8, 2, 4),
            ("book", None, 8),
        ],
    },
    "ghost_unspoken": {
        "rolls": [6, 9],
        "entries": [
            ("tag", "c:gems", 14),
            ("tag", "c:ingots", 10),
            ("item", "minecraft:diamond_block", 12, 3, 6),
            ("item", "minecraft:netherite_block", 10, 1, 3),
            ("item", "minecraft:totem_of_undying", 10, 3, 4),
            ("item", "minecraft:enchanted_golden_apple", 10, 3, 5),
            ("item", "minecraft:experience_bottle", 10, 16, 32),
            ("item", "minecraft:heart_of_the_sea", 8, 3, 5),
            ("item", "minecraft:nether_star", 2, 1, 2),
        ],
    },
}


# ---- cross-mod loot ----------------------------------------------------------
# Every crate rolls ONE extra item out of these broad conventional ("c:") item
# tags. NeoForge ships the tags with the vanilla members, and most mods add their
# own items to them, so a player's other mods show up in the crates without those
# mods knowing anything about MultiHooker.
#
# The sets are kept apart on purpose. The vanilla members of the "rich" tags are
# end-game loot (netherite ingots, ancient debris, netherite blocks), so those
# tags are only ever reachable from godly and above - a common crate must never
# hand out netherite.
BONUS_TAGS_COMMON = [
    "c:raw_materials",
    "c:dusts",
    "c:nuggets",
    "c:gems",
    "c:foods/cooked_meat",
]
BONUS_TAGS_RICH = BONUS_TAGS_COMMON + [
    "c:ingots",
    "c:ores",
    "c:storage_blocks",
]
# keyed by crate key; everything below godly uses the common set
BONUS_RICH_TIERS = {"godly", "mythic", "secret", "divine", "ultra_divine", "unspoken", "ghost_unspoken"}
BONUS_TAG_WEIGHT = 10
BONUS_ROLLS = 1

# One named cross-mod item that ONLY the SPECIAL crates may roll: AquaCulture 2's
# Neptune's Bounty. It rides in its own item tag (written by
# write_bonus_compat_tag) whose single member is optional, so the loot table
# still loads cleanly when AquaCulture is not installed - the tag simply
# resolves to nothing and the crate keeps its ordinary bonus roll.
BONUS_SPECIAL_TAG = "multihooker:neptunes_bounty"
BONUS_SPECIAL_ID = "aquaculture:neptunes_bounty"
BONUS_SPECIAL_NAME = "Neptune's Bounty"
BONUS_SPECIAL_WEIGHT = 5
# The special crate keys, derived from tiers.py so the split cannot drift. They
# are the only crates that may roll the named compat item above: the normal
# crates - even godly, mythic and secret - never do.
BONUS_SPECIAL_TIERS = {t["key"] for t in tiers.TIERS if t["group"] == "special"}


def bonus_tags(key):
    """The conventional tags this crate's bonus roll may pull from."""
    return BONUS_TAGS_RICH if key in BONUS_RICH_TIERS else BONUS_TAGS_COMMON


def bonus_pool(key):
    """The crate's second pool: one roll, one item from any installed mod."""
    entries = [
        {"type": "minecraft:tag", "name": tag, "expand": True, "weight": BONUS_TAG_WEIGHT} for tag in bonus_tags(key)
    ]

    # the special crates can ALSO surface the single named compat item.
    if key in BONUS_SPECIAL_TIERS:
        entries.append(
            {"type": "minecraft:tag", "name": BONUS_SPECIAL_TAG, "expand": True, "weight": BONUS_SPECIAL_WEIGHT}
        )

    return {"rolls": {"min": BONUS_ROLLS, "max": BONUS_ROLLS}, "entries": entries}


def bonus_entries(key):
    """The bonus pool as the client screen lists it: the conventional tags, plus
    the named special-crate compat item with the label the screen should show."""
    entries = [{"kind": "tag", "id": tag, "weight": BONUS_TAG_WEIGHT} for tag in bonus_tags(key)]
    if key in BONUS_SPECIAL_TIERS:
        entries.append({"kind": "tag", "id": BONUS_SPECIAL_TAG, "weight": BONUS_SPECIAL_WEIGHT, "name": BONUS_SPECIAL_NAME})
    return entries


def item_entry(name, weight, mn, mx):
    e = {"type": "minecraft:item", "name": name, "weight": weight}
    if mn is not None and mx is not None:
        e["functions"] = [{"function": "minecraft:set_count", "count": {"min": mn, "max": mx}}]
    return e


def write(path, data):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as fh:
        json.dump(data, fh, indent=2)
    print("wrote", os.path.relpath(path, ROOT))


# Block model geometry. The same shape is authored in Blender by build_crate.py
# (which also renders previews); it is mirrored here so all the crate JSONs can
# be regenerated without Blender. Every tier shares this geometry and differs
# only by the texture names.
BOXES = [
    ("body", (0.5, 0, 0.5), (15.5, 12, 15.5), "side"),
    ("lid", (0.5, 12, 0.5), (15.5, 15.5, 15.5), "top"),
]
FACE_TEX = {
    "body": {"up": "side", "down": "side", "north": "side", "south": "front", "east": "side", "west": "side"},
    "lid": {"up": "top", "down": "side", "north": "side", "south": "side", "east": "side", "west": "side"},
}
# The lid's sides are only 3/16 of a block tall, so they sample the top 3/16
# strip of the side texture (its metal band) instead of squashing the whole map.
UV_OVERRIDE = {
    ("lid", "north"): [0, 0, 16, 3],
    ("lid", "south"): [0, 0, 16, 3],
    ("lid", "east"): [0, 0, 16, 3],
    ("lid", "west"): [0, 0, 16, 3],
}

def crate_textures(key):
    return {
        "particle": f"multihooker:block/{key}_crate_side",
        "side": f"multihooker:block/{key}_crate_side",
        "front": f"multihooker:block/{key}_crate_front",
        "top": f"multihooker:block/{key}_crate_top",
        "inside": f"multihooker:block/{key}_crate_inside",
    }


def box_element(name, face_tex):
    """One of the crate's two boxes, with the texture slot given per face."""
    mn, mx = next((mn, mx) for box_name, mn, mx, _ in BOXES if box_name == name)
    faces = {}
    for face, slot in face_tex.items():
        uv = UV_OVERRIDE.get((name, face), [0, 0, 16, 16])
        faces[face] = {"uv": uv, "texture": f"#{slot}"}
    return {"from": list(mn), "to": list(mx), "faces": faces}


def write_model(key, name, elements):
    model = {
        "credit": "Authored in Blender by tools/crategen/build_crate.py",
        "parent": "minecraft:block/block",
        "render_type": "minecraft:cutout",
        "textures": crate_textures(key),
        "elements": elements,
    }
    write(os.path.join(RES, "assets", "multihooker", "models", "block", name), model)


def write_crate_models(key):
    """The three shapes every tier needs.

    <key>_crate       the shut crate: body plus flat lid. Baked by the placeable
                      block and parented by BOTH the crate item and the empty
                      crate item - an empty crate at rest looks like a full one.
    <key>_crate_body  the body alone with its top swapped to the dark interior.
                      This is what the empty crate block bakes, so the open box
                      is already showing from underneath when the lid swings up.
    <key>_crate_lid   the lid alone, its underside the dark interior. Drawn by
                      client.CrateLidRenderer against the crate's back rim, so it
                      is deliberately NOT reachable through any blockstate.
    """
    body = dict(FACE_TEX["body"])
    lid = dict(FACE_TEX["lid"])
    write_model(key, f"{key}_crate.json", [box_element("body", body), box_element("lid", lid)])

    open_body = dict(body)
    open_body["up"] = "inside"
    write_model(key, f"{key}_crate_body.json", [box_element("body", open_body)])

    # An open lid is seen from underneath: the underside becomes the face looking
    # out of the box, so it is the same flat dark interior as the body's top -
    # never the lid's engraved, branded top.
    open_lid = dict(lid)
    open_lid["down"] = "inside"
    write_model(key, f"{key}_crate_lid.json", [box_element("lid", open_lid)])


def make_inside_texture(key):
    """The open crate's inside: one flat, dark, unlit tone, like the inside of a
    vanilla chest. It carries no branding and no planks - the crate's own wood
    colour, averaged out, drained of colour and sunk hard into shadow, with the
    rim darker still so the open top reads as a hollow box rather than a lit lid.
    """
    src = os.path.join(OUT, f"{key}_crate_side.png")
    with Image.open(src) as image:
        image = image.convert("RGBA")
        pixels = image.load()
        width, height = image.size

        # Average the whole side texture down to one tone: the crate's wood colour,
        # with every plank line, band and logo averaged away.
        total = [0.0, 0.0, 0.0]
        count = 0
        for y in range(height):
            for x in range(width):
                red, green, blue, alpha = pixels[x, y]
                if alpha == 0:
                    continue
                total[0] += red
                total[1] += green
                total[2] += blue
                count += 1
        red, green, blue = (channel / count for channel in total) if count else (40.0, 38.0, 32.0)

        # Mostly grey, then dropped to roughly a fifth brightness.
        grey = 0.30 * red + 0.59 * green + 0.11 * blue
        base = (
            max(0, min(255, int((red + (grey - red) * 0.75) * 0.22))),
            max(0, min(255, int((green + (grey - green) * 0.75) * 0.22))),
            max(0, min(255, int((blue + (grey - blue) * 0.75) * 0.22))),
        )

        for y in range(height):
            for x in range(width):
                alpha = pixels[x, y][3]
                # The rim falls off into shadow, so the open top reads as hollow
                # rather than as a flat lid.
                edge = min(x, y, width - 1 - x, height - 1 - y)
                fall = 0.55 + 0.45 * min(edge / 4.0, 1.0)
                pixels[x, y] = (int(base[0] * fall), int(base[1] * fall), int(base[2] * fall), alpha)
        image.save(os.path.join(OUT, f"{key}_crate_inside.png"))


def facing_blockstate(model):
    """The crate's latch is on the SOUTH face of the base model, so facing=south
    is unrotated and the other three facings rotate it clockwise."""
    return {
        "variants": {
            "facing=south": {"model": model},
            "facing=west": {"model": model, "y": 90},
            "facing=north": {"model": model, "y": 180},
            "facing=east": {"model": model, "y": 270},
        }
    }


def self_drop(item):
    return {
        "type": "minecraft:block",
        "pools": [
            {
                "rolls": 1,
                "entries": [{"type": "minecraft:item", "name": f"multihooker:{item}"}],
                "conditions": [{"condition": "minecraft:survives_explosion"}],
            }
        ],
    }


def write_crate_tags():
    """Publish the crate categories as item tags: the special group (divine,
    ultra_divine, unspoken, ghost_unspoken) and everything else as normal.
    Derived straight from tiers.py so the split cannot drift."""
    for group, tag in tiers.GROUPS.items():
        values = [f"multihooker:{t['key']}_crate" for t in tiers.TIERS if t["group"] == group]
        write(
            os.path.join(RES, "data", "multihooker", "tags", "item", f"{tag}.json"),
            {"replace": False, "values": values},
        )


def write_bonus_compat_tag():
    """The optional single-item tag behind the special-crate compat bonus entry
    (see BONUS_SPECIAL_TAG). required:false keeps it harmless when AquaCulture is
    not installed, and generating it here keeps it in step with bonus_pool()."""
    write(
        os.path.join(RES, "data", "multihooker", "tags", "item", "neptunes_bounty.json"),
        {"replace": False, "values": [{"id": BONUS_SPECIAL_ID, "required": False}]},
    )


def write_crate_loot():
    """A small, client-readable summary of every crate's loot, generated from the
    SAME LOOT dict as the real loot tables, so the in-game "Crate Loot" screen can
    never drift from what the crates actually drop. It lives under assets/ (not
    data/) because loot tables are server-side, while this file must always be
    present on the client."""
    out = []
    for t in tiers.TIERS:
        spec = LOOT[t["key"]]
        entries = []
        for entry in spec["entries"]:
            kind = entry[0]
            if kind == "item":
                _, name, weight, mn, mx = entry
                entries.append({"kind": "item", "id": name, "weight": weight, "min": mn, "max": mx})
            elif kind == "tag":
                _, name, weight = entry
                entries.append({"kind": "tag", "id": name, "weight": weight})
            elif kind == "book":
                _, _name, weight = entry
                entries.append({"kind": "book", "id": "minecraft:enchanted_book", "weight": weight})
        out.append(
            {
                "key": t["key"],
                "group": t["group"],
                "rolls": spec["rolls"],
                "entries": entries,
                # the cross-mod bonus roll is its own pool, so the screen shows it
                # separately instead of mixing its weights into the main odds
                "bonus_entries": bonus_entries(t["key"]),
            }
        )
    write(os.path.join(RES, "assets", "multihooker", "crate_loot.json"), {"tiers": out})


def main():
    for t in tiers.TIERS:
        key = t["key"]

        # ---- the placeable, closed crate ----
        write(
            os.path.join(RES, "assets", "multihooker", "blockstates", f"{key}_crate.json"),
            facing_blockstate(f"multihooker:block/{key}_crate"),
        )
        write(
            os.path.join(RES, "assets", "multihooker", "models", "item", f"{key}_crate.json"),
            {"parent": f"multihooker:block/{key}_crate"},
        )
        write(os.path.join(RES, "data", "multihooker", "loot_table", "blocks", f"{key}_crate.json"), self_drop(f"{key}_crate"))

        # ---- the empty crate it becomes once opened ----
        # Same block shape and same four facings. Its own block + item means
        # breaking one gives back a crate that stays empty, and there is no
        # pick-block trick to turn an empty crate into a full one.
        #
        # The block itself bakes only the BODY: the lid is drawn by
        # client.CrateLidRenderer so it can swing, and the item uses the shut
        # model, because a crate that is not being looked into has its lid down.
        write(
            os.path.join(RES, "assets", "multihooker", "blockstates", f"{key}_crate_empty.json"),
            facing_blockstate(f"multihooker:block/{key}_crate_body"),
        )
        write(
            os.path.join(RES, "assets", "multihooker", "models", "item", f"{key}_crate_empty.json"),
            {"parent": f"multihooker:block/{key}_crate"},
        )
        write_crate_models(key)
        write(
            os.path.join(RES, "data", "multihooker", "loot_table", "blocks", f"{key}_crate_empty.json"),
            self_drop(f"{key}_crate_empty"),
        )
        # Models from before the lid became a hinged, animated part of the block.
        stale = os.path.join(RES, "assets", "multihooker", "models", "block", f"{key}_crate_open.json")
        if os.path.exists(stale):
            os.remove(stale)
            print("removed", os.path.relpath(stale, ROOT))

        spec = LOOT[key]
        entries = []
        for entry in spec["entries"]:
            kind = entry[0]
            if kind == "item":
                _, name, weight, mn, mx = entry
                entries.append(item_entry(name, weight, mn, mx))
            elif kind == "tag":
                _, name, weight = entry
                entries.append({"type": "minecraft:tag", "name": name, "expand": True, "weight": weight})
            elif kind == "book":
                _, name, weight = entry
                entries.append(
                    {
                        "type": "minecraft:item",
                        "name": "minecraft:enchanted_book",
                        "weight": weight,
                        "functions": [{"function": "minecraft:enchant_randomly"}],
                    }
                )
        crate_loot = {
            "type": "minecraft:chest",
            "pools": [
                {"rolls": {"min": spec["rolls"][0], "max": spec["rolls"][1]}, "entries": entries},
                bonus_pool(key),
            ],
        }
        write(os.path.join(RES, "data", "multihooker", "loot_table", "crates", f"{key}.json"), crate_loot)

        # copy this tier's textures (side/front/top), plus the derived inside one
        make_inside_texture(key)
        for slot in ("side", "front", "top", "inside"):
            src = os.path.join(OUT, f"{key}_crate_{slot}.png")
            dst = os.path.join(RES, "assets", "multihooker", "textures", "block", f"{key}_crate_{slot}.png")
            shutil.copyfile(src, dst)

    write_crate_tags()
    write_bonus_compat_tag()
    write_crate_loot()


if __name__ == "__main__":
    main()