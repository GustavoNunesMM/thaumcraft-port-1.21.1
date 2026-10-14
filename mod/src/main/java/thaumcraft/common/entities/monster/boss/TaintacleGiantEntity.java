package thaumcraft.common.entities.monster.boss;

import javax.annotation.Nullable;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import thaumcraft.api.entities.IEldritchMob;
import thaumcraft.common.entities.monster.cult.ThaumcraftBossEntity;
import thaumcraft.common.entities.monster.eldritch.EldritchHelper;
import thaumcraft.common.entities.monster.tainted.TaintacleEntity;
import thaumcraft.common.registers.ModMobs;

/**
 * EntityTaintacleGiant: a six block taintacle with a boss bar. It never spawns naturally; it regenerates, enrages
 * like the other bosses, and the last one alive within 48 blocks drops a primordial pearl.
 * It is always a champion (EntityUtils.makeChampion).
 */
public class TaintacleGiantEntity extends TaintacleEntity implements IEldritchMob {
   private static final EntityDataAccessor<Integer> AGGRO = SynchedEntityData.defineId(TaintacleGiantEntity.class, EntityDataSerializers.INT);
   protected final ServerBossEvent bossInfo = (ServerBossEvent) new ServerBossEvent(Component.empty(), BossEvent.BossBarColor.PURPLE,
      BossEvent.BossBarOverlay.PROGRESS).setDarkenScreen(true);

   public TaintacleGiantEntity(EntityType<? extends Monster> type, Level level) {
      super(type, level);
      xpReward = 20;
   }

   public static AttributeSupplier.Builder createGiantAttributes() {
      return TaintacleEntity.createAttributes().add(Attributes.MAX_HEALTH, 175.0).add(Attributes.ATTACK_DAMAGE, 9.0);
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder builder) {
      super.defineSynchedData(builder);
      builder.define(AGGRO, 0);
   }

   public int getAnger() {
      return entityData.get(AGGRO);
   }

   public void setAnger(int a) {
      entityData.set(AGGRO, a);
   }

   @Nullable
   @Override
   public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance diff, MobSpawnType reason, @Nullable SpawnGroupData data) {
      thaumcraft.common.entities.monster.mods.ChampionModifier.makeChampion(this, true);
      bossInfo.setName(getDisplayName());
      return data;
   }

   @Override
   public void tick() {
      super.tick();
      if (getAnger() > 0 && !level().isClientSide) setAnger(getAnger() - 1);
      if (level().isClientSide && random.nextInt(15) == 0 && getAnger() > 0) {
         level().addParticle(ParticleTypes.ANGRY_VILLAGER, getX() + random.nextFloat() * getBbWidth() - getBbWidth() / 2.0,
            getBoundingBox().minY + getBbHeight() + random.nextFloat() * 0.5, getZ() + random.nextFloat() * getBbWidth() - getBbWidth() / 2.0,
            random.nextGaussian() * 0.02, random.nextGaussian() * 0.02, random.nextGaussian() * 0.02);
      }
      if (!level().isClientSide && tickCount % 30 == 0) heal(1.0F);
   }

   @Override
   public boolean canSpawnHere() {
      return false;
   }

   @Override
   protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
      if (level.getEntitiesOfClass(TaintacleGiantEntity.class, getBoundingBox().inflate(48.0), e -> e != this && e.isAlive()).isEmpty()) {
         ThaumcraftBossEntity.dropSpecialItem(this, new ItemStack(ModMobs.PRIMORDIAL_PEARL.get()), getBbHeight() / 2.0F);
      }
   }

   @Override
   public boolean removeWhenFarAway(double distance) {
      return false;
   }

   @Override
   protected int decreaseAirSupply(int air) {
      return air;
   }

   @Override
   public boolean hurt(DamageSource source, float damage) {
      if (!level().isClientSide && damage > 35.0F) {
         if (getAnger() == 0) {
            addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, (int) (damage / 15.0F)));
            addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 200, (int) (damage / 10.0F)));
            addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 200, (int) (damage / 40.0F)));
            setAnger(200);
            if (source.getEntity() instanceof Player p) {
               p.displayClientMessage(Component.empty().append(getDisplayName()).append(" ").append(Component.translatable("tc.boss.enrage")), true);
            }
         }
         damage = 35.0F;
      }
      return super.hurt(source, damage);
   }

   @Override
   protected void customServerAiStep() {
      super.customServerAiStep();
      bossInfo.setProgress(getHealth() / getMaxHealth());
   }

   @Override
   public void startSeenByPlayer(ServerPlayer player) {
      super.startSeenByPlayer(player);
      bossInfo.addPlayer(player);
   }

   @Override
   public void stopSeenByPlayer(ServerPlayer player) {
      super.stopSeenByPlayer(player);
      bossInfo.removePlayer(player);
   }

   @Override
   public boolean isAlliedTo(Entity e) {
      return EldritchHelper.isEldritch(e) || super.isAlliedTo(e);
   }
}
