package thaumcraft.common.entities.monster.tainted;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.MoverType;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIAttackMelee;
import net.minecraft.entity.ai.EntityAIHurtByTarget;
import net.minecraft.entity.ai.EntityAINearestAttackableTarget;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import thaumcraft.api.ThaumcraftMaterials;
import thaumcraft.api.aura.AuraHelper;
import thaumcraft.api.blocks.BlocksTC;
import thaumcraft.api.entities.ITaintedMob;
import thaumcraft.api.potions.PotionFluxTaint;
import thaumcraft.client.fx.FXDispatcher;
import thaumcraft.common.blocks.world.taint.TaintHelper;
import thaumcraft.common.config.ConfigItems;
import thaumcraft.common.config.ModConfig;
import thaumcraft.common.lib.SoundsTC;
import thaumcraft.common.lib.utils.BlockUtils;
import thaumcraft.common.lib.utils.EntityUtils;
import thaumcraft.common.world.aura.AuraHandler;
import thaumcraft.common.world.biomes.BiomeHandler;

public class EntityTaintSeed extends EntityMob implements ITaintedMob {
   public int boost = 0;
   boolean firstRun = false;
   public float attackAnim = 0.0F;

   public EntityTaintSeed(World par1World) {
      super(par1World);
      this.setSize(1.5F, 1.25F);
      this.experienceValue = 8;
   }

   protected int getArea() {
      return 1;
   }

   protected void initEntityAI() {
      this.tasks.addTask(1, new EntityAIAttackMelee(this, 1.0, false));
      this.targetTasks.addTask(0, new EntityAIHurtByTarget(this, false, new Class[0]));
      this.targetTasks.addTask(1, new EntityAINearestAttackableTarget(this, EntityPlayer.class, true));
   }

   public void readEntityFromNBT(NBTTagCompound nbt) {
      super.readEntityFromNBT(nbt);
      this.boost = nbt.getInteger("boost");
   }

   public void writeEntityToNBT(NBTTagCompound nbt) {
      super.writeEntityToNBT(nbt);
      nbt.setInteger("boost", this.boost);
   }

   public boolean attackEntityAsMob(Entity entityIn) {
      this.world.setEntityState(this, (byte)16);
      this.playSound(SoundsTC.tentacle, this.getSoundVolume(), this.getSoundPitch());
      return super.attackEntityAsMob(entityIn);
   }

   @SideOnly(Side.CLIENT)
   public void handleStatusUpdate(byte par1) {
      if (par1 == 16) {
         this.attackAnim = 0.5F;
      } else {
         super.handleStatusUpdate(par1);
      }
   }

   public boolean canAttackClass(Class clazz) {
      return !ITaintedMob.class.isAssignableFrom(clazz);
   }

   public boolean isOnSameTeam(Entity otherEntity) {
      return otherEntity instanceof ITaintedMob || super.isOnSameTeam(otherEntity);
   }

   public boolean getCanSpawnHere() {
      return this.world.getDifficulty() != EnumDifficulty.PEACEFUL
         && this.isNotColliding()
         && EntityUtils.getEntitiesInRange(
                  this.getEntityWorld(), this.getPosition(), null, EntityTaintSeed.class, ModConfig.CONFIG_WORLD.taintSpreadArea * 0.8
               )
               .size()
            <= 0;
   }

   public boolean isNotColliding() {
      return !this.world.containsAnyLiquid(this.getEntityBoundingBox()) && this.world.checkNoEntityCollision(this.getEntityBoundingBox(), this);
   }

