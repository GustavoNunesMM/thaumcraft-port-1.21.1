package thaumcraft.common.entities.monster;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.IEntityLivingData;
import net.minecraft.entity.IRangedAttackMob;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIAttackMelee;
import net.minecraft.entity.ai.EntityAIAttackRanged;
import net.minecraft.entity.ai.EntityAIAvoidEntity;
import net.minecraft.entity.ai.EntityAIHurtByTarget;
import net.minecraft.entity.ai.EntityAILookIdle;
import net.minecraft.entity.ai.EntityAIMoveThroughVillage;
import net.minecraft.entity.ai.EntityAIMoveTowardsRestriction;
import net.minecraft.entity.ai.EntityAIOpenDoor;
import net.minecraft.entity.ai.EntityAISwimming;
import net.minecraft.entity.ai.EntityAIWander;
import net.minecraft.entity.ai.EntityAIWatchClosest;
import net.minecraft.entity.ai.EntityAIWatchClosest2;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityTippedArrow;
import net.minecraft.init.Enchantments;
import net.minecraft.init.Items;
import net.minecraft.init.MobEffects;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.inventory.ItemStackHelper;
import net.minecraft.item.Item;
import net.minecraft.item.ItemNameTag;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.pathfinding.PathNavigateGround;
import net.minecraft.potion.PotionEffect;
import net.minecraft.potion.PotionUtils;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.storage.loot.LootTableList;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.common.BiomeDictionary.Type;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import thaumcraft.Thaumcraft;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.casters.FocusEngine;
import thaumcraft.api.casters.FocusMediumRoot;
import thaumcraft.api.casters.FocusPackage;
import thaumcraft.api.casters.IFocusElement;
import thaumcraft.api.items.ItemsTC;
import thaumcraft.common.config.ModConfig;
import thaumcraft.common.entities.ai.pech.AINearestAttackableTargetPech;
import thaumcraft.common.entities.ai.pech.AIPechItemEntityGoto;
import thaumcraft.common.entities.ai.pech.AIPechTradePlayer;
import thaumcraft.common.items.casters.foci.FocusEffectAir;
import thaumcraft.common.items.casters.foci.FocusEffectCurse;
import thaumcraft.common.items.casters.foci.FocusEffectEarth;
import thaumcraft.common.items.casters.foci.FocusEffectFire;
import thaumcraft.common.items.casters.foci.FocusEffectFlux;
import thaumcraft.common.items.casters.foci.FocusMediumProjectile;
import thaumcraft.common.lib.SoundsTC;
import thaumcraft.common.lib.crafting.ThaumcraftCraftingManager;
import thaumcraft.common.lib.utils.InventoryUtils;
import thaumcraft.common.world.biomes.BiomeHandler;

public class EntityPech extends EntityMob implements IRangedAttackMob {
   public NonNullList<ItemStack> loot = NonNullList.withSize(9, ItemStack.EMPTY);
   public boolean trading = false;
   private final EntityAIAttackRanged aiArrowAttack = new EntityAIAttackRanged(this, 0.6, 20, 50, 15.0F);
   private final EntityAIAttackRanged aiBlastAttack = new EntityAIAttackRanged(this, 0.6, 20, 50, 15.0F);
   private final EntityAIAttackMelee aiMeleeAttack = new EntityAIAttackMelee(this, 0.6, false);
   private final EntityAIAvoidEntity aiAvoidPlayer = new EntityAIAvoidEntity(this, EntityPlayer.class, 8.0F, 0.5, 0.6);
   private static final DataParameter<Byte> TYPE = EntityDataManager.createKey(EntityPech.class, DataSerializers.BYTE);
   private static final DataParameter<Integer> ANGER = EntityDataManager.createKey(EntityPech.class, DataSerializers.VARINT);
   private static final DataParameter<Boolean> TAMED = EntityDataManager.createKey(EntityPech.class, DataSerializers.BOOLEAN);
   public static final ResourceLocation LOOT = LootTableList.register(new ResourceLocation("thaumcraft", "pech"));
   public float mumble = 0.0F;
   int chargecount = 0;
   public static HashMap<Integer, Integer> valuedItems = new HashMap<>();
   public static HashMap<Integer, ArrayList<List>> tradeInventory = new HashMap<>();

