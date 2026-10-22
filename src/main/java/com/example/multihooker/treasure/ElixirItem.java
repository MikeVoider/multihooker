package com.example.multihooker.treasure;

import com.example.multihooker.Config;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * A drinkable elixir. Effects are defined per elixir in {@link #create(String, boolean)}.
 * The unobtainable flag only adds an informational tooltip - none of these elixirs has a recipe,
 * so fishing is the only way in.
 *
 * <p>The two elixirs are deliberately unlike each other. Fortune is a vanilla status effect and
 * wears off. Vitality is not an effect at all: it banks permanent max health in
 * {@link VitalityHealth}, which is why it needs its own branch here rather than a list entry.
 */
public class ElixirItem extends Item {
   private final boolean unobtainable;
   private final List<MobEffectInstance> effects;
   private final boolean banksVitality;

   private ElixirItem(Properties properties, boolean unobtainable, List<MobEffectInstance> effects,
                      boolean banksVitality) {
      super(properties);
      this.unobtainable = unobtainable;
      this.effects = effects;
      this.banksVitality = banksVitality;
   }

   public static ElixirItem create(String name, boolean unobtainable) {
      boolean vitality = "elixir_vitality".equals(name);
      List<MobEffectInstance> effects = switch (name) {
         case "elixir_fortune" -> List.of(new MobEffectInstance(MobEffects.LUCK, 3600, 2));
         default -> List.of();
      };
      FoodProperties food = new FoodProperties.Builder().nutrition(0).saturationModifier(0.0F).alwaysEdible().build();
      return new ElixirItem(new Properties().food(food).stacksTo(16), unobtainable, effects, vitality);
   }

   /**
    * Refuses the drink once the bonus is maxed out.
    *
    * <p>Checked here rather than in {@code finishUsingItem}, because by then the bottle is already
    * being consumed and the drinking animation has played - the player would watch the potion get
    * drunk and get nothing for it. Returning FAIL stops the interaction before any of that.
    *
    * <p>Both sides run this check and both read the bonus off the synced max-health attribute, so
    * they cannot disagree and leave the client mid-drink with the server refusing.
    */
   @Override
   public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
      if (banksVitality && isMaxedOut(player)) {
         if (!level.isClientSide) {
            player.displayClientMessage(Component.translatable(
               "message.multihooker.vitality_maxed",
               VitalityHealth.bonusHearts(player)), true);
         }
         return InteractionResultHolder.fail(player.getItemInHand(hand));
      }
      return super.use(level, player, hand);
   }

   private static boolean isMaxedOut(Player player) {
      double cap = (Double)Config.MAX_HEARTS_GAINED.get();
      // A cap of 0 means the elixir is switched off, so it should not be drinkable at all.
      return VitalityHealth.bonusHearts(player) >= cap - 1.0E-6D;
   }

   @Override
   public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
      if (!level.isClientSide) {
         for (MobEffectInstance effect : effects) {
            entity.addEffect(new MobEffectInstance(effect));
         }
         if (banksVitality) {
            bankVitality(entity, level);
         }
         level.playSound(null, entity.blockPosition(), SoundEvents.GENERIC_DRINK, SoundSource.PLAYERS, 0.6F, 1.0F);
      }
      return super.finishUsingItem(stack, level, entity);
   }

   /**
    * Adds this drink's hearts to the player's permanent total and re-applies the attribute.
    *
    * <p>Current health is deliberately left alone: the bonus raises the ceiling, and whether to
    * also top the player up is a feel decision rather than a correctness one.
    */
   private static void bankVitality(LivingEntity entity, Level level) {
      MinecraftServer server = level.getServer();
      if (server == null) {
         return;
      }
      VitalityHealth data = VitalityHealth.get(server.overworld());
      double before = data.heartsOf(entity.getUUID());
      double total = data.grant(entity.getUUID(),
         (Double)Config.HEARTS_POTION_PER_DRINK.get(), (Double)Config.MAX_HEARTS_GAINED.get());
      VitalityHealth.sync(entity);

      if (entity instanceof ServerPlayer player) {
         // At the cap there is nothing to say beyond the total, so keep the message short.
         boolean capped = total - before < (Double)Config.HEARTS_POTION_PER_DRINK.get() - 1.0E-6D;
         player.displayClientMessage(Component.translatable(
            capped ? "message.multihooker.vitality_capped" : "message.multihooker.vitality_gain",
            total), true);
      }
   }

   @Override
   public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
      super.appendHoverText(stack, context, tooltip, flag);
      if (banksVitality) {
         tooltip.add(Component.translatable("tooltip.multihooker.elixir_vitality",
            Config.HEARTS_POTION_PER_DRINK.get(), Config.MAX_HEARTS_GAINED.get()));
      }
      if (unobtainable) {
         tooltip.add(Component.translatable("tooltip.multihooker.unobtainable"));
      }
   }
}
