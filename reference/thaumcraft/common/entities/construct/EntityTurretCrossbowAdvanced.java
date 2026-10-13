package thaumcraft.common.entities.construct;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.IEntityOwnable;
import net.minecraft.entity.MoverType;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIAttackRanged;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.ai.EntityAIHurtByTarget;
import net.minecraft.entity.ai.EntityAITarget;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.passive.IAnimals;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.scoreboard.Team;
import net.minecraft.util.EntitySelectors;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.FMLCommonHandler;
import org.apache.commons.lang3.StringUtils;
import thaumcraft.Thaumcraft;
import thaumcraft.api.blocks.BlocksTC;
import thaumcraft.api.items.ItemsTC;
import thaumcraft.common.lib.SoundsTC;
import thaumcraft.common.lib.utils.Utils;

public class EntityTurretCrossbowAdvanced extends EntityTurretCrossbow {
   private static final DataParameter<Byte> FLAGS = EntityDataManager.createKey(EntityTurretCrossbowAdvanced.class, DataSerializers.BYTE);

   public EntityTurretCrossbowAdvanced(World worldIn) {
      super(worldIn);
      this.setSize(0.95F, 1.5F);
      this.stepHeight = 0.0F;
   }

   @Override
   protected void initEntityAI() {
      this.tasks.taskEntries.clear();
      this.targetTasks.taskEntries.clear();
      this.tasks.addTask(1, new EntityAIAttackRanged(this, 0.0, 20, 40, 24.0F));
      this.tasks.addTask(2, new EntityTurretCrossbowAdvanced.EntityAIWatchTarget(this));
      this.targetTasks.addTask(1, new EntityAIHurtByTarget(this, false, new Class[0]));
      this.targetTasks
         .addTask(2, new EntityTurretCrossbowAdvanced.EntityAINearestValidTarget(this, EntityLivingBase.class, 5, true, false, (Predicate)null));
      this.setTargetMob(true);
   }

   @Override
   public float getEyeHeight() {
      return 1.0F;
   }

   public EntityTurretCrossbowAdvanced(World worldIn, BlockPos pos) {
      this(worldIn);
      this.setPositionAndRotation(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0.0F, 0.0F);
   }

   @Override
   protected void entityInit() {
      super.entityInit();
      this.getDataManager().register(FLAGS, (byte)0);
   }

   public boolean canAttackClass(Class clazz) {
      if (IAnimals.class.isAssignableFrom(clazz) && !IMob.class.isAssignableFrom(clazz) && this.getTargetAnimal()) {
         return true;
      } else if (IMob.class.isAssignableFrom(clazz) && this.getTargetMob()) {
         return true;
      } else if (!EntityPlayer.class.isAssignableFrom(clazz) || !this.getTargetPlayer()) {
         return false;
      } else if (!this.world.isRemote && !FMLCommonHandler.instance().getMinecraftServerInstance().isPVPEnabled() && !this.getTargetFriendly()) {
         this.setTargetPlayer(false);
         return false;
      } else {
         return true;
      }
   }

   public boolean getTargetAnimal() {
      return Utils.getBit((Byte)this.getDataManager().get(FLAGS), 0);
   }

   public void setTargetAnimal(boolean par1) {
      byte var2 = (Byte)this.getDataManager().get(FLAGS);
      if (par1) {
         this.getDataManager().set(FLAGS, (byte)Utils.setBit(var2, 0));
      } else {
         this.getDataManager().set(FLAGS, (byte)Utils.clearBit(var2, 0));
      }

      this.setAttackTarget(null);
   }

   public boolean getTargetMob() {
      return Utils.getBit((Byte)this.getDataManager().get(FLAGS), 1);
   }

   public void setTargetMob(boolean par1) {
      byte var2 = (Byte)this.getDataManager().get(FLAGS);
      if (par1) {
         this.getDataManager().set(FLAGS, (byte)Utils.setBit(var2, 1));
      } else {
         this.getDataManager().set(FLAGS, (byte)Utils.clearBit(var2, 1));
      }

      this.setAttackTarget(null);
   }

   public boolean getTargetPlayer() {
      return Utils.getBit((Byte)this.getDataManager().get(FLAGS), 2);
   }

   public void setTargetPlayer(boolean par1) {
      byte var2 = (Byte)this.getDataManager().get(FLAGS);
      if (par1) {
         this.getDataManager().set(FLAGS, (byte)Utils.setBit(var2, 2));
      } else {
         this.getDataManager().set(FLAGS, (byte)Utils.clearBit(var2, 2));
      }

      this.setAttackTarget(null);
   }

