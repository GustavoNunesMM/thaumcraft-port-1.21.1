package thaumcraft.common.golems;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.navigation.WallClimberNavigation;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.NameTagItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import thaumcraft.api.golems.EnumGolemTrait;
import thaumcraft.api.golems.IGolemAPI;
import thaumcraft.api.golems.IGolemProperties;
import thaumcraft.api.golems.tasks.Task;
import thaumcraft.common.golems.ai.AIFollowOwner;
import thaumcraft.common.golems.ai.AIGotoBlock;
import thaumcraft.common.golems.ai.AIGotoEntity;
import thaumcraft.common.golems.ai.AIGotoHome;
import thaumcraft.common.golems.ai.AIOwnerHurtByTarget;
import thaumcraft.common.golems.ai.AIOwnerHurtTarget;
import thaumcraft.common.golems.tasks.TaskHandler;
import thaumcraft.common.registers.ModGolems;
import thaumcraft.common.registers.ModSounds;
import thaumcraft.common.research.ResearchManager;

/** EntityThaumcraftGolem: a golem made of parts, working the tasks of nearby seals within its home range. */
public class GolemEntity extends OwnedConstructEntity implements IGolemAPI, RangedAttackMob {
   private static final EntityDataAccessor<Long> PROPS = SynchedEntityData.defineId(GolemEntity.class, EntityDataSerializers.LONG);
   /** PROPS3: byte 0 the colour, byte 1 the flags (bit 1 following, bit 3 in combat). */
   private static final EntityDataAccessor<Integer> PROPS3 = SynchedEntityData.defineId(GolemEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Byte> CLIMBING = SynchedEntityData.defineId(GolemEntity.class, EntityDataSerializers.BYTE);
   public static final int XPM = 1000;
   /** Golem emotes (ModConfig showGolemEmotes, a client graphics option in 1.12). */
   public static boolean showEmotes = true;

   int rankXp = 0;
   public boolean redrawParts = false;
   private boolean firstRun = true;
   protected Task task = null;
   protected int taskID = Integer.MAX_VALUE;
   private long cachedPropsKey = Long.MIN_VALUE;
   private IGolemProperties cachedProps;

   public GolemEntity(EntityType<? extends GolemEntity> type, Level level) {
      super(type, level);
      this.xpReward = 5;
   }

   public static AttributeSupplier.Builder createAttributes() {
      return Mob.createMobAttributes().add(Attributes.MOVEMENT_SPEED, 0.3).add(Attributes.MAX_HEALTH, 10.0).add(Attributes.ATTACK_DAMAGE, 0.0)
         .add(Attributes.FOLLOW_RANGE, 40.0);
   }

   @Override
   protected void registerGoals() {
      targetSelector.removeAllGoals(g -> true);
      goalSelector.addGoal(2, new AIGotoEntity(this));
      goalSelector.addGoal(3, new AIGotoBlock(this));
      goalSelector.addGoal(4, new AIGotoHome(this));
      goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 8.0F));
      goalSelector.addGoal(6, new RandomLookAroundGoal(this));
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder builder) {
      super.defineSynchedData(builder);
      builder.define(PROPS, 0L);
      builder.define(PROPS3, 0);
      builder.define(CLIMBING, (byte) 0);
   }

   @Override
   public IGolemProperties getProperties() {
      long l = entityData.get(PROPS);
      if (cachedProps == null || l != cachedPropsKey) {
         cachedProps = GolemProperties.fromLong(l);
         cachedPropsKey = l;
      }
      return cachedProps;
   }

   @Override
   public void setProperties(IGolemProperties prop) {
      entityData.set(PROPS, prop.toLong());
      cachedProps = null;
   }

   @Override
   public byte getGolemColor() {
      return (byte) (entityData.get(PROPS3) >>> 24);
   }

   public void setGolemColor(byte b) {
      entityData.set(PROPS3, entityData.get(PROPS3) & 0x00FFFFFF | (b & 0xFF) << 24);
   }

   public byte getFlags() {
      return (byte) (entityData.get(PROPS3) >>> 16);
   }

   public void setFlags(byte b) {
      entityData.set(PROPS3, entityData.get(PROPS3) & 0xFF00FFFF | (b & 0xFF) << 16);
   }

   private void updateEntityAttributes() {
      IGolemProperties p = getProperties();
      int mh = 10 + p.getMaterial().healthMod;
      if (p.hasTrait(EnumGolemTrait.FRAGILE)) mh = (int) (mh * 0.75);
      mh += p.getRank();
      getAttribute(Attributes.MAX_HEALTH).setBaseValue(mh);
      getAttribute(Attributes.STEP_HEIGHT).setBaseValue(p.hasTrait(EnumGolemTrait.WHEELED) ? 0.5 : 0.6);
      restrictTo(getRestrictCenter().equals(BlockPos.ZERO) ? blockPosition() : getRestrictCenter(), p.hasTrait(EnumGolemTrait.SCOUT) ? 48 : 32);
      getAttribute(Attributes.FOLLOW_RANGE).setBaseValue(p.hasTrait(EnumGolemTrait.SCOUT) ? 56.0 : 40.0);
      this.navigation = getGolemNavigator();
      if (p.hasTrait(EnumGolemTrait.FLYER)) this.moveControl = new FlyingMoveControl(this);
      if (p.hasTrait(EnumGolemTrait.FIGHTER)) {
         double da = p.getMaterial().damage;
         if (p.hasTrait(EnumGolemTrait.BRUTAL)) da = Math.max(da * 1.5, da + 1.0);
         da += p.getRank() * 0.25;
         getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(da);
      } else {
         getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(0.0);
      }
      createAI();
   }

   private void createAI() {
      goalSelector.removeAllGoals(g -> true);
      targetSelector.removeAllGoals(g -> true);
      if (isFollowingOwner()) {
         goalSelector.addGoal(4, new AIFollowOwner(this, 1.0, 10.0F, 2.0F));
      } else {
         goalSelector.addGoal(3, new AIGotoEntity(this));
         goalSelector.addGoal(4, new AIGotoBlock(this));
         goalSelector.addGoal(5, new AIGotoHome(this));
      }
      goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
      goalSelector.addGoal(9, new RandomLookAroundGoal(this));
      if (getProperties().hasTrait(EnumGolemTrait.FIGHTER)) {
         if (getNavigation() instanceof GroundPathNavigation) goalSelector.addGoal(0, new FloatGoal(this));
         if (getProperties().hasTrait(EnumGolemTrait.RANGED) && getProperties().getArms().function != null) {
            Goal aa = getProperties().getArms().function.getRangedAttackAI(this);
            if (aa != null) goalSelector.addGoal(1, aa);
         }
         goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.15, false));
         if (isFollowingOwner()) {
            targetSelector.addGoal(1, new AIOwnerHurtByTarget(this));
            targetSelector.addGoal(2, new AIOwnerHurtTarget(this));
         }
         targetSelector.addGoal(3, new HurtByTargetGoal(this));
      }
   }

   @Override
   public boolean onClimbable() {
      return isBesideClimbableBlock();
   }

   /** onInitialSpawn: the place it was put down becomes its home. */
   public void initialSpawn() {
      restrictTo(blockPosition(), 32);
      updateEntityAttributes();
   }

   /** /summon without NBT: a default golem (wood, basic head, arms and legs) set up like a placed one. */
   @Nullable
   @Override
   public net.minecraft.world.entity.SpawnGroupData finalizeSpawn(net.minecraft.world.level.ServerLevelAccessor level,
                                                               net.minecraft.world.DifficultyInstance diff,
                                                               net.minecraft.world.entity.MobSpawnType reason,
                                                               @Nullable net.minecraft.world.entity.SpawnGroupData data) {
      net.minecraft.world.entity.SpawnGroupData out = super.finalizeSpawn(level, diff, reason, data);
      if (reason == net.minecraft.world.entity.MobSpawnType.COMMAND) {
         initialSpawn();
         setHealth(getMaxHealth());
      }
      return out;
   }

   @Override
   public int getArmorValue() {
      int armor = getProperties().getMaterial().armor;
      if (getProperties().hasTrait(EnumGolemTrait.ARMORED)) armor = (int) Math.max(armor * 1.5, armor + 1);
      if (getProperties().hasTrait(EnumGolemTrait.FRAGILE)) armor = (int) (armor * 0.75);
      return armor;
   }

   @Override
   public void aiStep() {
      updateSwingTime();
      super.aiStep();
   }

   @Override
   public void tick() {
      super.tick();
      IGolemProperties p = getProperties();
      if (p.hasTrait(EnumGolemTrait.FLYER)) setNoGravity(true);
      if (!level().isClientSide) {
         if (firstRun) {
            firstRun = false;
            if (hasRestriction() && !blockPosition().equals(getRestrictCenter())) goHome();
         }
         if (task != null && task.isSuspended()) task = null;
         if (getTarget() != null && !getTarget().isAlive()) setTarget(null);
         if (getTarget() != null && p.hasTrait(EnumGolemTrait.RANGED) && distanceToSqr(getTarget()) > 1024.0) setTarget(null);
         if (getTarget() instanceof Player && level().getServer() != null && !level().getServer().isPvpAllowed()) setTarget(null);
         if (tickCount % (p.hasTrait(EnumGolemTrait.REPAIR) ? 40 : 100) == 0) heal(1.0F);
         if (p.hasTrait(EnumGolemTrait.CLIMBER)) setBesideClimbableBlock(horizontalCollision);
      } else if (tickCount < 20 || tickCount % 20 == 0) {
         redrawParts = true;
      }
      if (p.getHead().function != null) p.getHead().function.onUpdateTick(this);
      if (p.getArms().function != null) p.getArms().function.onUpdateTick(this);
      if (p.getLegs().function != null) p.getLegs().function.onUpdateTick(this);
      if (p.getAddon().function != null) p.getAddon().function.onUpdateTick(this);
   }

   @Override
   public void handleEntityEvent(byte id) {
      if (id >= 5 && id <= 9) {
         GolemClientHooks.emote.accept(this, id);
      } else {
         super.handleEntityEvent(id);
      }
   }

   public float getGolemMoveSpeed() {
      IGolemProperties p = getProperties();
      return 1.0F + p.getRank() * 0.025F + (p.hasTrait(EnumGolemTrait.LIGHT) ? 0.2F : 0.0F) + (p.hasTrait(EnumGolemTrait.HEAVY) ? -0.175F : 0.0F)
         + (p.hasTrait(EnumGolemTrait.FLYER) ? -0.33F : 0.0F) + (p.hasTrait(EnumGolemTrait.WHEELED) ? 0.25F : 0.0F);
   }

   /** PathNavigateGolemAir / PathNavigateClimber / PathNavigateGolemGround (the vanilla navigators, which do the same). */
   public PathNavigation getGolemNavigator() {
      if (getProperties().hasTrait(EnumGolemTrait.FLYER)) {
         FlyingPathNavigation nav = new FlyingPathNavigation(this, level());
         nav.setCanOpenDoors(true);
         nav.setCanPassDoors(true);
         return nav;
      }
      GroundPathNavigation nav = getProperties().hasTrait(EnumGolemTrait.CLIMBER) ? new WallClimberNavigation(this, level())
         : new GroundPathNavigation(this, level());
      nav.setCanOpenDoors(true);
      nav.setCanPassDoors(true);
      return nav;
   }

   @Override
   protected Entity.MovementEmission getMovementEmission() {
      return getProperties().hasTrait(EnumGolemTrait.HEAVY) && !getProperties().hasTrait(EnumGolemTrait.FLYER) ? Entity.MovementEmission.ALL
         : Entity.MovementEmission.EVENTS;
   }

   @Override
   public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
      if (getProperties().hasTrait(EnumGolemTrait.FLYER) || getProperties().hasTrait(EnumGolemTrait.CLIMBER)) return false;
      return super.causeFallDamage(distance, multiplier, source);
   }

   @Override
   public boolean fireImmune() {
      return getProperties().hasTrait(EnumGolemTrait.FIREPROOF) || super.fireImmune();
   }

   /** Back to its home block, moved up until it fits. */
   private void goHome() {
      Vec3 old = position();
      BlockPos home = getRestrictCenter();
      for (int dy = 0; dy < 16 && home.getY() + dy < level().getMaxBuildHeight(); dy++) {
         setPos(home.getX() + 0.5, home.getY() + dy, home.getZ() + 0.5);
         if (level().noCollision(this, getBoundingBox())) {
            teleportTo(home.getX() + 0.5, home.getY() + dy, home.getZ() + 0.5);
            getNavigation().stop();
            return;
         }
      }
      setPos(old);
   }

   @Override
   public void readAdditionalSaveData(CompoundTag nbt) {
      super.readAdditionalSaveData(nbt);
      setProperties(GolemProperties.fromLong(nbt.getLong("props")));
      restrictTo(BlockPos.of(nbt.getLong("homepos")), 32);
      setFlags(nbt.getByte("gflags"));
      rankXp = nbt.getInt("rankXP");
      setGolemColor(nbt.getByte("color"));
      updateEntityAttributes();
   }

   @Override
   public void addAdditionalSaveData(CompoundTag nbt) {
      super.addAdditionalSaveData(nbt);
      nbt.putLong("props", getProperties().toLong());
      nbt.putLong("homepos", getRestrictCenter().asLong());
      nbt.putByte("gflags", getFlags());
      nbt.putInt("rankXP", rankXp);
      nbt.putByte("color", getGolemColor());
   }

   @Override
   protected void actuallyHurt(DamageSource ds, float damage) {
      IGolemProperties p = getProperties();
      if (ds.is(DamageTypeTags.IS_FIRE) && p.hasTrait(EnumGolemTrait.FIREPROOF)) return;
      if (ds.is(DamageTypeTags.IS_EXPLOSION) && p.hasTrait(EnumGolemTrait.BLASTPROOF)) damage = Math.min(getMaxHealth() / 2.0F, damage * 0.3F);
      if (ds.is(DamageTypes.CACTUS)) return;
      if (hasRestriction() && (ds.is(DamageTypes.IN_WALL) || ds.is(DamageTypes.FELL_OUT_OF_WORLD))) goHome();
      super.actuallyHurt(ds, damage);
   }

   /** The placer item for this golem (props and rank xp). */
   public ItemStack toPlacer() {
      ItemStack placer = new ItemStack(ModGolems.GOLEM_PLACER.get());
      placer.set(ModGolems.GOLEM_PROPS.get(), getProperties().toLong());
      placer.set(ModGolems.GOLEM_XP.get(), rankXp);
      return placer;
   }

   @Override
   protected InteractionResult mobInteract(Player player, InteractionHand hand) {
      if (!isAlive()) return InteractionResult.PASS;
      ItemStack held = player.getItemInHand(hand);
      if (held.getItem() instanceof NameTagItem) return InteractionResult.PASS;
      if (!level().isClientSide && isOwner(player)) {
         if (player.isShiftKeyDown()) {
            playSound(ModSounds.ZAP.get(), 1.0F, 1.0F);
            if (task != null) task.setReserved(false);
            dropCarried();
            spawnAtLocation(toPlacer(), 0.5F);
            discard();
            player.swing(hand);
         } else if (held.getItem() instanceof GolemBellItem && ResearchManager.knowsResearch(player, "GOLEMDIRECT")) {
            if (task != null) task.setReserved(false);
            playSound(ModSounds.SCAN.get(), 1.0F, 1.0F);
            setFollowingOwner(!isFollowingOwner());
            if (isFollowingOwner()) {
               player.displayClientMessage(Component.translatable("golem.follow", ""), true);
               if (showEmotes) level().broadcastEntityEvent(this, (byte) 5);
               clearRestriction();
            } else {
               player.displayClientMessage(Component.translatable("golem.stay", ""), true);
               if (showEmotes) level().broadcastEntityEvent(this, (byte) 8);
               restrictTo(blockPosition(), getProperties().hasTrait(EnumGolemTrait.SCOUT) ? 48 : 32);
            }
            updateEntityAttributes();
            player.swing(hand);
         } else if (held.getItem() instanceof DyeItem dye) {
            playSound(ModSounds.ZAP.get(), 1.0F, 1.0F);
            setGolemColor((byte) (dye.getDyeColor().getId() + 1));
            held.shrink(1);
            player.swing(hand);
         }
         return InteractionResult.SUCCESS;
      }
      return super.mobInteract(player, hand);
   }

   @Override
   public void die(DamageSource cause) {
      if (task != null) task.setReserved(false);
      // Dropped first, so the vanilla equipment drop finds the hands empty.
      if (!level().isClientSide && !isRemoved() && !dead) dropCarried();
      super.die(cause);
   }

   protected void dropCarried() {
      for (ItemStack s : getCarrying()) {
         if (!s.isEmpty()) spawnAtLocation(s.copy(), 0.25F);
      }
      setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
      setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
   }

   /** dropFewItems: some of the components it was made of. */
   @Override
   protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
      super.dropCustomDeathLoot(level, source, recentlyHit);
      for (ItemStack stack : getProperties().generateComponents()) {
         ItemStack s = stack.copy();
         if (random.nextFloat() < 0.3F) {
            if (s.getCount() > 0) s.shrink(random.nextInt(s.getCount()));
            if (!s.isEmpty()) spawnAtLocation(s, 0.25F);
         }
      }
   }

   public boolean isBesideClimbableBlock() {
      return (entityData.get(CLIMBING) & 1) != 0;
   }

   public void setBesideClimbableBlock(boolean climbing) {
      byte b0 = entityData.get(CLIMBING);
      entityData.set(CLIMBING, climbing ? (byte) (b0 | 1) : (byte) (b0 & -2));
   }

   public boolean isFollowingOwner() {
      return (getFlags() & 1 << 1) != 0;
   }

   public void setFollowingOwner(boolean b) {
      setFlags((byte) (b ? getFlags() | 1 << 1 : getFlags() & ~(1 << 1)));
   }

   @Override
   public void setTarget(@Nullable LivingEntity target) {
      super.setTarget(target);
      setInCombat(getTarget() != null);
   }

   @Override
   public boolean isInCombat() {
      return (getFlags() & 1 << 3) != 0;
   }

   public void setInCombat(boolean b) {
      setFlags((byte) (b ? getFlags() | 1 << 3 : getFlags() & ~(1 << 3)));
   }

   @Override
   public boolean doHurtTarget(Entity ent) {
      boolean flag = super.doHurtTarget(ent);
      if (flag) {
         if (ent instanceof LivingEntity le && getProperties().hasTrait(EnumGolemTrait.DEFT) && level() instanceof ServerLevel sl) {
            // recentlyHit = 100: kills drop player-only loot and xp.
            le.setLastHurtByPlayer(FakePlayerFactory.getMinecraft(sl));
         }
         if (getProperties().getArms().function != null) getProperties().getArms().function.onMeleeAttack(this, ent);
         if (ent instanceof Mob m && !m.isAlive()) addRankXp(8);
      }
      return flag;
   }

   @Nullable
   public Task getTask() {
      if (task == null && taskID != Integer.MAX_VALUE) {
         task = TaskHandler.getTask(level(), taskID);
         taskID = Integer.MAX_VALUE;
      }
      return task;
   }

   public void setTask(@Nullable Task task) {
      this.task = task;
   }

   @Override
   public void addRankXp(int xp) {
      if (!getProperties().hasTrait(EnumGolemTrait.SMART) || level().isClientSide) return;
      int rank = getProperties().getRank();
      if (rank >= 10) return;
      rankXp += xp;
      int xn = (rank + 1) * (rank + 1) * XPM;
      if (rankXp >= xn) {
         rankXp -= xn;
         IGolemProperties props = GolemProperties.fromLong(getProperties().toLong());
         props.setRank(rank + 1);
         setProperties(props);
         if (showEmotes) {
            level().broadcastEntityEvent(this, (byte) 9);
            playSound(SoundEvents.PLAYER_LEVELUP, 0.25F, 1.0F);
         }
      }
   }

   public int getRankXp() {
      return rankXp;
   }

   public void setRankXp(int xp) {
      rankXp = xp;
   }

   private int slots() {
      return getProperties().hasTrait(EnumGolemTrait.HAULER) ? 2 : 1;
   }

   private static EquipmentSlot slot(int a) {
      return a == 0 ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
   }

   @Override
   public ItemStack holdItem(ItemStack stack) {
      if (stack.isEmpty()) return stack;
      for (int a = 0; a < slots(); a++) {
         ItemStack in = getItemBySlot(slot(a));
         if (in.isEmpty()) {
            setItemSlot(slot(a), stack);
            return ItemStack.EMPTY;
         }
         if (in.getCount() < in.getMaxStackSize() && ItemStack.isSameItemSameComponents(in, stack)) {
            int d = Math.min(stack.getCount(), in.getMaxStackSize() - in.getCount());
            stack.shrink(d);
            in.grow(d);
            if (stack.isEmpty()) stack = ItemStack.EMPTY;
         }
      }
      return stack;
   }

   @Override
   public ItemStack dropItem(ItemStack stack) {
      ItemStack out = ItemStack.EMPTY;
      for (int a = 0; a < slots(); a++) {
         ItemStack in = getItemBySlot(slot(a));
         if (in.isEmpty()) continue;
         if (!stack.isEmpty()) {
            if (ItemStack.isSameItemSameComponents(in, stack)) {
               out = in.copy();
               out.setCount(Math.min(stack.getCount(), out.getCount()));
               in.shrink(stack.getCount());
               if (in.getCount() <= 0) setItemSlot(slot(a), ItemStack.EMPTY);
            }
         } else {
            out = in.copy();
            setItemSlot(slot(a), ItemStack.EMPTY);
         }
         if (!out.isEmpty()) break;
      }
      if (getProperties().hasTrait(EnumGolemTrait.HAULER) && getItemBySlot(EquipmentSlot.MAINHAND).isEmpty()
         && !getItemBySlot(EquipmentSlot.OFFHAND).isEmpty()) {
         setItemSlot(EquipmentSlot.MAINHAND, getItemBySlot(EquipmentSlot.OFFHAND).copy());
         setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
      }
      return out;
   }

   @Override
   public int canCarryAmount(ItemStack stack) {
      int ss = 0;
      for (int a = 0; a < slots(); a++) {
         ItemStack in = getItemBySlot(slot(a));
         if (in.isEmpty()) {
            ss += stack.getMaxStackSize();
         } else if (ItemStack.isSameItemSameComponents(in, stack)) {
            ss += in.getMaxStackSize() - in.getCount();
         }
      }
      return ss;
   }

   @Override
   public boolean canCarry(ItemStack stack, boolean partial) {
      int ca = canCarryAmount(stack);
      return ca > 0 && (partial || ca >= stack.getCount());
   }

   @Override
   public boolean isCarrying(ItemStack stack) {
      if (stack.isEmpty()) return false;
      for (int a = 0; a < slots(); a++) {
         ItemStack in = getItemBySlot(slot(a));
         if (!in.isEmpty() && ItemStack.isSameItemSameComponents(in, stack)) return true;
      }
      return false;
   }

   @Override
   public NonNullList<ItemStack> getCarrying() {
      if (getProperties().hasTrait(EnumGolemTrait.HAULER)) {
         NonNullList<ItemStack> stacks = NonNullList.withSize(2, ItemStack.EMPTY);
         stacks.set(0, getItemBySlot(EquipmentSlot.MAINHAND));
         stacks.set(1, getItemBySlot(EquipmentSlot.OFFHAND));
         return stacks;
      }
      return NonNullList.withSize(1, getItemBySlot(EquipmentSlot.MAINHAND));
   }

   @Override
   public LivingEntity getGolemEntity() {
      return this;
   }

   @Override
   public Level getGolemWorld() {
      return level();
   }

   @Override
   public void swingArm() {
      swing(InteractionHand.MAIN_HAND);
   }

   @Override
   public void performRangedAttack(LivingEntity target, float range) {
      if (getProperties().getArms().function != null) getProperties().getArms().function.onRangedAttack(this, target, range);
   }

   @Override
   public boolean canPickUpLoot() {
      return false;
   }

   /** The golem's colour as a dye, or null for none. */
   @Nullable
   public DyeColor getDyeColor() {
      int c = getGolemColor();
      return c >= 1 && c <= 16 ? DyeColor.byId(c - 1) : null;
   }

   static class FlyingMoveControl extends MoveControl {
      private final GolemEntity golem;

      FlyingMoveControl(GolemEntity golem) {
         super(golem);
         this.golem = golem;
      }

      @Override
      public void tick() {
         if (operation != Operation.MOVE_TO) return;
         double d0 = wantedX - golem.getX();
         double d1 = wantedY - golem.getY();
         double d2 = wantedZ - golem.getZ();
         double d3 = Math.sqrt(d0 * d0 + d1 * d1 + d2 * d2);
         if (d3 < golem.getBoundingBox().getSize()) {
            operation = Operation.WAIT;
            golem.setDeltaMovement(golem.getDeltaMovement().scale(0.5));
         } else {
            golem.setDeltaMovement(golem.getDeltaMovement().add(d0 / d3 * 0.033 * speedModifier, d1 / d3 * 0.0125 * speedModifier,
               d2 / d3 * 0.033 * speedModifier));
            Vec3 m = golem.getDeltaMovement();
            if (golem.getTarget() == null) {
               golem.setYRot(-((float) Mth.atan2(m.x, m.z)) * (180.0F / (float) Math.PI));
            } else {
               double d4 = golem.getTarget().getX() - golem.getX();
               double d5 = golem.getTarget().getZ() - golem.getZ();
               golem.setYRot(-((float) Mth.atan2(d4, d5)) * (180.0F / (float) Math.PI));
            }
            golem.yBodyRot = golem.getYRot();
         }
      }
   }
}
