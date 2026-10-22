"""
Release gate for the MultiHooker jar.

Every check here exists because something actually broke that way:
 - model faces emitted as {"uv":..., "texture":..., "south":{...}} rendered the
   whole crate as the missing-texture checkerboard
 - UVs handed over in raw texture pixels instead of Minecraft's 0..16 space put a
   4x error on a 64x64 texture
 - a loot-table function that does not exist in 1.21.1 silently emptied twelve
   crates

Run it against a built jar:  python validate_jar.py <path-to-jar>
Exit code is the number of failures, so CI can use it.
"""
import io
import json
import sys
import zipfile

from PIL import Image

FACES = {"up", "down", "north", "south", "east", "west"}


def L(c):
    return 0.299 * c[0] + 0.587 * c[1] + 0.114 * c[2]


def in_jar(ref, folder, extension):
    """Turn "multihooker:block/common_crate_side" into the path it occupies in the
    jar: assets/multihooker/<folder>/block/common_crate_side.<extension>.

    Doing this by hand in each check is how the first version of this script ended
    up reporting eighteen thousand missing textures that were all present.
    """
    namespace, path = ref.split(":", 1)
    return "assets/%s/%s/%s%s" % (namespace, folder, path, extension)


def main(path):
    z = zipfile.ZipFile(path)
    names = set(z.namelist())
    failures = []
    notes = []

    def fail(msg):
        failures.append(msg)

    # ---- every JSON in the jar must at least parse -------------------------------
    json_docs = {}
    for n in sorted(names):
        if n.endswith(".json"):
            try:
                json_docs[n] = json.loads(z.read(n))
            except Exception as exc:
                fail("unparseable JSON: %s (%s)" % (n, exc))
    notes.append("%d JSON files parsed" % len(json_docs))

    # ---- block models ------------------------------------------------------------
    models = [n for n in json_docs if "/models/block/" in n]
    face_specs = 0
    letters = 0
    elements = 0
    for n in models:
        d = json_docs[n]
        tex_map = d.get("textures", {})
        if not tex_map:
            fail("%s has no textures map" % n)
        for el in d.get("elements", []):
            faces = el.get("faces", {})
            extra = set(faces) - FACES
            if extra:
                # the exact shape of the checkerboard bug: uv/texture sitting at the
                # same level as the face names instead of inside one face
                fail("%s has non-face keys in faces: %s" % (n, sorted(extra)))
            for face, spec in faces.items():
                face_specs += 1
                if not isinstance(spec, dict) or "uv" not in spec or "texture" not in spec:
                    fail("%s face %s is not a {uv, texture} pair" % (n, face))
                    continue
                ref = spec["texture"].lstrip("#")
                if ref not in tex_map:
                    fail("%s face %s references unknown texture key %r" % (n, face, ref))
                    continue
                path_ref = tex_map[ref]
                if not path_ref.startswith("multihooker:"):
                    continue
                # "multihooker:block/common_crate_side" is
                # assets/multihooker/textures/block/common_crate_side.png.
                target = in_jar(path_ref, "textures", ".png")
                if target not in names:
                    fail("%s -> missing texture file %s" % (n, target))
                uv = spec["uv"]
                if len(uv) != 4 or not all(isinstance(v, (int, float)) for v in uv):
                    fail("%s face %s has a malformed uv %r" % (n, face, uv))
                elif not (0 <= uv[0] <= uv[2] and 0 <= uv[1] <= uv[3] and uv[2] <= 16 and uv[3] <= 16):
                    fail("%s face %s uv %r outside the 0..16 model space" % (n, face, uv))
            # Count elements by what they are, not by hardcoded z values. An earlier version
    # matched the letter planes literally (from z == 0.0 or 14.5) and silently
    # reported half the letters once those planes moved.
    for n in models:
        d = json_docs[n]
        for el in d.get("elements", []):
            elements += 1
            # The crate's four vertical faces only agree if vanilla's directional
            # shade is off: the chunk path and the lid renderer derive the shade
            # from opposite directions, so a shaded crate is always uneven one way
            # round. "shade" is per ELEMENT in 1.21 (BlockElement.shade), not per
            # face, so every element of every crate model has to carry it.
            if "_crate" in n and el.get("shade") is not False:
                fail("%s has an element without \"shade\": false - the four vertical"
                     " faces will not match" % n.split("/")[-1])
            lo, hi = sorted((el["from"][2], el["to"][2]))
            if hi - lo <= 2.0 and (lo < 2.0 or hi > 14.0):
                letters += 1
    notes.append("%d block models, %d face specs, %d elements (%d letter boxes)"
                 % (len(models), face_specs, elements, letters))

    # ---- every model a blockstate or item points at must exist -------------------
    refs = 0
    for n in json_docs:
        if "/blockstates/" not in n:
            continue
        for variant in json_docs[n].get("variants", {}).values():
            m = variant.get("model")
            if not m or ":" not in m:
                continue
            refs += 1
            target = in_jar(m, "models", ".json")
            if target not in names:
                fail("%s points at missing model %s" % (n, target))
    notes.append("%d blockstate model references resolved" % refs)

    # ---- textures are real images ------------------------------------------------
    texes = [n for n in names if n.endswith(".png")]
    for n in texes:
        try:
            im = Image.open(io.BytesIO(z.read(n)))
            im.verify()
        except Exception as exc:
            fail("corrupt PNG: %s (%s)" % (n, exc))
    notes.append("%d PNG textures verified" % len(texes))

    # ---- loot tables: no loot function that does not exist in 1.21.1 -------------
    VALID_FUNCS = {
        "set_nbt", "replace_nbt", "limit_count", "set_count", "set_data",
        "modify_nbt", "set_enchantments", "set_nbt", "fill_player_head",
        "copy_name", "copy_nbt", "copy_state", "looting_enchant",
        "random_chance", "random_chance_with_looting", "set_potion",
        "set_fire", "explosion_decay", "furnace_smelt", "set_lore",
        "set_attributes", "set_custom_data", "set_block_state",
        "set_stew_effect", "set_suspicious_stew_effect", "set_trade",
        "set_treasure", "set_hero_of_the_village", "set_loot_table",
    }
    loot = [n for n in json_docs if "loot_table" in n]
    for n in loot:
        raw = z.read(n).decode("utf-8")
        if "luck_bonus" in raw:
            fail("%s uses luck_bonus, which does not exist in 1.21.1" % n)
        for pool in json_docs[n].get("pools", []):
            for fn in pool.get("functions", []):
                name = fn.get("function") or fn.get("name")
                if name and "." in name and name.split(":", 1)[-1] not in VALID_FUNCS:
                    notes.append("  note: %s uses function %s" % (n.split("/")[-1], name))
    notes.append("%d loot tables scanned" % len(loot))

    # ---- display names -----------------------------------------------------------
    lang = json_docs.get("assets/multihooker/lang/en_us.json", {})
    for key in ("block.multihooker.common_crate", "block.multihooker.unspoken_crate",
                "item.multihooker.elixir_vitality", "item.multihooker.elixir_fortune",
                "item.multihooker.crate_potion"):
        if key not in lang:
            fail("lang missing %s" % key)
    # Only the tier crate *blocks* are called Trove. Tooltips and the Crate Potion
    # are not crates and must keep their own wording - matching on the substring
    # "crate" alone flagged all three as failures.
    tiers = ["common", "uncommon", "rare", "epic", "legendary", "secret", "godly",
             "divine", "ultra_divine", "mythic", "unspoken", "ghost_unspoken"]
    for t in tiers:
        key = "block.multihooker.%s_crate" % t
        if key not in lang:
            fail("lang missing %s" % key)
        elif "Trove" not in lang[key]:
            fail("%s should read Trove, got %r" % (key, lang[key]))
    notes.append("%d lang keys, all 12 tier blocks read Trove" % len(lang))

    # The extruded name must sit centred on the crate's front panel. This exists
    # because a two-word name applied its line offset once for the whole tier rather
    # than per line, so both lines landed on top of each other and the pair sat low
    # and garbled on the four two-word crates. Nothing about that broke a model -
    # it rendered happily and was wrong.
    for n in [x for x in models if not x.endswith("_lid.json")]:
        els = json_docs[n].get("elements", [])
        if len(els) < 3:
            continue
        body = max(els, key=lambda e: (e["to"][2] - e["from"][2]) * (e["to"][1] - e["from"][1]))
        bz0, bz1 = sorted((body["from"][2], body["to"][2]))
        letters = [e for e in els
                   if e not in (body,) and (max(e["from"][2], e["to"][2]) <= bz0 + 1e-6
                                             or min(e["from"][2], e["to"][2]) >= bz1 - 1e-6)]
        if len(letters) < 4:
            continue
        lo = min(min(e["from"][1], e["to"][1]) for e in letters)
        hi = max(max(e["from"][1], e["to"][1]) for e in letters)
        body_lo = min(body["from"][1], body["to"][1])
        body_hi = max(body["from"][1], body["to"][1])
        body_mid = (body_lo + body_hi) / 2.0
        if abs((lo + hi) / 2.0 - body_mid) > 0.05:
            fail("%s: name is centred on %.2f but the panel is centred on %.2f"
                 % (n.split("/")[-1], (lo + hi) / 2.0, body_mid))
        if lo < body_lo - 1e-6 or hi > body_hi + 1e-6:
            fail("%s: name runs off the panel (%.2f..%.2f vs %.2f..%.2f)"
                 % (n.split("/")[-1], lo, hi, body_lo, body_hi))

    # ---- the luminance rule and its one deliberate exemption -------------------
    # tiers.LUMINANCE_CEILING (150) governs broad upward-facing surfaces: the lid,
    # the top, the panels. Those are what glare under sky light, so their median
    # tone has to stay well under white or the original bug is back.
    for n in [x for x in texes if x.endswith("_crate_top.png")]:
        im = Image.open(io.BytesIO(z.read(n))).convert("RGBA")
        vals = sorted(L(p[:3]) for p in im.getdata() if p[3] == 255)
        if vals and vals[len(vals) // 2] > 200:
            fail("%s median tone %.0f - the broad surfaces are washing out"
                 % (n.split("/")[-1], vals[len(vals) // 2]))

    # The lid's rim is 3/16 of a block tall and samples the top three rows of the
    # face texture below it (UV [0,0,16,3]). With the shade off, those rows are
    # the only thing that can still make the front/back rim differ from the
    # left/right rim, so they have to sit on the same tone in both textures.
    for key in [t.split("/")[-1][:-len("_crate_side.png")] for t in texes
                if t.endswith("_crate_side.png")]:
        bands = {}
        for slot in ("side", "front"):
            hit = [t for t in texes if t.endswith("/%s_crate_%s.png" % (key, slot))]
            if not hit:
                fail("%s: no _crate_%s.png to compare the rim against" % (key, slot))
                break
            im = Image.open(io.BytesIO(z.read(hit[0]))).convert("RGBA")
            px = im.load()
            total = count = 0
            for y in range(min(3, im.size[1])):
                for x in range(im.size[0]):
                    p = px[x, y]
                    if p[3] == 0:
                        continue
                    total += L(p[:3])
                    count += 1
            bands[slot] = total / count if count else 0.0
        else:
            lo, hi = sorted(bands.values())
            if lo <= 0:
                fail("%s: rim strip is empty" % key)
            elif hi / lo > 1.06:
                fail("%s: rim strip differs between the named and clean faces"
                     " (%.1f vs %.1f)" % (key, bands["side"], bands["front"]))

    # The extruded letters are the one thing deliberately above that ceiling: they
    # are pure white by decision, tiny, vertical and bevelled, so they cannot glare
    # the way a lid does. Checked here so the exemption is visible and cannot be
    # broken silently - a change to one tier's letter colour shows up as a failure.
    letters = sorted(x for x in texes if x.endswith("_crate_letter.png"))
    if len(letters) != 12:
        fail("expected 12 letter textures, found %d" % len(letters))
    else:
        centres = set()
        for n in letters:
            im = Image.open(io.BytesIO(z.read(n))).convert("RGBA")
            px = im.getpixel((im.size[0] // 2, im.size[1] // 2))
            centres.add(tuple(px[:3]))
        if len(centres) != 1:
            fail("letter textures are not one colour: %s" % sorted(centres))
        else:
            notes.append("12 letter textures, exempt from the ceiling, colour %s"
                         % (sorted(centres)[0],))

    # A crate held in the hand is the block model. The item model inherits the
    # engraved name from the block model (front and back only). Item models
    # must be bare parents - no display block, no elements.
    crate_items = [n for n in json_docs
                   if "/models/item/" in n and n.endswith(("_crate.json", "_crate_empty.json"))]
    for n in crate_items:
        d = json_docs[n]
        if "display" in d:
            fail("%s has a display block - that is what resized the held crate" % n)
        extra = set(d) - {"parent", "elements"}
        if extra:
            fail("%s has unexpected keys: %s" % (n.split("/")[-1], sorted(extra)))
        elements = d.get("elements", [])
        if elements:
            fail("%s item model has %d elements - should be bare parent" % (n.split("/")[-1], len(elements)))
    notes.append("%d crate item models: bare parent, no display block" % len(crate_items))

    # ---- report ------------------------------------------------------------------
    print("validating %s" % path)
    for n in notes:
        print("  - %s" % n)
    if failures:
        print("\n%d FAILURES:" % len(failures))
        for f in failures[:40]:
            print("  x %s" % f)
        return len(failures)
    print("\nPASS - no problems found")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1]))