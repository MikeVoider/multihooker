package com.example.multihooker;

import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec.BooleanValue;
import net.neoforged.neoforge.common.ModConfigSpec.Builder;
import net.neoforged.neoforge.common.ModConfigSpec.DoubleValue;
import net.neoforged.neoforge.common.ModConfigSpec.IntValue;

public final class Config {
   public static final ModConfigSpec SPEC;
   public static final IntValue EXTRA_HOOKS;
   public static final IntValue POINTS_PER_UPGRADE;
   public static final IntValue POINTS_PER_KEY;
   public static final DoubleValue LUCK_BONUS_PER_EXTRA_HOOK;
   public static final DoubleValue NET_BASE_CHANCE;
   public static final DoubleValue NET_CHANCE_PER_LEVEL;
   public static final DoubleValue NET_MAX_CHANCE;
   public static final IntValue NET_BASE_ROLLS;
   public static final IntValue NET_ROLLS_PER_LEVEL;
   public static final IntValue NET_XP_PER_LEVEL;
   public static final IntValue MAX_NET_LEVEL;
   public static final BooleanValue ONE_BITE_ALL_CATCH;
   public static final DoubleValue KEY_DROP_CHANCE;
   public static final DoubleValue SEA_MASTER_DROP_CHANCE;
   public static final DoubleValue COMMON_CRATE_CHANCE;
   public static final DoubleValue UNCOMMON_CRATE_CHANCE;
   public static final DoubleValue RARE_CRATE_CHANCE;
   public static final DoubleValue EPIC_CRATE_CHANCE;
   public static final DoubleValue LEGENDARY_CRATE_CHANCE;
   public static final DoubleValue GODLY_CRATE_CHANCE;
   public static final DoubleValue MYTHIC_CRATE_CHANCE;
   public static final DoubleValue SECRET_CRATE_CHANCE;
   public static final DoubleValue DIVINE_CRATE_CHANCE;
   public static final DoubleValue ULTRA_DIVINE_CRATE_CHANCE;
   public static final DoubleValue UNSPOKEN_CRATE_CHANCE;
   public static final DoubleValue GHOST_UNSPOKEN_CRATE_CHANCE;
   public static final DoubleValue ELIXIR_VITALITY_CHANCE;
   public static final DoubleValue HEARTS_POTION_PER_DRINK;
   public static final DoubleValue MAX_HEARTS_GAINED;
   public static final DoubleValue ELIXIR_FORTUNE_CHANCE;
   public static final DoubleValue CRATE_POTION_CHANCE;
   public static final DoubleValue CRATE_LUCK_BONUS;
   public static final DoubleValue RELIC_CHANCE;
   public static final DoubleValue BOOK_CHANCE_COMMON;
   public static final DoubleValue BOOK_CHANCE_UNCOMMON;
   public static final DoubleValue BOOK_CHANCE_RARE;
   public static final DoubleValue BOOK_CHANCE_EPIC;
   public static final DoubleValue BOOK_CHANCE_LEGENDARY;
   public static final DoubleValue BOOK_CHANCE_GODLY;
   public static final DoubleValue BOOK_CHANCE_MYTHIC;
   public static final DoubleValue BOOK_CHANCE_SECRET;
   public static final DoubleValue BOOK_CHANCE_DIVINE;
   public static final DoubleValue BOOK_CHANCE_ULTRA_DIVINE;
   public static final DoubleValue BOOK_CHANCE_UNSPOKEN;
   public static final DoubleValue BOOK_CHANCE_GHOST_UNSPOKEN;
   public static final BooleanValue INSTANT_CATCH_PICKUP;
   public static final IntValue MAX_SKILL_BONUS;
   public static final DoubleValue SPREAD_DEGREES;
   public static final BooleanValue DEBUG_LOG;
   public static final BooleanValue LINE_FROM_ROD_TIP;
   public static final DoubleValue LINE_ANCHOR_X;
   public static final DoubleValue LINE_ANCHOR_Y;
   public static final DoubleValue AQUA_LINE_ANCHOR_X;
   public static final DoubleValue AQUA_LINE_ANCHOR_Y;

