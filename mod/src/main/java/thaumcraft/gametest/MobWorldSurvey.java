package thaumcraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import thaumcraft.Thaumcraft;
import thaumcraft.common.world.MobWorldgen;

/** ./gradlew runSurvey: also reports the nearest magical forest and the ambient grass / vishrooms decorating it (milestone 13). */
@EventBusSubscriber(modid = Thaumcraft.MODID)
public final class MobWorldSurvey {
   private MobWorldSurvey() {
   }

   @SubscribeEvent(priority = EventPriority.HIGH)
   public static void started(ServerStartedEvent event) {
      if (!Boolean.getBoolean("thaumcraft.worldgenSurvey")) return;
      ServerLevel level = event.getServer().overworld();
      BlockPos spawn = level.getSharedSpawnPos();
      var found = level.findClosestBiome3d(b -> b.is(MobWorldgen.MAGICAL_FOREST), spawn, 6400, 32, 64);
      if (found == null) {
         Thaumcraft.LOGGER.info("[survey] no magical forest within 6400 blocks of spawn");
         return;
      }
      BlockPos p = found.getFirst();
      int grass = 0, shrooms = 0;
      for (int cx = -2; cx <= 2; cx++) {
         for (int cz = -2; cz <= 2; cz++) {
            LevelChunk c = level.getChunk((p.getX() >> 4) + cx, (p.getZ() >> 4) + cz);
            for (BlockPos b : BlockPos.betweenClosed(c.getPos().getMinBlockX(), level.getMinBuildHeight(), c.getPos().getMinBlockZ(),
               c.getPos().getMaxBlockX(), level.getMaxBuildHeight() - 1, c.getPos().getMaxBlockZ())) {
               var s = c.getBlockState(b);
               if (s.is(MobWorldgen.GRASS_AMBIENT.get())) grass++;
               else if (s.is(thaumcraft.common.registers.ModBlocks.VISHROOM.get())) shrooms++;
            }
         }
      }
      Thaumcraft.LOGGER.info("[survey] magical forest at {} ({} blocks from spawn): {} ambient grass, {} vishrooms in 5x5 chunks", p,
         (int) Math.sqrt(p.distSqr(spawn)), grass, shrooms);
   }
}
