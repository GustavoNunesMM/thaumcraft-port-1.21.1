package thaumcraft.common.tiles.devices;

import java.util.ArrayList;
import java.util.Collections;
import net.minecraft.block.Block;
import net.minecraft.block.IGrowable;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EnumSkyBlock;
import net.minecraftforge.fml.common.network.NetworkRegistry.TargetPoint;
import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.IEssentiaTransport;
import thaumcraft.common.blocks.IBlockEnabled;
import thaumcraft.common.lib.network.PacketHandler;
import thaumcraft.common.lib.network.fx.PacketFXBlockMist;
import thaumcraft.common.lib.utils.BlockStateUtils;
import thaumcraft.common.lib.utils.CropUtils;
import thaumcraft.common.tiles.TileThaumcraft;

public class TileLampGrowth extends TileThaumcraft implements IEssentiaTransport, ITickable {
   private boolean reserve = false;
   public int charges = -1;
   public int maxCharges = 20;
   int lx = 0;
   int ly = 0;
   int lz = 0;
   Block lid = Blocks.AIR;
   int lmd = 0;
   ArrayList<BlockPos> checklist = new ArrayList<>();
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
         if (this.charges <= 0) {
            if (this.reserve) {
               this.charges = this.maxCharges;
               this.reserve = false;
               this.markDirty();
               this.syncTile(true);
            } else if (this.drawEssentia()) {
               this.charges = this.maxCharges;
               this.markDirty();
               this.syncTile(true);
            }

            if (this.charges <= 0) {
               if (BlockStateUtils.isEnabled(this.getBlockMetadata())) {
                  this.world
                     .setBlockState(this.pos, this.world.getBlockState(this.getPos()).withProperty(IBlockEnabled.ENABLED, false), 3);
               }
            } else if (!this.gettingPower() && !BlockStateUtils.isEnabled(this.getBlockMetadata())) {
               this.world
                  .setBlockState(this.pos, this.world.getBlockState(this.getPos()).withProperty(IBlockEnabled.ENABLED, true), 3);
            }
         }

         if (!this.reserve && this.drawEssentia()) {
            this.reserve = true;
         }

         if (this.charges == 0) {
            this.charges = -1;
            this.syncTile(true);
         }

         if (!this.gettingPower() && this.charges > 0) {
            this.updatePlant();
         }
      }
   }

   boolean isPlant(BlockPos bp) {
      IBlockState b = this.world.getBlockState(bp);
      boolean flag = b.getBlock() instanceof IGrowable;
      Material mat = b.getMaterial();
      return (flag || mat == Material.CACTUS || mat == Material.PLANTS) && mat != Material.GRASS;
   }

   private void updatePlant() {
      IBlockState bs = this.world.getBlockState(new BlockPos(this.lx, this.ly, this.lz));
      if (this.lid != bs.getBlock() || this.lmd != bs.getBlock().getMetaFromState(bs)) {
         EntityPlayer p = this.world.getClosestPlayer(this.lx, this.ly, this.lz, 32.0, false);
         if (p != null) {
            PacketHandler.INSTANCE
               .sendToAllAround(
                  new PacketFXBlockMist(new BlockPos(this.lx, this.ly, this.lz), 4259648),
                  new TargetPoint(this.world.provider.getDimension(), this.lx, this.ly, this.lz, 32.0)
               );
         }

         this.lid = bs.getBlock();
         this.lmd = bs.getBlock().getMetaFromState(bs);
      }

      int distance = 6;
      if (this.checklist.size() == 0) {
         for (int a = -distance; a <= distance; a++) {
            for (int b = -distance; b <= distance; b++) {
               this.checklist.add(this.getPos().add(a, distance, b));
            }
         }

         Collections.shuffle(this.checklist, this.world.rand);
      }

      int x = this.checklist.get(0).getX();
      int y = this.checklist.get(0).getY();
      int z = this.checklist.get(0).getZ();
      this.checklist.remove(0);

      while (y >= this.pos.getY() - distance) {
         BlockPos bp = new BlockPos(x, y, z);
         if (!this.world.isAirBlock(bp)
            && this.isPlant(bp)
            && this.getDistanceSq(x + 0.5, y + 0.5, z + 0.5) < distance * distance
            && !CropUtils.isGrownCrop(this.world, bp)
            && CropUtils.doesLampGrow(this.world, bp)) {
            this.charges--;
            this.lx = x;
            this.ly = y;
            this.lz = z;
            IBlockState bs2 = this.world.getBlockState(bp);
            this.lid = bs2.getBlock();
            this.lmd = bs2.getBlock().getMetaFromState(bs2);
            this.world.scheduleUpdate(bp, this.lid, 1);
            return;
         }

         y--;
      }
   }

   @Override
   public void readSyncNBT(NBTTagCompound nbttagcompound) {
      this.reserve = nbttagcompound.getBoolean("reserve");
      this.charges = nbttagcompound.getInteger("charges");
   }

   @Override
   public NBTTagCompound writeSyncNBT(NBTTagCompound nbttagcompound) {
      nbttagcompound.setBoolean("reserve", this.reserve);
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
               && ic.takeEssentia(Aspect.PLANT, 1, BlockStateUtils.getFacing(this.getBlockMetadata()).getOpposite()) == 1) {
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
      return Aspect.PLANT;
   }

   @Override
   public int getSuctionAmount(EnumFacing face) {
      return face != BlockStateUtils.getFacing(this.getBlockMetadata()) || this.reserve && this.charges > 0 ? 0 : 128;
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
   public int takeEssentia(Aspect aspect, int amount, EnumFacing loc) {
      return 0;
   }

   @Override
   public int addEssentia(Aspect aspect, int amount, EnumFacing loc) {
      return 0;
   }
}
