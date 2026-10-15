package com.example.multihooker.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

public final class TooltipClient {
   private TooltipClient() {
   }

   public static boolean hasShiftDown() {
      return Screen.hasShiftDown();
   }

   public static void openRelicMenu() {
      Minecraft.getInstance().setScreen(new SeaMasterRelicScreen());
   }
}
