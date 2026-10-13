package thaumcraft.common.world.aura;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import thaumcraft.Thaumcraft;

/** ChunkEvents + the aura part of ServerEvents.worldTick. */
@EventBusSubscriber(modid = Thaumcraft.MODID)
public final class AuraEvents {
   private AuraEvents() {
   }

   @SubscribeEvent
   public static void chunkLoad(ChunkEvent.Load event) {
      if (event.getLevel() instanceof ServerLevel level && event.getChunk() instanceof LevelChunk chunk) {
         AuraHandler.chunkLoaded(level, chunk);
      }
   }

   @SubscribeEvent
   public static void chunkUnload(ChunkEvent.Unload event) {
      if (event.getLevel() instanceof ServerLevel level && event.getChunk() instanceof LevelChunk chunk) {
         AuraHandler.chunkUnloaded(level, chunk);
      }
   }

   @SubscribeEvent
   public static void levelUnload(LevelEvent.Unload event) {
      if (event.getLevel() instanceof ServerLevel level) AuraHandler.levelUnloaded(level);
   }

   @SubscribeEvent
   public static void levelTick(LevelTickEvent.Post event) {
      if (event.getLevel() instanceof ServerLevel level && level.getGameTime() % 20 == 0) {
         AuraHandler.tick(level);
      }
   }
}
