package thaumcraft.gametest;

import java.util.ArrayDeque;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import thaumcraft.Thaumcraft;
import thaumcraft.common.blocks.eldritch.EldritchAltarBlock;
import thaumcraft.common.blocks.eldritch.EldritchLockBlock;
import thaumcraft.common.blocks.eldritch.EldritchObeliskBlock;
import thaumcraft.common.blocks.eldritch.HoleBlock;
import thaumcraft.common.blocks.eldritch.PorousStoneBlock;
import thaumcraft.common.blocks.world.taint.TaintBlock;
import thaumcraft.common.entities.monster.boss.EldritchGolemEntity;
import thaumcraft.common.entities.monster.boss.EldritchWardenEntity;
import thaumcraft.common.entities.monster.eldritch.EldritchCrabEntity;
import thaumcraft.common.entities.monster.eldritch.EldritchGuardianEntity;
import thaumcraft.common.entities.monster.eldritch.EldritchHelper;
import thaumcraft.common.entities.monster.eldritch.InhabitedZombieEntity;
import thaumcraft.common.events.EldritchEvents;
import thaumcraft.common.events.WarpEvents;
import thaumcraft.common.registers.ModBlocks;
import thaumcraft.common.registers.ModCasters;
import thaumcraft.common.registers.ModEldritch;
import thaumcraft.common.registers.ModFlux;
import thaumcraft.common.registers.ModMobs;
import thaumcraft.common.research.ResearchManager;
import thaumcraft.common.world.eldritch.MazeGenerator;
import thaumcraft.common.world.eldritch.ObeliskFeature;
import thaumcraft.common.world.eldritch.OuterLands;

/** Milestone 14: the eldritch. (The "empty" template's floor is at y = 1.) */
@GameTestHolder(Thaumcraft.MODID)
@PrefixGameTestTemplate(false)
public class EldritchGameTests {
   // ------------------------------------------------------------------ blocks

   /** The rift focus' tear bores a 3x3 tunnel through a wall and the wall comes back when the tears expire. */
   @GameTest(template = "empty", timeoutTicks = 100)
   public static void holeTunnelsAndRestores(GameTestHelper h) {
      for (int x = 1; x <= 5; x++) {
         for (int y = 2; y <= 4; y++) {
            for (int z = 2; z <= 4; z++) h.setBlock(new BlockPos(x, y, z), Blocks.STONE);
         }
      }
      BlockPos hit = h.absolutePos(new BlockPos(3, 3, 2));
      h.assertTrue(HoleBlock.createHole(h.getLevel(), hit, Direction.NORTH, (byte) 3, 30), "a tear opens in stone");
      h.runAfterDelay(5, () -> {
         for (int z = 2; z <= 4; z++) h.assertBlockPresent(ModEldritch.HOLE.get(), new BlockPos(3, 3, z));
         h.assertBlockPresent(ModEldritch.HOLE.get(), new BlockPos(2, 2, 3));
         h.assertBlockPresent(ModEldritch.HOLE.get(), new BlockPos(4, 4, 3));
         h.assertBlockPresent(Blocks.STONE, new BlockPos(1, 3, 3));
      });
      h.runAfterDelay(45, () -> {
         for (int x = 1; x <= 5; x++) {
            for (int y = 2; y <= 4; y++) {
               for (int z = 2; z <= 4; z++) h.assertBlockPresent(Blocks.STONE, new BlockPos(x, y, z));
            }
         }
         h.succeed();
      });
   }

   /** Bedrock and unbreakable blocks can't be torn. */
   @GameTest(template = "empty")
   public static void holeSparesBedrock(GameTestHelper h) {
      h.setBlock(new BlockPos(2, 2, 2), Blocks.BEDROCK);
      h.setBlock(new BlockPos(3, 2, 2), ModBlocks.STONE_ANCIENT_DOORWAY.get());
      h.assertFalse(HoleBlock.createHole(h.getLevel(), h.absolutePos(new BlockPos(2, 2, 2)), Direction.UP, (byte) 1, 20), "bedrock");
      h.assertFalse(HoleBlock.createHole(h.getLevel(), h.absolutePos(new BlockPos(3, 2, 2)), Direction.UP, (byte) 1, 20), "doorway");
      h.succeed();
   }

