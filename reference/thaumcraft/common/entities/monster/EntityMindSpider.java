package thaumcraft.common.entities.monster;

import javax.annotation.Nullable;
import net.minecraft.entity.Entity;
import net.minecraft.entity.IEntityLivingData;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.monster.EntitySpider;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.World;
import thaumcraft.api.entities.IEldritchMob;

public class EntityMindSpider extends EntitySpider implements IEldritchMob {
   private int lifeSpan = Integer.MAX_VALUE;
   private static final DataParameter<Boolean> HARMLESS = EntityDataManager.createKey(EntityMindSpider.class, DataSerializers.BOOLEAN);
   private static final DataParameter<String> VIEWER = EntityDataManager.createKey(EntityMindSpider.class, DataSerializers.STRING);

   public EntityMindSpider(World par1World) {
      super(par1World);
      this.setSize(0.7F, 0.5F);
      this.experienceValue = 1;
   }

   @Nullable
   protected ResourceLocation getLootTable() {
      return null;
   }

   public float getEyeHeight() {
      return 0.45F;
   }

   protected int getExperiencePoints(EntityPlayer player) {
      return this.isHarmless() ? 0 : super.getExperiencePoints(player);
   }

   protected void applyEntityAttributes() {
      super.applyEntityAttributes();
      this.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(1.0);
      this.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).setBaseValue(1.0);
   }

   protected void entityInit() {
      super.entityInit();
      this.getDataManager().register(HARMLESS, false);
      this.getDataManager().register(VIEWER, String.valueOf(""));
   }

   public String getViewer() {
      return (String)this.getDataManager().get(VIEWER);
   }

   public void setViewer(String player) {
      this.getDataManager().set(VIEWER, String.valueOf(player));
   }

   public boolean isHarmless() {
      return (Boolean)this.getDataManager().get(HARMLESS);
   }

   public void setHarmless(boolean h) {
      if (h) {
         this.lifeSpan = 1200;
      }

      this.getDataManager().set(HARMLESS, h);
   }

   protected float getSoundPitch() {
      return 0.7F;
   }

   public void onUpdate() {
      super.onUpdate();
      if (!this.world.isRemote && this.ticksExisted > this.lifeSpan) {
         this.setDead();
      }
   }

   protected Item getDropItem() {
      return Item.getItemById(0);
   }

   protected void dropFewItems(boolean wasRecentlyHit, int lootingModifier) {
   }

   public boolean doesEntityNotTriggerPressurePlate() {
      return true;
   }

   protected boolean canTriggerWalking() {
      return false;
   }

   public boolean attackEntityAsMob(Entity entityIn) {
      return this.isHarmless() ? false : super.attackEntityAsMob(entityIn);
   }

   public void readEntityFromNBT(NBTTagCompound nbt) {
      super.readEntityFromNBT(nbt);
      this.setHarmless(nbt.getBoolean("harmless"));
      this.setViewer(nbt.getString("viewer"));
   }

   public void writeEntityToNBT(NBTTagCompound nbt) {
      super.writeEntityToNBT(nbt);
      nbt.setBoolean("harmless", this.isHarmless());
      nbt.setString("viewer", this.getViewer());
   }

   public IEntityLivingData onInitialSpawn(DifficultyInstance difficulty, IEntityLivingData livingdata) {
      return livingdata;
   }
}
