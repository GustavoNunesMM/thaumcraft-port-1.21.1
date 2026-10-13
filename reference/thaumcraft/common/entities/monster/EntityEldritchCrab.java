package thaumcraft.common.entities.monster;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EnumCreatureAttribute;
import net.minecraft.entity.IEntityLivingData;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIAttackMelee;
import net.minecraft.entity.ai.EntityAIHurtByTarget;
import net.minecraft.entity.ai.EntityAILeapAtTarget;
import net.minecraft.entity.ai.EntityAILookIdle;
import net.minecraft.entity.ai.EntityAINearestAttackableTarget;
import net.minecraft.entity.ai.EntityAISwimming;
import net.minecraft.entity.ai.EntityAIWander;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.monster.EntitySpider.GroupData;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.init.MobEffects;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.pathfinding.PathNavigateGround;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.World;
import thaumcraft.api.entities.IEldritchMob;
import thaumcraft.api.items.ItemsTC;
import thaumcraft.common.entities.monster.cult.EntityCultist;
import thaumcraft.common.lib.SoundsTC;

public class EntityEldritchCrab extends EntityMob implements IEldritchMob {
   private static final DataParameter<Boolean> HELM = EntityDataManager.createKey(EntityEldritchCrab.class, DataSerializers.BOOLEAN);
   private static final DataParameter<Integer> RIDING = EntityDataManager.createKey(EntityEldritchCrab.class, DataSerializers.VARINT);
   private int attackTime = 0;

   public EntityEldritchCrab(World par1World) {
      super(par1World);
      this.setSize(0.8F, 0.6F);
      this.experienceValue = 6;
      ((PathNavigateGround)this.getNavigator()).setBreakDoors(true);
   }

   protected void initEntityAI() {
      this.tasks.addTask(0, new EntityAISwimming(this));
      this.tasks.addTask(2, new EntityAILeapAtTarget(this, 0.63F));
      this.tasks.addTask(3, new EntityAIAttackMelee(this, 1.0, false));
      this.tasks.addTask(7, new EntityAIWander(this, 0.8));
      this.tasks.addTask(8, new EntityAILookIdle(this));
      this.targetTasks.addTask(1, new EntityAIHurtByTarget(this, true, new Class[0]));
      this.targetTasks.addTask(2, new EntityAINearestAttackableTarget(this, EntityPlayer.class, true));
      this.targetTasks.addTask(3, new EntityAINearestAttackableTarget(this, EntityCultist.class, true));
   }

   public double getYOffset() {
      return this.isRiding() ? 0.5 : 0.0;
   }

