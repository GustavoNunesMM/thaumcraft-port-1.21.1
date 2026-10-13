package thaumcraft.common.tiles.devices;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.fml.common.FMLCommonHandler;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IAspectSource;
import thaumcraft.api.aura.AuraHelper;
import thaumcraft.common.lib.events.EssentiaHandler;
import thaumcraft.common.lib.utils.BlockStateUtils;
import thaumcraft.common.lib.utils.Utils;
import thaumcraft.common.tiles.TileThaumcraft;

public class TileMirrorEssentia extends TileThaumcraft implements IAspectSource, ITickable {
   public boolean linked = false;
   public int linkX;
   public int linkY;
   public int linkZ;
   public int linkDim;
   public EnumFacing linkedFacing = EnumFacing.DOWN;
   public int instability;
   int count = 0;
   int inc = 40;

   @Override
   public void readSyncNBT(NBTTagCompound nbttagcompound) {
      this.linked = nbttagcompound.getBoolean("linked");
      this.linkX = nbttagcompound.getInteger("linkX");
      this.linkY = nbttagcompound.getInteger("linkY");
      this.linkZ = nbttagcompound.getInteger("linkZ");
      this.linkDim = nbttagcompound.getInteger("linkDim");
      this.instability = nbttagcompound.getInteger("instability");
   }

   @Override
   public NBTTagCompound writeSyncNBT(NBTTagCompound nbttagcompound) {
      nbttagcompound.setBoolean("linked", this.linked);
      nbttagcompound.setInteger("linkX", this.linkX);
      nbttagcompound.setInteger("linkY", this.linkY);
      nbttagcompound.setInteger("linkZ", this.linkZ);
      nbttagcompound.setInteger("linkDim", this.linkDim);
      nbttagcompound.setInteger("instability", this.instability);
      return nbttagcompound;
   }

   protected void addInstability(World targetWorld, int amt) {
      this.instability += amt;
      this.markDirty();
      if (targetWorld != null) {
         TileEntity te = targetWorld.getTileEntity(new BlockPos(this.linkX, this.linkY, this.linkZ));
         if (te != null && te instanceof TileMirrorEssentia) {
            ((TileMirrorEssentia)te).instability += amt;
            if (((TileMirrorEssentia)te).instability < 0) {
               ((TileMirrorEssentia)te).instability = 0;
            }

            te.markDirty();
         }
      }
   }

   public void restoreLink() {
      if (this.isDestinationValid()) {
         World targetWorld = FMLCommonHandler.instance().getMinecraftServerInstance().getWorld(this.linkDim);
         if (targetWorld == null) {
            return;
         }

         TileEntity te = targetWorld.getTileEntity(new BlockPos(this.linkX, this.linkY, this.linkZ));
         if (te != null && te instanceof TileMirrorEssentia) {
            TileMirrorEssentia tm = (TileMirrorEssentia)te;
            tm.linked = true;
            tm.linkX = this.getPos().getX();
            tm.linkY = this.getPos().getY();
            tm.linkZ = this.getPos().getZ();
            tm.linkDim = this.world.provider.getDimension();
            tm.syncTile(false);
            this.linkedFacing = BlockStateUtils.getFacing(targetWorld.getBlockState(new BlockPos(this.linkX, this.linkY, this.linkZ)));
            this.linked = true;
            this.markDirty();
            tm.markDirty();
            this.syncTile(false);
         }
      }
   }

   public void invalidateLink() {
      World targetWorld = DimensionManager.getWorld(this.linkDim);
      if (targetWorld != null) {
         if (Utils.isChunkLoaded(targetWorld, this.linkX, this.linkZ)) {
            TileEntity te = targetWorld.getTileEntity(new BlockPos(this.linkX, this.linkY, this.linkZ));
            if (te != null && te instanceof TileMirrorEssentia) {
               TileMirrorEssentia tm = (TileMirrorEssentia)te;
               tm.linked = false;
               tm.linkedFacing = EnumFacing.DOWN;
               this.markDirty();
               tm.markDirty();
               tm.syncTile(false);
            }
         }
      }
   }

   public boolean isLinkValid() {
      if (!this.linked) {
         return false;
      } else {
         World targetWorld = DimensionManager.getWorld(this.linkDim);
         if (targetWorld == null) {
            return false;
         } else {
            TileEntity te = targetWorld.getTileEntity(new BlockPos(this.linkX, this.linkY, this.linkZ));
            if (te != null && te instanceof TileMirrorEssentia) {
               TileMirrorEssentia tm = (TileMirrorEssentia)te;
               if (!tm.linked) {
                  this.linked = false;
                  this.markDirty();
                  this.syncTile(false);
                  return false;
               } else if (tm.linkX == this.getPos().getX()
                  && tm.linkY == this.getPos().getY()
                  && tm.linkZ == this.getPos().getZ()
                  && tm.linkDim == this.world.provider.getDimension()) {
                  return true;
               } else {
                  this.linked = false;
                  this.markDirty();
                  this.syncTile(false);
                  return false;
               }
            } else {
               this.linked = false;
               this.markDirty();
               this.syncTile(false);
               return false;
            }
         }
      }
   }

