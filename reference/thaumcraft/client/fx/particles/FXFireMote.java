package thaumcraft.client.fx.particles;

import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class FXFireMote extends Particle {
   float baseScale = 0.0F;
   float baseAlpha = 1.0F;
   int glowlayer = 0;

   public FXFireMote(World worldIn, double x, double y, double z, double vx, double vy, double vz, float r, float g, float b, float scale, int layer) {
      super(worldIn, x, y, z, 0.0, 0.0, 0.0);
      float colorR = r;
      float colorG = g;
      float colorB = b;
      if (r > 1.0) {
         colorR = r / 255.0F;
      }

      if (g > 1.0) {
         colorG = g / 255.0F;
      }

      if (b > 1.0) {
         colorB = b / 255.0F;
      }

      this.glowlayer = layer;
      this.setRBGColorF(colorR, colorG, colorB);
      this.particleMaxAge = 16;
      this.particleScale = scale;
      this.baseScale = scale;
      this.motionX = vx;
      this.motionY = vy;
      this.motionZ = vz;
      this.particleAngle = (float) (Math.PI * 2);
      this.setParticleTextureIndex(7);
   }

   public void setParticleTextureIndex(int particleTextureIndex) {
      this.particleTextureIndexX = particleTextureIndex % 64;
      this.particleTextureIndexY = particleTextureIndex / 64;
   }

   public void renderParticle(
      BufferBuilder worldRendererIn,
      Entity entityIn,
      float partialTicks,
      float rotationX,
      float rotationZ,
      float rotationYZ,
      float rotationXY,
      float rotationXZ
   ) {
      float f = this.particleTextureIndexX / 64.0F;
      float f1 = f + 0.015625F;
      float f2 = this.particleTextureIndexY / 64.0F;
      float f3 = f2 + 0.015625F;
      float f4 = 0.1F * this.particleScale;
      if (this.particleTexture != null) {
         f = this.particleTexture.getMinU();
         f1 = this.particleTexture.getMaxU();
         f2 = this.particleTexture.getMinV();
         f3 = this.particleTexture.getMaxV();
      }

      float f5 = (float)(this.prevPosX + (this.posX - this.prevPosX) * partialTicks - interpPosX);
      float f6 = (float)(this.prevPosY + (this.posY - this.prevPosY) * partialTicks - interpPosY);
      float f7 = (float)(this.prevPosZ + (this.posZ - this.prevPosZ) * partialTicks - interpPosZ);
      int i = this.getBrightnessForRender(partialTicks);
      int j = i >> 16 & 65535;
      int k = i & 65535;
      Vec3d[] avec3d = new Vec3d[]{
         new Vec3d(-rotationX * f4 - rotationXY * f4, -rotationZ * f4, -rotationYZ * f4 - rotationXZ * f4),
         new Vec3d(-rotationX * f4 + rotationXY * f4, rotationZ * f4, -rotationYZ * f4 + rotationXZ * f4),
         new Vec3d(rotationX * f4 + rotationXY * f4, rotationZ * f4, rotationYZ * f4 + rotationXZ * f4),
         new Vec3d(rotationX * f4 - rotationXY * f4, -rotationZ * f4, rotationYZ * f4 - rotationXZ * f4)
      };
      if (this.particleAngle != 0.0F) {
         float f8 = this.particleAngle + (this.particleAngle - this.prevParticleAngle) * partialTicks;
         float f9 = MathHelper.cos(f8 * 0.5F);
         float f10 = MathHelper.sin(f8 * 0.5F) * (float)cameraViewDir.x;
         float f11 = MathHelper.sin(f8 * 0.5F) * (float)cameraViewDir.y;
         float f12 = MathHelper.sin(f8 * 0.5F) * (float)cameraViewDir.z;
         Vec3d vec3d = new Vec3d(f10, f11, f12);

         for (int l = 0; l < 4; l++) {
            avec3d[l] = vec3d.scale(2.0 * avec3d[l].dotProduct(vec3d))
               .add(avec3d[l].scale(f9 * f9 - vec3d.dotProduct(vec3d)))
               .add(vec3d.crossProduct(avec3d[l]).scale(2.0F * f9));
         }
      }

      worldRendererIn.pos(f5 + avec3d[0].x, f6 + avec3d[0].y, f7 + avec3d[0].z)
         .tex(f1, f3)
         .color(this.particleRed, this.particleGreen, this.particleBlue, this.particleAlpha * this.baseAlpha)
         .lightmap(j, k)
         .endVertex();
      worldRendererIn.pos(f5 + avec3d[1].x, f6 + avec3d[1].y, f7 + avec3d[1].z)
         .tex(f1, f2)
         .color(this.particleRed, this.particleGreen, this.particleBlue, this.particleAlpha * this.baseAlpha)
         .lightmap(j, k)
         .endVertex();
      worldRendererIn.pos(f5 + avec3d[2].x, f6 + avec3d[2].y, f7 + avec3d[2].z)
         .tex(f, f2)
         .color(this.particleRed, this.particleGreen, this.particleBlue, this.particleAlpha * this.baseAlpha)
         .lightmap(j, k)
         .endVertex();
      worldRendererIn.pos(f5 + avec3d[3].x, f6 + avec3d[3].y, f7 + avec3d[3].z)
         .tex(f, f3)
         .color(this.particleRed, this.particleGreen, this.particleBlue, this.particleAlpha * this.baseAlpha)
         .lightmap(j, k)
         .endVertex();
   }

   public int getBrightnessForRender(float pTicks) {
      return 255;
   }

   public int getFXLayer() {
      return this.glowlayer;
   }

   public void onUpdate() {
      super.onUpdate();
      if (this.world.rand.nextInt(6) == 0) {
         this.particleAge++;
      }

      if (this.particleAge >= this.particleMaxAge) {
         this.setExpired();
      }

      float lifespan = (float)this.particleAge / this.particleMaxAge;
      this.particleScale = this.baseScale - this.baseScale * lifespan;
      this.baseAlpha = 1.0F - lifespan;
      this.prevParticleAngle = this.particleAngle++;
   }
}
