package thaumcraft.common.blocks.devices;

import com.mojang.serialization.MapCodec;
import java.util.EnumMap;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DiodeBlock;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.redstone.Redstone;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.ticks.TickPriority;
import thaumcraft.common.blocks.DeviceBlock;
import thaumcraft.common.registers.ModSounds;

/**
 * BlockRedstoneRelay: a repeater-like diode that turns on when its input reaches the "in" strength and then outputs
 * the "out" strength. Two little knobs on top set each (input: the one at the back, output: the one at the front).
 */
public class RedstoneRelayBlock extends DeviceBlock {
   public static final MapCodec<RedstoneRelayBlock> CODEC = simpleCodec(RedstoneRelayBlock::new);
   private static final VoxelShape BASE = box(0, 0, 0, 16, 2, 16);
   private static final Map<Direction, VoxelShape> SHAPES = new EnumMap<>(Direction.class);

   static {
      for (Direction d : Direction.Plane.HORIZONTAL) SHAPES.put(d, Shapes.or(BASE, Shapes.create(knobOut(d)), Shapes.create(knobIn(d))));
   }

   public RedstoneRelayBlock(Properties properties) {
      super(properties);
   }

   /** Rotation.quarterRotations about the block centre: south 0, west 1, north 2, east 3 quarter turns. */
   private static AABB rotate(double x0, double y0, double z0, double x1, double y1, double z1, Direction facing) {
      int turns = switch (facing) {
         case WEST -> 1;
         case NORTH -> 2;
         case EAST -> 3;
         default -> 0;
      };
      for (int t = 0; t < turns; t++) {
         double nx0 = -z0, nz0 = x0, nx1 = -z1, nz1 = x1;
         x0 = nx0;
         z0 = nz0;
         x1 = nx1;
         z1 = nz1;
      }
      return new AABB(Math.min(x0, x1) + 0.5, y0, Math.min(z0, z1) + 0.5, Math.max(x0, x1) + 0.5, y1, Math.max(z0, z1) + 0.5);
   }

   /** getCuboid0: the output knob. */
   public static AABB knobOut(Direction facing) {
      return rotate(-0.375, 0.0625, -0.375, -0.125, 0.25, -0.125, facing);
   }

   /** getCuboid1: the input knob. */
   public static AABB knobIn(Direction facing) {
      return rotate(-0.125, 0.0625, 0.125, 0.125, 0.25, 0.375, facing);
   }

   @Override
   protected MapCodec<? extends BaseEntityBlock> codec() {
      return CODEC;
   }

   @Override
   protected Facing facingType() {
      return Facing.HORIZONTAL;
   }

   @Override
   protected boolean hasEnabled() {
      return true;
   }

   @Override
   protected boolean redstoneDisables() {
      return false;
   }

