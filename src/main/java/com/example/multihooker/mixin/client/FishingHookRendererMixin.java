package com.example.multihooker.mixin.client;

import com.example.multihooker.client.LineAnchor;
import net.minecraft.client.renderer.entity.FishingHookRenderer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin({FishingHookRenderer.class})
public abstract class FishingHookRendererMixin {
   /**
    * Raw hand position straight from vanilla (or from another mod's override of it, e.g. Punchy's
    * animated grip). We must re-invoke the original to keep working when Punchy replaces it.
    */
   @Invoker("getPlayerHandPos")
   public abstract Vec3 multihooker$getPlayerHandPos(Player player, float anim, float partialTick);

   /**
    * Anchor the first-person fishing line at the rod tip. Done at the {@code render} call site
    * rather than on the inner {@code getPointOnPlane} call so it still applies when Punchy (or a
    * similar first-person animation mod) cancels {@code getPlayerHandPos} and supplies its own
    * position. See {@link LineAnchor#applyRodTipAnchor}.
    */
   @Redirect(
      method = {"render(Lnet/minecraft/world/entity/projectile/FishingHook;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/renderer/entity/FishingHookRenderer;getPlayerHandPos(Lnet/minecraft/world/entity/player/Player;FF)Lnet/minecraft/world/phys/Vec3;"
      ),
      require = 0
   )
   private Vec3 multihooker$rodTipAnchor(FishingHookRenderer self, Player player, float anim, float partialTick) {
      Vec3 base = this.multihooker$getPlayerHandPos(player, anim, partialTick);
      return LineAnchor.applyRodTipAnchor(player, anim, base);
   }
}
