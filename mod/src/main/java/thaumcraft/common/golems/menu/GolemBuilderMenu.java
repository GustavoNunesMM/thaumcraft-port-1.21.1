package thaumcraft.common.golems.menu;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import thaumcraft.common.golems.GolemBuilderBlockEntity;
import thaumcraft.common.golems.GolemPlacerItem;
import thaumcraft.common.registers.ModGolems;

/** ContainerGolemBuilder: the press's output slot and the cost progress. */
public class GolemBuilderMenu extends AbstractContainerMenu {
   public final GolemBuilderBlockEntity builder;
   /** Set when the inventory changed, so the screen re-checks which components the player has. */
   public static boolean redo = false;

   public GolemBuilderMenu(int id, Inventory inv, GolemBuilderBlockEntity be) {
      super(ModGolems.GOLEM_BUILDER_MENU.get(), id);
      this.builder = be;
      addSlot(new Slot(be, 0, 160, 104) {
         @Override
         public boolean mayPlace(ItemStack stack) {
            return false;
         }
      });
      for (int i = 0; i < 3; i++) {
         for (int j = 0; j < 9; j++) addSlot(new Slot(inv, j + i * 9 + 9, 24 + j * 18, 142 + i * 18));
      }
      for (int i = 0; i < 9; i++) addSlot(new Slot(inv, i, 24 + i * 18, 200));
      addDataSlot(new DataSlot() {
         @Override
         public int get() {
            return be.cost;
         }

         @Override
         public void set(int v) {
            be.cost = v;
         }
      });
      addDataSlot(new DataSlot() {
         @Override
         public int get() {
            return be.maxCost;
         }

         @Override
         public void set(int v) {
            be.maxCost = v;
         }
      });
   }

   public GolemBuilderMenu(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
      this(id, inv, (GolemBuilderBlockEntity) inv.player.level().getBlockEntity(buf.readBlockPos()));
   }

   @Override
   public void clicked(int slotId, int button, ClickType mode, Player player) {
      redo = true;
      super.clicked(slotId, button, mode, player);
   }

   @Override
   public void setItem(int slotId, int stateId, ItemStack stack) {
      redo = true;
      super.setItem(slotId, stateId, stack);
   }

   @Override
   public boolean clickMenuButton(Player p, int button) {
      if (button == 99) redo = true;
      return false;
   }

   @Override
   public boolean stillValid(Player player) {
      return Container.stillValidBlockEntity(builder, player);
   }

   @Override
   public ItemStack quickMoveStack(Player player, int index) {
      ItemStack stack = ItemStack.EMPTY;
      Slot slot = slots.get(index);
      if (slot.hasItem()) {
         ItemStack in = slot.getItem();
         stack = in.copy();
         if (index == 0) {
            if (!(in.getItem() instanceof GolemPlacerItem) || !moveItemStackTo(in, 1, slots.size(), true)) return ItemStack.EMPTY;
         } else {
            return ItemStack.EMPTY;
         }
         if (in.isEmpty()) slot.set(ItemStack.EMPTY);
         else slot.setChanged();
      }
      return stack;
   }
}
