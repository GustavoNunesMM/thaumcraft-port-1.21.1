package thaumcraft.client.fx;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.RandomSource;

/** The renderParticleFX bodies of the focus effects (client only; the effects call these with plain numbers). */
public final class FociParticles {
   private FociParticles() {
   }

   private static ClientLevel w() {
      return FocusFX.level();
   }

   private static RandomSource r() {
      return FocusFX.rand();
   }

   public static void fire(double x, double y, double z, double mx, double my, double mz) {
      FocusFX.GenPart pp = new FocusFX.GenPart();
      pp.grav = -0.2F;
      pp.age = 10;
      pp.alpha = new float[]{0.7F};
      pp.partStart = 640;
      pp.partInc = 1;
      pp.partNum = 10;
      pp.slowDown = 0.75;
      pp.scale = new float[]{(float) (1.5 + r().nextGaussian() * 0.2F)};
      FocusFX.drawGenericParticles(x, y, z, mx, my, mz, pp);
   }

   public static void frost(double x, double y, double z, double mx, double my, double mz) {
      FXGeneric fb = new FXGeneric(w(), x, y, z, mx, my, mz);
      fb.setMaxAge(40 + r().nextInt(40));
      fb.setAlphaF(1.0F, 0.0F);
      fb.setParticles(8, 1, 1);
      fb.setGravity(0.033F);
      fb.setSlowDown(0.8);
      fb.setRandomMovementScale(0.0025F, 1.0E-4F, 0.0025F);
      fb.setScale((float) (0.7F + r().nextGaussian() * 0.3F));
      fb.setRotationSpeed(r().nextFloat() * 3.0F, (float) r().nextGaussian() / 4.0F);
      FocusFX.add(fb);
   }

   public static void air(double x, double y, double z, double mx, double my, double mz) {
      FocusFX.GenPart pp = new FocusFX.GenPart();
      pp.grav = -0.1F;
      pp.age = 20 + r().nextInt(10);
      pp.alpha = new float[]{0.5F, 0.0F};
      pp.grid = 32;
      pp.partStart = 337;
      pp.partInc = 1;
      pp.partNum = 5;
      pp.slowDown = 0.75;
      pp.rot = (float) r().nextGaussian() / 2.0F;
      float s = (float) (2.0 + r().nextGaussian() * 0.5);
      pp.scale = new float[]{s, s * 2.0F};
      FocusFX.drawGenericParticles(x, y, z, mx, my, mz, pp);
   }

   public static void earth(double x, double y, double z, double mx, double my, double mz) {
      FocusFX.GenPart pp = new FocusFX.GenPart();
      pp.grav = 0.4F;
      pp.layer = 1;
      pp.age = 20 + r().nextInt(10);
      pp.alpha = new float[]{1.0F, 0.0F};
      pp.partStart = 75 + r().nextInt(4);
      pp.partInc = 1;
      pp.partNum = 1;
      pp.slowDown = 0.9;
      pp.rot = (float) r().nextGaussian();
      float s = (float) (1.0 + r().nextGaussian() * 0.2F);
      pp.scale = new float[]{s, s / 2.0F};
      FocusFX.drawGenericParticles(x, y, z, mx, my, mz, pp);
   }

   public static void breaking(double x, double y, double z, double mx, double my, double mz) {
      FXGeneric fb = new FXGeneric(w(), x, y, z, mx, my, mz);
      fb.setMaxAge(6 + r().nextInt(6));
      int q = r().nextInt(4);
      fb.setParticles(704 + q * 3, 3, 1);
      fb.setSlowDown(0.8);
      fb.setScale((float) (1.7F + r().nextGaussian() * 0.3F));
      FocusFX.add(fb);
   }

   public static void exchange(double x, double y, double z, double vx, double vy, double vz) {
      FXGeneric fb = new FXGeneric(w(), x, y, z, vx + r().nextGaussian() * 0.01, vy + r().nextGaussian() * 0.01, vz + r().nextGaussian() * 0.01);
      fb.setMaxAge(9);
      fb.setRBGColorF(0.25F + r().nextFloat() * 0.25F, 0.25F + r().nextFloat() * 0.25F, 0.25F + r().nextFloat() * 0.25F);
      fb.setAlphaF(0.0F, 0.6F, 0.6F, 0.0F);
      fb.setGridSize(64);
      fb.setParticles(448, 9, 1);
      fb.setScale(0.5F, 0.25F);
      fb.setGravity((float) (r().nextGaussian() * 0.01F));
      fb.setRandomMovementScale(0.0025F, 0.0025F, 0.0025F);
      FocusFX.add(fb);
   }

