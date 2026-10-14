package thaumcraft.common.golems;

import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.NameTagItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.scores.PlayerTeam;
import thaumcraft.common.registers.ModSounds;

/**
 * EntityOwnedConstruct: a construct that belongs to a player, never despawns and only exists when placed properly
 * (spawned any other way it vanishes). Kept with the golems: the turrets of a later milestone may reuse it.
 */
public class OwnedConstructEntity extends PathfinderMob implements OwnableEntity {
   protected static final EntityDataAccessor<Byte> TAMED = SynchedEntityData.defineId(OwnedConstructEntity.class, EntityDataSerializers.BYTE);
   protected static final EntityDataAccessor<Optional<UUID>> OWNER_UNIQUE_ID = SynchedEntityData.defineId(OwnedConstructEntity.class,
      EntityDataSerializers.OPTIONAL_UUID);
   boolean validSpawn = false;

   protected OwnedConstructEntity(EntityType<? extends PathfinderMob> type, Level level) {
      super(type, level);
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder builder) {
      super.defineSynchedData(builder);
      builder.define(TAMED, (byte) 0);
      builder.define(OWNER_UNIQUE_ID, Optional.empty());
   }

   public boolean isOwned() {
      return (entityData.get(TAMED) & 4) != 0;
   }

   public void setOwned(boolean tamed) {
      byte b0 = entityData.get(TAMED);
      entityData.set(TAMED, tamed ? (byte) (b0 | 4) : (byte) (b0 & -5));
   }

   @Nullable
   @Override
   public UUID getOwnerUUID() {
      return entityData.get(OWNER_UNIQUE_ID).orElse(null);
   }

   public void setOwnerId(@Nullable UUID id) {
      entityData.set(OWNER_UNIQUE_ID, Optional.ofNullable(id));
   }

   @Override
   protected int decreaseAirSupply(int air) {
      return air;
   }

   @Override
   protected SoundEvent getAmbientSound() {
      return ModSounds.CLACK.get();
   }

   @Override
   protected SoundEvent getHurtSound(DamageSource source) {
      return ModSounds.CLACK.get();
   }

   @Override
   protected SoundEvent getDeathSound() {
      return ModSounds.TOOL.get();
   }

   @Override
   public int getAmbientSoundInterval() {
      return 240;
   }

   @Override
   public boolean removeWhenFarAway(double distance) {
      return false;
   }

   @Override
   public void tick() {
      super.tick();
      if (getTarget() != null && isAlliedTo(getTarget())) setTarget(null);
      if (!level().isClientSide && !validSpawn) discard();
   }

   public void setValidSpawn() {
      validSpawn = true;
   }

   /**
    * A construct summoned by command (/summon, no NBT) is valid too: in 1.12 it vanished on its first tick, as only
    * the placer items mark their constructs valid.
    */
   @Nullable
   @Override
   public net.minecraft.world.entity.SpawnGroupData finalizeSpawn(net.minecraft.world.level.ServerLevelAccessor level,
                                                               net.minecraft.world.DifficultyInstance diff,
                                                               net.minecraft.world.entity.MobSpawnType reason,
                                                               @Nullable net.minecraft.world.entity.SpawnGroupData data) {
      if (reason == net.minecraft.world.entity.MobSpawnType.COMMAND) setValidSpawn();
      return super.finalizeSpawn(level, diff, reason, data);
   }

   @Override
   public void addAdditionalSaveData(CompoundTag tag) {
      super.addAdditionalSaveData(tag);
      tag.putBoolean("v", validSpawn);
      tag.putString("OwnerUUID", getOwnerUUID() == null ? "" : getOwnerUUID().toString());
   }

   @Override
   public void readAdditionalSaveData(CompoundTag tag) {
      super.readAdditionalSaveData(tag);
      validSpawn = tag.getBoolean("v");
      String s = tag.getString("OwnerUUID");
      if (!s.isEmpty()) {
         try {
            setOwnerId(UUID.fromString(s));
            setOwned(true);
         } catch (IllegalArgumentException e) {
            setOwned(false);
         }
      }
   }

   @Nullable
   public LivingEntity getOwnerEntity() {
      UUID uuid = getOwnerUUID();
      return uuid == null ? null : level().getPlayerByUUID(uuid);
   }

   public boolean isOwner(LivingEntity entity) {
      return entity == getOwnerEntity();
   }

   @Nullable
   @Override
   public PlayerTeam getTeam() {
      if (isOwned()) {
         LivingEntity owner = getOwnerEntity();
         if (owner != null) return owner.getTeam();
      }
      return super.getTeam();
   }

   @Override
   public boolean isAlliedTo(Entity other) {
      if (isOwned()) {
         LivingEntity owner = getOwnerEntity();
         if (other == owner) return true;
         if (owner != null) return owner.isAlliedTo(other);
      }
      return super.isAlliedTo(other);
   }

   @Override
   protected InteractionResult mobInteract(Player player, InteractionHand hand) {
      if (!isAlive()) return InteractionResult.PASS;
      if (!player.isShiftKeyDown() && !(player.getMainHandItem().getItem() instanceof NameTagItem)) {
         if (!level().isClientSide && !isOwner(player)) {
            player.displayClientMessage(Component.translatable("tc.notowned").withStyle(s -> s.withColor(0xAA00AA).withItalic(true)), true);
            return InteractionResult.SUCCESS;
         }
         return super.mobInteract(player, hand);
      }
      return InteractionResult.PASS;
   }
}
