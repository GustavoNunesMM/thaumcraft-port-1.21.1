package thaumcraft.common.entities.construct;

import com.google.common.base.Optional;
import java.util.UUID;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.IEntityOwnable;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemNameTag;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.management.PreYggdrasilConverter;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.World;
import thaumcraft.common.lib.SoundsTC;

public class EntityOwnedConstruct extends EntityCreature implements IEntityOwnable {
   protected static final DataParameter<Byte> TAMED = EntityDataManager.createKey(EntityOwnedConstruct.class, DataSerializers.BYTE);
   protected static final DataParameter<Optional<UUID>> OWNER_UNIQUE_ID = EntityDataManager.createKey(
      EntityOwnedConstruct.class, DataSerializers.OPTIONAL_UNIQUE_ID
   );
   boolean validSpawn = false;

   public EntityOwnedConstruct(World worldIn) {
      super(worldIn);
   }

   protected void entityInit() {
      super.entityInit();
      this.getDataManager().register(TAMED, (byte)0);
      this.getDataManager().register(OWNER_UNIQUE_ID, Optional.absent());
   }

   public boolean isOwned() {
      return ((Byte)this.getDataManager().get(TAMED) & 4) != 0;
   }

   public void setOwned(boolean tamed) {
      byte b0 = (Byte)this.getDataManager().get(TAMED);
      if (tamed) {
         this.getDataManager().set(TAMED, (byte)(b0 | 4));
      } else {
         this.getDataManager().set(TAMED, (byte)(b0 & -5));
      }
   }

   public UUID getOwnerId() {
      return (UUID)((Optional)this.getDataManager().get(OWNER_UNIQUE_ID)).orNull();
   }

   public void setOwnerId(UUID p_184754_1_) {
      this.getDataManager().set(OWNER_UNIQUE_ID, Optional.fromNullable(p_184754_1_));
   }

   protected int decreaseAirSupply(int air) {
      return air;
   }

   public boolean canBreatheUnderwater() {
      return true;
   }

   protected SoundEvent getAmbientSound() {
      return SoundsTC.clack;
   }

   protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
      return SoundsTC.clack;
   }

   protected SoundEvent getDeathSound() {
      return SoundsTC.tool;
   }

   public int getTalkInterval() {
      return 240;
   }

   protected boolean canDespawn() {
      return false;
   }

   public void onUpdate() {
      super.onUpdate();
      if (this.getAttackTarget() != null && this.isOnSameTeam(this.getAttackTarget())) {
         this.setAttackTarget(null);
      }

      if (!this.world.isRemote && !this.validSpawn) {
         this.setDead();
      }
   }

   public void setValidSpawn() {
      this.validSpawn = true;
   }

   public void writeEntityToNBT(NBTTagCompound tagCompound) {
      super.writeEntityToNBT(tagCompound);
      tagCompound.setBoolean("v", this.validSpawn);
      if (this.getOwnerId() == null) {
         tagCompound.setString("OwnerUUID", "");
      } else {
         tagCompound.setString("OwnerUUID", this.getOwnerId().toString());
      }
   }

   public void readEntityFromNBT(NBTTagCompound tagCompound) {
      super.readEntityFromNBT(tagCompound);
      this.validSpawn = tagCompound.getBoolean("v");
      String s = "";
      if (tagCompound.hasKey("OwnerUUID", 8)) {
         s = tagCompound.getString("OwnerUUID");
      } else {
         String s1 = tagCompound.getString("Owner");
         s = PreYggdrasilConverter.convertMobOwnerIfNeeded(this.getServer(), s1);
      }

      if (!s.isEmpty()) {
         try {
            this.setOwnerId(UUID.fromString(s));
            this.setOwned(true);
         } catch (Throwable var41) {
            this.setOwned(false);
         }
      }
   }

   public EntityLivingBase getOwnerEntity() {
      try {
         UUID uuid = this.getOwnerId();
         return uuid == null ? null : this.world.getPlayerEntityByUUID(uuid);
      } catch (IllegalArgumentException var21) {
         return null;
      }
   }

   public boolean isOwner(EntityLivingBase entityIn) {
      return entityIn == this.getOwnerEntity();
   }

   public Team getTeam() {
      if (this.isOwned()) {
         EntityLivingBase entitylivingbase = this.getOwnerEntity();
         if (entitylivingbase != null) {
            return entitylivingbase.getTeam();
         }
      }

      return super.getTeam();
   }

   public boolean isOnSameTeam(Entity otherEntity) {
      if (this.isOwned()) {
         EntityLivingBase entitylivingbase1 = this.getOwnerEntity();
         if (otherEntity == entitylivingbase1) {
            return true;
         }

         if (entitylivingbase1 != null) {
            return entitylivingbase1.isOnSameTeam(otherEntity);
         }
      }

      return super.isOnSameTeam(otherEntity);
   }

   public void onDeath(DamageSource cause) {
      if (!this.world.isRemote
         && this.world.getGameRules().getBoolean("showDeathMessages")
         && this.hasCustomName()
         && this.getOwnerEntity() instanceof EntityPlayerMP) {
         ((EntityPlayerMP)this.getOwnerEntity()).sendMessage(this.getCombatTracker().getDeathMessage());
      }

      super.onDeath(cause);
   }

   public Entity getOwner() {
      return this.getOwnerEntity();
   }

   protected boolean processInteract(EntityPlayer player, EnumHand hand) {
      if (this.isDead) {
         return false;
      } else if (!player.isSneaking() && (player.getHeldItemMainhand() == null || !(player.getHeldItemMainhand().getItem() instanceof ItemNameTag))) {
         if (!this.world.isRemote && !this.isOwner(player)) {
            player.sendStatusMessage(new TextComponentTranslation("§5§o" + I18n.translateToLocal("tc.notowned"), new Object[0]), true);
            return true;
         } else {
            return super.processInteract(player, hand);
         }
      } else {
         return false;
      }
   }
}
