package com.example.multihooker;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

public final class MultiHookerManager {
   private static final Map<UUID, MultiHookerManager.Entry> ENTRIES = new HashMap<>();
   private static final Set<FishingHook> PENDING_CASTS = new HashSet<>();
   private static final Set<UUID> REEL_PENDING = new HashSet<>();
   private static boolean SPAWNING = false;

   private MultiHookerManager() {
   }

   public static void onHookJoined(EntityJoinLevelEvent event) {
      if (!event.getLevel().isClientSide()) {
         try {
            if (!(!SPAWNING && event.getEntity() instanceof FishingHook hook)) {
               return;
            }

            if (!(hook.getOwner() instanceof Player player)) {
               return;
            }

            if (player.fishing != hook) {
               return;
            }

            if (ENTRIES.containsKey(player.getUUID()) || PENDING_CASTS.contains(hook)) {
               return;
            }

            PENDING_CASTS.add(hook);
            debug("Cast queued: primary={}, level={}", hook.getClass().getSimpleName(), event.getLevel().dimension().location());
         } catch (Throwable var4) {
            MultiHookerMod.LOGGER.error("MultiHooker: join handler failed", var4);
         }
      }
   }

   public static int extraHooksFor(Player player) {
      return (Integer)Config.EXTRA_HOOKS.get() + SkillData.bonusHooks(player);
   }

   private static void spawnExtrasFor(FishingHook hook, MinecraftServer server) {
      Player player = hook.getPlayerOwner();
      if (player != null) {
         if (hook.level() instanceof ServerLevel serverLevel) {
            if (hook.isAlive() && player.fishing == hook) {
               if (!ENTRIES.containsKey(player.getUUID())) {
                  int extraCount = extraHooksFor(player);
                  if (extraCount <= 0) {
                     debug("No extra hooks configured (extraHooks=0)");
                  } else {
                     boolean sameClass = hook.getClass() != FishingHook.class;
                     int luckBonus = (int)Math.round(Math.max(0.0, (Double)Config.LUCK_BONUS_PER_EXTRA_HOOK.get()));
                     debug(
                        "Spawning {} extra hook(s), sameClass={}, luckBonusPerHook={} (primary {})",
                        extraCount,
                        sameClass,
                        luckBonus,
                        hook.getClass().getSimpleName()
                     );
                     SPAWNING = true;

                     try {
                        List<FishingHook> extras = new ArrayList<>(extraCount);

                        for (int i = 0; i < extraCount; i++) {
                           try {
                              FishingHook extra;
                              if (sameClass) {
                                 extra = createSameClassExtra(hook, player, serverLevel, luckBonus);
                                 if (extra != null) {
                                    extra.moveTo(hook.getX(), hook.getY(), hook.getZ(), hook.getYRot(), hook.getXRot());
                                    extra.setDeltaMovement(hook.getDeltaMovement());
                                 }
                              } else {
                                 extra = new FishingHook(player, serverLevel, luckOf(hook) + luckBonus, lureOf(hook));
                              }

                              if (extra == null) {
                                 extra = new FishingHook(player, serverLevel, luckOf(hook) + luckBonus, lureOf(hook));
                              }

                              spread(extra, i, extraCount, (Double)Config.SPREAD_DEGREES.get());
                              serverLevel.addFreshEntity(extra);
                              extras.add(extra);
                           } catch (Throwable var13) {
                              MultiHookerMod.LOGGER.error("MultiHooker: failed to spawn extra hook", var13);
                           }
                        }

                        if (!extras.isEmpty()) {
                           ENTRIES.put(player.getUUID(), new MultiHookerManager.Entry(player.getUUID(), hook, extras));
                           debug("Spawned {} extra hook(s)", extras.size());
                        }
                     } finally {
                        SPAWNING = false;
                        player.fishing = hook;
                     }
                  }
               }
            }
         }
      }
   }

   private static int luckOf(FishingHook hook) {
      return finalIntOr(hook, "luck", 0);
   }

   private static int lureOf(FishingHook hook) {
      return finalIntOr(hook, "lureSpeed", 0);
   }

