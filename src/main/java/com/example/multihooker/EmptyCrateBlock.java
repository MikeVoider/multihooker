package com.example.multihooker;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A crate that has already been opened: empty for good. Right-clicking it does
 * nothing at all (no key can refill it), and breaking it gives back the empty
 * crate item, which places as another empty crate. It exists as its own block so
 * pick-block or the creative supply can never turn an empty crate back into a
 * full one.
 *
 * <p>Its lid is a real one. The lid itself is not part of the block model - it is
 * drawn by {@code client.CrateLidRenderer}, hinged on the crate's back rim, so it
 * can swing up while the loot window is open and settle back down when it closes.
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

   @Override
   protected boolean triggerEvent(BlockState state, Level level, BlockPos pos, int id, int param) {
      BlockEntity blockEntity = level.getBlockEntity(pos);
      return blockEntity != null && blockEntity.triggerEvent(id, param);
   }
}
