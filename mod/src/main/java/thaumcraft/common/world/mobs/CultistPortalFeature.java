package thaumcraft.common.world.mobs;

import thaumcraft.common.world.MobWorldgen;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.neoforged.neoforge.common.Tags;
import thaumcraft.common.entities.monster.cult.CultistPortalLesserEntity;
import thaumcraft.common.registers.ModMobs;

/**
 * ThaumcraftWorldGenerator.generateSurface: 1 chunk in 500 (the placed feature's rarity filter) gets a lesser crimson
 * portal standing on the surface, if that surface is ground, rock, sand or snow.
 */
public class CultistPortalFeature extends Feature<NoneFeatureConfiguration> {
   public CultistPortalFeature() {
      super(NoneFeatureConfiguration.CODEC);
   }

   @Override
   public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
      WorldGenLevel level = ctx.level();
      RandomSource random = ctx.random();
      BlockPos ground = ctx.origin();
      if (level instanceof WorldGenRegion) {
         if (!MobWorldgen.generateStructures() || !MobWorldgen.isNaturalOverworld(level, ctx.chunkGenerator())) return false;
         ground = MobWorldgen.surfaceStructurePos(level, ground, random).above(8);
      }
      return spawn(level, ground);
   }

   /** Material.GROUND / ROCK / SAND / SNOW of the 1.12 check. */
   static boolean validGround(BlockState bs) {
      return bs.is(BlockTags.DIRT) || bs.is(BlockTags.BASE_STONE_OVERWORLD) || bs.is(Tags.Blocks.STONES) || bs.is(BlockTags.SAND)
         || bs.is(Tags.Blocks.GRAVELS) || bs.is(Blocks.CLAY) || bs.is(Blocks.SNOW) || bs.is(Blocks.SNOW_BLOCK) || bs.is(Blocks.POWDER_SNOW)
         || bs.is(BlockTags.TERRACOTTA) || bs.is(Tags.Blocks.SANDSTONE_BLOCKS);
   }

   /** Spawns the portal standing on the block at ground. */
   public static boolean spawn(WorldGenLevel level, BlockPos ground) {
      if (!validGround(level.getBlockState(ground))) return false;
      CultistPortalLesserEntity portal = ModMobs.CULTIST_PORTAL_LESSER.get().create(level.getLevel());
      if (portal == null) return false;
      portal.moveTo(ground.getX() + 0.5, ground.getY() + 1, ground.getZ() + 0.5, 0.0F, 0.0F);
      portal.finalizeSpawn(level, level.getCurrentDifficultyAt(ground), MobSpawnType.STRUCTURE, null);
      level.addFreshEntityWithPassengers(portal);
      return true;
   }
}
