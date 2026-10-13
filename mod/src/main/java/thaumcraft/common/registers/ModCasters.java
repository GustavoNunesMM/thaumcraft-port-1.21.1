package thaumcraft.common.registers;

import java.util.function.Supplier;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import thaumcraft.Thaumcraft;
import thaumcraft.api.casters.FocusEngine;
import thaumcraft.api.casters.FocusMediumRoot;
import thaumcraft.common.blocks.crafting.FocalManipulatorBlock;
import thaumcraft.common.blocks.crafting.FocalManipulatorBlockEntity;
import thaumcraft.common.blocks.misc.SapEffectBlock;
import thaumcraft.common.entities.monster.SpellBatEntity;
import thaumcraft.common.entities.projectile.FocusCloudEntity;
import thaumcraft.common.entities.projectile.FocusMineEntity;
import thaumcraft.common.entities.projectile.FocusProjectileEntity;
import thaumcraft.common.items.casters.FocusItem;
import thaumcraft.common.items.casters.FocusPouchItem;
import thaumcraft.common.items.casters.foci.FocusEffectAir;
import thaumcraft.common.items.casters.foci.FocusEffectBreak;
import thaumcraft.common.items.casters.foci.FocusEffectCurse;
import thaumcraft.common.items.casters.foci.FocusEffectEarth;
import thaumcraft.common.items.casters.foci.FocusEffectExchange;
import thaumcraft.common.items.casters.foci.FocusEffectFire;
import thaumcraft.common.items.casters.foci.FocusEffectFlux;
import thaumcraft.common.items.casters.foci.FocusEffectFrost;
import thaumcraft.common.items.casters.foci.FocusEffectHeal;
import thaumcraft.common.items.casters.foci.FocusEffectRift;
import thaumcraft.common.items.casters.foci.FocusMediumBolt;
import thaumcraft.common.items.casters.foci.FocusMediumCloud;
import thaumcraft.common.items.casters.foci.FocusMediumMine;
import thaumcraft.common.items.casters.foci.FocusMediumPlan;
import thaumcraft.common.items.casters.foci.FocusMediumProjectile;
import thaumcraft.common.items.casters.foci.FocusMediumSpellBat;
import thaumcraft.common.items.casters.foci.FocusMediumTouch;
import thaumcraft.common.items.casters.foci.FocusModScatter;
import thaumcraft.common.items.casters.foci.FocusModSplitTarget;
import thaumcraft.common.items.casters.foci.FocusModSplitTrajectory;
import thaumcraft.common.menu.FocalManipulatorMenu;
import thaumcraft.common.menu.FocusPouchMenu;

