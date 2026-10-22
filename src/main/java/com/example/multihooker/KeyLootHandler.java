package com.example.multihooker;

import com.example.multihooker.compat.CuriosBridge;
import java.util.StringJoiner;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.LootParams.Builder;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.event.entity.player.ItemFishedEvent;

public final class KeyLootHandler {
   private KeyLootHandler() {
   }

   public static void onItemFished(ItemFishedEvent event) {
      try {
         Player player = event.getEntity();
         if (player.level().isClientSide()) {
            return;
         }

         FishingHook hook = event.getHookEntity();
         if ((Boolean)Config.DEBUG_LOG.get()) {
            StringJoiner drops = new StringJoiner(", ");

            for (ItemStack stack : event.getDrops()) {
               drops.add(stack.getCount() + "x " + stack.getItem());
            }

            MultiHookerMod.LOGGER
               .info(
                  "[MultiHooker] Catch event: hook={}, player={}, seaMaster={}, drops=[{}]",
                  new Object[]{hook.getClass().getSimpleName(), player.getName(), hasSeaMaster(player), drops}
               );
         }

         // Every AquaKey fished up is also worth points for the More Bobbers
         // upgrade (Config.pointsPerKey). The key itself is kept - upgrades spend
         // points, never keys - so the same key is still free to use on a crate.
         if (dropCustomItem(
            player, hook, new ItemStack((ItemLike)MultiHookerRegistry.AQUAKEY.get()), (Double)Config.KEY_DROP_CHANCE.get(), "AquaKey"
         )) {
            int points = (Integer)Config.POINTS_PER_KEY.get();
            SkillData.addPoints(player, points);
            if ((Boolean)Config.DEBUG_LOG.get()) {
               MultiHookerMod.LOGGER.info("[MultiHooker] {} fished up an AquaKey: +{} points", player.getName(), points);
            }
         }
         dropCustomItem(player, hook, new ItemStack((ItemLike)MultiHookerRegistry.SEA_MASTER.get()), (Double)Config.SEA_MASTER_DROP_CHANCE.get(), "SeaMaster");

      // The three drinks are deliberately NOT fished up any more. They used to be, and the
      // Crate Potion in particular was awkward here: its Crate Luck rides on the Luck attribute,
      // which fishing also reads, so a potion that makes fishing better really wants to come from
      // a crate rather than off the rod. All three are trove loot now - see CrateBonusLoot.

         // Every crate that really drops also unlocks its own advancement. The tier
         // keys line up with data/multihooker/advancement/crates/<tier>.json.
         dropCrate(player, hook, MultiHookerRegistry.COMMON_CRATE_ITEM.get(), (Double)Config.COMMON_CRATE_CHANCE.get(), "Common Trove", "common");
         dropCrate(player, hook, MultiHookerRegistry.UNCOMMON_CRATE_ITEM.get(), (Double)Config.UNCOMMON_CRATE_CHANCE.get(), "Uncommon Trove", "uncommon");
         dropCrate(player, hook, MultiHookerRegistry.RARE_CRATE_ITEM.get(), (Double)Config.RARE_CRATE_CHANCE.get(), "Rare Trove", "rare");
         dropCrate(player, hook, MultiHookerRegistry.EPIC_CRATE_ITEM.get(), (Double)Config.EPIC_CRATE_CHANCE.get(), "Epic Trove", "epic");
         dropCrate(player, hook, MultiHookerRegistry.LEGENDARY_CRATE_ITEM.get(), (Double)Config.LEGENDARY_CRATE_CHANCE.get(), "Legendary Trove", "legendary");
         dropCrate(player, hook, MultiHookerRegistry.GODLY_CRATE_ITEM.get(), (Double)Config.GODLY_CRATE_CHANCE.get(), "Godly Trove", "godly");
         dropCrate(player, hook, MultiHookerRegistry.MYTHIC_CRATE_ITEM.get(), (Double)Config.MYTHIC_CRATE_CHANCE.get(), "Mythic Trove", "mythic");
         dropCrate(player, hook, MultiHookerRegistry.SECRET_CRATE_ITEM.get(), (Double)Config.SECRET_CRATE_CHANCE.get(), "Secret Trove", "secret");
         dropCrate(player, hook, MultiHookerRegistry.DIVINE_CRATE_ITEM.get(), (Double)Config.DIVINE_CRATE_CHANCE.get(), "Divine Trove", "divine");
         dropCrate(player, hook, MultiHookerRegistry.ULTRA_DIVINE_CRATE_ITEM.get(), (Double)Config.ULTRA_DIVINE_CRATE_CHANCE.get(), "Ultra Divine Trove", "ultra_divine");
         dropCrate(player, hook, MultiHookerRegistry.UNSPOKEN_CRATE_ITEM.get(), (Double)Config.UNSPOKEN_CRATE_CHANCE.get(), "Unspoken Trove", "unspoken");
         dropCrate(player, hook, MultiHookerRegistry.GHOST_UNSPOKEN_CRATE_ITEM.get(), (Double)Config.GHOST_UNSPOKEN_CRATE_CHANCE.get(), "GhostUnspoken Trove", "ghost_unspoken");
         tryNetThrow(player, hook);
      } catch (Throwable var6) {
         MultiHookerMod.LOGGER.error("MultiHooker: catch handler failed", var6);
      }
   }

