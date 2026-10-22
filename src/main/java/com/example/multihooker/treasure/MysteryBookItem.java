package com.example.multihooker.treasure;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;

/**
 * Mystery Enchantment Book: right-click to reveal a random enchanted book
 * matching the tier's roll range. The rolled book is a plain vanilla
 * Enchanted Book, so it works with every mod and anvil behaviour is vanilla.
 *
 * Tiers only control which enchantments can be rolled and how strong they can
 * be. The registry is read at runtime, so enchantments added by other mods are
 * automatically included.
 */
public class MysteryBookItem extends Item {
   public enum Tier {
      COMMON(1, 1, 1, 2, false, true, false, ChatFormatting.GRAY),
      RARE(1, 2, 2, 3, false, true, false, ChatFormatting.AQUA),
      EPIC(1, 2, 3, 4, true, true, false, ChatFormatting.LIGHT_PURPLE),
      LEGENDARY(2, 2, 4, 5, true, true, false, ChatFormatting.GOLD),
      MYTHICAL(2, 3, 1, 6, true, true, true, ChatFormatting.RED);

      /**
       * The colour this tier's name and its glow share, so a book looks the same
       * whether you are reading its tooltip or watching the light sweep across it.
       * These are the standard rarity colours players already know from enchanted
       * items, which makes the tier readable without needing the name.
       */
      public final ChatFormatting colour;

      public final int minEnchants;
      public final int maxEnchants;
      public final int minLevel;
      public final int maxLevel;
      /** whether treasure-only enchantments (Mending, Frost Walker...) may be rolled */
      public final boolean allowTreasure;
      /** whether curses may be rolled */
      public final boolean allowCurses;
      /** whether every enchantment is rolled at its own maximum level */
      public final boolean maxLevelEnchants;

      Tier(int minEnchants, int maxEnchants, int minLevel, int maxLevel,
           boolean allowTreasure, boolean allowCurses, boolean maxLevelEnchants,
           ChatFormatting colour) {
         this.minEnchants = minEnchants;
         this.colour = colour;
         this.maxEnchants = maxEnchants;
         this.minLevel = minLevel;
         this.maxLevel = maxLevel;
         this.allowTreasure = allowTreasure;
         this.allowCurses = allowCurses;
         this.maxLevelEnchants = maxLevelEnchants;
      }
   }

   private final Tier tier;

   public MysteryBookItem(Tier tier) {
      super(new Properties().stacksTo(16));
      this.tier = tier;
   }

   /** The roll rules for this book, used by the JEI info page so it cannot drift. */
   public Tier getTier() {
      return this.tier;
   }

