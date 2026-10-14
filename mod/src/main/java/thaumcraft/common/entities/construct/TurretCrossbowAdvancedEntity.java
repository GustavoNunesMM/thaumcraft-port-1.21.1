package thaumcraft.common.entities.construct;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ambient.AmbientCreature;
import net.minecraft.world.entity.animal.AbstractGolem;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.PlayerTeam;
import thaumcraft.common.menu.TurretAdvancedMenu;
import thaumcraft.common.registers.ModBlocks;
import thaumcraft.common.registers.ModItems;
import thaumcraft.common.registers.ModMobs;

/**
 * EntityTurretCrossbowAdvanced: an armoured crossbow with a biothaumic mind; it can be told to target animals, mobs,
 * players and/or only "friendly" things (its owner's team and constructs) instead of everything else.
 */
public class TurretCrossbowAdvancedEntity extends TurretCrossbowEntity {
   private static final EntityDataAccessor<Byte> FLAGS = SynchedEntityData.defineId(TurretCrossbowAdvancedEntity.class, EntityDataSerializers.BYTE);

   public TurretCrossbowAdvancedEntity(EntityType<? extends TurretCrossbowAdvancedEntity> type, Level level) {
      super(type, level);
   }

   public static AttributeSupplier.Builder createAttributes() {
      return TurretCrossbowEntity.createAttributes().add(Attributes.MAX_HEALTH, 40.0).add(Attributes.ARMOR, 8.0);
   }

   @Override
   protected void registerGoals() {
      goalSelector.addGoal(1, new RangedAttackGoal(this, 0.0, 20, 40, 24.0F));
      goalSelector.addGoal(2, new WatchTargetGoal(this));
      targetSelector.addGoal(1, new HurtByTargetGoal(this));
      targetSelector.addGoal(2, new ValidTargetGoal(this, 5, true));
      setTargetMob(true);
   }

   @Override
   protected float eyeHeight() {
      return 1.0F;
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder builder) {
      super.defineSynchedData(builder);
      builder.define(FLAGS, (byte) 0);
   }

   /** canAttackClass, by the four target toggles. */
   public boolean canTargetKind(LivingEntity e) {
      boolean animal = !(e instanceof Enemy) && (e instanceof Animal || e instanceof AmbientCreature || e instanceof WaterAnimal || e instanceof AbstractGolem);
      if (animal && getTargetAnimal()) return true;
      if (e instanceof Enemy && getTargetMob()) return true;
      if (!(e instanceof Player) || !getTargetPlayer()) return false;
      if (!level().isClientSide && !((ServerLevel) level()).getServer().isPvpAllowed() && !getTargetFriendly()) {
         setTargetPlayer(false);
         return false;
      }
      return true;
   }

   private boolean getBit(int b) {
      return (entityData.get(FLAGS) & (1 << b)) != 0;
   }

   private void setBit(int b, boolean on) {
      byte v = entityData.get(FLAGS);
      entityData.set(FLAGS, on ? (byte) (v | (1 << b)) : (byte) (v & ~(1 << b)));
      setTarget(null);
   }

   public boolean getTargetAnimal() {
      return getBit(0);
   }

   public void setTargetAnimal(boolean b) {
      setBit(0, b);
   }

   public boolean getTargetMob() {
      return getBit(1);
   }

   public void setTargetMob(boolean b) {
      setBit(1, b);
   }

   public boolean getTargetPlayer() {
      return getBit(2);
   }

   public void setTargetPlayer(boolean b) {
      setBit(2, b);
   }

   public boolean getTargetFriendly() {
      return getBit(3);
   }

   public void setTargetFriendly(boolean b) {
      setBit(3, b);
   }

   @Override
   public void tick() {
      super.tick();
      if (!level().isClientSide && !((ServerLevel) level()).getServer().isPvpAllowed() && getTarget() instanceof Player
         && getTarget() != getOwnerEntity()) {
         setTarget(null);
      }
   }

   @Override
   public void readAdditionalSaveData(CompoundTag nbt) {
      super.readAdditionalSaveData(nbt);
      entityData.set(FLAGS, nbt.getByte("targets"));
   }

   @Override
   public void addAdditionalSaveData(CompoundTag nbt) {
      super.addAdditionalSaveData(nbt);
      nbt.putByte("targets", entityData.get(FLAGS));
   }

   @Override
   protected ItemStack placerItem() {
      return new ItemStack(ModMobs.TURRET_ADVANCED.get());
   }

   @Override
   protected void openMenu(Player player) {
      player.openMenu(new SimpleMenuProvider((id, inv, p) -> new TurretAdvancedMenu(id, inv, this), getDisplayName()),
         buf -> buf.writeVarInt(getId()));
   }

   @Override
   public void move(MoverType type, Vec3 v) {
      super.move(type, new Vec3(v.x * 20.0 / 15.0, v.y, v.z * 20.0 / 15.0));
   }

