package thaumcraft.client.fx;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import thaumcraft.common.entities.monster.boss.EldritchGolemEntity;

/** Client particles of the eldritch milestone (FXDispatcher sparkle / wispFXEG / smokeSpiral, FXSonic, the golem's arcs). */
public final class EldritchFX {
   private EldritchFX() {
   }

   private static RandomSource r() {
      return FocusFX.rand();
   }

   private static ClientLevel w() {
      return FocusFX.level();
   }

   /** FXDispatcher.sparkle: a small twinkling star (shown two times in three). */
   public static void sparkle(double x, double y, double z, float red, float green, float blue) {
      if (w() == null || r().nextInt(6) >= 4) return;
      FocusFX.generic(x, y, z, 0.0, 0.0, 0.0, red, green, blue, 0.9F, true, 320, 16, 1, 6 + r().nextInt(4), 0, 0.6F + r().nextFloat() * 0.2F, 0.0F, 0);
   }

   /** FXWispEG: the black smoke trailing from eldritch guardians and wardens, fading with distance from the viewer. */
   public static void wispEG(double x, double y, double z, Entity target) {
      if (w() == null) return;
      Entity view = Minecraft.getInstance().getCameraEntity();
      double dist = view == null ? 0.0 : view.distanceToSqr(x, y, z);
      float base = (float) (1.0 - Math.min(1024.0, dist) / 1024.0);
      if (base <= 0.0F) return;
      for (int a = 0; a < 2; a++) {
         FXGeneric fb = new FXGeneric(w(), x, y, z, r().nextGaussian() * 0.03 + target.getDeltaMovement().x, -0.05,
            r().nextGaussian() * 0.03 + target.getDeltaMovement().z);
         fb.setMaxAge((int) (40.0 / (Math.random() * 0.3 + 0.7)));
         fb.setParticles(192, 13, 1);
         fb.setLoop(true);
         fb.setLayer(1);
         float c = r().nextFloat() * 0.05F;
         fb.setRBGColorF(c, c, c);
         fb.setAlphaF(0.2F * base, 0.0F);
         fb.setScale(2.0F + r().nextFloat() * 2.0F);
         fb.setNoClip(false);
         FocusFX.add(fb);
      }
   }

   /** FXSmokeSpiral: smoke winding up around a rising warden. */
   public static void smokeSpiral(double x, double y, double z, float radius, int start, int miny, int color) {
      if (w() == null) return;
      FocusFX.add(new SmokeSpiral(w(), x, y, z, radius, start, miny, color));
   }

   private static final class SmokeSpiral extends FXGeneric {
      private final double cx, cy, cz;
      private final float radius;
      private final int start, miny;

      SmokeSpiral(ClientLevel level, double x, double y, double z, float radius, int start, int miny, int color) {
         super(level, x, y, z, 0.0, 0.0, 0.0);
         this.cx = x;
         this.cy = y;
         this.cz = z;
         this.radius = radius;
         this.start = start;
         this.miny = miny;
         setMaxAge(20 + level.random.nextInt(10));
         setParticles(1, 4, 1);
         setLayer(1);
         setRBGColorF((color >> 16 & 255) / 255.0F, (color >> 8 & 255) / 255.0F, (color & 255) / 255.0F);
         setAlphaF(0.66F, 0.0F);
         setScale(1.5F);
         setNoClip(true);
         spiral(0.0F);
      }

      private void spiral(float pt) {
         float r1 = start + 720.0F * ((age + pt) / lifetime);
         float r2 = 90.0F - 180.0F * ((age + pt) / lifetime);
         float mX = -Mth.sin(r1 / 180.0F * (float) Math.PI) * Mth.cos(r2 / 180.0F * (float) Math.PI) * radius;
         float mZ = Mth.cos(r1 / 180.0F * (float) Math.PI) * Mth.cos(r2 / 180.0F * (float) Math.PI) * radius;
         float mY = -Mth.sin(r2 / 180.0F * (float) Math.PI) * radius;
         setPos(cx + mX, Math.max(cy + mY, miny + 0.1F), cz + mZ);
      }

      @Override
      public void tick() {
         super.tick();
         xd = yd = zd = 0.0;
         xo = x;
         yo = y;
         zo = z;
         spiral(0.0F);
      }
   }

   /** FXSonic (an expanding ripple hemisphere in 1.12): a cone of pale rings flying out of the screecher's face. */
   public static void sonic(Entity source) {
      if (w() == null) return;
      Vec3 look = source.getLookAngle();
      Vec3 eye = source.getEyePosition();
      for (int i = 0; i < 24; i++) {
         double s = 0.25 + r().nextDouble() * 0.6;
         Vec3 v = look.add(r().nextGaussian() * 0.15, r().nextGaussian() * 0.15, r().nextGaussian() * 0.15).normalize().scale(s);
         FocusFX.generic(eye.x, eye.y, eye.z, v.x, v.y, v.z, 0.8F, 0.8F, 1.0F, 0.5F, false, 512, 16, 1, 12 + r().nextInt(6), 0,
            1.0F + r().nextFloat(), 0.0F, 1);
      }
   }

   /** PacketFXShield: the void shield flaring around an eldritch mob. */
   public static void shield(Entity e) {
      if (w() == null) return;
      for (int i = 0; i < 12; i++) {
         double a = r().nextDouble() * Math.PI * 2.0;
         double h = r().nextDouble() * e.getBbHeight();
         double rad = e.getBbWidth() * 0.8;
         FocusFX.generic(e.getX() + Math.cos(a) * rad, e.getY() + h, e.getZ() + Math.sin(a) * rad, 0.0, 0.0, 0.0, 0.5F, 0.1F, 0.8F, 0.6F,
            false, 448, 8, 1, 10, 0, 1.5F, 0.0F, 0);
      }
   }

   /** The headless construct: sparks and vented smoke at its neck, and arcs to the floor around it. */
   public static void golemHeadless(EldritchGolemEntity g, Vec3 v) {
      if (w() == null) return;
      RandomSource r = r();
      if (r.nextInt(20) == 0) {
         float a = (r.nextFloat() - r.nextFloat()) / 3.0F;
         float b = (r.nextFloat() - r.nextFloat()) / 3.0F;
         FocusFX.spark(g.getX() + v.x + a, g.getY() + g.getEyeHeight() - 0.75F, g.getZ() + v.z + b, 3.0F, 0.65F + r.nextFloat() * 0.1F, 1.0F, 1.0F, 0.8F);
      }
      FocusFX.generic(g.getX() + v.x * 0.4, g.getY() + g.getEyeHeight() - 1.25F, g.getZ() + v.z * 0.4, 0.0, 0.03, 0.0, 0.33F, 0.33F, 0.33F, 0.4F,
         false, 0, 8, 1, 20, 0, 4.0F, 0.0F, 1);
      if (g.arcing > 0) {
         FocusFX.arcBolt(g.getX(), g.getY() + g.getBbHeight() / 2.0F, g.getZ(), g.ax + 0.5, g.ay + 1, g.az + 0.5, 0.65F + r.nextFloat() * 0.1F,
            1.0F, 1.0F, 0.1F);
         g.arcing--;
      }
   }
}