   @Override
   public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
      ItemStack stack = player.getItemInHand(hand);
      if (level instanceof ServerLevel serverLevel) {
         ItemStack book = rollBook(serverLevel, player.getRandom());

         serverLevel.playSound(null, player.blockPosition(),
            SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.8F, 1.2F);

         ItemEntity entity = new ItemEntity(serverLevel,
            player.getX(), player.getY() + 1.0, player.getZ(), book);
         entity.setDefaultPickUpDelay();
         serverLevel.addFreshEntity(entity);

         if (!player.hasInfiniteMaterials()) {
            stack.shrink(1);
         }
         player.swing(hand);
      }
      return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
   }

   private ItemStack rollBook(ServerLevel level, RandomSource random) {
      var registry = level.registryAccess().registryOrThrow(Registries.ENCHANTMENT);

      List<Holder.Reference<Enchantment>> pool = registry.holders()
         .filter(holder -> isRollable(holder))
         .collect(java.util.stream.Collectors.toCollection(ArrayList::new));

      Map<Holder<Enchantment>, Integer> rolled = new LinkedHashMap<>();
      int count = Mth.nextInt(random, tier.minEnchants, tier.maxEnchants);
      while (rolled.size() < count && !pool.isEmpty()) {
         Holder.Reference<Enchantment> holder = pool.remove(random.nextInt(pool.size()));
         rolled.put(holder, rollLevel(holder.value(), random, holder.is(EnchantmentTags.CURSE)));
      }

      addGuaranteedExtras(registry, rolled, random);

      if (rolled.isEmpty()) {
         // extremely defensive fallback: vanilla Unbreaking I
         registry.getHolder(ResourceKey.create(Registries.ENCHANTMENT,
            ResourceLocation.withDefaultNamespace("unbreaking"))).ifPresent(h -> rolled.put(h, 1));
      }

      ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
      ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
      rolled.forEach(mutable::set);
      book.set(DataComponents.STORED_ENCHANTMENTS, mutable.toImmutable());
      return book;
   }

   private boolean isRollable(Holder.Reference<Enchantment> holder) {
      if (holder.is(EnchantmentTags.CURSE)) {
         // curses are rollable on every tier; their level is rolled in rollLevel()
         // and clamped to their own maximum, so a level-1 curse still fits a low tier.
         return tier.allowCurses;
      }
      if (!tier.allowTreasure && holder.is(EnchantmentTags.TREASURE)) {
         return false;
      }
      if (tier.maxLevelEnchants) {
         return true;
      }
      return Math.min(tier.maxLevel, holder.value().getMaxLevel()) >= tier.minLevel;
   }

   /**
    * Curses always start at level 1 and scale with the tier (Common = level 1,
    * higher tiers reach higher levels) but never past the curse's own maximum.
    * Regular enchantments keep the tier's normal level range.
    */
   private int rollLevel(Enchantment enchantment, RandomSource random, boolean curse) {
      if (tier.maxLevelEnchants) {
         return enchantment.getMaxLevel();
      }
      int min = curse ? 1 : tier.minLevel;
      int max = Math.max(min, Math.min(tier.maxLevel, enchantment.getMaxLevel()));
      return Mth.nextInt(random, min, max);
   }

   private void addGuaranteedExtras(net.minecraft.core.Registry<Enchantment> registry,
                                    Map<Holder<Enchantment>, Integer> rolled,
                                    RandomSource random) {
      switch (tier) {
         case EPIC -> {
            if (random.nextFloat() < 0.15F) {
               addByName(registry, rolled, "mending", 1);
            }
         }
         case LEGENDARY -> {
            if (random.nextBoolean()) {
               addByName(registry, rolled, "mending", 1);
            } else {
               addByName(registry, rolled, "unbreaking", 3);
            }
         }
         case MYTHICAL -> {
            if (random.nextFloat() < 0.15F) {
               addByName(registry, rolled, "mending", 1);
            }
            if (random.nextFloat() < 0.10F) {
               addByName(registry, rolled, "swift_sneak", 3);
            }
            if (random.nextFloat() < 0.10F) {
               addByName(registry, rolled, "quick_charge", 3);
            }
         }
         default -> { }
      }
   }

   private static void addByName(net.minecraft.core.Registry<Enchantment> registry,
                                 Map<Holder<Enchantment>, Integer> rolled,
                                 String path, int level) {
      registry.getHolder(ResourceKey.create(Registries.ENCHANTMENT,
            ResourceLocation.withDefaultNamespace(path)))
         .ifPresent(holder -> rolled.putIfAbsent(holder, Math.min(level, holder.value().getMaxLevel())));
   }

   /**
    * The book's name is tinted with its tier colour, so a stack in a chest is
    * identifiable at a glance by the same colour the glow uses on the item
    * itself. Without this the name would be plain white for every tier.
    */
   @Override
   public Component getName(ItemStack stack) {
      return super.getName(stack).copy().withStyle(tier.colour);
   }

   @Override
   public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
      super.appendHoverText(stack, context, tooltip, flag);
      tooltip.add(Component.translatable("tooltip.multihooker.mystery_book",
         tier.minEnchants, tier.maxEnchants));
   }
}
