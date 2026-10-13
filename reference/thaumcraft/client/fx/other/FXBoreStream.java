package thaumcraft.client.fx.other;

import com.sasmaster.glelwjgl.java.CoreGLE;
import java.awt.Color;
import java.util.ArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import org.lwjgl.opengl.GL11;
import thaumcraft.client.fx.ParticleEngine;
import thaumcraft.codechicken.lib.vec.Quat;

public class FXBoreStream extends Particle {
   private Entity target;
   private double startX;
   private double startY;
   private double startZ;
   private int count = 0;
   public int length = 5;
   private String key = "";
   private BlockPos startPos = null;
   CoreGLE gle = new CoreGLE();
   private static final ResourceLocation TEX0 = new ResourceLocation("thaumcraft", "textures/misc/essentia.png");
   int layer = 1;
   double[][] points;
   float[][] colours;
   double[] radii;
   int growing = -1;
   ArrayList<Quat> vecs = new ArrayList<>();

   public FXBoreStream(World w, double par2, double par4, double par6, Entity target, int count, int color, float scale, int extend, double my) {
      super(w, par2, par4, par6, 0.0, 0.0, 0.0);
      this.particleScale = (float)(scale * (1.0 + this.rand.nextGaussian() * 0.15F));
      this.length = Math.max(5, extend);
      this.count = count;
      this.target = target;
      this.particleMaxAge = this.length * 10;
      this.motionX = MathHelper.sin(count / 4.0F) * 0.15F;
      this.motionY = my + MathHelper.sin(count / 3.0F) * 0.15F;
      this.motionZ = MathHelper.sin(count / 2.0F) * 0.15F;
      Color c = new Color(color);
      this.particleRed = c.getRed() / 255.0F;
      this.particleGreen = c.getGreen() / 255.0F;
      this.particleBlue = c.getBlue() / 255.0F;
      this.particleGravity = 0.2F;
      this.vecs.add(new Quat(0.0, 0.0, 0.0, 0.001));
      this.vecs.add(new Quat(0.0, 0.0, 0.0, 0.001));
      this.startX = this.posX;
      this.startY = this.posY;
      this.startZ = this.posZ;
      this.startPos = new BlockPos(this.startX, this.startY, this.startZ);
   }

   public void renderParticle(BufferBuilder wr, Entity entity, float f, float f1, float f2, float f3, float f4, float f5) {
      Tessellator.getInstance().draw();
      GL11.glPushMatrix();
      double ePX = this.startX - interpPosX;
      double ePY = this.startY - interpPosY;
      double ePZ = this.startZ - interpPosZ;
      GL11.glTranslated(ePX, ePY, ePZ);
      if (this.points != null && this.points.length > 2) {
         Minecraft.getMinecraft().renderEngine.bindTexture(TEX0);
         this.gle.set_POLYCYL_TESS(8);
         this.gle.set__ROUND_TESS_PIECES(1);
         this.gle.gleSetJoinStyle(1042);
         this.gle
            .glePolyCone(
               this.points.length, this.points, this.colours, this.radii, 0.075F, this.growing < 0 ? 0.0F : 0.075F * (this.particleAge - this.growing + f)
            );
      }

      GL11.glPopMatrix();
      Minecraft.getMinecraft().renderEngine.bindTexture(ParticleEngine.particleTexture);
      wr.begin(7, DefaultVertexFormats.PARTICLE_POSITION_TEX_COLOR_LMAP);
   }

   public void setFXLayer(int l) {
      this.layer = l;
   }

   public int getFXLayer() {
      return this.layer;
   }

   public void onUpdate() {
      this.prevPosX = this.posX;
      this.prevPosY = this.posY;
      this.prevPosZ = this.posZ;
      if (this.particleAge++ < this.particleMaxAge && this.length >= 1) {
         this.motionY = this.motionY + 0.01 * this.particleGravity;
         this.move(this.motionX, this.motionY, this.motionZ);
         this.motionX *= 0.985;
         this.motionY *= 0.985;
         this.motionZ *= 0.985;
         double dx = this.target.posX - this.posX;
         double dy = this.target.posY + this.target.getEyeHeight() - this.posY;
         double dz = this.target.posZ - this.posZ;
         double d11 = MathHelper.sqrt(dx * dx + dy * dy + dz * dz);
         double clamp = d11 / 10.0;
         this.motionX = MathHelper.clamp((float)this.motionX, -clamp, clamp);
         this.motionY = MathHelper.clamp((float)this.motionY, -clamp, clamp);
         this.motionZ = MathHelper.clamp((float)this.motionZ, -clamp, clamp);
         dx /= d11;
         dy /= d11;
         dz /= d11;
         this.motionX = this.motionX + dx * (clamp / Math.min(1.0, d11));
         this.motionY = this.motionY + dy * (clamp / Math.min(1.0, d11));
         this.motionZ = this.motionZ + dz * (clamp / Math.min(1.0, d11));
         float scale = this.particleScale * (0.75F + MathHelper.sin((this.count + this.particleAge) / 2.0F) * 0.25F);
         if (d11 < 1.0) {
            float f = MathHelper.sin((float)(d11 * (Math.PI / 2)));
            scale *= f;
            this.particleScale *= f;
         }

         if (this.particleScale > 0.001) {
            this.vecs.add(new Quat(scale, this.posX - this.startX, this.posY - this.startY, this.posZ - this.startZ));
         } else {
            if (this.growing < 0) {
               this.growing = this.particleAge;
            }

            this.length--;
         }

         if (this.vecs.size() > this.length) {
            this.vecs.remove(0);
         }

         this.points = new double[this.vecs.size()][3];
         this.colours = new float[this.vecs.size()][4];
         this.radii = new double[this.vecs.size()];
         int c = this.vecs.size();

         for (Quat v : this.vecs) {
            c--;
            float variance = 1.0F + MathHelper.sin((c + this.particleAge) / 3.0F) * 0.2F;
            float xx = MathHelper.sin((c + this.particleAge) / 6.0F) * 0.03F;
            float yy = MathHelper.sin((c + this.particleAge) / 7.0F) * 0.03F;
            float zz = MathHelper.sin((c + this.particleAge) / 8.0F) * 0.03F;
            this.points[c][0] = v.x + xx;
            this.points[c][1] = v.y + yy;
            this.points[c][2] = v.z + zz;
            this.radii[c] = v.s * variance;
            if (c > this.vecs.size() - 10) {
               this.radii[c] = this.radii[c] * MathHelper.cos((float)((c - (this.vecs.size() - 12)) / 10.0F * (Math.PI / 2)));
            }

            if (c == 0) {
               this.radii[c] = 0.0;
            } else if (c == 1) {
               this.radii[c] = 0.0;
            } else if (c == 2) {
               this.radii[c] = (this.particleScale * 0.5 + this.radii[c]) / 2.0;
            } else if (c == 3) {
               this.radii[c] = (this.particleScale + this.radii[c]) / 2.0;
            } else if (c == 4) {
               this.radii[c] = (this.particleScale + this.radii[c] * 2.0) / 3.0;
            }

            float v2 = 1.0F - MathHelper.sin((c + this.particleAge) / 2.0F) * 0.1F;
            this.colours[c][0] = this.particleRed * v2;
            this.colours[c][1] = this.particleGreen * v2;
            this.colours[c][2] = this.particleBlue * v2;
            this.colours[c][3] = 1.0F;
         }
      } else {
         this.setExpired();
      }
   }

   public void setGravity(float value) {
      this.particleGravity = value;
   }
}
