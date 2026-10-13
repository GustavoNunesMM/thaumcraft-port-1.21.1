package thaumcraft.common.items.casters.foci;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.casters.FocusEffect;
import thaumcraft.api.casters.NodeSetting;
import thaumcraft.api.casters.Trajectory;
import thaumcraft.common.network.FocusFXPayload;

/** Earth: a stone blast that damages and shatters soft blocks. */
public class FocusEffectEarth extends FocusEffect {
   @Override
   public String getResearch() {
      return "FOCUSELEMENTAL";
   }

   @Override
   public String getKey() {
      return "thaumcraft.EARTH";
   }

   @Override
   public Aspect getAspect() {
      return Aspect.EARTH;
   }

   @Override
   public int getComplexity() {
      return getSettingValue("power") * 3;
   }

   @Override
   public float getDamageForDisplay(float finalPower) {
      return 2 * getSettingValue("power") * finalPower;
   }

   @Override
   public boolean execute(HitResult target, Trajectory trajectory, float finalPower, int num) {
      Level world = getPackage().world;
      FocusFXPayload.impact(world, target.getLocation(), getKey());
      if (target instanceof EntityHitResult ehr) {
         Entity hit = ehr.getEntity();
         hit.hurt(FocusUtils.thrown(world, hit, getPackage().getCaster()), getDamageForDisplay(finalPower));
         return true;
      }
      if (target instanceof BlockHitResult bhr) {
         BlockPos pos = bhr.getBlockPos();
         if (getPackage().getCaster() instanceof Player p && world.getBlockState(pos).getDestroySpeed(world, pos) <= getDamageForDisplay(finalPower) / 25.0F) {
            BlockWorks.addBreaker(world, pos, world.getBlockState(pos), p, false, false, 0, 1.0F, 0.0F, 1.0F, num, 0.1F, null);
         }
      }
      return false;
   }

   @Override
   public NodeSetting[] createSettings() {
      return new NodeSetting[]{new NodeSetting("power", "focus.common.power", new NodeSetting.NodeSettingIntRange(1, 5))};
   }

   @Override
   public void renderParticleFX(Level level, double x, double y, double z, double vx, double vy, double vz) {
      thaumcraft.client.fx.FociParticles.earth(x, y, z, vx, vy, vz);
   }

   @Override
   public void onCast(Entity caster) {
      caster.level().playSound(null, caster.blockPosition().above(), SoundEvents.DRAGON_FIREBALL_EXPLODE, SoundSource.PLAYERS, 0.25F,
         1.0F + (float) (caster.level().random.nextGaussian() * 0.05F));
   }
}
