package thaumcraft.client.fx.particles;

import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.fml.client.FMLClientHandler;
import org.lwjgl.opengl.GL11;

public class FXBoreSparkle extends Particle {
   private Entity target;
   private double targetX;
   private double targetY;
   private double targetZ;
   public int particle = 24;

   public FXBoreSparkle(World par1World, double par2, double par4, double par6, double tx, double ty, double tz) {
      super(par1World, par2, par4, par6, 0.0, 0.0, 0.0);
      this.particleRed = this.particleGreen = this.particleBlue = 0.6F;
      this.particleScale = this.rand.nextFloat() * 0.5F + 0.5F;
      this.targetX = tx;
      this.targetY = ty;
      this.targetZ = tz;
      double dx = tx - this.posX;
      double dy = ty - this.posY;
      double dz = tz - this.posZ;
      int base = (int)(MathHelper.sqrt(dx * dx + dy * dy + dz * dz) * 10.0F);
      if (base < 1) {
         base = 1;
      }

      this.particleMaxAge = base / 2 + this.rand.nextInt(base);
      float f3 = 0.01F;
      this.motionX = (float)this.rand.nextGaussian() * f3;
      this.motionY = (float)this.rand.nextGaussian() * f3;
      this.motionZ = (float)this.rand.nextGaussian() * f3;
      this.particleRed = 0.2F;
      this.particleGreen = 0.6F + this.rand.nextFloat() * 0.3F;
      this.particleBlue = 0.2F;
      this.particleGravity = 0.2F;
      Entity renderentity = FMLClientHandler.instance().getClient().getRenderViewEntity();
      int visibleDistance = 64;
      if (!FMLClientHandler.instance().getClient().gameSettings.fancyGraphics) {
         visibleDistance = 32;
      }

      if (renderentity.getDistance(this.posX, this.posY, this.posZ) > visibleDistance) {
         this.particleMaxAge = 0;
      }
   }

   public FXBoreSparkle(World par1World, double par2, double par4, double par6, Entity t) {
      this(par1World, par2, par4, par6, t.posX, t.posY + t.getEyeHeight(), t.posZ);
      this.target = t;
   }

   public void renderParticle(BufferBuilder wr, Entity entity, float f, float f1, float f2, float f3, float f4, float f5) {
      float bob = MathHelper.sin(this.particleAge / 3.0F) * 0.5F + 1.0F;
      GL11.glColor4f(1.0F, 1.0F, 1.0F, 0.75F);
      int part = this.particleAge % 4;
      float var8x = part / 64.0F;
      float var9 = var8x + 0.015625F;
      float var10 = 0.0625F;
      float var11 = var10 + 0.015625F;
      float var12 = 0.1F * this.particleScale * bob;
      float var13 = (float)(this.prevPosX + (this.posX - this.prevPosX) * f - interpPosX);
      float var14 = (float)(this.prevPosY + (this.posY - this.prevPosY) * f - interpPosY);
      float var15 = (float)(this.prevPosZ + (this.posZ - this.prevPosZ) * f - interpPosZ);
      float var16 = 1.0F;
      int i = 240;
      int j = i >> 16 & 65535;
      int k = i & 65535;
      wr.pos(var13 - f1 * var12 - f4 * var12, var14 - f2 * var12, var15 - f3 * var12 - f5 * var12)
         .tex(var9, var11)
         .color(this.particleRed * var16, this.particleGreen * var16, this.particleBlue * var16, 1.0F)
         .lightmap(j, k)
         .endVertex();
      wr.pos(var13 - f1 * var12 + f4 * var12, var14 + f2 * var12, var15 - f3 * var12 + f5 * var12)
         .tex(var9, var10)
         .color(this.particleRed * var16, this.particleGreen * var16, this.particleBlue * var16, 1.0F)
         .lightmap(j, k)
         .endVertex();
      wr.pos(var13 + f1 * var12 + f4 * var12, var14 + f2 * var12, var15 + f3 * var12 + f5 * var12)
         .tex(var8x, var10)
         .color(this.particleRed * var16, this.particleGreen * var16, this.particleBlue * var16, 1.0F)
         .lightmap(j, k)
         .endVertex();
      wr.pos(var13 + f1 * var12 - f4 * var12, var14 - f2 * var12, var15 + f3 * var12 - f5 * var12)
         .tex(var8x, var11)
         .color(this.particleRed * var16, this.particleGreen * var16, this.particleBlue * var16, 1.0F)
         .lightmap(j, k)
         .endVertex();
   }

   public void onUpdate() {
      this.prevPosX = this.posX;
      this.prevPosY = this.posY;
      this.prevPosZ = this.posZ;
      if (this.target != null) {
         this.targetX = this.target.posX;
         this.targetY = this.target.posY + this.target.getEyeHeight();
         this.targetZ = this.target.posZ;
      }

      if (this.particleAge++ < this.particleMaxAge
         && (
            MathHelper.floor(this.posX) != MathHelper.floor(this.targetX)
               || MathHelper.floor(this.posY) != MathHelper.floor(this.targetY)
               || MathHelper.floor(this.posZ) != MathHelper.floor(this.targetZ)
         )) {
         this.move(this.motionX, this.motionY, this.motionZ);
         this.motionX *= 0.985;
         this.motionY *= 0.95;
         this.motionZ *= 0.985;
         double dx = this.targetX - this.posX;
         double dy = this.targetY - this.posY;
         double dz = this.targetZ - this.posZ;
         double d11 = MathHelper.sqrt(dx * dx + dy * dy + dz * dz);
         double clamp = Math.min(0.25, d11 / 15.0);
         if (d11 < 2.0) {
            this.particleScale *= 0.9F;
         }

         dx /= d11;
         dy /= d11;
         dz /= d11;
         this.motionX += dx * clamp;
         this.motionY += dy * clamp;
         this.motionZ += dz * clamp;
         this.motionX = MathHelper.clamp((float)this.motionX, -clamp, clamp);
         this.motionY = MathHelper.clamp((float)this.motionY, -clamp, clamp);
         this.motionZ = MathHelper.clamp((float)this.motionZ, -clamp, clamp);
         this.motionX = this.motionX + this.rand.nextGaussian() * 0.01;
         this.motionY = this.motionY + this.rand.nextGaussian() * 0.01;
         this.motionZ = this.motionZ + this.rand.nextGaussian() * 0.01;
      } else {
         this.setExpired();
      }
   }

   public void setGravity(float value) {
      this.particleGravity = value;
   }
}
