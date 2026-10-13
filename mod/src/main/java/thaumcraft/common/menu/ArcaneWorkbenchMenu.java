package thaumcraft.common.menu;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerListener;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.event.EventHooks;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.crafting.IArcaneRecipe;
import thaumcraft.common.blocks.crafting.ArcaneWorkbenchBlockEntity;
import thaumcraft.common.items.VisCrystalItem;
import thaumcraft.common.items.casters.CasterManager;
import thaumcraft.common.registers.ModBlocks;
import thaumcraft.common.registers.ModItems;
import thaumcraft.common.registers.ModMenus;
import thaumcraft.common.registers.ModRecipes;
import thaumcraft.common.research.ResearchManager;

/**
 * ContainerArcaneWorkbench + SlotCraftingArcaneWorkbench. Slots: 0 result, 1-9 grid, 10-15 crystals, 16-42 inventory, 43-51 hotbar.
 * Arcane recipes need their research, the vis of the aura and the crystals; otherwise the grid works as a crafting table.
 */
public class ArcaneWorkbenchMenu extends AbstractContainerMenu {
   public static final int[] XX = {64, 17, 112, 17, 112, 64};
   public static final int[] YY = {13, 35, 35, 93, 93, 115};
   /** Crystal slot order (ShardType). */
   public static final Aspect[] CRYSTALS = {Aspect.AIR, Aspect.FIRE, Aspect.WATER, Aspect.EARTH, Aspect.ORDER, Aspect.ENTROPY};

   public final ArcaneWorkbenchBlockEntity table;
   private final Player player;
   private final ResultContainer result = new ResultContainer();
   private final DataSlot vis = DataSlot.standalone();
   private final ContainerListener listener = this::slotsChanged;
   private int lastVis = -1;
   private int ticks;

   public ArcaneWorkbenchMenu(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
      this(id, inv, clientTable(inv, buf.readBlockPos()));
   }

   private static ArcaneWorkbenchBlockEntity clientTable(Inventory inv, BlockPos pos) {
      if (inv.player.level().getBlockEntity(pos) instanceof ArcaneWorkbenchBlockEntity be) return be;
      ArcaneWorkbenchBlockEntity stand = new ArcaneWorkbenchBlockEntity(pos, ModBlocks.ARCANE_WORKBENCH.get().defaultBlockState());
      stand.setLevel(inv.player.level());
      return stand;
   }

   public ArcaneWorkbenchMenu(int id, Inventory inv, ArcaneWorkbenchBlockEntity table) {
      super(ModMenus.ARCANE_WORKBENCH.get(), id);
      this.table = table;
      this.player = inv.player;
      Container grid = table.inventory;
      addSlot(new ResultSlot(160, 64));
      for (int y = 0; y < 3; y++) {
         for (int x = 0; x < 3; x++) addSlot(new Slot(grid, x + y * 3, 40 + x * 24, 40 + y * 24));
      }
      for (int i = 0; i < 6; i++) addSlot(new CrystalSlot(grid, 9 + i, XX[i], YY[i], CRYSTALS[i]));
      for (int y = 0; y < 3; y++) {
         for (int x = 0; x < 9; x++) addSlot(new Slot(inv, x + y * 9 + 9, 16 + x * 18, 151 + y * 18));
      }
      for (int x = 0; x < 9; x++) addSlot(new Slot(inv, x, 16 + x * 18, 209));
      addDataSlot(vis);
      if (!player.level().isClientSide) {
         vis.set(table.getAura());
         table.inventory.addListener(listener);
         slotsChanged(grid);
      }
   }

   /** Vis available at the table (synced to the client). */
   public int getAvailableVis() {
      return vis.get();
   }

   public static boolean isValidCrystal(ItemStack stack, Aspect aspect) {
      return stack.is(ModItems.CRYSTAL_ESSENCE.get()) && VisCrystalItem.getAspect(stack) == aspect;
   }

