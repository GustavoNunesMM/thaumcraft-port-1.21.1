package thaumcraft.common.blocks.basic;

import java.util.Random;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import thaumcraft.api.crafting.IInfusionStabiliserExt;
import thaumcraft.common.blocks.BlockTC;

public class BlockCandle extends BlockTC implements IInfusionStabiliserExt {
   public final EnumDyeColor dye;

   public BlockCandle(String name, EnumDyeColor dye) {
      super(Material.CIRCUITS, name);
      this.setHardness(0.1F);
      this.setSoundType(SoundType.CLOTH);
      this.setLightLevel(0.9375F);
      this.dye = dye;
   }

   public MapColor getMapColor(IBlockState state, IBlockAccess worldIn, BlockPos pos) {
      return MapColor.getBlockColor(this.dye);
   }

   public BlockFaceShape getBlockFaceShape(IBlockAccess worldIn, IBlockState state, BlockPos pos, EnumFacing face) {
      return BlockFaceShape.UNDEFINED;
   }

   public boolean canPlaceBlockAt(World par1World, BlockPos pos) {
      return par1World.isSideSolid(pos, EnumFacing.UP);
   }

   public void neighborChanged(IBlockState state, World worldIn, BlockPos pos, Block blockIn, BlockPos pos2) {
      if (!this.canPlaceBlockAt(worldIn, pos.down())) {
         this.dropBlockAsItem(worldIn, pos, state, 0);
         worldIn.setBlockToAir(pos);
      }
   }

   public boolean canPlaceBlockOnSide(World par1World, BlockPos pos, EnumFacing par5) {
      return this.canPlaceBlockAt(par1World, pos.down());
   }

   public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
      return new AxisAlignedBB(0.375, 0.0, 0.375, 0.625, 0.5, 0.625);
   }

   public boolean isSideSolid(IBlockState state, IBlockAccess world, BlockPos pos, EnumFacing side) {
      return false;
   }

   public AxisAlignedBB getCollisionBoundingBox(IBlockState state, IBlockAccess worldIn, BlockPos pos) {
      return null;
   }

   public boolean isFullCube(IBlockState state) {
      return false;
   }

   public boolean isOpaqueCube(IBlockState state) {
      return false;
   }

   public void randomDisplayTick(IBlockState state, World par1World, BlockPos pos, Random par5Random) {
      double var7 = pos.getX() + 0.5F;
      double var9 = pos.getY() + 0.7F;
      double var11 = pos.getZ() + 0.5F;
      par1World.spawnParticle(EnumParticleTypes.SMOKE_NORMAL, var7, var9, var11, 0.0, 0.0, 0.0, new int[0]);
      par1World.spawnParticle(EnumParticleTypes.FLAME, var7, var9, var11, 0.0, 0.0, 0.0, new int[0]);
   }

   @Override
   public boolean canStabaliseInfusion(World world, BlockPos pos) {
      return true;
   }

   @Override
   public float getStabilizationAmount(World world, BlockPos pos) {
      return 0.1F;
   }

   @Override
   public boolean hasSymmetryPenalty(World world, BlockPos pos1, BlockPos pos2) {
      return false;
   }

   @Override
   public float getSymmetryPenalty(World world, BlockPos pos) {
      return 0.0F;
   }
}
