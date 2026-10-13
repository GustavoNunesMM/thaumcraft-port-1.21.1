package thaumcraft.common.items.armor;

import net.minecraft.core.Holder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.items.IRechargable;
import thaumcraft.api.items.RechargeHelper;
import thaumcraft.common.registers.ModGear;

/**
 * ItemBootsTraveller: boots of the traveller. While they hold vis charge (one point per minute of wear) they speed the
 * wearer up, raise the step height (GearEvents) and the jump, and soften falls.
 */
public class TravellerBootsItem extends TCArmorItem implements IRechargable {
   public TravellerBootsItem(Holder<ArmorMaterial> material, Properties p) {
      super(material, Type.BOOTS, p, "bootstraveler", () -> Ingredient.of(Items.LEATHER));
   }

   public static boolean isActive(ItemStack boots) {
      return boots.getItem() instanceof TravellerBootsItem && RechargeHelper.getCharge(boots) > 0;
   }

   /** onArmorTick. */
   @Override
   public void inventoryTick(ItemStack stack, Level world, Entity entity, int slot, boolean selected) {
      super.inventoryTick(stack, world, entity, slot, selected);
      if (!(entity instanceof Player player) || player.getItemBySlot(EquipmentSlot.FEET) != stack) return;
      boolean hasCharge = RechargeHelper.getCharge(stack) > 0;
      if (!world.isClientSide && player.tickCount % 20 == 0) {
         int e = stack.getOrDefault(ModGear.ENERGY.get(), 0);
         if (e > 0) {
            e--;
         } else if (RechargeHelper.consumeCharge(stack, player, 1)) {
            e = 60;
         }
         stack.set(ModGear.ENERGY.get(), e);
      }
      if (hasCharge && !player.getAbilities().flying && player.zza > 0.0F) {
         if (player.onGround()) {
            float bonus = 0.05F;
            if (player.isInWater()) bonus /= 4.0F;
            player.moveRelative(bonus, new Vec3(0.0, 0.0, 1.0));
         } else {
            if (player.isInWater()) player.moveRelative(0.025F, new Vec3(0.0, 0.0, 1.0));
            // jumpMovementFactor = 0.05 (the player default is 0.02): the extra air control.
            player.moveRelative(0.03F, new Vec3(player.xxa, 0.0, player.zza));
         }
      }
   }

   @Override
   public int getMaxCharge(ItemStack stack, LivingEntity player) {
      return 240;
   }

   @Override
   public EnumChargeDisplay showInHud(ItemStack stack, LivingEntity player) {
      return EnumChargeDisplay.PERIODIC;
   }
}
