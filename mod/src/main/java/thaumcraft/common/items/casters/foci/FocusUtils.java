package thaumcraft.common.items.casters.foci;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.casters.ICaster;

/** The EntityUtils / BlockUtils helpers the foci use. */
public final class FocusUtils {
   private FocusUtils() {
   }

   /** EntityUtils.getPointedEntityRay: the nearest entity along a ray (boxes grown by 0.8), with line of sight. */
   @Nullable
   public static EntityHitResult getPointedEntityRay(Level world, @Nullable Entity ignoreEntity, Vec3 startVec, Vec3 lookVec, double minrange,
                                                     double range, float padding, boolean nonCollide) {
      EntityHitResult pointed = null;
      Vec3 end = startVec.add(lookVec.x * range, lookVec.y * range, lookVec.z * range);
      AABB bb = ignoreEntity != null ? ignoreEntity.getBoundingBox() : new AABB(startVec, startVec).inflate(0.5);
      List<Entity> list = world.getEntities(ignoreEntity, bb.expandTowards(lookVec.x * range, lookVec.y * range, lookVec.z * range).inflate(padding));
      double d2 = 0.0;
      for (Entity entity : list) {
         if (startVec.distanceTo(entity.position()) < minrange) continue;
         if (!entity.isPickable() && !nonCollide) continue;
         Vec3 eye = new Vec3(entity.getX(), entity.getY() + entity.getEyeHeight(), entity.getZ());
         if (world.clip(new ClipContext(startVec, eye, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, CollisionContextEntity.of(ignoreEntity)))
            .getType() != HitResult.Type.MISS) continue;
         float f2 = Math.max(0.8F, entity.getPickRadius());
         AABB box = entity.getBoundingBox().inflate(f2);
         Optional<Vec3> hit = box.clip(startVec, end);
         if (box.contains(startVec)) {
            if (d2 >= 0.0) {
               pointed = new EntityHitResult(entity, hit.orElse(startVec));
               d2 = 0.0;
            }
         } else if (hit.isPresent()) {
            double d3 = startVec.distanceTo(hit.get());
            if (d3 < d2 || d2 == 0.0) {
               pointed = new EntityHitResult(entity, hit.get());
               d2 = d3;
            }
         }
      }
      return pointed;
   }

