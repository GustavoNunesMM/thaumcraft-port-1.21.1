package thaumcraft.client.renderers.entity.mob;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.entity.RenderLiving;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.RenderLivingEvent.Post;
import net.minecraftforge.client.event.RenderLivingEvent.Pre;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import thaumcraft.client.renderers.models.entity.ModelTaintSeed;
import thaumcraft.common.entities.monster.tainted.EntityTaintSeed;

@SideOnly(Side.CLIENT)
public class RenderTaintSeed extends RenderLiving<EntityTaintSeed> {
   private static final ResourceLocation rl = new ResourceLocation("thaumcraft", "textures/entity/taintseed.png");

   public RenderTaintSeed(RenderManager rm) {
      super(rm, new ModelTaintSeed(), 0.4F);
   }

   public RenderTaintSeed(RenderManager rm, ModelBase modelbase, float sz) {
      super(rm, modelbase, sz);
   }

   protected ResourceLocation getEntityTexture(EntityTaintSeed entity) {
      return rl;
   }

   public void doRender(EntityTaintSeed entity, double x, double y, double z, float entityYaw, float partialTicks) {
      if (!MinecraftForge.EVENT_BUS.post(new Pre(entity, this, x, y, z))) {
         GlStateManager.pushMatrix();
         GlStateManager.disableCull();
         this.mainModel.swingProgress = this.getSwingProgress(entity, partialTicks);
         boolean shouldSit = entity.isRiding() && entity.getRidingEntity() != null && entity.getRidingEntity().shouldRiderSit();
         this.mainModel.isRiding = shouldSit;
         this.mainModel.isChild = entity.isChild();

         try {
            GlStateManager.pushMatrix();
            float f = 0.0F;
            float f7 = entity.prevRotationPitch + (entity.rotationPitch - entity.prevRotationPitch) * partialTicks;
            this.renderLivingAt(entity, x, y, z);
            float f8 = this.handleRotationFloat(entity, partialTicks);
            this.applyRotations(entity, f8, f, partialTicks);
            float f4 = this.prepareScale(entity, partialTicks);
            float f5 = 0.0F;
            float f6 = 0.0F;
            f5 = entity.prevLimbSwingAmount + (entity.limbSwingAmount - entity.prevLimbSwingAmount) * partialTicks;
            f6 = entity.limbSwing - entity.limbSwingAmount * (1.0F - partialTicks);
            if (f5 > 1.0F) {
               f5 = 1.0F;
            }

            GlStateManager.enableAlpha();
            this.mainModel.setLivingAnimations(entity, f6, f5, partialTicks);
            this.mainModel.setRotationAngles(f6, f5, f8, f, f7, f4, entity);
            if (this.renderOutlines) {
               boolean flag1 = this.setScoreTeamColor(entity);
               GlStateManager.enableColorMaterial();
               GlStateManager.enableOutlineMode(this.getTeamColor(entity));
               if (!this.renderMarker) {
                  this.renderModel(entity, f6, f5, f8, f, f7, f4);
               }

               this.renderLayers(entity, f6, f5, partialTicks, f8, f, f7, f4);
               GlStateManager.disableOutlineMode();
               GlStateManager.disableColorMaterial();
               if (flag1) {
                  this.unsetScoreTeamColor();
               }
            } else {
               boolean flag = this.setDoRenderBrightness(entity, partialTicks);
               this.renderModel(entity, f6, f5, f8, f, f7, f4);
               if (flag) {
                  this.unsetBrightness();
               }

               GlStateManager.depthMask(true);
               this.renderLayers(entity, f6, f5, partialTicks, f8, f, f7, f4);
            }

            GlStateManager.popMatrix();
            GlStateManager.disableRescaleNormal();
         } catch (Exception var18) {
         }

         GlStateManager.setActiveTexture(OpenGlHelper.lightmapTexUnit);
         GlStateManager.enableTexture2D();
         GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
         GlStateManager.enableCull();
         GlStateManager.popMatrix();
         if (!this.renderOutlines) {
            this.renderName(entity, x, y, z);
         }

         MinecraftForge.EVENT_BUS.post(new Post(entity, this, x, y, z));
      }
   }
}
