package thaumcraft.client.fx;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.casters.FocusEffect;
import thaumcraft.api.casters.FocusEngine;
import thaumcraft.api.casters.IFocusElement;
import thaumcraft.common.network.FocusFXPayload;

/**
 * The client effects of the casters milestone: the FXDispatcher methods the foci use (GenPart particles, fire motes,
 * cloud puffs, sparks, vis sparkles, arc bolts), the delayed particle queue (ParticleEngine.addEffectWithDelay) and the
 * FocusFXPayload consumer (PacketFXFocusEffect, PacketFXFocusPartImpact(Burst), PacketFXZap).
 */
public final class FocusFX {
   private record Delayed(Particle p, int[] delay) {}

   private static final List<Delayed> DELAYED = new ArrayList<>();

   private FocusFX() {
   }

   public static ClientLevel level() {
      return Minecraft.getInstance().level;
   }

   public static RandomSource rand() {
      return level().random;
   }

   public static void add(Particle p) {
      Minecraft.getInstance().particleEngine.add(p);
   }

   public static void addWithDelay(Particle p, int delay) {
      if (delay <= 0) add(p);
      else DELAYED.add(new Delayed(p, new int[]{delay}));
   }

   /** Called every client tick. */
   public static void tick() {
      if (DELAYED.isEmpty()) return;
      if (level() == null) {
         DELAYED.clear();
         return;
      }
      for (Iterator<Delayed> it = DELAYED.iterator(); it.hasNext(); ) {
         Delayed d = it.next();
         if (--d.delay[0] <= 0) {
            it.remove();
            add(d.p);
         }
      }
   }

   // ------------------------------------------------------------------ FocusFXPayload

   public static void handle(FocusFXPayload p) {
      if (level() == null) return;
      switch (p.kind()) {
         case FocusFXPayload.TRAIL -> {
            int amt = Math.max(1, 10 / Math.max(1, p.keys().size()));
            for (String k : p.keys()) {
               if (FocusEngine.getElement(k) instanceof FocusEffect fe) {
                  for (int a = 0; a < amt; a++) {
                     fe.renderParticleFX(level(), p.pos().x, p.pos().y, p.pos().z, p.vec().x + rand().nextGaussian() / 20.0,
                        p.vec().y + rand().nextGaussian() / 20.0, p.vec().z + rand().nextGaussian() / 20.0);
                  }
               }
            }
         }
         case FocusFXPayload.IMPACT, FocusFXPayload.BURST -> {
            boolean burst = p.kind() == FocusFXPayload.BURST;
            int amt = Math.max(1, (burst ? 20 : 15) / Math.max(1, p.keys().size()));
            double sp = burst ? 0.4 : 0.15;
            for (String k : p.keys()) {
               IFocusElement part = FocusEngine.getElement(k);
               if (part instanceof FocusEffect fe) {
                  for (int a = 0; a < amt; a++) {
                     fe.renderParticleFX(level(), p.pos().x, p.pos().y, p.pos().z, rand().nextGaussian() * sp, rand().nextGaussian() * sp,
                        rand().nextGaussian() * sp);
                  }
               }
            }
         }
         case FocusFXPayload.ZAP -> {
            int c = p.color();
            arcBolt(p.pos().x, p.pos().y, p.pos().z, p.vec().x, p.vec().y, p.vec().z, (c >> 16 & 255) / 255.0F, (c >> 8 & 255) / 255.0F,
               (c & 255) / 255.0F, p.width());
         }
         default -> {
         }
      }
   }

   // ------------------------------------------------------------------ FXDispatcher methods

   /** FXDispatcher.GenPart. */
   public static class GenPart {
      public int grid = 64;
      public int age = 0;
      public float redStart = 1.0F, greenStart = 1.0F, blueStart = 1.0F;
      public float redEnd = 1.0F, greenEnd = 1.0F, blueEnd = 1.0F;
      public float[] alpha = {1.0F};
      public float[] scale = {1.0F};
      public float rot;
      public float rotstart = 0.0F;
      public boolean loop = false;
      public int partStart = 0;
      public int partNum = 1;
      public int partInc = 1;
      public int layer = 0;
      public double slowDown = 0.98F;
      public float grav = 0.0F;
      public int delay = 0;
   }

   public static void drawGenericParticles(double x, double y, double z, double mx, double my, double mz, GenPart part) {
      FXGeneric fb = new FXGeneric(level(), x, y, z, mx, my, mz);
      fb.setMaxAge(part.age);
      fb.setRBGColorF(part.redStart, part.greenStart, part.blueStart, part.redEnd, part.greenEnd, part.blueEnd);
      fb.setAlphaF(part.alpha);
      fb.setLoop(part.loop);
      fb.setParticles(part.partStart, part.partNum, part.partInc);
      fb.setScale(part.scale);
      fb.setLayer(part.layer);
      fb.setRotationSpeed(part.rotstart, part.rot);
      fb.setSlowDown(part.slowDown);
      fb.setGravity(part.grav);
      fb.setGridSize(part.grid);
      addWithDelay(fb, part.delay);
   }

