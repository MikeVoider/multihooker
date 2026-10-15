package com.example.multihooker.client;

import com.example.multihooker.NetVisualEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

public class NetVisualRenderer extends EntityRenderer<NetVisualEntity> {
   private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("multihooker", "textures/entity/net_visual.png");
   private static final RenderType RENDER_TYPE = RenderType.entityCutoutNoCull(TEXTURE);
   private static final float SIZE = 2.0F;

   public NetVisualRenderer(Context context) {
      super(context);
   }

   public ResourceLocation getTextureLocation(NetVisualEntity entity) {
      return TEXTURE;
   }

   public void render(NetVisualEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
      poseStack.pushPose();
      poseStack.translate(0.0F, 0.05F, 0.0F);
      poseStack.mulPose(Axis.YP.rotationDegrees(-entityYaw));
      poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
      float half = 1.0F;
      Matrix4f matrix = poseStack.last().pose();
      VertexConsumer consumer = bufferSource.getBuffer(RENDER_TYPE);
      quadVertex(consumer, matrix, -half, -half, packedLight, 0.0F, 0.0F);
      quadVertex(consumer, matrix, half, -half, packedLight, 1.0F, 0.0F);
      quadVertex(consumer, matrix, half, half, packedLight, 1.0F, 1.0F);
      quadVertex(consumer, matrix, -half, half, packedLight, 0.0F, 1.0F);
      poseStack.popPose();
      super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
   }

   private static void quadVertex(VertexConsumer consumer, Matrix4f matrix, float x, float y, int packedLight, float u, float v) {
      consumer.addVertex(matrix, x, y, 0.0F)
         .setColor(255, 255, 255, 255)
         .setUv(u, v)
         .setOverlay(OverlayTexture.NO_OVERLAY)
         .setLight(packedLight)
         .setNormal(0.0F, 0.0F, 1.0F);
   }
}
