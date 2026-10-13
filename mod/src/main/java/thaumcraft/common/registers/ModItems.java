package thaumcraft.common.registers;

import thaumcraft.common.items.AlumentumItem;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import thaumcraft.Thaumcraft;
import java.util.ArrayList;
import java.util.List;
import thaumcraft.common.items.CelestialNotesItem;
import thaumcraft.common.items.CurioItem;
import thaumcraft.common.items.SalisMundusItem;
import thaumcraft.common.items.ScribingToolsItem;
import thaumcraft.common.items.ThaumometerItem;
import thaumcraft.common.items.ThaumonomiconItem;
import thaumcraft.common.items.VisCrystalItem;

/**
 * Items of Thaumcraft 6. The 1.12 metadata items (ingot, nugget, cluster) are flattened into one item per
 * variant, named after their textures (ingot_thaumium, nugget_quicksilver, cluster_iron...).
 */
public final class ModItems {
   public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Thaumcraft.MODID);

   static {
      // Block items first so the creative tab lists blocks before items, as the original did.
      ModBlocks.registerBlockItems(ITEMS);
   }

   public static final DeferredItem<ThaumometerItem> THAUMOMETER = ITEMS.registerItem(
      "thaumometer", ThaumometerItem::new, new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)
   );

   public static final DeferredItem<ThaumonomiconItem> THAUMONOMICON = ITEMS.registerItem("thaumonomicon",
      p -> new ThaumonomiconItem(false, p), new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));
   public static final DeferredItem<ThaumonomiconItem> THAUMONOMICON_CHEAT = ITEMS.registerItem("thaumonomicon_cheat",
      p -> new ThaumonomiconItem(true, p), new Item.Properties().stacksTo(1).rarity(Rarity.EPIC));
   public static final DeferredItem<SalisMundusItem> SALIS_MUNDUS = ITEMS.registerItem("salis_mundus", SalisMundusItem::new,
      new Item.Properties().rarity(Rarity.UNCOMMON));
   public static final DeferredItem<ScribingToolsItem> SCRIBING_TOOLS = ITEMS.registerItem("scribing_tools", ScribingToolsItem::new);
   public static final List<DeferredItem<CurioItem>> CURIOS = new ArrayList<>();
   public static final List<DeferredItem<CelestialNotesItem>> CELESTIAL_NOTES = new ArrayList<>();

   static {
      for (String v : new String[]{"arcane", "preserved", "ancient", "eldritch", "knowledge", "twisted", "rites"}) {
         CURIOS.add(ITEMS.registerItem("curio_" + v, p -> new CurioItem(v, p), new Item.Properties().rarity(Rarity.UNCOMMON)));
      }
      for (int i = 0; i < CelestialNotesItem.VARIANTS.length; i++) {
         int idx = i;
         CELESTIAL_NOTES.add(ITEMS.registerItem("celestial_notes_" + CelestialNotesItem.VARIANTS[i], p -> new CelestialNotesItem(idx, p)));
      }
   }

   public static final DeferredItem<VisCrystalItem> CRYSTAL_ESSENCE = ITEMS.registerItem("crystal_essence", VisCrystalItem::new);
   public static final DeferredItem<Item> AMBER = ITEMS.registerSimpleItem("amber");
   public static final DeferredItem<Item> QUICKSILVER = ITEMS.registerSimpleItem("quicksilver");
   public static final DeferredItem<Item> TALLOW = ITEMS.registerSimpleItem("tallow");
   public static final DeferredItem<AlumentumItem> ALUMENTUM = ITEMS.registerItem("alumentum", AlumentumItem::new);

   public static final DeferredItem<Item> INGOT_THAUMIUM = ITEMS.registerSimpleItem("ingot_thaumium");
   public static final DeferredItem<Item> INGOT_VOID = ITEMS.registerSimpleItem("ingot_void", new Item.Properties().rarity(Rarity.UNCOMMON));
   public static final DeferredItem<Item> INGOT_BRASS = ITEMS.registerSimpleItem("ingot_brass");

   public static final DeferredItem<Item> NUGGET_THAUMIUM = ITEMS.registerSimpleItem("nugget_thaumium");
   public static final DeferredItem<Item> NUGGET_VOID = ITEMS.registerSimpleItem("nugget_void");
   public static final DeferredItem<Item> NUGGET_BRASS = ITEMS.registerSimpleItem("nugget_brass");
   public static final DeferredItem<Item> NUGGET_QUICKSILVER = ITEMS.registerSimpleItem("nugget_quicksilver");
   public static final DeferredItem<Item> NUGGET_QUARTZ = ITEMS.registerSimpleItem("nugget_quartz");
   // Vanilla has no copper nugget; Thaumcraft's is kept for smelting bonuses (Infernal Furnace, later).
   public static final DeferredItem<Item> NUGGET_COPPER = ITEMS.registerSimpleItem("nugget_copper");

   // Native clusters (metal purification in the crucible, later). Tin/silver/lead wait for those ores.
   public static final DeferredItem<Item> CLUSTER_IRON = ITEMS.registerSimpleItem("cluster_iron");
   public static final DeferredItem<Item> CLUSTER_GOLD = ITEMS.registerSimpleItem("cluster_gold");
   public static final DeferredItem<Item> CLUSTER_COPPER = ITEMS.registerSimpleItem("cluster_copper");
   public static final DeferredItem<Item> CLUSTER_CINNABAR = ITEMS.registerSimpleItem("cluster_cinnabar");
   public static final DeferredItem<Item> CLUSTER_QUARTZ = ITEMS.registerSimpleItem("cluster_quartz");

   // ------------------------------------------------------------------ essentia (milestone 7)
   public static final DeferredItem<thaumcraft.common.items.JarItem> JAR_NORMAL = ITEMS.registerItem("jar_normal",
      p -> new thaumcraft.common.items.JarItem(ModBlocks.JAR_NORMAL.get(), p), new Item.Properties().stacksTo(64));
   public static final DeferredItem<thaumcraft.common.items.JarItem> JAR_VOID = ITEMS.registerItem("jar_void",
      p -> new thaumcraft.common.items.JarItem(ModBlocks.JAR_VOID.get(), p), new Item.Properties().stacksTo(64));
   public static final DeferredItem<thaumcraft.common.items.PhialItem> PHIAL = ITEMS.registerItem("phial",
      p -> new thaumcraft.common.items.PhialItem(false, p));
   public static final DeferredItem<thaumcraft.common.items.PhialItem> PHIAL_FILLED = ITEMS.registerItem("phial_filled",
      p -> new thaumcraft.common.items.PhialItem(true, p), new Item.Properties().stacksTo(64));
   public static final DeferredItem<thaumcraft.common.items.LabelItem> LABEL = ITEMS.registerItem("label", thaumcraft.common.items.LabelItem::new);
   public static final DeferredItem<thaumcraft.common.items.LabelItem> LABEL_FILLED = ITEMS.registerItem("label_filled", thaumcraft.common.items.LabelItem::new);
   public static final DeferredItem<Item> JAR_BRACE = ITEMS.registerSimpleItem("jar_brace");
   public static final DeferredItem<thaumcraft.common.items.casters.CasterItem> CASTER_BASIC = ITEMS.registerItem("caster_basic",
      p -> new thaumcraft.common.items.casters.CasterItem(0, p), new Item.Properties().stacksTo(1));

   // ------------------------------------------------------------------ crafting components
   /** Rare earth (nugget variant 10 in 1.12): a rare extra drop from some ores. */
   public static final DeferredItem<Item> RARE_EARTH = ITEMS.registerSimpleItem("rare_earth");
   public static final DeferredItem<Item> FABRIC = ITEMS.registerSimpleItem("fabric");
   public static final DeferredItem<Item> VIS_RESONATOR = ITEMS.registerSimpleItem("vis_resonator");
   public static final DeferredItem<Item> MECHANISM_SIMPLE = ITEMS.registerSimpleItem("mechanism_simple");
   public static final DeferredItem<Item> MECHANISM_COMPLEX = ITEMS.registerSimpleItem("mechanism_complex");
   public static final DeferredItem<Item> PLATE_BRASS = ITEMS.registerSimpleItem("plate_brass");
   public static final DeferredItem<Item> PLATE_IRON = ITEMS.registerSimpleItem("plate_iron");
   public static final DeferredItem<Item> PLATE_THAUMIUM = ITEMS.registerSimpleItem("plate_thaumium");
   public static final DeferredItem<Item> PLATE_VOID = ITEMS.registerSimpleItem("plate_void");
   public static final DeferredItem<Item> FILTER = ITEMS.registerSimpleItem("filter");
   public static final DeferredItem<Item> MORPHIC_RESONATOR = ITEMS.registerSimpleItem("morphic_resonator");
   public static final DeferredItem<Item> MIRRORED_GLASS = ITEMS.registerSimpleItem("mirrored_glass");
   public static final DeferredItem<Item> VOID_SEED = ITEMS.registerSimpleItem("void_seed", new Item.Properties().rarity(Rarity.UNCOMMON));
   public static final DeferredItem<Item> MIND_CLOCKWORK = ITEMS.registerSimpleItem("mind_clockwork");
   public static final DeferredItem<Item> MIND_BIOTHAUMIC = ITEMS.registerSimpleItem("mind_biothaumic");
   public static final DeferredItem<Item> MODULE_VISION = ITEMS.registerSimpleItem("module_vision");
   public static final DeferredItem<Item> MODULE_AGGRESSION = ITEMS.registerSimpleItem("module_aggression");

   private ModItems() {
   }
}
