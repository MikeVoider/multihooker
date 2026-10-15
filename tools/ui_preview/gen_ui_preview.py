"""Before/after mock-up of the crate UIs, rendered from real game assets.

Two screens answer "the crate UI": the JEI recipe page you get by pressing R on
a crate, and the in-game Crate Loot screen. This script mocks both, as they were
("before") and as they are now ("after"), so the layout can be judged without
launching the game.

Real assets: vanilla slot sprite and item/block textures out of the 1.21.1
client jar, JEI's own slot sprites out of the JEI jar, and this mod's crate
textures.

Honest limits: item icons are pasted flat from the 16x16 textures (in game,
blocks are drawn as cubes and the crate as its block model), and the text uses
Arial, because the Minecraft font is not a plain TTF file. Slot sprites, sizes,
pitch and positions are the real ones.

Run:  python tools/ui_preview/gen_ui_preview.py
"""

from __future__ import annotations

import json
import os
import re
import zipfile

from PIL import Image, ImageDraw, ImageFont

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(os.path.dirname(HERE))
OUT = os.path.join(HERE, "preview")

MC_JAR = os.environ.get("MC_JAR", r"C:\Users\Mikeg\curseforge\minecraft\Install\versions\1.21.1\1.21.1.jar")
JEI_JAR = os.environ.get(
   "JEI_JAR",
   r"C:\Users\Mikeg\curseforge\minecraft\Instances\Neoforge Mike ModPack\mods\jei-1.21.1-neoforge-19.64.0.454.jar",
)

# ---- geometry copied from CrateLootRecipeCategory.java -----------------------
JEI_COLUMNS = 4
JEI_SLOT = 18
JEI_GRID_X = 24
JEI_GRID_Y = 4

# ---- geometry copied from CrateLootScreen.java -------------------------------
PANEL_WIDTH = 384
PANEL_HEIGHT = 268
LEFT_X = 8
LEFT_W = 132
RIGHT_X = 150
RIGHT_W = 224
LIST_TOP = 30
ROW_H = 17
SLOT = 18
ENTRY_TOP = 48
ENTRY_H = 18

# ---- colours, as the Java ints -----------------------------------------------
C_TITLE = -10929
C_LABEL = -6497026
C_DIM = -8355712
C_FILL = -871362544
C_EDGE = -11184811

# ---- item display names used by the mock ------------------------------------
NAMES = {
   "diamond": "Diamond",
   "emerald": "Emerald",
   "gold_ingot": "Gold Ingot",
   "iron_ingot": "Iron Ingot",
   "copper_ingot": "Copper Ingot",
   "coal": "Coal",
   "stick": "Stick",
   "bread": "Bread",
   "oak_log": "Oak Log",
   "string": "String",
   "leather": "Leather",
   "lapis_lazuli": "Lapis Lazuli",
   "experience_bottle": "Bottle o' Enchanting",
   "golden_apple": "Golden Apple",
   "enchanted_book": "Enchanted Book",
   "netherite_scrap": "Netherite Scrap",
   "ender_pearl": "Ender Pearl",
   "netherite_ingot": "Netherite Ingot",
   "heart_of_the_sea": "Heart of the Sea",
   "enchanted_golden_apple": "Enchanted Golden Apple",
   "nautilus_shell": "Nautilus Shell",
   "totem_of_undying": "Totem of Undying",
   "diamond_block": "Block of Diamond",
   "netherite_block": "Block of Netherite",
   "shulker_shell": "Shulker Shell",
   "nether_star": "Nether Star",
}


def rgb(argb):
   v = argb & 0xFFFFFFFF
   return ((v >> 16) & 0xFF, (v >> 8) & 0xFF, v & 0xFF)


def rgba(argb):
   v = argb & 0xFFFFFFFF
   return ((v >> 24) & 0xFF, (v >> 16) & 0xFF, (v >> 8) & 0xFF, v & 0xFF)


