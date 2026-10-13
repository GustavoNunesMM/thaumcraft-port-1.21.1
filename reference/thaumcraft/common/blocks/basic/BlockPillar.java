package thaumcraft.common.blocks.basic;

import java.util.Random;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.EnumPushReaction;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumFacing.Axis;
import net.minecraft.util.EnumFacing.Plane;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import thaumcraft.api.blocks.BlocksTC;
import thaumcraft.common.blocks.BlockTC;

public class BlockPillar extends BlockTC {
   public static final PropertyDirection FACING = PropertyDirection.create("facing", Plane.HORIZONTAL);
   private final Random rand = new Random();

   public BlockPillar(String name) {
      super(Material.ROCK, name);
      this.setHardness(2.5F);
      this.setSoundType(SoundType.STONE);
      IBlockState bs = this.blockState.getBaseState();
      bs.withProperty(FACING, EnumFacing.NORTH);
      this.setDefaultState(bs);
   }

   public EnumPushReaction getPushReaction(IBlockState state) {
      return EnumPushReaction.BLOCK;
   }

   public boolean isOpaqueCube(IBlockState state) {
      return false;
   }

   public BlockFaceShape getBlockFaceShape(IBlockAccess worldIn, IBlockState state, BlockPos pos, EnumFacing face) {
      return BlockFaceShape.UNDEFINED;
   }

   public boolean isFullCube(IBlockState state) {
      return false;
   }

   public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
      return new AxisAlignedBB(0.0, 0.0, 0.0, 1.0, 0.5, 1.0);
   }

   public AxisAlignedBB getCollisionBoundingBox(IBlockState blockState, IBlockAccess worldIn, BlockPos pos) {
      return new AxisAlignedBB(0.0, 0.0, 0.0, 1.0, 2.0, 1.0);
   }

   public IBlockState getStateForPlacement(World worldIn, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
      IBlockState bs = this.blockState.getBaseState();
      bs.withProperty(FACING, placer.getHorizontalFacing());
      return bs;
   }

   public void onBlockPlacedBy(World worldIn, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack) {
      EnumFacing enumfacing = EnumFacing.byHorizontalIndex(MathHelper.floor(placer.rotationYaw * 4.0F / 360.0F + 0.5) & 3).getOpposite();
      state = state.withProperty(FACING, enumfacing);
      worldIn.setBlockState(pos, state, 3);
   }

   public Item getItemDropped(IBlockState state, Random rand, int fortune) {
      return Item.getItemById(0);
   }

   public void breakBlock(World worldIn, BlockPos pos, IBlockState state) {
      if (state.getBlock() == BlocksTC.pillarArcane) {
         spawnAsEntity(worldIn, pos, new ItemStack(BlocksTC.stoneArcane, 2));
      }

      if (state.getBlock() == BlocksTC.pillarAncient) {
         spawnAsEntity(worldIn, pos, new ItemStack(BlocksTC.stoneAncient, 2));
      }

      if (state.getBlock() == BlocksTC.pillarEldritch) {
         spawnAsEntity(worldIn, pos, new ItemStack(BlocksTC.stoneEldritchTile, 2));
      }

      super.breakBlock(worldIn, pos, state);
   }

   public IBlockState getStateFromMeta(int meta) {
      EnumFacing enumfacing = EnumFacing.byHorizontalIndex(meta);
      return this.getBlockState().getBaseState().withProperty(FACING, enumfacing);
   }

   public static int calcMeta(EnumFacing enumfacing) {
      if (enumfacing.getAxis() == Axis.Y) {
         enumfacing = EnumFacing.NORTH;
      }

      IBlockState state = BlocksTC.pillarArcane.getBlockState().getBaseState();
      return BlocksTC.pillarArcane.getMetaFromState(state.withProperty(FACING, enumfacing));
   }

   public int getMetaFromState(IBlockState state) {
      return ((EnumFacing)state.getValue(FACING)).getHorizontalIndex();
   }

   protected BlockStateContainer createBlockState() {
      return new BlockStateContainer(this, new IProperty[]{FACING});
   }
}
