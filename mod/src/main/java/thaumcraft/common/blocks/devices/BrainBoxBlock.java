package thaumcraft.common.blocks.devices;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import thaumcraft.common.registers.ModDevices;

/** BlockBrainBox (mnemonic matrix): attached to a thaumatorium, it lets it remember two more recipes. */
public class BrainBoxBlock extends Block {
   private static final VoxelShape SHAPE = box(3, 3, 3, 13, 13, 13);

   public BrainBoxBlock(Properties properties) {
      super(properties);
      registerDefaultState(stateDefinition.any().setValue(DirectionalBlock.FACING, Direction.UP));
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
      builder.add(DirectionalBlock.FACING);
   }

   @Override
   protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
      return SHAPE;
   }

   private static boolean isThaumatorium(BlockState s) {
      return s.is(ModDevices.THAUMATORIUM.get()) || s.is(ModDevices.THAUMATORIUM_TOP.get());
   }

   @Nullable
   @Override
   public BlockState getStateForPlacement(BlockPlaceContext ctx) {
      Direction side = ctx.getClickedFace();
      BlockState against = ctx.getLevel().getBlockState(ctx.getClickedPos().relative(side.getOpposite()));
      if (!isThaumatorium(against) || against.getValue(ThaumatoriumBlock.HORIZONTAL_FACING) == side) return null;
      return defaultBlockState().setValue(DirectionalBlock.FACING, side.getOpposite());
   }

   @Override
   protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
      return isThaumatorium(level.getBlockState(pos.relative(state.getValue(DirectionalBlock.FACING))));
   }

   @Override
   protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean moving) {
      if (!canSurvive(state, level, pos)) level.destroyBlock(pos, true);
   }
}
