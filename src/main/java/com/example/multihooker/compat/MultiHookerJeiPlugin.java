package com.example.multihooker.compat;

import com.example.multihooker.Config;
import com.example.multihooker.MultiHookerMod;
import com.example.multihooker.MultiHookerRegistry;
import com.example.multihooker.client.CrateLootData;
import java.util.List;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ItemLike;

@JeiPlugin
public final class MultiHookerJeiPlugin implements IModPlugin {
   public ResourceLocation getPluginUid() {
      return ResourceLocation.fromNamespaceAndPath("multihooker", "jei");
   }

   /**
    * Adds the "Crate Loot" page, so pressing R on any crate shows what that crate
    * can give (with amounts and odds), just like looking up a recipe.
    */
   public void registerCategories(IRecipeCategoryRegistration registration) {
      registration.addRecipeCategories(new CrateLootRecipeCategory(registration.getJeiHelpers().getGuiHelper()));
   }

   public void registerRecipes(IRecipeRegistration registration) {
      List<CrateLootData.Tier> tiers = CrateLootData.tiersFromJar();
      if (tiers.isEmpty()) {
         MultiHookerMod.LOGGER.error("[MultiHooker] JEI: crate loot data is empty - no crate pages in JEI");
      } else {
         registration.addRecipes(CrateLootRecipeCategory.RECIPE_TYPE, tiers);
         MultiHookerMod.LOGGER.info("[MultiHooker] JEI: registered {} crate loot pages", tiers.size());
      }

      double chance = (Double)Config.KEY_DROP_CHANCE.get();
      int chancePercent = (int)Math.round(chance * 100.0);
      int cost = (Integer)Config.POINTS_PER_UPGRADE.get();
      int perKey = (Integer)Config.POINTS_PER_KEY.get();
      String seaMasterPercent = formatPercent((Double)Config.SEA_MASTER_DROP_CHANCE.get());
      registration.addIngredientInfo(
         (ItemLike)MultiHookerRegistry.AQUAKEY.get(),
         new Component[]{
            Component.translatable("jei.multihooker.aquakey.line1"),
            Component.literal(String.format("Dropped by fishing catches (%d%% per catch)", chancePercent)),
            Component.literal(String.format("Every key fished up is worth %d points", perKey)),
            Component.literal(String.format("Fishing Panel (key: P): %d points = +1 bobber", cost)),
            Component.literal("Right-click a crate with it to unlock it (uses 1 key)"),
            Component.literal("Works with every fishing rod, vanilla or modded")
         }
      );
      registration.addIngredientInfo(
         (ItemLike)MultiHookerRegistry.SEA_MASTER.get(),
         new Component[]{
            Component.literal("SeaMaster Relic - mastery of the deep."),
            Component.literal("Its ability is described in the Relic Menu:"),
            Component.literal("Shift + Right-Click the relic to open it."),
            Component.literal("Active ONLY while worn in the Curios neck slot -"),
            Component.literal("carrying it does nothing."),
            Component.literal("Obtained ONLY as a rare fishing catch (" + seaMasterPercent + "% per catch).")
         }
      );
   }

   private static String formatPercent(double fraction) {
      double percent = fraction * 100.0;
      return Math.abs(percent - (double)Math.round(percent)) < 0.05
         ? String.valueOf(Math.round(percent))
         : String.format("%.1f", percent);
   }
}
