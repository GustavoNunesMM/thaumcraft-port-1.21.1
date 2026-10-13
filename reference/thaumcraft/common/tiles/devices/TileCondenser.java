package thaumcraft.common.tiles.devices;

import java.util.ArrayList;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.IEssentiaTransport;
import thaumcraft.api.aura.AuraHelper;
import thaumcraft.api.blocks.BlocksTC;
import thaumcraft.client.fx.FXDispatcher;
import thaumcraft.common.blocks.devices.BlockCondenserLattice;
import thaumcraft.common.lib.utils.BlockStateUtils;
import thaumcraft.common.tiles.TileThaumcraft;

public class TileCondenser extends TileThaumcraft implements ITickable, IEssentiaTransport {
   private int essentia;
   private int flux;
   private int MAX = 100;
   private int count = 0;
   private ArrayList<Long> history = new ArrayList<>();
   private ArrayList<Long> blockList = new ArrayList<>();
   private ArrayList<Long> uncloggedList = new ArrayList<>();
   public float latticeCount = -1.0F;
   public int interval = 0;
   public int cost = 0;

   @Override
   public void readSyncNBT(NBTTagCompound nbttagcompound) {
      this.essentia = nbttagcompound.getShort("essentia");
      this.flux = nbttagcompound.getShort("flux");
   }

   @Override
   public NBTTagCompound writeSyncNBT(NBTTagCompound nbttagcompound) {
      nbttagcompound.setShort("essentia", (short)this.essentia);
      nbttagcompound.setShort("flux", (short)this.flux);
      return nbttagcompound;
   }

   public void update() {
      if (this.latticeCount < 0.0F) {
         this.triggerCheck();
      }

      this.count++;
      if (BlockStateUtils.isEnabled(this.getBlockMetadata()) && this.latticeCount > 0.0F) {
         if (this.world.isRemote) {
            if (this.essentia > 0 && this.uncloggedList.size() > 0 && this.count % Math.max(3, this.interval / 50) == 0) {
               BlockPos p = BlockPos.fromLong(this.uncloggedList.get(this.world.rand.nextInt(this.uncloggedList.size())));
               if (p != null) {
                  FXDispatcher.INSTANCE
                     .spark(
                        p.getX() + 0.5,
                        p.getY() + 0.5,
                        p.getZ() + 0.5,
                        4.5F + this.world.rand.nextFloat(),
                        0.33F + this.world.rand.nextFloat() * 0.66F,
                        0.33F + this.world.rand.nextFloat() * 0.66F,
                        0.33F + this.world.rand.nextFloat() * 0.66F,
                        0.8F
                     );
               }
            }
         } else {
            if (this.count % 5 == 0 && this.essentia < this.MAX) {
               this.fill();
            }

            if (this.interval > 0
               && this.essentia >= this.cost
               && this.flux < this.MAX
               && this.count % this.interval == 0
               && AuraHelper.getFlux(this.getWorld(), this.getPos()) >= 1.0F) {
               AuraHelper.drainFlux(this.getWorld(), this.getPos(), 1.0F, false);
               this.essentia = this.essentia - this.cost;
               this.flux++;
               if (this.world.rand.nextInt(50) == 0) {
                  this.makeLatticeDirty();
               }

               this.syncTile(false);
               this.markDirty();
            }
         }
      }
   }

   private void makeLatticeDirty() {
      if (this.uncloggedList.size() > 0) {
         int q = this.world.rand.nextInt(this.uncloggedList.size());
         if (q == 0) {
            q = this.world.rand.nextInt(this.uncloggedList.size());
         }

         BlockPos p = BlockPos.fromLong(this.uncloggedList.get(q));
         if (p != null) {
            IBlockState bs = this.world.getBlockState(p);
            if (bs.getBlock() == BlocksTC.condenserlattice) {
               this.world.setBlockState(p, BlocksTC.condenserlatticeDirty.getDefaultState(), 3);
               ((BlockCondenserLattice)bs.getBlock()).triggerUpdate(this.world, p);
            }
         }
      }
   }

