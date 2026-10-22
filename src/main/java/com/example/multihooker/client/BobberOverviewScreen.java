package com.example.multihooker.client;

import com.example.multihooker.Config;
import com.example.multihooker.KeyLootHandler;
import com.example.multihooker.MultiHookerNetworking;
import com.example.multihooker.SkillData;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;

public class BobberOverviewScreen extends Screen {
   private static final int PANEL_WIDTH = 190;
   private static final int PANEL_HEIGHT = 184;
   private static final int BUTTON_HEIGHT = 18;
   private int linesInWater;
   private int linesBiting;
   private boolean hasSeaMaster;
   private Button upgradeButton;
   private Button resetButton;

   public BobberOverviewScreen() {
      super(Component.literal("MultiHooker"));
   }

   protected void init() {
      int left = (this.width - PANEL_WIDTH) / 2;
      int top = (this.height - PANEL_HEIGHT) / 2;
      this.upgradeButton = (Button)this.addRenderableWidget(
         Button.builder(
               Component.literal("Upgrade"),
               button -> PacketDistributor.sendToServer(MultiHookerNetworking.UpgradeBobbersSkillPayload.INSTANCE, new CustomPacketPayload[0])
            )
            .bounds(left + (PANEL_WIDTH - 60) / 2, top + 82, 60, 16)
            .build()
      );
      int rowY = top + PANEL_HEIGHT - 22;
      this.addRenderableWidget(
         Button.builder(Component.literal("Trove Loot"), button -> this.openCrateLoot())
            .bounds(left + (PANEL_WIDTH - 120) / 2, top + 136, 120, 18)
            .tooltip(Tooltip.create(Component.literal("Browse what every trove can drop.")))
            .build()
      );
      this.resetButton = (Button)this.addRenderableWidget(
         Button.builder(
               Component.literal("Reset"),
               button -> PacketDistributor.sendToServer(MultiHookerNetworking.ResetBobbersSkillPayload.INSTANCE, new CustomPacketPayload[0])
            )
            .bounds(left + 10, rowY, 54, BUTTON_HEIGHT)
            .tooltip(Tooltip.create(Component.literal("Sets More Bobbers back to level 0 (one bobber).")))
            .build()
      );
      this.addRenderableWidget(
         Button.builder(Component.literal("Relic"), button -> TooltipClient.openRelicMenu())
            .bounds(left + 68, rowY, 54, BUTTON_HEIGHT)
            .tooltip(Tooltip.create(Component.literal("The SeaMaster Relic: The Net ability")))
            .build()
      );
      this.addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> this.onClose()).bounds(left + 126, rowY, 54, BUTTON_HEIGHT).build());
      this.refresh();
   }

   private void openCrateLoot() {
      if (this.minecraft != null) {
         this.minecraft.setScreen(new CrateLootScreen(this));
      }
   }

   public void tick() {
      this.refresh();
      super.tick();
   }

   private void refresh() {
      this.linesInWater = 0;
      this.linesBiting = 0;
      this.hasSeaMaster = false;
      Player player = this.minecraft == null ? null : this.minecraft.player;
      if (player != null) {
         this.linesInWater = HookStats.hooksInWater(player);
         this.linesBiting = HookStats.hooksBiting(player);
         this.hasSeaMaster = KeyLootHandler.hasSeaMaster(player);
         int nextCost = SkillData.costForNextLevel(player);
         this.upgradeButton.active = nextCost > 0 && SkillData.getPoints(player) >= nextCost;
         this.upgradeButton.setMessage(nextCost <= 0 ? Component.literal("Max") : Component.literal("Upgrade"));
      }
   }

   public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      super.render(graphics, mouseX, mouseY, partialTick);
      int left = (this.width - PANEL_WIDTH) / 2;
      int top = (this.height - PANEL_HEIGHT) / 2;
      int centerX = this.width / 2;
      graphics.fill(left, top, left + PANEL_WIDTH, top + PANEL_HEIGHT, -871362544);
      graphics.renderOutline(left, top, PANEL_WIDTH, PANEL_HEIGHT, -11184811);
      Player player = this.minecraft == null ? null : this.minecraft.player;
      int skillLevel = player == null ? 0 : SkillData.getLevel(player);
      int points = player == null ? 0 : SkillData.getPoints(player);
      int nextCost = player == null ? 0 : SkillData.costForNextLevel(player);
      int totalBobbers = 1 + (Integer)Config.EXTRA_HOOKS.get() + SkillData.bonusHooks(player);
      graphics.drawCenteredString(this.font, this.title, centerX, top + 6, -10929);
      graphics.drawCenteredString(this.font, Component.literal("Bobbers per cast: " + totalBobbers), centerX, top + 18, -6497026);
      graphics.drawCenteredString(
         this.font,
         Component.literal("In the water: " + this.linesInWater + "   Biting: " + this.linesBiting),
         centerX,
         top + 30,
         this.linesBiting > 0 ? -9699477 : -6497026
      );
      graphics.drawCenteredString(this.font, Component.literal("- Skills -"), centerX, top + 42, -10929);
      graphics.drawString(this.font, Component.literal("More Bobbers"), left + 12, top + 56, -6497026, false);
      graphics.drawString(this.font, Component.literal("Lv. " + skillLevel), left + 132, top + 56, -1, false);
      boolean affordable = points >= nextCost;
      this.drawCenteredFit(
         graphics,
         nextCost > 0 ? Component.literal("Next Upgrade: " + nextCost + " Points Required") : Component.literal("Skill maxed"),
         centerX,
         top + 70,
         nextCost > 0 ? (affordable ? -9699477 : -10929) : -8355712
      );
      graphics.drawCenteredString(this.font, Component.literal("Points: " + points), centerX, top + 102, -5005861);
      graphics.drawCenteredString(
         this.font,
         Component.literal(this.hasSeaMaster ? "SeaMaster: WORN - ACTIVE" : "SeaMaster: not worn"),
         centerX,
         top + 114,
         this.hasSeaMaster ? -9699477 : -8355712
      );
      this.drawCenteredFit(
         graphics,
         Component.literal("Catch AquaKeys for points - Shift + Right-Click the Relic"),
         centerX,
         top + 126,
         -8355712
      );
   }

   /**
    * Draws a centred line, shrinking it uniformly if it would be wider than the
    * panel (the hint lines are longer than the panel and used to spill out of
    * its sides). maxWidth is measured in the same units as PANEL_WIDTH.
    */
   private void drawCenteredFit(GuiGraphics graphics, Component text, int centerX, int y, int color) {
      int maxWidth = PANEL_WIDTH - 12;
      int textWidth = this.font.width(text);
      if (textWidth <= maxWidth) {
         graphics.drawCenteredString(this.font, text, centerX, y, color);
         return;
      }
      float scale = (float)maxWidth / (float)textWidth;
      PoseStack pose = graphics.pose();
      pose.pushPose();
      pose.translate((float)centerX, (float)y, 0.0F);
      pose.scale(scale, scale, 1.0F);
      graphics.drawCenteredString(this.font, text, 0, 0, color);
      pose.popPose();
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