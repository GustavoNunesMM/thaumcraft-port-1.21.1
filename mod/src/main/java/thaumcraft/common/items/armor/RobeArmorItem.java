package thaumcraft.common.items.armor;

import net.minecraft.core.Holder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import thaumcraft.api.items.IVisDiscountGear;
import thaumcraft.common.registers.ModItems;

/**
 * ItemRobeArmor: the thaumaturge's robes. Dyeable (minecraft:dyeable tag + dyed_color component, default 0x6A3880; the
 * vanilla armor-dye recipe replaces RecipesRobeArmorDyes, and a water cauldron washes the color off), vis discount 3%
 * (2% for the boots), repaired with fabric.
 */
public class RobeArmorItem extends TCArmorItem implements IVisDiscountGear {
   public static final int DEFAULT_COLOR = 6961280;

   public RobeArmorItem(Holder<ArmorMaterial> material, Type type, Properties p) {
      super(material, type, p, s -> s == EquipmentSlot.LEGS ? "robes_2" : "robes_1", s -> s == EquipmentSlot.LEGS ? "robes_2_overlay" : "robes_1_overlay",
         () -> Ingredient.of(ModItems.FABRIC.get()));
   }

   @Override
   public int getVisDiscount(ItemStack stack, Player player) {
      return getType() == Type.BOOTS ? 2 : 3;
   }
}