class Assets:
   def __init__(self, mc_path, jei_path):
      self.mc = zipfile.ZipFile(mc_path)
      self.jei = zipfile.ZipFile(jei_path)
      self.mc_names = set(self.mc.namelist())
      self.icons = {}
      self.vanilla_slot = self.mc_png("assets/minecraft/textures/gui/sprites/container/slot.png")
      self.jei_slot = self.jei_png("assets/jei/textures/gui/sprites/slot.png")
      self.jei_output = self.jei_png("assets/jei/textures/gui/sprites/output_slot.png")

   def mc_png(self, path):
      with self.mc.open(path) as handle:
         return Image.open(handle).convert("RGBA")

   def jei_png(self, path):
      with self.jei.open(path) as handle:
         return Image.open(handle).convert("RGBA")

   def item(self, item_id):
      """Flat stand-in for a minecraft:* item icon."""
      if item_id not in self.icons:
         name = item_id.split(":")[-1]
         icon = None
         for folder in ("item", "block"):
            path = f"assets/minecraft/textures/{folder}/{name}.png"
            if path in self.mc_names:
               icon = self.mc_png(path)
               break
         if icon is None:
            icon = Image.new("RGBA", (16, 16), (255, 0, 255, 255))
         self.icons[item_id] = icon
      return self.icons[item_id]

   def mod_png(self, *parts):
      return Image.open(os.path.join(ROOT, "src", "main", "resources", "assets", "multihooker", *parts)).convert("RGBA")

   def crate_icon(self, key):
      """A 16px stand-in for the crate's 3D block model, from its front texture."""
      return self.mod_png("textures", "block", f"{key}_crate_front.png").resize((16, 16), Image.LANCZOS)

   def crate_top(self, key):
      return self.mod_png("textures", "block", f"{key}_crate_top.png").resize((16, 16), Image.LANCZOS)

   def aquakey(self):
      return self.mod_png("textures", "item", "aquakey.png")


def load_loot():
   with open(os.path.join(ROOT, "src", "main", "resources", "assets", "multihooker", "crate_loot.json"), encoding="utf-8") as handle:
      return json.load(handle)


def load_lang():
   directory = os.path.join(ROOT, "src", "main", "resources", "assets", "multihooker", "lang", "en_us.json")
   with open(directory, encoding="utf-8") as handle:
      return json.load(handle)


def load_chances():
   """key -> default chance, straight out of Config.java."""
   out = {}
   with open(os.path.join(ROOT, "src", "main", "java", "com", "example", "multihooker", "Config.java"), encoding="utf-8") as handle:
      text = handle.read()
   for camel, value in re.findall(r'"([a-zA-Z]+)CrateChance",\s*([0-9.]+)', text):
      key = re.sub(r"(?<!^)(?=[A-Z])", "_", camel).lower()
      out[key] = float(value)
   return out


def entries_label(entry):
   name = NAMES.get(entry["id"].split(":")[-1], entry["id"])
   if entry["kind"] == "tag":
      return "Any #" + entry["id"] + " item"
   low = entry.get("min", 1)
   high = entry.get("max", low)
   if low <= 1 and high <= 1:
      return name
   return f"{name} x{low}" if low == high else f"{name} {low}-{high}"


