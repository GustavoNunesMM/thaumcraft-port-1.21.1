package thaumcraft.common.entities.monster.boss;

import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.entity.IEntityLivingData;
import net.minecraft.entity.MoverType;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.PotionEffect;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BossInfoServer;
import net.minecraft.world.World;
import net.minecraft.world.BossInfo.Color;
import net.minecraft.world.BossInfo.Overlay;
import net.minecraftforge.fml.common.network.NetworkRegistry.TargetPoint;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import thaumcraft.api.blocks.BlocksTC;
import thaumcraft.api.items.ItemsTC;
import thaumcraft.common.entities.monster.cult.EntityCultist;
import thaumcraft.common.entities.monster.cult.EntityCultistCleric;
import thaumcraft.common.entities.monster.cult.EntityCultistKnight;
import thaumcraft.common.lib.SoundsTC;
import thaumcraft.common.lib.network.PacketHandler;
import thaumcraft.common.lib.network.fx.PacketFXBlockArc;
import thaumcraft.common.lib.utils.EntityUtils;
import thaumcraft.common.tiles.misc.TileBanner;

public class EntityCultistPortalGreater extends EntityMob {
   protected final BossInfoServer bossInfo = (BossInfoServer)new BossInfoServer(this.getDisplayName(), Color.RED, Overlay.NOTCHED_6).setDarkenSky(true);
   int stage = 0;
   int stagecounter = 200;
   public int pulse = 0;

   public EntityCultistPortalGreater(World par1World) {
      super(par1World);
      this.isImmuneToFire = true;
      this.experienceValue = 30;
      this.setSize(1.5F, 3.0F);
   }

   public int getTotalArmorValue() {
      return 5;
   }

   protected void entityInit() {
      super.entityInit();
   }

   public void writeEntityToNBT(NBTTagCompound nbt) {
      super.writeEntityToNBT(nbt);
      nbt.setInteger("stage", this.stage);
   }

   public void readEntityFromNBT(NBTTagCompound nbt) {
      super.readEntityFromNBT(nbt);
      this.stage = nbt.getInteger("stage");
   }

   protected void applyEntityAttributes() {
      super.applyEntityAttributes();
      this.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(500.0);
      this.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).setBaseValue(0.0);
      this.getEntityAttribute(SharedMonsterAttributes.KNOCKBACK_RESISTANCE).setBaseValue(1.0);
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

