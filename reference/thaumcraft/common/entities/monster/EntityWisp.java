package thaumcraft.common.entities.monster;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.entity.EntityFlying;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.player.EntityPlayer;
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
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.NetworkRegistry.TargetPoint;
import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.client.fx.FXDispatcher;
import thaumcraft.common.lib.SoundsTC;
import thaumcraft.common.lib.network.PacketHandler;
import thaumcraft.common.lib.network.fx.PacketFXWispZap;

public class EntityWisp extends EntityFlying implements IMob {
   public int courseChangeCooldown = 0;
   public double waypointX;
   public double waypointY;
   public double waypointZ;
   private int aggroCooldown = 0;
   public int prevAttackCounter = 0;
   public int attackCounter = 0;
   private BlockPos currentFlightTarget;
   private static final DataParameter<String> TYPE = EntityDataManager.createKey(EntityWisp.class, DataSerializers.STRING);

   public EntityWisp(World world) {
      super(world);
      this.setSize(0.9F, 0.9F);
      this.experienceValue = 5;
   }

   protected void applyEntityAttributes() {
      super.applyEntityAttributes();
      this.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(22.0);
      this.getAttributeMap().registerAttribute(SharedMonsterAttributes.ATTACK_DAMAGE);
      this.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).setBaseValue(3.0);
   }

   protected boolean canTriggerWalking() {
      return false;
   }

   public int decreaseAirSupply(int par1) {
      return par1;
   }

   public boolean attackEntityFrom(DamageSource damagesource, float i) {
      if (damagesource.getTrueSource() instanceof EntityLivingBase) {
         this.setAttackTarget((EntityLivingBase)damagesource.getTrueSource());
         this.aggroCooldown = 200;
      }

      return super.attackEntityFrom(damagesource, i);
   }

   protected void entityInit() {
      super.entityInit();
      this.getDataManager().register(TYPE, String.valueOf(""));
   }

   public void onDeath(DamageSource par1DamageSource) {
      super.onDeath(par1DamageSource);
      if (this.world.isRemote) {
         FXDispatcher.INSTANCE.burst(this.posX, this.posY + 0.45F, this.posZ, 1.0F);
      }
   }

   public void onUpdate() {
      super.onUpdate();
      if (this.world.isRemote && this.ticksExisted <= 1) {
         FXDispatcher.INSTANCE.burst(this.posX, this.posY, this.posZ, 10.0F);
      }

      if (this.world.isRemote && this.world.rand.nextBoolean() && Aspect.getAspect(this.getType()) != null) {
         FXDispatcher.INSTANCE
            .drawWispParticles(
               this.posX + (this.world.rand.nextFloat() - this.world.rand.nextFloat()) * 0.7F,
               this.posY + (this.world.rand.nextFloat() - this.world.rand.nextFloat()) * 0.7F,
               this.posZ + (this.world.rand.nextFloat() - this.world.rand.nextFloat()) * 0.7F,
               0.0,
               0.0,
               0.0,
               Aspect.getAspect(this.getType()).getColor(),
               0
            );
      }

      this.motionY *= 0.6F;
   }

   public String getType() {
      return (String)this.getDataManager().get(TYPE);
   }

   public void setType(String t) {
      this.getDataManager().set(TYPE, String.valueOf(t));
   }

   public void onLivingUpdate() {
      super.onLivingUpdate();
      if (this.isServerWorld()) {
         if (!this.world.isRemote && Aspect.getAspect(this.getType()) == null) {
            if (this.world.rand.nextInt(10) != 0) {
               ArrayList<Aspect> as = Aspect.getPrimalAspects();
               this.setType(as.get(this.world.rand.nextInt(as.size())).getTag());
            } else {
               ArrayList<Aspect> as = Aspect.getCompoundAspects();
               this.setType(as.get(this.world.rand.nextInt(as.size())).getTag());
            }
         }

         if (!this.world.isRemote && this.world.getDifficulty() == EnumDifficulty.PEACEFUL) {
            this.setDead();
         }

         this.prevAttackCounter = this.attackCounter;
         double attackrange = 16.0;
         if (this.getAttackTarget() != null && this.canEntityBeSeen(this.getAttackTarget())) {
            if (this.getDistanceSq(this.getAttackTarget()) > attackrange * attackrange / 2.0 && this.canEntityBeSeen(this.getAttackTarget())) {
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
         } else {
            if (this.currentFlightTarget != null
               && (
                  !this.world.isAirBlock(this.currentFlightTarget)
                     || this.currentFlightTarget.getY() < 1
                     || this.currentFlightTarget.getY() > this.world.getPrecipitationHeight(this.currentFlightTarget).up(8).getY()
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
            this.motionX = this.motionX + (Math.signum(var1) * 0.5 - this.motionX) * 0.1F;
            this.motionY = this.motionY + (Math.signum(var3) * 0.7F - this.motionY) * 0.1F;
            this.motionZ = this.motionZ + (Math.signum(var5) * 0.5 - this.motionZ) * 0.1F;
            float var7 = (float)(Math.atan2(this.motionZ, this.motionX) * 180.0 / Math.PI) - 90.0F;
            float var8 = MathHelper.wrapDegrees(var7 - this.rotationYaw);
            this.moveForward = 0.15F;
            this.rotationYaw += var8;
         }

         if (this.getAttackTarget() instanceof EntityPlayer && ((EntityPlayer)this.getAttackTarget()).capabilities.disableDamage) {
            this.setAttackTarget(null);
         }

         if (this.getAttackTarget() != null && this.getAttackTarget().isDead) {
            this.setAttackTarget(null);
         }

         this.aggroCooldown--;
         if (this.world.rand.nextInt(1000) == 0 && (this.getAttackTarget() == null || this.aggroCooldown-- <= 0)) {
            this.setAttackTarget(this.world.getClosestPlayerToEntity(this, 16.0));
            if (this.getAttackTarget() != null) {
               this.aggroCooldown = 50;
            }
         }

         if (this.isEntityAlive() && this.getAttackTarget() != null && this.getAttackTarget().getDistanceSq(this) < attackrange * attackrange) {
            double d5 = this.getAttackTarget().posX - this.posX;
            double d6 = this.getAttackTarget().getEntityBoundingBox().minY
               + this.getAttackTarget().height / 2.0F
               - (this.posY + this.height / 2.0F);
            double d7 = this.getAttackTarget().posZ - this.posZ;
            this.renderYawOffset = this.rotationYaw = -((float)Math.atan2(d5, d7)) * 180.0F / 3.141593F;
            if (this.canEntityBeSeen(this.getAttackTarget())) {
               this.attackCounter++;
               if (this.attackCounter == 20) {
                  this.playSound(SoundsTC.zap, 1.0F, 1.1F);
                  PacketHandler.INSTANCE
                     .sendToAllAround(
                        new PacketFXWispZap(this.getEntityId(), this.getAttackTarget().getEntityId()),
                        new TargetPoint(this.world.provider.getDimension(), this.posX, this.posY, this.posZ, 32.0)
                     );
                  float damage = (float)this.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).getAttributeValue();
                  if (!(Math.abs(this.getAttackTarget().motionX) > 0.1F)
                     && !(Math.abs(this.getAttackTarget().motionY) > 0.1F)
                     && !(Math.abs(this.getAttackTarget().motionZ) > 0.1F)) {
                     if (this.world.rand.nextFloat() < 0.66F) {
                        this.getAttackTarget().attackEntityFrom(DamageSource.causeMobDamage(this), damage + 1.0F);
                     }
                  } else if (this.world.rand.nextFloat() < 0.4F) {
                     this.getAttackTarget().attackEntityFrom(DamageSource.causeMobDamage(this), damage);
                  }

                  this.attackCounter = -20 + this.world.rand.nextInt(20);
               }
            } else if (this.attackCounter > 0) {
               this.attackCounter--;
            }
         }
      }
   }

   protected SoundEvent getAmbientSound() {
      return SoundsTC.wisplive;
   }

   protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
      return SoundEvents.BLOCK_LAVA_EXTINGUISH;
   }

   protected SoundEvent getDeathSound() {
      return SoundsTC.wispdead;
   }

   protected Item getDropItem() {
      return Item.getItemById(0);
   }

   protected void dropFewItems(boolean flag, int i) {
      if (Aspect.getAspect(this.getType()) != null) {
         this.entityDropItem(ThaumcraftApiHelper.makeCrystal(Aspect.getAspect(this.getType())), 0.0F);
      }
   }

   protected float getSoundVolume() {
      return 0.25F;
   }

   protected boolean canDespawn() {
      return true;
   }

   public boolean getCanSpawnHere() {
      int count = 0;

      try {
         List l = this.world.getEntitiesWithinAABB(EntityWisp.class, this.getEntityBoundingBox().grow(16.0, 16.0, 16.0));
         if (l != null) {
            count = l.size();
         }
      } catch (Exception var3) {
      }

      return count < 8 && this.world.getDifficulty() != EnumDifficulty.PEACEFUL && this.isValidLightLevel() && super.getCanSpawnHere();
   }

   protected boolean isValidLightLevel() {
      BlockPos blockpos = new BlockPos(this.posX, this.getEntityBoundingBox().minY, this.posZ);
      if (this.world.getLightFor(EnumSkyBlock.SKY, blockpos) > this.rand.nextInt(32)) {
         return false;
      } else {
         int i = this.world.getLightFromNeighbors(blockpos);
         if (this.world.isThundering()) {
            int j = this.world.getSkylightSubtracted();
            this.world.setSkylightSubtracted(10);
            i = this.world.getLightFromNeighbors(blockpos);
            this.world.setSkylightSubtracted(j);
         }

         return i <= this.rand.nextInt(8);
      }
   }

   public void writeEntityToNBT(NBTTagCompound nbttagcompound) {
      super.writeEntityToNBT(nbttagcompound);
      nbttagcompound.setString("Type", this.getType());
   }

   public void readEntityFromNBT(NBTTagCompound nbttagcompound) {
      super.readEntityFromNBT(nbttagcompound);
      this.setType(nbttagcompound.getString("Type"));
   }

   public int getMaxSpawnedInChunk() {
      return 2;
   }
}
