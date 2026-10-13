package thaumcraft.common.tiles.devices;

import java.util.ArrayList;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IAspectContainer;
import thaumcraft.api.items.IRechargable;
import thaumcraft.api.items.RechargeHelper;
import thaumcraft.client.fx.FXDispatcher;
import thaumcraft.common.tiles.TileThaumcraftInventory;

public class TileRechargePedestal extends TileThaumcraftInventory implements IAspectContainer {
   private static final int[] slots = new int[]{0};
   int counter = 0;

   public TileRechargePedestal() {
      super(1);
      this.syncedSlots = new int[]{0};
   }

   @SideOnly(Side.CLIENT)
   public AxisAlignedBB getRenderBoundingBox() {
      return new AxisAlignedBB(
            this.getPos().getX(),
            this.getPos().getY(),
            this.getPos().getZ(),
            this.getPos().getX() + 1,
            this.getPos().getY() + 1,
            this.getPos().getZ() + 1
         )
         .grow(2.0, 2.0, 2.0);
   }

   @Override
   public void update() {
      super.update();
      if (!this.getWorld().isRemote
         && this.counter++ % 10 == 0
         && this.getStackInSlot(0) != null
         && RechargeHelper.rechargeItem(this.getWorld(), this.getStackInSlot(0), this.pos, null, 5) > 0.0F) {
         this.syncTile(false);
         this.markDirty();
         ArrayList<Aspect> al = Aspect.getPrimalAspects();
         this.world
            .addBlockEvent(this.pos, this.getBlockType(), 5, al.get(this.getWorld().rand.nextInt(al.size())).getColor());
      }
   }

   public void setInventorySlotContentsFromInfusion(int par1, ItemStack stack2) {
      this.setInventorySlotContents(par1, stack2);
      this.markDirty();
      if (!this.world.isRemote) {
         this.syncTile(false);
      }
   }

   @Override
   public boolean isItemValidForSlot(int par1, ItemStack stack) {
      return stack.getItem() instanceof IRechargable;
   }

   @Override
   public int[] getSlotsForFace(EnumFacing side) {
      return slots;
   }

   @Override
   public boolean canInsertItem(int par1, ItemStack stack, EnumFacing par3) {
      return stack.getItem() instanceof IRechargable;
   }

   @Override
   public boolean canExtractItem(int par1, ItemStack stack2, EnumFacing par3) {
      return true;
   }

   @Override
   public AspectList getAspects() {
      ItemStack s = this.world != null && !this.world.isRemote ? this.getStackInSlot(0) : this.getSyncedStackInSlot(0);
      if (s != null && s.getItem() instanceof IRechargable) {
         float c = RechargeHelper.getCharge(s);
         return new AspectList().add(Aspect.ENERGY, Math.round(c));
      } else {
         return null;
      }
   }

   @Override
   public void setAspects(AspectList aspects) {
   }

   @Override
   public int addToContainer(Aspect tag, int amount) {
      return 0;
   }

   @Override
   public boolean takeFromContainer(Aspect tag, int amount) {
      return false;
   }

   @Override
   public boolean takeFromContainer(AspectList ot) {
      return false;
   }

   @Override
   public boolean doesContainerContainAmount(Aspect tag, int amount) {
      return false;
   }

   @Override
   public boolean doesContainerContain(AspectList ot) {
      return false;
   }

   @Override
   public int containerContains(Aspect tag) {
      return 0;
   }

   @Override
   public boolean doesContainerAccept(Aspect tag) {
      return true;
   }

   public boolean receiveClientEvent(int i, int j) {
      if (i == 5) {
         if (this.world.isRemote) {
            FXDispatcher.INSTANCE
               .visSparkle(
                  this.pos.getX() + this.getWorld().rand.nextInt(3) - this.getWorld().rand.nextInt(3),
                  this.pos.up().getY() + this.getWorld().rand.nextInt(3),
                  this.pos.getZ() + this.getWorld().rand.nextInt(3) - this.getWorld().rand.nextInt(3),
                  this.pos.getX(),
                  this.pos.up().getY(),
                  this.pos.getZ(),
                  j
               );
         }

         return true;
      } else {
         return super.receiveClientEvent(i, j);
      }
   }
}
