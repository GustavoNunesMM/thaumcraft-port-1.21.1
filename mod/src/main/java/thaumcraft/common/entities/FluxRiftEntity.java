package thaumcraft.common.entities;

import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.entities.monster.WispEntity;
import thaumcraft.common.registers.ModMobs;
import net.minecraft.world.entity.MobSpawnType;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import thaumcraft.Thaumcraft;
import thaumcraft.api.casters.FocusEngine;
import thaumcraft.api.casters.FocusMediumRoot;
import thaumcraft.api.casters.FocusPackage;
import thaumcraft.common.blocks.world.taint.FluxEffects;
import thaumcraft.common.blocks.world.taint.TaintHelper;
import thaumcraft.common.entities.monster.tainted.TaintSeedPrimeEntity;
import thaumcraft.common.items.casters.foci.FocusEffectFlux;
import thaumcraft.common.items.casters.foci.FocusMediumCloud;
import thaumcraft.common.lib.FXHelper;
import thaumcraft.common.registers.ModFlux;
import thaumcraft.common.registers.ModItems;
import thaumcraft.common.registers.ModSounds;
import thaumcraft.common.research.PlayerWarp;
import thaumcraft.common.research.ResearchManager;
import thaumcraft.common.world.aura.AuraHandler;

/**
 * EntityFluxRift: a tear in reality opened by a flux-saturated aura (AuraHandler.RIFT_TRIGGER, see FluxEvents). It
 * feeds on flux to grow, eats blocks and entities along its length, loses stability over time and, when unstable,
 * releases chaotic events (which restore some stability). Infusion stabilizers nearby calm it; a causality collapser
 * (or too little substance) makes it collapse, dropping void seeds and hurting those nearby depending on its stability.
 *
 * <p>Port note: the primordial pearl it may drop is looked up by id.
 */
