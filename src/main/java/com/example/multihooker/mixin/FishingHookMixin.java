package com.example.multihooker.mixin;

import com.example.multihooker.MultiHookerManager;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({FishingHook.class})
public abstract class FishingHookMixin {
   @Inject(
      method = {"retrieve(Lnet/minecraft/world/item/ItemStack;)I"},
      at = {@At("HEAD")}
   )
   private void multihooker$onRetrieveStart(ItemStack rod, CallbackInfoReturnable<Integer> cir) {
      MultiHookerManager.onRetrieveStart((FishingHook)(Object)this);
   }

   @Inject(
      method = {"remove(Lnet/minecraft/world/entity/Entity$RemovalReason;)V"},
      at = {@At("HEAD")}
   )
   private void multihooker$onRemove(RemovalReason reason, CallbackInfo ci) {
      MultiHookerManager.onHookRemoved((FishingHook)(Object)this, reason);
   }

   @Inject(
      method = {"remove(Lnet/minecraft/world/entity/Entity$RemovalReason;)V"},
      at = {@At("RETURN")}
   )
   private void multihooker$afterRemove(RemovalReason reason, CallbackInfo ci) {
      MultiHookerManager.afterHookRemoved((FishingHook)(Object)this);
   }
}
