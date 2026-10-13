package thaumcraft.common.entities.monster;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.IEntityLivingData;
import net.minecraft.entity.monster.EntitySlime;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.World;
import thaumcraft.api.entities.ITaintedMob;
import thaumcraft.api.items.ItemsTC;
import thaumcraft.client.fx.FXDispatcher;
import thaumcraft.common.config.ConfigItems;
import thaumcraft.common.lib.SoundsTC;

public class EntityThaumicSlime extends EntitySlime implements ITaintedMob {
   int launched = 10;
   int spitCounter = 100;

   public EntityThaumicSlime(World par1World) {
      super(par1World);
      int i = 1 << 1 + this.rand.nextInt(3);
      this.setSlimeSize(i, true);
   }

   public EntityThaumicSlime(World par1World, EntityLivingBase par2EntityLiving, EntityLivingBase par3EntityLiving) {
      super(par1World);
      this.setSlimeSize(1, true);
      this.posY = (par2EntityLiving.getEntityBoundingBox().minY + par2EntityLiving.getEntityBoundingBox().maxY) / 2.0;
      double var6 = par3EntityLiving.posX - par2EntityLiving.posX;
      double var8 = par3EntityLiving.getEntityBoundingBox().minY + par3EntityLiving.height / 3.0F - this.posY;
      double var10 = par3EntityLiving.posZ - par2EntityLiving.posZ;
      double var12 = MathHelper.sqrt(var6 * var6 + var10 * var10);
      if (var12 >= 1.0E-7) {
         float var14 = (float)(Math.atan2(var10, var6) * 180.0 / Math.PI) - 90.0F;
         float var15 = (float)(-(Math.atan2(var8, var12) * 180.0 / Math.PI));
         double var16 = var6 / var12;
         double var18 = var10 / var12;
         this.setLocationAndAngles(par2EntityLiving.posX + var16, this.posY, par2EntityLiving.posZ + var18, var14, var15);
         float var20 = (float)var12 * 0.2F;
         this.shoot(var6, var8 + var20, var10, 1.5F, 1.0F);
      }
   }

   public void shoot(double par1, double par3, double par5, float par7, float par8) {
      float var9 = MathHelper.sqrt(par1 * par1 + par3 * par3 + par5 * par5);
      par1 /= var9;
      par3 /= var9;
      par5 /= var9;
      par1 += this.rand.nextGaussian() * 0.0075F * par8;
      par3 += this.rand.nextGaussian() * 0.0075F * par8;
      par5 += this.rand.nextGaussian() * 0.0075F * par8;
      par1 *= par7;
      par3 *= par7;
      par5 *= par7;
      this.motionX = par1;
      this.motionY = par3;
      this.motionZ = par5;
      float var10 = MathHelper.sqrt(par1 * par1 + par5 * par5);
      this.prevRotationYaw = this.rotationYaw = (float)(Math.atan2(par1, par5) * 180.0 / Math.PI);
      this.prevRotationPitch = this.rotationPitch = (float)(Math.atan2(par3, var10) * 180.0 / Math.PI);
   }

   public IEntityLivingData onInitialSpawn(DifficultyInstance difficulty, IEntityLivingData livingdata) {
      int i = this.rand.nextInt(3);
      if (i < 2 && this.rand.nextFloat() < 0.5F * difficulty.getClampedAdditionalDifficulty()) {
         i++;
      }

      int j = 1 << i;
      this.setSlimeSize(j, true);
      return super.onInitialSpawn(difficulty, livingdata);
   }

   public void setSlimeSize(int par1, boolean t) {
      super.setSlimeSize(par1, t);
      this.experienceValue = par1 + 2;
   }

   public void writeEntityToNBT(NBTTagCompound par1NBTTagCompound) {
      super.writeEntityToNBT(par1NBTTagCompound);
   }

   public void readEntityFromNBT(NBTTagCompound par1NBTTagCompound) {
      super.readEntityFromNBT(par1NBTTagCompound);
   }

