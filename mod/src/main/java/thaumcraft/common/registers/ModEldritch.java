package thaumcraft.common.registers;

import com.mojang.serialization.MapCodec;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import thaumcraft.Thaumcraft;
import thaumcraft.common.blocks.eldritch.CrabSpawnerBlock;
import thaumcraft.common.blocks.eldritch.EldritchAltarBlock;
import thaumcraft.common.blocks.eldritch.EldritchLockBlock;
import thaumcraft.common.blocks.eldritch.EldritchObeliskBlock;
import thaumcraft.common.blocks.eldritch.EldritchPortalBlock;
import thaumcraft.common.blocks.eldritch.EldritchPortalBlockEntity;
import thaumcraft.common.blocks.eldritch.HoleBlock;
import thaumcraft.common.blocks.eldritch.HoleBlockEntity;
import thaumcraft.common.blocks.eldritch.PorousStoneBlock;
import thaumcraft.common.entities.monster.boss.EldritchGolemEntity;
import thaumcraft.common.entities.monster.boss.EldritchWardenEntity;
import thaumcraft.common.entities.monster.boss.TaintacleGiantEntity;
import thaumcraft.common.entities.monster.eldritch.EldritchCrabEntity;
import thaumcraft.common.entities.monster.eldritch.EldritchGuardianEntity;
import thaumcraft.common.entities.monster.eldritch.InhabitedZombieEntity;
import thaumcraft.common.entities.projectile.EldritchOrbEntity;
import thaumcraft.common.items.eldritch.ObeliskPlacerItem;
import thaumcraft.common.items.eldritch.RunedTabletItem;
import thaumcraft.common.research.ScanningManager;
import thaumcraft.common.world.eldritch.ObeliskFeature;
import thaumcraft.common.world.eldritch.OuterLandsChunkGenerator;

/**
 * Milestone 14: the eldritch. Porous stone, the dimensional tear of the rift focus, the Outer Lands (chunk generator,
 * maze and its blocks: altar, obelisks, locks, portals, crab vents), the eldritch eye and runed tablet, and the
 * eldritch creatures and bosses. Blocks, items, block entities, entities and the obelisk feature go into the shared
 * registers; the chunk generator codec gets its own.
 */
public final class ModEldritch {
   public static final DeferredRegister<MapCodec<? extends ChunkGenerator>> CHUNK_GENERATORS = DeferredRegister.create(Registries.CHUNK_GENERATOR,
      Thaumcraft.MODID);
   private static final DeferredRegister.Blocks BLOCKS = ModBlocks.BLOCKS;
   private static final DeferredRegister.Items ITEMS = ModItems.ITEMS;

   public static final Supplier<MapCodec<OuterLandsChunkGenerator>> OUTER_LANDS_GENERATOR = CHUNK_GENERATORS.register("outer_lands",
      () -> OuterLandsChunkGenerator.CODEC);
   public static final Supplier<Feature<NoneFeatureConfiguration>> ELDRITCH_OBELISK_FEATURE = ModFeatures.FEATURES.register("eldritch_obelisk",
      ObeliskFeature::new);