   public boolean getTargetFriendly() {
      return Utils.getBit((Byte)this.getDataManager().get(FLAGS), 3);
   }

   public void setTargetFriendly(boolean par1) {
      byte var2 = (Byte)this.getDataManager().get(FLAGS);
      if (par1) {
         this.getDataManager().set(FLAGS, (byte)Utils.setBit(var2, 3));
      } else {
         this.getDataManager().set(FLAGS, (byte)Utils.clearBit(var2, 3));
      }

      this.setAttackTarget(null);
   }

   @Override
   public Team getTeam() {
      if (this.isOwned()) {
         EntityLivingBase entitylivingbase = this.getOwnerEntity();
         if (entitylivingbase != null) {
            return entitylivingbase.getTeam();
         }
      }

      return super.getTeam();
   }

   @Override
   protected void applyEntityAttributes() {
      super.applyEntityAttributes();
      this.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(40.0);
   }

   @Override
   public int getTotalArmorValue() {
      return 8;
   }

   @Override
   public void onUpdate() {
      super.onUpdate();
      if (!this.world.isRemote
         && !FMLCommonHandler.instance().getMinecraftServerInstance().isPVPEnabled()
         && this.getAttackTarget() != null
         && this.getAttackTarget() instanceof EntityPlayer
         && this.getAttackTarget() != this.getOwnerEntity()) {
         this.setAttackTarget(null);
      }
   }

   @Override
   public void readEntityFromNBT(NBTTagCompound nbt) {
      super.readEntityFromNBT(nbt);
      this.getDataManager().set(FLAGS, nbt.getByte("targets"));
   }

   @Override
   public void writeEntityToNBT(NBTTagCompound nbt) {
      super.writeEntityToNBT(nbt);
      nbt.setByte("targets", (Byte)this.getDataManager().get(FLAGS));
   }

   @Override
   public void knockBack(Entity entityIn, float strength, double xRatio, double zRatio) {
      super.knockBack(entityIn, strength, xRatio / 10.0, zRatio / 10.0);
   }

   @Override
   protected boolean processInteract(EntityPlayer player, EnumHand hand) {
      if (!this.world.isRemote && this.isOwner(player) && !this.isDead) {
         if (player.isSneaking()) {
            this.playSound(SoundsTC.zap, 1.0F, 1.0F);
            this.dropAmmo();
            this.entityDropItem(new ItemStack(ItemsTC.turretPlacer, 1, 1), 0.5F);
            this.setDead();
            player.swingArm(hand);
         } else {
            player.openGui(Thaumcraft.instance, 17, this.world, this.getEntityId(), 0, 0);
         }

         return true;
      } else {
         return super.processInteract(player, hand);
      }
   }

   @Override
   public void move(MoverType t, double x, double y, double z) {
      super.move(t, x / 15.0, y, z / 15.0);
   }

   @Override
   protected void dropFewItems(boolean wasRecentlyHit, int lootingModifier) {
      float b = lootingModifier * 0.15F;
      if (this.rand.nextFloat() < 0.2F + b) {
         this.entityDropItem(new ItemStack(ItemsTC.mind, 1, 1), 0.5F);
      }

      if (this.rand.nextFloat() < 0.5F + b) {
         this.entityDropItem(new ItemStack(ItemsTC.mechanismSimple), 0.5F);
      }

      if (this.rand.nextFloat() < 0.5F + b) {
         this.entityDropItem(new ItemStack(BlocksTC.plankGreatwood), 0.5F);
      }

      if (this.rand.nextFloat() < 0.5F + b) {
         this.entityDropItem(new ItemStack(BlocksTC.plankGreatwood), 0.5F);
      }

      if (this.rand.nextFloat() < 0.3F + b) {
         this.entityDropItem(new ItemStack(ItemsTC.plate, 1, 0), 0.5F);
      }

      if (this.rand.nextFloat() < 0.4F + b) {
         this.entityDropItem(new ItemStack(ItemsTC.plate, 1, 1), 0.5F);
      }

      if (this.rand.nextFloat() < 0.4F + b) {
         this.entityDropItem(new ItemStack(ItemsTC.plate, 1, 1), 0.5F);
      }
   }

   protected class EntityAINearestValidTarget extends EntityAITarget {
      protected final Class targetClass;
      private final int targetChance;
      protected final net.minecraft.entity.ai.EntityAINearestAttackableTarget.Sorter theNearestAttackableTargetSorter;
      protected Predicate targetEntitySelector;
      protected EntityLivingBase targetEntity;
      private int targetUnseenTicks;

