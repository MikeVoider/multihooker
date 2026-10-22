package com.example.multihooker.treasure;

import com.example.multihooker.Config;
import com.example.multihooker.MultiHookerRegistry;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.ModConfigSpec.DoubleValue;

/**
 * The two "special" things a crate can hand out: a stack of Mystery Books, and a
 * Curios relic. Both are rolled HERE rather than in the crate's loot table,
 * because both of their rates are config options and a loot table's
 * {@code random_chance} is baked into the JSON where a config value cannot reach
 * it.
 *
 * <p>Both are separate from the crate's own table so they cannot be diluted by
 * the ordinary loot: a book stack is all-or-nothing and of a single tier, and a
 * relic is its own very rare roll. Whatever they produce goes into the same
 * container as the rest of the loot, so it merges with matching stacks like
 * everything else does.
 */
public final class CrateBonusLoot {
   /** How many books a triggered stack holds. A book stacks to 16, so one slot. */
   private static final int BOOK_MIN = 3;
   private static final int BOOK_MAX = 7;

   /** The Curios slots a relic can come from. Every relic mod registers into these. */
   private static final String[] RELIC_SLOT_TAGS = {
      "curios:necklace", "curios:ring", "curios:charm", "curios:belt", "curios:head",
      "curios:back", "curios:body", "curios:hands", "curios:bracelet", "curios:feet",
      "curios:curio"
   };

   /** Cached relic list; only tags change it, and only on a resource reload. */
   private static final AtomicReference<List<Item>> RELICS = new AtomicReference<>();

   private CrateBonusLoot() {
   }

   /**
    * Rolls this crate's book stack and, separately, its relic.
    *
    * @param crateKey the crate's tier key, e.g. {@code "ghost_unspoken"}
    * @param random   the crate's randomness
    * @param sink     receives whatever was rolled
    */
   public static void rollInto(String crateKey, RandomSource random, Sink sink) {
      ItemStack book = rollBookStack(crateKey, random);
      if (!book.isEmpty()) {
         sink.add(book);
      }

      // The three drinks are rolled separately rather than picked from one shared roll, because
      // each can appear in a different set of tiers. A single weighted pick could not express
      // "Health from Rare up, Fortune from Legendary up, Crate Potion only in the top two".
      rollPotion(crateKey, HEALTH_TIERS, (Double)Config.ELIXIR_VITALITY_CHANCE.get(),
         MultiHookerRegistry.ELIXIR_VITALITY.get(), random, sink);
      rollPotion(crateKey, FORTUNE_TIERS, (Double)Config.ELIXIR_FORTUNE_CHANCE.get(),
         MultiHookerRegistry.ELIXIR_FORTUNE.get(), random, sink);
      rollPotion(crateKey, CRATE_POTION_TIERS, (Double)Config.CRATE_POTION_CHANCE.get(),
         MultiHookerRegistry.CRATE_POTION.get(), random, sink);

      ItemStack relic = rollRelic(random);
      if (!relic.isEmpty()) {
         sink.add(relic);
      }
   }

   /**
    * One chance-roll for one drink, if this crate is a tier that carries it.
    *
    * <p>Rolled here rather than in the crate's loot table for the same reason books and relics
    * are: the rate is a config option, and a loot table's {@code random_chance} is frozen into the
    * JSON where a config value cannot reach it.
    *
    * <p>Each drink is all-or-nothing and lands in the same container as the crate's ordinary
    * loot, so it merges with matching stacks like everything else. Because the three are rolled
    * independently, a single high-tier crate can in principle yield more than one drink.
    */
   private static void rollPotion(String crateKey, Set<String> tiers, double chance,
                                  Item item, RandomSource random, Sink sink) {
      if (!tiers.contains(crateKey) || random.nextDouble() >= chance) {
         return;
      }
      sink.add(new ItemStack(item));
   }

   /**
    * Potion of Health - the most generous of the three, so it starts at Rare.
    */
   private static final Set<String> HEALTH_TIERS = Set.of(
      "rare", "epic", "legendary", "godly", "mythic", "secret",
      "divine", "ultra_divine", "unspoken", "ghost_unspoken"
   );

   /** Elixir of Fortune - starts at Legendary. */
   private static final Set<String> FORTUNE_TIERS = Set.of(
      "legendary", "godly", "mythic", "secret",
      "divine", "ultra_divine", "unspoken", "ghost_unspoken"
   );

   /** Crate Potion - the two rarest troves only. */
   private static final Set<String> CRATE_POTION_TIERS = Set.of("unspoken", "ghost_unspoken");

