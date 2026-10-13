package thaumcraft.common.entities.monster.boss;

import java.util.ArrayList;
import java.util.HashMap;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.IEntityLivingData;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.MobEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.BossInfoServer;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.World;
import net.minecraft.world.BossInfo.Color;
import net.minecraft.world.BossInfo.Overlay;
import thaumcraft.api.entities.IEldritchMob;
import thaumcraft.api.items.ItemsTC;
import thaumcraft.common.lib.utils.EntityUtils;

public class EntityThaumcraftBoss extends EntityMob {
   protected final BossInfoServer bossInfo = (BossInfoServer)new BossInfoServer(this.getDisplayName(), Color.PURPLE, Overlay.PROGRESS).setDarkenSky(true);
   private static final DataParameter<Integer> AGGRO = EntityDataManager.createKey(EntityThaumcraftBoss.class, DataSerializers.VARINT);
   HashMap<Integer, Integer> aggro = new HashMap<>();
   int spawnTimer = 0;

   public EntityThaumcraftBoss(World world) {
      super(world);
      this.experienceValue = 50;
   }

   public void readEntityFromNBT(NBTTagCompound nbt) {
      super.readEntityFromNBT(nbt);
      if (nbt.hasKey("HomeD")) {
         this.setHomePosAndDistance(new BlockPos(nbt.getInteger("HomeX"), nbt.getInteger("HomeY"), nbt.getInteger("HomeZ")), nbt.getInteger("HomeD"));
      }
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

   protected void applyEntityAttributes() {
      super.applyEntityAttributes();
      this.getEntityAttribute(SharedMonsterAttributes.KNOCKBACK_RESISTANCE).setBaseValue(0.95);
      this.getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).setBaseValue(40.0);
   }

   protected void entityInit() {
      super.entityInit();
      this.getDataManager().register(AGGRO, 0);
   }

   protected void updateAITasks() {
      if (this.getSpawnTimer() == 0) {
         super.updateAITasks();
      }

      if (this.getAttackTarget() != null && this.getAttackTarget().isDead) {
         this.setAttackTarget(null);
      }

      this.bossInfo.setPercent(this.getHealth() / this.getMaxHealth());
   }

   public void removeTrackingPlayer(EntityPlayerMP player) {
      super.removeTrackingPlayer(player);
      this.bossInfo.removePlayer(player);
   }

   public void addTrackingPlayer(EntityPlayerMP player) {
      super.addTrackingPlayer(player);
      this.bossInfo.addPlayer(player);
   }

   public boolean isNonBoss() {
      return false;
   }

   public IEntityLivingData onInitialSpawn(DifficultyInstance diff, IEntityLivingData data) {
      this.setHomePosAndDistance(this.getPosition(), 24);
      this.generateName();
      this.bossInfo.setName(this.getDisplayName());
      return data;
   }

   public int getAnger() {
      return (Integer)this.getDataManager().get(AGGRO);
   }

   public void setAnger(int par1) {
      this.getDataManager().set(AGGRO, par1);
   }

   public int getSpawnTimer() {
      return this.spawnTimer;
   }

