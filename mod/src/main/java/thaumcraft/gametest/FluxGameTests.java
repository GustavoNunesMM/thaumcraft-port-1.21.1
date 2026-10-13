package thaumcraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import thaumcraft.Thaumcraft;
import thaumcraft.common.blocks.crafting.VoidSiphonBlockEntity;
import thaumcraft.common.blocks.devices.CondenserBlockEntity;
import thaumcraft.common.blocks.world.taint.FluxGooBlock;
import thaumcraft.common.blocks.world.taint.TaintHelper;
import thaumcraft.common.entities.FluxRiftEntity;
import thaumcraft.common.entities.monster.tainted.TaintSeedEntity;
import thaumcraft.common.registers.ModBlocks;
import thaumcraft.common.registers.ModFlux;
import thaumcraft.common.world.aura.AuraChunk;
import thaumcraft.common.world.aura.AuraHandler;

/** Milestone 12: flux and taint. */
@GameTestHolder(Thaumcraft.MODID)
@PrefixGameTestTemplate(false)
public class FluxGameTests {
   private static AuraChunk aura(GameTestHelper h, BlockPos rel) {
      AuraChunk ac = AuraHandler.getAuraChunk(h.getLevel(), h.absolutePos(rel));
      h.assertTrue(ac != null, "no aura in a loaded chunk");
      return ac;
   }

   private static FluxRiftEntity rift(GameTestHelper h, BlockPos rel, int size, float stability) {
      FluxRiftEntity rift = new FluxRiftEntity(ModFlux.FLUX_RIFT.get(), h.getLevel());
      rift.setRiftSeed(12345);
      Vec3 p = h.absoluteVec(Vec3.atCenterOf(rel));
      rift.moveTo(p.x, p.y, p.z, 0.0F, 0.0F);
      rift.setRiftSize(size);
      rift.setRiftStability(stability);
      h.getLevel().addFreshEntity(rift);
      return rift;
   }

   // ------------------------------------------------------------------ potions