   public EntityPech(World world) {
      super(world);
      this.setSize(0.6F, 1.8F);
      this.experienceValue = 8;
   }

   public String getName() {
      if (this.hasCustomName()) {
         return this.getCustomNameTag();
      } else {
         switch (this.getPechType()) {
            case 0:
               return I18n.translateToLocal("entity.Pech.name");
            case 1:
               return I18n.translateToLocal("entity.Pech.1.name");
            case 2:
               return I18n.translateToLocal("entity.Pech.2.name");
            default:
               return I18n.translateToLocal("entity.Pech.name");
         }
      }
   }

   protected void initEntityAI() {
      ((PathNavigateGround)this.getNavigator()).setCanSwim(false);
      this.setDropChance(EntityEquipmentSlot.MAINHAND, 0.2F);
      this.setDropChance(EntityEquipmentSlot.OFFHAND, 0.2F);
      this.tasks.addTask(0, new EntityAISwimming(this));
      this.tasks.addTask(1, new AIPechTradePlayer(this));
      this.tasks.addTask(3, new AIPechItemEntityGoto(this));
      this.tasks.addTask(5, new EntityAIOpenDoor(this, true));
      this.tasks.addTask(6, new EntityAIMoveTowardsRestriction(this, 0.5));
      this.tasks.addTask(6, new EntityAIMoveThroughVillage(this, 1.0, false));
      this.tasks.addTask(9, new EntityAIWander(this, 0.6));
      this.tasks.addTask(9, new EntityAIWatchClosest2(this, EntityPlayer.class, 3.0F, 1.0F));
      this.tasks.addTask(10, new EntityAIWatchClosest(this, EntityLiving.class, 8.0F));
      this.tasks.addTask(11, new EntityAILookIdle(this));
      this.targetTasks.addTask(1, new EntityAIHurtByTarget(this, false, new Class[0]));
      this.targetTasks.addTask(2, new AINearestAttackableTargetPech(this, EntityPlayer.class, true));
   }

   public void setCombatTask() {
      if (this.world != null && !this.world.isRemote) {
         this.tasks.removeTask(this.aiMeleeAttack);
         this.tasks.removeTask(this.aiArrowAttack);
         this.tasks.removeTask(this.aiBlastAttack);
         ItemStack itemstack = this.getHeldItemMainhand();
         if (itemstack.getItem() == Items.BOW) {
            this.tasks.addTask(2, this.aiArrowAttack);
         } else if (itemstack.getItem() == ItemsTC.pechWand) {
            this.tasks.addTask(2, this.aiBlastAttack);
         } else {
            this.tasks.addTask(2, this.aiMeleeAttack);
         }

         if (this.isTamed()) {
            this.tasks.removeTask(this.aiAvoidPlayer);
         } else {
            this.tasks.addTask(4, this.aiAvoidPlayer);
         }
      }
   }

