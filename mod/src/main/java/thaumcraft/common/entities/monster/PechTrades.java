package thaumcraft.common.entities.monster;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.enchantment.Enchantments;
import thaumcraft.Thaumcraft;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.items.VisCrystalItem;
import thaumcraft.common.registers.ModBlocks;
import thaumcraft.common.registers.ModGear;
import thaumcraft.common.registers.ModItems;
import thaumcraft.common.registers.ModMobs;

/** EntityPech.valuedItems / tradeInventory (ConfigEntities.initEntities): what pechs want and what they give, by value tier 1-5. */
public final class PechTrades {
   public record Trade(int value, Function<HolderLookup.Provider, ItemStack> stack) {
   }

   /** Items a pech values above their desire aspects. */
   private static final Map<Item, Integer> VALUED_ITEMS = new HashMap<>();
   private static final Map<Integer, List<Trade>> TRADES = new HashMap<>();

   private PechTrades() {
   }

   private static Trade t(int v, Item item) {
      return new Trade(v, r -> new ItemStack(item));
   }

   private static Trade tc(int v, String id) {
      return new Trade(v, r -> {
         ResourceLocation rl = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, id);
         return BuiltInRegistries.ITEM.containsKey(rl) ? new ItemStack(BuiltInRegistries.ITEM.get(rl)) : ItemStack.EMPTY;
      });
   }

   private static Trade crystal(int v, Aspect a) {
      return new Trade(v, r -> VisCrystalItem.of(a, 1));
   }

   private static Trade book(int v, ResourceKey<Enchantment> e) {
      return new Trade(v, r -> EnchantedBookItem.createForEnchantment(new EnchantmentInstance(r.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(e), 1)));
   }

   private static Trade potion(int v, net.minecraft.core.Holder<net.minecraft.world.item.alchemy.Potion> p) {
      return new Trade(v, r -> PotionContents.createItemStack(Items.POTION, p));
   }

   public static synchronized Map<Item, Integer> valuedItems() {
      if (TRADES.isEmpty()) init();
      return VALUED_ITEMS;
   }

   public static synchronized List<Trade> trades(int pechType) {
      if (TRADES.isEmpty()) init();
      return TRADES.getOrDefault(pechType, TRADES.get(0));
   }

   private static void init() {
      VALUED_ITEMS.put(Items.ENDER_PEARL, 15);
      List<Trade> forInv = new ArrayList<>();
      forInv.add(t(1, ModItems.CLUSTER_IRON.get()));
      forInv.add(t(1, ModItems.CLUSTER_GOLD.get()));
      forInv.add(t(1, ModItems.CLUSTER_CINNABAR.get()));
      forInv.add(t(1, ModItems.CLUSTER_QUARTZ.get()));
      forInv.add(t(1, ModItems.CLUSTER_COPPER.get()));
      forInv.add(t(2, Items.BLAZE_ROD));
      forInv.add(t(2, ModBlocks.SAPLING_GREATWOOD.get().asItem()));
      forInv.add(t(2, Items.DRAGON_BREATH));
      forInv.add(t(3, Items.EXPERIENCE_BOTTLE));
      forInv.add(t(3, Items.EXPERIENCE_BOTTLE));
      forInv.add(t(3, Items.GOLDEN_APPLE));
      forInv.add(t(4, ModGear.THAUMIUM_PICK.get()));
      forInv.add(t(4, ModGear.THAUMIUM_AXE.get()));
      forInv.add(t(4, ModGear.THAUMIUM_HOE.get()));
      forInv.add(t(5, Items.ENCHANTED_GOLDEN_APPLE));
      forInv.add(t(5, ModBlocks.SAPLING_SILVERWOOD.get().asItem()));
      forInv.add(tc(5, "curio_knowledge"));
      // The original added these three to the forager's list while building the other lists.
      forInv.add(t(5, Items.TOTEM_OF_UNDYING));
      forInv.add(t(2, Items.COMPASS));
      forInv.add(t(4, Items.SPECTRAL_ARROW));
      TRADES.put(0, forInv);

      List<Trade> forMag = new ArrayList<>();
      for (Aspect a : new Aspect[]{Aspect.AIR, Aspect.EARTH, Aspect.FIRE, Aspect.WATER, Aspect.ORDER, Aspect.ENTROPY}) forMag.add(crystal(1, a));
      forMag.add(potion(2, Potions.REGENERATION));
      forMag.add(potion(2, Potions.HEALING));
      forMag.add(crystal(2, Aspect.FLUX));
      forMag.add(t(3, Items.EXPERIENCE_BOTTLE));
      forMag.add(t(3, Items.EXPERIENCE_BOTTLE));
      forMag.add(crystal(3, Aspect.AURA));
      forMag.add(t(3, Items.GOLDEN_APPLE));
      forMag.add(t(4, ModGear.CLOTH_BOOTS.get()));
      forMag.add(t(4, ModGear.CLOTH_CHEST.get()));
      forMag.add(t(4, ModGear.CLOTH_LEGS.get()));
      forMag.add(t(5, Items.ENCHANTED_GOLDEN_APPLE));
      forMag.add(new Trade(5, r -> new ItemStack(ModMobs.PECH_WAND.get())));
      forMag.add(tc(5, "curio_knowledge"));
      forMag.add(t(5, ModGear.AMULET_VIS_FOUND.get()));
      TRADES.put(1, forMag);

      List<Trade> forArc = new ArrayList<>();
      for (int a = 0; a < 15; a++) forArc.add(t(1, ModBlocks.CANDLES.get(DyeColor.byId(15 - a)).get().asItem()));
      forArc.add(t(2, Items.GHAST_TEAR));
      forArc.add(book(2, Enchantments.POWER));
      forArc.add(t(3, Items.EXPERIENCE_BOTTLE));
      forArc.add(t(3, Items.EXPERIENCE_BOTTLE));
      forArc.add(t(3, Items.GOLDEN_APPLE));
      // STUB: the eldritch eye belongs to the eldritch milestone; it is skipped while it does not exist.
      forArc.add(tc(4, "eldritch_eye"));
      forArc.add(t(4, Items.ENCHANTED_GOLDEN_APPLE));
      forArc.add(t(5, ModGear.RING_APPRENTICE.get()));
      forArc.add(book(5, Enchantments.FLAME));
      forArc.add(book(5, Enchantments.INFINITY));
      forArc.add(tc(5, "curio_knowledge"));
      TRADES.put(2, forArc);
   }
}
