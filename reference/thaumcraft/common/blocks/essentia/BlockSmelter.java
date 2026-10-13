package thaumcraft.common.blocks.essentia;

import java.util.Random;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import thaumcraft.Thaumcraft;
import thaumcraft.api.aura.AuraHelper;
import thaumcraft.common.blocks.BlockTCDevice;
import thaumcraft.common.blocks.IBlockEnabled;
import thaumcraft.common.blocks.IBlockFacingHorizontal;
import thaumcraft.common.lib.utils.BlockStateUtils;
import thaumcraft.common.tiles.essentia.TileSmelter;

public class BlockSmelter extends BlockTCDevice implements IBlockEnabled, IBlockFacingHorizontal {
   public BlockSmelter(String name) {
      super(Material.IRON, TileSmelter.class, name);
      this.setSoundType(SoundType.METAL);
      IBlockState bs = this.blockState.getBaseState();
      bs.withProperty(IBlockFacingHorizontal.FACING, EnumFacing.NORTH);
      bs.withProperty(IBlockEnabled.ENABLED, false);
      this.setDefaultState(bs);
   }

   @Override
   public void onBlockAdded(World worldIn, BlockPos pos, IBlockState state) {
   }

   @Override
   public IBlockState getStateForPlacement(World worldIn, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
      IBlockState bs = this.getDefaultState();
      bs = bs.withProperty(IBlockFacingHorizontal.FACING, placer.getHorizontalFacing().getOpposite());
      return bs.withProperty(IBlockEnabled.ENABLED, false);
   }

   @Override
   public void neighborChanged(IBlockState state, World worldIn, BlockPos pos, Block blockIn, BlockPos pos2) {
      TileEntity te = worldIn.getTileEntity(pos);
      if (te != null && te instanceof TileSmelter) {
         ((TileSmelter)te).checkNeighbours();
      }
   }

   public boolean onBlockActivated(
      World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ
   ) {
      if (!world.isRemote && !player.isSneaking()) {
         player.openGui(Thaumcraft.instance, 9, world, pos.getX(), pos.getY(), pos.getZ());
      }

      return true;
   }

   public int getLightValue(IBlockState state, IBlockAccess world, BlockPos pos) {
      return BlockStateUtils.isEnabled(world.getBlockState(pos).getBlock().getMetaFromState(world.getBlockState(pos)))
         ? 13
         : super.getLightValue(state, world, pos);
   }

   public boolean hasComparatorInputOverride(IBlockState state) {
      return true;
   }

   @Override
   public int damageDropped(IBlockState state) {
      return 0;
   }

   public int getComparatorInputOverride(IBlockState state, World world, BlockPos pos) {
      TileEntity te = world.getTileEntity(pos);
      return te != null && te instanceof IInventory ? Container.calcRedstoneFromInventory((IInventory)te) : 0;
   }

   public static void setFurnaceState(World world, BlockPos pos, boolean state) {
      if (state != BlockStateUtils.isEnabled(world.getBlockState(pos).getBlock().getMetaFromState(world.getBlockState(pos)))) {
         TileEntity tileentity = world.getTileEntity(pos);
         keepInventory = true;
         world.setBlockState(pos, world.getBlockState(pos).withProperty(IBlockEnabled.ENABLED, state), 3);
         world.setBlockState(pos, world.getBlockState(pos).withProperty(IBlockEnabled.ENABLED, state), 3);
         if (tileentity != null) {
            tileentity.validate();
            world.setTileEntity(pos, tileentity);
         }

         keepInventory = false;
      }
   }

   @Override
   public void breakBlock(World worldIn, BlockPos pos, IBlockState state) {
      TileEntity tileentity = worldIn.getTileEntity(pos);
      if (tileentity instanceof TileSmelter && !worldIn.isRemote && ((TileSmelter)tileentity).vis > 0) {
         int ess = ((TileSmelter)tileentity).vis;
         AuraHelper.polluteAura(worldIn, pos, ess, true);
      }

      super.breakBlock(worldIn, pos, state);
   }

   @SideOnly(Side.CLIENT)
   public void randomDisplayTick(IBlockState state, World w, BlockPos pos, Random r) {
      if (BlockStateUtils.isEnabled(state)) {
         float f = pos.getX() + 0.5F;
         float f1 = pos.getY() + 0.2F + r.nextFloat() * 5.0F / 16.0F;
         float f2 = pos.getZ() + 0.5F;
         float f3 = 0.52F;
         float f4 = r.nextFloat() * 0.5F - 0.25F;
         if (BlockStateUtils.getFacing(state) == EnumFacing.WEST) {
            w.spawnParticle(EnumParticleTypes.SMOKE_NORMAL, f - f3, f1, f2 + f4, 0.0, 0.0, 0.0, new int[0]);
            w.spawnParticle(EnumParticleTypes.FLAME, f - f3, f1, f2 + f4, 0.0, 0.0, 0.0, new int[0]);
         }

         if (BlockStateUtils.getFacing(state) == EnumFacing.EAST) {
            w.spawnParticle(EnumParticleTypes.SMOKE_NORMAL, f + f3, f1, f2 + f4, 0.0, 0.0, 0.0, new int[0]);
            w.spawnParticle(EnumParticleTypes.FLAME, f + f3, f1, f2 + f4, 0.0, 0.0, 0.0, new int[0]);
         }

         if (BlockStateUtils.getFacing(state) == EnumFacing.NORTH) {
            w.spawnParticle(EnumParticleTypes.SMOKE_NORMAL, f + f4, f1, f2 - f3, 0.0, 0.0, 0.0, new int[0]);
            w.spawnParticle(EnumParticleTypes.FLAME, f + f4, f1, f2 - f3, 0.0, 0.0, 0.0, new int[0]);
         }

         if (BlockStateUtils.getFacing(state) == EnumFacing.SOUTH) {
            w.spawnParticle(EnumParticleTypes.SMOKE_NORMAL, f + f4, f1, f2 + f3, 0.0, 0.0, 0.0, new int[0]);
            w.spawnParticle(EnumParticleTypes.FLAME, f + f4, f1, f2 + f3, 0.0, 0.0, 0.0, new int[0]);
         }
      }
   }
}
