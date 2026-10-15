package com.example.multihooker.client;

import java.lang.reflect.Field;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.level.Level;

public final class HookStats {
   private static final double MAX_HOOK_DISTANCE_SQR = 1024.0;
   private static Field bitingAccessorField;

   private HookStats() {
   }

   public static int hooksInWater(Player player) {
      Level level = player.level();
      return level.getEntitiesOfClass(
            FishingHook.class,
            player.getBoundingBox().inflate(33.0),
            hook -> hook.isAlive() && hook.getOwner() == player && hook.distanceToSqr(player) <= 1024.0
         )
         .size();
   }

   public static int hooksBiting(Player player) {
      Level level = player.level();
      int biting = 0;

      for (FishingHook hook : level.getEntitiesOfClass(
         FishingHook.class, player.getBoundingBox().inflate(33.0), h -> h.isAlive() && h.getOwner() == player && h.distanceToSqr(player) <= 1024.0
      )) {
         if (isBiting(hook)) {
            biting++;
         }
      }

      return biting;
   }

   private static boolean isBiting(FishingHook hook) {
      try {
         if (bitingAccessorField == null) {
            bitingAccessorField = FishingHook.class.getDeclaredField("DATA_BITING");
            bitingAccessorField.setAccessible(true);
         }

         if (bitingAccessorField.get(null) instanceof EntityDataAccessor<?> realAccessor) {
            return Boolean.TRUE.equals(hook.getEntityData().get(realAccessor));
         }
      } catch (Throwable var4) {
         bitingAccessorField = null;
      }

      return false;
   }
}
