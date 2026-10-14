package thaumcraft.common.registers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.common.util.DeferredSoundType;
import net.neoforged.neoforge.items.wrapper.SidedInvWrapper;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.blocks.DeviceBlock;
import thaumcraft.common.blocks.devices.ArcaneEarBlock;
import thaumcraft.common.blocks.devices.ArcaneEarBlockEntity;
import thaumcraft.common.blocks.devices.ArcaneLampBlockEntity;
import thaumcraft.common.blocks.devices.BannerBlock;
import thaumcraft.common.blocks.devices.BannerBlockEntity;
import thaumcraft.common.blocks.devices.BarrierBlock;
import thaumcraft.common.blocks.devices.BarrierStoneBlockEntity;
import thaumcraft.common.blocks.devices.BrainBoxBlock;
import thaumcraft.common.blocks.devices.DioptraBlock;
import thaumcraft.common.blocks.devices.DioptraBlockEntity;
import thaumcraft.common.blocks.devices.EssentiaMirrorBlockEntity;
import thaumcraft.common.blocks.devices.EverfullUrnBlock;
import thaumcraft.common.blocks.devices.EverfullUrnBlockEntity;
import thaumcraft.common.blocks.devices.FertilityLampBlockEntity;
import thaumcraft.common.blocks.devices.GrowthLampBlockEntity;
import thaumcraft.common.blocks.devices.HungryChestBlock;
import thaumcraft.common.blocks.devices.HungryChestBlockEntity;
import thaumcraft.common.blocks.devices.InfernalFurnaceBlock;
import thaumcraft.common.blocks.devices.InfernalFurnaceBlockEntity;
import thaumcraft.common.blocks.devices.JarBrainBlock;
import thaumcraft.common.blocks.devices.JarBrainBlockEntity;
import thaumcraft.common.blocks.devices.LampBlock;
import thaumcraft.common.blocks.devices.LevitatorBlock;
import thaumcraft.common.blocks.devices.LevitatorBlockEntity;
import thaumcraft.common.blocks.devices.LootBlock;
import thaumcraft.common.blocks.devices.MirrorBlock;
import thaumcraft.common.blocks.devices.MirrorBlockEntity;
import thaumcraft.common.blocks.devices.PatternCrafterBlock;
import thaumcraft.common.blocks.devices.PatternCrafterBlockEntity;
import thaumcraft.common.blocks.devices.PavingStoneBlock;
import thaumcraft.common.blocks.devices.PlaceholderBlock;
import thaumcraft.common.blocks.devices.PotionSprayerBlock;
import thaumcraft.common.blocks.devices.PotionSprayerBlockEntity;
import thaumcraft.common.blocks.devices.RechargePedestalBlock;
import thaumcraft.common.blocks.devices.RechargePedestalBlockEntity;
import thaumcraft.common.blocks.devices.RedstoneRelayBlock;
import thaumcraft.common.blocks.devices.RedstoneRelayBlockEntity;
import thaumcraft.common.blocks.devices.SpaBlock;
import thaumcraft.common.blocks.devices.SpaBlockEntity;
import thaumcraft.common.blocks.devices.ThaumatoriumBlock;
import thaumcraft.common.blocks.devices.ThaumatoriumBlockEntity;
import thaumcraft.common.blocks.devices.ThaumatoriumTopBlockEntity;
import thaumcraft.common.blocks.devices.VisBatteryBlock;
import thaumcraft.common.blocks.devices.VisGeneratorBlock;
import thaumcraft.common.blocks.devices.VisGeneratorBlockEntity;
import thaumcraft.common.items.BannerItem;
import thaumcraft.common.items.HandMirrorItem;
import thaumcraft.common.items.ICreativeVariants;
import thaumcraft.common.items.JarBrainItem;
import thaumcraft.common.items.MirrorItem;
import thaumcraft.common.menu.HandMirrorMenu;
import thaumcraft.common.menu.PotionSprayerMenu;
import thaumcraft.common.menu.SpaMenu;
import thaumcraft.common.menu.ThaumatoriumMenu;

