package thaumcraft.common.blocks.world;

import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.world.aura.AuraHandler;

/**
 * Port of BlockCrystal: a cluster of vis crystals growing out of the rock faces around it.
 *
 * <p>In 1.12 the faces a crystal grows from were unlisted (render-only) properties; here they are real
 * block state properties so the multipart model can pick a crystal model per face.
 *
 * <p>Crystals live off the aura: with plenty of vis (flux, for vitium) they grow and spread to nearby rock, and in a
 * starved aura they shrink and give their vis back.
 */
public class CrystalBlock extends Block {
   public static final IntegerProperty SIZE = IntegerProperty.create("size", 0, 3);
   public static final IntegerProperty GENERATION = IntegerProperty.create("gen", 1, 4);
   public static final Map<Direction, BooleanProperty> FACES = PipeBlock.PROPERTY_BY_DIRECTION;

   private static final VoxelShape FULL = Shapes.block();
   private static final Map<Direction, VoxelShape> HALF = Map.of(
      Direction.UP, Block.box(0, 8, 0, 16, 16, 16),
      Direction.DOWN, Block.box(0, 0, 0, 16, 8, 16),
      Direction.EAST, Block.box(8, 0, 0, 16, 16, 16),
      Direction.WEST, Block.box(0, 0, 0, 8, 16, 16),
      Direction.SOUTH, Block.box(0, 0, 8, 16, 16, 16),
      Direction.NORTH, Block.box(0, 0, 0, 16, 16, 8)
   );

   public final Aspect aspect;

   public CrystalBlock(Aspect aspect, Properties properties) {
      super(properties);
      this.aspect = aspect;
      BlockState def = this.stateDefinition.any().setValue(SIZE, 0).setValue(GENERATION, 1);
      for (BooleanProperty p : FACES.values()) def = def.setValue(p, false);
      this.registerDefaultState(def);
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
      builder.add(SIZE, GENERATION);
      FACES.values().forEach(builder::add);
   }

   /** Rock in 1.12 terms: a pickaxe block whose face towards the crystal is solid. */
   public static boolean isRock(BlockGetter level, BlockPos pos, Direction towardsRock) {
      BlockPos rp = pos.relative(towardsRock);
      BlockState rs = level.getBlockState(rp);
      return rs.is(BlockTags.MINEABLE_WITH_PICKAXE) && rs.isFaceSturdy(level, rp, towardsRock.getOpposite());
   }

   public static boolean isTouchingRock(BlockGetter level, BlockPos pos) {
      for (Direction d : Direction.values()) {
         if (isRock(level, pos, d)) return true;
      }
      return false;
   }

   /** The given state with its face properties matching the rock around pos. */
   public BlockState withFaces(BlockState state, BlockGetter level, BlockPos pos) {
      for (Direction d : Direction.values()) state = state.setValue(FACES.get(d), isRock(level, pos, d));
      return state;
   }

   public BlockState worldgenState(BlockGetter level, BlockPos pos, int size) {
      return withFaces(defaultBlockState().setValue(SIZE, size), level, pos);
   }

   @Nullable
   @Override
   public BlockState getStateForPlacement(BlockPlaceContext ctx) {
      BlockState s = withFaces(defaultBlockState(), ctx.getLevel(), ctx.getClickedPos());
      return canSurvive(s, ctx.getLevel(), ctx.getClickedPos()) ? s : null;
   }

   @Override
   protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
      return isTouchingRock(level, pos);
   }

   @Override
   protected BlockState updateShape(BlockState state, Direction dir, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
      // Like neighborChanged in the original: a crystal with no rock left to hold it breaks off (and drops).
      if (!canSurvive(state, level, pos)) return Blocks.AIR.defaultBlockState();
      return state.setValue(FACES.get(dir), isRock(level, pos, dir));
   }

   @Override
   protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
      Direction only = null;
      int count = 0;
      for (Direction d : Direction.values()) {
         if (state.getValue(FACES.get(d))) {
            only = d;
            count++;
         }
      }
      return count == 1 ? HALF.get(only) : FULL;
   }

   @Override
   protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
      return true;
   }

   public int getGrowth(BlockState state) {
      return state.getValue(SIZE);
   }

   private static final int THRESHOLD = 10;

   @Override
   protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource rand) {
      int growth = state.getValue(SIZE);
      int generation = state.getValue(GENERATION);
      if (rand.nextInt(3 + generation) != 0) return;
      boolean flux = aspect == Aspect.FLUX;
      float amount = flux ? AuraHandler.getFlux(level, pos) : AuraHandler.getVis(level, pos);
      if (amount <= THRESHOLD) {
         if (growth > 0) {
            level.setBlockAndUpdate(pos, state.setValue(SIZE, growth - 1));
            giveBack(level, pos, flux);
         } else if (isTouching(level, pos, this)) {
            level.removeBlock(pos, false);
            giveBack(level, pos, flux);
         }
      } else if (amount > AuraHandler.getAuraBase(level, pos) + THRESHOLD) {
         if (growth < 3 && growth < 5 - generation + pos.asLong() % 3L) {
            if (take(level, pos, flux) > 0.0F) level.setBlockAndUpdate(pos, state.setValue(SIZE, growth + 1));
         } else if (generation < 4) {
            BlockPos p2 = spreadCrystal(level, pos, rand);
            if (p2 != null && take(level, pos, flux) > 0.0F) {
               if (rand.nextInt(6) == 0) generation--;
               level.setBlockAndUpdate(p2, withFaces(defaultBlockState().setValue(GENERATION, generation + 1), level, p2));
            }
         }
      }
   }

   private static void giveBack(ServerLevel level, BlockPos pos, boolean flux) {
      if (flux) AuraHandler.addFlux(level, pos, THRESHOLD);
      else AuraHandler.addVis(level, pos, THRESHOLD);
   }

   private static float take(ServerLevel level, BlockPos pos, boolean flux) {
      return flux ? AuraHandler.drainFlux(level, pos, THRESHOLD, false) : AuraHandler.drainVis(level, pos, THRESHOLD, false);
   }

   private static boolean isTouching(BlockGetter level, BlockPos pos, Block block) {
      for (Direction d : Direction.values()) {
         if (level.getBlockState(pos.relative(d)).is(block)) return true;
      }
      return false;
   }

   /** A random open spot next to this crystal that also touches rock, 1 time in 16. */
   @Nullable
   public static BlockPos spreadCrystal(ServerLevel level, BlockPos pos, RandomSource rand) {
      BlockPos t = pos.offset(rand.nextInt(3) - 1, rand.nextInt(3) - 1, rand.nextInt(3) - 1);
      if (t.equals(pos)) return null;
      BlockState bs = level.getBlockState(t);
      return bs.getFluidState().isEmpty() && (bs.isAir() || bs.canBeReplaced()) && rand.nextInt(16) == 0 && isTouchingRock(level, t) ? t : null;
   }
}