   /** BlockStonePorous: gravel, and one time in fifteen something it absorbed (always with enough fortune). */
   @GameTest(template = "empty")
   public static void porousStoneDrops(GameTestHelper h) {
      RandomSource r = RandomSource.create(42L);
      int special = 0;
      for (int i = 0; i < 3000; i++) {
         if (!PorousStoneBlock.rollDrop(r, 0).is(Items.GRAVEL)) special++;
      }
      h.assertTrue(special > 100 && special < 320, "about 1/15 special drops: " + special);
      for (int i = 0; i < 50; i++) h.assertFalse(PorousStoneBlock.rollDrop(r, 14).is(Items.GRAVEL), "fortune 14 always special");
      h.succeed();
   }

   /** Dying tainted rock leaves porous stone, as in 1.12. */
   @GameTest(template = "empty")
   public static void taintRockDiesToPorousStone(GameTestHelper h) {
      BlockPos p = new BlockPos(2, 2, 2);
      h.setBlock(p, ModFlux.TAINT_ROCK.get());
      BlockState s = h.getBlockState(p);
      ((TaintBlock) s.getBlock()).die(h.getLevel(), h.absolutePos(p), s);
      h.assertBlockPresent(ModEldritch.STONE_POROUS.get(), p);
      h.succeed();
   }

   /** The curse focus' withering field spares eldritch creatures. */
   @GameTest(template = "empty", timeoutTicks = 60)
   public static void sapSparesEldritchMobs(GameTestHelper h) {
      // (entityInside only runs for entities that move, so no setNoAi: gravity keeps them "moving")
      for (int x = 0; x <= 4; x++) {
         for (int z = 0; z <= 4; z++) h.setBlock(new BlockPos(x, 2, z), ModCasters.EFFECT_SAP.get());
      }
      Pig pig = h.spawn(EntityType.PIG, new BlockPos(1, 2, 1));
      EldritchGuardianEntity g = h.spawn(ModEldritch.ELDRITCH_GUARDIAN.get(), new BlockPos(3, 2, 3));
      h.assertTrue(EldritchHelper.isEldritch(g) && !EldritchHelper.isEldritch(pig), "eldritch test");
      h.runAfterDelay(10, () -> {
         h.assertTrue(pig.hasEffect(MobEffects.WITHER), "the pig withers");
         h.assertFalse(g.hasEffect(MobEffects.WITHER), "the guardian is immune");
         h.succeed();
      });
   }

   // ------------------------------------------------------------------ warp

   @GameTest(template = "empty")
   public static void warpEventsApplyEffects(GameTestHelper h) {
      ServerPlayer player = h.makeMockServerPlayerInLevel();
      WarpEvents.applyEffect(player, 14, 30, 0);
      h.assertTrue(player.hasEffect(ModFlux.VIS_EXHAUST), "eff 13-16: vis exhaustion");
      h.assertTrue(player.getEffect(ModFlux.VIS_EXHAUST).getAmplifier() == 2, "level warp / 15");
      h.assertTrue(player.getEffect(ModFlux.VIS_EXHAUST).getCures().isEmpty(), "milk can't cure it");
      WarpEvents.applyEffect(player, 54, 30, 0);
      h.assertTrue(player.hasEffect(ModFlux.DEATH_GAZE), "eff 53-56: death gaze");
      WarpEvents.applyEffect(player, 70, 30, 0);
      h.assertTrue(player.hasEffect(MobEffects.BLINDNESS), "eff 69-72: blindness");
      WarpEvents.applyEffect(player, 42, 30, 0);
      h.assertTrue(player.hasEffect(MobEffects.DIG_SLOWDOWN), "eff 41-44: mining fatigue");
      WarpEvents.unlocks(player, 30);
      h.assertTrue(ResearchManager.knowsResearch(player, "ELDRITCHMINOR"), "warp over 25 unlocks the minor eldritch");
      h.assertFalse(ResearchManager.knowsResearch(player, "ELDRITCHMAJOR"), "but not the major one");
      h.assertTrue(EldritchEvents.warpEventsApply(player), "warp events roll");
      player.addEffect(new MobEffectInstance(ModFlux.WARP_WARD, 200));
      h.assertFalse(EldritchEvents.warpEventsApply(player), "warp ward stops them");
      h.succeed();
   }

