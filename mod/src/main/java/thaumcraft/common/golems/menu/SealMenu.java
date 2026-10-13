package thaumcraft.common.golems.menu;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import thaumcraft.api.golems.seals.ISealConfigArea;
import thaumcraft.api.golems.seals.ISealConfigFilter;
import thaumcraft.api.golems.seals.ISealConfigToggles;
import thaumcraft.api.golems.seals.ISealEntity;
import thaumcraft.api.golems.seals.ISealGui;
import thaumcraft.api.golems.seals.SealPos;
import thaumcraft.common.golems.seals.SealEntity;
import thaumcraft.common.golems.seals.SealHandler;
import thaumcraft.common.registers.ModGolems;

/**
 * SealBaseContainer: the settings of one seal, by category (priority/lock, filter, area, toggles, requirements).
 * The server applies every change; the client copy of the seal follows through the data slots and the filter slots.
 */
public class SealMenu extends AbstractContainerMenu {
   public final ISealEntity seal;
   public final int[] categories;
   public int category;
   private final int filterSlots;

   public SealMenu(int id, Inventory inv, ISealEntity seal) {
      super(ModGolems.SEAL_MENU.get(), id);
      this.seal = seal;
      this.categories = seal.getSeal() instanceof ISealGui g ? g.getGuiCategories() : new int[]{ISealGui.CAT_PRIORITY};
      this.category = categories[0];
      this.filterSlots = seal.getSeal() instanceof ISealConfigFilter f ? f.getFilterSize() : 0;
      if (seal.getSeal() instanceof ISealConfigFilter f) {
         int s = f.getFilterSize();
         int sx = 16 + (s - 1) % 3 * 12;
         int sy = 16 + (s - 1) / 3 * 12;
         Container temp = new FilterContainer(f);
         for (int a = 0; a < s; a++) {
            addSlot(new GhostSlot(temp, a, 88 + a % 3 * 24 - sx + 8, 72 + a / 3 * 24 - sy + 8));
         }
      }
      for (int i = 0; i < 3; i++) {
         for (int j = 0; j < 9; j++) addSlot(new Slot(inv, j + i * 9 + 9, 8 + j * 18, 150 + i * 18));
      }
      for (int i = 0; i < 9; i++) addSlot(new Slot(inv, i, 8 + i * 18, 208));
      addDataSlot(data(() -> seal.getPriority(), v -> seal.setPriority((byte) v)));
      addDataSlot(data(() -> seal.getArea().getX(), v -> seal.setArea(new BlockPos(v, seal.getArea().getY(), seal.getArea().getZ()))));
      addDataSlot(data(() -> seal.getArea().getY(), v -> seal.setArea(new BlockPos(seal.getArea().getX(), v, seal.getArea().getZ()))));
      addDataSlot(data(() -> seal.getArea().getZ(), v -> seal.setArea(new BlockPos(seal.getArea().getX(), seal.getArea().getY(), v))));
      addDataSlot(data(() -> seal.getColor(), v -> seal.setColor((byte) v)));
      addDataSlot(data(this::getFlags, this::setFlags));
      addDataSlot(data(this::getToggleBits, this::setToggleBits));
      if (seal.getSeal() instanceof ISealConfigFilter f) {
         for (int a = 0; a < f.getFilterSize(); a++) {
            int idx = a;
            addDataSlot(data(() -> f.getFilterSlotSize(idx), v -> f.setFilterSlotSize(idx, v)));
         }
      }
   }

   public SealMenu(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
      this(id, inv, readSeal(inv.player, buf));
   }

   private static ISealEntity readSeal(Player player, RegistryFriendlyByteBuf buf) {
      BlockPos pos = buf.readBlockPos();
      Direction face = Direction.from3DDataValue(buf.readByte());
      CompoundTag tag = buf.readNbt();
      SealEntity se = new SealEntity();
      if (tag != null) se.readNBT(tag, player.registryAccess());
      if (se.getSealPos() == null) return new SealEntity(new SealPos(pos, face), null);
      return se;
   }

   private static DataSlot data(java.util.function.IntSupplier get, java.util.function.IntConsumer set) {
      return new DataSlot() {
         @Override
         public int get() {
            return get.getAsInt();
         }

         @Override
         public void set(int value) {
            set.accept(value);
         }
      };
   }

