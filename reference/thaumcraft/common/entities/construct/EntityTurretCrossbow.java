package thaumcraft.common.entities.construct;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import net.minecraft.block.BlockRailBase;
import net.minecraft.block.BlockRailPowered;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.IRangedAttackMob;
import net.minecraft.entity.MoverType;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIAttackRanged;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.ai.EntityAIHurtByTarget;
import net.minecraft.entity.ai.EntityAITarget;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityTippedArrow;
import net.minecraft.entity.projectile.EntityArrow.PickupStatus;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemArrow;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.scoreboard.Team;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityDispenser;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EntitySelectors;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import thaumcraft.Thaumcraft;
import thaumcraft.api.blocks.BlocksTC;
import thaumcraft.api.items.ItemsTC;
import thaumcraft.common.lib.SoundsTC;

public class EntityTurretCrossbow extends EntityOwnedConstruct implements IRangedAttackMob {
   int loadProgressInt = 0;
   boolean isLoadInProgress = false;
   float loadProgress = 0.0F;
   float prevLoadProgress = 0.0F;
   public float loadProgressForRender = 0.0F;
   boolean attackedLastTick = false;
   int attackCount = 0;

   public EntityTurretCrossbow(World worldIn) {
      super(worldIn);
      this.setSize(0.95F, 1.25F);
      this.stepHeight = 0.0F;
   }

   protected void initEntityAI() {
      this.tasks.addTask(1, new EntityAIAttackRanged(this, 0.0, 20, 60, 24.0F));
      this.tasks.addTask(2, new EntityTurretCrossbow.EntityAIWatchTarget(this));
      this.targetTasks.addTask(1, new EntityAIHurtByTarget(this, false, new Class[0]));
      this.targetTasks.addTask(2, new EntityTurretCrossbow.EntityAINearestValidTarget(this, EntityLiving.class, 5, true, false, IMob.MOB_SELECTOR));
   }

   public EntityTurretCrossbow(World worldIn, BlockPos pos) {
      this(worldIn);
      this.setPositionAndRotation(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0.0F, 0.0F);
   }

   public void attackEntityWithRangedAttack(EntityLivingBase target, float range) {
      if (this.getHeldItemMainhand() != null && !this.getHeldItemMainhand().isEmpty() && this.getHeldItemMainhand().getCount() > 0) {
         EntityTippedArrow entityarrow = new EntityTippedArrow(this.world, this);
         entityarrow.setDamage(2.25 + range * 2.0F + this.rand.nextGaussian() * 0.25);
         entityarrow.setPotionEffect(this.getHeldItemMainhand());
         Vec3d vec3d = this.getLook(1.0F);
         if (!this.isRiding()) {
            entityarrow.posX = entityarrow.posX - vec3d.x * 0.9F;
            entityarrow.posY = entityarrow.posY - vec3d.y * 0.9F;
            entityarrow.posZ = entityarrow.posZ - vec3d.z * 0.9F;
         } else {
            entityarrow.posX = entityarrow.posX + vec3d.x * 1.75;
            entityarrow.posY = entityarrow.posY + vec3d.y * 1.75;
            entityarrow.posZ = entityarrow.posZ + vec3d.z * 1.75;
         }

         entityarrow.pickupStatus = PickupStatus.DISALLOWED;
         double d0 = target.posX - this.posX;
         double d1 = target.getEntityBoundingBox().minY + target.getEyeHeight() + range * range * 3.0F - entityarrow.posY;
         double d2 = target.posZ - this.posZ;
         entityarrow.shoot(d0, d1, d2, 2.0F, 2.0F);
         this.world.spawnEntity(entityarrow);
         this.world.setEntityState(this, (byte)16);
         this.playSound(SoundEvents.ENTITY_ARROW_SHOOT, 1.0F, 1.0F / (this.getRNG().nextFloat() * 0.4F + 0.8F));
         this.getHeldItemMainhand().shrink(1);
         if (this.getHeldItemMainhand().getCount() <= 0) {
            this.setHeldItem(EnumHand.MAIN_HAND, ItemStack.EMPTY);
         }
      }
   }

