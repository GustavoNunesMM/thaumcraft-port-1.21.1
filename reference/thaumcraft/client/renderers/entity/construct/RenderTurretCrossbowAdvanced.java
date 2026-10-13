package thaumcraft.client.renderers.entity.construct;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderLiving;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.opengl.GL11;
import thaumcraft.client.lib.obj.AdvancedModelLoader;
import thaumcraft.client.lib.obj.IModelCustom;
import thaumcraft.common.entities.construct.EntityTurretCrossbow;

public class RenderTurretCrossbowAdvanced extends RenderLiving {
   private IModelCustom model;
   private static final ResourceLocation TURMODEL = new ResourceLocation("thaumcraft", "models/obj/crossbow_advanced.obj");
   private static final ResourceLocation rl = new ResourceLocation("thaumcraft", "textures/entity/crossbow_advanced.png");

   public RenderTurretCrossbowAdvanced(RenderManager rm) {
      super(rm, null, 0.5F);
      this.model = AdvancedModelLoader.loadModel(TURMODEL);
   }

   public void renderTurret(EntityTurretCrossbow turret, double x, double y, double z, float par8, float pTicks) {
      this.bindEntityTexture(turret);
      GL11.glPushMatrix();
      GL11.glEnable(32826);
      GL11.glEnable(3042);
      GL11.glBlendFunc(770, 771);
      GL11.glTranslated(x, y + 0.75, z);
      GL11.glPushMatrix();
      if (turret.isRiding() && turret.getRidingEntity() != null && turret.getRidingEntity() instanceof EntityMinecart) {
         GL11.glScaled(0.66, 0.75, 0.66);
      }

      this.model.renderPart("legs");
      GL11.glPopMatrix();
      GL11.glPushMatrix();
      if (turret.hurtTime > 0) {
         GlStateManager.color(1.0F, 0.5F, 0.5F, 1.0F);
         float jiggle = turret.hurtTime / 500.0F;
         GL11.glTranslated(
            turret.getRNG().nextGaussian() * jiggle, turret.getRNG().nextGaussian() * jiggle, turret.getRNG().nextGaussian() * jiggle
         );
      }

      GL11.glRotatef(turret.prevRotationYawHead + (turret.rotationYawHead - turret.prevRotationYawHead) * pTicks, 0.0F, -1.0F, 0.0F);
      GL11.glRotatef(turret.prevRotationPitch + (turret.rotationPitch - turret.prevRotationPitch) * pTicks, 1.0F, 0.0F, 0.0F);
      this.model.renderPart("mech");
      this.model.renderPart("box");
      this.model.renderPart("shield");
      this.model.renderPart("brain");
      GL11.glPushMatrix();
      GL11.glTranslated(0.0, 0.0, MathHelper.sin(MathHelper.sqrt(turret.loadProgressForRender) * (float) Math.PI * 2.0F) / 12.0F);
      this.model.renderPart("loader");
      GL11.glPopMatrix();
      float sp = 0.0F;
      if (this.getSwingProgress(turret, pTicks) > -9990.0F) {
         sp = MathHelper.sin(MathHelper.sqrt(this.getSwingProgress(turret, pTicks)) * (float) Math.PI * 2.0F) * 20.0F;
      }

      GL11.glTranslated(0.0, 0.0, 0.375);
      GL11.glPushMatrix();
      GL11.glRotatef(sp, 0.0F, 1.0F, 0.0F);
      this.model.renderPart("bow1");
      GL11.glPopMatrix();
      GL11.glPushMatrix();
      GL11.glRotatef(sp, 0.0F, -1.0F, 0.0F);
      this.model.renderPart("bow2");
      GL11.glPopMatrix();
      GL11.glPopMatrix();
      GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
      GL11.glDisable(3042);
      GL11.glDisable(32826);
      GL11.glPopMatrix();
   }

   protected float getSwingProgress(EntityLivingBase e, float partialTickTime) {
      ((EntityTurretCrossbow)e).loadProgressForRender = ((EntityTurretCrossbow)e).getLoadProgress(partialTickTime);
      return super.getSwingProgress(e, partialTickTime);
   }

   private void translateFromOrientation(int orientation) {
      GL11.glTranslated(0.0, 0.5, 0.0);
      if (orientation == 0) {
         GL11.glRotatef(180.0F, 1.0F, 0.0F, 0.0F);
      } else if (orientation != 1) {
         if (orientation == 2) {
            GL11.glRotatef(-90.0F, 1.0F, 0.0F, 0.0F);
         } else if (orientation == 3) {
            GL11.glRotatef(90.0F, 1.0F, 0.0F, 0.0F);
         } else if (orientation == 4) {
            GL11.glRotatef(90.0F, 0.0F, 0.0F, 1.0F);
         } else if (orientation == 5) {
            GL11.glRotatef(-90.0F, 0.0F, 0.0F, 1.0F);
         }
      }

      GL11.glTranslated(0.0, -0.5, 0.0);
   }

   public void doRender(EntityLiving par1Entity, double par2, double par4, double par6, float par8, float par9) {
      this.renderTurret((EntityTurretCrossbow)par1Entity, par2, par4, par6, par8, par9);
   }

   protected ResourceLocation getEntityTexture(Entity entity) {
      return rl;
   }
}