/**
 * Devices milestone: lamps, levitator, hungry chest, arcane ears, redstone relay, everfull urn, paving stones, vis
 * battery, recharge pedestal, dioptra, vis generator, arcane spa, infernal furnace, thaumatorium, potion sprayer,
 * pattern crafter, mirrors, brain in a jar, mnemonic matrix, flesh, loot crates/urns and banners. Everything registers
 * into the shared DeferredRegisters; {@link #init()} is called from the Thaumcraft constructor.
 */
public final class ModDevices {
   private ModDevices() {
   }

   public static void init(IEventBus modBus) {
      modBus.addListener(ModDevices::registerCapabilities);
   }

   // ------------------------------------------------------------------ data components

   /** Where a mirror (or hand mirror) item is linked to ("linkX/Y/Z/Dim" in 1.12). */
   public static final Supplier<DataComponentType<GlobalPos>> MIRROR_LINK = ModDataComponents.COMPONENTS.registerComponentType(
      "mirror_link", b -> b.persistent(GlobalPos.CODEC).networkSynchronized(GlobalPos.STREAM_CODEC));
   /** The experience stored in a brain in a jar ("xp"). */
   public static final Supplier<DataComponentType<Integer>> BRAIN_XP = ModDataComponents.COMPONENTS.registerComponentType(
      "brain_xp", b -> b.persistent(com.mojang.serialization.Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));
   /** The aspect painted on a banner ("aspect"). */
   public static final Supplier<DataComponentType<Aspect>> BANNER_ASPECT = ModDataComponents.COMPONENTS.registerComponentType(
      "banner_aspect", b -> b.persistent(Aspect.CODEC).networkSynchronized(Aspect.STREAM_CODEC));

   public static final SoundType GORE_SOUND = new DeferredSoundType(1.0F, 1.0F, ModSounds.GORE, ModSounds.GORE, ModSounds.GORE, ModSounds.GORE,
      ModSounds.GORE);

   // ------------------------------------------------------------------ blocks

   private static final List<DeferredItem<? extends Item>> ITEMS = new ArrayList<>();

   private static <B extends Block> DeferredBlock<B> block(String name, Function<BlockBehaviour.Properties, ? extends B> f, BlockBehaviour.Properties p) {
      DeferredBlock<B> b = ModBlocks.BLOCKS.registerBlock(name, f, p);
      ITEMS.add(ModItems.ITEMS.register(name, () -> new BlockItem(b.get(), new Item.Properties())));
      return b;
   }

   private static <B extends Block> DeferredBlock<B> blockNoItem(String name, Function<BlockBehaviour.Properties, ? extends B> f, BlockBehaviour.Properties p) {
      return ModBlocks.BLOCKS.registerBlock(name, f, p);
   }

   private static BlockBehaviour.Properties props(MapColor color, float hardness, float resistance, SoundType sound) {
      return BlockBehaviour.Properties.of().mapColor(color).strength(hardness, resistance).sound(sound);
   }

   private static BlockBehaviour.Properties lamp() {
      return props(MapColor.METAL, 1.0F, 2.0F, SoundType.METAL).noOcclusion()
         .lightLevel(s -> s.hasProperty(DeviceBlock.ENABLED) && s.getValue(DeviceBlock.ENABLED) ? 15 : 0);
   }

