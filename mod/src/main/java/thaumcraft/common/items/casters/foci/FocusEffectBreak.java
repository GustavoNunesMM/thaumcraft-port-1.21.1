package thaumcraft.common.items.casters.foci;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.casters.FocusEffect;
import thaumcraft.api.casters.NodeSetting;
import thaumcraft.api.casters.Trajectory;
import thaumcraft.common.network.FocusFXPayload;

/** Break: mines the block hit over a few ticks, optionally with fortune or silk touch. */
public class FocusEffectBreak extends FocusEffect {
   @Override
   public String getResearch() {
      return "FOCUSBREAK";
   }

   @Override
   public String getKey() {
      return "thaumcraft.BREAK";
   }

   @Override
   public Aspect getAspect() {
      return Aspect.ENTROPY;
   }

   @Override
   public int getComplexity() {
      return getSettingValue("power") * 3 + getSettingValue("silk") * 4
         + (getSettingValue("fortune") == 0 ? 0 : (getSettingValue("fortune") + 1) * 3);
   }

   @Override
   public boolean execute(HitResult target, Trajectory trajectory, float finalPower, int num) {
      if (target instanceof BlockHitResult bhr) {
         Level world = getPackage().world;
         FocusFXPayload.impact(world, Vec3.atCenterOf(bhr.getBlockPos()), getKey());
         boolean silk = getSettingValue("silk") > 0;
         int fortune = getSettingValue("fortune");
         float strength = getSettingValue("power") * finalPower;
         float dur = (float) Math.sqrt(world.getBlockState(bhr.getBlockPos()).getDestroySpeed(world, bhr.getBlockPos()) * 100.0F);
         if (getPackage().getCaster() instanceof Player p) {
            BlockWorks.addBreaker(world, bhr.getBlockPos(), world.getBlockState(bhr.getBlockPos()), p, true, silk, fortune, strength, dur, dur,
               (int) (dur / strength / 3.0F * num), 0.25F + (silk ? 0.25F : 0.0F) + fortune * 0.1F, null);
         }
      }
      return true;
   }

   @Override
   public NodeSetting[] createSettings() {
      int[] silk = {0, 1};
      String[] silkDesc = {"focus.common.no", "focus.common.yes"};
      int[] fortune = {0, 1, 2, 3, 4};
      String[] fortuneDesc = {"focus.common.no", "I", "II", "III", "IV"};
      return new NodeSetting[]{new NodeSetting("power", "focus.break.power", new NodeSetting.NodeSettingIntRange(1, 5)),
         new NodeSetting("fortune", "focus.common.fortune", new NodeSetting.NodeSettingIntList(fortune, fortuneDesc)),
         new NodeSetting("silk", "focus.common.silk", new NodeSetting.NodeSettingIntList(silk, silkDesc))};
   }

   @Override
   public void renderParticleFX(Level level, double x, double y, double z, double vx, double vy, double vz) {
      thaumcraft.client.fx.FociParticles.breaking(x, y, z, vx, vy, vz);
   }

   @Override
   public void onCast(Entity caster) {
      caster.level().playSound(null, caster.blockPosition().above(), SoundEvents.END_GATEWAY_SPAWN, SoundSource.PLAYERS, 0.1F,
         2.0F + (float) (caster.level().random.nextGaussian() * 0.05F));
   }
}