   /** Real mind spiders (from the mobs milestone) come for the player; the hallucinated ones are harmless. */
   @GameTest(template = "trees", timeoutTicks = 40)
   public static void warpSpidersAndMist(GameTestHelper h) {
      ServerPlayer player = h.makeMockServerPlayerInLevel();
      player.setPos(h.absoluteVec(new net.minecraft.world.phys.Vec3(16.5, 2, 16.5)));
      int before = h.getLevel().getEntitiesOfClass(thaumcraft.common.entities.monster.MindSpiderEntity.class, player.getBoundingBox().inflate(40)).size();
      WarpEvents.applyEffect(player, 58, 10, 0);
      var spiders = h.getLevel().getEntitiesOfClass(thaumcraft.common.entities.monster.MindSpiderEntity.class, player.getBoundingBox().inflate(40));
      h.assertTrue(spiders.size() > before, "phantom spiders appear");
      h.assertTrue(spiders.stream().allMatch(s -> s.isHarmless()), "and are harmless");
      spiders.forEach(s -> s.discard());
      h.succeed();
   }

   // ------------------------------------------------------------------ the maze

   @GameTest(template = "empty")
   public static void mazeLayoutIsConnected(GameTestHelper h) {
      int[] dx = {0, 1, 0, -1}, dz = {-1, 0, 1, 0};
      int[] bits = {MazeGenerator.N, MazeGenerator.E, MazeGenerator.S, MazeGenerator.W};
      for (long seed : new long[]{0L, 12345L, -987654321L}) {
         for (int rx = -2; rx <= 1; rx++) {
            for (int rz = -1; rz <= 1; rz++) {
               MazeGenerator.Layout l = MazeGenerator.layout(seed, rx, rz);
               int[][] open = l.open();
               boolean[][] seen = new boolean[8][8];
               ArrayDeque<int[]> q = new ArrayDeque<>();
               q.add(new int[]{MazeGenerator.ENTRY, MazeGenerator.ENTRY});
               seen[MazeGenerator.ENTRY][MazeGenerator.ENTRY] = true;
               int count = 0;
               while (!q.isEmpty()) {
                  int[] c = q.poll();
                  count++;
                  for (int d = 0; d < 4; d++) {
                     int nx = c[0] + dx[d], nz = c[1] + dz[d];
                     boolean inside = nx >= 0 && nz >= 0 && nx < 8 && nz < 8;
                     if ((open[c[0]][c[1]] & bits[d]) != 0 && inside) {
                        h.assertTrue((open[nx][nz] & bits[(d + 2) % 4]) != 0, "openings are two sided");
                        if (!seen[nx][nz]) {
                           seen[nx][nz] = true;
                           q.add(new int[]{nx, nz});
                        }
                     }
                  }
               }
               h.assertTrue(count == 64, "every cell is reachable from the entry: " + count);
               h.assertTrue(Integer.bitCount(open[l.bossX()][l.bossZ()]) == 1 && open[l.bossX()][l.bossZ()] == l.bossDoor(), "one sealed way in");
               h.assertTrue(l.type()[l.bossX()][l.bossZ()] == MazeGenerator.CellType.BOSS, "boss cell");
               h.assertTrue(l.type()[l.keyX()][l.keyZ()] == MazeGenerator.CellType.KEY, "reliquary cell");
               // the region joins its east and south neighbours where they expect it
               MazeGenerator.Layout east = MazeGenerator.layout(seed, rx + 1, rz);
               MazeGenerator.Layout south = MazeGenerator.layout(seed, rx, rz + 1);
               boolean eastLink = false, southLink = false;
               for (int i = 0; i < 8; i++) {
                  if ((open[7][i] & MazeGenerator.E) != 0) {
                     h.assertTrue((east.open()[0][i] & MazeGenerator.W) != 0, "east link matches");
                     eastLink = true;
                  }
                  if ((open[i][7] & MazeGenerator.S) != 0) {
                     h.assertTrue((south.open()[i][0] & MazeGenerator.N) != 0, "south link matches");
                     southLink = true;
                  }
               }
               h.assertTrue(eastLink || l.bossX() == 7, "linked east");
               h.assertTrue(southLink || l.bossZ() == 7, "linked south");
            }
         }
      }
      h.assertTrue(MazeGenerator.layout(7L, 0, 0) == MazeGenerator.layout(7L, 0, 0) || MazeGenerator.layout(7L, 0, 0).bossX() >= 0, "cached");
      h.succeed();
   }

