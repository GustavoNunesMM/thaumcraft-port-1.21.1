package thaumcraft.common.tiles.devices;

import java.util.ArrayList;
import java.util.WeakHashMap;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import thaumcraft.common.blocks.IBlockEnabled;
import thaumcraft.common.blocks.devices.BlockArcaneEarToggle;
import thaumcraft.common.lib.utils.BlockStateUtils;

public class TileArcaneEar extends TileEntity implements ITickable {
   public byte note = 0;
   public byte tone = 0;
   public int redstoneSignal = 0;
   public static WeakHashMap<Integer, ArrayList<Integer[]>> noteBlockEvents = new WeakHashMap<>();

   public NBTTagCompound writeToNBT(NBTTagCompound par1NBTTagCompound) {
      super.writeToNBT(par1NBTTagCompound);
      par1NBTTagCompound.setByte("note", this.note);
      par1NBTTagCompound.setByte("tone", this.tone);
      return par1NBTTagCompound;
   }

   public void readFromNBT(NBTTagCompound par1NBTTagCompound) {
      super.readFromNBT(par1NBTTagCompound);
      this.note = par1NBTTagCompound.getByte("note");
      this.tone = par1NBTTagCompound.getByte("tone");
      if (this.note < 0) {
         this.note = 0;
      }

      if (this.note > 24) {
         this.note = 24;
      }
   }

   public void update() {
      if (!this.world.isRemote) {
         if (this.redstoneSignal > 0) {
            this.redstoneSignal--;
            if (this.redstoneSignal == 0) {
               EnumFacing facing = BlockStateUtils.getFacing(this.getBlockMetadata()).getOpposite();
               TileEntity tileentity = this.world.getTileEntity(this.pos);
               this.world
                  .setBlockState(this.pos, this.world.getBlockState(this.pos).withProperty(IBlockEnabled.ENABLED, false), 3);
               if (tileentity != null) {
                  tileentity.validate();
                  this.world.setTileEntity(this.pos, tileentity);
               }

               this.world.notifyNeighborsOfStateChange(this.pos, this.getBlockType(), true);
               this.world.notifyNeighborsOfStateChange(this.pos.offset(facing), this.getBlockType(), true);
               IBlockState state = this.world.getBlockState(this.pos);
               this.world.markAndNotifyBlock(this.pos, this.world.getChunk(this.pos), state, state, 3);
            }
         }

         ArrayList<Integer[]> nbe = noteBlockEvents.get(this.world.provider.getDimension());
         if (nbe != null) {
            for (Integer[] dat : nbe) {
               if (dat[3] == this.tone
                  && dat[4] == this.note
                  && this.getDistanceSq(dat[0].intValue() + 0.5, dat[1].intValue() + 0.5, dat[2].intValue() + 0.5) <= 4096.0) {
                  EnumFacing facing = BlockStateUtils.getFacing(this.getBlockMetadata()).getOpposite();
                  this.triggerNote(this.world, this.pos, true);
                  TileEntity tileentity = this.world.getTileEntity(this.pos);
                  IBlockState state = this.world.getBlockState(this.pos);
                  if (this.getBlockType() instanceof BlockArcaneEarToggle) {
                     this.world.setBlockState(this.pos, state.withProperty(IBlockEnabled.ENABLED, !BlockStateUtils.isEnabled(state)), 3);
                  } else {
                     this.redstoneSignal = 10;
                     this.world.setBlockState(this.pos, state.withProperty(IBlockEnabled.ENABLED, true), 3);
                  }

                  if (tileentity != null) {
                     tileentity.validate();
                     this.world.setTileEntity(this.pos, tileentity);
                  }

                  this.world.notifyNeighborsOfStateChange(this.pos, this.getBlockType(), true);
                  this.world.notifyNeighborsOfStateChange(this.pos.offset(facing), this.getBlockType(), true);
                  IBlockState state2 = this.world.getBlockState(this.pos);
                  this.world.markAndNotifyBlock(this.pos, this.world.getChunk(this.pos), state2, state2, 3);
                  break;
               }
            }
         }
      }
   }

   public void updateTone() {
      try {
         EnumFacing facing = BlockStateUtils.getFacing(this.getBlockMetadata()).getOpposite();
         IBlockState iblockstate = this.world.getBlockState(this.pos.offset(facing));
         Material material = iblockstate.getMaterial();
         this.tone = 0;
         if (material == Material.ROCK) {
            this.tone = 1;
         }

         if (material == Material.SAND) {
            this.tone = 2;
         }

         if (material == Material.GLASS) {
            this.tone = 3;
         }

         if (material == Material.WOOD) {
            this.tone = 4;
         }

         Block block = iblockstate.getBlock();
         if (block == Blocks.CLAY) {
            this.tone = 5;
         }

         if (block == Blocks.GOLD_BLOCK) {
            this.tone = 6;
         }

         if (block == Blocks.WOOL) {
            this.tone = 7;
         }

         if (block == Blocks.PACKED_ICE) {
            this.tone = 8;
         }

         if (block == Blocks.BONE_BLOCK) {
            this.tone = 9;
         }

         this.markDirty();
      } catch (Exception var5) {
      }
   }

   public void changePitch() {
      this.note = (byte)((this.note + 1) % 25);
      this.markDirty();
   }

   public void triggerNote(World world, BlockPos pos, boolean sound) {
      byte i = -1;
      if (sound) {
         EnumFacing facing = BlockStateUtils.getFacing(this.getBlockMetadata()).getOpposite();
         IBlockState iblockstate = world.getBlockState(pos.offset(facing));
         Material material = iblockstate.getMaterial();
         i = 0;
         if (material == Material.ROCK) {
            i = 1;
         }

         if (material == Material.SAND) {
            i = 2;
         }

         if (material == Material.GLASS) {
            i = 3;
         }

         if (material == Material.WOOD) {
            i = 4;
         }

         Block block = iblockstate.getBlock();
         if (block == Blocks.CLAY) {
            i = 5;
         }

         if (block == Blocks.GOLD_BLOCK) {
            i = 6;
         }

         if (block == Blocks.WOOL) {
            i = 7;
         }

         if (block == Blocks.PACKED_ICE) {
            i = 8;
         }

         if (block == Blocks.BONE_BLOCK) {
            i = 9;
         }
      }

      world.addBlockEvent(pos, this.getBlockType(), i, this.note);
   }
}
