package thaumcraft.api.items;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/** Items holding a vis charge (the thaumcraft:charge component), refilled by the vis amulet or a recharge pedestal. */
public interface IRechargable {
   int getMaxCharge(ItemStack stack, LivingEntity player);

   EnumChargeDisplay showInHud(ItemStack stack, LivingEntity player);

   enum EnumChargeDisplay {
      NEVER, NORMAL, PERIODIC
   }
}
