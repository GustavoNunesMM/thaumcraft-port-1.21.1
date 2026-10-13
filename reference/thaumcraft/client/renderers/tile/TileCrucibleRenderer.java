package thaumcraft.client.renderers.tile;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import org.lwjgl.opengl.GL11;
import thaumcraft.api.blocks.BlocksTC;
import thaumcraft.client.lib.UtilsFX;
import thaumcraft.common.tiles.crafting.TileCrucible;

public class TileCrucibleRenderer extends TileEntitySpecialRenderer {
   public void renderEntityAt(TileCrucible cr, double x, double y, double z, float fq) {
      if (cr.tank.getFluidAmount() > 0) {
         this.renderFluid(cr, x, y, z);
      }
   }

   public void renderFluid(TileCrucible cr, double x, double y, double z) {
      GL11.glPushMatrix();
      GL11.glTranslated(x, y + cr.getFluidHeight(), z + 1.0);
      GL11.glRotatef(90.0F, -1.0F, 0.0F, 0.0F);
      if (cr.tank.getFluidAmount() > 0) {
         TextureAtlasSprite icon = Minecraft.getMinecraft().getBlockRendererDispatcher().getBlockModelShapes().getTexture(Blocks.WATER.getDefaultState());
         float recolor = cr.aspects.visSize() / 500.0F;
         if (recolor > 0.0F) {
            recolor = 0.5F + recolor / 2.0F;
         }

         if (recolor > 1.0F) {
            recolor = 1.0F;
         }

         UtilsFX.renderQuadFromIcon(
            icon,
            1.0F,
            1.0F - recolor / 3.0F,
            1.0F - recolor,
            1.0F - recolor / 2.0F,
            BlocksTC.crucible.getPackedLightmapCoords(cr.getWorld().getBlockState(cr.getPos()), cr.getWorld(), cr.getPos()),
            771,
            1.0F
         );
      }

      GL11.glPopMatrix();
   }

   public void render(TileEntity te, double x, double y, double z, float partialTicks, int destroyStage, float alpha) {
      super.render(te, x, y, z, partialTicks, destroyStage, alpha);
      this.renderEntityAt((TileCrucible)te, x, y, z, partialTicks);
   }
}