   private int getFlags() {
      int f = 0;
      if (seal.getSeal() instanceof ISealConfigFilter cf && cf.isBlacklist()) f |= 1;
      if (seal.isLocked()) f |= 2;
      if (seal.isRedstoneSensitive()) f |= 4;
      return f;
   }

   private void setFlags(int f) {
      if (seal.getSeal() instanceof ISealConfigFilter cf) cf.setBlacklist((f & 1) != 0);
      seal.setLocked((f & 2) != 0);
      seal.setRedstoneSensitive((f & 4) != 0);
   }

   private int getToggleBits() {
      int b = 0;
      if (seal.getSeal() instanceof ISealConfigToggles t) {
         for (int i = 0; i < t.getToggles().length; i++) {
            if (t.getToggles()[i].getValue()) b |= 1 << i;
         }
      }
      return b;
   }

   private void setToggleBits(int b) {
      if (seal.getSeal() instanceof ISealConfigToggles t) {
         for (int i = 0; i < t.getToggles().length; i++) t.setToggle(i, (b & 1 << i) != 0);
      }
   }

   public int getFilterSlotCount() {
      return filterSlots;
   }

   private boolean isOwner(Player player) {
      return seal.getOwner().equals(player.getUUID().toString());
   }

   /** Called on both sides for category switches; settings change on the server only (they sync back). */
   @Override
   public boolean clickMenuButton(Player player, int id) {
      if (id >= 0 && id < categories.length) {
         category = categories[id];
         return true;
      }
      if (player.level().isClientSide) return true;
      boolean changed = apply(player, id);
      if (changed && player.level() instanceof ServerLevel sl) {
         seal.syncToClient(sl);
         SealHandler.markDirty(sl);
      }
      return changed;
   }

   private boolean apply(Player player, int id) {
      if (category == ISealGui.CAT_TOGGLES && seal.getSeal() instanceof ISealConfigToggles cp) {
         int n = cp.getToggles().length;
         if (id >= 30 && id < 30 + n) {
            cp.setToggle(id - 30, true);
            return true;
         }
         if (id >= 60 && id < 60 + n) {
            cp.setToggle(id - 60, false);
            return true;
         }
      }
      if (category == ISealGui.CAT_PRIORITY && (id == 25 || id == 26) && isOwner(player)) {
         seal.setLocked(id == 25);
         return true;
      }
      if ((id == 27 || id == 28) && isOwner(player)) {
         seal.setRedstoneSensitive(id == 27);
         return true;
      }
      if (category == ISealGui.CAT_FILTER && seal.getSeal() instanceof ISealConfigFilter cp && (id == 20 || id == 21)) {
         cp.setBlacklist(id == 20);
         return true;
      }
      if (id == 80 && seal.getPriority() > -5) {
         seal.setPriority((byte) (seal.getPriority() - 1));
         return true;
      }
      if (id == 81 && seal.getPriority() < 5) {
         seal.setPriority((byte) (seal.getPriority() + 1));
         return true;
      }
      if (id == 82 && seal.getColor() > 0) {
         seal.setColor((byte) (seal.getColor() - 1));
         return true;
      }
      if (id == 83 && seal.getColor() < 16) {
         seal.setColor((byte) (seal.getColor() + 1));
         return true;
      }
      if (seal.getSeal() instanceof ISealConfigArea) {
         BlockPos a = seal.getArea();
         if (id == 90 && a.getY() > 1) seal.setArea(a.offset(0, -1, 0));
         else if (id == 91 && a.getY() < 8) seal.setArea(a.offset(0, 1, 0));
         else if (id == 92 && a.getX() > 1) seal.setArea(a.offset(-1, 0, 0));
         else if (id == 93 && a.getX() < 8) seal.setArea(a.offset(1, 0, 0));
         else if (id == 94 && a.getZ() > 1) seal.setArea(a.offset(0, 0, -1));
         else if (id == 95 && a.getZ() < 8) seal.setArea(a.offset(0, 0, 1));
         else return false;
         return true;
      }
      return false;
   }