   // ------------------------------------------------------------------ blocks
   private static BlockBehaviour.Properties eldritchStone() {
      return BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).instrument(NoteBlockInstrument.BASEDRUM).strength(-1.0F, 3600000.0F)
         .sound(SoundType.STONE).noLootTable();
   }

   public static final DeferredBlock<PorousStoneBlock> STONE_POROUS = withItem("stone_porous", PorousStoneBlock::new,
      BlockBehaviour.Properties.of().mapColor(MapColor.STONE).instrument(NoteBlockInstrument.BASEDRUM).strength(1.0F, 5.0F).sound(SoundType.STONE));
   public static final DeferredBlock<HoleBlock> HOLE = BLOCKS.registerBlock("hole", HoleBlock::new,
      BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(-1.0F, 6000000.0F).sound(SoundType.WOOL).lightLevel(s -> 10)
         .noCollission().noOcclusion().noLootTable().pushReaction(PushReaction.BLOCK).isValidSpawn((s, l, p, e) -> false));
   /** BlocksTC.eldritch: the altar keeps the bare id (ritualist cultists and the theorycraft aid look it up). */
   public static final DeferredBlock<EldritchAltarBlock> ELDRITCH_ALTAR = withItem("eldritch", EldritchAltarBlock::new, eldritchStone().lightLevel(
      s -> s.getValue(EldritchAltarBlock.EYES) * 3));
   public static final DeferredBlock<EldritchObeliskBlock> ELDRITCH_OBELISK = withItem("eldritch_obelisk", EldritchObeliskBlock::new,
      eldritchStone().lightLevel(s -> s.getValue(EldritchObeliskBlock.LIT) ? 8 : 0));
   public static final DeferredBlock<EldritchObeliskBlock> ELDRITCH_CAP = withItem("eldritch_cap", p -> new EldritchObeliskBlock(p, true),
      eldritchStone().lightLevel(s -> s.getValue(EldritchObeliskBlock.LIT) ? 10 : 0));
   public static final DeferredBlock<EldritchLockBlock> ELDRITCH_LOCK = withItem("eldritch_lock", EldritchLockBlock::new, eldritchStone());
   public static final DeferredBlock<EldritchPortalBlock> ELDRITCH_PORTAL = BLOCKS.registerBlock("eldritch_portal", EldritchPortalBlock::new,
      BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(-1.0F, 3600000.0F).noCollission().noOcclusion().lightLevel(s -> 12)
         .noLootTable().pushReaction(PushReaction.BLOCK));
   public static final DeferredBlock<CrabSpawnerBlock> CRAB_SPAWNER = withItem("eldritch_crab_spawner", CrabSpawnerBlock::new, eldritchStone());

   private static <B extends Block> DeferredBlock<B> withItem(String name, Function<BlockBehaviour.Properties, ? extends B> f, BlockBehaviour.Properties p) {
      DeferredBlock<B> b = BLOCKS.registerBlock(name, f, p);
      ITEMS.registerSimpleBlockItem(b);
      return b;
   }

   // ------------------------------------------------------------------ block entities
   public static final Supplier<BlockEntityType<HoleBlockEntity>> HOLE_BE = ModBlockEntities.BLOCK_ENTITIES.register("hole",
      () -> BlockEntityType.Builder.of(HoleBlockEntity::new, HOLE.get()).build(null));
   public static final Supplier<BlockEntityType<EldritchPortalBlockEntity>> ELDRITCH_PORTAL_BE = ModBlockEntities.BLOCK_ENTITIES.register(
      "eldritch_portal", () -> BlockEntityType.Builder.of(EldritchPortalBlockEntity::new, ELDRITCH_PORTAL.get()).build(null));
   public static final Supplier<BlockEntityType<CrabSpawnerBlock.Tile>> CRAB_SPAWNER_BE = ModBlockEntities.BLOCK_ENTITIES.register(
      "eldritch_crab_spawner", () -> BlockEntityType.Builder.of(CrabSpawnerBlock.Tile::new, CRAB_SPAWNER.get()).build(null));

   // ------------------------------------------------------------------ items
   public static final DeferredItem<RunedTabletItem> ELDRITCH_EYE = ITEMS.registerItem("eldritch_eye",
      p -> new RunedTabletItem(p, "item.eldritch_eye.text"), new Item.Properties().rarity(Rarity.UNCOMMON).stacksTo(16));
   public static final DeferredItem<RunedTabletItem> RUNED_TABLET = ITEMS.registerItem("runed_tablet",
      p -> new RunedTabletItem(p, "item.runed_tablet.text"), new Item.Properties().rarity(Rarity.UNCOMMON).stacksTo(1));
   public static final DeferredItem<ObeliskPlacerItem> CREATIVE_PLACER_OBELISK = ITEMS.registerItem("creative_placer_obelisk", ObeliskPlacerItem::new,
      new Item.Properties().rarity(Rarity.EPIC).stacksTo(1));

   // ------------------------------------------------------------------ entities
   private static <T extends net.minecraft.world.entity.Entity> Supplier<EntityType<T>> entity(String name, EntityType.Builder<T> b) {
      return ModEntities.ENTITIES.register(name, () -> b.build(name));
   }

   public static final Supplier<EntityType<EldritchGuardianEntity>> ELDRITCH_GUARDIAN = entity("eldritch_guardian",
      EntityType.Builder.of(EldritchGuardianEntity::new, MobCategory.MONSTER).sized(0.8F, 2.25F).eyeHeight(2.1F).clientTrackingRange(8));
   public static final Supplier<EntityType<EldritchCrabEntity>> ELDRITCH_CRAB = entity("eldritch_crab",
      EntityType.Builder.of(EldritchCrabEntity::new, MobCategory.MONSTER).sized(0.8F, 0.6F).clientTrackingRange(8));
   public static final Supplier<EntityType<InhabitedZombieEntity>> INHABITED_ZOMBIE = entity("inhabited_zombie",
      EntityType.Builder.of(InhabitedZombieEntity::new, MobCategory.MONSTER).sized(0.6F, 1.95F).eyeHeight(1.74F).clientTrackingRange(8));
   public static final Supplier<EntityType<EldritchWardenEntity>> ELDRITCH_WARDEN = entity("eldritch_warden",
      EntityType.Builder.of(EldritchWardenEntity::new, MobCategory.MONSTER).sized(1.5F, 3.5F).eyeHeight(3.1F).fireImmune().clientTrackingRange(8));
   public static final Supplier<EntityType<EldritchGolemEntity>> ELDRITCH_GOLEM = entity("eldritch_golem",
      EntityType.Builder.of(EldritchGolemEntity::new, MobCategory.MONSTER).sized(1.75F, 3.5F).eyeHeight(3.0F).fireImmune().clientTrackingRange(8));
   public static final Supplier<EntityType<TaintacleGiantEntity>> TAINTACLE_GIANT = entity("taintacle_giant",
      EntityType.Builder.of(TaintacleGiantEntity::new, MobCategory.MONSTER).sized(1.1F, 6.0F).clientTrackingRange(10));
   public static final Supplier<EntityType<EldritchOrbEntity>> ELDRITCH_ORB = entity("eldritch_orb",
      EntityType.Builder.<EldritchOrbEntity>of(EldritchOrbEntity::new, MobCategory.MISC).sized(0.25F, 0.25F).clientTrackingRange(4).updateInterval(10));

   // ------------------------------------------------------------------ spawn eggs (ConfigEntities egg colours)
   static {
      egg("eldritch_guardian", ELDRITCH_GUARDIAN, 8421504, 0);
      egg("eldritch_crab", ELDRITCH_CRAB, 8421504, 5570560);
      egg("inhabited_zombie", INHABITED_ZOMBIE, 8421504, 5570560);
      egg("eldritch_warden", ELDRITCH_WARDEN, 6842578, 8421504);
      egg("eldritch_golem", ELDRITCH_GOLEM, 6842578, 8947848);
      egg("taintacle_giant", TAINTACLE_GIANT, 6842578, 10618530);
   }

   private static DeferredItem<DeferredSpawnEggItem> egg(String name, Supplier<? extends EntityType<? extends Mob>> type, int bg, int fg) {
      return ITEMS.register(name + "_spawn_egg", () -> new DeferredSpawnEggItem(type, bg, fg, new Item.Properties()));
   }

   private ModEldritch() {
   }

   /** Called from the Thaumcraft constructor. */
   public static void init(IEventBus modBus) {
      CHUNK_GENERATORS.register(modBus);
      modBus.addListener(ModEldritch::attributes);
      modBus.addListener(ModEldritch::spawnPlacements);
      modBus.addListener((FMLCommonSetupEvent e) -> e.enqueueWork(ModEldritch::scans));
   }

   private static void attributes(EntityAttributeCreationEvent event) {
      event.put(ELDRITCH_GUARDIAN.get(), EldritchGuardianEntity.createAttributes().build());
      event.put(ELDRITCH_CRAB.get(), EldritchCrabEntity.createAttributes().build());
      event.put(INHABITED_ZOMBIE.get(), InhabitedZombieEntity.createAttributes().build());
      event.put(ELDRITCH_WARDEN.get(), EldritchWardenEntity.createAttributes().build());
      event.put(ELDRITCH_GOLEM.get(), EldritchGolemEntity.createAttributes().build());
      event.put(TAINTACLE_GIANT.get(), TaintacleGiantEntity.createGiantAttributes().build());
   }

   private static void spawnPlacements(RegisterSpawnPlacementsEvent event) {
      var op = RegisterSpawnPlacementsEvent.Operation.REPLACE;
      event.register(ELDRITCH_GUARDIAN.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
         EldritchGuardianEntity::checkSpawnRules, op);
      event.register(INHABITED_ZOMBIE.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
         InhabitedZombieEntity::checkSpawnRules, op);
      event.register(ELDRITCH_CRAB.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
         net.minecraft.world.entity.monster.Monster::checkMonsterSpawnRules, op);
   }

   /** ConfigResearch scannables of the eldritch: its creatures, bosses and ancient stones. */
   private static void scans() {
      ScanningManager.addScannableThing(ScanningManager.ScanEntity.of("!EldritchCrab", EldritchCrabEntity.class));
      ScanningManager.addScannableThing(ScanningManager.ScanEntity.of("!EldritchCrab", InhabitedZombieEntity.class));
      ScanningManager.addScannableThing(ScanningManager.ScanEntity.of("!EldritchGuardian", EldritchGuardianEntity.class));
      ScanningManager.addScannableThing(new ScanningManager.ScanEntity("!ORMOB", thaumcraft.common.entities.monster.eldritch.EldritchHelper::isEldritch));
      ScanningManager.addScannableThing(ScanningManager.ScanEntity.of("!ORBOSS", thaumcraft.common.entities.monster.cult.ThaumcraftBossEntity.class));
      ScanningManager.addScannableThing(ScanningManager.ScanBlock.of("!ORBLOCK1", ModBlocks.STONE_ANCIENT, ModBlocks.STONE_ANCIENT_TILE));
      ScanningManager.addScannableThing(ScanningManager.ScanBlock.of("!ORBLOCK2", ModBlocks.STONE_ELDRITCH_TILE));
      ScanningManager.addScannableThing(ScanningManager.ScanBlock.of("!ORBLOCK3", ModBlocks.STONE_ANCIENT_GLYPHED));
   }
}
