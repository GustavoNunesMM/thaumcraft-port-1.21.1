package thaumcraft.common.tiles.devices;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.EnumSkyBlock;
import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.IEssentiaTransport;
import thaumcraft.common.blocks.IBlockEnabled;
import thaumcraft.common.lib.utils.BlockStateUtils;
import thaumcraft.common.tiles.TileThaumcraft;

public class TileLampFertility extends TileThaumcraft implements IEssentiaTransport, ITickable {
   public int charges = 0;
   int count = 0;
   int drawDelay = 0;

   @Override
   public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
      super.onDataPacket(net, pkt);
      if (this.world != null && this.world.isRemote) {
         this.world.checkLightFor(EnumSkyBlock.BLOCK, this.getPos());
      }
   }

   public void update() {
      if (!this.world.isRemote) {
         if (this.charges < 10) {
            if (this.drawEssentia()) {
               this.charges++;
               this.markDirty();
               this.syncTile(true);
            }

            if (this.charges <= 1) {
               if (BlockStateUtils.isEnabled(this.getBlockMetadata())) {
                  this.world
                     .setBlockState(this.pos, this.world.getBlockState(this.getPos()).withProperty(IBlockEnabled.ENABLED, false), 3);
               }
            } else if (!this.gettingPower() && !BlockStateUtils.isEnabled(this.getBlockMetadata())) {
               this.world
                  .setBlockState(this.pos, this.world.getBlockState(this.getPos()).withProperty(IBlockEnabled.ENABLED, true), 3);
            }
         }

         if (!this.gettingPower() && this.charges > 1 && this.count++ % 300 == 0) {
            this.updateAnimals();
         }
      }
   }

   private void updateAnimals() {
      int distance = 7;
      List<EntityAnimal> var5 = this.world
         .getEntitiesWithinAABB(
            EntityAnimal.class,
            new AxisAlignedBB(
                  this.pos.getX(),
                  this.pos.getY(),
                  this.pos.getZ(),
                  this.pos.getX() + 1,
                  this.pos.getY() + 1,
                  this.pos.getZ() + 1
               )
               .grow(distance, distance, distance)
         );

      for (EntityAnimal var3 : var5) {
         EntityLivingBase var4 = var3;
         if (var3.getGrowingAge() == 0 && !var3.isInLove()) {
            ArrayList<EntityAnimal> sa = new ArrayList<>();

            for (EntityLivingBase var7 : var5) {
               if (var7.getClass().equals(var4.getClass())) {
                  sa.add((EntityAnimal)var7);
               }
            }

            if (sa == null || sa.size() <= 9) {
               Iterator var22 = sa.iterator();
               EntityAnimal partner = null;

               while (var22.hasNext()) {
                  EntityAnimal var33 = (EntityAnimal)var22.next();
                  if (var33.getGrowingAge() == 0 && !var33.isInLove()) {
                     if (partner != null) {
                        this.charges -= 5;
                        var33.setInLove(null);
                        partner.setInLove(null);
                        return;
                     }

                     partner = var33;
                  }
               }
            }
         }
      }
   }

   @Override
   public void readSyncNBT(NBTTagCompound nbttagcompound) {
      this.charges = nbttagcompound.getInteger("charges");
   }

   @Override
   public NBTTagCompound writeSyncNBT(NBTTagCompound nbttagcompound) {
      nbttagcompound.setInteger("charges", this.charges);
      return nbttagcompound;
   }

   boolean drawEssentia() {
      if (++this.drawDelay % 5 != 0) {
         return false;
      } else {
         TileEntity te = ThaumcraftApiHelper.getConnectableTile(this.world, this.getPos(), BlockStateUtils.getFacing(this.getBlockMetadata()));
         if (te != null) {
            IEssentiaTransport ic = (IEssentiaTransport)te;
            if (!ic.canOutputTo(BlockStateUtils.getFacing(this.getBlockMetadata()).getOpposite())) {
               return false;
            }

            if (ic.getSuctionAmount(BlockStateUtils.getFacing(this.getBlockMetadata()).getOpposite())
                  < this.getSuctionAmount(BlockStateUtils.getFacing(this.getBlockMetadata()))
               && ic.takeEssentia(Aspect.DESIRE, 1, BlockStateUtils.getFacing(this.getBlockMetadata()).getOpposite()) == 1) {
               return true;
            }
         }

         return false;
      }
   }

   @Override
   public boolean isConnectable(EnumFacing face) {
      return face == BlockStateUtils.getFacing(this.getBlockMetadata());
   }

   @Override
   public boolean canInputFrom(EnumFacing face) {
      return face == BlockStateUtils.getFacing(this.getBlockMetadata());
   }

   @Override
   public boolean canOutputTo(EnumFacing face) {
      return false;
   }

   @Override
   public void setSuction(Aspect aspect, int amount) {
   }

   @Override
   public int getMinimumSuction() {
      return 0;
   }

   @Override
   public Aspect getSuctionType(EnumFacing face) {
      return Aspect.DESIRE;
   }

   @Override
   public int getSuctionAmount(EnumFacing face) {
      return face == BlockStateUtils.getFacing(this.getBlockMetadata()) ? 128 - this.charges * 10 : 0;
   }

   @Override
   public Aspect getEssentiaType(EnumFacing loc) {
      return null;
   }

   @Override
   public int getEssentiaAmount(EnumFacing loc) {
      return 0;
   }

   @Override
   public int takeEssentia(Aspect aspect, int amount, EnumFacing facing) {
      return 0;
   }

   @Override
   public int addEssentia(Aspect aspect, int amount, EnumFacing facing) {
      return 0;
   }
}
