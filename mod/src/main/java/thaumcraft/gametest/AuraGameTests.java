package thaumcraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import thaumcraft.Thaumcraft;
import thaumcraft.common.blocks.world.CrystalBlock;
import thaumcraft.common.registers.ModBlocks;
import thaumcraft.common.world.aura.AuraChunk;
import thaumcraft.common.world.aura.AuraHandler;

/** Milestone 3: the aura. */
@GameTestHolder(Thaumcraft.MODID)
@PrefixGameTestTemplate(false)
public class AuraGameTests {
   private static AuraChunk aura(GameTestHelper h, BlockPos rel) {
      AuraChunk ac = AuraHandler.getAuraChunk(h.getLevel(), h.absolutePos(rel));
      h.assertTrue(ac != null, "no aura in a loaded chunk");
      return ac;
   }

   @GameTest(template = "empty")
   public static void loadedChunksHaveAura(GameTestHelper h) {
      AuraChunk ac = aura(h, new BlockPos(2, 1, 2));
      h.assertTrue(ac.getBase() > 0 && ac.getBase() <= AuraHandler.AURA_CEILING, "base out of range: " + ac);
      h.succeed();
   }

   @GameTest(template = "empty")
   public static void drainAndAddVis(GameTestHelper h) {
      ServerLevel level = h.getLevel();
      BlockPos pos = h.absolutePos(new BlockPos(2, 1, 2));
      AuraChunk ac = aura(h, new BlockPos(2, 1, 2));
      ac.setVis(100);
      ac.setFlux(0);
      h.assertTrue(AuraHandler.drainVis(level, pos, 30, true) == 30 && ac.getVis() == 100, "simulated drain changed the aura");
      h.assertTrue(AuraHandler.drainVis(level, pos, 30, false) == 30 && ac.getVis() == 70, "drain 30: " + ac);
      h.assertTrue(AuraHandler.drainVis(level, pos, 500, false) == 70 && ac.getVis() == 0, "drain more than there is: " + ac);
      AuraHandler.addVis(level, pos, 12.5F);
      AuraHandler.addFlux(level, pos, 4);
      h.assertTrue(ac.getVis() == 12.5F && ac.getFlux() == 4, "add: " + ac);
      h.assertTrue(AuraHandler.drainFlux(level, pos, 10, false) == 4 && ac.getFlux() == 0, "drain flux: " + ac);
      h.succeed();
   }

   @GameTest(template = "empty")
   public static void auraSpreadsAndRegenerates(GameTestHelper h) {
      // No moon modifiers: phaseVis 0.25 (full moon regen), phaseMax 1.
      AuraHandler.Simulation sim = new AuraHandler.Simulation(0.25F, 1.0F, RandomSource.create(1));
      AuraChunk full = new AuraChunk((short) 100, 100, 0);
      AuraChunk poor = new AuraChunk((short) 100, 10, 0);
      sim.process(full, new AuraChunk[]{poor, null, null, null});
      h.assertTrue(poor.getVis() == 11, "1 vis should flow to the poorer neighbour: " + poor);
      h.assertTrue(full.getVis() == 99.25F, "the giver loses 1 and regenerates 0.25: " + full);

      AuraChunk fluxy = new AuraChunk((short) 100, 100, 50);
      AuraChunk clean = new AuraChunk((short) 100, 100, 0);
      sim.process(fluxy, new AuraChunk[]{null, clean, null, null});
      h.assertTrue(clean.getFlux() == 1 && fluxy.getFlux() == 49, "flux spreads too: " + fluxy + " / " + clean);

      AuraChunk low = new AuraChunk((short) 100, 50, 0);
      sim.process(low, new AuraChunk[4]);
      h.assertTrue(low.getVis() == 50.25F, "regenerates towards base: " + low);
      h.succeed();
   }

   private static BlockPos crystalOnStone(GameTestHelper h, int size) {
      BlockPos rock = new BlockPos(2, 1, 2);
      h.setBlock(rock, Blocks.STONE);
      CrystalBlock c = ModBlocks.CRYSTAL_TERRA.get();
      h.setBlock(rock.above(), c.worldgenState(h.getLevel(), h.absolutePos(rock.above()), size));
      return rock.above();
   }

   private static void tickUntil(GameTestHelper h, BlockPos rel, java.util.function.Predicate<BlockState> done, String what) {
      ServerLevel level = h.getLevel();
      BlockPos abs = h.absolutePos(rel);
      for (int i = 0; i < 400 && !done.test(level.getBlockState(abs)); i++) {
         level.getBlockState(abs).randomTick(level, abs, level.random);
      }
      h.assertTrue(done.test(level.getBlockState(abs)), what + ", state: " + level.getBlockState(abs));
   }

   @GameTest(template = "empty")
   public static void crystalGrowsOnVis(GameTestHelper h) {
      BlockPos at = crystalOnStone(h, 0);
      AuraChunk ac = aura(h, at);
      ac.setVis(ac.getBase() + 100);
      float before = ac.getVis();
      tickUntil(h, at, s -> s.getValue(CrystalBlock.SIZE) == 1, "crystal did not grow with plenty of vis");
      h.assertTrue(ac.getVis() == before - 10, "growing should cost 10 vis: " + before + " -> " + ac.getVis());
      h.succeed();
   }

   @GameTest(template = "empty")
   public static void crystalShrinksWithoutVis(GameTestHelper h) {
      BlockPos at = crystalOnStone(h, 2);
      AuraChunk ac = aura(h, at);
      ac.setVis(0);
      tickUntil(h, at, s -> s.getValue(CrystalBlock.SIZE) == 1, "crystal did not shrink in an empty aura");
      h.assertTrue(ac.getVis() == 10, "shrinking gives 10 vis back: " + ac.getVis());
      h.succeed();
   }

   @GameTest(template = "empty")
   public static void silverwoodLeavesRefillAura(GameTestHelper h) {
      BlockPos at = new BlockPos(2, 2, 2);
      h.setBlock(at.below(), ModBlocks.LOG_SILVERWOOD.get());
      h.setBlock(at, ModBlocks.LEAVES_SILVERWOOD.get().defaultBlockState().setValue(net.minecraft.world.level.block.LeavesBlock.DISTANCE, 1));
      AuraChunk ac = aura(h, at);
      ac.setVis(0);
      BlockPos abs = h.absolutePos(at);
      h.getLevel().getBlockState(abs).randomTick(h.getLevel(), abs, h.getLevel().random);
      h.assertTrue(ac.getVis() > 0, "natural silverwood leaves should add vis: " + ac);
      h.succeed();
   }
}
