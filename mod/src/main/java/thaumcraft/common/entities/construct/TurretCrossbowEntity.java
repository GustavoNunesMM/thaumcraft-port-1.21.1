package thaumcraft.common.entities.construct;

import java.util.EnumSet;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import thaumcraft.common.menu.TurretBasicMenu;
import thaumcraft.common.registers.ModBlocks;
import thaumcraft.common.registers.ModItems;
import thaumcraft.common.registers.ModMobs;
import thaumcraft.common.registers.ModSounds;

/**
 * EntityTurretCrossbow: an automated crossbow on a tripod. It shoots the arrows in its slot (tipped arrows keep their
 * effect) at hostile mobs, reloads from a dispenser facing up below it, and is switched off by a powered activator rail.
 */
public class TurretCrossbowEntity extends OwnedConstructEntity implements RangedAttackMob {
   int loadProgressInt = 0;
   boolean isLoadInProgress = false;
   float loadProgress = 0.0F;
   float prevLoadProgress = 0.0F;
   public float loadProgressForRender = 0.0F;

   public TurretCrossbowEntity(EntityType<? extends TurretCrossbowEntity> type, Level level) {
      super(type, level);
      setDropChance(EquipmentSlot.MAINHAND, 0.0F);
   }

   public static AttributeSupplier.Builder createAttributes() {
      return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 30.0).add(Attributes.FOLLOW_RANGE, 24.0).add(Attributes.ARMOR, 2.0)
         .add(Attributes.STEP_HEIGHT, 0.0);
   }

   /** Places a new turret on a block (ItemTurretPlacer). */
   public void placeAt(BlockPos pos) {
      moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0.0F, 0.0F);
   }

   @Override
   protected void registerGoals() {
      goalSelector.addGoal(1, new RangedAttackGoal(this, 0.0, 20, 60, 24.0F));
      goalSelector.addGoal(2, new WatchTargetGoal(this));
      targetSelector.addGoal(1, new HurtByTargetGoal(this));
      targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Mob.class, 5, true, false, e -> e instanceof Enemy));
   }

   /** getEyeHeight: two thirds of its height. */
   protected float eyeHeight() {
      return getType().getHeight() * 0.66F;
   }

   @Override
   protected EntityDimensions getDefaultDimensions(Pose pose) {
      return super.getDefaultDimensions(pose).withEyeHeight(eyeHeight());
   }

   @Override
   public void performRangedAttack(LivingEntity target, float range) {
      ItemStack ammo = getMainHandItem();
      if (ammo.isEmpty()) return;
      Arrow arrow = new Arrow(level(), this, ammo.copyWithCount(1), null);
      arrow.setBaseDamage(2.25 + range * 2.0F + random.nextGaussian() * 0.25);
      Vec3 look = getViewVector(1.0F);
      if (!isPassenger()) {
         arrow.setPos(arrow.getX() - look.x * 0.9F, arrow.getY() - look.y * 0.9F, arrow.getZ() - look.z * 0.9F);
      } else {
         arrow.setPos(arrow.getX() + look.x * 1.75, arrow.getY() + look.y * 1.75, arrow.getZ() + look.z * 1.75);
      }
      arrow.pickup = AbstractArrow.Pickup.DISALLOWED;
      double d0 = target.getX() - getX();
      double d1 = target.getBoundingBox().minY + target.getEyeHeight() + range * range * 3.0F - arrow.getY();
      double d2 = target.getZ() - getZ();
      arrow.shoot(d0, d1, d2, 2.0F, 2.0F);
      level().addFreshEntity(arrow);
      swing(InteractionHand.MAIN_HAND);
      playSound(SoundEvents.ARROW_SHOOT, 1.0F, 1.0F / (getRandom().nextFloat() * 0.4F + 0.8F));
      ammo.shrink(1);
      if (ammo.isEmpty()) setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
   }

   @Override
   public void handleEntityEvent(byte id) {
      if (id == 17) {
         if (!isLoadInProgress) {
            loadProgressInt = -1;
            isLoadInProgress = true;
         }
      } else {
         super.handleEntityEvent(id);
      }
   }

   /** The reloading lever's animation, 0..1. */
   public float getLoadProgress(float pt) {
      float f1 = loadProgress - prevLoadProgress;
      if (f1 < 0.0F) f1++;
      return prevLoadProgress + f1 * pt;
   }

   private void updateLoadProgress() {
      if (isLoadInProgress) {
         loadProgressInt++;
         if (loadProgressInt >= 10) {
            loadProgressInt = 0;
            isLoadInProgress = false;
         }
      } else {
         loadProgressInt = 0;
      }
      loadProgress = loadProgressInt / 10.0F;
   }

   @Override
   public void baseTick() {
      prevLoadProgress = loadProgress;
      if (!level().isClientSide && getMainHandItem().isEmpty()) {
         BlockPos p = blockPosition().below();
         BlockState st = level().getBlockState(p);
         if (level().getBlockEntity(p) instanceof DispenserBlockEntity d && st.hasProperty(DispenserBlock.FACING)
            && st.getValue(DispenserBlock.FACING) == Direction.UP) {
            for (int a = 0; a < d.getContainerSize(); a++) {
               ItemStack s = d.getItem(a);
               if (!s.isEmpty() && s.getItem() instanceof ArrowItem) {
                  setItemSlot(EquipmentSlot.MAINHAND, d.removeItem(a, s.getCount()));
                  playSound(ModSounds.TICKS.get(), 1.0F, 1.0F);
                  level().broadcastEntityEvent(this, (byte) 17);
                  break;
               }
            }
         }
      }
      super.baseTick();
   }

   @Override
   public void tick() {
      super.tick();
      if (getTarget() != null && (!getTarget().isAlive() || isAlliedTo(getTarget()))) setTarget(null);
      if (!level().isClientSide) {
         setYRot(getYHeadRot());
         if (tickCount % 80 == 0) heal(1.0F);
         Boolean powered = activatorRailPowered();
         if (powered != null) setNoAi(powered);
      } else {
         updateLoadProgress();
      }
   }

   @Override
   public boolean isPushable() {
      return true;
   }

   @Override
   public boolean isPickable() {
      return true;
   }

   @Override
   public boolean hurt(DamageSource source, float amount) {
      setYRot((float) (getYRot() + getRandom().nextGaussian() * 45.0));
      setXRot((float) (getXRot() + getRandom().nextGaussian() * 20.0));
      return super.hurt(source, amount);
   }

   @Override
   public void knockback(double strength, double x, double z) {
      super.knockback(strength, x / 10.0, z / 10.0);
      Vec3 m = getDeltaMovement();
      if (m.y > 0.1) setDeltaMovement(m.x, 0.1, m.z);
   }

   /** The placer item this construct turns back into (ItemTurretPlacer meta). */
   protected ItemStack placerItem() {
      return new ItemStack(ModMobs.TURRET_BASIC.get());
   }

   protected void openMenu(Player player) {
      player.openMenu(new SimpleMenuProvider((id, inv, p) -> new TurretBasicMenu(id, inv, this), getDisplayName()), buf -> buf.writeVarInt(getId()));
   }

   @Override
   protected InteractionResult mobInteract(Player player, InteractionHand hand) {
      if (isOwner(player) && !isRemoved()) {
         if (!level().isClientSide) {
            if (player.isShiftKeyDown()) {
               playSound(ModSounds.ZAP.get(), 1.0F, 1.0F);
               dropAmmo();
               spawnAtLocation(placerItem(), 0.5F);
               discard();
               player.swing(hand);
            } else {
               openMenu(player);
            }
         }
         return InteractionResult.sidedSuccess(level().isClientSide);
      }
      return super.mobInteract(player, hand);
   }

   @Override
   public void move(MoverType type, Vec3 v) {
      super.move(type, new Vec3(v.x / 20.0, v.y, v.z / 20.0));
   }

   @Override
   public void die(DamageSource cause) {
      super.die(cause);
      if (!level().isClientSide) dropAmmo();
   }

   protected void dropAmmo() {
      if (!getMainHandItem().isEmpty()) {
         spawnAtLocation(getMainHandItem(), 0.5F);
         setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
      }
   }

   protected static int looting(ServerLevel level, DamageSource source) {
      if (!(source.getEntity() instanceof LivingEntity le)) return 0;
      return EnchantmentHelper.getEnchantmentLevel(level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.LOOTING), le);
   }

   @Override
   protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
      super.dropCustomDeathLoot(level, source, recentlyHit);
      dropParts(level, source);
   }

   /** dropFewItems: the parts it was made of. */
   protected void dropParts(ServerLevel level, DamageSource source) {
      float b = looting(level, source) * 0.15F;
      if (random.nextFloat() < 0.2F + b) spawnAtLocation(new ItemStack(ModItems.MIND_CLOCKWORK.get()), 0.5F);
      if (random.nextFloat() < 0.5F + b) spawnAtLocation(new ItemStack(ModItems.MECHANISM_SIMPLE.get()), 0.5F);
      if (random.nextFloat() < 0.5F + b) spawnAtLocation(new ItemStack(ModBlocks.PLANK_GREATWOOD.get()), 0.5F);
      if (random.nextFloat() < 0.5F + b) spawnAtLocation(new ItemStack(ModBlocks.PLANK_GREATWOOD.get()), 0.5F);
   }

   @Override
   public int getMaxHeadXRot() {
      return 20;
   }

   /** EntityAIWatchTarget: keeps looking at the current target for a while. */
   protected static class WatchTargetGoal extends Goal {
      protected final Mob watcher;
      @Nullable
      protected Entity closest;
      private int lookTime;

      public WatchTargetGoal(Mob watcher) {
         this.watcher = watcher;
         setFlags(EnumSet.of(Flag.LOOK));
      }

      @Override
      public boolean canUse() {
         if (watcher.getTarget() != null) closest = watcher.getTarget();
         return closest != null;
      }

      @Override
      public boolean canContinueToUse() {
         float d = (float) targetDistance();
         return closest != null && closest.isAlive() && !(watcher.distanceToSqr(closest) > d * d) && lookTime > 0;
      }

      @Override
      public void start() {
         lookTime = 40 + watcher.getRandom().nextInt(40);
      }

      @Override
      public void stop() {
         closest = null;
      }

      @Override
      public void tick() {
         if (closest == null) return;
         watcher.getLookControl().setLookAt(closest.getX(), closest.getEyeY(), closest.getZ(), 10.0F, watcher.getMaxHeadXRot());
         lookTime--;
      }

      protected double targetDistance() {
         var a = watcher.getAttribute(Attributes.FOLLOW_RANGE);
         return a == null ? 16.0 : a.getValue();
      }
   }

   static float sqrtSin(float f) {
      return Mth.sin(Mth.sqrt(f) * (float) Math.PI * 2.0F);
   }
}
