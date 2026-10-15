# MultiHooker

**Fishing, but with crates.**

[![Download on CurseForge](https://img.shields.io/badge/CurseForge-Download-F16436?logo=curseforge&logoColor=white)](https://www.curseforge.com/minecraft/mc-mods/multihooker)
[![YouTube](https://img.shields.io/badge/YouTube-@MikeVoider-FF0000?logo=youtube&logoColor=white)](https://www.youtube.com/@MikeVoider)

MultiHooker casts several fishing hooks with a single cast. The vanilla bobber goes
out as always, and one extra hook per configured extra goes with it, so you can pull
in multiple fish from one cast. Reeling in brings every hook back at once.

On top of that it adds a full crate system: 12 crates to fish up, unlock with an
AquaKey and open for loot.

| | |
|---|---|
| **Download** | [CurseForge](https://www.curseforge.com/minecraft/mc-mods/multihooker) |
| **Minecraft** | 1.21.1 |
| **Loader** | NeoForge 21.1.x |
| **Latest version** | 3.10.5 |
| **License** | MIT |

---

## Features

### Multi-hook fishing
- Cast multiple hooks with one cast (configurable, capped at 12 bobbers total).
- The vanilla bobber is thrown as always and brings in every hook with it on reel-in.

### The crate system (12 crates)
- **8 normal crates:** Common, Uncommon, Rare, Epic, Legendary, Godly, **Mythic** (new) and Secret.
- **4 special crates:** Divine, Ultra Divine, Unspoken and GhostUnspoken.
- Every crate is a **placeable block** with an **animated lid**; right-click it while
  holding an **AquaKey** to open it. An AquaKey in the off-hand works too.
- Opening a crate leaves a placeable **empty crate** behind.
- Loot is **data-driven** (`data/multihooker/loot_table/crates/<tier>.json`) and
  escalates with the tier.
- **Cross-mod loot:** every crate also rolls one extra item from the standard material
  groups (gems, ingots, nuggets, dusts, raw materials, ores, storage blocks, cooked
  meat), so items from other installed mods show up automatically. Godly and above also
  draw from the heavier set.
- **AquaCulture 2:** Neptune's Bounty appears in the Special crates.

### Looking up what a crate gives
- **In-game Crate Loot browser** (key **P** &rarr; Crate Loot): every crate, its drops,
  its odds and its catch chance, with the cross-mod bonus roll as its own section.
- **JEI:** press **R** on any crate for its Crate Loot page, bonus roll included.

### Progression
- **AquaKey** drops while fishing and is spent on upgrades in the Fishing Panel.
- **SeaMaster Relic** (fishing-only) and its **"The Net"** ability.
- **In-game config screen** (Options &rarr; Mods &rarr; MultiHooker &rarr; Config).
- **12 crate advancements** in a dedicated MultiHooker advancement tab, plus the
  *"Is that??? SPECIAL???"* challenge for all four Special crates.

---

## Requirements

- **Minecraft 1.21.1** with **NeoForge 21.1.x**.

Optional integrations:

- **[Curios](https://www.curseforge.com/minecraft/mc-mods/curios):** wear the SeaMaster
  Relic in the necklace slot. Without it the relic works from your inventory.
- **[JEI](https://www.curseforge.com/minecraft/mc-mods/jei):** the Crate Loot pages.
- **[AquaCulture 2](https://www.curseforge.com/minecraft/mc-mods/aquaculture):** extra
  hooks also cast on Aquaculture rods, and Neptune's Bounty in the Special crates.

---

## Installation

Download the latest jar from **[CurseForge](https://www.curseforge.com/minecraft/mc-mods/multihooker)**.

1. Install [NeoForge](https://neoforged.net/) for Minecraft 1.21.1.
2. Drop `multihooker-<version>.jar` into your `mods` folder.
2. (Optional) Drop any of the optional mods above into the same folder.

---

## Reporting a bug

Please [open an issue](https://github.com/MikeVoider/multihooker/issues/new/choose)
and include:

- the **MultiHooker version** (shown in the mod list) and your **NeoForge** version,
- your **full log or crash report** (`logs/latest.log` or `crash-reports/`),
- your **mod list** (or a link to the modpack),
- **steps to reproduce**, what you expected and what actually happened.

If it is about another mod's items not showing up in crates, mention that mod's name -
that is almost always a missing item tag and is easy to fix.

---

## Building from source

```sh
./gradlew build
```

The jar is written to `build/libs/`.

To run a development client:

```
./gradlew runClient
```

---

## Credits

- **Author:** mikeVoider
- **YouTube:** [@MikeVoider](https://www.youtube.com/@MikeVoider)
- Built on the [NeoForged MDK](https://github.com/NeoForged/MDK).

## License

This project is licensed under the MIT License — see [LICENSE](LICENSE).
