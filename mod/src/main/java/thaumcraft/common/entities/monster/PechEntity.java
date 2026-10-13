package thaumcraft.common.entities.monster;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerLevelAccessor;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.OpenDoorGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.NameTagItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.Tags;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectHelper;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.casters.FocusEngine;
import thaumcraft.api.casters.FocusMediumRoot;
import thaumcraft.api.casters.FocusPackage;
import thaumcraft.api.casters.IFocusElement;
import thaumcraft.common.entities.ai.pech.PechItemGotoGoal;
import thaumcraft.common.entities.ai.pech.PechNearestAttackableTargetGoal;
import thaumcraft.common.entities.ai.pech.PechTradeGoal;
import thaumcraft.common.items.casters.foci.FocusEffectAir;
import thaumcraft.common.items.casters.foci.FocusEffectCurse;
import thaumcraft.common.items.casters.foci.FocusEffectEarth;
import thaumcraft.common.items.casters.foci.FocusEffectFire;
import thaumcraft.common.items.casters.foci.FocusEffectFlux;
import thaumcraft.common.items.casters.foci.FocusMediumProjectile;
import thaumcraft.common.menu.PechMenu;
import thaumcraft.common.registers.ModMobs;
import thaumcraft.common.registers.ModSounds;

/**
 * EntityPech: greedy little traders of the magical biomes. Feeding one something it values may tame it; a tamed pech
 * trades (GuiPech). Type 0 forager (melee), 1 mage (pech wand, casts curse/flux/earth/air/fire bolts), 2 stalker (bow).
 */
public class PechEntity extends Monster implements RangedAttackMob {
   private static final EntityDataAccessor<Byte> TYPE = SynchedEntityData.defineId(PechEntity.class, EntityDataSerializers.BYTE);
   private static final EntityDataAccessor<Integer> ANGER = SynchedEntityData.defineId(PechEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Boolean> TAMED = SynchedEntityData.defineId(PechEntity.class, EntityDataSerializers.BOOLEAN);
   /** Marks items a pech handed back so it does not pick them up again ("PechDrop" thrower). */
   public static final String PECH_DROP = "thaumcraft:pech_drop";

   public NonNullList<ItemStack> loot = NonNullList.withSize(9, ItemStack.EMPTY);
   public boolean trading;
   private final RangedAttackGoal aiArrowAttack = new RangedAttackGoal(this, 0.6, 20, 50, 15.0F);
   private final RangedAttackGoal aiBlastAttack = new RangedAttackGoal(this, 0.6, 20, 50, 15.0F);
   private final MeleeAttackGoal aiMeleeAttack = new MeleeAttackGoal(this, 0.6, false);
   private final AvoidEntityGoal<Player> aiAvoidPlayer = new AvoidEntityGoal<>(this, Player.class, 8.0F, 0.5, 0.6);
   public float mumble;
   int chargecount;

   public PechEntity(EntityType<? extends PechEntity> type, Level level) {
      super(type, level);
      xpReward = 8;
      if (getNavigation() instanceof GroundPathNavigation g) {
         g.setCanOpenDoors(true);
         g.setCanFloat(false);
      }
      setDropChance(EquipmentSlot.MAINHAND, 0.2F);
      setDropChance(EquipmentSlot.OFFHAND, 0.2F);
   }

   public static AttributeSupplier.Builder createAttributes() {
      return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 30.0).add(Attributes.ATTACK_DAMAGE, 6.0)
         .add(Attributes.MOVEMENT_SPEED, 0.5).add(Attributes.ARMOR, 2.0);
   }