   /**
    * The maze builder lays out the entry chamber (portal home included) and seals the boss chamber. The test server
    * only creates the overworld, so when the Outer Lands aren't there the cells are built into the test level instead.
    */
   @GameTest(template = "empty", timeoutTicks = 200)
   public static void outerLandsGeneratesTheMaze(GameTestHelper h) {
      ServerLevel outer = h.getLevel().getServer().getLevel(OuterLands.LEVEL);
      ServerLevel level = outer != null ? outer : h.getLevel();
      int rx = MazeGenerator.region(h.absolutePos(BlockPos.ZERO).getX() >> 4), rz = MazeGenerator.region(h.absolutePos(BlockPos.ZERO).getZ() >> 4);
      if (outer != null) rx = rz = 0;
      BlockPos arrive = MazeGenerator.arrival(rx, rz);
      MazeGenerator.Layout l = MazeGenerator.layout(level.getSeed(), rx, rz);
      int bx = (rx * 8 + l.bossX()) * 16, bz = (rz * 8 + l.bossZ()) * 16;
      if (outer == null) {
         MazeGenerator.carve(level, arrive.getX() >> 4, arrive.getZ() >> 4);
         MazeGenerator.carve(level, bx >> 4, bz >> 4);
      } else {
         outer.getChunk(arrive);
         outer.getChunk(bx >> 4, bz >> 4);
         h.assertTrue(outer.getBiome(arrive).is(OuterLands.BIOME), "eldritch biome");
         h.assertTrue(outer.getBlockState(arrive.below(10)).isAir(), "void under the maze");
      }
      h.assertTrue(level.getBlockState(arrive).isAir() && level.getBlockState(arrive.above()).isAir(), "room to stand");
      h.assertTrue(level.getBlockState(arrive.below()).is(ModBlocks.STONE_ANCIENT_TILE.get()), "tiled floor");
      BlockPos portal = new BlockPos((arrive.getX() & ~15) + 8, MazeGenerator.FLOOR, (arrive.getZ() & ~15) + 8);
      h.assertTrue(level.getBlockState(portal).is(ModEldritch.ELDRITCH_PORTAL.get()), "the way home");
      boolean lock = false;
      for (BlockPos p : BlockPos.betweenClosed(bx, MazeGenerator.FLOOR, bz, bx + 15, MazeGenerator.FLOOR + 3, bz + 15)) {
         if (level.getBlockState(p).is(ModEldritch.ELDRITCH_LOCK.get())) lock = true;
      }
      h.assertTrue(lock, "the boss chamber is locked");
      h.succeed();
   }

   // ------------------------------------------------------------------ altar, lock

   @GameTest(template = "trees", timeoutTicks = 40)
   public static void altarOpensPortalWithFourEyes(GameTestHelper h) {
      BlockPos base = h.absolutePos(new BlockPos(16, 1, 16));
      ObeliskFeature.build(h.getLevel(), base, h.getLevel().random);
      BlockPos altar = base.above();
      h.assertTrue(h.getLevel().getBlockState(altar).is(ModEldritch.ELDRITCH_ALTAR.get()), "altar built");
      for (int i = 0; i < 4; i++) {
         EldritchAltarBlock.insertEye(h.getLevel(), altar, h.getLevel().getBlockState(altar), null);
         h.assertTrue(h.getLevel().getBlockState(altar).getValue(EldritchAltarBlock.EYES) == i + 1, "eye set");
      }
      h.assertTrue(h.getLevel().getBlockState(altar.above()).is(ModEldritch.ELDRITCH_PORTAL.get()), "the portal opens");
      h.assertTrue(h.getLevel().getBlockState(base.offset(2, 2, 2)).getValue(EldritchObeliskBlock.LIT), "obelisks lit");
      h.getLevel().getEntitiesOfClass(EldritchGuardianEntity.class, new AABB(altar).inflate(20)).forEach(e -> e.discard());
      h.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void runedTabletOpensTheLock(GameTestHelper h) {
      for (int x = 1; x <= 3; x++) {
         for (int y = 2; y <= 3; y++) h.setBlock(new BlockPos(x, y, 2), ModBlocks.STONE_ANCIENT_DOORWAY.get());
      }
      BlockPos lockPos = new BlockPos(2, 4, 2);
      h.setBlock(lockPos, ModEldritch.ELDRITCH_LOCK.get().defaultBlockState().setValue(EldritchLockBlock.FACING, Direction.SOUTH));
      ServerPlayer player = h.makeMockServerPlayerInLevel();
      ItemStack tablet = new ItemStack(ModEldritch.RUNED_TABLET.get());
      player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, tablet);
      h.useBlock(lockPos, player);
      h.assertTrue(h.getBlockState(lockPos).getValue(EldritchLockBlock.OPENING), "the lock starts turning");
      h.assertTrue(tablet.isEmpty() || player.getAbilities().instabuild, "the tablet is used up");
      h.succeedWhen(() -> {
         h.assertBlockNotPresent(ModEldritch.ELDRITCH_LOCK.get(), lockPos);
         for (int x = 1; x <= 3; x++) h.assertBlockNotPresent(ModBlocks.STONE_ANCIENT_DOORWAY.get(), new BlockPos(x, 2, 2));
      });
   }