/** Milestone 9 (casters and foci): foci, focus pouch, focal manipulator, focus entities and the focus elements. */
public final class ModCasters {
   public static final DeferredItem<FocusItem> FOCUS_1 = ModItems.ITEMS.registerItem("focus_1", p -> new FocusItem(15, p),
      new Item.Properties().stacksTo(1).rarity(Rarity.RARE));
   public static final DeferredItem<FocusItem> FOCUS_2 = ModItems.ITEMS.registerItem("focus_2", p -> new FocusItem(25, p),
      new Item.Properties().stacksTo(1).rarity(Rarity.RARE));
   public static final DeferredItem<FocusItem> FOCUS_3 = ModItems.ITEMS.registerItem("focus_3", p -> new FocusItem(50, p),
      new Item.Properties().stacksTo(1).rarity(Rarity.RARE));
   public static final DeferredItem<FocusPouchItem> FOCUS_POUCH = ModItems.ITEMS.registerItem("focus_pouch", FocusPouchItem::new,
      new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));

   public static final DeferredBlock<FocalManipulatorBlock> WAND_WORKBENCH = ModBlocks.BLOCKS.registerBlock("wand_workbench", FocalManipulatorBlock::new,
      BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(1.5F, 2.0F).sound(SoundType.STONE).noOcclusion());
   public static final DeferredItem<BlockItem> WAND_WORKBENCH_ITEM = ModItems.ITEMS.registerSimpleBlockItem(WAND_WORKBENCH);
   /** BlockEffect "sap": the field a curse focus leaves on blocks. */
   public static final DeferredBlock<SapEffectBlock> EFFECT_SAP = ModBlocks.BLOCKS.registerBlock("effect_sap", SapEffectBlock::new,
      BlockBehaviour.Properties.of().replaceable().noCollission().noLootTable().air().randomTicks().lightLevel(s -> 7).strength(0.0F, 999.0F)
         .pushReaction(PushReaction.DESTROY));

   public static final Supplier<BlockEntityType<FocalManipulatorBlockEntity>> FOCAL_MANIPULATOR_BE = ModBlockEntities.BLOCK_ENTITIES.register(
      "focal_manipulator", () -> BlockEntityType.Builder.of(FocalManipulatorBlockEntity::new, WAND_WORKBENCH.get()).build(null));

   public static final Supplier<MenuType<FocalManipulatorMenu>> FOCAL_MANIPULATOR_MENU = ModMenus.MENUS.register("focal_manipulator",
      () -> IMenuTypeExtension.create(FocalManipulatorMenu::new));
   public static final Supplier<MenuType<FocusPouchMenu>> FOCUS_POUCH_MENU = ModMenus.MENUS.register("focus_pouch",
      () -> IMenuTypeExtension.create(FocusPouchMenu::new));

   public static final Supplier<EntityType<FocusProjectileEntity>> FOCUS_PROJECTILE = ModEntities.ENTITIES.register("focus_projectile",
      () -> EntityType.Builder.<FocusProjectileEntity>of(FocusProjectileEntity::new, MobCategory.MISC).sized(0.15F, 0.15F)
         .clientTrackingRange(4).updateInterval(20).build("focus_projectile"));
   public static final Supplier<EntityType<FocusCloudEntity>> FOCUS_CLOUD = ModEntities.ENTITIES.register("focus_cloud",
      () -> EntityType.Builder.<FocusCloudEntity>of(FocusCloudEntity::new, MobCategory.MISC).sized(0.15F, 0.15F)
         .clientTrackingRange(4).updateInterval(20).build("focus_cloud"));
   public static final Supplier<EntityType<FocusMineEntity>> FOCUS_MINE = ModEntities.ENTITIES.register("focus_mine",
      () -> EntityType.Builder.<FocusMineEntity>of(FocusMineEntity::new, MobCategory.MISC).sized(0.15F, 0.15F)
         .clientTrackingRange(4).updateInterval(20).build("focus_mine"));
   public static final Supplier<EntityType<SpellBatEntity>> SPELL_BAT = ModEntities.ENTITIES.register("spell_bat",
      () -> EntityType.Builder.<SpellBatEntity>of(SpellBatEntity::new, MobCategory.MISC).sized(0.5F, 0.9F)
         .clientTrackingRange(5).build("spell_bat"));

   private ModCasters() {
   }

   private static ResourceLocation icon(String name) {
      return ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/foci/" + name + ".png");
   }

   /** Called from the Thaumcraft constructor: the registrations above, and the focus elements (ConfigItems.initFoci). */
   public static void init() {
      if (FocusEngine.getElement("thaumcraft.TOUCH") != null) return;
      FocusEngine.registerElement(FocusMediumRoot::new, icon("root"), 10066329);
      FocusEngine.registerElement(FocusMediumTouch::new, icon("touch"), 11371909);
      FocusEngine.registerElement(FocusMediumBolt::new, icon("bolt"), 11377029);
      FocusEngine.registerElement(FocusMediumProjectile::new, icon("projectile"), 11382149);
      FocusEngine.registerElement(FocusMediumCloud::new, icon("cloud"), 10071429);
      FocusEngine.registerElement(FocusMediumMine::new, icon("mine"), 8760709);
      FocusEngine.registerElement(FocusMediumPlan::new, icon("plan"), 8760728);
      FocusEngine.registerElement(FocusMediumSpellBat::new, icon("spellbat"), 8760748);
      FocusEngine.registerElement(FocusEffectFire::new, icon("fire"), 16734721);
      FocusEngine.registerElement(FocusEffectFrost::new, icon("frost"), 14811135);
      FocusEngine.registerElement(FocusEffectAir::new, icon("air"), 16777086);
      FocusEngine.registerElement(FocusEffectEarth::new, icon("earth"), 5685248);
      FocusEngine.registerElement(FocusEffectFlux::new, icon("flux"), 8388736);
      FocusEngine.registerElement(FocusEffectBreak::new, icon("break"), 9063176);
      FocusEngine.registerElement(FocusEffectRift::new, icon("rift"), 3084645);
      FocusEngine.registerElement(FocusEffectExchange::new, icon("exchange"), 5735255);
      FocusEngine.registerElement(FocusEffectCurse::new, icon("curse"), 6946821);
      FocusEngine.registerElement(FocusEffectHeal::new, icon("heal"), 14548997);
      FocusEngine.registerElement(FocusModScatter::new, icon("scatter"), 10066329);
      FocusEngine.registerElement(FocusModSplitTarget::new, icon("split_target"), 10066329);
      FocusEngine.registerElement(FocusModSplitTrajectory::new, icon("split_trajectory"), 10066329);
   }

   @EventBusSubscriber(modid = Thaumcraft.MODID, bus = EventBusSubscriber.Bus.MOD)
   public static final class Events {
      private Events() {
      }

      @SubscribeEvent
      public static void attributes(EntityAttributeCreationEvent event) {
         event.put(SPELL_BAT.get(), SpellBatEntity.createAttributes().build());
      }
   }
}