   public void attackEntityWithRangedAttack(EntityLivingBase target, float f) {
      if (this.getPechType() == 2) {
         EntityTippedArrow entityarrow = new EntityTippedArrow(this.world, this);
         if (this.world.rand.nextFloat() < 0.2) {
            ItemStack itemstack = new ItemStack(Items.TIPPED_ARROW);
            PotionUtils.appendEffects(itemstack, Collections.singletonList(new PotionEffect(MobEffects.POISON, 40)));
            entityarrow.setPotionEffect(itemstack);
         }

         double d0 = target.posX - this.posX;
         double d1 = target.getEntityBoundingBox().minY + target.height / 3.0F - entityarrow.posY;
         double d2 = target.posZ - this.posZ;
         double d3 = MathHelper.sqrt(d0 * d0 + d2 * d2);
         entityarrow.shoot(d0, d1 + d3 * 0.2F, d2, 1.6F, 14 - this.world.getDifficulty().getId() * 4);
         int i = EnchantmentHelper.getMaxEnchantmentLevel(Enchantments.POWER, this);
         int j = EnchantmentHelper.getMaxEnchantmentLevel(Enchantments.PUNCH, this);
         entityarrow.setDamage(f * 2.0F + this.rand.nextGaussian() * 0.25 + this.world.getDifficulty().getId() * 0.11F);
         if (i > 0) {
            entityarrow.setDamage(entityarrow.getDamage() + i * 0.5 + 0.5);
         }

         if (j > 0) {
            entityarrow.setKnockbackStrength(j);
         }

         if (EnchantmentHelper.getMaxEnchantmentLevel(Enchantments.FLAME, this) > 0) {
            entityarrow.setFire(100);
         }

         this.playSound(SoundEvents.ENTITY_ARROW_SHOOT, 1.0F, 1.0F / (this.getRNG().nextFloat() * 0.4F + 0.8F));
         this.world.spawnEntity(entityarrow);
      } else if (this.getPechType() == 1) {
         FocusPackage p = new FocusPackage(this);
         FocusMediumRoot root = new FocusMediumRoot();
         double off = this.getDistance(target) / 6.0F;
         root.setupFromCasterToTarget(this, target, off);
         p.addNode(root);
         FocusMediumProjectile fp = new FocusMediumProjectile();
         fp.initialize();
         fp.getSetting("speed").setValue(2);
         p.addNode(fp);
         p.addNode(
            (IFocusElement)(this.rand.nextBoolean()
               ? new FocusEffectCurse()
               : (
                  this.rand.nextBoolean()
                     ? new FocusEffectFlux()
                     : (
                        this.rand.nextBoolean()
                           ? new FocusEffectEarth()
                           : (this.rand.nextBoolean() ? new FocusEffectAir() : new FocusEffectFire())
                     )
               ))
         );
         FocusEngine.castFocusPackage(this, p, true);
         this.swingArm(this.getActiveHand());
      }
   }

   public void setItemStackToSlot(EntityEquipmentSlot slotIn, @Nullable ItemStack stack) {
      super.setItemStackToSlot(slotIn, stack);
      if (!this.world.isRemote && slotIn == EntityEquipmentSlot.MAINHAND) {
         this.setCombatTask();
      }
   }

   protected void setEquipmentBasedOnDifficulty(DifficultyInstance difficulty) {
      super.setEquipmentBasedOnDifficulty(difficulty);
      switch (this.rand.nextInt(20)) {
         case 0:
         case 12:
            this.setHeldItem(this.getActiveHand(), new ItemStack(ItemsTC.pechWand));
            break;
         case 1:
            this.setHeldItem(this.getActiveHand(), new ItemStack(Items.STONE_SWORD));
            break;
         case 2:
         case 4:
         case 10:
         case 11:
         case 13:
            this.setHeldItem(this.getActiveHand(), new ItemStack(Items.BOW));
            break;
         case 3:
            this.setHeldItem(this.getActiveHand(), new ItemStack(Items.STONE_AXE));
            break;
         case 5:
            this.setHeldItem(this.getActiveHand(), new ItemStack(Items.IRON_SWORD));
            break;
         case 6:
            this.setHeldItem(this.getActiveHand(), new ItemStack(Items.IRON_AXE));
            break;
         case 7:
            this.setHeldItem(this.getActiveHand(), new ItemStack(Items.FISHING_ROD));
            break;
         case 8:
            this.setHeldItem(this.getActiveHand(), new ItemStack(Items.STONE_PICKAXE));
            break;
         case 9:
            this.setHeldItem(this.getActiveHand(), new ItemStack(Items.IRON_PICKAXE));
      }
   }

