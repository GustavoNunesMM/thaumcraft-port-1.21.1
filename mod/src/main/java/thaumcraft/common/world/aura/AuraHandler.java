package thaumcraft.common.world.aura;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import thaumcraft.common.registers.ModAttachments;
import thaumcraft.common.world.BiomeHandler;

/**
 * Port of AuraHandler + AuraThread. Every loaded chunk has an {@link AuraChunk}; once a second the server evens vis
 * and flux out between neighbouring chunks, regenerates vis towards the chunk's base (faster around the full moon)
 * and slowly turns stagnant vis into flux.
 *
 * <p>Port changes: the simulation runs on the server thread (1.12 used a separate thread per dimension), and it is
 * paced by game time, so it keeps running with the daylight cycle turned off.
 */
public final class AuraHandler {
   public static final int AURA_CEILING = 500;
   private static final float[] PHASE_TABLE = {0.25F, 0.15F, 0.1F, 0.05F, 0.0F, 0.05F, 0.1F, 0.15F};
   private static final float[] MAX_TABLE = {0.15F, 0.05F, 0.0F, -0.05F, -0.15F, -0.05F, 0.0F, 0.05F};

   /** Loaded chunks per level, to find neighbours without loading anything. */
   private static final Map<ServerLevel, Long2ObjectMap<LevelChunk>> LOADED = new WeakHashMap<>();
   /** Where the last aura tick decided a flux rift should open, per level (EntityFluxRift arrives with the flux milestone). */
   public static final Map<ServerLevel, BlockPos> RIFT_TRIGGER = new WeakHashMap<>();

   private AuraHandler() {
   }

   // ------------------------------------------------------------------ chunk bookkeeping

   static void chunkLoaded(ServerLevel level, LevelChunk chunk) {
      chunk.getData(ModAttachments.AURA); // generates the aura of new chunks
      LOADED.computeIfAbsent(level, l -> new Long2ObjectOpenHashMap<>()).put(chunk.getPos().toLong(), chunk);
   }

   static void chunkUnloaded(ServerLevel level, LevelChunk chunk) {
      Long2ObjectMap<LevelChunk> m = LOADED.get(level);
      if (m != null) m.remove(chunk.getPos().toLong());
   }

   static void levelUnloaded(ServerLevel level) {
      LOADED.remove(level);
      RIFT_TRIGGER.remove(level);
   }

   /** The aura of a loaded chunk; null if the chunk isn't loaded (or this is the client), like a missing AuraChunk in 1.12. */
   @Nullable
   public static AuraChunk getAuraChunk(Level level, int chunkX, int chunkZ) {
      if (!(level instanceof ServerLevel sl)) return null;
      LevelChunk c = sl.getChunkSource().getChunkNow(chunkX, chunkZ);
      if (c == null) return null;
      AuraChunk ac = c.getData(ModAttachments.AURA);
      return ac.isGenerated() ? ac : null;
   }

   @Nullable
   public static AuraChunk getAuraChunk(Level level, BlockPos pos) {
      return getAuraChunk(level, pos.getX() >> 4, pos.getZ() >> 4);
   }

   private static void markDirty(Level level, BlockPos pos) {
      if (level instanceof ServerLevel sl) {
         LevelChunk c = sl.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
         if (c != null) c.setUnsaved(true);
      }
   }

   // ------------------------------------------------------------------ generation

   /** generateAura: the chunk's base is the average aura level of its biome and its four neighbours, +-10% noise. */
   public static AuraChunk generate(ServerLevel level, ChunkPos chunk, RandomSource rand) {
      float life = BiomeHandler.getBiomeAuraModifier(level.getBiome(new BlockPos(chunk.getMinBlockX() + 8, 50, chunk.getMinBlockZ() + 8)));
      for (Direction dir : Direction.Plane.HORIZONTAL) {
         life += BiomeHandler.getBiomeAuraModifier(level.getBiome(new BlockPos(
            (chunk.x + dir.getStepX()) * 16 + 8, 50, (chunk.z + dir.getStepZ()) * 16 + 8)));
      }
      life /= 5.0F;
      float noise = (float) (1.0 + rand.nextGaussian() * 0.1F);
      short base = (short) Mth.clamp((int) (life * AURA_CEILING * noise), 0, AURA_CEILING);
      return new AuraChunk(base, base, 0.0F);
   }

