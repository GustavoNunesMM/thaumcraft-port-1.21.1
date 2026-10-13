package thaumcraft.common.entities.monster.tainted;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.entities.ITaintedMob;
import thaumcraft.common.blocks.world.taint.TaintHelper;
import thaumcraft.common.items.VisCrystalItem;

/** What the tainted creatures share: they never attack each other and drop vitium crystals (ConfigItems.FLUX_CRYSTAL). */
public abstract class TaintedMonster extends Monster implements ITaintedMob {
   protected TaintedMonster(EntityType<? extends Monster> type, Level level) {
      super(type, level);
   }

   @Override
   public boolean canAttack(LivingEntity target) {
      return !(target instanceof ITaintedMob) && super.canAttack(target);
   }

   @Override
   public boolean isAlliedTo(Entity other) {
      return other instanceof ITaintedMob || super.isAlliedTo(other);
   }

   protected void dropFluxCrystal() {
      spawnAtLocation(VisCrystalItem.of(Aspect.FLUX, 1), getBbHeight() / 2.0F);
   }

   /** The block at or under the position is taint (ThaumcraftMaterials.MATERIAL_TAINT). */
   public static boolean onTaint(Level level, BlockPos pos) {
      return TaintHelper.isTaint(level.getBlockState(pos)) || TaintHelper.isTaint(level.getBlockState(pos.below()));
   }

   /** spawnTentacles: a tendril bursts out of the taint under the target. (The eldritch biome case comes with that milestone.) */
   protected void spawnTentacles(Entity entity) {
      if (onTaint(level(), entity.blockPosition())) {
         TaintacleSmallEntity taintlet = new TaintacleSmallEntity(thaumcraft.common.registers.ModFlux.TAINTACLE_TINY.get(), level());
         taintlet.moveTo(entity.getX() + random.nextFloat() - random.nextFloat(), entity.getY(), entity.getZ() + random.nextFloat() - random.nextFloat(),
            0.0F, 0.0F);
         level().addFreshEntity(taintlet);
         playSound(thaumcraft.common.registers.ModSounds.TENTACLE.get(), getSoundVolume(), getVoicePitch());
      }
   }
}
