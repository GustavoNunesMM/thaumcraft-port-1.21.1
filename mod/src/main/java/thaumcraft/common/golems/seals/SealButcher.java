package thaumcraft.common.golems.seals;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ambient.AmbientCreature;
import net.minecraft.world.entity.animal.AbstractGolem;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import thaumcraft.api.golems.EnumGolemTrait;
import thaumcraft.api.golems.GolemHelper;
import thaumcraft.api.golems.IGolemAPI;
import thaumcraft.api.golems.seals.ISeal;
import thaumcraft.api.golems.seals.ISealConfigArea;
import thaumcraft.api.golems.seals.ISealEntity;
import thaumcraft.api.golems.tasks.Task;
import thaumcraft.common.golems.tasks.TaskHandler;

/** Butcher: culls adult animals in the area while more than two of a kind remain. */
public class SealButcher extends SealBase implements ISealConfigArea {
   int delay = RAND.nextInt(200);
   boolean wait = false;

   public SealButcher() {
      super("seal_butcher");
   }

   @Override
   public String getKey() {
      return "thaumcraft:butcher";
   }

   @Override
   public ISeal newInstance() {
      return new SealButcher();
   }

   @Override
   public void tickSeal(Level level, ISealEntity seal) {
      if (delay++ % 200 != 0 || wait) return;
      AABB area = GolemHelper.getBoundsForArea(seal);
      for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area)) {
         if (!isValidTarget(target)) continue;
         int count = 0;
         for (LivingEntity other : level.getEntitiesOfClass(target.getClass(), area)) {
            if (isValidTarget(other) && ++count >= 3) break;
         }
         if (count > 2) {
            Task task = new Task(seal.getSealPos(), target);
            task.setPriority(seal.getPriority());
            task.setLifespan((short) 10);
            TaskHandler.addTask(level, task);
            wait = true;
            break;
         }
      }
   }

   private boolean isValidTarget(LivingEntity target) {
      return (target instanceof Animal || target instanceof WaterAnimal || target instanceof AmbientCreature) && !(target instanceof Enemy)
         && !(target instanceof TamableAnimal t && t.isTame()) && !(target instanceof AbstractGolem) && !(target instanceof Animal a && a.isBaby());
   }

   @Override
   public void onTaskStarted(Level level, IGolemAPI golem, Task task) {
      if (task.getEntity() instanceof LivingEntity le && isValidTarget(le)) {
         ((Mob) golem).setTarget(le);
         golem.addRankXp(1);
      }
      task.setSuspended(true);
      wait = false;
   }

   @Override
   public boolean onTaskCompletion(Level level, IGolemAPI golem, Task task) {
      task.setSuspended(true);
      wait = false;
      return true;
   }

   @Override
   public boolean canGolemPerformTask(IGolemAPI golem, Task task) {
      return true;
   }

   @Override
   public int[] getGuiCategories() {
      return new int[]{CAT_AREA, CAT_PRIORITY, CAT_TAGS};
   }

   @Override
   public EnumGolemTrait[] getRequiredTags() {
      return new EnumGolemTrait[]{EnumGolemTrait.FIGHTER, EnumGolemTrait.SMART};
   }

   @Override
   public void onTaskSuspension(Level level, Task task) {
      wait = false;
   }

   @Override
   public void onRemoval(Level level, net.minecraft.core.BlockPos pos, net.minecraft.core.Direction side) {
      wait = false;
   }
}
