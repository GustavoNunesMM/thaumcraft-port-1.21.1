package thaumcraft.common.menu;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import thaumcraft.common.items.HandMirrorItem;
import thaumcraft.common.registers.ModDevices;

/** ContainerHandMirror: whatever goes into the slot is sent through the linked mirror. */
public class HandMirrorMenu extends AbstractContainerMenu {
   private final Player player;
   private final ItemStack mirror;
   public final int selected;
   private boolean busy;
   private final SimpleContainer input = new SimpleContainer(1) {
      @Override
      public void setChanged() {
         super.setChanged();
         slotsChanged(this);
      }
   };

   public HandMirrorMenu(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
      this(id, inv);
   }

   public HandMirrorMenu(int id, Inventory inv) {
      super(ModDevices.HAND_MIRROR_MENU.get(), id);
      this.player = inv.player;
      this.mirror = inv.getSelected();
      this.selected = inv.selected;
      addSlot(new Slot(input, 0, 80, 24));
      DeviceMenus.addPlayerSlots(this::addSlot, inv, 8, 84);
   }

   @Override
   public void slotsChanged(Container container) {
      ItemStack in = input.getItem(0);
      if (busy || in.isEmpty() || player.level().isClientSide) return;
      if (in.getItem() instanceof HandMirrorItem) return;
      busy = true;
      try {
         ItemStack is = in.copy();
         input.removeItemNoUpdate(0);
         if (!HandMirrorItem.transport(mirror, is, player)) input.setItem(0, is);
         broadcastChanges();
      } finally {
         busy = false;
      }
   }

   @Override
   public void clicked(int slotId, int button, ClickType clickType, Player player) {
      if (slotId >= 0 && slotId < slots.size() && slots.get(slotId).getItem().getItem() instanceof HandMirrorItem) return;
      if (clickType == ClickType.SWAP && button == selected) return;
      super.clicked(slotId, button, clickType, player);
   }

   @Override
   public ItemStack quickMoveStack(Player player, int index) {
      ItemStack stack = ItemStack.EMPTY;
      Slot slot = slots.get(index);
      if (slot.hasItem() && !(slot.getItem().getItem() instanceof HandMirrorItem)) {
         ItemStack in = slot.getItem();
         stack = in.copy();
         if (index == 0) {
            if (!moveItemStackTo(in, 1, slots.size(), true)) return ItemStack.EMPTY;
         } else if (!moveItemStackTo(in, 0, 1, false)) {
            return ItemStack.EMPTY;
         }
         if (in.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
         else slot.setChanged();
      }
      return stack;
   }

   @Override
   public boolean stillValid(Player player) {
      return true;
   }

   @Override
   public void removed(Player player) {
      super.removed(player);
      if (!player.level().isClientSide) clearContainer(player, input);
   }
}
