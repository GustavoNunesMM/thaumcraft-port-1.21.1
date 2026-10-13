package thaumcraft.client.fx.particles;

import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

public class FXGenericP2P extends FXGeneric {
   private double targetX;
   private double targetY;
   private double targetZ;

   public FXGenericP2P(World world, double x, double y, double z, double xx, double yy, double zz) {
      super(world, x, y, z, 0.0, 0.0, 0.0);
      this.setSize(0.1F, 0.1F);
      this.prevPosX = this.posX;
      this.prevPosY = this.posY;
      this.prevPosZ = this.posZ;
      this.targetX = xx;
      this.targetY = yy;
      this.targetZ = zz;
      double dx = xx - this.posX;
      double dy = yy - this.posY;
      double dz = zz - this.posZ;
      int base = (int)(MathHelper.sqrt(dx * dx + dy * dy + dz * dz) * 3.0F);
      if (base < 1) {
         base = 1;
      }

      this.particleMaxAge = base / 2 + this.rand.nextInt(base);
      this.prevPosX = x;
      this.prevPosY = y;
      this.prevPosZ = z;
      this.particleTextureJitterX = 0.0F;
      this.particleTextureJitterY = 0.0F;
      float f3 = 0.01F;
      this.motionX = (float)world.rand.nextGaussian() * f3;
      this.motionY = (float)world.rand.nextGaussian() * f3;
      this.motionZ = (float)world.rand.nextGaussian() * f3;
      this.particleGravity = 0.2F;
   }

   @Override
   public void onUpdate() {
      super.onUpdate();
      double dx = this.targetX - this.posX;
      double dy = this.targetY - this.posY;
      double dz = this.targetZ - this.posZ;
      double d13 = 0.3;
      double d11 = MathHelper.sqrt(dx * dx + dy * dy + dz * dz);
      if (d11 < 4.0) {
         this.particleScale *= 0.9F;
         d13 = 0.6;
      }

      dx /= d11;
      dy /= d11;
      dz /= d11;
      this.motionX += dx * d13;
      this.motionY += dy * d13;
      this.motionZ += dz * d13;
      this.motionX = MathHelper.clamp((float)this.motionX, -0.35F, 0.35F);
      this.motionY = MathHelper.clamp((float)this.motionY, -0.35F, 0.35F);
      this.motionZ = MathHelper.clamp((float)this.motionZ, -0.35F, 0.35F);
   }
}
