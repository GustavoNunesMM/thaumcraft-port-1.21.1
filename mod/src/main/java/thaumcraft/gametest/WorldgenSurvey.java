package thaumcraft.gametest;

import java.util.Map;
import java.util.TreeMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import thaumcraft.Thaumcraft;

/**
 * Dev-only (./gradlew runSurvey): generates the chunks around spawn of a fresh world, logs how many of
 * each Thaumcraft block world generation placed (ores also by height), then stops the server.
 */
@EventBusSubscriber(modid = Thaumcraft.MODID)
public final class WorldgenSurvey {
   private static final int RADIUS = 12; // chunks: a 24x24 chunk area (384x384 blocks) around spawn
   // Silverwood is rare (1 try in 80 chunks, then the biome check): -PtcSurveyForestRadius=32 gives it a fair sample.
   private static final int FOREST_RADIUS = Integer.getInteger("thaumcraft.surveyForestRadius", 8);

   private WorldgenSurvey() {
   }

   @SubscribeEvent
   public static void started(ServerStartedEvent event) {
      if (!Boolean.getBoolean("thaumcraft.worldgenSurvey")) return;
      MinecraftServer server = event.getServer();
      ServerLevel level = server.overworld();
      BlockPos spawn = level.getSharedSpawnPos();
      survey(level, "spawn", spawn, RADIUS);
      // Greatwood and silverwood only grow in some biomes: also look at the nearest forest.
      var forest = level.findClosestBiome3d(b -> b.is(Tags.Biomes.IS_FOREST), spawn, 6400, 32, 64);
      if (forest != null) {
         survey(level, "forest (" + forest.getSecond().unwrapKey().map(k -> k.location().getPath()).orElse("?") + ")", forest.getFirst(), FOREST_RADIUS);
      } else {
         Thaumcraft.LOGGER.info("[survey] no forest found near spawn");
      }
      Thaumcraft.LOGGER.info("[survey] DONE");
      server.halt(false);
   }

   private static void survey(ServerLevel level, String label, BlockPos center, int radius) {
      int scx = center.getX() >> 4, scz = center.getZ() >> 4;
      Map<String, Integer> counts = new TreeMap<>();
      Map<String, int[]> oreHeights = new TreeMap<>(); // ore -> [min, max, sum]
      Map<String, String> firstAt = new TreeMap<>();
      long start = System.currentTimeMillis();
      for (int cx = scx - radius; cx < scx + radius; cx++) {
         for (int cz = scz - radius; cz < scz + radius; cz++) {
            LevelChunk chunk = level.getChunk(cx, cz);
            LevelChunkSection[] sections = chunk.getSections();
            for (int si = 0; si < sections.length; si++) {
               LevelChunkSection sec = sections[si];
               if (sec.hasOnlyAir() || !sec.maybeHas(WorldgenSurvey::isOurs)) continue;
               int baseY = chunk.getSectionYFromSectionIndex(si) << 4;
               for (int x = 0; x < 16; x++) {
                  for (int y = 0; y < 16; y++) {
                     for (int z = 0; z < 16; z++) {
                        BlockState s = sec.getBlockState(x, y, z);
                        if (!isOurs(s)) continue;
                        String name = BuiltInRegistries.BLOCK.getKey(s.getBlock()).getPath();
                        counts.merge(name, 1, Integer::sum);
                        // Leaves that don't know a log within 6 blocks decay on the next random ticks.
                        if (s.getBlock() instanceof LeavesBlock && !s.getValue(LeavesBlock.PERSISTENT)
                           && s.getValue(LeavesBlock.DISTANCE) >= LeavesBlock.DECAY_DISTANCE) {
                           counts.merge(name + " (decaying)", 1, Integer::sum);
                           firstAt.putIfAbsent(name + " (decaying)", (cx * 16 + x) + " " + (baseY + y) + " " + (cz * 16 + z));
                        }
                        int wy = baseY + y;
                        firstAt.putIfAbsent(name, (cx * 16 + x) + " " + wy + " " + (cz * 16 + z));
                        if (name.contains("ore")) {
                           int[] h = oreHeights.computeIfAbsent(name, k -> new int[]{Integer.MAX_VALUE, Integer.MIN_VALUE, 0});
                           h[0] = Math.min(h[0], wy);
                           h[1] = Math.max(h[1], wy);
                           h[2] += wy;
                        }
                     }
                  }
               }
            }
         }
      }
      int chunks = 4 * radius * radius;
      Thaumcraft.LOGGER.info("[survey] {}: {} chunks around {} in {} ms", label, chunks, center, System.currentTimeMillis() - start);
      counts.forEach((name, n) -> {
         int[] h = oreHeights.get(name);
         String heights = h == null ? "" : String.format(" y %d..%d (avg %d)", h[0], h[1], h[2] / n);
         Thaumcraft.LOGGER.info("[survey]   {}: {} ({} per chunk){} first at {}", name, n, String.format("%.2f", n / (double) chunks), heights, firstAt.get(name));
      });
   }

   private static boolean isOurs(BlockState s) {
      return BuiltInRegistries.BLOCK.getKey(s.getBlock()).getNamespace().equals(Thaumcraft.MODID);
   }
}
