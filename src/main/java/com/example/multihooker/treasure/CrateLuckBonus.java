package com.example.multihooker.treasure;

import com.example.multihooker.Config;
import com.example.multihooker.MultiHookerMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Turns the Crate Potion's Crate Luck into a real Luck bonus on the player.
 *
 * <p>Originally the bonus was added by hand in {@code CrateBlock} when a MultiHooker trove was
 * opened, which meant it only ever helped MultiHooker's own crates. Every other crate in a pack
 * rolls its loot from the vanilla loot-table machinery, and that reads a single number:
 * {@code Player.getLuck()}. Aquaculture's Neptune's Bounty and Fishing Crate's four crates are both
 * ordinary blocks with ordinary loot tables, so neither of them can ever see a mod-specific buff.
 *
 * <p>Putting the bonus on the Luck attribute instead makes it work for all of them at once,
 * because they are all reading the same stat MultiHooker already was.
 *
 * <p>The modifier is transient: it lives only as long as the effect does, and it is re-applied
 * after a respawn. That keeps Crate Luck a temporary buff rather than something the player
 * accumulates.
 */
public final class CrateLuckBonus {
   private static final ResourceLocation MODIFIER_ID =
      ResourceLocation.fromNamespaceAndPath(MultiHookerMod.MOD_ID, "crate_luck_bonus");

   private CrateLuckBonus() {
   }

   /**
    * Adds the bonus, or tops it up to the current configured value.
    *
    * <p>Idempotent, and never stacks: the modifier is removed before being re-added, so drinking a
    * second bottle while the effect is already running refreshes the amount instead of doubling it.
    */
   public static void apply(LivingEntity entity) {
      if (entity.level().isClientSide()) {
         return;
      }
      AttributeInstance luck = entity.getAttribute(Attributes.LUCK);
      if (luck == null) {
         return;
      }
      double amount = (Double)Config.CRATE_LUCK_BONUS.get();
      AttributeModifier existing = luck.getModifier(MODIFIER_ID);
      if (existing != null && existing.amount() == amount) {
         return;
      }
      if (existing != null) {
         luck.removeModifier(existing);
      }
      luck.addTransientModifier(
         new AttributeModifier(MODIFIER_ID, amount, AttributeModifier.Operation.ADD_VALUE));
   }

   /** Takes the bonus away again once Crate Luck stops running. */
   public static void clear(LivingEntity entity) {
      if (entity.level().isClientSide()) {
         return;
      }
      AttributeInstance luck = entity.getAttribute(Attributes.LUCK);
      if (luck == null) {
         return;
      }
      AttributeModifier existing = luck.getModifier(MODIFIER_ID);
      if (existing != null) {
         luck.removeModifier(existing);
      }
   }
}
