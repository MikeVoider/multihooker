package com.example.multihooker.client;

import java.lang.reflect.Method;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;

/**
 * Hands MultiHooker's drops to whichever pickup notifier the pack has installed.
 *
 * <p>Notifiers work off the packet the game sends when a player walks over an item entity lying
 * on the ground. MultiHooker's {@code instantCatchPickup} option skips that entirely and puts the
 * stack straight into the inventory, so a notifier never hears about a MultiHooker drop even
 * though it happily announces everything else in the pack.
 *
 * <p>The server therefore sends an explicit payload when it hands something over, and this class
 * replays it into the notifier's own entry point.
 *
 * <p>Loot Journal is called reflectively and behind a {@link ModList} check on purpose. It is an
 * optional integration: the class is not on our compile classpath, and a hard reference would make
 * MultiHooker fail to load in any pack that does not have it. Every failure path here is silent
 * by design - a missing or renamed notifier must never interrupt a fishing session.
 */
public final class PickupNotifierBridge {
   private static final String LOOT_JOURNAL_ID = "loot_journal";
   private static final String LOOT_JOURNAL_HELPER = "dev.obscuria.lootjournal.LootJournalHelper";
   private static final String PICKUP_ITEM = "pickupItem";

   /** Resolved once; the notifier cannot appear or vanish without a restart. */
   private static Method lootJournalPickup;
   private static boolean resolved;

   private PickupNotifierBridge() {
   }

   /**
    * Announces a stack the server just handed this player directly.
    *
    * <p>Client thread only - it is called from the payload handler's work queue.
    */
   public static void announce(LocalPlayer player, ItemStack stack) {
      if (player == null || stack.isEmpty()) {
         return;
      }
      if (notifyLootJournal(player, stack)) {
         return;
      }
      // No notifier to feed. The action bar line the server sends is the fallback, and the
      // player has already got the item, so there is nothing else to do here.
   }

   private static boolean notifyLootJournal(LocalPlayer player, ItemStack stack) {
      if (!isLootJournalLoaded()) {
         return false;
      }
      try {
         if (lootJournalPickup == null) {
            Class<?> helper = Class.forName(LOOT_JOURNAL_HELPER);
            // AbstractClientPlayer, not Player: getMethod matches parameter types exactly, so
            // asking for the supertype would not find this method at all.
            lootJournalPickup = helper.getMethod(PICKUP_ITEM,
               net.minecraft.client.player.AbstractClientPlayer.class, ItemStack.class);
         }
         lootJournalPickup.invoke(null, player, stack);
         return true;
      } catch (ReflectiveOperationException | RuntimeException unavailable) {
         // A notifier that changed its API must not break the game. Give up quietly and
         // remember it, so we do not retry on every single drop.
         lootJournalPickup = null;
         MultiHookerClient.LOG.warn(
            "Loot Journal is installed but its pickup API could not be called; "
               + "MultiHooker drops will not be announced by it", unavailable);
         return false;
      }
   }

   private static boolean isLootJournalLoaded() {
      return ModList.get() != null && ModList.get().isLoaded(LOOT_JOURNAL_ID);
   }
}