   public static void heal(double x, double y, double z, double vx, double vy, double vz) {
      FXGeneric fb = new FXGeneric(w(), x, y, z, vx + r().nextGaussian() * 0.01, vy + r().nextGaussian() * 0.01, vz + r().nextGaussian() * 0.01);
      fb.setMaxAge((int) (10.0F + 10.0F * r().nextFloat()));
      fb.setRBGColorF(1.0F, 1.0F, 1.0F);
      fb.setAlphaF(0.0F, 0.7F, 0.7F, 0.0F);
      fb.setGridSize(64);
      fb.setParticles(0, 1, 1);
      fb.setScale(r().nextFloat() * 2.0F, r().nextFloat());
      fb.setSlowDown(0.8);
      fb.setGravity((float) (r().nextGaussian() * 0.1F));
      fb.setRandomMovementScale(0.0125F, 0.0125F, 0.0125F);
      fb.setRotationSpeed((float) r().nextGaussian());
      FocusFX.addWithDelay(fb, r().nextInt(4));
   }

   public static void curse(double x, double y, double z, double mx, double my, double mz) {
      FXGeneric fb = new FXGeneric(w(), x, y, z, mx, my, mz);
      fb.setMaxAge(8);
      fb.setRBGColorF(0.41F + r().nextFloat() * 0.2F, 0.0F, 0.019F + r().nextFloat() * 0.2F);
      fb.setAlphaF(0.0F, r().nextFloat(), r().nextFloat(), r().nextFloat(), 0.0F);
      fb.setGridSize(16);
      fb.setParticles(72 + r().nextInt(4), 1, 1);
      fb.setScale(2.0F + r().nextFloat() * 4.0F);
      fb.setLoop(false);
      fb.setSlowDown(0.9);
      fb.setGravity(0.0F);
      fb.setRotationSpeed(r().nextFloat(), 0.0F);
      FocusFX.addWithDelay(fb, r().nextInt(4));
   }

   public static void flux(double x, double y, double z, double vx, double vy, double vz) {
      FXGeneric fb = new FXGeneric(w(), x, y, z, vx + r().nextGaussian() * 0.01, vy + r().nextGaussian() * 0.01, vz + r().nextGaussian() * 0.01);
      fb.setMaxAge((int) (15.0F + 10.0F * r().nextFloat()));
      fb.setRBGColorF(0.25F + r().nextFloat() * 0.25F, 0.0F, 0.25F + r().nextFloat() * 0.25F);
      fb.setAlphaF(0.0F, 1.0F, 1.0F, 0.0F);
      fb.setGridSize(64);
      fb.setParticles(128, 14, 1);
      fb.setScale(2.0F + r().nextFloat(), 0.25F + r().nextFloat() * 0.25F);
      fb.setLoop(true);
      fb.setSlowDown(0.9);
      fb.setGravity((float) (r().nextGaussian() * 0.1F));
      fb.setRandomMovementScale(0.0125F, 0.0125F, 0.0125F);
      fb.setRotationSpeed((float) r().nextGaussian());
      FocusFX.addWithDelay(fb, r().nextInt(4));
   }

   public static void rift(double x, double y, double z, double mx, double my, double mz) {
      FXGeneric fb = new FXGeneric(w(), x, y, z, mx, my, mz);
      fb.setMaxAge(16 + r().nextInt(16));
      fb.setParticles(384 + r().nextInt(16), 1, 1);
      fb.setSlowDown(0.75);
      fb.setAlphaF(1.0F, 0.0F);
      fb.setScale((float) (0.7F + r().nextGaussian() * 0.3F));
      fb.setRBGColorF(0.25F, 0.25F, 1.0F);
      fb.setRandomMovementScale(0.01F, 0.01F, 0.01F);
      FocusFX.add(fb);
   }

   /** BlockEffect (sap) randomDisplayTick. */
   public static void sap(double x, double y, double z) {
      float h = r().nextFloat() * 0.33F;
      FocusFX.spark(x + r().nextFloat(), y + 0.1515F + h / 2.0F, z + r().nextFloat(), 3.0F + h * 6.0F, 0.3F - r().nextFloat() * 0.1F, 0.0F,
         0.5F + r().nextFloat() * 0.2F, 1.0F);
   }
}
