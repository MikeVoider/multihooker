package com.example.multihooker;

import com.example.multihooker.client.TooltipClient;
import com.example.multihooker.treasure.CratePotionItem;
import com.example.multihooker.treasure.ElixirItem;
import com.example.multihooker.treasure.MysteryBookItem;
import com.mojang.serialization.Codec;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.EntityType.Builder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.storage.loot.LootTable;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.registries.DeferredRegister.Blocks;
import net.neoforged.neoforge.registries.DeferredRegister.Items;

public final class MultiHookerRegistry {
   public static final Items ITEMS = DeferredRegister.createItems("multihooker");
   public static final Blocks BLOCKS = DeferredRegister.createBlocks("multihooker");
   public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, "multihooker");
   public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, "multihooker");
   public static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(Registries.MENU, "multihooker");
   /**
    * Status effects added by MultiHooker. Crate Luck is the buff granted by the
    * Crate Potion.
    */
   public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, "multihooker");
   public static final DeferredHolder<MobEffect, MobEffect> CRATE_LUCK = EFFECTS.register("crate_luck", CrateLuckEffect::new);
   /** The hinge carried by every empty crate; see {@link CrateBlockEntity}. */
   public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(
      Registries.BLOCK_ENTITY_TYPE, "multihooker"
   );
   /**
    * The loot window a crate opens. The extra data is the crate's tier key and
    * its position, so the client can build the identical menu (and CrateScreen
    * can show that tier's odds) without needing a block entity.
    */
   public static final DeferredHolder<MenuType<?>, MenuType<CrateMenu>> CRATE_MENU = MENU_TYPES.register(
      "crate_menu",
      () -> IMenuTypeExtension.<CrateMenu>create((id, inventory, data) -> new CrateMenu(id, inventory, data))
   );
   public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, "multihooker");
   public static final DeferredHolder<EntityType<?>, EntityType<NetVisualEntity>> NET_VISUAL = ENTITY_TYPES.register(
      "net_visual", () -> Builder.of(NetVisualEntity::new, MobCategory.MISC).sized(2.0F, 0.25F).clientTrackingRange(10).updateInterval(2).build("net_visual")
   );
   public static final DeferredHolder<Item, Item> AQUAKEY = ITEMS.register("aquakey", () -> new Item(new Properties()) {
      public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
         tooltip.add(Component.translatable("item.multihooker.aquakey.tooltip").withStyle(ChatFormatting.GRAY));
      }
   });
   // Each crate and the empty crate it becomes. The two are distinct blocks so
   // that breaking an opened crate can only ever give back the empty item - there
   // is no pick-block or creative trick that turns an empty crate back into a full one.
   public static final DeferredHolder<Block, Block> COMMON_CRATE_EMPTY = BLOCKS.register("common_crate_empty", () -> new EmptyCrateBlock(crateProps()));
   public static final DeferredHolder<Item, BlockItem> COMMON_CRATE_EMPTY_ITEM = ITEMS.register("common_crate_empty", () -> emptyCrateItem(COMMON_CRATE_EMPTY));
   public static final DeferredHolder<Block, Block> COMMON_CRATE = BLOCKS.register(
      "common_crate", () -> new CrateBlock(crateProps(), crateLoot("common"), COMMON_CRATE_EMPTY)
   );
   public static final DeferredHolder<Item, BlockItem> COMMON_CRATE_ITEM = ITEMS.register("common_crate", () -> crateItem(COMMON_CRATE, false));
   public static final DeferredHolder<Block, Block> UNCOMMON_CRATE_EMPTY = BLOCKS.register("uncommon_crate_empty", () -> new EmptyCrateBlock(crateProps()));
   public static final DeferredHolder<Item, BlockItem> UNCOMMON_CRATE_EMPTY_ITEM = ITEMS.register(
      "uncommon_crate_empty", () -> emptyCrateItem(UNCOMMON_CRATE_EMPTY)
   );
   public static final DeferredHolder<Block, Block> UNCOMMON_CRATE = BLOCKS.register(
      "uncommon_crate", () -> new CrateBlock(crateProps(), crateLoot("uncommon"), UNCOMMON_CRATE_EMPTY)
   );
   public static final DeferredHolder<Item, BlockItem> UNCOMMON_CRATE_ITEM = ITEMS.register("uncommon_crate", () -> crateItem(UNCOMMON_CRATE, false));
   public static final DeferredHolder<Block, Block> RARE_CRATE_EMPTY = BLOCKS.register("rare_crate_empty", () -> new EmptyCrateBlock(crateProps()));
   public static final DeferredHolder<Item, BlockItem> RARE_CRATE_EMPTY_ITEM = ITEMS.register("rare_crate_empty", () -> emptyCrateItem(RARE_CRATE_EMPTY));
   public static final DeferredHolder<Block, Block> RARE_CRATE = BLOCKS.register(
      "rare_crate", () -> new CrateBlock(crateProps(), crateLoot("rare"), RARE_CRATE_EMPTY)
   );
   public static final DeferredHolder<Item, BlockItem> RARE_CRATE_ITEM = ITEMS.register("rare_crate", () -> crateItem(RARE_CRATE, false));
   public static final DeferredHolder<Block, Block> EPIC_CRATE_EMPTY = BLOCKS.register("epic_crate_empty", () -> new EmptyCrateBlock(crateProps()));
   public static final DeferredHolder<Item, BlockItem> EPIC_CRATE_EMPTY_ITEM = ITEMS.register("epic_crate_empty", () -> emptyCrateItem(EPIC_CRATE_EMPTY));
   public static final DeferredHolder<Block, Block> EPIC_CRATE = BLOCKS.register(
      "epic_crate", () -> new CrateBlock(crateProps(), crateLoot("epic"), EPIC_CRATE_EMPTY)
   );
   public static final DeferredHolder<Item, BlockItem> EPIC_CRATE_ITEM = ITEMS.register("epic_crate", () -> crateItem(EPIC_CRATE, false));
   public static final DeferredHolder<Block, Block> LEGENDARY_CRATE_EMPTY = BLOCKS.register(
      "legendary_crate_empty", () -> new EmptyCrateBlock(crateProps())
   );
   public static final DeferredHolder<Item, BlockItem> LEGENDARY_CRATE_EMPTY_ITEM = ITEMS.register(
      "legendary_crate_empty", () -> emptyCrateItem(LEGENDARY_CRATE_EMPTY)
   );
   public static final DeferredHolder<Block, Block> LEGENDARY_CRATE = BLOCKS.register(
      "legendary_crate", () -> new CrateBlock(crateProps(), crateLoot("legendary"), LEGENDARY_CRATE_EMPTY)
   );
   public static final DeferredHolder<Item, BlockItem> LEGENDARY_CRATE_ITEM = ITEMS.register("legendary_crate", () -> crateItem(LEGENDARY_CRATE, false));
   public static final DeferredHolder<Block, Block> GODLY_CRATE_EMPTY = BLOCKS.register("godly_crate_empty", () -> new EmptyCrateBlock(crateProps()));
   public static final DeferredHolder<Item, BlockItem> GODLY_CRATE_EMPTY_ITEM = ITEMS.register("godly_crate_empty", () -> emptyCrateItem(GODLY_CRATE_EMPTY));
   public static final DeferredHolder<Block, Block> GODLY_CRATE = BLOCKS.register(
      "godly_crate", () -> new CrateBlock(crateProps(), crateLoot("godly"), GODLY_CRATE_EMPTY)
   );
   public static final DeferredHolder<Item, BlockItem> GODLY_CRATE_ITEM = ITEMS.register("godly_crate", () -> crateItem(GODLY_CRATE, false));
   public static final DeferredHolder<Block, Block> MYTHIC_CRATE_EMPTY = BLOCKS.register("mythic_crate_empty", () -> new EmptyCrateBlock(crateProps()));
   public static final DeferredHolder<Item, BlockItem> MYTHIC_CRATE_EMPTY_ITEM = ITEMS.register("mythic_crate_empty", () -> emptyCrateItem(MYTHIC_CRATE_EMPTY));
   public static final DeferredHolder<Block, Block> MYTHIC_CRATE = BLOCKS.register(
      "mythic_crate", () -> new CrateBlock(crateProps(), crateLoot("mythic"), MYTHIC_CRATE_EMPTY)
   );
   public static final DeferredHolder<Item, BlockItem> MYTHIC_CRATE_ITEM = ITEMS.register("mythic_crate", () -> crateItem(MYTHIC_CRATE, false));
   public static final DeferredHolder<Block, Block> SECRET_CRATE_EMPTY = BLOCKS.register("secret_crate_empty", () -> new EmptyCrateBlock(crateProps()));
   public static final DeferredHolder<Item, BlockItem> SECRET_CRATE_EMPTY_ITEM = ITEMS.register("secret_crate_empty", () -> emptyCrateItem(SECRET_CRATE_EMPTY));
   public static final DeferredHolder<Block, Block> SECRET_CRATE = BLOCKS.register(
      "secret_crate", () -> new CrateBlock(crateProps(), crateLoot("secret"), SECRET_CRATE_EMPTY)
   );
   public static final DeferredHolder<Item, BlockItem> SECRET_CRATE_ITEM = ITEMS.register("secret_crate", () -> crateItem(SECRET_CRATE, false));
   public static final DeferredHolder<Block, Block> DIVINE_CRATE_EMPTY = BLOCKS.register("divine_crate_empty", () -> new EmptyCrateBlock(crateProps()));
   public static final DeferredHolder<Item, BlockItem> DIVINE_CRATE_EMPTY_ITEM = ITEMS.register("divine_crate_empty", () -> emptyCrateItem(DIVINE_CRATE_EMPTY));
   public static final DeferredHolder<Block, Block> DIVINE_CRATE = BLOCKS.register(
      "divine_crate", () -> new CrateBlock(crateProps(), crateLoot("divine"), DIVINE_CRATE_EMPTY)
   );
   public static final DeferredHolder<Item, BlockItem> DIVINE_CRATE_ITEM = ITEMS.register("divine_crate", () -> crateItem(DIVINE_CRATE, true));
   public static final DeferredHolder<Block, Block> ULTRA_DIVINE_CRATE_EMPTY = BLOCKS.register(
      "ultra_divine_crate_empty", () -> new EmptyCrateBlock(crateProps())
   );
   public static final DeferredHolder<Item, BlockItem> ULTRA_DIVINE_CRATE_EMPTY_ITEM = ITEMS.register(
      "ultra_divine_crate_empty", () -> emptyCrateItem(ULTRA_DIVINE_CRATE_EMPTY)
   );
   public static final DeferredHolder<Block, Block> ULTRA_DIVINE_CRATE = BLOCKS.register(
      "ultra_divine_crate", () -> new CrateBlock(crateProps(), crateLoot("ultra_divine"), ULTRA_DIVINE_CRATE_EMPTY)
   );
   public static final DeferredHolder<Item, BlockItem> ULTRA_DIVINE_CRATE_ITEM = ITEMS.register("ultra_divine_crate", () -> crateItem(ULTRA_DIVINE_CRATE, true));
   public static final DeferredHolder<Block, Block> UNSPOKEN_CRATE_EMPTY = BLOCKS.register("unspoken_crate_empty", () -> new EmptyCrateBlock(crateProps()));
   public static final DeferredHolder<Item, BlockItem> UNSPOKEN_CRATE_EMPTY_ITEM = ITEMS.register(
      "unspoken_crate_empty", () -> emptyCrateItem(UNSPOKEN_CRATE_EMPTY)
   );
   public static final DeferredHolder<Block, Block> UNSPOKEN_CRATE = BLOCKS.register(
      "unspoken_crate", () -> new CrateBlock(crateProps(), crateLoot("unspoken"), UNSPOKEN_CRATE_EMPTY)
   );
   public static final DeferredHolder<Item, BlockItem> UNSPOKEN_CRATE_ITEM = ITEMS.register("unspoken_crate", () -> crateItem(UNSPOKEN_CRATE, true));
   public static final DeferredHolder<Block, Block> GHOST_UNSPOKEN_CRATE_EMPTY = BLOCKS.register(
      "ghost_unspoken_crate_empty", () -> new EmptyCrateBlock(crateProps())
   );
   public static final DeferredHolder<Item, BlockItem> GHOST_UNSPOKEN_CRATE_EMPTY_ITEM = ITEMS.register(
      "ghost_unspoken_crate_empty", () -> emptyCrateItem(GHOST_UNSPOKEN_CRATE_EMPTY)
   );
   /**
    * Every opened crate shares one hinge entity type. The lid swing is the same
    * 0.1-per-tick swing for all twelve tiers; only the lid model differs, and the
    * renderer resolves that from the block itself.
    */
   public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CrateBlockEntity>> CRATE_LID = BLOCK_ENTITIES.register(
      "crate_lid",
      () -> BlockEntityType.Builder.of(
            CrateBlockEntity::new,
            COMMON_CRATE_EMPTY.get(),
            UNCOMMON_CRATE_EMPTY.get(),
            RARE_CRATE_EMPTY.get(),
            EPIC_CRATE_EMPTY.get(),
            LEGENDARY_CRATE_EMPTY.get(),
            GODLY_CRATE_EMPTY.get(),
            MYTHIC_CRATE_EMPTY.get(),
            SECRET_CRATE_EMPTY.get(),
            DIVINE_CRATE_EMPTY.get(),
            ULTRA_DIVINE_CRATE_EMPTY.get(),
            UNSPOKEN_CRATE_EMPTY.get(),
            GHOST_UNSPOKEN_CRATE_EMPTY.get()
         )
         .build(null)
   );
   public static final DeferredHolder<Block, Block> GHOST_UNSPOKEN_CRATE = BLOCKS.register(
      "ghost_unspoken_crate", () -> new CrateBlock(crateProps(), crateLoot("ghost_unspoken"), GHOST_UNSPOKEN_CRATE_EMPTY)
   );
   public static final DeferredHolder<Item, BlockItem> GHOST_UNSPOKEN_CRATE_ITEM = ITEMS.register(
      "ghost_unspoken_crate", () -> crateItem(GHOST_UNSPOKEN_CRATE, true)
   );

   private static BlockBehaviour.Properties crateProps() {
      return BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(1.5F, 6.0F).sound(SoundType.WOOD).noOcclusion();
   }

   private static ResourceKey<LootTable> crateLoot(String key) {
      return ResourceKey.create(Registries.LOOT_TABLE, ResourceLocation.fromNamespaceAndPath("multihooker", "crates/" + key));
   }

   private static BlockItem crateItem(DeferredHolder<Block, Block> block, boolean foil) {
      return new BlockItem(block.get(), new Properties()) {
         public boolean isFoil(ItemStack stack) {
            return foil;
         }

         public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
            tooltip.add(Component.translatable("block.multihooker.crate.tooltip").withStyle(ChatFormatting.GRAY));
         }
      };
   }

   /** The leftover of an opened crate: no foil - it is just an empty box. */
   private static BlockItem emptyCrateItem(DeferredHolder<Block, Block> block) {
      return new BlockItem(block.get(), new Properties()) {
         public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
            tooltip.add(Component.translatable("block.multihooker.crate_empty.tooltip").withStyle(ChatFormatting.GRAY));
         }
      };
   }

   public static final DeferredHolder<Item, Item> SEA_MASTER = ITEMS.register(
      "sea_master", () -> new Item(new Properties().rarity(Rarity.EPIC).stacksTo(1)) {
      public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
         tooltip.add(Component.translatable("item.multihooker.sea_master.tooltip1").withStyle(ChatFormatting.GRAY));
         if (TooltipClient.hasShiftDown()) {
            tooltip.add(Component.literal("Active ONLY while worn in the Curios neck slot.").withStyle(ChatFormatting.AQUA));
            tooltip.add(Component.literal("Shift + Right-Click to view its ability.").withStyle(ChatFormatting.YELLOW));
         } else {
            tooltip.add(Component.literal("<Hold Shift for details>").withStyle(ChatFormatting.DARK_GRAY));
         }
      }

      public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
         if (player.isShiftKeyDown() && level.isClientSide()) {
            TooltipClient.openRelicMenu();
            return InteractionResultHolder.success(player.getItemInHand(hand));
         } else {
            return InteractionResultHolder.pass(player.getItemInHand(hand));
         }
      }
   });

   // Treasure items (elixirs, Crate Potion and the Mystery Enchantment Books).
   // Absorbed from the standalone "MultiHooker Treasures" add-on, so the ids and
   // behaviour now live here. All three drinks are fishing-only, so all three carry the
    // "cannot be crafted" tooltip.
   public static final DeferredHolder<Item, ElixirItem> ELIXIR_VITALITY = ITEMS.register(
      "elixir_vitality", () -> ElixirItem.create("elixir_vitality", true)
   );
   public static final DeferredHolder<Item, ElixirItem> ELIXIR_FORTUNE = ITEMS.register(
      "elixir_fortune", () -> ElixirItem.create("elixir_fortune", true)
   );
   public static final DeferredHolder<Item, CratePotionItem> CRATE_POTION = ITEMS.register(
      "crate_potion", CratePotionItem::new
   );
   public static final DeferredHolder<Item, MysteryBookItem> MYSTERY_BOOK_COMMON = ITEMS.register(
      "mystery_book_common", () -> new MysteryBookItem(MysteryBookItem.Tier.COMMON)
   );
   public static final DeferredHolder<Item, MysteryBookItem> MYSTERY_BOOK_RARE = ITEMS.register(
      "mystery_book_rare", () -> new MysteryBookItem(MysteryBookItem.Tier.RARE)
   );
   public static final DeferredHolder<Item, MysteryBookItem> MYSTERY_BOOK_EPIC = ITEMS.register(
      "mystery_book_epic", () -> new MysteryBookItem(MysteryBookItem.Tier.EPIC)
   );
   public static final DeferredHolder<Item, MysteryBookItem> MYSTERY_BOOK_LEGENDARY = ITEMS.register(
      "mystery_book_legendary", () -> new MysteryBookItem(MysteryBookItem.Tier.LEGENDARY)
   );
   public static final DeferredHolder<Item, MysteryBookItem> MYSTERY_BOOK_MYTHICAL = ITEMS.register(
      "mystery_book_mythical", () -> new MysteryBookItem(MysteryBookItem.Tier.MYTHICAL)
   );

   public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MULTIHOOKER_TAB = CREATIVE_MODE_TABS.register(
      "main",
      () -> CreativeModeTab.builder()
         .title(Component.translatable("itemGroup.multihooker"))
         .icon(() -> new ItemStack((ItemLike)AQUAKEY.get()))
         .displayItems((parameters, output) -> {
            output.accept((ItemLike)AQUAKEY.get());
            output.accept((ItemLike)SEA_MASTER.get());
            output.accept((ItemLike)COMMON_CRATE_ITEM.get());
            output.accept((ItemLike)UNCOMMON_CRATE_ITEM.get());
            output.accept((ItemLike)RARE_CRATE_ITEM.get());
            output.accept((ItemLike)EPIC_CRATE_ITEM.get());
            output.accept((ItemLike)LEGENDARY_CRATE_ITEM.get());
            output.accept((ItemLike)GODLY_CRATE_ITEM.get());
            output.accept((ItemLike)MYTHIC_CRATE_ITEM.get());
            output.accept((ItemLike)SECRET_CRATE_ITEM.get());
            output.accept((ItemLike)DIVINE_CRATE_ITEM.get());
            output.accept((ItemLike)ULTRA_DIVINE_CRATE_ITEM.get());
            output.accept((ItemLike)UNSPOKEN_CRATE_ITEM.get());
            output.accept((ItemLike)GHOST_UNSPOKEN_CRATE_ITEM.get());
            output.accept((ItemLike)ELIXIR_VITALITY.get());
            output.accept((ItemLike)ELIXIR_FORTUNE.get());
            output.accept((ItemLike)CRATE_POTION.get());
            output.accept((ItemLike)MYSTERY_BOOK_COMMON.get());
            output.accept((ItemLike)MYSTERY_BOOK_RARE.get());
            output.accept((ItemLike)MYSTERY_BOOK_EPIC.get());
            output.accept((ItemLike)MYSTERY_BOOK_LEGENDARY.get());
            output.accept((ItemLike)MYSTERY_BOOK_MYTHICAL.get());
         })
         .build()
   );
   public static final DeferredHolder<AttachmentType<?>, AttachmentType<Integer>> MORE_BOBBERS_LEVEL = ATTACHMENTS.register(
      "more_bobbers_level",
      () -> AttachmentType.builder(() -> 0).serialize(Codec.INT).copyOnDeath().sync((holder, player) -> holder == player, ByteBufCodecs.VAR_INT).build()
   );
   public static final DeferredHolder<AttachmentType<?>, AttachmentType<Integer>> BOBBER_POINTS = ATTACHMENTS.register(
      "bobber_points",
      () -> AttachmentType.builder(() -> 0).serialize(Codec.INT).copyOnDeath().sync((holder, player) -> holder == player, ByteBufCodecs.VAR_INT).build()
   );
   // One-time migration marker: legacy AquaKeys (the pre-points upgrade currency)
   // are deleted on the player's first login. Server-side only, never synced.
   public static final DeferredHolder<AttachmentType<?>, AttachmentType<Boolean>> LEGACY_KEYS_WIPED = ATTACHMENTS.register(
      "legacy_keys_wiped",
      () -> AttachmentType.builder(() -> false).serialize(Codec.BOOL).copyOnDeath().build()
   );
   public static final DeferredHolder<AttachmentType<?>, AttachmentType<Integer>> SEA_MASTER_LEVEL = ATTACHMENTS.register(
      "sea_master_level",
      () -> AttachmentType.builder(() -> 0).serialize(Codec.INT).copyOnDeath().sync((holder, player) -> holder == player, ByteBufCodecs.VAR_INT).build()
   );

   /** Beneficial, colour is the crate brown used by the treasures content. */
   private static final class CrateLuckEffect extends MobEffect {
      private CrateLuckEffect() {
         super(MobEffectCategory.BENEFICIAL, 0x663300);
      }
   }

   private MultiHookerRegistry() {
   }
}