   public static boolean hasSeaMaster(Player player) {
      // The Net ability only works when the SeaMaster Relic is actually worn
      // in the Curios necklace slot. Without Curios there is no necklace slot,
      // so the ability never fires from the inventory.
      if (!ModList.get().isLoaded("curios")) {
         return false;
      }
      return CuriosBridge.isEquipped(player);
   }

   static boolean dropCustomItem(Player player, FishingHook hook, ItemStack stack, double chance, String debugName) {
      if (chance <= 0.0 || player.getRandom().nextDouble() >= chance) {
         return false;
      }

      Level level = player.level();
      if (level.isClientSide()) {
         return false;
      }

      // Straight into the inventory when enabled; Inventory.add() shrinks the stack to
      // whatever did not fit, so anything left over is still tossed to the player.
      if ((Boolean)Config.INSTANT_CATCH_PICKUP.get()) {
         // Inventory.add() shrinks the stack down to whatever did NOT fit, so the count is kept
         // here first - after the call, `stack` holds only the leftover.
         ItemStack offered = stack.copy();
         player.getInventory().add(stack);
         int delivered = offered.getCount() - stack.getCount();
         if (delivered > 0) {
            offered.setCount(delivered);
            announceDirectPickup(player, offered);
         }
         if (stack.isEmpty()) {
            if ((Boolean)Config.DEBUG_LOG.get()) {
               MultiHookerMod.LOGGER.info("[MultiHooker] {} -> {}'s inventory", debugName, player.getName());
            }

            return true;
         }
      }

      double dx = player.getX() - hook.getX();
      double dy = player.getY() - hook.getY();
      double dz = player.getZ() - hook.getZ();
      ItemEntity drop = new ItemEntity(level, hook.getX(), hook.getY(), hook.getZ(), stack);
      drop.setDeltaMovement(dx * 0.1, dy * 0.1 + Math.sqrt(Math.sqrt(dx * dx + dy * dy + dz * dz)) * 0.08, dz * 0.1);
      level.addFreshEntity(drop);
      if ((Boolean)Config.DEBUG_LOG.get()) {
         MultiHookerMod.LOGGER.info("[MultiHooker] {} tossed to {} (inventory full)", debugName, player.getName());
      }

      return true;
   }

   /**
    * Tells the client a stack went straight into the player's inventory.
    *
    * <p>Only needed for the instant pickup path. Items thrown on the ground are picked up the
    * normal way and already produce the pickup packet that notifier mods listen for.
    */
   private static void announceDirectPickup(Player player, ItemStack stack) {
      if (player instanceof ServerPlayer serverPlayer) {
         PacketDistributor.sendToPlayer(serverPlayer, new MultiHookerNetworking.ItemDroppedPayload(stack));
      }
   }

