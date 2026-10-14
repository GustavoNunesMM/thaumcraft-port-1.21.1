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
 * Walks "depth * power" blocks against the hit side and opens a tunnel of dimensional tears (HoleBlock). Fizzles in
 * the Outer Lands.
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
      if (!(target instanceof BlockHitResult bhr) || bhr.getType() != HitResult.Type.BLOCK) return false;
      net.minecraft.world.level.Level world = getPackage().world;
      if (thaumcraft.common.world.eldritch.OuterLands.isOuterLands(world)) {
         world.playSound(null, bhr.getBlockPos(), ModSounds.WANDFAIL.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
         return false;
      }
      float maxdis = getSettingValue("depth") * finalPower;
      int dur = 20 * getSettingValue("duration");
      int distance;
      net.minecraft.core.BlockPos pos = bhr.getBlockPos();
      for (distance = 0; distance < maxdis; distance++) {
         net.minecraft.world.level.block.state.BlockState bi = world.getBlockState(pos);
         if (thaumcraft.common.blocks.eldritch.HoleBlock.isBlacklisted(bi) || bi.is(net.minecraft.world.level.block.Blocks.BEDROCK)
            || bi.is(thaumcraft.common.registers.ModEldritch.HOLE.get()) || bi.isAir() || bi.getDestroySpeed(world, pos) == -1.0F) break;
         pos = pos.relative(bhr.getDirection().getOpposite());
      }
      thaumcraft.common.blocks.eldritch.HoleBlock.createHole(world, bhr.getBlockPos(), bhr.getDirection(), (byte) Math.round((float) (distance + 1)), dur);
      return true;
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
