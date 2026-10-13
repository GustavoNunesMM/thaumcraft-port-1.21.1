package thaumcraft.common.registers;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;
import thaumcraft.Thaumcraft;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.blocks.crafting.ArcaneWorkbenchBlock;
import thaumcraft.common.blocks.crafting.CrucibleBlock;
import thaumcraft.common.blocks.misc.NitorBlock;
import thaumcraft.common.blocks.crafting.ArcaneWorkbenchChargerBlock;
import thaumcraft.common.blocks.crafting.ResearchTableBlock;
import thaumcraft.common.blocks.crafting.TableBlock;
import thaumcraft.common.blocks.world.CinderpearlBlock;
import thaumcraft.common.blocks.world.CrystalBlock;
import thaumcraft.common.blocks.world.ShimmerleafBlock;
import thaumcraft.common.blocks.world.SilverwoodLeavesBlock;
import thaumcraft.common.blocks.world.TCSaplingBlock;
import thaumcraft.common.blocks.world.VishroomBlock;

/** Blocks of Thaumcraft 6, ported as they become needed. Names follow the 1.12 registry names. */
public final class ModBlocks {
   public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Thaumcraft.MODID);

   /** Blocks that get a plain BlockItem, in creative-tab order. */
   private static final List<DeferredBlock<? extends Block>> WITH_ITEM = new ArrayList<>();

   // ------------------------------------------------------------------ ores
   public static final DeferredBlock<DropExperienceBlock> ORE_AMBER = register("ore_amber",
      p -> new DropExperienceBlock(UniformInt.of(1, 4), p), stone(1.5F, 5.0F));
   public static final DeferredBlock<Block> ORE_CINNABAR = register("ore_cinnabar", Block::new, stone(2.0F, 5.0F));
   public static final DeferredBlock<DropExperienceBlock> ORE_QUARTZ = register("ore_quartz",
      p -> new DropExperienceBlock(UniformInt.of(1, 4), p), stone(3.0F, 5.0F));
   // Port addition: 1.21 worlds go down to y=-64, where most cinnabar/quartz now generates.
   public static final DeferredBlock<Block> DEEPSLATE_ORE_CINNABAR = register("deepslate_ore_cinnabar", Block::new, deepslate(3.5F));
   public static final DeferredBlock<DropExperienceBlock> DEEPSLATE_ORE_QUARTZ = register("deepslate_ore_quartz",
      p -> new DropExperienceBlock(UniformInt.of(1, 4), p), deepslate(4.5F));

   // ------------------------------------------------------------------ vis crystals
   public static final DeferredBlock<CrystalBlock> CRYSTAL_AER = crystal("crystal_aer", Aspect.AIR);
   public static final DeferredBlock<CrystalBlock> CRYSTAL_IGNIS = crystal("crystal_ignis", Aspect.FIRE);
   public static final DeferredBlock<CrystalBlock> CRYSTAL_AQUA = crystal("crystal_aqua", Aspect.WATER);
   public static final DeferredBlock<CrystalBlock> CRYSTAL_TERRA = crystal("crystal_terra", Aspect.EARTH);
   public static final DeferredBlock<CrystalBlock> CRYSTAL_ORDO = crystal("crystal_ordo", Aspect.ORDER);
   public static final DeferredBlock<CrystalBlock> CRYSTAL_PERDITIO = crystal("crystal_perditio", Aspect.ENTROPY);
   public static final DeferredBlock<CrystalBlock> CRYSTAL_VITIUM = crystal("crystal_vitium", Aspect.FLUX);

   // ------------------------------------------------------------------ trees
   public static final ResourceKey<ConfiguredFeature<?, ?>> GREATWOOD_TREE = featureKey("greatwood_tree");
   public static final ResourceKey<ConfiguredFeature<?, ?>> SILVERWOOD_TREE = featureKey("silverwood_tree");
   /** Greatwood only grows from 2x2 saplings, like the original (TreeGrower's "mega" tree). */
   public static final TreeGrower GREATWOOD_GROWER = new TreeGrower(
      Thaumcraft.MODID + ":greatwood", Optional.of(GREATWOOD_TREE), Optional.empty(), Optional.empty());
   public static final TreeGrower SILVERWOOD_GROWER = new TreeGrower(
      Thaumcraft.MODID + ":silverwood", Optional.empty(), Optional.of(SILVERWOOD_TREE), Optional.empty());

   public static final DeferredBlock<TCSaplingBlock> SAPLING_GREATWOOD = register("sapling_greatwood",
      p -> new TCSaplingBlock(GREATWOOD_GROWER, p), sapling());
   public static final DeferredBlock<TCSaplingBlock> SAPLING_SILVERWOOD = register("sapling_silverwood",
      p -> new TCSaplingBlock(SILVERWOOD_GROWER, p), sapling());
   public static final DeferredBlock<RotatedPillarBlock> LOG_GREATWOOD = register("log_greatwood",
      RotatedPillarBlock::new, log(MapColor.COLOR_BROWN, 0));
   public static final DeferredBlock<RotatedPillarBlock> LOG_SILVERWOOD = register("log_silverwood",
      RotatedPillarBlock::new, log(MapColor.QUARTZ, 5));
   public static final DeferredBlock<LeavesBlock> LEAVES_GREATWOOD = register("leaves_greatwood",
      LeavesBlock::new, leaves(MapColor.PLANT));
   public static final DeferredBlock<SilverwoodLeavesBlock> LEAVES_SILVERWOOD = register("leaves_silverwood",
      SilverwoodLeavesBlock::new, leaves(MapColor.COLOR_LIGHT_BLUE));
   public static final DeferredBlock<Block> PLANK_GREATWOOD = register("plank_greatwood", Block::new, planks(MapColor.COLOR_BROWN));
   public static final DeferredBlock<Block> PLANK_SILVERWOOD = register("plank_silverwood", Block::new, planks(MapColor.QUARTZ));
   public static final DeferredBlock<StairBlock> STAIRS_GREATWOOD = register("stairs_greatwood",
      p -> new StairBlock(PLANK_GREATWOOD.get().defaultBlockState(), p), planks(MapColor.COLOR_BROWN));
   public static final DeferredBlock<StairBlock> STAIRS_SILVERWOOD = register("stairs_silverwood",
      p -> new StairBlock(PLANK_SILVERWOOD.get().defaultBlockState(), p), planks(MapColor.QUARTZ));
   public static final DeferredBlock<SlabBlock> SLAB_GREATWOOD = register("slab_greatwood",
      SlabBlock::new, planks(MapColor.COLOR_BROWN).strength(1.2F, 2.0F));
   public static final DeferredBlock<SlabBlock> SLAB_SILVERWOOD = register("slab_silverwood",
      SlabBlock::new, planks(MapColor.QUARTZ).strength(1.0F, 2.0F));

   // ------------------------------------------------------------------ plants
   public static final DeferredBlock<ShimmerleafBlock> SHIMMERLEAF = register("shimmerleaf", ShimmerleafBlock::new, plant(6));
   public static final DeferredBlock<CinderpearlBlock> CINDERPEARL = register("cinderpearl", CinderpearlBlock::new, plant(7));
   public static final DeferredBlock<VishroomBlock> VISHROOM = register("vishroom", VishroomBlock::new, plant(6));

   // ------------------------------------------------------------------ crafting
   public static final DeferredBlock<TableBlock> TABLE_WOOD = register("table_wood", p -> new TableBlock(true, p),
      BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.0F).sound(SoundType.WOOD).noOcclusion().ignitedByLava());
   public static final DeferredBlock<TableBlock> TABLE_STONE = register("table_stone", p -> new TableBlock(false, p),
      BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(2.0F).sound(SoundType.STONE).noOcclusion());
   public static final DeferredBlock<ResearchTableBlock> RESEARCH_TABLE = register("research_table", ResearchTableBlock::new,
      BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.5F).sound(SoundType.WOOD).noOcclusion());
   public static final DeferredBlock<ArcaneWorkbenchBlock> ARCANE_WORKBENCH = register("arcane_workbench", ArcaneWorkbenchBlock::new,
      BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.5F).sound(SoundType.WOOD).noOcclusion().ignitedByLava());
   public static final DeferredBlock<ArcaneWorkbenchChargerBlock> ARCANE_WORKBENCH_CHARGER = register("arcane_workbench_charger",
      ArcaneWorkbenchChargerBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(1.25F, 10.0F).sound(SoundType.WOOD).noOcclusion());

   public static final DeferredBlock<CrucibleBlock> CRUCIBLE = register("crucible", CrucibleBlock::new,
      BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(2.0F).sound(SoundType.METAL).noOcclusion().requiresCorrectToolForDrops());

   // ------------------------------------------------------------------ nitor (one block per dye color, as in the original)
   public static final java.util.Map<net.minecraft.world.item.DyeColor, DeferredBlock<NitorBlock>> NITOR = nitor();

   private static java.util.Map<net.minecraft.world.item.DyeColor, DeferredBlock<NitorBlock>> nitor() {
      var map = new java.util.EnumMap<net.minecraft.world.item.DyeColor, DeferredBlock<NitorBlock>>(net.minecraft.world.item.DyeColor.class);
      for (var dye : net.minecraft.world.item.DyeColor.values()) {
         map.put(dye, register("nitor_" + dye.getSerializedName(), p -> new NitorBlock(dye, p), BlockBehaviour.Properties.of()
            .mapColor(dye).strength(0.1F).sound(SoundType.WOOL).lightLevel(s -> 15).noCollission().noOcclusion()
            .pushReaction(net.minecraft.world.level.material.PushReaction.DESTROY)));
      }
      return java.util.Collections.unmodifiableMap(map);
   }

   // ------------------------------------------------------------------ essentia
   public static final DeferredBlock<thaumcraft.common.blocks.essentia.JarBlock> JAR_NORMAL = registerNoItem("jar_normal",
      thaumcraft.common.blocks.essentia.JarBlock::new, jar());
   public static final DeferredBlock<thaumcraft.common.blocks.essentia.JarBlock> JAR_VOID = registerNoItem("jar_void",
      thaumcraft.common.blocks.essentia.JarBlock::new, jar());
   public static final DeferredBlock<thaumcraft.common.blocks.essentia.TubeBlock> TUBE = tube("tube", thaumcraft.common.blocks.essentia.TubeBlock.Kind.NORMAL);
   public static final DeferredBlock<thaumcraft.common.blocks.essentia.TubeBlock> TUBE_VALVE = tube("tube_valve", thaumcraft.common.blocks.essentia.TubeBlock.Kind.VALVE);
   public static final DeferredBlock<thaumcraft.common.blocks.essentia.TubeBlock> TUBE_RESTRICT = tube("tube_restrict", thaumcraft.common.blocks.essentia.TubeBlock.Kind.RESTRICT);
   public static final DeferredBlock<thaumcraft.common.blocks.essentia.TubeBlock> TUBE_ONEWAY = tube("tube_oneway", thaumcraft.common.blocks.essentia.TubeBlock.Kind.ONEWAY);
   public static final DeferredBlock<thaumcraft.common.blocks.essentia.TubeBlock> TUBE_FILTER = tube("tube_filter", thaumcraft.common.blocks.essentia.TubeBlock.Kind.FILTER);
   public static final DeferredBlock<thaumcraft.common.blocks.essentia.TubeBlock> TUBE_BUFFER = tube("tube_buffer", thaumcraft.common.blocks.essentia.TubeBlock.Kind.BUFFER);
   public static final DeferredBlock<thaumcraft.common.blocks.essentia.SmelterBlock> SMELTER_BASIC = register("smelter_basic",
      thaumcraft.common.blocks.essentia.SmelterBlock::new, smelter());
   public static final DeferredBlock<thaumcraft.common.blocks.essentia.SmelterBlock> SMELTER_THAUMIUM = register("smelter_thaumium",
      thaumcraft.common.blocks.essentia.SmelterBlock::new, smelter());
   public static final DeferredBlock<thaumcraft.common.blocks.essentia.SmelterBlock> SMELTER_VOID = register("smelter_void",
      thaumcraft.common.blocks.essentia.SmelterBlock::new, smelter());
   public static final DeferredBlock<thaumcraft.common.blocks.essentia.SmelterPartBlock> SMELTER_AUX = register("smelter_aux",
      p -> new thaumcraft.common.blocks.essentia.SmelterPartBlock(false, p), smelterPart());
   public static final DeferredBlock<thaumcraft.common.blocks.essentia.SmelterPartBlock> SMELTER_VENT = register("smelter_vent",
      p -> new thaumcraft.common.blocks.essentia.SmelterPartBlock(true, p), smelterPart());
   public static final DeferredBlock<thaumcraft.common.blocks.essentia.AlembicBlock> ALEMBIC = register("alembic",
      thaumcraft.common.blocks.essentia.AlembicBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.0F).sound(SoundType.WOOD).noOcclusion());
   public static final DeferredBlock<thaumcraft.common.blocks.essentia.CentrifugeBlock> CENTRIFUGE = register("centrifuge",
      thaumcraft.common.blocks.essentia.CentrifugeBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.0F).sound(SoundType.WOOD).noOcclusion());
   public static final DeferredBlock<thaumcraft.common.blocks.devices.BellowsBlock> BELLOWS = register("bellows",
      thaumcraft.common.blocks.devices.BellowsBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(1.0F).sound(SoundType.WOOD).noOcclusion());
   public static final DeferredBlock<thaumcraft.common.blocks.essentia.EssentiaTransportBlock> ESSENTIA_INPUT = register("essentia_input",
      p -> new thaumcraft.common.blocks.essentia.EssentiaTransportBlock(true, p), smelterPart().noOcclusion());
   public static final DeferredBlock<thaumcraft.common.blocks.essentia.EssentiaTransportBlock> ESSENTIA_OUTPUT = register("essentia_output",
      p -> new thaumcraft.common.blocks.essentia.EssentiaTransportBlock(false, p), smelterPart().noOcclusion());

   // ------------------------------------------------------------------ stone
   public static final DeferredBlock<Block> STONE_ARCANE = register("stone_arcane", Block::new, tcStone(2.0F));
   public static final DeferredBlock<Block> STONE_ARCANE_BRICK = register("stone_arcane_brick", Block::new, tcStone(2.0F));
   public static final DeferredBlock<Block> STONE_ANCIENT = register("stone_ancient", Block::new, tcStone(2.0F));
   public static final DeferredBlock<Block> STONE_ANCIENT_TILE = register("stone_ancient_tile", Block::new, tcStone(2.0F));
   public static final DeferredBlock<Block> STONE_ANCIENT_GLYPHED = register("stone_ancient_glyphed", Block::new, tcStone(2.0F));
   public static final DeferredBlock<Block> STONE_ANCIENT_ROCK = register("stone_ancient_rock", Block::new, tcStone(-1.0F).strength(-1.0F, 3600000.0F));
   public static final DeferredBlock<Block> STONE_ANCIENT_DOORWAY = register("stone_ancient_doorway", Block::new, tcStone(-1.0F).strength(-1.0F, 3600000.0F));
   public static final DeferredBlock<Block> STONE_ELDRITCH_TILE = register("stone_eldritch_tile", Block::new, tcStone(15.0F).strength(15.0F, 1000.0F));
   public static final DeferredBlock<StairBlock> STAIRS_ARCANE = register("stairs_arcane",
      p -> new StairBlock(STONE_ARCANE.get().defaultBlockState(), p), tcStone(2.0F));
   public static final DeferredBlock<StairBlock> STAIRS_ARCANE_BRICK = register("stairs_arcane_brick",
      p -> new StairBlock(STONE_ARCANE_BRICK.get().defaultBlockState(), p), tcStone(2.0F));
   public static final DeferredBlock<StairBlock> STAIRS_ANCIENT = register("stairs_ancient",
      p -> new StairBlock(STONE_ANCIENT.get().defaultBlockState(), p), tcStone(2.0F));
   public static final DeferredBlock<SlabBlock> SLAB_ARCANE_STONE = register("slab_arcane_stone", SlabBlock::new, tcStone(2.0F));
   public static final DeferredBlock<SlabBlock> SLAB_ARCANE_BRICK = register("slab_arcane_brick", SlabBlock::new, tcStone(2.0F));
   public static final DeferredBlock<SlabBlock> SLAB_ANCIENT = register("slab_ancient", SlabBlock::new, tcStone(2.0F));
   public static final DeferredBlock<SlabBlock> SLAB_ELDRITCH = register("slab_eldritch", SlabBlock::new, tcStone(2.0F));

   // ------------------------------------------------------------------ infusion (milestone 8)
   public static final DeferredBlock<thaumcraft.common.blocks.infusion.InfusionMatrixBlock> INFUSION_MATRIX = register("infusion_matrix",
      thaumcraft.common.blocks.infusion.InfusionMatrixBlock::new, tcStone(2.0F).noOcclusion().lightLevel(s -> 8));
   public static final DeferredBlock<thaumcraft.common.blocks.infusion.PedestalBlock> PEDESTAL_ARCANE = register("pedestal_arcane",
      thaumcraft.common.blocks.infusion.PedestalBlock::new, tcStone(2.0F).noOcclusion());
   public static final DeferredBlock<thaumcraft.common.blocks.infusion.PedestalBlock> PEDESTAL_ANCIENT = register("pedestal_ancient",
      thaumcraft.common.blocks.infusion.PedestalBlock::new, tcStone(2.0F).noOcclusion());
   public static final DeferredBlock<thaumcraft.common.blocks.infusion.PedestalBlock> PEDESTAL_ELDRITCH = register("pedestal_eldritch",
      thaumcraft.common.blocks.infusion.PedestalBlock::new, tcStone(2.0F).noOcclusion());
   public static final DeferredBlock<thaumcraft.common.blocks.infusion.PillarBlock> PILLAR_ARCANE = register("pillar_arcane",
      thaumcraft.common.blocks.infusion.PillarBlock::new, tcStone(2.5F).noOcclusion());
   public static final DeferredBlock<thaumcraft.common.blocks.infusion.PillarBlock> PILLAR_ANCIENT = register("pillar_ancient",
      thaumcraft.common.blocks.infusion.PillarBlock::new, tcStone(2.5F).noOcclusion());
   public static final DeferredBlock<thaumcraft.common.blocks.infusion.PillarBlock> PILLAR_ELDRITCH = register("pillar_eldritch",
      thaumcraft.common.blocks.infusion.PillarBlock::new, tcStone(2.5F).noOcclusion());
   public static final DeferredBlock<Block> MATRIX_SPEED = register("matrix_speed", Block::new, tcStone(2.0F));
   public static final DeferredBlock<Block> MATRIX_COST = register("matrix_cost", Block::new, tcStone(2.0F));
   public static final DeferredBlock<thaumcraft.common.blocks.devices.StabilizerBlock> STABILIZER = register("stabilizer",
      thaumcraft.common.blocks.devices.StabilizerBlock::new, tcStone(2.0F).noOcclusion().lightLevel(s -> 4));
   public static final DeferredBlock<thaumcraft.common.blocks.infusion.InlayBlock> INLAY = register("inlay",
      thaumcraft.common.blocks.infusion.InlayBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(0.5F).sound(SoundType.METAL)
         .noOcclusion().lightLevel(s -> 1).pushReaction(PushReaction.DESTROY));
   public static final java.util.Map<net.minecraft.world.item.DyeColor, DeferredBlock<thaumcraft.common.blocks.devices.CandleBlock>> CANDLES = candles();
   /** BlockEffect "glimmer": the invisible light left behind by lamplight tools. */
   public static final DeferredBlock<Block> EFFECT_GLIMMER = registerNoItem("effect_glimmer", Block::new, BlockBehaviour.Properties.of()
      .replaceable().noCollission().noLootTable().air().lightLevel(s -> 15).instabreak());

   private static java.util.Map<net.minecraft.world.item.DyeColor, DeferredBlock<thaumcraft.common.blocks.devices.CandleBlock>> candles() {
      var map = new java.util.EnumMap<net.minecraft.world.item.DyeColor, DeferredBlock<thaumcraft.common.blocks.devices.CandleBlock>>(net.minecraft.world.item.DyeColor.class);
      for (var dye : net.minecraft.world.item.DyeColor.values()) {
         map.put(dye, register("candle_" + dye.getSerializedName(), p -> new thaumcraft.common.blocks.devices.CandleBlock(dye, p),
            BlockBehaviour.Properties.of().mapColor(dye).strength(0.1F).sound(SoundType.WOOL).lightLevel(s -> 14).noOcclusion()
               .pushReaction(PushReaction.DESTROY)));
      }
      return java.util.Collections.unmodifiableMap(map);
   }

   // ------------------------------------------------------------------ material blocks
   public static final DeferredBlock<Block> AMBER_BLOCK = register("amber_block", Block::new, amber());
   public static final DeferredBlock<Block> AMBER_BRICK = register("amber_brick", Block::new, amber());
   public static final DeferredBlock<Block> METAL_THAUMIUM = register("metal_thaumium", Block::new, metal(MapColor.COLOR_PURPLE));
   public static final DeferredBlock<Block> METAL_VOID = register("metal_void", Block::new, metal(MapColor.COLOR_BLACK));
   public static final DeferredBlock<Block> METAL_BRASS = register("metal_brass", Block::new, metal(MapColor.GOLD));

   private static <B extends Block> DeferredBlock<B> register(String name, Function<BlockBehaviour.Properties, ? extends B> f, BlockBehaviour.Properties p) {
      DeferredBlock<B> b = BLOCKS.registerBlock(name, f, p);
      WITH_ITEM.add(b);
      return b;
   }

   private static <B extends Block> DeferredBlock<B> registerNoItem(String name, Function<BlockBehaviour.Properties, ? extends B> f, BlockBehaviour.Properties p) {
      return BLOCKS.registerBlock(name, f, p);
   }

   private static DeferredBlock<thaumcraft.common.blocks.essentia.TubeBlock> tube(String name, thaumcraft.common.blocks.essentia.TubeBlock.Kind kind) {
      return register(name, p -> new thaumcraft.common.blocks.essentia.TubeBlock(kind, p), BlockBehaviour.Properties.of().mapColor(MapColor.METAL)
         .strength(0.5F, 5.0F).sound(SoundType.METAL).noOcclusion());
   }

   private static BlockBehaviour.Properties jar() {
      return BlockBehaviour.Properties.of().mapColor(MapColor.NONE).strength(0.3F).sound(ModSounds.JAR_SOUND).noOcclusion()
         .isValidSpawn((s, l, p, e) -> false).isRedstoneConductor((s, l, p) -> false).isSuffocating((s, l, p) -> false).isViewBlocking((s, l, p) -> false);
   }

   private static BlockBehaviour.Properties smelter() {
      return BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(2.0F, 10.0F).sound(SoundType.METAL)
         .lightLevel(s -> s.getValue(thaumcraft.common.blocks.DeviceBlock.ENABLED) ? 13 : 0);
   }

   private static BlockBehaviour.Properties smelterPart() {
      return BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(1.0F, 10.0F).sound(SoundType.METAL);
   }

   private static BlockBehaviour.Properties tcStone(float hardness) {
      return BlockBehaviour.Properties.of().mapColor(MapColor.STONE).instrument(NoteBlockInstrument.BASEDRUM)
         .requiresCorrectToolForDrops().strength(hardness, 10.0F).sound(SoundType.STONE);
   }

   private static DeferredBlock<CrystalBlock> crystal(String name, Aspect aspect) {
      return register(name, p -> new CrystalBlock(aspect, p), BlockBehaviour.Properties.of()
         .mapColor(MapColor.NONE)
         .strength(0.25F)
         .randomTicks()
         .sound(ModSounds.CRYSTAL_SOUND)
         .noOcclusion()
         .noCollission()
         .lightLevel(s -> 1)
         .emissiveRendering((s, l, pos) -> true)
         .pushReaction(PushReaction.DESTROY));
   }

   private static ResourceKey<ConfiguredFeature<?, ?>> featureKey(String name) {
      return ResourceKey.create(Registries.CONFIGURED_FEATURE, ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, name));
   }

   private static BlockBehaviour.Properties stone(float hardness, float resistance) {
      return BlockBehaviour.Properties.of().mapColor(MapColor.STONE).instrument(NoteBlockInstrument.BASEDRUM)
         .requiresCorrectToolForDrops().strength(hardness, resistance);
   }

   private static BlockBehaviour.Properties deepslate(float hardness) {
      return BlockBehaviour.Properties.ofLegacyCopy(Blocks.DEEPSLATE_IRON_ORE).strength(hardness, 5.0F);
   }

   private static BlockBehaviour.Properties sapling() {
      return BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_SAPLING);
   }

   private static BlockBehaviour.Properties log(MapColor color, int light) {
      return BlockBehaviour.Properties.of().mapColor(color).instrument(NoteBlockInstrument.BASS)
         .strength(2.0F, 5.0F).sound(SoundType.WOOD).ignitedByLava().lightLevel(s -> light);
   }

   private static BlockBehaviour.Properties leaves(MapColor color) {
      return BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_LEAVES).mapColor(color);
   }

   private static BlockBehaviour.Properties planks(MapColor color) {
      return BlockBehaviour.Properties.of().mapColor(color).instrument(NoteBlockInstrument.BASS)
         .strength(2.0F, 3.0F).sound(SoundType.WOOD).ignitedByLava();
   }

   private static BlockBehaviour.Properties plant(int light) {
      return BlockBehaviour.Properties.of().mapColor(MapColor.PLANT).noCollission().instabreak()
         .sound(SoundType.GRASS).offsetType(BlockBehaviour.OffsetType.XZ).pushReaction(PushReaction.DESTROY)
         .lightLevel(s -> light);
   }

   private static BlockBehaviour.Properties amber() {
      return BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE).strength(0.5F).sound(SoundType.STONE)
         .noOcclusion().isValidSpawn((s, l, p, e) -> false).isRedstoneConductor((s, l, p) -> false)
         .isSuffocating((s, l, p) -> false).isViewBlocking((s, l, p) -> false);
   }

   private static BlockBehaviour.Properties metal(MapColor color) {
      return BlockBehaviour.Properties.of().mapColor(color).requiresCorrectToolForDrops().strength(4.0F, 10.0F).sound(SoundType.METAL);
   }

   /** Block items for every block registered here; called from ModItems so they share its register. */
   static void registerBlockItems(DeferredRegister.Items items) {
      for (DeferredBlock<? extends Block> b : WITH_ITEM) {
         items.register(b.getId().getPath(), () -> new BlockItem(b.get(), new Item.Properties()));
      }
   }

   public static List<CrystalBlock> crystals() {
      return List.of(CRYSTAL_AER.get(), CRYSTAL_IGNIS.get(), CRYSTAL_AQUA.get(), CRYSTAL_TERRA.get(),
         CRYSTAL_ORDO.get(), CRYSTAL_PERDITIO.get(), CRYSTAL_VITIUM.get());
   }

   /** Same values as the original's getFlammability/getFireSpreadSpeed overrides (planks use vanilla wood values). */
   public static void registerFlammables() {
      FireBlock fire = (FireBlock) Blocks.FIRE;
      for (var b : List.of(LOG_GREATWOOD, LOG_SILVERWOOD)) fire.setFlammable(b.get(), 5, 5);
      for (var b : List.of(LEAVES_GREATWOOD, LEAVES_SILVERWOOD, SAPLING_GREATWOOD, SAPLING_SILVERWOOD)) fire.setFlammable(b.get(), 30, 60);
      for (var b : List.of(PLANK_GREATWOOD, PLANK_SILVERWOOD, STAIRS_GREATWOOD, STAIRS_SILVERWOOD, SLAB_GREATWOOD, SLAB_SILVERWOOD)) {
         fire.setFlammable(b.get(), 5, 20);
      }
   }

   private ModBlocks() {
   }
}
