package thaumcraft.common.blocks.basic;

import java.util.Random;
import net.minecraft.block.Block;
import net.minecraft.block.BlockSlab;
import net.minecraft.block.SoundType;
import net.minecraft.block.BlockSlab.EnumBlockHalf;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import thaumcraft.common.config.ConfigItems;

public class BlockSlabTC extends BlockSlab {
   public static final PropertyEnum<BlockSlabTC.Variant> VARIANT = PropertyEnum.create("variant", BlockSlabTC.Variant.class);
   boolean wood = false;
   Block drop = null;

   protected BlockSlabTC(String name, Block b, boolean wood) {
      super(wood ? Material.WOOD : Material.ROCK);
      this.drop = b;
      this.wood = wood;
      this.setTranslationKey(name);
      this.setRegistryName("thaumcraft", name);
      IBlockState iblockstate = this.blockState.getBaseState();
      if (!this.isDouble()) {
         iblockstate = iblockstate.withProperty(HALF, EnumBlockHalf.BOTTOM);
         this.setCreativeTab(ConfigItems.TABTC);
      }

      this.setSoundType(wood ? SoundType.WOOD : SoundType.STONE);
      this.setDefaultState(iblockstate.withProperty(VARIANT, BlockSlabTC.Variant.DEFAULT));
      this.useNeighborBrightness = !this.isDouble();
   }

   public Item getItemDropped(IBlockState state, Random rand, int fortune) {
      return this.drop == null ? Item.getItemFromBlock(state.getBlock()) : Item.getItemFromBlock(this.drop);
   }

   @SideOnly(Side.CLIENT)
   public ItemStack getItem(World worldIn, BlockPos pos, IBlockState state) {
      return new ItemStack(state.getBlock());
   }

   public IBlockState getStateFromMeta(int meta) {
      IBlockState iblockstate = this.getDefaultState().withProperty(VARIANT, BlockSlabTC.Variant.DEFAULT);
      if (!this.isDouble()) {
         iblockstate = iblockstate.withProperty(HALF, (meta & 8) == 0 ? EnumBlockHalf.BOTTOM : EnumBlockHalf.TOP);
      }

      return iblockstate;
   }

   public int getMetaFromState(IBlockState state) {
      int i = 0;
      if (!this.isDouble() && state.getValue(HALF) == EnumBlockHalf.TOP) {
         i |= 8;
      }

      return i;
   }

   protected BlockStateContainer createBlockState() {
      return this.isDouble()
         ? new BlockStateContainer(this, new IProperty[]{VARIANT})
         : new BlockStateContainer(this, new IProperty[]{HALF, VARIANT});
   }

   public int damageDropped(IBlockState state) {
      return 0;
   }

   public boolean isDouble() {
      return false;
   }

   public String getTranslationKey(int meta) {
      return this.getTranslationKey();
   }

   public IProperty<?> getVariantProperty() {
      return VARIANT;
   }

   public Comparable<?> getTypeForItem(ItemStack stack) {
      return BlockSlabTC.Variant.DEFAULT;
   }

   public int getFlammability(IBlockAccess world, BlockPos pos, EnumFacing face) {
      return this.wood ? 20 : super.getFlammability(world, pos, face);
   }

   public int getFireSpreadSpeed(IBlockAccess world, BlockPos pos, EnumFacing face) {
      return this.wood ? 5 : super.getFireSpreadSpeed(world, pos, face);
   }

   public static class Double extends BlockSlabTC {
      public Double(String name, Block b, boolean wood) {
         super(name, b, wood);
      }

      @Override
      public boolean isDouble() {
         return true;
      }
   }

   public static class Half extends BlockSlabTC {
      public Half(String name, Block b, boolean wood) {
         super(name, b, wood);
      }

      @Override
      public boolean isDouble() {
         return false;
      }
   }

   public static enum Variant implements IStringSerializable {
      DEFAULT;

      public String getName() {
         return "default";
      }
   }
}
