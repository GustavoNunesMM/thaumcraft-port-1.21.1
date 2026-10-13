package thaumcraft.common.golems;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.IEntityLivingData;
import net.minecraft.entity.IRangedAttackMob;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIAttackMelee;
import net.minecraft.entity.ai.EntityAIAttackRanged;
import net.minecraft.entity.ai.EntityAIHurtByTarget;
import net.minecraft.entity.ai.EntityAILookIdle;
import net.minecraft.entity.ai.EntityAISwimming;
import net.minecraft.entity.ai.EntityAIWatchClosest;
import net.minecraft.entity.ai.EntityMoveHelper;
import net.minecraft.entity.ai.EntityMoveHelper.Action;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemNameTag;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagInt;
import net.minecraft.nbt.NBTTagLong;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.network.play.server.SPacketAnimation;
import net.minecraft.pathfinding.PathNavigate;
import net.minecraft.pathfinding.PathNavigateClimber;
import net.minecraft.pathfinding.PathNavigateGround;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.oredict.OreDictionary;
import thaumcraft.api.capabilities.ThaumcraftCapabilities;
import thaumcraft.api.golems.EnumGolemTrait;
import thaumcraft.api.golems.IGolemAPI;
import thaumcraft.api.golems.IGolemProperties;
import thaumcraft.api.golems.tasks.Task;
import thaumcraft.api.items.ItemsTC;
import thaumcraft.client.fx.FXDispatcher;
import thaumcraft.common.config.ConfigAspects;
import thaumcraft.common.config.ModConfig;
import thaumcraft.common.entities.construct.EntityOwnedConstruct;
import thaumcraft.common.golems.ai.AIFollowOwner;
import thaumcraft.common.golems.ai.AIGotoBlock;
import thaumcraft.common.golems.ai.AIGotoEntity;
import thaumcraft.common.golems.ai.AIGotoHome;
import thaumcraft.common.golems.ai.AIOwnerHurtByTarget;
import thaumcraft.common.golems.ai.AIOwnerHurtTarget;
import thaumcraft.common.golems.ai.PathNavigateGolemAir;
import thaumcraft.common.golems.ai.PathNavigateGolemGround;
import thaumcraft.common.golems.tasks.TaskHandler;
import thaumcraft.common.lib.SoundsTC;
import thaumcraft.common.lib.utils.Utils;

public class EntityThaumcraftGolem extends EntityOwnedConstruct implements IGolemAPI, IRangedAttackMob {
   int rankXp = 0;
   private static final DataParameter<Integer> PROPS1 = EntityDataManager.createKey(EntityThaumcraftGolem.class, DataSerializers.VARINT);
   private static final DataParameter<Integer> PROPS2 = EntityDataManager.createKey(EntityThaumcraftGolem.class, DataSerializers.VARINT);
   private static final DataParameter<Integer> PROPS3 = EntityDataManager.createKey(EntityThaumcraftGolem.class, DataSerializers.VARINT);
   private static final DataParameter<Byte> CLIMBING = EntityDataManager.createKey(EntityThaumcraftGolem.class, DataSerializers.BYTE);
   public boolean redrawParts = false;
   private boolean firstRun = true;
   protected Task task = null;
   protected int taskID = Integer.MAX_VALUE;
   public static final int XPM = 1000;

   public EntityThaumcraftGolem(World worldIn) {
      super(worldIn);
      this.setSize(0.4F, 0.9F);
      this.experienceValue = 5;
   }

   protected void initEntityAI() {
      this.targetTasks.taskEntries.clear();
      this.tasks.addTask(2, new AIGotoEntity(this));
      this.tasks.addTask(3, new AIGotoBlock(this));
      this.tasks.addTask(4, new AIGotoHome(this));
      this.tasks.addTask(5, new EntityAIWatchClosest(this, EntityPlayer.class, 8.0F));
      this.tasks.addTask(6, new EntityAILookIdle(this));
   }

   @Override
   protected void entityInit() {
      super.entityInit();
      this.getDataManager().register(PROPS1, 0);
      this.getDataManager().register(PROPS2, 0);
      this.getDataManager().register(PROPS3, 0);
      this.getDataManager().register(CLIMBING, (byte)0);
   }

   @Override
   public IGolemProperties getProperties() {
      ByteBuffer bb = ByteBuffer.allocate(8);
      bb.putInt((Integer)this.getDataManager().get(PROPS1));
      bb.putInt((Integer)this.getDataManager().get(PROPS2));
      return GolemProperties.fromLong(bb.getLong(0));
   }

