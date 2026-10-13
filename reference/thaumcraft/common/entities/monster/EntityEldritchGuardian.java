package thaumcraft.common.entities.monster;

import java.util.List;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.EnumCreatureAttribute;
import net.minecraft.entity.IEntityLivingData;
import net.minecraft.entity.IRangedAttackMob;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIAttackMelee;
import net.minecraft.entity.ai.EntityAIHurtByTarget;
import net.minecraft.entity.ai.EntityAILookIdle;
import net.minecraft.entity.ai.EntityAIMoveTowardsRestriction;
import net.minecraft.entity.ai.EntityAINearestAttackableTarget;
import net.minecraft.entity.ai.EntityAISwimming;
import net.minecraft.entity.ai.EntityAIWander;
import net.minecraft.entity.ai.EntityAIWatchClosest;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.MobEffects;
import net.minecraft.item.Item;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.pathfinding.PathNavigateGround;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.NetworkRegistry.TargetPoint;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.capabilities.IPlayerWarp;
import thaumcraft.api.entities.IEldritchMob;
import thaumcraft.client.fx.FXDispatcher;
import thaumcraft.common.config.ModConfig;
import thaumcraft.common.entities.ai.combat.AILongRangeAttack;
import thaumcraft.common.entities.monster.cult.EntityCultist;
import thaumcraft.common.entities.projectile.EntityEldritchOrb;
import thaumcraft.common.lib.SoundsTC;
import thaumcraft.common.lib.network.PacketHandler;
import thaumcraft.common.lib.network.fx.PacketFXSonic;
import thaumcraft.common.lib.network.misc.PacketMiscEvent;

public class EntityEldritchGuardian extends EntityMob implements IRangedAttackMob, IEldritchMob {
   public float armLiftL = 0.0F;
   public float armLiftR = 0.0F;
   boolean lastBlast = false;

   public EntityEldritchGuardian(World p_i1745_1_) {
      super(p_i1745_1_);
      ((PathNavigateGround)this.getNavigator()).setBreakDoors(true);
      this.setSize(0.8F, 2.25F);
      this.experienceValue = 20;
   }

   protected void initEntityAI() {
      this.tasks.addTask(0, new EntityAISwimming(this));
      this.tasks.addTask(2, new AILongRangeAttack(this, 8.0, 1.0, 20, 40, 24.0F));
      this.tasks.addTask(3, new EntityAIAttackMelee(this, 1.0, false));
      this.tasks.addTask(5, new EntityAIMoveTowardsRestriction(this, 0.8));
      this.tasks.addTask(7, new EntityAIWander(this, 1.0));
      this.tasks.addTask(8, new EntityAIWatchClosest(this, EntityPlayer.class, 8.0F));
      this.tasks.addTask(8, new EntityAILookIdle(this));
      this.targetTasks.addTask(1, new EntityAIHurtByTarget(this, false, new Class[0]));
      this.targetTasks.addTask(2, new EntityAINearestAttackableTarget(this, EntityPlayer.class, true));
      this.targetTasks.addTask(3, new EntityAINearestAttackableTarget(this, EntityCultist.class, true));
   }

