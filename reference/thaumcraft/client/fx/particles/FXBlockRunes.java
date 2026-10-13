package thaumcraft.client.fx.particles;

import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import org.lwjgl.opengl.GL11;

public class FXBlockRunes extends Particle {
   double ofx = 0.0;
   double ofy = 0.0;
   float rotation = 0.0F;
   int runeIndex = 0;

   public FXBlockRunes(World world, double d, double d1, double d2, float f1, float f2, float f3, int m) {
      super(world, d, d1, d2, 0.0, 0.0, 0.0);
      if (f1 == 0.0F) {
         f1 = 1.0F;
      }

      this.rotation = this.rand.nextInt(4) * 90;
      this.particleRed = f1;
      this.particleGreen = f2;
      this.particleBlue = f3;
      this.particleGravity = 0.0F;
      this.motionX = this.motionY = this.motionZ = 0.0;
      this.particleMaxAge = 3 * m;
      this.setSize(0.01F, 0.01F);
      this.prevPosX = this.posX;
      this.prevPosY = this.posY;
      this.prevPosZ = this.posZ;
      this.runeIndex = (int)(Math.random() * 16.0 + 224.0);
      this.ofx = this.rand.nextFloat() * 0.2;
      this.ofy = -0.3 + this.rand.nextFloat() * 0.6;
      this.particleScale = (float)(1.0 + this.rand.nextGaussian() * 0.1F);
      this.particleAlpha = 0.0F;
   }

   public void setScale(float s) {
      this.particleScale = s;
   }

   public void setOffsetX(double f) {
      this.ofx = f;
   }

   public void renderParticle(BufferBuilder wr, Entity entityIn, float f, float f1, float f2, float f3, float f4, float f5) {
      Tessellator.getInstance().draw();
      GL11.glPushMatrix();
      GL11.glColor4f(1.0F, 1.0F, 1.0F, this.particleAlpha / 2.0F);
      float var13 = (float)(this.prevPosX + (this.posX - this.prevPosX) * f - interpPosX);
      float var14 = (float)(this.prevPosY + (this.posY - this.prevPosY) * f - interpPosY);
      float var15 = (float)(this.prevPosZ + (this.posZ - this.prevPosZ) * f - interpPosZ);
      GL11.glTranslated(var13, var14, var15);
      GL11.glRotatef(this.rotation, 0.0F, 1.0F, 0.0F);
      GL11.glRotatef(90.0F, 0.0F, 0.0F, 1.0F);
      GL11.glTranslated(this.ofx, this.ofy, -0.51);
      float var8x = this.runeIndex % 16 / 64.0F;
      float var9 = var8x + 0.015625F;
      float var10 = 0.09375F;
      float var11 = var10 + 0.015625F;
      float var12 = 0.3F * this.particleScale;
      float var16 = 1.0F;
      wr.begin(7, DefaultVertexFormats.PARTICLE_POSITION_TEX_COLOR_LMAP);
      int i = 240;
      int j = i >> 16 & 65535;
      int k = i & 65535;
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

      this.motionY = this.motionY - 0.04 * this.particleGravity;
      this.posX = this.posX + this.motionX;
      this.posY = this.posY + this.motionY;
      this.posZ = this.posZ + this.motionZ;
   }

   public void setGravity(float value) {
      this.particleGravity = value;
   }
}
