package thaumcraft.common.menu;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import thaumcraft.common.blocks.devices.PotionSprayerBlockEntity;
import thaumcraft.common.registers.ModDevices;

/** ContainerPotionSprayer: the potion slot (SlotPotion). */
public class PotionSprayerMenu extends AbstractContainerMenu {
   public final PotionSprayerBlockEntity sprayer;

   public PotionSprayerMenu(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
      this(id, inv, (PotionSprayerBlockEntity) inv.player.level().getBlockEntity(buf.readBlockPos()));
   }

   public PotionSprayerMenu(int id, Inventory inv, PotionSprayerBlockEntity te) {
      super(ModDevices.POTION_SPRAYER_MENU.get(), id);
      this.sprayer = te;
      addSlot(new Slot(te != null ? te : new SimpleContainer(1), 0, 56, 64) {
         @Override
         public boolean mayPlace(ItemStack stack) {
            return PotionSprayerBlockEntity.isValidPotion(stack);
         }

         @Override
         public int getMaxStackSize() {
            return 1;
         }
      });
      DeviceMenus.addPlayerSlots(this::addSlot, inv, 16, 151);
   }

   @Override
   public boolean stillValid(Player player) {
      return sprayer != null && sprayer.stillValid(player);
   }

   @Override
   public ItemStack quickMoveStack(Player player, int index) {
      ItemStack stack = ItemStack.EMPTY;
      Slot slot = slots.get(index);
      if (slot.hasItem()) {
         ItemStack in = slot.getItem();
         stack = in.copy();
         if (index == 0) {
            if (!moveItemStackTo(in, 1, slots.size(), true)) return ItemStack.EMPTY;
         } else if (!PotionSprayerBlockEntity.isValidPotion(in) || !moveItemStackTo(in, 0, 1, false)) {
            return ItemStack.EMPTY;
         }
         if (in.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
         else slot.setChanged();
      }
      return stack;
   }
}
