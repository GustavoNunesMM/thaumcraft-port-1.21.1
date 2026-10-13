package thaumcraft.common.blocks.world;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import thaumcraft.common.blocks.BlockTC;
import thaumcraft.common.lib.SoundsTC;
import thaumcraft.common.lib.utils.Utils;

public class BlockLoot extends BlockTC {
   BlockLoot.LootType type;
   Random rand = new Random();

   public BlockLoot(Material mat, String name, BlockLoot.LootType type) {
      super(mat, name);
      this.setHardness(0.15F);
      this.setResistance(0.0F);
      this.type = type;
   }

   public SoundType getSoundType() {
      return this.material == Material.WOOD ? SoundType.WOOD : SoundsTC.URN;
   }

   public boolean isOpaqueCube(IBlockState state) {
      return false;
   }

   public boolean isFullCube(IBlockState state) {
      return false;
   }

   protected boolean canSilkHarvest() {
      return true;
   }

   public boolean canHarvestBlock(IBlockAccess world, BlockPos pos, EntityPlayer player) {
      return true;
   }

   public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
      return this.getMaterial(state) == Material.ROCK
         ? new AxisAlignedBB(0.125, 0.0625, 0.125, 0.875, 0.8125, 0.875)
         : new AxisAlignedBB(0.0625, 0.0, 0.0625, 0.9375, 0.875, 0.9375);
   }

   public List<ItemStack> getDrops(IBlockAccess world, BlockPos pos, IBlockState state, int fortune) {
      ArrayList<ItemStack> ret = new ArrayList<>();
      int q = 1 + this.type.ordinal() + this.rand.nextInt(3);

      for (int a = 0; a < q; a++) {
         ItemStack is = Utils.generateLoot(this.type.ordinal(), this.rand);
         if (is != null && !is.isEmpty()) {
            ret.add(is.copy());
         }
      }

      return ret;
   }

   public static enum LootType {
      COMMON,
      UNCOMMON,
      RARE;
   }
}
