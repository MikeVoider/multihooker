package com.example.multihooker.client;

import com.example.multihooker.AbstractCrateBlock;
import com.example.multihooker.CrateBlockEntity;
import com.example.multihooker.EmptyCrateBlock;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.model.data.ModelData;

/**
 * Draws the lid of an already-opened crate.
 *
 * <p>The rest of the crate (its body, with the dark open top) is an ordinary
 * block model, so it is baked into the chunk like any other block. Only the lid
 * needs drawing here, because only the lid moves. It is hinged on the crate's
 * back rim - the far edge of the opening, straight across from the face that
 * carries the engraved name - and swings up and back, which is exactly what a
 * vanilla chest's lid does.
 *
 * <p>The lid model is authored lying flat in its shut position. The name side is
 * the model's EAST face (see tools/crategen/gen_resources.facing_blockstate, which
 * turns every facing a quarter turn so that face looks at the player), so the
 * hinge is the WEST edge of the lid: x = 0.5/16 and y = 12/16 (the top of the
 * body). Rotating about Z there lifts the far edge up and over.
 */
public class CrateLidRenderer implements BlockEntityRenderer<CrateBlockEntity> {
   /** The hinge: the back rim of the crate, where the body stops and the lid starts. */
   private static final float HINGE_Y = 12.0F / 16.0F;
   /** West edge, the rim furthest from the engraved name. */
   private static final float HINGE_X = 0.5F / 16.0F;
   /**
    * The extra quarter turn the blockstate puts on the body, so the lid and body
    * stay in step. Must match facing_blockstate in tools/crategen/gen_resources.py.
    */
   private static final float NAMED_FACE_YAW = 90.0F;

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
      poseStack.mulPose(Axis.YP.rotationDegrees(-(facing.toYRot() + NAMED_FACE_YAW)));
      poseStack.translate(-0.5F, -0.5F, -0.5F);
      // Hinge, swing, un-hinge. These must be T(+hinge) * R * T(-hinge), i.e. the
      // pose is rotated about the hinge point itself. Swapping the two translates
      // rotates about -hinge instead, a point below and west of the crate, and the
      // lid then sweeps across the crate instead of pivoting on its rim.
      poseStack.translate(HINGE_X, HINGE_Y, 0.0F);
      poseStack.mulPose(Axis.ZP.rotationDegrees(angle * 90.0F));
      poseStack.translate(-HINGE_X, -HINGE_Y, 0.0F);

      BlockPos pos = blockEntity.getBlockPos();
      RenderType renderType = RenderType.cutout();
      VertexConsumer consumer = bufferSource.getBuffer(renderType);
      Level level = blockEntity.getLevel();

      // Draw the lid the same way the chunk renderer draws the body, not with
      // renderModel. renderModel skips both directional shading (up 1.0,
      // north/south 0.8, east/west 0.6) and ambient occlusion, so the lid came
      // out uniformly full brightness while the body right underneath it was
      // shaded - a bright lid on a dark crate. tesselateBlock is the chunk path,
      // so the lid now matches the body it is hinged to.
      if (level != null) {
         minecraft.getBlockRenderer()
            .getModelRenderer()
            .tesselateBlock(
               level,
               lid,
               state,
               pos,
               poseStack,
               consumer,
               false,
               RandomSource.create(),
               state.getSeed(pos),
               packedOverlay,
               ModelData.EMPTY,
               renderType
            );
      } else {
         // No level to sample light and neighbours from: fall back to the plain
         // path rather than skipping the lid entirely.
         minecraft.getBlockRenderer()
            .getModelRenderer()
            .renderModel(
               poseStack.last(), consumer, state, lid, 1.0F, 1.0F, 1.0F, packedLight, packedOverlay, ModelData.EMPTY, renderType
            );
      }

      poseStack.popPose();
   }

   /**
    * A fully open lid stands a whole crate-height above the block and leans back
    * past it, so the default one-cube box would cull the crate while the lid is
    * still on screen. The open lid reaches y = 12/16 + 15/16, so the box has to
    * be two blocks tall, not one.
    */
   @Override
   public AABB getRenderBoundingBox(CrateBlockEntity blockEntity) {
      return AABB.encapsulatingFullBlocks(blockEntity.getBlockPos().offset(-1, 0, -1), blockEntity.getBlockPos().offset(1, 2, 1));
   }
}
