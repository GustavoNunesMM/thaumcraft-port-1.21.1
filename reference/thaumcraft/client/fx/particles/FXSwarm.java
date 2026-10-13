package thaumcraft.client.fx.particles;

import java.util.ArrayList;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.lwjgl.opengl.GL11;
import thaumcraft.common.lib.SoundsTC;

public class FXSwarm extends Particle {
   private Entity target;
   private float turnSpeed = 10.0F;
   private float speed = 0.2F;
   int deathtimer = 0;
   private static ArrayList<Long> buzzcount = new ArrayList<>();
   float rotationPitch;
   float rotationYaw;
   public int particle = 40;

   public FXSwarm(World par1World, double x, double y, double z, Entity target, float r, float g, float b) {
      super(par1World, x, y, z, 0.0, 0.0, 0.0);
      this.particleRed = r;
      this.particleGreen = g;
      this.particleBlue = b;
      this.particleScale = this.rand.nextFloat() * 0.5F + 1.0F;
      this.target = target;
      float f3 = 0.2F;
      this.motionX = (this.rand.nextFloat() - this.rand.nextFloat()) * f3;
      this.motionY = (this.rand.nextFloat() - this.rand.nextFloat()) * f3;
      this.motionZ = (this.rand.nextFloat() - this.rand.nextFloat()) * f3;
      this.particleGravity = 0.1F;
   }

   public FXSwarm(World par1World, double x, double y, double z, Entity target, float r, float g, float b, float sp, float ts, float pg) {
      this(par1World, x, y, z, target, r, g, b);
      this.speed = sp;
      this.turnSpeed = ts;
      this.particleGravity = pg;
   }