   private static int finalIntOr(FishingHook hook, String field, int fallback) {
      try {
         Field f = FishingHook.class.getDeclaredField(field);
         f.setAccessible(true);
         return f.getInt(hook);
      } catch (Throwable var4) {
         return fallback;
      }
   }

   private static FishingHook createSameClassExtra(FishingHook primary, Player player, ServerLevel level, int luckBonus) {
      Class<?> cls = primary.getClass();

      for (Constructor<?> ctor : cls.getDeclaredConstructors()) {
         try {
            Class<?>[] params = ctor.getParameterTypes();
            if (params.length >= 4
               && params[0].isAssignableFrom(Player.class)
               && params[1].isAssignableFrom(Level.class)
               && isIntParam(params[2])
               && isIntParam(params[3])) {
               List<Object> pool = subclassFieldValues(primary);
               Object[] args = new Object[params.length];
               args[0] = player;
               args[1] = level;
               args[2] = luckOf(primary) + luckBonus;
               args[3] = lureOf(primary);
               boolean matched = true;

               for (int i = 4; i < params.length; i++) {
                  Object match = takeMatching(pool, params[i]);
                  if (match == null) {
                     matched = false;
                     break;
                  }

                  args[i] = match;
               }

               if (matched) {
                  ctor.setAccessible(true);
                  FishingHook extra = (FishingHook)ctor.newInstance(args);
                  debug("Cloned extra hook via constructor: {}", cls.getSimpleName());
                  return extra;
               }
            }
         } catch (Throwable var16) {
         }
      }

      try {
         FishingHook extra = (FishingHook)primary.getType().create(level);
         if (extra != null && extra.getClass() == cls) {
            copySubclassFields(primary, extra);
            setFinalInt(FishingHook.class, "luck", extra, luckOf(primary) + luckBonus);
            setFinalInt(FishingHook.class, "lureSpeed", extra, lureOf(primary));
            extra.setOwner(player);
            debug("Cloned extra hook via EntityType.create: {}", cls.getSimpleName());
            return extra;
         }
      } catch (Throwable var15) {
         debug("Same-class clone failed for {}: {}", cls.getSimpleName(), var15.toString());
      }

      return null;
   }

   private static boolean isIntParam(Class<?> type) {
      return type == int.class || type == Integer.class;
   }

   private static List<Object> subclassFieldValues(FishingHook hook) {
      List<Object> values = new ArrayList<>();

      for (Class<?> cls = hook.getClass(); cls != null && cls != FishingHook.class && cls != Object.class; cls = cls.getSuperclass()) {
         for (Field field : cls.getDeclaredFields()) {
            if (!Modifier.isStatic(field.getModifiers()) && !field.isSynthetic()) {
               try {
                  field.setAccessible(true);
                  values.add(field.get(hook));
               } catch (Throwable var8) {
               }
            }
         }
      }

      return values;
   }

   private static Object takeMatching(List<Object> pool, Class<?> targetType) {
      if (targetType.isPrimitive()) {
         return null;
      } else {
         Iterator<Object> it = pool.iterator();

         while (it.hasNext()) {
            Object value = it.next();
            if (value != null && targetType.isAssignableFrom(value.getClass())) {
               it.remove();
               return value;
            }
         }

         return null;
      }
   }

   private static void copySubclassFields(FishingHook from, FishingHook to) {
      for (Class<?> cls = from.getClass(); cls != null && cls != FishingHook.class && cls != Object.class; cls = cls.getSuperclass()) {
         for (Field field : cls.getDeclaredFields()) {
            if (!Modifier.isStatic(field.getModifiers()) && !field.isSynthetic()) {
               try {
                  field.setAccessible(true);
                  field.set(to, field.get(from));
               } catch (Throwable var8) {
               }
            }
         }
      }
   }

   private static void setFinalInt(Class<?> owner, String name, Object target, int value) {
      try {
         Field field = owner.getDeclaredField(name);
         field.setAccessible(true);
         field.setInt(target, value);
      } catch (Throwable var5) {
         debug("Could not set {} on cloned hook: {}", name, var5.toString());
      }
   }

