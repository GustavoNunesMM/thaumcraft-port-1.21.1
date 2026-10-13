package thaumcraft.common.entities.monster.boss;

import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.IEntityLivingData;
import net.minecraft.entity.IRangedAttackMob;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIAttackMelee;
import net.minecraft.entity.ai.EntityAILookIdle;
import net.minecraft.entity.ai.EntityAIMoveTowardsRestriction;
import net.minecraft.entity.ai.EntityAINearestAttackableTarget;
import net.minecraft.entity.ai.EntityAISwimming;
import net.minecraft.entity.ai.EntityAIWander;
import net.minecraft.entity.ai.EntityAIWatchClosest;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.World;
import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.api.items.ItemsTC;
import thaumcraft.client.fx.FXDispatcher;
import thaumcraft.common.entities.ai.combat.AICultistHurtByTarget;
import thaumcraft.common.entities.ai.combat.AILongRangeAttack;
import thaumcraft.common.entities.monster.cult.EntityCultist;
import thaumcraft.common.entities.monster.cult.EntityCultistCleric;
import thaumcraft.common.entities.monster.cult.EntityCultistKnight;
import thaumcraft.common.entities.monster.mods.ChampionModifier;
import thaumcraft.common.entities.projectile.EntityGolemOrb;
import thaumcraft.common.lib.SoundsTC;
import thaumcraft.common.lib.utils.EntityUtils;

public class EntityCultistLeader extends EntityThaumcraftBoss implements IRangedAttackMob {
   private static final DataParameter<Byte> NAME = EntityDataManager.createKey(EntityCultistLeader.class, DataSerializers.BYTE);
   String[] titles = new String[]{
      "Alberic",
      "Anselm",
      "Bastian",
      "Beturian",
      "Chabier",
      "Chorache",
      "Chuse",
      "Dodorol",
      "Ebardo",
      "Ferrando",
      "Fertus",
      "Guillen",
      "Larpe",
      "Obano",
      "Zelipe"
   };

   public EntityCultistLeader(World p_i1745_1_) {
      super(p_i1745_1_);
      this.setSize(0.75F, 2.25F);
      this.experienceValue = 40;
   }

   protected void initEntityAI() {
      this.tasks.addTask(0, new EntityAISwimming(this));
      this.tasks.addTask(2, new AILongRangeAttack(this, 16.0, 1.0, 30, 40, 24.0F));
      this.tasks.addTask(3, new EntityAIAttackMelee(this, 1.1, false));
      this.tasks.addTask(6, new EntityAIMoveTowardsRestriction(this, 0.8));
      this.tasks.addTask(7, new EntityAIWander(this, 0.8));
      this.tasks.addTask(8, new EntityAIWatchClosest(this, EntityPlayer.class, 8.0F));
      this.tasks.addTask(8, new EntityAILookIdle(this));
      this.targetTasks.addTask(1, new AICultistHurtByTarget(this, true));
      this.targetTasks.addTask(2, new EntityAINearestAttackableTarget(this, EntityPlayer.class, true));
   }