   public void renderParticle(BufferBuilder wr, Entity entity, float f, float f1, float f2, float f3, float f4, float f5) {
      float bob = MathHelper.sin(this.particleAge / 3.0F) * 0.25F + 1.0F;
      GL11.glColor4f(1.0F, 1.0F, 1.0F, 0.75F);
      int part = 7 + this.particleAge % 8;
      float var8x = part / 64.0F;
      float var9 = var8x + 0.015625F;
      float var10 = 0.0625F;
      float var11 = var10 + 0.015625F;
      float var12 = 0.1F * this.particleScale * bob;
      float var13 = (float)(this.prevPosX + (this.posX - this.prevPosX) * f - interpPosX);
      float var14 = (float)(this.prevPosY + (this.posY - this.prevPosY) * f - interpPosY);
      float var15 = (float)(this.prevPosZ + (this.posZ - this.prevPosZ) * f - interpPosZ);
      float var16 = 1.0F;
      float trans = (50.0F - this.deathtimer) / 50.0F;
      int i = 240;
      int j = i >> 16 & 65535;
      int k = i & 65535;
      float dd = 1.0F;
      if (this.target instanceof EntityLivingBase && ((EntityLivingBase)this.target).hurtTime > 0) {
         dd = 2.0F;
      }

      wr.pos(var13 - f1 * var12 - f4 * var12, var14 - f2 * var12, var15 - f3 * var12 - f5 * var12)
         .tex(var9, var11)
         .color(this.particleRed * var16, this.particleGreen * var16 / dd, this.particleBlue * var16 / dd, trans)
         .lightmap(j, k)
         .endVertex();
      wr.pos(var13 - f1 * var12 + f4 * var12, var14 + f2 * var12, var15 - f3 * var12 + f5 * var12)
         .tex(var9, var10)
         .color(this.particleRed * var16, this.particleGreen * var16 / dd, this.particleBlue * var16 / dd, trans)
         .lightmap(j, k)
         .endVertex();
      wr.pos(var13 + f1 * var12 + f4 * var12, var14 + f2 * var12, var15 + f3 * var12 + f5 * var12)
         .tex(var8x, var10)
         .color(this.particleRed * var16, this.particleGreen * var16 / dd, this.particleBlue * var16 / dd, trans)
         .lightmap(j, k)
         .endVertex();
      wr.pos(var13 + f1 * var12 - f4 * var12, var14 - f2 * var12, var15 + f3 * var12 - f5 * var12)
         .tex(var8x, var11)
         .color(this.particleRed * var16, this.particleGreen * var16 / dd, this.particleBlue * var16 / dd, trans)
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
      this.particleAge++;
      if (this.target == null || this.target.isDead || this.target instanceof EntityLivingBase && ((EntityLivingBase)this.target).deathTime > 0) {
         this.deathtimer++;
         this.motionX *= 0.9;
         this.motionZ *= 0.9;
         this.motionY = this.motionY - this.particleGravity / 2.0F;
         if (this.deathtimer > 50) {
            this.setExpired();
         }
      } else {
         this.motionY = this.motionY + this.particleGravity;
      }

      this.move(this.motionX, this.motionY, this.motionZ);
      this.motionX *= 0.985;
      this.motionY *= 0.985;
      this.motionZ *= 0.985;
      if (this.target != null
         && !this.target.isDead
         && (!(this.target instanceof EntityLivingBase) || ((EntityLivingBase)this.target).deathTime <= 0)) {
         boolean hurt = false;
         if (this.target instanceof EntityLivingBase) {
            hurt = ((EntityLivingBase)this.target).hurtTime > 0;
         }

         Vec3d v1 = new Vec3d(this.posX, this.posY, this.posZ);
         if (v1.squareDistanceTo(this.target.posX, this.target.posY, this.target.posZ) > this.target.width && !hurt) {
            this.faceEntity(
               this.target,
               this.turnSpeed / 2.0F + this.rand.nextInt((int)(this.turnSpeed / 2.0F)),
               this.turnSpeed / 2.0F + this.rand.nextInt((int)(this.turnSpeed / 2.0F))
            );
         } else {
            this.faceEntity(
               this.target,
               -(this.turnSpeed / 2.0F + this.rand.nextInt((int)(this.turnSpeed / 2.0F))),
               -(this.turnSpeed / 2.0F + this.rand.nextInt((int)(this.turnSpeed / 2.0F)))
            );
         }

         this.motionX = -MathHelper.sin(this.rotationYaw / 180.0F * (float) Math.PI)
            * MathHelper.cos(this.rotationPitch / 180.0F * (float) Math.PI);
         this.motionZ = MathHelper.cos(this.rotationYaw / 180.0F * (float) Math.PI)
            * MathHelper.cos(this.rotationPitch / 180.0F * (float) Math.PI);
         this.motionY = -MathHelper.sin(this.rotationPitch / 180.0F * (float) Math.PI);
         this.setHeading(this.motionX, this.motionY, this.motionZ, this.speed, 15.0F);
      }

      if (buzzcount.size() < 3
         && this.rand.nextInt(50) == 0
         && this.world.getClosestPlayer(this.posX, this.posY, this.posZ, 8.0, false) != null) {
         this.world
            .playSound(
               this.posX,
               this.posY,
               this.posZ,
               SoundsTC.fly,
               SoundCategory.HOSTILE,
               0.03F,
               0.5F + this.rand.nextFloat() * 0.4F,
               false
            );
         buzzcount.add(System.nanoTime() + 1500000L);
      }

      if (buzzcount.size() >= 3 && buzzcount.get(0) < System.nanoTime()) {
         buzzcount.remove(0);
      }
   }

   public void faceEntity(Entity par1Entity, float par2, float par3) {
      double d0 = par1Entity.posX - this.posX;
      double d1 = par1Entity.posZ - this.posZ;
      double d2 = (par1Entity.getEntityBoundingBox().minY + par1Entity.getEntityBoundingBox().maxY) / 2.0
         - (this.getBoundingBox().minY + this.getBoundingBox().maxY) / 2.0;
      double d3 = MathHelper.sqrt(d0 * d0 + d1 * d1);
      float f2 = (float)(Math.atan2(d1, d0) * 180.0 / Math.PI) - 90.0F;
      float f3 = (float)(-(Math.atan2(d2, d3) * 180.0 / Math.PI));
      this.rotationPitch = this.updateRotation(this.rotationPitch, f3, par3);
      this.rotationYaw = this.updateRotation(this.rotationYaw, f2, par2);
   }

   private float updateRotation(float par1, float par2, float par3) {
      float f3 = MathHelper.wrapDegrees(par2 - par1);
      if (f3 > par3) {
         f3 = par3;
      }

      if (f3 < -par3) {
         f3 = -par3;
      }

      return par1 + f3;
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
}
