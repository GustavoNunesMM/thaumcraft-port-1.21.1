package thaumcraft.common.blocks.devices;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import thaumcraft.Thaumcraft;

/**
 * ThaumcraftApi.addSmeltingBonus / CommonInternals.smeltingBonus: extra items the infernal furnace may give when
 * smelting something (nuggets from ores and clusters, rare earth...). Items of milestones not ported yet are looked up
 * by id and skipped while missing.
 */
public final class SmeltingBonus {
   public record Bonus(Predicate<ItemStack> in, Supplier<ItemStack> out, float chance) {
   }

   private static final List<Bonus> BONUSES = new ArrayList<>();

   private SmeltingBonus() {
   }

   public static void add(Predicate<ItemStack> in, Supplier<ItemStack> out, float chance) {
      BONUSES.add(new Bonus(in, out, chance));
   }

   private static TagKey<Item> tag(String path) {
      return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", path));
   }

   private static Supplier<ItemStack> tc(String id) {
      return () -> {
         ResourceLocation rl = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, id);
         return BuiltInRegistries.ITEM.containsKey(rl) ? new ItemStack(BuiltInRegistries.ITEM.get(rl)) : ItemStack.EMPTY;
      };
   }

   private static Predicate<ItemStack> tcItem(String id) {
      ResourceLocation rl = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, id);
      return s -> BuiltInRegistries.ITEM.containsKey(rl) && s.is(BuiltInRegistries.ITEM.get(rl));
   }

   /** ConfigRecipes.initializeSmeltingBonuses, with the 1.21 ore tags. */
   public static void registerDefaults() {
      BONUSES.clear();
      Supplier<ItemStack> goldNugget = () -> new ItemStack(Items.GOLD_NUGGET), ironNugget = () -> new ItemStack(Items.IRON_NUGGET);
      add(s -> s.is(tag("ores/gold")), goldNugget, 0.33F);
      add(s -> s.is(tag("ores/iron")), ironNugget, 0.33F);
      add(s -> s.is(tag("ores/cinnabar")), tc("nugget_quicksilver"), 0.33F);
      add(s -> s.is(tag("ores/copper")), tc("nugget_copper"), 0.33F);
      add(s -> s.is(tag("ores/tin")), tc("nugget_tin"), 0.33F);
      add(s -> s.is(tag("ores/silver")), tc("nugget_silver"), 0.33F);
      add(s -> s.is(tag("ores/lead")), tc("nugget_lead"), 0.33F);
      add(s -> s.is(tag("ores/quartz")), tc("nugget_quartz"), 0.33F);
      add(tcItem("cluster_iron"), ironNugget, 0.33F);
      add(tcItem("cluster_gold"), goldNugget, 0.33F);
      add(tcItem("cluster_cinnabar"), tc("nugget_quicksilver"), 0.33F);
      add(tcItem("cluster_copper"), tc("nugget_copper"), 0.33F);
      add(tcItem("cluster_tin"), tc("nugget_tin"), 0.33F);
      add(tcItem("cluster_silver"), tc("nugget_silver"), 0.33F);
      add(tcItem("cluster_lead"), tc("nugget_lead"), 0.33F);
      add(tcItem("cluster_quartz"), tc("nugget_quartz"), 0.33F);
      add(s -> s.is(Items.BEEF), tc("chunk_beef"), 0.33F);
      add(s -> s.is(Items.CHICKEN), tc("chunk_chicken"), 0.33F);
      add(s -> s.is(Items.PORKCHOP), tc("chunk_pork"), 0.33F);
      add(s -> s.is(ItemTags.FISHES), tc("chunk_fish"), 0.33F);
      add(s -> s.is(Items.RABBIT), tc("chunk_rabbit"), 0.33F);
      add(s -> s.is(Items.MUTTON), tc("chunk_mutton"), 0.33F);
      Supplier<ItemStack> rareEarth = tc("rare_earth");
      add(s -> s.is(tag("ores/diamond")), rareEarth, 0.025F);
      add(s -> s.is(tag("ores/redstone")), rareEarth, 0.01F);
      add(s -> s.is(tag("ores/lapis")), rareEarth, 0.01F);
      add(s -> s.is(tag("ores/emerald")), rareEarth, 0.025F);
      add(s -> s.is(tag("ores/gold")), rareEarth, 0.02F);
      add(s -> s.is(tag("ores/iron")), rareEarth, 0.01F);
      add(s -> s.is(tag("ores/cinnabar")), rareEarth, 0.025F);
      add(s -> s.is(tag("ores/copper")), rareEarth, 0.01F);
      add(s -> s.is(tag("ores/tin")), rareEarth, 0.01F);
      add(s -> s.is(tag("ores/silver")), rareEarth, 0.02F);
      add(s -> s.is(tag("ores/lead")), rareEarth, 0.01F);
      add(s -> s.is(tag("ores/quartz")), rareEarth, 0.01F);
      add(s -> BuiltInRegistries.ITEM.getKey(s.getItem()).getNamespace().equals(Thaumcraft.MODID)
         && BuiltInRegistries.ITEM.getKey(s.getItem()).getPath().startsWith("cluster_"), rareEarth, 0.02F);
   }

   /** getSmeltingBonus: the bonuses rolled for one smelted item. */
   public static List<ItemStack> roll(ItemStack in, RandomSource rand) {
      if (BONUSES.isEmpty()) registerDefaults();
      List<ItemStack> out = new ArrayList<>();
      for (Bonus b : BONUSES) {
         if (b.in().test(in) && rand.nextFloat() <= b.chance()) {
            ItemStack is = b.out().get().copy();
            if (!is.isEmpty()) out.add(is);
         }
      }
      return out;
   }
}
