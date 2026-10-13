package thaumcraft.common.blocks.devices;

import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagInt;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import thaumcraft.common.blocks.BlockTCDevice;
import thaumcraft.common.blocks.IBlockFacing;
import thaumcraft.common.lib.SoundsTC;
import thaumcraft.common.lib.utils.BlockStateUtils;
import thaumcraft.common.tiles.devices.TileMirror;
import thaumcraft.common.tiles.devices.TileMirrorEssentia;

public class BlockMirror extends BlockTCDevice implements IBlockFacing {
   public BlockMirror(Class cls, String name) {
      super(Material.IRON, cls, name);
      this.setSoundType(SoundsTC.JAR);
      this.setHardness(0.1F);
      this.setHarvestLevel(null, 0);
      IBlockState bs = this.blockState.getBaseState();
      bs.withProperty(IBlockFacing.FACING, EnumFacing.UP);
      this.setDefaultState(bs);
   }

   public SoundType getSoundType() {
      return SoundsTC.JAR;
   }

   public BlockFaceShape getBlockFaceShape(IBlockAccess worldIn, IBlockState state, BlockPos pos, EnumFacing face) {
      return BlockFaceShape.UNDEFINED;
   }

   @Override
   public boolean canHarvestBlock(IBlockAccess world, BlockPos pos, EntityPlayer player) {
      return true;
   }

   public EnumBlockRenderType getRenderType(IBlockState state) {
      return EnumBlockRenderType.INVISIBLE;
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

   @Override
   public IBlockState getStateForPlacement(World worldIn, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
      IBlockState bs = this.getDefaultState();
      return bs.withProperty(IBlockFacing.FACING, facing);
   }

   @Override
   public void onBlockAdded(World worldIn, BlockPos pos, IBlockState state) {
   }

   @Override
   public void neighborChanged(IBlockState state, World worldIn, BlockPos pos, Block blockIn, BlockPos pos2) {
      EnumFacing d = BlockStateUtils.getFacing(state);
      if (!worldIn.getBlockState(pos.offset(d.getOpposite())).isSideSolid(worldIn, pos.offset(d.getOpposite()), d)) {
         this.dropBlockAsItem(worldIn, pos, this.getDefaultState(), 0);
         worldIn.setBlockToAir(pos);
      }
   }

   // $VF: Unable to simplify switch-on-enum, as the enum class was not able to be found.
   // Please report this to the Vineflower issue tracker, at https://github.com/Vineflower/vineflower/issues with a copy of the class file (if you have the rights to distribute it!)
   public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
      EnumFacing facing = BlockStateUtils.getFacing(state);
      switch (facing.ordinal()) {
         case 1:
            return new AxisAlignedBB(0.0, 0.0, 0.0, 1.0, 0.125, 1.0);
         case 2:
            return new AxisAlignedBB(0.0, 0.0, 0.875, 1.0, 1.0, 1.0);
         case 3:
            return new AxisAlignedBB(0.0, 0.0, 0.0, 1.0, 1.0, 0.125);
         case 4:
            return new AxisAlignedBB(0.875, 0.0, 0.0, 1.0, 1.0, 1.0);
         case 5:
            return new AxisAlignedBB(0.0, 0.0, 0.0, 0.125, 1.0, 1.0);
         default:
            return new AxisAlignedBB(0.0, 0.875, 0.0, 1.0, 1.0, 1.0);
      }
   }

   public boolean canPlaceBlockOnSide(World worldIn, BlockPos pos, EnumFacing side) {
      return worldIn.getBlockState(pos.offset(side.getOpposite())).isSideSolid(worldIn, pos.offset(side.getOpposite()), side);
   }

   public boolean onBlockActivated(
      World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ
   ) {
      return world.isRemote ? true : true;
   }

   public void dropBlockAsItemWithChance(World worldIn, BlockPos pos, IBlockState state, float chance, int fortune) {
      TileEntity te = worldIn.getTileEntity(pos);
      if (!(te instanceof TileMirror) && !(te instanceof TileMirrorEssentia)) {
         super.dropBlockAsItemWithChance(worldIn, pos, state, chance, fortune);
      } else {
         this.dropMirror(worldIn, pos, state, te);
      }
   }

   public void harvestBlock(World worldIn, EntityPlayer player, BlockPos pos, IBlockState state, TileEntity te, ItemStack stack) {
      if (!(te instanceof TileMirror) && !(te instanceof TileMirrorEssentia)) {
         super.harvestBlock(worldIn, player, pos, state, (TileEntity)null, stack);
      } else {
         this.dropMirror(worldIn, pos, state, te);
      }
   }

   public void dropMirror(World world, BlockPos pos, IBlockState state, TileEntity te) {
      if (this.tileClass == TileMirror.class) {
         TileMirror tm = (TileMirror)te;
         ItemStack drop = new ItemStack(this, 1, 0);
         if (tm != null && tm instanceof TileMirror) {
            if (tm.linked) {
               drop.setItemDamage(1);
               drop.setTagInfo("linkX", new NBTTagInt(tm.linkX));
               drop.setTagInfo("linkY", new NBTTagInt(tm.linkY));
               drop.setTagInfo("linkZ", new NBTTagInt(tm.linkZ));
               drop.setTagInfo("linkDim", new NBTTagInt(tm.linkDim));
               tm.invalidateLink();
            }

            spawnAsEntity(world, pos, drop);
         }
      } else {
         TileMirrorEssentia tm = (TileMirrorEssentia)te;
         ItemStack drop = new ItemStack(this, 1, 0);
         if (tm != null && tm instanceof TileMirrorEssentia) {
            if (tm.linked) {
               drop.setItemDamage(1);
               drop.setTagInfo("linkX", new NBTTagInt(tm.linkX));
               drop.setTagInfo("linkY", new NBTTagInt(tm.linkY));
               drop.setTagInfo("linkZ", new NBTTagInt(tm.linkZ));
               drop.setTagInfo("linkDim", new NBTTagInt(tm.linkDim));
               tm.invalidateLink();
            }

            spawnAsEntity(world, pos, drop);
         }
      }
   }

   public void onEntityCollision(World world, BlockPos pos, IBlockState state, Entity entity) {
      if (!world.isRemote
         && this.tileClass == TileMirror.class
         && entity instanceof EntityItem
         && !entity.isDead
         && ((EntityItem)entity).timeUntilPortal == 0) {
         TileMirror taf = (TileMirror)world.getTileEntity(pos);
         if (taf != null) {
            taf.transport((EntityItem)entity);
         }
      }

      super.onEntityCollision(world, pos, state, entity);
   }
}
