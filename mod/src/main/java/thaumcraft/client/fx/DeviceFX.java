package thaumcraft.client.fx;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;

/** The FXDispatcher effects used by the devices (levitator motes, sparks, vents, runes, water trails...). */
public final class DeviceFX {
   private DeviceFX() {
   }

   private static ClientLevel level() {
      return Minecraft.getInstance().level;
   }

   private static void add(FXGeneric fx) {
      Minecraft.getInstance().particleEngine.add(fx);
   }

   private static float r(int color) {
      return (color >> 16 & 255) / 255.0F;
   }

   private static float g(int color) {
      return (color >> 8 & 255) / 255.0F;
   }

   private static float b(int color) {
      return (color & 255) / 255.0F;
   }

   public static void drawLevitatorParticles(double x, double y, double z, double x2, double y2, double z2) {
      ClientLevel level = level();
      if (level == null) return;
      FXGeneric fb = new FXGeneric(level, x, y, z, x2, y2, z2);
      fb.setMaxAge(200 + level.random.nextInt(100));
      fb.setRBGColorF(0.5F, 0.5F, 0.2F);
      fb.setAlphaF(0.3F, 0.0F);
      fb.setGridSize(16);
      fb.setParticles(56, 1, 1);
      fb.setScale(2.0F, 5.0F);
      fb.setLayer(0);
      fb.setSlowDown(1.0);
      fb.setRotationSpeed(level.random.nextFloat(), level.random.nextBoolean() ? -1.0F : 1.0F);
      add(fb);
   }

   public static void spark(double x, double y, double z, float size, float r, float g, float b, float a) {
      ClientLevel level = level();
      if (level == null) return;
      FXGeneric fb = new FXGeneric(level, x, y, z, 0.0, 0.0, 0.0);
      fb.setMaxAge(5 + level.random.nextInt(5));
      fb.setAlphaF(a);
      fb.setRBGColorF(r, g, b);
      fb.setGridSize(16);
      fb.setParticles(8 + level.random.nextInt(3) * 16, 8, 1);
      fb.setScale(size);
      fb.setFlipped(level.random.nextBoolean());
      add(fb);
   }

   /** visSparkle: a mote of vis flying from one block to another. */
   public static void visSparkle(int x, int y, int z, int x2, int y2, int z2, int color) {
      ClientLevel level = level();
      if (level == null) return;
      RandomSource rand = level.random;
      Minecraft.getInstance().particleEngine.add(new EssentiaFX.Stream(level, x + rand.nextFloat(), y + rand.nextFloat(), z + rand.nextFloat(),
         x2 + 0.4 + rand.nextFloat() * 0.2F, y2 + 0.4 + rand.nextFloat() * 0.2F, z2 + 0.4 + rand.nextFloat() * 0.2F, color, 0));
   }

   public static void drawVentParticles(double x, double y, double z, double x2, double y2, double z2, int color, float scale) {
      ClientLevel level = level();
      if (level == null) return;
      FXGeneric fb = new FXGeneric(level, x, y, z, x2, y2, z2);
      fb.setMaxAge(40);
      fb.setRBGColorF(r(color), g(color), b(color));
      fb.setAlphaF(0.4F, 0.0F);
      fb.setParticles(64, 8, 1);
      fb.setScale(scale, scale * 2.5F);
      fb.setLayer(1);
      fb.setSlowDown(0.975);
      fb.setRandomMovementScale(0.0025F, 0.0F, 0.0025F);
      add(fb);
   }

   /** drawVentParticles2: the potion sprayer's mist, with the odd spark. */
   public static void drawVentParticles2(double x, double y, double z, double x2, double y2, double z2, int color, float scale) {
      drawVentParticles(x, y, z, x2, y2, z2, color, scale / 2.0F);
      ClientLevel level = level();
      if (level != null && level.random.nextInt(6) < 2) {
         FXDispatcher.drawGenericParticles(x, y, z, x2 / 2.0, y2 / 2.0, z2 / 2.0, 1.0F, 0.7F, 0.2F, 0.9F, true, 320, 16, 1,
            10 + level.random.nextInt(4), 0.5F, 0.0F, 1);
      }
   }

   /** waterTrailFx: the everfull urn's stream of water to the block it fills. */
   public static void waterTrail(BlockPos p1, BlockPos p2, int color) {
      ClientLevel level = level();
      if (level == null) return;
      Minecraft.getInstance().particleEngine.add(new EssentiaFX.Stream(level, p1.getX() + 0.5, p1.getY() + 0.66, p1.getZ() + 0.5,
         p2.getX() + 0.5, p2.getY() + 0.5, p2.getZ() + 0.5, color, 0));
   }

   public static void jarSplash(double x, double y, double z) {
      ClientLevel level = level();
      if (level == null) return;
      RandomSource rand = level.random;
      FXGeneric fb = new FXGeneric(level, x + rand.nextGaussian() * 0.075F, y, z + rand.nextGaussian() * 0.075F, rand.nextGaussian() * 0.015F,
         0.075F + rand.nextFloat() * 0.05F, rand.nextGaussian() * 0.015F);
      fb.setMaxAge(20 + rand.nextInt(10));
      fb.setRBGColorF(r(2650102), g(2650102), b(2650102));
      fb.setAlphaF(0.5F);
      fb.setLoop(false);
      fb.setParticles(73, 1, 1);
      fb.setScale(0.4F + rand.nextFloat() * 0.3F, 0.0F);
      fb.setLayer(1);
      fb.setGravity(0.3F);
      fb.setRotationSpeed(0.0F);
      add(fb);
   }

   public static void furnaceLava(int x, int y, int z, int facingX, int facingZ) {
      ClientLevel level = level();
      if (level == null) return;
      RandomSource rand = level.random;
      float qx = facingX == 0 ? (rand.nextFloat() - rand.nextFloat()) * 0.5F : facingX * rand.nextFloat();
      float qz = facingZ == 0 ? (rand.nextFloat() - rand.nextFloat()) * 0.5F : facingZ * rand.nextFloat();
      level.addParticle(ParticleTypes.LAVA, x + 0.5F + (rand.nextFloat() - rand.nextFloat()) * 0.3F + facingX, y + 0.3F,
         z + 0.5F + (rand.nextFloat() - rand.nextFloat()) * 0.3F + facingZ, 0.15F * qx, 0.2F * rand.nextFloat(), 0.15F * qz);
   }

   /** blockRunes: a glowing rune floating on the side of a block. */
   public static void blockRunes(double x, double y, double z, float r, float g, float b, int dur, float grav) {
      ClientLevel level = level();
      if (level == null) return;
      RandomSource rand = level.random;
      double ox = rand.nextFloat() * 0.2, oy = -0.3 + rand.nextFloat() * 0.6;
      FXGeneric fb = new FXGeneric(level, x + 0.5 + ox - 0.1, y + 0.5 + oy, z + 0.5 + (rand.nextBoolean() ? 0.51 : -0.51), 0, 0, 0);
      fb.setMaxAge(3 * dur);
      fb.setRBGColorF(r == 0.0F ? 1.0F : r, g, b);
      fb.setAlphaF(0.0F, 0.66F, 0.0F);
      fb.setGridSize(16);
      fb.setParticle(224 + rand.nextInt(16));
      fb.setScale((float) (0.4 + rand.nextGaussian() * 0.05F));
      fb.setGravity(grav);
      fb.setLayer(0);
      add(fb);
   }
}