   /** Drops a crate and - only when it really landed - unlocks that crate's advancement. */
   private static void dropCrate(Player player, FishingHook hook, ItemLike item, double chance, String debugName, String tier) {
      if (!dropCustomItem(player, hook, new ItemStack(item), chance, debugName)) {
         return;
      }
      announceCrateCatch(player, debugName);
      awardCrateAdvancement(player, tier);
   }

   /**
    * Tells the player a trove came up.
    *
    * <p>A crate appearing in the inventory is easy to miss: it lands while the fishing animation
    * is still playing and the cast bar is already gone by the time the item is there. The
    * advancement does not help either, because the toast only fires the first time each tier is
    * ever caught. So every catch says so on the action bar, and the four special tiers get a
    * louder line so they stand out from the ordinary ladder.
    */
   private static void announceCrateCatch(Player player, String debugName) {
      if (!(player instanceof ServerPlayer serverPlayer)) {
         return;
      }
      serverPlayer.displayClientMessage(
         Component.translatable(
            SPECIAL_CRATES_DISPLAY.contains(debugName)
               ? "message.multihooker.crate_caught_special"
               : "message.multihooker.crate_caught",
            debugName),
         true);
   }

   /** The four special troves, by the display name used in {@link #dropCrate}. */
   private static final java.util.List<String> SPECIAL_CRATES_DISPLAY =
      java.util.List.of("Divine Trove", "Ultra Divine Trove", "Unspoken Trove", "GhostUnspoken Trove");

   /**
    * Completes "multihooker:crates/&lt;tier&gt;" for a player who has just fished that crate
    * up, so the crate ladder fills in tier by tier.
    *
    * <p>The advancement files deliberately use the "impossible" trigger, which nothing
    * in the game fires on its own: a crate only counts when it comes out of the water,
    * not when it is picked up, placed and broken or handed out with /give.
    *
    * <p>Catching ANY special crate also checks the whole category: once Divine, Ultra
    * Divine, Unspoken AND GhostUnspoken have all been fished up, the "Is that???
    * SPECIAL???" challenge (multihooker:crates/special) completes. The list below is
    * the same four crates that data/multihooker/tags/item/special_crates.json
    * publishes, so "the special category" means one thing everywhere.
    *
    * <p>The tab's own root node ("multihooker:crates/root") is not awarded here: it
    * uses the "minecraft:tick" trigger, so the MultiHooker tab is already in the
    * advancement menu from the first tick of a world, with every crate still locked.
    */
   private static void awardCrateAdvancement(Player player, String tier) {
      if (!(player instanceof ServerPlayer serverPlayer)) {
         return;
      }

      award(serverPlayer, tier);

      for (String special : SPECIAL_CRATES) {
         if (!isFished(serverPlayer, special)) {
            return;
         }
      }

      award(serverPlayer, SPECIAL_COMPLETE);
   }

   /** The four crates of the special category (see special_crates.json). */
   private static final String[] SPECIAL_CRATES = {"divine", "ultra_divine", "unspoken", "ghost_unspoken"};
   private static final String SPECIAL_COMPLETE = "special";

   /** Whether this player has already fished that crate up. */
   private static boolean isFished(ServerPlayer serverPlayer, String path) {
      AdvancementHolder advancement = serverPlayer.server
         .getAdvancements()
         .get(ResourceLocation.fromNamespaceAndPath("multihooker", "crates/" + path));
      return advancement != null && serverPlayer.getAdvancements().getOrStartProgress(advancement).isDone();
   }

   private static void award(ServerPlayer serverPlayer, String path) {
      AdvancementHolder advancement = serverPlayer.server
         .getAdvancements()
         .get(ResourceLocation.fromNamespaceAndPath("multihooker", "crates/" + path));
      if (advancement != null) {
         serverPlayer.getAdvancements().award(advancement, "fished");
      }
   }