      public EntityAINearestValidTarget(EntityCreature p_i45878_1_, Class p_i45878_2_, boolean p_i45878_3_) {
         this(p_i45878_1_, p_i45878_2_, p_i45878_3_, false);
      }

      public EntityAINearestValidTarget(EntityCreature p_i45879_1_, Class p_i45879_2_, boolean p_i45879_3_, boolean p_i45879_4_) {
         this(p_i45879_1_, p_i45879_2_, 10, p_i45879_3_, p_i45879_4_, (Predicate)null);
      }

      public EntityAINearestValidTarget(
         EntityCreature p_i45880_1_, Class p_i45880_2_, int p_i45880_3_, boolean p_i45880_4_, boolean p_i45880_5_, final Predicate tselector
      ) {
         super(p_i45880_1_, p_i45880_4_, p_i45880_5_);
         this.targetClass = p_i45880_2_;
         this.targetChance = p_i45880_3_;
         this.theNearestAttackableTargetSorter = new net.minecraft.entity.ai.EntityAINearestAttackableTarget.Sorter(p_i45880_1_);
         this.setMutexBits(1);
         this.targetEntitySelector = new Predicate() {
            private static final String __OBFID = "CL_00001621";

            public boolean applySelection(EntityLivingBase entity) {
               if (tselector != null && !tselector.apply(entity)) {
                  return false;
               } else {
                  if (entity instanceof EntityPlayer) {
                     double d0 = EntityAINearestValidTarget.this.getTargetDistance();
                     if (entity.isSneaking()) {
                        d0 *= 0.8F;
                     }

                     if (entity.isInvisible()) {
                        float f = ((EntityPlayer)entity).getArmorVisibility();
                        if (f < 0.1F) {
                           f = 0.1F;
                        }

                        d0 *= 0.7F * f;
                     }

                     if (entity.getDistance(EntityAINearestValidTarget.this.taskOwner) > d0) {
                        return false;
                     }
                  }

                  return EntityAINearestValidTarget.this.isSuitableTarget(entity, false);
               }
            }

            public boolean apply(Object p_apply_1_) {
               return this.applySelection((EntityLivingBase)p_apply_1_);
            }
         };
      }

      public boolean shouldContinueExecuting() {
         EntityLivingBase entitylivingbase = this.taskOwner.getAttackTarget();
         if (entitylivingbase == null) {
            return false;
         } else if (!entitylivingbase.isEntityAlive()) {
            return false;
         } else {
            Team team = this.taskOwner.getTeam();
            Team team1 = entitylivingbase.getTeam();
            if (team != null && team1 == team && !((EntityTurretCrossbowAdvanced)this.taskOwner).getTargetFriendly()) {
               return false;
            } else if (team != null && team1 != team && ((EntityTurretCrossbowAdvanced)this.taskOwner).getTargetFriendly()) {
               return false;
            } else {
               double d0 = this.getTargetDistance();
               if (this.taskOwner.getDistanceSq(entitylivingbase) > d0 * d0) {
                  return false;
               } else {
                  if (this.shouldCheckSight) {
                     if (this.taskOwner.getEntitySenses().canSee(entitylivingbase)) {
                        this.targetUnseenTicks = 0;
                     } else if (++this.targetUnseenTicks > 60) {
                        return false;
                     }
                  }

                  return true;
               }
            }
         }
      }

      protected boolean isSuitableTarget(EntityLivingBase target, boolean includeInvincibles) {
         return !this.isGoodTarget(this.taskOwner, target, includeInvincibles, this.shouldCheckSight)
            ? false
            : this.taskOwner.isWithinHomeDistanceFromPosition(new BlockPos(target));
      }

