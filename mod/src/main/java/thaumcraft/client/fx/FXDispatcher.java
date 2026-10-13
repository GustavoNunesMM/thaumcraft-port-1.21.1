package thaumcraft.client.fx;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.registers.ModSounds;

/** FXDispatcher: the client effects of the original, ported as their features are. */
public final class FXDispatcher {
   private FXDispatcher() {
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

   public static void drawNitorCore(double x, double y, double z, double x2, double y2, double z2) {
      ClientLevel level = level();
      FXGeneric fb = new FXGeneric(level, x, y, z, x2, y2, z2);
      fb.setMaxAge(10);
      fb.setRBGColorF(1.0F, 1.0F, 1.0F);
      fb.setAlphaF(1.0F);
      fb.setParticles(457, 1, 1);
      fb.setScale(1.0F, 1.0F + (float) level.random.nextGaussian() * 0.1F, 1.0F);
      fb.setLayer(1);
      fb.setRandomMovementScale(2.0E-4F, 2.0E-4F, 2.0E-4F);
      add(fb);
   }

   public static void drawNitorFlames(double x, double y, double z, double x2, double y2, double z2, int color) {
      ClientLevel level = level();
      FXGeneric fb = new FXGeneric(level, x, y, z, x2, y2, z2);
      fb.setMaxAge(10 + level.random.nextInt(5));
      fb.setRBGColorF(r(color), g(color), b(color));
      fb.setAlphaF(0.66F);
      fb.setLoop(true);
      fb.setGridSize(64);
      fb.setParticles(264, 8, 1);
      fb.setScale(3.0F + level.random.nextFloat(), 0.05F);
      fb.setRandomMovementScale(0.0025F, 0.0F, 0.0025F);
      add(fb);
   }

   /** drawAlumentum (FXFireMote: a shrinking glowing mote, frame 7). */
   public static void drawAlumentum(float x, float y, float z, float vx, float vy, float vz, float r, float g, float b, float alpha, float scale) {
      FXGeneric fb = new FXGeneric(level(), x, y, z, vx, vy, vz);
      fb.setMaxAge(16);
      fb.setRBGColorF(r, g, b);
      fb.setAlphaF(alpha);
      fb.setParticle(7);
      fb.setScale(scale, 0.0F);
      fb.setLayer(0);
      fb.setSlowDown(1.0);
      add(fb);
   }

   public static void drawGenericParticles(double x, double y, double z, double x2, double y2, double z2, float r, float g, float b, float alpha,
                                           boolean loop, int start, int num, int inc, int age, float scale, float rot, int layer) {
      FXGeneric fb = new FXGeneric(level(), x, y, z, x2, y2, z2);
      fb.setMaxAge(age);
      fb.setRBGColorF(r, g, b);
      fb.setAlphaF(alpha);
      fb.setLoop(loop);
      fb.setParticles(start, num, inc);
      fb.setScale(scale);
      fb.setLayer(layer);
      fb.setRotationSpeed(rot);
      add(fb);
   }

   public static void crucibleBubble(float x, float y, float z, float cr, float cg, float cb) {
      ClientLevel level = level();
      FXGeneric fb = new FXGeneric(level, x, y, z);
      fb.setMaxAge(15 + level.random.nextInt(10));
      fb.setScale(level.random.nextFloat() * 0.3F + 0.3F);
      fb.setRBGColorF(cr, cg, cb);
      fb.setRandomMovementScale(0.002F, 0.002F, 0.002F);
      fb.setGravity(-0.001F);
      fb.setParticle(64);
      fb.setFinalFrames(65, 66, 66);
      add(fb);
   }

   public static void crucibleFroth(float x, float y, float z) {
      ClientLevel level = level();
      FXGeneric fb = new FXGeneric(level, x, y, z);
      fb.setMaxAge(4 + level.random.nextInt(3));
      fb.setScale(level.random.nextFloat() * 0.2F + 0.2F);
      fb.setRBGColorF(0.5F, 0.5F, 0.7F);
      fb.setRandomMovementScale(0.001F, 0.001F, 0.001F);
      fb.setGravity(0.1F);
      fb.setParticle(64);
      fb.setFinalFrames(65, 66);
      add(fb);
   }

   public static void crucibleFrothDown(float x, float y, float z) {
      ClientLevel level = level();
      FXGeneric fb = new FXGeneric(level, x, y, z);
      fb.setMaxAge(12 + level.random.nextInt(12));
      fb.setScale(level.random.nextFloat() * 0.2F + 0.4F);
      fb.setRBGColorF(0.25F, 0.0F, 0.75F);
      fb.setAlphaF(0.8F);
      fb.setRandomMovementScale(0.001F, 0.001F, 0.001F);
      fb.setGravity(0.05F);
      fb.setNoClip(false);
      fb.setParticle(73);
      fb.setFinalFrames(65, 66);
      fb.setLayer(1);
      add(fb);
   }

   public static void crucibleBoil(BlockPos pos, float fluidHeight, AspectList aspects, int j) {
      ClientLevel level = level();
      RandomSource rand = level.random;
      for (int a = 0; a < 2; a++) {
         FXGeneric fb = new FXGeneric(level, pos.getX() + 0.2F + rand.nextFloat() * 0.6F, pos.getY() + 0.1F + fluidHeight,
            pos.getZ() + 0.2F + rand.nextFloat() * 0.6F, 0.0, 0.002, 0.0);
         fb.setMaxAge((int) (7.0 + 8.0 / (Math.random() * 0.8 + 0.2)));
         fb.setScale(rand.nextFloat() * 0.3F + 0.2F);
         if (aspects.isEmpty()) {
            fb.setRBGColorF(1.0F, 1.0F, 1.0F);
         } else {
            int c = aspects.getAspects()[rand.nextInt(aspects.size())].getColor();
            fb.setRBGColorF(r(c), g(c), b(c));
         }
         fb.setRandomMovementScale(0.001F, 0.001F, 0.001F);
         fb.setGravity(-0.025F * j);
         fb.setParticle(64);
         fb.setFinalFrames(65, 66);
         add(fb);
      }
   }

   public static void drawWispyMotes(double x, double y, double z, double vx, double vy, double vz, int age, float grav) {
      RandomSource rand = level().random;
      drawWispyMotes(x, y, z, vx, vy, vz, age, 0.25F + rand.nextFloat() * 0.75F, 0.25F + rand.nextFloat() * 0.75F,
         0.25F + rand.nextFloat() * 0.75F, grav);
   }

   public static void drawWispyMotes(double x, double y, double z, double vx, double vy, double vz, int age, float r, float g, float b, float grav) {
      ClientLevel level = level();
      FXGeneric fb = new FXGeneric(level, x, y, z, vx, vy, vz);
      fb.setMaxAge((int) (age + age / 2 * level.random.nextFloat()));
      fb.setRBGColorF(r, g, b);
      fb.setAlphaF(0.0F, 0.6F, 0.6F, 0.0F);
      fb.setGridSize(64);
      fb.setParticles(512, 16, 1);
      fb.setScale(1.0F, 0.5F);
      fb.setLoop(true);
      fb.setWind(0.001);
      fb.setGravity(grav);
      fb.setRandomMovementScale(0.0025F, 0.0F, 0.0025F);
      add(fb);
   }

   public static void drawBamf(double x, double y, double z, boolean sound, boolean flair, Direction side) {
      drawBamf(x, y, z, 0.5F, 0.1F, 0.6F, sound, flair, side);
   }

   public static void drawBamf(double x, double y, double z, int color, boolean sound, boolean flair, Direction side) {
      drawBamf(x, y, z, r(color), g(color), b(color), sound, flair, side);
   }

   /** drawBamf: a puff of colored smoke, with a flash and motes if flair (the curly wisps come with the wisp FX). */
   public static void drawBamf(double x, double y, double z, float r, float g, float b, boolean sound, boolean flair, Direction side) {
      ClientLevel level = level();
      RandomSource rand = level.random;
      if (sound) {
         level.playLocalSound(x, y, z, ModSounds.POOF.get(), SoundSource.BLOCKS, 0.4F, 1.0F + (float) rand.nextGaussian() * 0.05F, false);
      }
      for (int a = 0; a < 6 + rand.nextInt(3) + 2; a++) {
         double vx = (0.05F + rand.nextFloat() * 0.05F) * (rand.nextBoolean() ? -1 : 1);
         double vy = (0.05F + rand.nextFloat() * 0.05F) * (rand.nextBoolean() ? -1 : 1);
         double vz = (0.05F + rand.nextFloat() * 0.05F) * (rand.nextBoolean() ? -1 : 1);
         if (side != null) {
            vx += side.getStepX() * 0.1F;
            vy += side.getStepY() * 0.1F;
            vz += side.getStepZ() * 0.1F;
         }
         FXGeneric fb2 = new FXGeneric(level, x + vx * 2.0, y + vy * 2.0, z + vz * 2.0, vx / 2.0, vy / 2.0, vz / 2.0);
         fb2.setMaxAge(20 + rand.nextInt(15));
         fb2.setRBGColorF(Mth.clamp(r * (1.0F + (float) rand.nextGaussian() * 0.1F), 0.0F, 1.0F),
            Mth.clamp(g * (1.0F + (float) rand.nextGaussian() * 0.1F), 0.0F, 1.0F),
            Mth.clamp(b * (1.0F + (float) rand.nextGaussian() * 0.1F), 0.0F, 1.0F));
         fb2.setAlphaF(1.0F, 0.1F);
         fb2.setGridSize(16);
         fb2.setParticles(123, 5, 1);
         fb2.setScale(3.0F, 4.0F + rand.nextFloat() * 3.0F);
         fb2.setLayer(1);
         fb2.setSlowDown(0.7);
         fb2.setRotationSpeed(rand.nextFloat(), rand.nextBoolean() ? -1.0F : 1.0F);
         add(fb2);
      }
      if (flair) {
         for (int a = 0; a < 2 + rand.nextInt(3); a++) {
            double vx = (0.025F + rand.nextFloat() * 0.025F) * (rand.nextBoolean() ? -1 : 1);
            double vy = (0.025F + rand.nextFloat() * 0.025F) * (rand.nextBoolean() ? -1 : 1);
            double vz = (0.025F + rand.nextFloat() * 0.025F) * (rand.nextBoolean() ? -1 : 1);
            drawWispyMotes(x + vx * 2.0, y + vy * 2.0, z + vz * 2.0, vx, vy, vz, 15 + rand.nextInt(10), -0.01F);
         }
         FXGeneric fb = new FXGeneric(level, x, y, z);
         fb.setMaxAge(10 + rand.nextInt(5));
         fb.setRBGColorF(1.0F, 0.9F, 1.0F);
         fb.setAlphaF(1.0F, 0.0F);
         fb.setGridSize(16);
         fb.setParticles(77, 1, 1);
         fb.setScale(10.0F + rand.nextFloat() * 2.0F, 0.0F);
         fb.setLayer(0);
         fb.setRotationSpeed(rand.nextFloat(), (float) rand.nextGaussian());
         add(fb);
      }
   }
}
