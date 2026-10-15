package com.example.multihooker.client;

import com.example.multihooker.AbstractCrateBlock;
import com.example.multihooker.CrateBlockEntity;
import com.example.multihooker.EmptyCrateBlock;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * Draws the lid of an already-opened crate.
 *
 * <p>The rest of the crate (its body, with the dark open top) is an ordinary
 * block model, so it is baked into the chunk like any other block. Only the lid
 * needs drawing here, because only the lid moves. It is hinged on the crate's
 * back rim - the far edge of the opening, straight across from the latch - and
 * swings up and back, which is exactly what a vanilla chest's lid does.
 *
 * <p>The lid model is authored lying flat in its shut position, so the hinge is
 * the back edge of the lid: y = 12/16 (the top of the body) and z = 0.5/16 (the
 * back face). Rotating about X there lifts the front edge up and over.
 */
public class CrateLidRenderer implements BlockEntityRenderer<CrateBlockEntity> {
   /** The hinge: the back rim of the crate, where the body stops and the lid starts. */
   private static final float HINGE_Y = 12.0F / 16.0F;
   private static final float HINGE_Z = 0.5F / 16.0F;

   public CrateLidRenderer(BlockEntityRendererProvider.Context context) {
   }

   @Override
   public void render(
      CrateBlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay
   ) {
      BlockState state = blockEntity.getBlockState();
      if (!(state.getBlock() instanceof EmptyCrateBlock crate)) {
         return;
      }

      Minecraft minecraft = Minecraft.getInstance();
      BakedModel lid = minecraft.getModelManager().getModel(ModelResourceLocation.standalone(crate.lidModel()));

      // Vanilla's lid curve: the swing is eased so it starts and stops gently,
      // and reaches a full quarter turn at openness 1.
      float openness = blockEntity.getOpenNess(partialTick);
      float angle = 1.0F - openness;
      angle = 1.0F - angle * angle * angle;
      Direction facing = state.getValue(AbstractCrateBlock.FACING);

      poseStack.pushPose();
      // The same yaw the blockstate gives the body, so the two stay in step.
      poseStack.translate(0.5F, 0.5F, 0.5F);
      poseStack.mulPose(Axis.YP.rotationDegrees(-facing.toYRot()));
      poseStack.translate(-0.5F, -0.5F, -0.5F);
      // Hinge, swing, un-hinge.
      poseStack.translate(0.0F, HINGE_Y, HINGE_Z);
      poseStack.mulPose(Axis.XP.rotationDegrees(-angle * 90.0F));
      poseStack.translate(0.0F, -HINGE_Y, -HINGE_Z);

      minecraft.getBlockRenderer()
         .getModelRenderer()
         .renderModel(
            poseStack.last(),
            bufferSource.getBuffer(RenderType.cutout()),
            state,
            lid,
            1.0F,
            1.0F,
            1.0F,
            packedLight,
            packedOverlay
         );
      poseStack.popPose();
   }

   /**
    * A fully open lid stands a whole crate-height above the block and leans back
    * past it, so the default one-cube box would cull the crate while the lid is
    * still on screen.
    */
   @Override
   public AABB getRenderBoundingBox(CrateBlockEntity blockEntity) {
      return AABB.encapsulatingFullBlocks(blockEntity.getBlockPos().offset(-1, 0, -1), blockEntity.getBlockPos().offset(1, 1, 1));
   }
}
