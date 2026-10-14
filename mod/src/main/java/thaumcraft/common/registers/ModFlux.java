package thaumcraft.common.registers;

import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.SoundActions;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.common.util.DeferredSoundType;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import thaumcraft.Thaumcraft;
import thaumcraft.common.blocks.crafting.VoidSiphonBlock;
import thaumcraft.common.blocks.crafting.VoidSiphonBlockEntity;
import thaumcraft.common.blocks.devices.CondenserBlock;
import thaumcraft.common.blocks.devices.CondenserBlockEntity;
import thaumcraft.common.blocks.devices.CondenserLatticeBlock;
import thaumcraft.common.blocks.misc.LiquidDeathBlock;
import thaumcraft.common.blocks.misc.PurifyingFluidBlock;
import thaumcraft.common.blocks.world.taint.FluxGooBlock;
import thaumcraft.common.blocks.world.taint.FluxGooFluid;
import thaumcraft.common.blocks.world.taint.TaintBlock;
import thaumcraft.common.blocks.world.taint.TaintFeatureBlock;
import thaumcraft.common.blocks.world.taint.TaintFibreBlock;
import thaumcraft.common.blocks.world.taint.TaintLogBlock;
import thaumcraft.common.entities.FallingTaintEntity;
import thaumcraft.common.entities.FluxRiftEntity;
import thaumcraft.common.entities.monster.tainted.TaintCrawlerEntity;
import thaumcraft.common.entities.monster.tainted.TaintSeedEntity;
import thaumcraft.common.entities.monster.tainted.TaintSeedPrimeEntity;
import thaumcraft.common.entities.monster.tainted.TaintSwarmEntity;
import thaumcraft.common.entities.monster.tainted.TaintacleEntity;
import thaumcraft.common.entities.monster.tainted.TaintacleSmallEntity;
import thaumcraft.common.entities.projectile.BottleTaintEntity;
import thaumcraft.common.entities.projectile.CausalityCollapserEntity;
import thaumcraft.common.items.consumables.BathSaltsItem;
import thaumcraft.common.items.consumables.BottleTaintItem;
import thaumcraft.common.items.consumables.CausalityCollapserItem;
import thaumcraft.common.items.consumables.FluxGooBucketItem;
import thaumcraft.common.items.consumables.SanitySoapItem;
import thaumcraft.common.lib.potions.FluxTaintEffect;
import thaumcraft.common.lib.potions.InfectiousVisExhaustEffect;
import thaumcraft.common.lib.potions.SunScornedEffect;
import thaumcraft.common.lib.potions.TCMobEffect;
import thaumcraft.common.lib.potions.ThaumarhiaEffect;
import thaumcraft.common.lib.potions.UnnaturalHungerEffect;
import thaumcraft.common.menu.VoidSiphonMenu;

/**
 * Milestone 12: flux and taint. Potions, the three fluids (flux goo, liquid death, purifying fluid), the taint blocks,
 * flux rifts and the tainted creatures, the flux handling devices (condenser, lattice, void siphon) and consumables.
 * Blocks, items, block entities, entities and menus go into the shared registers; mob effects, fluid types and fluids
 * get their own.
 */
