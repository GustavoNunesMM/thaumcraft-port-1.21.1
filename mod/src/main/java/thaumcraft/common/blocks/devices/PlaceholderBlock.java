package thaumcraft.common.blocks.devices;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;

/**
 * BlockPlaceholder: the invisible stand-ins for the nether bricks and obsidian of an infernal furnace (the furnace
 * model draws the whole structure). Breaking one takes the furnace apart. They drop the block they replaced.
 */
public class PlaceholderBlock extends Block {
   private final boolean obsidian;

   public PlaceholderBlock(boolean obsidian, Properties properties) {
      super(properties);
      this.obsidian = obsidian;
   }

   @Override
   protected RenderShape getRenderShape(BlockState state) {
      return RenderShape.INVISIBLE;
   }

   @Override
   public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
      return new ItemStack(obsidian ? Blocks.OBSIDIAN : Blocks.NETHER_BRICKS);
   }

   @Override
   protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
      super.onRemove(state, level, pos, newState, moving);
      if (state.is(newState.getBlock()) || InfernalFurnaceBlock.ignore || level.isClientSide) return;
      for (BlockPos p : BlockPos.betweenClosed(pos.offset(-1, -1, -1), pos.offset(1, 1, 1))) {
         BlockState s = level.getBlockState(p);
         if (s.getBlock() instanceof InfernalFurnaceBlock) {
            InfernalFurnaceBlock.destroyFurnace(level, p.immutable(), s, pos);
            break;
         }
      }
   }
}
