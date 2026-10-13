package thaumcraft.client.fx.beams;

import java.util.ArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.lwjgl.opengl.GL11;
import thaumcraft.client.fx.ParticleEngine;
import thaumcraft.client.fx.particles.FXGeneric;
import thaumcraft.common.lib.utils.Utils;

public class FXArc extends Particle {
   public int particle = 16;
   ArrayList<Vec3d> points = new ArrayList<>();
   private Entity targetEntity = null;
   private double tX = 0.0;
   private double tY = 0.0;
   private double tZ = 0.0;
   ResourceLocation beam = new ResourceLocation("thaumcraft", "textures/misc/beamh.png");
   public int blendmode = 1;
   public float length = 1.0F;

   public FXArc(World par1World, double x, double y, double z, double tx, double ty, double tz, float red, float green, float blue, double hg) {
      super(par1World, x, y, z, 0.0, 0.0, 0.0);
      this.particleRed = red;
      this.particleGreen = green;
      this.particleBlue = blue;
      this.setSize(0.02F, 0.02F);
      this.motionX = 0.0;
      this.motionY = 0.0;
      this.motionZ = 0.0;
      this.tX = tx - x;
      this.tY = ty - y;
      this.tZ = tz - z;
      this.particleMaxAge = 3;
      double xx = 0.0;
      double yy = 0.0;
      double zz = 0.0;
      double gravity = 0.115;
      double noise = 0.25;
      Vec3d vs = new Vec3d(xx, yy, zz);
      Vec3d ve = new Vec3d(this.tX, this.tY, this.tZ);
      Vec3d vc = new Vec3d(xx, yy, zz);
      this.length = (float)ve.length();
      Vec3d vv = Utils.calculateVelocity(vs, ve, hg, gravity);
      double l = Utils.distanceSquared3d(new Vec3d(0.0, 0.0, 0.0), vv);
      this.points.add(vs);

      for (int c = 0; Utils.distanceSquared3d(ve, vc) > l && c < 50; c++) {
         Vec3d vt = vc.add(vv.x, vv.y, vv.z);
         vc = new Vec3d(vt.x, vt.y, vt.z);
         vt = vt.add(
            (this.rand.nextDouble() - this.rand.nextDouble()) * noise,
            (this.rand.nextDouble() - this.rand.nextDouble()) * noise,
            (this.rand.nextDouble() - this.rand.nextDouble()) * noise
         );
         this.points.add(vt);
         FXGeneric fb = new FXGeneric(par1World, x + vt.x, y + vt.y, z + vt.z, 0.0, 0.0, 0.0);
         int age = 30 + this.rand.nextInt(20);
         fb.setMaxAge(age);
         fb.setRBGColorF(
            MathHelper.clamp(red * 3.0F, 0.0F, 1.0F),
            MathHelper.clamp(green * 3.0F, 0.0F, 1.0F),
            MathHelper.clamp(blue * 3.0F, 0.0F, 1.0F),
            this.rand.nextFloat(),
            this.rand.nextFloat(),
            this.rand.nextFloat()
         );
         float[] alphas = new float[6 + this.rand.nextInt(age / 3)];

         for (int a = 1; a < alphas.length - 1; a++) {
            alphas[a] = this.rand.nextFloat();
         }

         alphas[0] = 1.0F;
         fb.setAlphaF(alphas);
         boolean sp = this.rand.nextFloat() < 0.2;
         fb.setParticles(sp ? 320 : 512, 16, 1);
         fb.setLoop(true);
         fb.setGravity(sp ? 0.0F : 0.125F);
         fb.setScale(0.5F, 0.125F);
         fb.setLayer(0);
         fb.setSlowDown(0.995);
         fb.setRandomMovementScale(0.0025F, 0.001F, 0.0025F);
         ParticleEngine.addEffectWithDelay(par1World, fb, 2 + this.rand.nextInt(3));
         vv = vv.subtract(0.0, gravity / 1.9, 0.0);
      }

      this.points.add(ve);
   }

   public void onUpdate() {
      this.prevPosX = this.posX;
      this.prevPosY = this.posY;
      this.prevPosZ = this.posZ;
      if (this.particleAge++ >= this.particleMaxAge) {
         this.setExpired();
      }
   }

   public void setRGB(float r, float g, float b) {
      this.particleRed = r;
      this.particleGreen = g;
      this.particleBlue = b;
   }

   public void renderParticle(BufferBuilder wr, Entity entity, float f, float f1, float f2, float f3, float f4, float f5) {
      Tessellator.getInstance().draw();
      GL11.glPushMatrix();
      double ePX = this.prevPosX + (this.posX - this.prevPosX) * f - interpPosX;
      double ePY = this.prevPosY + (this.posY - this.prevPosY) * f - interpPosY;
      double ePZ = this.prevPosZ + (this.posZ - this.prevPosZ) * f - interpPosZ;
      GL11.glTranslated(ePX, ePY, ePZ);
      float size = 0.125F;
      Minecraft.getMinecraft().renderEngine.bindTexture(this.beam);
      GL11.glDepthMask(false);
      GL11.glEnable(3042);
      GL11.glBlendFunc(770, 1);
      GL11.glDisable(2884);
      int i = 220;
      int j = i >> 16 & 65535;
      int k = i & 65535;
      float alpha = 1.0F - (this.particleAge + f) / this.particleMaxAge;
      wr.begin(5, DefaultVertexFormats.POSITION_TEX_LMAP_COLOR);
      float f9 = 0.0F;
      float f10 = 1.0F;

      for (int c = 0; c < this.points.size(); c++) {
         Vec3d v = this.points.get(c);
         float f13 = c / this.length;
         double dx = v.x;
         double dy = v.y;
         double dz = v.z;
         wr.pos(dx, dy - size, dz)
            .tex(f13, f10)
            .lightmap(j, k)
            .color(this.particleRed, this.particleGreen, this.particleBlue, alpha)
            .endVertex();
         wr.pos(dx, dy + size, dz)
            .tex(f13, f9)
            .lightmap(j, k)
            .color(this.particleRed, this.particleGreen, this.particleBlue, alpha)
            .endVertex();
      }

      Tessellator.getInstance().draw();
      wr.begin(5, DefaultVertexFormats.POSITION_TEX_LMAP_COLOR);

      for (int c = 0; c < this.points.size(); c++) {
         Vec3d v = this.points.get(c);
         float f13 = c / this.length;
         double dx = v.x;
         double dy = v.y;
         double dz = v.z;
         wr.pos(dx - size, dy, dz - size)
            .tex(f13, f10)
            .lightmap(j, k)
            .color(this.particleRed, this.particleGreen, this.particleBlue, alpha)
            .endVertex();
         wr.pos(dx + size, dy, dz + size)
            .tex(f13, f9)
            .lightmap(j, k)
            .color(this.particleRed, this.particleGreen, this.particleBlue, alpha)
            .endVertex();
      }

      Tessellator.getInstance().draw();
      GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
      GL11.glEnable(2884);
      GL11.glBlendFunc(770, 771);
      GL11.glDisable(3042);
      GL11.glDepthMask(true);
      GL11.glPopMatrix();
      Minecraft.getMinecraft().renderEngine.bindTexture(ParticleManager.PARTICLE_TEXTURES);
      wr.begin(7, DefaultVertexFormats.PARTICLE_POSITION_TEX_COLOR_LMAP);
   }
}
