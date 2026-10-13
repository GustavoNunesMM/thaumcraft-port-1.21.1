package thaumcraft.common.lib.enchantment;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.world.item.ItemStack;
import thaumcraft.common.registers.ModDataComponents;

/**
 * EnumInfusionEnchantment: Thaumcraft's own enchantments, applied by infusion and kept in the
 * thaumcraft:infusion_enchantments component (id -> level), "infench" in 1.12.
 */
public enum EnumInfusionEnchantment {
   COLLECTOR(Set.of("axe", "pickaxe", "shovel", "weapon"), 1, "INFUSIONENCHANTMENT"),
   DESTRUCTIVE(Set.of("axe", "pickaxe", "shovel"), 1, "INFUSIONENCHANTMENT"),
   BURROWING(Set.of("axe", "pickaxe"), 1, "INFUSIONENCHANTMENT"),
   SOUNDING(Set.of("pickaxe"), 4, "INFUSIONENCHANTMENT"),
   REFINING(Set.of("pickaxe"), 4, "INFUSIONENCHANTMENT"),
   ARCING(Set.of("weapon"), 4, "INFUSIONENCHANTMENT"),
   ESSENCE(Set.of("weapon"), 5, "INFUSIONENCHANTMENT"),
   VISBATTERY(Set.of("chargable"), 3, "?"),
   VISCHARGE(Set.of("chargable"), 1, "?"),
   SWIFT(Set.of("boots"), 4, "IEARMOR"),
   AGILE(Set.of("legs"), 1, "IEARMOR"),
   INFESTED(Set.of("chest"), 1, "IETAINT"),
   LAMPLIGHT(Set.of("axe", "pickaxe", "shovel"), 1, "INFUSIONENCHANTMENT");

   public final Set<String> toolClasses;
   public final int maxLevel;
   public final String research;

   EnumInfusionEnchantment(Set<String> toolClasses, int maxLevel, String research) {
      this.toolClasses = toolClasses;
      this.maxLevel = maxLevel;
      this.research = research;
   }

   public String id() {
      return name().toLowerCase();
   }

   public static EnumInfusionEnchantment byId(String id) {
      for (EnumInfusionEnchantment e : values()) {
         if (e.id().equals(id)) return e;
      }
      return null;
   }

   private static Map<String, Integer> map(ItemStack stack) {
      return stack.isEmpty() ? Map.of() : stack.getOrDefault(ModDataComponents.INFUSION_ENCHANTMENTS.get(), Map.of());
   }

   public static List<EnumInfusionEnchantment> getInfusionEnchantments(ItemStack stack) {
      return map(stack).keySet().stream().map(EnumInfusionEnchantment::byId).filter(e -> e != null).toList();
   }

   public static int getInfusionEnchantmentLevel(ItemStack stack, EnumInfusionEnchantment ench) {
      return map(stack).getOrDefault(ench.id(), 0);
   }

   public static void addInfusionEnchantment(ItemStack stack, EnumInfusionEnchantment ench, int level) {
      if (stack.isEmpty() || level > ench.maxLevel) return;
      Map<String, Integer> m = new LinkedHashMap<>(map(stack));
      if (m.getOrDefault(ench.id(), 0) >= level) return;
      m.put(ench.id(), level);
      stack.set(ModDataComponents.INFUSION_ENCHANTMENTS.get(), Map.copyOf(m));
   }

   static Map<String, Integer> copy(ItemStack stack) {
      return new HashMap<>(map(stack));
   }
}