   public static final DeferredBlock<LampBlock> LAMP_ARCANE = block("lamp_arcane", p -> new LampBlock(LampBlock.Kind.ARCANE, p), lamp());
   public static final DeferredBlock<LampBlock> LAMP_GROWTH = block("lamp_growth", p -> new LampBlock(LampBlock.Kind.GROWTH, p), lamp());
   public static final DeferredBlock<LampBlock> LAMP_FERTILITY = block("lamp_fertility", p -> new LampBlock(LampBlock.Kind.FERTILITY, p), lamp());
   public static final DeferredBlock<LevitatorBlock> LEVITATOR = block("levitator", LevitatorBlock::new,
      props(MapColor.WOOD, 1.5F, 2.0F, SoundType.WOOD).noOcclusion());
   public static final DeferredBlock<HungryChestBlock> HUNGRY_CHEST = block("hungry_chest", HungryChestBlock::new,
      props(MapColor.WOOD, 2.5F, 2.5F, SoundType.WOOD).noOcclusion().ignitedByLava());
   public static final DeferredBlock<ArcaneEarBlock> ARCANE_EAR = block("arcane_ear", p -> new ArcaneEarBlock(false, p),
      props(MapColor.WOOD, 1.0F, 2.0F, SoundType.WOOD).noOcclusion().pushReaction(PushReaction.DESTROY));
   public static final DeferredBlock<ArcaneEarBlock> ARCANE_EAR_TOGGLE = block("arcane_ear_toggle", p -> new ArcaneEarBlock(true, p),
      props(MapColor.WOOD, 1.0F, 2.0F, SoundType.WOOD).noOcclusion().pushReaction(PushReaction.DESTROY));
   public static final DeferredBlock<RedstoneRelayBlock> REDSTONE_RELAY = block("redstone_relay", RedstoneRelayBlock::new,
      BlockBehaviour.Properties.of().instabreak().sound(SoundType.WOOD).noOcclusion().pushReaction(PushReaction.DESTROY));
   public static final DeferredBlock<EverfullUrnBlock> EVERFULL_URN = block("everfull_urn", EverfullUrnBlock::new,
      props(MapColor.STONE, 1.5F, 2.0F, SoundType.STONE).noOcclusion());
   public static final DeferredBlock<PavingStoneBlock> PAVING_STONE_TRAVEL = block("paving_stone_travel", p -> new PavingStoneBlock(false, p),
      props(MapColor.STONE, 2.5F, 2.0F, SoundType.STONE).randomTicks());
   public static final DeferredBlock<PavingStoneBlock> PAVING_STONE_BARRIER = block("paving_stone_barrier", p -> new PavingStoneBlock(true, p),
      props(MapColor.STONE, 2.5F, 2.0F, SoundType.STONE).randomTicks());
   public static final DeferredBlock<BarrierBlock> BARRIER = blockNoItem("barrier", BarrierBlock::new,
      BlockBehaviour.Properties.of().replaceable().noOcclusion().noLootTable().dynamicShape().strength(-1.0F, 0.0F)
         .pushReaction(PushReaction.DESTROY).isValidSpawn((s, l, p, e) -> false).isViewBlocking((s, l, p) -> false)
         .isSuffocating((s, l, p) -> false).isRedstoneConductor((s, l, p) -> false));
   public static final DeferredBlock<VisBatteryBlock> VIS_BATTERY = block("vis_battery", VisBatteryBlock::new,
      props(MapColor.STONE, 0.5F, 2.0F, SoundType.STONE).randomTicks().lightLevel(s -> s.getValue(VisBatteryBlock.CHARGE)));
   public static final DeferredBlock<RechargePedestalBlock> RECHARGE_PEDESTAL = block("recharge_pedestal", RechargePedestalBlock::new,
      props(MapColor.STONE, 1.5F, 2.0F, SoundType.STONE).noOcclusion());
   public static final DeferredBlock<DioptraBlock> DIOPTRA = block("dioptra", DioptraBlock::new,
      props(MapColor.STONE, 1.5F, 2.0F, SoundType.STONE).noOcclusion());
   public static final DeferredBlock<VisGeneratorBlock> VIS_GENERATOR = block("vis_generator", VisGeneratorBlock::new,
      props(MapColor.WOOD, 1.5F, 2.0F, SoundType.WOOD).noOcclusion());
   public static final DeferredBlock<SpaBlock> SPA = block("spa", SpaBlock::new, props(MapColor.STONE, 1.5F, 2.0F, SoundType.STONE));
   public static final DeferredBlock<InfernalFurnaceBlock> INFERNAL_FURNACE = blockNoItem("infernal_furnace", InfernalFurnaceBlock::new,
      props(MapColor.NETHER, 1.5F, 2.0F, SoundType.STONE).noOcclusion().lightLevel(s -> 13).noLootTable());
   /** The furnace's item: only the "crafted" result of its salis mundus structure (not in the creative tab). */
   public static final DeferredItem<Item> INFERNAL_FURNACE_ITEM = ModItems.ITEMS.register("infernal_furnace", () -> new HiddenBlockItem(INFERNAL_FURNACE.get()));
   public static final DeferredBlock<PlaceholderBlock> PLACEHOLDER_BRICK = blockNoItem("placeholder_brick", p -> new PlaceholderBlock(false, p),
      props(MapColor.NETHER, 2.5F, 2.0F, SoundType.STONE).noOcclusion().pushReaction(PushReaction.BLOCK));
   public static final DeferredBlock<PlaceholderBlock> PLACEHOLDER_OBSIDIAN = blockNoItem("placeholder_obsidian", p -> new PlaceholderBlock(true, p),
      props(MapColor.COLOR_BLACK, 2.5F, 2.0F, SoundType.STONE).noOcclusion().pushReaction(PushReaction.BLOCK));
   public static final DeferredBlock<Block> METAL_ALCHEMICAL = block("metal_alchemical", Block::new,
      props(MapColor.METAL, 4.0F, 10.0F, SoundType.METAL).requiresCorrectToolForDrops());
   public static final DeferredBlock<Block> METAL_ALCHEMICAL_ADVANCED = block("metal_alchemical_advanced", Block::new,
      props(MapColor.METAL, 4.0F, 10.0F, SoundType.METAL).requiresCorrectToolForDrops());
   public static final DeferredBlock<ThaumatoriumBlock> THAUMATORIUM = blockNoItem("thaumatorium", p -> new ThaumatoriumBlock(false, p),
      props(MapColor.METAL, 1.5F, 2.0F, SoundType.METAL).noOcclusion());
   /** BlockThaumatorium's item block: the THAUMATORIUM research icon and its multiblock's display (not in the creative tab). */
   public static final DeferredItem<Item> THAUMATORIUM_ITEM = ModItems.ITEMS.register("thaumatorium", () -> new HiddenBlockItem(THAUMATORIUM.get()));
   public static final DeferredBlock<ThaumatoriumBlock> THAUMATORIUM_TOP = blockNoItem("thaumatorium_top", p -> new ThaumatoriumBlock(true, p),
      props(MapColor.METAL, 1.5F, 2.0F, SoundType.METAL).noOcclusion());
   public static final DeferredBlock<BrainBoxBlock> BRAIN_BOX = block("brain_box", BrainBoxBlock::new,
      props(MapColor.METAL, 1.0F, 10.0F, SoundType.METAL).noOcclusion());
   public static final DeferredBlock<PotionSprayerBlock> POTION_SPRAYER = block("potion_sprayer", PotionSprayerBlock::new,
      props(MapColor.METAL, 1.5F, 2.0F, SoundType.METAL));
   public static final DeferredBlock<PatternCrafterBlock> PATTERN_CRAFTER = block("pattern_crafter", PatternCrafterBlock::new,
      props(MapColor.METAL, 1.5F, 2.0F, SoundType.METAL).noOcclusion());
   public static final DeferredBlock<MirrorBlock> MIRROR = blockNoItem("mirror", p -> new MirrorBlock(false, p),
      props(MapColor.METAL, 0.1F, 2.0F, ModSounds.JAR_SOUND).noOcclusion());
   public static final DeferredBlock<MirrorBlock> MIRROR_ESSENTIA = blockNoItem("mirror_essentia", p -> new MirrorBlock(true, p),
      props(MapColor.METAL, 0.1F, 2.0F, ModSounds.JAR_SOUND).noOcclusion());
   public static final DeferredItem<MirrorItem> MIRROR_ITEM = ModItems.ITEMS.register("mirror",
      () -> new MirrorItem(MIRROR.get(), new Item.Properties().rarity(Rarity.UNCOMMON)));
   public static final DeferredItem<MirrorItem> MIRROR_ESSENTIA_ITEM = ModItems.ITEMS.register("mirror_essentia",
      () -> new MirrorItem(MIRROR_ESSENTIA.get(), new Item.Properties().rarity(Rarity.UNCOMMON)));
   public static final DeferredBlock<JarBrainBlock> JAR_BRAIN = blockNoItem("jar_brain", JarBrainBlock::new,
      BlockBehaviour.Properties.of().mapColor(MapColor.NONE).strength(0.3F).sound(ModSounds.JAR_SOUND).noOcclusion()
         .isValidSpawn((s, l, p, e) -> false).isRedstoneConductor((s, l, p) -> false).isSuffocating((s, l, p) -> false)
         .isViewBlocking((s, l, p) -> false));
   public static final DeferredItem<JarBrainItem> JAR_BRAIN_ITEM = ModItems.ITEMS.register("jar_brain",
      () -> new JarBrainItem(JAR_BRAIN.get(), new Item.Properties()));
   public static final DeferredBlock<Block> FLESH_BLOCK = block("flesh_block", Block::new, props(MapColor.COLOR_RED, 0.25F, 2.0F, GORE_SOUND));
   public static final DeferredBlock<LootBlock> LOOT_CRATE_COMMON = block("loot_crate_common", p -> new LootBlock(false, p), crate());
   public static final DeferredBlock<LootBlock> LOOT_CRATE_UNCOMMON = block("loot_crate_uncommon", p -> new LootBlock(false, p), crate());
   public static final DeferredBlock<LootBlock> LOOT_CRATE_RARE = block("loot_crate_rare", p -> new LootBlock(false, p), crate());
   public static final DeferredBlock<LootBlock> LOOT_URN_COMMON = block("loot_urn_common", p -> new LootBlock(true, p), urn());
   public static final DeferredBlock<LootBlock> LOOT_URN_UNCOMMON = block("loot_urn_uncommon", p -> new LootBlock(true, p), urn());
   public static final DeferredBlock<LootBlock> LOOT_URN_RARE = block("loot_urn_rare", p -> new LootBlock(true, p), urn());

