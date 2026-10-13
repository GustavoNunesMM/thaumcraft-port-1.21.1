package thaumcraft.client.fx.particles;

import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.lwjgl.opengl.GL11;

public class FXWispEG extends Particle {
   Entity target = null;
   double rx = 0.0;
   double ry = 0.0;
   double rz = 0.0;
   public int blendmode = 1;

   public FXWispEG(World world, double posX, double posY, double posZ, Entity target2) {
      super(world, posX, posY, posZ, 0.0, 0.0, 0.0);
      this.target = target2;
      this.motionX = this.rand.nextGaussian() * 0.03;
      this.motionY = -0.05;
      this.motionZ = this.rand.nextGaussian() * 0.03;
      this.particleScale *= 0.4F;
      this.particleMaxAge = (int)(40.0 / (Math.random() * 0.3 + 0.7));
      this.setSize(0.01F, 0.01F);
      this.prevPosX = posX;
      this.prevPosY = posY;
      this.prevPosZ = posZ;
      this.blendmode = 771;
      this.particleRed = this.rand.nextFloat() * 0.05F;
      this.particleGreen = this.rand.nextFloat() * 0.05F;
      this.particleBlue = this.rand.nextFloat() * 0.05F;
   }

   public void renderParticle(BufferBuilder wr, Entity entity, float f, float f1, float f2, float f3, float f4, float f5) {
      Entity e = Minecraft.getMinecraft().getRenderViewEntity();
      float agescale = 1.0F - (float)this.particleAge / this.particleMaxAge;
      float d6 = 1024.0F;
      double dist = new Vec3d(e.posX, e.posY, e.posZ)
         .squareDistanceTo(new Vec3d(this.posX, this.posY, this.posZ));
      float base = (float)(1.0 - Math.min((double)d6, dist) / d6);
      GL11.glColor4f(1.0F, 1.0F, 1.0F, 0.75F * base);
      float f10 = 0.5F * this.particleScale;
      float f11 = (float)(this.prevPosX + (this.posX - this.prevPosX) * f - interpPosX);
      float f12 = (float)(this.prevPosY + (this.posY - this.prevPosY) * f - interpPosY);
      float f13 = (float)(this.prevPosZ + (this.posZ - this.prevPosZ) * f - interpPosZ);
      float var8x = this.particleAge % 13 / 64.0F;
      float var9 = var8x + 0.015625F;
      float var10 = 0.046875F;
      float var11 = var10 + 0.015625F;
      int i = 240;
      int j = i >> 16 & 65535;
      int k = i & 65535;
      wr.pos(f11 - f1 * f10 - f4 * f10, f12 - f2 * f10, f13 - f3 * f10 - f5 * f10)
         .tex(var9, var11)
         .color(this.particleRed, this.particleGreen, this.particleBlue, 0.2F * agescale * base)
         .lightmap(j, k)
         .endVertex();
      wr.pos(f11 - f1 * f10 + f4 * f10, f12 + f2 * f10, f13 - f3 * f10 + f5 * f10)
         .tex(var9, var10)
         .color(this.particleRed, this.particleGreen, this.particleBlue, 0.2F * agescale * base)
         .lightmap(j, k)
         .endVertex();
      wr.pos(f11 + f1 * f10 + f4 * f10, f12 + f2 * f10, f13 + f3 * f10 + f5 * f10)
         .tex(var8x, var10)
         .color(this.particleRed, this.particleGreen, this.particleBlue, 0.2F * agescale * base)
         .lightmap(j, k)
         .endVertex();
      wr.pos(f11 + f1 * f10 - f4 * f10, f12 - f2 * f10, f13 + f3 * f10 - f5 * f10)
         .tex(var8x, var11)
         .color(this.particleRed, this.particleGreen, this.particleBlue, 0.2F * agescale * base)
         .lightmap(j, k)
         .endVertex();
   }

   public int getFXLayer() {
      return this.blendmode == 1 ? 0 : 1;
   }

   public void onUpdate() {
      super.onUpdate();
      if (this.target != null && !this.onGround) {
         this.posX = this.posX + this.target.motionX;
         this.posZ = this.posZ + this.target.motionZ;
      }
   }
}
