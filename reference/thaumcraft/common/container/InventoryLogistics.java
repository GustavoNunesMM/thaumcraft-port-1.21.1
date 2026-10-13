package thaumcraft.common.container;

import net.minecraft.inventory.IInventoryChangedListener;
import net.minecraft.inventory.InventoryBasic;

public class InventoryLogistics extends InventoryBasic {
   public InventoryLogistics(IInventoryChangedListener listener) {
      super("container.logistics", false, 81);
      this.addInventoryChangeListener(listener);
   }

   public int getInventoryStackLimit() {
      return Integer.MAX_VALUE;
   }
}
