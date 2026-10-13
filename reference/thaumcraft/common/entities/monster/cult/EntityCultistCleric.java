package thaumcraft.common.entities.monster.cult;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.IRangedAttackMob;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIAttackMelee;
import net.minecraft.entity.ai.EntityAILookIdle;
import net.minecraft.entity.ai.EntityAIMoveTowardsRestriction;
import net.minecraft.entity.ai.EntityAINearestAttackableTarget;
import net.minecraft.entity.ai.EntityAIOpenDoor;
import net.minecraft.entity.ai.EntityAIRestrictOpenDoor;
import net.minecraft.entity.ai.EntityAISwimming;
import net.minecraft.entity.ai.EntityAIWander;
import net.minecraft.entity.ai.EntityAIWatchClosest;
import net.minecraft.entity.monster.AbstractIllager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntitySmallFireball;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
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
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.IEntityAdditionalSpawnData;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import thaumcraft.api.items.ItemsTC;
import thaumcraft.common.entities.ai.combat.AICultistHurtByTarget;
import thaumcraft.common.entities.ai.combat.AILongRangeAttack;
import thaumcraft.common.entities.ai.misc.AIAltarFocus;
import thaumcraft.common.entities.monster.EntityEldritchGuardian;
import thaumcraft.common.entities.projectile.EntityGolemOrb;
import thaumcraft.common.lib.SoundsTC;

public class EntityCultistCleric extends EntityCultist implements IRangedAttackMob, IEntityAdditionalSpawnData {
   public int rage = 0;
   private static final DataParameter<Boolean> RITUALIST = EntityDataManager.createKey(EntityCultistCleric.class, DataSerializers.BOOLEAN);

   public EntityCultistCleric(World p_i1745_1_) {
      super(p_i1745_1_);
   }

   protected void initEntityAI() {
      this.tasks.addTask(0, new EntityAISwimming(this));
      this.tasks.addTask(1, new AIAltarFocus(this));
      this.tasks.addTask(2, new AILongRangeAttack(this, 2.0, 1.0, 20, 40, 24.0F));
      this.tasks.addTask(3, new EntityAIAttackMelee(this, 1.0, false));
      this.tasks.addTask(4, new EntityAIRestrictOpenDoor(this));
      this.tasks.addTask(5, new EntityAIOpenDoor(this, true));
      this.tasks.addTask(6, new EntityAIMoveTowardsRestriction(this, 0.8));
      this.tasks.addTask(7, new EntityAIWander(this, 0.8));
      this.tasks.addTask(8, new EntityAIWatchClosest(this, EntityPlayer.class, 8.0F));
      this.tasks.addTask(8, new EntityAILookIdle(this));
      this.targetTasks.addTask(1, new AICultistHurtByTarget(this, true));
      this.targetTasks.addTask(2, new EntityAINearestAttackableTarget(this, EntityPlayer.class, true));
      this.targetTasks.addTask(3, new EntityAINearestAttackableTarget(this, EntityEldritchGuardian.class, true));
      this.targetTasks.addTask(4, new EntityAINearestAttackableTarget(this, AbstractIllager.class, true));
   }

   @Override
   protected void applyEntityAttributes() {
      super.applyEntityAttributes();
      this.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(24.0);
   }

   @Override
   protected void setLoot(DifficultyInstance diff) {
      this.setItemStackToSlot(EntityEquipmentSlot.HEAD, new ItemStack(ItemsTC.crimsonRobeHelm));
      this.setItemStackToSlot(EntityEquipmentSlot.CHEST, new ItemStack(ItemsTC.crimsonRobeChest));
      this.setItemStackToSlot(EntityEquipmentSlot.LEGS, new ItemStack(ItemsTC.crimsonRobeLegs));
      if (this.rand.nextFloat() < (this.world.getDifficulty() == EnumDifficulty.HARD ? 0.3F : 0.1F)) {
         this.setItemStackToSlot(EntityEquipmentSlot.FEET, new ItemStack(ItemsTC.crimsonBoots));
      }
   }

   public void attackEntityWithRangedAttack(EntityLivingBase entitylivingbase, float f) {
      double d0 = entitylivingbase.posX - this.posX;
      double d1 = entitylivingbase.getEntityBoundingBox().minY + entitylivingbase.height / 2.0F - (this.posY + this.height / 2.0F);
      double d2 = entitylivingbase.posZ - this.posZ;
      this.swingArm(this.getActiveHand());
      float rf = this.rand.nextFloat();
      if (rf > 0.66F) {
         EntityGolemOrb blast = new EntityGolemOrb(this.world, this, entitylivingbase, true);
         Vec3d v = entitylivingbase.getPositionVector()
            .add(entitylivingbase.motionX * 10.0, entitylivingbase.motionY * 10.0, entitylivingbase.motionZ * 10.0)
            .subtract(this.getPositionVector())
            .normalize();
         blast.setPosition(blast.posX + v.x, blast.posY + v.y, blast.posZ + v.z);
         blast.shoot(v.x, v.y, v.z, 0.66F, 3.0F);
         this.playSound(SoundsTC.egattack, 1.0F, 1.0F + this.rand.nextFloat() * 0.1F);
         this.world.spawnEntity(blast);
      } else {
         float f1 = MathHelper.sqrt(f) * 0.5F;
         this.world.playEvent((EntityPlayer)null, 1009, this.getPosition(), 0);

         for (int i = 0; i < 3; i++) {
            EntitySmallFireball entitysmallfireball = new EntitySmallFireball(
               this.world, this, d0 + this.rand.nextGaussian() * f1, d1, d2 + this.rand.nextGaussian() * f1
            );
            entitysmallfireball.posY = this.posY + this.height / 2.0F + 0.5;
            this.world.spawnEntity(entitysmallfireball);
         }
      }
   }

