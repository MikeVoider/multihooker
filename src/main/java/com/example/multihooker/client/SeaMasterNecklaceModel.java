package com.example.multihooker.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

public class SeaMasterNecklaceModel extends Model {
   public static final ModelLayerLocation LAYER = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath("multihooker", "sea_master_necklace"), "main");
   private final ModelPart root;

   public SeaMasterNecklaceModel(ModelPart root) {
      super(RenderType::armorCutoutNoCull);
      this.root = root;
   }

   public static LayerDefinition createLayer() {
      MeshDefinition mesh = new MeshDefinition();
      PartDefinition root = mesh.getRoot();
      root.addOrReplaceChild("chain", CubeListBuilder.create().texOffs(0, 0).addBox(-3.0F, 0.0F, -0.5F, 6.0F, 1.0F, 1.0F), PartPose.offset(0.0F, 0.6F, -2.35F));
      root.addOrReplaceChild(
         "pendant", CubeListBuilder.create().texOffs(14, 0).addBox(-2.0F, 0.0F, -0.5F, 4.0F, 4.0F, 1.0F), PartPose.offset(0.0F, 2.0F, -2.7F)
      );
      return LayerDefinition.create(mesh, 32, 16);
   }

   public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color) {
      this.root.render(poseStack, buffer, packedLight, packedOverlay, color);
   }
}
