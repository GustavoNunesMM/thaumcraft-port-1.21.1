package thaumcraft.common.entities.monster.cult;

import java.util.EnumSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.phys.AABB;
import thaumcraft.Thaumcraft;

/** The cult's AI: AICultistHurtByTarget, AILongRangeAttack and AIAltarFocus. */
public final class CultGoals {
   private CultGoals() {
   }

   /** AICultistHurtByTarget: revenge, calling every nearby cultist to help. */
   public static class HurtByTarget extends HurtByTargetGoal {
      private final boolean callsHelp;

      public HurtByTarget(PathfinderMob owner, boolean callsHelp) {
         super(owner);
         this.callsHelp = callsHelp;
      }

      @Override
      public void start() {
         super.start();
         unseenMemoryTicks = 300;
      }

      @Override
      protected void alertOthers() {
         if (!callsHelp) return;
         double d0 = getFollowDistance();
         LivingEntity revenge = mob.getLastHurtByMob();
         if (revenge == null) return;
         AABB box = AABB.unitCubeFromLowerCorner(mob.position()).inflate(d0, 10.0, d0);
         for (CultistEntity c : mob.level().getEntitiesOfClass(CultistEntity.class, box)) {
            if (c != mob && c.getTarget() == null && !c.isAlliedTo(revenge)) c.setTarget(revenge);
         }
      }
   }

   /** AILongRangeAttack: a ranged attack only used when the target is farther than a minimum distance. */
   public static class LongRangeAttack extends RangedAttackGoal {
      private final Mob wielder;
      private final double minDistance;

      public LongRangeAttack(RangedAttackMob mob, double min, double speed, int intervalMin, int intervalMax, float radius) {
         super(mob, speed, intervalMin, intervalMax, radius);
         this.wielder = (Mob) mob;
         this.minDistance = min;
      }

      @Override
      public boolean canUse() {
         boolean ex = super.canUse();
         if (ex) {
            LivingEntity t = wielder.getTarget();
            if (t == null) return false;
            if (!t.isAlive()) {
               wielder.setTarget(null);
               return false;
            }
            if (wielder.distanceToSqr(t.getX(), t.getBoundingBox().minY, t.getZ()) < minDistance * minDistance) return false;
         }
         return ex;
      }
   }

   /**
    * AIAltarFocus: a ritualist cleric stays focused on its altar until it is hurt, wanders off or the altar goes.
    * STUB: the eldritch altar block belongs to the eldritch milestone; it is looked up by id.
    */
   public static class AltarFocus extends Goal {
      private static final ResourceLocation ALTAR = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "eldritch");
      private final CultistClericEntity entity;

      public AltarFocus(CultistClericEntity entity) {
         this.entity = entity;
         setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
      }

      @Override
      public boolean canUse() {
         return entity.getIsRitualist() && entity.hasRestriction();
      }

      @Override
      public boolean canContinueToUse() {
         return canUse();
      }

      @Override
      public void tick() {
         if (entity.hasRestriction() && entity.tickCount % 40 == 0
            && (entity.getRestrictCenter().distToCenterSqr(entity.position()) > 16.0
            || !BuiltInRegistries.BLOCK.getKey(entity.level().getBlockState(entity.getRestrictCenter()).getBlock()).equals(ALTAR))) {
            entity.setIsRitualist(false);
         }
      }
   }
}
