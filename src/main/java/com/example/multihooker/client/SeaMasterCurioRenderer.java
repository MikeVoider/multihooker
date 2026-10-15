package com.example.multihooker.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.client.ICurioRenderer;

public class SeaMasterCurioRenderer implements ICurioRenderer {
   private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("multihooker", "textures/item/sea_master_necklace.png");
   private final SeaMasterNecklaceModel necklace;

   public SeaMasterCurioRenderer() {
      EntityModelSet models = Minecraft.getInstance().getEntityModels();
      this.necklace = new SeaMasterNecklaceModel(models.bakeLayer(SeaMasterNecklaceModel.LAYER));
   }

   public <T extends LivingEntity, M extends EntityModel<T>> void render(
      ItemStack stack,
      SlotContext slotContext,
      PoseStack poseStack,
      RenderLayerParent<T, M> renderLayerParent,
      MultiBufferSource buffer,
      int light,
      float limbSwing,
      float limbSwingAmount,
      float partialTick,
      float ageInTicks,
      float netHeadYaw,
      float headPitch
   ) {
      if (renderLayerParent.getModel() instanceof HumanoidModel<?> humanoid) {
         poseStack.pushPose();
         ICurioRenderer.rotateIfSneaking(poseStack, slotContext.entity());
         humanoid.body.translateAndRotate(poseStack);
         VertexConsumer consumer = buffer.getBuffer(RenderType.armorCutoutNoCull(TEXTURE));
         this.necklace.renderToBuffer(poseStack, consumer, light, OverlayTexture.NO_OVERLAY, -1);
         poseStack.popPose();
      }
   }
}
