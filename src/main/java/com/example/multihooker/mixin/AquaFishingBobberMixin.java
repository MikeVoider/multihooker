package com.example.multihooker.mixin;

import com.example.multihooker.MultiHookerManager;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(
   targets = {"com.teammetallurgy.aquaculture.entity.AquaFishingBobberEntity"}
)
public abstract class AquaFishingBobberMixin {
   @Inject(
      method = {"retrieve(Lnet/minecraft/world/item/ItemStack;)I"},
      at = {@At("HEAD")},
      require = 0
   )
   private void multihooker$onRetrieveStart(ItemStack rod, CallbackInfoReturnable<Integer> cir) {
      MultiHookerManager.onRetrieveStart((FishingHook)(Object)this);
   }
}
