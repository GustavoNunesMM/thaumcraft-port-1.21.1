package thaumcraft.client.renderers.entity.mob;

import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.RenderSpider;
import net.minecraft.client.renderer.entity.layers.LayerSpiderEyes;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;
import thaumcraft.common.entities.monster.EntityMindSpider;

@SideOnly(Side.CLIENT)
public class RenderMindSpider extends RenderSpider {
   public RenderMindSpider(RenderManager rm) {
      super(rm);
      this.shadowSize = 0.5F;
      this.addLayer(new LayerSpiderEyes(this));
   }

   protected void preRenderCallback(EntityLivingBase par1EntityLiving, float par2) {
      GL11.glScalef(0.6F, 0.6F, 0.6F);
   }

   public void doRender(EntityLiving entity, double x, double y, double z, float entityYaw, float partialTicks) {
      if (((EntityMindSpider)entity).getViewer().length() == 0
         || ((EntityMindSpider)entity).getViewer().equals(this.renderManager.renderViewEntity.getName())) {
         super.doRender(entity, x, y, z, entityYaw, partialTicks);
      }
   }

   protected void renderModel(
      EntityLivingBase entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scaleFactor
   ) {
      this.bindEntityTexture(entity);
      GL11.glPushMatrix();
      GL11.glColor4f(1.0F, 1.0F, 1.0F, Math.min(0.1F, entity.ticksExisted / 100.0F));
      GL11.glDepthMask(false);
      GL11.glEnable(3042);
      GL11.glBlendFunc(770, 771);
      GL11.glAlphaFunc(516, 0.003921569F);
      this.mainModel.render(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scaleFactor);
      GL11.glBlendFunc(770, 771);
      GL11.glDisable(3042);
      GL11.glAlphaFunc(516, 0.1F);
      GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
      GL11.glPopMatrix();
      GL11.glDepthMask(true);
   }
}
