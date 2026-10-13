package thaumcraft.common.container;

import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventoryChangedListener;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.item.ItemStack;

public class InventoryHandMirror extends InventoryBasic {
   Container container;

   public InventoryHandMirror(IInventoryChangedListener listener) {
      super("container.handmirror", false, 1);
      this.addInventoryChangeListener(listener);
      this.container = (Container)listener;
   }

   public void setInventorySlotContents(int index, ItemStack stack) {
      super.setInventorySlotContents(index, stack);
      if (!stack.isEmpty()) {
         this.container.onCraftMatrixChanged(this);
      }
   }
}
