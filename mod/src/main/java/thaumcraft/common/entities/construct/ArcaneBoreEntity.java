package thaumcraft.common.entities.construct;

import com.mojang.authlib.GameProfile;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.NameTagItem;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.level.BlockEvent;
import thaumcraft.common.blocks.devices.DeviceUtils;
import thaumcraft.common.lib.enchantment.EnumInfusionEnchantment;
import thaumcraft.common.menu.ArcaneBoreMenu;
import thaumcraft.common.registers.ModBlocks;
import thaumcraft.common.registers.ModItems;
import thaumcraft.common.registers.ModMobs;
import thaumcraft.common.registers.ModSounds;
import thaumcraft.common.world.aura.AuraHandler;

/**
 * EntityArcaneBore: a mining turret. Given a pickaxe and a redstone signal (or an unpowered activator rail) it digs a
 * spiralling tunnel in the direction it faces, paying 0.25 vis per block from the aura. The pickaxe's material,
 * enchantments and infusion enchantments set the tunnel size, speed, fortune, silk touch and refining.
 */
public class ArcaneBoreEntity extends OwnedConstructEntity {
   private static final EntityDataAccessor<Direction> FACING = SynchedEntityData.defineId(ArcaneBoreEntity.class, EntityDataSerializers.DIRECTION);
   private static final EntityDataAccessor<Boolean> ACTIVE = SynchedEntityData.defineId(ArcaneBoreEntity.class, EntityDataSerializers.BOOLEAN);
   private static final GameProfile BORE_PROFILE = new GameProfile(UUID.fromString("5a2b7c1e-7f3d-4c8e-9b1a-3e6d2f4a8b70"), "FakeThaumcraftBore");

   @Nullable
   BlockPos digTarget = null;
   @Nullable
   BlockPos digTargetPrev = null;
   float digCost = 0.25F;
   long soundDelay = 0L;
   int breakCounter = 0;
   int digDelay = 0;
   int digDelayMax = 0;
   float radInc = 0.0F;
   public int spiral = 0;
   public float currentRadius = 0.0F;
   private float charge = 0.0F;
   /** Set by entity events 16/17: the client draws the dig beam. */
   public boolean clientDigging = false;

   public ArcaneBoreEntity(EntityType<? extends ArcaneBoreEntity> type, Level level) {
      super(type, level);
      setDropChance(EquipmentSlot.MAINHAND, 0.0F);
   }

