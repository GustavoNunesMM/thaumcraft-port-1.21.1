package thaumcraft.common.entities.monster.cult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import thaumcraft.Thaumcraft;
import thaumcraft.common.entities.SpecialItemEntity;
import thaumcraft.common.registers.ModMobs;

/**
 * EntityThaumcraftBoss: boss bar, aggro table, extra health and damage per extra player, slow regeneration, enrage on
 * huge hits, and a primordial pearl plus a rare loot bag on death. (Kept in the cult package: the eldritch bosses
 * belong to another milestone.)
 */
public abstract class ThaumcraftBossEntity extends Monster {
   private static final EntityDataAccessor<Integer> AGGRO = SynchedEntityData.defineId(ThaumcraftBossEntity.class, EntityDataSerializers.INT);
   private static final AttributeModifier[] HPBUFF = new AttributeModifier[5];
   private static final AttributeModifier[] DMGBUFF = new AttributeModifier[5];

   static {
      for (int a = 0; a < 5; a++) {
         HPBUFF[a] = new AttributeModifier(ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "boss_health_buff_" + (a + 1)), 50.0,
            AttributeModifier.Operation.ADD_VALUE);
         DMGBUFF[a] = new AttributeModifier(ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "boss_damage_buff_" + (a + 1)), 0.5,
            AttributeModifier.Operation.ADD_VALUE);
      }
   }

   protected final ServerBossEvent bossInfo = (ServerBossEvent) new ServerBossEvent(Component.empty(), BossEvent.BossBarColor.PURPLE,
      BossEvent.BossBarOverlay.PROGRESS).setDarkenScreen(true);
   private final Map<Integer, Integer> aggro = new HashMap<>();
   protected int spawnTimer;

   protected ThaumcraftBossEntity(EntityType<? extends ThaumcraftBossEntity> type, Level level) {
      super(type, level);
      xpReward = 50;
   }

   public static AttributeSupplier.Builder createAttributes() {
      return Monster.createMonsterAttributes().add(Attributes.KNOCKBACK_RESISTANCE, 0.95).add(Attributes.FOLLOW_RANGE, 40.0);
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder builder) {
      super.defineSynchedData(builder);
      builder.define(AGGRO, 0);
   }

   @Override
   public void readAdditionalSaveData(CompoundTag nbt) {
      super.readAdditionalSaveData(nbt);
      if (nbt.contains("HomeD")) restrictTo(new BlockPos(nbt.getInt("HomeX"), nbt.getInt("HomeY"), nbt.getInt("HomeZ")), nbt.getInt("HomeD"));
      bossInfo.setName(getDisplayName());
   }

   @Override
   public void addAdditionalSaveData(CompoundTag nbt) {
      super.addAdditionalSaveData(nbt);
      if (hasRestriction() && getRestrictRadius() > 0.0F) {
         nbt.putInt("HomeD", (int) getRestrictRadius());
         nbt.putInt("HomeX", getRestrictCenter().getX());
         nbt.putInt("HomeY", getRestrictCenter().getY());
         nbt.putInt("HomeZ", getRestrictCenter().getZ());
      }
   }

   @Override
   protected void customServerAiStep() {
      if (spawnTimer == 0) super.customServerAiStep();
      if (getTarget() != null && !getTarget().isAlive()) setTarget(null);
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

   @Nullable
   @Override
   public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance diff, MobSpawnType reason, @Nullable SpawnGroupData data) {
      restrictTo(blockPosition(), 24);
      generateName();
      bossInfo.setName(getDisplayName());
      return data;
   }

   public int getAnger() {
      return entityData.get(AGGRO);
   }

   public void setAnger(int a) {
      entityData.set(AGGRO, a);
   }

   public int getSpawnTimer() {
      return spawnTimer;
   }

   @Override
   public void tick() {
      super.tick();
      if (spawnTimer > 0) spawnTimer--;
      if (getAnger() > 0 && !level().isClientSide) setAnger(getAnger() - 1);
      if (level().isClientSide && random.nextInt(15) == 0 && getAnger() > 0) {
         level().addParticle(ParticleTypes.ANGRY_VILLAGER, getX() + random.nextFloat() * getBbWidth() - getBbWidth() / 2.0,
            getBoundingBox().minY + getBbHeight() + random.nextFloat() * 0.5, getZ() + random.nextFloat() * getBbWidth() - getBbWidth() / 2.0,
            random.nextGaussian() * 0.02, random.nextGaussian() * 0.02, random.nextGaussian() * 0.02);
      }
      if (!level().isClientSide) {
         if (tickCount % 30 == 0) heal(1.0F);
         LivingEntity target = getTarget();
         if (target != null && tickCount % 20 == 0) {
            List<Integer> dl = new ArrayList<>();
            int players = 0;
            int hei = target.getId();
            int ad = aggro.getOrDefault(hei, 0);
            int ld = ad;
            Entity newTarget = null;
            for (Map.Entry<Integer, Integer> e : aggro.entrySet()) {
               Entity en = level().getEntity(e.getKey());
               if (en == null || !en.isAlive() || distanceToSqr(en) > 16384.0) {
                  dl.add(e.getKey());
                  continue;
               }
               if (en instanceof Player) players++;
               int ca = e.getValue();
               if (ca > ad + 25 && ca > ad * 1.1 && ca > ld) {
                  newTarget = en;
                  ld = ca;
               }
            }
            dl.forEach(aggro::remove);
            if (newTarget instanceof LivingEntity le && newTarget != target) setTarget(le);
            float om = getMaxHealth();
            AttributeInstance hp = getAttribute(Attributes.MAX_HEALTH);
            AttributeInstance dmg = getAttribute(Attributes.ATTACK_DAMAGE);
            for (int a = 0; a < 5; a++) {
               hp.removeModifier(HPBUFF[a].id());
               if (dmg != null) dmg.removeModifier(DMGBUFF[a].id());
            }
            for (int a = 0; a < Math.min(5, players - 1); a++) {
               hp.addTransientModifier(HPBUFF[a]);
               if (dmg != null) dmg.addTransientModifier(DMGBUFF[a]);
            }
            double mm = getMaxHealth() / om;
            setHealth((float) (getHealth() * mm));
         }
      }
   }

   @Override
   public boolean isInvulnerableTo(DamageSource source) {
      return super.isInvulnerableTo(source) || spawnTimer > 0;
   }

   // canBreatheUnderwater: the entity type tag minecraft:can_breathe_under_water (gen_marco13.py)

   @Override
   protected int decreaseAirSupply(int air) {
      return air;
   }

   @Override
   public void makeStuckInBlock(net.minecraft.world.level.block.state.BlockState state, net.minecraft.world.phys.Vec3 motionMultiplier) {
   }

   @Override
   public boolean canPickUpLoot() {
      return false;
   }

   @Override
   public boolean removeWhenFarAway(double dist) {
      return false;
   }

   @Override
   protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
      super.dropCustomDeathLoot(level, source, recentlyHit);
      dropSpecialItem(this, new ItemStack(ModMobs.PRIMORDIAL_PEARL.get()), getBbHeight() / 2.0F);
      spawnAtLocation(new ItemStack(ModMobs.LOOT_BAG_RARE.get()), 1.5F);
   }

   /** EntityUtils.entityDropSpecialItem: a floating, indestructible item. */
   public static void dropSpecialItem(Entity entity, ItemStack stack, float height) {
      SpecialItemEntity e = new SpecialItemEntity(entity.level(), entity.getX(), entity.getY() + height, entity.getZ(), stack);
      e.setDefaultPickUpDelay();
      e.setDeltaMovement(0.0, 0.1F, 0.0);
      entity.level().addFreshEntity(e);
   }

   @Override
   public boolean hurt(DamageSource source, float damage) {
      if (!level().isClientSide) {
         if (source.getEntity() instanceof LivingEntity le) aggro.merge(le.getId(), (int) damage, Integer::sum);
         if (damage > 35.0F) {
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
      }
      return super.hurt(source, damage);
   }

   public void generateName() {
   }
}
