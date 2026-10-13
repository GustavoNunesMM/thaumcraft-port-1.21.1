package thaumcraft.common.blocks.devices;

import java.util.Random;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import thaumcraft.client.fx.FXDispatcher;
import thaumcraft.common.blocks.BlockTCDevice;
import thaumcraft.common.blocks.IBlockEnabled;
import thaumcraft.common.blocks.IBlockFacing;
import thaumcraft.common.lib.utils.BlockStateUtils;
import thaumcraft.common.lib.utils.Utils;
import thaumcraft.common.tiles.devices.TileVisGenerator;

public class BlockVisGenerator extends BlockTCDevice implements IBlockFacing, IBlockEnabled {
   public BlockVisGenerator() {
      super(Material.WOOD, TileVisGenerator.class, "vis_generator");
      this.setSoundType(SoundType.WOOD);
   }

   public boolean isOpaqueCube(IBlockState state) {
      return false;
   }

   public boolean isFullCube(IBlockState state) {
      return false;
   }

   @Override
   public int damageDropped(IBlockState state) {
      return 0;
   }

   public BlockFaceShape getBlockFaceShape(IBlockAccess worldIn, IBlockState state, BlockPos pos, EnumFacing face) {
      return BlockFaceShape.UNDEFINED;
   }

   @Override
   public IBlockState getStateForPlacement(World worldIn, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
      for (EnumFacing face : EnumFacing.VALUES) {
         TileEntity tileentity = worldIn.getTileEntity(pos.offset(face));
         if (tileentity != null && tileentity.hasCapability(CapabilityEnergy.ENERGY, face.getOpposite())) {
            IEnergyStorage capability = (IEnergyStorage)tileentity.getCapability(CapabilityEnergy.ENERGY, face.getOpposite());
            if (capability.canReceive()) {
               IBlockState bs = this.getDefaultState();
               bs = bs.withProperty(IBlockFacing.FACING, face);
               return bs.withProperty(IBlockEnabled.ENABLED, true);
            }
         }
      }

      return super.getStateForPlacement(worldIn, pos, facing, hitX, hitY, hitZ, meta, placer);
   }

   @SideOnly(Side.CLIENT)
   public void randomDisplayTick(IBlockState state, World world, BlockPos pos, Random rand) {
      Block block = state.getBlock();
      if (block.hasTileEntity(state)) {
         TileEntity tileentity = world.getTileEntity(pos);
         if (tileentity != null) {
            EnumFacing face = BlockStateUtils.getFacing(state);
            if (tileentity.hasCapability(CapabilityEnergy.ENERGY, face)) {
               IEnergyStorage capability = (IEnergyStorage)tileentity.getCapability(CapabilityEnergy.ENERGY, face);
               if (capability.getEnergyStored() > 0) {
                  double x = face.getXOffset() == 0 ? rand.nextGaussian() * 0.1 : face.getXOffset() * 0.1;
                  double y = face.getYOffset() == 0 ? rand.nextGaussian() * 0.1 : face.getYOffset() * 0.1;
                  double z = face.getZOffset() == 0 ? rand.nextGaussian() * 0.1 : face.getZOffset() * 0.1;
                  FXDispatcher.INSTANCE
                     .spark(
                        pos.getX() + 0.5 + x,
                        pos.getY() + 0.5 + y,
                        pos.getZ() + 0.5 + z,
                        0.66F + rand.nextFloat(),
                        0.65F + rand.nextFloat() * 0.1F,
                        1.0F,
                        1.0F,
                        0.8F
                     );
               }
            }
         }
      }
   }

   public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
      return Utils.rotateBlockAABB(new AxisAlignedBB(0.25, 0.0, 0.25, 0.75, 0.875, 0.75), BlockStateUtils.getFacing(this.getMetaFromState(state)));
   }
}
