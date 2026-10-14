package thaumcraft.common.entities.projectile;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.network.syncher.SynchedEntityData;
import net.neoforged.fml.util.ObfuscationReflectionHelper;
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn;
import thaumcraft.common.registers.ModExtras;

/**
 * EntityGrapple: the arcane grappler's head. It flies for up to 30 ticks; once it hits something it stops there and
 * pulls its thrower towards it (both sides run the pull, as the client moves its own player) until they sneak. One
 * grapple per thrower: a new one removes the old.
 */
public class GrappleEntity extends ThrowableProjectile implements IEntityWithComplexSpawn {
   private static final Map<Integer, Integer> GRAPPLES_SERVER = new HashMap<>();
   private static final Map<Integer, Integer> GRAPPLES_CLIENT = new HashMap<>();
   private static Field floatingTicks;

   public InteractionHand hand = InteractionHand.MAIN_HAND;
   private boolean pulling;
   private boolean boost;
   private int prevDist;
   private int count;
   private boolean added;
   /** Client: how much the rope sways (grows while flying, settles while pulling). */
   public float ampl;

   public GrappleEntity(EntityType<? extends GrappleEntity> type, Level level) {
      super(type, level);
   }

   public GrappleEntity(Level level, LivingEntity thrower, InteractionHand hand) {
      super(ModExtras.GRAPPLE.get(), thrower, level);
      this.hand = hand;
   }

   private static Map<Integer, Integer> grapples(Level level) {
      return level.isClientSide ? GRAPPLES_CLIENT : GRAPPLES_SERVER;
   }

   /** Whether this entity has a live grapple out (ItemGrappleGun keeps its "loaded" look meanwhile). */
   public static boolean hasGrapple(Level level, Entity thrower) {
      return grapples(level).containsKey(thrower.getId());
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder builder) {
   }

   @Override
   public void writeSpawnData(RegistryFriendlyByteBuf buf) {
      buf.writeBoolean(hand == InteractionHand.MAIN_HAND);
   }

   @Override
   public void readSpawnData(RegistryFriendlyByteBuf buf) {
      hand = buf.readBoolean() ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
   }

   @Nullable
   public LivingEntity getThrower() {
      return getOwner() instanceof LivingEntity l ? l : null;
   }

   @Override
   public boolean shouldRenderAtSqrDistance(double distance) {
      return distance < 4096.0;
   }

   @Override
   public void shoot(double x, double y, double z, float velocity, float inaccuracy) {
      super.shoot(x, y, z, velocity, 0.0F);
   }

   @Override
   protected double getDefaultGravity() {
      return pulling ? 0.0 : 0.03;
   }

   public boolean isPulling() {
      return pulling;
   }

   private void startPulling() {
      pulling = true;
      setDeltaMovement(Vec3.ZERO);
   }

   @Override
   public void tick() {
      if (pulling) {
         // stuck where it hit: no flight, no further impacts
         baseTick();
      } else {
         super.tick();
      }
      LivingEntity thrower = getThrower();
      if (!pulling && !isRemoved() && (tickCount > 30 || thrower == null)) {
         if (thrower != null) grapples(level()).remove(thrower.getId());
         discard();
      }
      if (thrower == null) return;
      Map<Integer, Integer> map = grapples(level());
      if (!level().isClientSide && !isRemoved() && !added) {
         Integer old = map.get(thrower.getId());
         if (old != null && old != getId()) {
            Entity e = level().getEntity(old);
            if (e != null) e.discard();
         }
         map.put(thrower.getId(), getId());
         added = true;
      }
      Integer mine = map.get(thrower.getId());
      if (mine != null && mine != getId()) discard();

      double dis = thrower.distanceTo(this);
      if (pulling && !isRemoved()) {
         if (thrower.isShiftKeyDown()) {
            map.remove(thrower.getId());
            discard();
         } else {
            if (thrower instanceof ServerPlayer sp) resetFloating(sp.connection);
            thrower.fallDistance = 0.0F;
            double mx = getX() - thrower.getX();
            double my = getY() - thrower.getY();
            double mz = getZ() - thrower.getZ();
            double dd = dis;
            if (dis < 8.0) dd = dis * (8.0 - dis);
            dd = Math.max(1.0E-9, dd);
            mx /= dd * 5.0;
            my /= dd * 5.0;
            mz /= dd * 5.0;
            Vec3 v2 = new Vec3(mx, my, mz);
            if (v2.length() > 0.25) {
               v2 = v2.normalize();
               mx = v2.x / 4.0;
               my = v2.y / 4.0;
               mz = v2.z / 4.0;
            }
            Vec3 m = thrower.getDeltaMovement().add(mx, my + 0.033, mz);
            if (!boost) {
               m = m.add(0, 0.4F, 0);
               boost = true;
            }
            thrower.setDeltaMovement(m);
            int d = (int) (dis / 2.0);
            count = d == prevDist ? count + 1 : 0;
            prevDist = d;
         }
      }
      if (level().isClientSide) {
         if (!pulling) ampl += 0.02F;
         else ampl *= 0.66F;
      }
   }

   private static void resetFloating(ServerGamePacketListenerImpl connection) {
      try {
         if (floatingTicks == null) floatingTicks = ObfuscationReflectionHelper.findField(ServerGamePacketListenerImpl.class, "aboveGroundTickCount");
         floatingTicks.setInt(connection, 0);
      } catch (Exception ignored) {
         // not fatal: only matters on servers that kick floating players
      }
   }

   @Override
   public void handleEntityEvent(byte id) {
      if (id == 6) startPulling();
      else super.handleEntityEvent(id);
   }

   @Override
   protected void onHit(HitResult mop) {
      if (!level().isClientSide && !pulling) {
         startPulling();
         Vec3 at = mop.getLocation();
         setPos(at.x, at.y, at.z);
         level().broadcastEntityEvent(this, (byte) 6);
      }
   }

   @Override
   public void remove(RemovalReason reason) {
      LivingEntity thrower = getThrower();
      if (thrower != null) {
         Map<Integer, Integer> map = grapples(level());
         Integer mine = map.get(thrower.getId());
         if (mine != null && mine == getId()) map.remove(thrower.getId());
      }
      super.remove(reason);
   }
}
