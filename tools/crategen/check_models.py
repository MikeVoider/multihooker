"""Sanity-check every generated block model JSON.

Guards against the class of bug that shipped once already: Minecraft block-model
UVs live in 0..16 model space (16 = the WHOLE sprite), NOT the texture's pixel
size, so stamping [0,0,32,32] on a 32x32 texture samples past the sprite and the
block renders garbled in game.

Run from tools/crategen:  python check_models.py
"""
import json
import glob
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.normpath(os.path.join(HERE, "..", ".."))
RES = os.path.join(ROOT, "src", "main", "resources")
BLOCK_MODELS = os.path.join(RES, "assets", "multihooker", "models", "block")
TEX_DIR = os.path.join(RES, "assets", "multihooker", "textures", "block")

FACES = ("up", "down", "north", "south", "east", "west")


def main():
    problems = []
    models = sorted(
        glob.glob(os.path.join(BLOCK_MODELS, "*_crate.json"))
        + glob.glob(os.path.join(BLOCK_MODELS, "*_crate_body.json"))
        + glob.glob(os.path.join(BLOCK_MODELS, "*_crate_lid.json"))
    )
    for path in models:
        name = os.path.basename(path)
        with open(path, encoding="utf-8") as fh:
            m = json.load(fh)

        # texture slots must point at files that exist
        for slot, ref in m.get("textures", {}).items():
            if not ref.startswith("multihooker:block/"):
                continue
            tex = ref.split("/", 1)[1] + ".png"
            if not os.path.exists(os.path.join(TEX_DIR, tex)):
                problems.append(f"{name}: texture slot '{slot}' -> missing {tex}")

        for i, el in enumerate(m.get("elements", [])):
            for face, data in el.get("faces", {}).items():
                if face not in FACES:
                    problems.append(f"{name}: element {i} unknown face '{face}'")
                uv = data.get("uv")
                if uv is None:
                    continue
                if len(uv) != 4 or any(u < 0 or u > 16 for u in uv):
                    problems.append(f"{name}: element {i} face {face} UV {uv} out of 0..16")

    if problems:
        print("PROBLEMS:")
        for p in problems:
            print("  -", p)
        sys.exit(1)
    print(f"OK: {len(models)} crate models (shut + body + lid), textures resolve, all UVs within 0..16")


if __name__ == "__main__":
    main()
