package thaumcraft.common.registers;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import thaumcraft.Thaumcraft;
import thaumcraft.common.crafting.dust.DustTrigger;
import thaumcraft.common.crafting.dust.DustTriggerMultiblock;
import thaumcraft.common.golems.GolemBellItem;
import thaumcraft.common.golems.GolemBuilderBlock;
import thaumcraft.common.golems.GolemBuilderBlockEntity;
import thaumcraft.common.golems.GolemEntity;
import thaumcraft.common.golems.GolemPlacerItem;
import thaumcraft.common.golems.GolemProperties;
import thaumcraft.common.golems.PlaceholderBlock;
import thaumcraft.common.golems.menu.GolemBuilderMenu;
import thaumcraft.common.golems.menu.LogisticsMenu;
import thaumcraft.common.golems.menu.SealMenu;
import thaumcraft.common.golems.seals.SealBreaker;
import thaumcraft.common.golems.seals.SealBreakerAdvanced;
import thaumcraft.common.golems.seals.SealButcher;
import thaumcraft.common.golems.seals.SealEmpty;
import thaumcraft.common.golems.seals.SealEmptyAdvanced;
import thaumcraft.common.golems.seals.SealFill;
import thaumcraft.common.golems.seals.SealFillAdvanced;
import thaumcraft.common.golems.seals.SealGuard;
import thaumcraft.common.golems.seals.SealGuardAdvanced;
import thaumcraft.common.golems.seals.SealHandler;
import thaumcraft.common.golems.seals.SealHarvest;
import thaumcraft.common.golems.seals.SealLumber;
import thaumcraft.common.golems.seals.SealPickup;
import thaumcraft.common.golems.seals.SealPickupAdvanced;
import thaumcraft.common.golems.seals.SealPlacerItem;
import thaumcraft.common.golems.seals.SealProvide;
import thaumcraft.common.golems.seals.SealStock;
import thaumcraft.common.golems.seals.SealUse;
import thaumcraft.common.golems.tasks.TaskHandler;

/** Milestone 11: golems, seals, the golem press and the logistics screen. */
public final class ModGolems {
   /** The seal types in the 1.12 registration order (ConfigItems.preInitSeals); their items are seal_&lt;name&gt;. */
   public static final String[] SEAL_NAMES = {"pickup", "pickup_advanced", "fill", "fill_advanced", "empty", "empty_advanced", "harvest", "butcher",
      "guard", "guard_advanced", "lumber", "breaker", "use", "provider", "stock", "breaker_advanced"};

   public static final Supplier<DataComponentType<Long>> GOLEM_PROPS = ModDataComponents.COMPONENTS.registerComponentType("golem_props",
      b -> b.persistent(com.mojang.serialization.Codec.LONG).networkSynchronized(ByteBufCodecs.VAR_LONG));
   public static final Supplier<DataComponentType<Integer>> GOLEM_XP = ModDataComponents.COMPONENTS.registerComponentType("golem_xp",
      b -> b.persistent(com.mojang.serialization.Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));

   public static final Supplier<EntityType<GolemEntity>> GOLEM = ModEntities.ENTITIES.register("golem",
      () -> EntityType.Builder.<GolemEntity>of(GolemEntity::new, MobCategory.MISC).sized(0.4F, 0.9F).eyeHeight(0.7F).clientTrackingRange(10)
         .build("golem"));
   // golem_dart is registered by ModMobs (common/entities/projectile/GolemDartEntity)

   public static final DeferredItem<GolemPlacerItem> GOLEM_PLACER = ModItems.ITEMS.registerItem("golem", GolemPlacerItem::new, new Item.Properties());
   public static final DeferredItem<GolemBellItem> GOLEM_BELL = ModItems.ITEMS.registerItem("golem_bell", GolemBellItem::new,
      new Item.Properties().stacksTo(1));
   public static final DeferredItem<SealPlacerItem> SEAL_BLANK = ModItems.ITEMS.registerItem("seal_blank", p -> new SealPlacerItem(null, p),
      new Item.Properties());
   public static final Map<String, DeferredItem<SealPlacerItem>> SEALS = new LinkedHashMap<>();

