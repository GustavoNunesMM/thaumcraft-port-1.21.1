package thaumcraft.common.blocks.world.taint;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.phys.AABB;
import thaumcraft.common.config.ModConfig;
import thaumcraft.common.entities.monster.tainted.TaintSeedEntity;
import thaumcraft.common.registers.ModFlux;
import thaumcraft.common.world.aura.AuraHandler;

/**
 * TaintHelper: taint only lives near taint seeds (the server keeps a per-level list of their positions), and spreads
 * from fibres, rock, logs and swollen growths onto the blocks around them, faster the more flux saturates the aura.
 *
 * <p>Port notes: 1.12 materials are approximated with tags and the note-block instrument that 1.20 gave to each former
 * material (BASEDRUM = rock, SNARE = sand/gravel, BASS = wood). The taint config (taintSpreadRate, taintSpreadArea) are
 * the static fields below.
 */
public final class TaintHelper {
   /** ModConfig.CONFIG_WORLD.taintSpreadRate: % chance of a taint fibre spreading on a block tick. */
   public static float taintSpreadRate = 100.0F;
   /** ModConfig.CONFIG_WORLD.taintSpreadArea: how far taint spreads from a seed. */
   public static int taintSpreadArea = 32;

   private static final Map<Level, List<BlockPos>> TAINT_SEEDS = new WeakHashMap<>();

   private TaintHelper() {
   }

   public static synchronized void addTaintSeed(Level level, BlockPos pos) {
      TAINT_SEEDS.computeIfAbsent(level, l -> new ArrayList<>()).add(pos.immutable());
   }

   public static synchronized void removeTaintSeed(Level level, BlockPos pos) {
      List<BlockPos> locs = TAINT_SEEDS.get(level);
      if (locs != null) locs.remove(pos);
   }

   public static synchronized boolean isNearTaintSeed(Level level, BlockPos pos) {
      double area = taintSpreadArea * taintSpreadArea;
      List<BlockPos> locs = TAINT_SEEDS.get(level);
      if (locs != null) {
         for (BlockPos p : new ArrayList<>(locs)) {
            if (p.distSqr(pos) <= area) {
               if (level.getEntitiesOfClass(TaintSeedEntity.class, new AABB(p).inflate(1.0)).isEmpty()) {
                  removeTaintSeed(level, p);
                  return false;
               }
               return true;
            }
         }
      }
      return false;
   }

   public static synchronized boolean isAtTaintSeedEdge(Level level, BlockPos pos) {
      double area = taintSpreadArea * taintSpreadArea;
      double fringe = taintSpreadArea * 0.8 * (taintSpreadArea * 0.8);
      List<BlockPos> locs = TAINT_SEEDS.get(level);
      if (locs != null) {
         for (BlockPos p : locs) {
            double d = p.distSqr(pos);
            if (d < area && d > fringe) return true;
         }
      }
      return false;
   }

   // ------------------------------------------------------------------ "materials"

   /** state.getMaterial() == ThaumcraftMaterials.MATERIAL_TAINT. */
   public static boolean isTaint(BlockState state) {
      return state.getBlock() instanceof ITaintBlock || state.getBlock() instanceof FluxGooBlock;
   }

   /** Utils.isWoodLog (taint logs count, they can hold leaves). */
   public static boolean isWoodLog(BlockGetter level, BlockPos pos) {
      BlockState bs = level.getBlockState(pos);
      return bs.is(BlockTags.LOGS) || bs.getBlock() instanceof TaintLogBlock;
   }

   public static boolean isLeaves(BlockState bs) {
      return bs.is(BlockTags.LEAVES) || bs.getBlock() instanceof LeavesBlock;
   }

   /** BlockUtils.isAdjacentToSolidBlock. */
   public static boolean isAdjacentToSolidBlock(Level level, BlockPos pos) {
      for (Direction face : Direction.values()) {
         BlockPos p = pos.relative(face);
         if (level.getBlockState(p).isFaceSturdy(level, p, face.getOpposite())) return true;
      }
      return false;
   }

   /** BlockUtils.getFaceBlockTouching. */
   public static Direction getFaceBlockTouching(Level level, BlockPos pos, net.minecraft.world.level.block.Block block) {
      for (Direction face : Direction.values()) {
         if (level.getBlockState(pos.relative(face)).is(block)) return face;
      }
      return null;
   }

   /** BlockTaintFibre.isOnlyAdjacentToTaint: no solid non-taint face touches this block. */
   public static boolean isOnlyAdjacentToTaint(Level level, BlockPos pos) {
      for (Direction dir : Direction.values()) {
         BlockPos p = pos.relative(dir);
         BlockState bs = level.getBlockState(p);
         if (!bs.isAir() && !isTaint(bs) && bs.isFaceSturdy(level, p, dir.getOpposite())) return false;
      }
      return true;
   }

   /** BlockTaintFibre.isHemmedByTaint. */
   public static boolean isHemmedByTaint(Level level, BlockPos pos) {
      int c = 0;
      for (Direction dir : Direction.values()) {
         BlockPos p = pos.relative(dir);
         BlockState block = level.getBlockState(p);
         if (isTaint(block)) {
            c++;
         } else if (block.isAir()) {
            c--;
         } else if (block.getFluidState().isEmpty() && !block.isFaceSturdy(level, p, dir.getOpposite())) {
            c--;
         }
      }
      return c > 0;
   }

