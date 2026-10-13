package thaumcraft.common.blocks.world.taint;

import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import thaumcraft.common.registers.ModFlux;

/**
 * BlockTaintFibre: a film of taint on the faces of the solid blocks around it, sometimes with a growth (decided by the
 * position, as in 1.12). The 1.12 "actual state" (connections and growth) is stored in the state and kept up to date
 * in updateShape. Growth 3 (the glowing bulb) drops a vitium crystal (loot table) and pollutes the aura when broken.
 */
public class TaintFibreBlock extends Block implements ITaintBlock {
   public static final BooleanProperty NORTH = BooleanProperty.create("north");
   public static final BooleanProperty EAST = BooleanProperty.create("east");
   public static final BooleanProperty SOUTH = BooleanProperty.create("south");
   public static final BooleanProperty WEST = BooleanProperty.create("west");
   public static final BooleanProperty UP = BooleanProperty.create("up");
   public static final BooleanProperty DOWN = BooleanProperty.create("down");
   public static final BooleanProperty GROWTH1 = BooleanProperty.create("growth1");
   public static final BooleanProperty GROWTH2 = BooleanProperty.create("growth2");
   public static final BooleanProperty GROWTH3 = BooleanProperty.create("growth3");
   public static final BooleanProperty GROWTH4 = BooleanProperty.create("growth4");

   private static final VoxelShape AABB_UP = Block.box(0, 15.2, 0, 16, 16, 16);
   private static final VoxelShape AABB_DOWN = Block.box(0, 0, 0, 16, 0.8, 16);
   private static final VoxelShape AABB_EAST = Block.box(15.2, 0, 0, 16, 16, 16);
   private static final VoxelShape AABB_WEST = Block.box(0, 0, 0, 0.8, 16, 16);
   private static final VoxelShape AABB_SOUTH = Block.box(0, 0, 15.2, 16, 16, 16);
   private static final VoxelShape AABB_NORTH = Block.box(0, 0, 0, 16, 16, 0.8);
   private static final VoxelShape G1 = Block.box(1.6, 0, 1.6, 14.4, 6.4, 14.4);
   private static final VoxelShape G2 = Block.box(3.2, 0, 3.2, 12.8, 16, 12.8);
   private static final VoxelShape G3 = Block.box(4, 0, 4, 12, 5, 12);
   private static final VoxelShape G4 = Block.box(1.6, 4.8, 1.6, 14.4, 16, 14.4);

   public TaintFibreBlock(Properties properties) {
      super(properties);
      registerDefaultState(stateDefinition.any().setValue(NORTH, false).setValue(EAST, false).setValue(SOUTH, false).setValue(WEST, false)
         .setValue(UP, false).setValue(DOWN, false).setValue(GROWTH1, false).setValue(GROWTH2, false).setValue(GROWTH3, false).setValue(GROWTH4, false));
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
      builder.add(NORTH, EAST, SOUTH, WEST, UP, DOWN, GROWTH1, GROWTH2, GROWTH3, GROWTH4);
   }

   @Override
   public void die(Level level, BlockPos pos, BlockState state) {
      level.removeBlock(pos, false);
   }

   public static boolean hasGrowth(BlockState state) {
      return state.getValue(GROWTH1) || state.getValue(GROWTH2) || state.getValue(GROWTH3) || state.getValue(GROWTH4);
   }

   // ------------------------------------------------------------------ actual state

   private static boolean drawAt(BlockGetter level, BlockPos pos, Direction side) {
      BlockState b = level.getBlockState(pos);
      return !(b.getBlock() instanceof TaintFibreBlock) && !(b.getBlock() instanceof TaintFeatureBlock) && b.isFaceSturdy(level, pos, side.getOpposite());
   }

   /** getActualState: faces it is drawn on and the position-seeded growth. */
   public BlockState actualState(BlockState state, BlockGetter level, BlockPos pos) {
      boolean d = drawAt(level, pos.below(), Direction.DOWN);
      boolean u = drawAt(level, pos.above(), Direction.UP);
      int growth = 0;
      int q = new Random(pos.asLong()).nextInt(50);
      if (d) {
         if (q < 4) growth = 1;
         else if (q == 4 || q == 5) growth = 2;
         else if (q == 6) growth = 3;
      }
      if (u && q > 47) growth = 4;
      return state.setValue(DOWN, d).setValue(UP, u)
         .setValue(NORTH, drawAt(level, pos.north(), Direction.NORTH)).setValue(SOUTH, drawAt(level, pos.south(), Direction.SOUTH))
         .setValue(WEST, drawAt(level, pos.west(), Direction.WEST)).setValue(EAST, drawAt(level, pos.east(), Direction.EAST))
         .setValue(GROWTH1, growth == 1).setValue(GROWTH2, growth == 2).setValue(GROWTH3, growth == 3).setValue(GROWTH4, growth == 4);
   }

