package thaumcraft.client.fx.beams;

import com.sasmaster.glelwjgl.java.CoreGLE;
import java.util.ArrayList;
import java.util.Random;
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

public class FXBolt extends Particle {
   float width = 0.0F;
   ArrayList<Vec3d> points = new ArrayList<>();
   ArrayList<Float> pointsWidth = new ArrayList<>();
   float dr = 0.0F;
   long seed = 0L;
   private Entity targetEntity = null;
   private double tX = 0.0;
   private double tY = 0.0;
   private double tZ = 0.0;
   ResourceLocation beam = new ResourceLocation("thaumcraft", "textures/misc/essentia.png");
   public float length = 1.0F;
   CoreGLE gle = new CoreGLE();

   public FXBolt(World par1World, double x, double y, double z, double tx, double ty, double tz, float red, float green, float blue, float width) {
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
      this.width = width;
      this.particleMaxAge = 3;
      Vec3d vs = new Vec3d(0.0, 0.0, 0.0);
      Vec3d ve = new Vec3d(this.tX, this.tY, this.tZ);
      this.length = (float)(ve.length() * Math.PI);
      int steps = (int)this.length;
      this.points.add(vs);
      this.pointsWidth.add(width);
      this.dr = (float)(this.rand.nextInt(50) * Math.PI);
      float ampl = 0.1F;

      for (int a = 1; a < steps - 1; a++) {
         float dist = a * (this.length / steps) + this.dr;
         double dx = this.tX / steps * a + MathHelper.sin(dist / 4.0F) * ampl;
         double dy = this.tY / steps * a + MathHelper.sin(dist / 3.0F) * ampl;
         double dz = this.tZ / steps * a + MathHelper.sin(dist / 2.0F) * ampl;
         dx += (this.rand.nextFloat() - this.rand.nextFloat()) * 0.1F;
         dy += (this.rand.nextFloat() - this.rand.nextFloat()) * 0.1F;
         dz += (this.rand.nextFloat() - this.rand.nextFloat()) * 0.1F;
         Vec3d vp = new Vec3d(dx, dy, dz);
         this.points.add(vp);
         this.pointsWidth.add(width);
      }

      this.pointsWidth.add(width);
      this.points.add(ve);
      this.seed = this.rand.nextInt(1000);
   }

   public void onUpdate() {
      this.prevPosX = this.posX;
      this.prevPosY = this.posY;
      this.prevPosZ = this.posZ;
      if (this.particleAge++ >= this.particleMaxAge) {
         this.setExpired();
      }
   }

   private void calcSteps(float f) {
      Random rr = new Random(this.seed);
      this.points.clear();
      this.pointsWidth.clear();
      Vec3d vs = new Vec3d(0.0, 0.0, 0.0);
      Vec3d ve = new Vec3d(this.tX, this.tY, this.tZ);
      int steps = (int)this.length;
      this.points.add(vs);
      this.pointsWidth.add(this.width);
      float ampl = (this.particleAge + f) / 10.0F;

      for (int a = 1; a < steps - 1; a++) {
         float dist = a * (this.length / steps) + this.dr;
         double dx = this.tX / steps * a + MathHelper.sin(dist / 4.0F) * ampl;
         double dy = this.tY / steps * a + MathHelper.sin(dist / 3.0F) * ampl;
         double dz = this.tZ / steps * a + MathHelper.sin(dist / 2.0F) * ampl;
         dx += (rr.nextFloat() - rr.nextFloat()) * 0.1F;
         dy += (rr.nextFloat() - rr.nextFloat()) * 0.1F;
         dz += (rr.nextFloat() - rr.nextFloat()) * 0.1F;
         Vec3d vp = new Vec3d(dx, dy, dz);
         this.points.add(vp);
         this.pointsWidth.add((rr.nextInt(4) == 0 ? 1.0F - this.particleAge * 0.25F : 1.0F) * this.width);
      }

      this.pointsWidth.add(this.width);
      this.points.add(ve);
   }

   public void setRGB(float r, float g, float b) {
      this.particleRed = r;
      this.particleGreen = g;
      this.particleBlue = b;
   }

   public void renderParticle(BufferBuilder wr, Entity entity, float f, float cosyaw, float cospitch, float sinyaw, float cossinpitch, float f5) {
      Tessellator.getInstance().draw();
      GL11.glPushMatrix();
      double ePX = this.prevPosX + (this.posX - this.prevPosX) * f - interpPosX;
      double ePY = this.prevPosY + (this.posY - this.prevPosY) * f - interpPosY;
      double ePZ = this.prevPosZ + (this.posZ - this.prevPosZ) * f - interpPosZ;
      GL11.glTranslated(ePX, ePY, ePZ);
      Minecraft.getMinecraft().renderEngine.bindTexture(this.beam);
      GL11.glDepthMask(false);
      GL11.glEnable(3042);
      GL11.glBlendFunc(770, 1);
      GL11.glDisable(2884);
      this.calcSteps(f);
      float alpha = MathHelper.clamp(1.0F - (float)this.particleAge / this.particleMaxAge, 0.1F, 1.0F);
      if (this.points != null && this.points.size() > 2) {
         double[][] pp = new double[this.points.size()][3];
         float[][] colours = new float[this.points.size()][4];
         double[] radii = new double[this.points.size()];

         for (int a = 0; a < this.points.size(); a++) {
            pp[a][0] = this.points.get(a).x;
            pp[a][1] = this.points.get(a).y;
            pp[a][2] = this.points.get(a).z;
            colours[a][0] = this.particleRed;
            colours[a][1] = this.particleGreen;
            colours[a][2] = this.particleBlue;
            colours[a][3] = alpha;
            radii[a] = this.pointsWidth.get(a) / 10.0F;
         }

         this.gle.set_POLYCYL_TESS(5);
         this.gle.gleSetJoinStyle(1042);
         this.gle.glePolyCone(pp.length, pp, colours, radii, 1.0F, 0.0F);

         for (int a = 0; a < this.points.size(); a++) {
            radii[a] /= 3.0;
         }

         this.gle.glePolyCone(pp.length, pp, colours, radii, 1.0F, 0.0F);
      }

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
