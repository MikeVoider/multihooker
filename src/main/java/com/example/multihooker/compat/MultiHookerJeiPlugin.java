package com.example.multihooker.compat;

import com.example.multihooker.Config;
import com.example.multihooker.MultiHookerMod;
import com.example.multihooker.MultiHookerRegistry;
import com.example.multihooker.client.CrateLootData;
import com.example.multihooker.treasure.MysteryBookItem;
import java.util.ArrayList;
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
            Component.literal("Unlocks the locked one you fished up (uses 1 key)"),
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

      addMysteryBookInfo(registration);
      addTreasureInfo(registration);
   }

   /**
    * One info page per drinkable treasure. The drop chances come straight from the
    * config, so the page always states what is actually configured, and the brew
    */
   private static void addTreasureInfo(IRecipeRegistration registration) {
      String vitalityChance = formatPercent((Double)Config.ELIXIR_VITALITY_CHANCE.get());
      String fortuneChance = formatPercent((Double)Config.ELIXIR_FORTUNE_CHANCE.get());
      String potionChance = formatPercent((Double)Config.CRATE_POTION_CHANCE.get());

      registration.addIngredientInfo(
         (ItemLike)MultiHookerRegistry.ELIXIR_VITALITY.get(),
         new Component[]{
            Component.translatable("jei.multihooker.elixir_vitality.effects"),
            Component.literal("In a Trove: " + vitalityChance + "% per opening"),
            Component.translatable("jei.multihooker.elixir_vitality.trove_note")
         }
      );
      registration.addIngredientInfo(
         (ItemLike)MultiHookerRegistry.ELIXIR_FORTUNE.get(),
         new Component[]{
            Component.translatable("jei.multihooker.elixir_fortune.effects"),
            Component.literal("In a Trove: " + fortuneChance + "% per opening"),
            Component.translatable("jei.multihooker.elixir_fortune.trove_note")
         }
      );

      String bonus = trimNumber((Double)Config.CRATE_LUCK_BONUS.get());
      registration.addIngredientInfo(
         (ItemLike)MultiHookerRegistry.CRATE_POTION.get(),
         new Component[]{
            Component.translatable("jei.multihooker.crate_potion.effects"),
            Component.literal("Adds +" + bonus + " Luck to every opening,"),
            Component.literal("on top of your own luck, for 4:00."),
            Component.literal("In a Trove: " + potionChance + "% per opening"),
            Component.translatable("jei.multihooker.crate_potion.trove_note")
         }
      );
   }

   /** Formats a config number without a pointless trailing ".0". */
   private static String trimNumber(double value) {
      return value == Math.rint(value) ? String.valueOf((int)value) : String.valueOf(value);
   }

   /**
    * One info page per Mystery Enchantment Book. The lines are built from the
    * item's own {@link MysteryBookItem.Tier}, so the JEI text can never drift
    * from the real roll rules.
    */
   private static void addMysteryBookInfo(IRecipeRegistration registration) {
      List<MysteryBookItem> books = List.of(
         MultiHookerRegistry.MYSTERY_BOOK_COMMON.get(),
         MultiHookerRegistry.MYSTERY_BOOK_RARE.get(),
         MultiHookerRegistry.MYSTERY_BOOK_EPIC.get(),
         MultiHookerRegistry.MYSTERY_BOOK_LEGENDARY.get(),
         MultiHookerRegistry.MYSTERY_BOOK_MYTHICAL.get()
      );

      for (MysteryBookItem book : books) {
         MysteryBookItem.Tier tier = book.getTier();
         List<Component> lines = new ArrayList<>();
         lines.add(Component.translatable("jei.multihooker.mystery_book.open"));
         if (tier.minEnchants == tier.maxEnchants) {
            lines.add(Component.translatable("jei.multihooker.mystery_book.rolls_exact", tier.maxEnchants));
         } else {
            lines.add(Component.translatable("jei.multihooker.mystery_book.rolls",
               tier.minEnchants, tier.maxEnchants));
         }
         if (tier.maxLevelEnchants) {
            lines.add(Component.translatable("jei.multihooker.mystery_book.max_levels"));
         } else {
            lines.add(Component.translatable("jei.multihooker.mystery_book.levels",
               tier.minLevel, tier.maxLevel));
         }
         if (tier.allowTreasure) {
            lines.add(Component.translatable("jei.multihooker.mystery_book.treasure"));
         }
         if (tier.allowCurses) {
            lines.add(Component.translatable("jei.multihooker.mystery_book.curse"));
         }
         lines.add(Component.translatable("jei.multihooker.mystery_book.cannot_craft"));
         registration.addIngredientInfo((ItemLike)book, lines.toArray(new Component[0]));
      }
   }

   private static String formatPercent(double fraction) {
      double percent = fraction * 100.0;
      return Math.abs(percent - (double)Math.round(percent)) < 0.05
         ? String.valueOf(Math.round(percent))
         : String.format("%.1f", percent);
   }
}
