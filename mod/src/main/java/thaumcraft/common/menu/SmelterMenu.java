package thaumcraft.common.menu;

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
import thaumcraft.api.aspects.AspectHelper;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.blocks.essentia.SmelterBlockEntity;
import thaumcraft.common.registers.ModMenus;

/** ContainerSmelter: the item to melt (only things with aspects) and the fuel. */
public class SmelterMenu extends AbstractContainerMenu {
   private final Container smelter;
   private final ContainerData data;
   private final Player player;

   public SmelterMenu(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
      this(id, inv, new SimpleContainer(2), new SimpleContainerData(5));
   }

   public SmelterMenu(int id, Inventory inv, Container smelter, ContainerData data) {
      super(ModMenus.SMELTER.get(), id);
      this.smelter = smelter;
      this.data = data;
      this.player = inv.player;
      addSlot(new Slot(smelter, 0, 80, 8) {
         @Override
         public boolean mayPlace(ItemStack stack) {
            return hasAspects(stack);
         }
      });
      addSlot(new Slot(smelter, 1, 80, 48));
      for (int i = 0; i < 3; i++) {
         for (int j = 0; j < 9; j++) addSlot(new Slot(inv, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
      }
      for (int i = 0; i < 9; i++) addSlot(new Slot(inv, i, 8 + i * 18, 142));
      addDataSlots(data);
   }

   private boolean hasAspects(ItemStack stack) {
      AspectList al = AspectHelper.getObjectAspects(player.level(), stack);
      return al != null && al.size() > 0;
   }

   public int getCookProgressScaled(int scale) {
      int smeltTime = Math.max(1, data.get(4));
      return data.get(0) * scale / smeltTime;
   }

   public int getVisScaled(int scale) {
      return data.get(3) * scale / SmelterBlockEntity.MAX_VIS;
   }

   public int getBurnTimeRemainingScaled(int scale) {
      int current = data.get(2);
      if (current == 0) current = 200;
      return data.get(1) * scale / current;
   }

   @Override
   public boolean stillValid(Player player) {
      return smelter.stillValid(player);
   }

   @Override
   public ItemStack quickMoveStack(Player player, int index) {
      ItemStack itemstack = ItemStack.EMPTY;
      Slot slot = slots.get(index);
      if (slot.hasItem()) {
         ItemStack stack = slot.getItem();
         itemstack = stack.copy();
         if (index != 1 && index != 0) {
            if (SmelterBlockEntity.isItemFuel(stack)) {
               if (!moveItemStackTo(stack, 1, 2, false) && !moveItemStackTo(stack, 0, 1, false)) return ItemStack.EMPTY;
            } else if (hasAspects(stack)) {
               if (!moveItemStackTo(stack, 0, 1, false)) return ItemStack.EMPTY;
            } else if (index < 29) {
               if (!moveItemStackTo(stack, 29, 38, false)) return ItemStack.EMPTY;
            } else if (!moveItemStackTo(stack, 2, 29, false)) {
               return ItemStack.EMPTY;
            }
         } else if (!moveItemStackTo(stack, 2, 38, false)) {
            return ItemStack.EMPTY;
         }
         if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
         else slot.setChanged();
         if (stack.getCount() == itemstack.getCount()) return ItemStack.EMPTY;
         slot.onTake(player, stack);
      }
      return itemstack;
   }
}
