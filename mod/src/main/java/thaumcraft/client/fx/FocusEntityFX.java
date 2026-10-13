package thaumcraft.client.fx;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import thaumcraft.api.casters.FocusEffect;
import thaumcraft.api.casters.FocusEngine;
import thaumcraft.common.entities.projectile.FocusCloudEntity;
import thaumcraft.common.entities.projectile.FocusMineEntity;
import thaumcraft.common.entities.projectile.FocusProjectileEntity;

/** Client particles of the focus entities and the focal manipulator. */
public final class FocusEntityFX {
   private FocusEntityFX() {
   }

   /** EntityFocusMine (armed): a puff of one of its effects every 5 ticks. */
   public static void mine(FocusMineEntity e) {
      if (e.effects == null) e.effects = e.focusPackage.getFocusEffects();
      if (e.effects.length == 0) return;
      RandomSource r = FocusFX.rand();
      FocusEffect eff = e.effects[r.nextInt(e.effects.length)];
      eff.renderParticleFX(e.level(), e.getX() + r.nextGaussian() * 0.1, e.getY() + r.nextGaussian() * 0.1, e.getZ() + r.nextGaussian() * 0.1,
         r.nextGaussian() * 0.01F, r.nextGaussian() * 0.01F, r.nextGaussian() * 0.01F);
   }

   /** EntityFocusCloud: coloured puffs and effect particles through the cloud. */
   public static void cloud(FocusCloudEntity e) {
      if (e.effects == null) e.effects = e.focusPackage.getFocusEffects();
      if (e.effects.length == 0) return;
      RandomSource r = FocusFX.rand();
      float rad = e.getRadius();
      for (int a = 0; a < rad; a++) {
         FocusEffect eff = e.effects[r.nextInt(e.effects.length)];
         FocusFX.drawFocusCloudParticle(e.getX() + r.nextGaussian() * rad / 2.0 * 0.85, e.getY() + r.nextGaussian() * rad / 2.0 * 0.85,
            e.getZ() + r.nextGaussian() * rad / 2.0 * 0.85, r.nextGaussian() * 0.01, r.nextGaussian() * 0.01, r.nextGaussian() * 0.01,
            FocusEngine.getElementColor(eff.getKey()));
         eff.renderParticleFX(e.level(), e.getX() + r.nextGaussian() * rad / 2.0, e.getY() + r.nextGaussian() * rad / 2.0,
            e.getZ() + r.nextGaussian() * rad / 2.0, r.nextGaussian() * 0.01F, r.nextGaussian() * 0.01F, r.nextGaussian() * 0.01F);
      }
   }

   /** EntityFocusProjectile.renderParticle: a fire mote of an effect's colour, plus one effect particle per tick. */
   public static void projectile(FocusProjectileEntity e, float coeff) {
      e.lastRenderTick = coeff;
      if (e.effects == null) e.effects = e.focusPackage.getFocusEffects();
      if (e.effects.length == 0) return;
      RandomSource r = FocusFX.rand();
      FocusEffect eff = e.effects[r.nextInt(e.effects.length)];
      int c = FocusEngine.getElementColor(eff.getKey());
      double px = e.xo + (e.getX() - e.xo) * coeff, py = e.yo + (e.getY() - e.yo) * coeff + e.getBbHeight() / 2.0F, pz = e.zo + (e.getZ() - e.zo) * coeff;
      FocusFX.drawFireMote((float) px, (float) py, (float) pz, 0.0125F * (r.nextFloat() - 0.5F), 0.0125F * (r.nextFloat() - 0.5F),
         0.0125F * (r.nextFloat() - 0.5F), (c >> 16 & 255) / 255.0F, (c >> 8 & 255) / 255.0F, (c & 255) / 255.0F, 0.5F, 7.0F);
      if (e.firstParticle) {
         e.firstParticle = false;
         eff.renderParticleFX(e.level(), px + r.nextGaussian() * 0.1F, py + r.nextGaussian() * 0.1F, pz + r.nextGaussian() * 0.1F,
            r.nextGaussian() * 0.01F, r.nextGaussian() * 0.01F, r.nextGaussian() * 0.01F);
      }
   }

   /** TileFocalManipulator while crafting: sparkles above the table. */
   public static void manipulatorCrafting(Level level, BlockPos pos) {
      RandomSource r = level.random;
      FocusFX.generic(pos.getX() + 0.5 + (r.nextFloat() - r.nextFloat()) * 0.3F, pos.getY() + 1.4 + (r.nextFloat() - r.nextFloat()) * 0.3F,
         pos.getZ() + 0.5 + (r.nextFloat() - r.nextFloat()) * 0.3F, 0.0, 0.0, 0.0, 0.5F + r.nextFloat() * 0.4F, 1.0F - r.nextFloat() * 0.4F,
         1.0F - r.nextFloat() * 0.4F, 0.8F, false, 448, 9, 1, 6 + r.nextInt(5), 0, 0.3F + r.nextFloat() * 0.3F, 0.0F, 0);
   }

   /** Block event 5: vis drawn in from the aura around. */
   public static void manipulatorSparkle(Level level, BlockPos pos) {
      RandomSource r = level.random;
      FocusFX.visSparkle(pos.getX() + r.nextInt(3) - r.nextInt(3), pos.getY() + r.nextInt(3), pos.getZ() + r.nextInt(3) - r.nextInt(3), pos.getX(),
         pos.getY() + 1, pos.getZ(), 0x33CC33);
   }
}
