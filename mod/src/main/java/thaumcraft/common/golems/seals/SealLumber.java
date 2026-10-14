package thaumcraft.common.golems.seals;

import java.util.HashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import thaumcraft.api.golems.EnumGolemTrait;
import thaumcraft.api.golems.GolemHelper;
import thaumcraft.api.golems.IGolemAPI;
import thaumcraft.api.golems.seals.ISeal;
import thaumcraft.api.golems.seals.ISealConfigArea;
import thaumcraft.api.golems.seals.ISealEntity;
import thaumcraft.api.golems.tasks.Task;
import thaumcraft.common.golems.GolemBlockUtils;
import thaumcraft.common.golems.GolemInteractionHelper;
import thaumcraft.common.golems.tasks.TaskHandler;

/** Lumberjack: breakers fell the trees in the area, log by log from the top. */
public class SealLumber extends SealBase implements ISealConfigArea {
   int delay = RAND.nextInt(33);
   final HashMap<Integer, Long> cache = new HashMap<>();

   public SealLumber() {
      super("seal_lumber");
   }

   @Override
   public String getKey() {
      return "thaumcraft:lumber";
   }

   @Override
   public ISeal newInstance() {
      return new SealLumber();
   }

   @Override
   public void tickSeal(Level level, ISealEntity seal) {
      if (delay % 100 == 0) cache.keySet().removeIf(id -> TaskHandler.getTask(level, id) == null);
      delay++;
      BlockPos p = GolemHelper.getPosInArea(seal, delay);
      if (!cache.containsValue(p.asLong()) && GolemBlockUtils.isWoodLog(level, p)) {
         Task task = new Task(seal.getSealPos(), p);
         task.setPriority(seal.getPriority());
         TaskHandler.addTask(level, task);
         cache.put(task.getId(), p.asLong());
      }
   }

   @Override
   public boolean onTaskCompletion(Level level, IGolemAPI golem, Task task) {
      if (cache.containsKey(task.getId()) && GolemBlockUtils.isWoodLog(level, task.getPos()) && level instanceof ServerLevel sl) {
         golem.swingArm();
         if (GolemBlockUtils.breakFurthestBlock(sl, task.getPos(), level.getBlockState(task.getPos()),
            GolemInteractionHelper.fakePlayer(sl, golem.getGolemEntity()))) {
            task.setLifespan((short) Math.max(task.getLifespan(), 10L));
            golem.addRankXp(1);
            return false;
         }
         cache.remove(task.getId());
      }
      task.setSuspended(true);
      return true;
   }

   @Override
   public boolean canGolemPerformTask(IGolemAPI golem, Task task) {
      if (cache.containsKey(task.getId()) && GolemBlockUtils.isWoodLog(golem.getGolemWorld(), task.getPos())) return true;
      task.setSuspended(true);
      return false;
   }

   @Override
   public void onTaskSuspension(Level level, Task task) {
      cache.remove(task.getId());
   }

   @Override
   public int[] getGuiCategories() {
      return new int[]{CAT_AREA, CAT_PRIORITY, CAT_TAGS};
   }

   @Override
   public EnumGolemTrait[] getRequiredTags() {
      return new EnumGolemTrait[]{EnumGolemTrait.BREAKER, EnumGolemTrait.SMART};
   }
}
