package thaumcraft.common.entities.monster.boss;

import java.util.List;
import net.minecraft.entity.IEntityLivingData;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.MobEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.BossInfoServer;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.World;
import net.minecraft.world.BossInfo.Color;
import net.minecraft.world.BossInfo.Overlay;
import thaumcraft.api.entities.IEldritchMob;
import thaumcraft.api.entities.ITaintedMob;
import thaumcraft.api.items.ItemsTC;
import thaumcraft.common.entities.monster.tainted.EntityTaintacle;
import thaumcraft.common.lib.utils.EntityUtils;

public class EntityTaintacleGiant extends EntityTaintacle implements ITaintedMob, IEldritchMob {
   protected final BossInfoServer bossInfo = (BossInfoServer)new BossInfoServer(this.getDisplayName(), Color.PURPLE, Overlay.PROGRESS).setDarkenSky(true);
   private static final DataParameter<Integer> AGGRO = EntityDataManager.createKey(EntityTaintacleGiant.class, DataSerializers.VARINT);

   public EntityTaintacleGiant(World par1World) {
      super(par1World);
      this.setSize(1.1F, 6.0F);
      this.experienceValue = 20;
   }

   @Override
   protected void applyEntityAttributes() {
      super.applyEntityAttributes();
      this.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(175.0);
      this.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).setBaseValue(9.0);
   }

   public IEntityLivingData onInitialSpawn(DifficultyInstance diff, IEntityLivingData data) {
      EntityUtils.makeChampion(this, true);
      return data;
   }

   @Override
   public void onUpdate() {
      super.onUpdate();
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

      if (!this.world.isRemote && this.ticksExisted % 30 == 0) {
         this.heal(1.0F);
      }
   }

   protected void entityInit() {
      super.entityInit();
      this.getDataManager().register(AGGRO, 0);
   }

   public int getAnger() {
      return (Integer)this.getDataManager().get(AGGRO);
   }

   public void setAnger(int par1) {
      this.getDataManager().set(AGGRO, par1);
   }

   @Override
   public boolean getCanSpawnHere() {
      return false;
   }

   @Override
   protected void dropFewItems(boolean flag, int i) {
      List<EntityTaintacleGiant> ents = EntityUtils.getEntitiesInRange(
         this.world, this.posX, this.posY, this.posZ, this, EntityTaintacleGiant.class, 48.0
      );
      if (ents == null || ents.size() <= 0) {
         EntityUtils.entityDropSpecialItem(this, new ItemStack(ItemsTC.primordialPearl), this.height / 2.0F);
      }
   }

   protected boolean canDespawn() {
      return false;
   }

   public boolean canBreatheUnderwater() {
      return true;
   }

   protected int decreaseAirSupply(int air) {
      return air;
   }

   public boolean attackEntityFrom(DamageSource source, float damage) {
      if (!this.world.isRemote && damage > 35.0F) {
         if (this.getAnger() == 0) {
            try {
               this.addPotionEffect(new PotionEffect(MobEffects.REGENERATION, 200, (int)(damage / 15.0F)));
               this.addPotionEffect(new PotionEffect(MobEffects.STRENGTH, 200, (int)(damage / 10.0F)));
               this.addPotionEffect(new PotionEffect(MobEffects.HASTE, 200, (int)(damage / 40.0F)));
               this.setAnger(200);
            } catch (Exception var4) {
            }

            if (source.getTrueSource() != null && source.getTrueSource() instanceof EntityPlayer) {
               ((EntityPlayer)source.getTrueSource())
                  .sendStatusMessage(new TextComponentTranslation(this.getName() + " " + I18n.translateToLocal("tc.boss.enrage"), new Object[0]), true);
            }
         }

         damage = 35.0F;
      }

      return super.attackEntityFrom(source, damage);
   }

   protected void updateAITasks() {
      super.updateAITasks();
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
}
