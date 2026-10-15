package com.example.multihooker;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The shape and facing shared by the two crate blocks: the placeable crate
 * ({@link CrateBlock}) and the empty one it turns into when opened
 * ({@link EmptyCrateBlock}). Both use the same 15x15x15.5 box and the same
 * four-way latch facing, so an opened crate keeps the pose it was placed in.
 */
public abstract class AbstractCrateBlock extends Block {
   /** Horizontal facing: the latch side points at whoever placed the crate. */
   public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

   /**
    * The crate is not a full cube: the body and its flat lid share the same 15x15
    * footprint (a half-pixel gap all round) and the whole thing is 15.5/16 of a
    * block tall, so it nearly fills the block like a barrel. Without this the
    * block would fall back to a full 16x16x16 selection and collision box. A
    * single box matches the model exactly (and it is centred, so this needs no
    * facing rotation).
    */
   protected static final VoxelShape SHAPE = Block.box(0.5, 0.0, 0.5, 15.5, 15.5, 15.5);

   /**
    * How much faster an axe cuts a crate than the block's own default. A crate is
    * a wooden box, so an axe is the natural tool for breaking one.
    */
   private static final float AXE_SPEED_MULTIPLIER = 3.0F;

   protected AbstractCrateBlock(Properties properties) {
      super(properties);
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
      builder.add(FACING);
   }

   @Override
   public BlockState getStateForPlacement(BlockPlaceContext context) {
      return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
   }

   @Override
   protected BlockState rotate(BlockState state, Rotation rotation) {
      return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
   }

   @Override
   protected BlockState mirror(BlockState state, Mirror mirror) {
      return state.rotate(mirror.getRotation(state.getValue(FACING)));
   }

   @Override
   protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      return SHAPE;
   }

   @Override
   protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      return SHAPE;
   }

   /**
    * An axe chews through a wooden crate far faster than a bare hand or a pickaxe.
    * This only changes how QUICKLY a crate breaks - opening one is still the
    * AquaKey's job, so an axe is never a way to reach the loot inside.
    */
   @Override
   public float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
      if (player.getMainHandItem().is(ItemTags.AXES)) {
         float hardness = state.getDestroySpeed(level, pos);
         if (hardness < 0.0F) {
            return 0.0F;
         }

         // Treat the axe as the crate's correct tool (the /30 divisor) and then
         // multiply, so it is plainly the fastest way to break one.
         return Math.min(1.0F, player.getDestroySpeed(state) * AXE_SPEED_MULTIPLIER / hardness / 30.0F);
      }

      return super.getDestroyProgress(state, player, level, pos);
   }

   /**
    * Vanilla's break effect is one level event that carries BOTH the break sound
    * and a dense 4x4x4 = 64-particle dust burst (the count comes from how close
    * the crate is to a full block). We want the sound, just a lighter dusting, so
    * play the sound ourselves and send a modest puff of the crate's block dust.
    */
   @Override
   protected void spawnDestroyParticles(Level level, Player player, BlockPos pos, BlockState state) {
      SoundType sound = state.getSoundType();
      level.playSound(
         null, pos, sound.getBreakSound(), net.minecraft.sounds.SoundSource.BLOCKS, (sound.getVolume() + 1.0F) / 2.0F, sound.getPitch() * 0.8F
      );
      if (level instanceof ServerLevel server) {
         server.sendParticles(
            new BlockParticleOption(ParticleTypes.BLOCK, state),
            pos.getX() + 0.5,
            pos.getY() + 0.5,
            pos.getZ() + 0.5,
            24,
            0.45,
            0.45,
            0.45,
            0.12
         );
      }
   }
}