   // ------------------------------------------------------------------ AuraHelper API

   public static float getVis(Level level, BlockPos pos) {
      AuraChunk ac = getAuraChunk(level, pos);
      return ac != null ? ac.getVis() : 0.0F;
   }

   public static float getFlux(Level level, BlockPos pos) {
      AuraChunk ac = getAuraChunk(level, pos);
      return ac != null ? ac.getFlux() : 0.0F;
   }

   public static int getAuraBase(Level level, BlockPos pos) {
      AuraChunk ac = getAuraChunk(level, pos);
      return ac != null ? ac.getBase() : 0;
   }

   public static float getTotalAura(Level level, BlockPos pos) {
      AuraChunk ac = getAuraChunk(level, pos);
      return ac != null ? ac.getVis() + ac.getFlux() : 0.0F;
   }

   public static float getFluxSaturation(Level level, BlockPos pos) {
      AuraChunk ac = getAuraChunk(level, pos);
      return ac != null && ac.getBase() > 0 ? ac.getFlux() / ac.getBase() : 0.0F;
   }

   /** True when a player who has researched aura preservation (or a machine: null player) should leave the aura alone. */
   public static boolean shouldPreserveAura(Level level, @Nullable Player player, BlockPos pos) {
      boolean knows = player == null || player.getData(ModAttachments.KNOWLEDGE).isResearchKnown("AURAPRESERVE");
      int base = getAuraBase(level, pos);
      return knows && base > 0 && getVis(level, pos) / base < 0.1;
   }

   public static void addVis(Level level, BlockPos pos, float amount) {
      if (amount < 0) return;
      AuraChunk ac = getAuraChunk(level, pos);
      if (ac != null) {
         ac.setVis(ac.getVis() + amount);
         markDirty(level, pos);
      }
   }

   public static void addFlux(Level level, BlockPos pos, float amount) {
      if (amount < 0) return;
      AuraChunk ac = getAuraChunk(level, pos);
      if (ac != null) {
         ac.setFlux(ac.getFlux() + amount);
         markDirty(level, pos);
      }
   }

   /** Takes up to {@code amount} vis from the chunk; returns how much it got (0 for unloaded chunks). */
   public static float drainVis(Level level, BlockPos pos, float amount, boolean simulate) {
      AuraChunk ac = getAuraChunk(level, pos);
      if (ac == null) return 0.0F;
      amount = Math.min(amount, ac.getVis());
      if (!simulate) {
         ac.setVis(ac.getVis() - amount);
         markDirty(level, pos);
      }
      return amount;
   }

   public static float drainFlux(Level level, BlockPos pos, float amount, boolean simulate) {
      AuraChunk ac = getAuraChunk(level, pos);
      if (ac == null) return 0.0F;
      amount = Math.min(amount, ac.getFlux());
      if (!simulate) {
         ac.setFlux(ac.getFlux() - amount);
         markDirty(level, pos);
      }
      return amount;
   }

   // ------------------------------------------------------------------ simulation (AuraThread)

   /** One AuraThread pass over every loaded chunk of the level. */
   static void tick(ServerLevel level) {
      Long2ObjectMap<LevelChunk> chunks = LOADED.get(level);
      if (chunks == null || chunks.isEmpty()) return;
      int phase = level.getMoonPhase();
      Simulation sim = new Simulation(PHASE_TABLE[phase], 1.0F + MAX_TABLE[phase], level.random);
      for (LevelChunk chunk : new ArrayList<>(chunks.values())) {
         AuraChunk ac = chunk.getData(ModAttachments.AURA);
         if (!ac.isGenerated()) continue;
         ChunkPos p = chunk.getPos();
         AuraChunk[] neighbours = new AuraChunk[4];
         LevelChunk[] neighbourChunks = new LevelChunk[4];
         int i = 0;
         for (Direction dir : Direction.Plane.HORIZONTAL) {
            LevelChunk n = chunks.get(ChunkPos.asLong(p.x + dir.getStepX(), p.z + dir.getStepZ()));
            neighbourChunks[i] = n;
            AuraChunk na = n == null ? null : n.getData(ModAttachments.AURA);
            neighbours[i++] = na != null && na.isGenerated() ? na : null;
         }
         Simulation.Result r = sim.process(ac, neighbours);
         if (r.changedSelf()) chunk.setUnsaved(true);
         if (r.changedNeighbour() >= 0) neighbourChunks[r.changedNeighbour()].setUnsaved(true);
         if (r.changedNeighbour2() >= 0) neighbourChunks[r.changedNeighbour2()].setUnsaved(true);
         if (r.rift()) RIFT_TRIGGER.put(level, new BlockPos(p.x * 16, 0, p.z * 16));
      }
   }

