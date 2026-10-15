package com.example.multihooker.client;

import com.example.multihooker.Config;
import net.minecraft.client.Camera.NearPlane;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.ItemAbilities;

public final class LineAnchor {
   private LineAnchor() {
   }

   public static Vec3 rodTipAnchor(NearPlane plane, float x, float y) {
      if (!(Boolean)Config.LINE_FROM_ROD_TIP.get()) {
         return plane.getPointOnPlane(x, y);
      } else {
         float side = x < 0.0F ? -1.0F : 1.0F;
         float anchorX = ((Double)Config.LINE_ANCHOR_X.get()).floatValue();
         float anchorY = ((Double)Config.LINE_ANCHOR_Y.get()).floatValue();
         return plane.getPointOnPlane(side * anchorX, anchorY);
      }
   }

   public static Vec3 aquaRodTipAnchor(NearPlane plane, float x, float y) {
      if (!(Boolean)Config.LINE_FROM_ROD_TIP.get()) {
         return plane.getPointOnPlane(x, y);
      } else {
         float side = x < 0.0F ? -1.0F : 1.0F;
         float anchorX = ((Double)Config.AQUA_LINE_ANCHOR_X.get()).floatValue();
         float anchorY = ((Double)Config.AQUA_LINE_ANCHOR_Y.get()).floatValue();
         return plane.getPointOnPlane(side * anchorX, anchorY);
      }
   }

   /**
    * Punchy-compatible rod-tip anchor, applied at the {@code FishingHookRenderer.render} call
    * site instead of inside {@code getPlayerHandPos}.
    *
    * <p>First-person animation mods such as Punchy cancel {@code getPlayerHandPos} at the very
    * top and substitute their own position computed from their animated arm rig, so a redirect
    * on the inner {@code getPointOnPlane} call never runs and the configured anchor has no
    * effect. Here we keep whatever position the renderer produced (vanilla hand point <em>or</em>
    * Punchy's animated grip) and shift it by the world-space delta that separates the vanilla
    * hand point from our rod-tip anchor. Scale and rotation below are linear, so when nothing
    * overrides the vanilla method this is exactly the old vanilla-style anchored tip, and when
    * Punchy does, the line stays glued to Punchy's animated rod but starts at the tip.</p>
    */
   public static Vec3 applyRodTipAnchor(Player player, float anim, Vec3 base) {
      if (!(Boolean)Config.LINE_FROM_ROD_TIP.get()) {
         return base;
      } else {
         Minecraft minecraft = Minecraft.getInstance();
         if (minecraft.player != player) {
            return base;
         } else if (!minecraft.options.getCameraType().isFirstPerson()) {
            return base;
         } else {
            NearPlane plane = minecraft.gameRenderer.getMainCamera().getNearPlane();
            int i = player.getMainArm() == HumanoidArm.RIGHT ? 1 : -1;
            if (!player.getMainHandItem().canPerformAction(ItemAbilities.FISHING_ROD_CAST)) {
               i = -i;
            }

            float side = i >= 0 ? 1.0F : -1.0F;
            float anchorX = ((Double)Config.LINE_ANCHOR_X.get()).floatValue();
            float anchorY = ((Double)Config.LINE_ANCHOR_Y.get()).floatValue();
            double d4 = 960.0 / (double)minecraft.options.fov().get().intValue();
            Vec3 delta = plane.getPointOnPlane(side * anchorX, anchorY)
               .subtract(plane.getPointOnPlane((float)i * 0.525F, -0.1F))
               .scale(d4)
               .yRot(anim * 0.5F)
               .xRot(-anim * 0.7F);
            return base.add(delta);
         }
      }
   }
}
