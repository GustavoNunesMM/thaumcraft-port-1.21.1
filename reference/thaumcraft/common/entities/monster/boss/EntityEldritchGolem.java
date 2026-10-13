package thaumcraft.common.entities.monster.boss;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
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
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.api.entities.IEldritchMob;
import thaumcraft.client.fx.FXDispatcher;
import thaumcraft.common.blocks.world.BlockLoot;
import thaumcraft.common.entities.ai.combat.AILongRangeAttack;
import thaumcraft.common.entities.monster.mods.ChampionModifier;
import thaumcraft.common.entities.projectile.EntityGolemOrb;
import thaumcraft.common.lib.SoundsTC;

public class EntityEldritchGolem extends EntityThaumcraftBoss implements IEldritchMob, IRangedAttackMob {
   private static final DataParameter<Boolean> HEADLESS = EntityDataManager.createKey(EntityEldritchGolem.class, DataSerializers.BOOLEAN);
   int beamCharge = 0;
   boolean chargingBeam = false;
   int arcing = 0;
   int ax = 0;
   int ay = 0;
   int az = 0;
   private int attackTimer;

   public EntityEldritchGolem(World p_i1745_1_) {
      super(p_i1745_1_);
      this.setSize(1.75F, 3.5F);
      this.isImmuneToFire = true;
   }

   protected void initEntityAI() {
      this.tasks.addTask(0, new EntityAISwimming(this));
      this.tasks.addTask(3, new EntityAIAttackMelee(this, 1.1, false));
      this.tasks.addTask(6, new EntityAIMoveTowardsRestriction(this, 0.8));
      this.tasks.addTask(7, new EntityAIWander(this, 0.8));
      this.tasks.addTask(8, new EntityAIWatchClosest(this, EntityPlayer.class, 8.0F));
      this.tasks.addTask(8, new EntityAILookIdle(this));
      this.targetTasks.addTask(1, new EntityAIHurtByTarget(this, true, new Class[0]));
      this.targetTasks.addTask(2, new EntityAINearestAttackableTarget(this, EntityPlayer.class, true));
   }

   @Override
   public void generateName() {
      int t = (int)this.getEntityAttribute(ThaumcraftApiHelper.CHAMPION_MOD).getAttributeValue();
      if (t >= 0) {
         this.setCustomNameTag(String.format(I18n.translateToLocal("entity.Thaumcraft.EldritchGolem.name.custom"), ChampionModifier.mods[t].getModNameLocalized()));
      }
   }

   @Override
   protected void entityInit() {
      super.entityInit();
      this.getDataManager().register(HEADLESS, false);
   }

   public boolean isHeadless() {
      return (Boolean)this.getDataManager().get(HEADLESS);
   }

   public void setHeadless(boolean par1) {
      this.getDataManager().set(HEADLESS, par1);
   }

   @Override
   public void writeEntityToNBT(NBTTagCompound nbt) {
      super.writeEntityToNBT(nbt);
      nbt.setBoolean("headless", this.isHeadless());
   }

   @Override
   public void readEntityFromNBT(NBTTagCompound nbt) {
      super.readEntityFromNBT(nbt);
      this.setHeadless(nbt.getBoolean("headless"));
      if (this.isHeadless()) {
         this.makeHeadless();
      }
   }

   public float getEyeHeight() {
      return this.isHeadless() ? 3.33F : 3.0F;
   }

   public int getTotalArmorValue() {
      return super.getTotalArmorValue() + 6;
   }