# --------------------------------------------------------------------------- JEI
def jei_page(assets, tier, after):
   width = JEI_GRID_X + JEI_COLUMNS * JEI_SLOT + 10
   height = JEI_GRID_Y + 3 * JEI_SLOT + 10
   page = Image.new("RGBA", (width, height), (0, 0, 0, 0))
   pad = 6

   def slot(x, y, sprite, dx, dy):
      page.alpha_composite(sprite, (pad + x + dx, pad + y + dy))

   def item(image, x, y):
      page.alpha_composite(image, (pad + x, pad + y))

   slot(2, JEI_GRID_Y, assets.jei_slot, -1, -1)
   item(assets.crate_icon(tier["key"]), 2, JEI_GRID_Y)
   slot(2, JEI_GRID_Y + JEI_SLOT + 2, assets.jei_slot, -1, -1)
   item(assets.aquakey(), 2, JEI_GRID_Y + JEI_SLOT + 2)

   icons = []
   for index, entry in enumerate(tier["entries"]):
      if entry["kind"] == "tag":
         icons.append(None)
      else:
         icons.append(assets.item(entry["id"]))

   for index, icon in enumerate(icons):
      x = JEI_GRID_X + (index % JEI_COLUMNS) * JEI_SLOT
      y = JEI_GRID_Y + (index // JEI_COLUMNS) * JEI_SLOT
      if after:
         slot(x, y, assets.jei_slot, -1, -1)
      else:
         # JEI's output background: a 26x26 sprite drawn at (-5,-5).
         slot(x, y, assets.jei_output, -5, -5)
      if icon is not None:
         item(icon, x, y)

   return page


# --------------------------------------------------------------------------- GUI
class Screen:
   def __init__(self, size, font):
      self.image = Image.new("RGBA", size, (0, 0, 0, 0))
      self.draw = ImageDraw.Draw(self.image)
      self.font = font
      self.pos = (0, 0)

   def at(self, x, y):
      self.pos = (int(x), int(y))

   def text(self, string, colour, shadow=True):
      x, y = self.pos
      if shadow:
         self.draw.text((x + 1, y + 1), string, font=self.font, fill=(30, 30, 30, 255))
      self.draw.text((x, y), string, font=self.font, fill=tuple(colour) + (255,))

   def text_right(self, string, colour):
      x, y = self.pos
      self.draw.text((x - self.font.getlength(string), y), string, font=self.font, fill=tuple(colour) + (255,))


def button(screen, x, y, w, h, label):
   screen.draw.rectangle((x, y, x + w - 1, y + h - 1), fill=(72, 72, 72, 255), outline=(160, 160, 160, 255))
   screen.draw.line((x + 1, y + 1, x + w - 2, y + 1), fill=(120, 120, 120, 255))
   screen.at(x + (w - screen.font.getlength(label)) / 2, y + (h - 8) / 2)
   screen.text(label, (255, 255, 255))


def crate_screen(assets, data, font, tier_key, after, chances):
   tiers = data["tiers"]
   selected = next(i for i, t in enumerate(tiers) if t["key"] == tier_key)
   tier = tiers[selected]
   screen = Screen((PANEL_WIDTH, PANEL_HEIGHT), font)

   def title_for(t):
      pretty = "".join(" " + ch if ch.isupper() else ch for ch in t).strip()
      return pretty + " Crate"

   def panel_base():
      screen.image.alpha_composite(Image.new("RGBA", (PANEL_WIDTH, PANEL_HEIGHT), rgba(C_FILL)), (0, 0))
      screen.draw.rectangle((0, 0, PANEL_WIDTH - 1, PANEL_HEIGHT - 1), outline=rgb(C_EDGE) + (255,))

   def tier_list():
      for index, t in enumerate(tiers):
         label = ("> " if index == selected else "") + title_for(t["key"])
         button(screen, LEFT_X, LIST_TOP + index * ROW_H, LEFT_W, 16, label)

   def done_button(x, w=120):
      button(screen, x, PANEL_HEIGHT - 26, w, 18, "Done")

   def heading():
      screen.draw.rectangle((0, 0, 0, 0))
      screen.at((PANEL_WIDTH - font.getlength("Crate Loot")) / 2, 8)
      screen.text("Crate Loot", rgb(C_TITLE))

   stats = f"Rolls {tier['rolls'][0]}-{tier['rolls'][1]}   {len(tier['entries'])} drops   {chances[tier['key']] * 100:g}% catch"
   total = sum(e["weight"] for e in tier["entries"])

   if after:
      panel_base()
      tier_list()
      done_button(LEFT_X, LEFT_W)
      heading()
      screen.at(LEFT_X, 20)
      screen.text("Odds are per roll", rgb(C_DIM))
      screen.image.alpha_composite(assets.vanilla_slot, (RIGHT_X, 20))
      screen.image.alpha_composite(assets.crate_icon(tier["key"]), (RIGHT_X + 1, 21))
      screen.at(RIGHT_X + SLOT + 6, 23)
      screen.text(title_for(tier["key"]), rgb(C_TITLE))
      screen.at(RIGHT_X + SLOT + 6, 36)
      screen.text(stats, rgb(C_DIM))
      pitch, top, text_dx = ENTRY_H, ENTRY_TOP, SLOT + 6
   else:
      # The old screen draws super.render() first and then darkens the panel
      # over it, so the two buttons are covered by the overlay.
      tier_list()
      done_button((PANEL_WIDTH - 120) / 2)
      panel_base()
      heading()
      screen.image.alpha_composite(assets.crate_icon(tier["key"]), (RIGHT_X, 24))
      screen.at(RIGHT_X + 20, 25)
      screen.text(title_for(tier["key"]), rgb(C_TITLE))
      screen.at(RIGHT_X + 20, 38)
      screen.text(stats, rgb(C_DIM))
      screen.at(LEFT_X, PANEL_HEIGHT - 20)
      screen.text("Odds are per roll (higher tier = more rolls, better loot)", rgb(C_DIM))

   pitch = ENTRY_H if after else 14
   top = ENTRY_TOP if after else 54
   room = (PANEL_HEIGHT - 4 - ENTRY_TOP) // ENTRY_H

   for index, entry in enumerate(tier["entries"]):
      if after and index >= room:
         break
      y = top + index * ENTRY_H if after else top + index * 14
      icon = None if entry["kind"] == "tag" else assets.item(entry["id"])
      if after:
         screen.image.alpha_composite(assets.vanilla_slot, (RIGHT_X, y))
         if icon is not None:
            screen.image.alpha_composite(icon, (RIGHT_X + 1, y + 1))
         text_y = y + 5
         percent_x = RIGHT_X + RIGHT_W
      else:
         if icon is not None:
            screen.image.alpha_composite(icon, (RIGHT_X, y))
         text_y = y + 4
         percent_x = RIGHT_X + RIGHT_W
      screen.at(RIGHT_X + 24, text_y)
      screen.text(entries_label(entry), rgb(C_LABEL))
      if entry["kind"] == "tag":
         percent = f"{round(100.0 * entry['weight'] / total)}%"
      else:
         percent = f"{round(100.0 * entry['weight'] / total)}%"
      screen.at(percent_x, text_y)
      screen.text_right(percent, rgb(C_DIM))

   return screen.image


def compose(assets, data, chances):
   tier = next(t for t in data["tiers"] if t["key"] == "ultra_divine")
   font = ImageFont.truetype(os.path.join(os.environ.get("WINDIR", r"C:\Windows"), "Fonts", "arial.ttf"), 8)
   small = ImageFont.truetype(os.path.join(os.environ.get("WINDIR", r"C:\Windows"), "Fonts", "arial.ttf"), 7)

   jei_before = jei_page(assets, tier, False)
   jei_after = jei_page(assets, tier, True)
   gui_before = crate_screen(assets, data, font, tier["key"], False, chances)
   gui_after = crate_screen(assets, data, font, tier["key"], True, chances)

   pad = 16
   zoom = 3
   jei_w, jei_h = jei_before.size[0] * zoom, jei_before.size[1] * zoom
   width = pad * 3 + max(jei_w * 2 + pad, PANEL_WIDTH * 2 + pad)
   height = pad * 4 + 30 + jei_h + 30 + PANEL_HEIGHT
   sheet = Image.new("RGBA", (width, height), (28, 28, 32, 255))
   draw = ImageDraw.Draw(sheet)

   caption = lambda x, y, text, fill= (230, 230, 230, 255), f=None: draw.text((x, y), text, font=f or small, fill=fill)
   caption(pad, 12, "MultiHooker - crate UI before / after", (255, 255, 255, 255), ImageFont.truetype(
      os.path.join(os.environ.get("WINDIR", r"C:\Windows"), "Fonts", "arial.ttf"), 12))

   y = 40
   caption(pad, y, "JEI page (press R on a crate)", (200, 200, 255, 255))
   caption(pad + jei_w + pad, y, "loot squares no longer overlap", (200, 200, 255, 255))
   y += 14
   sheet.alpha_composite(jei_before.resize((jei_w, jei_h), Image.NEAREST), (pad, y))
   sheet.alpha_composite(jei_after.resize((jei_w, jei_h), Image.NEAREST), (pad + jei_w + pad, y))
   caption(pad, y + jei_h + 2, "BEFORE", (255, 130, 130, 255))
   caption(pad + jei_w + pad, y + jei_h + 2, "AFTER", (130, 255, 130, 255))

   y += jei_h + 22
   caption(pad, y, "Crate Loot screen (opened from the crate)", (200, 200, 255, 255))
   y += 12
   sheet.alpha_composite(gui_before, (pad, y))
   sheet.alpha_composite(gui_after, (pad + PANEL_WIDTH + pad, y))
   caption(pad, y + PANEL_HEIGHT + 2, "BEFORE: icons sit next to their labels, 14px rows, no slot", (255, 130, 130, 255))
   caption(pad + PANEL_WIDTH + pad, y + PANEL_HEIGHT + 2, "AFTER: vanilla slot 18x18, item centred in it", (130, 255, 130, 255))

   sheet = sheet.resize((sheet.width * 2, sheet.height * 2), Image.NEAREST)
   return sheet


def main():
   os.makedirs(OUT, exist_ok=True)
   assets = Assets(MC_JAR, JEI_JAR)
   data = load_loot()
   chances = load_chances()
   sheet = compose(assets, data, chances)
   path = os.path.join(OUT, "crate_ui_before_after.png")
   sheet.save(path)
   print("wrote", path, sheet.size)


if __name__ == "__main__":
   main()
