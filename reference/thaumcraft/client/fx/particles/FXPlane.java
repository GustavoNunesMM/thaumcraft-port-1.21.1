package thaumcraft.client.fx.particles;

import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import org.lwjgl.opengl.GL11;

public class FXPlane extends Particle {
   float angle;
   float angleYaw;
   float anglePitch;

   public FXPlane(World world, double d, double d1, double d2, double m, double m1, double m2, int life) {
      super(world, d, d1, d2, 0.0, 0.0, 0.0);
      this.particleRed = 1.0F;
      this.particleGreen = 1.0F;
      this.particleBlue = 1.0F;
      this.particleGravity = 0.0F;
      this.motionX = this.motionY = this.motionZ = 0.0;
      this.particleMaxAge = life;
      this.setSize(0.01F, 0.01F);
      this.prevPosX = this.posX;
      this.prevPosY = this.posY;
      this.prevPosZ = this.posZ;
      this.particleScale = 1.0F;
      this.particleAlpha = 0.0F;
      double dx = m - this.posX;
      double dy = m1 - this.posY;
      double dz = m2 - this.posZ;
      this.motionX = dx / this.particleMaxAge;
      this.motionY = dy / this.particleMaxAge;
      this.motionZ = dz / this.particleMaxAge;
      this.particleTextureIndexX = 22;
      this.particleTextureIndexY = 10;
      double d3 = MathHelper.sqrt(dx * dx + dz * dz);
      this.angleYaw = 0.0F;
      this.anglePitch = 0.0F;
      if (d3 >= 1.0E-7) {
         this.angleYaw = (float)(MathHelper.atan2(dz, dx) * 180.0 / Math.PI) - 90.0F;
         this.anglePitch = (float)(-(MathHelper.atan2(dy, d3) * 180.0 / Math.PI));
      }

      this.angle = (float)(this.rand.nextGaussian() * 20.0);
   }

   public int getFXLayer() {
      return 0;
   }

   public void renderParticle(BufferBuilder wr, Entity entityIn, float f, float f1, float f2, float f3, float f4, float f5) {
      Tessellator.getInstance().draw();
      GL11.glPushMatrix();
      GL11.glColor4f(1.0F, 1.0F, 1.0F, this.particleAlpha / 2.0F);
      float var13 = (float)(this.prevPosX + (this.posX - this.prevPosX) * f - interpPosX);
      float var14 = (float)(this.prevPosY + (this.posY - this.prevPosY) * f - interpPosY);
      float var15 = (float)(this.prevPosZ + (this.posZ - this.prevPosZ) * f - interpPosZ);
      GL11.glTranslated(var13, var14, var15);
      GL11.glRotatef(-this.angleYaw + 90.0F, 0.0F, 1.0F, 0.0F);
      GL11.glRotatef(this.anglePitch + 90.0F, 0.0F, 0.0F, 1.0F);
      GL11.glRotatef(this.angle, 0.0F, 1.0F, 0.0F);
      this.particleTextureIndexX = 22 + Math.round((this.particleAge + f) / this.particleMaxAge * 8.0F);
      float var8x = this.particleTextureIndexX / 32.0F;
      float var9 = var8x + 0.03125F;
      float var10 = this.particleTextureIndexY / 32.0F;
      float var11 = var10 + 0.03125F;
      float var12 = this.particleScale * (0.5F + (this.particleAge + f) / this.particleMaxAge);
      float var16 = 1.0F;
      int i = 240;
      int j = i >> 16 & 65535;
      int k = i & 65535;
      GL11.glDisable(2884);
      wr.begin(7, DefaultVertexFormats.PARTICLE_POSITION_TEX_COLOR_LMAP);
      wr.pos(-0.5 * var12, 0.5 * var12, 0.0)
         .tex(var9, var11)
         .color(this.particleRed * var16, this.particleGreen * var16, this.particleBlue * var16, this.particleAlpha / 2.0F)
         .lightmap(j, k)
         .endVertex();
      wr.pos(0.5 * var12, 0.5 * var12, 0.0)
         .tex(var9, var10)
         .color(this.particleRed * var16, this.particleGreen * var16, this.particleBlue * var16, this.particleAlpha / 2.0F)
         .lightmap(j, k)
         .endVertex();
      wr.pos(0.5 * var12, -0.5 * var12, 0.0)
         .tex(var8x, var10)
         .color(this.particleRed * var16, this.particleGreen * var16, this.particleBlue * var16, this.particleAlpha / 2.0F)
         .lightmap(j, k)
         .endVertex();
      wr.pos(-0.5 * var12, -0.5 * var12, 0.0)
         .tex(var8x, var11)
         .color(this.particleRed * var16, this.particleGreen * var16, this.particleBlue * var16, this.particleAlpha / 2.0F)
         .lightmap(j, k)
         .endVertex();
      Tessellator.getInstance().draw();
      GL11.glEnable(2884);
      GL11.glPopMatrix();
      wr.begin(7, DefaultVertexFormats.PARTICLE_POSITION_TEX_COLOR_LMAP);
   }

   public void onUpdate() {
      this.prevPosX = this.posX;
      this.prevPosY = this.posY;
      this.prevPosZ = this.posZ;
      float threshold = this.particleMaxAge / 5.0F;
      if (this.particleAge <= threshold) {
         this.particleAlpha = this.particleAge / threshold;
      } else {
         this.particleAlpha = (float)(this.particleMaxAge - this.particleAge) / this.particleMaxAge;
      }

      if (this.particleAge++ >= this.particleMaxAge) {
         this.setExpired();
      }

      this.posX = this.posX + this.motionX;
      this.posY = this.posY + this.motionY;
      this.posZ = this.posZ + this.motionZ;
   }

   public void setGravity(float value) {
      this.particleGravity = value;
   }
}
