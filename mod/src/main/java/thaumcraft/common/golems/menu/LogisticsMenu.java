package thaumcraft.common.golems.menu;

import java.util.Locale;
import java.util.TreeMap;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.IItemHandler;
import thaumcraft.common.golems.GolemInvHelper;
import thaumcraft.common.golems.seals.SealEntity;
import thaumcraft.common.golems.seals.SealHandler;
import thaumcraft.common.golems.seals.SealProvide;
import thaumcraft.common.registers.ModGolems;

/**
 * ContainerLogistics: lists what the player's provider seals within 32 blocks can supply (9x9 page, scrollable);
 * the screen requests items from it.
 */
public class LogisticsMenu extends AbstractContainerMenu {
   private final Level level;
   private final Player player;
   public final SimpleContainer input = new SimpleContainer(81) {
      @Override
      public int getMaxStackSize() {
         return Integer.MAX_VALUE;
      }
   };
   TreeMap<String, ItemStack> items = new TreeMap<>();
   int lastTotal = 0;
   public int start = 0;
   public int end = 0;
   public String searchText = "";
   int lastStart = -1;
   public boolean updated = false;
   @Nullable
   public final BlockPos target;
   @Nullable
   public final Direction side;

   public LogisticsMenu(int id, Inventory inv, @Nullable BlockPos target, @Nullable Direction side) {
      super(ModGolems.LOGISTICS_MENU.get(), id);
      this.level = inv.player.level();
      this.player = inv.player;
      this.target = target;
      this.side = side;
      for (int a = 0; a < input.getContainerSize(); a++) addSlot(new DisplaySlot(input, a, 19 + a % 9 * 19, 19 + a / 9 * 19));
      addDataSlot(new DataSlot() {
         @Override
         public int get() {
            return start;
         }

         @Override
         public void set(int v) {
            start = v;
            updated = true;
         }
      });
      addDataSlot(new DataSlot() {
         @Override
         public int get() {
            return end;
         }

         @Override
         public void set(int v) {
            end = v;
            updated = true;
         }
      });
      if (!level.isClientSide) refreshItemList(true);
   }

   public LogisticsMenu(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
      this(id, inv, buf.readBoolean() ? buf.readBlockPos() : null, buf.readableBytes() > 0 ? Direction.from3DDataValue(buf.readByte()) : null);
   }

   public void refreshItemList(boolean full) {
      int newTotal = lastTotal;
      TreeMap<String, ItemStack> ti = new TreeMap<>();
      if (full) {
         newTotal = 0;
         for (SealEntity seal : SealHandler.getSealsInRange(level, player.blockPosition(), 32)) {
            if (!(seal.getSeal() instanceof SealProvide sp) || !seal.getOwner().equals(player.getUUID().toString())) continue;
            IItemHandler handler = GolemInvHelper.getItemHandlerAt(level, seal.getSealPos().pos, seal.getSealPos().face);
            if (handler == null) continue;
            for (int slot = 0; slot < handler.getSlots(); slot++) {
               ItemStack stack = handler.getStackInSlot(slot).copy();
               if (stack.isEmpty() || !sp.matchesFilters(stack)) continue;
               String name = stack.getHoverName().getString();
               if (!searchText.isEmpty() && !name.toLowerCase(Locale.ROOT).contains(searchText.toLowerCase(Locale.ROOT))) continue;
               String key = name + stack.getComponentsPatch();
               if (ti.containsKey(key)) stack.grow(ti.get(key).getCount());
               ti.put(key, stack);
               newTotal += handler.getStackInSlot(slot).getCount();
            }
         }
      }
      if (lastTotal != newTotal || start != lastStart || full && ti.size() != items.size()) {
         lastTotal = newTotal;
         lastStart = start;
         if (full) items = ti;
         input.clearContent();
         int j = 0;
         int q = 0;
         for (ItemStack s : items.values()) {
            if (++j > start * 9) {
               input.setItem(q, s);
               if (++q >= input.getContainerSize()) break;
            }
         }
         end = Math.max(0, items.size() / 9 - 8);
      }
   }

   public void setSearch(String text) {
      searchText = text.length() > 10 ? text.substring(0, 10) : text;
      refreshItemList(true);
      broadcastChanges();
   }

   @Override
   public boolean clickMenuButton(Player p, int id) {
      if (p.level().isClientSide) return true;
      if (id == 22) {
         refreshItemList(true);
      } else if (id == 0) {
         if (start < items.size() / 9 - 8) {
            start++;
            refreshItemList(false);
         }
      } else if (id == 1) {
         if (start > 0) {
            start--;
            refreshItemList(false);
         }
      } else if (id >= 100) {
         int s = id - 100;
         if (s <= items.size() / 9 - 8) {
            start = s;
            refreshItemList(false);
         }
      } else {
         return false;
      }
      return true;
   }

   /** The listed stacks can't be taken; clicking only selects them (in the screen). */
   @Override
   public void clicked(int slotId, int button, ClickType mode, Player p) {
      if (slotId >= 0 && slotId < 81) return;
      super.clicked(slotId, button, mode, p);
   }

   @Override
   public ItemStack quickMoveStack(Player p, int index) {
      return ItemStack.EMPTY;
   }

   @Override
   public boolean stillValid(Player p) {
      return true;
   }

   /** SlotGhostFull: shows a stack of any size. */
   public static class DisplaySlot extends Slot {
      DisplaySlot(SimpleContainer c, int index, int x, int y) {
         super(c, index, x, y);
      }

      @Override
      public boolean mayPickup(Player player) {
         return false;
      }

      @Override
      public boolean mayPlace(ItemStack stack) {
         return false;
      }

      @Override
      public int getMaxStackSize() {
         return Integer.MAX_VALUE;
      }

      @Override
      public int getMaxStackSize(ItemStack stack) {
         return Integer.MAX_VALUE;
      }
   }
}
