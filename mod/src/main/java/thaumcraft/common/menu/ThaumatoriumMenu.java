package thaumcraft.common.menu;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import thaumcraft.common.blocks.devices.ThaumatoriumBlockEntity;
import thaumcraft.common.registers.ModDevices;

/** ContainerThaumatorium: the catalyst slot. Recipes are picked with ThaumatoriumSelectPayload. */
public class ThaumatoriumMenu extends AbstractContainerMenu {
   public final ThaumatoriumBlockEntity thaumatorium;
   private final Player player;

   public ThaumatoriumMenu(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
      this(id, inv, (ThaumatoriumBlockEntity) inv.player.level().getBlockEntity(buf.readBlockPos()));
   }

   public ThaumatoriumMenu(int id, Inventory inv, ThaumatoriumBlockEntity te) {
      super(ModDevices.THAUMATORIUM_MENU.get(), id);
      this.thaumatorium = te;
      this.player = inv.player;
      addSlot(new Slot(te != null ? te : new SimpleContainer(1), 0, 55, 24));
      DeviceMenus.addPlayerSlots(this::addSlot, inv, 8, 135);
      if (te != null) te.updateRecipes(player);
   }

   @Override
   public void broadcastChanges() {
      super.broadcastChanges();
      if (thaumatorium != null) thaumatorium.updateRecipes(player);
   }

   @Override
   public boolean stillValid(Player player) {
      return thaumatorium != null && thaumatorium.stillValid(player);
   }

   @Override
   public ItemStack quickMoveStack(Player player, int index) {
      ItemStack itemstack = ItemStack.EMPTY;
      Slot slot = slots.get(index);
      if (slot.hasItem()) {
         ItemStack stack = slot.getItem();
         itemstack = stack.copy();
         if (index != 0) {
            if (!moveItemStackTo(stack, 0, 1, false)) return ItemStack.EMPTY;
         } else if (!moveItemStackTo(stack, 1, 37, false)) {
            return ItemStack.EMPTY;
         }
         if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
         else slot.setChanged();
         if (stack.getCount() == itemstack.getCount()) return ItemStack.EMPTY;
         slot.onTake(player, stack);
      }
      return itemstack;
   }
}
