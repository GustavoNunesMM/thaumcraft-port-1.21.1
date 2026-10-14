package thaumcraft.common.menu;

import java.util.function.Consumer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/** Shared bits of the device menus. */
public final class DeviceMenus {
   private DeviceMenus() {
   }

   /** The player's inventory (3 rows at y) and hotbar (at y + 58). */
   public static void addPlayerSlots(Consumer<Slot> add, Inventory inv, int x, int y) {
      for (int i = 0; i < 3; i++) {
         for (int j = 0; j < 9; j++) add.accept(new Slot(inv, j + i * 9 + 9, x + j * 18, y + i * 18));
      }
      for (int i = 0; i < 9; i++) add.accept(new Slot(inv, i, x + i * 18, y + 58));
   }
}
