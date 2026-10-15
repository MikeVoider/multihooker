package com.example.multihooker.client;

import com.example.multihooker.CrateMenu;
import com.example.multihooker.EmptyCrateBlock;
import com.example.multihooker.MultiHookerMod;
import com.example.multihooker.MultiHookerRegistry;
import com.mojang.blaze3d.platform.InputConstants.Type;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent.Post;
import net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterLayerDefinitions;
import net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;
import top.theillusivec4.curios.api.client.CuriosRendererRegistry;

public final class MultiHookerClient {
   private static final KeyMapping OPEN_PANEL = new KeyMapping("key.multihooker.open_panel", Type.KEYSYM, 80, "key.categories.multihooker");

   private MultiHookerClient() {
   }

   public static void init(IEventBus modEventBus, ModContainer modContainer) {
      modEventBus.addListener(MultiHookerClient::onRegisterKeyMappings);
      NeoForge.EVENT_BUS.addListener(MultiHookerClient::onClientTick);
      modEventBus.addListener(MultiHookerClient::onClientSetup);
      modEventBus.addListener(MultiHookerClient::onRegisterLayerDefinitions);
      modEventBus.addListener(MultiHookerClient::onRegisterRenderers);
      modEventBus.addListener(MultiHookerClient::onRegisterAdditionalModels);
      modEventBus.addListener(MultiHookerClient::onRegisterMenuScreens);
      // In-game settings screen: Options > Mods > MultiHooker > Config. The
      // Rendering and Diagnostics sections are hidden (internal tuning), and the
      // SeaMaster relic's own catch chance is kept out of the list - the relic is
      // not meant to be tuned. Every crate and the AquaKey stay adjustable.
      modContainer.registerExtensionPoint(
         IConfigScreenFactory.class,
         (container, parent) -> new ConfigurationScreen(
               container,
               parent,
               (screen, type, modConfig, title) -> new MultiHookerConfigScreen(
                     screen, type, modConfig, title,
                     (context, key, original) -> MultiHookerConfigScreen.isHiddenEntry(key) ? null : original
                  )
            )
      );
      MultiHookerMod.LOGGER
         .info("MultiHooker client init: Fishing Panel keybind registered (default P), config screen registered (Rendering/Diagnostics locked)");
   }

   private static void onClientSetup(FMLClientSetupEvent event) {
      if (ModList.get().isLoaded("curios")) {
         event.enqueueWork(() -> CuriosRendererRegistry.register((Item)MultiHookerRegistry.SEA_MASTER.get(), SeaMasterCurioRenderer::new));
      }
   }

   private static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
      event.register(OPEN_PANEL);
   }

   private static void onRegisterLayerDefinitions(RegisterLayerDefinitions event) {
      event.registerLayerDefinition(SeaMasterNecklaceModel.LAYER, SeaMasterNecklaceModel::createLayer);
   }

   private static void onRegisterRenderers(RegisterRenderers event) {
      event.registerEntityRenderer((EntityType)MultiHookerRegistry.NET_VISUAL.get(), NetVisualRenderer::new);
      // The swinging lid of an opened crate. Every tier shares one block entity
      // type; the renderer picks the right lid model off the block.
      event.registerBlockEntityRenderer(MultiHookerRegistry.CRATE_LID.get(), CrateLidRenderer::new);
   }

   /**
    * The lid is drawn by {@code CrateLidRenderer}, not by the blockstate, so its
    * model is never reached through a blockstate or an item and Minecraft would
    * not otherwise bake it. Registered as a standalone model for every empty
    * crate, found by walking the block registry (so adding a tier needs nothing
    * here).
    */
   private static void onRegisterAdditionalModels(ModelEvent.RegisterAdditional event) {
      for (Block block : BuiltInRegistries.BLOCK) {
         if (block instanceof EmptyCrateBlock crate) {
            event.register(ModelResourceLocation.standalone(crate.lidModel()));
         }
      }
   }

   /** The chest-style loot window opened by right-clicking a placed crate. */
   private static void onRegisterMenuScreens(RegisterMenuScreensEvent event) {
      event.register(MultiHookerRegistry.CRATE_MENU.get(), CrateScreen::new);
   }

   private static void onClientTick(Post event) {
      Minecraft minecraft = Minecraft.getInstance();
      if (minecraft.player != null && minecraft.screen == null) {
         while (OPEN_PANEL.consumeClick()) {
            minecraft.setScreen(new BobberOverviewScreen());
         }
      }
   }
}