   @SideOnly(Side.CLIENT)
   public void handleStatusUpdate(byte par1) {
      if (par1 == 16) {
         if (!this.isSwingInProgress) {
            this.swingProgressInt = -1;
            this.isSwingInProgress = true;
         }
      } else if (par1 == 17) {
         if (!this.isLoadInProgress) {
            this.loadProgressInt = -1;
            this.isLoadInProgress = true;
         }
      } else {
         super.handleStatusUpdate(par1);
      }
   }

   @SideOnly(Side.CLIENT)
   public float getLoadProgress(float pt) {
      float f1 = this.loadProgress - this.prevLoadProgress;
      if (f1 < 0.0F) {
         f1++;
      }

      return this.prevLoadProgress + f1 * pt;
   }

   protected void updateArmSwingProgress() {
      if (this.isSwingInProgress) {
         this.swingProgressInt++;
         if (this.swingProgressInt >= 6) {
            this.swingProgressInt = 0;
            this.isSwingInProgress = false;
         }
      } else {
         this.swingProgressInt = 0;
      }

      this.swingProgress = this.swingProgressInt / 6.0F;
      if (this.isLoadInProgress) {
         this.loadProgressInt++;
         if (this.loadProgressInt >= 10) {
            this.loadProgressInt = 0;
            this.isLoadInProgress = false;
         }
      } else {
         this.loadProgressInt = 0;
      }

      this.loadProgress = this.loadProgressInt / 10.0F;
   }

   public void onEntityUpdate() {
      this.prevLoadProgress = this.loadProgress;
      if (!this.world.isRemote
         && (this.getHeldItemMainhand() == null || this.getHeldItemMainhand().isEmpty() || this.getHeldItemMainhand().getCount() <= 0)) {
         BlockPos p = this.getPosition().down();
         TileEntity t = this.world.getTileEntity(p);
         if (t != null && t instanceof TileEntityDispenser && EnumFacing.byIndex(t.getBlockMetadata() & 7) == EnumFacing.UP) {
            TileEntityDispenser d = (TileEntityDispenser)t;

            for (int a = 0; a < d.getSizeInventory(); a++) {
               if (d.getStackInSlot(a) != null && !d.getStackInSlot(a).isEmpty() && d.getStackInSlot(a).getItem() instanceof ItemArrow) {
                  this.setHeldItem(EnumHand.MAIN_HAND, d.decrStackSize(a, d.getStackInSlot(a).getCount()));
                  this.playSound(SoundsTC.ticks, 1.0F, 1.0F);
                  this.world.setEntityState(this, (byte)17);
                  break;
               }
            }
         }
      }

      super.onEntityUpdate();
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

   public float getEyeHeight() {
      return this.height * 0.66F;
   }

   protected void applyEntityAttributes() {
      super.applyEntityAttributes();
      this.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(30.0);
      this.getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).setBaseValue(24.0);
   }

   public int getTotalArmorValue() {
      return 2;
   }

   @Override
   public void onUpdate() {
      super.onUpdate();
      if (this.getAttackTarget() != null && (this.getAttackTarget().isDead || this.isOnSameTeam(this.getAttackTarget()))) {
         this.setAttackTarget(null);
      }

      if (!this.world.isRemote) {
         this.rotationYaw = this.rotationYawHead;
         if (this.ticksExisted % 80 == 0) {
            this.heal(1.0F);
         }

         int k = MathHelper.floor(this.posX);
         int l = MathHelper.floor(this.posY);
         int i1 = MathHelper.floor(this.posZ);
         if (BlockRailBase.isRailBlock(this.world, new BlockPos(k, l - 1, i1))) {
            l--;
         }

         BlockPos blockpos = new BlockPos(k, l, i1);
         IBlockState iblockstate = this.world.getBlockState(blockpos);
         if (BlockRailBase.isRailBlock(iblockstate) && iblockstate.getBlock() == BlocksTC.activatorRail) {
            boolean ac = (Boolean)iblockstate.getValue(BlockRailPowered.POWERED);
            this.setNoAI(ac);
         }
      } else {
         this.updateArmSwingProgress();
      }
   }

   public boolean canBePushed() {
      return true;
   }

   public boolean canBeCollidedWith() {
      return true;
   }

   @Override
   public void readEntityFromNBT(NBTTagCompound nbt) {
      super.readEntityFromNBT(nbt);
   }