   /** The ghost filter slots: clicks set the filter item or (with size limiters) its count. */
   @Override
   public void clicked(int slotId, int button, ClickType mode, Player player) {
      if (slotId >= 0 && slotId < slots.size() && slots.get(slotId) instanceof GhostSlot slot && seal.getSeal() instanceof ISealConfigFilter f) {
         if (player.level().isClientSide) return;
         int n = slot.getContainerSlot();
         ItemStack ic = getCarried().isEmpty() ? ItemStack.EMPTY : getCarried().copy();
         boolean sizes = f.hasStacksizeLimiters();
         if (button == 1) {
            if (!sizes) {
               slot.set(ItemStack.EMPTY);
               f.setFilterSlotSize(n, 0);
            } else if (ic.isEmpty()) {
               if (slot.hasItem()) {
                  f.setFilterSlotSize(n, f.getFilterSlotSize(n) - (mode == ClickType.QUICK_MOVE ? 10 : 1));
                  if (f.getFilterSlotSize(n) < 0) {
                     slot.set(ItemStack.EMPTY);
                     f.setFilterSlotSize(n, 0);
                  }
               }
            } else if (slot.hasItem() && ItemStack.isSameItemSameComponents(ic, slot.getItem())) {
               f.setFilterSlotSize(n, f.getFilterSlotSize(n) - ic.getCount());
               if (f.getFilterSlotSize(n) < 0) {
                  slot.set(ItemStack.EMPTY);
                  f.setFilterSlotSize(n, 0);
               }
            }
         } else if (ic.isEmpty()) {
            if (sizes && slot.hasItem()) f.setFilterSlotSize(n, f.getFilterSlotSize(n) + (mode == ClickType.QUICK_MOVE ? 10 : 1));
         } else {
            if (!sizes) {
               ic.setCount(1);
               f.setFilterSlotSize(n, 0);
            } else {
               int os = ic.getCount();
               ic.setCount(1);
               if (slot.hasItem() && ItemStack.isSameItemSameComponents(ic, slot.getItem())) {
                  f.setFilterSlotSize(n, f.getFilterSlotSize(n) + os);
               } else {
                  f.setFilterSlotSize(n, 0);
               }
            }
            slot.set(ic);
         }
         if (player.level() instanceof ServerLevel sl) {
            seal.syncToClient(sl);
            SealHandler.markDirty(sl);
         }
         broadcastChanges();
         return;
      }
      super.clicked(slotId, button, mode, player);
   }

   @Override
   public ItemStack quickMoveStack(Player player, int index) {
      return ItemStack.EMPTY;
   }

   @Override
   public boolean stillValid(Player player) {
      return true;
   }

   @Override
   public void removed(Player player) {
      super.removed(player);
      if (player.level() instanceof ServerLevel sl && seal.getSeal() != null) {
         seal.syncToClient(sl);
         SealHandler.markDirty(sl);
      }
   }

   /** The filter list seen as a container. */
   static class FilterContainer implements Container {
      private final ISealConfigFilter f;

      FilterContainer(ISealConfigFilter f) {
         this.f = f;
      }

      @Override
      public int getContainerSize() {
         return f.getFilterSize();
      }

      @Override
      public boolean isEmpty() {
         return f.getInv().stream().allMatch(ItemStack::isEmpty);
      }

      @Override
      public ItemStack getItem(int slot) {
         return f.getFilterSlot(slot);
      }

      @Override
      public ItemStack removeItem(int slot, int amount) {
         return ItemStack.EMPTY;
      }

      @Override
      public ItemStack removeItemNoUpdate(int slot) {
         return ItemStack.EMPTY;
      }

      @Override
      public void setItem(int slot, ItemStack stack) {
         f.setFilterSlot(slot, stack);
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
         for (int i = 0; i < getContainerSize(); i++) setItem(i, ItemStack.EMPTY);
      }
   }

   /** SlotGhost: shows the filter item; only visible on the filter page. */
   public class GhostSlot extends Slot {
      GhostSlot(Container c, int index, int x, int y) {
         super(c, index, x, y);
      }

      @Override
      public boolean isActive() {
         return category == ISealGui.CAT_FILTER;
      }

      @Override
      public boolean mayPickup(Player player) {
         return false;
      }

      @Override
      public boolean mayPlace(ItemStack stack) {
         return false;
      }
   }
}
