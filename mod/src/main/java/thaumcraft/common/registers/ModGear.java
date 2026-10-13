package thaumcraft.common.registers;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Unit;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import thaumcraft.Thaumcraft;
import thaumcraft.common.crafting.InfusionDataRecipe;
import thaumcraft.common.crafting.InfusionRunicAugmentRecipe;
import thaumcraft.common.items.armor.CultistArmors;
import thaumcraft.common.items.armor.FortressArmorItem;
import thaumcraft.common.items.armor.GogglesItem;
import thaumcraft.common.items.armor.RobeArmorItem;
import thaumcraft.common.items.armor.TCArmorItem;
import thaumcraft.common.items.armor.TravellerBootsItem;
import thaumcraft.common.items.armor.VoidArmorItem;
import thaumcraft.common.items.armor.VoidRobeArmorItem;
import thaumcraft.common.items.baubles.AmuletVisItem;
import thaumcraft.common.items.baubles.BaubleItem;
import thaumcraft.common.items.baubles.CloudRingItem;
import thaumcraft.common.items.baubles.VerdantCharmItem;
import thaumcraft.common.items.baubles.VoidseerCharmItem;
import thaumcraft.common.items.tools.CrimsonBladeItem;
import thaumcraft.common.items.tools.ElementalTools;
import thaumcraft.common.items.tools.PrimalCrusherItem;
import thaumcraft.common.items.tools.ResonatorItem;
import thaumcraft.common.items.tools.ThaumiumTools;
import thaumcraft.common.items.tools.VoidTools;

/**
 * Milestone 10: tools, armor and baubles (Curios). Registers into the shared item, component and recipe serializer
 * registers; the armor materials (a registry of their own in 1.21) get their own DeferredRegister.
 */
public final class ModGear {
   public static final DeferredRegister<ArmorMaterial> ARMOR_MATERIALS = DeferredRegister.create(Registries.ARMOR_MATERIAL, Thaumcraft.MODID);
   private static final DeferredRegister.Items ITEMS = ModItems.ITEMS;

   // ------------------------------------------------------------------ data components
   /** Vis charge of IRechargable items ("tc.charge"). */
   public static final Supplier<DataComponentType<Integer>> CHARGE = intComponent("charge");
   /** Runic shielding added by infusion ("TC.RUNIC"). */
   public static final Supplier<DataComponentType<Integer>> RUNIC = intComponent("runic");
   /** Fortress helm mask: 0 grinning devil, 1 angry ghost, 2 sipping fiend ("mask"). */
   public static final Supplier<DataComponentType<Integer>> MASK = intComponent("mask");
   /** Goggles infused into a fortress helm ("goggles"). */
   public static final Supplier<DataComponentType<Unit>> GOGGLES = ModDataComponents.COMPONENTS.registerComponentType("goggles",
      b -> b.persistent(Unit.CODEC).networkSynchronized(StreamCodec.unit(Unit.INSTANCE)));
   /** Verdant charm kind: 1 life, 2 sustain ("type"). */
   public static final Supplier<DataComponentType<Integer>> VERDANT_TYPE = intComponent("verdant_type");
   /** Elemental shovel plane orientation ("or"). */
   public static final Supplier<DataComponentType<Integer>> ORIENTATION = intComponent("orientation");
   /** Traveller boots: seconds left before the next point of charge is spent ("energy"). */
   public static final Supplier<DataComponentType<Integer>> ENERGY = intComponent("energy");

   private static Supplier<DataComponentType<Integer>> intComponent(String name) {
      return ModDataComponents.COMPONENTS.registerComponentType(name,
         b -> b.persistent(com.mojang.serialization.Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));
   }

