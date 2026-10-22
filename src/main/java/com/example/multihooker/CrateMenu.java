package com.example.multihooker;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * The loot window a crate opens: a chest-style 3x9 grid holding the rolled
 * loot, with the player's own inventory underneath. The odds panel is drawn
 * beside it by {@code client.CrateScreen}.
 *
 * <p>The grid is an ordinary container, so taking loot out is just moving it to
 * the inventory (or shift-clicking). Whatever is left in the grid when the
 * window closes is handed to the player, so loot cannot be lost by walking away.
 */
public class CrateMenu extends AbstractContainerMenu {
   public static final int LOOT_SLOTS = 27;
   private static final int LOOT_ROWS = 3;
   private static final int LOOT_COLUMNS = 9;
   private static final int COLUMN_PITCH = 18;

   /** Slot layout, identical to a chest so the background lines up. */
   private static final int LOOT_X = 8;
   private static final int LOOT_Y = 18;
   private static final int INVENTORY_X = 8;
   private static final int INVENTORY_Y = 84;
   private static final int HOTBAR_Y = 142;

   private final Container loot;
   private final String tierKey;
   private final BlockPos pos;
   /** true when the grid is an opened crate's own storage, which must outlive the window. */
   private final boolean keepContents;
   private boolean emptied;

   /** Server side: the loot has already been rolled and is passed in. */
   public CrateMenu(int id, Inventory playerInventory, Container loot, String tierKey, BlockPos pos) {
      this(id, playerInventory, loot, tierKey, pos, false);
   }

   /**
    * Server side, storage flavour: the container is an already-opened crate's own
    * inventory. {@code keepContents} stops {@link #removed(Player)} from handing the
    * grid back to the player, so the crate is real storage rather than a one-shot.
    */
   public CrateMenu(int id, Inventory playerInventory, Container loot, String tierKey, BlockPos pos, boolean keepContents) {
      super(MultiHookerRegistry.CRATE_MENU.get(), id);
      this.loot = loot;
      this.tierKey = tierKey;
      this.pos = pos;
      this.keepContents = keepContents;
      loot.startOpen(playerInventory.player);
      // Raise the crate's lid while this window is up. Server side only: the
      // client learns about it through the block event the entity sends.
      if (!playerInventory.player.level().isClientSide()) {
         if (playerInventory.player.level().getBlockEntity(pos) instanceof CrateBlockEntity crate) {
            crate.addViewer();
         }
      }

      for (int row = 0; row < LOOT_ROWS; row++) {
         for (int column = 0; column < LOOT_COLUMNS; column++) {
            this.addSlot(new Slot(loot, column + row * LOOT_COLUMNS, LOOT_X + column * COLUMN_PITCH, LOOT_Y + row * COLUMN_PITCH));
         }
      }

      for (int row = 0; row < 3; row++) {
         for (int column = 0; column < LOOT_COLUMNS; column++) {
            this.addSlot(new Slot(playerInventory, column + row * 9 + 9, INVENTORY_X + column * COLUMN_PITCH, INVENTORY_Y + row * COLUMN_PITCH));
         }
      }

      for (int column = 0; column < LOOT_COLUMNS; column++) {
         this.addSlot(new Slot(playerInventory, column, INVENTORY_X + column * COLUMN_PITCH, HOTBAR_Y));
      }
   }

   /** Client side: rebuilt from the tier key + position the crate sent along. */
   public CrateMenu(int id, Inventory playerInventory, FriendlyByteBuf data) {
      this(id, playerInventory, new SimpleContainer(LOOT_SLOTS), data.readUtf(), data.readBlockPos());
   }

   public String getTierKey() {
      return this.tierKey;
   }

   public BlockPos getPos() {
      return this.pos;
   }

   /**
    * The window closes once the crate block is gone, but not merely because it
    * turned into its empty form - that happens the instant it is opened, while
    * the window is still on screen.
    */
   @Override
   public boolean stillValid(Player player) {
      return player.level().getBlockState(this.pos).getBlock() instanceof AbstractCrateBlock;
   }

   @Override
   public void removed(Player player) {
      super.removed(player);
      this.loot.stopOpen(player);
      // The window is gone, so the lid goes back down. Server side only, and it
      // must still happen if the crate block itself was broken in the meantime.
      if (!player.level().isClientSide() && player.level().getBlockEntity(this.pos) instanceof CrateBlockEntity crate) {
         crate.removeViewer();
      }
      // Anything the player left behind is given back rather than deleted. Runs
      // server-side only (the client's copy of the grid is not authoritative), and
      // never for an opened crate's storage, where the grid is the point.
      if (!player.level().isClientSide() && !this.emptied && !this.keepContents) {
         this.emptied = true;
         clearContainer(player, this.loot);
      }
   }

   @Override
   public ItemStack quickMoveStack(Player player, int index) {
      Slot slot = this.slots.get(index);
      if (slot == null || !slot.hasItem()) {
         return ItemStack.EMPTY;
      }

      ItemStack stack = slot.getItem();
      ItemStack copy = stack.copy();
      if (index < LOOT_SLOTS) {
         if (!this.moveItemStackTo(stack, LOOT_SLOTS, this.slots.size(), true)) {
            return ItemStack.EMPTY;
         }
      } else if (!this.moveItemStackTo(stack, 0, LOOT_SLOTS, false)) {
         return ItemStack.EMPTY;
      }

      if (stack.isEmpty()) {
         slot.setByPlayer(ItemStack.EMPTY);
      } else {
         slot.setChanged();
      }

      return copy;
   }
}
