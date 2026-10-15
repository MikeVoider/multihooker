package com.example.multihooker.compat;

import com.example.multihooker.MultiHookerMod;
import com.example.multihooker.MultiHookerRegistry;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

public final class CuriosBridge {
   private static final String NECK_SLOT = "necklace";

   private CuriosBridge() {
   }

   public static void register() {
      try {
         CuriosApi.registerCurio((Item)MultiHookerRegistry.SEA_MASTER.get(), new CuriosBridge.SeaMasterCurio());
         MultiHookerMod.LOGGER.info("MultiHooker: SeaMaster Relic registered as a Curios neck-slot relic");
      } catch (Throwable var1) {
         MultiHookerMod.LOGGER.error("MultiHooker: Curios registration failed", var1);
      }
   }

   public static boolean isEquipped(Player player) {
      try {
         return CuriosApi.getCuriosInventory(player).map(handler -> {
            IItemHandlerModifiable equipped = handler.getEquippedCurios();

            for (int i = 0; i < equipped.getSlots(); i++) {
               ItemStack stack = equipped.getStackInSlot(i);
               if (!stack.isEmpty() && stack.is(((Item)MultiHookerRegistry.SEA_MASTER.get()).asItem())) {
                  return true;
               }
            }

            return false;
         }).orElse(false);
      } catch (Throwable var2) {
         return false;
      }
   }

   public static class SeaMasterCurio implements ICurioItem {
      public boolean canEquip(SlotContext slotContext, ItemStack stack) {
         return "necklace".equals(slotContext.identifier());
      }

      public boolean canEquipFromUse(SlotContext slotContext, ItemStack stack) {
         return "necklace".equals(slotContext.identifier());
      }
   }
}
