package thaumcraft.gametest;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import thaumcraft.Thaumcraft;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.aspects.AspectManager;
import thaumcraft.common.blocks.world.CrystalBlock;
import thaumcraft.common.items.VisCrystalItem;
import thaumcraft.common.registers.ModBlocks;
import thaumcraft.common.registers.ModItems;

/** Checks for milestone 2: ores, vis crystals, magic trees, materials and world generation. */
@GameTestHolder(Thaumcraft.MODID)
@PrefixGameTestTemplate(false)
public class WorldGameTests {
   private static AspectList aspects(GameTestHelper h, ItemStack stack) {
      return AspectManager.SERVER.getObjectAspects(h.getLevel().registryAccess(), stack);
   }

   private static int amount(GameTestHelper h, ItemStack stack, Aspect a) {
      AspectList l = aspects(h, stack);
      return l == null ? 0 : l.getAmount(a);
   }

   @GameTest(template = "empty")
   public static void crystalDropsVisCrystals(GameTestHelper h) {
      BlockPos rock = new BlockPos(2, 1, 2);
      BlockPos at = rock.above();
      h.setBlock(rock, Blocks.STONE);
      CrystalBlock aer = ModBlocks.CRYSTAL_AER.get();
      ServerLevel level = h.getLevel();
      BlockState state = aer.worldgenState(level, h.absolutePos(at), 2);
      h.assertTrue(state.getValue(CrystalBlock.FACES.get(Direction.DOWN)), "crystal on stone should grow from its bottom face");
      h.assertFalse(state.getValue(CrystalBlock.FACES.get(Direction.UP)), "nothing above the crystal");
      h.setBlock(at, state);

      List<ItemStack> drops = Block.getDrops(state, level, h.absolutePos(at), null);
      int count = drops.stream().mapToInt(ItemStack::getCount).sum();
      h.assertTrue(count == 3, "size 2 crystal should drop 3 vis crystals, got " + drops);
      h.assertTrue(drops.stream().allMatch(s -> s.is(ModItems.CRYSTAL_ESSENCE.get()) && VisCrystalItem.getAspect(s) == Aspect.AIR),
         "drops should be aer vis crystals: " + drops);

      // Without rock to hold it, the crystal breaks off.
      h.setBlock(rock, Blocks.AIR);
      h.assertBlockNotPresent(aer, at);
      h.succeed();
   }