   protected void applyEntityAttributes() {
      super.applyEntityAttributes();
      this.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(50.0);
      this.getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).setBaseValue(40.0);
      this.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.28);
      this.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).setBaseValue(7.0);
   }

   protected void entityInit() {
      super.entityInit();
   }

   public int getTotalArmorValue() {
      return 4;
   }

   public boolean canPickUpLoot() {
      return false;
   }

   public boolean attackEntityFrom(DamageSource source, float damage) {
      if (source.isMagicDamage()) {
         damage /= 2.0F;
      }

      return super.attackEntityFrom(source, damage);
   }

   public void onUpdate() {
      super.onUpdate();
      if (this.world.isRemote) {
         if (this.armLiftL > 0.0F) {
            this.armLiftL -= 0.05F;
         }

         if (this.armLiftR > 0.0F) {
            this.armLiftR -= 0.05F;
         }

         float x = (float)(this.posX + (this.rand.nextFloat() - this.rand.nextFloat()) * 0.2F);
         float z = (float)(this.posZ + (this.rand.nextFloat() - this.rand.nextFloat()) * 0.2F);
         FXDispatcher.INSTANCE.wispFXEG(x, (float)(this.posY + 0.22 * this.height), z, this);
      } else if (this.world.provider.getDimension() != ModConfig.CONFIG_WORLD.dimensionOuterId
         && (this.ticksExisted == 0 || this.ticksExisted % 100 == 0)
         && this.world.getDifficulty() != EnumDifficulty.EASY) {
         double d6 = this.world.getDifficulty() == EnumDifficulty.HARD ? 576.0 : 256.0;

         for (int i = 0; i < this.world.playerEntities.size(); i++) {
            EntityPlayer entityplayer1 = (EntityPlayer)this.world.playerEntities.get(i);
            if (entityplayer1.isEntityAlive()) {
               double d5 = entityplayer1.getDistanceSq(this.posX, this.posY, this.posZ);
               if (d5 < d6) {
                  PacketHandler.INSTANCE.sendTo(new PacketMiscEvent((byte)2), (EntityPlayerMP)entityplayer1);
               }
            }
         }
      }
   }

   public boolean attackEntityAsMob(Entity entityIn) {
      boolean flag = super.attackEntityAsMob(entityIn);
      if (flag) {
         int i = this.world.getDifficulty().getId();
         if (this.getHeldItemMainhand() == null && this.isBurning() && this.rand.nextFloat() < i * 0.3F) {
            entityIn.setFire(2 * i);
         }
      }

      return flag;
   }

   protected SoundEvent getAmbientSound() {
      return SoundsTC.egidle;
   }

   protected SoundEvent getDeathSound() {
      return SoundsTC.egdeath;
   }

   public int getTalkInterval() {
      return 500;
   }

   protected Item getDropItem() {
      return Item.getItemById(0);
   }

   protected void dropFewItems(boolean flag, int i) {
      super.dropFewItems(flag, i);
   }

   public EnumCreatureAttribute getCreatureAttribute() {
      return EnumCreatureAttribute.UNDEAD;
   }

   public void writeEntityToNBT(NBTTagCompound nbt) {
      super.writeEntityToNBT(nbt);
      if (this.getHomePosition() != null && this.getMaximumHomeDistance() > 0.0F) {
         nbt.setInteger("HomeD", (int)this.getMaximumHomeDistance());
         nbt.setInteger("HomeX", this.getHomePosition().getX());
         nbt.setInteger("HomeY", this.getHomePosition().getY());
         nbt.setInteger("HomeZ", this.getHomePosition().getZ());
      }
   }

   public void readEntityFromNBT(NBTTagCompound nbt) {
      super.readEntityFromNBT(nbt);
      if (nbt.hasKey("HomeD")) {
         this.setHomePosAndDistance(new BlockPos(nbt.getInteger("HomeX"), nbt.getInteger("HomeY"), nbt.getInteger("HomeZ")), nbt.getInteger("HomeD"));
      }
   }

   public IEntityLivingData onInitialSpawn(DifficultyInstance diff, IEntityLivingData data) {
      IEntityLivingData dd = super.onInitialSpawn(diff, data);
      float f = diff.getClampedAdditionalDifficulty();
      if (this.world.provider.getDimension() == ModConfig.CONFIG_WORLD.dimensionOuterId) {
         int bh = (int)this.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).getBaseValue() / 2;
         this.setAbsorptionAmount(this.getAbsorptionAmount() + bh);
      }

      return dd;
   }

   protected void updateAITasks() {
      super.updateAITasks();
      if (this.world.provider.getDimension() == ModConfig.CONFIG_WORLD.dimensionOuterId
         && this.hurtResistantTime <= 0
         && this.ticksExisted % 25 == 0) {
         int bh = (int)this.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).getBaseValue() / 2;
         if (this.getAbsorptionAmount() < bh) {
            this.setAbsorptionAmount(this.getAbsorptionAmount() + 1.0F);
         }
      }
   }

   @SideOnly(Side.CLIENT)
   public void handleStatusUpdate(byte id) {
      if (id == 15) {
         this.armLiftL = 0.5F;
      } else if (id == 16) {
         this.armLiftR = 0.5F;
      } else if (id == 17) {
         this.armLiftL = 0.9F;
         this.armLiftR = 0.9F;
      } else {
         super.handleStatusUpdate(id);
      }
   }

   protected boolean canDespawn() {
      return !this.hasHome();
   }

   public float getEyeHeight() {
      return 2.1F;
   }

   public boolean getCanSpawnHere() {
      List ents = this.world
         .getEntitiesWithinAABB(
            EntityEldritchGuardian.class,
            new AxisAlignedBB(
                  this.posX, this.posY, this.posZ, this.posX + 1.0, this.posY + 1.0, this.posZ + 1.0
               )
               .grow(32.0, 16.0, 32.0)
         );
      return ents.size() > 0 ? false : super.getCanSpawnHere();
   }

   protected boolean isValidLightLevel() {
      return true;
   }

   protected float getSoundVolume() {
      return 1.5F;
   }

   public void attackEntityWithRangedAttack(EntityLivingBase entitylivingbase, float f) {
      if (this.rand.nextFloat() > 0.15F) {
         EntityEldritchOrb blast = new EntityEldritchOrb(this.world, this);
         this.lastBlast = !this.lastBlast;
         this.world.setEntityState(this, (byte)(this.lastBlast ? 16 : 15));
         int rr = this.lastBlast ? 90 : 180;
         double xx = MathHelper.cos((this.rotationYaw + rr) % 360.0F / 180.0F * (float) Math.PI) * 0.5F;
         double yy = 0.057777777 * this.height;
         double zz = MathHelper.sin((this.rotationYaw + rr) % 360.0F / 180.0F * (float) Math.PI) * 0.5F;
         blast.setPosition(blast.posX - xx, blast.posY, blast.posZ - zz);
         Vec3d v = entitylivingbase.getPositionVector()
            .add(entitylivingbase.motionX * 10.0, entitylivingbase.motionY * 10.0, entitylivingbase.motionZ * 10.0)
            .subtract(this.getPositionVector())
            .normalize();
         blast.shoot(v.x, v.y, v.z, 1.1F, 2.0F);
         this.playSound(SoundsTC.egattack, 2.0F, 1.0F + this.rand.nextFloat() * 0.1F);
         this.world.spawnEntity(blast);
      } else if (this.canEntityBeSeen(entitylivingbase)) {
         PacketHandler.INSTANCE
            .sendToAllAround(
               new PacketFXSonic(this.getEntityId()),
               new TargetPoint(this.world.provider.getDimension(), this.posX, this.posY, this.posZ, 32.0)
            );

         try {
            entitylivingbase.addPotionEffect(new PotionEffect(MobEffects.WITHER, 400, 0));
         } catch (Exception var12) {
         }

         if (entitylivingbase instanceof EntityPlayer) {
            ThaumcraftApi.internalMethods
               .addWarpToPlayer((EntityPlayer)entitylivingbase, 1 + this.world.rand.nextInt(3), IPlayerWarp.EnumWarpType.TEMPORARY);
         }

         this.playSound(SoundsTC.egscreech, 3.0F, 1.0F + this.rand.nextFloat() * 0.1F);
      }
   }

   public boolean isOnSameTeam(Entity el) {
      return el instanceof IEldritchMob;
   }

   public void setSwingingArms(boolean swingingArms) {
   }
}
