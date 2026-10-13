package thaumcraft.common.items.casters.foci;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.casters.FocusEffect;
import thaumcraft.api.casters.NodeSetting;
import thaumcraft.api.casters.Trajectory;
import thaumcraft.common.network.FocusFXPayload;

/** Fire: burns and sets alight; may start fires on blocks. */
public class FocusEffectFire extends FocusEffect {
   @Override
   public String getResearch() {
      return "BASEAUROMANCY";
   }

   @Override
   public String getKey() {
      return "thaumcraft.FIRE";
   }

   @Override
   public Aspect getAspect() {
      return Aspect.FIRE;
   }

   @Override
   public int getComplexity() {
      return getSettingValue("duration") + getSettingValue("power") * 2;
   }

   @Override
   public float getDamageForDisplay(float finalPower) {
      return (3 + getSettingValue("power")) * finalPower;
   }

   @Override
   public boolean execute(HitResult target, Trajectory trajectory, float finalPower, int num) {
      Level world = getPackage().world;
      FocusFXPayload.impact(world, target.getLocation(), getKey());
      if (target instanceof EntityHitResult ehr) {
         Entity hit = ehr.getEntity();
         if (hit.fireImmune()) return false;
         float fire = 1 + getSettingValue("duration") * getSettingValue("duration");
         float damage = getDamageForDisplay(finalPower);
         fire *= finalPower;
         hit.hurt(FocusUtils.fireball(world, hit, getPackage().getCaster()), damage);
         if (fire > 0.0F) hit.igniteForSeconds(Math.round(fire));
         return true;
      }
      if (target instanceof BlockHitResult bhr && getSettingValue("duration") > 0) {
         BlockPos pos = bhr.getBlockPos().relative(bhr.getDirection());
         if (world.isEmptyBlock(pos) && world.random.nextFloat() < finalPower) {
            world.playSound(null, pos, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 1.0F, world.random.nextFloat() * 0.4F + 0.8F);
            world.setBlock(pos, Blocks.FIRE.defaultBlockState(), 11);
            return true;
         }
      }
      return false;
   }

   @Override
   public NodeSetting[] createSettings() {
      return new NodeSetting[]{new NodeSetting("power", "focus.common.power", new NodeSetting.NodeSettingIntRange(1, 5)),
         new NodeSetting("duration", "focus.fire.burn", new NodeSetting.NodeSettingIntRange(0, 5))};
   }

   @Override
   public void renderParticleFX(Level level, double x, double y, double z, double vx, double vy, double vz) {
      thaumcraft.client.fx.FociParticles.fire(x, y, z, vx, vy, vz);
   }

   @Override
   public void onCast(Entity caster) {
      caster.level().playSound(null, caster.blockPosition().above(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1.0F,
         1.0F + (float) (caster.level().random.nextGaussian() * 0.05F));
   }
}
