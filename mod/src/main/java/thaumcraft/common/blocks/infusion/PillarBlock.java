package thaumcraft.common.blocks.infusion;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * BlockPillar: the corner pillars of an infusion altar, made from two arcane (ancient, eldritch) stones by salis
 * mundus. The block is two blocks tall (the one above is air); breaking it gives the two stones back (loot table).
 */
public class PillarBlock extends HorizontalDirectionalBlock {
   public static final com.mojang.serialization.MapCodec<PillarBlock> CODEC = simpleCodec(PillarBlock::new);
   private static final VoxelShape OUTLINE = box(0, 0, 0, 16, 8, 16);
   private static final VoxelShape COLLISION = box(0, 0, 0, 16, 32, 16);

   public PillarBlock(Properties properties) {
      super(properties.pushReaction(PushReaction.BLOCK));
      registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
   }

   @Override
   protected com.mojang.serialization.MapCodec<? extends HorizontalDirectionalBlock> codec() {
      return CODEC;
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
      builder.add(FACING);
   }

   @Override
   public BlockState getStateForPlacement(BlockPlaceContext ctx) {
      return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
   }

   @Override
   protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
      return OUTLINE;
   }

   @Override
   protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
      return COLLISION;
   }
}
