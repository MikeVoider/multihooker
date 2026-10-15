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
               "base x 3... so with 30: level 1 = 30 points, level 2 = 60, level 3 = 90.",
               "Raised from 25 alongside the 7.2% -> 8% AquaKey buff, so the extra keys are",
               "paid for instead of speeding the ladder up for free.",
               "AquaKeys themselves are NOT consumed by upgrades - you keep them for crates."
            }
         )
         .defineInRange("pointsPerUpgrade", 30, 1, 100000);
      POINTS_PER_KEY = builder.comment(
            new String[]{
               "Points granted each time you fish up an AquaKey.",
               "Deliberately NOT 1 point per key: the default is 1 key = 10 points."
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
      COMMON_CRATE_CHANCE = builder.comment(
            new String[]{
               "CRATES (12 tiers: 8 normal + 4 special). Chance per fishing CATCH that",
               "a crate is hauled up alongside the loot. These are the real, much",
               "rarer rates, so every tier gets harder to catch the higher you climb.",
               "Every crate",
               "is a placeable BLOCK: place it, then right-click it with an AquaKey to",
               "open it for its special loot. 0 disables a tier's drops."
            }
         )
         .defineInRange("commonCrateChance", 0.06, 0.0, 1.0);
      UNCOMMON_CRATE_CHANCE = builder.comment("Uncommon Crate catch chance (0.035 = 3.5%).")
         .defineInRange("uncommonCrateChance", 0.035, 0.0, 1.0);
      RARE_CRATE_CHANCE = builder.comment("Rare Crate catch chance (0.018 = 1.8%).")
         .defineInRange("rareCrateChance", 0.018, 0.0, 1.0);
      EPIC_CRATE_CHANCE = builder.comment("Epic Crate catch chance (0.009 = 0.9%).")
         .defineInRange("epicCrateChance", 0.009, 0.0, 1.0);
      LEGENDARY_CRATE_CHANCE = builder.comment("Legendary Crate catch chance (0.0045 = 0.45%).")
         .defineInRange("legendaryCrateChance", 0.0045, 0.0, 1.0);
      GODLY_CRATE_CHANCE = builder.comment("Godly Crate catch chance (0.0022 = 0.22%).")
         .defineInRange("godlyCrateChance", 0.0022, 0.0, 1.0);
      MYTHIC_CRATE_CHANCE = builder.comment(
            new String[]{
               "Mythic Crate catch chance (0.0014 = 0.14%). Mythic sits between Godly",
               "and Secret, so it is a little rarer than Godly and a little easier to",
               "catch than Secret. 0 disables Mythic Crate drops."
            }
         )
         .defineInRange("mythicCrateChance", 0.0014, 0.0, 1.0);
      SECRET_CRATE_CHANCE = builder.comment(
            new String[]{
               "Secret Crate catch chance (0.0009 = 0.09%). The strongest NORMAL crate:",
               "it sits ABOVE Mythic, so its loot is better and its catch rate is a",
               "little rarer. Normal group order: common, uncommon, rare, epic,",
               "legendary, godly, mythic, secret. The four special crates below sit",
               "above the whole normal group."
            }
         )
         .defineInRange("secretCrateChance", 0.0009, 0.0, 1.0);
      DIVINE_CRATE_CHANCE = builder.comment(
            new String[]{
               "DIVINE CRATE (Divine category, lower tier). 0.00045 = 0.045%.",
               "0 disables Divine Crate drops."
            }
         )
         .defineInRange("divineCrateChance", 0.00045, 0.0, 1.0);
      ULTRA_DIVINE_CRATE_CHANCE = builder.comment(
            new String[]{
               "ULTRA DIVINE CRATE (Divine category, upper tier). 0.00022 = 0.022%.",
               "A little rarer than Divine."
            }
         )
         .defineInRange("ultraDivineCrateChance", 0.00022, 0.0, 1.0);
      UNSPOKEN_CRATE_CHANCE = builder.comment(
            new String[]{
               "UNSPOKEN CRATE (Unspoken category, lower tier). 0.00009 = 0.009% -",
               "extremely rare, and always rarer than GhostUnspoken above it.",
               "0 disables Unspoken Crate drops."
            }
         )
         .defineInRange("unspokenCrateChance", 0.00009, 0.0, 1.0);
      GHOST_UNSPOKEN_CRATE_CHANCE = builder.comment(
            new String[]{
               "GHOSTUNSPOKEN CRATE (Unspoken category, upper tier). 0.00005 = 0.005%.",
               "A little rarer than Unspoken."
            }
         )
          .defineInRange("ghostUnspokenCrateChance", 0.00005, 0.0, 1.0);
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
               "-1 = no cap (infinite scaling). Example: 8 caps the skill at 9 total hooks."
            }
         )
         .defineInRange("maxSkillBonus", 11, -1, 1000);
      LUCK_BONUS_PER_EXTRA_HOOK = builder.comment(
            new String[]{
               "SCALING BONUS: every EXTRA hook beyond the first gets this much bonus",
               "luck added to its own loot rolls, on top of your rod enchantments AND",
               "any luck from equipped trinkets/relics (which already apply to every",
               "line). 1.0 = each extra hook fishes as if with +1 Luck of the Sea.",
               "0 disables the bonus."
            }
         )
         .defineInRange("luckBonusPerExtraHook", 1.0, 0.0, 10.0);
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
