package thaumcraft.common.items.armor;

import net.minecraft.core.Holder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import thaumcraft.api.items.IGoggles;
import thaumcraft.api.items.IRevealer;
import thaumcraft.api.items.IVisDiscountGear;
import thaumcraft.api.items.IWarpingGear;
import thaumcraft.common.items.tools.VoidTools;

/**
 * ItemVoidRobeArmor: void robes (ModelRobe). Dyeable like the robes, self-repairing, 5% vis discount, three warp; the
 * hood works as goggles. Its ISpecialArmor magic resistance is applied in GearEvents.
 */
public class VoidRobeArmorItem extends TCArmorItem implements IVisDiscountGear, IGoggles, IRevealer, IWarpingGear {
   public VoidRobeArmorItem(Holder<ArmorMaterial> material, Type type, Properties p) {
      // type == null (the dyed pass) used the overlay texture in 1.12, the plain pass the armor texture.
      super(material, type, p, s -> "void_robe_armor_overlay", s -> "void_robe_armor", null);
   }

   @Override
   public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
      super.inventoryTick(stack, level, entity, slot, selected);
      VoidTools.selfRepair(stack, level, entity);
   }

   @Override
   public boolean showNodes(ItemStack stack, LivingEntity wearer) {
      return getType() == Type.HELMET;
   }

   @Override
   public boolean showIngamePopups(ItemStack stack, LivingEntity wearer) {
      return getType() == Type.HELMET;
   }

   @Override
   public int getVisDiscount(ItemStack stack, Player player) {
      return 5;
   }

   @Override
   public int getWarp(ItemStack stack, Player player) {
      return 3;
   }
}
