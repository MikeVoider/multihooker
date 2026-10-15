package com.example.multihooker.client;

import com.example.multihooker.MultiHookerNetworking;
import com.example.multihooker.MultiHookerRegistry;
import com.example.multihooker.RelicData;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.network.PacketDistributor;

public class SeaMasterRelicScreen extends Screen {
   private static final int PANEL_WIDTH = 190;
   private static final int PANEL_HEIGHT = 116;
   private static final int BUTTON_WIDTH = 84;
   private static final int BUTTON_HEIGHT = 18;
   private Button empowerButton;

   public SeaMasterRelicScreen() {
      super(Component.literal("SeaMaster Relic"));
   }

   protected void init() {
      int left = (this.width - PANEL_WIDTH) / 2;
      int top = (this.height - PANEL_HEIGHT) / 2;
      int buttonY = top + PANEL_HEIGHT - 24;
      this.empowerButton = (Button)this.addRenderableWidget(
         Button.builder(
               Component.literal("Empower"),
               button -> PacketDistributor.sendToServer(MultiHookerNetworking.LevelUpSeaMasterPayload.INSTANCE, new CustomPacketPayload[0])
            )
            .bounds(left + 8, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT)
            .build()
      );
      this.addRenderableWidget(
         Button.builder(Component.translatable("gui.done"), button -> this.onClose()).bounds(left + PANEL_WIDTH - BUTTON_WIDTH - 8, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT).build()
      );
      this.refresh();
   }

   public void tick() {
      this.refresh();
      super.tick();
   }

   private void refresh() {
      Player player = this.minecraft == null ? null : this.minecraft.player;
      if (player != null) {
         int cost = RelicData.costForNextLevel(player);
         this.empowerButton.active = cost > 0 && player.experienceLevel >= cost;
         this.empowerButton.setMessage(cost <= 0 ? Component.literal("Max") : Component.literal("Empower (" + cost + " XP)"));
      }
   }

   public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      super.render(graphics, mouseX, mouseY, partialTick);
      int left = (this.width - PANEL_WIDTH) / 2;
      int top = (this.height - PANEL_HEIGHT) / 2;
      graphics.fill(left, top, left + PANEL_WIDTH, top + PANEL_HEIGHT, -535818224);
      graphics.renderOutline(left, top, PANEL_WIDTH, PANEL_HEIGHT, -3562969);
      Player player = this.minecraft == null ? null : this.minecraft.player;
      int level = player == null ? 0 : RelicData.getLevel(player);
      int xpLevels = player == null ? 0 : player.experienceLevel;
      int cost = player == null ? 0 : RelicData.costForNextLevel(player);
      double chance = RelicData.throwChance(level);
      int rolls = RelicData.rollsPerThrow(level);
      double nextChance = RelicData.throwChance(level + 1);
      int nextRolls = RelicData.rollsPerThrow(level + 1);
      graphics.renderItem(new ItemStack((ItemLike)MultiHookerRegistry.SEA_MASTER.get()), left + 8, top + 8);
      graphics.drawString(this.font, this.title, left + 28, top + 9, -10929, false);
      graphics.drawString(this.font, Component.literal("The Net  -  Lv. " + level), left + 28, top + 20, -9710081, false);
      int y = top + 34;
      graphics.drawString(this.font, Component.literal(String.format("Chance %d%%", Math.round(chance * 100.0))), left + 12, y, -6497026, false);
      graphics.drawString(this.font, Component.literal(cost > 0 ? String.format("> %d%%", Math.round(nextChance * 100.0)) : "MAX"), left + 118, y, cost > 0 ? -9699477 : -8355712, false);
      y += 14;
      graphics.drawString(this.font, Component.literal(String.format("Rolls %d", rolls)), left + 12, y, -6497026, false);
      graphics.drawString(this.font, Component.literal(cost > 0 ? String.format("> %d", nextRolls) : "MAX"), left + 118, y, cost > 0 ? -9699477 : -8355712, false);
      y += 14;
      graphics.drawString(this.font, Component.literal(cost > 0 ? "Next: " + cost + " XP levels" : "Fully mastered"), left + 12, y, -5005861, false);
      y += 12;
      graphics.drawString(this.font, Component.literal("You have " + xpLevels + " XP levels"), left + 12, y, -8355712, false);
   }

   public void onClose() {
      if (this.minecraft != null) {
         this.minecraft.setScreen(null);
      }
   }

   public boolean isPauseScreen() {
      return false;
   }
}