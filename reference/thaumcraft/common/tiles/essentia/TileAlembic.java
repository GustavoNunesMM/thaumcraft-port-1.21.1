package thaumcraft.common.tiles.essentia;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IAspectContainer;
import thaumcraft.api.aspects.IEssentiaTransport;
import thaumcraft.common.tiles.TileThaumcraft;

public class TileAlembic extends TileThaumcraft implements IAspectContainer, IEssentiaTransport {
   public Aspect aspect;
   public Aspect aspectFilter = null;
   public int amount = 0;
   public int maxAmount = 128;
   public int facing = EnumFacing.DOWN.ordinal();
   public boolean aboveFurnace = false;
   EnumFacing fd = null;

   @Override
   public AspectList getAspects() {
      return this.aspect != null ? new AspectList().add(this.aspect, this.amount) : new AspectList();
   }

   @Override
   public void setAspects(AspectList aspects) {
   }

   @SideOnly(Side.CLIENT)
   public AxisAlignedBB getRenderBoundingBox() {
      return new AxisAlignedBB(
         this.getPos().getX() - 0.1,
         this.getPos().getY() - 0.1,
         this.getPos().getZ() - 0.1,
         this.getPos().getX() + 1.1,
         this.getPos().getY() + 1.1,
         this.getPos().getZ() + 1.1
      );
   }

   @Override
   public void readSyncNBT(NBTTagCompound nbttagcompound) {
      this.facing = nbttagcompound.getByte("facing");
      this.aspectFilter = Aspect.getAspect(nbttagcompound.getString("AspectFilter"));
      String tag = nbttagcompound.getString("aspect");
      if (tag != null) {
         this.aspect = Aspect.getAspect(tag);
      }

      this.amount = nbttagcompound.getShort("amount");
      this.fd = EnumFacing.VALUES[this.facing];
   }

   @Override
   public NBTTagCompound writeSyncNBT(NBTTagCompound nbttagcompound) {
      if (this.aspect != null) {
         nbttagcompound.setString("aspect", this.aspect.getTag());
      }

      if (this.aspectFilter != null) {
         nbttagcompound.setString("AspectFilter", this.aspectFilter.getTag());
      }

      nbttagcompound.setShort("amount", (short)this.amount);
      nbttagcompound.setByte("facing", (byte)this.facing);
      return nbttagcompound;
   }

   @Override
   public int addToContainer(Aspect tt, int am) {
      if (this.aspectFilter != null && tt != this.aspectFilter) {
         return am;
      } else {
         if (this.amount < this.maxAmount && tt == this.aspect || this.amount == 0) {
            this.aspect = tt;
            int added = Math.min(am, this.maxAmount - this.amount);
            this.amount += added;
            am -= added;
         }

         this.markDirty();
         this.syncTile(false);
         return am;
      }
   }

   @Override
   public boolean takeFromContainer(Aspect tt, int am) {
      if (this.amount == 0 || this.aspect == null) {
         this.aspect = null;
         this.amount = 0;
      }

      if (this.aspect != null && this.amount >= am && tt == this.aspect) {
         this.amount -= am;
         if (this.amount <= 0) {
            this.aspect = null;
            this.amount = 0;
         }

         this.markDirty();
         this.syncTile(false);
         return true;
      } else {
         return false;
      }
   }

   @Override
   public boolean doesContainerContain(AspectList ot) {
      return this.amount > 0 && this.aspect != null && ot.getAmount(this.aspect) > 0;
   }

   @Override
   public boolean doesContainerContainAmount(Aspect tt, int am) {
      return this.amount >= am && tt == this.aspect;
   }

   @Override
   public int containerContains(Aspect tt) {
      return tt == this.aspect ? this.amount : 0;
   }

   @Override
   public boolean doesContainerAccept(Aspect tag) {
      return true;
   }

   @Override
   public boolean takeFromContainer(AspectList ot) {
      return false;
   }

   @Override
   public boolean isConnectable(EnumFacing face) {
      return face != EnumFacing.VALUES[this.facing] && face != EnumFacing.DOWN;
   }

   @Override
   public boolean canInputFrom(EnumFacing face) {
      return false;
   }

   @Override
   public boolean canOutputTo(EnumFacing face) {
      return face != EnumFacing.VALUES[this.facing] && face != EnumFacing.DOWN;
   }

   @Override
   public void setSuction(Aspect aspect, int amount) {
   }

   @Override
   public Aspect getSuctionType(EnumFacing loc) {
      return null;
   }

   @Override
   public int getSuctionAmount(EnumFacing loc) {
      return 0;
   }

   @Override
   public Aspect getEssentiaType(EnumFacing loc) {
      return this.aspect;
   }

   @Override
   public int getEssentiaAmount(EnumFacing loc) {
      return this.amount;
   }

   @Override
   public int takeEssentia(Aspect aspect, int amount, EnumFacing face) {
      return this.canOutputTo(face) && this.takeFromContainer(aspect, amount) ? amount : 0;
   }

   @Override
   public int addEssentia(Aspect aspect, int amount, EnumFacing loc) {
      return 0;
   }

   @Override
   public int getMinimumSuction() {
      return 0;
   }

   protected static boolean processAlembics(World world, BlockPos pos, Aspect aspect) {
      int deep = 1;

      while (true) {
         TileEntity te = world.getTileEntity(pos.up(deep));
         if (te == null || !(te instanceof TileAlembic)) {
            deep = 1;

            while (true) {
               te = world.getTileEntity(pos.up(deep));
               if (te == null || !(te instanceof TileAlembic)) {
                  return false;
               }

               TileAlembic alembic = (TileAlembic)te;
               if ((alembic.aspectFilter == null || alembic.aspectFilter == aspect) && alembic.addToContainer(aspect, 1) == 0) {
                  return true;
               }

               deep++;
            }
         }

         TileAlembic alembic = (TileAlembic)te;
         if (alembic.amount > 0 && alembic.aspect == aspect && alembic.addToContainer(aspect, 1) == 0) {
            return true;
         }

         deep++;
      }
   }
}