   /** World.rayTraceBlocks: a block hit or null. */
   @Nullable
   public static BlockHitResult rayTraceBlocks(Level world, Vec3 start, Vec3 end, @Nullable Entity e) {
      BlockHitResult r = world.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, CollisionContextEntity.of(e)));
      return r.getType() == HitResult.Type.BLOCK ? r : null;
   }

   public static boolean isFriendly(@Nullable Entity source, @Nullable Entity target) {
      if (source == null || target == null) return false;
      if (source.getId() == target.getId()) return true;
      if (source.isPassengerOfSameVehicle(target) || source.hasPassenger(target) || target.hasPassenger(source)) return true;
      if (source.isAlliedTo(target)) return true;
      if (target instanceof OwnableEntity o && o.getOwner() != null && o.getOwner().equals(source)) return true;
      return !target.level().isClientSide && target instanceof Player && target.level() instanceof ServerLevel sl && !sl.getServer().isPvpAllowed();
   }

   public static <T extends Entity> List<T> getEntitiesInRange(Level world, double x, double y, double z, @Nullable Entity entity, Class<T> cls,
                                                               double range) {
      List<T> out = new ArrayList<>();
      for (T ent : world.getEntitiesOfClass(cls, new AABB(x, y, z, x, y, z).inflate(range))) {
         if (entity == null || entity.getId() != ent.getId()) out.add(ent);
      }
      return out;
   }

   public static <T extends Entity> List<T> getEntitiesInRangeSorted(Level world, Entity entity, Class<T> cls, double range) {
      List<T> list = getEntitiesInRange(world, entity.getX(), entity.getY(), entity.getZ(), entity, cls, range);
      list.sort(Comparator.comparingDouble(entity::distanceToSqr));
      return list;
   }

   /** Utils.isLyingInCone with the entity's look vector. */
   public static boolean isVisibleTo(float fov, Entity ent, Entity ent2, float range) {
      double[] x = {ent2.getX(), ent2.getBoundingBox().minY + ent2.getBbHeight() / 2.0F, ent2.getZ()};
      double[] t = {ent.getX(), ent.getBoundingBox().minY + ent.getEyeHeight(), ent.getZ()};
      Vec3 q = ent.getLookAngle().scale(range);
      Vec3 l = q.add(ent.getX(), ent.getBoundingBox().minY + ent.getEyeHeight(), ent.getZ());
      double[] b = {l.x, l.y, l.z};
      return isLyingInCone(x, t, b, fov);
   }

   private static boolean isLyingInCone(double[] x, double[] t, double[] b, float aperture) {
      double halfAperture = aperture / 2.0F;
      double[] apexToXVect = {t[0] - x[0], t[1] - x[1], t[2] - x[2]};
      double[] axisVect = {t[0] - b[0], t[1] - b[1], t[2] - b[2]};
      double dot = apexToXVect[0] * axisVect[0] + apexToXVect[1] * axisVect[1] + apexToXVect[2] * axisVect[2];
      double m1 = Math.sqrt(apexToXVect[0] * apexToXVect[0] + apexToXVect[1] * apexToXVect[1] + apexToXVect[2] * apexToXVect[2]);
      double m2 = Math.sqrt(axisVect[0] * axisVect[0] + axisVect[1] * axisVect[1] + axisVect[2] * axisVect[2]);
      boolean isInInfiniteCone = dot / m1 / m2 > Math.cos(halfAperture);
      if (!isInInfiniteCone) return false;
      return dot / m2 < m2;
   }

   public static boolean canEntityBeSeen(Entity looking, Entity target) {
      return rayTraceBlocks(looking.level(), new Vec3(looking.getX(), looking.getY() + looking.getBbHeight() / 2.0F, looking.getZ()),
         new Vec3(target.getX(), target.getY() + target.getBbHeight() / 2.0F, target.getZ()), looking) == null;
   }

   /** BlockUtils.isBlockExposed: some neighbour is not a full opaque block. */
   public static boolean isBlockExposed(Level world, BlockPos pos) {
      for (Direction face : Direction.values()) {
         BlockPos p = pos.relative(face);
         if (!world.getBlockState(p).isSolidRender(world, p)) return true;
      }
      return false;
   }

   /** The caster gauntlet in the entity's hands (main hand first). */
   public static ItemStack heldCaster(@Nullable LivingEntity e) {
      if (e == null) return ItemStack.EMPTY;
      if (e.getMainHandItem().getItem() instanceof ICaster) return e.getMainHandItem();
      if (e.getOffhandItem().getItem() instanceof ICaster) return e.getOffhandItem();
      return ItemStack.EMPTY;
   }

   static DamageSource source(Level level, net.minecraft.resources.ResourceKey<net.minecraft.world.damagesource.DamageType> type,
                              @Nullable Entity direct, @Nullable Entity causing) {
      return new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(type), direct, causing);
   }

   /** EntityDamageSourceIndirect("fireball").setFireDamage(). */
   static DamageSource fireball(Level level, @Nullable Entity direct, @Nullable Entity causing) {
      return source(level, DamageTypes.FIREBALL, direct, causing);
   }

   /** DamageSource.causeThrownDamage. */
   static DamageSource thrown(Level level, @Nullable Entity direct, @Nullable Entity causing) {
      return source(level, DamageTypes.THROWN, direct, causing);
   }

   /** DamageSource.causeIndirectMagicDamage. */
   static DamageSource indirectMagic(Level level, @Nullable Entity direct, @Nullable Entity causing) {
      return source(level, DamageTypes.INDIRECT_MAGIC, direct, causing);
   }

   /** CollisionContext for an optional entity. */
   static final class CollisionContextEntity {
      static net.minecraft.world.phys.shapes.CollisionContext of(@Nullable Entity e) {
         return e == null ? net.minecraft.world.phys.shapes.CollisionContext.empty() : net.minecraft.world.phys.shapes.CollisionContext.of(e);
      }
   }
}
