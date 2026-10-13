package thaumcraft.common.blocks;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;

/**
 * BlockTCDevice: a block entity block that may face a direction (IBlockFacing: any side, IBlockFacingHorizontal) and
 * may have an "enabled" state that a redstone signal turns off (IBlockEnabled).
 * Subclasses pick these with {@link #facingType()} / {@link #hasEnabled()}, which must return constants
 * (they are read while the block state definition is built, before the subclass constructor runs).
 */
public abstract class DeviceBlock extends BaseEntityBlock {
   public static final DirectionProperty FACING = BlockStateProperties.FACING;
   public static final DirectionProperty HORIZONTAL_FACING = BlockStateProperties.HORIZONTAL_FACING;
   public static final BooleanProperty ENABLED = BooleanProperty.create("enabled");

   public enum Facing {
      NONE, HORIZONTAL, ALL
   }

   protected DeviceBlock(Properties properties) {
      super(properties);
      BlockState bs = stateDefinition.any();
      if (facingType() == Facing.HORIZONTAL) bs = bs.setValue(HORIZONTAL_FACING, Direction.NORTH);
      if (facingType() == Facing.ALL) bs = bs.setValue(FACING, Direction.UP);
      if (hasEnabled()) bs = bs.setValue(ENABLED, true);
      registerDefaultState(bs);
   }

   protected Facing facingType() {
      return Facing.NONE;
   }

   protected boolean hasEnabled() {
      return false;
   }

   /** Whether a redstone signal turns "enabled" off (false for blocks that use it for something else, like a lit smelter). */
   protected boolean redstoneDisables() {
      return hasEnabled();
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
      if (facingType() == Facing.HORIZONTAL) builder.add(HORIZONTAL_FACING);
      if (facingType() == Facing.ALL) builder.add(FACING);
      if (hasEnabled()) builder.add(ENABLED);
   }

   @Override
   protected RenderShape getRenderShape(BlockState state) {
      return RenderShape.MODEL;
   }

   @Nullable
   @Override
   public BlockState getStateForPlacement(BlockPlaceContext ctx) {
      BlockState bs = defaultBlockState();
      boolean sneak = ctx.getPlayer() != null && ctx.getPlayer().isShiftKeyDown();
      if (facingType() == Facing.HORIZONTAL) {
         Direction d = ctx.getHorizontalDirection();
         bs = bs.setValue(HORIZONTAL_FACING, sneak ? d : d.getOpposite());
      }
      if (facingType() == Facing.ALL) {
         Direction d = ctx.getNearestLookingDirection().getOpposite();
         bs = bs.setValue(FACING, sneak ? d.getOpposite() : d);
      }
      if (hasEnabled()) bs = bs.setValue(ENABLED, !redstoneDisables() || !ctx.getLevel().hasNeighborSignal(ctx.getClickedPos()));
      return bs;
   }

   @Override
   protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean moving) {
      updateState(level, pos, state);
      super.neighborChanged(state, level, pos, block, fromPos, moving);
   }

   @Override
   protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean moving) {
      super.onPlace(state, level, pos, oldState, moving);
      updateState(level, pos, state);
   }

   protected void updateState(Level level, BlockPos pos, BlockState state) {
      if (redstoneDisables() && !level.isClientSide) {
         boolean flag = !level.hasNeighborSignal(pos);
         if (flag != state.getValue(ENABLED)) level.setBlock(pos, state.setValue(ENABLED, flag), 3);
      }
   }

   /** BlockStateUtils.getFacing. */
   public static Direction getFacing(BlockState state) {
      if (state.hasProperty(FACING)) return state.getValue(FACING);
      if (state.hasProperty(HORIZONTAL_FACING)) return state.getValue(HORIZONTAL_FACING);
      return Direction.UP;
   }

   public static boolean isEnabled(BlockState state) {
      return !state.hasProperty(ENABLED) || state.getValue(ENABLED);
   }

   @Override
   protected BlockState rotate(BlockState state, Rotation rot) {
      if (state.hasProperty(FACING)) return state.setValue(FACING, rot.rotate(state.getValue(FACING)));
      if (state.hasProperty(HORIZONTAL_FACING)) return state.setValue(HORIZONTAL_FACING, rot.rotate(state.getValue(HORIZONTAL_FACING)));
      return state;
   }

   @Override
   protected BlockState mirror(BlockState state, Mirror mirror) {
      if (state.hasProperty(FACING)) return state.rotate(mirror.getRotation(state.getValue(FACING)));
      if (state.hasProperty(HORIZONTAL_FACING)) return state.rotate(mirror.getRotation(state.getValue(HORIZONTAL_FACING)));
      return state;
   }
}
