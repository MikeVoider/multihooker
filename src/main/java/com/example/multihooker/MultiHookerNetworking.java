package com.example.multihooker;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class MultiHookerNetworking {
   private MultiHookerNetworking() {
   }

   public static void register(RegisterPayloadHandlersEvent event) {
      PayloadRegistrar registrar = event.registrar("1");
      registrar.playToServer(
         MultiHookerNetworking.UpgradeBobbersSkillPayload.TYPE,
         MultiHookerNetworking.UpgradeBobbersSkillPayload.STREAM_CODEC,
         (payload, context) -> context.enqueueWork(() -> SkillData.handleUpgradeRequest((ServerPlayer)context.player()))
      );
      registrar.playToServer(
         MultiHookerNetworking.ResetBobbersSkillPayload.TYPE,
         MultiHookerNetworking.ResetBobbersSkillPayload.STREAM_CODEC,
         (payload, context) -> context.enqueueWork(() -> SkillData.handleResetRequest((ServerPlayer)context.player()))
      );
      registrar.playToServer(
         MultiHookerNetworking.LevelUpSeaMasterPayload.TYPE,
         MultiHookerNetworking.LevelUpSeaMasterPayload.STREAM_CODEC,
         (payload, context) -> context.enqueueWork(() -> RelicData.handleLevelUpRequest((ServerPlayer)context.player()))
      );
   }

   public record LevelUpSeaMasterPayload() implements CustomPacketPayload {
      public static final MultiHookerNetworking.LevelUpSeaMasterPayload INSTANCE = new MultiHookerNetworking.LevelUpSeaMasterPayload();
      public static final Type<MultiHookerNetworking.LevelUpSeaMasterPayload> TYPE = new Type(
         ResourceLocation.fromNamespaceAndPath("multihooker", "level_up_sea_master")
      );
      public static final StreamCodec<RegistryFriendlyByteBuf, MultiHookerNetworking.LevelUpSeaMasterPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

      public Type<MultiHookerNetworking.LevelUpSeaMasterPayload> type() {
         return TYPE;
      }
   }

   public record ResetBobbersSkillPayload() implements CustomPacketPayload {
      public static final MultiHookerNetworking.ResetBobbersSkillPayload INSTANCE = new MultiHookerNetworking.ResetBobbersSkillPayload();
      public static final Type<MultiHookerNetworking.ResetBobbersSkillPayload> TYPE = new Type(
         ResourceLocation.fromNamespaceAndPath("multihooker", "reset_bobbers_skill")
      );
      public static final StreamCodec<RegistryFriendlyByteBuf, MultiHookerNetworking.ResetBobbersSkillPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

      public Type<MultiHookerNetworking.ResetBobbersSkillPayload> type() {
         return TYPE;
      }
   }

   public record UpgradeBobbersSkillPayload() implements CustomPacketPayload {
      public static final MultiHookerNetworking.UpgradeBobbersSkillPayload INSTANCE = new MultiHookerNetworking.UpgradeBobbersSkillPayload();
      public static final Type<MultiHookerNetworking.UpgradeBobbersSkillPayload> TYPE = new Type(
         ResourceLocation.fromNamespaceAndPath("multihooker", "upgrade_bobbers_skill")
      );
      public static final StreamCodec<RegistryFriendlyByteBuf, MultiHookerNetworking.UpgradeBobbersSkillPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

      public Type<MultiHookerNetworking.UpgradeBobbersSkillPayload> type() {
         return TYPE;
      }
   }
}
