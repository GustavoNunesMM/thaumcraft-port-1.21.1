package thaumcraft.common.container;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import thaumcraft.common.container.slot.SlotTurretBasic;
import thaumcraft.common.entities.construct.EntityTurretCrossbow;

public class ContainerTurretBasic extends Container {
   private EntityTurretCrossbow turret;
   private EntityPlayer player;
   private final World theWorld;

   public ContainerTurretBasic(InventoryPlayer par1InventoryPlayer, World par3World, EntityTurretCrossbow ent) {
      this.turret = ent;
      this.theWorld = par3World;
      this.player = par1InventoryPlayer.player;
      this.addSlotToContainer(new SlotTurretBasic(this.turret, 0, 80, 29));

      for (int i = 0; i < 3; i++) {
         for (int j = 0; j < 9; j++) {
            this.addSlotToContainer(new Slot(par1InventoryPlayer, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
         }
      }

      for (int var6 = 0; var6 < 9; var6++) {
         this.addSlotToContainer(new Slot(par1InventoryPlayer, var6, 8 + var6 * 18, 142));
      }
   }

   public boolean enchantItem(EntityPlayer par1EntityPlayer, int par2) {
      return par2 == 0 ? true : super.enchantItem(par1EntityPlayer, par2);
   }

   @SideOnly(Side.CLIENT)
   public void updateProgressBar(int par1, int par2) {
   }

   public boolean canInteractWith(EntityPlayer par1EntityPlayer) {
      return true;
   }

   public ItemStack transferStackInSlot(EntityPlayer par1EntityPlayer, int slot) {
      ItemStack stack = ItemStack.EMPTY;
      Slot slotObject = (Slot)this.inventorySlots.get(slot);
      if (slotObject != null && slotObject.getHasStack()) {
         ItemStack stackInSlot = slotObject.getStack();
         stack = stackInSlot.copy();
         if (slot == 0) {
            if (!this.mergeItemStack(stackInSlot, 1, this.inventorySlots.size(), true)) {
               return ItemStack.EMPTY;
            }
         } else if (!this.mergeItemStack(stackInSlot, 0, 1, false)) {
            return ItemStack.EMPTY;
         }

         if (stackInSlot.getCount() == 0) {
            slotObject.putStack(ItemStack.EMPTY);
         } else {
            slotObject.onSlotChanged();
         }
      }

      return stack;
   }
}
