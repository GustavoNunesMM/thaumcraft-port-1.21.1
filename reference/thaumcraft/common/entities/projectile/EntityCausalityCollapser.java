package thaumcraft.common.entities.projectile;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.projectile.EntityThrowable;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import thaumcraft.client.fx.FXDispatcher;
import thaumcraft.common.entities.EntityFluxRift;
import thaumcraft.common.lib.utils.EntityUtils;

public class EntityCausalityCollapser extends EntityThrowable {
   public EntityCausalityCollapser(World par1World) {
      super(par1World);
   }

   public EntityCausalityCollapser(World par1World, EntityLivingBase par2EntityLiving) {
      super(par1World, par2EntityLiving);
   }

   public EntityCausalityCollapser(World par1World, double par2, double par4, double par6) {
      super(par1World, par2, par4, par6);
   }

   public void shoot(double x, double y, double z, float velocity, float inaccuracy) {
      super.shoot(x, y, z, 0.8F, inaccuracy);
   }

   public void onUpdate() {
      super.onUpdate();
      if (this.world.isRemote) {
         for (double i = 0.0; i < 3.0; i++) {
            double coeff = i / 3.0;
            FXDispatcher.INSTANCE
               .drawAlumentum(
                  (float)(this.prevPosX + (this.posX - this.prevPosX) * coeff),
                  (float)(this.prevPosY + (this.posY - this.prevPosY) * coeff) + this.height / 2.0F,
                  (float)(this.prevPosZ + (this.posZ - this.prevPosZ) * coeff),
                  0.0125F * (this.rand.nextFloat() - 0.5F),
                  0.0125F * (this.rand.nextFloat() - 0.5F),
                  0.0125F * (this.rand.nextFloat() - 0.5F),
                  0.8F + this.rand.nextFloat() * 0.2F,
                  0.3F + this.rand.nextFloat() * 0.1F,
                  this.rand.nextFloat() * 0.1F,
                  0.5F,
                  4.0F
               );
            FXDispatcher.INSTANCE
               .drawGenericParticles(
                  this.posX + this.world.rand.nextGaussian() * 0.2F,
                  this.posY + this.world.rand.nextGaussian() * 0.2F,
                  this.posZ + this.world.rand.nextGaussian() * 0.2F,
                  0.0,
                  0.0,
                  0.0,
                  1.0F,
                  1.0F,
                  1.0F,
                  0.7F,
                  false,
                  448,
                  8,
                  1,
                  8,
                  0,
                  0.3F,
                  0.0F,
                  1
               );
         }
      }
   }

   protected void onImpact(RayTraceResult par1RayTraceResult) {
      if (!this.world.isRemote) {
         this.world.createExplosion(this, this.posX, this.posY, this.posZ, 2.0F, true);

         for (EntityFluxRift fr : EntityUtils.getEntitiesInRange(
            this.world, this.posX, this.posY, this.posZ, this, EntityFluxRift.class, 3.0
         )) {
            fr.setCollapse(true);
         }

         this.setDead();
      }
   }
}