   private CraftingInput.Positioned gridInput() {
      List<ItemStack> items = new ArrayList<>(9);
      for (int i = 0; i < 9; i++) items.add(table.inventory.getItem(i));
      return CraftingInput.ofPositioned(3, 3, items);
   }

   /** ThaumcraftCraftingManager.findMatchingArcaneRecipe: a recipe matching the grid whose crystals are in the crystal slots. */
   @Nullable
   public static IArcaneRecipe findArcaneRecipe(Level level, Container inv, CraftingInput input) {
      for (RecipeHolder<IArcaneRecipe> h : level.getRecipeManager().getRecipesFor(ModRecipes.ARCANE_CRAFTING.get(), input, level)) {
         if (hasCrystals(inv, h.value().getCrystals())) return h.value();
      }
      return null;
   }

   private static boolean hasCrystals(Container inv, AspectList crystals) {
      for (Aspect a : crystals.getAspects()) {
         int count = 0;
         for (int i = 9; i < 15; i++) {
            ItemStack s = inv.getItem(i);
            if (isValidCrystal(s, a)) count += s.getCount();
         }
         if (count < crystals.getAmount(a)) return false;
      }
      return true;
   }

   /** The arcane recipe the grid currently makes (research known), for the screen and the result slot. */
   @Nullable
   public IArcaneRecipe currentArcaneRecipe() {
      IArcaneRecipe r = findArcaneRecipe(player.level(), table.inventory, gridInput().input());
      return r != null && ResearchManager.knowsResearch(player, r.getResearch()) ? r : null;
   }

   public int visCost(IArcaneRecipe recipe) {
      return (int) (recipe.getVis() * (1.0F - CasterManager.getTotalVisDiscount(player)));
   }

