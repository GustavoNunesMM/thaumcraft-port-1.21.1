package thaumcraft.common.entities.monster.tainted;

import java.util.ArrayList;
import net.minecraft.client.particle.Particle;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.item.Item;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import thaumcraft.api.entities.ITaintedMob;
import thaumcraft.client.fx.FXDispatcher;
import thaumcraft.common.blocks.world.taint.TaintHelper;
import thaumcraft.common.config.ConfigItems;
import thaumcraft.common.lib.SoundsTC;

public class EntityTaintSwarm extends EntityMob implements ITaintedMob {
   private BlockPos currentFlightTarget;
   private static final DataParameter<Boolean> SUMMONED = EntityDataManager.createKey(EntityTaintSwarm.class, DataSerializers.BOOLEAN);
   public int damBonus = 0;
   public ArrayList swarm = new ArrayList();
   private int attackTime;

   public EntityTaintSwarm(World par1World) {
      super(par1World);
      this.setSize(2.0F, 2.0F);
   }

   public boolean canAttackClass(Class clazz) {
      return !ITaintedMob.class.isAssignableFrom(clazz);
   }

   public boolean isOnSameTeam(Entity otherEntity) {
      return otherEntity instanceof ITaintedMob || super.isOnSameTeam(otherEntity);
   }

   protected void entityInit() {
      super.entityInit();
      this.getDataManager().register(SUMMONED, false);
   }

   @SideOnly(Side.CLIENT)
   public int getBrightnessForRender() {
      return 15728880;
   }

   protected boolean canDespawn() {
      return true;
   }

   public float getBrightness() {
      return 1.0F;
   }

   protected float getSoundVolume() {
      return 0.1F;
   }

   protected SoundEvent getAmbientSound() {
      return SoundsTC.swarm;
   }

   protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
      return SoundsTC.swarmattack;
   }

   protected SoundEvent getDeathSound() {
      return SoundsTC.swarmattack;
   }

   public boolean canBePushed() {
      return false;
   }

   protected void applyEntityAttributes() {
      super.applyEntityAttributes();
      this.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(30.0);
      this.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).setBaseValue(2 + this.damBonus);
   }

   public boolean getIsSummoned() {
      return (Boolean)this.getDataManager().get(SUMMONED);
   }

   public void setIsSummoned(boolean par1) {
      this.getDataManager().set(SUMMONED, par1);
   }

   public void onUpdate() {
      super.onUpdate();
      this.motionY *= 0.6F;
      if (this.world.isRemote) {
         for (int a = 0; a < this.swarm.size(); a++) {
            if (this.swarm.get(a) == null || !((Particle)this.swarm.get(a)).isAlive()) {
               this.swarm.remove(a);
               break;
            }
         }

         if (this.swarm.size() < 30) {
            this.swarm.add(FXDispatcher.INSTANCE.swarmParticleFX(this, 0.22F, 15.0F, 0.08F));
         }
      }
   }

   public void onLivingUpdate() {
      super.onLivingUpdate();
      if (this.attackTime > 0) {
         this.attackTime--;
      }

      if (this.getAttackTarget() == null) {
         if (this.getIsSummoned()) {
            this.attackEntityFrom(DamageSource.GENERIC, 5.0F);
         }

         if (this.currentFlightTarget != null
            && (
               !this.world.isAirBlock(this.currentFlightTarget)
                  || this.currentFlightTarget.getY() < 1
                  || this.currentFlightTarget.getY() > this.world.getPrecipitationHeight(this.currentFlightTarget).up(2).getY()
                  || !TaintHelper.isNearTaintSeed(this.world, this.currentFlightTarget)
            )) {
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
         this.motionX = this.motionX + (Math.signum(var1) * 0.5 - this.motionX) * 0.015000000014901161;
         this.motionY = this.motionY + (Math.signum(var3) * 0.7F - this.motionY) * 0.1F;
         this.motionZ = this.motionZ + (Math.signum(var5) * 0.5 - this.motionZ) * 0.015000000014901161;
         float var7 = (float)(Math.atan2(this.motionZ, this.motionX) * 180.0 / Math.PI) - 90.0F;
         float var8 = MathHelper.wrapDegrees(var7 - this.rotationYaw);
         this.moveForward = 0.1F;
         this.rotationYaw += var8;
      } else if (this.getAttackTarget() != null) {
         double var1 = this.getAttackTarget().posX - this.posX;
         double var3 = this.getAttackTarget().posY + this.getAttackTarget().getEyeHeight() - this.posY;
         double var5 = this.getAttackTarget().posZ - this.posZ;
         this.motionX = this.motionX + (Math.signum(var1) * 0.5 - this.motionX) * 0.025000000149011613;
         this.motionY = this.motionY + (Math.signum(var3) * 0.7F - this.motionY) * 0.1F;
         this.motionZ = this.motionZ + (Math.signum(var5) * 0.5 - this.motionZ) * 0.02500000001490116;
         float var7 = (float)(Math.atan2(this.motionZ, this.motionX) * 180.0 / Math.PI) - 90.0F;
         float var8 = MathHelper.wrapDegrees(var7 - this.rotationYaw);
         this.moveForward = 0.1F;
         this.rotationYaw += var8;
      }

      if (this.getAttackTarget() == null) {
         this.setAttackTarget((EntityLivingBase)this.findPlayerToAttack());
      } else if (this.isEntityAlive() && this.getAttackTarget().isEntityAlive()) {
         float f = this.getAttackTarget().getDistance(this);
         if (this.canEntityBeSeen(this.getAttackTarget())) {
            this.attackEntity(this.getAttackTarget(), f);
         }
      } else {
         this.setAttackTarget(null);
      }

      if (this.getAttackTarget() instanceof EntityPlayer && ((EntityPlayer)this.getAttackTarget()).capabilities.disableDamage) {
         this.setAttackTarget(null);
      }
   }

   protected void updateAITasks() {
      super.updateAITasks();
   }

   protected boolean canTriggerWalking() {
      return false;
   }

   public void fall(float distance, float damageMultiplier) {
   }

   public boolean doesEntityNotTriggerPressurePlate() {
      return true;
   }

   public boolean attackEntityFrom(DamageSource par1DamageSource, float par2) {
      return this.isEntityInvulnerable(par1DamageSource) ? false : super.attackEntityFrom(par1DamageSource, par2);
   }

   protected void attackEntity(Entity par1Entity, float par2) {
      if (this.attackTime <= 0
         && par2 < 3.0F
         && par1Entity.getEntityBoundingBox().maxY > this.getEntityBoundingBox().minY
         && par1Entity.getEntityBoundingBox().minY < this.getEntityBoundingBox().maxY) {
         if (this.getIsSummoned()) {
            ((EntityLivingBase)par1Entity).recentlyHit = 100;
         }

         this.attackTime = 15 + this.rand.nextInt(10);
         double mx = par1Entity.motionX;
         double my = par1Entity.motionY;
         double mz = par1Entity.motionZ;
         if (this.attackEntityAsMob(par1Entity) && !this.world.isRemote && par1Entity instanceof EntityLivingBase) {
            ((EntityLivingBase)par1Entity).addPotionEffect(new PotionEffect(MobEffects.WEAKNESS, 100, 0));
         }

         par1Entity.isAirBorne = false;
         par1Entity.motionX = mx;
         par1Entity.motionY = my;
         par1Entity.motionZ = mz;
         this.playSound(SoundsTC.swarmattack, 0.3F, 0.9F + this.world.rand.nextFloat() * 0.2F);
      }
   }

   protected Entity findPlayerToAttack() {
      double var1 = 8.0;
      return this.getIsSummoned() ? null : this.world.getClosestPlayerToEntity(this, var1);
   }

   public void readEntityFromNBT(NBTTagCompound par1NBTTagCompound) {
      super.readEntityFromNBT(par1NBTTagCompound);
      this.setIsSummoned(par1NBTTagCompound.getBoolean("summoned"));
      this.damBonus = par1NBTTagCompound.getByte("damBonus");
   }

   public void writeEntityToNBT(NBTTagCompound par1NBTTagCompound) {
      super.writeEntityToNBT(par1NBTTagCompound);
      par1NBTTagCompound.setBoolean("summoned", this.getIsSummoned());
      par1NBTTagCompound.setByte("damBonus", (byte)this.damBonus);
   }

   public boolean getCanSpawnHere() {
      int var1 = MathHelper.floor(this.getEntityBoundingBox().minY);
      int var2 = MathHelper.floor(this.posX);
      int var3 = MathHelper.floor(this.posZ);
      int var4 = this.world.getLight(new BlockPos(var2, var1, var3));
      byte var5 = 7;
      return var4 > this.rand.nextInt(var5) ? false : super.getCanSpawnHere();
   }

   protected boolean isValidLightLevel() {
      return true;
   }

   protected Item getDropItem() {
      return Item.getItemById(0);
   }

   protected void dropFewItems(boolean flag, int i) {
      if (this.world.rand.nextBoolean()) {
         this.entityDropItem(ConfigItems.FLUX_CRYSTAL.copy(), this.height / 2.0F);
      }
   }
}
