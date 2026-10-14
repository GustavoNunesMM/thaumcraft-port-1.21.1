package thaumcraft.common.registers;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import thaumcraft.Thaumcraft;
import thaumcraft.common.entities.construct.ArcaneBoreEntity;
import thaumcraft.common.entities.construct.TurretCrossbowAdvancedEntity;
import thaumcraft.common.entities.construct.TurretCrossbowEntity;
import thaumcraft.common.entities.monster.BrainyZombieEntity;
import thaumcraft.common.entities.monster.FireBatEntity;
import thaumcraft.common.entities.monster.GiantBrainyZombieEntity;
import thaumcraft.common.entities.monster.MindSpiderEntity;
import thaumcraft.common.entities.monster.PechEntity;
import thaumcraft.common.entities.monster.ThaumicSlimeEntity;
import thaumcraft.common.entities.monster.WispEntity;
import thaumcraft.common.entities.monster.cult.CultistClericEntity;
import thaumcraft.common.entities.monster.cult.CultistKnightEntity;
import thaumcraft.common.entities.monster.cult.CultistLeaderEntity;
import thaumcraft.common.entities.monster.cult.CultistPortalGreaterEntity;
import thaumcraft.common.entities.monster.cult.CultistPortalLesserEntity;
import thaumcraft.common.entities.projectile.GolemDartEntity;
import thaumcraft.common.entities.projectile.GolemOrbEntity;
import thaumcraft.common.items.mobs.LootBagItem;
import thaumcraft.common.items.mobs.PechWandItem;
import thaumcraft.common.items.mobs.PrimordialPearlItem;
import thaumcraft.common.items.mobs.TurretPlacerItem;
import thaumcraft.common.menu.ArcaneBoreMenu;
import thaumcraft.common.menu.PechMenu;
import thaumcraft.common.menu.TurretAdvancedMenu;
import thaumcraft.common.menu.TurretBasicMenu;
import thaumcraft.common.world.MobWorldgen;

/**
 * Milestone 13 (mobs and world): the creatures of ConfigEntities, the constructs (turrets, arcane bore), their items
 * (loot bags, primordial pearl, meat nuggets and the triple meat treat, pech wand, turret placers, spawn eggs) and menus.
 * Registers into the shared DeferredRegisters; world generation lives in {@link MobWorldgen}.
 */
public final class ModMobs {
   private ModMobs() {
   }

   private static <T extends net.minecraft.world.entity.Entity> Supplier<EntityType<T>> entity(String name, EntityType.Builder<T> b) {
      return ModEntities.ENTITIES.register(name, () -> b.build(name));
   }