   @Override
   public void setProperties(IGolemProperties prop) {
      ByteBuffer bb = ByteBuffer.allocate(8);
      bb.putLong(prop.toLong());
      ((Buffer)bb).rewind();
      this.getDataManager().set(PROPS1, bb.getInt());
      this.getDataManager().set(PROPS2, bb.getInt());
   }

   @Override
   public byte getGolemColor() {
      byte[] ba = Utils.intToByteArray((Integer)this.getDataManager().get(PROPS3));
      return ba[0];
   }

   public void setGolemColor(byte b) {
      byte[] ba = Utils.intToByteArray((Integer)this.getDataManager().get(PROPS3));
      ba[0] = b;
      this.getDataManager().set(PROPS3, Utils.byteArraytoInt(ba));
   }

   public byte getFlags() {
      byte[] ba = Utils.intToByteArray((Integer)this.getDataManager().get(PROPS3));
      return ba[1];
   }

   public void setFlags(byte b) {
      byte[] ba = Utils.intToByteArray((Integer)this.getDataManager().get(PROPS3));
      ba[1] = b;
      this.getDataManager().set(PROPS3, Utils.byteArraytoInt(ba));
   }

   public float getEyeHeight() {
      return 0.7F;
   }

   protected void applyEntityAttributes() {
      super.applyEntityAttributes();
      this.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.3);
      this.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(10.0);
      this.getAttributeMap().registerAttribute(SharedMonsterAttributes.ATTACK_DAMAGE);
      this.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).setBaseValue(0.0);
      this.getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).setBaseValue(40.0);
   }

   private void updateEntityAttributes() {
      int mh = 10 + this.getProperties().getMaterial().healthMod;
      if (this.getProperties().hasTrait(EnumGolemTrait.FRAGILE)) {
         mh = (int)(mh * 0.75);
      }

      mh += this.getProperties().getRank();
      this.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(mh);
      this.stepHeight = this.getProperties().hasTrait(EnumGolemTrait.WHEELED) ? 0.5F : 0.6F;
      this.setHomePosAndDistance(
         this.getHomePosition() == BlockPos.ORIGIN ? this.getPosition() : this.getHomePosition(),
         this.getProperties().hasTrait(EnumGolemTrait.SCOUT) ? 48 : 32
      );
      this.getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).setBaseValue(this.getProperties().hasTrait(EnumGolemTrait.SCOUT) ? 56.0 : 40.0);
      this.navigator = this.getGolemNavigator();
      if (this.getProperties().hasTrait(EnumGolemTrait.FLYER)) {
         this.moveHelper = new EntityThaumcraftGolem.FlyingMoveControl(this);
      }

      if (this.getProperties().hasTrait(EnumGolemTrait.FIGHTER)) {
         double da = this.getProperties().getMaterial().damage;
         if (this.getProperties().hasTrait(EnumGolemTrait.BRUTAL)) {
            da = Math.max(da * 1.5, da + 1.0);
         }

         da += this.getProperties().getRank() * 0.25;
         this.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).setBaseValue(da);
      } else {
         this.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).setBaseValue(0.0);
      }

      this.createAI();
   }

   private void createAI() {
      this.tasks.taskEntries.clear();
      this.targetTasks.taskEntries.clear();
      if (this.isFollowingOwner()) {
         this.tasks.addTask(4, new AIFollowOwner(this, 1.0, 10.0F, 2.0F));
      } else {
         this.tasks.addTask(3, new AIGotoEntity(this));
         this.tasks.addTask(4, new AIGotoBlock(this));
         this.tasks.addTask(5, new AIGotoHome(this));
      }

      this.tasks.addTask(8, new EntityAIWatchClosest(this, EntityPlayer.class, 8.0F));
      this.tasks.addTask(9, new EntityAILookIdle(this));
      if (this.getProperties().hasTrait(EnumGolemTrait.FIGHTER)) {
         if (this.getNavigator() instanceof PathNavigateGround) {
            this.tasks.addTask(0, new EntityAISwimming(this));
         }

         if (this.getProperties().hasTrait(EnumGolemTrait.RANGED)) {
            EntityAIAttackRanged aa = null;
            if (this.getProperties().getArms().function != null) {
               aa = this.getProperties().getArms().function.getRangedAttackAI(this);
            }

            if (aa != null) {
               this.tasks.addTask(1, aa);
            }
         }

         this.tasks.addTask(2, new EntityAIAttackMelee(this, 1.15, false));
         if (this.isFollowingOwner()) {
            this.targetTasks.addTask(1, new AIOwnerHurtByTarget(this));
            this.targetTasks.addTask(2, new AIOwnerHurtTarget(this));
         }

         this.targetTasks.addTask(3, new EntityAIHurtByTarget(this, false, new Class[0]));
      }
   }

   public boolean isOnLadder() {
      return this.isBesideClimbableBlock();
   }

   public IEntityLivingData onInitialSpawn(DifficultyInstance diff, IEntityLivingData ld) {
      this.setHomePosAndDistance(this.getPosition(), 32);
      this.updateEntityAttributes();
      return ld;
   }

   public int getTotalArmorValue() {
      int armor = this.getProperties().getMaterial().armor;
      if (this.getProperties().hasTrait(EnumGolemTrait.ARMORED)) {
         armor = (int)Math.max(armor * 1.5, (double)(armor + 1));
      }

      if (this.getProperties().hasTrait(EnumGolemTrait.FRAGILE)) {
         armor = (int)(armor * 0.75);
      }

      return armor;
   }

   public void onLivingUpdate() {
      this.updateArmSwingProgress();
      super.onLivingUpdate();
   }

   @Override
   public void onUpdate() {
      super.onUpdate();
      if (this.getProperties().hasTrait(EnumGolemTrait.FLYER)) {
         this.setNoGravity(true);
      }

      if (!this.world.isRemote) {
         if (this.firstRun) {
            this.firstRun = false;
            if (this.hasHome() && !this.getPosition().equals(this.getHomePosition())) {
               this.goHome();
            }
         }

         if (this.task != null && this.task.isSuspended()) {
            this.task = null;
         }

         if (this.getAttackTarget() != null && this.getAttackTarget().isDead) {
            this.setAttackTarget(null);
         }

         if (this.getAttackTarget() != null && this.getProperties().hasTrait(EnumGolemTrait.RANGED) && this.getDistanceSq(this.getAttackTarget()) > 1024.0) {
            this.setAttackTarget(null);
         }

         if (!FMLCommonHandler.instance().getMinecraftServerInstance().isPVPEnabled()
            && this.getAttackTarget() != null
            && this.getAttackTarget() instanceof EntityPlayer) {
            this.setAttackTarget(null);
         }

         if (this.ticksExisted % (this.getProperties().hasTrait(EnumGolemTrait.REPAIR) ? 40 : 100) == 0) {
            this.heal(1.0F);
         }

         if (this.getProperties().hasTrait(EnumGolemTrait.CLIMBER)) {
            this.setBesideClimbableBlock(this.collidedHorizontally);
         }
      } else if (this.ticksExisted < 20 || this.ticksExisted % 20 == 0) {
         this.redrawParts = true;
      }

      if (this.getProperties().getHead().function != null) {
         this.getProperties().getHead().function.onUpdateTick(this);
      }

      if (this.getProperties().getArms().function != null) {
         this.getProperties().getArms().function.onUpdateTick(this);
      }

      if (this.getProperties().getLegs().function != null) {
         this.getProperties().getLegs().function.onUpdateTick(this);
      }

      if (this.getProperties().getAddon().function != null) {
         this.getProperties().getAddon().function.onUpdateTick(this);
      }
   }

   @SideOnly(Side.CLIENT)
   public void handleStatusUpdate(byte par1) {
      if (par1 == 5) {
         FXDispatcher.INSTANCE
            .drawGenericParticles(
               this.posX,
               this.posY + this.height + 0.1,
               this.posZ,
               0.0,
               0.0,
               0.0,
               1.0F,
               1.0F,
               1.0F,
               0.5F,
               false,
               704 + (this.rand.nextBoolean() ? 0 : 3),
               3,
               1,
               6,
               0,
               2.0F,
               0.0F,
               1
            );
      } else if (par1 == 6) {
         FXDispatcher.INSTANCE
            .drawGenericParticles(
               this.posX,
               this.posY + this.height + 0.1,
               this.posZ,
               0.0,
               0.025,
               0.0,
               0.1F,
               1.0F,
               1.0F,
               0.5F,
               false,
               15,
               1,
               1,
               10,
               0,
               2.0F,
               0.0F,
               1
            );
      } else if (par1 == 7) {
         FXDispatcher.INSTANCE
            .drawGenericParticles(
               this.posX,
               this.posY + this.height + 0.1,
               this.posZ,
               0.0,
               0.05,
               0.0,
               1.0F,
               1.0F,
               1.0F,
               0.5F,
               false,
               640,
               10,
               1,
               10,
               0,
               2.0F,
               0.0F,
               1
            );
      } else if (par1 == 8) {
         FXDispatcher.INSTANCE
            .drawGenericParticles(
               this.posX,
               this.posY + this.height + 0.1,
               this.posZ,
               0.0,
               0.01,
               0.0,
               1.0F,
               1.0F,
               0.1F,
               0.5F,
               false,
               14,
               1,
               1,
               20,
               0,
               2.0F,
               0.0F,
               1
            );
      } else if (par1 == 9) {
         for (int a = 0; a < 5; a++) {
            FXDispatcher.INSTANCE
               .drawGenericParticles(
                  this.posX,
                  this.posY + this.height,
                  this.posZ,
                  this.rand.nextGaussian() * 0.01F,
                  this.rand.nextFloat() * 0.02,
                  this.rand.nextGaussian() * 0.01F,
                  1.0F,
                  1.0F,
                  1.0F,
                  0.5F,
                  false,
                  13,
                  1,
                  1,
                  20 + this.rand.nextInt(20),
                  0,
                  0.3F + this.rand.nextFloat() * 0.4F,
                  0.0F,
                  1
               );
         }
      } else {
         super.handleStatusUpdate(par1);
      }
   }

   public float getGolemMoveSpeed() {
      return 1.0F
         + this.getProperties().getRank() * 0.025F
         + (this.getProperties().hasTrait(EnumGolemTrait.LIGHT) ? 0.2F : 0.0F)
         + (this.getProperties().hasTrait(EnumGolemTrait.HEAVY) ? -0.175F : 0.0F)
         + (this.getProperties().hasTrait(EnumGolemTrait.FLYER) ? -0.33F : 0.0F)
         + (this.getProperties().hasTrait(EnumGolemTrait.WHEELED) ? 0.25F : 0.0F);
   }

   public PathNavigate getGolemNavigator() {
      return (PathNavigate)(this.getProperties().hasTrait(EnumGolemTrait.FLYER)
         ? new PathNavigateGolemAir(this, this.world)
         : (
            this.getProperties().hasTrait(EnumGolemTrait.CLIMBER)
               ? new PathNavigateClimber(this, this.world)
               : new PathNavigateGolemGround(this, this.world)
         ));
   }

   protected boolean canTriggerWalking() {
      return this.getProperties().hasTrait(EnumGolemTrait.HEAVY) && !this.getProperties().hasTrait(EnumGolemTrait.FLYER);
   }

   public void fall(float distance, float damageMultiplier) {
      if (!this.getProperties().hasTrait(EnumGolemTrait.FLYER) && !this.getProperties().hasTrait(EnumGolemTrait.CLIMBER)) {
         super.fall(distance, damageMultiplier);
      }
   }

   private void goHome() {
      double d0 = this.posX;
      double d1 = this.posY;
      double d2 = this.posZ;
      this.posX = this.getHomePosition().getX() + 0.5;
      this.posY = this.getHomePosition().getY();
      this.posZ = this.getHomePosition().getZ() + 0.5;
      boolean flag = false;
      BlockPos blockpos = new BlockPos(this);
      boolean flag1 = false;

      while (!flag1 && blockpos.getY() < this.world.getActualHeight()) {
         BlockPos blockpos1 = blockpos.up();
         IBlockState iblockstate = this.world.getBlockState(blockpos1);
         if (iblockstate.getMaterial().blocksMovement()) {
            flag1 = true;
         } else {
            this.posY++;
            blockpos = blockpos1;
         }
      }

      if (flag1) {
         this.setPositionAndUpdate(this.posX, this.posY, this.posZ);
         if (this.world.getCollisionBoxes(this, this.getEntityBoundingBox()).isEmpty()) {
            flag = true;
         }
      }

      if (!flag) {
         this.setPositionAndUpdate(d0, d1, d2);
      } else if (this instanceof EntityCreature) {
         this.getNavigator().clearPath();
      }
   }

   @Override
   public void readEntityFromNBT(NBTTagCompound nbt) {
      super.readEntityFromNBT(nbt);
      this.setProperties(GolemProperties.fromLong(nbt.getLong("props")));
      this.setHomePosAndDistance(BlockPos.fromLong(nbt.getLong("homepos")), 32);
      this.setFlags(Byte.valueOf(nbt.getByte("gflags")));
      this.rankXp = nbt.getInteger("rankXP");
      this.setGolemColor(nbt.getByte("color"));
      this.updateEntityAttributes();
   }

   @Override
   public void writeEntityToNBT(NBTTagCompound nbt) {
      super.writeEntityToNBT(nbt);
      nbt.setLong("props", this.getProperties().toLong());
      nbt.setLong("homepos", this.getHomePosition().toLong());
      nbt.setByte("gflags", this.getFlags());
      nbt.setInteger("rankXP", this.rankXp);
      nbt.setByte("color", this.getGolemColor());
   }

   protected void damageEntity(DamageSource ds, float damage) {
      if (!ds.isFireDamage() || !this.getProperties().hasTrait(EnumGolemTrait.FIREPROOF)) {
         if (ds.isExplosion() && this.getProperties().hasTrait(EnumGolemTrait.BLASTPROOF)) {
            damage = Math.min(this.getMaxHealth() / 2.0F, damage * 0.3F);
         }

         if (ds != DamageSource.CACTUS) {
            if (this.hasHome() && (ds == DamageSource.IN_WALL || ds == DamageSource.OUT_OF_WORLD)) {
               this.goHome();
            }

            super.damageEntity(ds, damage);
         }
      }
   }

   @Override
   protected boolean processInteract(EntityPlayer player, EnumHand hand) {
      if (this.isDead) {
         return false;
      } else if (player.getHeldItem(hand).getItem() instanceof ItemNameTag) {
         return false;
      } else if (!this.world.isRemote && this.isOwner(player) && !this.isDead) {
         if (player.isSneaking()) {
            this.playSound(SoundsTC.zap, 1.0F, 1.0F);
            if (this.task != null) {
               this.task.setReserved(false);
            }

            this.dropCarried();
            ItemStack placer = new ItemStack(ItemsTC.golemPlacer);
            placer.setTagInfo("props", new NBTTagLong(this.getProperties().toLong()));
            placer.setTagInfo("xp", new NBTTagInt(this.rankXp));
            this.entityDropItem(placer, 0.5F);
            this.setDead();
            player.swingArm(hand);
         } else if (player.getHeldItem(hand).getItem() instanceof ItemGolemBell
            && ThaumcraftCapabilities.getKnowledge(player).isResearchKnown("GOLEMDIRECT")) {
            if (this.task != null) {
               this.task.setReserved(false);
            }

            this.playSound(SoundsTC.scan, 1.0F, 1.0F);
            this.setFollowingOwner(!this.isFollowingOwner());
            if (this.isFollowingOwner()) {
               player.sendStatusMessage(new TextComponentTranslation("golem.follow", new Object[]{""}), true);
               if (ModConfig.CONFIG_GRAPHICS.showGolemEmotes) {
                  this.world.setEntityState(this, (byte)5);
               }

               this.detachHome();
            } else {
               player.sendStatusMessage(new TextComponentTranslation("golem.stay", new Object[]{""}), true);
               if (ModConfig.CONFIG_GRAPHICS.showGolemEmotes) {
                  this.world.setEntityState(this, (byte)8);
               }

               this.setHomePosAndDistance(this.getPosition(), this.getProperties().hasTrait(EnumGolemTrait.SCOUT) ? 48 : 32);
            }

            this.updateEntityAttributes();
            player.swingArm(hand);
         } else if (!player.getHeldItem(hand).isEmpty()) {
            int[] ids = OreDictionary.getOreIDs(player.getHeldItem(hand));
            if (ids != null && ids.length > 0) {
               for (int id : ids) {
                  String s = OreDictionary.getOreName(id);
                  if (s.startsWith("dye")) {
                     for (int a = 0; a < ConfigAspects.dyes.length; a++) {
                        if (s.equals(ConfigAspects.dyes[a])) {
                           this.playSound(SoundsTC.zap, 1.0F, 1.0F);
                           this.setGolemColor((byte)(16 - a));
                           player.getHeldItem(hand).shrink(1);
                           player.swingArm(hand);
                           return true;
                        }
                     }
                  }
               }
            }
         }

         return true;
      } else {
         return super.processInteract(player, hand);
      }
   }

   @Override
   public void onDeath(DamageSource cause) {
      if (this.task != null) {
         this.task.setReserved(false);
      }

      super.onDeath(cause);
      if (!this.world.isRemote) {
         this.dropCarried();
      }
   }

   protected void dropCarried() {
      for (ItemStack s : this.getCarrying()) {
         if (s != null && !s.isEmpty()) {
            this.entityDropItem(s, 0.25F);
         }
      }
   }

   protected void dropFewItems(boolean wasRecentlyHit, int lootingModifier) {
      float b = lootingModifier * 0.15F;

      for (ItemStack stack : this.getProperties().generateComponents()) {
         ItemStack s = stack.copy();
         if (this.rand.nextFloat() < 0.3F + b) {
            if (s.getCount() > 0) {
               s.shrink(this.rand.nextInt(s.getCount()));
            }

            this.entityDropItem(s, 0.25F);
         }
      }
   }

   public boolean isBesideClimbableBlock() {
      return ((Byte)this.dataManager.get(CLIMBING) & 1) != 0;
   }

   public void setBesideClimbableBlock(boolean climbing) {
      byte b0 = (Byte)this.dataManager.get(CLIMBING);
      if (climbing) {
         b0 = (byte)(b0 | 1);
      } else {
         b0 = (byte)(b0 & -2);
      }

      this.dataManager.set(CLIMBING, b0);
   }

   public boolean isFollowingOwner() {
      return Utils.getBit(this.getFlags(), 1);
   }

   public void setFollowingOwner(boolean par1) {
      byte var2 = this.getFlags();
      if (par1) {
         this.setFlags(Byte.valueOf((byte)Utils.setBit(var2, 1)));
      } else {
         this.setFlags(Byte.valueOf((byte)Utils.clearBit(var2, 1)));
      }
   }

   public void setAttackTarget(EntityLivingBase entitylivingbaseIn) {
      super.setAttackTarget(entitylivingbaseIn);
      this.setInCombat(this.getAttackTarget() != null);
   }

   @Override
   public boolean isInCombat() {
      return Utils.getBit(this.getFlags(), 3);
   }

   public void setInCombat(boolean par1) {
      byte var2 = this.getFlags();
      if (par1) {
         this.setFlags(Byte.valueOf((byte)Utils.setBit(var2, 3)));
      } else {
         this.setFlags(Byte.valueOf((byte)Utils.clearBit(var2, 3)));
      }
   }

   public boolean attackEntityAsMob(Entity ent) {
      float dmg = (float)this.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).getAttributeValue();
      int kb = 0;
      if (ent instanceof EntityLivingBase) {
         dmg += EnchantmentHelper.getModifierForCreature(this.getHeldItemMainhand(), ((EntityLivingBase)ent).getCreatureAttribute());
         kb += EnchantmentHelper.getKnockbackModifier(this);
      }

      boolean flag = ent.attackEntityFrom(DamageSource.causeMobDamage(this), dmg);
      if (flag) {
         if (ent instanceof EntityLivingBase && this.getProperties().hasTrait(EnumGolemTrait.DEFT)) {
            ((EntityLivingBase)ent).recentlyHit = 100;
         }

         if (kb > 0) {
            ent.addVelocity(
               -MathHelper.sin(this.rotationYaw * (float) Math.PI / 180.0F) * kb * 0.5F,
               0.1,
               MathHelper.cos(this.rotationYaw * (float) Math.PI / 180.0F) * kb * 0.5F
            );
            this.motionX *= 0.6;
            this.motionZ *= 0.6;
         }

         int j = EnchantmentHelper.getFireAspectModifier(this);
         if (j > 0) {
            ent.setFire(j * 4);
         }

         this.applyEnchantments(this, ent);
         if (this.getProperties().getArms().function != null) {
            this.getProperties().getArms().function.onMeleeAttack(this, ent);
         }

         if (ent instanceof EntityLiving && !((EntityLiving)ent).isEntityAlive()) {
            this.addRankXp(8);
         }
      }

      return flag;
   }

   public Task getTask() {
      if (this.task == null && this.taskID != Integer.MAX_VALUE) {
         this.task = TaskHandler.getTask(this.world.provider.getDimension(), this.taskID);
         this.taskID = Integer.MAX_VALUE;
      }

      return this.task;
   }

   public void setTask(Task task) {
      this.task = task;
   }

   @Override
   public void addRankXp(int xp) {
      if (this.getProperties().hasTrait(EnumGolemTrait.SMART) && !this.world.isRemote) {
         int rank = this.getProperties().getRank();
         if (rank < 10) {
            this.rankXp += xp;
            int xn = (rank + 1) * (rank + 1) * 1000;
            if (this.rankXp >= xn) {
               this.rankXp -= xn;
               IGolemProperties props = this.getProperties();
               props.setRank(rank + 1);
               this.setProperties(props);
               if (ModConfig.CONFIG_GRAPHICS.showGolemEmotes) {
                  this.world.setEntityState(this, (byte)9);
                  this.playSound(SoundEvents.ENTITY_PLAYER_LEVELUP, 0.25F, 1.0F);
               }
            }
         }
      }
   }

   @Override
   public ItemStack holdItem(ItemStack stack) {
      if (stack != null && !stack.isEmpty() && stack.getCount() > 0) {
         for (int a = 0; a < (this.getProperties().hasTrait(EnumGolemTrait.HAULER) ? 2 : 1); a++) {
            if (this.getItemStackFromSlot(EntityEquipmentSlot.values()[a]) == null || this.getItemStackFromSlot(EntityEquipmentSlot.values()[a]).isEmpty()) {
               this.setItemStackToSlot(EntityEquipmentSlot.values()[a], stack);
               return ItemStack.EMPTY;
            }

            if (this.getItemStackFromSlot(EntityEquipmentSlot.values()[a]).getCount() < this.getItemStackFromSlot(EntityEquipmentSlot.values()[a]).getMaxStackSize()
               && ItemStack.areItemsEqual(this.getItemStackFromSlot(EntityEquipmentSlot.values()[a]), stack)
               && ItemStack.areItemStackTagsEqual(this.getItemStackFromSlot(EntityEquipmentSlot.values()[a]), stack)) {
               int d = Math.min(
                  stack.getCount(),
                  this.getItemStackFromSlot(EntityEquipmentSlot.values()[a]).getMaxStackSize() - this.getItemStackFromSlot(EntityEquipmentSlot.values()[a]).getCount()
               );
               stack.shrink(d);
               this.getItemStackFromSlot(EntityEquipmentSlot.values()[a]).grow(d);
               if (stack.getCount() <= 0) {
                  stack = ItemStack.EMPTY;
               }
            }
         }

         return stack;
      } else {
         return stack;
      }
   }

   @Override
   public ItemStack dropItem(ItemStack stack) {
      ItemStack out = ItemStack.EMPTY;

      for (int a = 0; a < (this.getProperties().hasTrait(EnumGolemTrait.HAULER) ? 2 : 1); a++) {
         if (this.getItemStackFromSlot(EntityEquipmentSlot.values()[a]) != null && !this.getItemStackFromSlot(EntityEquipmentSlot.values()[a]).isEmpty()) {
            if (stack != null && !stack.isEmpty()) {
               if (ItemStack.areItemsEqual(this.getItemStackFromSlot(EntityEquipmentSlot.values()[a]), stack)
                  && ItemStack.areItemStackTagsEqual(this.getItemStackFromSlot(EntityEquipmentSlot.values()[a]), stack)) {
                  out = this.getItemStackFromSlot(EntityEquipmentSlot.values()[a]).copy();
                  out.setCount(Math.min(stack.getCount(), out.getCount()));
                  this.getItemStackFromSlot(EntityEquipmentSlot.values()[a]).shrink(stack.getCount());
                  if (this.getItemStackFromSlot(EntityEquipmentSlot.values()[a]).getCount() <= 0) {
                     this.setItemStackToSlot(EntityEquipmentSlot.values()[a], ItemStack.EMPTY);
                  }
               }
            } else {
               out = this.getItemStackFromSlot(EntityEquipmentSlot.values()[a]).copy();
               this.setItemStackToSlot(EntityEquipmentSlot.values()[a], ItemStack.EMPTY);
            }

            if (out != null && !out.isEmpty()) {
               break;
            }
         }
      }

      if (this.getProperties().hasTrait(EnumGolemTrait.HAULER)
         && (this.getItemStackFromSlot(EntityEquipmentSlot.values()[0]) == null || this.getItemStackFromSlot(EntityEquipmentSlot.values()[0]).isEmpty())
         && this.getItemStackFromSlot(EntityEquipmentSlot.values()[1]) != null
         && !this.getItemStackFromSlot(EntityEquipmentSlot.values()[1]).isEmpty()) {
         this.setItemStackToSlot(EntityEquipmentSlot.values()[0], this.getItemStackFromSlot(EntityEquipmentSlot.values()[1]).copy());
         this.setItemStackToSlot(EntityEquipmentSlot.values()[1], ItemStack.EMPTY);
      }

      return out;
   }

   @Override
   public int canCarryAmount(ItemStack stack) {
      int ss = 0;

      for (int a = 0; a < (this.getProperties().hasTrait(EnumGolemTrait.HAULER) ? 2 : 1); a++) {
         if (this.getItemStackFromSlot(EntityEquipmentSlot.values()[a]) == null || this.getItemStackFromSlot(EntityEquipmentSlot.values()[a]).isEmpty()) {
            ss += this.getItemStackFromSlot(EntityEquipmentSlot.values()[a]).getMaxStackSize();
         }

         if (ItemStack.areItemsEqual(this.getItemStackFromSlot(EntityEquipmentSlot.values()[a]), stack)
            && ItemStack.areItemStackTagsEqual(this.getItemStackFromSlot(EntityEquipmentSlot.values()[a]), stack)) {
            ss += this.getItemStackFromSlot(EntityEquipmentSlot.values()[a]).getMaxStackSize() - this.getItemStackFromSlot(EntityEquipmentSlot.values()[a]).getCount();
         }
      }

      return ss;
   }

   @Override
   public boolean canCarry(ItemStack stack, boolean partial) {
      int ca = this.canCarryAmount(stack);
      return ca > 0 && (partial || ca >= stack.getCount());
   }

   @Override
   public boolean isCarrying(ItemStack stack) {
      if (stack != null && !stack.isEmpty()) {
         for (int a = 0; a < (this.getProperties().hasTrait(EnumGolemTrait.HAULER) ? 2 : 1); a++) {
            if (this.getItemStackFromSlot(EntityEquipmentSlot.values()[a]) != null
               && !this.getItemStackFromSlot(EntityEquipmentSlot.values()[a]).isEmpty()
               && this.getItemStackFromSlot(EntityEquipmentSlot.values()[a]).getCount() > 0
               && ItemStack.areItemsEqual(this.getItemStackFromSlot(EntityEquipmentSlot.values()[a]), stack)
               && ItemStack.areItemStackTagsEqual(this.getItemStackFromSlot(EntityEquipmentSlot.values()[a]), stack)) {
               return true;
            }
         }

         return false;
      } else {
         return false;
      }
   }

   @Override
   public NonNullList<ItemStack> getCarrying() {
      if (this.getProperties().hasTrait(EnumGolemTrait.HAULER)) {
         NonNullList<ItemStack> stacks = NonNullList.withSize(2, ItemStack.EMPTY);
         stacks.set(0, this.getItemStackFromSlot(EntityEquipmentSlot.values()[0]));
         stacks.set(1, this.getItemStackFromSlot(EntityEquipmentSlot.values()[1]));
         return stacks;
      } else {
         return NonNullList.withSize(1, this.getItemStackFromSlot(EntityEquipmentSlot.values()[0]));
      }
   }

   @Override
   public EntityLivingBase getGolemEntity() {
      return this;
   }

   @Override
   public World getGolemWorld() {
      return this.getEntityWorld();
   }

   @Override
   public void swingArm() {
      if (!this.isSwingInProgress || this.swingProgressInt >= 3 || this.swingProgressInt < 0) {
         this.swingProgressInt = -1;
         this.isSwingInProgress = true;
         if (this.world instanceof WorldServer) {
            ((WorldServer)this.world).getEntityTracker().sendToTrackingAndSelf(this, new SPacketAnimation(this, 0));
         }
      }
   }

   public void attackEntityWithRangedAttack(EntityLivingBase target, float range) {
      if (this.getProperties().getArms().function != null) {
         this.getProperties().getArms().function.onRangedAttack(this, target, range);
      }
   }

   public void setSwingingArms(boolean swingingArms) {
   }

   class FlyingMoveControl extends EntityMoveHelper {
      public FlyingMoveControl(EntityThaumcraftGolem vex) {
         super(vex);
      }

      public void onUpdateMoveHelper() {
         if (this.action == Action.MOVE_TO) {
            double d0 = this.posX - EntityThaumcraftGolem.this.posX;
            double d1 = this.posY - EntityThaumcraftGolem.this.posY;
            double d2 = this.posZ - EntityThaumcraftGolem.this.posZ;
            double d3 = d0 * d0 + d1 * d1 + d2 * d2;
            d3 = MathHelper.sqrt(d3);
            if (d3 < EntityThaumcraftGolem.this.getEntityBoundingBox().getAverageEdgeLength()) {
               this.action = Action.WAIT;
               EntityThaumcraftGolem.this.motionX *= 0.5;
               EntityThaumcraftGolem.this.motionY *= 0.5;
               EntityThaumcraftGolem.this.motionZ *= 0.5;
            } else {
               EntityThaumcraftGolem.this.motionX = EntityThaumcraftGolem.this.motionX + d0 / d3 * 0.033 * this.speed;
               EntityThaumcraftGolem.this.motionY = EntityThaumcraftGolem.this.motionY + d1 / d3 * 0.0125 * this.speed;
               EntityThaumcraftGolem.this.motionZ = EntityThaumcraftGolem.this.motionZ + d2 / d3 * 0.033 * this.speed;
               if (EntityThaumcraftGolem.this.getAttackTarget() == null) {
                  EntityThaumcraftGolem.this.rotationYaw = -(
                        (float)MathHelper.atan2(EntityThaumcraftGolem.this.motionX, EntityThaumcraftGolem.this.motionZ)
                     )
                     * (180.0F / (float)Math.PI);
                  EntityThaumcraftGolem.this.renderYawOffset = EntityThaumcraftGolem.this.rotationYaw;
               } else {
                  double d4 = EntityThaumcraftGolem.this.getAttackTarget().posX - EntityThaumcraftGolem.this.posX;
                  double d5 = EntityThaumcraftGolem.this.getAttackTarget().posZ - EntityThaumcraftGolem.this.posZ;
                  EntityThaumcraftGolem.this.rotationYaw = -((float)MathHelper.atan2(d4, d5)) * (180.0F / (float)Math.PI);
                  EntityThaumcraftGolem.this.renderYawOffset = EntityThaumcraftGolem.this.rotationYaw;
               }
            }
         }
      }
   }
}
