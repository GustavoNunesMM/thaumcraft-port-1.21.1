package thaumcraft.common.menu;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import thaumcraft.common.entities.monster.PechEntity;
import thaumcraft.common.entities.monster.PechTrades;
import thaumcraft.common.registers.ModMobs;
import thaumcraft.common.registers.ModSounds;

/** ContainerPech + InventoryPech: put something the pech values in the left slot and press the button to get its offer. */
public class PechMenu extends AbstractContainerMenu {
   @Nullable
   public final PechEntity pech;
   private final Player player;
   private final Level level;
   public final SimpleContainer inventory = new SimpleContainer(5) {
      @Override
      public boolean canPlaceItem(int index, ItemStack stack) {
         return index == 0;
      }
   };

   public PechMenu(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
      this(id, inv, inv.player.level().getEntity(buf.readVarInt()) instanceof PechEntity p ? p : null);
   }

   public PechMenu(int id, Inventory inv, @Nullable PechEntity pech) {
      super(ModMobs.PECH_MENU.get(), id);
      this.pech = pech;
      this.player = inv.player;
      this.level = inv.player.level();
      if (pech != null) pech.trading = true;
      addSlot(new Slot(inventory, 0, 36, 29));
      for (int i = 0; i < 2; i++) {
         for (int j = 0; j < 2; j++) {
            addSlot(new Slot(inventory, 1 + j + i * 2, 106 + 18 * j, 20 + 18 * i) {
               @Override
               public boolean mayPlace(ItemStack stack) {
                  return false;
               }
            });
         }
      }
      DeviceMenus.addPlayerSlots(this::addSlot, inv, 8, 84);
   }

   /** The trade button is lit (and works) with a valued item in and the output slots empty. */
   public boolean canTrade() {
      if (pech == null || inventory.getItem(0).isEmpty() || !pech.isValued(inventory.getItem(0))) return false;
      for (int a = 1; a < 5; a++) {
         if (!inventory.getItem(a).isEmpty()) return false;
      }
      return true;
   }

   @Override
   public boolean clickMenuButton(Player player, int button) {
      if (button == 0) {
         generateContents();
         return true;
      }
      return super.clickMenuButton(player, button);
   }

   private boolean hasStuffInPack() {
      for (ItemStack stack : pech.loot) {
         if (!stack.isEmpty()) return true;
      }
      return false;
   }

   private void generateContents() {
      if (level.isClientSide || !canTrade()) return;
      var rand = level.random;
      int value = pech.getValue(inventory.getItem(0));
      if (rand.nextInt(100) <= value / 2) {
         pech.setTamed(false);
         pech.playSound(ModSounds.PECH_TRADE.get(), 0.4F, 1.0F);
      }
      if (rand.nextInt(5) == 0) {
         value += rand.nextInt(3);
      } else if (rand.nextBoolean()) {
         value -= rand.nextInt(3);
      }
      List<PechTrades.Trade> pos = PechTrades.trades(pech.getPechType());
      while (value > 0) {
         int am = Math.min(5, Math.max((value + 1) / 2, rand.nextInt(value) + 1));
         value -= am;
         if (am == 1 && rand.nextBoolean() && hasStuffInPack()) {
            List<Integer> loot = new ArrayList<>();
            for (int a = 0; a < pech.loot.size(); a++) {
               if (!pech.loot.get(a).isEmpty()) loot.add(a);
            }
            int r = loot.get(rand.nextInt(loot.size()));
            ItemStack is = pech.loot.get(r).copyWithCount(1);
            addStack(is);
            pech.loot.get(r).shrink(1);
            if (pech.loot.get(r).isEmpty()) pech.loot.set(r, ItemStack.EMPTY);
         } else if (am < 4 || !rand.nextBoolean()) {
            List<PechTrades.Trade> tier = new ArrayList<>();
            for (PechTrades.Trade t : pos) {
               if (t.value() == am) tier.add(t);
            }
            if (tier.isEmpty()) continue;
            ItemStack is = tier.get(rand.nextInt(tier.size())).stack().apply(level.registryAccess()).copy();
            if (is.isEmpty()) continue;
            is.onCraftedBy(level, player, 0);
            addStack(is);
         }
      }
      inventory.removeItem(0, 1);
      broadcastChanges();
   }

   private void addStack(ItemStack s) {
      for (int a = 1; a < 5; a++) {
         ItemStack in = inventory.getItem(a);
         if (in.isEmpty()) {
            inventory.setItem(a, s);
            break;
         }
         if (ItemStack.isSameItemSameComponents(in, s) && in.getCount() + s.getCount() < in.getMaxStackSize()) {
            in.grow(s.getCount());
            // the original went on looking for a slot afterwards and could duplicate the stack; it stops here
            break;
         }
      }
   }

   @Override
   public boolean stillValid(Player player) {
      return pech != null && pech.isAlive() && pech.isTamed() && player.distanceToSqr(pech) < 64.0;
   }

   @Override
   public ItemStack quickMoveStack(Player player, int index) {
      ItemStack itemstack = ItemStack.EMPTY;
      Slot slot = slots.get(index);
      if (slot.hasItem()) {
         ItemStack in = slot.getItem();
         itemstack = in.copy();
         if (index < 5) {
            if (!moveItemStackTo(in, 5, 41, true)) return ItemStack.EMPTY;
         } else if (!moveItemStackTo(in, 0, 1, true)) {
            return ItemStack.EMPTY;
         }
         if (in.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
         else slot.setChanged();
         if (in.getCount() == itemstack.getCount()) return ItemStack.EMPTY;
         slot.onTake(player, in);
      }
      return itemstack;
   }

   @Override
   public void removed(Player player) {
      super.removed(player);
      if (pech != null) pech.trading = false;
      if (!level.isClientSide) {
         for (int a = 0; a < 5; a++) {
            ItemStack itemstack = inventory.removeItemNoUpdate(a);
            if (!itemstack.isEmpty()) {
               ItemEntity ei = player.drop(itemstack, false);
               if (ei != null) ei.getPersistentData().putBoolean(PechEntity.PECH_DROP, true);
            }
         }
      }
   }
}
