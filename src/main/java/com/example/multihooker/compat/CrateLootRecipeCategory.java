package com.example.multihooker.compat;

import com.example.multihooker.MultiHookerRegistry;
import com.example.multihooker.client.CrateLootData;
import java.util.List;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * The JEI page shown when you press R on a crate: the crate (and the AquaKey it
 * costs) on the left, and everything that crate can give on the right.
 *
 * <p>One recipe per crate tier, so JEI looks up exactly that crate - like a
 * normal recipe. Hovering a loot slot shows how much you get and how likely it
 * is per roll. The data comes from the same generated file the in-game Crate
 * Loot screen uses, so the two always agree.
 *
 * <p>The page has TWO grids, matching the in-game Crate Loot screen: the crate's
 * OWN loot, then its cross-mod BONUS roll (one extra item pulled from the broad
 * conventional tags, where other installed mods' loot shows up - including
 * AquaCulture's Neptune's Bounty). The two pools have unrelated weights, so the
 * bonus grid sits under its own heading and its chances are worked out against
 * the bonus pool alone.
 *
 * <p>The category tab's own icon is that same crate: JEI asks {@link #getIcon()}
 * for it every frame and tells us which recipe it is drawing, so the icon at the
 * top-left of the page always matches the crate the player opened.
 *
 * <p>IMPORTANT: the crate is registered as an OUTPUT slot, not an input. JEI's
 * "R" (show recipes) key only ever searches OUTPUT slots of a recipe, so with
 * the crate as an input pressing R on a crate would find nothing at all. It is
 * still drawn as an ordinary slot on the left, where it reads naturally as
 * "this crate + this key give these items".
 */
public final class CrateLootRecipeCategory implements IRecipeCategory<CrateLootData.Tier> {
   public static final RecipeType<CrateLootData.Tier> RECIPE_TYPE = new RecipeType<>(
      ResourceLocation.fromNamespaceAndPath("multihooker", "crate_loot"), CrateLootData.Tier.class
   );

   private static final int COLUMNS = 4;
   private static final int SLOT = 18;
   private static final int GRID_X = 24;
   private static final int GRID_Y = 4;
   private static final int WIDTH = GRID_X + COLUMNS * SLOT + 4;
   /** Rows for the crate's own loot: the biggest tier has 12 entries, 4 across = 3 rows. */
   private static final int LOOT_ROWS = 3;
   /** Rows for the bonus pool: the biggest has 9 entries, 4 across = 3 rows. */
   private static final int BONUS_ROWS = 3;
   /** Blank band between the two grids, holding the divider and the "Bonus roll" label. */
   private static final int BONUS_GAP = 20;
   private static final int BONUS_Y = GRID_Y + LOOT_ROWS * SLOT + BONUS_GAP;
   private static final int HEIGHT = BONUS_Y + BONUS_ROWS * SLOT + 4;
   /** Greys for the divider between the own-loot grid and the bonus grid. */
   private static final int DIVIDER = 0xFF5A5A5A;
   private static final int BONUS_LABEL = 0xFFFFAA00;
   /** JEI caps a category tab icon at 16x16. */
   private static final int ICON = 16;

   private final IDrawable icon;
   /**
    * The crate whose page is on screen right now. JEI draws this page's icon on
    * the category tab (top-left of the recipe window). The icon below is a live
    * drawable that reads this field, so the tab always shows the crate the player
    * opened - whether JEI asks for the icon once or every frame.
    */
   private volatile CrateLootData.Tier viewed;

   public CrateLootRecipeCategory(IGuiHelper guiHelper) {
      this.icon = new IDrawable() {
         public int getWidth() {
            return ICON;
         }

         public int getHeight() {
            return ICON;
         }

         public void draw(GuiGraphics guiGraphics, int xOffset, int yOffset) {
            CrateLootData.Tier tier = viewed;
            ItemStack stack = tier != null ? tier.crateStack() : new ItemStack(MultiHookerRegistry.COMMON_CRATE_ITEM.get());
            if (!stack.isEmpty()) {
               guiHelper.createDrawableItemStack(stack).draw(guiGraphics, xOffset, yOffset);
            }
         }
      };
   }

   public RecipeType<CrateLootData.Tier> getRecipeType() {
      return RECIPE_TYPE;
   }

   public Component getTitle() {
      return Component.translatable("jei.multihooker.crate_loot.title");
   }

   public IDrawable getIcon() {
      return this.icon;
   }