   @Override
   protected void dropParts(ServerLevel level, DamageSource source) {
      // EntityTurretCrossbowAdvanced.dropFewItems (replaces the basic turret's drops)
      float b = looting(level, source) * 0.15F;
      if (random.nextFloat() < 0.2F + b) spawnAtLocation(new ItemStack(ModItems.MIND_BIOTHAUMIC.get()), 0.5F);
      if (random.nextFloat() < 0.5F + b) spawnAtLocation(new ItemStack(ModItems.MECHANISM_SIMPLE.get()), 0.5F);
      if (random.nextFloat() < 0.5F + b) spawnAtLocation(new ItemStack(ModBlocks.PLANK_GREATWOOD.get()), 0.5F);
      if (random.nextFloat() < 0.5F + b) spawnAtLocation(new ItemStack(ModBlocks.PLANK_GREATWOOD.get()), 0.5F);
      if (random.nextFloat() < 0.3F + b) spawnAtLocation(new ItemStack(ModItems.PLATE_BRASS.get()), 0.5F);
      if (random.nextFloat() < 0.4F + b) spawnAtLocation(new ItemStack(ModItems.PLATE_IRON.get()), 0.5F);
      if (random.nextFloat() < 0.4F + b) spawnAtLocation(new ItemStack(ModItems.PLATE_IRON.get()), 0.5F);
   }

   /** EntityAINearestValidTarget with the advanced turret's isGoodTarget. */
   static class ValidTargetGoal extends TargetGoal {
      private final TurretCrossbowAdvancedEntity turret;
      private final int chance;
      private int unseenTicks;
      @Nullable
      private LivingEntity found;

      ValidTargetGoal(TurretCrossbowAdvancedEntity turret, int chance, boolean checkSight) {
         super(turret, checkSight, false);
         this.turret = turret;
         this.chance = chance;
         setFlags(EnumSet.of(Flag.TARGET));
      }

      private boolean selectable(LivingEntity e) {
         if (e instanceof Player p) {
            if (p.isSpectator()) return false;
            double d0 = getFollowDistance();
            if (p.isShiftKeyDown()) d0 *= 0.8F;
            if (p.isInvisible()) d0 *= 0.7F * Math.max(0.1F, p.getArmorCoverPercentage());
            if (p.distanceTo(turret) > d0) return false;
         }
         return isGoodTarget(e, false) && turret.isWithinRestriction(e.blockPosition());
      }

      private boolean isGoodTarget(@Nullable LivingEntity t, boolean includeInvincibles) {
         if (t == null || t == turret || !t.isAlive() || !turret.canTargetKind(t)) return false;
         PlayerTeam team = turret.getTeam();
         PlayerTeam team1 = t.getTeam();
         boolean friendly = turret.getTargetFriendly();
         if (team != null && team1 == team && !friendly) return false;
         if (team != null && team1 != team && friendly) return false;
         UUID owner = turret.getOwnerUUID();
         if (owner != null) {
            if (t instanceof OwnableEntity o && owner.equals(o.getOwnerUUID()) && !friendly) return false;
            if (!(t instanceof OwnableEntity) && !(t instanceof Player) && friendly) return false;
            if (t instanceof OwnableEntity o && !owner.equals(o.getOwnerUUID()) && friendly) return false;
            if (t == turret.getOwner() && !friendly) return false;
         } else if (t instanceof Player p && !includeInvincibles && p.getAbilities().invulnerable && !friendly) {
            return false;
         }
         return !mustSee || turret.getSensing().hasLineOfSight(t);
      }

      @Override
      public boolean canUse() {
         if (chance > 0 && turret.getRandom().nextInt(chance) != 0) return false;
         double d0 = getFollowDistance();
         List<LivingEntity> list = turret.level().getEntitiesOfClass(LivingEntity.class, turret.getBoundingBox().inflate(d0, 4.0, d0),
            e -> EntitySelector.NO_SPECTATORS.test(e) && selectable(e));
         if (list.isEmpty()) return false;
         list.sort(Comparator.comparingDouble(turret::distanceToSqr));
         found = list.get(0);
         return true;
      }

      @Override
      public boolean canContinueToUse() {
         LivingEntity t = turret.getTarget();
         if (t == null || !t.isAlive()) return false;
         PlayerTeam team = turret.getTeam();
         PlayerTeam team1 = t.getTeam();
         if (team != null && team1 == team && !turret.getTargetFriendly()) return false;
         if (team != null && team1 != team && turret.getTargetFriendly()) return false;
         double d0 = getFollowDistance();
         if (turret.distanceToSqr(t) > d0 * d0) return false;
         if (mustSee) {
            if (turret.getSensing().hasLineOfSight(t)) {
               unseenTicks = 0;
            } else if (++unseenTicks > 60) {
               return false;
            }
         }
         return true;
      }

      @Override
      public void start() {
         turret.setTarget(found);
         unseenTicks = 0;
         super.start();
      }
   }
}
