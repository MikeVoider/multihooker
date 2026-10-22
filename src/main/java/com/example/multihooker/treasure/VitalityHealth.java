package com.example.multihooker.treasure;

import com.example.multihooker.MultiHookerMod;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * The max health a player has permanently gained from Elixir of Vitality.
 *
 * <p>The total lives in world save data rather than on the player entity. An attribute modifier
 * alone would be enough to hold the number, but it is the <em>displayed</em> value that matters
 * here, and a player who dies or logs out mid-session must not quietly lose hearts they already
 * drank. Keeping the authoritative total here and pushing it onto the attribute means the two can
 * be re-synced at any time - on login, on respawn, after a config change - without ever trusting
 * the entity to have remembered correctly.
 *
 * <p>Hearts and health points are kept apart deliberately: the player-facing number is hearts,
 * the attribute is half that in points. One heart is two points.
 */
public final class VitalityHealth extends SavedData {
   private static final String DATA_NAME = "multihooker_vitality";

   /**
    * One shared id for the modifier. It does not need to be per-player, because attribute
    * modifiers are stored on the player's own attribute instance.
    */
   private static final ResourceLocation MODIFIER_ID =
      ResourceLocation.fromNamespaceAndPath(MultiHookerMod.MOD_ID, "vitality_hearts");

   private static final Factory<VitalityHealth> FACTORY =
      new Factory<>(VitalityHealth::new, VitalityHealth::load, DataFixTypes.LEVEL);

   private final Map<UUID, Double> heartsByPlayer = new HashMap<>();

   public static VitalityHealth get(ServerLevel level) {
      return level.getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
   }

   private static VitalityHealth load(CompoundTag tag, HolderLookup.Provider registries) {
      VitalityHealth data = new VitalityHealth();
      CompoundTag stored = tag.getCompound("hearts");
      for (String key : stored.getAllKeys()) {
         try {
            data.heartsByPlayer.put(UUID.fromString(key), stored.getDouble(key));
         } catch (IllegalArgumentException malformedKey) {
            // A corrupt key should cost that one entry, not the whole save.
            MultiHookerMod.LOGGER.warn("Vitality: ignoring unparsable player id \"{}\"", key);
         }
      }
      return data;
   }

   @Override
   public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
      CompoundTag stored = new CompoundTag();
      for (Map.Entry<UUID, Double> entry : heartsByPlayer.entrySet()) {
         stored.putDouble(entry.getKey().toString(), entry.getValue());
      }
      tag.put("hearts", stored);
      return tag;
   }

   /** Hearts permanently gained so far. Never negative. */
   public double heartsOf(UUID player) {
      return Math.max(0.0, heartsByPlayer.getOrDefault(player, 0.0D));
   }

   /**
    * Banks one drink's worth of hearts, stopping at the cap.
    *
    * @return the new total, so the caller can tell the player what they ended up on
    */
   public double grant(UUID player, double perDrink, double cap) {
      double total = Math.min(heartsOf(player) + Math.max(0.0D, perDrink), Math.max(0.0D, cap));
      if (total != heartsOf(player)) {
         heartsByPlayer.put(player, total);
         setDirty();
      }
      return total;
   }

   /**
    * Pushes the banked total onto the player's max-health attribute.
    *
    * <p>Safe to call as often as you like: it replaces rather than adds, so re-applying after a
    * death or a relog cannot stack a second copy of the bonus.
    */
   public static void sync(LivingEntity entity) {
      if (!(entity instanceof Player player)) {
         return;
      }
      MinecraftServer server = player.level().getServer();
      if (server == null) {
         return;
      }
      ServerLevel overworld = server.overworld();
      AttributeInstance maxHealth = player.getAttribute(Attributes.MAX_HEALTH);
      if (maxHealth == null) {
         return;
      }
      double points = get(overworld).heartsOf(player.getUUID()) * 2.0D;
      maxHealth.addOrReplacePermanentModifier(
         new AttributeModifier(MODIFIER_ID, points, AttributeModifier.Operation.ADD_VALUE));
   }

   /**
    * Hearts the player is currently carrying, read back off the attribute rather than the save
    * data.
    *
    * <p>Deliberately client-safe: max health is synced to the client, so the client and the server
    * always agree on the number. That matters because {@code Item.use} runs on both sides, and if
    * they disagreed the player would start drinking and then have the server refuse it.
    */
   public static double bonusHearts(LivingEntity entity) {
      AttributeInstance maxHealth = entity.getAttribute(Attributes.MAX_HEALTH);
      if (maxHealth == null) {
         return 0.0D;
      }
      for (AttributeModifier modifier : maxHealth.getModifiers()) {
         if (modifier.is(MODIFIER_ID)) {
            return Math.max(0.0D, modifier.amount() / 2.0D);
         }
      }
      return 0.0D;
   }
}
