package thaumcraft.common.entities.monster.cult;

import java.util.List;
import net.minecraft.entity.Entity;
import net.minecraft.entity.IEntityLivingData;
import net.minecraft.entity.MoverType;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import thaumcraft.common.lib.SoundsTC;
import thaumcraft.common.lib.utils.EntityUtils;

public class EntityCultistPortalLesser extends EntityMob {
   private static final DataParameter<Boolean> ACTIVE = EntityDataManager.createKey(EntityCultistPortalLesser.class, DataSerializers.BOOLEAN);
   int stagecounter = 100;
   public int activeCounter = 0;
   public int pulse = 0;

   public EntityCultistPortalLesser(World par1World) {
      super(par1World);
      this.isImmuneToFire = true;
      this.experienceValue = 10;
      this.setSize(1.5F, 3.0F);
   }

   public int getTotalArmorValue() {
      return 4;
   }

   protected void entityInit() {
      super.entityInit();
      this.getDataManager().register(ACTIVE, false);
   }

   public boolean isActive() {
      return (Boolean)this.getDataManager().get(ACTIVE);
   }

   public void setActive(boolean active) {
      this.getDataManager().set(ACTIVE, active);
   }

   protected void applyEntityAttributes() {
      super.applyEntityAttributes();
      this.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(100.0);
      this.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).setBaseValue(0.0);
      this.getEntityAttribute(SharedMonsterAttributes.KNOCKBACK_RESISTANCE).setBaseValue(1.0);
   }

   protected boolean canDespawn() {
      return false;
   }

   public void writeEntityToNBT(NBTTagCompound nbt) {
      super.writeEntityToNBT(nbt);
      nbt.setBoolean("active", this.isActive());
   }

   public void readEntityFromNBT(NBTTagCompound nbt) {
      super.readEntityFromNBT(nbt);
      this.setActive(nbt.getBoolean("active"));
   }

   public boolean canBeCollidedWith() {
      return true;
   }

   public boolean canBePushed() {
      return false;
   }

   public void move(MoverType mt, double par1, double par3, double par5) {
   }

   public void onLivingUpdate() {
   }

   public boolean isInRangeToRenderDist(double par1) {
      return par1 < 4096.0;
   }

   @SideOnly(Side.CLIENT)
   public int getBrightnessForRender() {
      return 15728880;
   }

   public float getBrightness() {
      return 1.0F;
   }

   public void onUpdate() {
      super.onUpdate();
      if (this.isActive()) {
         this.activeCounter++;
      }

      if (!this.world.isRemote) {
         if (!this.isActive()) {
            if (this.ticksExisted % 10 == 0) {
               EntityPlayer p = this.world.getClosestPlayerToEntity(this, 32.0);
               if (p != null) {
                  this.setActive(true);
                  this.playSound(SoundsTC.craftstart, 1.0F, 1.0F);
               }
            }
         } else if (this.stagecounter-- <= 0) {
            EntityPlayer p = this.world.getClosestPlayerToEntity(this, 32.0);
            if (p != null && this.canEntityBeSeen(p)) {
               int count = this.world.getDifficulty() == EnumDifficulty.HARD
                  ? 6
                  : (this.world.getDifficulty() == EnumDifficulty.NORMAL ? 4 : 2);

               try {
                  List l = this.world.getEntitiesWithinAABB(EntityCultist.class, this.getEntityBoundingBox().grow(32.0, 32.0, 32.0));
                  if (l != null) {
                     count -= l.size();
                  }
               } catch (Exception var4) {
               }

               if (count > 0) {
                  this.world.setEntityState(this, (byte)16);
                  this.spawnMinions();
               }
            }

            this.stagecounter = 50 + this.rand.nextInt(50);
         }
      }

      if (this.pulse > 0) {
         this.pulse--;
      }
   }

   int getTiming() {
      List<Entity> l = EntityUtils.getEntitiesInRange(
         this.world, this.posX, this.posY, this.posZ, this, EntityCultist.class, 32.0
      );
      return l.size() * 20;
   }

   void spawnMinions() {
      EntityCultist cultist = null;
      if (this.rand.nextFloat() > 0.33) {
         cultist = new EntityCultistKnight(this.world);
      } else {
         cultist = new EntityCultistCleric(this.world);
      }

      cultist.setPosition(
         this.posX + this.rand.nextFloat() - this.rand.nextFloat(),
         this.posY + 0.25,
         this.posZ + this.rand.nextFloat() - this.rand.nextFloat()
      );
      cultist.onInitialSpawn(this.world.getDifficultyForLocation(new BlockPos(cultist.getPosition())), (IEntityLivingData)null);
      this.world.spawnEntity(cultist);
      cultist.spawnExplosionParticle();
      cultist.playSound(SoundsTC.wandfail, 1.0F, 1.0F);
      this.attackEntityFrom(DamageSource.OUT_OF_WORLD, 5 + this.rand.nextInt(5));
   }

   protected boolean isValidLightLevel() {
      return true;
   }

   public void onCollideWithPlayer(EntityPlayer p) {
      if (this.getDistanceSq(p) < 3.0 && p.attackEntityFrom(DamageSource.causeIndirectMagicDamage(this, this), 4.0F)) {
         this.playSound(SoundsTC.zap, 1.0F, (this.rand.nextFloat() - this.rand.nextFloat()) * 0.1F + 1.0F);
      }
   }

   protected float getSoundVolume() {
      return 0.75F;
   }

   public int getTalkInterval() {
      return 540;
   }

   protected SoundEvent getAmbientSound() {
      return SoundsTC.monolith;
   }

   protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
      return SoundsTC.zap;
   }

   protected SoundEvent getDeathSound() {
      return SoundsTC.shock;
   }

   protected Item getDropItem() {
      return Item.getItemById(0);
   }

   protected void dropFewItems(boolean flag, int fortune) {
   }

   @SideOnly(Side.CLIENT)
   public void handleStatusUpdate(byte msg) {
      if (msg == 16) {
         this.pulse = 10;
      } else {
         super.handleStatusUpdate(msg);
      }
   }

   public void addPotionEffect(PotionEffect potioneffectIn) {
   }

   public void fall(float distance, float damageMultiplier) {
   }

   public void onDeath(DamageSource cause) {
      if (!this.world.isRemote) {
         this.world.newExplosion(this, this.posX, this.posY, this.posZ, 1.5F, false, false);
      }

      super.onDeath(cause);
   }
}
