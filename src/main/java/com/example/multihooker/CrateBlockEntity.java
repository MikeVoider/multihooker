package com.example.multihooker;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The hinge of an already-opened crate. Only the empty crate carries one: the
 * placeable crate is a plain static model, and the instant it is opened the
 * block is swapped for its empty form, whose lid then swings up while the loot
 * window is on screen and settles back down when it closes.
 *
 * <p>The lid itself is not drawn by the block model - {@code client.CrateLidRenderer}
 * draws it, hinged on the crate's back rim, exactly the way vanilla chests do
 * theirs. This entity only holds how far open the lid is, plus (server side) how
 * many players are looking inside; the open/closed state reaches the clients as a
 * block event, so there is no per-tick packet traffic.
 */
public class CrateBlockEntity extends BlockEntity {
   /** How far the lid moves each client tick. Vanilla chests use the same 0.1. */
   private static final float STEP = 0.1F;

   private boolean shouldBeOpen;
   private float openness;
   private float previousOpenness;
   private int viewers;

   public CrateBlockEntity(BlockPos pos, BlockState state) {
      super(MultiHookerRegistry.CRATE_LID.get(), pos, state);
   }

   /** One client tick of the lid swing. */
   public void animateLid() {
      this.previousOpenness = this.openness;
      float target = this.shouldBeOpen ? 1.0F : 0.0F;
      if (this.openness < target) {
         this.openness = Math.min(this.openness + STEP, target);
      } else if (this.openness > target) {
         this.openness = Math.max(this.openness - STEP, target);
      }
   }

   /** 0 = lid flat/shut, 1 = lid standing fully open, smoothed across the tick. */
   public float getOpenNess(float partialTick) {
      return Mth.lerp(partialTick, this.previousOpenness, this.openness);
   }

   /** Server side: the loot window for this crate was opened. */
   public void addViewer() {
      this.viewers++;
      this.signalOpenCount();
   }

   /** Server side: that window closed again. */
   public void removeViewer() {
      if (this.viewers > 0) {
         this.viewers--;
      }
      this.signalOpenCount();
   }

   /**
    * Tell the clients whether the lid should be up. Sent as a block event rather
    * than an update packet so Minecraft's own "does this section need redrawing"
    * tracking handles the repaint.
    */
   private void signalOpenCount() {
      Level level = this.level;
      if (level != null && !level.isClientSide()) {
         level.blockEvent(this.worldPosition, this.getBlockState().getBlock(), 1, this.viewers > 0 ? 1 : 0);
      }
   }

   @Override
   public boolean triggerEvent(int id, int type) {
      if (id == 1) {
         this.shouldBeOpen = type > 0;
         return true;
      }
      return super.triggerEvent(id, type);
   }
}
