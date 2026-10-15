package com.example.multihooker;

import java.util.Locale;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * The crate loot never contains an instruction book.
 *
 * <p>Guidebooks - Patchouli manuals and the like, such as Adorable Hamster Pets'
 * "Hamster Tips" - are things a player is meant to be handed (or craft), not
 * loot. Because the crates' cross-mod bonus roll pulls whatever other installed
 * mods drop into the broad {@code c:} conventional tags, a mod could slip its
 * own guide into a crate that way. This is the one place that says "not that",
 * applied to both the real roll (see {@code CrateBlock}) and the two browsers
 * (see {@code CrateLootData}, which feeds the Crate Loot screen and the JEI
 * page), so the three can never disagree about what a crate can give.
 *
 * <p>A book is recognised in two ways, neither of which needs a compile-time
 * dependency on Patchouli or on any particular mod:
 * <ul>
 *   <li>it carries Patchouli's {@code patchouli:book} data component - the
 *       canonical marker for a Patchouli guide; or</li>
 *   <li>its item id looks like a guide/manual, for books that only get their
 *       component at craft time (the hamster guidebook is one of these).</li>
 * </ul>
 */
public final class CrateLootFilter {
   /** Patchouli marks every guide with this data component. */
   private static final ResourceLocation PATCHOULI_BOOK = ResourceLocation.fromNamespaceAndPath("patchouli", "book");

   /**
    * Id fragments that give away an instruction book when the component is not
    * present. Deliberately narrow: "enchanted_book" (real crate loot) and
    * spellbooks/tomes (real loot from other mods) must not match.
    */
   private static final String[] INSTRUCTION_BOOK_MARKERS = {"guide", "manual", "handbook", "instruction", "knowledge_book"};

   private static boolean patchouliBookLookedUp;
   private static DataComponentType<?> patchouliBook;

   private CrateLootFilter() {
   }

   /** True for guide/instruction books, which a crate must never give or show. */
   public static boolean isInstructionBook(ItemStack stack) {
      if (stack == null || stack.isEmpty()) {
         return false;
      }

      DataComponentType<?> component = patchouliBookType();
      if (component != null && hasComponent(stack, component)) {
         return true;
      }

      ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
      if (id == null) {
         return false;
      }

      String path = id.getPath().toLowerCase(Locale.ROOT);

      for (String marker : INSTRUCTION_BOOK_MARKERS) {
         if (path.contains(marker)) {
            return true;
         }
      }

      return false;
   }

   /**
    * Patchouli's component, or {@code null} when Patchouli is not installed. Found
    * by id so this class compiles (and runs) with or without the mod.
    */
   private static DataComponentType<?> patchouliBookType() {
      if (!patchouliBookLookedUp) {
         patchouliBookLookedUp = true;
         patchouliBook = BuiltInRegistries.DATA_COMPONENT_TYPE.get(PATCHOULI_BOOK);
      }

      return patchouliBook;
   }

   @SuppressWarnings({"rawtypes", "unchecked"})
   private static boolean hasComponent(ItemStack stack, DataComponentType<?> component) {
      return stack.has((DataComponentType)component);
   }
}
