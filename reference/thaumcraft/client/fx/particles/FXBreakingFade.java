package thaumcraft.client.fx.particles;

import net.minecraft.client.particle.ParticleBreaking;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class FXBreakingFade extends ParticleBreaking {
   public FXBreakingFade(
      World worldIn,
      double p_i1197_2_,
      double p_i1197_4_,
      double p_i1197_6_,
      double p_i1197_8_,
      double p_i1197_10_,
      double p_i1197_12_,
      Item p_i1197_14_,
      int p_i1197_15_
   ) {
      super(worldIn, p_i1197_2_, p_i1197_4_, p_i1197_6_, p_i1197_8_, p_i1197_10_, p_i1197_12_, p_i1197_14_, p_i1197_15_);
   }

   public FXBreakingFade(World worldIn, double p_i1196_2_, double p_i1196_4_, double p_i1196_6_, Item p_i1196_8_, int p_i1196_9_) {
      super(worldIn, p_i1196_2_, p_i1196_4_, p_i1196_6_, p_i1196_8_, p_i1196_9_);
   }

   public FXBreakingFade(World worldIn, double p_i1195_2_, double p_i1195_4_, double p_i1195_6_, Item p_i1195_8_) {
      super(worldIn, p_i1195_2_, p_i1195_4_, p_i1195_6_, p_i1195_8_);
   }

   public void setParticleMaxAge(int particleMaxAge) {
      this.particleMaxAge = particleMaxAge;
   }

   public void setParticleGravity(float f) {
      this.particleGravity = f;
   }

   public int getFXLayer() {
      return 1;
   }

   public void setSpeed(double x, double y, double z) {
      this.motionX = x;
      this.motionY = y;
      this.motionZ = z;
   }

   public void renderParticle(
      BufferBuilder buffer,
      Entity entityIn,
      float partialTicks,
      float rotationX,
      float rotationZ,
      float rotationYZ,
      float rotationXY,
      float rotationXZ
   ) {
      GlStateManager.depthMask(false);
      float f6 = (this.particleTextureIndexX + this.particleTextureJitterX / 4.0F) / 16.0F;
      float f7 = f6 + 0.015609375F;
      float f8 = (this.particleTextureIndexY + this.particleTextureJitterY / 4.0F) / 16.0F;
      float f9 = f8 + 0.015609375F;
      float f10 = 0.1F * this.particleScale;
      float fade = 1.0F - (float)this.particleAge / this.particleMaxAge;
      if (this.particleTexture != null) {
         f6 = this.particleTexture.getInterpolatedU(this.particleTextureJitterX / 4.0F * 16.0F);
         f7 = this.particleTexture.getInterpolatedU((this.particleTextureJitterX + 1.0F) / 4.0F * 16.0F);
         f8 = this.particleTexture.getInterpolatedV(this.particleTextureJitterY / 4.0F * 16.0F);
         f9 = this.particleTexture.getInterpolatedV((this.particleTextureJitterY + 1.0F) / 4.0F * 16.0F);
      }

      int i = this.getBrightnessForRender(partialTicks);
      int j = i >> 16 & 65535;
      int k = i & 65535;
      float f11 = (float)(this.prevPosX + (this.posX - this.prevPosX) * partialTicks - interpPosX);
      float f12 = (float)(this.prevPosY + (this.posY - this.prevPosY) * partialTicks - interpPosY);
      float f13 = (float)(this.prevPosZ + (this.posZ - this.prevPosZ) * partialTicks - interpPosZ);
      buffer.pos(f11 - rotationX * f10 - rotationXY * f10, f12 - rotationZ * f10, f13 - rotationYZ * f10 - rotationXZ * f10)
         .tex(f6, f9)
         .color(this.particleRed, this.particleGreen, this.particleBlue, this.particleAlpha * fade)
         .lightmap(j, k)
         .endVertex();
      buffer.pos(f11 - rotationX * f10 + rotationXY * f10, f12 + rotationZ * f10, f13 - rotationYZ * f10 + rotationXZ * f10)
         .tex(f6, f8)
         .color(this.particleRed, this.particleGreen, this.particleBlue, this.particleAlpha * fade)
         .lightmap(j, k)
         .endVertex();
      buffer.pos(f11 + rotationX * f10 + rotationXY * f10, f12 + rotationZ * f10, f13 + rotationYZ * f10 + rotationXZ * f10)
         .tex(f7, f8)
         .color(this.particleRed, this.particleGreen, this.particleBlue, this.particleAlpha * fade)
         .lightmap(j, k)
         .endVertex();
      buffer.pos(f11 + rotationX * f10 - rotationXY * f10, f12 - rotationZ * f10, f13 + rotationYZ * f10 - rotationXZ * f10)
         .tex(f7, f9)
         .color(this.particleRed, this.particleGreen, this.particleBlue, this.particleAlpha * fade)
         .lightmap(j, k)
         .endVertex();
      GlStateManager.depthMask(true);
   }

   public void boom() {
      float f = (float)(Math.random() + Math.random() + 1.0) * 0.15F;
      float f1 = MathHelper.sqrt(
         this.motionX * this.motionX + this.motionY * this.motionY + this.motionZ * this.motionZ
      );
      this.motionX = this.motionX / f1 * f * 0.9640000000596046;
      this.motionY = this.motionY / f1 * f * 0.9640000000596046 + 0.1F;
      this.motionZ = this.motionZ / f1 * f * 0.9640000000596046;
   }
}