   /** The per-chunk rules of AuraThread.processAuraChunk, separated from the world so they can be tested. */
   public static final class Simulation {
      private final float phaseVis;
      private final float phaseMax;
      private final float phaseFlux;
      private final RandomSource rand;

      public Simulation(float phaseVis, float phaseMax, RandomSource rand) {
         this.phaseVis = phaseVis;
         this.phaseMax = phaseMax;
         this.phaseFlux = 0.25F - phaseVis;
         this.rand = rand;
      }

      /** Which chunks changed: self, and the indexes (into neighbours) that received vis / flux, or -1. */
      public record Result(boolean changedSelf, int changedNeighbour, int changedNeighbour2, boolean rift) {}

      public Result process(AuraChunk chunk, AuraChunk[] neighbours) {
         List<Integer> order = new ArrayList<>(List.of(0, 1, 2, 3));
         Collections.shuffle(order, new java.util.Random(rand.nextLong()));
         float base = chunk.getBase() * phaseMax;
         boolean dirty = false;
         float currentVis = chunk.getVis();
         float currentFlux = chunk.getFlux();
         int visTarget = -1;
         int fluxTarget = -1;
         float lowestVis = Float.MAX_VALUE;
         float lowestFlux = Float.MAX_VALUE;
         for (int i : order) {
            AuraChunk n = neighbours[i];
            if (n == null) continue;
            if ((visTarget < 0 || lowestVis > n.getVis()) && n.getVis() + n.getFlux() < n.getBase() * phaseMax) {
               visTarget = i;
               lowestVis = n.getVis();
            }
            if (fluxTarget < 0 || lowestFlux > n.getFlux()) {
               fluxTarget = i;
               lowestFlux = n.getFlux();
            }
         }

         int gotVis = -1;
         int gotFlux = -1;
         if (visTarget >= 0 && lowestVis < currentVis && lowestVis / currentVis < 0.75) {
            float inc = Math.min(currentVis - lowestVis, 1.0F);
            currentVis -= inc;
            neighbours[visTarget].setVis(lowestVis + inc);
            dirty = true;
            gotVis = visTarget;
         }
         if (fluxTarget >= 0 && currentFlux > Math.max(5.0F, chunk.getBase() / 10.0F) && lowestFlux < currentFlux / 1.75) {
            float inc = Math.min(currentFlux - lowestFlux, 1.0F);
            currentFlux -= inc;
            neighbours[fluxTarget].setFlux(lowestFlux + inc);
            dirty = true;
            gotFlux = fluxTarget;
         }

         if (currentVis + currentFlux < base) {
            currentVis += Math.min(base - (currentVis + currentFlux), phaseVis);
            dirty = true;
         } else if (currentVis > base * 1.25 && rand.nextFloat() < 0.1) {
            currentFlux += phaseFlux;
            currentVis -= phaseFlux;
            dirty = true;
         } else if (currentVis <= base * 0.1 && currentVis >= currentFlux && rand.nextFloat() < 0.1) {
            currentFlux += phaseFlux;
            dirty = true;
         }

         if (dirty) {
            chunk.setVis(currentVis);
            chunk.setFlux(currentFlux);
         }
         boolean rift = currentFlux > base * 0.75 && rand.nextFloat() < currentFlux / 500.0F / 10.0F;
         return new Result(dirty, gotVis, gotFlux, rift);
      }
   }
}