   // ------------------------------------------------------------------ creatures
   public static final Supplier<EntityType<WispEntity>> WISP = entity("wisp",
      EntityType.Builder.<WispEntity>of(WispEntity::new, MobCategory.MONSTER).sized(0.9F, 0.9F).clientTrackingRange(8).updateInterval(3));
   public static final Supplier<EntityType<FireBatEntity>> FIREBAT = entity("firebat",
      EntityType.Builder.<FireBatEntity>of(FireBatEntity::new, MobCategory.MONSTER).sized(0.5F, 0.7F).fireImmune().clientTrackingRange(8)
         .updateInterval(3));
   public static final Supplier<EntityType<PechEntity>> PECH = entity("pech",
      EntityType.Builder.<PechEntity>of(PechEntity::new, MobCategory.MONSTER).sized(0.6F, 1.8F).clientTrackingRange(8).updateInterval(3));
   public static final Supplier<EntityType<MindSpiderEntity>> MIND_SPIDER = entity("mind_spider",
      EntityType.Builder.<MindSpiderEntity>of(MindSpiderEntity::new, MobCategory.MONSTER).sized(0.7F, 0.5F).clientTrackingRange(8)
         .updateInterval(3));
   public static final Supplier<EntityType<BrainyZombieEntity>> BRAINY_ZOMBIE = entity("brainy_zombie",
      EntityType.Builder.<BrainyZombieEntity>of(BrainyZombieEntity::new, MobCategory.MONSTER).sized(0.6F, 1.95F).eyeHeight(1.74F)
         .clientTrackingRange(8).updateInterval(3));
   public static final Supplier<EntityType<GiantBrainyZombieEntity>> GIANT_BRAINY_ZOMBIE = entity("giant_brainy_zombie",
      EntityType.Builder.<GiantBrainyZombieEntity>of(GiantBrainyZombieEntity::new, MobCategory.MONSTER).sized(0.6F, 1.95F).eyeHeight(1.74F)
         .clientTrackingRange(8).updateInterval(3));
   public static final Supplier<EntityType<ThaumicSlimeEntity>> THAUMIC_SLIME = entity("thaumic_slime",
      EntityType.Builder.<ThaumicSlimeEntity>of(ThaumicSlimeEntity::new, MobCategory.MONSTER).sized(0.52F, 0.52F).eyeHeight(0.325F)
         .clientTrackingRange(8).updateInterval(3));
   public static final Supplier<EntityType<CultistKnightEntity>> CULTIST_KNIGHT = entity("cultist_knight",
      EntityType.Builder.<CultistKnightEntity>of(CultistKnightEntity::new, MobCategory.MONSTER).sized(0.6F, 1.95F).clientTrackingRange(8)
         .updateInterval(3));
   public static final Supplier<EntityType<CultistClericEntity>> CULTIST_CLERIC = entity("cultist_cleric",
      EntityType.Builder.<CultistClericEntity>of(CultistClericEntity::new, MobCategory.MONSTER).sized(0.6F, 1.95F).clientTrackingRange(8)
         .updateInterval(3));
   public static final Supplier<EntityType<CultistLeaderEntity>> CULTIST_LEADER = entity("cultist_leader",
      EntityType.Builder.<CultistLeaderEntity>of(CultistLeaderEntity::new, MobCategory.MONSTER).sized(0.75F, 2.25F).clientTrackingRange(8)
         .updateInterval(3));
   public static final Supplier<EntityType<CultistPortalLesserEntity>> CULTIST_PORTAL_LESSER = entity("cultist_portal_lesser",
      EntityType.Builder.<CultistPortalLesserEntity>of(CultistPortalLesserEntity::new, MobCategory.MONSTER).sized(1.5F, 3.0F).fireImmune()
         .clientTrackingRange(8).updateInterval(20));
   public static final Supplier<EntityType<CultistPortalGreaterEntity>> CULTIST_PORTAL_GREATER = entity("cultist_portal_greater",
      EntityType.Builder.<CultistPortalGreaterEntity>of(CultistPortalGreaterEntity::new, MobCategory.MONSTER).sized(1.5F, 3.0F).fireImmune()
         .clientTrackingRange(8).updateInterval(20));

   // ------------------------------------------------------------------ constructs and projectiles
   public static final Supplier<EntityType<TurretCrossbowEntity>> TURRET_CROSSBOW = entity("turret_crossbow",
      EntityType.Builder.<TurretCrossbowEntity>of(TurretCrossbowEntity::new, MobCategory.MISC).sized(0.95F, 1.25F).clientTrackingRange(8)
         .updateInterval(3));
   public static final Supplier<EntityType<TurretCrossbowAdvancedEntity>> TURRET_CROSSBOW_ADVANCED = entity("turret_crossbow_advanced",
      EntityType.Builder.<TurretCrossbowAdvancedEntity>of(TurretCrossbowAdvancedEntity::new, MobCategory.MISC).sized(0.95F, 1.5F)
         .clientTrackingRange(8).updateInterval(3));
   public static final Supplier<EntityType<ArcaneBoreEntity>> ARCANE_BORE = entity("arcane_bore",
      EntityType.Builder.<ArcaneBoreEntity>of(ArcaneBoreEntity::new, MobCategory.MISC).sized(0.9F, 0.9F).clientTrackingRange(8)
         .updateInterval(3));
   public static final Supplier<EntityType<GolemOrbEntity>> GOLEM_ORB = entity("golem_orb",
      EntityType.Builder.<GolemOrbEntity>of(GolemOrbEntity::new, MobCategory.MISC).sized(0.5F, 0.5F).clientTrackingRange(4).updateInterval(3));
   public static final Supplier<EntityType<GolemDartEntity>> GOLEM_DART = entity("golem_dart",
      EntityType.Builder.<GolemDartEntity>of(GolemDartEntity::new, MobCategory.MISC).sized(0.2F, 0.2F).clientTrackingRange(4).updateInterval(20));