   @Override
   public void slotsChanged(Container container) {
      Level level = player.level();
      if (level.isClientSide) return;
      CraftingInput input = gridInput().input();
      ItemStack out = ItemStack.EMPTY;
      IArcaneRecipe arcane = currentArcaneRecipe();
      if (arcane != null) {
         if (table.getAura() >= visCost(arcane)) out = arcane.assemble(input, level.registryAccess());
      } else {
         Optional<RecipeHolder<CraftingRecipe>> r = level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, level);
         if (r.isPresent()) out = r.get().value().assemble(input, level.registryAccess());
      }
      result.setItem(0, out);
      broadcastChanges();
   }

   @Override
   public void broadcastChanges() {
      if (!player.level().isClientSide && ++ticks % 10 == 0) {
         vis.set(table.getAura());
         if (vis.get() != lastVis) {
            lastVis = vis.get();
            slotsChanged(table.inventory);
            return;
         }
      }
      super.broadcastChanges();
   }

   @Override
   public void removed(Player p) {
      super.removed(p);
      table.inventory.removeListener(listener);
   }

   @Override
   public boolean stillValid(Player p) {
      return !table.isRemoved() && table.getLevel() != null && table.getLevel().getBlockEntity(table.getBlockPos()) == table
         && p.distanceToSqr(table.getBlockPos().getCenter()) <= 64.0;
   }

   @Override
   public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
      return slot.container != result && super.canTakeItemForPickAll(stack, slot);
   }

   @Override
   public ItemStack quickMoveStack(Player p, int index) {
      Slot slot = slots.get(index);
      if (!slot.hasItem()) return ItemStack.EMPTY;
      ItemStack stack = slot.getItem();
      ItemStack copy = stack.copy();
      if (index == 0) {
         stack.getItem().onCraftedBy(stack, p.level(), p);
         if (!moveItemStackTo(stack, 16, 52, true)) return ItemStack.EMPTY;
         slot.onQuickCraft(stack, copy);
      } else if (index >= 16) {
         for (int i = 0; i < 6 && !stack.isEmpty(); i++) {
            if (isValidCrystal(stack, CRYSTALS[i])) moveItemStackTo(stack, 10 + i, 11 + i, false);
         }
         if (!stack.isEmpty()) {
            if (index < 43) {
               if (!moveItemStackTo(stack, 43, 52, false)) return ItemStack.EMPTY;
            } else if (!moveItemStackTo(stack, 16, 43, false)) {
               return ItemStack.EMPTY;
            }
         }
      } else if (!moveItemStackTo(stack, 16, 52, false)) {
         return ItemStack.EMPTY;
      }
      if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
      else slot.setChanged();
      if (stack.getCount() == copy.getCount()) return ItemStack.EMPTY;
      slot.onTake(p, stack);
      if (index == 0) p.drop(stack, false);
      return copy;
   }

   static class CrystalSlot extends Slot {
      private final Aspect aspect;

      CrystalSlot(Container c, int index, int x, int y, Aspect aspect) {
         super(c, index, x, y);
         this.aspect = aspect;
      }

      @Override
      public boolean mayPlace(ItemStack stack) {
         return isValidCrystal(stack, aspect);
      }
   }

   /** SlotCraftingArcaneWorkbench. */
   class ResultSlot extends Slot {
      private int removeCount;

      ResultSlot(int x, int y) {
         super(result, 0, x, y);
      }

      @Override
      public boolean mayPlace(ItemStack stack) {
         return false;
      }

      @Override
      public ItemStack remove(int amount) {
         if (hasItem()) removeCount += Math.min(amount, getItem().getCount());
         return super.remove(amount);
      }

      @Override
      protected void onQuickCraft(ItemStack stack, int amount) {
         removeCount += amount;
         checkTakeAchievements(stack);
      }

      @Override
      protected void onSwapCraft(int amount) {
         removeCount += amount;
      }

      @Override
      protected void checkTakeAchievements(ItemStack stack) {
         if (removeCount > 0) {
            stack.onCraftedBy(player.level(), player, removeCount);
            EventHooks.firePlayerCraftingEvent(player, stack, table.inventory);
         }
         removeCount = 0;
      }

      @Override
      public void onTake(Player p, ItemStack stack) {
         checkTakeAchievements(stack);
         Level level = p.level();
         Container inv = table.inventory;
         CraftingInput.Positioned positioned = gridInput();
         CraftingInput input = positioned.input();
         IArcaneRecipe recipe = currentArcaneRecipe();
         CommonHooks.setCraftingPlayer(p);
         NonNullList<ItemStack> remaining = recipe != null ? recipe.getRemainingItems(input)
            : level.getRecipeManager().getRemainingItemsFor(RecipeType.CRAFTING, input, level);
         CommonHooks.setCraftingPlayer(null);
         if (recipe != null) {
            int cost = visCost(recipe);
            if (cost > 0) {
               table.getAura();
               table.spendAura(cost);
            }
         }
         for (int y = 0; y < input.height(); y++) {
            for (int x = 0; x < input.width(); x++) {
               int slot = x + positioned.left() + (y + positioned.top()) * 3;
               ItemStack cur = inv.getItem(slot);
               ItemStack rem = remaining.get(x + y * input.width());
               if (!cur.isEmpty()) {
                  inv.removeItem(slot, 1);
                  cur = inv.getItem(slot);
               }
               if (!rem.isEmpty()) {
                  if (cur.isEmpty()) {
                     inv.setItem(slot, rem);
                  } else if (ItemStack.isSameItemSameComponents(cur, rem)) {
                     rem.grow(cur.getCount());
                     inv.setItem(slot, rem);
                  } else if (!p.getInventory().add(rem)) {
                     p.drop(rem, false);
                  }
               }
            }
         }
         if (recipe != null) {
            AspectList crystals = recipe.getCrystals();
            for (Aspect a : crystals.getAspects()) {
               int need = crystals.getAmount(a);
               for (int i = 9; i < 15 && need > 0; i++) {
                  ItemStack s = inv.getItem(i);
                  if (isValidCrystal(s, a)) need -= inv.removeItem(i, need).getCount();
               }
            }
         }
      }
   }
}
