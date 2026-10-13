package thaumcraft.common.items;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import thaumcraft.common.network.ModNetwork;
import thaumcraft.common.network.SyncAuraPayload;
import thaumcraft.common.registers.ModSounds;
import thaumcraft.common.research.ScanningManager;
import thaumcraft.common.world.aura.AuraChunk;
import thaumcraft.common.world.aura.AuraHandler;

/**
 * Port of ItemThaumometer: right-click to scan the entity or block you are looking at; while held, it reads the
 * aura of the chunk you stand in (shown by the HUD). Not yet ported: the scan-highlight particles.
 */
public class ThaumometerItem extends Item {
   private static final double ENTITY_RANGE = 9.0;

   public ThaumometerItem(Properties properties) {
      super(properties);
   }

   @Override
   public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
      if (level.isClientSide) {
         drawFX(level, player);
         level.playLocalSound(player.getX(), player.getY(), player.getZ(), ModSounds.SCAN.get(), SoundSource.PLAYERS, 0.5F, 1.0F, false);
      } else if (player instanceof ServerPlayer sp) {
         doScan(level, sp);
      }
      return InteractionResultHolder.success(player.getItemInHand(hand));
   }

   @Override
   public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
      if (level.isClientSide || !(entity instanceof ServerPlayer player) || player.tickCount % 20 != 0) return;
      if (selected || player.getOffhandItem() == stack) {
         AuraChunk ac = AuraHandler.getAuraChunk(level, player.blockPosition());
         if (ac != null) ModNetwork.sendTo(player, SyncAuraPayload.of(ac));
      }
   }

   private void doScan(Level level, ServerPlayer player) {
      Entity target = getPointedEntity(level, player, ENTITY_RANGE);
      if (target != null) {
         ScanningManager.scanTheThing(player, target);
         return;
      }
      BlockHitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY);
      if (hit.getType() == HitResult.Type.BLOCK) {
         ScanningManager.scanTheThing(player, hit.getBlockPos());
      } else {
         ScanningManager.scanTheThing(player, null);
      }
   }

   private void drawFX(Level level, Player player) {
      RandomSource rand = level.random;
      Entity target = getPointedEntity(level, player, ENTITY_RANGE);
      Vec3 center;
      double spread;
      if (target != null) {
         center = target.position().add(0, target.getBbHeight() / 2.0, 0);
         spread = Math.max(target.getBbWidth(), 0.5);
      } else {
         BlockHitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY);
         if (hit.getType() != HitResult.Type.BLOCK) return;
         BlockPos pos = hit.getBlockPos();
         center = Vec3.atCenterOf(pos);
         spread = 1.0;
      }
      // Stand-in for the original "block runes" FX until the custom particle system is ported.
      for (int i = 0; i < 20; i++) {
         level.addParticle(
            ParticleTypes.ENCHANT,
            center.x + (rand.nextDouble() - 0.5) * spread, center.y + (rand.nextDouble() - 0.5) * spread, center.z + (rand.nextDouble() - 0.5) * spread,
            (rand.nextDouble() - 0.5) * 0.5, rand.nextDouble() * 0.5, (rand.nextDouble() - 0.5) * 0.5
         );
      }
   }

   /** Equivalent of EntityUtils.getPointedEntity: nearest entity (items included) on the look vector, not behind a block. */
   public static Entity getPointedEntity(Level level, Player player, double range) {
      Vec3 eye = player.getEyePosition();
      Vec3 look = player.getViewVector(1.0F);
      Vec3 end = eye.add(look.scale(range));
      AABB box = player.getBoundingBox().expandTowards(look.scale(range)).inflate(1.0);
      EntityHitResult hit = ProjectileUtil.getEntityHitResult(
         player, eye, end, box, e -> !e.isSpectator() && (e.isPickable() || e instanceof ItemEntity), range * range
      );
      if (hit == null) return null;
      BlockHitResult block = level.clip(new ClipContext(eye, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
      if (block.getType() == HitResult.Type.BLOCK && block.getLocation().distanceToSqr(eye) < hit.getLocation().distanceToSqr(eye)) {
         return null;
      }
      return hit.getEntity();
   }
}
