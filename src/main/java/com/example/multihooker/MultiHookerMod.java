package com.example.multihooker;

import com.example.multihooker.client.MultiHookerClient;
import com.example.multihooker.compat.CuriosBridge;
import com.mojang.logging.LogUtils;
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
      MultiHookerRegistry.CREATIVE_MODE_TABS.register(modEventBus);
      MultiHookerRegistry.ENTITY_TYPES.register(modEventBus);
      MultiHookerRegistry.MENU_TYPES.register(modEventBus);
      MultiHookerRegistry.BLOCK_ENTITIES.register(modEventBus);
      modEventBus.addListener(MultiHookerMod::onRegisterPayloads);
      NeoForge.EVENT_BUS.addListener(MultiHookerMod::onEntityJoinLevel);
      NeoForge.EVENT_BUS.addListener(MultiHookerMod::onPlayerLoggedIn);
      NeoForge.EVENT_BUS.addListener(MultiHookerMod::onRodRightClick);
      NeoForge.EVENT_BUS.addListener(KeyLootHandler::onItemFished);
      NeoForge.EVENT_BUS.addListener(MultiHookerMod::onServerTick);
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

   private static void onEntityJoinLevel(EntityJoinLevelEvent event) {
      MultiHookerManager.onHookJoined(event);
   }

   private static void onPlayerLoggedIn(PlayerLoggedInEvent event) {
      Player player = (Player)event.getEntity();
      if (player.level().isClientSide()) {
         return;
      }

      int removed = SkillData.wipeLegacyKeys(player);
      if (removed > 0 && (Boolean)Config.DEBUG_LOG.get()) {
         LOGGER.info("[MultiHooker] {}: deleted {} legacy AquaKey(s) - keys are no longer the upgrade currency", player.getName(), removed);
      }
   }

   private static void onRodRightClick(RightClickItem event) {
      MultiHookerManager.onRodRightClick(event.getEntity(), event.getItemStack());
   }

   private static void onServerTick(Post event) {
      MultiHookerManager.tick(event.getServer());
   }
}
