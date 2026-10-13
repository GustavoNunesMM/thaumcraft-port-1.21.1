package thaumcraft.common.items.armor;

import net.minecraft.core.Holder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import thaumcraft.api.items.IGoggles;
import thaumcraft.api.items.IRevealer;
import thaumcraft.api.items.IVisDiscountGear;
import thaumcraft.common.registers.ModItems;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

/**
 * ItemGoggles: goggles of revealing. A helmet or a head curio (IBauble HEAD in 1.12): 5% vis discount, aspect popups.
 * Repaired with brass.
 */
public class GogglesItem extends TCArmorItem implements IVisDiscountGear, IRevealer, IGoggles, ICurioItem {
   public GogglesItem(Holder<ArmorMaterial> material, Properties p) {
      super(material, Type.HELMET, p, "goggles", () -> Ingredient.of(ModItems.INGOT_BRASS.get()));
   }

   @Override
   public int getVisDiscount(ItemStack stack, Player player) {
      return 5;
   }

   @Override
   public boolean showNodes(ItemStack stack, LivingEntity wearer) {
      return true;
   }

   @Override
   public boolean showIngamePopups(ItemStack stack, LivingEntity wearer) {
      return true;
   }
}