public class FluxRiftEntity extends Entity {
   private static final EntityDataAccessor<Integer> SEED = SynchedEntityData.defineId(FluxRiftEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Integer> SIZE = SynchedEntityData.defineId(FluxRiftEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Float> STABILITY = SynchedEntityData.defineId(FluxRiftEntity.class, EntityDataSerializers.FLOAT);
   private static final EntityDataAccessor<Boolean> COLLAPSE = SynchedEntityData.defineId(FluxRiftEntity.class, EntityDataSerializers.BOOLEAN);
   /** event, weight, stability gained, allowed near taint. */
   private static final int[][] EVENTS = {{0, 50, 5, 1}, {1, 10, 0, 0}, {2, 20, 10, 1}, {3, 20, 10, 1}, {4, 1, 0, 1}};

   public enum Stability {
      VERY_STABLE, STABLE, UNSTABLE, VERY_UNSTABLE
   }

   int maxSize;
   private int lastSize = -1;
   private int lastSeed;
   public final List<Vec3> points = new ArrayList<>();
   public final List<Float> pointsWidth = new ArrayList<>();

   public FluxRiftEntity(EntityType<? extends FluxRiftEntity> type, Level level) {
      super(type, level);
      noPhysics = true;
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder builder) {
      builder.define(SEED, 0).define(SIZE, 5).define(STABILITY, 0.0F).define(COLLAPSE, false);
   }

   public boolean getCollapse() {
      return entityData.get(COLLAPSE);
   }

   public void setCollapse(boolean b) {
      if (b) maxSize = getRiftSize();
      entityData.set(COLLAPSE, b);
   }

   public float getRiftStability() {
      return entityData.get(STABILITY);
   }

   public void setRiftStability(float s) {
      entityData.set(STABILITY, Mth.clamp(s, -100.0F, 100.0F));
   }

   public int getRiftSize() {
      return entityData.get(SIZE);
   }

   public void setRiftSize(int s) {
      entityData.set(SIZE, s);
      updateShape();
   }

   public int getRiftSeed() {
      return entityData.get(SEED);
   }

   public void setRiftSeed(int s) {
      entityData.set(SEED, s);
      updateShape();
   }

   @Override
   public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
      super.onSyncedDataUpdated(key);
      if (SIZE.equals(key) || SEED.equals(key)) updateShape();
   }

   /** setSize: the rift's shape (a seeded random walk both ways from the centre) and the bounding box around it. */
   protected void updateShape() {
      calcSteps(points, pointsWidth, new Random(getRiftSeed()));
      lastSize = getRiftSize();
      lastSeed = getRiftSeed();
      setBoundingBox(makeBoundingBox());
   }

   @Override
   protected AABB makeBoundingBox() {
      if (points == null || points.isEmpty()) return super.makeBoundingBox();
      double x0 = Double.MAX_VALUE, y0 = Double.MAX_VALUE, z0 = Double.MAX_VALUE;
      double x1 = -Double.MAX_VALUE, y1 = -Double.MAX_VALUE, z1 = -Double.MAX_VALUE;
      for (Vec3 v : points) {
         x0 = Math.min(x0, v.x);
         x1 = Math.max(x1, v.x);
         y0 = Math.min(y0, v.y);
         y1 = Math.max(y1, v.y);
         z0 = Math.min(z0, v.z);
         z1 = Math.max(z1, v.z);
      }
      return new AABB(getX() + x0, getY() + y0, getZ() + z0, getX() + x1, getY() + y1, getZ() + z1);
   }

   @Override
   protected void addAdditionalSaveData(CompoundTag tag) {
      tag.putInt("MaxSize", maxSize);
      tag.putInt("RiftSize", getRiftSize());
      tag.putInt("RiftSeed", getRiftSeed());
      tag.putFloat("Stability", getRiftStability());
      tag.putBoolean("collapse", getCollapse());
   }

   @Override
   protected void readAdditionalSaveData(CompoundTag tag) {
      maxSize = tag.getInt("MaxSize");
      setRiftSize(tag.getInt("RiftSize"));
      setRiftSeed(tag.getInt("RiftSeed"));
      setRiftStability(tag.getFloat("Stability"));
      entityData.set(COLLAPSE, tag.getBoolean("collapse"));
   }

   @Override
   public void move(MoverType type, Vec3 movement) {
   }

   @Override
   public boolean isPickable() {
      return false;
   }

   @Override
   public boolean displayFireAnimation() {
      return false;
   }

   @Override
   public boolean fireImmune() {
      return true;
   }

   @Override
   public void tick() {
      super.tick();
      if (lastSize != getRiftSize() || lastSeed != getRiftSeed()) updateShape();
      if (level().isClientSide) {
         clientTick();
         return;
      }
      if (getRiftSeed() == 0) setRiftSeed(random.nextInt());
      if (points.size() > 1) {
         int pi = random.nextInt(points.size() - 1);
         Vec3 v1 = points.get(pi).add(position());
         Vec3 v2 = points.get(pi + 1).add(position());
         BlockHitResult rt = level().clip(new ClipContext(v1, v2, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
         if (rt.getType() == HitResult.Type.BLOCK) {
            BlockPos p = rt.getBlockPos();
            BlockState bs = level().getBlockState(p);
            if (!bs.isAir() && bs.getDestroySpeed(level(), p) >= 0.0F) {
               level().levelEvent(2001, p, net.minecraft.world.level.block.Block.getId(bs));
               level().removeBlock(p, false);
            }
         }
         for (Entity e : level().getEntities(this, new AABB(v1, v1).inflate(0.5))) {
            if (!e.isAlive() || e instanceof Player pl && pl.isCreative()) continue;
            if (e.distanceToSqr(v1) > 0.25 + e.getBbWidth() * e.getBbWidth()) continue;
            e.hurt(damageSources().fellOutOfWorld(), 2.0F);
            if (e instanceof ItemEntity) e.discard();
         }
      }
      if (points.size() < 3 && !getCollapse()) setCollapse(true);
      if (getCollapse()) {
         setRiftSize(getRiftSize() - 1);
         if (random.nextBoolean()) AuraHandler.addVis(level(), blockPosition(), 1.0F);
         else AuraHandler.addFlux(level(), blockPosition(), 1.0F);
         if (random.nextInt(10) == 0) {
            level().explode(this, getX() + random.nextGaussian() * 2.0, getY() + random.nextGaussian() * 2.0, getZ() + random.nextGaussian() * 2.0,
               random.nextFloat() / 2.0F, Level.ExplosionInteraction.NONE);
         }
         if (getRiftSize() <= 1) {
            completeCollapse();
            return;
         }
      }
      if (tickCount % 120 == 0) setRiftStability(getRiftStability() - 0.2F);
      if (tickCount % 600 == getId() % 600) {
         float flux = AuraHandler.getFlux(level(), blockPosition());
         double size = Math.sqrt(getRiftSize() * 2);
         if (flux >= size && getRiftSize() < 100 && getStability() != Stability.VERY_STABLE) {
            AuraHandler.drainFlux(level(), blockPosition(), (float) size, false);
            setRiftSize(getRiftSize() + 1);
         }
         if (getRiftStability() < 0.0F && random.nextInt(1000) < Math.abs(getRiftStability()) + getRiftSize()) executeRiftEvent();
      }
      if (isAlive() && tickCount % 300 == 0) {
         level().playSound(null, getX(), getY(), getZ(), ModSounds.EVILPORTAL.get(), SoundSource.NEUTRAL, (float) (0.15F + random.nextGaussian() * 0.066),
            (float) (0.75 + random.nextGaussian() * 0.1));
      }
   }

   private void clientTick() {
      if (points.size() <= 2) return;
      if (!getCollapse() && getRiftStability() < 0.0F && random.nextInt(150) < Math.abs(getRiftStability())) {
         int pi = 1 + random.nextInt(points.size() - 2);
         Vec3 v1 = points.get(pi).add(position());
         thaumcraft.client.fx.TaintFX.curlyWisp(v1.x, v1.y, v1.z, 0.1F + pointsWidth.get(pi) * 3.0F, 1.0F, 1.0F, 1.0F, 0.25F);
      }
      if (getCollapse()) {
         int pi = 1 + random.nextInt(points.size() - 2);
         Vec3 v1 = points.get(pi).add(position());
         thaumcraft.client.fx.TaintFX.curlyWisp(v1.x, v1.y, v1.z, 0.1F + pointsWidth.get(pi) * 3.0F, 1.0F, 0.3F + random.nextFloat() * 0.1F,
            0.3F + random.nextFloat() * 0.1F, 0.4F);
      }
   }

   /** createRift: opens a rift somewhere in the 16x16 column area at pos, if the flux there is high enough. */
   public static boolean createRift(Level level, BlockPos pos) {
      pos = pos.offset(level.random.nextInt(16), 0, level.random.nextInt(16));
      BlockPos p2 = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, pos);
      int top = level.getMinBuildHeight() + level.dimensionType().logicalHeight();
      if (!level.dimensionType().hasSkyLight()) {
         p2 = new BlockPos(p2.getX(), Math.max(10, level.getMinBuildHeight() + 10), p2.getZ());
         while (!level.isEmptyBlock(p2)) {
            if (p2.getY() > top - 5) return false;
            p2 = p2.above(level.random.nextInt(5) + 1);
         }
      }
      if (p2.getY() >= top - 4) return false;
      if (!level.getEntitiesOfClass(FluxRiftEntity.class, new AABB(p2).inflate(32.0)).isEmpty()) return false;
      FluxRiftEntity rift = new FluxRiftEntity(ModFlux.FLUX_RIFT.get(), level);
      rift.setRiftSeed(level.random.nextInt());
      rift.moveTo(p2.getX() + 0.5, p2.getY() + 0.5, p2.getZ() + 0.5, level.random.nextInt(360), 0.0F);
      float flux = AuraHandler.getFlux(level, p2);
      double size = Math.sqrt(flux * 3.0F);
      if (size > 5.0) {
         rift.setRiftSize((int) size);
         if (!level.addFreshEntity(rift)) return false;
         AuraHandler.drainFlux(level, p2, (float) size, false);
         for (Player target : level.getEntitiesOfClass(Player.class, new AABB(p2).inflate(32.0))) {
            if (!ResearchManager.knowsResearch(target, "f_toomuchflux")) {
               target.displayClientMessage(Component.translatable("tc.fluxevent.3").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC), true);
               ResearchManager.completeResearch(target, "f_toomuchflux");
            }
         }
         return true;
      }
      return false;
   }

   private void executeRiftEvent() {
      int total = 0;
      for (int[] e : EVENTS) total += e[1];
      int roll = random.nextInt(total);
      int[] ei = EVENTS[EVENTS.length - 1];
      for (int[] e : EVENTS) {
         roll -= e[1];
         if (roll < 0) {
            ei = e;
            break;
         }
      }
      if (ei[3] == 0 && TaintHelper.isNearTaintSeed(level(), blockPosition())) return;
      boolean didit = false;
      switch (ei[0]) {
         case 0 -> {
            WispEntity wisp = new WispEntity(ModMobs.WISP.get(), level());
            wisp.moveTo(getX() + random.nextGaussian() * 5.0, getY() + random.nextGaussian() * 5.0, getZ() + random.nextGaussian() * 5.0, 0.0F, 0.0F);
            if (level().random.nextInt(5) == 0) wisp.setWispType(Aspect.FLUX.getTag());
            if (level() instanceof ServerLevel sl
               && WispEntity.checkSpawnRules(ModMobs.WISP.get(), sl, MobSpawnType.EVENT, wisp.blockPosition(), random)
               && sl.noCollision(wisp) && !sl.containsAnyLiquid(wisp.getBoundingBox()) && sl.addFreshEntity(wisp)) {
               didit = true;
            }
         }
         case 1 -> {
            TaintSeedPrimeEntity seed = new TaintSeedPrimeEntity(ModFlux.TAINT_SEED_PRIME.get(), level());
            seed.moveTo((int) (getX() + random.nextGaussian() * 5.0) + 0.5, (int) (getY() + random.nextGaussian() * 5.0),
               (int) (getZ() + random.nextGaussian() * 5.0) + 0.5, random.nextInt(360), 0.0F);
            if (seed.canSpawnHere() && level().addFreshEntity(seed)) {
               didit = true;
               seed.boost = getRiftSize();
               FluxEffects.pollute(level(), blockPosition(), getRiftSize() / 2, true);
               discard();
            }
         }
         case 2 -> {
            for (LivingEntity target : level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(16.0))) {
               didit = true;
               if (target instanceof Player p) {
                  p.displayClientMessage(Component.translatable("tc.fluxevent.2").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC), true);
               }
               MobEffectInstance pe = new MobEffectInstance(ModFlux.INFECTIOUS_VIS_EXHAUST, 3000, 2);
               pe.getCures().clear();
               target.addEffect(pe);
            }
         }
         case 3 -> {
            Player target = level().getNearestPlayer(this, 16.0);
            if (target != null) {
               FocusPackage p = new FocusPackage(target);
               FocusMediumRoot root = new FocusMediumRoot();
               root.setupFromCasterToTarget(target, target, 0.5);
               p.addNode(root);
               FocusMediumCloud fp = new FocusMediumCloud();
               fp.initialize();
               fp.getSetting("radius").setValue(Mth.nextInt(random, 1, 3));
               fp.getSetting("duration").setValue(Mth.nextInt(random, Math.min(getRiftSize() / 2, 30), Math.min(getRiftSize(), 120)));
               p.addNode(fp);
               p.addNode(new FocusEffectFlux());
               FocusEngine.castFocusPackage(target, p, true);
            }
         }
         default -> setCollapse(true);
      }
      if (didit) setRiftStability(getRiftStability() + ei[2]);
   }

   private void calcSteps(List<Vec3> pp, List<Float> ww, Random rr) {
      pp.clear();
      ww.clear();
      Vec3 right = new Vec3(rr.nextGaussian(), rr.nextGaussian(), rr.nextGaussian()).normalize();
      Vec3 left = right.scale(-1.0);
      Vec3 lr = Vec3.ZERO;
      Vec3 ll = Vec3.ZERO;
      int steps = Mth.ceil(getRiftSize() / 3.0F);
      float girth = getRiftSize() / 300.0F;
      double angle = 0.33;
      float dec = girth / steps;
      for (int a = 0; a < steps; a++) {
         girth -= dec;
         right = right.xRot((float) (rr.nextGaussian() * angle)).yRot((float) (rr.nextGaussian() * angle));
         lr = lr.add(right.scale(0.2));
         pp.add(lr);
         ww.add(girth);
         left = left.xRot((float) (rr.nextGaussian() * angle)).yRot((float) (rr.nextGaussian() * angle));
         ll = ll.add(left.scale(0.2));
         pp.add(0, ll);
         ww.add(0, girth);
      }
      lr = lr.add(right.scale(0.1));
      pp.add(lr);
      ww.add(0.0F);
      ll = ll.add(left.scale(0.1));
      pp.add(0, ll);
      ww.add(0, 0.0F);
   }

   /** TileStabilizer.tryAddStability. */
   public void addStability() {
      setRiftStability(getRiftStability() + 0.125F);
   }

   public Stability getStability() {
      float s = getRiftStability();
      return s > 50.0F ? Stability.VERY_STABLE : s >= 0.0F ? Stability.STABLE : s > -25.0F ? Stability.UNSTABLE : Stability.VERY_UNSTABLE;
   }

   private void completeCollapse() {
      int qq = (int) Math.sqrt(maxSize);
      Item pearl = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "primordial_pearl"));
      if (pearl != Items.AIR && random.nextInt(100) < qq) {
         ItemStack ps = new ItemStack(pearl);
         if (ps.isDamageableItem()) ps.setDamageValue(4 + random.nextInt(4));
         spawnAtLocation(ps, 0.0F);
      }
      for (int a = 0; a < qq; a++) spawnAtLocation(new ItemStack(ModItems.VOID_SEED.get()), 0.0F);
      if (level() instanceof ServerLevel sl) FXHelper.bamf(sl, getX(), getY(), getZ(), -9999, true, true, null);
      List<LivingEntity> list = level().getEntitiesOfClass(LivingEntity.class, new AABB(position(), position()).inflate(32.0),
         e -> e.distanceToSqr(this) <= 32.0 * 32.0);
      Stability st = getStability();
      if (st == Stability.VERY_UNSTABLE) {
         for (LivingEntity p : list) {
            int w = (int) ((1.0 - p.distanceToSqr(this) / 32.0) * 120.0);
            if (w > 0) p.addEffect(new MobEffectInstance(ModFlux.FLUX_TAINT, w * 20, 0));
         }
      }
      if (st == Stability.VERY_UNSTABLE || st == Stability.UNSTABLE) {
         for (LivingEntity p : list) {
            int w = (int) ((1.0 - p.distanceToSqr(this) / 32.0) * 300.0);
            if (w > 0) p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, w * 20, 0));
         }
      }
      if (st != Stability.VERY_STABLE) {
         for (LivingEntity p : list) {
            if (p instanceof Player pl) {
               int w = (int) ((1.0 - p.distanceToSqr(this) / 32.0) * 25.0);
               if (w > 0) {
                  ResearchManager.addWarp(pl, w, PlayerWarp.Type.NORMAL);
                  ResearchManager.addWarp(pl, w, PlayerWarp.Type.TEMPORARY);
               }
            }
         }
      }
      discard();
   }

   @Override
   public boolean shouldRenderAtSqrDistance(double distance) {
      return distance < 64.0 * 64.0 * 4;
   }
}
