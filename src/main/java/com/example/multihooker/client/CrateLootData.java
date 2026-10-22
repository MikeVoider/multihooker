package com.example.multihooker.client;

import com.example.multihooker.Config;
import com.example.multihooker.CrateLootFilter;
import com.example.multihooker.MultiHookerMod;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import com.example.multihooker.treasure.CrateBonusLoot;
import net.neoforged.neoforge.common.ModConfigSpec.DoubleValue;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * The crate loot table, as read by the client: every crate tier, the amount of
 * loot rolls it gets, and each entry (item, amount range and weight) it can give.
 *
 * <p>The data comes from assets/multihooker/crate_loot.json, which is generated
 * by tools/crategen/gen_resources.py from the same table as the real loot files,
 * so neither the Crate Loot screen nor the JEI page can drift from what the
 * crates actually give.
 */
public final class CrateLootData {
   public static final ResourceLocation DATA_ID = ResourceLocation.fromNamespaceAndPath("multihooker", "crate_loot.json");
   private static final String JAR_PATH = "/assets/multihooker/crate_loot.json";

   private static List<Tier> cache;

   private CrateLootData() {
   }

   /** The tiers as the running client sees them (honours resource packs). Cached. */
   public static synchronized List<Tier> tiers() {
      if (cache == null) {
         cache = fromResourceManager();
         if (cache.isEmpty()) {
            cache = tiersFromJar();
         }
      }

      return cache;
   }

   /** The tier with this key, or {@code null} if the data file has no such tier. */
   public static Tier byKey(String key) {
      for (Tier tier : tiers()) {
         if (tier.key.equals(key)) {
            return tier;
         }
      }

      return null;
   }

