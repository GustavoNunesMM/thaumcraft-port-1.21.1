package thaumcraft.common.world.mobs;

import thaumcraft.common.world.MobWorldgen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.RandomSource;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import thaumcraft.Thaumcraft;
import thaumcraft.common.registers.ModDevices;

/**
 * WorldGenMound: the barrow mound (1 chunk in 100 in the overworld, ThaumcraftWorldGenerator.generateSurface). Its
 * fixed blocks (extracted from the bytecode by tools/mound_extract.py) are the structure template thaumcraft:mound; the
 * random part of generate2 (two loot crates/urns, a trapped chest over TNT or a dungeon chest, a skeleton and a zombie
 * spawner) is placed here.
 */
public class MoundFeature extends Feature<NoneFeatureConfiguration> {
   public static final ResourceLocation TEMPLATE = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "mound");

   public MoundFeature() {
      super(NoneFeatureConfiguration.CODEC);
   }

   @Override
   public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
      WorldGenLevel level = ctx.level();
      RandomSource random = ctx.random();
      BlockPos origin = ctx.origin();
      if (level instanceof WorldGenRegion) {
         if (!MobWorldgen.generateStructures() || !MobWorldgen.isNaturalOverworld(level, ctx.chunkGenerator())) return false;
         // The 1/100 chunk roll is the placed feature's rarity filter.
         origin = MobWorldgen.surfaceStructurePos(level, origin, random);
      }
      return generate(level, random, origin);
   }

   /** WorldGenMound.LocationIsValidSpawn */
   static boolean locationIsValidSpawn(WorldGenLevel world, BlockPos pos) {
      int distanceToAir = 0;
      while (!world.isEmptyBlock(pos.above(distanceToAir)) && distanceToAir < 3) distanceToAir++;
      if (distanceToAir > 2) return false;
      BlockPos pos2 = pos.above(distanceToAir - 1);
      BlockState state = world.getBlockState(pos2);
      BlockState below = world.getBlockState(pos2.below());
      if (!world.isEmptyBlock(pos2.above())) return false;
      return valid(state) || ((state.is(Blocks.SNOW) || state.is(Blocks.SHORT_GRASS) || state.is(Blocks.FERN)) && valid(below));
   }

   private static boolean valid(BlockState s) {
      return s.is(Blocks.STONE) || s.is(Blocks.GRASS_BLOCK) || s.is(Blocks.DIRT);
   }

   /** WorldGenMound.generate: pos is the mound's base corner, 9 blocks under the surface. */
   public static boolean generate(WorldGenLevel world, RandomSource rand, BlockPos pos) {
      if (!locationIsValidSpawn(world, pos.offset(9, 9, 9)) || !locationIsValidSpawn(world, pos.offset(0, 9, 0))
         || !locationIsValidSpawn(world, pos.offset(18, 9, 0)) || !locationIsValidSpawn(world, pos.offset(18, 9, 18))
         || !locationIsValidSpawn(world, pos.offset(0, 9, 18))) {
         return false;
      }
      StructureTemplate template = world.getLevel().getStructureManager().get(TEMPLATE).orElse(null);
      if (template == null) {
         Thaumcraft.LOGGER.warn("Missing structure template {}", TEMPLATE);
         return false;
      }
      template.placeInWorld(world, pos, pos, new StructurePlaceSettings(), rand, Block.UPDATE_CLIENTS);
      // generate2's random tail
      world.setBlock(pos.offset(9, 1, 7), lootBlock(world, rand), Block.UPDATE_CLIENTS);
      world.setBlock(pos.offset(9, 1, 11), lootBlock(world, rand), Block.UPDATE_CLIENTS);
      BlockPos chest = pos.offset(10, 1, 9);
      if (rand.nextInt(3) == 0) {
         world.setBlock(chest, Blocks.TRAPPED_CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.WEST), Block.UPDATE_CLIENTS);
         world.setBlock(pos.offset(10, -1, 9), Blocks.TNT.defaultBlockState(), Block.UPDATE_CLIENTS);
      } else {
         world.setBlock(chest, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.WEST), Block.UPDATE_CLIENTS);
      }
      RandomizableContainer.setBlockEntityLootTable(world, rand, chest, BuiltInLootTables.SIMPLE_DUNGEON);
      spawner(world, rand, pos.offset(4, 5, 4), EntityType.SKELETON);
      spawner(world, rand, pos.offset(4, 5, 14), EntityType.ZOMBIE);
      return true;
   }

   private static void spawner(WorldGenLevel world, RandomSource rand, BlockPos p, EntityType<?> type) {
      world.setBlock(p, Blocks.SPAWNER.defaultBlockState(), Block.UPDATE_CLIENTS);
      if (world.getBlockEntity(p) instanceof SpawnerBlockEntity spawner) spawner.setEntityId(type, rand);
   }

   /** Rarity 2 below 0.1, 1 below 0.33, else 0; then a crate or (30%) an urn of that rarity. */
   private static BlockState lootBlock(WorldGenLevel world, RandomSource rand) {
      float f = rand.nextFloat();
      int rarity = f < 0.1F ? 2 : (f < 0.33F ? 1 : 0);
      boolean crate = rand.nextFloat() < 0.3F;
      Block b = switch (rarity) {
         case 1 -> crate ? ModDevices.LOOT_CRATE_UNCOMMON.get() : ModDevices.LOOT_URN_UNCOMMON.get();
         case 2 -> crate ? ModDevices.LOOT_CRATE_RARE.get() : ModDevices.LOOT_URN_RARE.get();
         default -> crate ? ModDevices.LOOT_CRATE_COMMON.get() : ModDevices.LOOT_URN_COMMON.get();
      };
      return b.defaultBlockState();
   }

   static int surfaceY(WorldGenLevel level, int x, int z) {
      return level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
   }
}