   private static BlockBehaviour.Properties crate() {
      return props(MapColor.WOOD, 0.15F, 0.0F, SoundType.WOOD).noOcclusion().ignitedByLava();
   }

   private static BlockBehaviour.Properties urn() {
      return props(MapColor.STONE, 0.15F, 0.0F, ModSounds.URN_SOUND).noOcclusion();
   }

   public static final Map<DyeColor, DeferredBlock<BannerBlock>> BANNERS = banners();
   public static final DeferredBlock<BannerBlock> BANNER_CRIMSON_CULT = banner("banner_crimson_cult", null);

   private static Map<DyeColor, DeferredBlock<BannerBlock>> banners() {
      Map<DyeColor, DeferredBlock<BannerBlock>> map = new EnumMap<>(DyeColor.class);
      for (DyeColor dye : DyeColor.values()) map.put(dye, banner("banner_" + dye.getSerializedName(), dye));
      return Collections.unmodifiableMap(map);
   }

   private static DeferredBlock<BannerBlock> banner(String name, DyeColor dye) {
      DeferredBlock<BannerBlock> b = ModBlocks.BLOCKS.registerBlock(name, p -> new BannerBlock(dye, p),
         props(dye == null ? MapColor.COLOR_RED : dye.getMapColor(), 1.0F, 2.0F, SoundType.WOOD).noCollission().noOcclusion().ignitedByLava());
      ITEMS.add(ModItems.ITEMS.register(name, () -> new BannerItem(b.get(), new Item.Properties().stacksTo(16))));
      return b;
   }