   @Override
   protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
      return SHAPES.get(getFacing(state));
   }

   @Override
   protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
      return BASE;
   }

   @Nullable
   @Override
   public BlockState getStateForPlacement(BlockPlaceContext ctx) {
      Direction d = ctx.getHorizontalDirection();
      boolean sneak = ctx.getPlayer() != null && ctx.getPlayer().isShiftKeyDown();
      return defaultBlockState().setValue(HORIZONTAL_FACING, sneak ? d : d.getOpposite()).setValue(ENABLED, false);
   }

   @Override
   protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
      return canSupportRigidBlock(level, pos.below());
   }

   @Override
   protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
      if (!player.mayBuild()) return InteractionResult.PASS;
      if (!(level.getBlockEntity(pos) instanceof RedstoneRelayBlockEntity tile)) return InteractionResult.PASS;
      Direction f = getFacing(state);
      boolean out = LevitatorBlock.hits(hit, pos, knobOut(f));
      boolean in = !out && LevitatorBlock.hits(hit, pos, knobIn(f));
      if (!out && !in) return InteractionResult.PASS;
      if (!level.isClientSide) {
         if (out) tile.increaseOut();
         else tile.increaseIn();
         level.playSound(null, pos, ModSounds.KEY.get(), SoundSource.BLOCKS, 0.5F, 1.0F);
         checkTickOnNeighbor(level, pos, state);
         updateNeighborsInFront(level, pos, state);
      }
      return InteractionResult.sidedSuccess(level.isClientSide);
   }

   @Override
   protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
      boolean flag = shouldBePowered(level, pos, state);
      boolean powered = isEnabled(state);
      if (powered && !flag) {
         level.setBlock(pos, state.setValue(ENABLED, false), 2);
         updateNeighborsInFront(level, pos, state);
      } else if (!powered) {
         level.setBlock(pos, state.setValue(ENABLED, true), 2);
         updateNeighborsInFront(level, pos, state);
         if (!flag) level.scheduleTick(pos, this, 2, TickPriority.VERY_HIGH);
      }
   }

   @Override
   protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean moving) {
      if (state.canSurvive(level, pos)) {
         checkTickOnNeighbor(level, pos, state);
      } else {
         BlockEntity be = state.hasBlockEntity() ? level.getBlockEntity(pos) : null;
         dropResources(state, level, pos, be);
         level.removeBlock(pos, false);
         for (Direction d : Direction.values()) level.updateNeighborsAt(pos.relative(d), this);
      }
   }

   private void checkTickOnNeighbor(Level level, BlockPos pos, BlockState state) {
      boolean flag = shouldBePowered(level, pos, state);
      boolean powered = isEnabled(state);
      if (powered != flag && !level.getBlockTicks().willTickThisTick(pos, this)) {
         TickPriority p = TickPriority.HIGH;
         if (isFacingTowardsRepeater(level, pos, state)) p = TickPriority.EXTREMELY_HIGH;
         else if (powered) p = TickPriority.VERY_HIGH;
         level.scheduleTick(pos, this, 2, p);
      }
   }

   private boolean shouldBePowered(Level level, BlockPos pos, BlockState state) {
      int pr = level.getBlockEntity(pos) instanceof RedstoneRelayBlockEntity tile ? tile.getIn() : 1;
      return calculateInputStrength(level, pos, state) >= pr;
   }

   private int calculateInputStrength(Level level, BlockPos pos, BlockState state) {
      Direction facing = getFacing(state);
      BlockPos p1 = pos.relative(facing);
      int i = level.getSignal(p1, facing);
      if (i >= Redstone.SIGNAL_MAX) return i;
      BlockState s1 = level.getBlockState(p1);
      return Math.max(i, s1.is(Blocks.REDSTONE_WIRE) ? s1.getValue(RedStoneWireBlock.POWER) : 0);
   }

   private boolean isFacingTowardsRepeater(Level level, BlockPos pos, BlockState state) {
      Direction d = getFacing(state).getOpposite();
      BlockState s = level.getBlockState(pos.relative(d));
      return s.getBlock() instanceof DiodeBlock && s.getValue(DiodeBlock.FACING) != d;
   }

   private void updateNeighborsInFront(Level level, BlockPos pos, BlockState state) {
      Direction facing = getFacing(state);
      BlockPos front = pos.relative(facing.getOpposite());
      if (net.neoforged.neoforge.event.EventHooks.onNeighborNotify(level, pos, level.getBlockState(pos), java.util.EnumSet.of(facing.getOpposite()), false).isCanceled()) return;
      level.neighborChanged(front, this, pos);
      level.updateNeighborsAtExceptFromFacing(front, this, facing);
   }

   @Override
   protected boolean isSignalSource(BlockState state) {
      return true;
   }

   @Override
   protected int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction side) {
      return getSignal(state, level, pos, side);
   }

   @Override
   protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction side) {
      if (!isEnabled(state) || getFacing(state) != side) return 0;
      return level.getBlockEntity(pos) instanceof RedstoneRelayBlockEntity tile ? tile.getOut() : 0;
   }

   @Override
   public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
      if (shouldBePowered(level, pos, state)) level.scheduleTick(pos, this, 1);
   }

   @Override
   protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean moving) {
      updateNeighborsInFront(level, pos, state);
   }

   @Override
   protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
      super.onRemove(state, level, pos, newState, moving);
      if (!moving && !state.is(newState.getBlock())) updateNeighborsInFront(level, pos, state);
   }

   @Nullable
   @Override
   public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
      return new RedstoneRelayBlockEntity(pos, state);
   }
}