   public void onUpdate() {
      int i = this.getSlimeSize();
      if (this.onGround && !this.wasOnGround) {
         this.wasOnGround = true;
         float sa = this.squishAmount;
         super.onUpdate();
         this.squishAmount = sa;
         if (this.world.isRemote) {
            for (int j = 0; j < i * 2; j++) {
               FXDispatcher.INSTANCE.slimeJumpFX(this, i);
            }
         }

         if (this.makesSoundOnJump()) {
            this.playSound(
               this.getJumpSound(), this.getSoundVolume(), ((this.getRNG().nextFloat() - this.getRNG().nextFloat()) * 0.2F + 1.0F) * 0.8F
            );
         }

         this.squishAmount = -0.5F;
         this.wasOnGround = this.onGround;
         this.alterSquishAmount();
      } else {
         super.onUpdate();
      }

      if (this.world.isRemote) {
         if (this.launched > 0) {
            this.launched--;

            for (int j = 0; j < i * (this.launched + 1); j++) {
               FXDispatcher.INSTANCE.slimeJumpFX(this, i);
            }
         }

         float ff = this.getSlimeSize();
         this.setSize(0.6F * ff, 0.6F * ff);
         this.setSize(0.51000005F * ff, 0.51000005F * ff);
      } else if (!this.isDead) {
         EntityPlayer entityplayer = this.world.getClosestPlayerToEntity(this, 16.0);
         if (entityplayer != null) {
            if (this.spitCounter > 0) {
               this.spitCounter--;
            }

            this.faceEntity(entityplayer, 10.0F, 20.0F);
            if (this.getDistance(entityplayer) > 4.0F && this.spitCounter <= 0 && this.getSlimeSize() > 2) {
               this.spitCounter = 101;
               if (!this.world.isRemote) {
                  EntityThaumicSlime flyslime = new EntityThaumicSlime(this.world, this, entityplayer);
                  this.world.spawnEntity(flyslime);
               }

               this.playSound(SoundsTC.gore, 1.0F, ((this.rand.nextFloat() - this.rand.nextFloat()) * 0.2F + 1.0F) * 0.8F);
               this.setSlimeSize(this.getSlimeSize() - 1, true);
            }
         }
      }
   }

   protected EntityThaumicSlime createInstance() {
      return new EntityThaumicSlime(this.world);
   }

   public void setDead() {
      int i = this.getSlimeSize();
      if (!this.world.isRemote && i > 1 && this.getHealth() <= 0.0F) {
         for (int k = 0; k < i; k++) {
            float f = (k % 2 - 0.5F) * i / 4.0F;
            float f1 = (k / 2 - 0.5F) * i / 4.0F;
            EntityThaumicSlime entityslime = this.createInstance();
            entityslime.setSlimeSize(1, true);
            entityslime.setLocationAndAngles(this.posX + f, this.posY + 0.5, this.posZ + f1, this.rand.nextFloat() * 360.0F, 0.0F);
            this.world.spawnEntity(entityslime);
         }
      }

      this.isDead = true;
   }

   public boolean getCanSpawnHere() {
      return false;
   }

   protected int getAttackStrength() {
      return this.getSlimeSize() + 1;
   }

   protected boolean canDamagePlayer() {
      return true;
   }

   protected void dealDamage(EntityLivingBase entityIn) {
      int i = this.getSlimeSize();
      if (this.launched > 0) {
         i += 2;
      }

      if (this.isEntityAlive()
         && this.canEntityBeSeen(entityIn)
         && this.getDistanceSq(entityIn) < 0.6 * i * 0.6 * i
         && entityIn.attackEntityFrom(DamageSource.causeMobDamage(this), this.getAttackStrength())) {
         this.playSound(SoundEvents.ENTITY_SLIME_ATTACK, 1.0F, (this.rand.nextFloat() - this.rand.nextFloat()) * 0.2F + 1.0F);
         this.applyEnchantments(this, entityIn);
      }
   }

   protected Item getDropItem() {
      return this.getSlimeSize() > 1 ? ItemsTC.crystalEssence : Item.getItemById(0);
   }

   protected void dropFewItems(boolean flag, int i) {
      if (this.getSlimeSize() > 1) {
         this.entityDropItem(ConfigItems.FLUX_CRYSTAL.copy(), this.height / 2.0F);
      }
   }
}
