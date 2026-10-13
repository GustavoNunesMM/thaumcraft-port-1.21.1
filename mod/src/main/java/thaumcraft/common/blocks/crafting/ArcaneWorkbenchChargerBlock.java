package thaumcraft.common.blocks.crafting;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import thaumcraft.common.registers.ModBlocks;

/** BlockArcaneWorkbenchCharger: sits on an arcane workbench and lets it draw vis from the 3x3 chunks around. */
public class ArcaneWorkbenchChargerBlock extends Block {
   private static final VoxelShape SHAPE = box(0, 0, 0, 16, 16, 16);

   public ArcaneWorkbenchChargerBlock(Properties properties) {
      super(properties);
   }

   @Override
   protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
      return SHAPE;
   }

   @Override
   protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
      return level.getBlockState(pos.below()).is(ModBlocks.ARCANE_WORKBENCH.get());
   }

   @Override
   protected BlockState updateShape(BlockState state, Direction dir, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
      return dir == Direction.DOWN && !canSurvive(state, level, pos) ? Blocks.AIR.defaultBlockState()
         : super.updateShape(state, dir, neighbor, level, pos, neighborPos);
   }

   @Override
   protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
      return ArcaneWorkbenchBlock.open(level, pos.below(), player);
   }
}
