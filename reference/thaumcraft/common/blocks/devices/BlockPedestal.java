package thaumcraft.common.blocks.devices;

import java.util.Random;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.EnumFacing.Plane;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import thaumcraft.api.blocks.BlocksTC;
import thaumcraft.api.crafting.IInfusionStabiliserExt;
import thaumcraft.client.fx.FXDispatcher;
import thaumcraft.common.blocks.BlockTCTile;
import thaumcraft.common.lib.utils.InventoryUtils;
import thaumcraft.common.tiles.crafting.TilePedestal;

public class BlockPedestal extends BlockTCTile implements IInfusionStabiliserExt {
   public static BlockPedestal instance;

   public BlockPedestal(String name) {
      super(Material.ROCK, TilePedestal.class, name);
      this.setSoundType(SoundType.STONE);
      instance = this;
      IBlockState bs = this.blockState.getBaseState();
      bs.withProperty(BlockInlay.CHARGE, 0);
      this.setDefaultState(bs);
   }

   public boolean isOpaqueCube(IBlockState state) {
      return false;
   }

   public boolean isFullCube(IBlockState state) {
      return false;
   }

   public BlockFaceShape getBlockFaceShape(IBlockAccess worldIn, IBlockState state, BlockPos pos, EnumFacing face) {
      return BlockFaceShape.UNDEFINED;
   }

   public boolean onBlockActivated(
      World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ
   ) {
      if (world.isRemote) {
         return true;
      } else {
         TileEntity tile = world.getTileEntity(pos);
         if (tile != null && tile instanceof TilePedestal) {
            TilePedestal ped = (TilePedestal)tile;
            if (ped.getStackInSlot(0).isEmpty()
               && !player.inventory.getCurrentItem().isEmpty()
               && player.inventory.getCurrentItem().getCount() > 0) {
               ItemStack i = player.getHeldItem(hand).copy();
               i.setCount(1);
               ped.setInventorySlotContents(0, i);
               player.getHeldItem(hand).shrink(1);
               if (player.getHeldItem(hand).getCount() == 0) {
                  player.setHeldItem(hand, ItemStack.EMPTY);
               }

               player.inventory.markDirty();
               world.playSound(
                  null,
                  pos,
                  SoundEvents.ENTITY_ITEM_PICKUP,
                  SoundCategory.BLOCKS,
                  0.2F,
                  ((world.rand.nextFloat() - world.rand.nextFloat()) * 0.7F + 1.0F) * 1.6F
               );
               return true;
            }

            if (!ped.getStackInSlot(0).isEmpty()) {
               InventoryUtils.dropItemsAtEntity(world, pos, player);
               world.playSound(
                  null,
                  pos,
                  SoundEvents.ENTITY_ITEM_PICKUP,
                  SoundCategory.BLOCKS,
                  0.2F,
                  ((world.rand.nextFloat() - world.rand.nextFloat()) * 0.7F + 1.0F) * 1.5F
               );
               return true;
            }
         }

         return super.onBlockActivated(world, pos, state, player, hand, side, hitX, hitY, hitZ);
      }
   }

   public IBlockState getStateFromMeta(int meta) {
      return this.getDefaultState().withProperty(BlockInlay.CHARGE, meta);
   }

   public int getMetaFromState(IBlockState state) {
      return (Integer)state.getValue(BlockInlay.CHARGE);
   }

   protected BlockStateContainer createBlockState() {
      return new BlockStateContainer(this, new IProperty[]{BlockInlay.CHARGE});
   }

   @SideOnly(Side.CLIENT)
   public void randomDisplayTick(IBlockState stateIn, World worldIn, BlockPos pos, Random rand) {
      int charge = (Integer)stateIn.getValue(BlockInlay.CHARGE);
      if (charge > 0) {
         FXDispatcher.INSTANCE.blockRunes2(pos.getX(), pos.getY() - 0.375, pos.getZ(), 1.0F, 0.0F, 0.0F, 10, 0.0F);
      }
   }

   public void onBlockAdded(World worldIn, BlockPos pos, IBlockState state) {
      if (!worldIn.isRemote) {
         BlockInlay.updateSurroundingInlay(worldIn, pos, state);

         for (EnumFacing enumfacing1 : Plane.HORIZONTAL) {
            BlockInlay.notifyInlayNeighborsOfStateChange(worldIn, pos.offset(enumfacing1));
         }
      }
   }

   @Override
   public void breakBlock(World worldIn, BlockPos pos, IBlockState state) {
      super.breakBlock(worldIn, pos, state);
      if (!worldIn.isRemote) {
         for (EnumFacing enumfacing : Plane.HORIZONTAL) {
            worldIn.notifyNeighborsOfStateChange(pos.offset(enumfacing), this, false);
         }

         BlockInlay.updateSurroundingInlay(worldIn, pos, state);

         for (EnumFacing enumfacing1 : Plane.HORIZONTAL) {
            BlockInlay.notifyInlayNeighborsOfStateChange(worldIn, pos.offset(enumfacing1));
         }
      }
   }

   public void neighborChanged(IBlockState state, World worldIn, BlockPos pos, Block blockIn, BlockPos fromPos) {
      if (!worldIn.isRemote) {
         BlockInlay.updateSurroundingInlay(worldIn, pos, state);
      }
   }

   @Override
   public boolean canStabaliseInfusion(World world, BlockPos pos) {
      return true;
   }

   @Override
   public float getStabilizationAmount(World world, BlockPos pos) {
      Block b = world.getBlockState(pos).getBlock();
      return b == BlocksTC.pedestalEldritch ? 0.1F : 0.0F;
   }

   @Override
   public boolean hasSymmetryPenalty(World world, BlockPos pos1, BlockPos pos2) {
      TileEntity te1 = world.getTileEntity(pos1);
      TileEntity te2 = world.getTileEntity(pos2);
      if (world.isRemote) {
         if (te1 != null && te2 != null && te1 instanceof TilePedestal && te2 instanceof TilePedestal) {
            return ((TilePedestal)te1).getSyncedStackInSlot(0).isEmpty() != ((TilePedestal)te2).getSyncedStackInSlot(0).isEmpty();
         }
      } else if (te1 != null && te2 != null && te1 instanceof TilePedestal && te2 instanceof TilePedestal) {
         return ((TilePedestal)te1).getStackInSlot(0).isEmpty() != ((TilePedestal)te2).getStackInSlot(0).isEmpty();
      }

      return false;
   }

   @Override
   public float getSymmetryPenalty(World world, BlockPos pos) {
      return 0.1F;
   }
}
