package thaumcraft.common.blocks.devices;

import java.util.ArrayList;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import thaumcraft.api.blocks.BlocksTC;
import thaumcraft.api.items.ItemsTC;
import thaumcraft.common.blocks.BlockTC;
import thaumcraft.common.config.ConfigItems;
import thaumcraft.common.tiles.devices.TileCondenser;

public class BlockCondenserLattice extends BlockTC {
   public static final PropertyBool NORTH = PropertyBool.create("north");
   public static final PropertyBool EAST = PropertyBool.create("east");
   public static final PropertyBool SOUTH = PropertyBool.create("south");
   public static final PropertyBool WEST = PropertyBool.create("west");
   public static final PropertyBool UP = PropertyBool.create("up");
   public static final PropertyBool DOWN = PropertyBool.create("down");
   private ArrayList<Long> history = new ArrayList<>();

   public BlockCondenserLattice(boolean dirty) {
      super(Material.IRON, dirty ? "condenser_lattice_dirty" : "condenser_lattice");
      this.setHardness(0.5F);
      this.setResistance(5.0F);
      this.setSoundType(SoundType.METAL);
      this.setLightLevel(dirty ? 0.0F : 0.33F);
      this.setDefaultState(
         this.blockState
            .getBaseState()
            .withProperty(NORTH, false)
            .withProperty(EAST, false)
            .withProperty(SOUTH, false)
            .withProperty(WEST, false)
            .withProperty(UP, false)
            .withProperty(DOWN, false)
      );
   }

   public BlockFaceShape getBlockFaceShape(IBlockAccess worldIn, IBlockState state, BlockPos pos, EnumFacing face) {
      return BlockFaceShape.UNDEFINED;
   }

   protected BlockStateContainer createBlockState() {
      return new BlockStateContainer(this, new IProperty[]{NORTH, EAST, SOUTH, WEST, UP, DOWN});
   }

   public boolean isOpaqueCube(IBlockState state) {
      return false;
   }

   public boolean isFullCube(IBlockState state) {
      return false;
   }

   public int getMetaFromState(IBlockState state) {
      return 0;
   }

   public IBlockState getActualState(IBlockState state, IBlockAccess worldIn, BlockPos pos) {
      Boolean[] cons = this.makeConnections(state, worldIn, pos);
      return state.withProperty(DOWN, cons[0])
         .withProperty(UP, cons[1])
         .withProperty(NORTH, cons[2])
         .withProperty(SOUTH, cons[3])
         .withProperty(WEST, cons[4])
         .withProperty(EAST, cons[5]);
   }

   private Boolean[] makeConnections(IBlockState state, IBlockAccess world, BlockPos pos) {
      Boolean[] cons = new Boolean[]{false, false, false, false, false, false};
      int a = 0;

      for (EnumFacing face : EnumFacing.VALUES) {
         Block b = world.getBlockState(pos.offset(face)).getBlock();
         if (b instanceof BlockCondenserLattice || face == EnumFacing.DOWN && b == BlocksTC.condenser) {
            cons[a] = true;
         }

         a++;
      }

      return cons;
   }

   public void onBlockAdded(World worldIn, BlockPos pos, IBlockState state) {
      super.onBlockAdded(worldIn, pos, state);
      this.triggerUpdate(worldIn, pos);
   }

   public void neighborChanged(IBlockState state, World worldIn, BlockPos pos, Block blockIn, BlockPos fromPos) {
      super.neighborChanged(state, worldIn, pos, blockIn, fromPos);
      if (blockIn == BlocksTC.condenserlattice || blockIn == BlocksTC.condenserlatticeDirty || blockIn == BlocksTC.condenser) {
         this.triggerUpdate(worldIn, pos);
      }
   }

   public boolean onBlockActivated(
      World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ
   ) {
      if (worldIn.isRemote) {
         return true;
      } else {
         if (state.getBlock() == BlocksTC.condenserlatticeDirty && playerIn.getHeldItem(hand).getItem() == ItemsTC.filter) {
            playerIn.getHeldItem(hand).shrink(1);
            if (worldIn.rand.nextBoolean()) {
               worldIn.spawnEntity(
                  new EntityItem(
                     worldIn,
                     pos.getX() + 0.5F + facing.getXOffset() / 3.0F,
                     pos.getY() + 0.5F,
                     pos.getZ() + 0.5F + facing.getZOffset() / 3.0F,
                     ConfigItems.FLUX_CRYSTAL.copy()
                  )
               );
            }

            worldIn.playSound(
               null,
               pos,
               SoundEvents.ENTITY_ITEM_PICKUP,
               SoundCategory.BLOCKS,
               0.2F,
               ((worldIn.rand.nextFloat() - worldIn.rand.nextFloat()) * 0.7F + 1.0F) * 1.6F
            );
            worldIn.setBlockState(pos, BlocksTC.condenserlattice.getDefaultState(), 3);
            IBlockState state2 = worldIn.getBlockState(pos);
            if (state2.getBlock() instanceof BlockCondenserLattice) {
               ((BlockCondenserLattice)state2.getBlock()).triggerUpdate(worldIn, pos);
            }
         }

         return super.onBlockActivated(worldIn, pos, state, playerIn, hand, facing, hitX, hitY, hitZ);
      }
   }

   public void onBlockHarvested(World worldIn, BlockPos pos, IBlockState state, EntityPlayer player) {
      super.onBlockHarvested(worldIn, pos, state, player);
      this.triggerUpdate(worldIn, pos);
   }

   public void triggerUpdate(World world, BlockPos pos) {
      this.history.clear();
      BlockPos p = this.processUpdate(world, pos);
      if (p == null || p.distanceSq(pos) > 74.0) {
         this.dropBlockAsItem(world, pos, this.getDefaultState(), 0);
         world.setBlockToAir(pos);
      }

      this.history.clear();
   }

   private BlockPos processUpdate(World world, BlockPos pos) {
      this.history.add(pos.toLong());

      for (EnumFacing face : EnumFacing.VALUES) {
         BlockPos p2 = pos.offset(face);
         if (!this.history.contains(p2.toLong())) {
            Block b = world.getBlockState(p2).getBlock();
            if (b instanceof BlockCondenserLattice) {
               BlockPos pp = this.processUpdate(world, p2);
               if (pp != null) {
                  return pp;
               }
            }

            if (face == EnumFacing.DOWN && b == BlocksTC.condenser) {
               TileEntity te = world.getTileEntity(p2);
               if (te != null && te instanceof TileCondenser) {
                  ((TileCondenser)te).latticeCount = -1.0F;
               }

               return p2;
            }
         }
      }

      return null;
   }

   public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
      float minx = 0.3125F;
      float maxx = 0.6875F;
      float miny = 0.3125F;
      float maxy = 0.6875F;
      float minz = 0.3125F;
      float maxz = 0.6875F;
      EnumFacing fd = null;

      for (int side = 0; side < 6; side++) {
         fd = EnumFacing.VALUES[side];
         Block b = source.getBlockState(pos.offset(fd)).getBlock();
         if (b instanceof BlockCondenserLattice || fd == EnumFacing.DOWN && b == BlocksTC.condenser) {
            switch (side) {
               case 0:
                  miny = 0.0F;
                  break;
               case 1:
                  maxy = 1.0F;
                  break;
               case 2:
                  minz = 0.0F;
                  break;
               case 3:
                  maxz = 1.0F;
                  break;
               case 4:
                  minx = 0.0F;
                  break;
               case 5:
                  maxx = 1.0F;
            }
         }
      }

      return new AxisAlignedBB(minx, miny, minz, maxx, maxy, maxz);
   }
}