   private static boolean isCatching(FishingHook hook) {
      if (hook.getHookedIn() != null) {
         return true;
      } else {
         try {
            Field nibbleField = FishingHook.class.getDeclaredField("nibble");
            nibbleField.setAccessible(true);
            return nibbleField.getInt(hook) > 0;
         } catch (Throwable var2) {
            return false;
         }
      }
   }

   private static void forceCatch(FishingHook hook) {
      if (hook.getHookedIn() == null) {
         try {
            Field nibbleField = FishingHook.class.getDeclaredField("nibble");
            nibbleField.setAccessible(true);
            if (nibbleField.getInt(hook) <= 0) {
               nibbleField.setInt(hook, 1);
            }
         } catch (Throwable var2) {
            debug("forceCatch failed: {}", var2.toString());
         }
      }
   }

   private static boolean anyLineCatching(MultiHookerManager.Entry entry) {
      if (entry.primary().isAlive() && isCatching(entry.primary())) {
         return true;
      } else {
         for (FishingHook extra : entry.extras()) {
            if (extra.isAlive() && isCatching(extra)) {
               return true;
            }
         }

         return false;
      }
   }

   public static void onRetrieveStart(FishingHook hook) {
      try {
         if (hook.level().isClientSide() || !(Boolean)Config.ONE_BITE_ALL_CATCH.get()) {
            return;
         }

         Player player = hook.getPlayerOwner();
         if (player == null || player.fishing != hook) {
            return;
         }

         if (!REEL_PENDING.contains(player.getUUID())) {
            return;
         }

         MultiHookerManager.Entry entry = ENTRIES.get(player.getUUID());
         if (entry == null) {
            return;
         }

         if (anyLineCatching(entry)) {
            forceCatch(hook);
            debug("Synced bites: primary joins the haul");
         }
      } catch (Throwable var3) {
         MultiHookerMod.LOGGER.error("MultiHooker: retrieve-start handler failed", var3);
      }
   }

   public static void onRodRightClick(Player player, ItemStack held) {
      try {
         if (player.level().isClientSide()) {
            return;
         }

         if (player.fishing == null) {
            return;
         }

         if (!held.canPerformAction(ItemAbilities.FISHING_ROD_CAST)) {
            return;
         }

         REEL_PENDING.add(player.getUUID());
      } catch (Throwable var3) {
         MultiHookerMod.LOGGER.error("MultiHooker: right-click handler failed", var3);
      }
   }

   public static void onHookRemoved(FishingHook hook, RemovalReason reason) {
      try {
         if (hook.level().isClientSide()) {
            return;
         }

         MultiHookerManager.Entry entry = findEntryByPrimary(hook);
         if (entry == null) {
            return;
         }

         ENTRIES.remove(entry.playerId());
         Player player = hook.getPlayerOwner();
         boolean reelIn = reason == RemovalReason.DISCARDED
            && player != null
            && player.isAlive()
            && REEL_PENDING.remove(entry.playerId())
            && isHoldingRod(player);
         if (reelIn) {
            ItemStack rod = rodInHand(player);
            debug("Reel-in detected (primary={}): retrieving {} extra hook(s)", hook.getClass().getSimpleName(), entry.extras().size());
            boolean synced = (Boolean)Config.ONE_BITE_ALL_CATCH.get() && anyLineCatching(entry);

            for (FishingHook extra : entry.extras()) {
               if (extra.isAlive() && extra != hook) {
                  try {
                     if (synced) {
                        forceCatch(extra);
                     }

                     extra.retrieve(rod);
                  } catch (Throwable var10) {
                     MultiHookerMod.LOGGER.error("MultiHooker: failed to reel in an extra hook", var10);
                  }
               }
            }
         } else {
            debug("Primary removed without a reel-in ({}): discarding {} extra hook(s)", reason, entry.extras().size());

            for (FishingHook extrax : entry.extras()) {
               if (extrax.isAlive() && extrax != hook) {
                  extrax.discard();
               }
            }
         }
      } catch (Throwable var11) {
         MultiHookerMod.LOGGER.error("MultiHooker: removal handler failed", var11);
      }
   }