public final class ModFlux {
   public static final DeferredRegister<MobEffect> MOB_EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, Thaumcraft.MODID);
   public static final DeferredRegister<FluidType> FLUID_TYPES = DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, Thaumcraft.MODID);
   public static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(Registries.FLUID, Thaumcraft.MODID);
   private static final DeferredRegister.Blocks BLOCKS = ModBlocks.BLOCKS;
   private static final DeferredRegister.Items ITEMS = ModItems.ITEMS;

   // ------------------------------------------------------------------ potions
   public static final DeferredHolder<MobEffect, MobEffect> FLUX_TAINT = MOB_EFFECTS.register("flux_taint", FluxTaintEffect::new);
   public static final DeferredHolder<MobEffect, MobEffect> VIS_EXHAUST = MOB_EFFECTS.register("vis_exhaust",
      () -> new TCMobEffect(MobEffectCategory.HARMFUL, 6702199));
   public static final DeferredHolder<MobEffect, MobEffect> INFECTIOUS_VIS_EXHAUST = MOB_EFFECTS.register("infectious_vis_exhaust",
      InfectiousVisExhaustEffect::new);
   public static final DeferredHolder<MobEffect, MobEffect> UNNATURAL_HUNGER = MOB_EFFECTS.register("unnatural_hunger", UnnaturalHungerEffect::new);
   public static final DeferredHolder<MobEffect, MobEffect> WARP_WARD = MOB_EFFECTS.register("warp_ward",
      () -> new TCMobEffect(MobEffectCategory.BENEFICIAL, 14742263));
   public static final DeferredHolder<MobEffect, MobEffect> DEATH_GAZE = MOB_EFFECTS.register("death_gaze",
      () -> new TCMobEffect(MobEffectCategory.HARMFUL, 6702131));
   public static final DeferredHolder<MobEffect, MobEffect> BLURRED_VISION = MOB_EFFECTS.register("blurred_vision",
      () -> new TCMobEffect(MobEffectCategory.HARMFUL, 8421504));
   public static final DeferredHolder<MobEffect, MobEffect> SUN_SCORNED = MOB_EFFECTS.register("sun_scorned", SunScornedEffect::new);
   public static final DeferredHolder<MobEffect, MobEffect> THAUMARHIA = MOB_EFFECTS.register("thaumarhia", ThaumarhiaEffect::new);

   // ------------------------------------------------------------------ sound
   /** SoundsTC.GORE. */
   public static final SoundType GORE = new DeferredSoundType(0.5F, 1.0F, ModSounds.GORE, ModSounds.GORE, ModSounds.GORE, ModSounds.GORE, ModSounds.GORE);

   // ------------------------------------------------------------------ fluids
   /** FluidFluxGoo: viscous (6000), dense (8); the goo itself is a finite block (BlockFluidFinite), see FluxGooBlock. */
   public static final DeferredHolder<FluidType, FluidType> FLUX_GOO_TYPE = FLUID_TYPES.register("flux_goo", () -> new FluidType(
      FluidType.Properties.create().descriptionId("block.thaumcraft.flux_goo").viscosity(6000).density(8).canSwim(false).canDrown(false)
         .motionScale(0.0007).sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL).sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY)));
   /** FluidDeath: viscosity 1500, rare, 4 quanta per block. */
   public static final DeferredHolder<FluidType, FluidType> LIQUID_DEATH_TYPE = FLUID_TYPES.register("liquid_death", () -> new FluidType(
      FluidType.Properties.create().descriptionId("block.thaumcraft.liquid_death").viscosity(1500).rarity(Rarity.RARE).canDrown(true)
         .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL).sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY)));
   /** FluidPure: water-like, luminosity 5, rare. */
   public static final DeferredHolder<FluidType, FluidType> PURIFYING_FLUID_TYPE = FLUID_TYPES.register("purifying_fluid", () -> new FluidType(
      FluidType.Properties.create().descriptionId("block.thaumcraft.purifying_fluid").lightLevel(5).rarity(Rarity.RARE).canDrown(true)
         .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL).sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY)));

   public static final DeferredHolder<Fluid, FluxGooFluid> FLUX_GOO_FLUID = FLUIDS.register("flux_goo", FluxGooFluid::new);
   public static final DeferredHolder<Fluid, FlowingFluid> LIQUID_DEATH = FLUIDS.register("liquid_death",
      () -> new BaseFlowingFluid.Source(deathProps()));
   public static final DeferredHolder<Fluid, FlowingFluid> FLOWING_LIQUID_DEATH = FLUIDS.register("flowing_liquid_death",
      () -> new BaseFlowingFluid.Flowing(deathProps()));
   public static final DeferredHolder<Fluid, FlowingFluid> PURIFYING_FLUID = FLUIDS.register("purifying_fluid",
      () -> new BaseFlowingFluid.Source(pureProps()));
   public static final DeferredHolder<Fluid, FlowingFluid> FLOWING_PURIFYING_FLUID = FLUIDS.register("flowing_purifying_fluid",
      () -> new BaseFlowingFluid.Flowing(pureProps()));

   private static BaseFlowingFluid.Properties deathProps() {
      // quantaPerBlock 4: two levels lost per block.
      return new BaseFlowingFluid.Properties(LIQUID_DEATH_TYPE, LIQUID_DEATH, FLOWING_LIQUID_DEATH).bucket(ModFlux.BUCKET_DEATH)
         .block(ModFlux.LIQUID_DEATH_BLOCK).levelDecreasePerBlock(2).tickRate(1500 / 200).slopeFindDistance(2);
   }

   private static BaseFlowingFluid.Properties pureProps() {
      return new BaseFlowingFluid.Properties(PURIFYING_FLUID_TYPE, PURIFYING_FLUID, FLOWING_PURIFYING_FLUID).bucket(ModFlux.BUCKET_PURE)
         .block(ModFlux.PURIFYING_FLUID_BLOCK);
   }

   // ------------------------------------------------------------------ blocks
   private static BlockBehaviour.Properties taint(float hardness, float resistance) {
      return BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(hardness, resistance).sound(GORE).randomTicks();
   }

   public static final DeferredBlock<FluxGooBlock> FLUX_GOO = BLOCKS.register("flux_goo", () -> new FluxGooBlock(
      BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).replaceable().noCollission().strength(100.0F).sound(GORE).randomTicks()
         .noLootTable().pushReaction(PushReaction.DESTROY).liquid()));
   public static final DeferredBlock<LiquidDeathBlock> LIQUID_DEATH_BLOCK = BLOCKS.register("liquid_death", () -> new LiquidDeathBlock(
      LIQUID_DEATH.get(), BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).replaceable().noCollission().strength(100.0F)
         .noLootTable().pushReaction(PushReaction.DESTROY).liquid()));
   public static final DeferredBlock<PurifyingFluidBlock> PURIFYING_FLUID_BLOCK = BLOCKS.register("purifying_fluid", () -> new PurifyingFluidBlock(
      PURIFYING_FLUID.get(), BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_GRAY).replaceable().noCollission().strength(100.0F)
         .lightLevel(s -> 5).noLootTable().pushReaction(PushReaction.DESTROY).liquid()));

   public static final DeferredBlock<TaintFibreBlock> TAINT_FIBRE = withItem("taint_fibre", TaintFibreBlock::new,
      taint(1.0F, 1.0F).noOcclusion().replaceable().ignitedByLava().pushReaction(PushReaction.DESTROY)
         .lightLevel(s -> s.getValue(TaintFibreBlock.GROWTH3) ? 12 : s.getValue(TaintFibreBlock.GROWTH2) || s.getValue(TaintFibreBlock.GROWTH4) ? 6 : 0));
   public static final DeferredBlock<TaintBlock> TAINT_CRUST = withItem("taint_crust", TaintBlock::new, taint(10.0F, 100.0F));
   public static final DeferredBlock<TaintBlock> TAINT_SOIL = withItem("taint_soil", TaintBlock::new, taint(10.0F, 100.0F));
   public static final DeferredBlock<TaintBlock> TAINT_ROCK = withItem("taint_rock", TaintBlock::new, taint(10.0F, 100.0F));
   public static final DeferredBlock<TaintBlock> TAINT_GEYSER = withItem("taint_geyser", TaintBlock::new, taint(10.0F, 100.0F));
   public static final DeferredBlock<TaintFeatureBlock> TAINT_FEATURE = withItem("taint_feature", TaintFeatureBlock::new,
      taint(0.1F, 0.1F).noOcclusion().lightLevel(s -> 9).pushReaction(PushReaction.DESTROY));
   public static final DeferredBlock<TaintLogBlock> TAINT_LOG = withItem("taint_log", TaintLogBlock::new,
      taint(3.0F, 100.0F).ignitedByLava());

   private static BlockBehaviour.Properties device() {
      return BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(1.5F, 10.0F).sound(SoundType.METAL).noOcclusion();
   }

   public static final DeferredBlock<CondenserBlock> CONDENSER = withItem("condenser", CondenserBlock::new, device());
   public static final DeferredBlock<CondenserLatticeBlock> CONDENSER_LATTICE = withItem("condenser_lattice", p -> new CondenserLatticeBlock(p, false),
      BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(0.5F, 5.0F).sound(SoundType.METAL).noOcclusion().lightLevel(s -> 5));
   public static final DeferredBlock<CondenserLatticeBlock> CONDENSER_LATTICE_DIRTY = withItem("condenser_lattice_dirty",
      p -> new CondenserLatticeBlock(p, true), BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(0.5F, 5.0F).sound(SoundType.METAL).noOcclusion());
   public static final DeferredBlock<VoidSiphonBlock> VOID_SIPHON = withItem("void_siphon", VoidSiphonBlock::new, device());

   private static <B extends Block> DeferredBlock<B> withItem(String name, Function<BlockBehaviour.Properties, ? extends B> f, BlockBehaviour.Properties p) {
      DeferredBlock<B> b = BLOCKS.registerBlock(name, f, p);
      ITEMS.registerSimpleBlockItem(b);
      return b;
   }

   // ------------------------------------------------------------------ block entities
   public static final Supplier<BlockEntityType<CondenserBlockEntity>> CONDENSER_BE = ModBlockEntities.BLOCK_ENTITIES.register("condenser",
      () -> BlockEntityType.Builder.of(CondenserBlockEntity::new, CONDENSER.get()).build(null));
   public static final Supplier<BlockEntityType<VoidSiphonBlockEntity>> VOID_SIPHON_BE = ModBlockEntities.BLOCK_ENTITIES.register("void_siphon",
      () -> BlockEntityType.Builder.of(VoidSiphonBlockEntity::new, VOID_SIPHON.get()).build(null));

   // ------------------------------------------------------------------ menus
   public static final Supplier<MenuType<VoidSiphonMenu>> VOID_SIPHON_MENU = ModMenus.MENUS.register("void_siphon",
      () -> IMenuTypeExtension.create(VoidSiphonMenu::new));

   // ------------------------------------------------------------------ items
   public static final DeferredItem<BucketItem> BUCKET_DEATH = ITEMS.register("bucket_death",
      () -> new BucketItem(LIQUID_DEATH.get(), new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1).rarity(Rarity.RARE)));
   public static final DeferredItem<BucketItem> BUCKET_PURE = ITEMS.register("bucket_pure",
      () -> new BucketItem(PURIFYING_FLUID.get(), new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1).rarity(Rarity.RARE)));
   public static final DeferredItem<FluxGooBucketItem> BUCKET_GOO = ITEMS.registerItem("bucket_goo", FluxGooBucketItem::new,
      new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1));
   public static final DeferredItem<BottleTaintItem> BOTTLE_TAINT = ITEMS.registerItem("bottle_taint", BottleTaintItem::new,
      new Item.Properties().stacksTo(8));
   public static final DeferredItem<CausalityCollapserItem> CAUSALITY_COLLAPSER = ITEMS.registerItem("causality_collapser",
      CausalityCollapserItem::new, new Item.Properties().stacksTo(16));
   public static final DeferredItem<SanitySoapItem> SANITY_SOAP = ITEMS.registerItem("sanity_soap", SanitySoapItem::new);
   public static final DeferredItem<BathSaltsItem> BATH_SALTS = ITEMS.registerItem("bath_salts", BathSaltsItem::new);

   // ------------------------------------------------------------------ entities
   private static <T extends net.minecraft.world.entity.Entity> Supplier<EntityType<T>> entity(String name, EntityType.Builder<T> b) {
      return ModEntities.ENTITIES.register(name, () -> b.build(name));
   }

   public static final Supplier<EntityType<FluxRiftEntity>> FLUX_RIFT = entity("flux_rift",
      EntityType.Builder.<FluxRiftEntity>of(FluxRiftEntity::new, MobCategory.MISC).sized(2.0F, 2.0F).fireImmune().clientTrackingRange(4).updateInterval(20));
   public static final Supplier<EntityType<FallingTaintEntity>> FALLING_TAINT = entity("falling_taint",
      EntityType.Builder.<FallingTaintEntity>of(FallingTaintEntity::new, MobCategory.MISC).sized(0.98F, 0.98F).clientTrackingRange(4).updateInterval(3));
   public static final Supplier<EntityType<BottleTaintEntity>> BOTTLE_TAINT_ENTITY = entity("bottle_taint",
      EntityType.Builder.<BottleTaintEntity>of(BottleTaintEntity::new, MobCategory.MISC).sized(0.25F, 0.25F).clientTrackingRange(4).updateInterval(10));
   public static final Supplier<EntityType<CausalityCollapserEntity>> CAUSALITY_COLLAPSER_ENTITY = entity("causality_collapser",
      EntityType.Builder.<CausalityCollapserEntity>of(CausalityCollapserEntity::new, MobCategory.MISC).sized(0.25F, 0.25F).clientTrackingRange(4)
         .updateInterval(10));
   public static final Supplier<EntityType<TaintCrawlerEntity>> TAINT_CRAWLER = entity("taint_crawler",
      EntityType.Builder.of(TaintCrawlerEntity::new, MobCategory.MONSTER).sized(0.5F, 0.4F).eyeHeight(0.1F).clientTrackingRange(8));
   public static final Supplier<EntityType<TaintacleEntity>> TAINTACLE = entity("taintacle",
      EntityType.Builder.of(TaintacleEntity::new, MobCategory.MONSTER).sized(0.8F, 3.0F).clientTrackingRange(8));
   public static final Supplier<EntityType<TaintacleSmallEntity>> TAINTACLE_TINY = entity("taintacle_tiny",
      EntityType.Builder.of(TaintacleSmallEntity::new, MobCategory.MONSTER).sized(0.22F, 1.0F).clientTrackingRange(8));
   public static final Supplier<EntityType<TaintSeedEntity>> TAINT_SEED = entity("taint_seed",
      EntityType.Builder.of(TaintSeedEntity::new, MobCategory.MONSTER).sized(1.5F, 1.25F).clientTrackingRange(8));
   public static final Supplier<EntityType<TaintSeedPrimeEntity>> TAINT_SEED_PRIME = entity("taint_seed_prime",
      EntityType.Builder.of(TaintSeedPrimeEntity::new, MobCategory.MONSTER).sized(2.0F, 2.0F).clientTrackingRange(8));
   public static final Supplier<EntityType<TaintSwarmEntity>> TAINT_SWARM = entity("taint_swarm",
      EntityType.Builder.of(TaintSwarmEntity::new, MobCategory.MONSTER).sized(2.0F, 2.0F).clientTrackingRange(8));

   // ------------------------------------------------------------------ spawn eggs (ConfigEntities egg colours)
   public static final DeferredItem<DeferredSpawnEggItem> TAINT_CRAWLER_EGG = egg("taint_crawler", TAINT_CRAWLER, 10618530, 3158064);
   public static final DeferredItem<DeferredSpawnEggItem> TAINTACLE_EGG = egg("taintacle", TAINTACLE, 10618530, 4469572);
   public static final DeferredItem<DeferredSpawnEggItem> TAINT_SWARM_EGG = egg("taint_swarm", TAINT_SWARM, 10618530, 16744576);
   public static final DeferredItem<DeferredSpawnEggItem> TAINT_SEED_EGG = egg("taint_seed", TAINT_SEED, 10618530, 4465237);
   public static final DeferredItem<DeferredSpawnEggItem> TAINT_SEED_PRIME_EGG = egg("taint_seed_prime", TAINT_SEED_PRIME, 10618530, 5583718);

   private static DeferredItem<DeferredSpawnEggItem> egg(String name, Supplier<? extends EntityType<? extends net.minecraft.world.entity.Mob>> type,
      int bg, int fg) {
      return ITEMS.register(name + "_spawn_egg", () -> new DeferredSpawnEggItem(type, bg, fg, new Item.Properties()));
   }

   private ModFlux() {
   }

   /** Called from the Thaumcraft constructor: loads the holders above and wires the registers and mod bus listeners. */
   public static void init(IEventBus modBus) {
      MOB_EFFECTS.register(modBus);
      FLUID_TYPES.register(modBus);
      FLUIDS.register(modBus);
      modBus.addListener(ModFlux::attributes);
      modBus.addListener(ModFlux::spawnPlacements);
      modBus.addListener(ModFlux::capabilities);
      modBus.addListener((FMLCommonSetupEvent e) -> e.enqueueWork(() -> {
         FireBlock fire = (FireBlock) Blocks.FIRE;
         fire.setFlammable(TAINT_FIBRE.get(), 3, 3);
         fire.setFlammable(TAINT_LOG.get(), 4, 4);
      }));
   }

   private static void attributes(EntityAttributeCreationEvent event) {
      event.put(TAINT_CRAWLER.get(), TaintCrawlerEntity.createAttributes().build());
      event.put(TAINTACLE.get(), TaintacleEntity.createAttributes().build());
      event.put(TAINTACLE_TINY.get(), TaintacleSmallEntity.createSmallAttributes().build());
      event.put(TAINT_SEED.get(), TaintSeedEntity.createAttributes().build());
      event.put(TAINT_SEED_PRIME.get(), TaintSeedPrimeEntity.createPrimeAttributes().build());
      event.put(TAINT_SWARM.get(), TaintSwarmEntity.createAttributes().build());
   }

   private static void spawnPlacements(RegisterSpawnPlacementsEvent event) {
      // None of them spawn naturally (taint seeds come from taint spread, the rest from seeds and geysers); the
      // placements only keep spawn eggs / summon commands sane.
      event.register(TAINT_CRAWLER.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
         (t, l, r, p, rand) -> true, RegisterSpawnPlacementsEvent.Operation.REPLACE);
   }

   private static void capabilities(RegisterCapabilitiesEvent event) {
      event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, VOID_SIPHON_BE.get(), (be, side) -> be.itemHandler());
   }

   /** The horizontal faces, in EnumFacing.HORIZONTALS order (S, W, N, E). */
   public static final Direction[] HORIZONTALS = {Direction.SOUTH, Direction.WEST, Direction.NORTH, Direction.EAST};
}
