package thaumcraft.client.renderers.tile;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;
import thaumcraft.client.renderers.models.block.ModelCentrifuge;
import thaumcraft.common.tiles.essentia.TileCentrifuge;

public class TileCentrifugeRenderer extends TileEntitySpecialRenderer {
   private ModelCentrifuge model = new ModelCentrifuge();
   private static final ResourceLocation TEX = new ResourceLocation("thaumcraft", "textures/models/centrifuge.png");

   public void renderEntityAt(TileCentrifuge cf, double x, double y, double z, float fq, int destroyStage) {
      this.bindTexture(TEX);
      GL11.glPushMatrix();
      if (destroyStage >= 0) {
         this.bindTexture(DESTROY_STAGES[destroyStage]);
         GlStateManager.matrixMode(5890);
         GlStateManager.pushMatrix();
         GlStateManager.scale(4.0F, 4.0F, 1.0F);
         GlStateManager.translate(0.0625F, 0.0625F, 0.0625F);
         GlStateManager.matrixMode(5888);
      }

      GL11.glTranslated(x + 0.5, y + 0.5, z + 0.5);
      this.model.renderBoxes();
      GL11.glRotated(cf.rotation, 0.0, 1.0, 0.0);
      this.model.renderSpinnyBit();
      if (destroyStage >= 0) {
         GlStateManager.matrixMode(5890);
         GlStateManager.popMatrix();
         GlStateManager.matrixMode(5888);
      }

      GL11.glPopMatrix();
   }

   public void render(TileEntity te, double x, double y, double z, float partialTicks, int destroyStage, float alpha) {
      super.render(te, x, y, z, partialTicks, destroyStage, alpha);
      this.renderEntityAt((TileCentrifuge)te, x, y, z, partialTicks, destroyStage);
   }
}