   /**
    * The tiers exactly as they ship inside the mod jar. Unlike {@link #tiers()} this
    * needs neither a world nor the client, which is what the JEI plugin starts with.
    */
   public static List<Tier> tiersFromJar() {
      try (InputStream in = CrateLootData.class.getResourceAsStream(JAR_PATH)) {
         if (in == null) {
            MultiHookerMod.LOGGER.warn("[MultiHooker] {} is missing from the mod jar", JAR_PATH);
            return List.of();
         }

         return parse(new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8)));
      } catch (Exception exception) {
         MultiHookerMod.LOGGER.warn("[MultiHooker] failed to read {}", JAR_PATH, exception);
         return List.of();
      }
   }

   private static List<Tier> fromResourceManager() {
      try {
         Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(DATA_ID);
         if (resource.isEmpty()) {
            MultiHookerMod.LOGGER.warn("[MultiHooker] {} is missing - the Crate Loot screen will be empty", DATA_ID);
            return List.of();
         }

         try (BufferedReader reader = resource.get().openAsReader()) {
            return parse(reader);
         }
      } catch (Exception exception) {
         MultiHookerMod.LOGGER.warn("[MultiHooker] failed to read {}", DATA_ID, exception);
         return List.of();
      }
   }

   private static List<Tier> parse(BufferedReader reader) {
      List<Tier> out = new ArrayList<>();
      JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();

      for (JsonElement element : root.getAsJsonArray("tiers")) {
         JsonObject obj = element.getAsJsonObject();
         Tier tier = new Tier(obj.get("key").getAsString(), obj.get("group").getAsString());
         tier.rollMin = obj.getAsJsonArray("rolls").get(0).getAsInt();
         tier.rollMax = obj.getAsJsonArray("rolls").get(1).getAsInt();

         for (JsonElement entryElement : obj.getAsJsonArray("entries")) {
            JsonObject entry = entryElement.getAsJsonObject();
            int min = entry.has("min") ? entry.get("min").getAsInt() : 1;
            int max = entry.has("max") ? entry.get("max").getAsInt() : min;
            tier.entries.add(new Entry(entry.get("kind").getAsString(), entry.get("id").getAsString(), entry.get("weight").getAsInt(), min, max));
         }

         // The cross-mod bonus roll is a separate pool of its own, so it is listed
         // separately: its weights mean nothing next to the main pool's.
         if (obj.has("bonus_entries")) {
            for (JsonElement entryElement : obj.getAsJsonArray("bonus_entries")) {
               JsonObject entry = entryElement.getAsJsonObject();
               String entryName = entry.has("name") ? entry.get("name").getAsString() : null;
               tier.bonusEntries.add(new Entry(entry.get("kind").getAsString(), entry.get("id").getAsString(), entry.get("weight").getAsInt(), 1, 1, entryName));
            }
         }

         // The book stack is a THIRD pool: it has its own low trigger chance and
         // rolls several books at once, so neither its odds nor its amounts can
         // be folded in with the main pool's - it is listed on its own.
         if (obj.has("book_entries")) {
            for (JsonElement entryElement : obj.getAsJsonArray("book_entries")) {
               JsonObject entry = entryElement.getAsJsonObject();
               int min = entry.has("min") ? entry.get("min").getAsInt() : 1;
               int max = entry.has("max") ? entry.get("max").getAsInt() : 1;
               // books are rolled in code from the config, so the screen reads the
               // LIVE number rather than whatever was generated
               DoubleValue cfg = CrateBonusLoot.bookChance(tier.key);
               double chance = cfg == null ? 0.0 : (Double)cfg.get();
               tier.bookEntries.add(new Entry(
                  entry.get("kind").getAsString(),
                  entry.get("id").getAsString(),
                  entry.get("weight").getAsInt(),
                  min,
                  max,
                  null,
                  chance
               ));
            }
         }

         // The relic pool is a fourth pool: a brutal 0.2% trigger on the Curios
         // slot tags, so it is listed on its own rather than as ordinary loot.
         if (obj.has("relic_entries")) {
            for (JsonElement entryElement : obj.getAsJsonArray("relic_entries")) {
               JsonObject entry = entryElement.getAsJsonObject();
               // likewise read live from the config
               double chance = (Double)Config.RELIC_CHANCE.get();
               tier.relicEntries.add(new Entry(
                  entry.get("kind").getAsString(),
                  entry.get("id").getAsString(),
                  entry.get("weight").getAsInt(),
                  1,
                  1,
                  null,
                  chance
               ));
            }
         }

         out.add(tier);
      }

      return out;
   }

   /** That tier's current catch chance, straight from the config. */
   public static double chanceFor(String key) {
      return switch (key) {
         case "common" -> (Double)Config.COMMON_CRATE_CHANCE.get();
         case "uncommon" -> (Double)Config.UNCOMMON_CRATE_CHANCE.get();
         case "rare" -> (Double)Config.RARE_CRATE_CHANCE.get();
         case "epic" -> (Double)Config.EPIC_CRATE_CHANCE.get();
         case "legendary" -> (Double)Config.LEGENDARY_CRATE_CHANCE.get();
         case "godly" -> (Double)Config.GODLY_CRATE_CHANCE.get();
         case "secret" -> (Double)Config.SECRET_CRATE_CHANCE.get();
         case "divine" -> (Double)Config.DIVINE_CRATE_CHANCE.get();
         case "ultra_divine" -> (Double)Config.ULTRA_DIVINE_CRATE_CHANCE.get();
         case "unspoken" -> (Double)Config.UNSPOKEN_CRATE_CHANCE.get();
          case "ghost_unspoken" -> (Double)Config.GHOST_UNSPOKEN_CRATE_CHANCE.get();
          case "mythic" -> (Double)Config.MYTHIC_CRATE_CHANCE.get();
         default -> 0.0;
      };
   }

   public static String formatPercent(double fraction) {
      double percent = fraction * 100.0;
      return Math.abs(percent - Math.round(percent)) < 0.05
         ? Math.round(percent) + "%"
         : String.format(Locale.ROOT, "%.1f%%", percent);
   }

   public static ItemStack stackOf(String path) {
      ResourceLocation id = ResourceLocation.tryParse(path);
      if (id == null) {
         return ItemStack.EMPTY;
      }

      Item item = BuiltInRegistries.ITEM.get(id);
      return item == null || item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
   }

   public static final class Tier {
      public final String key;
      public final String group;
      public final List<Entry> entries = new ArrayList<>();
      /** The bonus roll's pool: one item from any installed mod. */
      public final List<Entry> bonusEntries = new ArrayList<>();
      /** The separate, rarely-triggering book stack pool. */
      public final List<Entry> bookEntries = new ArrayList<>();
      /** The very rare Curios relic/trinket pool. */
      public final List<Entry> relicEntries = new ArrayList<>();
      public int rollMin = 1;
      public int rollMax = 1;

      Tier(String key, String group) {
         this.key = key;
         this.group = group;
      }

      public ItemStack crateStack() {
         return stackOf("multihooker:" + this.key + "_crate");
      }

      public int totalWeight() {
         int total = 0;

         for (Entry entry : this.entries) {
            total += entry.weight;
         }

         return total;
      }

      public int bonusTotalWeight() {
         int total = 0;

         for (Entry entry : this.bonusEntries) {
            total += entry.weight;
         }

         return total;
      }
   }

   public static final class Entry {
      public final String kind;
      public final String id;
      public final int weight;
      public final int min;
      public final int max;
      /** Optional display name (used for named compat entries); may be null. */
      public final String name;
      /**
       * For the book pool: the chance the pool triggers at all, so the screen can
       * state it honestly. 0 for every ordinary entry, whose odds come from its
       * weight in the main pool instead.
       */
      public final double chance;
      private List<ItemStack> tagCache;

      Entry(String kind, String id, int weight, int min, int max) {
         this(kind, id, weight, min, max, null);
      }

      Entry(String kind, String id, int weight, int min, int max, String name) {
         this(kind, id, weight, min, max, name, 0.0);
      }

      Entry(String kind, String id, int weight, int min, int max, String name, double chance) {
         this.kind = kind;
         this.id = id;
         this.weight = weight;
         this.min = min;
         this.max = max;
         this.name = name;
         this.chance = chance;
      }

      public boolean isTag() {
         return "tag".equals(this.kind);
      }

      /** A single stack for this entry, used to draw it in a list. Tags have no icon. */
      public ItemStack stack() {
         return this.isTag() ? ItemStack.EMPTY : stackOf(this.id);
      }

      /** This entry as something we can render: a plain item, or every item of the tag. */
      public List<ItemStack> stacks() {
         if (!this.isTag()) {
            ItemStack stack = stackOf(this.id);
            return stack.isEmpty() || CrateLootFilter.isInstructionBook(stack) ? List.of() : List.of(stack);
         }

         // Resolved once (and kept only when something was found): the screen asks
         // for this every frame, and tags only change on a resource reload.
         if (this.tagCache == null || this.tagCache.isEmpty()) {
            this.tagCache = tagStacks(this.id);
         }

         return this.tagCache;
      }

      public String label() {
         String label;
         if (this.name != null && !this.name.isEmpty()) {
            label = this.name;
         } else if (this.isTag()) {
            label = "Any " + tagFriendlyName(this.id);
         } else {
            ItemStack stack = stackOf(this.id);
            label = stack.isEmpty() ? this.id : stack.getHoverName().getString();
         }

         if (this.min <= 1 && this.max <= 1) {
            return label;
         }

         return this.min == this.max ? label + " x" + this.min : label + " " + this.min + "-" + this.max;
      }

      /** "c:gems" or "c:ingots" as a readable word: "Gems", "Ingots". */
      private static String tagFriendlyName(String tagId) {
         String path = tagId;
         int slash = path.lastIndexOf('/');
         if (slash >= 0) {
            path = path.substring(slash + 1);
         }

         StringBuilder out = new StringBuilder();

         for (String word : path.replace('_', ' ').split(" ")) {
            if (word.isEmpty()) {
               continue;
            }

            if (out.length() > 0) {
               out.append(' ');
            }

            out.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
         }

         return out.length() == 0 ? tagId : out.toString();
      }

      /** The odds of this entry coming up on a single roll, as a whole percentage. */
      public int chancePercent(int totalWeight) {
         // A conditional pool (the book stack) knows its own trigger chance;
         // its weight only says what it hands out, not how often.
         if (this.chance > 0.0) {
            return (int)Math.round(this.chance * 100.0);
         }
         return totalWeight <= 0 ? 0 : (int)Math.round(100.0 * this.weight / totalWeight);
      }
   }

   private static List<ItemStack> tagStacks(String tagId) {
      ResourceLocation id = ResourceLocation.tryParse(tagId);
      if (id == null) {
         return List.of();
      }

      Optional<HolderSet.Named<Item>> tag = BuiltInRegistries.ITEM.getTag(TagKey.create(Registries.ITEM, id));
      if (tag.isEmpty()) {
         return List.of();
      }

      List<ItemStack> out = new ArrayList<>();

      for (Holder<Item> holder : tag.get()) {
         Item item = holder.value();
         if (item == Items.AIR) {
            continue;
         }

         // Instruction books (guidebooks/manuals) are never crate loot, even when
         // another mod has tagged one into the conventional tag we are expanding.
         ItemStack stack = new ItemStack(item);
         if (!CrateLootFilter.isInstructionBook(stack)) {
            out.add(stack);
         }
      }

      return out;
   }
}
