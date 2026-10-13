package thaumcraft.client.fx.other;

import com.sasmaster.glelwjgl.java.CoreGLE;
import java.util.ArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import org.lwjgl.opengl.ARBShaderObjects;
import org.lwjgl.opengl.GL11;
import thaumcraft.client.fx.ParticleEngine;
import thaumcraft.client.lib.ender.ShaderCallback;
import thaumcraft.client.lib.ender.ShaderHelper;
import thaumcraft.codechicken.lib.vec.Quat;

public class FXVoidStream extends Particle {
   private double targetX;
   private double targetY;
   private double targetZ;
   private double startX;
   private double startY;
   private double startZ;
   private int seed = 0;
   public int length = 20;
   private final ShaderCallback shaderCallback;
   private static final ResourceLocation starsTexture = new ResourceLocation("textures/entity/end_portal.png");
   CoreGLE gle = new CoreGLE();
   int layer = 1;
   double[][] points;
   float[][] colours;
   double[] radii;
   int growing = -1;
   ArrayList<Quat> vecs = new ArrayList<>();

   public FXVoidStream(World w, double par2, double par4, double par6, double tx, double ty, double tz, int seed, float scale) {
      super(w, par2, par4, par6, 0.0, 0.0, 0.0);
      this.shaderCallback = new ShaderCallback() {
         @Override
         public void call(int shader) {
            Minecraft mc = Minecraft.getMinecraft();
            int x = ARBShaderObjects.glGetUniformLocationARB(shader, "yaw");
            ARBShaderObjects.glUniform1fARB(x, (float)(mc.player.rotationYaw * 2.0F * Math.PI / 360.0));
            int z = ARBShaderObjects.glGetUniformLocationARB(shader, "pitch");
            ARBShaderObjects.glUniform1fARB(z, -((float)(mc.player.rotationPitch * 2.0F * Math.PI / 360.0)));
         }
      };
      this.particleScale = (float)(scale * (1.0 + this.rand.nextGaussian() * 0.15F));
      this.length = 40;
      this.seed = seed;
      this.targetX = tx;
      this.targetY = ty;
      this.targetZ = tz;
      double dx = tx - this.posX;
      double dy = ty - this.posY;
      double dz = tz - this.posZ;
      int base = (int)(MathHelper.sqrt(dx * dx + dy * dy + dz * dz) * 21.0F);
      if (base < 1) {
         base = 1;
      }

      this.particleMaxAge = base * 2;
      this.motionX = MathHelper.sin(seed / 4.0F) * 0.025F;
      this.motionY = MathHelper.sin(seed / 3.0F) * 0.025F;
      this.motionZ = MathHelper.sin(seed / 2.0F) * 0.025F;
      this.particleGravity = 0.2F;
      this.vecs.add(new Quat(0.0, 0.0, 0.0, 0.001));
      this.vecs.add(new Quat(0.0, 0.0, 0.0, 0.001));
      this.startX = this.posX;
      this.startY = this.posY;
      this.startZ = this.posZ;
   }

   public void renderParticle(BufferBuilder wr, Entity entity, float f, float f1, float f2, float f3, float f4, float f5) {
      Tessellator.getInstance().draw();
      GL11.glPushMatrix();
      double ePX = this.startX - interpPosX;
      double ePY = this.startY - interpPosY;
      double ePZ = this.startZ - interpPosZ;
      GL11.glTranslated(ePX, ePY, ePZ);

      for (int q = 0; q <= 1; q++) {
         if (q < 1) {
            GlStateManager.depthMask(false);
         }

         GL11.glBlendFunc(770, q < 1 ? 1 : 771);
         if (this.points != null && this.points.length > 2) {
            Minecraft.getMinecraft().renderEngine.bindTexture(starsTexture);
            ShaderHelper.useShader(ShaderHelper.endShader, this.shaderCallback);
            double[] r2 = new double[this.radii.length];
            int ri = 0;
            float m = (1.5F - q) / 1.0F;

            for (double d : this.radii) {
               r2[ri] = this.radii[ri] * m;
               ri++;
            }

            this.gle.set_POLYCYL_TESS(3);
            this.gle.set__ROUND_TESS_PIECES(1);
            this.gle.gleSetJoinStyle(1042);
            this.gle
               .glePolyCone(
                  this.points.length, this.points, this.colours, r2, 0.075F, this.growing < 0 ? 0.0F : 0.075F * (this.particleAge - this.growing + f)
               );
            ShaderHelper.releaseShader();
         }

         if (q < 1) {
            GlStateManager.depthMask(true);
         }
      }

      GlStateManager.depthMask(false);
      GL11.glBlendFunc(770, 771);
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
         this.motionX = MathHelper.clamp((float)this.motionX, -0.04F, 0.04F);
         this.motionY = MathHelper.clamp((float)this.motionY, -0.04F, 0.04F);
         this.motionZ = MathHelper.clamp((float)this.motionZ, -0.04F, 0.04F);
         double dx = this.targetX - this.posX;
         double dy = this.targetY - this.posY;
         double dz = this.targetZ - this.posZ;
         double d13 = 0.01;
         double d11 = MathHelper.sqrt(dx * dx + dy * dy + dz * dz);
         dx /= d11;
         dy /= d11;
         dz /= d11;
         this.motionX = this.motionX + (dx * (d13 / Math.min(1.0, d11)) + this.rand.nextGaussian() * 0.015F);
         this.motionY = this.motionY + (dy * (d13 / Math.min(1.0, d11)) + this.rand.nextGaussian() * 0.015F);
         this.motionZ = this.motionZ + (dz * (d13 / Math.min(1.0, d11)) + this.rand.nextGaussian() * 0.015F);
         float scale = this.particleScale * (0.75F + MathHelper.sin((this.seed + this.particleAge) / 2.0F) * 0.25F);
         if (d11 < 0.5) {
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
            float xx = MathHelper.sin((c + this.particleAge) / 6.0F) * 0.01F;
            float yy = MathHelper.sin((c + this.particleAge) / 7.0F) * 0.01F;
            float zz = MathHelper.sin((c + this.particleAge) / 8.0F) * 0.01F;
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

            this.colours[c][0] = 1.0F;
            this.colours[c][1] = 1.0F;
            this.colours[c][2] = 1.0F;
            this.colours[c][3] = 1.0F;
         }

         if (this.vecs.size() > 2 && this.rand.nextBoolean()) {
            int q = this.rand.nextInt(3);
            if (this.rand.nextBoolean()) {
               q = this.vecs.size() - 2;
            }
         }
      } else {
         this.setExpired();
      }
   }

   public void setGravity(float value) {
      this.particleGravity = value;
   }
}
