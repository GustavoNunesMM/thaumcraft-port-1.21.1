package thaumcraft.common.tiles.misc;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;

public class TileMemory extends TileEntity {
   public IBlockState oldblock = Blocks.AIR.getDefaultState();
   public NBTTagCompound tileEntityCompound;

   public TileMemory() {
   }

   public TileMemory(IBlockState bi) {
      this.oldblock = bi;
   }

   public void readFromNBT(NBTTagCompound nbttagcompound) {
      super.readFromNBT(nbttagcompound);
      Block b = Block.getBlockById(nbttagcompound.getInteger("oldblock"));
      int meta = nbttagcompound.getInteger("oldmeta");
      this.oldblock = b.getStateFromMeta(meta);
   }

   public NBTTagCompound writeToNBT(NBTTagCompound nbttagcompound) {
      super.writeToNBT(nbttagcompound);
      nbttagcompound.setInteger("oldblock", Block.getIdFromBlock(this.oldblock.getBlock()));
      nbttagcompound.setInteger("oldmeta", this.oldblock.getBlock().getMetaFromState(this.oldblock));
      return nbttagcompound;
   }
}