   /**
    * JEI hands us the recipe it is drawing each frame, which is how the tab icon
    * knows which crate the player is looking at. This is also where the divider
    * and the "Bonus roll" label are drawn, above the bonus grid.
    */
   @Override
   public void draw(CrateLootData.Tier recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
      this.viewed = recipe;
      if (recipe == null || recipe.bonusEntries.isEmpty()) {
         return;
      }

      Font font = Minecraft.getInstance().font;
      int dividerY = GRID_Y + LOOT_ROWS * SLOT + 3;
      guiGraphics.fill(GRID_X, dividerY, WIDTH - 4, dividerY + 1, DIVIDER);
      guiGraphics.drawString(font, Component.translatable("jei.multihooker.crate_loot.bonus"), GRID_X, dividerY + 4, BONUS_LABEL, false);
   }

   public int getWidth() {
      return WIDTH;
   }

   public int getHeight() {
      return HEIGHT;
   }

   public void setRecipe(IRecipeLayoutBuilder builder, CrateLootData.Tier tier, IFocusGroup focuses) {
      ItemStack crate = tier.crateStack();
      if (!crate.isEmpty()) {
         // Registered as an OUTPUT on purpose: JEI's "R" (show recipes) only looks at
         // OUTPUT slots, so this is what makes pressing R on a crate find this page.
         // The standard slot background keeps it looking like a plain slot.
         builder.addOutputSlot(2, GRID_Y)
            .setStandardSlotBackground()
            .addItemStack(crate)
            .addRichTooltipCallback(
               (view, tooltip) -> {
                  tooltip.add(
                     Component.translatable(
                        "jei.multihooker.crate_loot.catch", CrateLootData.formatPercent(CrateLootData.chanceFor(tier.key))
                     )
                  );
                  tooltip.add(Component.translatable("jei.multihooker.crate_loot.rolls", tier.rollMin, tier.rollMax));
                  tooltip.add(Component.translatable("jei.multihooker.crate_loot.how_to_open"));
               }
            );
      }

      builder.addInputSlot(2, GRID_Y + SLOT + 2)
         .setStandardSlotBackground()
         .addItemStack(new ItemStack(MultiHookerRegistry.AQUAKEY.get()))
         .addRichTooltipCallback(
            (view, tooltip) -> tooltip.add(Component.translatable("jei.multihooker.crate_loot.key_use"))
         );

      // The crate's own loot, then its cross-mod bonus roll - two grids, two pools.
      addEntries(builder, GRID_X, GRID_Y, tier.entries, tier.totalWeight());
      addEntries(builder, GRID_X, BONUS_Y, tier.bonusEntries, tier.bonusTotalWeight());
   }

   /**
    * Puts one 4-across grid of loot slots at ({@code gridX}, {@code gridY}), odds
    * worked out against the given pool's total weight.
    */
   private static void addEntries(
      IRecipeLayoutBuilder builder, int gridX, int gridY, List<CrateLootData.Entry> entries, int totalWeight
   ) {
      int index = 0;

      for (CrateLootData.Entry entry : entries) {
         List<ItemStack> stacks = entry.stacks();
         if (stacks.isEmpty()) {
            continue;
         }

         final String amount = entry.min == entry.max ? String.valueOf(entry.min) : entry.min + "-" + entry.max;
         final String percent = entry.chancePercent(totalWeight) + "%";
         final String tag = entry.isTag() ? entry.id : null;
         final String name = entry.name;
         // The ordinary 18x18 slot everywhere. setOutputSlotBackground() looks tempting
         // but JEI's output sprite is 26x26 and is drawn with a (-5,-5) offset: on an
         // 18px pitch every square overlapped its neighbours and the item sat off-centre
         // in it. Role (OUTPUT) and background are independent, so this stays OUTPUT.
         builder.addOutputSlot(gridX + index % COLUMNS * SLOT, gridY + index / COLUMNS * SLOT)
            .setStandardSlotBackground()
            .addItemStacks(stacks)
            .addRichTooltipCallback(
               (view, tooltip) -> {
                  tooltip.add(Component.translatable("jei.multihooker.crate_loot.amount", amount));
                  tooltip.add(Component.translatable("jei.multihooker.crate_loot.per_roll", percent));
                  if (name != null && !name.isEmpty()) {
                     tooltip.add(Component.literal(name));
                  } else if (tag != null) {
                     tooltip.add(Component.translatable("jei.multihooker.crate_loot.any_tag", tag));
                  }
               }
            );
         index++;
      }
   }
}