   public static void afterHookRemoved(FishingHook hook) {
      try {
         if (hook.level().isClientSide()) {
            return;
         }

         MultiHookerManager.Entry entry = findEntryByExtra(hook);
         if (entry == null) {
            return;
         }

         Player player = entry.primary().getPlayerOwner();
         if (player != null && entry.primary().isAlive()) {
            player.fishing = entry.primary();
         }
      } catch (Throwable var3) {
         MultiHookerMod.LOGGER.error("MultiHooker: post-removal handler failed", var3);
      }
   }

   private static MultiHookerManager.Entry findEntryByPrimary(FishingHook hook) {
      for (MultiHookerManager.Entry entry : ENTRIES.values()) {
         if (entry.primary() == hook) {
            return entry;
         }
      }

      return null;
   }

   private static MultiHookerManager.Entry findEntryByExtra(FishingHook hook) {
      for (MultiHookerManager.Entry entry : ENTRIES.values()) {
         if (entry.extras().contains(hook)) {
            return entry;
         }
      }

      return null;
   }

   private static boolean isHoldingRod(Player player) {
      return player.getMainHandItem().canPerformAction(ItemAbilities.FISHING_ROD_CAST)
         || player.getOffhandItem().canPerformAction(ItemAbilities.FISHING_ROD_CAST);
   }

   private static ItemStack rodInHand(Player player) {
      if (player.getMainHandItem().canPerformAction(ItemAbilities.FISHING_ROD_CAST)) {
         return player.getMainHandItem();
      } else {
         return player.getOffhandItem().canPerformAction(ItemAbilities.FISHING_ROD_CAST) ? player.getOffhandItem() : ItemStack.EMPTY;
      }
   }

   private static void spread(FishingHook hook, int index, int total, double maxSpread) {
      if (!(maxSpread <= 0.0) && total > 0) {
         double fraction = 2.0 * (index + 1.0) / (total + 1.0) - 1.0;
         double yaw = Math.toRadians(maxSpread * fraction);
         double cos = Math.cos(yaw);
         double sin = Math.sin(yaw);
         Vec3 motion = hook.getDeltaMovement();
         hook.setDeltaMovement(motion.x * cos - motion.z * sin, motion.y, motion.x * sin + motion.z * cos);
      }
   }

   public static void tick(MinecraftServer server) {
      REEL_PENDING.clear();
      if (!PENDING_CASTS.isEmpty()) {
         for (FishingHook hook : new ArrayList<>(PENDING_CASTS)) {
            PENDING_CASTS.remove(hook);

            try {
               spawnExtrasFor(hook, server);
            } catch (Throwable var6) {
               MultiHookerMod.LOGGER.error("MultiHooker: failed to process queued cast", var6);
            }
         }
      }

      if (!ENTRIES.isEmpty()) {
         Iterator<Map.Entry<UUID, MultiHookerManager.Entry>> it = ENTRIES.entrySet().iterator();

         while (it.hasNext()) {
            Map.Entry<UUID, MultiHookerManager.Entry> mapEntry = it.next();

            try {
               MultiHookerManager.Entry entry = mapEntry.getValue();
               if (entry.primary().isAlive()) {
                  entry.extras().removeIf(hook -> !hook.isAlive());
               } else {
                  for (FishingHook extra : entry.extras()) {
                     if (extra.isAlive()) {
                        extra.discard();
                     }
                  }

                  it.remove();
               }
            } catch (Throwable var7) {
               MultiHookerMod.LOGGER.error("MultiHooker: cleanup failed for tracked player", var7);
               it.remove();
            }
         }
      }
   }

   private static void debug(String message, Object... args) {
      if ((Boolean)Config.DEBUG_LOG.get()) {
         MultiHookerMod.LOGGER.info("[MultiHooker] " + message, args);
      }
   }

   private record Entry(UUID playerId, FishingHook primary, List<FishingHook> extras) {
   }
}
