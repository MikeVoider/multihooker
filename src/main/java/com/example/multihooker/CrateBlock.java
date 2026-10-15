package com.example.multihooker;

import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;

/**
 * A placeable fishing crate. Right-click it while holding an AquaKey to open it:
 * the key is consumed, the crate's loot table is rolled into a chest-style loot
 * window (see {@link CrateMenu}) that shows the rolled items next to a preview
 * of that crate's odds, and the block itself is left behind as its empty form
 * with the lid up. The empty crate cannot be opened or refilled again.
 *
 * <p>One instance per tier, each bound to its own loot table under
 * data/multihooker/loot_table/crates/&lt;tier&gt;.json and to the empty block
 * that replaces it under data/multihooker/loot_table/blocks/&lt;tier&gt;_crate_empty.json.
 */
public class CrateBlock extends AbstractCrateBlock {
   private final ResourceKey<LootTable> lootTable;
   private final Supplier<Block> emptyBlock;

   public CrateBlock(Properties properties, ResourceKey<LootTable> lootTable, Supplier<Block> emptyBlock) {
      super(properties);
      this.lootTable = lootTable;
      this.emptyBlock = emptyBlock;
   }

   /**
    * The crate is locked: it only opens when the player right-clicks it while
    * holding the AquaKey. The key is consumed on success (never in creative).
    * Any other item falls through to normal behaviour, so blocks can still be
    * placed against the crate.
    */
   @Override
   protected ItemInteractionResult useItemOn(
      ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult
   ) {
      if (!stack.is(MultiHookerRegistry.AQUAKEY.get())) {
         return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
      }

      open(level, pos, state, player, stack);
      return ItemInteractionResult.sidedSuccess(level.isClientSide());
   }

   /**
    * Empty main hand: an AquaKey held in the OFF-hand opens the crate too, the
    * same way off-hand keys count for the Fishing Panel. Anything else (including
    * a non-empty main hand) falls through.
    */
   @Override
   protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
      if (!player.getMainHandItem().isEmpty()) {
         return InteractionResult.PASS;
      }

      ItemStack offhand = player.getOffhandItem();
      if (!offhand.is(MultiHookerRegistry.AQUAKEY.get())) {
         return InteractionResult.PASS;
      }

      open(level, pos, state, player, offhand);
      return InteractionResult.sidedSuccess(level.isClientSide());
   }

   private void open(Level level, BlockPos pos, BlockState state, Player player, ItemStack keyStack) {
      if (!(level instanceof ServerLevel server) || !(player instanceof ServerPlayer serverPlayer)) {
         return;
      }

      String tier = tierName();
      ResourceKey<LootTable> key = ResourceKey.create(Registries.LOOT_TABLE, ResourceLocation.fromNamespaceAndPath("multihooker", "crates/" + tier));
      LootParams params = new LootParams.Builder(server)
         .withParameter(LootContextParams.ORIGIN, pos.getCenter())
         .withLuck(player.getLuck())
         .create(LootContextParamSets.CHEST);
      LootTable table = server.getServer().reloadableRegistries().getLootTable(key);

      // Roll the loot into the window's chest grid (27 slots). A crate rolls at
      // most 9 stacks, so this only overflows if a pack makes a crate enormous;
      // the overflow goes straight to the player rather than being dropped on the
      // floor.
      SimpleContainer loot = new SimpleContainer(CrateMenu.LOOT_SLOTS);
      int slot = 0;

      for (ItemStack stack : table.getRandomItems(params)) {
         // Instruction books (a guidebook/manual that another mod tagged into one
         // of the cross-mod tags) are never crate loot: skip them instead of
         // handing one to the player.
         if (CrateLootFilter.isInstructionBook(stack)) {
            continue;
         }

         if (slot < loot.getContainerSize()) {
            loot.setItem(slot++, stack);
         } else if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
         }
      }

      // The crate springs open: it stays behind as its empty form, keeping its
      // facing, and can never be opened again.
      level.setBlock(pos, this.emptyBlock.get().defaultBlockState().setValue(FACING, state.getValue(FACING)), Block.UPDATE_ALL);

      server.sendParticles(
         ParticleTypes.END_ROD,
         pos.getX() + 0.5,
         pos.getY() + 0.6,
         pos.getZ() + 0.5,
         8,
         0.45,
         0.4,
         0.45,
         0.01
      );
      server.playSound(null, pos, SoundEvents.CHEST_OPEN, SoundSource.BLOCKS, 1.0F, 1.2F);

      if (!player.getAbilities().instabuild) {
         keyStack.shrink(1);
      }

      serverPlayer.openMenu(
         new SimpleMenuProvider(
            (id, inventory, opener) -> new CrateMenu(id, inventory, loot, tier, pos),
            Component.translatable("block.multihooker." + tier + "_crate")
         ),
         data -> {
            data.writeUtf(tier);
            data.writeBlockPos(pos);
         }
      );
   }

   private String tierName() {
      String path = lootTable.location().getPath();
      return path.substring(path.lastIndexOf('/') + 1);
   }
}
