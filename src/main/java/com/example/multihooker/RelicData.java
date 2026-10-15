package com.example.multihooker;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public final class RelicData {
   private RelicData() {
   }

   public static int getLevel(Player player) {
      return Math.max(0, (Integer)player.getData(MultiHookerRegistry.SEA_MASTER_LEVEL));
   }

   public static double throwChance(int level) {
      double chance = (Double)Config.NET_BASE_CHANCE.get() + (Double)Config.NET_CHANCE_PER_LEVEL.get() * level;
      double cap = (Double)Config.NET_MAX_CHANCE.get();
      return Math.min(cap, Math.max(0.0, chance));
   }

   public static int rollsPerThrow(int level) {
      return Math.max(0, (Integer)Config.NET_BASE_ROLLS.get() + (Integer)Config.NET_ROLLS_PER_LEVEL.get() * level);
   }

   public static int costForNextLevel(Player player) {
      int level = getLevel(player);
      int levelCap = (Integer)Config.MAX_NET_LEVEL.get();
      if (levelCap >= 0 && level >= levelCap) {
         return 0;
      }

      // Once the throw chance has reached its cap the ability is fully mastered:
      // further levels would not increase the chance, so no more levelling.
      if (throwChance(level) >= (Double)Config.NET_MAX_CHANCE.get() - 1.0E-6) {
         return 0;
      }

      return (Integer)Config.NET_XP_PER_LEVEL.get() * (level + 1);
   }

   public static void handleLevelUpRequest(ServerPlayer player) {
      try {
         int level = getLevel(player);
         int cost = costForNextLevel(player);
         if (cost <= 0) {
            MultiHookerMod.LOGGER.info("[MultiHooker] {} hit the Net ability cap", player.getName());
            return;
         }

         if (player.experienceLevel < cost) {
            debug(player, "not enough XP levels (need " + cost + ")");
            return;
         }

         player.giveExperienceLevels(-cost);
         player.setData(MultiHookerRegistry.SEA_MASTER_LEVEL, level + 1);
         debug(player, "The Net level " + level + " -> " + (level + 1) + " (cost " + cost + " XP levels)");
      } catch (Throwable var3) {
         MultiHookerMod.LOGGER.error("MultiHooker: relic level-up failed", var3);
      }
   }

   private static void debug(ServerPlayer player, String message) {
      if ((Boolean)Config.DEBUG_LOG.get()) {
         MultiHookerMod.LOGGER.info("[MultiHooker] {}: {}", player.getName(), message);
      }
   }
}
