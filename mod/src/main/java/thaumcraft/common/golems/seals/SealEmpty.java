package thaumcraft.common.golems.seals;

import java.util.HashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import thaumcraft.api.golems.EnumGolemTrait;
import thaumcraft.api.golems.IGolemAPI;
import thaumcraft.api.golems.seals.ISeal;
import thaumcraft.api.golems.seals.ISealConfigToggles;
import thaumcraft.api.golems.seals.ISealEntity;
import thaumcraft.api.golems.tasks.Task;
import thaumcraft.common.golems.GolemInvHelper;
import thaumcraft.common.golems.tasks.TaskHandler;

/** Empty: golems take items (that pass the filter) out of the inventory. */
public class SealEmpty extends SealFiltered {
   int delay = RAND.nextInt(30);
   int filterInc = 0;
   HashMap<Integer, ItemStack> cache = new HashMap<>();
   protected ISealConfigToggles.SealToggle[] props = new ISealConfigToggles.SealToggle[]{
      new ISealConfigToggles.SealToggle(true, "pmeta", "golem.prop.meta"), new ISealConfigToggles.SealToggle(true, "pnbt", "golem.prop.nbt"),
      new ISealConfigToggles.SealToggle(false, "pore", "golem.prop.ore"), new ISealConfigToggles.SealToggle(false, "pmod", "golem.prop.mod"),
      new ISealConfigToggles.SealToggle(false, "pcycle", "golem.prop.cycle"), new ISealConfigToggles.SealToggle(false, "pleave", "golem.prop.leave")};

   public SealEmpty() {
      this("seal_empty");
   }

   protected SealEmpty(String icon) {
      super(icon);
   }

   @Override
   public String getKey() {
      return "thaumcraft:empty";
   }

   @Override
   public ISeal newInstance() {
      return new SealEmpty();
   }

   @Override
   public void tickSeal(Level level, ISealEntity seal) {
      if (delay % 100 == 0) cache.keySet().removeIf(id -> TaskHandler.getTask(level, id) == null);
      if (delay++ % 20 == 0) {
         ItemStack stack = GolemInvHelper.findFirstMatchFromFilter(getInv(filterInc), isBlacklist(),
            GolemInvHelper.getItemHandlerAt(level, seal.getSealPos().pos, seal.getSealPos().face), invFilter(props), props[5].value);
         if (!stack.isEmpty()) {
            Task task = new Task(seal.getSealPos(), seal.getSealPos().pos);
            task.setPriority(seal.getPriority());
            task.setLifespan((short) 5);
            TaskHandler.addTask(level, task);
            cache.put(task.getId(), stack.copy());
         }
      }
   }

   @Override
   public boolean onTaskCompletion(Level level, IGolemAPI golem, Task task) {
      ItemStack stack = cache.get(task.getId());
      BlockPos pos = task.getSealPos().pos;
      Direction face = task.getSealPos().face;
      if (stack != null && !stack.isEmpty()) {
         int sa = GolemInvHelper.countTotalItemsIn(level, pos, face, stack, invFilter(props));
         if (props[5].value && sa <= stack.getCount()) {
            stack = stack.copy();
            stack.setCount(sa - 1);
         }
      }
      if (stack != null && !stack.isEmpty()) {
         int limit = golem.canCarryAmount(stack);
         if (limit > 0) {
            ItemStack s = golem.holdItem(GolemInvHelper.removeStackFrom(level, pos, face, GolemInvHelper.copyLimitedStack(stack, limit),
               invFilter(props), false));
            if (!s.isEmpty()) GolemInvHelper.ejectStackAt(level, pos.relative(face), face.getOpposite(), s);
            golem.getGolemEntity().playSound(SoundEvents.ITEM_PICKUP, 0.125F, ((level.random.nextFloat() - level.random.nextFloat()) * 0.7F + 1.0F) * 2.0F);
            golem.swingArm();
         }
      }
      cache.remove(task.getId());
      filterInc++;
      task.setSuspended(true);
      return true;
   }

   @Override
   public boolean canGolemPerformTask(IGolemAPI golem, Task task) {
      ItemStack stack = cache.get(task.getId());
      return stack != null && !stack.isEmpty() && golem.canCarry(stack, true);
   }

   @Override
   public boolean canPlaceAt(Level level, BlockPos pos, Direction side) {
      return GolemInvHelper.getItemHandlerAt(level, pos, side) != null;
   }

   public NonNullList<ItemStack> getInv(int c) {
      return super.getInv();
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
   public void onTaskSuspension(Level level, Task task) {
      cache.remove(task.getId());
   }
}
