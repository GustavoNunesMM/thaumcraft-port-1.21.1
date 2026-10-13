package thaumcraft.common.blocks.essentia;

import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * BlockSmelterAux / BlockSmelterVent: attached to the side (not the front) of a smeltery, facing it.
 * The pump lets alembics above it take essentia too; the vent catches some of the flux.
 */
public class SmelterPartBlock extends HorizontalDirectionalBlock {
   public static final MapCodec<SmelterPartBlock> CODEC = simpleCodec(p -> new SmelterPartBlock(false, p));
   private final boolean vent;

   public SmelterPartBlock(boolean vent, Properties properties) {
      super(properties);
      this.vent = vent;
      registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
   }

   @Override
   protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
      return CODEC;
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
      builder.add(FACING);
   }

   @Nullable
   @Override
   public BlockState getStateForPlacement(BlockPlaceContext ctx) {
      Direction side = ctx.getClickedFace();
      if (!side.getAxis().isHorizontal()) return null;
      BlockState s = defaultBlockState().setValue(FACING, side.getOpposite());
      return canSurvive(s, ctx.getLevel(), ctx.getClickedPos()) ? s : null;
   }

   /** canPlaceBlockOnSide: only against a smeltery, and not on its front. */
   @Override
   protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
      Direction f = state.getValue(FACING);
      BlockState smelter = level.getBlockState(pos.relative(f));
      return smelter.getBlock() instanceof SmelterBlock && SmelterBlock.getFacing(smelter) != f.getOpposite();
   }

   @Override
   protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
      if (!vent) return Shapes.block();
      return switch (state.getValue(FACING)) {
         case SOUTH -> box(2, 2, 8, 14, 14, 16);
         case WEST -> box(0, 2, 2, 8, 14, 14);
         case EAST -> box(8, 2, 2, 16, 14, 14);
         default -> box(2, 2, 0, 14, 14, 8);
      };
   }
}