   public IEntityLivingData onInitialSpawn(DifficultyInstance diff, IEntityLivingData data) {
      this.setEquipmentBasedOnDifficulty(diff);
      ItemStack itemstack = this.getHeldItem(this.getActiveHand());
      if (itemstack.getItem() == ItemsTC.pechWand) {
         this.setPechType(1);
         this.setDropChance(this.getActiveHand() == EnumHand.MAIN_HAND ? EntityEquipmentSlot.MAINHAND : EntityEquipmentSlot.OFFHAND, 0.1F);
      } else if (!itemstack.isEmpty()) {
         if (itemstack.getItem() == Items.BOW) {
            this.setPechType(2);
         }

         this.setEnchantmentBasedOnDifficulty(diff);
      }

      float f = diff.getClampedAdditionalDifficulty();
      this.setCanPickUpLoot(this.rand.nextFloat() < 0.75F * f);
      this.setCombatTask();
      return super.onInitialSpawn(diff, data);
   }

   public boolean getCanSpawnHere() {
      Biome biome = this.world.getBiome(new BlockPos(this));
      boolean magicBiome = false;
      if (biome != null) {
         magicBiome = BiomeDictionary.hasType(biome, Type.MAGICAL);
      }

      int count = 0;

      try {
         List l = this.world.getEntitiesWithinAABB(EntityPech.class, this.getEntityBoundingBox().grow(16.0, 16.0, 16.0));
         if (l != null) {
            count = l.size();
         }
      } catch (Exception var5) {
      }

      if (this.world.provider.getDimension() != ModConfig.CONFIG_WORLD.overworldDim
         && biome != BiomeHandler.MAGICAL_FOREST
         && biome != BiomeHandler.EERIE) {
         magicBiome = false;
      }

      return count < 4 && magicBiome && super.getCanSpawnHere();
   }

   public float getEyeHeight() {
      return this.height * 0.66F;
   }

   public void onLivingUpdate() {
      super.onLivingUpdate();
   }

   protected void entityInit() {
      super.entityInit();
      this.getDataManager().register(TYPE, (byte)0);
      this.getDataManager().register(ANGER, 0);
      this.getDataManager().register(TAMED, false);
   }

   public int getPechType() {
      return (Byte)this.getDataManager().get(TYPE);
   }

   public int getAnger() {
      return (Integer)this.getDataManager().get(ANGER);
   }

   public boolean isTamed() {
      return (Boolean)this.getDataManager().get(TAMED);
   }

   public void setPechType(int par1) {
      this.getDataManager().set(TYPE, (byte)par1);
   }

   public void setAnger(int par1) {
      this.getDataManager().set(ANGER, par1);
   }

   public void setTamed(boolean par1) {
      this.getDataManager().set(TAMED, par1);
   }