   /** The book stack this crate can give, or empty if the roll missed or there is no book tier. */
   public static ItemStack rollBookStack(String crateKey, RandomSource random) {
      DoubleValue chance = bookChance(crateKey);
      if (chance == null || random.nextDouble() >= (Double)chance.get()) {
         return ItemStack.EMPTY;
      }

      Item book = bookItem(crateKey);
      if (book == null) {
         return ItemStack.EMPTY;
      }

      return new ItemStack(book, BOOK_MIN + random.nextInt(BOOK_MAX - BOOK_MIN + 1));
   }

   /** One Curios relic, or empty when the very rare roll misses (or Curios has nothing). */
   public static ItemStack rollRelic(RandomSource random) {
      if (random.nextDouble() >= (Double)Config.RELIC_CHANCE.get()) {
         return ItemStack.EMPTY;
      }

      List<Item> relics = allRelics();
      if (relics.isEmpty()) {
         return ItemStack.EMPTY;
      }

      return new ItemStack(relics.get(random.nextInt(relics.size())));
   }

   /** Every item in the Curios slot tags, deduplicated and cached. */
   public static List<Item> allRelics() {
      List<Item> cached = RELICS.get();
      if (cached == null) {
         cached = collectRelics();
         RELICS.set(cached);
      }
      return cached;
   }

   /** Drops the cached list, so a resource reload picks up newly installed relics. */
   public static void invalidateRelics() {
      RELICS.set(null);
   }

   private static List<Item> collectRelics() {
      List<Item> out = new ArrayList<>();
      for (String slot : RELIC_SLOT_TAGS) {
         ResourceLocation id = ResourceLocation.tryParse(slot);
         if (id == null) {
            continue;
         }
         Optional<HolderSet.Named<Item>> set = BuiltInRegistries.ITEM.getTag(TagKey.create(Registries.ITEM, id));
         if (set.isEmpty()) {
            continue;
         }
         for (Holder<Item> holder : set.get()) {
            Item item = holder.value();
            if (item != Items.AIR && !out.contains(item)) {
               out.add(item);
            }
         }
      }
      return List.copyOf(out);
   }

   /** The config entry for this crate's book chance, or null if the crate is unknown. */
   public static DoubleValue bookChance(String crateKey) {
      int index = indexOf(crateKey);
      return index < 0 ? null : BOOK_CHANCES[index];
   }

   /** The book tier this crate hands out: only the four special crates reach Mythical. */
   public static Item bookItem(String crateKey) {
      int index = indexOf(crateKey);
      return index < 0 ? null : bookOf(BOOK_TIERS[index]);
   }

   private static Item bookOf(String tier) {
      return switch (tier) {
         case "common" -> MultiHookerRegistry.MYSTERY_BOOK_COMMON.get();
         case "rare" -> MultiHookerRegistry.MYSTERY_BOOK_RARE.get();
         case "epic" -> MultiHookerRegistry.MYSTERY_BOOK_EPIC.get();
         case "legendary" -> MultiHookerRegistry.MYSTERY_BOOK_LEGENDARY.get();
         case "mythical" -> MultiHookerRegistry.MYSTERY_BOOK_MYTHICAL.get();
         default -> null;
      };
   }

   private static int indexOf(String crateKey) {
      for (int i = 0; i < CRATE_KEYS.length; i++) {
         if (CRATE_KEYS[i].equals(crateKey)) {
            return i;
         }
      }
      return -1;
   }

   // ---- the table, in the same crate order as the config ----
   public static final String[] CRATE_KEYS = {
      "common", "uncommon", "rare", "epic", "legendary", "godly", "mythic", "secret",
      "divine", "ultra_divine", "unspoken", "ghost_unspoken"
   };

   private static final String[] BOOK_TIERS = {
      "common", "rare", "rare", "epic", "legendary", "legendary", "legendary", "legendary",
      "mythical", "mythical", "mythical", "mythical"
   };

   private static final DoubleValue[] BOOK_CHANCES = {
      Config.BOOK_CHANCE_COMMON, Config.BOOK_CHANCE_UNCOMMON, Config.BOOK_CHANCE_RARE,
      Config.BOOK_CHANCE_EPIC, Config.BOOK_CHANCE_LEGENDARY, Config.BOOK_CHANCE_GODLY,
      Config.BOOK_CHANCE_MYTHIC, Config.BOOK_CHANCE_SECRET, Config.BOOK_CHANCE_DIVINE,
      Config.BOOK_CHANCE_ULTRA_DIVINE, Config.BOOK_CHANCE_UNSPOKEN,
      Config.BOOK_CHANCE_GHOST_UNSPOKEN
   };


   /** Where the rolled stacks go. Kept as an interface so this class stays container-agnostic. */
   public interface Sink {
      void add(ItemStack stack);
   }
}