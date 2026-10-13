package thaumcraft.common.entities.monster;

import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAILeapAtTarget;
import net.minecraft.init.Items;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;

public class EntityGiantBrainyZombie extends EntityBrainyZombie {
   private static final DataParameter<Float> ANGER = EntityDataManager.createKey(EntityGiantBrainyZombie.class, DataSerializers.FLOAT);

   public EntityGiantBrainyZombie(World world) {
      super(world);
      this.experienceValue = 15;
      this.tasks.addTask(2, new EntityAILeapAtTarget(this, 0.4F));
   }

   public void onLivingUpdate() {
      super.onLivingUpdate();
      if (this.getAnger() > 1.0F) {
         this.setAnger(this.getAnger() - 0.002F);
         this.setSize(0.6F + this.getAnger() * 0.6F, 1.95F + this.getAnger() * 1.95F);
         this.multiplySize(1.0F);
      }

      this.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).setBaseValue(7.0F + (this.getAnger() - 1.0F) * 5.0F);
   }

   public float getEyeHeight() {
      float f = 1.74F + this.getAnger() * 1.74F;
      if (this.isChild()) {
         f = (float)(f - 0.81);
      }

      return f;
   }

   protected void entityInit() {
      super.entityInit();
      this.getDataManager().register(ANGER, 0.0F);
   }

   public float getAnger() {
      return (Float)this.getDataManager().get(ANGER);
   }

   public void setAnger(float par1) {
      this.getDataManager().set(ANGER, par1);
   }

   public boolean attackEntityFrom(DamageSource par1DamageSource, float par2) {
      this.setAnger(Math.min(2.0F, this.getAnger() + 0.1F));
      return super.attackEntityFrom(par1DamageSource, par2);
   }

   @Override
   protected void applyEntityAttributes() {
      super.applyEntityAttributes();
      this.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(60.0);
      this.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).setBaseValue(7.0);
   }

   protected void dropFewItems(boolean flag, int i) {
      for (int a = 0; a < 6; a++) {
         if (this.world.rand.nextBoolean()) {
            this.dropItem(Items.ROTTEN_FLESH, 2);
         }
      }

      for (int ax = 0; ax < 6; ax++) {
         if (this.world.rand.nextBoolean()) {
            this.dropItem(Items.ROTTEN_FLESH, 2);
         }
      }
   }
}
