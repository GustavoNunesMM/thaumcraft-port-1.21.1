package thaumcraft.common.blocks.world.taint;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import thaumcraft.common.entities.monster.tainted.TaintCrawlerEntity;
import thaumcraft.common.registers.ModFlux;

/**
 * BlockTaintFeature (swollen taint): grows on the faces of taint logs; breaking it may release a taint crawler, on top
 * of a vertical taint log it may become a taint geyser.
 */
public class TaintFeatureBlock extends Block implements ITaintBlock {
   public static final DirectionProperty FACING = BlockStateProperties.FACING;
   private static final VoxelShape[] SHAPES = {
      Block.box(2, 10, 2, 14, 16, 14), Block.box(2, 0, 2, 14, 6, 14), Block.box(2, 2, 10, 14, 14, 16),
      Block.box(2, 2, 0, 14, 14, 6), Block.box(10, 2, 2, 16, 14, 14), Block.box(0, 2, 2, 6, 14, 14)};

   public TaintFeatureBlock(Properties properties) {
      super(properties);
      registerDefaultState(stateDefinition.any().setValue(FACING, Direction.UP));
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
      builder.add(FACING);
   }

   @Override
   public BlockState getStateForPlacement(BlockPlaceContext ctx) {
      return defaultBlockState().setValue(FACING, ctx.getClickedFace());
   }

   @Override
   protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
      return SHAPES[state.getValue(FACING).get3DDataValue()];
   }

   @Override
   public void die(Level level, BlockPos pos, BlockState state) {
      level.setBlockAndUpdate(pos, ModFlux.FLUX_GOO.get().defaultBlockState());
   }

   @Override
   protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
      if (!TaintHelper.isNearTaintSeed(level, pos) && random.nextInt(10) == 0) {
         die(level, pos, state);
         return;
      }
      TaintHelper.spreadFibres(level, pos);
      BlockState below = level.getBlockState(pos.below());
      if (below.is(ModFlux.TAINT_LOG.get()) && below.getValue(RotatedPillarBlock.AXIS) == Axis.Y && level.random.nextInt(100) == 0) {
         level.setBlockAndUpdate(pos, ModFlux.TAINT_GEYSER.get().defaultBlockState());
      }
   }

   /** breakBlock: a third of the time a taint crawler crawls out, otherwise the flux escapes. */
   @Override
   protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
      if (!level.isClientSide && !state.is(newState.getBlock())) {
         if (level.random.nextFloat() < 0.333F) {
            TaintCrawlerEntity e = new TaintCrawlerEntity(ModFlux.TAINT_CRAWLER.get(), level);
            e.moveTo(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, level.random.nextInt(360), 0.0F);
            level.addFreshEntity(e);
         } else {
            FluxEffects.pollute(level, pos, 1.0F, true);
         }
      }
      super.onRemove(state, level, pos, newState, moving);
   }

   @Override
   protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean moving) {
      Direction f = state.getValue(FACING);
      BlockPos support = pos.relative(f.getOpposite());
      if (!level.isClientSide && !level.getBlockState(support).isFaceSturdy(level, support, f)) level.removeBlock(pos, false);
   }
}
