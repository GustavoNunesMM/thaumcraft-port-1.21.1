package thaumcraft.common.items.armor;

import net.minecraft.core.Holder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import thaumcraft.api.items.IVisDiscountGear;
import thaumcraft.api.items.IWarpingGear;

/**
 * The Crimson Cult's armor (dropped by cultists, which come with the eldritch milestone): knight plate (ModelKnightArmor),
 * ritualist robes (ModelRobe), the praetor's armor (ModelLeaderArmor) and the cult boots. All repaired with iron.
 */
public final class CultistArmors {
   private CultistArmors() {
   }

   private static Ingredient iron() {
      return Ingredient.of(Items.IRON_INGOT);
   }

   /** ItemCultistPlateArmor. */
   public static class Plate extends TCArmorItem {
      public Plate(Holder<ArmorMaterial> material, Type type, Properties p) {
         super(material, type, p, "cultist_plate_armor", CultistArmors::iron);
      }
   }

   /** ItemCultistRobeArmor: 1% vis discount and one warp. */
   public static class Robe extends TCArmorItem implements IVisDiscountGear, IWarpingGear {
      public Robe(Holder<ArmorMaterial> material, Type type, Properties p) {
         super(material, type, p, "cultist_robe_armor", CultistArmors::iron);
      }

      @Override
      public int getVisDiscount(ItemStack stack, Player player) {
         return 1;
      }

      @Override
      public int getWarp(ItemStack stack, Player player) {
         return 1;
      }
   }

   /** ItemCultistLeaderArmor. */
   public static class Leader extends TCArmorItem {
      public Leader(Holder<ArmorMaterial> material, Type type, Properties p) {
         super(material, type, p, "cultist_leader_armor", CultistArmors::iron);
      }
   }

   /** ItemCultistBoots: iron armor stats, 1% vis discount and one warp. */
   public static class Boots extends TCArmorItem implements IVisDiscountGear, IWarpingGear {
      public Boots(Holder<ArmorMaterial> material, Properties p) {
         super(material, Type.BOOTS, p, "cultistboots", CultistArmors::iron);
      }

      @Override
      public int getVisDiscount(ItemStack stack, Player player) {
         return 1;
      }

      @Override
      public int getWarp(ItemStack stack, Player player) {
         return 1;
      }
   }
}