   protected void applyEntityAttributes() {
      super.applyEntityAttributes();
      this.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(75.0);
      this.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).setBaseValue(4.0);
      this.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.0);
   }

   public void onDeath(DamageSource cause) {
      TaintHelper.removeTaintSeed(this.getEntityWorld(), this.getPosition());
      super.onDeath(cause);
   }

   public void onUpdate() {
      super.onUpdate();
      if (!this.world.isRemote) {
         if (!this.firstRun || this.ticksExisted % 1200 == 0) {
            TaintHelper.removeTaintSeed(this.getEntityWorld(), this.getPosition());
            TaintHelper.addTaintSeed(this.getEntityWorld(), this.getPosition());
            this.firstRun = true;
         }

         if (this.isEntityAlive()) {
            boolean tickFlag = this.ticksExisted % 20 == 0;
            if (this.boost > 0 || tickFlag) {
               float mod = this.boost > 0 ? 1.0F : AuraHandler.getFluxSaturation(this.world, this.getPosition());
               if (this.boost > 0) {
                  this.boost--;
               }

               if (mod <= 0.0F) {
                  this.attackEntityFrom(DamageSource.STARVE, 0.5F);
                  AuraHelper.polluteAura(this.getEntityWorld(), this.getPosition(), 0.1F, false);
               } else {
                  TaintHelper.spreadFibres(
                     this.world,
                     this.getPosition()
                        .add(
                           MathHelper.getInt(this.getRNG(), -this.getArea() * 3, this.getArea() * 3),
                           MathHelper.getInt(this.getRNG(), -this.getArea(), this.getArea()),
                           MathHelper.getInt(this.getRNG(), -this.getArea() * 3, this.getArea() * 3)
                        ),
                     true
                  );
               }
            }

            if (tickFlag) {
               if (this.getAttackTarget() != null
                  && this.getDistanceSq(this.getAttackTarget()) < this.getArea() * 256
                  && this.getEntitySenses().canSee(this.getAttackTarget())) {
                  this.spawnTentacles(this.getAttackTarget());
               }

               for (EntityLivingBase elb : EntityUtils.getEntitiesInRange(
                  this.getEntityWorld(), this.getPosition(), this, EntityLivingBase.class, this.getArea() * 4
               )) {
                  elb.addPotionEffect(new PotionEffect(PotionFluxTaint.instance, 100, this.getArea() - 1, false, true));
               }
            }
         }
      } else {
         if (this.attackAnim > 0.0F) {
            this.attackAnim *= 0.75F;
         }

         if (this.attackAnim < 0.001) {
            this.attackAnim = 0.0F;
         }

         float xx = 1.0F * MathHelper.sin(this.ticksExisted * 0.05F - 0.5F) / 5.0F;
         float zz = 1.0F * MathHelper.sin(this.ticksExisted * 0.06F - 0.5F) / 5.0F + this.hurtTime / 200.0F + this.attackAnim;
         if (this.rand.nextFloat() < 0.033) {
            FXDispatcher.INSTANCE
               .drawLightningFlash(
                  (float)this.posX + xx,
                  (float)this.posY + this.height + 0.25F,
                  (float)this.posZ + zz,
                  0.7F,
                  0.1F,
                  0.9F,
                  0.5F,
                  1.5F + this.rand.nextFloat()
               );
         } else {
            FXDispatcher.INSTANCE
               .drawTaintParticles(
                  (float)this.posX + xx,
                  (float)this.posY + this.height + 0.25F,
                  (float)this.posZ + zz,
                  (float)this.rand.nextGaussian() * 0.05F,
                  0.1F + 0.01F * this.rand.nextFloat(),
                  (float)this.rand.nextGaussian() * 0.05F,
                  2.0F
               );
         }
      }
   }

   protected void spawnTentacles(Entity entity) {
      if (this.world.getBiome(entity.getPosition()) == BiomeHandler.ELDRITCH
         || this.world.getBlockState(entity.getPosition()).getMaterial() == ThaumcraftMaterials.MATERIAL_TAINT
         || this.world.getBlockState(entity.getPosition().down()).getMaterial() == ThaumcraftMaterials.MATERIAL_TAINT) {
         EntityTaintacleSmall taintlet = new EntityTaintacleSmall(this.world);
         taintlet.setLocationAndAngles(
            entity.posX + this.world.rand.nextFloat() - this.world.rand.nextFloat(),
            entity.posY,
            entity.posZ + this.world.rand.nextFloat() - this.world.rand.nextFloat(),
            0.0F,
            0.0F
         );
         this.world.spawnEntity(taintlet);
         this.playSound(SoundsTC.tentacle, this.getSoundVolume(), this.getSoundPitch());
         if (this.world.getBiome(entity.getPosition()) == BiomeHandler.ELDRITCH
            && this.world.isAirBlock(entity.getPosition())
            && BlockUtils.isAdjacentToSolidBlock(this.world, entity.getPosition())) {
            this.world.setBlockState(entity.getPosition(), BlocksTC.taintFibre.getDefaultState());
         }
      }
   }

   public int getTalkInterval() {
      return 200;
   }

   protected SoundEvent getAmbientSound() {
      return SoundEvents.BLOCK_CHORUS_FLOWER_DEATH;
   }

   protected float getSoundPitch() {
      return 1.3F - this.height / 10.0F;
   }

   protected float getSoundVolume() {
      return this.height / 8.0F;
   }

   protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
      return SoundsTC.tentacle;
   }

   protected SoundEvent getDeathSound() {
      return SoundsTC.tentacle;
   }

   protected Item getDropItem() {
      return Item.getItemById(0);
   }

   protected void dropFewItems(boolean flag, int i) {
      this.entityDropItem(ConfigItems.FLUX_CRYSTAL.copy(), this.height / 2.0F);
   }

   public boolean canBePushed() {
      return false;
   }

   public boolean canBeCollidedWith() {
      return true;
   }

   public void moveRelative(float strafe, float forward, float friction, float g) {
   }

   public void move(MoverType mt, double par1, double par3, double par5) {
      par1 = 0.0;
      par5 = 0.0;
      if (par3 > 0.0) {
         par3 = 0.0;
      }

      super.move(mt, par1, par3, par5);
   }

   protected int decreaseAirSupply(int air) {
      return air;
   }

   public boolean canBreatheUnderwater() {
      return true;
   }

   protected boolean canDespawn() {
      return false;
   }
}
