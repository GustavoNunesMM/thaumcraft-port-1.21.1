package thaumcraft.common.lib;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotResult;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;

/** BaublesApi replacement: what an entity wears in its Curios slots. */
public final class CuriosHelper {
   private CuriosHelper() {
   }

   /** Every non-empty curio worn (BaublesApi.getBaubles + loop over the slots in 1.12). */
   public static List<ItemStack> getCurios(LivingEntity entity) {
      List<ItemStack> out = new ArrayList<>();
      if (entity == null) return out;
      Optional<ICuriosItemHandler> inv = CuriosApi.getCuriosInventory(entity);
      if (inv.isEmpty()) return out;
      var handler = inv.get().getEquippedCurios();
      for (int i = 0; i < handler.getSlots(); i++) {
         ItemStack s = handler.getStackInSlot(i);
         if (!s.isEmpty()) out.add(s);
      }
      return out;
   }

   /** BaublesApi.isBaubleEquipped(...) >= 0. */
   public static boolean isEquipped(LivingEntity entity, Item item) {
      return findFirst(entity, item).isPresent();
   }

   public static Optional<SlotResult> findFirst(LivingEntity entity, Item item) {
      if (entity == null) return Optional.empty();
      return CuriosApi.getCuriosInventory(entity).flatMap(h -> h.findFirstCurio(item));
   }

   /** Puts a stack into the first free slot of the given type (tests, commands); false if there is none. */
   public static boolean equip(LivingEntity entity, String slot, ItemStack stack) {
      Optional<ICuriosItemHandler> inv = CuriosApi.getCuriosInventory(entity);
      if (inv.isEmpty()) return false;
      var handler = inv.get().getStacksHandler(slot);
      if (handler.isEmpty()) return false;
      var stacks = handler.get().getStacks();
      for (int i = 0; i < stacks.getSlots(); i++) {
         if (stacks.getStackInSlot(i).isEmpty()) {
            stacks.setStackInSlot(i, stack);
            return true;
         }
      }
      return false;
   }
}
