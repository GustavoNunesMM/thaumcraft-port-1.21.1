package thaumcraft.common.entities.monster.tainted;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.common.blocks.world.taint.TaintHelper;
import thaumcraft.common.registers.ModFlux;

/** EntityTaintCrawler: a grub that leaves taint fibres behind it; its bite may give flux taint. */
public class TaintCrawlerEntity extends TaintedMonster {
   private BlockPos lastPos = BlockPos.ZERO;

   public TaintCrawlerEntity(EntityType<? extends Monster> type, Level level) {
      super(type, level);
      xpReward = 3;
   }

   public static AttributeSupplier.Builder createAttributes() {
      return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 8.0).add(Attributes.MOVEMENT_SPEED, 0.275).add(Attributes.ATTACK_DAMAGE, 2.0);
   }

   @Override
   protected void registerGoals() {
      goalSelector.addGoal(1, new FloatGoal(this));
      goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, false));
      goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 1.0));
      goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8.0F));
      goalSelector.addGoal(8, new RandomLookAroundGoal(this));
      targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
      targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
   }

   @Override
   public void tick() {
      if (!level().isClientSide && isAlive() && tickCount % 40 == 0 && !lastPos.equals(blockPosition())) {
         lastPos = blockPosition();
         BlockState bs = level().getBlockState(lastPos);
         if (!TaintHelper.isLeaves(bs) && bs.getFluidState().isEmpty() && !TaintHelper.isTaint(bs)
            && (bs.isAir() || bs.canBeReplaced() || bs.getBlock() instanceof BushBlock) && TaintHelper.isAdjacentToSolidBlock(level(), lastPos)
            && !TaintHelper.isOnlyAdjacentToTaint(level(), lastPos)) {
            level().setBlockAndUpdate(lastPos, ModFlux.TAINT_FIBRE.get().defaultBlockState());
         }
      }
      super.tick();
   }

   @Override
   public float getVoicePitch() {
      return 0.7F;
   }

   @Override
   protected SoundEvent getAmbientSound() {
      return SoundEvents.SILVERFISH_AMBIENT;
   }

   @Override
   protected SoundEvent getHurtSound(DamageSource source) {
      return SoundEvents.SILVERFISH_HURT;
   }

   @Override
   protected SoundEvent getDeathSound() {
      return SoundEvents.SILVERFISH_DEATH;
   }

   @Override
   protected void playStepSound(BlockPos pos, BlockState block) {
      playSound(SoundEvents.SILVERFISH_STEP, 0.15F, 1.0F);
   }

   @Override
   protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
      if (random.nextInt(8) == 0) dropFluxCrystal();
   }

   @Override
   public boolean doHurtTarget(Entity victim) {
      if (!super.doHurtTarget(victim)) return false;
      if (victim instanceof LivingEntity le) {
         int b0 = level().getDifficulty() == Difficulty.NORMAL ? 3 : level().getDifficulty() == Difficulty.HARD ? 6 : 0;
         if (b0 > 0 && random.nextInt(b0 + 1) > 2) le.addEffect(new MobEffectInstance(ModFlux.FLUX_TAINT, b0 * 20, 0));
      }
      return true;
   }
}