   @Override
   public void writeEntityToNBT(NBTTagCompound nbt) {
      super.writeEntityToNBT(nbt);
   }

   public boolean attackEntityFrom(DamageSource source, float amount) {
      this.rotationYaw = (float)(this.rotationYaw + this.getRNG().nextGaussian() * 45.0);
      this.rotationPitch = (float)(this.rotationPitch + this.getRNG().nextGaussian() * 20.0);
      return super.attackEntityFrom(source, amount);
   }

   public void knockBack(Entity entityIn, float strength, double xRatio, double zRatio) {
      super.knockBack(entityIn, strength, xRatio / 10.0, zRatio / 10.0);
      if (this.motionY > 0.1) {
         this.motionY = 0.1;
      }
   }

   @Override
   protected boolean processInteract(EntityPlayer player, EnumHand hand) {
      if (!this.world.isRemote && this.isOwner(player) && !this.isDead) {
         if (player.isSneaking()) {
            this.playSound(SoundsTC.zap, 1.0F, 1.0F);
            this.dropAmmo();
            this.entityDropItem(new ItemStack(ItemsTC.turretPlacer, 1, 0), 0.5F);
            this.setDead();
            player.swingArm(hand);
         } else {
            player.openGui(Thaumcraft.instance, 16, this.world, this.getEntityId(), 0, 0);
         }

         return true;
      } else {
         return super.processInteract(player, hand);
      }
   }

   public void move(MoverType mt, double x, double y, double z) {
      super.move(mt, x / 20.0, y, z / 20.0);
   }

   @Override
   public void onDeath(DamageSource cause) {
      super.onDeath(cause);
      if (!this.world.isRemote) {
         this.dropAmmo();
      }
   }

   protected void dropAmmo() {
      if (this.getHeldItemMainhand() != null && !this.getHeldItemMainhand().isEmpty()) {
         this.entityDropItem(this.getHeldItemMainhand(), 0.5F);
      }
   }

   protected void dropFewItems(boolean wasRecentlyHit, int lootingModifier) {
      float b = lootingModifier * 0.15F;
      if (this.rand.nextFloat() < 0.2F + b) {
         this.entityDropItem(new ItemStack(ItemsTC.mind), 0.5F);
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
   }

   protected RayTraceResult getRayTraceResult() {
      float f = this.prevRotationPitch + (this.rotationPitch - this.prevRotationPitch);
      float f1 = this.prevRotationYaw + (this.rotationYaw - this.prevRotationYaw);
      double d0 = this.prevPosX + (this.posX - this.prevPosX);
      double d1 = this.prevPosY + (this.posY - this.prevPosY) + this.getEyeHeight();
      double d2 = this.prevPosZ + (this.posZ - this.prevPosZ);
      Vec3d vec3 = new Vec3d(d0, d1, d2);
      float f2 = MathHelper.cos(-f1 * (float) (Math.PI / 180.0) - (float) Math.PI);
      float f3 = MathHelper.sin(-f1 * (float) (Math.PI / 180.0) - (float) Math.PI);
      float f4 = -MathHelper.cos(-f * (float) (Math.PI / 180.0));
      float f5 = MathHelper.sin(-f * (float) (Math.PI / 180.0));
      float f6 = f3 * f4;
      float f7 = f2 * f4;
      double d3 = 5.0;
      Vec3d vec31 = vec3.add(f6 * d3, f5 * d3, f7 * d3);
      return this.world.rayTraceBlocks(vec3, vec31, true, false, false);
   }

   public int getVerticalFaceSpeed() {
      return 20;
   }

   public void setSwingingArms(boolean swingingArms) {
   }

   protected class EntityAINearestValidTarget extends EntityAITarget {
      protected final Class targetClass;
      private final int targetChance;
      protected final net.minecraft.entity.ai.EntityAINearestAttackableTarget.Sorter theNearestAttackableTargetSorter;
      protected Predicate targetEntitySelector;
      protected EntityLivingBase targetEntity;

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

      protected boolean isSuitableTarget(EntityLivingBase target, boolean includeInvincibles) {
         return !isSuitableTarget(this.taskOwner, target, includeInvincibles, this.shouldCheckSight)
            ? false
            : this.taskOwner.isWithinHomeDistanceFromPosition(new BlockPos(target));
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
