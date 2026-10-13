package thaumcraft.common.menu;

import javax.annotation.Nullable;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import thaumcraft.common.blocks.crafting.VoidSiphonBlockEntity;
import thaumcraft.common.registers.ModFlux;
import thaumcraft.common.registers.ModItems;

/** ContainerVoidSiphon: the output slot (take only) and the player inventory; progress is synced as data slot 0. */
public class VoidSiphonMenu extends AbstractContainerMenu {
   private final Container siphon;
   private final ContainerData data;
   @Nullable
   private final VoidSiphonBlockEntity be;

   public VoidSiphonMenu(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
      this(id, inv, new SimpleContainer(1), new SimpleContainerData(1), null);
   }

   public VoidSiphonMenu(int id, Inventory inv, Container siphon, ContainerData data, @Nullable VoidSiphonBlockEntity be) {
      super(ModFlux.VOID_SIPHON_MENU.get(), id);
      this.siphon = siphon;
      this.data = data;
      this.be = be;
      addSlot(new Slot(siphon, 0, 80, 32) {
         @Override
         public boolean mayPlace(ItemStack stack) {
            return false;
         }
      });
      for (int i = 0; i < 3; i++) {
         for (int j = 0; j < 9; j++) addSlot(new Slot(inv, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
      }
      for (int i = 0; i < 9; i++) addSlot(new Slot(inv, i, 8 + i * 18, 142));
      addDataSlots(data);
   }

   public int getProgress() {
      return data.get(0);
   }

   @Override
   public boolean stillValid(Player player) {
      return be == null || Container.stillValidBlockEntity(be, player);
   }

   @Override
   public ItemStack quickMoveStack(Player player, int index) {
      Slot slot = slots.get(index);
      if (!slot.hasItem()) return ItemStack.EMPTY;
      ItemStack stack = slot.getItem();
      ItemStack copy = stack.copy();
      if (index == 0) {
         if (!stack.is(ModItems.VOID_SEED.get()) || !moveItemStackTo(stack, 1, slots.size(), true)) return ItemStack.EMPTY;
      } else {
         return ItemStack.EMPTY;
      }
      if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
      else slot.setChanged();
      return copy;
   }
}