   static {
      for (String s : SEAL_NAMES) {
         SEALS.put(s, ModItems.ITEMS.registerItem("seal_" + s, p -> new SealPlacerItem("thaumcraft:" + s, p), new Item.Properties()));
      }
   }

   public static final DeferredBlock<GolemBuilderBlock> GOLEM_BUILDER = ModBlocks.BLOCKS.registerBlock("golem_builder", GolemBuilderBlock::new,
      BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(1.5F, 10.0F).sound(SoundType.STONE).noOcclusion());
   public static final DeferredBlock<PlaceholderBlock> PLACEHOLDER_BARS = placeholder("placeholder_bars", 0);
   public static final DeferredBlock<PlaceholderBlock> PLACEHOLDER_ANVIL = placeholder("placeholder_anvil", 0);
   public static final DeferredBlock<PlaceholderBlock> PLACEHOLDER_CAULDRON = placeholder("placeholder_cauldron", 13);
   public static final DeferredBlock<PlaceholderBlock> PLACEHOLDER_TABLE = placeholder("placeholder_table", 0);

   public static final Supplier<BlockEntityType<GolemBuilderBlockEntity>> GOLEM_BUILDER_BE = ModBlockEntities.BLOCK_ENTITIES.register("golem_builder",
      () -> BlockEntityType.Builder.of(GolemBuilderBlockEntity::new, GOLEM_BUILDER.get()).build(null));

   public static final Supplier<MenuType<SealMenu>> SEAL_MENU = ModMenus.MENUS.register("seal", () -> IMenuTypeExtension.create(SealMenu::new));
   public static final Supplier<MenuType<LogisticsMenu>> LOGISTICS_MENU = ModMenus.MENUS.register("logistics",
      () -> IMenuTypeExtension.create(LogisticsMenu::new));
   public static final Supplier<MenuType<GolemBuilderMenu>> GOLEM_BUILDER_MENU = ModMenus.MENUS.register("golem_builder",
      () -> IMenuTypeExtension.create(GolemBuilderMenu::new));

   private ModGolems() {
   }

   private static DeferredBlock<PlaceholderBlock> placeholder(String name, int light) {
      return ModBlocks.BLOCKS.registerBlock(name, PlaceholderBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(2.5F)
         .sound(SoundType.STONE).noOcclusion().lightLevel(s -> light).pushReaction(PushReaction.BLOCK));
   }

   /** Called from the Thaumcraft constructor: the registrations above, the golem parts and the seal types. */
   public static void init(IEventBus modBus) {
      GolemProperties.registerParts();
      if (SealHandler.types.isEmpty()) {
         SealHandler.registerSeal(new SealPickup());
         SealHandler.registerSeal(new SealPickupAdvanced());
         SealHandler.registerSeal(new SealFill());
         SealHandler.registerSeal(new SealFillAdvanced());
         SealHandler.registerSeal(new SealEmpty());
         SealHandler.registerSeal(new SealEmptyAdvanced());
         SealHandler.registerSeal(new SealHarvest());
         SealHandler.registerSeal(new SealButcher());
         SealHandler.registerSeal(new SealGuard());
         SealHandler.registerSeal(new SealGuardAdvanced());
         SealHandler.registerSeal(new SealLumber());
         SealHandler.registerSeal(new SealBreaker());
         SealHandler.registerSeal(new SealUse());
         SealHandler.registerSeal(new SealProvide());
         SealHandler.registerSeal(new SealStock());
         SealHandler.registerSeal(new SealBreakerAdvanced());
      }
      modBus.addListener(EventPriority.LOWEST, (FMLCommonSetupEvent e) -> e.enqueueWork(ModGolems::registerDustTriggers));
   }

