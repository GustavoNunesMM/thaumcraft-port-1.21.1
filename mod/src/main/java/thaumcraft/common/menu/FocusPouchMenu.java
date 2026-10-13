package thaumcraft.common.menu;

import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import thaumcraft.common.items.casters.FocusItem;
import thaumcraft.common.items.casters.FocusPouchItem;
import thaumcraft.common.registers.ModCasters;

/** ContainerFocusPouch: the 18 focus slots of the pouch held in hand; the pouch's own hotbar slot is locked. */
public class FocusPouchMenu extends AbstractContainerMenu {
   private final Player player;
   private final InteractionHand hand;
   /** Hotbar slot of the pouch (main hand), -1 for the off hand. */
   public final int pouchSlot;
   private final int blockSlot;
   private boolean loading;
   public final SimpleContainer input = new SimpleContainer(FocusPouchItem.SIZE) {
      @Override
      public void setChanged() {
         super.setChanged();
         save();
      }
   };

   public FocusPouchMenu(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
      this(id, inv, buf.readBoolean() ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND, buf.readVarInt());
   }

   public FocusPouchMenu(int id, Inventory inv, InteractionHand hand, int pouchSlot) {
      super(ModCasters.FOCUS_POUCH_MENU.get(), id);
      this.player = inv.player;
      this.hand = hand;
      this.pouchSlot = pouchSlot;
      for (int a = 0; a < FocusPouchItem.SIZE; a++) {
         addSlot(new Slot(input, a, 37 + a % 6 * 18, 51 + a / 6 * 18) {
            @Override
            public boolean mayPlace(ItemStack stack) {
               return stack.getItem() instanceof FocusItem;
            }
         });
      }
      for (int i = 0; i < 3; i++) {
         for (int j = 0; j < 9; j++) addSlot(new Slot(inv, j + i * 9 + 9, 8 + j * 18, 151 + i * 18));
      }
      for (int i = 0; i < 9; i++) addSlot(new Slot(inv, i, 8 + i * 18, 209));
      this.blockSlot = pouchSlot >= 0 ? FocusPouchItem.SIZE + 27 + pouchSlot : -1;
      if (!player.level().isClientSide) {
         loading = true;
         NonNullList<ItemStack> list = FocusPouchItem.getInventory(pouch());
         for (int a = 0; a < list.size(); a++) input.setItem(a, list.get(a));
         loading = false;
      }
   }

   private ItemStack pouch() {
      return player.getItemInHand(hand);
   }

   /** Writes the slots back into the pouch (server side, on every change). */
   private void save() {
      if (loading || player.level().isClientSide) return;
      ItemStack pouch = pouch();
      if (!(pouch.getItem() instanceof FocusPouchItem)) return;
      NonNullList<ItemStack> list = NonNullList.withSize(FocusPouchItem.SIZE, ItemStack.EMPTY);
      for (int a = 0; a < list.size(); a++) list.set(a, input.getItem(a).copy());
      FocusPouchItem.setInventory(pouch, list);
   }

   @Override
   public boolean stillValid(Player player) {
      return pouch().getItem() instanceof FocusPouchItem;
   }

   @Override
   public void clicked(int slotId, int button, ClickType type, Player player) {
      if (slotId == blockSlot && blockSlot >= 0) return;
      if (type == ClickType.SWAP && pouchSlot >= 0 && button == pouchSlot) return;
      super.clicked(slotId, button, type, player);
   }

   @Override
   public ItemStack quickMoveStack(Player player, int slot) {
      if (slot == blockSlot) return ItemStack.EMPTY;
      ItemStack stack = ItemStack.EMPTY;
      Slot slotObject = slots.get(slot);
      if (slotObject.hasItem()) {
         ItemStack stackInSlot = slotObject.getItem();
         stack = stackInSlot.copy();
         if (slot < FocusPouchItem.SIZE) {
            if (!moveItemStackTo(stackInSlot, FocusPouchItem.SIZE, slots.size(), true)) return ItemStack.EMPTY;
         } else if (!(stackInSlot.getItem() instanceof FocusItem) || !moveItemStackTo(stackInSlot, 0, FocusPouchItem.SIZE, false)) {
            return ItemStack.EMPTY;
         }
         if (stackInSlot.isEmpty()) slotObject.setByPlayer(ItemStack.EMPTY);
         else slotObject.setChanged();
      }
      return stack;
   }

   @Override
   public void removed(Player player) {
      super.removed(player);
      save();
   }
}