   @GameTest(template = "empty")
   public static void materialAspects(GameTestHelper h) {
      ItemStack ignis = VisCrystalItem.of(Aspect.FIRE, 1);
      AspectList ia = aspects(h, ignis);
      h.assertTrue(ia != null && ia.size() == 1 && ia.getAmount(Aspect.FIRE) == 1, "fire vis crystal holds 1 ignis: " + ia);
      h.assertTrue(amount(h, new ItemStack(ModBlocks.ORE_CINNABAR.get()), Aspect.METAL) == 10, "cinnabar ore metallum");
      h.assertTrue(amount(h, new ItemStack(ModBlocks.DEEPSLATE_ORE_CINNABAR.get()), Aspect.ALCHEMY) == 5, "deepslate cinnabar via c:ores/cinnabar");
      h.assertTrue(amount(h, new ItemStack(ModBlocks.CRYSTAL_ORDO.get()), Aspect.ORDER) == 15, "order crystal");
      h.assertTrue(amount(h, new ItemStack(ModBlocks.LOG_SILVERWOOD.get()), Aspect.AURA) == 5, "silverwood log auram");
      h.assertTrue(amount(h, new ItemStack(ModBlocks.PLANK_GREATWOOD.get()), Aspect.PLANT) > 0, "greatwood planks derive from the log recipe");
      h.assertTrue(amount(h, new ItemStack(ModItems.NUGGET_QUICKSILVER.get()), Aspect.METAL) > 0, "quicksilver drops derive from quicksilver");
      h.assertTrue(h.getLevel().getServer().getRecipeManager().byKey(ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "plankgreatwood")).isPresent(),
         "greatwood plank recipe missing");
      h.succeed();
   }

   private static BlockPos growSapling(GameTestHelper h, TreeGrower grower, BlockPos at) {
      ServerLevel level = h.getLevel();
      BlockPos abs = h.absolutePos(at);
      BlockState sapling = level.getBlockState(abs).setValue(SaplingBlock.STAGE, 1);
      h.assertTrue(grower.growTree(level, level.getChunkSource().getGenerator(), abs, sapling, level.random), "tree did not grow");
      return abs;
   }

   /** Leaves must know their distance to the trunk, or 1.21 decays them. */
   private static void assertLeavesAttached(GameTestHelper h, BlockPos trunk, Block log, Block leaves) {
      ServerLevel level = h.getLevel();
      BlockPos p = trunk;
      while (level.getBlockState(p).is(log)) p = p.above();
      BlockState top = level.getBlockState(p);
      h.assertTrue(top.is(leaves), "expected leaves above the trunk at " + p + ", found " + top);
      h.assertTrue(top.getValue(LeavesBlock.DISTANCE) < LeavesBlock.DECAY_DISTANCE, "leaves above the trunk would decay: " + top);
   }

   // "trees" is a 33x40x33 room (tools/make_structure.py); the default template's barrier walls would stop the trees.
   @GameTest(template = "trees", skyAccess = true)
   public static void silverwoodSaplingGrows(GameTestHelper h) {
      BlockPos at = new BlockPos(16, 1, 16);
      h.setBlock(at.below(), Blocks.DIRT);
      h.setBlock(at, ModBlocks.SAPLING_SILVERWOOD.get());
      BlockPos trunk = growSapling(h, ModBlocks.SILVERWOOD_GROWER, at);
      h.assertBlockPresent(ModBlocks.LOG_SILVERWOOD.get(), at);
      assertLeavesAttached(h, trunk, ModBlocks.LOG_SILVERWOOD.get(), ModBlocks.LEAVES_SILVERWOOD.get());
      h.succeed();
   }

   @GameTest(template = "trees", skyAccess = true)
   public static void greatwoodNeedsFourSaplings(GameTestHelper h) {
      BlockPos at = new BlockPos(15, 1, 15);
      for (BlockPos p : BlockPos.betweenClosed(at, at.offset(1, 0, 1))) {
         h.setBlock(p.below(), Blocks.DIRT);
         h.setBlock(p, ModBlocks.SAPLING_GREATWOOD.get());
      }
      // A single sapling (one of the four removed) must not grow.
      h.setBlock(at.offset(1, 0, 1), Blocks.AIR);
      ServerLevel level = h.getLevel();
      BlockState one = level.getBlockState(h.absolutePos(at)).setValue(SaplingBlock.STAGE, 1);
      h.assertFalse(ModBlocks.GREATWOOD_GROWER.growTree(level, level.getChunkSource().getGenerator(), h.absolutePos(at), one, level.random),
         "greatwood grew from fewer than four saplings");
      h.setBlock(at.offset(1, 0, 1), ModBlocks.SAPLING_GREATWOOD.get());

      growSapling(h, ModBlocks.GREATWOOD_GROWER, at);
      for (BlockPos p : BlockPos.betweenClosed(at, at.offset(1, 0, 1))) h.assertBlockPresent(ModBlocks.LOG_GREATWOOD.get(), p);
      h.succeed();
   }

   private static boolean hasFeature(GameTestHelper h, ResourceKey<Biome> biome, String feature) {
      Biome b = h.getLevel().registryAccess().registryOrThrow(Registries.BIOME).getOrThrow(biome);
      ResourceLocation id = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, feature);
      for (HolderSet<PlacedFeature> step : b.getGenerationSettings().features()) {
         if (step.stream().anyMatch(f -> f.is(id))) return true;
      }
      return false;
   }

   @GameTest(template = "empty")
   public static void worldgenAddedToBiomes(GameTestHelper h) {
      for (String f : List.of("ore_cinnabar", "ore_quartz", "ore_amber", "vis_crystals", "greatwood", "silverwood")) {
         h.assertTrue(hasFeature(h, Biomes.FOREST, f), "forest lacks " + f);
      }
      h.assertTrue(hasFeature(h, Biomes.DESERT, "cinderpearl"), "desert lacks cinderpearl");
      h.assertFalse(hasFeature(h, Biomes.FOREST, "cinderpearl"), "cinderpearl should only be in hot biomes");
      h.assertFalse(hasFeature(h, Biomes.NETHER_WASTES, "ore_cinnabar"), "no thaumcraft ores in the nether");
      h.succeed();
   }
}