   @Override
   public BlockState getStateForPlacement(BlockPlaceContext ctx) {
      return actualState(defaultBlockState(), ctx.getLevel(), ctx.getClickedPos());
   }

   @Override
   protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean moving) {
      super.onPlace(state, level, pos, oldState, moving);
      BlockState actual = actualState(state, level, pos);
      if (actual != state) level.setBlock(pos, actual, 2);
   }

   @Override
   protected BlockState updateShape(BlockState state, Direction dir, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
      return actualState(state, level, pos);
   }

   @Override
   protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean moving) {
      super.neighborChanged(state, level, pos, block, fromPos, moving);
      if (!level.isClientSide && !hasGrowth(state) && TaintHelper.isOnlyAdjacentToTaint(level, pos)) level.removeBlock(pos, false);
   }

   // ------------------------------------------------------------------ ticking

   @Override
   protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
      if (!hasGrowth(state) && TaintHelper.isOnlyAdjacentToTaint(level, pos)) {
         die(level, pos, state);
      } else if (!TaintHelper.isNearTaintSeed(level, pos)) {
         die(level, pos, state);
      } else {
         TaintHelper.spreadFibres(level, pos);
      }
   }

   @Override
   public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
      if (!level.isClientSide && entity instanceof LivingEntity le && !le.getType().is(EntityTypeTags.UNDEAD) && level.random.nextInt(750) == 0) {
         le.addEffect(new MobEffectInstance(ModFlux.FLUX_TAINT, 200, 0, false, true));
      }
   }

   @Override
   protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
      stepOn(level, pos, state, entity);
   }

   @Override
   protected boolean triggerEvent(BlockState state, Level level, BlockPos pos, int id, int param) {
      if (id == 1) {
         if (level.isClientSide) {
            level.playLocalSound(pos, SoundEvents.CHORUS_FLOWER_DEATH, SoundSource.BLOCKS, 0.1F, 0.9F + level.random.nextFloat() * 0.2F, false);
         }
         return true;
      }
      return super.triggerEvent(state, level, pos, id, param);
   }

   /** dropBlockAsItemWithChance: breaking the glowing growth releases its flux (the crystal is in the loot table). */
   @Override
   protected void spawnAfterBreak(BlockState state, ServerLevel level, BlockPos pos, ItemStack tool, boolean dropXp) {
      super.spawnAfterBreak(state, level, pos, tool, dropXp);
      if (state.getValue(GROWTH3)) FluxEffects.pollute(level, pos, 1.0F, true);
   }

   // ------------------------------------------------------------------ shapes

   private static VoxelShape faces(BlockState s) {
      VoxelShape v = Shapes.empty();
      if (s.getValue(UP)) v = Shapes.or(v, AABB_UP);
      if (s.getValue(DOWN)) v = Shapes.or(v, AABB_DOWN);
      if (s.getValue(EAST)) v = Shapes.or(v, AABB_EAST);
      if (s.getValue(WEST)) v = Shapes.or(v, AABB_WEST);
      if (s.getValue(SOUTH)) v = Shapes.or(v, AABB_SOUTH);
      if (s.getValue(NORTH)) v = Shapes.or(v, AABB_NORTH);
      return v;
   }

   @Override
   protected VoxelShape getShape(BlockState s, BlockGetter level, BlockPos pos, CollisionContext ctx) {
      if (s.getValue(GROWTH1)) return G1;
      if (s.getValue(GROWTH2)) return G2;
      if (s.getValue(GROWTH3)) return G3;
      if (s.getValue(GROWTH4)) return G4;
      VoxelShape f = faces(s);
      return f.isEmpty() ? AABB_DOWN : f;
   }

   @Override
   protected VoxelShape getCollisionShape(BlockState s, BlockGetter level, BlockPos pos, CollisionContext ctx) {
      return faces(s);
   }

   @Override
   protected boolean isPathfindable(BlockState state, net.minecraft.world.level.pathfinder.PathComputationType type) {
      return true;
   }
}
