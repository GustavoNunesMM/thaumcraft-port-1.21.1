package thaumcraft.client.renderers.entity.construct;

import net.minecraft.client.renderer.entity.RenderLiving;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.ResourceLocation;
import thaumcraft.client.renderers.models.entity.ModelCrossbow;
import thaumcraft.common.entities.construct.EntityTurretCrossbow;

public class RenderTurretCrossbow extends RenderLiving {
   private static final ResourceLocation rl = new ResourceLocation("thaumcraft", "textures/entity/crossbow.png");

   public RenderTurretCrossbow(RenderManager rm) {
      super(rm, new ModelCrossbow(), 0.5F);
   }

   protected float getSwingProgress(EntityLivingBase e, float partialTickTime) {
      ((EntityTurretCrossbow)e).loadProgressForRender = ((EntityTurretCrossbow)e).getLoadProgress(partialTickTime);
      e.renderYawOffset = 0.0F;
      e.prevRenderYawOffset = 0.0F;
      return super.getSwingProgress(e, partialTickTime);
   }

   protected void preRenderCallback(EntityLivingBase entitylivingbaseIn, float partialTickTime) {
      entitylivingbaseIn.renderYawOffset = 0.0F;
      entitylivingbaseIn.prevRenderYawOffset = 0.0F;
      super.preRenderCallback(entitylivingbaseIn, partialTickTime);
   }

   protected ResourceLocation getEntityTexture(Entity entity) {
      return rl;
   }
}
