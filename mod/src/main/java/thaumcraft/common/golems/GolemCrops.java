package thaumcraft.common.golems;

import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * CropUtils and ThaumcraftApi.getSeed: which blocks are ripe crops, and the seed that replants them. Standard crops
 * are harvested when grown, "stacked" crops (cane, cactus) only above their own kind, clickable crops are right-clicked.
 */
public final class GolemCrops {
   public static final Set<Block> standardCrops = new HashSet<>();
   public static final Set<Block> clickableCrops = new HashSet<>();
   public static final Set<Block> stackedCrops = new HashSet<>();

   static {
      standardCrops.add(Blocks.MELON);
      standardCrops.add(Blocks.PUMPKIN);
      stackedCrops.add(Blocks.SUGAR_CANE);
      stackedCrops.add(Blocks.CACTUS);
      stackedCrops.add(Blocks.BAMBOO);
   }

   private GolemCrops() {
   }

   public static boolean isGrownCrop(Level level, BlockPos pos) {
      if (level.isEmptyBlock(pos)) return false;
      BlockState bs = level.getBlockState(pos);
      Block bi = bs.getBlock();
      if (stackedCrops.contains(bi)) return level.getBlockState(pos.below()).is(bi);
      if (standardCrops.contains(bi) || clickableCrops.contains(bi)) return true;
      if (bi instanceof CropBlock crop) return crop.isMaxAge(bs);
      if (bi instanceof NetherWartBlock) return bs.getValue(NetherWartBlock.AGE) >= 3;
      return bi instanceof BonemealableBlock bm && !(bi instanceof StemBlock) && bi instanceof BushBlock && !bm.isValidBonemealTarget(level, pos, bs);
   }

   /** The item that plants this crop (the IPlantable items of 1.12), or empty. */
   public static ItemStack getSeed(Block block) {
      if (block instanceof CocoaBlock) return new ItemStack(Items.COCOA_BEANS);
      if (block instanceof BushBlock && block.asItem() instanceof BlockItem bi && bi.getBlock() == block) return new ItemStack(bi);
      return ItemStack.EMPTY;
   }
}
