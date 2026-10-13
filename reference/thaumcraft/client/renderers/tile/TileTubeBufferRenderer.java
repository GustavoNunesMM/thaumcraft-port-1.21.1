package thaumcraft.client.renderers.tile;

import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;
import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.client.renderers.models.block.ModelTubeValve;
import thaumcraft.common.tiles.essentia.TileTubeBuffer;

public class TileTubeBufferRenderer extends TileEntitySpecialRenderer {
   private ModelTubeValve model = new ModelTubeValve();
   private static final ResourceLocation TEX_VALVE = new ResourceLocation("thaumcraft", "textures/models/valve.png");

   public void renderEntityAt(TileTubeBuffer buffer, double x, double y, double z, float fq) {
      this.bindTexture(TEX_VALVE);
      if (buffer.getWorld() != null) {
         for (EnumFacing dir : EnumFacing.VALUES) {
            if (buffer.chokedSides[dir.ordinal()] != 0
               && buffer.openSides[dir.ordinal()]
               && ThaumcraftApiHelper.getConnectableTile(buffer.getWorld(), buffer.getPos(), dir) != null) {
               GL11.glPushMatrix();
               GL11.glTranslated(x + 0.5, y + 0.5, z + 0.5);
               if (dir.getOpposite().getYOffset() == 0) {
                  GL11.glRotatef(90.0F, 0.0F, 1.0F, 0.0F);
               } else {
                  GL11.glRotatef(90.0F, -1.0F, 0.0F, 0.0F);
                  GL11.glRotatef(90.0F, dir.getOpposite().getYOffset(), 0.0F, 0.0F);
               }

               GL11.glRotatef(90.0F, dir.getOpposite().getXOffset(), dir.getOpposite().getYOffset(), dir.getOpposite().getZOffset());
               GL11.glPushMatrix();
               if (buffer.chokedSides[dir.ordinal()] == 2) {
                  GL11.glColor3f(1.0F, 0.3F, 0.3F);
               } else {
                  GL11.glColor3f(0.3F, 0.3F, 1.0F);
               }

               GL11.glScaled(2.0, 1.0, 2.0);
               GL11.glTranslated(0.0, -0.5, 0.0);
               this.model.renderRod();
               GL11.glPopMatrix();
               GL11.glColor3f(1.0F, 1.0F, 1.0F);
               GL11.glPopMatrix();
            }
         }
      }
   }

   public void render(TileEntity te, double x, double y, double z, float partialTicks, int destroyStage, float alpha) {
      super.render(te, x, y, z, partialTicks, destroyStage, alpha);
      this.renderEntityAt((TileTubeBuffer)te, x, y, z, partialTicks);
   }
}