   private static void tryNetThrow(Player player, FishingHook hook) {
      if (hasSeaMaster(player)) {
         int level = RelicData.getLevel(player);
         double chance = RelicData.throwChance(level);
         if (!(chance <= 0.0) && !(player.getRandom().nextDouble() >= chance)) {
            int rolls = RelicData.rollsPerThrow(level);
            if (rolls > 0) {
               int spawned = spawnFishingRolls(player, hook, rolls);
               playNetVisual(player.level(), hook);
               if ((Boolean)Config.DEBUG_LOG.get() && spawned > 0) {
                  MultiHookerMod.LOGGER.info("[MultiHooker] The Net (Lv. {}) threw and hauled in {} item(s)", level, spawned);
               }
            }
         }
      }
   }

   private static void playNetVisual(Level level, FishingHook hook) {
      if (level instanceof ServerLevel serverLevel) {
         double x = hook.getX();
         double y = hook.getY() + 0.05;
         double z = hook.getZ();
         NetVisualEntity netEntity = new NetVisualEntity((EntityType<? extends NetVisualEntity>)MultiHookerRegistry.NET_VISUAL.get(), serverLevel);
         netEntity.moveTo(x, y, z, hook.getYRot(), 0.0F);
         serverLevel.addFreshEntity(netEntity);
         serverLevel.sendParticles(ParticleTypes.SPLASH, x, y + 0.2, z, 16, 0.9, 0.1, 0.9, 0.0);
         serverLevel.sendParticles(ParticleTypes.CLOUD, x, y + 0.1, z, 8, 0.8, 0.1, 0.8, 0.01);
         serverLevel.playSound(null, x, y, z, SoundEvents.FISHING_BOBBER_SPLASH, SoundSource.NEUTRAL, 1.0F, 0.9F);
      }
   }

   private static int spawnFishingRolls(Player player, FishingHook hook, int rolls) {
      if (!(player.level() instanceof ServerLevel serverLevel)) {
         return 0;
      } else {
         ItemStack var20 = rodInHand(player);
         LootParams params = new Builder(serverLevel)
            .withParameter(LootContextParams.ORIGIN, hook.position())
            .withParameter(LootContextParams.TOOL, var20)
            .withParameter(LootContextParams.THIS_ENTITY, hook)
            .withParameter(LootContextParams.ATTACKING_ENTITY, player)
            .withLuck(EnchantmentHelper.getFishingLuckBonus(serverLevel, var20, player) + player.getLuck())
            .create(LootContextParamSets.FISHING);
         ResourceKey tableKey = BuiltInLootTables.FISHING;
         LootTable table = serverLevel.getServer().reloadableRegistries().getLootTable(tableKey);
         int spawned = 0;

         for (int i = 0; i < rolls; i++) {
            for (ItemStack stack : table.getRandomItems(params)) {
               double dx = player.getX() - hook.getX();
               double dy = player.getY() - hook.getY();
               double dz = player.getZ() - hook.getZ();
               ItemEntity drop = new ItemEntity(serverLevel, hook.getX(), hook.getY(), hook.getZ(), stack);
               drop.setDeltaMovement(dx * 0.1, dy * 0.1 + Math.sqrt(Math.sqrt(dx * dx + dy * dy + dz * dz)) * 0.08, dz * 0.1);
               serverLevel.addFreshEntity(drop);
               spawned++;
            }
         }

         return spawned;
      }
   }

   private static ItemStack rodInHand(Player player) {
      if (player.getMainHandItem().canPerformAction(ItemAbilities.FISHING_ROD_CAST)) {
         return player.getMainHandItem();
      } else {
         return player.getOffhandItem().canPerformAction(ItemAbilities.FISHING_ROD_CAST) ? player.getOffhandItem() : ItemStack.EMPTY;
      }
   }
}