      private boolean isGoodTarget(EntityLiving attacker, EntityLivingBase posTar, boolean includeInvincibles, boolean checkSight) {
         if (posTar == null) {
            return false;
         } else if (posTar == attacker) {
            return false;
         } else if (!posTar.isEntityAlive()) {
            return false;
         } else if (!attacker.canAttackClass(posTar.getClass())) {
            return false;
         } else {
            Team team = attacker.getTeam();
            Team team1 = posTar.getTeam();
            if (team != null && team1 == team && !((EntityTurretCrossbowAdvanced)attacker).getTargetFriendly()) {
               return false;
            } else if (team != null && team1 != team && ((EntityTurretCrossbowAdvanced)attacker).getTargetFriendly()) {
               return false;
            } else {
               if (attacker instanceof IEntityOwnable && StringUtils.isNotEmpty(((IEntityOwnable)attacker).getOwnerId().toString())) {
                  if (posTar instanceof IEntityOwnable
                     && ((IEntityOwnable)attacker).getOwnerId().equals(((IEntityOwnable)posTar).getOwnerId())
                     && !((EntityTurretCrossbowAdvanced)attacker).getTargetFriendly()) {
                     return false;
                  }

                  if (!(posTar instanceof IEntityOwnable) && !(posTar instanceof EntityPlayer) && ((EntityTurretCrossbowAdvanced)attacker).getTargetFriendly()) {
                     return false;
                  }

                  if (posTar instanceof IEntityOwnable
                     && !((IEntityOwnable)attacker).getOwnerId().equals(((IEntityOwnable)posTar).getOwnerId())
                     && ((EntityTurretCrossbowAdvanced)attacker).getTargetFriendly()) {
                     return false;
                  }

                  if (posTar == ((IEntityOwnable)attacker).getOwner() && !((EntityTurretCrossbowAdvanced)attacker).getTargetFriendly()) {
                     return false;
                  }
               } else if (posTar instanceof EntityPlayer
                  && !includeInvincibles
                  && ((EntityPlayer)posTar).capabilities.disableDamage
                  && !((EntityTurretCrossbowAdvanced)attacker).getTargetFriendly()) {
                  return false;
               }

               return !checkSight || attacker.getEntitySenses().canSee(posTar);
            }
         }
      }

      public boolean shouldExecute() {
         if (this.targetChance > 0 && this.taskOwner.getRNG().nextInt(this.targetChance) != 0) {
            return false;
         } else {
            double d0 = this.getTargetDistance();
            List list = this.taskOwner
               .world
               .getEntitiesWithinAABB(
                  this.targetClass,
                  this.taskOwner.getEntityBoundingBox().grow(d0, 4.0, d0),
                  Predicates.and(this.targetEntitySelector, EntitySelectors.NOT_SPECTATING)
               );
            Collections.sort(list, this.theNearestAttackableTargetSorter);
            if (list.isEmpty()) {
               return false;
            } else {
               this.targetEntity = (EntityLivingBase)list.get(0);
               return true;
            }
         }
      }

      public void startExecuting() {
         this.taskOwner.setAttackTarget(this.targetEntity);
         this.targetUnseenTicks = 0;
         super.startExecuting();
      }

      public class Sorter implements Comparator {
         private final Entity theEntity;
         private static final String __OBFID = "CL_00001622";

         public Sorter(Entity p_i1662_1_) {
            this.theEntity = p_i1662_1_;
         }

         public int compare(Entity p_compare_1_, Entity p_compare_2_) {
            double d0 = this.theEntity.getDistanceSq(p_compare_1_);
            double d1 = this.theEntity.getDistanceSq(p_compare_2_);
            return d0 < d1 ? -1 : (d0 > d1 ? 1 : 0);
         }

         @Override
         public int compare(Object p_compare_1_, Object p_compare_2_) {
            return this.compare((Entity)p_compare_1_, (Entity)p_compare_2_);
         }
      }
   }

   protected class EntityAIWatchTarget extends EntityAIBase {
      protected EntityLiving theWatcher;
      protected Entity closestEntity;
      private int lookTime;

      public EntityAIWatchTarget(EntityLiving p_i1631_1_) {
         this.theWatcher = p_i1631_1_;
         this.setMutexBits(2);
      }

      public boolean shouldExecute() {
         if (this.theWatcher.getAttackTarget() != null) {
            this.closestEntity = this.theWatcher.getAttackTarget();
         }

         return this.closestEntity != null;
      }

      public boolean shouldContinueExecuting() {
         float d = (float)this.getTargetDistance();
         return !this.closestEntity.isEntityAlive() ? false : (this.theWatcher.getDistanceSq(this.closestEntity) > d * d ? false : this.lookTime > 0);
      }

      public void startExecuting() {
         this.lookTime = 40 + this.theWatcher.getRNG().nextInt(40);
      }

      public void resetTask() {
         this.closestEntity = null;
      }

      public void updateTask() {
         this.theWatcher
            .getLookHelper()
            .setLookPosition(
               this.closestEntity.posX,
               this.closestEntity.posY + this.closestEntity.getEyeHeight(),
               this.closestEntity.posZ,
               10.0F,
               this.theWatcher.getVerticalFaceSpeed()
            );
         this.lookTime--;
      }

      protected double getTargetDistance() {
         IAttributeInstance iattributeinstance = this.theWatcher.getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE);
         return iattributeinstance == null ? 16.0 : iattributeinstance.getAttributeValue();
      }
   }
}