   /** FXFireMote: a glowing mote (frame 7), a third of the size on the blended layer half the time. */
   public static void drawFireMote(float x, float y, float z, float vx, float vy, float vz, float r, float g, float b, float alpha, float scale) {
      boolean bb = rand().nextBoolean();
      FXGeneric fb = new FXGeneric(level(), x, y, z, vx, vy, vz);
      fb.setMaxAge(16);
      fb.setRBGColorF(r, g, b);
      fb.setAlphaF(alpha);
      fb.setParticle(7);
      fb.setScale(bb ? scale / 3.0F : scale, 0.0F);
      fb.setLayer(bb ? 1 : 0);
      fb.setSlowDown(1.0);
      add(fb);
   }

   public static void drawFocusCloudParticle(double x, double y, double z, double mx, double my, double mz, int c) {
      FXGeneric fb = new FXGeneric(level(), x, y, z, mx, my, mz);
      fb.setMaxAge(20 + rand().nextInt(10));
      fb.setRBGColorF((c >> 16 & 255) / 255.0F, (c >> 8 & 255) / 255.0F, (c & 255) / 255.0F);
      fb.setAlphaF(0.0F, 0.66F, 0.0F);
      fb.setGridSize(16);
      fb.setParticles(56 + rand().nextInt(4), 1, 1);
      fb.setScale(5.0F + rand().nextFloat(), 10.0F + rand().nextFloat());
      fb.setLayer(0);
      fb.setSlowDown(0.99);
      fb.setWind(0.001);
      fb.setRotationSpeed(rand().nextFloat(), rand().nextBoolean() ? -0.25F : 0.25F);
      add(fb);
   }

   public static void spark(double x, double y, double z, float size, float r, float g, float b, float a) {
      FXGeneric fb = new FXGeneric(level(), x, y, z, 0.0, 0.0, 0.0);
      fb.setMaxAge(5 + rand().nextInt(5));
      fb.setAlphaF(a);
      fb.setRBGColorF(r, g, b);
      fb.setGridSize(16);
      fb.setParticles(8 + rand().nextInt(3) * 16, 8, 1);
      fb.setScale(size);
      fb.setFlipped(rand().nextBoolean());
      add(fb);
   }

   /** FXDispatcher.drawGenericParticles with explicit values. */
   public static void generic(double x, double y, double z, double x2, double y2, double z2, float r, float g, float b, float alpha, boolean loop,
                              int start, int num, int inc, int age, int delay, float scale, float rot, int layer) {
      FXGeneric fb = new FXGeneric(level(), x, y, z, x2, y2, z2);
      fb.setMaxAge(age);
      fb.setRBGColorF(r, g, b);
      fb.setAlphaF(alpha);
      fb.setLoop(loop);
      fb.setParticles(start, num, inc);
      fb.setScale(scale);
      fb.setLayer(layer);
      fb.setRotationSpeed(rot);
      addWithDelay(fb, delay);
   }

   /** FXVisSparkle: a green mote drawn from (x, y, z) to the target, growing then shrinking as it arrives. */
   public static void visSparkle(int x, int y, int z, int x2, int y2, int z2, int color) {
      RandomSource r = rand();
      VisSparkle fb = new VisSparkle(level(), x + r.nextFloat(), y + r.nextFloat(), z + r.nextFloat(), x2 + 0.4 + r.nextFloat() * 0.2F,
         y2 + 0.4 + r.nextFloat() * 0.2F, z2 + 0.4 + r.nextFloat() * 0.2F);
      fb.setRBGColorF((color >> 16 & 255) / 255.0F, (color >> 8 & 255) / 255.0F, (color & 255) / 255.0F);
      add(fb);
   }

   private static final class VisSparkle extends FXGeneric {
      private final double tx, ty, tz;
      private final float sizeMod;
      private float sc;

      VisSparkle(ClientLevel level, double x, double y, double z, double tx, double ty, double tz) {
         super(level, x, y, z, random(level).nextGaussian() * 0.01, random(level).nextGaussian() * 0.01, random(level).nextGaussian() * 0.01);
         this.tx = tx;
         this.ty = ty;
         this.tz = tz;
         this.sizeMod = 45 + random.nextInt(15);
         setMaxAge(400);
         setLoop(true);
         setParticles(512, 16, 1);
         setAlphaF(0.5F);
         setNoClip(true);
         setSlowDown(1.0);
      }

      private static RandomSource random(ClientLevel l) {
         return l.random;
      }

