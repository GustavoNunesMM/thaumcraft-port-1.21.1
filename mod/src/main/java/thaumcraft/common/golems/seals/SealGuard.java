package thaumcraft.common.golems.seals;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ambient.AmbientCreature;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import thaumcraft.api.golems.EnumGolemTrait;
import thaumcraft.api.golems.GolemHelper;
import thaumcraft.api.golems.IGolemAPI;
import thaumcraft.api.golems.seals.ISeal;
import thaumcraft.api.golems.seals.ISealConfigArea;
import thaumcraft.api.golems.seals.ISealConfigToggles;
import thaumcraft.api.golems.seals.ISealEntity;
import thaumcraft.api.golems.tasks.Task;
import thaumcraft.common.golems.tasks.TaskHandler;

/** Guard: fighters attack the mobs (or animals, or players with PvP on) entering the area. */
public class SealGuard extends SealBase implements ISealConfigArea {
   int delay = RAND.nextInt(22);
   protected ISealConfigToggles.SealToggle[] props = new ISealConfigToggles.SealToggle[]{
      new ISealConfigToggles.SealToggle(true, "pmob", "golem.prop.mob"), new ISealConfigToggles.SealToggle(false, "panimal", "golem.prop.animal"),
      new ISealConfigToggles.SealToggle(false, "pplayer", "golem.prop.player")};

   public SealGuard() {
      this("seal_guard");
   }

   protected SealGuard(String icon) {
      super(icon);
   }

   @Override
   public String getKey() {
      return "thaumcraft:guard";
   }

   @Override
   public ISeal newInstance() {
      return new SealGuard();
   }

   @Override
   public void tickSeal(Level level, ISealEntity seal) {
      if (delay++ % 20 != 0) return;
      for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, GolemHelper.getBoundsForArea(seal))) {
         if (isValidTarget(level, target)) {
            Task task = new Task(seal.getSealPos(), target);
            task.setPriority(seal.getPriority());
            task.setLifespan((short) 10);
            TaskHandler.addTask(level, task);
         }
      }
   }

   private boolean isValidTarget(Level level, LivingEntity target) {
      boolean valid = props[0].value && target instanceof Enemy;
      if (props[1].value && (target instanceof Animal || target instanceof WaterAnimal || target instanceof AmbientCreature)) valid = true;
      if (props[2].value && target instanceof Player && level.getServer() != null && level.getServer().isPvpAllowed()) valid = true;
      return valid;
   }

   @Override
   public void onTaskStarted(Level level, IGolemAPI golem, Task task) {
      if (task.getEntity() instanceof LivingEntity le && isValidTarget(level, le)) {
         ((Mob) golem).setTarget(le);
         golem.addRankXp(1);
      }
      task.setSuspended(true);
   }

   @Override
   public boolean onTaskCompletion(Level level, IGolemAPI golem, Task task) {
      task.setSuspended(true);
      return true;
   }

   @Override
   public boolean canGolemPerformTask(IGolemAPI golem, Task task) {
      return !golem.getGolemEntity().isAlliedTo(task.getEntity());
   }

   @Override
   public int[] getGuiCategories() {
      return new int[]{CAT_AREA, CAT_PRIORITY, CAT_TAGS};
   }

   @Override
   public EnumGolemTrait[] getRequiredTags() {
      return new EnumGolemTrait[]{EnumGolemTrait.FIGHTER};
   }
}
