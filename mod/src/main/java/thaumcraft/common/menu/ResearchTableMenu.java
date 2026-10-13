package thaumcraft.common.menu;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;
import thaumcraft.api.research.ResearchItem;
import thaumcraft.api.research.theorycraft.ResearchTableData;
import thaumcraft.api.research.theorycraft.TheorycraftCard;
import thaumcraft.common.blocks.crafting.ResearchTableBlockEntity;
import thaumcraft.common.lib.InventoryHelper;
import thaumcraft.common.registers.ModMenus;

/**
 * ContainerResearchTable. Buttons (clickMenuButton): 1 = confirm the played card, 2/3 = draw 2/3 cards,
 * 4-6 = play card 0-2, 7 = complete the theory, 9 = scrap it.
 */
public class ResearchTableMenu extends AbstractContainerMenu {
   private static final Map<UUID, Long> ANTI_SPAM = new HashMap<>();
   private final ResearchTableBlockEntity initial;
   private final Player player;

   /** Client side: the table may not have reached the client yet, so a stand-in is used until it does (see {@link #table()}). */
   public ResearchTableMenu(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
      this(id, inv, clientTable(inv, buf.readBlockPos()));
   }

   private static ResearchTableBlockEntity clientTable(Inventory inv, net.minecraft.core.BlockPos pos) {
      if (inv.player.level().getBlockEntity(pos) instanceof ResearchTableBlockEntity be) return be;
      ResearchTableBlockEntity stand = new ResearchTableBlockEntity(pos, thaumcraft.common.registers.ModBlocks.RESEARCH_TABLE.get().defaultBlockState());
      stand.setLevel(inv.player.level());
      return stand;
   }

   public ResearchTableMenu(int id, Inventory inv, ResearchTableBlockEntity table) {
      super(ModMenus.RESEARCH_TABLE.get(), id);
      this.initial = table;
      this.player = inv.player;
      addSlot(new SlotItemHandler(table.getItems(), 0, 16, 15));
      addSlot(new SlotItemHandler(table.getItems(), 1, 224, 16));
      for (int i = 0; i < 3; i++) {
         for (int j = 0; j < 9; j++) addSlot(new Slot(inv, j + i * 9 + 9, 77 + j * 18, 190 + i * 18));
      }
      for (int i = 0; i < 3; i++) {
         for (int j = 0; j < 3; j++) addSlot(new Slot(inv, i + j * 3, 20 + i * 18, 190 + j * 18));
      }
   }

   /** The table in the world (or the stand-in, until the client has the real one). */
   public ResearchTableBlockEntity table() {
      if (initial.isRemoved() || initial.getLevel() == null) return initial;
      return initial.getLevel().getBlockEntity(initial.getBlockPos()) instanceof ResearchTableBlockEntity be ? be : initial;
   }

   @Override
   public boolean clickMenuButton(Player playerIn, int button) {
      ResearchTableBlockEntity table = table();
      ResearchTableData data = table.data;
      if (button == 1) {
         if (data == null) return false;
         if (data.lastDraw != null) data.savedCards.add(data.lastDraw.card.getSeed());
         for (ResearchTableData.CardChoice cc : data.cardChoices) {
            if (cc.selected) {
               data.lastDraw = cc;
               break;
            }
         }
         data.cardChoices.clear();
         table.sync();
         return true;
      }
      if (button >= 4 && button <= 6) {
         if (data == null || button - 4 >= data.cardChoices.size()) return false;
         long now = System.currentTimeMillis();
         if (now - ANTI_SPAM.getOrDefault(playerIn.getUUID(), 0L) < 333L) return false;
         ANTI_SPAM.put(playerIn.getUUID(), now);
         TheorycraftCard card = data.cardChoices.get(button - 4).card;
         ItemStack[] req = card.getRequiredItems();
         if (req != null) {
            for (ItemStack stack : req) {
               if (!stack.isEmpty() && !InventoryHelper.isCarrying(player, new ResearchItem(stack, null))) return false;
            }
            boolean[] consumed = card.getRequiredItemsConsumed();
            if (consumed != null && consumed.length == req.length) {
               for (int a = 0; a < req.length; a++) {
                  if (consumed[a] && !req[a].isEmpty()) InventoryHelper.consume(player, new ResearchItem(req[a], null));
               }
            }
         }
         if (card.activate(playerIn, data)) {
            table.consumeInk();
            data.cardChoices.get(button - 4).selected = true;
            data.addInspiration(-card.getInspirationCost());
            table.sync();
            return true;
         }
         return false;
      }
      if (button == 7 && data != null && data.isComplete()) {
         table.finishTheory(playerIn);
         return true;
      }
      if (button == 9 && data != null && !data.isComplete()) {
         table.data = null;
         table.sync();
         return true;
      }
      if (button == 2 || button == 3) {
         if (data != null && !data.isComplete() && table.consumePaper()) {
            data.drawCards(button, playerIn);
            table.sync();
         }
         return true;
      }
      return false;
   }

   @Override
   public ItemStack quickMoveStack(Player p, int slot) {
      ItemStack stack = ItemStack.EMPTY;
      Slot s = slots.get(slot);
      if (s.hasItem()) {
         ItemStack in = s.getItem();
         stack = in.copy();
         if (slot < 2) {
            if (!moveItemStackTo(in, 2, slots.size(), true)) return ItemStack.EMPTY;
         } else if (!moveItemStackTo(in, 0, 2, false)) {
            return ItemStack.EMPTY;
         }
         if (in.isEmpty()) s.setByPlayer(ItemStack.EMPTY);
         else s.setChanged();
      }
      return stack;
   }

   @Override
   public boolean stillValid(Player p) {
      ResearchTableBlockEntity table = table();
      return !table.isRemoved() && p.distanceToSqr(table.getBlockPos().getCenter()) <= 64.0;
   }
}
