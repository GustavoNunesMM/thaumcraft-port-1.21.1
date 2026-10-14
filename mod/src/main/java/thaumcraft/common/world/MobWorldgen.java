package thaumcraft.common.world;

import com.google.common.collect.ImmutableSet;
import com.mojang.datafixers.util.Pair;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSourceParameterList;
import net.minecraft.world.level.biome.MultiNoiseBiomeSourceParameterLists;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.minecraft.world.item.BlockItem;
import thaumcraft.Thaumcraft;
import thaumcraft.common.blocks.world.GrassAmbientBlock;
import thaumcraft.common.registers.ModBlocks;
import thaumcraft.common.registers.ModFeatures;
import thaumcraft.common.registers.ModItems;
import thaumcraft.common.world.features.BigMagicTreeFeature;
import thaumcraft.common.world.mobs.CultistPortalFeature;
import thaumcraft.common.world.mobs.MagicalForestFeatures;
import thaumcraft.common.world.mobs.MoundFeature;

/**
 * Milestone 13 world generation: the magical forest and eerie biomes (data: worldgen/biome), the ambient grass, the
 * barrow mounds and lesser crimson portals of ThaumcraftWorldGenerator.generateSurface, and the injection of the
 * magical forest into the overworld's climate parameters (the 1.12 BiomeManager.addBiome; there is no biome API in
 * NeoForge, see {@link #injectMagicalForest}). The eerie biome was registered but never added to the overworld in 1.12,
 * so it is only reachable through commands and other features.
 */