      @Override
      public void tick() {
         xd *= 0.985;
         yd *= 0.985;
         zd *= 0.985;
         double dx = tx - x, dy = ty - y, dz = tz - z;
         double d11 = Math.sqrt(dx * dx + dy * dy + dz * dz);
         if (d11 < 2.0) sc *= 0.95F;
         if (d11 < 0.2) remove();
         if (age < 10) sc = age / sizeMod;
         dx /= d11;
         dy /= d11;
         dz /= d11;
         xd += dx * 0.1F;
         yd += dy * 0.1F;
         zd += dz * 0.1F;
         xd = Mth.clamp(xd, -0.1F, 0.1F);
         yd = Mth.clamp(yd, -0.1F, 0.1F);
         zd = Mth.clamp(zd, -0.1F, 0.1F);
         super.tick();
         float bob = Mth.sin(age / 3.0F) * 0.3F + 6.0F;
         scaleFrames = new float[]{sc * bob};
      }
   }

   // ------------------------------------------------------------------ arc bolt (FXBolt)

   private static final ParticleRenderType BOLT_TYPE = new ParticleRenderType() {
      @Override
      public BufferBuilder begin(Tesselator tesselator, TextureManager textureManager) {
         RenderSystem.setShader(GameRenderer::getPositionColorShader);
         RenderSystem.enableBlend();
         RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
         RenderSystem.depthMask(false);
         RenderSystem.disableCull();
         return tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
      }

      @Override
      public String toString() {
         return "thaumcraft:bolt";
      }
   };

   public static void arcBolt(double x, double y, double z, double tx, double ty, double tz, float r, float g, float b, float width) {
      add(new Bolt(level(), x, y, z, tx, ty, tz, r, g, b, width));
   }

   /** FXBolt: a wavy lightning tube between two points (drawn as two camera-facing ribbons instead of GLE cones). */
   private static final class Bolt extends Particle {
      private final double dX, dY, dZ;
      private final float width, length, dr;
      private final long seed;

      Bolt(ClientLevel level, double x, double y, double z, double tx, double ty, double tz, float r, float g, float b, float width) {
         super(level, x, y, z, 0, 0, 0);
         setColor(r, g, b);
         this.dX = tx - x;
         this.dY = ty - y;
         this.dZ = tz - z;
         this.width = width;
         this.lifetime = 3;
         this.length = (float) (Math.sqrt(dX * dX + dY * dY + dZ * dZ) * Math.PI);
         this.dr = (float) (random.nextInt(50) * Math.PI);
         this.seed = random.nextInt(1000);
         this.xd = this.yd = this.zd = 0;
         this.hasPhysics = false;
      }

      @Override
      public void tick() {
         xo = x;
         yo = y;
         zo = z;
         if (age++ >= lifetime) remove();
      }

      @Override
      public ParticleRenderType getRenderType() {
         return BOLT_TYPE;
      }

      @Override
      public void render(VertexConsumer buffer, Camera camera, float partialTicks) {
         Random rr = new Random(seed);
         int steps = (int) length;
         if (steps < 3) steps = 3;
         List<Vec3> points = new ArrayList<>();
         List<Float> widths = new ArrayList<>();
         points.add(Vec3.ZERO);
         widths.add(width);
         float ampl = (age + partialTicks) / 10.0F;
         for (int a = 1; a < steps - 1; a++) {
            float dist = a * (length / steps) + dr;
            double px = dX / steps * a + Mth.sin(dist / 4.0F) * ampl + (rr.nextFloat() - rr.nextFloat()) * 0.1F;
            double py = dY / steps * a + Mth.sin(dist / 3.0F) * ampl + (rr.nextFloat() - rr.nextFloat()) * 0.1F;
            double pz = dZ / steps * a + Mth.sin(dist / 2.0F) * ampl + (rr.nextFloat() - rr.nextFloat()) * 0.1F;
            points.add(new Vec3(px, py, pz));
            widths.add((rr.nextInt(4) == 0 ? 1.0F - age * 0.25F : 1.0F) * width);
         }
         points.add(new Vec3(dX, dY, dZ));
         widths.add(width);
         float alpha = Mth.clamp(1.0F - (float) age / lifetime, 0.1F, 1.0F);
         Vec3 cam = camera.getPosition();
         Vec3 origin = new Vec3(Mth.lerp(partialTicks, xo, x), Mth.lerp(partialTicks, yo, y), Mth.lerp(partialTicks, zo, z));
         for (float div : new float[]{10.0F, 30.0F}) {
            for (int i = 0; i < points.size() - 1; i++) {
               Vec3 a0 = origin.add(points.get(i)), a1 = origin.add(points.get(i + 1));
               Vec3 dir = a1.subtract(a0);
               Vec3 side = dir.cross(a0.subtract(cam)).normalize();
               Vec3 s0 = side.scale(widths.get(i) / div), s1 = side.scale(widths.get(i + 1) / div);
               vtx(buffer, a0.subtract(s0), cam, alpha);
               vtx(buffer, a0.add(s0), cam, alpha);
               vtx(buffer, a1.add(s1), cam, alpha);
               vtx(buffer, a1.subtract(s1), cam, alpha);
            }
         }
      }

      private void vtx(VertexConsumer b, Vec3 p, Vec3 cam, float alpha) {
         b.addVertex((float) (p.x - cam.x), (float) (p.y - cam.y), (float) (p.z - cam.z)).setColor(rCol, gCol, bCol, alpha);
      }
   }
}
