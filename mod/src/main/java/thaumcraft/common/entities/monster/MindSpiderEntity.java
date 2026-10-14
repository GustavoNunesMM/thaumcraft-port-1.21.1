package thaumcraft.common.entities.monster;

import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;

/**
 * EntityMindSpider: a tiny phantom spider. Harmless ones are hallucinations seen only by their "viewer" (the warp
 * events that spawn them are in WarpEvents).
 */
public class MindSpiderEntity extends Spider implements thaumcraft.api.entities.IEldritchMob {
   private static final EntityDataAccessor<Boolean> HARMLESS = SynchedEntityData.defineId(MindSpiderEntity.class, EntityDataSerializers.BOOLEAN);
   private static final EntityDataAccessor<String> VIEWER = SynchedEntityData.defineId(MindSpiderEntity.class, EntityDataSerializers.STRING);
   private int lifeSpan = Integer.MAX_VALUE;

   public MindSpiderEntity(EntityType<? extends Spider> type, Level level) {
      super(type, level);
      xpReward = 1;
   }

   public static AttributeSupplier.Builder createAttributes() {
      return Spider.createAttributes().add(Attributes.MAX_HEALTH, 1.0).add(Attributes.ATTACK_DAMAGE, 1.0);
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder builder) {
      super.defineSynchedData(builder);
      builder.define(HARMLESS, false);
      builder.define(VIEWER, "");
   }

   public String getViewer() {
      return entityData.get(VIEWER);
   }

   public void setViewer(String player) {
      entityData.set(VIEWER, player);
   }

   public boolean isHarmless() {
      return entityData.get(HARMLESS);
   }

   public void setHarmless(boolean h) {
      if (h) lifeSpan = 1200;
      entityData.set(HARMLESS, h);
   }

   @Override
   protected int getBaseExperienceReward() {
      return isHarmless() ? 0 : super.getBaseExperienceReward();
   }

   @Override
   public float getVoicePitch() {
      return 0.7F;
   }

   @Override
   public void tick() {
      super.tick();
      if (!level().isClientSide && tickCount > lifeSpan) discard();
   }

   @Override
   protected boolean shouldDropLoot() {
      return false;
   }

   @Override
   public boolean isIgnoringBlockTriggers() {
      return true;
   }

   @Override
   protected Entity.MovementEmission getMovementEmission() {
      return Entity.MovementEmission.NONE;
   }

   @Override
   public boolean doHurtTarget(Entity target) {
      return !isHarmless() && super.doHurtTarget(target);
   }

   @Override
   public void readAdditionalSaveData(CompoundTag nbt) {
      super.readAdditionalSaveData(nbt);
      setHarmless(nbt.getBoolean("harmless"));
      setViewer(nbt.getString("viewer"));
   }

   @Override
   public void addAdditionalSaveData(CompoundTag nbt) {
      super.addAdditionalSaveData(nbt);
      nbt.putBoolean("harmless", isHarmless());
      nbt.putString("viewer", getViewer());
   }

   /** onInitialSpawn: no spider jockeys or potion effects. */
   @Nullable
   @Override
   public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType spawnType, @Nullable SpawnGroupData data) {
      return data;
   }
}