public final class MobWorldgen {
   public static final ResourceKey<Biome> MAGICAL_FOREST = ResourceKey.create(Registries.BIOME,
      ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "magical_forest"));
   public static final ResourceKey<Biome> EERIE = ResourceKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "eerie"));

   public static final DeferredBlock<GrassAmbientBlock> GRASS_AMBIENT = ModBlocks.BLOCKS.registerBlock("grass_ambient", GrassAmbientBlock::new,
      BlockBehaviour.Properties.ofFullCopy(Blocks.GRASS_BLOCK).strength(0.6F));
   public static final DeferredItem<BlockItem> GRASS_AMBIENT_ITEM = ModItems.ITEMS.registerSimpleBlockItem(GRASS_AMBIENT);

   public static final Supplier<Feature<NoneFeatureConfiguration>> MOUND = ModFeatures.FEATURES.register("mound", MoundFeature::new);
   public static final Supplier<Feature<NoneFeatureConfiguration>> CULTIST_PORTAL = ModFeatures.FEATURES.register("cultist_portal",
      CultistPortalFeature::new);
   public static final Supplier<Feature<NoneFeatureConfiguration>> BIG_MAGIC_TREE = ModFeatures.FEATURES.register("big_magic_tree",
      BigMagicTreeFeature::new);
   public static final Supplier<Feature<NoneFeatureConfiguration>> MAGICAL_FOREST_TREES = ModFeatures.FEATURES.register("magical_forest_trees",
      MagicalForestFeatures.Trees::new);
   public static final Supplier<Feature<NoneFeatureConfiguration>> MAGICAL_FOREST_DECORATION = ModFeatures.FEATURES.register(
      "magical_forest_decoration", MagicalForestFeatures.Decoration::new);

   // ModConfig.CONFIG_WORLD options of this milestone (config/thaumcraft-world.toml)
   private static final ModConfigSpec SPEC;
   private static final ModConfigSpec.BooleanValue GENERATE_STRUCTURE;
   private static final ModConfigSpec.BooleanValue GENERATE_MAGIC_FOREST;
   private static final ModConfigSpec.IntValue MAGIC_FOREST_WEIGHT;

   static {
      ModConfigSpec.Builder b = new ModConfigSpec.Builder();
      b.push("world");
      GENERATE_STRUCTURE = b.comment("Generate barrow mounds and lesser crimson portals in the overworld.").define("generateStructure", true);
      GENERATE_MAGIC_FOREST = b.comment("Add the magical forest biome to the overworld (it takes a slice of the forest climate regions).")
         .define("generateMagicForest", true);
      MAGIC_FOREST_WEIGHT = b.comment("Higher values increase the number of magical forest biomes (5 = a third of the forests, 15+ = all of them).")
         .defineInRange("biomeMagicalForestWeight", 5, 0, 100);
      b.pop();
      SPEC = b.build();
   }

   private MobWorldgen() {
   }

   public static void init(IEventBus modBus) {
      ModLoadingContext.get().getActiveContainer().registerConfig(ModConfig.Type.COMMON, SPEC, "thaumcraft-world.toml");
      NeoForge.EVENT_BUS.addListener((ServerAboutToStartEvent e) -> injectMagicalForest(e.getServer()));
   }

   public static boolean generateStructures() {
      return !SPEC.isLoaded() || GENERATE_STRUCTURE.get();
   }

   private static boolean generateMagicForest() {
      return !SPEC.isLoaded() || GENERATE_MAGIC_FOREST.get();
   }

   private static int magicForestWeight() {
      return SPEC.isLoaded() ? MAGIC_FOREST_WEIGHT.get() : 5;
   }

   /** generateSurface only ran in the overworld, and never on flat worlds. */
   public static boolean isNaturalOverworld(WorldGenLevel level, ChunkGenerator generator) {
      return level.getLevel().dimension() == Level.OVERWORLD && !(generator instanceof FlatLevelSource);
   }

   /** generateSurface's spot: the chunk centre +-4, 9 blocks under the precipitation height. */
   public static BlockPos surfaceStructurePos(WorldGenLevel level, BlockPos chunkOrigin, RandomSource random) {
      int x = (chunkOrigin.getX() & ~15) + 8 + Mth.nextInt(random, -4, 4);
      int z = (chunkOrigin.getZ() & ~15) + 8 + Mth.nextInt(random, -4, 4);
      return new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z), z).below(9);
   }

   /**
    * BiomeManager.addBiome(WARM/COOL, magical forest): the overworld climate parameter list (the registry entry
    * minecraft:overworld of worldgen/multi_noise_biome_source_parameter_list, rebuilt from the preset on every server
    * start and saved by reference, so nothing leaks into level.dat) gets the top slice of the weirdness range of every
    * forest point handed to the magical forest. Everything is checked and reverted on failure: worst case, no magical
    * forests generate.
    */
   @SuppressWarnings("unchecked")
   public static void injectMagicalForest(MinecraftServer server) {
      if (!generateMagicForest() || magicForestWeight() <= 0) return;
      Field paramsField = null;
      MultiNoiseBiomeSourceParameterList preset = null;
      Climate.ParameterList<Holder<Biome>> old = null;
      BiomeSource source = null;
      Field possibleField = null;
      Object oldPossible = null;
      ChunkGenerator generator = null;
      try {
         RegistryAccess ra = server.registryAccess();
         preset = ra.registryOrThrow(Registries.MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST).get(MultiNoiseBiomeSourceParameterLists.OVERWORLD);
         LevelStem stem = ra.registryOrThrow(Registries.LEVEL_STEM).get(LevelStem.OVERWORLD);
         Holder<Biome> magic = ra.registryOrThrow(Registries.BIOME).getHolder(MAGICAL_FOREST).orElse(null);
         if (preset == null || stem == null || magic == null) return;
         generator = stem.generator();
         source = generator.getBiomeSource();
         old = preset.parameters();
         if (old.values().stream().anyMatch(p -> p.getSecond().is(MAGICAL_FOREST))) return;
         if (source.possibleBiomes().stream().noneMatch(h -> h.is(Biomes.FOREST))) return; // not a vanilla-like overworld

         double share = Math.min(1.0, magicForestWeight() / 15.0);
         List<Pair<Climate.ParameterPoint, Holder<Biome>>> out = new ArrayList<>();
         int split = 0;
         for (Pair<Climate.ParameterPoint, Holder<Biome>> pair : old.values()) {
            Climate.ParameterPoint pp = pair.getFirst();
            Climate.Parameter w = pp.weirdness();
            if (pair.getSecond().is(Biomes.FOREST) && w.max() > w.min()) {
               long cut = share >= 1.0 ? w.min() : w.max() - (long) ((w.max() - w.min()) * share);
               if (cut > w.min()) {
                  out.add(Pair.of(point(pp, new Climate.Parameter(w.min(), cut)), pair.getSecond()));
               }
               out.add(Pair.of(point(pp, new Climate.Parameter(cut, w.max())), magic));
               split++;
            } else {
               out.add(pair);
            }
         }
         if (split == 0) return;
         paramsField = MultiNoiseBiomeSourceParameterList.class.getDeclaredField("parameters");
         paramsField.setAccessible(true);
         paramsField.set(preset, new Climate.ParameterList<>(out));
         possibleField = BiomeSource.class.getDeclaredField("possibleBiomes");
         possibleField.setAccessible(true);
         oldPossible = possibleField.get(source);
         Set<Holder<Biome>> biomes = out.stream().map(Pair::getSecond).distinct().collect(ImmutableSet.toImmutableSet());
         possibleField.set(source, (Supplier<Set<Holder<Biome>>>) () -> biomes);
         generator.refreshFeaturesPerStep();
         generator.validate(); // throws on a feature order cycle: better to lose the biome than the world
         Thaumcraft.LOGGER.info("Added the magical forest to {} overworld forest climate regions", split);
      } catch (Throwable t) {
         Thaumcraft.LOGGER.error("Could not add the magical forest to the overworld; it will not generate naturally", t);
         try {
            if (paramsField != null && preset != null && old != null) paramsField.set(preset, old);
            if (possibleField != null && oldPossible != null) possibleField.set(source, oldPossible);
            if (generator != null) generator.refreshFeaturesPerStep();
         } catch (Throwable t2) {
            Thaumcraft.LOGGER.error("Could not revert the magical forest injection", t2);
         }
      }
   }

   private static Climate.ParameterPoint point(Climate.ParameterPoint p, Climate.Parameter weirdness) {
      return new Climate.ParameterPoint(p.temperature(), p.humidity(), p.continentalness(), p.erosion(), p.depth(), weirdness, p.offset());
   }
}
