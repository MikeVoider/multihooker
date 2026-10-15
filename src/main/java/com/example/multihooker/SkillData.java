package com.example.multihooker;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * The More Bobbers upgrade economy.
 *
 * Points are the currency. You earn points by fishing up AquaKeys (see
 * KeyLootHandler and Config.POINTS_PER_KEY); the AquaKey item itself is kept,
 * never spent on upgrades, so it stays free to use on crates.
 */
public final class SkillData {
   private SkillData() {
   }

   public static int getLevel(Player player) {
      return Math.max(0, (Integer)player.getData(MultiHookerRegistry.MORE_BOBBERS_LEVEL));
   }

   public static int bonusHooks(Player player) {
      int level = getLevel(player);
      int cap = (Integer)Config.MAX_SKILL_BONUS.get();
      if (cap >= 0 && level > cap) {
         level = cap;
      }

      return level;
   }

   public static int getPoints(Player player) {
      return Math.max(0, (Integer)player.getData(MultiHookerRegistry.BOBBER_POINTS));
   }

   public static void addPoints(Player player, int amount) {
      if (amount <= 0) {
         return;
      }

      player.setData(MultiHookerRegistry.BOBBER_POINTS, getPoints(player) + amount);
   }

   /**
    * One-time migration. The OLD More Bobbers currency was AquaKeys sitting in the
    * inventory; points replace them, and legacy keys are deliberately NOT converted
    * - they are simply deleted on the player's first login. Keys fished up after
    * that are never touched, because the marker makes this run once per player.
    *
    * @return the number of legacy keys deleted
    */
   public static int wipeLegacyKeys(Player player) {
      if ((Boolean)player.getData(MultiHookerRegistry.LEGACY_KEYS_WIPED)) {
         return 0;
      }

      player.setData(MultiHookerRegistry.LEGACY_KEYS_WIPED, true);
      Item key = (Item)MultiHookerRegistry.AQUAKEY.get();
      int removed = 0;

      for (ItemStack stack : player.getInventory().items) {
         if (!stack.isEmpty() && stack.is(key)) {
            removed += stack.getCount();
            stack.setCount(0);
         }
      }

      for (ItemStack stack : player.getInventory().offhand) {
         if (!stack.isEmpty() && stack.is(key)) {
            removed += stack.getCount();
            stack.setCount(0);
         }
      }

      return removed;
   }

   public static int costForNextLevel(Player player) {
      int level = getLevel(player);
      int cap = (Integer)Config.MAX_SKILL_BONUS.get();
      return cap >= 0 && level >= cap ? 0 : (Integer)Config.POINTS_PER_UPGRADE.get() * (level + 1);
   }

   public static void handleUpgradeRequest(ServerPlayer player) {
      try {
         int level = getLevel(player);
         int cost = costForNextLevel(player);
         if (cost <= 0) {
            MultiHookerMod.LOGGER.info("[MultiHooker] {} hit the skill cap", player.getName());
            return;
         }

         int points = getPoints(player);
         if (points < cost) {
            debug(player, "not enough points (need " + cost + ", have " + points + ")");
            return;
         }

         player.setData(MultiHookerRegistry.BOBBER_POINTS, points - cost);
         player.setData(MultiHookerRegistry.MORE_BOBBERS_LEVEL, level + 1);
         debug(player, "More Bobbers level " + level + " -> " + (level + 1) + " (cost " + cost + " points)");
      } catch (Throwable var3) {
         MultiHookerMod.LOGGER.error("MultiHooker: skill upgrade failed", var3);
      }
   }

   public static void handleResetRequest(ServerPlayer player) {
      try {
         int level = getLevel(player);
         if (level == 0) {
            return;
         }

         player.setData(MultiHookerRegistry.MORE_BOBBERS_LEVEL, 0);
         debug(player, "More Bobbers reset from level " + level + " to 0");
      } catch (Throwable var2) {
         MultiHookerMod.LOGGER.error("MultiHooker: skill reset failed", var2);
      }
   }

   private static void debug(ServerPlayer player, String message) {
      if ((Boolean)Config.DEBUG_LOG.get()) {
         MultiHookerMod.LOGGER.info("[MultiHooker] {}: {}", player.getName(), message);
      }
   }
}
