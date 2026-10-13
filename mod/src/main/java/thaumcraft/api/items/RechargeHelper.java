package thaumcraft.api.items;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import thaumcraft.common.registers.ModGear;
import thaumcraft.common.world.aura.AuraHandler;

/** RechargeHelper: the vis charge of IRechargable items ("tc.charge" in 1.12, the thaumcraft:charge component here). */
public final class RechargeHelper {
   private RechargeHelper() {
   }

   /** Drains up to amt vis from the aura at pos into the item; returns what was added. */
   public static float rechargeItem(Level level, ItemStack is, BlockPos pos, Player player, int amt) {
      if (is.isEmpty() || !(is.getItem() instanceof IRechargable chargeItem)) return 0.0F;
      if (player != null && AuraHandler.shouldPreserveAura(level, player, pos)) return 0.0F;
      amt = Math.min(amt, chargeItem.getMaxCharge(is, player) - getCharge(is));
      if (amt <= 0) return 0.0F;
      int drained = (int) AuraHandler.drainVis(level, pos, amt, false);
      if (drained > 0) {
         addCharge(is, player, drained);
         return drained;
      }
      return 0.0F;
   }

   public static float rechargeItemBlindly(ItemStack is, Player player, int amt) {
      if (is.isEmpty() || !(is.getItem() instanceof IRechargable chargeItem)) return 0.0F;
      amt = Math.min(amt, chargeItem.getMaxCharge(is, player) - getCharge(is));
      if (amt > 0) addCharge(is, player, amt);
      return amt;
   }

   private static void addCharge(ItemStack is, LivingEntity player, int amt) {
      if (is.getItem() instanceof IRechargable chargeItem) {
         is.set(ModGear.CHARGE.get(), Math.min(chargeItem.getMaxCharge(is, player), amt + getCharge(is)));
      }
   }

   /** -1 for items that hold no charge. */
   public static int getCharge(ItemStack is) {
      if (is.isEmpty() || !(is.getItem() instanceof IRechargable)) return -1;
      return is.getOrDefault(ModGear.CHARGE.get(), 0);
   }

   public static float getChargePercentage(ItemStack is, Player player) {
      if (is.isEmpty() || !(is.getItem() instanceof IRechargable r)) return -1.0F;
      return getCharge(is) / (float) r.getMaxCharge(is, player);
   }

   public static boolean consumeCharge(ItemStack is, LivingEntity player, int amt) {
      if (is.isEmpty() || !(is.getItem() instanceof IRechargable)) return false;
      int charge = is.getOrDefault(ModGear.CHARGE.get(), 0);
      if (charge >= amt) {
         is.set(ModGear.CHARGE.get(), charge - amt);
         return true;
      }
      return false;
   }
}