   private Config() {
   }

   static {
      Builder builder = new Builder();
      builder.comment("MultiHooker").push("fishing");
      EXTRA_HOOKS = builder.comment(
            new String[]{
               "BASE extra fishing hooks thrown alongside the vanilla bobber, before",
               "any More Bobbers skill levels (the Fishing Panel adds those on top).",
               "0 = vanilla behaviour, 2 = 3 hooks total before skills."
            }
         )
         .defineInRange("extraHooks", 0, 0, 8);
      POINTS_PER_UPGRADE = builder.comment(
            new String[]{
               "BASE point cost of one More Bobbers upgrade in the Fishing Panel (key: P).",
               "You earn points by fishing up AquaKeys (see pointsPerKey). Costs SCALE with",
               "the level: the first upgrade costs this base, the next costs base x 2, then",
               "base x 3... so with 25: level 1 = 25 points, level 2 = 50, level 3 = 75.",
               "Paired with pointsPerKey 10 to land on exactly 2.5 keys for the first",
               "upgrade. The ladder itself is unchanged, it just ends sooner because the",
               "hook cap is lower - so this is priced to stay worth walking.",
               "AquaKeys themselves are NOT consumed by upgrades - you keep them for troves."
            }
         )
         .defineInRange("pointsPerUpgrade", 25, 1, 100000);
      POINTS_PER_KEY = builder.comment(
            new String[]{
               "Points granted each time you fish up an AquaKey.",
               "Deliberately NOT 1 point per key: the default is 1 key = 10 points.",
               "Together with pointsPerUpgrade 25 that puts the first upgrade at exactly",
               "2.5 keys. Keys are worth more than they were at 8, but the upgrade costs",
               "25 instead of 20, so 2.5 keys is still cheaper than the original 3."
            }
         )
         .defineInRange("pointsPerKey", 10, 1, 1000);
      KEY_DROP_CHANCE = builder.comment(
            new String[]{"Chance per fishing CATCH that an AquaKey drops alongside the loot.", "0.08 = 8%. 0 disables key drops."}
         )
         .defineInRange("keyDropChance", 0.08, 0.0, 1.0);
      SEA_MASTER_DROP_CHANCE = builder.comment(
            new String[]{
               "Chance per fishing CATCH that the SeaMaster relic itself is caught.", "0.035 = 3.5% - a rare, exciting catch. It is ONLY obtained by fishing."
            }
         )
         .defineInRange("seaMasterDropChance", 0.035, 0.0, 1.0);
      // All twelve trove CATCH chances below were trimmed by roughly a tenth
      // when the trove relic pool was doubled from 0.2% to 0.4%, so a fishing
      // catch did not quietly become worth more. The order (each trove rarer
      // than the last) is unchanged.
      COMMON_CRATE_CHANCE = builder.comment(
            new String[]{
               "TROVES (12 tiers: 8 normal + 4 special). Chance per fishing CATCH that",
               "a trove is hauled up alongside the loot. These are the real, much",
               "rarer rates, so every tier gets harder to catch the higher you climb.",
               "Every trove",
               "is a placeable BLOCK: place it, then right-click it with an AquaKey to",
               "open it for its special loot. 0 disables a tier's drops."
            }
         )
         .defineInRange("commonCrateChance", 0.054, 0.0, 1.0);
      UNCOMMON_CRATE_CHANCE = builder.comment("Uncommon Trove catch chance (0.035 = 3.5%).")
         .defineInRange("uncommonCrateChance", 0.0315, 0.0, 1.0);
      RARE_CRATE_CHANCE = builder.comment("Rare Trove catch chance (0.018 = 1.8%).")
         .defineInRange("rareCrateChance", 0.016, 0.0, 1.0);
      EPIC_CRATE_CHANCE = builder.comment("Epic Trove catch chance (0.009 = 0.9%).")
         .defineInRange("epicCrateChance", 0.008, 0.0, 1.0);
      LEGENDARY_CRATE_CHANCE = builder.comment("Legendary Trove catch chance (0.0045 = 0.45%).")
         .defineInRange("legendaryCrateChance", 0.004, 0.0, 1.0);
      GODLY_CRATE_CHANCE = builder.comment("Godly Trove catch chance (0.0022 = 0.22%).")
         .defineInRange("godlyCrateChance", 0.002, 0.0, 1.0);
      MYTHIC_CRATE_CHANCE = builder.comment(
            new String[]{
               "Mythic Trove catch chance (0.0014 = 0.14%). Mythic sits between Godly",
               "and Secret, so it is a little rarer than Godly and a little easier to",
               "catch than Secret. 0 disables Mythic Trove drops."
            }
         )
         .defineInRange("mythicCrateChance", 0.00125, 0.0, 1.0);
      SECRET_CRATE_CHANCE = builder.comment(
            new String[]{
               "Secret Trove catch chance (0.0009 = 0.09%). The strongest NORMAL trove:",
               "it sits ABOVE Mythic, so its loot is better and its catch rate is a",
               "little rarer. Normal group order: common, uncommon, rare, epic,",
               "legendary, godly, mythic, secret. The four special troves below sit",
               "above the whole normal group."
            }
         )
         .defineInRange("secretCrateChance", 0.0008, 0.0, 1.0);
      DIVINE_CRATE_CHANCE = builder.comment(
            new String[]{
               "DIVINE TROVE (Divine category, lower tier). 0.00045 = 0.045%.",
               "0 disables Divine Trove drops."
            }
         )
         .defineInRange("divineCrateChance", 0.0004, 0.0, 1.0);
      ULTRA_DIVINE_CRATE_CHANCE = builder.comment(
            new String[]{
               "ULTRA DIVINE TROVE (Divine category, upper tier). 0.00022 = 0.022%.",
               "A little rarer than Divine."
            }
         )
         .defineInRange("ultraDivineCrateChance", 0.0002, 0.0, 1.0);
      UNSPOKEN_CRATE_CHANCE = builder.comment(
            new String[]{
               "UNSPOKEN TROVE (Unspoken category, lower tier). 0.00009 = 0.009% -",
               "extremely rare, and always rarer than GhostUnspoken above it.",
               "0 disables Unspoken Trove drops."
            }
         )
         .defineInRange("unspokenCrateChance", 0.00008, 0.0, 1.0);
      GHOST_UNSPOKEN_CRATE_CHANCE = builder.comment(
            new String[]{
               "GHOSTUNSPOKEN TROVE (Unspoken category, upper tier). 0.00005 = 0.005%.",
               "A little rarer than Unspoken."
            }
         )
          .defineInRange("ghostUnspokenCrateChance", 0.000045, 0.0, 1.0);
      ELIXIR_VITALITY_CHANCE = builder.comment(
            new String[]{
               "POTION DROPS - off by default. The three drinks now come out of troves",
               "(the [potions] section below), not from fishing. Raise these above",
               "0 to also drop them on a catch."
            }
         )
         .defineInRange("elixirVitalityChance", 0.0107, 0.0, 1.0);
      HEARTS_POTION_PER_DRINK = builder.comment(
            new String[]{
               "POTION OF HEALTH - permanent max health. Each bottle adds this many",
               "hearts to the player's permanent total; the bonus is kept in the world",
               "save, so it survives dying and logging off.",
               "maxHeartsGained below caps the total."
            }
         )
         .defineInRange("heartsPotionPerDrink", 1.0, 0.0, 20.0);
      MAX_HEARTS_GAINED = builder.comment(
            new String[]{
               "Ceiling on permanently gained hearts, on top of the base 10.",
               "The default 15 means a maximum of 25 hearts (50 health).",
               "Set to 0 to make the elixir do nothing."
            }
         )
         .defineInRange("maxHeartsGained", 15.0, 0.0, 60.0);
      ELIXIR_FORTUNE_CHANCE = builder.comment(
            new String[]{
               "Elixir of Fortune (Luck III for 3:00). Only Legendary troves and",
               "above carry it."
            }
         )
         .defineInRange("elixirFortuneChance", 0.0041, 0.0, 1.0);
      CRATE_POTION_CHANCE = builder.comment(
            new String[]{
               "Chance per catch for the Crate Potion (Crate Luck for 5:00).",
               "Fishing is its only source - it has no recipe."
            }
         )
         .defineInRange("cratePotionChance", 0.0286, 0.0, 1.0);
      CRATE_LUCK_BONUS = builder.comment(
            new String[]{
               "CRATE LUCK - extra luck the Crate Potion grants when a trove is",
               "opened, added on top of your own luck. Loot tables read it as a luck",
               "level, so higher rolls favour the rarer entries. 2.5 = opens troves",
               "as if you had 2 Luck of the Sea. 0 disables the bonus."
            }
         )
         .defineInRange("crateLuckBonus", 2.5, 0.0, 20.0);
      builder.comment(
            "MYSTERY BOOK STACKS - chance per trove opened that the trove hands out a stack of",
            "3-7 Mystery Enchantment Books, all of one tier. Every trove rolls its OWN number so",
            "the odds identify the trove, and the chance FALLS as the trove gets better because the",
            "reward RISES with it: a Common trove is the easiest place to find books but can only",
            "give Common ones, while the four special troves are the hardest and the only route to",
            "Mythical. 0 disables that trove books."
         )
         .push("books");
      BOOK_CHANCE_COMMON = builder.comment(
            "Chance that opening a Common Trove yields a stack of 3-7 Mystery Books (0.154 = 15.4%)."
         )
         .defineInRange("bookChanceCommon", 0.154, 0.0, 1.0);
      BOOK_CHANCE_UNCOMMON = builder.comment(
            "Chance that opening a Uncommon Trove yields a stack of 3-7 Mystery Books (0.144 = 14.4%)."
         )
         .defineInRange("bookChanceUncommon", 0.144, 0.0, 1.0);
      BOOK_CHANCE_RARE = builder.comment(
            "Chance that opening a Rare Trove yields a stack of 3-7 Mystery Books (0.134 = 13.4%)."
         )
         .defineInRange("bookChanceRare", 0.134, 0.0, 1.0);
      BOOK_CHANCE_EPIC = builder.comment(
            "Chance that opening a Epic Trove yields a stack of 3-7 Mystery Books (0.124 = 12.4%)."
         )
         .defineInRange("bookChanceEpic", 0.124, 0.0, 1.0);
      BOOK_CHANCE_LEGENDARY = builder.comment(
            "Chance that opening a Legendary Trove yields a stack of 3-7 Mystery Books (0.114 = 11.4%)."
         )
         .defineInRange("bookChanceLegendary", 0.114, 0.0, 1.0);
      BOOK_CHANCE_GODLY = builder.comment(
            "Chance that opening a Godly Trove yields a stack of 3-7 Mystery Books (0.104 = 10.4%)."
         )
         .defineInRange("bookChanceGodly", 0.104, 0.0, 1.0);
      BOOK_CHANCE_MYTHIC = builder.comment(
            "Chance that opening a Mythic Trove yields a stack of 3-7 Mystery Books (0.094 = 9.4%)."
         )
         .defineInRange("bookChanceMythic", 0.094, 0.0, 1.0);
      BOOK_CHANCE_SECRET = builder.comment(
            "Chance that opening a Secret Trove yields a stack of 3-7 Mystery Books (0.089 = 8.9%)."
         )
         .defineInRange("bookChanceSecret", 0.089, 0.0, 1.0);
      BOOK_CHANCE_DIVINE = builder.comment(
            "Chance that opening a Divine Trove yields a stack of 3-7 Mystery Books (0.084 = 8.4%)."
         )
         .defineInRange("bookChanceDivine", 0.084, 0.0, 1.0);
      BOOK_CHANCE_ULTRA_DIVINE = builder.comment(
            "Chance that opening a Ultra Divine Trove yields a stack of 3-7 Mystery Books (0.079 = 7.9%)."
         )
         .defineInRange("bookChanceUltraDivine", 0.079, 0.0, 1.0);
      BOOK_CHANCE_UNSPOKEN = builder.comment(
            "Chance that opening a Unspoken Trove yields a stack of 3-7 Mystery Books (0.074 = 7.4%)."
         )
         .defineInRange("bookChanceUnspoken", 0.074, 0.0, 1.0);
      BOOK_CHANCE_GHOST_UNSPOKEN = builder.comment(
            "Chance that opening a GhostUnspoken Trove yields a stack of 3-7 Mystery Books (0.069 = 6.9%)."
         )
         .defineInRange("bookChanceGhostUnspoken", 0.069, 0.0, 1.0);
      builder.pop();
      RELIC_CHANCE = builder.comment(
            new String[]{
               "RELIC DROP - chance that opening ANY trove yields one Curios relic",
               "or trinket, drawn from the Curios slot tags (necklace, ring, charm,",
               "belt, head, back, body, hands, bracelet, feet, curio). Every relic mod",
               "registers its items there, so this needs no item ids and covers relic",
               "mods you add later as well. 0.004 = 0.4%, about one relic per 250",
               "troves opened. 0 disables relics entirely."
            }
         )
         .defineInRange("relicChance", 0.004, 0.0, 1.0);
      INSTANT_CATCH_PICKUP = builder.comment(
            new String[]{
               "true = an AquaKey or the SeaMaster relic caught while fishing goes",
               "STRAIGHT into your inventory (only the part that does not fit is dropped",
               "as a flying item). false = the old behaviour, where they are thrown to you",
               "and you have to pick them up."
            }
         )
         .define("instantCatchPickup", true);
      MAX_SKILL_BONUS = builder.comment(
            new String[]{
               "Safety cap for the More Bobbers skill: maximum extra hooks purchasable.",
               "11 by default, so the skill buys up to 11 extra hooks. The ceiling is 235,",
               "which keeps the skill finite without making 11 a wall - raise it if you",
               "want a longer ladder. -1 means no cap at all."
            }
         )
         .defineInRange("maxSkillBonus", 11, -1, 235);
      LUCK_BONUS_PER_EXTRA_HOOK = builder.comment(
            new String[]{
               "BONUS LUCK: every extra hook gets this much luck of its own, on top",
               "of your rod enchantments and any luck from equipped trinkets/relics.",
               "This is a FLAT bonus, not a ladder - every extra hook gets the same",
               "amount, so the hook count multiplies the benefit but does not raise it.",
               "The value is rounded to a whole number, so anything below 0.5 does",
               "nothing at all. The default 4.5 rounds up to +5 Luck of the Sea on",
               "every extra hook. 0 disables the bonus. Ceiling 14.1."
            }
         )
         .defineInRange("luckBonusPerExtraHook", 4.5, 0.0, 14.1);
      NET_BASE_CHANCE = builder.comment(
            new String[]{
               "SEAMASTER RELIC - The Net ability: base chance per catch that the relic",
               "throws its net and hauls in random extra fishing loot (0.10 = 10%).",
               "The relic must be carried or worn; it is NOT passively active anymore."
            }
         )
         .defineInRange("netBaseChance", 0.1, 0.0, 1.0);
      NET_CHANCE_PER_LEVEL = builder.comment("Extra throw chance per Net ability level (0.05 = +5% per level).")
         .defineInRange("netChancePerLevel", 0.05, 0.0, 1.0);
      NET_MAX_CHANCE = builder.comment(
            new String[]{
               "Hard CAP on The Net's throw chance. The chance stops growing once it",
               "reaches this value, and the ability can no longer be levelled up",
               "(the Relic menu shows 'Fully mastered'). 0.45 = 45%."
            }
         )
         .defineInRange("netMaxChance", 0.45, 0.0, 1.0);
      NET_BASE_ROLLS = builder.comment("Full fishing loot rolls the net adds when it throws (at level 0).").defineInRange("netBaseRolls", 2, 0, 20);
      NET_ROLLS_PER_LEVEL = builder.comment("Extra net rolls per Net ability level.").defineInRange("netRollsPerLevel", 1, 0, 10);
      NET_XP_PER_LEVEL = builder.comment(
            new String[]{
               "XP LEVELS needed to raise the Net ability: base x (level + 1), so with 2", "the first upgrade costs 2 XP levels, the next 4, then 6..."
            }
         )
         .defineInRange("netXpPerLevel", 2, 1, 30);
      MAX_NET_LEVEL = builder.comment("Cap for the Net ability level. -1 = infinite.").defineInRange("maxNetLevel", -1, -1, 100);
      ONE_BITE_ALL_CATCH = builder.comment(
            new String[]{
               "SYNCED BITES: when ONE of your hooks has a bite (bobber goes down) and you",
               "reel in, every other line out also yields a catch - so a single bite means",
               "a full haul on any rod, vanilla or modded. If NO hook is biting, reeling",
               "gives nothing, exactly like vanilla. Set false for fully independent hooks."
            }
         )
         .define("oneBiteAllCatch", true);
      SPREAD_DEGREES = builder.comment(
            new String[]{
               "Maximum yaw spread (in degrees) given to each extra hook so the lines",
               "fan out instead of landing exactly on top of each other. 0 disables the spread."
            }
         )
         .defineInRange("spreadDegrees", 12.0, 0.0, 45.0);
      builder.pop();
      builder.comment("Diagnostics").push("diagnostics");
      DEBUG_LOG = builder.comment(
            new String[]{
               "Log MultiHooker's cast/reel-in detections to the game log (INFO level).",
               "Useful when reporting issues with specific modded rods. Small log noise."
            }
         )
         .define("debugLog", false);
      builder.pop();
      builder.comment("Rendering").push("rendering");
      LINE_FROM_ROD_TIP = builder.comment(
            new String[]{
               "First person: anchor every fishing line at the rod TIP instead of the",
               "vanilla hand point in the middle of the rod. With several lines out at",
               "once this is the difference between 'all lines from the rod tip' and",
               "'all lines from mid-rod'. Set false for vanilla line behaviour."
            }
         )
         .define("lineFromRodTip", true);
      LINE_ANCHOR_X = builder.comment(
            new String[]{
               "Horizontal position of the first-person line anchor, as a fraction of",
               "half the screen from the centre: 0.0 = centre, positive = right.",
               "Tweak in small steps (0.02) until the lines start exactly at your rod tip."
            }
         )
         .defineInRange("lineAnchorX", 0.145, -1.0, 1.0);
      LINE_ANCHOR_Y = builder.comment(
            new String[]{
               "Vertical position of the first-person line anchor, as a fraction of",
               "half the screen from the centre: 0.0 = centre, positive = up.",
               "Tweak in small steps (0.02) until the lines start exactly at your rod tip."
            }
         )
         .defineInRange("lineAnchorY", 0.53, -1.0, 1.0);
      AQUA_LINE_ANCHOR_X = builder.comment(
            new String[]{
               "Horizontal position of the AQUACULTURE rod line anchor (same units as",
               "lineAnchorX: fraction of half the screen from the centre, positive = right).",
               "Aquaculture rods are a different model held in a different spot than the",
               "vanilla rod, so they need their own anchor. Default is calibrated to the",
               "Aquaculture rod tip; tweak in small steps (0.02) if your setup differs."
            }
         )
         .defineInRange("aquaLineAnchorX", 0.335, -1.0, 1.0);
      AQUA_LINE_ANCHOR_Y = builder.comment(
            new String[]{
               "Vertical position of the AQUACULTURE rod line anchor (same units as",
               "lineAnchorY: fraction of half the screen from the centre, positive = up).",
               "Default is calibrated to the Aquaculture rod tip."
            }
         )
         .defineInRange("aquaLineAnchorY", 0.16, -1.0, 1.0);
      builder.pop();
      SPEC = builder.build();
   }
}
