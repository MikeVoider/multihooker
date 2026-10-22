package com.example.multihooker;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.BlockHitResult;

/**
 * A crate that has already been opened: its loot is gone for good and no key can
 * ever refill it. What it becomes instead is storage. Right-clicking it opens a
 * plain chest window - no key, no cost - holding that crate's own 27 slots, which
 * are saved with the world and spill on the floor if the block is broken. It
 * exists as its own block so pick-block or the creative supply can never turn an
 * empty crate back into a full one.
 *
 * <p>Its lid is a real one. The lid itself is not part of the block model - it is
 * drawn by {@code client.CrateLidRenderer}, hinged on the crate's back rim, so it
 * can swing up while the loot or storage window is open and settle back down when
 * it closes.
 */
public class EmptyCrateBlock extends AbstractCrateBlock implements EntityBlock {
   private static final String SUFFIX = "_crate_empty";

   public EmptyCrateBlock(Properties properties) {
      super(properties);
   }

   /**
    * This crate's tier key ("common", "ghost_unspoken", ...), read straight off
    * the block's registry name so it can never drift from what was registered.
    */
   public String tierKey() {
      String path = BuiltInRegistries.BLOCK.getKey(this).getPath();
      return path.endsWith(SUFFIX) ? path.substring(0, path.length() - SUFFIX.length()) : path;
   }

   /** The lid model of this crate, as loaded for {@code CrateLidRenderer}. */
   public ResourceLocation lidModel() {
      return ResourceLocation.fromNamespaceAndPath(MultiHookerMod.MOD_ID, "block/" + this.tierKey() + "_crate_lid");
   }

   @Override
   public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
      return new CrateBlockEntity(pos, state);
   }

   /**
    * The lid is only animated on the client: the open/closed state itself is
    * pushed to every client as a block event, so the server has nothing to tick.
    */
   @Override
   public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
      if (!level.isClientSide() || type != MultiHookerRegistry.CRATE_LID.get()) {
         return null;
      }
      return (tickLevel, pos, tickState, blockEntity) -> ((CrateBlockEntity)blockEntity).animateLid();
   }

   /**
    * Right-clicking an already-opened crate opens its storage. It is a chest, so it
    * needs nothing in hand and takes no key: the default {@code useItemOn} hands the
    * interaction here. Sneaking still places blocks against it, exactly like a chest.
    */
   @Override
   protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
      if (level.isClientSide()) {
         return InteractionResult.SUCCESS;
      }

      if (!(player instanceof ServerPlayer serverPlayer) || !(level.getBlockEntity(pos) instanceof CrateBlockEntity crate)) {
         return InteractionResult.PASS;
      }

      String tier = tierKey();
      level.playSound(null, pos, SoundEvents.CHEST_OPEN, SoundSource.BLOCKS, 1.0F, 1.0F);

      serverPlayer.openMenu(
         new SimpleMenuProvider(
            (id, inventory, opener) -> new CrateMenu(id, inventory, crate, tier, pos, true),
            Component.translatable("block.multihooker." + tier + "_crate_empty")
         ),
         data -> {
            data.writeUtf(tier);
            data.writeBlockPos(pos);
         }
      );
      return InteractionResult.CONSUME;
   }

   /**
    * Breaking an opened crate empties its storage onto the floor, the same way a
    * chest does, so stored items are never lost with the block.
    */
   @Override
   protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
      Containers.dropContentsOnDestroy(state, newState, level, pos);
      super.onRemove(state, level, pos, newState, movedByPiston);
   }

   @Override
   protected boolean triggerEvent(BlockState state, Level level, BlockPos pos, int id, int param) {
      BlockEntity blockEntity = level.getBlockEntity(pos);
      return blockEntity != null && blockEntity.triggerEvent(id, param);
   }
}
