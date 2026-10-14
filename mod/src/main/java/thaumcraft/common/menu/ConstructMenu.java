package thaumcraft.common.menu;

import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * The common part of ContainerTurretBasic / ContainerTurretAdvanced / ContainerArcaneBore: one slot that is the
 * construct's main hand (SlotMobEquipment) plus the player's inventory.
 */
public abstract class ConstructMenu<E extends Mob> extends AbstractContainerMenu {
   @Nullable
   public final E entity;

   protected ConstructMenu(@Nullable MenuType<?> type, int id, Inventory inv, @Nullable E entity, int slotX, int slotY, Predicate<ItemStack> valid) {
      super(type, id);
      this.entity = entity;
      addSlot(new Slot(new HandContainer(entity), 0, slotX, slotY) {
         @Override
         public boolean mayPlace(ItemStack stack) {
            return valid.test(stack);
         }
      });
      DeviceMenus.addPlayerSlots(this::addSlot, inv, 8, 84);
   }

   @Override
   public boolean stillValid(Player player) {
      return entity == null || entity.isAlive() && player.distanceToSqr(entity) < 64.0;
   }

   @Override
   public ItemStack quickMoveStack(Player player, int slot) {
      ItemStack stack = ItemStack.EMPTY;
      Slot s = slots.get(slot);
      if (s.hasItem()) {
         ItemStack in = s.getItem();
         stack = in.copy();
         if (slot == 0) {
            if (!moveItemStackTo(in, 1, slots.size(), true)) return ItemStack.EMPTY;
         } else if (!moveItemStackTo(in, 0, 1, false)) {
            return ItemStack.EMPTY;
         }
         if (in.isEmpty()) s.setByPlayer(ItemStack.EMPTY);
         else s.setChanged();
      }
      return stack;
   }

   /** SlotMobEquipment: a one-slot container that is the mob's main hand. */
   static class HandContainer implements Container {
      @Nullable
      private final Mob mob;
      private ItemStack fallback = ItemStack.EMPTY;

      HandContainer(@Nullable Mob mob) {
         this.mob = mob;
      }

      @Override
      public int getContainerSize() {
         return 1;
      }

      @Override
      public boolean isEmpty() {
         return getItem(0).isEmpty();
      }

      @Override
      public ItemStack getItem(int i) {
         return mob != null ? mob.getMainHandItem() : fallback;
      }

      @Override
      public ItemStack removeItem(int i, int amount) {
         ItemStack s = getItem(0);
         if (s.isEmpty()) return ItemStack.EMPTY;
         ItemStack out = s.split(amount);
         if (s.isEmpty()) setItem(0, ItemStack.EMPTY);
         return out;
      }

      @Override
      public ItemStack removeItemNoUpdate(int i) {
         ItemStack s = getItem(0);
         setItem(0, ItemStack.EMPTY);
         return s;
      }

      @Override
      public void setItem(int i, ItemStack stack) {
         if (mob != null) mob.setItemSlot(EquipmentSlot.MAINHAND, stack);
         else fallback = stack;
      }

      @Override
      public void setChanged() {
      }

      @Override
      public boolean stillValid(Player player) {
         return true;
      }

      @Override
      public void clearContent() {
         setItem(0, ItemStack.EMPTY);
      }
   }
}
