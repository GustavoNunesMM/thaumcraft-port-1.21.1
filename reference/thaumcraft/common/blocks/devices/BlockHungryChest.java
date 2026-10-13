package thaumcraft.common.blocks.devices;

import com.google.common.collect.UnmodifiableIterator;
import java.util.Random;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.EnumFacing.Axis;
import net.minecraft.util.EnumFacing.Plane;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import thaumcraft.api.ThaumcraftInvHelper;
import thaumcraft.common.config.ConfigItems;
import thaumcraft.common.tiles.devices.TileHungryChest;

public class BlockHungryChest extends BlockContainer {
   public static final PropertyDirection FACING = PropertyDirection.create("facing", Plane.HORIZONTAL);
   private final Random rand = new Random();

   public BlockHungryChest() {
      super(Material.WOOD);
      this.setTranslationKey("hungry_chest");
      this.setRegistryName("thaumcraft", "hungry_chest");
      this.setDefaultState(this.blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH));
      this.setHardness(2.5F);
      this.setSoundType(SoundType.WOOD);
      this.setCreativeTab(ConfigItems.TABTC);
   }

   public BlockFaceShape getBlockFaceShape(IBlockAccess worldIn, IBlockState state, BlockPos pos, EnumFacing face) {
      return BlockFaceShape.UNDEFINED;
   }

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

   public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
      return new AxisAlignedBB(0.0625, 0.0, 0.0625, 0.9375, 0.875, 0.9375);
   }

   public AxisAlignedBB getCollisionBoundingBox(IBlockState state, IBlockAccess worldIn, BlockPos pos) {
      return new AxisAlignedBB(0.0625, 0.0, 0.0625, 0.9375, 0.875, 0.9375);
   }

   public IBlockState getStateForPlacement(World worldIn, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
      return this.getDefaultState().withProperty(FACING, placer.getHorizontalFacing());
   }

   public void onBlockPlacedBy(World worldIn, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack) {
      EnumFacing enumfacing = EnumFacing.byHorizontalIndex(MathHelper.floor(placer.rotationYaw * 4.0F / 360.0F + 0.5) & 3).getOpposite();
      state = state.withProperty(FACING, enumfacing);
      worldIn.setBlockState(pos, state, 3);
   }

   public void breakBlock(World worldIn, BlockPos pos, IBlockState state) {
      TileEntity tileentity = worldIn.getTileEntity(pos);
      if (tileentity instanceof IInventory) {
         InventoryHelper.dropInventoryItems(worldIn, pos, (IInventory)tileentity);
         worldIn.updateComparatorOutputLevel(pos, this);
      }

      super.breakBlock(worldIn, pos, state);
   }

   public boolean onBlockActivated(
      World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ
   ) {
      if (world.isRemote) {
         return true;
      } else {
         TileEntity tileentity = world.getTileEntity(pos);
         if (tileentity instanceof IInventory) {
            player.displayGUIChest((IInventory)tileentity);
         }

         return true;
      }
   }

   public void onEntityCollision(World world, BlockPos pos, IBlockState state, Entity entity) {
      Object var10 = (TileHungryChest)world.getTileEntity(pos);
      if (var10 != null) {
         if (!world.isRemote) {
            if (entity instanceof EntityItem && !entity.isDead) {
               ItemStack leftovers = ThaumcraftInvHelper.insertStackAt(world, pos, EnumFacing.UP, ((EntityItem)entity).getItem(), false);
               if (leftovers == null || leftovers.isEmpty() || leftovers.getCount() != ((EntityItem)entity).getItem().getCount()) {
                  entity.playSound(SoundEvents.ENTITY_GENERIC_EAT, 0.25F, (world.rand.nextFloat() - world.rand.nextFloat()) * 0.2F + 1.0F);
               }

               if (leftovers != null && !leftovers.isEmpty()) {
                  ((EntityItem)entity).setItem(leftovers);
               } else {
                  entity.setDead();
               }

               ((TileHungryChest)var10).markDirty();
            }
         }
      }
   }

   public boolean hasComparatorInputOverride(IBlockState state) {
      return true;
   }

   public int getComparatorInputOverride(IBlockState state, World worldIn, BlockPos pos) {
      Object var10 = worldIn.getTileEntity(pos);
      return var10 instanceof TileHungryChest ? Container.calcRedstoneFromInventory((IInventory)var10) : 0;
   }

   public boolean rotateBlock(World world, BlockPos pos, EnumFacing axis) {
      IBlockState state = world.getBlockState(pos);
      UnmodifiableIterator var5 = state.getProperties().keySet().iterator();

      while (var5.hasNext()) {
         IProperty<?> prop = (IProperty<?>)var5.next();
         if (prop.getName().equals("facing")) {
            world.setBlockState(pos, state.cycleProperty(prop));
            return true;
         }
      }

      return false;
   }

   public IBlockState getStateFromMeta(int meta) {
      EnumFacing enumfacing = EnumFacing.byIndex(meta);
      if (enumfacing.getAxis() == Axis.Y) {
         enumfacing = EnumFacing.NORTH;
      }

      return this.getDefaultState().withProperty(FACING, enumfacing);
   }

   public int getMetaFromState(IBlockState state) {
      return ((EnumFacing)state.getValue(FACING)).getIndex();
   }

   protected BlockStateContainer createBlockState() {
      return new BlockStateContainer(this, new IProperty[]{FACING});
   }

   public TileEntity createNewTileEntity(World par1World, int m) {
      return new TileHungryChest();
   }

   public IBlockState withRotation(IBlockState state, Rotation rot) {
      return state.withProperty(FACING, rot.rotate((EnumFacing)state.getValue(FACING)));
   }

   public IBlockState withMirror(IBlockState state, Mirror mirrorIn) {
      return state.withRotation(mirrorIn.toRotation((EnumFacing)state.getValue(FACING)));
   }
}
