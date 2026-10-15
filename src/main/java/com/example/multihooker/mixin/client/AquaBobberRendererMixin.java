package com.example.multihooker.mixin.client;

import com.example.multihooker.client.LineAnchor;
import net.minecraft.client.Camera.NearPlane;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(
   targets = {"com.teammetallurgy.aquaculture.client.renderer.entity.AquaBobberRenderer"}
)
public abstract class AquaBobberRendererMixin {
   @Redirect(
      method = {"getPlayerHandPos"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/Camera$NearPlane;getPointOnPlane(FF)Lnet/minecraft/world/phys/Vec3;"
      ),
      require = 0
   )
   private Vec3 multihooker$rodTipAnchor(NearPlane plane, float x, float y) {
      return LineAnchor.aquaRodTipAnchor(plane, x, y);
   }
}
