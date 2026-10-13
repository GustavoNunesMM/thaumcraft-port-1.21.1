package thaumcraft.client.fx.particles;

import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import org.lwjgl.opengl.GL11;

public class FXSlimyBubble extends Particle {
   int particle = 144;

   public FXSlimyBubble(World world, double d, double d1, double d2, float f) {
      super(world, d, d1, d2, 0.0, 0.0, 0.0);
      this.particleRed = 1.0F;
      this.particleGreen = 1.0F;
      this.particleBlue = 1.0F;
      this.particleGravity = 0.0F;
      this.motionX = this.motionY = this.motionZ = 0.0;
      this.particleScale = f;
      this.particleMaxAge = 15 + world.rand.nextInt(5);
      this.setSize(0.01F, 0.01F);
   }

   public void renderParticle(BufferBuilder wr, Entity entity, float f, float f1, float f2, float f3, float f4, float f5) {
      GL11.glColor4f(1.0F, 1.0F, 1.0F, this.particleAlpha);
      float var8x = this.particle % 16 / 64.0F;
      float var9 = var8x + 0.015625F;
      float var10 = this.particle / 16 / 64.0F;
      float var11 = var10 + 0.015625F;
      float var12 = this.particleScale;
      float var13 = (float)(this.prevPosX + (this.posX - this.prevPosX) * f - interpPosX);
      float var14 = (float)(this.prevPosY + (this.posY - this.prevPosY) * f - interpPosY);
      float var15 = (float)(this.prevPosZ + (this.posZ - this.prevPosZ) * f - interpPosZ);
      int i = this.getBrightnessForRender(f);
      int j = i >> 16 & 65535;
      int k = i & 65535;
      wr.pos(var13 - f1 * var12 - f4 * var12, var14 - f2 * var12, var15 - f3 * var12 - f5 * var12)
         .tex(var9, var11)
         .color(this.particleRed, this.particleGreen, this.particleBlue, this.particleAlpha)
         .lightmap(j, k)
         .endVertex();
      wr.pos(var13 - f1 * var12 + f4 * var12, var14 + f2 * var12, var15 - f3 * var12 + f5 * var12)
         .tex(var9, var10)
         .color(this.particleRed, this.particleGreen, this.particleBlue, this.particleAlpha)
         .lightmap(j, k)
         .endVertex();
      wr.pos(var13 + f1 * var12 + f4 * var12, var14 + f2 * var12, var15 + f3 * var12 + f5 * var12)
         .tex(var8x, var10)
         .color(this.particleRed, this.particleGreen, this.particleBlue, this.particleAlpha)
         .lightmap(j, k)
         .endVertex();
      wr.pos(var13 + f1 * var12 - f4 * var12, var14 - f2 * var12, var15 + f3 * var12 - f5 * var12)
         .tex(var8x, var11)
         .color(this.particleRed, this.particleGreen, this.particleBlue, this.particleAlpha)
         .lightmap(j, k)
         .endVertex();
   }

   public int getFXLayer() {
      return 1;
   }

   public void onUpdate() {
      this.prevPosX = this.posX;
      this.prevPosY = this.posY;
      this.prevPosZ = this.posZ;
      if (this.particleAge++ >= this.particleMaxAge) {
         this.setExpired();
      }

      if (this.particleAge - 1 < 6) {
         this.particle = 144 + this.particleAge / 2;
         if (this.particleAge == 5) {
            this.posY += 0.1;
         }
      } else if (this.particleAge < this.particleMaxAge - 4) {
         this.motionY += 0.005;
         this.particle = 147 + this.particleAge % 4 / 2;
      } else {
         this.motionY /= 2.0;
         this.particle = 150 - (this.particleMaxAge - this.particleAge) / 2;
      }

      this.posY = this.posY + this.motionY;
   }
}