   protected void applyEntityAttributes() {
      super.applyEntityAttributes();
      this.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(20.0);
      this.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).setBaseValue(4.0);
      this.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(this.hasHelm() ? 0.275 : 0.3);
   }

   protected void entityInit() {
      super.entityInit();
      this.getDataManager().register(HELM, false);
      this.getDataManager().register(RIDING, -1);
   }

   public int getRiding() {
      return (Integer)this.getDataManager().get(RIDING);
   }

   public void setRiding(int s) {
      this.getDataManager().set(RIDING, s);
   }

   public boolean canPickUpLoot() {
      return false;
   }

   public int getTotalArmorValue() {
      return this.hasHelm() ? 5 : 0;
   }

   public IEntityLivingData onInitialSpawn(DifficultyInstance diff, IEntityLivingData data) {
      if (this.world.getDifficulty() == EnumDifficulty.HARD) {
         this.setHelm(true);
      } else {
         this.setHelm(this.rand.nextFloat() < 0.33F);
      }

      if (data == null) {
         data = new GroupData();
         if (this.world.getDifficulty() == EnumDifficulty.HARD && this.world.rand.nextFloat() < 0.1F * diff.getClampedAdditionalDifficulty()) {
            ((GroupData)data).setRandomEffect(this.world.rand);
         }
      }

      if (data instanceof GroupData) {
         Potion potion = ((GroupData)data).effect;
         if (potion != null) {
            this.addPotionEffect(new PotionEffect(potion, Integer.MAX_VALUE));
         }
      }

      return super.onInitialSpawn(diff, data);
   }

   public boolean hasHelm() {
      return (Boolean)this.getDataManager().get(HELM);
   }

   public void setHelm(boolean par1) {
      this.getDataManager().set(HELM, par1);
   }

   public void onUpdate() {
      super.onUpdate();
      this.attackTime--;
      if (this.ticksExisted < 20) {
         this.fallDistance = 0.0F;
      }

      if (!this.world.isRemote) {
         if (this.getRidingEntity() == null
            && this.getAttackTarget() != null
            && !this.getAttackTarget().isBeingRidden()
            && !this.onGround
            && !this.hasHelm()
            && !this.getAttackTarget().isDead
            && this.posY - this.getAttackTarget().posY >= this.getAttackTarget().height / 2.0F
            && this.getDistanceSq(this.getAttackTarget()) < 4.0) {
            this.startRiding(this.getAttackTarget());
            this.setRiding(this.getAttackTarget().getEntityId());
         }

         if (this.getRidingEntity() != null && !this.isDead && this.attackTime <= 0) {
            this.attackTime = 10 + this.rand.nextInt(10);
            this.attackEntityAsMob(this.getRidingEntity());
            if (this.rand.nextFloat() < 0.2) {
               this.dismountRidingEntity();
               this.setRiding(-1);
            }
         }

         if (this.getRidingEntity() == null && this.getRiding() != -1) {
            this.setRiding(-1);
         }
      } else if (this.getRidingEntity() == null && this.getRiding() != -1) {
         Entity e = this.world.getEntityByID(this.getRiding());
         if (e != null) {
            this.startRiding(e);
         }
      } else if (this.getRidingEntity() != null && this.getRiding() == -1) {
         this.dismountRidingEntity();
      }
   }

   protected Item getDropItem() {
      return Item.getItemById(0);
   }

   protected void dropFewItems(boolean wasRecentlyHit, int lootingModifier) {
      super.dropFewItems(wasRecentlyHit, lootingModifier);
      if (wasRecentlyHit && (this.rand.nextInt(3) == 0 || this.rand.nextInt(1 + lootingModifier) > 0)) {
         this.dropItem(Items.ENDER_PEARL, 1);
      }
   }

   public boolean attackEntityAsMob(Entity entityIn) {
      if (super.attackEntityAsMob(entityIn)) {
         this.playSound(SoundsTC.crabclaw, 1.0F, 0.9F + this.world.rand.nextFloat() * 0.2F);
         return true;
      } else {
         return false;
      }
   }

   public boolean attackEntityFrom(DamageSource source, float damage) {
      boolean b = super.attackEntityFrom(source, damage);
      if (this.hasHelm() && this.getHealth() / this.getMaxHealth() <= 0.5F) {
         this.setHelm(false);
         this.renderBrokenItemStack(new ItemStack(ItemsTC.crimsonPlateChest));
         this.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.3);
      }

      return b;
   }

   public void readEntityFromNBT(NBTTagCompound par1NBTTagCompound) {
      super.readEntityFromNBT(par1NBTTagCompound);
      this.setHelm(par1NBTTagCompound.getBoolean("helm"));
      if (!this.hasHelm()) {
         this.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.3);
      }
   }

   public void writeEntityToNBT(NBTTagCompound par1NBTTagCompound) {
      super.writeEntityToNBT(par1NBTTagCompound);
      par1NBTTagCompound.setBoolean("helm", this.hasHelm());
   }

   public int getTalkInterval() {
      return 160;
   }

   protected SoundEvent getAmbientSound() {
      return SoundsTC.crabtalk;
   }

   protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
      return SoundEvents.ENTITY_GENERIC_HURT;
   }

   protected SoundEvent getDeathSound() {
      return SoundsTC.crabdeath;
   }

   protected void playStepSound(BlockPos pos, Block blockIn) {
      this.playSound(SoundEvents.ENTITY_SPIDER_STEP, 0.15F, 1.0F);
   }

   public EnumCreatureAttribute getCreatureAttribute() {
      return EnumCreatureAttribute.ARTHROPOD;
   }

   public boolean isPotionApplicable(PotionEffect potioneffectIn) {
      return potioneffectIn.getPotion().equals(MobEffects.POISON) ? false : super.isPotionApplicable(potioneffectIn);
   }

   public boolean isOnSameTeam(Entity el) {
      return el instanceof EntityEldritchCrab;
   }
}
