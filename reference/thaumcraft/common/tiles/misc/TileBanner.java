package thaumcraft.common.tiles.misc;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.blocks.basic.BlockBannerTC;
import thaumcraft.common.tiles.TileThaumcraft;

public class TileBanner extends TileThaumcraft {
   private byte facing = 0;
   private Aspect aspect = null;
   private boolean onWall = false;

   @SideOnly(Side.CLIENT)
   public AxisAlignedBB getRenderBoundingBox() {
      return new AxisAlignedBB(
         this.getPos().getX(),
         this.getPos().getY() - 1,
         this.getPos().getZ(),
         this.getPos().getX() + 1,
         this.getPos().getY() + 2,
         this.getPos().getZ() + 1
      );
   }

   public byte getBannerFacing() {
      return this.facing;
   }

   public void setBannerFacing(byte face) {
      this.facing = face;
      this.markDirty();
   }

   public boolean getWall() {
      return this.onWall;
   }

   public void setWall(boolean b) {
      this.onWall = b;
      this.markDirty();
   }

   @Override
   public void readSyncNBT(NBTTagCompound nbttagcompound) {
      this.facing = nbttagcompound.getByte("facing");
      String as = nbttagcompound.getString("aspect");
      if (as != null && as.length() > 0) {
         this.setAspect(Aspect.getAspect(as));
      } else {
         this.aspect = null;
      }

      this.onWall = nbttagcompound.getBoolean("wall");
   }

   @Override
   public NBTTagCompound writeSyncNBT(NBTTagCompound nbttagcompound) {
      nbttagcompound.setByte("facing", this.facing);
      nbttagcompound.setString("aspect", this.getAspect() == null ? "" : this.getAspect().getTag());
      nbttagcompound.setBoolean("wall", this.onWall);
      return nbttagcompound;
   }

   public Aspect getAspect() {
      return this.aspect;
   }

   public void setAspect(Aspect aspect) {
      this.aspect = aspect;
   }

   @SideOnly(Side.CLIENT)
   public int getColor() {
      return this.getBlockType() != null && this.getBlockType() instanceof BlockBannerTC && ((BlockBannerTC)this.getBlockType()).dye != null
         ? ((BlockBannerTC)this.getBlockType()).dye.getColorValue()
         : -1;
   }
}