   @Override
   protected void registerGoals() {
      goalSelector.addGoal(0, new FloatGoal(this));
      goalSelector.addGoal(1, new PechTradeGoal(this));
      goalSelector.addGoal(3, new PechItemGotoGoal(this));
      goalSelector.addGoal(5, new OpenDoorGoal(this, true));
      goalSelector.addGoal(6, new MoveTowardsRestrictionGoal(this, 0.5));
      goalSelector.addGoal(9, new RandomStrollGoal(this, 0.6));
      goalSelector.addGoal(9, new LookAtPlayerGoal(this, Player.class, 3.0F, 1.0F));
      goalSelector.addGoal(10, new LookAtPlayerGoal(this, Mob.class, 8.0F));
      goalSelector.addGoal(11, new RandomLookAroundGoal(this));
      targetSelector.addGoal(1, new HurtByTargetGoal(this));
      targetSelector.addGoal(2, new PechNearestAttackableTargetGoal(this, Player.class, true));
   }

   @Override
   public Component getName() {
      if (hasCustomName()) return super.getName();
      return switch (getPechType()) {
         case 1 -> Component.translatable("entity.thaumcraft.pech.mage");
         case 2 -> Component.translatable("entity.thaumcraft.pech.stalker");
         default -> super.getName();
      };
   }

   public void setCombatTask() {
      if (level().isClientSide) return;
      goalSelector.removeGoal(aiMeleeAttack);
      goalSelector.removeGoal(aiArrowAttack);
      goalSelector.removeGoal(aiBlastAttack);
      ItemStack itemstack = getMainHandItem();
      if (itemstack.is(Items.BOW)) {
         goalSelector.addGoal(2, aiArrowAttack);
      } else if (itemstack.is(ModMobs.PECH_WAND.get())) {
         goalSelector.addGoal(2, aiBlastAttack);
      } else {
         goalSelector.addGoal(2, aiMeleeAttack);
      }
      goalSelector.removeGoal(aiAvoidPlayer);
      if (!isTamed()) goalSelector.addGoal(4, aiAvoidPlayer);
   }

   @Override
   public void performRangedAttack(LivingEntity target, float f) {
      if (getPechType() == 2) {
         ItemStack ammo = new ItemStack(Items.ARROW);
         Arrow arrow = new Arrow(level(), this, ammo, getMainHandItem());
         if (random.nextFloat() < 0.2F) arrow.addEffect(new MobEffectInstance(MobEffects.POISON, 40));
         double d0 = target.getX() - getX();
         double d1 = target.getBoundingBox().minY + target.getBbHeight() / 3.0F - arrow.getY();
         double d2 = target.getZ() - getZ();
         double d3 = Math.sqrt(d0 * d0 + d2 * d2);
         arrow.shoot(d0, d1 + d3 * 0.2F, d2, 1.6F, 14 - level().getDifficulty().getId() * 4);
         arrow.setBaseDamage(f * 2.0F + random.nextGaussian() * 0.25 + level().getDifficulty().getId() * 0.11F);
         playSound(SoundEvents.ARROW_SHOOT, 1.0F, 1.0F / (getRandom().nextFloat() * 0.4F + 0.8F));
         level().addFreshEntity(arrow);
      } else if (getPechType() == 1) {
         FocusPackage p = new FocusPackage(this);
         FocusMediumRoot root = new FocusMediumRoot();
         root.setupFromCasterToTarget(this, target, distanceTo(target) / 6.0F);
         p.addNode(root);
         FocusMediumProjectile fp = new FocusMediumProjectile();
         fp.getSetting("speed").setValue(2);
         p.addNode(fp);
         IFocusElement effect = random.nextBoolean() ? new FocusEffectCurse()
            : random.nextBoolean() ? new FocusEffectFlux()
            : random.nextBoolean() ? new FocusEffectEarth()
            : random.nextBoolean() ? new FocusEffectAir() : new FocusEffectFire();
         p.addNode(effect);
         FocusEngine.castFocusPackage(this, p, true);
         swing(InteractionHand.MAIN_HAND);
      }
   }

   @Override
   public void setItemSlot(EquipmentSlot slot, ItemStack stack) {
      super.setItemSlot(slot, stack);
      if (!level().isClientSide && slot == EquipmentSlot.MAINHAND) setCombatTask();
   }

