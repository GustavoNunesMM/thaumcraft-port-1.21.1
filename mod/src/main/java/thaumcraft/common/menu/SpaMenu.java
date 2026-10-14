package thaumcraft.common.menu;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import thaumcraft.common.blocks.devices.DeviceStubs;
import thaumcraft.common.blocks.devices.SpaBlockEntity;
import thaumcraft.common.registers.ModDevices;

/** ContainerSpa: the bath salts slot; button 1 toggles mixing. */
public class SpaMenu extends AbstractContainerMenu {
   public final SpaBlockEntity spa;

   public SpaMenu(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
      this(id, inv, (SpaBlockEntity) inv.player.level().getBlockEntity(buf.readBlockPos()));
   }

   public SpaMenu(int id, Inventory inv, SpaBlockEntity spa) {
      super(ModDevices.SPA_MENU.get(), id);
      this.spa = spa;
      addSlot(new Slot(spa != null ? spa : new SimpleContainer(1), 0, 65, 31) {
         @Override
         public boolean mayPlace(ItemStack stack) {
            return DeviceStubs.isBathSalts(stack);
         }
      });
      DeviceMenus.addPlayerSlots(this::addSlot, inv, 8, 84);
   }

   @Override
   public boolean clickMenuButton(Player player, int id) {
      if (id == 1 && spa != null) {
         spa.toggleMix();
         return true;
      }
      return false;
   }

   @Override
   public boolean stillValid(Player player) {
      return spa != null && spa.stillValid(player);
   }

   @Override
   public ItemStack quickMoveStack(Player player, int index) {
      ItemStack stack = ItemStack.EMPTY;
      Slot slot = slots.get(index);
      if (slot.hasItem()) {
         ItemStack in = slot.getItem();
         stack = in.copy();
         if (index == 0) {
            if (!moveItemStackTo(in, 1, slots.size(), true)) return ItemStack.EMPTY;
         } else if (!DeviceStubs.isBathSalts(in) || !moveItemStackTo(in, 0, 1, false)) {
            return ItemStack.EMPTY;
         }
         if (in.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
         else slot.setChanged();
      }
      return stack;
   }
}