   @Override
   protected void applyEntityAttributes() {
      super.applyEntityAttributes();
      this.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.32);
      this.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(150.0);
      this.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).setBaseValue(5.0);
   }

   @Override
   protected void entityInit() {
      super.entityInit();
      this.getDataManager().register(NAME, (byte)0);
   }

   @Override
   public void generateName() {
      int t = (int)this.getEntityAttribute(ThaumcraftApiHelper.CHAMPION_MOD).getAttributeValue();
      if (t >= 0) {
         this.setCustomNameTag(
            String.format(I18n.translateToLocal("entity.Thaumcraft.CultistLeader.name.custom"), this.getTitle(), ChampionModifier.mods[t].getModNameLocalized())
         );
      }
   }

   private String getTitle() {
      return this.titles[this.getDataManager().get(NAME)];
   }

   private void setTitle(int title) {
      this.getDataManager().set(NAME, (byte)title);
   }

   @Override
   public void writeEntityToNBT(NBTTagCompound nbt) {
      super.writeEntityToNBT(nbt);
      nbt.setByte("title", (Byte)this.getDataManager().get(NAME));
   }

   @Override
   public void readEntityFromNBT(NBTTagCompound nbt) {
      super.readEntityFromNBT(nbt);
      this.setTitle(nbt.getByte("title"));
   }

   protected void setEquipmentBasedOnDifficulty(DifficultyInstance difficulty) {
      this.setItemStackToSlot(EntityEquipmentSlot.HEAD, new ItemStack(ItemsTC.crimsonPraetorHelm));
      this.setItemStackToSlot(EntityEquipmentSlot.CHEST, new ItemStack(ItemsTC.crimsonPraetorChest));
      this.setItemStackToSlot(EntityEquipmentSlot.LEGS, new ItemStack(ItemsTC.crimsonPraetorLegs));
      this.setItemStackToSlot(EntityEquipmentSlot.FEET, new ItemStack(ItemsTC.crimsonBoots));
      if (this.world.getDifficulty() == EnumDifficulty.EASY) {
         this.setHeldItem(this.getActiveHand(), new ItemStack(ItemsTC.voidSword));
      } else {
         this.setHeldItem(this.getActiveHand(), new ItemStack(ItemsTC.crimsonBlade));
      }
   }

   @Override
   protected void setEnchantmentBasedOnDifficulty(DifficultyInstance diff) {
      float f = diff.getClampedAdditionalDifficulty();
      if (this.getHeldItemMainhand() != null && this.rand.nextFloat() < 0.5F * f) {
         EnchantmentHelper.addRandomEnchantment(this.rand, this.getHeldItemMainhand(), (int)(7.0F + f * this.rand.nextInt(22)), false);
      }
   }

   @Override
   public boolean isOnSameTeam(Entity el) {
      return el instanceof EntityCultist || el instanceof EntityCultistLeader;
   }

   public boolean canAttackClass(Class clazz) {
      return clazz != EntityCultistCleric.class && clazz != EntityCultistLeader.class && clazz != EntityCultistKnight.class ? super.canAttackClass(clazz) : false;
   }

   protected Item getDropItem() {
      return Item.getItemById(0);
   }

   @Override
   protected void dropFewItems(boolean flag, int i) {
      this.entityDropItem(new ItemStack(ItemsTC.lootBag, 1, 2), 1.5F);
   }

   @Override
   public IEntityLivingData onInitialSpawn(DifficultyInstance diff, IEntityLivingData data) {
      this.setEquipmentBasedOnDifficulty(diff);
      this.setEnchantmentBasedOnDifficulty(diff);
      this.setTitle(this.rand.nextInt(this.titles.length));
      return super.onInitialSpawn(diff, data);
   }

   @Override
   protected void updateAITasks() {
      super.updateAITasks();

      for (Entity e : EntityUtils.getEntitiesInRange(
         this.world, this.posX, this.posY, this.posZ, this, EntityCultist.class, 8.0
      )) {
         try {
            if (e instanceof EntityCultist && !((EntityCultist)e).isPotionActive(MobEffects.REGENERATION)) {
               ((EntityCultist)e).addPotionEffect(new PotionEffect(MobEffects.REGENERATION, 60, 1));
            }
         } catch (Exception var5) {
         }
      }
   }

   public void attackEntityWithRangedAttack(EntityLivingBase entitylivingbase, float f) {
      if (this.canEntityBeSeen(entitylivingbase)) {
         this.swingArm(this.getActiveHand());
         this.getLookHelper()
            .setLookPosition(
               entitylivingbase.posX,
               entitylivingbase.getEntityBoundingBox().minY + entitylivingbase.height / 2.0F,
               entitylivingbase.posZ,
               30.0F,
               30.0F
            );
         EntityGolemOrb blast = new EntityGolemOrb(this.world, this, entitylivingbase, true);
         blast.posX = blast.posX + blast.motionX / 2.0;
         blast.posZ = blast.posZ + blast.motionZ / 2.0;
         blast.setPosition(blast.posX, blast.posY, blast.posZ);
         double d0 = entitylivingbase.posX - this.posX;
         double d1 = entitylivingbase.getEntityBoundingBox().minY + entitylivingbase.height / 2.0F - (this.posY + this.height / 2.0F);
         double d2 = entitylivingbase.posZ - this.posZ;
         blast.shoot(d0, d1 + 2.0, d2, 0.66F, 3.0F);
         this.playSound(SoundsTC.egattack, 1.0F, 1.0F + this.rand.nextFloat() * 0.1F);
         this.world.spawnEntity(blast);
      }
   }

   public void spawnExplosionParticle() {
      if (this.world.isRemote) {
         for (int i = 0; i < 20; i++) {
            double d0 = this.rand.nextGaussian() * 0.05;
            double d1 = this.rand.nextGaussian() * 0.05;
            double d2 = this.rand.nextGaussian() * 0.05;
            double d3 = 2.0;
            FXDispatcher.INSTANCE
               .cultistSpawn(
                  this.posX + this.rand.nextFloat() * this.width * 2.0F - this.width + d0 * d3,
                  this.posY + this.rand.nextFloat() * this.height + d1 * d3,
                  this.posZ + this.rand.nextFloat() * this.width * 2.0F - this.width + d2 * d3,
                  d0,
                  d1,
                  d2
               );
         }
      } else {
         this.world.setEntityState(this, (byte)20);
      }
   }

   public void setSwingingArms(boolean swingingArms) {
   }
}