   @Override
   protected void populateDefaultEquipmentSlots(RandomSource rand, DifficultyInstance difficulty) {
      super.populateDefaultEquipmentSlots(rand, difficulty);
      ItemStack held = switch (rand.nextInt(20)) {
         case 0, 12 -> new ItemStack(ModMobs.PECH_WAND.get());
         case 1 -> new ItemStack(Items.STONE_SWORD);
         case 2, 4, 10, 11, 13 -> new ItemStack(Items.BOW);
         case 3 -> new ItemStack(Items.STONE_AXE);
         case 5 -> new ItemStack(Items.IRON_SWORD);
         case 6 -> new ItemStack(Items.IRON_AXE);
         case 7 -> new ItemStack(Items.FISHING_ROD);
         case 8 -> new ItemStack(Items.STONE_PICKAXE);
         case 9 -> new ItemStack(Items.IRON_PICKAXE);
         default -> ItemStack.EMPTY;
      };
      if (!held.isEmpty()) setItemSlot(EquipmentSlot.MAINHAND, held);
   }

   @Nullable
   @Override
   public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance diff, MobSpawnType reason, @Nullable SpawnGroupData data) {
      populateDefaultEquipmentSlots(random, diff);
      ItemStack itemstack = getMainHandItem();
      if (itemstack.is(ModMobs.PECH_WAND.get())) {
         setPechType(1);
         setDropChance(EquipmentSlot.MAINHAND, 0.1F);
      } else if (!itemstack.isEmpty()) {
         if (itemstack.is(Items.BOW)) setPechType(2);
         populateDefaultEquipmentEnchantments(level, random, diff);
      }
      setCanPickUpLoot(random.nextFloat() < 0.75F * diff.getSpecialMultiplier());
      setCombatTask();
      return data;
   }

   /** getCanSpawnHere: at most 4 pechs around, in a magical biome. */
   public static boolean checkSpawnRules(EntityType<PechEntity> type, ServerLevelAccessor level, MobSpawnType reason, BlockPos pos, RandomSource random) {
      if (!checkMonsterSpawnRules(type, level, reason, pos, random)) return false;
      if (MobSpawnType.isSpawner(reason)) return true;
      if (!level.getBiome(pos).is(Tags.Biomes.IS_MAGICAL)) return false;
      return level.getEntitiesOfClass(PechEntity.class, new AABB(pos).inflate(16.0)).size() < 4;
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder builder) {
      super.defineSynchedData(builder);
      builder.define(TYPE, (byte) 0);
      builder.define(ANGER, 0);
      builder.define(TAMED, false);
   }

   public int getPechType() {
      return entityData.get(TYPE);
   }

   public int getAnger() {
      return entityData.get(ANGER);
   }

   public boolean isTamed() {
      return entityData.get(TAMED);
   }

   public void setPechType(int t) {
      entityData.set(TYPE, (byte) t);
   }

   public void setAnger(int a) {
      entityData.set(ANGER, a);
   }

   public void setTamed(boolean t) {
      entityData.set(TAMED, t);
   }

   @Override
   public void addAdditionalSaveData(CompoundTag nbt) {
      super.addAdditionalSaveData(nbt);
      nbt.putByte("PechType", (byte) getPechType());
      nbt.putShort("Anger", (short) getAnger());
      nbt.putBoolean("Tamed", isTamed());
      ContainerHelper.saveAllItems(nbt, loot, registryAccess());
   }

   @Override
   public void readAdditionalSaveData(CompoundTag nbt) {
      super.readAdditionalSaveData(nbt);
      if (nbt.contains("PechType")) setPechType(nbt.getByte("PechType"));
      setAnger(nbt.getShort("Anger"));
      setTamed(nbt.getBoolean("Tamed"));
      loot = NonNullList.withSize(9, ItemStack.EMPTY);
      ContainerHelper.loadAllItems(nbt, loot, registryAccess());
      setCombatTask();
   }

   /** canDespawn: a pech carrying five or more stacks of loot stays. */
   @Override
   public boolean removeWhenFarAway(double dist) {
      int q = 0;
      for (ItemStack is : loot) {
         if (!is.isEmpty()) q++;
      }
      return q < 5;
   }

   @Override
   public boolean canBeLeashed() {
      return false;
   }

   @Override
   protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
      for (ItemStack is : loot) {
         if (!is.isEmpty() && random.nextFloat() < 0.33F) spawnAtLocation(is.copy(), 1.5F);
      }
      super.dropCustomDeathLoot(level, source, recentlyHit);
   }

   @Override
   public void handleEntityEvent(byte id) {
      switch (id) {
         case 16 -> mumble = (float) Math.PI;
         case 17 -> mumble = (float) (Math.PI * 2);
         case 18 -> particles(ParticleTypes.HAPPY_VILLAGER, 5);
         case 19 -> {
            particles(ParticleTypes.ANGRY_VILLAGER, 5);
            mumble = (float) (Math.PI * 2);
         }
         default -> super.handleEntityEvent(id);
      }
   }

   private void particles(ParticleOptions type, int count) {
      for (int i = 0; i < count; i++) {
         level().addParticle(type, getX() + random.nextFloat() * getBbWidth() * 2.0F - getBbWidth(), getY() + 0.5 + random.nextFloat() * getBbHeight(),
            getZ() + random.nextFloat() * getBbWidth() * 2.0F - getBbWidth(), random.nextGaussian() * 0.02, random.nextGaussian() * 0.02,
            random.nextGaussian() * 0.02);
      }
   }

   @Override
   public void playAmbientSound() {
      if (!level().isClientSide) {
         if (random.nextInt(3) == 0) {
            for (Entity e : level().getEntities(this, getBoundingBox().inflate(4.0, 2.0, 4.0))) {
               if (e instanceof PechEntity) {
                  level().broadcastEntityEvent(this, (byte) 17);
                  playSound(ModSounds.PECH_TRADE.get(), getSoundVolume(), getVoicePitch());
                  return;
               }
            }
         }
         level().broadcastEntityEvent(this, (byte) 16);
      }
      super.playAmbientSound();
   }

   @Override
   public int getAmbientSoundInterval() {
      return 120;
   }

   @Override
   protected float getSoundVolume() {
      return 0.4F;
   }

   @Override
   protected SoundEvent getAmbientSound() {
      return ModSounds.PECH_IDLE.get();
   }

   @Override
   protected SoundEvent getHurtSound(DamageSource src) {
      return ModSounds.PECH_HIT.get();
   }

   @Override
   protected SoundEvent getDeathSound() {
      return ModSounds.PECH_DEATH.get();
   }

   private void becomeAngryAt(Entity entity) {
      if (entity instanceof Player p && p.isCreative()) return;
      if (getAnger() <= 0) {
         level().broadcastEntityEvent(this, (byte) 19);
         playSound(ModSounds.PECH_CHARGE.get(), getSoundVolume(), getVoicePitch());
      }
      if (entity instanceof LivingEntity le) setTarget(le);
      setAnger(400 + random.nextInt(400));
      setTamed(false);
      setCombatTask();
   }

   @Override
   public boolean hurt(DamageSource source, float amount) {
      if (isInvulnerableTo(source)) return false;
      Entity entity = source.getEntity();
      if (entity instanceof Player && !level().isClientSide) {
         for (Entity e : level().getEntities(this, getBoundingBox().inflate(32.0, 16.0, 32.0))) {
            if (e instanceof PechEntity pech) pech.becomeAngryAt(entity);
         }
         becomeAngryAt(entity);
      }
      return super.hurt(source, amount);
   }

   @Override
   public void tick() {
      if (mumble > 0.0F) mumble *= 0.75F;
      if (!level().isClientSide) {
         if (getAnger() > 0) setAnger(getAnger() - 1);
         if (getAnger() > 0 && getTarget() != null) {
            if (chargecount > 0) chargecount--;
            if (chargecount == 0) {
               chargecount = 100;
               playSound(ModSounds.PECH_CHARGE.get(), getSoundVolume(), getVoicePitch());
            }
            level().broadcastEntityEvent(this, (byte) 17);
         }
      } else {
         if (random.nextInt(15) == 0 && getAnger() > 0) particles(ParticleTypes.ANGRY_VILLAGER, 1);
         if (random.nextInt(25) == 0 && isTamed()) particles(ParticleTypes.HAPPY_VILLAGER, 1);
      }
      super.tick();
   }

   @Override
   protected void customServerAiStep() {
      super.customServerAiStep();
      if (tickCount % 40 == 0) heal(1.0F);
   }

   /** canPickup: valued items while untamed, or anything that fits in its pack. */
   public boolean canPickup(ItemStack stack) {
      if (stack.isEmpty()) return false;
      if (!isTamed() && PechTrades.valuedItems().containsKey(stack.getItem())) return true;
      for (int a = 0; a < loot.size(); a++) {
         ItemStack l = loot.get(a);
         if (l.isEmpty()) return true;
         if (ItemStack.isSameItemSameComponents(stack, l) && stack.getCount() + l.getCount() <= l.getMaxStackSize()) return true;
      }
      return false;
   }

   /** pickupItem: eats valued items (maybe getting tamed) or stores the stack; returns what is left over. */
   public ItemStack pickupItem(ItemStack entityItem) {
      if (entityItem.isEmpty()) return ItemStack.EMPTY;
      if (!isTamed() && isValued(entityItem)) {
         if (random.nextInt(10) < getValue(entityItem)) {
            setTamed(true);
            setCombatTask();
            level().broadcastEntityEvent(this, (byte) 18);
         }
         entityItem.shrink(1);
         return entityItem.isEmpty() ? ItemStack.EMPTY : entityItem;
      }
      for (int a = 0; a < loot.size() && !entityItem.isEmpty(); a++) {
         ItemStack l = loot.get(a);
         if (!l.isEmpty() && l.getCount() < l.getMaxStackSize() && ItemStack.isSameItemSameComponents(entityItem, l)) {
            int sz = Math.min(entityItem.getCount(), l.getMaxStackSize() - l.getCount());
            l.grow(sz);
            entityItem.shrink(sz);
         }
      }
      if (entityItem.isEmpty()) return ItemStack.EMPTY;
      for (int a = 0; a < loot.size(); a++) {
         if (loot.get(a).isEmpty()) {
            loot.set(a, entityItem.copy());
            return ItemStack.EMPTY;
         }
      }
      return entityItem;
   }

   @Override
   protected InteractionResult mobInteract(Player player, InteractionHand hand) {
      if (player.isShiftKeyDown() || player.getItemInHand(hand).getItem() instanceof NameTagItem) return InteractionResult.PASS;
      if (isTamed()) {
         if (!level().isClientSide) {
            player.openMenu(new SimpleMenuProvider((id, inv, p) -> new PechMenu(id, inv, this), getDisplayName()), buf -> buf.writeVarInt(getId()));
         }
         return InteractionResult.sidedSuccess(level().isClientSide);
      }
      return super.mobInteract(player, hand);
   }

   public boolean isValued(ItemStack item) {
      if (item.isEmpty()) return false;
      if (PechTrades.valuedItems().containsKey(item.getItem())) return true;
      AspectList al = AspectHelper.getObjectAspects(level(), item);
      return al != null && al.getAmount(Aspect.DESIRE) > 1;
   }

   public int getValue(ItemStack item) {
      if (item.isEmpty()) return 0;
      int value = PechTrades.valuedItems().getOrDefault(item.getItem(), 0);
      if (value == 0) {
         AspectList al = AspectHelper.getObjectAspects(level(), item);
         value = al == null ? 0 : Math.min(32, al.getAmount(Aspect.DESIRE) / 2);
      }
      return value;
   }

   @Override
   public boolean checkSpawnRules(LevelAccessor level, MobSpawnType reason) {
      return true;
   }

   /** The stacks in its pack, for tests and the trade menu. */
   public List<ItemStack> getLoot() {
      return loot;
   }
}
