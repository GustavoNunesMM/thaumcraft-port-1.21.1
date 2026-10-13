package thaumcraft.client.renderers.entity.mob;

import java.util.ArrayList;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderLivingBase;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.entity.EntityLiving;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.opengl.GL11;

public class LayerTainted implements LayerRenderer<EntityLiving> {
   private static final ResourceLocation TAINT_TEXTURE = new ResourceLocation("thaumcraft:textures/models/taint_fibres.png");
   private final RenderLivingBase renderer;
   private final ModelBase model;
   public static ArrayList<Integer> taintLayers = new ArrayList<>();

   public LayerTainted(int i, RenderLivingBase witherRendererIn, ModelBase model) {
      this.renderer = witherRendererIn;
      this.model = model;
      taintLayers.add(i);
   }

   public void doRenderLayer(
      EntityLiving entitylivingbaseIn,
      float limbSwing,
      float limbSwingAmount,
      float partialTicks,
      float ageInTicks,
      float netHeadYaw,
      float headPitch,
      float scale
   ) {
      if (taintLayers.contains(entitylivingbaseIn.getEntityId())) {
         boolean flag = entitylivingbaseIn.isInvisible();
         GlStateManager.depthMask(!flag);
         this.renderer.bindTexture(TAINT_TEXTURE);
         GlStateManager.matrixMode(5890);
         GlStateManager.loadIdentity();
         float f = entitylivingbaseIn.getEntityId();
         float f1 = MathHelper.cos(f * 2.5E-4F);
         float f2 = f * 0.001F;
         GlStateManager.scale(8.0F, 4.0F, 4.0F);
         GlStateManager.translate(f1, f2, 0.0F);
         GlStateManager.matrixMode(5888);
         GlStateManager.enableBlend();
         GL11.glColor4f(1.0F, 1.0F, 1.0F, 0.66F);
         GL11.glBlendFunc(770, 771);
         this.model.setLivingAnimations(entitylivingbaseIn, limbSwing, limbSwingAmount, partialTicks);
         this.model.setModelAttributes(this.renderer.getMainModel());
         this.model.render(entitylivingbaseIn, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale);
         GlStateManager.matrixMode(5890);
         GlStateManager.loadIdentity();
         GlStateManager.matrixMode(5888);
         GlStateManager.disableBlend();
         GlStateManager.depthMask(flag);
      }
   }

   public boolean shouldCombineTextures() {
      return false;
   }
}
