package thaumcraft.client.fx.particles;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

public class FXGenericP2E extends FXGeneric {
   private Entity target;

   public FXGenericP2E(World world, double x, double y, double z, Entity target) {
      super(world, x, y, z, 0.0, 0.0, 0.0);
      this.setSize(0.1F, 0.1F);
      this.prevPosX = this.posX;
      this.prevPosY = this.posY;
      this.prevPosZ = this.posZ;
      this.target = target;
      double dx = target.posX - this.posX;
      double dy = target.posY - this.posY;
      double dz = target.posZ - this.posZ;
      int base = (int)(MathHelper.sqrt(dx * dx + dy * dy + dz * dz) * 5.0F);
      if (base < 1) {
         base = 1;
      }

      this.particleMaxAge = base;
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
      double dx = this.target.posX - this.posX;
      double dy = this.target.posY - this.posY;
      double dz = this.target.posZ - this.posZ;
      double d13 = 0.3;
      double d11 = MathHelper.sqrt(dx * dx + dy * dy + dz * dz);
      if (d11 < 4.0) {
         this.particleScale *= 0.9F;
         d13 = 0.6;
      }

      if (d11 < 0.25) {
         this.setExpired();
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