   private void fill() {
      EnumFacing[] var1 = EnumFacing.HORIZONTALS;
      int var2 = var1.length;
      int var3 = 0;

      while (true) {
         label38: {
            if (var3 < var2) {
               EnumFacing face = var1[var3];
               TileEntity te = ThaumcraftApiHelper.getConnectableTile(this.world, this.pos, face);
               if (te == null) {
                  break label38;
               }

               IEssentiaTransport ic = (IEssentiaTransport)te;
               Aspect ta = null;
               if (!ic.canOutputTo(face.getOpposite())) {
                  return;
               }

               if (ic.getEssentiaAmount(face.getOpposite()) > 0
                  && ic.getSuctionAmount(face.getOpposite()) < this.getSuctionAmount(face)
                  && this.getSuctionAmount(face) >= ic.getMinimumSuction()) {
                  ta = ic.getEssentiaType(face.getOpposite());
               }

               if (ta == null) {
                  break label38;
               }

               if (ta != Aspect.FLUX) {
                  this.essentia = this.essentia + ic.takeEssentia(ta, 1, face.getOpposite());
               } else {
                  this.makeLatticeDirty();
               }

               this.syncTile(false);
               this.markDirty();
               if (this.essentia < this.MAX) {
                  break label38;
               }
            }

            return;
         }

         var3++;
      }
   }

   public void triggerCheck() {
      this.history.clear();
      this.blockList.clear();
      this.uncloggedList.clear();
      this.latticeCount = 0.0F;
      this.interval = 0;
      this.performCheck(this.pos, true, false);
      this.history.clear();
      if (this.latticeCount <= 0.0F) {
         this.latticeCount = 0.0F;
      } else {
         if (this.latticeCount > 40.0F) {
            this.latticeCount = 40.0F;
         }

         this.interval = Math.round(600.0F - this.latticeCount * 15.0F);
         if (this.interval < 5) {
            this.interval = 5;
         }

         this.cost = (int)(4.0 + Math.sqrt(this.blockList.size()));
      }
   }

   private void performCheck(BlockPos pos, boolean skip, boolean clogged) {
      if (!(this.latticeCount < 0.0F)) {
         this.history.add(pos.toLong());
         boolean found = false;
         int sides = 0;

         for (EnumFacing face : EnumFacing.VALUES) {
            if (!skip || face == EnumFacing.UP) {
               BlockPos p2 = pos.offset(face);
               IBlockState bs = this.world.getBlockState(p2);
               boolean lattice = bs.getBlock() == BlocksTC.condenserlattice;
               boolean latticeDirty = bs.getBlock() == BlocksTC.condenserlatticeDirty;
               if (skip && latticeDirty) {
                  clogged = true;
               }

               if (lattice || latticeDirty) {
                  sides++;
               }

               if (!this.history.contains(p2.toLong())) {
                  if (face == EnumFacing.DOWN && this.world.getBlockState(p2).getBlock() == BlocksTC.condenser) {
                     this.latticeCount = -99.0F;
                     return;
                  }

                  if (this.getPos().getY() < p2.getY()
                     && !(this.getPos().distanceSq(p2) > 74.0)
                     && (lattice || latticeDirty)) {
                     this.blockList.add(p2.toLong());
                     if (lattice) {
                        this.uncloggedList.add(p2.toLong());
                     }

                     found = true;
                     this.performCheck(p2, false, clogged || latticeDirty);
                     if (skip) {
                        break;
                     }
                  }
               }
            }
         }

         if (found && !clogged) {
            float f = 1.0F - 0.15F * sides;
            this.latticeCount += f;
         }
      }
   }

   @Override
   public boolean isConnectable(EnumFacing face) {
      return face != EnumFacing.UP;
   }

   @Override
   public boolean canInputFrom(EnumFacing face) {
      return face != EnumFacing.UP && face != EnumFacing.DOWN;
   }

   @Override
   public boolean canOutputTo(EnumFacing face) {
      return face == EnumFacing.DOWN;
   }

   @Override
   public void setSuction(Aspect aspect, int amount) {
   }

   @Override
   public Aspect getSuctionType(EnumFacing face) {
      return null;
   }

   @Override
   public int getSuctionAmount(EnumFacing face) {
      return face != EnumFacing.DOWN && this.essentia < this.MAX ? 128 : 0;
   }

   @Override
   public int takeEssentia(Aspect aspect, int amount, EnumFacing face) {
      int amt = !this.canOutputTo(face) || aspect != null && aspect != Aspect.FLUX ? 0 : Math.min(amount, this.flux);
      if (amt > 0) {
         this.flux -= amt;
         this.syncTile(false);
         this.markDirty();
      }

      return amt;
   }

   @Override
   public int addEssentia(Aspect aspect, int amount, EnumFacing face) {
      int amt = this.canInputFrom(face) ? Math.min(amount, this.MAX - this.essentia) : 0;
      if (amt > 0) {
         this.syncTile(false);
         this.markDirty();
      }

      return amt;
   }

   @Override
   public Aspect getEssentiaType(EnumFacing face) {
      return Aspect.FLUX;
   }

   @Override
   public int getEssentiaAmount(EnumFacing face) {
      return this.flux;
   }

   @Override
   public int getMinimumSuction() {
      return 0;
   }
}