   @GameTest(template = "empty", timeoutTicks = 100)
   public static void fluxTaintHurtsLivingAndSparesUndead(GameTestHelper h) {
      Pig pig = h.spawn(EntityType.PIG, new BlockPos(1, 1, 1));
      Zombie zombie = h.spawn(EntityType.HUSK, new BlockPos(3, 1, 3));
      zombie.setNoAi(true);
      pig.addEffect(new MobEffectInstance(ModFlux.FLUX_TAINT, 200, 1));
      zombie.addEffect(new MobEffectInstance(ModFlux.FLUX_TAINT, 200, 1));
      h.assertTrue(pig.hasEffect(ModFlux.FLUX_TAINT), "flux taint registered and applied");
      h.assertTrue(net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.containsKey(
         net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "vis_exhaust")), "thaumcraft:vis_exhaust id");
      float pigHealth = pig.getHealth();
      float zombieHealth = zombie.getHealth();
      h.runAfterDelay(45, () -> {
         h.assertTrue(pig.getHealth() < pigHealth, "pig should be hurt by taint: " + pig.getHealth());
         h.assertTrue(zombie.getHealth() >= zombieHealth, "undead are spared");
         h.succeed();
      });
   }

   @GameTest(template = "empty", timeoutTicks = 100)
   public static void fluxPhageSpreads(GameTestHelper h) {
      Pig a = h.spawn(EntityType.PIG, new BlockPos(1, 1, 1));
      Pig b = h.spawn(EntityType.PIG, new BlockPos(3, 1, 3));
      a.addEffect(new MobEffectInstance(ModFlux.INFECTIOUS_VIS_EXHAUST, 200, 1));
      h.succeedWhen(() -> {
         MobEffectInstance e = b.getEffect(ModFlux.INFECTIOUS_VIS_EXHAUST);
         h.assertTrue(e != null && e.getAmplifier() == 0, "the phage should spread one level weaker");
      });
   }

   // ------------------------------------------------------------------ rifts

   @GameTest(template = "empty")
   public static void saturatedFluxTriggersRift(GameTestHelper h) {
      AuraHandler.Simulation sim = new AuraHandler.Simulation(0.0F, 1.0F, RandomSource.create(7));
      boolean rift = false;
      for (int i = 0; i < 500 && !rift; i++) rift = sim.process(new AuraChunk((short) 100, 100, 500), new AuraChunk[4]).rift();
      h.assertTrue(rift, "a flux saturated chunk should trigger a rift");
      AuraChunk clean = new AuraChunk((short) 100, 100, 0);
      for (int i = 0; i < 200; i++) h.assertTrue(!sim.process(clean, new AuraChunk[4]).rift(), "no rift without flux");
      h.succeed();
   }

   @GameTest(template = "trees", timeoutTicks = 100, batch = "flux_rift_trigger")
   public static void riftTriggerSpawnsRift(GameTestHelper h) {
      ServerLevel level = h.getLevel();
      BlockPos rel = new BlockPos(16, 1, 16);
      BlockPos abs = h.absolutePos(rel);
      AuraChunk ac = aura(h, rel);
      ac.setFlux(300);
      BlockPos chunkOrigin = new BlockPos(abs.getX() >> 4 << 4, 0, abs.getZ() >> 4 << 4);
      AABB column = new AABB(chunkOrigin.getX() - 1, level.getMinBuildHeight(), chunkOrigin.getZ() - 1, chunkOrigin.getX() + 17, level.getMaxBuildHeight(),
         chunkOrigin.getZ() + 17);
      level.getEntitiesOfClass(FluxRiftEntity.class, column.inflate(40)).forEach(net.minecraft.world.entity.Entity::discard);
      AuraHandler.RIFT_TRIGGER.put(level, chunkOrigin);
      h.succeedWhen(() -> {
         var rifts = level.getEntitiesOfClass(FluxRiftEntity.class, column);
         if (rifts.isEmpty()) AuraHandler.RIFT_TRIGGER.put(level, chunkOrigin); // another flux-saturated chunk may have taken the slot
         h.assertTrue(!rifts.isEmpty(), "the rift trigger should open a rift");
         FluxRiftEntity r = rifts.get(0);
         h.assertTrue(r.getRiftSize() >= 5, "rift size from sqrt(flux*3): " + r.getRiftSize());
         h.assertTrue(ac.getFlux() < 300, "opening the rift drains flux: " + ac.getFlux());
         r.discard();
      });
   }

   @GameTest(template = "trees", timeoutTicks = 200, batch = "flux_rifts")
   public static void stabilizerCalmsRift(GameTestHelper h) {
      h.setBlock(new BlockPos(16, 1, 10), ModBlocks.STABILIZER.get());
      FluxRiftEntity r = rift(h, new BlockPos(16, 6, 16), 6, -10.0F);
      h.succeedWhen(() -> {
         h.assertTrue(r.getRiftStability() > -10.0F, "the stabilizer should add stability: " + r.getRiftStability());
         r.discard();
      });
   }

   @GameTest(template = "trees", timeoutTicks = 100, batch = "flux_rifts")
   public static void causalityCollapseDropsSeeds(GameTestHelper h) {
      FluxRiftEntity r = rift(h, new BlockPos(16, 10, 16), 9, 0.0F);
      r.setCollapse(true);
      h.succeedWhen(() -> {
         h.assertTrue(!r.isAlive(), "a collapsing rift shrinks away");
         h.assertTrue(!h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(h.absolutePos(new BlockPos(16, 10, 16))).inflate(4),
            e -> e.getItem().is(thaumcraft.common.registers.ModItems.VOID_SEED.get())).isEmpty(), "void seeds should drop");
      });
   }

   @GameTest(template = "trees", timeoutTicks = 200, batch = "flux_rifts")
   public static void voidSiphonLeechesRift(GameTestHelper h) {
      h.setBlock(new BlockPos(16, 1, 16), ModFlux.VOID_SIPHON.get());
      FluxRiftEntity r = rift(h, new BlockPos(16, 6, 16), 16, 0.0F);
      h.succeedWhen(() -> {
         VoidSiphonBlockEntity be = (VoidSiphonBlockEntity) h.getBlockEntity(new BlockPos(16, 1, 16));
         h.assertTrue(be.progress > 0, "the siphon should drain the rift");
         h.assertTrue(r.getRiftStability() < 0.0F, "draining destabilizes the rift");
         r.discard();
      });
   }

   // ------------------------------------------------------------------ taint

   @GameTest(template = "trees", timeoutTicks = 400)
   public static void taintSpreadsFromSeed(GameTestHelper h) {
      ServerLevel level = h.getLevel();
      BlockPos seedRel = new BlockPos(16, 1, 16);
      aura(h, seedRel).setFlux(200);
      TaintSeedEntity seed = h.spawn(ModFlux.TAINT_SEED.get(), seedRel);
      h.succeedWhen(() -> {
         h.assertTrue(seed.isAlive(), "seed alive");
         h.assertTrue(TaintHelper.isNearTaintSeed(level, h.absolutePos(seedRel.east(4))), "the seed registers its taint area");
         boolean found = false;
         for (BlockPos p : BlockPos.betweenClosed(h.absolutePos(seedRel.offset(-5, 0, -5)), h.absolutePos(seedRel.offset(5, 3, 5)))) {
            if (level.getBlockState(p).is(ModFlux.TAINT_FIBRE.get())) {
               found = true;
               break;
            }
         }
         h.assertTrue(found, "taint fibres should spread around the seed");
      });
   }

   @GameTest(template = "empty")
   public static void taintDiesAwayFromSeeds(GameTestHelper h) {
      BlockPos p = new BlockPos(2, 1, 2);
      h.setBlock(p, ModFlux.TAINT_FIBRE.get());
      var state = h.getBlockState(p);
      state.randomTick(h.getLevel(), h.absolutePos(p), h.getLevel().random);
      h.assertBlockNotPresent(ModFlux.TAINT_FIBRE.get(), p);
      h.setBlock(p, ModFlux.TAINT_SOIL.get());
      for (int i = 0; i < 200 && h.getBlockState(p).is(ModFlux.TAINT_SOIL.get()); i++) {
         h.getBlockState(p).randomTick(h.getLevel(), h.absolutePos(p), h.getLevel().random);
      }
      h.assertBlockPresent(Blocks.DIRT, p);
      h.succeed();
   }

   // ------------------------------------------------------------------ fluids

   @GameTest(template = "empty", timeoutTicks = 300)
   public static void fluxGooFallsAndSpreads(GameTestHelper h) {
      BlockPos p = new BlockPos(2, 4, 2);
      h.setBlock(p, ModFlux.FLUX_GOO.get());
      h.succeedWhen(() -> {
         int cells = 0;
         for (BlockPos q : BlockPos.betweenClosed(h.absolutePos(new BlockPos(0, 1, 0)), h.absolutePos(new BlockPos(4, 1, 4)))) {
            if (h.getLevel().getBlockState(q).getBlock() instanceof FluxGooBlock) cells++;
         }
         h.assertBlockNotPresent(ModFlux.FLUX_GOO.get(), p);
         h.assertTrue(cells >= 2, "goo should fall to the floor and level out: " + cells);
      });
   }

   @GameTest(template = "empty", timeoutTicks = 60)
   public static void fluxGooExhausts(GameTestHelper h) {
      h.setBlock(new BlockPos(2, 1, 2), ModFlux.FLUX_GOO.get().defaultBlockState().setValue(FluxGooBlock.LEVEL, 7));
      Pig pig = h.spawn(EntityType.PIG, new BlockPos(2, 1, 2));
      h.succeedWhen(() -> h.assertTrue(pig.hasEffect(ModFlux.VIS_EXHAUST), "wading through goo gives flux flu"));
   }

   @GameTest(template = "empty", timeoutTicks = 60)
   public static void bathSaltsPurifyWater(GameTestHelper h) {
      BlockPos w = new BlockPos(2, 1, 2);
      h.setBlock(w, Blocks.WATER);
      ItemEntity salts = new ItemEntity(h.getLevel(), h.absolutePos(w).getX() + 0.5, h.absolutePos(w).getY() + 0.2, h.absolutePos(w).getZ() + 0.5,
         new ItemStack(ModFlux.BATH_SALTS.get()));
      salts.setDeltaMovement(Vec3.ZERO);
      salts.lifespan = 2;
      h.getLevel().addFreshEntity(salts);
      h.succeedWhen(() -> h.assertBlockPresent(ModFlux.PURIFYING_FLUID_BLOCK.get(), w));
   }

   // ------------------------------------------------------------------ condenser

   @GameTest(template = "empty", timeoutTicks = 100)
   public static void condenserDrainsFlux(GameTestHelper h) {
      BlockPos c = new BlockPos(2, 1, 2);
      h.setBlock(c, ModFlux.CONDENSER.get());
      h.setBlock(c.above(), ModFlux.CONDENSER_LATTICE.get());
      h.setBlock(c.above(2), ModFlux.CONDENSER_LATTICE.get());
      AuraChunk ac = aura(h, c);
      ac.setFlux(50);
      h.runAfterDelay(3, () -> {
         CondenserBlockEntity be = (CondenserBlockEntity) h.getBlockEntity(c);
         h.assertTrue(be.latticeCount > 0.0F && be.interval > 0, "the lattice should be counted: " + be.latticeCount);
         h.assertTrue(be.cost == (int) (4.0 + Math.sqrt(2)), "cost grows with the lattice: " + be.cost);
         be.setEssentia(100);
         be.interval = 5; // the real interval (~570 ticks for two pieces) is too slow for a test
         h.succeedWhen(() -> {
            h.assertTrue(be.getFlux() > 0, "the condenser should turn aura flux into vitium");
            h.assertTrue(ac.getFlux() < 50, "flux drained from the aura: " + ac.getFlux());
            h.assertTrue(be.getEssentia() < 100, "essentia spent");
         });
      });
   }
}