   /**
    * The MINDCLOCKWORK golem press multiblock (ConfigRecipes): iron bars over a cauldron, an anvil, a piston facing up
    * (which becomes the press) and a stone table. Runs after ModDustTriggers.register, which clears the list.
    */
   public static void registerDustTriggers() {
      if (DustTrigger.TRIGGERS.stream().anyMatch(t -> t instanceof PressTrigger)) return;
      DustTriggerMultiblock.Part gp1 = DustTriggerMultiblock.Part.of(s -> s.is(Blocks.IRON_BARS), d -> PLACEHOLDER_BARS.get().defaultBlockState());
      DustTriggerMultiblock.Part gp2 = DustTriggerMultiblock.Part.of(s -> s.is(BlockTags.CAULDRONS), d -> PLACEHOLDER_CAULDRON.get().defaultBlockState());
      DustTriggerMultiblock.Part gp3 = DustTriggerMultiblock.Part.of(s -> s.is(Blocks.PISTON) && s.getValue(PistonBaseBlock.FACING) == Direction.UP,
         d -> GOLEM_BUILDER.get().defaultBlockState().setValue(GolemBuilderBlock.HORIZONTAL_FACING, d));
      DustTriggerMultiblock.Part gp4 = DustTriggerMultiblock.Part.of(s -> s.is(BlockTags.ANVIL), d -> PLACEHOLDER_ANVIL.get().defaultBlockState());
      DustTriggerMultiblock.Part gp5 = DustTriggerMultiblock.Part.of(s -> s.is(ModBlocks.TABLE_STONE.get()), d -> PLACEHOLDER_TABLE.get().defaultBlockState());
      DustTriggerMultiblock.Part[][][] blueprint = {{{null, null}, {gp1, null}}, {{gp2, gp4}, {gp3, gp5}}};
      DustTrigger.register(new PressTrigger(blueprint));
   }

   /** The golem press's multiblock trigger (its own class, so it is registered once). */
   public static final class PressTrigger extends DustTriggerMultiblock {
      PressTrigger(Part[][][] blueprint) {
         super("MINDCLOCKWORK", blueprint, new ItemStack(Blocks.PISTON));
      }
   }

   @EventBusSubscriber(modid = Thaumcraft.MODID, bus = EventBusSubscriber.Bus.MOD)
   public static final class ModEvents {
      private ModEvents() {
      }

      @SubscribeEvent
      public static void attributes(EntityAttributeCreationEvent event) {
         event.put(GOLEM.get(), GolemEntity.createAttributes().build());
      }
   }

   /** ServerEvents/WorldEvents/ChunkEvents: seals tick with their level, load and save with it, and reach the clients. */
   @EventBusSubscriber(modid = Thaumcraft.MODID)
   public static final class GameEvents {
      private GameEvents() {
      }

      @SubscribeEvent
      public static void levelTick(LevelTickEvent.Post event) {
         if (!(event.getLevel() instanceof ServerLevel level)) return;
         if (level.getGameTime() % 20 == 0) TaskHandler.clearSuspendedOrExpiredTasks(level);
         SealHandler.tickSealEntities(level);
      }

      @SubscribeEvent
      public static void levelLoad(LevelEvent.Load event) {
         if (event.getLevel() instanceof ServerLevel level) SealHandler.loadLevel(level);
      }

      @SubscribeEvent
      public static void levelUnload(LevelEvent.Unload event) {
         if (event.getLevel() instanceof ServerLevel level) {
            SealHandler.unloadLevel(level);
         } else if (event.getLevel().isClientSide()) {
            SealHandler.clearClient();
         }
      }

      @SubscribeEvent
      public static void login(PlayerEvent.PlayerLoggedInEvent event) {
         if (event.getEntity() instanceof ServerPlayer sp) SealHandler.sendAll(sp);
      }

      @SubscribeEvent
      public static void changeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
         if (event.getEntity() instanceof ServerPlayer sp) SealHandler.sendAll(sp);
      }

      @SubscribeEvent
      public static void respawn(PlayerEvent.PlayerRespawnEvent event) {
         if (event.getEntity() instanceof ServerPlayer sp) SealHandler.sendAll(sp);
      }
   }
}