   // ------------------------------------------------------------------ items
   public static final DeferredItem<LootBagItem> LOOT_BAG_COMMON = ModItems.ITEMS.registerItem("loot_bag_common", p -> new LootBagItem(0, p),
      new Item.Properties().stacksTo(16));
   public static final DeferredItem<LootBagItem> LOOT_BAG_UNCOMMON = ModItems.ITEMS.registerItem("loot_bag_uncommon", p -> new LootBagItem(1, p),
      new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON));
   public static final DeferredItem<LootBagItem> LOOT_BAG_RARE = ModItems.ITEMS.registerItem("loot_bag_rare", p -> new LootBagItem(2, p),
      new Item.Properties().stacksTo(16).rarity(Rarity.RARE));
   public static final DeferredItem<PrimordialPearlItem> PRIMORDIAL_PEARL = ModItems.ITEMS.registerItem("primordial_pearl", PrimordialPearlItem::new,
      new Item.Properties().durability(8).rarity(Rarity.UNCOMMON).setNoRepair());
   public static final DeferredItem<PechWandItem> PECH_WAND = ModItems.ITEMS.registerItem("pech_wand", PechWandItem::new,
      new Item.Properties().rarity(Rarity.RARE));
   /** ItemTripleMeatTreat: always edible, 66% chance of a short regeneration. */
   public static final DeferredItem<Item> TRIPLE_MEAT_TREAT = ModItems.ITEMS.registerSimpleItem("triple_meat_treat",
      new Item.Properties().food(new FoodProperties.Builder().nutrition(6).saturationModifier(0.8F).alwaysEdible()
         .effect(() -> new MobEffectInstance(MobEffects.REGENERATION, 100, 0), 0.66F).build()));
   /** ItemChunksEdible: the meat nuggets the infernal furnace gives (eaten in 10 ticks). */
   public static final String[] CHUNK_VARIANTS = {"beef", "chicken", "pork", "fish", "rabbit", "mutton"};
   public static final List<DeferredItem<Item>> CHUNKS = new ArrayList<>();

   static {
      for (String v : CHUNK_VARIANTS) {
         CHUNKS.add(ModItems.ITEMS.registerItem("chunk_" + v, p -> new Item(p) {
            @Override
            public int getUseDuration(net.minecraft.world.item.ItemStack stack, net.minecraft.world.entity.LivingEntity entity) {
               return 10;
            }
         }, new Item.Properties().food(new FoodProperties.Builder().nutrition(1).saturationModifier(0.3F).build())));
      }
   }

   public static final DeferredItem<TurretPlacerItem> TURRET_BASIC = ModItems.ITEMS.registerItem("turret_basic", p -> new TurretPlacerItem(0, p));
   public static final DeferredItem<TurretPlacerItem> TURRET_ADVANCED = ModItems.ITEMS.registerItem("turret_advanced", p -> new TurretPlacerItem(1, p));
   public static final DeferredItem<TurretPlacerItem> TURRET_BORE = ModItems.ITEMS.registerItem("turret_bore", p -> new TurretPlacerItem(2, p));

   /** The spawn eggs (ConfigEntities colours, as unsigned RGB). */
   public static final List<DeferredItem<DeferredSpawnEggItem>> SPAWN_EGGS = new ArrayList<>();

   private static void egg(String name, Supplier<? extends EntityType<? extends Mob>> type, int bg, int fg) {
      SPAWN_EGGS.add(ModItems.ITEMS.registerItem(name + "_spawn_egg", p -> new DeferredSpawnEggItem(type, bg & 0xFFFFFF, fg & 0xFFFFFF, p)));
   }

   static {
      egg("wisp", WISP, -16129, -1);
      egg("firebat", FIREBAT, -16129, -806354944);
      egg("pech", PECH, -16129, -12582848);
      egg("mind_spider", MIND_SPIDER, 4996656, 4473924);
      egg("cultist_knight", CULTIST_KNIGHT, 9438728, 128);
      egg("cultist_cleric", CULTIST_CLERIC, 9438728, 8388608);
      egg("cultist_leader", CULTIST_LEADER, 6842578, 9438728);
      egg("thaumic_slime", THAUMIC_SLIME, 10618530, -32513);
      egg("brainy_zombie", BRAINY_ZOMBIE, -16129, -16744448);
      egg("giant_brainy_zombie", GIANT_BRAINY_ZOMBIE, -16129, -16760832);
      // No eggs in 1.12 for the portals; handy for testing.
      egg("cultist_portal_lesser", CULTIST_PORTAL_LESSER, 9438728, 6842578);
      egg("cultist_portal_greater", CULTIST_PORTAL_GREATER, 6842578, 9438728);
   }

   // ------------------------------------------------------------------ menus
   public static final Supplier<MenuType<PechMenu>> PECH_MENU = ModMenus.MENUS.register("pech", () -> IMenuTypeExtension.create(PechMenu::new));
   public static final Supplier<MenuType<TurretBasicMenu>> TURRET_BASIC_MENU = ModMenus.MENUS.register("turret_basic",
      () -> IMenuTypeExtension.create(TurretBasicMenu::new));
   public static final Supplier<MenuType<TurretAdvancedMenu>> TURRET_ADVANCED_MENU = ModMenus.MENUS.register("turret_advanced",
      () -> IMenuTypeExtension.create(TurretAdvancedMenu::new));
   public static final Supplier<MenuType<ArcaneBoreMenu>> ARCANE_BORE_MENU = ModMenus.MENUS.register("arcane_bore",
      () -> IMenuTypeExtension.create(ArcaneBoreMenu::new));

   // ------------------------------------------------------------------ loot
   public static final net.neoforged.neoforge.registries.DeferredRegister<net.minecraft.world.level.storage.loot.functions.LootItemFunctionType<?>>
      LOOT_FUNCTIONS = net.neoforged.neoforge.registries.DeferredRegister.create(net.minecraft.core.registries.Registries.LOOT_FUNCTION_TYPE,
      Thaumcraft.MODID);
   public static final Supplier<net.minecraft.world.level.storage.loot.functions.LootItemFunctionType<thaumcraft.common.lib.loot.GearRollFunction>>
      GEAR_ROLL = LOOT_FUNCTIONS.register("gear_roll",
      () -> new net.minecraft.world.level.storage.loot.functions.LootItemFunctionType<>(thaumcraft.common.lib.loot.GearRollFunction.CODEC));

   public static final Supplier<net.minecraft.world.item.crafting.RecipeSerializer<thaumcraft.common.crafting.TripleMeatTreatRecipe>>
      TRIPLE_MEAT_TREAT_RECIPE = ModRecipes.SERIALIZERS.register("triple_meat_treat",
      () -> new net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer<>(thaumcraft.common.crafting.TripleMeatTreatRecipe::new));

   /** Called from the Thaumcraft constructor (before the DeferredRegisters are registered). */
   public static void init(IEventBus modBus) {
      LOOT_FUNCTIONS.register(modBus);
      MobWorldgen.init(modBus);
   }

   @EventBusSubscriber(modid = Thaumcraft.MODID, bus = EventBusSubscriber.Bus.MOD)
   public static final class Events {
      private Events() {
      }

      @SubscribeEvent
      public static void attributes(EntityAttributeCreationEvent e) {
         e.put(WISP.get(), WispEntity.createAttributes().build());
         e.put(FIREBAT.get(), FireBatEntity.createAttributes().build());
         e.put(PECH.get(), PechEntity.createAttributes().build());
         e.put(MIND_SPIDER.get(), MindSpiderEntity.createAttributes().build());
         e.put(BRAINY_ZOMBIE.get(), BrainyZombieEntity.createAttributes().build());
         e.put(GIANT_BRAINY_ZOMBIE.get(), GiantBrainyZombieEntity.createAttributes().build());
         e.put(THAUMIC_SLIME.get(), Monster.createMonsterAttributes().build());
         e.put(CULTIST_KNIGHT.get(), CultistKnightEntity.createAttributes().build());
         e.put(CULTIST_CLERIC.get(), CultistClericEntity.createAttributes().build());
         e.put(CULTIST_LEADER.get(), CultistLeaderEntity.createAttributes().build());
         e.put(CULTIST_PORTAL_LESSER.get(), CultistPortalLesserEntity.createAttributes().build());
         e.put(CULTIST_PORTAL_GREATER.get(), CultistPortalGreaterEntity.createAttributes().build());
         e.put(TURRET_CROSSBOW.get(), TurretCrossbowEntity.createAttributes().build());
         e.put(TURRET_CROSSBOW_ADVANCED.get(), TurretCrossbowAdvancedEntity.createAttributes().build());
         e.put(ARCANE_BORE.get(), ArcaneBoreEntity.createAttributes().build());
      }

      @SubscribeEvent
      public static void spawnPlacements(RegisterSpawnPlacementsEvent e) {
         var op = RegisterSpawnPlacementsEvent.Operation.REPLACE;
         e.register(WISP.get(), SpawnPlacementTypes.NO_RESTRICTIONS, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, WispEntity::checkSpawnRules, op);
         e.register(FIREBAT.get(), SpawnPlacementTypes.NO_RESTRICTIONS, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, FireBatEntity::checkSpawnRules,
            op);
         e.register(PECH.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, PechEntity::checkSpawnRules, op);
         e.register(BRAINY_ZOMBIE.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Monster::checkMonsterSpawnRules,
            op);
         e.register(GIANT_BRAINY_ZOMBIE.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
            Monster::checkMonsterSpawnRules, op);
         e.register(MIND_SPIDER.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Monster::checkMonsterSpawnRules,
            op);
      }
   }
}
