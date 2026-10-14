package thaumcraft.common.golems.seals;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Tuple;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import thaumcraft.api.golems.EnumGolemTrait;
import thaumcraft.api.golems.IGolemAPI;
import thaumcraft.api.golems.seals.ISeal;
import thaumcraft.api.golems.seals.ISealConfigToggles;
import thaumcraft.api.golems.seals.ISealEntity;
import thaumcraft.api.golems.tasks.Task;
import thaumcraft.common.golems.GolemInvHelper;
import thaumcraft.common.golems.tasks.TaskHandler;

/** Store: golems put what they carry (that passes the filter) into the inventory, or drop it in front of the seal. */
public class SealFill extends SealFiltered {
   int delay = RAND.nextInt(50);
   int watchedTask = Integer.MIN_VALUE;
   protected ISealConfigToggles.SealToggle[] props = new ISealConfigToggles.SealToggle[]{
      new ISealConfigToggles.SealToggle(true, "pmeta", "golem.prop.meta"), new ISealConfigToggles.SealToggle(true, "pnbt", "golem.prop.nbt"),
      new ISealConfigToggles.SealToggle(false, "pore", "golem.prop.ore"), new ISealConfigToggles.SealToggle(false, "pmod", "golem.prop.mod"),
      new ISealConfigToggles.SealToggle(false, "pexist", "golem.prop.exist")};

   public SealFill() {
      this("seal_fill");
   }

   protected SealFill(String icon) {
      super(icon);
   }

   @Override
   public String getKey() {
      return "thaumcraft:fill";
   }

   @Override
   public ISeal newInstance() {
      return new SealFill();
   }

   @Override
   public void tickSeal(Level level, ISealEntity seal) {
      if (delay++ % 20 == 0) {
         Task oldTask = TaskHandler.getTask(level, watchedTask);
         if (oldTask == null || oldTask.isReserved() || oldTask.isSuspended() || oldTask.isCompleted()) {
            Task task = new Task(seal.getSealPos(), seal.getSealPos().pos);
            task.setPriority(seal.getPriority());
            TaskHandler.addTask(level, task);
            watchedTask = task.getId();
         }
      }
   }

   @Override
   public void onTaskStarted(Level level, IGolemAPI golem, Task task) {
      ISealEntity se = SealHandler.getSealEntity(level, task.getSealPos());
      if (se != null && !se.isStoppedByRedstone(level)) {
         Task newTask = new Task(task.getSealPos(), task.getSealPos().pos);
         newTask.setPriority(se.getPriority());
         TaskHandler.addTask(level, newTask);
         watchedTask = newTask.getId();
      }
   }

   @Override
   public boolean onTaskCompletion(Level level, IGolemAPI golem, Task task) {
      GolemInvHelper.InvFilter filt = invFilter(props);
      Tuple<ItemStack, Integer> tuple = GolemInvHelper.findFirstMatchFromFilterTuple(getInv(), getSizes(), isBlacklist(), golem.getCarrying(), filt);
      if (!tuple.getA().isEmpty()) {
         IItemHandler inv = GolemInvHelper.getItemHandlerAt(level, task.getSealPos().pos, task.getSealPos().face);
         int limit = tuple.getA().getCount();
         if (hasStacksizeLimiters() && tuple.getB() != null && tuple.getB() > 0) {
            int c = inv == null ? GolemInvHelper.countStackInWorld(level, task.getSealPos().pos, tuple.getA(), 1.5, filt)
               : GolemInvHelper.countTotalItemsIn(inv, tuple.getA(), filt);
            limit = c < tuple.getB() ? tuple.getB() - c : 0;
         }
         if (limit > 0) {
            ItemStack t = tuple.getA().copy();
            t.setCount(limit);
            ItemStack s = golem.dropItem(t);
            if (inv == null) {
               ItemEntity ie = new ItemEntity(level, task.getSealPos().pos.getX() + 0.5 + task.getSealPos().face.getStepX(),
                  task.getSealPos().pos.getY() + 0.5 + task.getSealPos().face.getStepY(),
                  task.getSealPos().pos.getZ() + 0.5 + task.getSealPos().face.getStepZ(), s);
               ie.setDeltaMovement(ie.getDeltaMovement().multiply(0.2, 0.5, 0.2));
               level.addFreshEntity(ie);
            } else {
               golem.holdItem(ItemHandlerHelper.insertItemStacked(inv, s, false));
            }
            golem.getGolemEntity().playSound(SoundEvents.ITEM_PICKUP, 0.125F, (level.random.nextFloat() - level.random.nextFloat()) * 0.7F + 1.0F);
            golem.addRankXp(1);
            golem.swingArm();
         }
      }
      task.setSuspended(true);
      return true;
   }

   @Override
   public boolean canGolemPerformTask(IGolemAPI golem, Task task) {
      GolemInvHelper.InvFilter filt = invFilter(props);
      Tuple<ItemStack, Integer> tuple = GolemInvHelper.findFirstMatchFromFilterTuple(getInv(), getSizes(), isBlacklist(), golem.getCarrying(), filt);
      ItemStack first = tuple.getA();
      if (first.isEmpty()) return false;
      Level level = golem.getGolemWorld();
      IItemHandler inv = GolemInvHelper.getItemHandlerAt(level, task.getSealPos().pos, task.getSealPos().face);
      if (inv != null) {
         if (props[4].value && GolemInvHelper.countTotalItemsIn(inv, first, filt) <= 0) return false;
         if (GolemInvHelper.hasRoomForSome(level, task.getSealPos().pos, task.getSealPos().face, first)) {
            if (!hasStacksizeLimiters() || tuple.getB() == null || tuple.getB() <= 0) return true;
            return GolemInvHelper.countTotalItemsIn(inv, first, filt) < tuple.getB();
         }
         return false;
      }
      if (hasStacksizeLimiters() && tuple.getB() != null && tuple.getB() > 0) {
         return GolemInvHelper.countStackInWorld(level, task.getSealPos().pos, first, 1.5, filt) < tuple.getB();
      }
      return true;
   }

   @Override
   public int[] getGuiCategories() {
      return new int[]{CAT_FILTER, CAT_PRIORITY, CAT_TAGS};
   }

   @Override
   public EnumGolemTrait[] getForbiddenTags() {
      return new EnumGolemTrait[]{EnumGolemTrait.CLUMSY};
   }

   @Override
   public boolean hasStacksizeLimiters() {
      return !isBlacklist();
   }
}
