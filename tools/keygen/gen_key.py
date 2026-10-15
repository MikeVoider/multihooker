"""Generate the AquaKey item texture (16x16).

An upright skeleton key: a thick, aqua-metal ring bow at the top, a stout
two-pixel shaft and two teeth stepping off the bottom-right. Lit from the
top-left (bright upper-left arc, darker lower-right), with a dark rim on the
bow so the hole reads clearly at item size.

Usage:
  python gen_key.py          # write the texture + print an ASCII preview
  python gen_key.py --dry    # print the preview only
"""
import math
import os
import sys

from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
DEST = os.path.join(
    ROOT, "src", "main", "resources", "assets", "multihooker", "textures", "item", "aquakey.png"
)
S = 16

RIM = (12, 68, 82, 255)
CAP = (20, 116, 130, 255)
BODY = (60, 200, 200, 255)
BODY_D = (38, 162, 168, 255)
LIGHT = (136, 240, 224, 255)
SPEC = (242, 255, 253, 255)

BOW_X, BOW_Y, BOW_R, BOW_IN = 7.5, 4.6, 4.0, 1.4
SHAFT_TOP = 7.2


def part(x, y):
    """Which piece of the key covers this pixel (or None for empty)."""
    cx, cy = x + 0.5, y + 0.5
    if BOW_IN <= math.hypot(cx - BOW_X, cy - BOW_Y) <= BOW_R:
        return "bow"
    if cy >= SHAFT_TOP and 7.0 <= cx <= 9.0:
        return "shaft"
    if y == 11 and 9.0 <= cx <= 11.0:
        return "tooth"
    if y == 13 and 9.0 <= cx <= 10.0:
        return "tooth"
    return None


def rim(x, y):
    for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
        if part(x + dx, y + dy) is None:
            return True
    return False


def build():
    img = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    for y in range(S):
        for x in range(S):
            p = part(x, y)
            if not p:
                continue
            cx, cy = x + 0.5, y + 0.5
            if p == "bow":
                if rim(x, y):
                    c = RIM
                else:
                    t = ((cx - BOW_X) + (cy - BOW_Y)) / (BOW_R * 1.5)
                    c = LIGHT if t < -0.3 else (BODY_D if t > 0.3 else BODY)
            elif p == "shaft":
                c = CAP if y >= 15 else (LIGHT if x <= 7 else BODY_D)
            else:  # tooth
                c = BODY if x == 9 else BODY_D
            img.putpixel((x, y), c)
    img.putpixel((6, 2), SPEC)          # glint on the lit arc of the bow
    return img


def preview(img):
    ramp = " .:-=+*#%@"
    print("   " + "".join(str(x % 10) for x in range(S)))
    for y in range(S):
        row = ""
        for x in range(S):
            r, g, b, a = img.getpixel((x, y))
            row += " " if a < 16 else ramp[min(9, int((0.299 * r + 0.587 * g + 0.114 * b) / 255 * 9.99))]
        print(f"{y:02d} {row}")


def main():
    img = build()
    preview(img)
    if "--dry" not in sys.argv:
        img.save(DEST)
        print("wrote", DEST)


if __name__ == "__main__":
    main()
