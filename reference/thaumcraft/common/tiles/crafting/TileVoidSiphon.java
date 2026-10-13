package thaumcraft.common.tiles.crafting;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.Vec3d;
import thaumcraft.api.items.ItemsTC;
import thaumcraft.client.fx.FXDispatcher;
import thaumcraft.common.entities.EntityFluxRift;
import thaumcraft.common.lib.utils.BlockStateUtils;
import thaumcraft.common.lib.utils.EntityUtils;
import thaumcraft.common.tiles.TileThaumcraftInventory;

public class TileVoidSiphon extends TileThaumcraftInventory {
   private static final int[] slots = new int[]{0};
   int counter = 0;
   public int progress = 0;
   public final int PROGREQ = 2000;

   public TileVoidSiphon() {
      super(1);
   }

   @Override
   public void update() {
      super.update();
      this.counter++;
      if ((!this.getWorld().isRemote || !BlockStateUtils.isEnabled(this.getBlockMetadata()))
         && !this.getWorld().isRemote
         && BlockStateUtils.isEnabled(this.getBlockMetadata())
         && this.counter % 20 == 0
         && this.progress < 2000
         && (
            this.getStackInSlot(0).isEmpty()
               || this.getStackInSlot(0).getItem() == ItemsTC.voidSeed && this.getStackInSlot(0).getCount() < this.getStackInSlot(0).getMaxStackSize()
         )) {
         List<EntityFluxRift> frl = this.getValidRifts();
         boolean b = false;

         for (EntityFluxRift fr : frl) {
            double d = Math.sqrt(fr.getRiftSize());
            this.progress = (int)(this.progress + d);
            fr.setRiftStability((float)(fr.getRiftStability() - d / 15.0));
            if (this.world.rand.nextInt(33) == 0) {
               fr.setRiftSize(fr.getRiftSize() - 1);
            }

            b = d >= 1.0;
         }

         if (b && this.counter % 40 == 0) {
            this.world.addBlockEvent(this.pos, this.getBlockType(), 5, this.counter);
         }

         for (b = false;
            this.progress >= 2000
               && (
                  this.getStackInSlot(0).isEmpty()
                     || this.getStackInSlot(0).getItem() == ItemsTC.voidSeed && this.getStackInSlot(0).getCount() < this.getStackInSlot(0).getMaxStackSize()
               );
            b = true
         ) {
            this.progress -= 2000;
            if (this.getStackInSlot(0).isEmpty()) {
               this.setInventorySlotContents(0, new ItemStack(ItemsTC.voidSeed));
            } else {
               this.getStackInSlot(0).setCount(this.getStackInSlot(0).getCount() + 1);
            }
         }

         if (b) {
            this.syncTile(false);
            this.markDirty();
         }
      }
   }

   private List<EntityFluxRift> getValidRifts() {
      ArrayList<EntityFluxRift> ret = new ArrayList<>();

      for (EntityFluxRift fr : EntityUtils.getEntitiesInRange(this.getWorld(), this.getPos(), null, EntityFluxRift.class, 8.0)) {
         if (!fr.isDead && fr.getRiftSize() >= 2) {
            double xx = this.getPos().getX() + 0.5;
            double yy = this.getPos().getY() + 1;
            double zz = this.getPos().getZ() + 0.5;
            Vec3d v1 = new Vec3d(xx, yy, zz);
            Vec3d v2 = new Vec3d(fr.posX, fr.posY, fr.posZ);
            v1 = v1.add(v2.subtract(v1).normalize());
            if (EntityUtils.canEntityBeSeen(fr, v1.x, v1.y, v1.z)) {
               ret.add(fr);
            }
         }
      }

      return ret;
   }

   @Override
   public void readFromNBT(NBTTagCompound nbt) {
      super.readFromNBT(nbt);
      this.progress = nbt.getShort("progress");
   }

   @Override
   public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
      super.writeToNBT(nbt);
      nbt.setShort("progress", (short)this.progress);
      return nbt;
   }

   @Override
   public boolean isItemValidForSlot(int par1, ItemStack stack) {
      return stack.getItem() == ItemsTC.voidSeed;
   }

   @Override
   public int[] getSlotsForFace(EnumFacing side) {
      return slots;
   }

   @Override
   public boolean canInsertItem(int par1, ItemStack stack, EnumFacing par3) {
      return false;
   }

   @Override
   public boolean canExtractItem(int par1, ItemStack stack2, EnumFacing par3) {
      return true;
   }

   public boolean receiveClientEvent(int i, int j) {
      if (i != 5) {
         return super.receiveClientEvent(i, j);
      } else {
         if (this.world.isRemote) {
            for (EntityFluxRift fr : this.getValidRifts()) {
               FXDispatcher.INSTANCE
                  .voidStreak(
                     fr.posX,
                     fr.posY,
                     fr.posZ,
                     this.getPos().getX() + 0.5,
                     this.getPos().getY() + 0.5625F,
                     this.getPos().getZ() + 0.5,
                     j,
                     0.04F
                  );
            }
         }

         return true;
      }
   }
}