   public boolean isLinkValidSimple() {
      if (!this.linked) {
         return false;
      } else {
         World targetWorld = DimensionManager.getWorld(this.linkDim);
         if (targetWorld == null) {
            return false;
         } else {
            TileEntity te = targetWorld.getTileEntity(new BlockPos(this.linkX, this.linkY, this.linkZ));
            if (te != null && te instanceof TileMirrorEssentia) {
               TileMirrorEssentia tm = (TileMirrorEssentia)te;
               return !tm.linked
                  ? false
                  : tm.linkX == this.getPos().getX()
                     && tm.linkY == this.getPos().getY()
                     && tm.linkZ == this.getPos().getZ()
                     && tm.linkDim == this.world.provider.getDimension();
            } else {
               return false;
            }
         }
      }
   }

   public boolean isDestinationValid() {
      World targetWorld = DimensionManager.getWorld(this.linkDim);
      if (targetWorld == null) {
         return false;
      } else {
         TileEntity te = targetWorld.getTileEntity(new BlockPos(this.linkX, this.linkY, this.linkZ));
         if (te != null && te instanceof TileMirrorEssentia) {
            TileMirrorEssentia tm = (TileMirrorEssentia)te;
            return !tm.isLinkValid();
         } else {
            this.linked = false;
            this.markDirty();
            this.syncTile(false);
            return false;
         }
      }
   }

   @Override
   public AspectList getAspects() {
      return null;
   }

   @Override
   public void setAspects(AspectList aspects) {
   }

   @Override
   public boolean doesContainerAccept(Aspect tag) {
      World targetWorld = DimensionManager.getWorld(this.linkDim);
      if (this.linkedFacing == EnumFacing.DOWN && targetWorld != null) {
         this.linkedFacing = BlockStateUtils.getFacing(targetWorld.getBlockState(new BlockPos(this.linkX, this.linkY, this.linkZ)));
      }

      TileEntity te = targetWorld.getTileEntity(new BlockPos(this.linkX, this.linkY, this.linkZ));
      return te != null && te instanceof TileMirrorEssentia ? EssentiaHandler.canAcceptEssentia(te, tag, this.linkedFacing, 8, true) : true;
   }

   @Override
   public int addToContainer(Aspect tag, int amount) {
      if (this.isLinkValid() && amount <= 1) {
         World targetWorld = DimensionManager.getWorld(this.linkDim);
         if (this.linkedFacing == EnumFacing.DOWN && targetWorld != null) {
            this.linkedFacing = BlockStateUtils.getFacing(targetWorld.getBlockState(new BlockPos(this.linkX, this.linkY, this.linkZ)));
         }

         TileEntity te = targetWorld.getTileEntity(new BlockPos(this.linkX, this.linkY, this.linkZ));
         if (te != null && te instanceof TileMirrorEssentia) {
            boolean b = EssentiaHandler.addEssentia(te, tag, this.linkedFacing, 8, true, 5);
            if (b) {
               this.addInstability(null, amount);
            }

            return b ? 0 : 1;
         } else {
            return amount;
         }
      } else {
         return amount;
      }
   }

   @Override
   public boolean takeFromContainer(Aspect tag, int amount) {
      if (this.isLinkValid() && amount <= 1) {
         World targetWorld = DimensionManager.getWorld(this.linkDim);
         if (this.linkedFacing == EnumFacing.DOWN && targetWorld != null) {
            this.linkedFacing = BlockStateUtils.getFacing(targetWorld.getBlockState(new BlockPos(this.linkX, this.linkY, this.linkZ)));
         }

         TileEntity te = targetWorld.getTileEntity(new BlockPos(this.linkX, this.linkY, this.linkZ));
         if (te != null && te instanceof TileMirrorEssentia) {
            boolean b = EssentiaHandler.drainEssentia(te, tag, this.linkedFacing, 8, true, 5);
            if (b) {
               this.addInstability(null, amount);
            }

            return b;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   @Override
   public boolean takeFromContainer(AspectList ot) {
      return false;
   }

   @Override
   public boolean doesContainerContainAmount(Aspect tag, int amount) {
      if (this.isLinkValid() && amount <= 1) {
         World targetWorld = DimensionManager.getWorld(this.linkDim);
         if (this.linkedFacing == EnumFacing.DOWN && targetWorld != null) {
            this.linkedFacing = BlockStateUtils.getFacing(targetWorld.getBlockState(new BlockPos(this.linkX, this.linkY, this.linkZ)));
         }

         TileEntity te = targetWorld.getTileEntity(new BlockPos(this.linkX, this.linkY, this.linkZ));
         return te != null && te instanceof TileMirrorEssentia ? EssentiaHandler.findEssentia(te, tag, this.linkedFacing, 8, true) : false;
      } else {
         return false;
      }
   }

   @Override
   public boolean doesContainerContain(AspectList ot) {
      return false;
   }

   @Override
   public int containerContains(Aspect tag) {
      return 0;
   }

   public void update() {
      if (!this.world.isRemote) {
         this.checkInstability();
         if (this.count++ % this.inc == 0) {
            if (!this.isLinkValidSimple()) {
               if (this.inc < 600) {
                  this.inc += 20;
               }

               this.restoreLink();
            } else {
               this.inc = 40;
            }
         }
      }
   }

   public void checkInstability() {
      if (this.instability > 64) {
         AuraHelper.polluteAura(this.world, this.pos, 1.0F, true);
         this.instability -= 64;
         this.markDirty();
      }

      if (this.instability > 0 && this.count % 100 == 0) {
         this.instability--;
      }
   }

   @Override
   public boolean isBlocked() {
      return false;
   }
}
