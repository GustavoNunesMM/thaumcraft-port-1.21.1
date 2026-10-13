package thaumcraft.common.blocks.world.plants;

import com.google.common.collect.UnmodifiableIterator;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumFacing.Axis;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import thaumcraft.api.blocks.BlocksTC;
import thaumcraft.common.blocks.BlockTC;

public class BlockLogsTC extends BlockTC {
   public static final PropertyEnum AXIS = PropertyEnum.create("axis", Axis.class);

   public BlockLogsTC(String name) {
      super(Material.WOOD, name);
      this.setHarvestLevel("axe", 0);
      this.setHardness(2.0F);
      this.setResistance(5.0F);
      this.setSoundType(SoundType.WOOD);
      this.setDefaultState(this.blockState.getBaseState().withProperty(AXIS, Axis.Y));
   }

   public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing side, float hitX, float hitY, float hitZ, int metadata, EntityLivingBase entity) {
      return super.getStateForPlacement(world, pos, side, hitX, hitY, hitZ, metadata, entity).withProperty(AXIS, side.getAxis());
   }

   public int getLightValue(IBlockState state, IBlockAccess world, BlockPos pos) {
      return state.getBlock() == BlocksTC.logSilverwood ? 5 : super.getLightValue(state, world, pos);
   }

   public boolean rotateBlock(World world, BlockPos pos, EnumFacing axis) {
      IBlockState state = world.getBlockState(pos);
      UnmodifiableIterator var5 = state.getProperties().keySet().iterator();

      while (var5.hasNext()) {
         IProperty<?> prop = (IProperty<?>)var5.next();
         if (prop.getName().equals("axis")) {
            world.setBlockState(pos, state.cycleProperty(prop));
            return true;
         }
      }

      return false;
   }

   protected ItemStack getSilkTouchDrop(IBlockState state) {
      return new ItemStack(Item.getItemFromBlock(this));
   }

   public IBlockState getStateFromMeta(int meta) {
      int axis = meta % 3;
      return this.getDefaultState().withProperty(AXIS, Axis.values()[axis]);
   }

   public int getMetaFromState(IBlockState state) {
      return ((Axis)state.getValue(AXIS)).ordinal();
   }

   protected BlockStateContainer createBlockState() {
      return new BlockStateContainer(this, new IProperty[]{AXIS});
   }

   public boolean canSustainLeaves(IBlockState state, IBlockAccess world, BlockPos pos) {
      return true;
   }

   public boolean isWood(IBlockAccess world, BlockPos pos) {
      return true;
   }

   public void breakBlock(World worldIn, BlockPos pos, IBlockState state) {
      byte b0 = 4;
      int i = b0 + 1;
      if (worldIn.isAreaLoaded(pos.add(-i, -i, -i), pos.add(i, i, i))) {
         for (BlockPos blockpos1 : BlockPos.getAllInBox(pos.add(-b0, -b0, -b0), pos.add(b0, b0, b0))) {
            IBlockState iblockstate1 = worldIn.getBlockState(blockpos1);
            if (iblockstate1.getBlock().isLeaves(iblockstate1, worldIn, blockpos1)) {
               iblockstate1.getBlock().beginLeavesDecay(iblockstate1, worldIn, blockpos1);
            }
         }
      }
   }

   public int getFlammability(IBlockAccess world, BlockPos pos, EnumFacing face) {
      return 5;
   }

   public int getFireSpreadSpeed(IBlockAccess world, BlockPos pos, EnumFacing face) {
      return 5;
   }
}