   @Override
   protected void applyEntityAttributes() {
      super.applyEntityAttributes();
      this.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.3);
      this.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).setBaseValue(10.0);
      this.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(400.0);
   }

   protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
      return SoundEvents.ENTITY_IRONGOLEM_HURT;
   }

   protected SoundEvent getDeathSound() {
      return SoundEvents.ENTITY_IRONGOLEM_DEATH;
   }

   protected void playStepSound(BlockPos pos, Block blockIn) {
      this.playSound(SoundEvents.ENTITY_IRONGOLEM_STEP, 1.0F, 1.0F);
   }

   @Override
   public IEntityLivingData onInitialSpawn(DifficultyInstance diff, IEntityLivingData data) {
      this.spawnTimer = 100;
      return super.onInitialSpawn(diff, data);
   }

   public void onLivingUpdate() {
      super.onLivingUpdate();
      if (this.attackTimer > 0) {
         this.attackTimer--;
      }

      if (this.motionX * this.motionX + this.motionZ * this.motionZ > 2.5000003E-7F && this.rand.nextInt(5) == 0) {
         IBlockState bs = this.world.getBlockState(this.getPosition());
         if (bs.getMaterial() != Material.AIR) {
            this.world
               .spawnParticle(
                  EnumParticleTypes.BLOCK_CRACK,
                  this.posX + (this.rand.nextFloat() - 0.5) * this.width,
                  this.getEntityBoundingBox().minY + 0.1,
                  this.posZ + (this.rand.nextFloat() - 0.5) * this.width,
                  4.0 * (this.rand.nextFloat() - 0.5),
                  0.5,
                  (this.rand.nextFloat() - 0.5) * 4.0,
                  new int[]{Block.getStateId(bs)}
               );
         }

         if (!this.world.isRemote && bs.getBlock() instanceof BlockLoot) {
            this.world.destroyBlock(this.getPosition(), true);
         }
      }

      if (!this.world.isRemote) {
         IBlockState bsx = this.world.getBlockState(this.getPosition());
         float h = bsx.getBlockHardness(this.world, this.getPosition());
         if (h >= 0.0F && h <= 0.15F) {
            this.world.destroyBlock(this.getPosition(), true);
         }
      }
   }

   @Override
   public boolean attackEntityFrom(DamageSource source, float damage) {
      if (!this.world.isRemote && damage > this.getHealth() && !this.isHeadless()) {
         this.setHeadless(true);
         this.spawnTimer = 100;
         double xx = MathHelper.cos(this.rotationYaw % 360.0F / 180.0F * (float) Math.PI) * 0.75F;
         double zz = MathHelper.sin(this.rotationYaw % 360.0F / 180.0F * (float) Math.PI) * 0.75F;
         this.world.createExplosion(this, this.posX + xx, this.posY + this.getEyeHeight(), this.posZ + zz, 2.0F, false);
         this.makeHeadless();
         return false;
      } else {
         return super.attackEntityFrom(source, damage);
      }
   }

   void makeHeadless() {
      this.tasks.addTask(2, new AILongRangeAttack(this, 3.0, 1.0, 5, 5, 24.0F));
   }

   public boolean attackEntityAsMob(Entity target) {
      if (this.attackTimer > 0) {
         return false;
      } else {
         this.attackTimer = 10;
         this.world.setEntityState(this, (byte)4);
         boolean flag = target.attackEntityFrom(
            DamageSource.causeMobDamage(this), (float)this.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).getAttributeValue() * 0.75F
         );
         if (flag) {
            target.motionY += 0.2000000059604645;
            if (this.isHeadless()) {
               target.addVelocity(
                  -MathHelper.sin(this.rotationYaw * (float) Math.PI / 180.0F) * 1.5F,
                  0.1,
                  MathHelper.cos(this.rotationYaw * (float) Math.PI / 180.0F) * 1.5F
               );
            }
         }

         return flag;
      }
   }

   public void attackEntityWithRangedAttack(EntityLivingBase entitylivingbase, float f) {
      if (this.canEntityBeSeen(entitylivingbase) && !this.chargingBeam && this.beamCharge > 0) {
         this.beamCharge = this.beamCharge - (15 + this.rand.nextInt(5));
         this.getLookHelper()
            .setLookPosition(
               entitylivingbase.posX,
               entitylivingbase.getEntityBoundingBox().minY + entitylivingbase.height / 2.0F,
               entitylivingbase.posZ,
               30.0F,
               30.0F
            );
         Vec3d v = this.getLook(1.0F);
         EntityGolemOrb blast = new EntityGolemOrb(this.world, this, entitylivingbase, false);
         blast.posX = blast.posX + v.x;
         blast.posZ = blast.posZ + v.z;
         blast.setPosition(blast.posX, blast.posY, blast.posZ);
         double d0 = entitylivingbase.posX + entitylivingbase.motionX - this.posX;
         double d1 = entitylivingbase.posY - this.posY - entitylivingbase.height / 2.0F;
         double d2 = entitylivingbase.posZ + entitylivingbase.motionZ - this.posZ;
         blast.shoot(d0, d1, d2, 0.66F, 5.0F);
         this.playSound(SoundsTC.egattack, 1.0F, 1.0F + this.rand.nextFloat() * 0.1F);
         this.world.spawnEntity(blast);
      }
   }

   @SideOnly(Side.CLIENT)
   public void handleStatusUpdate(byte id) {
      if (id == 4) {
         this.attackTimer = 10;
         this.playSound(SoundEvents.ENTITY_IRONGOLEM_ATTACK, 1.0F, 1.0F);
      } else if (id == 18) {
         this.spawnTimer = 150;
      } else if (id == 19) {
         if (this.arcing == 0) {
            float radius = 2.0F + this.rand.nextFloat() * 2.0F;
            double radians = Math.toRadians(this.rand.nextInt(360));
            double deltaX = radius * Math.cos(radians);
            double deltaZ = radius * Math.sin(radians);
            int bx = MathHelper.floor(this.posX + deltaX);
            int by = MathHelper.floor(this.posY);
            int bz = MathHelper.floor(this.posZ + deltaZ);
            BlockPos bp = new BlockPos(bx, by, bz);

            for (int c = 0; c < 5 && this.world.isAirBlock(bp); by--) {
               c++;
            }

            if (this.world.isAirBlock(bp.up()) && !this.world.isAirBlock(bp)) {
               this.ax = bx;
               this.ay = by;
               this.az = bz;
               this.arcing = 8 + this.rand.nextInt(5);
               this.playSound(SoundsTC.jacobs, 0.8F, 1.0F + (this.rand.nextFloat() - this.rand.nextFloat()) * 0.05F);
            }
         }
      } else {
         super.handleStatusUpdate(id);
      }
   }

   @Override
   public void onUpdate() {
      if (this.getSpawnTimer() == 150) {
         this.world.setEntityState(this, (byte)18);
      }

      if (this.getSpawnTimer() > 0) {
         this.heal(2.0F);
      }

      super.onUpdate();
      if (this.world.isRemote) {
         if (this.isHeadless()) {
            this.rotationPitch = 0.0F;
            float f1 = MathHelper.cos(-this.renderYawOffset * (float) (Math.PI / 180.0) - (float) Math.PI);
            float f2 = MathHelper.sin(-this.renderYawOffset * (float) (Math.PI / 180.0) - (float) Math.PI);
            float f3 = -MathHelper.cos(-this.rotationPitch * (float) (Math.PI / 180.0));
            float f4 = MathHelper.sin(-this.rotationPitch * (float) (Math.PI / 180.0));
            Vec3d v = new Vec3d(f2 * f3, f4, f1 * f3);
            if (this.rand.nextInt(20) == 0) {
               float a = (this.rand.nextFloat() - this.rand.nextFloat()) / 3.0F;
               float b = (this.rand.nextFloat() - this.rand.nextFloat()) / 3.0F;
               FXDispatcher.INSTANCE
                  .spark(
                     (float)(this.posX + v.x + a),
                     (float)this.posY + this.getEyeHeight() - 0.75F,
                     (float)(this.posZ + v.z + b),
                     3.0F,
                     0.65F + this.rand.nextFloat() * 0.1F,
                     1.0F,
                     1.0F,
                     0.8F
                  );
            }

            FXDispatcher.INSTANCE
               .drawVentParticles(
                  (float)this.posX + v.x * 0.4,
                  (float)this.posY + this.getEyeHeight() - 1.25F,
                  (float)this.posZ + v.z * 0.4,
                  0.0,
                  0.001,
                  0.0,
                  5592405,
                  4.0F
               );
            if (this.arcing > 0) {
               FXDispatcher.INSTANCE
                  .arcLightning(
                     this.posX,
                     this.posY + this.height / 2.0F,
                     this.posZ,
                     this.ax + 0.5,
                     this.ay + 1,
                     this.az + 0.5,
                     0.65F + this.rand.nextFloat() * 0.1F,
                     1.0F,
                     1.0F,
                     1.0F - this.arcing / 10.0F
                  );
               this.arcing--;
            }
         }
      } else {
         if (this.isHeadless() && this.beamCharge <= 0) {
            this.chargingBeam = true;
         }

         if (this.isHeadless() && this.chargingBeam) {
            this.beamCharge++;
            this.world.setEntityState(this, (byte)19);
            if (this.beamCharge == 150) {
               this.chargingBeam = false;
            }
         }
      }
   }

   @SideOnly(Side.CLIENT)
   public int getAttackTimer() {
      return this.attackTimer;
   }

   public void setSwingingArms(boolean swingingArms) {
   }
}
