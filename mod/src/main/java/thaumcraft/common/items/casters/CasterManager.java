package thaumcraft.common.items.casters;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import thaumcraft.api.items.IVisDiscountGear;

/** CasterManager (the parts ported so far). */
public final class CasterManager {
   private CasterManager() {
   }

   /** Total vis discount (0..1) of the armor worn; baubles and vis exhaustion come with those features. */
   public static float getTotalVisDiscount(Player player) {
      if (player == null) return 0.0F;
      int total = 0;
      for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
         ItemStack s = player.getItemBySlot(slot);
         if (s.getItem() instanceof IVisDiscountGear g) total += g.getVisDiscount(s, player);
      }
      return total / 100.0F;
   }
}
