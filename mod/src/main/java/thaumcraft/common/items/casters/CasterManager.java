package thaumcraft.common.items.casters;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import thaumcraft.api.items.IVisDiscountGear;

/** CasterManager (the parts ported so far). */
public final class CasterManager {
   private CasterManager() {
   }

   /**
    * Total vis discount (fraction) of the curios and armor worn, less 10% per level of vis exhaustion (the
    * thaumcraft:vis_exhaust / infectious_vis_exhaust effects, looked up by id until the warp milestone registers them).
    */
   public static float getTotalVisDiscount(Player player) {
      if (player == null) return 0.0F;
      int total = 0;
      for (ItemStack s : thaumcraft.common.lib.CuriosHelper.getCurios(player)) {
         if (s.getItem() instanceof IVisDiscountGear g) total += g.getVisDiscount(s, player);
      }
      for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
         ItemStack s = player.getItemBySlot(slot);
         if (s.getItem() instanceof IVisDiscountGear g) total += g.getVisDiscount(s, player);
      }
      int exhaust = -1;
      for (String id : new String[]{"vis_exhaust", "infectious_vis_exhaust"}) {
         var effect = net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.getHolder(
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(thaumcraft.Thaumcraft.MODID, id));
         if (effect.isPresent() && player.hasEffect(effect.get())) exhaust = Math.max(exhaust, player.getEffect(effect.get()).getAmplifier());
      }
      if (exhaust >= 0) total -= (exhaust + 1) * 10;
      return total / 100.0F;
   }
}
