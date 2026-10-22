package com.example.multihooker;

import com.example.multihooker.client.MultiHookerClient;
import com.example.multihooker.compat.CuriosBridge;
import com.example.multihooker.treasure.CrateBonusLoot;
import com.example.multihooker.treasure.CrateLuckBonus;
import com.example.multihooker.treasure.VitalityHealth;
import com.mojang.logging.LogUtils;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig.Type;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.TagsUpdatedEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.Clone;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickItem;
import net.neoforged.neoforge.event.tick.ServerTickEvent.Post;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import org.slf4j.Logger;

@Mod("multihooker")
public final class MultiHookerMod {
   public static final String MOD_ID = "multihooker";
   public static final Logger LOGGER = LogUtils.getLogger();

   public MultiHookerMod(IEventBus modEventBus, ModContainer modContainer) {
      modContainer.registerConfig(Type.COMMON, Config.SPEC);
      MultiHookerRegistry.BLOCKS.register(modEventBus);
      MultiHookerRegistry.ITEMS.register(modEventBus);
      MultiHookerRegistry.ATTACHMENTS.register(modEventBus);
      MultiHookerRegistry.EFFECTS.register(modEventBus);
      MultiHookerRegistry.CREATIVE_MODE_TABS.register(modEventBus);
      MultiHookerRegistry.ENTITY_TYPES.register(modEventBus);
      MultiHookerRegistry.MENU_TYPES.register(modEventBus);
      MultiHookerRegistry.BLOCK_ENTITIES.register(modEventBus);
      modEventBus.addListener(MultiHookerMod::onRegisterPayloads);
      NeoForge.EVENT_BUS.addListener(MultiHookerMod::onEntityJoinLevel);
      NeoForge.EVENT_BUS.addListener(MultiHookerMod::onPlayerLoggedIn);
      NeoForge.EVENT_BUS.addListener(MultiHookerMod::onClone);
      NeoForge.EVENT_BUS.addListener(MultiHookerMod::onCrateLuckAdded);
      NeoForge.EVENT_BUS.addListener(MultiHookerMod::onCrateLuckRemoved);
      NeoForge.EVENT_BUS.addListener(MultiHookerMod::onCrateLuckExpired);
      NeoForge.EVENT_BUS.addListener(MultiHookerMod::onRodRightClick);
      NeoForge.EVENT_BUS.addListener(KeyLootHandler::onItemFished);
      NeoForge.EVENT_BUS.addListener(MultiHookerMod::onServerTick);
      NeoForge.EVENT_BUS.addListener(MultiHookerMod::onTagsUpdated);
      if (FMLEnvironment.dist.isClient()) {
         MultiHookerClient.init(modEventBus, modContainer);
      }

      // Curios: MultiHookerRegistry.SEA_MASTER is a deferred holder, so it is still
      // unbound while this constructor runs. Asking it for the item here threw
      // "Trying to access unbound value: ResourceKey[minecraft:item /
      // multihooker:sea_master]" and the relic was never handed to Curios at all.
      // Common setup runs AFTER the item registry event, so by then the holder is
      // bound and the real item can be registered. Queried live by Curios, so the
      // timing is safe.
      if (ModList.get().isLoaded("curios")) {
         modEventBus.addListener(MultiHookerMod::onCommonSetup);
      }
   }

   private static void onCommonSetup(FMLCommonSetupEvent event) {
      event.enqueueWork(CuriosBridge::register);
   }

   private static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
      MultiHookerNetworking.register(event);
   }


   /**
    * The crate relic list is built from the Curios slot tags, so it has to be
    * rebuilt when tags change - otherwise enabling a new relic mod after launch
    * would leave the crates unable to drop its items until a restart.
    */
   private static void onTagsUpdated(TagsUpdatedEvent event) {
      CrateBonusLoot.invalidateRelics();
   }

   private static void onEntityJoinLevel(EntityJoinLevelEvent event) {
      MultiHookerManager.onHookJoined(event);
   }

   private static void onPlayerLoggedIn(PlayerLoggedInEvent event) {
      Player player = (Player)event.getEntity();
      if (player.level().isClientSide()) {
         return;
      }

      VitalityHealth.sync(player);

      int removed = SkillData.wipeLegacyKeys(player);
      if (removed > 0 && (Boolean)Config.DEBUG_LOG.get()) {
         LOGGER.info("[MultiHooker] {}: deleted {} legacy AquaKey(s) - keys are no longer the upgrade currency", player.getName(), removed);
      }
   }

   /**
    * Re-applies permanent Vitality health after a respawn. The total is stored in world save
    * data, so it is already safe; this only puts it back onto the new entity's attribute.
    */
   private static void onClone(Clone event) {
      if (!event.isWasDeath() || event.getEntity().level().isClientSide()) {
         return;
      }
      VitalityHealth.sync(event.getEntity());
      if (event.getEntity().hasEffect(MultiHookerRegistry.CRATE_LUCK)) {
         CrateLuckBonus.apply(event.getEntity());
      }
   }

   /**
    * Keeps Crate Luck attached to the player's Luck attribute for as long as the effect runs.
    *
    * <p>Done through the effect's own lifecycle events rather than a per-tick check, so it costs
    * nothing while nobody is drinking the potion.
    */
   private static void onCrateLuckAdded(MobEffectEvent.Added event) {
      if (isCrateLuck(event.getEffectInstance())) {
         CrateLuckBonus.apply(event.getEntity());
      }
   }

   /** Covers milk and any other cure, not just natural expiry. */
   private static void onCrateLuckRemoved(MobEffectEvent.Remove event) {
      if (event.getEffect().is(MultiHookerRegistry.CRATE_LUCK)) {
         CrateLuckBonus.clear(event.getEntity());
      }
   }

   private static void onCrateLuckExpired(MobEffectEvent.Expired event) {
      if (isCrateLuck(event.getEffectInstance())) {
         CrateLuckBonus.clear(event.getEntity());
      }
   }

   private static boolean isCrateLuck(MobEffectInstance instance) {
      return instance != null && instance.getEffect().is(MultiHookerRegistry.CRATE_LUCK);
   }

   private static void onRodRightClick(RightClickItem event) {
      MultiHookerManager.onRodRightClick(event.getEntity(), event.getItemStack());
   }

   private static void onServerTick(Post event) {
      MultiHookerManager.tick(event.getServer());
   }
}
