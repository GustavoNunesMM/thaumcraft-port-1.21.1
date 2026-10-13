package thaumcraft.common.lib;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import thaumcraft.api.research.ResearchItem;

/** The InventoryUtils player helpers the port needs. */
public final class InventoryHelper {
   private InventoryHelper() {
   }

   /** isPlayerCarryingAmount: enough matching items in the main inventory (no creative exception, as in 1.12). */
   public static boolean isCarrying(Player player, ResearchItem item) {
      int need = item.count();
      for (ItemStack s : player.getInventory().items) {
         if (item.matches(s)) need -= s.getCount();
         if (need <= 0) return true;
      }
      return false;
   }

   /** consumePlayerItem: removes the items from the main inventory. */
   public static boolean consume(Player player, ResearchItem item) {
      if (!isCarrying(player, item)) return false;
      int need = item.count();
      var inv = player.getInventory();
      for (int i = 0; i < inv.items.size() && need > 0; i++) {
         ItemStack s = inv.items.get(i);
         if (item.matches(s)) {
            int take = Math.min(need, s.getCount());
            s.shrink(take);
            need -= take;
         }
      }
      inv.setChanged();
      return true;
   }

   /** Puts the stack in the inventory or drops it at the player's feet. */
   public static void giveOrDrop(Player player, ItemStack stack) {
      if (!player.getInventory().add(stack)) player.drop(stack, false);
   }
}