   protected void applyEntityAttributes() {
      super.applyEntityAttributes();
      this.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(30.0);
      this.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).setBaseValue(6.0);
      this.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.5);
   }

   public void writeEntityToNBT(NBTTagCompound nbt) {
      super.writeEntityToNBT(nbt);
      nbt.setByte("PechType", (byte)this.getPechType());
      nbt.setShort("Anger", (short)this.getAnger());
      nbt.setBoolean("Tamed", this.isTamed());
      ItemStackHelper.saveAllItems(nbt, this.loot);
   }

   public void readEntityFromNBT(NBTTagCompound nbt) {
      super.readEntityFromNBT(nbt);
      if (nbt.hasKey("PechType")) {
         byte b0 = nbt.getByte("PechType");
         this.setPechType(b0);
      }

      this.setAnger(nbt.getShort("Anger"));
      this.setTamed(nbt.getBoolean("Tamed"));
      this.loot = NonNullList.withSize(9, ItemStack.EMPTY);
      ItemStackHelper.loadAllItems(nbt, this.loot);
      this.setCombatTask();
   }

   protected boolean canDespawn() {
      try {
         if (this.loot == null) {
            return true;
         } else {
            int q = 0;

            for (ItemStack is : this.loot) {
               if (is != null && is.getCount() > 0) {
                  q++;
               }
            }

            return q < 5;
         }
      } catch (Exception var4) {
         return true;
      }
   }

   public boolean canBeLeashedTo(EntityPlayer player) {
      return false;
   }

   protected ResourceLocation getLootTable() {
      return LOOT;
   }

   protected void dropLoot(boolean wasRecentlyHit, int lootingModifier, DamageSource source) {
      for (int a = 0; a < this.loot.size(); a++) {
         if (!((ItemStack)this.loot.get(a)).isEmpty() && this.world.rand.nextFloat() < 0.33F) {
            this.entityDropItem(((ItemStack)this.loot.get(a)).copy(), 1.5F);
         }
      }

      super.dropLoot(wasRecentlyHit, lootingModifier, source);
   }

   @SideOnly(Side.CLIENT)
   public void handleStatusUpdate(byte par1) {
      if (par1 == 16) {
         this.mumble = (float) Math.PI;
      } else if (par1 == 17) {
         this.mumble = (float) (Math.PI * 2);
      } else if (par1 == 18) {
         for (int i = 0; i < 5; i++) {
            double d0 = this.rand.nextGaussian() * 0.02;
            double d1 = this.rand.nextGaussian() * 0.02;
            double d2 = this.rand.nextGaussian() * 0.02;
            this.world
               .spawnParticle(
                  EnumParticleTypes.VILLAGER_HAPPY,
                  this.posX + this.rand.nextFloat() * this.width * 2.0F - this.width,
                  this.posY + 0.5 + this.rand.nextFloat() * this.height,
                  this.posZ + this.rand.nextFloat() * this.width * 2.0F - this.width,
                  d0,
                  d1,
                  d2,
                  new int[0]
               );
         }
      }

      if (par1 == 19) {
         for (int i = 0; i < 5; i++) {
            double d0 = this.rand.nextGaussian() * 0.02;
            double d1 = this.rand.nextGaussian() * 0.02;
            double d2 = this.rand.nextGaussian() * 0.02;
            this.world
               .spawnParticle(
                  EnumParticleTypes.VILLAGER_ANGRY,
                  this.posX + this.rand.nextFloat() * this.width * 2.0F - this.width,
                  this.posY + 0.5 + this.rand.nextFloat() * this.height,
                  this.posZ + this.rand.nextFloat() * this.width * 2.0F - this.width,
                  d0,
                  d1,
                  d2,
                  new int[0]
               );
         }

         this.mumble = (float) (Math.PI * 2);
      } else {
         super.handleStatusUpdate(par1);
      }
   }

   public void playLivingSound() {
      if (!this.world.isRemote) {
         if (this.rand.nextInt(3) == 0) {
            List list = this.world.getEntitiesWithinAABBExcludingEntity(this, this.getEntityBoundingBox().grow(4.0, 2.0, 4.0));

            for (int i = 0; i < list.size(); i++) {
               Entity entity1 = (Entity)list.get(i);
               if (entity1 instanceof EntityPech) {
                  this.world.setEntityState(this, (byte)17);
                  this.playSound(SoundsTC.pech_trade, this.getSoundVolume(), this.getSoundPitch());
                  return;
               }
            }
         }

         this.world.setEntityState(this, (byte)16);
      }

      super.playLivingSound();
   }

   public int getTalkInterval() {
      return 120;
   }

   protected float getSoundVolume() {
      return 0.4F;
   }

   protected SoundEvent getAmbientSound() {
      return SoundsTC.pech_idle;
   }

   protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
      return SoundsTC.pech_hit;
   }

   protected SoundEvent getDeathSound() {
      return SoundsTC.pech_death;
   }

   private void becomeAngryAt(Entity entity) {
      if (!(entity instanceof EntityPlayer) || !((EntityPlayer)entity).isCreative()) {
         if (this.getAnger() <= 0) {
            this.world.setEntityState(this, (byte)19);
            this.playSound(SoundsTC.pech_charge, this.getSoundVolume(), this.getSoundPitch());
         }

         this.setAttackTarget((EntityLivingBase)entity);
         this.setAnger(400 + this.rand.nextInt(400));
         this.setTamed(false);
         this.setCombatTask();
      }
   }

   public int getTotalArmorValue() {
      int i = super.getTotalArmorValue() + 2;
      if (i > 20) {
         i = 20;
      }

      return i;
   }

   public boolean attackEntityFrom(DamageSource damSource, float par2) {
      if (this.isEntityInvulnerable(damSource)) {
         return false;
      } else {
         Entity entity = damSource.getTrueSource();
         if (entity instanceof EntityPlayer) {
            List list = this.world.getEntitiesWithinAABBExcludingEntity(this, this.getEntityBoundingBox().grow(32.0, 16.0, 32.0));

            for (int i = 0; i < list.size(); i++) {
               Entity entity1 = (Entity)list.get(i);
               if (entity1 instanceof EntityPech) {
                  EntityPech entitypech = (EntityPech)entity1;
                  entitypech.becomeAngryAt(entity);
               }
            }

            this.becomeAngryAt(entity);
         }

         return super.attackEntityFrom(damSource, par2);
      }
   }

   public void onUpdate() {
      if (this.mumble > 0.0F) {
         this.mumble *= 0.75F;
      }

      if (this.getAnger() > 0) {
         this.setAnger(this.getAnger() - 1);
      }

      if (this.getAnger() > 0 && this.getAttackTarget() != null) {
         if (this.chargecount > 0) {
            this.chargecount--;
         }

         if (this.chargecount == 0) {
            this.chargecount = 100;
            this.playSound(SoundsTC.pech_charge, this.getSoundVolume(), this.getSoundPitch());
         }

         this.world.setEntityState(this, (byte)17);
      }

      if (this.world.isRemote && this.rand.nextInt(15) == 0 && this.getAnger() > 0) {
         double d0 = this.rand.nextGaussian() * 0.02;
         double d1 = this.rand.nextGaussian() * 0.02;
         double d2 = this.rand.nextGaussian() * 0.02;
         this.world
            .spawnParticle(
               EnumParticleTypes.VILLAGER_ANGRY,
               this.posX + this.rand.nextFloat() * this.width * 2.0F - this.width,
               this.posY + 0.5 + this.rand.nextFloat() * this.height,
               this.posZ + this.rand.nextFloat() * this.width * 2.0F - this.width,
               d0,
               d1,
               d2,
               new int[0]
            );
      }

      if (this.world.isRemote && this.rand.nextInt(25) == 0 && this.isTamed()) {
         double d0 = this.rand.nextGaussian() * 0.02;
         double d1 = this.rand.nextGaussian() * 0.02;
         double d2 = this.rand.nextGaussian() * 0.02;
         this.world
            .spawnParticle(
               EnumParticleTypes.VILLAGER_HAPPY,
               this.posX + this.rand.nextFloat() * this.width * 2.0F - this.width,
               this.posY + 0.5 + this.rand.nextFloat() * this.height,
               this.posZ + this.rand.nextFloat() * this.width * 2.0F - this.width,
               d0,
               d1,
               d2,
               new int[0]
            );
      }

      super.onUpdate();
   }

   public void updateAITasks() {
      super.updateAITasks();
      if (this.ticksExisted % 40 == 0) {
         this.heal(1.0F);
      }
   }

   public boolean canPickup(ItemStack entityItem) {
      if (entityItem == null) {
         return false;
      } else if (!this.isTamed() && valuedItems.containsKey(Item.getIdFromItem(entityItem.getItem()))) {
         return true;
      } else {
         for (int a = 0; a < this.loot.size(); a++) {
            if (!((ItemStack)this.loot.get(a)).isEmpty() && ((ItemStack)this.loot.get(a)).getCount() <= 0) {
               this.loot.set(a, ItemStack.EMPTY);
            }

            if (((ItemStack)this.loot.get(a)).isEmpty()) {
               return true;
            }

            if (InventoryUtils.areItemStacksEqualStrict(entityItem, (ItemStack)this.loot.get(a))
               && entityItem.getCount() + ((ItemStack)this.loot.get(a)).getCount() <= ((ItemStack)this.loot.get(a)).getMaxStackSize()) {
               return true;
            }
         }

         return false;
      }
   }

   public ItemStack pickupItem(ItemStack entityItem) {
      if (entityItem != null && !entityItem.isEmpty()) {
         if (!this.isTamed() && this.isValued(entityItem)) {
            if (this.rand.nextInt(10) < this.getValue(entityItem)) {
               this.setTamed(true);
               this.setCombatTask();
               this.world.setEntityState(this, (byte)18);
            }

            entityItem.shrink(1);
            return entityItem.getCount() <= 0 ? ItemStack.EMPTY : entityItem;
         } else {
            for (int a = 0; a < this.loot.size(); a++) {
               if (this.loot.get(a) != null && ((ItemStack)this.loot.get(a)).getCount() <= 0) {
                  this.loot.set(a, ItemStack.EMPTY);
               }

               if (entityItem != null
                  && !entityItem.isEmpty()
                  && entityItem.getCount() > 0
                  && !((ItemStack)this.loot.get(a)).isEmpty()
                  && ((ItemStack)this.loot.get(a)).getCount() < ((ItemStack)this.loot.get(a)).getMaxStackSize()
                  && InventoryUtils.areItemStacksEqualStrict(entityItem, (ItemStack)this.loot.get(a))) {
                  if (entityItem.getCount() + ((ItemStack)this.loot.get(a)).getCount() <= ((ItemStack)this.loot.get(a)).getMaxStackSize()) {
                     ((ItemStack)this.loot.get(a)).grow(entityItem.getCount());
                     return ItemStack.EMPTY;
                  }

                  int sz = Math.min(entityItem.getCount(), ((ItemStack)this.loot.get(a)).getMaxStackSize() - ((ItemStack)this.loot.get(a)).getCount());
                  ((ItemStack)this.loot.get(a)).grow(sz);
                  entityItem.shrink(sz);
               }

               if (entityItem != null && !entityItem.isEmpty() && entityItem.getCount() <= 0) {
                  entityItem = ItemStack.EMPTY;
               }
            }

            for (int a = 0; a < this.loot.size(); a++) {
               if (!((ItemStack)this.loot.get(a)).isEmpty() && ((ItemStack)this.loot.get(a)).getCount() <= 0) {
                  this.loot.set(a, ItemStack.EMPTY);
               }

               if (entityItem != null && entityItem.getCount() > 0 && ((ItemStack)this.loot.get(a)).isEmpty()) {
                  this.loot.set(a, entityItem.copy());
                  return null;
               }
            }

            if (entityItem != null && !entityItem.isEmpty() && entityItem.getCount() <= 0) {
               entityItem = ItemStack.EMPTY;
            }

            return entityItem;
         }
      } else {
         return ItemStack.EMPTY;
      }
   }

   protected boolean processInteract(EntityPlayer player, EnumHand hand) {
      if (!player.isSneaking() && (player.getHeldItem(hand) == null || !(player.getHeldItem(hand).getItem() instanceof ItemNameTag))) {
         if (this.isTamed()) {
            if (!this.world.isRemote) {
               player.openGui(Thaumcraft.instance, 1, this.world, this.getEntityId(), 0, 0);
            }

            return true;
         } else {
            return super.processInteract(player, hand);
         }
      } else {
         return false;
      }
   }

   public boolean isValued(ItemStack item) {
      if (item != null && !item.isEmpty()) {
         boolean value = valuedItems.containsKey(Item.getIdFromItem(item.getItem()));
         if (!value) {
            AspectList al = ThaumcraftCraftingManager.getObjectTags(item);
            if (al.getAmount(Aspect.DESIRE) > 1) {
               value = true;
            }
         }

         return value;
      } else {
         return false;
      }
   }

   public int getValue(ItemStack item) {
      if (item != null && !item.isEmpty()) {
         int value = valuedItems.containsKey(Item.getIdFromItem(item.getItem())) ? valuedItems.get(Item.getIdFromItem(item.getItem())) : 0;
         if (value == 0) {
            AspectList al = ThaumcraftCraftingManager.getObjectTags(item);
            value = Math.min(32, al.getAmount(Aspect.DESIRE) / 2);
         }

         return value;
      } else {
         return 0;
      }
   }

   public void setSwingingArms(boolean swingingArms) {
   }
}