   public static AttributeSupplier.Builder createAttributes() {
      return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 50.0).add(Attributes.FOLLOW_RANGE, 32.0);
   }

   public void placeAt(BlockPos pos, Direction facing) {
      setFacing(facing);
      moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0.0F, 0.0F);
   }

   @Override
   protected EntityDimensions getDefaultDimensions(Pose pose) {
      return super.getDefaultDimensions(pose).withEyeHeight(0.8125F);
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder builder) {
      super.defineSynchedData(builder);
      builder.define(FACING, Direction.DOWN);
      builder.define(ACTIVE, false);
   }

   @Override
   public void tick() {
      super.tick();
      if (!level().isClientSide) {
         setYRot(getYHeadRot());
         if (tickCount % 50 == 0) heal(1.0F);
         if (tickCount % 10 == 0 && getCharge() < 10.0F) rechargeVis();
         Boolean rail = activatorRailPowered();
         if (rail != null) {
            setActive(!rail);
         } else if (!onRail() && !isPassenger()) {
            setActive(level().hasNeighborSignal(blockPosition().below()));
         }
      }
      if (!isActive()) {
         digTarget = null;
         getLookControl().setLookAt(getX() + getFacing().getStepX(), getY(), getZ() + getFacing().getStepZ(), 10.0F, 33.0F);
      }
      if (digTarget != null && getCharge() >= digCost && !level().isClientSide) {
         getLookControl().setLookAt(digTarget.getX() + 0.5, digTarget.getY(), digTarget.getZ() + 0.5, 10.0F, 90.0F);
         if (digDelay-- <= 0 && dig()) {
            setCharge(getCharge() - digCost);
            if (soundDelay < System.currentTimeMillis()) {
               soundDelay = System.currentTimeMillis() + 1200L + level().random.nextInt(100);
               playSound(ModSounds.RUMBLE.get(), 0.25F, 0.9F + level().random.nextFloat() * 0.2F);
            }
         }
      }
      if (!level().isClientSide && digTarget == null && isActive() && validInventory()) {
         findNextBlockToDig();
         if (digTarget != null) {
            level().broadcastEntityEvent(this, (byte) 16);
            // STUB: PacketFXBoreDig (the stream of block particles flying into the bore) is not ported; the client
            // draws the dig beam only.
         } else {
            level().broadcastEntityEvent(this, (byte) 17);
            getLookControl().setLookAt(getX() + getFacing().getStepX() * 2, getY() + getFacing().getStepY() * 2 + getEyeHeight(),
               getZ() + getFacing().getStepZ() * 2, 10.0F, 33.0F);
         }
      }
   }

   public static boolean isPickaxe(ItemStack s) {
      return !s.isEmpty() && (s.getItem() instanceof PickaxeItem || s.canPerformAction(ItemAbilities.PICKAXE_DIG));
   }

   public boolean validInventory() {
      ItemStack s = getMainHandItem();
      return isPickaxe(s) && !(s.getDamageValue() + 1 >= s.getMaxDamage());
   }

   private int ench(ResourceKey<Enchantment> key) {
      ItemStack s = getMainHandItem();
      if (s.isEmpty()) return 0;
      Holder<Enchantment> h = level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key);
      return EnchantmentHelper.getItemEnchantmentLevel(h, s);
   }

   public int getDigRadius() {
      int r = 0;
      ItemStack s = getMainHandItem();
      if (isPickaxe(s)) {
         r = s.getItem().getEnchantmentValue(s) / 3;
         r += EnumInfusionEnchantment.getInfusionEnchantmentLevel(s, EnumInfusionEnchantment.DESTRUCTIVE) * 2;
      }
      return r <= 1 ? 2 : r;
   }

   public int getDigDepth() {
      int r = getDigRadius() * 8;
      return r + EnumInfusionEnchantment.getInfusionEnchantmentLevel(getMainHandItem(), EnumInfusionEnchantment.BURROWING) * 16;
   }

   public int getFortune() {
      int r = 0;
      if (validInventory()) {
         r = ench(Enchantments.FORTUNE);
         r = Math.max(r, EnumInfusionEnchantment.getInfusionEnchantmentLevel(getMainHandItem(), EnumInfusionEnchantment.SOUNDING));
      }
      return r;
   }

   public int getDigSpeed(BlockState state) {
      int speed = 0;
      if (validInventory()) {
         speed += (int) (getMainHandItem().getDestroySpeed(state) / 2.0F);
         speed += ench(Enchantments.EFFICIENCY);
      }
      return speed;
   }

   public int getRefining() {
      return getMainHandItem().isEmpty() ? 0 : EnumInfusionEnchantment.getInfusionEnchantmentLevel(getMainHandItem(), EnumInfusionEnchantment.REFINING);
   }

   public boolean hasSilkTouch() {
      return ench(Enchantments.SILK_TOUCH) > 0;
   }

   /** Utils.findSpecialMiningResult: ore drops may come out as native clusters. */
   static ItemStack specialMiningResult(ItemStack is, float chance, net.minecraft.util.RandomSource rand) {
      Item out = null;
      Item i = is.getItem();
      if (i == Items.RAW_IRON || i == Items.IRON_ORE || i == Items.DEEPSLATE_IRON_ORE) out = ModItems.CLUSTER_IRON.get();
      else if (i == Items.RAW_GOLD || i == Items.GOLD_ORE || i == Items.DEEPSLATE_GOLD_ORE) out = ModItems.CLUSTER_GOLD.get();
      else if (i == Items.RAW_COPPER || i == Items.COPPER_ORE || i == Items.DEEPSLATE_COPPER_ORE) out = ModItems.CLUSTER_COPPER.get();
      else if (i == Items.QUARTZ) out = ModItems.CLUSTER_QUARTZ.get();
      else if (i == ModBlocks.ORE_CINNABAR.get().asItem() || i == ModBlocks.DEEPSLATE_ORE_CINNABAR.get().asItem()) out = ModItems.CLUSTER_CINNABAR.get();
      if (out != null && rand.nextFloat() <= chance) return new ItemStack(out, is.getCount());
      return is;
   }

   private boolean dig() {
      boolean b = false;
      if (digTarget != null && level() instanceof ServerLevel sl && !sl.isEmptyBlock(digTarget)) {
         BlockState digBs = sl.getBlockState(digTarget);
         boolean silktouch = hasSilkTouch();
         int fortune = silktouch ? 0 : getFortune();
         FakePlayer fp = FakePlayerFactory.get(sl, BORE_PROFILE);
         fp.moveTo(getX(), getY(), getZ(), getYRot(), getXRot());
         fp.setItemInHand(InteractionHand.MAIN_HAND, getMainHandItem());
         BlockEvent.BreakEvent ev = CommonHooks.fireBlockBreak(sl, fp.gameMode.getGameModeForPlayer(), fp, digTarget, digBs);
         fp.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
         if (ev.isCanceled()) {
            digTarget = null;
            return false;
         }
         ItemStack tool = getMainHandItem().copy();
         if (!silktouch && fortune > ench(Enchantments.FORTUNE)) {
            tool.enchant(sl.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.FORTUNE), fortune);
         }
         BlockEntity be = sl.getBlockEntity(digTarget);
         List<ItemStack> items = new ArrayList<>(Block.getDrops(digBs, sl, digTarget, be, this, tool));
         sl.levelEvent(null, 2001, digTarget, Block.getId(digBs));
         b = sl.removeBlock(digTarget, false);
         if (b) {
            for (ItemEntity e : sl.getEntitiesOfClass(ItemEntity.class, new AABB(digTarget).inflate(1.5))) {
               items.add(e.getItem().copy());
               e.discard();
            }
            int refining = getRefining();
            for (ItemStack is : items) {
               ItemStack dropped = is.copy();
               if (!silktouch && refining > 0) dropped = specialMiningResult(is, (refining + 1) * 0.125F, sl.random);
               if (dropped.isEmpty()) continue;
               boolean e = false;
               for (Direction f : Direction.values()) {
                  if (DeviceUtils.getItemHandlerAt(sl, blockPosition().relative(f), f) != null) {
                     DeviceUtils.ejectStackAt(sl, blockPosition(), f, dropped);
                     e = true;
                     break;
                  }
               }
               if (!e) DeviceUtils.ejectStackAt(sl, blockPosition(), getFacing().getOpposite(), dropped);
            }
            breakCounter++;
         }
         if (!getMainHandItem().isEmpty()) {
            if (breakCounter >= 50) {
               breakCounter -= 50;
               getMainHandItem().hurtAndBreak(1, this, EquipmentSlot.MAINHAND);
            }
         } else {
            breakCounter = 0;
         }
      }
      digTarget = null;
      return b;
   }

   static Vec3 rotateAroundX(Vec3 v, float a) {
      float c = Mth.cos(a), s = Mth.sin(a);
      return new Vec3(v.x, v.y * c + v.z * s, v.z * c - v.y * s);
   }

   static Vec3 rotateAroundY(Vec3 v, float a) {
      float c = Mth.cos(a), s = Mth.sin(a);
      return new Vec3(v.x * c + v.z * s, v.y, v.z * c - v.x * s);
   }

   static Vec3 rotateAroundZ(Vec3 v, float a) {
      float c = Mth.cos(a), s = Mth.sin(a);
      return new Vec3(v.x * c + v.y * s, v.y * c - v.x * s, v.z);
   }

   @Nullable
   private BlockHitResult clip(Vec3 from, Vec3 to) {
      BlockHitResult r = level().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
      return r.getType() == HitResult.Type.MISS ? null : r;
   }

   private void findNextBlockToDig() {
      int digRadius = getDigRadius();
      if (digTargetPrev == null || digTargetPrev.distToCenterSqr(position()) > (digRadius + 1) * (digRadius + 1)) {
         digTargetPrev = blockPosition();
      }
      if (radInc == 0.0F) radInc = 1.0F;
      int digDepth = getDigDepth();
      int x = digTargetPrev.getX();
      int z = digTargetPrev.getZ();
      int y = digTargetPrev.getY();
      Direction f = getFacing();
      BlockPos end = new BlockPos(x + f.getStepX() * digDepth, y + f.getStepY() * digDepth, z + f.getStepZ() * digDepth);
      BlockHitResult mop = clip(Vec3.atCenterOf(digTargetPrev), Vec3.atCenterOf(end));
      if (mop != null) {
         Vec3 digger = new Vec3(getX() + f.getStepX(), getY() + getEyeHeight() + f.getStepY(), getZ() + f.getStepZ());
         mop = clip(digger, Vec3.atCenterOf(mop.getBlockPos()));
         if (mop != null) {
            BlockPos mp = mop.getBlockPos();
            BlockState bs = level().getBlockState(mp);
            float hardness = bs.getDestroySpeed(level(), mp);
            if (hardness > -1.0F && !bs.getCollisionShape(level(), mp).isEmpty()) {
               digDelay = Math.max(10 - getDigSpeed(bs), (int) (hardness * 2.0F) - getDigSpeed(bs) * 2);
               if (digDelay < 1) digDelay = 1;
               digDelayMax = digDelay;
               if (!mp.equals(blockPosition()) && !mp.equals(blockPosition().below())) {
                  digTarget = mp;
                  return;
               }
            }
         }
      }
      int guard = 0;
      while (x == digTargetPrev.getX() && z == digTargetPrev.getZ() && y == digTargetPrev.getY() && guard++ < 10000) {
         if (Math.abs(currentRadius) > digRadius) currentRadius = digRadius;
         spiral = (int) (spiral + (3.0F + Math.max(0.0F, (10.0F - Math.abs(currentRadius)) * 2.0F)));
         if (spiral >= 360) {
            spiral -= 360;
            currentRadius += radInc;
            if (currentRadius > digRadius || currentRadius < -digRadius) currentRadius = 0.0F;
         }
         Vec3 vsource = new Vec3((int) getX() + 0.5 + f.getStepX(), getY() + f.getStepY() + getEyeHeight(), (int) getZ() + 0.5 + f.getStepZ());
         Vec3 vtar = new Vec3(0.0, currentRadius, 0.0);
         vtar = rotateAroundZ(vtar, spiral / 180.0F * (float) Math.PI);
         vtar = rotateAroundY(vtar, (float) (Math.PI / 2) * f.getStepX());
         vtar = rotateAroundX(vtar, (float) (Math.PI / 2) * f.getStepY());
         Vec3 vres = vsource.add(vtar);
         x = Mth.floor(vres.x);
         y = Mth.floor(vres.y);
         z = Mth.floor(vres.z);
      }
      digTargetPrev = new BlockPos(x, y, z);
   }

   @Override
   public boolean hurt(DamageSource source, float amount) {
      if (source.getEntity() instanceof LivingEntity le && isOwner(le)) {
         Direction f = nearestFacing(le);
         if (f != Direction.DOWN) setFacing(f);
         return false;
      }
      setYRot((float) (getYRot() + getRandom().nextGaussian() * 45.0));
      setXRot((float) (getXRot() + getRandom().nextGaussian() * 20.0));
      return super.hurt(source, amount);
   }

   /** EnumFacing.getDirectionFromEntityLiving: the side of the bore facing the attacker. */
   private Direction nearestFacing(LivingEntity e) {
      BlockPos pos = blockPosition();
      if (Math.abs(e.getX() - (pos.getX() + 0.5F)) < 2.0 && Math.abs(e.getZ() - (pos.getZ() + 0.5F)) < 2.0) {
         double d0 = e.getEyeY();
         if (d0 - pos.getY() > 2.0) return Direction.UP;
         if (pos.getY() - d0 > 0.0) return Direction.DOWN;
      }
      return e.getDirection().getOpposite();
   }

   protected void rechargeVis() {
      setCharge(getCharge() + AuraHandler.drainVis(level(), blockPosition(), 10.0F, false));
   }

   @Override
   public boolean isPushable() {
      return true;
   }

   @Override
   public boolean isPickable() {
      return true;
   }

   @Override
   public void die(DamageSource cause) {
      super.die(cause);
      if (!level().isClientSide) dropStuff();
   }

   protected void dropStuff() {
      if (!getMainHandItem().isEmpty()) {
         spawnAtLocation(getMainHandItem(), 0.5F);
         setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
      }
   }

   @Override
   protected InteractionResult mobInteract(Player player, InteractionHand hand) {
      if (player.getItemInHand(hand).getItem() instanceof NameTagItem) return InteractionResult.PASS;
      if (isOwner(player) && !isRemoved()) {
         if (!level().isClientSide) {
            if (player.isShiftKeyDown()) {
               playSound(ModSounds.ZAP.get(), 1.0F, 1.0F);
               dropStuff();
               spawnAtLocation(new ItemStack(ModMobs.TURRET_BORE.get()), 0.5F);
               discard();
               player.swing(hand);
            } else {
               player.openMenu(new SimpleMenuProvider((id, inv, p) -> new ArcaneBoreMenu(id, inv, this), getDisplayName()),
                  buf -> buf.writeVarInt(getId()));
            }
         }
         return InteractionResult.sidedSuccess(level().isClientSide);
      }
      return super.mobInteract(player, hand);
   }

   @Override
   public void knockback(double strength, double x, double z) {
      super.knockback(strength, x / 10.0, z / 10.0);
      Vec3 m = getDeltaMovement();
      if (m.y > 0.1) setDeltaMovement(m.x, 0.1, m.z);
   }

   public boolean isActive() {
      return entityData.get(ACTIVE);
   }

   public void setActive(boolean b) {
      entityData.set(ACTIVE, b);
   }

   @Override
   public void readAdditionalSaveData(CompoundTag nbt) {
      super.readAdditionalSaveData(nbt);
      setCharge(nbt.getFloat("charge"));
      setFacing(Direction.from3DDataValue(nbt.getByte("faceing")));
      setActive(nbt.getBoolean("active"));
   }

   @Override
   public void addAdditionalSaveData(CompoundTag nbt) {
      super.addAdditionalSaveData(nbt);
      nbt.putFloat("charge", getCharge());
      nbt.putByte("faceing", (byte) getFacing().get3DDataValue());
      nbt.putBoolean("active", isActive());
   }

   public Direction getFacing() {
      return entityData.get(FACING);
   }

   public void setFacing(Direction face) {
      entityData.set(FACING, face);
   }

   public float getCharge() {
      return charge;
   }

   public void setCharge(float c) {
      charge = c;
   }

   @Override
   public void move(MoverType type, Vec3 v) {
      super.move(type, new Vec3(v.x / 5.0, v.y, v.z / 5.0));
   }

   @Override
   public void kill() {
      hurt(damageSources().fellOutOfWorld(), 400.0F);
   }

   @Override
   protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
      super.dropCustomDeathLoot(level, source, recentlyHit);
      float b = TurretCrossbowEntity.looting(level, source) * 0.15F;
      if (random.nextFloat() < 0.2F + b) spawnAtLocation(new ItemStack(ModItems.MIND_CLOCKWORK.get()), 0.5F);
      if (random.nextFloat() < 0.2F + b) spawnAtLocation(new ItemStack(ModItems.MORPHIC_RESONATOR.get()), 0.5F);
      if (random.nextFloat() < 0.2F + b) spawnAtLocation(new ItemStack(ModBlocks.CRYSTAL_AER.get()), 0.5F);
      if (random.nextFloat() < 0.2F + b) spawnAtLocation(new ItemStack(ModBlocks.CRYSTAL_TERRA.get()), 0.5F);
      if (random.nextFloat() < 0.5F + b) spawnAtLocation(new ItemStack(ModItems.MECHANISM_SIMPLE.get()), 0.5F);
      if (random.nextFloat() < 0.5F + b) spawnAtLocation(new ItemStack(ModItems.PLATE_BRASS.get()), 0.5F);
      if (random.nextFloat() < 0.5F + b) spawnAtLocation(new ItemStack(ModBlocks.PLANK_GREATWOOD.get()), 0.5F);
   }

   @Override
   public int getMaxHeadXRot() {
      return 10;
   }

   @Override
   public void handleEntityEvent(byte id) {
      if (id == 16) {
         clientDigging = true;
      } else if (id == 17) {
         clientDigging = false;
      } else {
         super.handleEntityEvent(id);
      }
   }

   /** Used by the GUI: Blocks.STONE speed. */
   public int stoneDigSpeed() {
      return getDigSpeed(Blocks.STONE.defaultBlockState());
   }
}
