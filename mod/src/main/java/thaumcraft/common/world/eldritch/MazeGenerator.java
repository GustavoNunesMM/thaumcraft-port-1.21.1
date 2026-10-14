package thaumcraft.common.world.eldritch;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.core.registries.Registries;
import thaumcraft.Thaumcraft;
import thaumcraft.common.blocks.eldritch.EldritchLockBlock;
import thaumcraft.common.registers.ModBlocks;
import thaumcraft.common.registers.ModDevices;
import thaumcraft.common.registers.ModEldritch;
import thaumcraft.common.registers.ModFlux;
import thaumcraft.common.registers.ModMobs;

/**
 * The labyrinth of the Outer Lands.
 *
 * <p>Port note: the maze generator of Thaumcraft 4 (MazeHandler/MazeThread/GenCommon) did not survive into Thaumcraft 6
 * (1.12 kept only the dimension id, the eldritch biome and the creatures), so this is a fresh, deterministic version of
 * the same idea. Every chunk is one cell; 8x8 cells form a region holding one perfect maze (a seeded depth-first
 * labyrinth with a few extra loops), joined to its neighbours by one opening per side. Each region has:
 * <ul>
 * <li>an entry chamber with the eldritch portal back home (where travellers arrive),</li>
 * <li>a sealed boss chamber at the farthest dead end: an ancient doorway with a locking mechanism; behind it waits an
 * eldritch warden, an eldritch construct, a greater crimson portal or a giant taintacle (the four tc.boss.* rooms),</li>
 * <li>a reliquary chest holding the runed tablet that opens the lock,</li>
 * <li>rooms of treasure urns and crates, and crusted openings full of crabs.</li>
 * </ul>
 * Everything is a pure function of the world seed and the cell, so chunks generate independently.
 */
public final class MazeGenerator {
   /** The first air layer of the corridors. */
   public static final int FLOOR = 50;
   public static final int REGION = 8;
   public static final int ENTRY = 3;
   public static final int N = 1, E = 2, S = 4, W = 8;
   private static final int[] DIRS = {N, E, S, W};
   private static final int[] DX = {0, 1, 0, -1};
   private static final int[] DZ = {-1, 0, 1, 0};

