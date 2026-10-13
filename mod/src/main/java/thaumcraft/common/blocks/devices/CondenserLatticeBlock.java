package thaumcraft.common.blocks.devices;

import java.util.HashSet;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.items.VisCrystalItem;
import thaumcraft.common.registers.ModFlux;
import thaumcraft.common.registers.ModItems;

/**
 * BlockCondenserLattice (clean and clogged): connects to other lattice pieces and to a condenser below; pieces that
 * lose their path to a condenser (or are more than ~8.6 blocks from it) drop off. A clogged piece is cleaned by using a
 * filter on it (which may give back a vitium crystal).
 */
public class CondenserLatticeBlock extends Block {
   public static final BooleanProperty NORTH = BooleanProperty.create("north");
   public static final BooleanProperty EAST = BooleanProperty.create("east");
   public static final BooleanProperty SOUTH = BooleanProperty.create("south");
   public static final BooleanProperty WEST = BooleanProperty.create("west");
   public static final BooleanProperty UP = BooleanProperty.create("up");
   public static final BooleanProperty DOWN = BooleanProperty.create("down");
   public final boolean dirty;

   public CondenserLatticeBlock(Properties properties, boolean dirty) {
      super(properties);
      this.dirty = dirty;
      registerDefaultState(stateDefinition.any().setValue(NORTH, false).setValue(EAST, false).setValue(SOUTH, false).setValue(WEST, false)
         .setValue(UP, false).setValue(DOWN, false));
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
      builder.add(NORTH, EAST, SOUTH, WEST, UP, DOWN);
   }

   private static BooleanProperty prop(Direction d) {
      return switch (d) {
         case DOWN -> DOWN;
         case UP -> UP;
         case NORTH -> NORTH;
         case SOUTH -> SOUTH;
         case WEST -> WEST;
         case EAST -> EAST;
      };
   }

   private static boolean connects(BlockGetter level, BlockPos pos, Direction face) {
      BlockState b = level.getBlockState(pos.relative(face));
      return b.getBlock() instanceof CondenserLatticeBlock || face == Direction.DOWN && b.is(ModFlux.CONDENSER.get());
   }

   private BlockState actual(BlockState state, BlockGetter level, BlockPos pos) {
      for (Direction d : Direction.values()) state = state.setValue(prop(d), connects(level, pos, d));
      return state;
   }

   @Override
   public BlockState getStateForPlacement(BlockPlaceContext ctx) {
      return actual(defaultBlockState(), ctx.getLevel(), ctx.getClickedPos());
   }

   @Override
   protected BlockState updateShape(BlockState state, Direction dir, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
      return state.setValue(prop(dir), connects(level, pos, dir));
   }

   @Override
   protected VoxelShape getShape(BlockState s, BlockGetter level, BlockPos pos, CollisionContext ctx) {
      return Block.box(s.getValue(WEST) ? 0 : 5, s.getValue(DOWN) ? 0 : 5, s.getValue(NORTH) ? 0 : 5,
         s.getValue(EAST) ? 16 : 11, s.getValue(UP) ? 16 : 11, s.getValue(SOUTH) ? 16 : 11);
   }

   @Override
   protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean moving) {
      super.onPlace(state, level, pos, oldState, moving);
      if (!level.isClientSide && !oldState.is(state.getBlock())) triggerUpdate(level, pos);
   }

   @Override
   protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean moving) {
      super.neighborChanged(state, level, pos, block, fromPos, moving);
      if (!level.isClientSide && (block instanceof CondenserLatticeBlock || block == ModFlux.CONDENSER.get())) triggerUpdate(level, pos);
   }

   @Override
   protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
      super.onRemove(state, level, pos, newState, moving);
      if (!level.isClientSide && !state.is(newState.getBlock())) resetCondenser(level, pos, new HashSet<>());
   }

   @Override
   protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                             BlockHitResult hit) {
      if (!dirty || !stack.is(ModItems.FILTER.get())) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
      if (!level.isClientSide) {
         stack.consume(1, player);
         Direction facing = hit.getDirection();
         if (level.random.nextBoolean()) {
            level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5 + facing.getStepX() / 3.0, pos.getY() + 0.5,
               pos.getZ() + 0.5 + facing.getStepZ() / 3.0, VisCrystalItem.of(Aspect.FLUX, 1)));
         }
         level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.2F,
            ((level.random.nextFloat() - level.random.nextFloat()) * 0.7F + 1.0F) * 1.6F);
         level.setBlock(pos, ModFlux.CONDENSER_LATTICE.get().defaultBlockState(), 3);
         ModFlux.CONDENSER_LATTICE.get().triggerUpdate(level, pos);
      }
      return ItemInteractionResult.sidedSuccess(level.isClientSide);
   }

   /** Drops this piece if it can't reach a condenser close enough; resets the condenser it reaches. */
   public void triggerUpdate(Level level, BlockPos pos) {
      BlockPos p = processUpdate(level, pos, new HashSet<>());
      if (p == null || p.distSqr(pos) > 74.0) {
         if (level.getBlockState(pos).getBlock() instanceof CondenserLatticeBlock) level.destroyBlock(pos, true);
      }
   }

   @Nullable
   private BlockPos processUpdate(Level level, BlockPos pos, Set<Long> history) {
      history.add(pos.asLong());
      for (Direction face : Direction.values()) {
         BlockPos p2 = pos.relative(face);
         if (history.contains(p2.asLong())) continue;
         BlockState b = level.getBlockState(p2);
         if (b.getBlock() instanceof CondenserLatticeBlock) {
            BlockPos pp = processUpdate(level, p2, history);
            if (pp != null) return pp;
         }
         if (face == Direction.DOWN && b.is(ModFlux.CONDENSER.get())) {
            if (level.getBlockEntity(p2) instanceof CondenserBlockEntity te) te.latticeCount = -1.0F;
            return p2;
         }
      }
      return null;
   }

   /** onBlockHarvested: the condenser (if any) recounts its lattice. */
   private void resetCondenser(Level level, BlockPos pos, Set<Long> history) {
      history.add(pos.asLong());
      for (Direction face : Direction.values()) {
         BlockPos p2 = pos.relative(face);
         if (history.contains(p2.asLong())) continue;
         BlockState b = level.getBlockState(p2);
         if (b.getBlock() instanceof CondenserLatticeBlock) resetCondenser(level, p2, history);
         else if (face == Direction.DOWN && level.getBlockEntity(p2) instanceof CondenserBlockEntity te) te.latticeCount = -1.0F;
      }
   }
}
