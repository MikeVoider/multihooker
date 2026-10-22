"""
Render the crate name at six protrusion depths so the depth can be chosen by eye.

Each depth is a real regeneration: gen_resources.py is rewritten with that
depth's face planes, the models are rebuilt, and Blender renders the result from
the shipped model JSONs. So the six tiles are not an illustration of the range -
they are what each depth actually produces.

A letter at depth d spans (1-d)..0 on the front and 0..(1-d) on the back, reaching
the block edge; the panel is pulled back by d to make room.

The source is restored to its starting depth when the run finishes, whatever
happens in between.

    python depth_sweep.py
"""
import io
import os
import re
import shutil
import subprocess
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.normpath(os.path.join(HERE, "..", ".."))
GEN = os.path.join(HERE, "gen_resources.py")
BLENDER = r"C:\Program Files\Blender Foundation\Blender 5.2\blender.exe"
SWEEP = os.path.join(HERE, "out", "sweep")

DEPTHS = [0.20, 0.35, 0.50, 0.65, 0.80, 1.00]


def set_depth(source, depth):
    """Face planes for a given protrusion: the panel steps back by `depth` and
    the letters reach the block edge from there."""
    source = re.sub(r"^FRONT_Z = .*$", "FRONT_Z = %.4f" % (16.0 - depth),
                    source, flags=re.M)
    source = re.sub(r"^BACK_Z = .*$", "BACK_Z = %.4f" % depth,
                    source, flags=re.M)
    return source


def main():
    if not os.path.isfile(BLENDER):
        print("Blender not found at %s" % BLENDER)
        return 1
    original = io.open(GEN, encoding="utf-8").read()
    os.makedirs(SWEEP, exist_ok=True)
    try:
        for depth in DEPTHS:
            io.open(GEN, "w", encoding="utf-8").write(set_depth(original, depth))
            subprocess.run([sys.executable, "gen_resources.py"], cwd=HERE,
                           capture_output=True, text=True)
            subprocess.run([BLENDER, "--background", "--python",
                            "render_blender.py", "--", "common"],
                           cwd=HERE, capture_output=True, text=True)
            src = os.path.join(HERE, "preview", "blender", "common_shut.png")
            dst = os.path.join(SWEEP, "depth_%.2f.png" % depth)
            if os.path.isfile(src):
                shutil.copyfile(src, dst)
                print("  rendered depth %.2f" % depth)
            else:
                print("  FAILED at depth %.2f - no render produced" % depth)
    finally:
        # always put the generator back the way it was
        io.open(GEN, "w", encoding="utf-8").write(original)
        subprocess.run([sys.executable, "gen_resources.py"], cwd=HERE,
                       capture_output=True, text=True)
        print("  restored gen_resources.py and rebuilt the models")
    return 0


if __name__ == "__main__":
    sys.exit(main())