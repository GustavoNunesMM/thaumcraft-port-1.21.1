package thaumcraft.common.entities.monster.cult;

import net.minecraft.entity.Entity;
import net.minecraft.entity.IEntityLivingData;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.pathfinding.PathNavigateGround;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.World;
import net.minecraft.world.storage.loot.LootTableList;
import thaumcraft.client.fx.FXDispatcher;
import thaumcraft.common.entities.monster.boss.EntityCultistLeader;

public class EntityCultist extends EntityMob {
   public static final ResourceLocation LOOT = LootTableList.register(new ResourceLocation("thaumcraft", "cultist"));

   public EntityCultist(World p_i1745_1_) {
      super(p_i1745_1_);
      this.setSize(0.6F, 1.8F);
      this.experienceValue = 10;
      ((PathNavigateGround)this.getNavigator()).setBreakDoors(true);
      this.setDropChance(EntityEquipmentSlot.CHEST, 0.05F);
      this.setDropChance(EntityEquipmentSlot.FEET, 0.05F);
      this.setDropChance(EntityEquipmentSlot.HEAD, 0.05F);
      this.setDropChance(EntityEquipmentSlot.LEGS, 0.05F);
   }

   protected void applyEntityAttributes() {
      super.applyEntityAttributes();
      this.getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).setBaseValue(32.0);
      this.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.3);
      this.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).setBaseValue(4.0);
   }

   protected void entityInit() {
      super.entityInit();
   }

   public boolean canPickUpLoot() {
      return false;
   }

   protected boolean isValidLightLevel() {
      return true;
   }

   protected Item getDropItem() {
      return Item.getItemById(0);
   }

   protected ResourceLocation getLootTable() {
      return LOOT;
   }

   protected void setLoot(DifficultyInstance diff) {
   }

   protected void setEnchantmentBasedOnDifficulty(DifficultyInstance diff) {
   }

   public IEntityLivingData onInitialSpawn(DifficultyInstance diff, IEntityLivingData data) {
      this.setLoot(diff);
      this.setEnchantmentBasedOnDifficulty(diff);
      return super.onInitialSpawn(diff, data);
   }

   protected boolean canDespawn() {
      return true;
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

   public boolean isOnSameTeam(Entity el) {
      return el instanceof EntityCultist || el instanceof EntityCultistLeader;
   }

   public boolean canAttackClass(Class clazz) {
      return clazz != EntityCultistCleric.class && clazz != EntityCultistLeader.class && clazz != EntityCultistKnight.class ? super.canAttackClass(clazz) : false;
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
}
