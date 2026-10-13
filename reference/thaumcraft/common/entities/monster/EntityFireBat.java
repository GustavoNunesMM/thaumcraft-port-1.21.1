package thaumcraft.common.entities.monster;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.DamageSource;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class EntityFireBat extends EntityMob {
   private BlockPos currentFlightTarget;
   public EntityLivingBase owner = null;
   private static final DataParameter<Boolean> HANGING = EntityDataManager.createKey(EntityFireBat.class, DataSerializers.BOOLEAN);
   public int damBonus = 0;
   private int attackTime;

   public EntityFireBat(World par1World) {
      super(par1World);
      this.setSize(0.5F, 0.9F);
      this.setIsBatHanging(true);
      this.isImmuneToFire = true;
   }

   public void entityInit() {
      super.entityInit();
      this.getDataManager().register(HANGING, false);
   }

   @SideOnly(Side.CLIENT)
   public int getBrightnessForRender() {
      return 15728880;
   }

   public float getBrightness() {
      return 1.0F;
   }

   protected float getSoundVolume() {
      return 0.1F;
   }

   protected float getSoundPitch() {
      return super.getSoundPitch() * 0.95F;
   }

   protected SoundEvent getAmbientSound() {
      return this.getIsBatHanging() && this.rand.nextInt(4) != 0 ? null : SoundEvents.ENTITY_BAT_AMBIENT;
   }

   protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
      return SoundEvents.ENTITY_BAT_HURT;
   }

   protected SoundEvent getDeathSound() {
      return SoundEvents.ENTITY_BAT_DEATH;
   }

   public boolean canBePushed() {
      return false;
   }

   protected void applyEntityAttributes() {
      super.applyEntityAttributes();
      this.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(5.0);
      this.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).setBaseValue(1.0);
   }

   public boolean getIsBatHanging() {
      return (Boolean)this.getDataManager().get(HANGING);
   }

   public void setIsBatHanging(boolean par1) {
      this.getDataManager().set(HANGING, par1);
   }

   public void onLivingUpdate() {
      if (this.isWet()) {
         this.attackEntityFrom(DamageSource.DROWN, 1.0F);
      }

      super.onLivingUpdate();
   }

   public void onUpdate() {
      super.onUpdate();
      if (this.getIsBatHanging()) {
         this.motionX = this.motionY = this.motionZ = 0.0;
         this.posY = MathHelper.floor(this.posY) + 1.0 - this.height;
      } else {
         this.motionY *= 0.6F;
      }
   }

   protected void updateAITasks() {
      super.updateAITasks();
      if (this.attackTime > 0) {
         this.attackTime--;
      }

      BlockPos blockpos = new BlockPos(this);
      BlockPos blockpos1 = blockpos.up();
      if (this.getIsBatHanging()) {
         if (!this.world.getBlockState(blockpos1).isNormalCube()) {
            this.setIsBatHanging(false);
            this.world.playEvent((EntityPlayer)null, 1025, blockpos, 0);
         } else {
            if (this.rand.nextInt(200) == 0) {
               this.rotationYawHead = this.rand.nextInt(360);
            }

            if (this.world.getClosestPlayerToEntity(this, 4.0) != null) {
               this.setIsBatHanging(false);
               this.world.playEvent((EntityPlayer)null, 1025, blockpos, 0);
            }
         }
      } else if (this.getAttackTarget() == null) {
         if (this.currentFlightTarget != null && (!this.world.isAirBlock(this.currentFlightTarget) || this.currentFlightTarget.getY() < 1)) {
            this.currentFlightTarget = null;
         }

         if (this.currentFlightTarget == null || this.rand.nextInt(30) == 0 || this.getDistanceSqToCenter(this.currentFlightTarget) < 4.0) {
            this.currentFlightTarget = new BlockPos(
               (int)this.posX + this.rand.nextInt(7) - this.rand.nextInt(7),
               (int)this.posY + this.rand.nextInt(6) - 2,
               (int)this.posZ + this.rand.nextInt(7) - this.rand.nextInt(7)
            );
         }

         double var1 = this.currentFlightTarget.getX() + 0.5 - this.posX;
         double var3 = this.currentFlightTarget.getY() + 0.1 - this.posY;
         double var5 = this.currentFlightTarget.getZ() + 0.5 - this.posZ;
         this.motionX = this.motionX + (Math.signum(var1) * 0.5 - this.motionX) * 0.1F;
         this.motionY = this.motionY + (Math.signum(var3) * 0.7F - this.motionY) * 0.1F;
         this.motionZ = this.motionZ + (Math.signum(var5) * 0.5 - this.motionZ) * 0.1F;
         float var7 = (float)(Math.atan2(this.motionZ, this.motionX) * 180.0 / Math.PI) - 90.0F;
         float var8 = MathHelper.wrapDegrees(var7 - this.rotationYaw);
         this.moveForward = 0.5F;
         this.rotationYaw += var8;
         if (this.rand.nextInt(100) == 0 && this.world.getBlockState(blockpos1).isNormalCube()) {
            this.setIsBatHanging(true);
         }
      } else {
         double var1 = this.getAttackTarget().posX - this.posX;
         double var3 = this.getAttackTarget().posY + this.getAttackTarget().getEyeHeight() * 0.66F - this.posY;
         double var5 = this.getAttackTarget().posZ - this.posZ;
         this.motionX = this.motionX + (Math.signum(var1) * 0.5 - this.motionX) * 0.1F;
         this.motionY = this.motionY + (Math.signum(var3) * 0.7F - this.motionY) * 0.1F;
         this.motionZ = this.motionZ + (Math.signum(var5) * 0.5 - this.motionZ) * 0.1F;
         float var7 = (float)(Math.atan2(this.motionZ, this.motionX) * 180.0 / Math.PI) - 90.0F;
         float var8 = MathHelper.wrapDegrees(var7 - this.rotationYaw);
         this.moveForward = 0.5F;
         this.rotationYaw += var8;
      }

      if (this.getAttackTarget() == null) {
         this.setAttackTarget(this.findPlayerToAttack());
      } else if (this.getAttackTarget().isEntityAlive()) {
         float f = this.getAttackTarget().getDistance(this);
         if (this.isEntityAlive() && this.canEntityBeSeen(this.getAttackTarget())) {
            this.attackEntity(this.getAttackTarget(), f);
         }
      } else {
         this.setAttackTarget(null);
      }

      if (this.getAttackTarget() instanceof EntityPlayer && ((EntityPlayer)this.getAttackTarget()).capabilities.disableDamage) {
         this.setAttackTarget(null);
      }
   }

   protected boolean canTriggerWalking() {
      return false;
   }

   public void fall(float par1, float damageMultiplier) {
   }

   protected void updateFallState(double p_180433_1_, boolean p_180433_3_, IBlockState state, BlockPos pos) {
   }

   public boolean doesEntityNotTriggerPressurePlate() {
      return true;
   }

   public boolean attackEntityFrom(DamageSource par1DamageSource, float par2) {
      if (!this.isEntityInvulnerable(par1DamageSource) && !par1DamageSource.isFireDamage() && !par1DamageSource.isExplosion()) {
         if (!this.world.isRemote && this.getIsBatHanging()) {
            this.setIsBatHanging(false);
         }

         return super.attackEntityFrom(par1DamageSource, par2);
      } else {
         return false;
      }
   }

   protected void attackEntity(Entity entity, float par2) {
      if (this.attackTime <= 0
         && par2 < Math.max(2.5F, entity.width * 1.1F)
         && entity.getEntityBoundingBox().maxY > this.getEntityBoundingBox().minY
         && entity.getEntityBoundingBox().minY < this.getEntityBoundingBox().maxY) {
         this.attackTime = 20 + this.world.rand.nextInt(20);
         if (this.world.rand.nextInt(10) == 0 && !this.world.isRemote) {
            entity.hurtResistantTime = 0;
            this.world.newExplosion(this, this.posX, this.posY, this.posZ, 1.5F, false, false);
            this.setDead();
         }

         this.playSound(SoundEvents.ENTITY_BAT_HURT, 0.5F, 0.9F + this.world.rand.nextFloat() * 0.2F);
         this.attackEntityAsMob(entity);
      }
   }

   protected EntityLivingBase findPlayerToAttack() {
      double var1 = 12.0;
      return this.world.getClosestPlayerToEntity(this, var1);
   }

   public void readEntityFromNBT(NBTTagCompound nbt) {
      super.readEntityFromNBT(nbt);
      this.setIsBatHanging(nbt.getBoolean("hang"));
      this.damBonus = nbt.getByte("damBonus");
   }

   public void writeEntityToNBT(NBTTagCompound nbt) {
      super.writeEntityToNBT(nbt);
      nbt.setBoolean("hang", this.getIsBatHanging());
      nbt.setByte("damBonus", (byte)this.damBonus);
   }

   public boolean getCanSpawnHere() {
      int i = MathHelper.floor(this.posX);
      int j = MathHelper.floor(this.getEntityBoundingBox().minY);
      int k = MathHelper.floor(this.posZ);
      BlockPos blockpos = new BlockPos(i, j, k);
      int var4 = this.world.getLight(blockpos);
      byte var5 = 7;
      return var4 > this.rand.nextInt(var5) ? false : super.getCanSpawnHere();
   }

   protected Item getDropItem() {
      return Items.GUNPOWDER;
   }

   protected boolean isValidLightLevel() {
      return true;
   }
}