   public static final ResourceKey<LootTable> KEY_CHEST = ResourceKey.create(Registries.LOOT_TABLE,
      ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "chests/outer_lands_reliquary"));

   public enum CellType {
      CORRIDOR, ENTRY, BOSS, KEY, LOOT, CRAB
   }

   public enum BossKind {
      WARDEN("tc.boss.warden"), GOLEM("tc.boss.golem"), CRIMSON("tc.boss.crimson"), TAINT("tc.boss.taint");

      public final String message;

      BossKind(String message) {
         this.message = message;
      }
   }

   /** One region: the open sides of every cell (N/E/S/W bits) and what each cell holds. */
   public record Layout(int[][] open, CellType[][] type, int bossX, int bossZ, int bossDoor, BossKind bossKind, int keyX, int keyZ) {
   }

   private static final Map<String, Layout> CACHE = new ConcurrentHashMap<>();

   private MazeGenerator() {
   }

   private static int bit(int d) {
      return DIRS[d];
   }

   private static int opposite(int d) {
      return (d + 2) % 4;
   }

   /** The row (or column) where the region (rx, rz) opens towards its east (axis 0) or south (axis 1) neighbour. */
   private static int link(long seed, int rx, int rz, int axis) {
      long h = seed * 6364136223846793005L + rx * 341873128712L + rz * 132897987541L + axis * 0x9E3779B97F4A7C15L;
      return (int) Math.floorMod(new Random(h).nextLong(), REGION);
   }

   public static Layout layout(long seed, int rx, int rz) {
      String key = seed + ":" + rx + ":" + rz;
      Layout l = CACHE.get(key);
      if (l == null) {
         if (CACHE.size() > 512) CACHE.clear();
         l = build(seed, rx, rz);
         CACHE.put(key, l);
      }
      return l;
   }

   private static Layout build(long seed, int rx, int rz) {
      Random rnd = new Random(seed ^ rx * 341873128712L ^ rz * 132897987541L ^ 0x0E1D817CL);
      int[][] open = new int[REGION][REGION];
      // depth first labyrinth from the entry
      boolean[][] seen = new boolean[REGION][REGION];
      int[][] dist = new int[REGION][REGION];
      ArrayDeque<int[]> stack = new ArrayDeque<>();
      stack.push(new int[]{ENTRY, ENTRY});
      seen[ENTRY][ENTRY] = true;
      while (!stack.isEmpty()) {
         int[] c = stack.peek();
         List<Integer> next = new ArrayList<>();
         for (int d = 0; d < 4; d++) {
            int nx = c[0] + DX[d], nz = c[1] + DZ[d];
            if (nx >= 0 && nz >= 0 && nx < REGION && nz < REGION && !seen[nx][nz]) next.add(d);
         }
         if (next.isEmpty()) {
            stack.pop();
            continue;
         }
         int d = next.get(rnd.nextInt(next.size()));
         int nx = c[0] + DX[d], nz = c[1] + DZ[d];
         open[c[0]][c[1]] |= bit(d);
         open[nx][nz] |= bit(opposite(d));
         seen[nx][nz] = true;
         dist[nx][nz] = dist[c[0]][c[1]] + 1;
         stack.push(new int[]{nx, nz});
      }
      // boss chamber: the farthest dead end; reliquary: the farthest other cell
      int bx = -1, bz = -1, kx = -1, kz = -1;
      // (kept off the region's rim when possible, so the openings to the neighbours never run into it)
      for (int pass = 0; pass < 2 && bx < 0; pass++) {
         for (int x = 0; x < REGION; x++) {
            for (int z = 0; z < REGION; z++) {
               if (x == ENTRY && z == ENTRY || Integer.bitCount(open[x][z]) != 1) continue;
               if (pass == 0 && (x == 0 || z == 0 || x == REGION - 1 || z == REGION - 1)) continue;
               if (bx < 0 || dist[x][z] > dist[bx][bz]) {
                  bx = x;
                  bz = z;
               }
            }
         }
      }
      for (int x = 0; x < REGION; x++) {
         for (int z = 0; z < REGION; z++) {
            if (x == ENTRY && z == ENTRY || x == bx && z == bz) continue;
            if (kx < 0 || dist[x][z] > dist[kx][kz]) {
               kx = x;
               kz = z;
            }
         }
      }
      int door = open[bx][bz];
      // a few loops (never into the boss chamber)
      for (int x = 0; x < REGION; x++) {
         for (int z = 0; z < REGION; z++) {
            if (x == bx && z == bz || rnd.nextFloat() >= 0.1F) continue;
            int d = rnd.nextInt(4);
            int nx = x + DX[d], nz = z + DZ[d];
            if (nx < 0 || nz < 0 || nx >= REGION || nz >= REGION || nx == bx && nz == bz) continue;
            open[x][z] |= bit(d);
            open[nx][nz] |= bit(opposite(d));
         }
      }
      // openings to the neighbouring regions (moved off the boss chamber if they'd hit it)
      int east = link(seed, rx, rz, 0), west = link(seed, rx - 1, rz, 0);
      int south = link(seed, rx, rz, 1), north = link(seed, rx, rz - 1, 1);
      if (!(bx == REGION - 1 && bz == east)) open[REGION - 1][east] |= E;
      if (!(bx == 0 && bz == west)) open[0][west] |= W;
      if (!(bz == REGION - 1 && bx == south)) open[south][REGION - 1] |= S;
      if (!(bz == 0 && bx == north)) open[north][0] |= N;

      CellType[][] type = new CellType[REGION][REGION];
      for (int x = 0; x < REGION; x++) {
         for (int z = 0; z < REGION; z++) {
            float f = rnd.nextFloat();
            type[x][z] = f < 0.12F ? CellType.LOOT : f < 0.22F ? CellType.CRAB : CellType.CORRIDOR;
         }
      }
      type[ENTRY][ENTRY] = CellType.ENTRY;
      type[bx][bz] = CellType.BOSS;
      type[kx][kz] = CellType.KEY;
      BossKind kind = BossKind.values()[rnd.nextInt(BossKind.values().length)];
      return new Layout(open, type, bx, bz, door, kind, kx, kz);
   }

   public static int region(int chunk) {
      return Math.floorDiv(chunk, REGION);
   }

   public static int local(int chunk) {
      return Math.floorMod(chunk, REGION);
   }

   /** Where travellers arrive in the region (a corner of the entry chamber). */
   public static BlockPos arrival(int rx, int rz) {
      int cx = rx * REGION + ENTRY, cz = rz * REGION + ENTRY;
      return new BlockPos(cx * 16 + 4, FLOOR, cz * 16 + 4);
   }

   private static BlockState wall(RandomSource r, boolean boss) {
      if (boss) return ModBlocks.STONE_ELDRITCH_TILE.get().defaultBlockState();
      return r.nextInt(30) == 0 ? ModBlocks.STONE_ANCIENT_GLYPHED.get().defaultBlockState() : ModBlocks.STONE_ANCIENT.get().defaultBlockState();
   }

   /** Builds the cell of one chunk (called from OuterLandsChunkGenerator.applyBiomeDecoration). */
   public static void carve(WorldGenLevel level, int chunkX, int chunkZ) {
      long seed = level.getSeed();
      Layout l = layout(seed, region(chunkX), region(chunkZ));
      int lx = local(chunkX), lz = local(chunkZ);
      CellType type = l.type()[lx][lz];
      int open = l.open()[lx][lz];
      boolean boss = type == CellType.BOSS;
      RandomSource r = RandomSource.create(seed ^ chunkX * 0x5DEECE66DL ^ (long) chunkZ << 20);
      int x0 = chunkX * 16, z0 = chunkZ * 16;
      BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
      int lo = boss ? 1 : type == CellType.ENTRY ? 2 : 3;
      int hi = 15 - lo;
      int roomTop = FLOOR + (boss ? 7 : 5);
      BlockState air = Blocks.AIR.defaultBlockState();
      BlockState tile = ModBlocks.STONE_ANCIENT_TILE.get().defaultBlockState();
      for (int x = 0; x < 16; x++) {
         for (int z = 0; z < 16; z++) {
            boolean room = x >= lo && x <= hi && z >= lo && z <= hi;
            boolean passN = (open & N) != 0 && x >= 6 && x <= 9 && z < lo;
            boolean passS = (open & S) != 0 && x >= 6 && x <= 9 && z > hi;
            boolean passW = (open & W) != 0 && z >= 6 && z <= 9 && x < lo;
            boolean passE = (open & E) != 0 && z >= 6 && z <= 9 && x > hi;
            boolean pass = passN || passS || passW || passE;
            int top = room ? roomTop : pass ? FLOOR + 3 : FLOOR - 1;
            for (int y = FLOOR - 4; y <= FLOOR + 8; y++) {
               p.set(x0 + x, y, z0 + z);
               BlockState s;
               if (y >= FLOOR && y <= top) s = air;
               else if (y == FLOOR - 1 && (room || pass)) s = boss && type == CellType.BOSS && l.bossKind() == BossKind.TAINT && room
                  ? ModFlux.TAINT_SOIL.get().defaultBlockState() : tile;
               else s = wall(r, boss);
               level.setBlock(p, s, 2);
            }
         }
      }
      switch (type) {
         case ENTRY -> level.setBlock(new BlockPos(x0 + 8, FLOOR, z0 + 8), ModEldritch.ELDRITCH_PORTAL.get().defaultBlockState(), 2);
         case LOOT -> {
            int[][] corners = {{lo, lo}, {hi, lo}, {lo, hi}, {hi, hi}};
            for (int[] c : corners) {
               if (r.nextInt(4) == 0) continue;
               float f = r.nextFloat();
               boolean urn = r.nextBoolean();
               BlockState s = (f < 0.1F ? urn ? ModDevices.LOOT_URN_RARE : ModDevices.LOOT_CRATE_RARE
                  : f < 0.4F ? urn ? ModDevices.LOOT_URN_UNCOMMON : ModDevices.LOOT_CRATE_UNCOMMON
                  : urn ? ModDevices.LOOT_URN_COMMON : ModDevices.LOOT_CRATE_COMMON).get().defaultBlockState();
               level.setBlock(new BlockPos(x0 + c[0], FLOOR, z0 + c[1]), s, 2);
            }
         }
         case CRAB -> level.setBlock(new BlockPos(x0 + 8, FLOOR - 1, z0 + 8), ModEldritch.CRAB_SPAWNER.get().defaultBlockState(), 2);
         case KEY -> {
            BlockPos chest = new BlockPos(x0 + 8, FLOOR, z0 + 8);
            level.setBlock(chest, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.SOUTH), 2);
            RandomizableContainer.setBlockEntityLootTable(level, r, chest, KEY_CHEST);
         }
         case BOSS -> {
            if (l.bossKind() == BossKind.WARDEN) {
               level.setBlock(new BlockPos(x0 + 8, FLOOR, z0 + 8), ModBlocks.PEDESTAL_ELDRITCH.get().defaultBlockState(), 2);
            }
            sealDoor(level, x0, z0, l.bossDoor());
         }
         default -> {
         }
      }
   }

   /** The ancient doorway across the boss chamber's passage, at the cell edge, with the lock in its middle. */
   private static void sealDoor(WorldGenLevel level, int x0, int z0, int door) {
      Direction into;
      for (int i = 6; i <= 9; i++) {
         for (int y = FLOOR; y <= FLOOR + 3; y++) {
            BlockPos p = switch (door) {
               case N -> new BlockPos(x0 + i, y, z0);
               case S -> new BlockPos(x0 + i, y, z0 + 15);
               case W -> new BlockPos(x0, y, z0 + i);
               default -> new BlockPos(x0 + 15, y, z0 + i);
            };
            level.setBlock(p, ModBlocks.STONE_ANCIENT_DOORWAY.get().defaultBlockState(), 2);
         }
      }
      into = switch (door) {
         case N -> Direction.SOUTH;
         case S -> Direction.NORTH;
         case W -> Direction.EAST;
         default -> Direction.WEST;
      };
      BlockPos lock = switch (door) {
         case N -> new BlockPos(x0 + 7, FLOOR + 1, z0);
         case S -> new BlockPos(x0 + 8, FLOOR + 1, z0 + 15);
         case W -> new BlockPos(x0, FLOOR + 1, z0 + 8);
         default -> new BlockPos(x0 + 15, FLOOR + 1, z0 + 7);
      };
      level.setBlock(lock, ModEldritch.ELDRITCH_LOCK.get().defaultBlockState().setValue(EldritchLockBlock.FACING, into), 2);
   }

   /** Wakes the guardian of the maze boss chamber containing {@code inside} when its lock opens (Outer Lands only). */
   public static void wakeBoss(ServerLevel level, BlockPos inside) {
      if (!OuterLands.isOuterLands(level)) return;
      int cx = inside.getX() >> 4, cz = inside.getZ() >> 4;
      Layout l = layout(level.getSeed(), region(cx), region(cz));
      if (l.type()[local(cx)][local(cz)] != CellType.BOSS) return;
      spawnBoss(level, new BlockPos(cx * 16 + 8, FLOOR, cz * 16 + 8), l.bossKind());
   }

   public static Entity spawnBoss(ServerLevel level, BlockPos center, BossKind kind) {
      Entity boss = switch (kind) {
         case WARDEN -> ModEldritch.ELDRITCH_WARDEN.get().create(level);
         case GOLEM -> ModEldritch.ELDRITCH_GOLEM.get().create(level);
         case CRIMSON -> ModMobs.CULTIST_PORTAL_GREATER.get().create(level);
         case TAINT -> ModEldritch.TAINTACLE_GIANT.get().create(level);
      };
      if (boss == null) return null;
      double y = center.getY() + (level.getBlockState(center).is(ModBlocks.PEDESTAL_ELDRITCH.get()) ? 1.0 : 0.0);
      boss.moveTo(center.getX() + 0.5, y, center.getZ() + 0.5, level.random.nextFloat() * 360.0F, 0.0F);
      if (boss instanceof Mob mob) {
         mob.finalizeSpawn(level, level.getCurrentDifficultyAt(center), MobSpawnType.EVENT, null);
         mob.setPersistenceRequired();
      }
      level.addFreshEntity(boss);
      if (kind == BossKind.TAINT) {
         Entity seed = ModFlux.TAINT_SEED_PRIME.get().create(level);
         if (seed != null) {
            seed.moveTo(center.getX() + 3.5, center.getY(), center.getZ() + 3.5, 0.0F, 0.0F);
            if (seed instanceof Mob m) m.setPersistenceRequired();
            level.addFreshEntity(seed);
         }
      }
      for (Player p : level.players()) {
         if (p.distanceToSqr(center.getX(), center.getY(), center.getZ()) < 48 * 48) {
            p.displayClientMessage(Component.translatable(kind.message).withStyle(net.minecraft.ChatFormatting.DARK_PURPLE,
               net.minecraft.ChatFormatting.ITALIC), false);
         }
      }
      return boss;
   }
}
