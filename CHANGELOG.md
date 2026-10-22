# MultiHooker 3.10.6

The treasure update. Crates become Troves, and every Trove now carries its own name carved into the wood, alongside a new batch of things to find inside.

## Added
- **Engraved Trove names.** Each of the twelve tiers has its name carved into the two long sides of the box. It reads while the Trove is in your hand, and it turns to face you when you place it down. The two short sides stay clean, so the lettering never wraps around a corner.
- **Potion of Health.** Drinking one permanently raises your maximum health by one heart, up to fifteen extra hearts (twenty-five total). The bonus lives in your save, so it survives dying, respawning and logging out.
- **Elixir of Fortune.** Luck III for three minutes.
- **Crate Potion.** Crate Luck for four minutes: +2.5 luck on every Trove you open while it lasts.
- **Crate Luck effect**, the buff behind the Crate Potion, with its own icon and a chat line when it takes hold.
- **Mystery Enchantment Books**, in five tiers: Common, Rare, Epic, Legendary and Mythical. Right-click one to turn it into real Enchanted Books. Higher tiers roll more enchantments, at higher levels, and are likelier to hit treasure enchantments such as Mending and Frost Walker.
- **Loot Journal support.** Drops that go straight into your inventory are handed to Loot Journal, so they get the same pickup toast as everything else you pick up.
- **Action bar messages** when you reel in a Trove, with a separate, louder line for the four Special Troves.
- **Every rate is a config option:** all twelve Trove catch chances, each drink's chance, each tier's Mystery Book chance, the Curio relic chance, hearts per bottle, the heart cap, the Crate Luck bonus and the bobber upgrade costs.

## Changed
- **Crates are now Troves** everywhere you see them: block names, item names, tooltips, the loot browser, JEI and the advancements. Only the display names changed - registry IDs and recipes are untouched, so existing worlds, stored Troves and existing loot keep working.
- **MultiHooker Treasures is no longer a separate add-on.** Its contents are part of the main mod, using the same IDs, so installing both will not duplicate anything and removing the old add-on loses nothing.
- **The Potion of Health replaces the Elixir of Vitality.** Same item, same ID, new name and a much clearer job: it now raises your maximum health for good instead of being a temporary buff.
- **Trove and potion names are coloured** to match their tier.

## Balance
- **Trove catch chances were trimmed by about a tenth** across all twelve tiers, because the Curio relic pool was doubled. The rarer Troves are still rarer than the common ones, in the same order as before.
- **Bobber upgrades cost 25 points per level**, with AquaKeys worth 10 points each, and the ladder still ends at twelve bobbers.

## Fixed
- **The lid of an empty Trove no longer mismatches its body.** The lid is drawn separately from the block, and the two were picking up opposite directional shading, so whichever way a Trove faced, one pair of faces came out darker than the other - most visibly on the lid's front and back rim. Both are now drawn with no directional shading, with the darkening of the sides baked into the textures instead, so all four faces agree and each rim face continues the body face directly below it.
- **The lid is hinged on the back rim** and swings up and back like a chest lid, instead of sweeping over the Trove as it opens.
- **Removed a dead luck entry** from all twelve Trove loot tables. That key does not exist in 1.21.1, so it did nothing.

## The 12 Troves
- **Normal (8):** Common, Uncommon, Rare, Epic, Legendary, Godly, Mythic, Secret.
- **Special (4):** Divine, Ultra Divine, Unspoken, GhostUnspoken.

# MultiHooker 3.10.5

Fishing, but with crates. This update finishes the crate ladder and reworks what you find inside.

## Added
- **Animated crate lids.** Crates open like a chest - the lid swings up while you look at the loot, and drops back down when you close it.
- **Mythic Crate**, a new tier between Godly and Secret.
- **Empty crates.** Opening a crate leaves a placeable empty shell behind. It is loot only - never in the creative tab.
- **Cross-mod loot.** Every crate also rolls one extra item from the common materials (gems, ingots, nuggets, dusts, raw materials, ores, storage blocks, cooked meat), so items from other installed mods show up automatically. Godly and above can also draw from the heavier set. If a mod's items don't appear, report it on my CurseForge page.
- **AquaCulture 2 support.** Neptune's Bounty now appears only in the Special crates (Divine, Ultra Divine, Unspoken and GhostUnspoken).
- **Crate Loot browser** (key **P** -> Crate Loot) now shows the bonus roll as its own section and scrolls with the mouse wheel.
- **JEI Crate Loot page.** Press **R** on any crate to see exactly what it gives - the cross-mod bonus roll included.
- **Crate advancements.** All 12 crates now have their own advancement in a new MultiHooker advancement tab.

## Balance
- **Drop rates retuned** to the real, much rarer values - the old test numbers made every crate far too easy.
- **More Bobbers is capped at 12 bobbers total** (11 upgrades); it no longer scales forever.
- **"Is that??? SPECIAL???" advancement** now unlocks for ALL FOUR Special crates (Divine + Ultra Divine + Unspoken + GhostUnspoken) instead of a single Divine crate.

## Buffs
- **AquaKey drop chance 7.2% -> 8%** per catch.
- **Axes break crates faster.**

## Nerfs
- **SeaMaster Relic catch chance 4% -> 3.5%** per catch (still fishing-only).
- **"The Net" is capped at 45%** throw chance (was up to 100%) and reads "Fully mastered" at the cap.
- **Bobber upgrades cost 30 points per level** (was 25).

## Removed
- **Legacy keys** (leftover Fisherman's Keys) are deleted on first login - not converted to points.
- **No instruction books.** Guidebooks and manuals (e.g. Adorable Hamster Pets' "Hamster Tips") are filtered out of the cross-mod roll, so a crate never gives one.

## Fixed
- **The SeaMaster relic is no longer stackable.**
- **Fishing lines** now anchor at your rod tip, with separate anchors for vanilla and AquaCulture rods (and compatibility with animation mods such as Punchy).
- **Fixed a startup crash** for players without AquaCulture installed.
- **The version** is now reported correctly in the mods list.

## The 12 Crates
- **Normal (8):** Common, Uncommon, Rare, Epic, Legendary, Godly, Mythic, Secret.
- **Special (4):** Divine, Ultra Divine, Unspoken, GhostUnspoken.
