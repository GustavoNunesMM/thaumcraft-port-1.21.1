package thaumcraft.common.golems.seals;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import thaumcraft.api.golems.EnumGolemTrait;
import thaumcraft.api.golems.GolemHelper;
import thaumcraft.api.golems.IGolemAPI;
import thaumcraft.api.golems.seals.ISeal;
import thaumcraft.api.golems.seals.ISealConfigToggles;
import thaumcraft.api.golems.seals.ISealEntity;
import thaumcraft.api.golems.tasks.Task;
import thaumcraft.common.golems.GolemInteractionHelper;
import thaumcraft.common.golems.GolemInvHelper;
import thaumcraft.common.golems.tasks.TaskHandler;

/** Use: golems click the block (left or right, sneaking or not) with a carried item that passes the filter. */
public class SealUse extends SealFiltered implements ISealConfigToggles {
   int delay = RAND.nextInt(49);
   int watchedTask = Integer.MIN_VALUE;
   protected ISealConfigToggles.SealToggle[] props = new ISealConfigToggles.SealToggle[]{
      new ISealConfigToggles.SealToggle(true, "pmeta", "golem.prop.meta"), new ISealConfigToggles.SealToggle(true, "pnbt", "golem.prop.nbt"),
      new ISealConfigToggles.SealToggle(false, "pore", "golem.prop.ore"), new ISealConfigToggles.SealToggle(false, "pmod", "golem.prop.mod"),
      new ISealConfigToggles.SealToggle(false, "pleft", "golem.prop.left"), new ISealConfigToggles.SealToggle(false, "pempty", "golem.prop.empty"),
      new ISealConfigToggles.SealToggle(false, "pemptyhand", "golem.prop.emptyhand"), new ISealConfigToggles.SealToggle(false, "psneak", "golem.prop.sneak"),
      new ISealConfigToggles.SealToggle(false, "ppro", "golem.prop.provision.wl")};

   public SealUse() {
      super("seal_use");
   }

   @Override
   public String getKey() {
      return "thaumcraft:use";
   }

   @Override
   public ISeal newInstance() {
      return new SealUse();
   }

   @Override
   public void tickSeal(Level level, ISealEntity seal) {
      if (delay++ % 5 != 0) return;
      Task oldTask = TaskHandler.getTask(level, watchedTask);
      if (oldTask == null || oldTask.isSuspended() || oldTask.isCompleted()) {
         if (getToggles()[5].value != level.isEmptyBlock(seal.getSealPos().pos)) return;
         Task task = new Task(seal.getSealPos(), seal.getSealPos().pos);
         task.setPriority(seal.getPriority());
         TaskHandler.addTask(level, task);
         watchedTask = task.getId();
      }
   }

   @Override
   public boolean onTaskCompletion(Level level, IGolemAPI golem, Task task) {
      if (getToggles()[5].value == level.isEmptyBlock(task.getPos())) {
         ItemStack clickStack = golem.getCarrying().get(0);
         if (!filter.get(0).isEmpty()) {
            clickStack = GolemInvHelper.findFirstMatchFromFilter(filter, filterSize, blacklist, golem.getCarrying(), invFilter(props));
         }
         if (!clickStack.isEmpty() || props[6].value) {
            ItemStack ss = ItemStack.EMPTY;
            if (!clickStack.isEmpty()) {
               ss = clickStack.copy();
               golem.dropItem(clickStack.copy());
            }
            GolemInteractionHelper.golemClick(level, golem, task.getPos(), task.getSealPos().face, props[6].value ? ItemStack.EMPTY : ss, props[7].value,
               !getToggles()[4].value);
         }
      }
      task.setSuspended(true);
      return true;
   }

   @Override
   public boolean canGolemPerformTask(IGolemAPI golem, Task task) {
      if (props[6].value) return true;
      boolean found = !GolemInvHelper.findFirstMatchFromFilter(filter, filterSize, blacklist, golem.getCarrying(), invFilter(props)).isEmpty();
      if (!found && getToggles()[8].value && !blacklist && !getInv().get(0).isEmpty()) {
         ISealEntity se = SealHandler.getSealEntity(golem.getGolemWorld(), task.getSealPos());
         if (se != null) GolemHelper.requestProvisioning(golem.getGolemWorld(), se, getInv().get(0).copy());
      }
      return found;
   }

   @Override
   public boolean canPlaceAt(Level level, BlockPos pos, Direction side) {
      return true;
   }

   @Override
   public int[] getGuiCategories() {
      return new int[]{CAT_FILTER, CAT_TOGGLES, CAT_PRIORITY, CAT_TAGS};
   }

   @Override
   public EnumGolemTrait[] getRequiredTags() {
      return new EnumGolemTrait[]{EnumGolemTrait.DEFT, EnumGolemTrait.SMART};
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
