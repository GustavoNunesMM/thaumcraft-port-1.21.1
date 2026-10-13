package thaumcraft.common.items.casters.foci;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.casters.FocusEffect;
import thaumcraft.api.casters.NodeSetting;
import thaumcraft.api.casters.Trajectory;
import thaumcraft.common.registers.ModSounds;

/**
 * Rift: shifts blocks into another reality for a while (a portable hole).
 * STUB: the hole block (BlockHole/TileHole) and the Outer Lands come with later milestones; until then the effect
 * only fizzles (the wandfail sound the original plays where holes cannot be made).
 */
public class FocusEffectRift extends FocusEffect {
   @Override
   public String getResearch() {
      return "FOCUSRIFT";
   }

   @Override
   public String getKey() {
      return "thaumcraft.RIFT";
   }

   @Override
   public Aspect getAspect() {
      return Aspect.ELDRITCH;
   }

   @Override
   public int getComplexity() {
      return 3 + getSettingValue("duration") / 2 + getSettingValue("depth") / 4;
   }

   @Override
   public boolean execute(HitResult target, Trajectory trajectory, float finalPower, int num) {
      if (!(target instanceof BlockHitResult bhr)) return false;
      // TODO(hole milestone): walk "depth * finalPower" blocks against the hit side and call createHole as FocusEffectRift did.
      getPackage().world.playSound(null, bhr.getBlockPos(), ModSounds.WANDFAIL.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
      return false;
   }

   @Override
   public NodeSetting[] createSettings() {
      return new NodeSetting[]{new NodeSetting("depth", "focus.rift.depth",
         new NodeSetting.NodeSettingIntList(new int[]{8, 16, 24, 32}, new String[]{"8", "16", "24", "32"})),
         new NodeSetting("duration", "focus.common.duration", new NodeSetting.NodeSettingIntRange(2, 10))};
   }

   @Override
   public void renderParticleFX(Level level, double x, double y, double z, double vx, double vy, double vz) {
      thaumcraft.client.fx.FociParticles.rift(x, y, z, vx, vy, vz);
   }

   @Override
   public void onCast(Entity caster) {
      caster.level().playSound(null, caster.blockPosition().above(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.2F, 0.7F);
   }
}
