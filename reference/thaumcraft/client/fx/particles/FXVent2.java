package thaumcraft.client.fx.particles;

import java.awt.Color;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.fml.client.FMLClientHandler;
import org.lwjgl.opengl.GL11;

public class FXVent2 extends Particle {
   float grav = 0.0F;
   float psm = 1.0F;

   public FXVent2(World par1World, double par2, double par4, double par6, double par8, double par10, double par12, int color) {
      super(par1World, par2, par4, par6, par8, par10, par12);
      this.setSize(0.02F, 0.02F);
      this.particleScale = this.rand.nextFloat() * 0.1F + 0.05F;
      this.motionX = par8;
      this.motionY = par10;
      this.motionZ = par12;
      Color c = new Color(color);
      this.particleRed = (float)MathHelper.clamp(c.getRed() / 255.0F + this.rand.nextGaussian() * 0.05, 0.0, 1.0);
      this.particleBlue = (float)MathHelper.clamp(c.getBlue() / 255.0F + this.rand.nextGaussian() * 0.05, 0.0, 1.0);
      this.particleGreen = (float)MathHelper.clamp(c.getGreen() / 255.0F + this.rand.nextGaussian() * 0.05, 0.0, 1.0);
      Entity renderentity = FMLClientHandler.instance().getClient().getRenderViewEntity();
      int visibleDistance = 50;
      if (!FMLClientHandler.instance().getClient().gameSettings.fancyGraphics) {
         visibleDistance = 25;
      }

      if (renderentity.getDistance(this.posX, this.posY, this.posZ) > visibleDistance) {
         this.particleMaxAge = 0;
      }

      this.prevPosX = this.posX;
      this.prevPosY = this.posY;
      this.prevPosZ = this.posZ;
      this.grav = (float)(this.rand.nextGaussian() * 0.0075);
   }

   public void setScale(float f) {
      this.particleScale *= f;
      this.psm *= f;
   }

   public void setHeading(double par1, double par3, double par5, float par7, float par8) {
      float f2 = MathHelper.sqrt(par1 * par1 + par3 * par3 + par5 * par5);
      par1 /= f2;
      par3 /= f2;
      par5 /= f2;
      par1 += this.rand.nextGaussian() * (this.rand.nextBoolean() ? -1 : 1) * 0.0075F * par8;
      par3 += this.rand.nextGaussian() * (this.rand.nextBoolean() ? -1 : 1) * 0.0075F * par8;
      par5 += this.rand.nextGaussian() * (this.rand.nextBoolean() ? -1 : 1) * 0.0075F * par8;
      par1 *= par7;
      par3 *= par7;
      par5 *= par7;
      this.motionX = par1;
      this.motionY = par3;
      this.motionZ = par5;
   }

   public void onUpdate() {
      this.prevPosX = this.posX;
      this.prevPosY = this.posY;
      this.prevPosZ = this.posZ;
      this.particleAge++;
      if (this.particleScale >= this.psm) {
         this.setExpired();
      }

      this.motionY = this.motionY + this.grav;
      this.move(this.motionX, this.motionY, this.motionZ);
      this.motionX *= 0.8500000190734863;
      this.motionY *= 0.8500000190734863;
      this.motionZ *= 0.8500000190734863;
      if (this.particleScale < this.psm) {
         this.particleScale = (float)(this.particleScale * 1.2);
      }

      if (this.particleScale > this.psm) {
         this.particleScale = this.psm;
      }

      if (this.onGround) {
         this.motionX *= 0.7F;
         this.motionZ *= 0.7F;
      }
   }

   public void setRGB(float r, float g, float b) {
      this.particleRed = r;
      this.particleGreen = g;
      this.particleBlue = b;
   }

   public void renderParticle(BufferBuilder wr, Entity entity, float f, float f1, float f2, float f3, float f4, float f5) {
      GL11.glColor4f(1.0F, 1.0F, 1.0F, 0.33F);
      int part = (int)(1.0F + this.particleScale / this.psm * 4.0F);
      float var8x = part % 16 / 64.0F;
      float var9 = var8x + 0.015625F;
      float var10 = part / 64 / 64.0F;
      float var11 = var10 + 0.015625F;
      float var12 = 0.3F * this.particleScale;
      float var13 = (float)(this.prevPosX + (this.posX - this.prevPosX) * f - interpPosX);
      float var14 = (float)(this.prevPosY + (this.posY - this.prevPosY) * f - interpPosY);
      float var15 = (float)(this.prevPosZ + (this.posZ - this.prevPosZ) * f - interpPosZ);
      float var16 = 1.0F;
      int i = this.getBrightnessForRender(f);
      int j = i >> 16 & 65535;
      int k = i & 65535;
      float alpha = this.particleAlpha * ((this.psm - this.particleScale) / this.psm);
      wr.pos(var13 - f1 * var12 - f4 * var12, var14 - f2 * var12, var15 - f3 * var12 - f5 * var12)
         .tex(var9, var11)
         .color(this.particleRed * var16, this.particleGreen * var16, this.particleBlue * var16, alpha)
         .lightmap(j, k)
         .endVertex();
      wr.pos(var13 - f1 * var12 + f4 * var12, var14 + f2 * var12, var15 - f3 * var12 + f5 * var12)
         .tex(var9, var10)
         .color(this.particleRed * var16, this.particleGreen * var16, this.particleBlue * var16, alpha)
         .lightmap(j, k)
         .endVertex();
      wr.pos(var13 + f1 * var12 + f4 * var12, var14 + f2 * var12, var15 + f3 * var12 + f5 * var12)
         .tex(var8x, var10)
         .color(this.particleRed * var16, this.particleGreen * var16, this.particleBlue * var16, alpha)
         .lightmap(j, k)
         .endVertex();
      wr.pos(var13 + f1 * var12 - f4 * var12, var14 - f2 * var12, var15 + f3 * var12 - f5 * var12)
         .tex(var8x, var11)
         .color(this.particleRed * var16, this.particleGreen * var16, this.particleBlue * var16, alpha)
         .lightmap(j, k)
         .endVertex();
   }

   public int getFXLayer() {
      return 1;
   }
}
