package thaumcraft.client.fx.particles;

import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import org.lwjgl.opengl.GL11;

public class FXSmokeSpiral extends Particle {
   private float radius = 1.0F;
   private int start = 0;
   private int miny = 0;

   public FXSmokeSpiral(World world, double d, double d1, double d2, float radius, int start, int miny) {
      super(world, d, d1, d2, 0.0, 0.0, 0.0);
      this.particleGravity = -0.01F;
      this.motionX = this.motionY = this.motionZ = 0.0;
      this.particleScale *= 1.0F;
      this.particleMaxAge = 20 + world.rand.nextInt(10);
      this.setSize(0.01F, 0.01F);
      this.prevPosX = this.posX;
      this.prevPosY = this.posY;
      this.prevPosZ = this.posZ;
      this.radius = radius;
      this.start = start;
      this.miny = miny;
   }

   public void renderParticle(BufferBuilder wr, Entity entity, float f, float f1, float f2, float f3, float f4, float f5) {
      GL11.glColor4f(1.0F, 1.0F, 1.0F, 0.66F * this.particleAlpha);
      int particle = (int)(1.0F + (float)this.particleAge / this.particleMaxAge * 4.0F);
      float r1 = this.start + 720.0F * ((this.particleAge + f) / this.particleMaxAge);
      float r2 = 90.0F - 180.0F * ((this.particleAge + f) / this.particleMaxAge);
      float mX = -MathHelper.sin(r1 / 180.0F * (float) Math.PI) * MathHelper.cos(r2 / 180.0F * (float) Math.PI);
      float mZ = MathHelper.cos(r1 / 180.0F * (float) Math.PI) * MathHelper.cos(r2 / 180.0F * (float) Math.PI);
      float mY = -MathHelper.sin(r2 / 180.0F * (float) Math.PI);
      mX *= this.radius;
      mY *= this.radius;
      mZ *= this.radius;
      float var8x = particle % 16 / 64.0F;
      float var9 = var8x + 0.015625F;
      float var10 = particle / 16 / 64.0F;
      float var11 = var10 + 0.015625F;
      float var12 = 0.15F * this.particleScale;
      float var13 = (float)(this.posX + mX - interpPosX);
      float var14 = (float)(Math.max(this.posY + mY, (double)(this.miny + 0.1F)) - interpPosY);
      float var15 = (float)(this.posZ + mZ - interpPosZ);
      float var16 = 1.0F;
      int i = this.getBrightnessForRender(f);
      int j = i >> 16 & 65535;
      int k = i & 65535;
      wr.pos(var13 - f1 * var12 - f4 * var12, var14 - f2 * var12, var15 - f3 * var12 - f5 * var12)
         .tex(var9, var11)
         .color(this.particleRed * var16, this.particleGreen * var16, this.particleBlue * var16, 0.66F * this.particleAlpha)
         .lightmap(j, k)
         .endVertex();
      wr.pos(var13 - f1 * var12 + f4 * var12, var14 + f2 * var12, var15 - f3 * var12 + f5 * var12)
         .tex(var9, var10)
         .color(this.particleRed * var16, this.particleGreen * var16, this.particleBlue * var16, 0.66F * this.particleAlpha)
         .lightmap(j, k)
         .endVertex();
      wr.pos(var13 + f1 * var12 + f4 * var12, var14 + f2 * var12, var15 + f3 * var12 + f5 * var12)
         .tex(var8x, var10)
         .color(this.particleRed * var16, this.particleGreen * var16, this.particleBlue * var16, 0.66F * this.particleAlpha)
         .lightmap(j, k)
         .endVertex();
      wr.pos(var13 + f1 * var12 - f4 * var12, var14 - f2 * var12, var15 + f3 * var12 - f5 * var12)
         .tex(var8x, var11)
         .color(this.particleRed * var16, this.particleGreen * var16, this.particleBlue * var16, 0.66F * this.particleAlpha)
         .lightmap(j, k)
         .endVertex();
   }

   public int getFXLayer() {
      return 1;
   }

   public void onUpdate() {
      this.setAlphaF((float)(this.particleMaxAge - this.particleAge) / this.particleMaxAge);
      if (this.particleAge++ >= this.particleMaxAge) {
         this.setExpired();
      }
   }
}
