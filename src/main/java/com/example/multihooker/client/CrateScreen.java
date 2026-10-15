package com.example.multihooker.client;

import com.example.multihooker.CrateMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * The crate loot window. It is deliberately just a plain chest: the vanilla
 * 3-row chest background with the player's inventory underneath, and the crate's
 * name as the title. There is no odds panel and no extra labels - the loot you
 * rolled is the whole window.
 *
 * <p>Same sprite and layout vanilla uses for a chest ({@code generic_54}), so it
 * reads as "I opened a container", nothing more.
 */
public class CrateScreen extends AbstractContainerScreen<CrateMenu> {
   private static final ResourceLocation TEXTURE = ResourceLocation.withDefaultNamespace("textures/gui/container/generic_54.png");
   /** The crate holds 27 slots, i.e. a chest's three rows. */
   private static final int ROWS = 3;

   public CrateScreen(CrateMenu menu, Inventory inventory, Component title) {
      super(menu, inventory, title);
      this.imageHeight = 114 + ROWS * 18;
      this.inventoryLabelY = this.imageHeight - 94;
   }

   @Override
   public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      super.render(graphics, mouseX, mouseY, partialTick);
      // AbstractContainerScreen does not draw tooltips for us (ContainerScreen does).
      this.renderTooltip(graphics, mouseX, mouseY);
   }

   @Override
   protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
      int x = this.leftPos;
      int y = this.topPos;
      graphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, ROWS * 18 + 17);
      graphics.blit(TEXTURE, x, y + ROWS * 18 + 17, 0, 126, this.imageWidth, 96);
   }
}