   private static boolean isPlant(BlockState bs) {
      return bs.getBlock() instanceof BushBlock || bs.is(BlockTags.FLOWERS);
   }

   private static boolean isWoodish(BlockState bs) {
      return bs.is(Blocks.RED_MUSHROOM_BLOCK) || bs.is(Blocks.BROWN_MUSHROOM_BLOCK) || bs.is(Blocks.MUSHROOM_STEM) || bs.is(Blocks.PUMPKIN)
         || bs.is(Blocks.CARVED_PUMPKIN) || bs.is(Blocks.JACK_O_LANTERN) || bs.is(Blocks.MELON) || bs.is(Blocks.CACTUS) || bs.is(BlockTags.CORAL_BLOCKS)
         || bs.is(Blocks.SPONGE) || bs.is(Blocks.WET_SPONGE) || bs.instrument() == NoteBlockInstrument.BASS && bs.ignitedByLava();
   }

   private static boolean isSoilish(BlockState bs) {
      return bs.is(BlockTags.DIRT) || bs.is(BlockTags.SAND) || bs.is(Blocks.GRAVEL) || bs.is(Blocks.CLAY) || bs.is(Blocks.FARMLAND)
         || bs.is(Blocks.DIRT_PATH) || bs.instrument() == NoteBlockInstrument.SNARE;
   }

   private static boolean isRock(BlockState bs) {
      return bs.instrument() == NoteBlockInstrument.BASEDRUM && bs.requiresCorrectToolForDrops();
   }

   // ------------------------------------------------------------------ spreading

   public static void spreadFibres(Level level, BlockPos pos) {
      spreadFibres(level, pos, false);
   }

   public static void spreadFibres(Level level, BlockPos pos, boolean ignore) {
      if (!ignore && ModConfig.wussMode()) return;
      float mod = 0.001F + AuraHandler.getFluxSaturation(level, pos) * 2.0F;
      if (!ignore && level.random.nextFloat() > taintSpreadRate / 100.0F * mod) return;
      if (!isNearTaintSeed(level, pos)) return;
      BlockPos t = pos.offset(level.random.nextInt(3) - 1, level.random.nextInt(3) - 1, level.random.nextInt(3) - 1);
      if (t.equals(pos)) return;
      BlockState bs = level.getBlockState(t);
      float bh = bs.getDestroySpeed(level, t);
      if (bh < 0.0F || bh > 10.0F) return;

      if (!isLeaves(bs) && bs.getFluidState().isEmpty() && (bs.isAir() || bs.canBeReplaced() || isPlant(bs))
         && isAdjacentToSolidBlock(level, t) && !isOnlyAdjacentToTaint(level, t)) {
         placeTaint(level, t, ModFlux.TAINT_FIBRE.get().defaultBlockState());
         return;
      }

      if (isLeaves(bs)) {
         Direction face;
         if (level.random.nextFloat() < 0.6 && (face = getFaceBlockTouching(level, t, ModFlux.TAINT_LOG.get())) != null) {
            level.setBlockAndUpdate(t, ModFlux.TAINT_FEATURE.get().defaultBlockState().setValue(TaintFeatureBlock.FACING, face.getOpposite()));
         } else {
            placeTaint(level, t, ModFlux.TAINT_FIBRE.get().defaultBlockState());
         }
         return;
      }

      if (isHemmedByTaint(level, t) && bh < 5.0F) {
         if (isWoodLog(level, t) && !isTaint(bs)) {
            var axis = bs.hasProperty(RotatedPillarBlock.AXIS) ? bs.getValue(RotatedPillarBlock.AXIS) : Direction.Axis.Y;
            level.setBlockAndUpdate(t, ModFlux.TAINT_LOG.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, axis));
            return;
         }
         if (isWoodish(bs)) {
            placeTaint(level, t, ModFlux.TAINT_CRUST.get().defaultBlockState());
            return;
         }
         if (isSoilish(bs)) {
            placeTaint(level, t, ModFlux.TAINT_SOIL.get().defaultBlockState());
            return;
         }
         if (isRock(bs)) {
            placeTaint(level, t, ModFlux.TAINT_ROCK.get().defaultBlockState());
            return;
         }
      }

      if ((bs.is(ModFlux.TAINT_SOIL.get()) || bs.is(ModFlux.TAINT_ROCK.get())) && level.isEmptyBlock(t.above())
         && AuraHandler.getFlux(level, t) >= 5.0F && level.random.nextFloat() < taintSpreadRate / 100.0F * 0.33 && isAtTaintSeedEdge(level, t)) {
         TaintSeedEntity e = new TaintSeedEntity(ModFlux.TAINT_SEED.get(), level);
         e.moveTo(t.getX() + 0.5, t.above().getY(), t.getZ() + 0.5, level.random.nextInt(360), 0.0F);
         if (e.canSpawnHere()) {
            AuraHandler.drainFlux(level, t, 5.0F, false);
            level.addFreshEntity(e);
         }
      }
   }

   private static void placeTaint(Level level, BlockPos t, BlockState state) {
      level.setBlockAndUpdate(t, state);
      level.blockEvent(t, state.getBlock(), 1, 0);
      AuraHandler.drainFlux(level, t, 0.01F, false);
   }
}