   @Override
   protected boolean canDespawn() {
      return !this.getIsRitualist();
   }

   @Override
   public void entityInit() {
      super.entityInit();
      this.getDataManager().register(RITUALIST, false);
   }

   public boolean getIsRitualist() {
      return (Boolean)this.getDataManager().get(RITUALIST);
   }

   public void setIsRitualist(boolean par1) {
      this.getDataManager().set(RITUALIST, par1);
   }

   public boolean attackEntityFrom(DamageSource source, float amount) {
      if (this.isEntityInvulnerable(source)) {
         return false;
      } else {
         this.setIsRitualist(false);
         return super.attackEntityFrom(source, amount);
      }
   }

   @Override
   public void readEntityFromNBT(NBTTagCompound par1NBTTagCompound) {
      super.readEntityFromNBT(par1NBTTagCompound);
      this.setIsRitualist(Boolean.valueOf(par1NBTTagCompound.getBoolean("ritualist")));
   }

   @Override
   public void writeEntityToNBT(NBTTagCompound par1NBTTagCompound) {
      super.writeEntityToNBT(par1NBTTagCompound);
      par1NBTTagCompound.setBoolean("ritualist", this.getIsRitualist());
   }

   public void writeSpawnData(ByteBuf data) {
      data.writeInt(this.getHomePosition().getX());
      data.writeInt(this.getHomePosition().getY());
      data.writeInt(this.getHomePosition().getZ());
   }

   public void readSpawnData(ByteBuf data) {
      this.setHomePosAndDistance(new BlockPos(data.readInt(), data.readInt(), data.readInt()), 8);
   }

   public void onUpdate() {
      super.onUpdate();
      if (this.world.isRemote && this.getIsRitualist()) {
         double d0 = this.getHomePosition().getX() + 0.5 - this.posX;
         double d1 = this.getHomePosition().getY() + 1.5 - (this.posY + this.getEyeHeight());
         double d2 = this.getHomePosition().getZ() + 0.5 - this.posZ;
         double d3 = MathHelper.sqrt(d0 * d0 + d2 * d2);
         float f = (float)(Math.atan2(d2, d0) * 180.0 / Math.PI) - 90.0F;
         float f1 = (float)(-(Math.atan2(d1, d3) * 180.0 / Math.PI));
         this.rotationPitch = this.updateRotation(this.rotationPitch, f1, 10.0F);
         this.rotationYawHead = this.updateRotation(this.rotationYawHead, f, this.getVerticalFaceSpeed());
      }

      if (!this.world.isRemote && this.getIsRitualist() && this.rage >= 5) {
         this.setIsRitualist(false);
      }
   }

   private float updateRotation(float p_75652_1_, float p_75652_2_, float p_75652_3_) {
      float f3 = MathHelper.wrapDegrees(p_75652_2_ - p_75652_1_);
      if (f3 > p_75652_3_) {
         f3 = p_75652_3_;
      }

      if (f3 < -p_75652_3_) {
         f3 = -p_75652_3_;
      }

      return p_75652_1_ + f3;
   }

   protected SoundEvent getAmbientSound() {
      return SoundsTC.chant;
   }

   public int getTalkInterval() {
      return 500;
   }

   @SideOnly(Side.CLIENT)
   public void handleStatusUpdate(byte par1) {
      if (par1 == 19) {
         for (int i = 0; i < 3; i++) {
            double d0 = this.rand.nextGaussian() * 0.02;
            double d1 = this.rand.nextGaussian() * 0.02;
            double d2 = this.rand.nextGaussian() * 0.02;
            this.world
               .spawnParticle(
                  EnumParticleTypes.VILLAGER_ANGRY,
                  this.posX + this.rand.nextFloat() * this.width * 2.0F - this.width,
                  this.posY + 0.5 + this.rand.nextFloat() * this.height,
                  this.posZ + this.rand.nextFloat() * this.width * 2.0F - this.width,
                  d0,
                  d1,
                  d2,
                  new int[0]
               );
         }
      } else {
         super.handleStatusUpdate(par1);
      }
   }

   public void setSwingingArms(boolean swingingArms) {
   }
}
