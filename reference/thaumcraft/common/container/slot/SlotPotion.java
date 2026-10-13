package thaumcraft.common.container.slot;

import net.minecraft.init.Items;
import net.minecraft.init.PotionTypes;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionType;
import net.minecraft.potion.PotionUtils;

public class SlotPotion extends Slot {
   int limit = 64;

   public SlotPotion(IInventory par2IInventory, int par3, int par4, int par5) {
      super(par2IInventory, par3, par4, par5);
   }

   public SlotPotion(int limit, IInventory par2IInventory, int par3, int par4, int par5) {
      super(par2IInventory, par3, par4, par5);
      this.limit = limit;
   }

   public boolean isItemValid(ItemStack stack) {
      return stack != null && !stack.isEmpty() && isValidPotion(stack);
   }

   public static boolean isValidPotion(ItemStack stack) {
      if (stack.getItem() == Items.POTIONITEM || stack.getItem() == Items.LINGERING_POTION || stack.getItem() == Items.SPLASH_POTION) {
         try {
            PotionType potion = PotionUtils.getPotionFromItem(stack);
            return potion != null
               && potion != PotionTypes.WATER
               && potion != PotionTypes.AWKWARD
               && potion != PotionTypes.EMPTY
               && potion != PotionTypes.MUNDANE
               && potion != PotionTypes.THICK;
         } catch (Exception var2) {
         }
      }

      return false;
   }

   public int getSlotStackLimit() {
      return this.limit;
   }
}
