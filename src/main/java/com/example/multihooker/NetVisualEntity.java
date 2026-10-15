package com.example.multihooker;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData.Builder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

public class NetVisualEntity extends Entity {
   public static final int LIFETIME = 80;

   public NetVisualEntity(EntityType<? extends NetVisualEntity> type, Level level) {
      super(type, level);
      this.noPhysics = true;
   }

   protected void defineSynchedData(Builder builder) {
   }

   protected void readAdditionalSaveData(CompoundTag tag) {
   }

   protected void addAdditionalSaveData(CompoundTag tag) {
   }

   public boolean isPickable() {
      return false;
   }

   public boolean isAttackable() {
      return false;
   }

   public void tick() {
      super.tick();
      if (!this.level().isClientSide() && this.tickCount > 80) {
         this.discard();
      }
   }

   public boolean shouldRenderAtSqrDistance(double distanceSqr) {
      return distanceSqr < 16384.0;
   }
}