   public void onUpdate() {
      super.onUpdate();
      if (this.getSpawnTimer() > 0) {
         this.spawnTimer--;
      }

      if (this.getAnger() > 0) {
         this.setAnger(this.getAnger() - 1);
      }

      if (this.world.isRemote && this.rand.nextInt(15) == 0 && this.getAnger() > 0) {
         double d0 = this.rand.nextGaussian() * 0.02;
         double d1 = this.rand.nextGaussian() * 0.02;
         double d2 = this.rand.nextGaussian() * 0.02;
         this.world
            .spawnParticle(
               EnumParticleTypes.VILLAGER_ANGRY,
               this.posX + this.rand.nextFloat() * this.width - this.width / 2.0,
               this.getEntityBoundingBox().minY + this.height + this.rand.nextFloat() * 0.5,
               this.posZ + this.rand.nextFloat() * this.width - this.width / 2.0,
               d0,
               d1,
               d2,
               new int[0]
            );
      }

      if (!this.world.isRemote) {
         if (this.ticksExisted % 30 == 0) {
            this.heal(1.0F);
         }

         if (this.getAttackTarget() != null && this.ticksExisted % 20 == 0) {
            ArrayList<Integer> dl = new ArrayList<>();
            int players = 0;
            int hei = this.getAttackTarget().getEntityId();
            int ad = this.aggro.containsKey(hei) ? this.aggro.get(hei) : 0;
            int ld = ad;
            Entity newTarget = null;

            for (Integer ei : this.aggro.keySet()) {
               int ca = this.aggro.get(ei);
               if (ca > ad + 25 && ca > ad * 1.1 && ca > ld) {
                  newTarget = this.world.getEntityByID(hei);
                  if (newTarget != null && !newTarget.isDead && !(this.getDistanceSq(newTarget) > 16384.0)) {
                     hei = ei;
                     ld = ei;
                     if (newTarget instanceof EntityPlayer) {
                        players++;
                     }
                  } else {
                     dl.add(ei);
                  }
               }
            }

            for (Integer eix : dl) {
               this.aggro.remove(eix);
            }

            if (newTarget != null && hei != this.getAttackTarget().getEntityId()) {
               this.setAttackTarget((EntityLivingBase)newTarget);
            }

            float om = this.getMaxHealth();
            IAttributeInstance iattributeinstance = this.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH);
            IAttributeInstance iattributeinstance2 = this.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE);

            for (int a = 0; a < 5; a++) {
               iattributeinstance2.removeModifier(EntityUtils.DMGBUFF[a]);
               iattributeinstance.removeModifier(EntityUtils.HPBUFF[a]);
            }

            for (int a = 0; a < Math.min(5, players - 1); a++) {
               iattributeinstance.applyModifier(EntityUtils.HPBUFF[a]);
               iattributeinstance2.applyModifier(EntityUtils.DMGBUFF[a]);
            }

            double mm = this.getMaxHealth() / om;
            this.setHealth((float)(this.getHealth() * mm));
         }
      }
   }

   public boolean isEntityInvulnerable(DamageSource ds) {
      return super.isEntityInvulnerable(ds) || this.getSpawnTimer() > 0;
   }

   public boolean canBreatheUnderwater() {
      return true;
   }

   public boolean canBePushed() {
      return super.canBePushed() && !this.isEntityInvulnerable(DamageSource.STARVE);
   }

   protected int decreaseAirSupply(int air) {
      return air;
   }

   public void setInWeb() {
   }

   public boolean canPickUpLoot() {
      return false;
   }

   protected void setEnchantmentBasedOnDifficulty(DifficultyInstance diff) {
   }

   protected boolean canDespawn() {
      return false;
   }

   public boolean isOnSameTeam(Entity el) {
      return el instanceof IEldritchMob;
   }

   protected void dropFewItems(boolean flag, int fortune) {
      EntityUtils.entityDropSpecialItem(this, new ItemStack(ItemsTC.primordialPearl), this.height / 2.0F);
      this.entityDropItem(new ItemStack(ItemsTC.lootBag, 1, 2), 1.5F);
   }

   public boolean attackEntityFrom(DamageSource source, float damage) {
      if (!this.world.isRemote) {
         if (source.getTrueSource() != null && source.getTrueSource() instanceof EntityLivingBase) {
            int target = source.getTrueSource().getEntityId();
            int ad = (int)damage;
            if (this.aggro.containsKey(target)) {
               ad += this.aggro.get(target);
            }

            this.aggro.put(target, ad);
         }

         if (damage > 35.0F) {
            if (this.getAnger() == 0) {
               try {
                  try {
                     this.addPotionEffect(new PotionEffect(MobEffects.REGENERATION, 200, (int)(damage / 15.0F)));
                     this.addPotionEffect(new PotionEffect(MobEffects.STRENGTH, 200, (int)(damage / 10.0F)));
                     this.addPotionEffect(new PotionEffect(MobEffects.HASTE, 200, (int)(damage / 40.0F)));
                  } catch (Exception var5) {
                  }

                  this.setAnger(200);
               } catch (Exception var6) {
               }

               if (source.getTrueSource() != null && source.getTrueSource() instanceof EntityPlayer) {
                  ((EntityPlayer)source.getTrueSource())
                     .sendStatusMessage(new TextComponentTranslation(this.getName() + " " + I18n.translateToLocal("tc.boss.enrage"), new Object[0]), true);
               }
            }

            damage = 35.0F;
         }
      }

      return super.attackEntityFrom(source, damage);
   }

   public void generateName() {
   }
}