   public static List<BannerBlock> allBanners() {
      List<BannerBlock> out = new ArrayList<>();
      BANNERS.values().forEach(b -> out.add(b.get()));
      out.add(BANNER_CRIMSON_CULT.get());
      return out;
   }

   // ------------------------------------------------------------------ items

   public static final DeferredItem<HandMirrorItem> HAND_MIRROR = ModItems.ITEMS.registerItem("hand_mirror", HandMirrorItem::new,
      new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));
   /** STUB: the zombie brain (dropped by brainy zombies, a later milestone); here only as the jar of brain's ingredient. */
   public static final DeferredItem<Item> BRAIN = ModItems.ITEMS.registerSimpleItem("brain",
      new Item.Properties().food(new FoodProperties.Builder().nutrition(4).saturationModifier(0.2F).alwaysEdible().build()));

   // ------------------------------------------------------------------ block entities

   private static <T extends net.minecraft.world.level.block.entity.BlockEntity> Supplier<BlockEntityType<T>> be(String name,
      BlockEntityType.BlockEntitySupplier<T> f, Supplier<List<Block>> blocks) {
      return ModBlockEntities.BLOCK_ENTITIES.register(name, () -> BlockEntityType.Builder.of(f, blocks.get().toArray(Block[]::new)).build(null));
   }

   public static final Supplier<BlockEntityType<ArcaneLampBlockEntity>> LAMP_ARCANE_BE = be("lamp_arcane", ArcaneLampBlockEntity::new,
      () -> List.of(LAMP_ARCANE.get()));
   public static final Supplier<BlockEntityType<GrowthLampBlockEntity>> LAMP_GROWTH_BE = be("lamp_growth", GrowthLampBlockEntity::new,
      () -> List.of(LAMP_GROWTH.get()));
   public static final Supplier<BlockEntityType<FertilityLampBlockEntity>> LAMP_FERTILITY_BE = be("lamp_fertility", FertilityLampBlockEntity::new,
      () -> List.of(LAMP_FERTILITY.get()));
   public static final Supplier<BlockEntityType<LevitatorBlockEntity>> LEVITATOR_BE = be("levitator", LevitatorBlockEntity::new,
      () -> List.of(LEVITATOR.get()));
   public static final Supplier<BlockEntityType<HungryChestBlockEntity>> HUNGRY_CHEST_BE = be("hungry_chest", HungryChestBlockEntity::new,
      () -> List.of(HUNGRY_CHEST.get()));
   public static final Supplier<BlockEntityType<ArcaneEarBlockEntity>> ARCANE_EAR_BE = be("arcane_ear", ArcaneEarBlockEntity::new,
      () -> List.of(ARCANE_EAR.get(), ARCANE_EAR_TOGGLE.get()));
   public static final Supplier<BlockEntityType<RedstoneRelayBlockEntity>> REDSTONE_RELAY_BE = be("redstone_relay", RedstoneRelayBlockEntity::new,
      () -> List.of(REDSTONE_RELAY.get()));
   public static final Supplier<BlockEntityType<EverfullUrnBlockEntity>> EVERFULL_URN_BE = be("everfull_urn", EverfullUrnBlockEntity::new,
      () -> List.of(EVERFULL_URN.get()));
   public static final Supplier<BlockEntityType<BarrierStoneBlockEntity>> BARRIER_STONE_BE = be("barrier_stone", BarrierStoneBlockEntity::new,
      () -> List.of(PAVING_STONE_BARRIER.get()));
   public static final Supplier<BlockEntityType<RechargePedestalBlockEntity>> RECHARGE_PEDESTAL_BE = be("recharge_pedestal",
      RechargePedestalBlockEntity::new, () -> List.of(RECHARGE_PEDESTAL.get()));
   public static final Supplier<BlockEntityType<DioptraBlockEntity>> DIOPTRA_BE = be("dioptra", DioptraBlockEntity::new, () -> List.of(DIOPTRA.get()));
   public static final Supplier<BlockEntityType<VisGeneratorBlockEntity>> VIS_GENERATOR_BE = be("vis_generator", VisGeneratorBlockEntity::new,
      () -> List.of(VIS_GENERATOR.get()));
   public static final Supplier<BlockEntityType<SpaBlockEntity>> SPA_BE = be("spa", SpaBlockEntity::new, () -> List.of(SPA.get()));
   public static final Supplier<BlockEntityType<InfernalFurnaceBlockEntity>> INFERNAL_FURNACE_BE = be("infernal_furnace",
      InfernalFurnaceBlockEntity::new, () -> List.of(INFERNAL_FURNACE.get()));
   public static final Supplier<BlockEntityType<ThaumatoriumBlockEntity>> THAUMATORIUM_BE = be("thaumatorium", ThaumatoriumBlockEntity::new,
      () -> List.of(THAUMATORIUM.get()));
   public static final Supplier<BlockEntityType<ThaumatoriumTopBlockEntity>> THAUMATORIUM_TOP_BE = be("thaumatorium_top",
      ThaumatoriumTopBlockEntity::new, () -> List.of(THAUMATORIUM_TOP.get()));
   public static final Supplier<BlockEntityType<PotionSprayerBlockEntity>> POTION_SPRAYER_BE = be("potion_sprayer", PotionSprayerBlockEntity::new,
      () -> List.of(POTION_SPRAYER.get()));
   public static final Supplier<BlockEntityType<PatternCrafterBlockEntity>> PATTERN_CRAFTER_BE = be("pattern_crafter",
      PatternCrafterBlockEntity::new, () -> List.of(PATTERN_CRAFTER.get()));
   public static final Supplier<BlockEntityType<MirrorBlockEntity>> MIRROR_BE = be("mirror", MirrorBlockEntity::new, () -> List.of(MIRROR.get()));
   public static final Supplier<BlockEntityType<EssentiaMirrorBlockEntity>> MIRROR_ESSENTIA_BE = be("mirror_essentia", EssentiaMirrorBlockEntity::new,
      () -> List.of(MIRROR_ESSENTIA.get()));
   public static final Supplier<BlockEntityType<JarBrainBlockEntity>> JAR_BRAIN_BE = be("jar_brain", JarBrainBlockEntity::new,
      () -> List.of(JAR_BRAIN.get()));
   public static final Supplier<BlockEntityType<BannerBlockEntity>> BANNER_BE = be("banner", BannerBlockEntity::new,
      () -> new ArrayList<>(allBanners()));

   // ------------------------------------------------------------------ menus

   public static final Supplier<MenuType<SpaMenu>> SPA_MENU = ModMenus.MENUS.register("spa", () -> IMenuTypeExtension.create(SpaMenu::new));
   public static final Supplier<MenuType<ThaumatoriumMenu>> THAUMATORIUM_MENU = ModMenus.MENUS.register("thaumatorium",
      () -> IMenuTypeExtension.create(ThaumatoriumMenu::new));
   public static final Supplier<MenuType<PotionSprayerMenu>> POTION_SPRAYER_MENU = ModMenus.MENUS.register("potion_sprayer",
      () -> IMenuTypeExtension.create(PotionSprayerMenu::new));
   public static final Supplier<MenuType<HandMirrorMenu>> HAND_MIRROR_MENU = ModMenus.MENUS.register("hand_mirror",
      () -> IMenuTypeExtension.create(HandMirrorMenu::new));

   // ------------------------------------------------------------------ capabilities

   private static void registerCapabilities(RegisterCapabilitiesEvent e) {
      e.registerBlockEntity(Capabilities.FluidHandler.BLOCK, EVERFULL_URN_BE.get(), (be, side) -> side == net.minecraft.core.Direction.UP ? be.tank : null);
      e.registerBlockEntity(Capabilities.FluidHandler.BLOCK, SPA_BE.get(), (be, side) -> be.tank);
      e.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, VIS_GENERATOR_BE.get(),
         (be, side) -> side == DeviceBlock.getFacing(be.getBlockState()) ? be : null);
      e.registerBlockEntity(Capabilities.ItemHandler.BLOCK, HUNGRY_CHEST_BE.get(),
         (be, side) -> new net.neoforged.neoforge.items.wrapper.InvWrapper(be));
      e.registerBlockEntity(Capabilities.ItemHandler.BLOCK, INFERNAL_FURNACE_BE.get(), (be, side) -> new SidedInvWrapper(be, side));
      e.registerBlockEntity(Capabilities.ItemHandler.BLOCK, SPA_BE.get(), (be, side) -> new SidedInvWrapper(be, side));
      e.registerBlockEntity(Capabilities.ItemHandler.BLOCK, POTION_SPRAYER_BE.get(), (be, side) -> new SidedInvWrapper(be, side));
      e.registerBlockEntity(Capabilities.ItemHandler.BLOCK, THAUMATORIUM_BE.get(), (be, side) -> new SidedInvWrapper(be, side));
      e.registerBlockEntity(Capabilities.ItemHandler.BLOCK, THAUMATORIUM_TOP_BE.get(),
         (be, side) -> be.thaumatorium() != null ? new SidedInvWrapper(be.thaumatorium(), side) : null);
      e.registerBlockEntity(Capabilities.ItemHandler.BLOCK, RECHARGE_PEDESTAL_BE.get(), (be, side) -> new SidedInvWrapper(be, side));
   }

   /** A block item that stays out of the creative tab. */
   public static class HiddenBlockItem extends BlockItem implements ICreativeVariants {
      public HiddenBlockItem(Block block) {
         super(block, new Item.Properties());
      }

      @Override
      public void addCreativeStacks(java.util.function.Consumer<ItemStack> output) {
      }
   }
}