   // $VF: Unable to simplify switch-on-enum, as the enum class was not able to be found.
   // Please report this to the Vineflower issue tracker, at https://github.com/Vineflower/vineflower/issues with a copy of the class file (if you have the rights to distribute it!)
   public void onUpdate() {
      super.onUpdate();
      if (!this.world.isRemote) {
         if (this.stagecounter <= 0) {
            if (this.world.getClosestPlayerToEntity(this, 48.0) != null) {
               this.world.setEntityState(this, (byte)16);
               switch (this.stage) {
                  case 0:
                  case 1:
                  case 2:
                  case 3:
                  case 4:
                     this.stagecounter = 15 + this.rand.nextInt(10 - this.stage) - this.stage;
                     this.spawnMinions();
                     break;
                  case 5:
                  case 6:
                  case 7:
                  case 8:
                  case 9:
                  case 10:
                  case 11:
                  default:
                     int t = this.getTiming();
                     this.stagecounter = t + this.rand.nextInt(5 + t / 3);
                     this.spawnMinions();
                     break;
                  case 12:
                     this.stagecounter = 50 + this.getTiming() * 2 + this.rand.nextInt(50);
                     this.spawnBoss();
               }

               this.stage++;
            } else {
               this.stagecounter = 30 + this.rand.nextInt(30);
            }
         } else {
            this.stagecounter--;
            if (this.stagecounter == 160 && this.stage == 0) {
               this.world.setEntityState(this, (byte)16);

               for (EnumFacing dir : EnumFacing.HORIZONTALS) {
                  BlockPos bp = new BlockPos(
                     (int)this.posX - dir.getXOffset() * 6, (int)this.posY, (int)this.posZ + dir.getZOffset() * 6
                  );
                  this.world.setBlockState(bp, BlocksTC.bannerCrimsonCult.getDefaultState(), 3);
                  TileEntity te = this.world
                     .getTileEntity(
                        new BlockPos(
                           (int)this.posX - dir.getXOffset() * 6, (int)this.posY, (int)this.posZ + dir.getZOffset() * 6
                        )
                     );
                  if (te != null && te instanceof TileBanner) {
                     int face = 0;
                     switch (dir.ordinal()) {
                        case 2:
                           face = 8;
                           break;
                        case 3:
                           face = 0;
                           break;
                        case 4:
                           face = 12;
                           break;
                        case 5:
                           face = 4;
                     }

                     ((TileBanner)te).setBannerFacing((byte)face);
                     PacketHandler.INSTANCE
                        .sendToAllAround(
                           new PacketFXBlockArc(
                              new BlockPos(
                                 (int)this.posX - dir.getXOffset() * 6, (int)this.posY, (int)this.posZ + dir.getZOffset() * 6
                              ),
                              this,
                              0.5F + this.rand.nextFloat() * 0.2F,
                              0.0F,
                              0.0F
                           ),
                           new TargetPoint(this.world.provider.getDimension(), this.posX, this.posY, this.posZ, 32.0)
                        );
                     this.playSound(SoundsTC.wandfail, 1.0F, 1.0F);
                  }
               }
            }

            if (this.stagecounter > 20 && this.stagecounter < 150 && this.stage == 0 && this.stagecounter % 13 == 0) {
               int a = (int)this.posX + this.rand.nextInt(5) - this.rand.nextInt(5);
               int b = (int)this.posZ + this.rand.nextInt(5) - this.rand.nextInt(5);
               BlockPos bp = new BlockPos(a, (int)this.posY, b);
               if (a != (int)this.posX && b != (int)this.posZ && this.world.isAirBlock(bp)) {
                  this.world.setEntityState(this, (byte)16);
                  float rr = this.world.rand.nextFloat();
                  int md = rr < 0.05F ? 2 : (rr < 0.2F ? 1 : 0);
                  Block bb = BlocksTC.lootCrateCommon;
                  switch (md) {
                     case 1:
                        bb = BlocksTC.lootCrateUncommon;
                        break;
                     case 2:
                        bb = BlocksTC.lootCrateRare;
                  }

                  this.world.setBlockState(bp, bb.getDefaultState());
                  PacketHandler.INSTANCE
                     .sendToAllAround(
                        new PacketFXBlockArc(new BlockPos(a, (int)this.posY, b), this, 0.5F + this.rand.nextFloat() * 0.2F, 0.0F, 0.0F),
                        new TargetPoint(this.world.provider.getDimension(), this.posX, this.posY, this.posZ, 32.0)
                     );
                  this.playSound(SoundsTC.wandfail, 1.0F, 1.0F);
               }
            }
         }

         if (this.stage < 12) {
            this.heal(1.0F);
         }
      }

      if (this.pulse > 0) {
         this.pulse--;
      }
   }

   int getTiming() {
      List<EntityCultist> l = EntityUtils.getEntitiesInRange(
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
      cultist.setHomePosAndDistance(this.getPosition(), 32);
      this.world.spawnEntity(cultist);
      cultist.spawnExplosionParticle();
      cultist.playSound(SoundsTC.wandfail, 1.0F, 1.0F);
      if (this.stage > 12) {
         this.attackEntityFrom(DamageSource.OUT_OF_WORLD, 5 + this.rand.nextInt(5));
      }
   }

   void spawnBoss() {
      EntityCultistLeader cultist = new EntityCultistLeader(this.world);
      cultist.setPosition(
         this.posX + this.rand.nextFloat() - this.rand.nextFloat(),
         this.posY + 0.25,
         this.posZ + this.rand.nextFloat() - this.rand.nextFloat()
      );
      cultist.onInitialSpawn(this.world.getDifficultyForLocation(new BlockPos(cultist.getPosition())), (IEntityLivingData)null);
      cultist.setHomePosAndDistance(this.getPosition(), 32);
      this.world.spawnEntity(cultist);
      cultist.spawnExplosionParticle();
      cultist.playSound(SoundsTC.wandfail, 1.0F, 1.0F);
   }

   public void onCollideWithPlayer(EntityPlayer p) {
      if (this.getDistanceSq(p) < 3.0 && p.attackEntityFrom(DamageSource.causeIndirectMagicDamage(this, this), 8.0F)) {
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
      EntityUtils.entityDropSpecialItem(this, new ItemStack(ItemsTC.primordialPearl), this.height / 2.0F);
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
         this.world.newExplosion(this, this.posX, this.posY, this.posZ, 2.0F, false, false);
      }

      super.onDeath(cause);
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

   protected void updateAITasks() {
      super.updateAITasks();
      this.bossInfo.setPercent(this.getHealth() / this.getMaxHealth());
   }
}
