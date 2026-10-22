package com.example.multihooker.treasure;

import com.example.multihooker.MultiHookerRegistry;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

/**
 * Crate Potion: a drink that grants Crate Luck for 5:00.
 * The buff is {@link MultiHookerRegistry#CRATE_LUCK}, and it is live:
 * {@code CrateBlock} adds {@code Config.CRATE_LUCK_BONUS} to the opening roll
 * for as long as the effect is active.
 */
public class CratePotionItem extends Item {
   private static final int EFFECT_DURATION = 4800; // 4:00
   private static final int COOLDOWN_TICKS = 20;

   public CratePotionItem() {
      super(new Properties().stacksTo(16).food(new FoodProperties.Builder()
         .nutrition(0)
         .saturationModifier(0.0F)
         .alwaysEdible()
         .build()));
   }

   @Override
   public UseAnim getUseAnimation(ItemStack stack) {
      return UseAnim.DRINK;
   }

   @Override
   public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
      if (!level.isClientSide) {
         entity.addEffect(new MobEffectInstance(MultiHookerRegistry.CRATE_LUCK, EFFECT_DURATION, 0));
         // Belt and braces: MobEffectEvent.Added normally does this, but applying here too
         // means the bonus lands even if that event is ever skipped.
         CrateLuckBonus.apply(entity);
         if (entity instanceof Player player) {
            player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
            if (player instanceof ServerPlayer serverPlayer) {
               serverPlayer.displayClientMessage(
                  Component.translatable("message.multihooker.crate_luck"), true);
            }
         }
      }
      return super.finishUsingItem(stack, level, entity);
   }

   @Override
   public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
      super.appendHoverText(stack, context, tooltip, flag);
      tooltip.add(Component.translatable("tooltip.multihooker.crate_potion"));
   }
}
