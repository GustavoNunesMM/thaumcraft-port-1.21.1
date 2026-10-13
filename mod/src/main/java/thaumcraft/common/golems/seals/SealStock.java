package thaumcraft.common.golems.seals;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.IItemHandler;
import thaumcraft.api.golems.EnumGolemTrait;
import thaumcraft.api.golems.GolemHelper;
import thaumcraft.api.golems.IGolemAPI;
import thaumcraft.api.golems.seals.ISeal;
import thaumcraft.api.golems.seals.ISealConfigToggles;
import thaumcraft.api.golems.seals.ISealEntity;
import thaumcraft.api.golems.tasks.Task;
import thaumcraft.common.golems.GolemInvHelper;

/** Stock: requests provisioning until the inventory holds the amounts set in the filter. */
public class SealStock extends SealFiltered implements ISealConfigToggles {
   int delay = RAND.nextInt(50);
   protected ISealConfigToggles.SealToggle[] props = new ISealConfigToggles.SealToggle[]{
      new ISealConfigToggles.SealToggle(true, "pmeta", "golem.prop.meta"), new ISealConfigToggles.SealToggle(true, "pnbt", "golem.prop.nbt"),
      new ISealConfigToggles.SealToggle(false, "pore", "golem.prop.ore"), new ISealConfigToggles.SealToggle(false, "pmod", "golem.prop.mod")};

   public SealStock() {
      super("seal_stock");
   }

   @Override
   public String getKey() {
      return "thaumcraft:stock";
   }

   @Override
   public ISeal newInstance() {
      return new SealStock();
   }

   @Override
   public int getFilterSize() {
      return 9;
   }

   @Override
   public void tickSeal(Level level, ISealEntity seal) {
      if (delay++ % 20 != 0) return;
      IItemHandler inv = GolemInvHelper.getItemHandlerAt(level, seal.getSealPos().pos, seal.getSealPos().face);
      if (inv == null) return;
      for (int a = 0; a < 9; a++) {
         if (getFilterSlot(a).isEmpty()) continue;
         int amt = GolemInvHelper.countTotalItemsIn(inv, getFilterSlot(a), invFilter(props));
         if (amt < getFilterSlotSize(a)) {
            ItemStack fs = getFilterSlot(a).copy();
            fs.setCount(Math.min(fs.getMaxStackSize(), getFilterSlotSize(a) - amt));
            fs = GolemInvHelper.hasRoomFor(level, seal.getSealPos().pos, seal.getSealPos().face, fs);
            if (!fs.isEmpty()) GolemHelper.requestProvisioning(level, seal.getSealPos().pos, seal.getSealPos().face, fs);
         }
      }
   }

   @Override
   public boolean onTaskCompletion(Level level, IGolemAPI golem, Task task) {
      return true;
   }

   @Override
   public boolean canGolemPerformTask(IGolemAPI golem, Task task) {
      return false;
   }

   @Override
   public boolean canPlaceAt(Level level, BlockPos pos, Direction side) {
      return GolemInvHelper.getItemHandlerAt(level, pos, side) != null;
   }

   @Override
   public int[] getGuiCategories() {
      return new int[]{CAT_FILTER, CAT_TOGGLES, CAT_PRIORITY, CAT_TAGS};
   }

   @Override
   public EnumGolemTrait[] getForbiddenTags() {
      return new EnumGolemTrait[]{EnumGolemTrait.CLUMSY};
   }

   @Override
   public boolean hasStacksizeLimiters() {
      return true;
   }

   @Override
   public boolean isBlacklist() {
      return false;
   }

   @Override
   public SealToggle[] getToggles() {
      return props;
   }

   @Override
   public void setToggle(int indx, boolean value) {
      props[indx].setValue(value);
   }
}