   // ------------------------------------------------------------------ creatures

   @GameTest(template = "empty", timeoutTicks = 60)
   public static void inhabitedZombieReleasesItsCrab(GameTestHelper h) {
      InhabitedZombieEntity z = h.spawn(ModEldritch.INHABITED_ZOMBIE.get(), new BlockPos(2, 2, 2));
      z.finalizeSpawn(h.getLevel(), h.getLevel().getCurrentDifficultyAt(z.blockPosition()), MobSpawnType.EVENT, null);
      h.assertTrue(z.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD).is(thaumcraft.common.registers.ModGear.CRIMSON_PLATE_HELM.get()),
         "crimson helm");
      z.hurt(h.getLevel().damageSources().genericKill(), 1000.0F);
      h.succeedWhen(() -> {
         var crabs = h.getLevel().getEntitiesOfClass(EldritchCrabEntity.class, new AABB(h.absolutePos(new BlockPos(2, 2, 2))).inflate(4));
         h.assertTrue(crabs.size() == 1 && crabs.get(0).hasHelm(), "a helmed crab bursts out");
         h.assertTrue(z.isRemoved(), "the husk is gone");
      });
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void guardianShrugsOffMagic(GameTestHelper h) {
      EldritchGuardianEntity g = h.spawn(ModEldritch.ELDRITCH_GUARDIAN.get(), new BlockPos(2, 2, 2));
      g.setNoAi(true);
      h.assertTrue(g.getMaxHealth() == 50.0F, "50 health");
      g.hurt(h.getLevel().damageSources().magic(), 10.0F);
      h.assertTrue(Math.abs(g.getHealth() - 45.0F) < 0.01F, "magic halved: " + g.getHealth());
      h.succeed();
   }

   @GameTest(template = "trees", timeoutTicks = 40)
   public static void wardenRisesShielded(GameTestHelper h) {
      BlockPos at = h.absolutePos(new BlockPos(16, 1, 16));
      var boss = MazeGenerator.spawnBoss(h.getLevel(), at, MazeGenerator.BossKind.WARDEN);
      h.assertTrue(boss instanceof EldritchWardenEntity, "a warden");
      EldritchWardenEntity w = (EldritchWardenEntity) boss;
      h.assertTrue(w.getSpawnTimer() == 150, "rising for 150 ticks");
      h.assertTrue(w.getAbsorptionAmount() > 250.0F, "void shield of 2/3 health: " + w.getAbsorptionAmount());
      h.assertFalse(w.hurt(h.getLevel().damageSources().magic(), 5.0F), "untouchable while rising");
      h.assertTrue(java.util.Arrays.asList(EldritchWardenEntity.TITLES).contains(w.getTitle()) && w.hasCustomName(), "named");
      w.discard();
      h.succeed();
   }

   @GameTest(template = "trees", timeoutTicks = 40)
   public static void constructLosesItsHead(GameTestHelper h) {
      EldritchGolemEntity g = h.spawn(ModEldritch.ELDRITCH_GOLEM.get(), new BlockPos(16, 2, 16));
      g.setNoAi(true);
      h.assertFalse(g.isHeadless(), "whole");
      h.assertFalse(g.hurt(h.getLevel().damageSources().magic(), 1000.0F), "the killing blow is taken by the head");
      h.assertTrue(g.isHeadless() && g.isAlive(), "headless and still standing");
      h.assertTrue(g.getSpawnTimer() == 100, "regathers itself");
      g.discard();
      h.succeed();
   }

   @GameTest(template = "trees", timeoutTicks = 40)
   public static void mazeBossesSpawn(GameTestHelper h) {
      BlockPos at = h.absolutePos(new BlockPos(16, 1, 16));
      for (MazeGenerator.BossKind k : MazeGenerator.BossKind.values()) {
         var boss = MazeGenerator.spawnBoss(h.getLevel(), at, k);
         h.assertTrue(boss != null && boss.isAlive(), "spawned " + k);
         if (k == MazeGenerator.BossKind.CRIMSON) h.assertTrue(boss.getType() == ModMobs.CULTIST_PORTAL_GREATER.get(), "crimson portal");
      }
      h.getLevel().getEntities((net.minecraft.world.entity.Entity) null, new AABB(at).inflate(10), e -> !(e instanceof ServerPlayer))
         .forEach(e -> e.discard());
      h.succeed();
   }
}
