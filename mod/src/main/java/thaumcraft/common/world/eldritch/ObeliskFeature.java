package thaumcraft.common.world.eldritch;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import thaumcraft.common.blocks.eldritch.EldritchObeliskBlock;
import thaumcraft.common.registers.ModBlocks;
import thaumcraft.common.registers.ModEldritch;

/**
 * An eldritch altar among four obelisks on a platform of ancient stone, rarely found on the surface of the overworld
 * (and built by the creative obelisk placer). The way into the Outer Lands. (Thaumcraft 4 design, see
 * EldritchAltarBlock.)
 */
public class ObeliskFeature extends Feature<NoneFeatureConfiguration> {
   public ObeliskFeature() {
      super(NoneFeatureConfiguration.CODEC);
   }

   @Override
   public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
      WorldGenLevel level = ctx.level();
      BlockPos pos = ctx.origin().below();
      BlockState ground = level.getBlockState(pos);
      if (!ground.isSolid() || ground.is(BlockTags.LEAVES) || ground.is(BlockTags.LOGS) || !level.getFluidState(pos.above()).isEmpty()) return false;
      // fairly flat: the corners may not be more than two blocks off
      for (int[] c : new int[][]{{-2, -2}, {2, -2}, {2, 2}, {-2, 2}}) {
         int h = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE_WG, pos.getX() + c[0], pos.getZ() + c[1]);
         if (Math.abs(h - 1 - pos.getY()) > 2) return false;
      }
      build(level, pos, ctx.random());
      return true;
   }

   /** Builds the altar platform with its top surface at {@code base}. */
   public static void build(WorldGenLevel level, BlockPos base, RandomSource random) {
      BlockState tile = ModBlocks.STONE_ANCIENT_TILE.get().defaultBlockState();
      BlockState stone = ModBlocks.STONE_ANCIENT.get().defaultBlockState();
      for (int x = -2; x <= 2; x++) {
         for (int z = -2; z <= 2; z++) {
            boolean edge = Math.abs(x) == 2 || Math.abs(z) == 2;
            level.setBlock(base.offset(x, 0, z), edge && random.nextInt(4) == 0 ? ModBlocks.STONE_ANCIENT_GLYPHED.get().defaultBlockState() : tile, 2);
            for (int y = 1; y <= 4; y++) {
               BlockPos below = base.offset(x, -y, z);
               if (!level.getBlockState(below).canBeReplaced()) break;
               level.setBlock(below, stone, 2);
            }
            for (int y = 1; y <= 5; y++) level.setBlock(base.offset(x, y, z), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 2);
         }
      }
      level.setBlock(base.above(), ModEldritch.ELDRITCH_ALTAR.get().defaultBlockState(), 2);
      for (int[] c : new int[][]{{-2, -2}, {2, -2}, {2, 2}, {-2, 2}}) {
         BlockPos o = base.offset(c[0], 1, c[1]);
         level.setBlock(o, ModEldritch.ELDRITCH_OBELISK.get().defaultBlockState().setValue(EldritchObeliskBlock.PART, EldritchObeliskBlock.Part.BOTTOM), 2);
         level.setBlock(o.above(), ModEldritch.ELDRITCH_OBELISK.get().defaultBlockState().setValue(EldritchObeliskBlock.PART, EldritchObeliskBlock.Part.MIDDLE), 2);
         level.setBlock(o.above(2), ModEldritch.ELDRITCH_OBELISK.get().defaultBlockState().setValue(EldritchObeliskBlock.PART, EldritchObeliskBlock.Part.TOP), 2);
         level.setBlock(o.above(3), ModEldritch.ELDRITCH_CAP.get().defaultBlockState(), 2);
      }
   }
}