   // ------------------------------------------------------------------ recipe serializers
   public static final Supplier<RecipeSerializer<InfusionRunicAugmentRecipe>> INFUSION_RUNIC_SERIALIZER =
      ModRecipes.SERIALIZERS.register("infusion_runic_augment", InfusionRunicAugmentRecipe.Serializer::new);
   public static final Supplier<RecipeSerializer<InfusionDataRecipe>> INFUSION_DATA_SERIALIZER =
      ModRecipes.SERIALIZERS.register("infusion_data", InfusionDataRecipe.Serializer::new);

   // ------------------------------------------------------------------ armor materials (ThaumcraftMaterials.ARMORMAT_*)
   /** 1.12 order of the reduction amounts: boots, legs, chest, helmet. */
   private static DeferredHolder<ArmorMaterial, ArmorMaterial> material(String name, int[] def, int ench, Holder<SoundEvent> sound, float toughness,
                                                                        Supplier<Ingredient> repair, boolean dyeable) {
      return ARMOR_MATERIALS.register(name, () -> {
         EnumMap<ArmorItem.Type, Integer> m = new EnumMap<>(ArmorItem.Type.class);
         m.put(ArmorItem.Type.BOOTS, def[0]);
         m.put(ArmorItem.Type.LEGGINGS, def[1]);
         m.put(ArmorItem.Type.CHESTPLATE, def[2]);
         m.put(ArmorItem.Type.HELMET, def[3]);
         m.put(ArmorItem.Type.BODY, def[2]);
         ResourceLocation id = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, name);
         List<ArmorMaterial.Layer> layers = dyeable ? List.of(new ArmorMaterial.Layer(id, "", true), new ArmorMaterial.Layer(id, "_overlay", false))
            : List.of(new ArmorMaterial.Layer(id));
         return new ArmorMaterial(m, ench, sound, repair, layers, toughness, 0.0F);
      });
   }

   private static Supplier<Ingredient> thaumium() {
      return () -> Ingredient.of(ModItems.INGOT_THAUMIUM.get());
   }

   private static Supplier<Ingredient> voidMetal() {
      return () -> Ingredient.of(ModItems.INGOT_VOID.get());
   }

   private static Supplier<Ingredient> iron() {
      return () -> Ingredient.of(net.minecraft.world.item.Items.IRON_INGOT);
   }

   public static final DeferredHolder<ArmorMaterial, ArmorMaterial> ARMORMAT_THAUMIUM = material("thaumium", new int[]{2, 5, 6, 2}, 25,
      SoundEvents.ARMOR_EQUIP_IRON, 1.0F, thaumium(), false);
   public static final DeferredHolder<ArmorMaterial, ArmorMaterial> ARMORMAT_SPECIAL = material("special", new int[]{1, 2, 3, 1}, 25,
      SoundEvents.ARMOR_EQUIP_LEATHER, 1.0F, () -> Ingredient.EMPTY, false);
   /** ARMORMAT_SPECIAL as worn by the dyeable robes (the dyed layer plus the overlay). */
   public static final DeferredHolder<ArmorMaterial, ArmorMaterial> ARMORMAT_ROBE = material("robe", new int[]{1, 2, 3, 1}, 25,
      SoundEvents.ARMOR_EQUIP_LEATHER, 1.0F, () -> Ingredient.EMPTY, true);
   public static final DeferredHolder<ArmorMaterial, ArmorMaterial> ARMORMAT_VOID = material("void", new int[]{3, 6, 8, 3}, 10,
      SoundEvents.ARMOR_EQUIP_CHAIN, 1.0F, voidMetal(), false);
   public static final DeferredHolder<ArmorMaterial, ArmorMaterial> ARMORMAT_VOIDROBE = material("voidrobe", new int[]{4, 7, 9, 4}, 10,
      SoundEvents.ARMOR_EQUIP_LEATHER, 2.0F, voidMetal(), true);
   public static final DeferredHolder<ArmorMaterial, ArmorMaterial> ARMORMAT_FORTRESS = material("fortress", new int[]{3, 6, 7, 3}, 25,
      SoundEvents.ARMOR_EQUIP_IRON, 3.0F, thaumium(), false);
   public static final DeferredHolder<ArmorMaterial, ArmorMaterial> ARMORMAT_CULTIST_PLATE = material("cultist_plate", new int[]{2, 5, 6, 2}, 13,
      SoundEvents.ARMOR_EQUIP_IRON, 0.0F, iron(), false);
   public static final DeferredHolder<ArmorMaterial, ArmorMaterial> ARMORMAT_CULTIST_ROBE = material("cultist_robe", new int[]{2, 4, 5, 2}, 13,
      SoundEvents.ARMOR_EQUIP_CHAIN, 0.0F, iron(), false);
   public static final DeferredHolder<ArmorMaterial, ArmorMaterial> ARMORMAT_CULTIST_LEADER = material("cultist_leader", new int[]{3, 6, 7, 3}, 20,
      SoundEvents.ARMOR_EQUIP_IRON, 1.0F, iron(), false);

   private static Item.Properties armor(ArmorItem.Type type, int factor, Rarity rarity) {
      return new Item.Properties().durability(type.getDurability(factor)).rarity(rarity);
   }

   // ------------------------------------------------------------------ tools
   public static final DeferredItem<Item> THAUMIUM_AXE = ITEMS.register("thaumium_axe", () -> ThaumiumTools.axe(new Item.Properties()));
   public static final DeferredItem<Item> THAUMIUM_SWORD = ITEMS.register("thaumium_sword", () -> ThaumiumTools.sword(new Item.Properties()));
   public static final DeferredItem<Item> THAUMIUM_SHOVEL = ITEMS.register("thaumium_shovel", () -> ThaumiumTools.shovel(new Item.Properties()));
   public static final DeferredItem<Item> THAUMIUM_PICK = ITEMS.register("thaumium_pick", () -> ThaumiumTools.pickaxe(new Item.Properties()));
   public static final DeferredItem<Item> THAUMIUM_HOE = ITEMS.register("thaumium_hoe", () -> ThaumiumTools.hoe(new Item.Properties()));
   public static final DeferredItem<VoidTools.Axe> VOID_AXE = ITEMS.registerItem("void_axe", VoidTools.Axe::new);
   public static final DeferredItem<VoidTools.Sword> VOID_SWORD = ITEMS.registerItem("void_sword", VoidTools.Sword::new);
   public static final DeferredItem<VoidTools.Shovel> VOID_SHOVEL = ITEMS.registerItem("void_shovel", VoidTools.Shovel::new);
   public static final DeferredItem<VoidTools.Pickaxe> VOID_PICK = ITEMS.registerItem("void_pick", VoidTools.Pickaxe::new);
   public static final DeferredItem<VoidTools.Hoe> VOID_HOE = ITEMS.registerItem("void_hoe", VoidTools.Hoe::new);
   public static final DeferredItem<ElementalTools.Axe> ELEMENTAL_AXE = ITEMS.registerItem("elemental_axe", ElementalTools.Axe::new,
      new Item.Properties().rarity(Rarity.RARE));
   public static final DeferredItem<ElementalTools.Sword> ELEMENTAL_SWORD = ITEMS.registerItem("elemental_sword", ElementalTools.Sword::new,
      new Item.Properties().rarity(Rarity.RARE));
   public static final DeferredItem<ElementalTools.Shovel> ELEMENTAL_SHOVEL = ITEMS.registerItem("elemental_shovel", ElementalTools.Shovel::new,
      new Item.Properties().rarity(Rarity.RARE));
   public static final DeferredItem<ElementalTools.Pickaxe> ELEMENTAL_PICK = ITEMS.registerItem("elemental_pick", ElementalTools.Pickaxe::new,
      new Item.Properties().rarity(Rarity.RARE));
   public static final DeferredItem<ElementalTools.Hoe> ELEMENTAL_HOE = ITEMS.registerItem("elemental_hoe", ElementalTools.Hoe::new,
      new Item.Properties().rarity(Rarity.RARE));
   public static final DeferredItem<PrimalCrusherItem> PRIMAL_CRUSHER = ITEMS.registerItem("primal_crusher", PrimalCrusherItem::new);
   public static final DeferredItem<CrimsonBladeItem> CRIMSON_BLADE = ITEMS.registerItem("crimson_blade", CrimsonBladeItem::new,
      new Item.Properties().rarity(Rarity.EPIC));
   public static final DeferredItem<ResonatorItem> RESONATOR = ITEMS.registerItem("resonator", ResonatorItem::new,
      new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));
   /** ItemSanityChecker: shows the warp meter (SanityHud) while held. */
   public static final DeferredItem<Item> SANITY_CHECKER = ITEMS.registerSimpleItem("sanity_checker", new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));

   // ------------------------------------------------------------------ armor
   public static final DeferredItem<GogglesItem> GOGGLES_ITEM = ITEMS.register("goggles",
      () -> new GogglesItem(ARMORMAT_SPECIAL, new Item.Properties().durability(350).rarity(Rarity.RARE)));
   public static final DeferredItem<TCArmorItem> THAUMIUM_HELM = thaumium("thaumium_helm", ArmorItem.Type.HELMET);
   public static final DeferredItem<TCArmorItem> THAUMIUM_CHEST = thaumium("thaumium_chest", ArmorItem.Type.CHESTPLATE);
   public static final DeferredItem<TCArmorItem> THAUMIUM_LEGS = thaumium("thaumium_legs", ArmorItem.Type.LEGGINGS);
   public static final DeferredItem<TCArmorItem> THAUMIUM_BOOTS = thaumium("thaumium_boots", ArmorItem.Type.BOOTS);
   public static final DeferredItem<RobeArmorItem> CLOTH_CHEST = robe("cloth_chest", ArmorItem.Type.CHESTPLATE);
   public static final DeferredItem<RobeArmorItem> CLOTH_LEGS = robe("cloth_legs", ArmorItem.Type.LEGGINGS);
   public static final DeferredItem<RobeArmorItem> CLOTH_BOOTS = robe("cloth_boots", ArmorItem.Type.BOOTS);
   public static final DeferredItem<TravellerBootsItem> TRAVELLER_BOOTS = ITEMS.register("traveller_boots",
      () -> new TravellerBootsItem(ARMORMAT_SPECIAL, new Item.Properties().durability(350).rarity(Rarity.RARE)));
   public static final DeferredItem<FortressArmorItem> FORTRESS_HELM = fortress("fortress_helm", ArmorItem.Type.HELMET);
   public static final DeferredItem<FortressArmorItem> FORTRESS_CHEST = fortress("fortress_chest", ArmorItem.Type.CHESTPLATE);
   public static final DeferredItem<FortressArmorItem> FORTRESS_LEGS = fortress("fortress_legs", ArmorItem.Type.LEGGINGS);
   public static final DeferredItem<VoidArmorItem> VOID_HELM = voidArmor("void_helm", ArmorItem.Type.HELMET);
   public static final DeferredItem<VoidArmorItem> VOID_CHEST = voidArmor("void_chest", ArmorItem.Type.CHESTPLATE);
   public static final DeferredItem<VoidArmorItem> VOID_LEGS = voidArmor("void_legs", ArmorItem.Type.LEGGINGS);
   public static final DeferredItem<VoidArmorItem> VOID_BOOTS = voidArmor("void_boots", ArmorItem.Type.BOOTS);
   public static final DeferredItem<VoidRobeArmorItem> VOID_ROBE_HELM = voidRobe("void_robe_helm", ArmorItem.Type.HELMET);
   public static final DeferredItem<VoidRobeArmorItem> VOID_ROBE_CHEST = voidRobe("void_robe_chest", ArmorItem.Type.CHESTPLATE);
   public static final DeferredItem<VoidRobeArmorItem> VOID_ROBE_LEGS = voidRobe("void_robe_legs", ArmorItem.Type.LEGGINGS);
   public static final DeferredItem<CultistArmors.Plate> CRIMSON_PLATE_HELM = plate("crimson_plate_helm", ArmorItem.Type.HELMET);
   public static final DeferredItem<CultistArmors.Plate> CRIMSON_PLATE_CHEST = plate("crimson_plate_chest", ArmorItem.Type.CHESTPLATE);
   public static final DeferredItem<CultistArmors.Plate> CRIMSON_PLATE_LEGS = plate("crimson_plate_legs", ArmorItem.Type.LEGGINGS);
   public static final DeferredItem<CultistArmors.Boots> CRIMSON_BOOTS = ITEMS.register("crimson_boots",
      () -> new CultistArmors.Boots(ArmorMaterials.IRON, armor(ArmorItem.Type.BOOTS, 15, Rarity.UNCOMMON)));
   public static final DeferredItem<CultistArmors.Robe> CRIMSON_ROBE_HELM = cultistRobe("crimson_robe_helm", ArmorItem.Type.HELMET);
   public static final DeferredItem<CultistArmors.Robe> CRIMSON_ROBE_CHEST = cultistRobe("crimson_robe_chest", ArmorItem.Type.CHESTPLATE);
   public static final DeferredItem<CultistArmors.Robe> CRIMSON_ROBE_LEGS = cultistRobe("crimson_robe_legs", ArmorItem.Type.LEGGINGS);
   public static final DeferredItem<CultistArmors.Leader> CRIMSON_PRAETOR_HELM = leader("crimson_praetor_helm", ArmorItem.Type.HELMET);
   public static final DeferredItem<CultistArmors.Leader> CRIMSON_PRAETOR_CHEST = leader("crimson_praetor_chest", ArmorItem.Type.CHESTPLATE);
   public static final DeferredItem<CultistArmors.Leader> CRIMSON_PRAETOR_LEGS = leader("crimson_praetor_legs", ArmorItem.Type.LEGGINGS);

   private static DeferredItem<TCArmorItem> thaumium(String name, ArmorItem.Type type) {
      return ITEMS.register(name, () -> new TCArmorItem(ARMORMAT_THAUMIUM, type, armor(type, 25, Rarity.UNCOMMON),
         s -> s == net.minecraft.world.entity.EquipmentSlot.LEGS ? "thaumium_2" : "thaumium_1", null, null));
   }

   private static DeferredItem<RobeArmorItem> robe(String name, ArmorItem.Type type) {
      return ITEMS.register(name, () -> new RobeArmorItem(ARMORMAT_ROBE, type, armor(type, 25, Rarity.UNCOMMON)));
   }

   private static DeferredItem<FortressArmorItem> fortress(String name, ArmorItem.Type type) {
      return ITEMS.register(name, () -> new FortressArmorItem(ARMORMAT_FORTRESS, type, armor(type, 40, Rarity.RARE)));
   }

   private static DeferredItem<VoidArmorItem> voidArmor(String name, ArmorItem.Type type) {
      return ITEMS.register(name, () -> new VoidArmorItem(ARMORMAT_VOID, type, armor(type, 10, Rarity.UNCOMMON)));
   }

   private static DeferredItem<VoidRobeArmorItem> voidRobe(String name, ArmorItem.Type type) {
      return ITEMS.register(name, () -> new VoidRobeArmorItem(ARMORMAT_VOIDROBE, type, armor(type, 18, Rarity.EPIC)));
   }

   private static DeferredItem<CultistArmors.Plate> plate(String name, ArmorItem.Type type) {
      return ITEMS.register(name, () -> new CultistArmors.Plate(ARMORMAT_CULTIST_PLATE, type, armor(type, 18, Rarity.UNCOMMON)));
   }

   private static DeferredItem<CultistArmors.Robe> cultistRobe(String name, ArmorItem.Type type) {
      return ITEMS.register(name, () -> new CultistArmors.Robe(ARMORMAT_CULTIST_ROBE, type, armor(type, 17, Rarity.UNCOMMON)));
   }

   private static DeferredItem<CultistArmors.Leader> leader(String name, ArmorItem.Type type) {
      return ITEMS.register(name, () -> new CultistArmors.Leader(ARMORMAT_CULTIST_LEADER, type, armor(type, 30, Rarity.RARE)));
   }

   // ------------------------------------------------------------------ baubles (curios)
   public static final List<DeferredItem<BaubleItem>> BAUBLES = new ArrayList<>();
   public static final DeferredItem<BaubleItem> AMULET_MUNDANE = bauble("amulet_mundane", Rarity.COMMON, 0);
   public static final DeferredItem<BaubleItem> RING_MUNDANE = bauble("ring_mundane", Rarity.COMMON, 0);
   public static final DeferredItem<BaubleItem> GIRDLE_MUNDANE = bauble("girdle_mundane", Rarity.COMMON, 0);
   public static final DeferredItem<BaubleItem> RING_APPRENTICE = bauble("ring_apprentice", Rarity.UNCOMMON, 5);
   public static final DeferredItem<BaubleItem> AMULET_FANCY = bauble("amulet_fancy", Rarity.UNCOMMON, 0);
   public static final DeferredItem<BaubleItem> RING_FANCY = bauble("ring_fancy", Rarity.UNCOMMON, 0);
   public static final DeferredItem<BaubleItem> GIRDLE_FANCY = bauble("girdle_fancy", Rarity.UNCOMMON, 0);
   public static final DeferredItem<AmuletVisItem> AMULET_VIS_FOUND = ITEMS.register("amulet_vis_found",
      () -> new AmuletVisItem(new Item.Properties().rarity(Rarity.UNCOMMON), false));
   public static final DeferredItem<AmuletVisItem> AMULET_VIS_CRAFTED = ITEMS.register("amulet_vis_crafted",
      () -> new AmuletVisItem(new Item.Properties().rarity(Rarity.RARE), true));
   public static final DeferredItem<VerdantCharmItem> VERDANT_CHARM = ITEMS.registerItem("verdant_charm", VerdantCharmItem::new,
      new Item.Properties().rarity(Rarity.RARE));
   /** ItemCuriosityBand: half of every experience orb becomes knowledge now and then (GearEvents). */
   public static final DeferredItem<BaubleItem> CURIOSITY_BAND = ITEMS.register("curiosity_band",
      () -> new BaubleItem(new Item.Properties().rarity(Rarity.RARE), 0));
   public static final DeferredItem<VoidseerCharmItem> VOIDSEER_CHARM = ITEMS.registerItem("voidseer_charm", VoidseerCharmItem::new,
      new Item.Properties().rarity(Rarity.RARE));
   public static final DeferredItem<CloudRingItem> CLOUD_RING = ITEMS.registerItem("cloud_ring", CloudRingItem::new,
      new Item.Properties().rarity(Rarity.RARE));
   /** ItemCharmUndying: a totem of undying worn as a charm (GearEvents.onDeath). */
   public static final DeferredItem<BaubleItem> CHARM_UNDYING = ITEMS.register("charm_undying",
      () -> new BaubleItem(new Item.Properties().rarity(Rarity.RARE), 0));

   private static DeferredItem<BaubleItem> bauble(String name, Rarity rarity, int discount) {
      DeferredItem<BaubleItem> d = ITEMS.register(name, () -> new BaubleItem(new Item.Properties().rarity(rarity), discount));
      BAUBLES.add(d);
      return d;
   }

   private ModGear() {
   }

   /** Called from the Thaumcraft constructor: loads the holders above and registers the armor materials. */
   public static void init(IEventBus modBus) {
      ARMOR_MATERIALS.register(modBus);
   }
}
