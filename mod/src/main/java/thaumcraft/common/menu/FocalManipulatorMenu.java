package thaumcraft.common.menu;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import thaumcraft.common.blocks.crafting.FocalManipulatorBlockEntity;
import thaumcraft.common.items.casters.FocusItem;
import thaumcraft.common.registers.ModCasters;
import thaumcraft.common.registers.ModSounds;

/** ContainerFocalManipulator: the focus slot; the player's inventory sits in a column to the left (hotbar as a 3x3). */
public class FocalManipulatorMenu extends AbstractContainerMenu {
   public final FocalManipulatorBlockEntity table;

   public FocalManipulatorMenu(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
      this(id, inv, (FocalManipulatorBlockEntity) inv.player.level().getBlockEntity(buf.readBlockPos()));
   }

   public FocalManipulatorMenu(int id, Inventory inv, FocalManipulatorBlockEntity table) {
      super(ModCasters.FOCAL_MANIPULATOR_MENU.get(), id);
      this.table = table;
      addSlot(new Slot(table.inventory, 0, 31, 191) {
         @Override
         public boolean mayPlace(ItemStack stack) {
            return stack.getItem() instanceof FocusItem;
         }

         @Override
         public int getMaxStackSize() {
            return 1;
         }
      });
      for (int i = 0; i < 3; i++) {
         for (int j = 0; j < 9; j++) addSlot(new Slot(inv, j + i * 9 + 9, i * 18 - 62, 64 + j * 18));
      }
      for (int i = 0; i < 3; i++) {
         for (int j = 0; j < 3; j++) addSlot(new Slot(inv, i + j * 3, i * 18 - 62, j * 18 + 7));
      }
   }

   /** Button 0: start crafting (enchantItem in 1.12). */
   @Override
   public boolean clickMenuButton(Player p, int button) {
      if (button == 0 && !table.startCraft(p)) {
         p.level().playSound(null, table.getBlockPos(), ModSounds.CRAFTFAIL.get(), SoundSource.BLOCKS, 0.33F, 1.0F);
      }
      return false;
   }

   @Override
   public boolean stillValid(Player player) {
      return table.inventory.stillValid(player);
   }

   @Override
   public ItemStack quickMoveStack(Player player, int index) {
      ItemStack itemstack = ItemStack.EMPTY;
      Slot slot = slots.get(index);
      if (slot.hasItem()) {
         ItemStack itemstack1 = slot.getItem();
         itemstack = itemstack1.copy();
         if (index != 0) {
            if (itemstack1.getItem() instanceof FocusItem) {
               if (!moveItemStackTo(itemstack1, 0, 1, false)) return ItemStack.EMPTY;
            } else if (index < 28) {
               if (!moveItemStackTo(itemstack1, 28, 37, false)) return ItemStack.EMPTY;
            } else if (!moveItemStackTo(itemstack1, 1, 28, false)) {
               return ItemStack.EMPTY;
            }
         } else if (!moveItemStackTo(itemstack1, 1, 37, false)) {
            return ItemStack.EMPTY;
         }
         if (itemstack1.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
         else slot.setChanged();
         if (itemstack1.getCount() == itemstack.getCount()) return ItemStack.EMPTY;
         slot.onTake(player, itemstack1);
      }
      return itemstack;
   }
}
