package thaumcraft.client.renderers.tile;

import java.awt.Color;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;
import thaumcraft.client.lib.UtilsFX;
import thaumcraft.client.renderers.models.block.ModelBanner;
import thaumcraft.common.tiles.misc.TileBanner;

@SideOnly(Side.CLIENT)
public class TileBannerRenderer extends TileEntitySpecialRenderer {
   private ModelBanner model = new ModelBanner();
   private static final ResourceLocation TEX_CULT = new ResourceLocation("thaumcraft", "textures/models/banner_cultist.png");
   private static final ResourceLocation TEX_BLANK = new ResourceLocation("thaumcraft", "textures/models/banner_blank.png");

   public void renderTileEntityAt(TileBanner banner, double par2, double par4, double par6, float par8) {
      GL11.glPushMatrix();
      if (banner.getAspect() == null && banner.getColor() == -1) {
         this.bindTexture(TEX_CULT);
      } else {
         this.bindTexture(TEX_BLANK);
      }

      GL11.glTranslatef((float)par2 + 0.5F, (float)par4 + 1.5F, (float)par6 + 0.5F);
      GL11.glRotatef(180.0F, 1.0F, 0.0F, 0.0F);
      GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
      if (banner.getWorld() != null) {
         GL11.glRotatef(180.0F, 0.0F, 1.0F, 0.0F);
         float f2 = banner.getBannerFacing() * 360 / 16.0F;
         GL11.glRotatef(f2, 0.0F, 1.0F, 0.0F);
      }

      if (!banner.getWall()) {
         this.model.renderPole();
      } else {
         GL11.glTranslated(0.0, 1.0, -0.4125);
      }

      this.model.renderBeam();
      if (banner.getColor() != -1) {
         Color c = new Color(banner.getColor());
         GL11.glColor4f(c.getRed() / 255.0F, c.getGreen() / 255.0F, c.getBlue() / 255.0F, 1.0F);
      }

      this.model.renderTabs();
      EntityPlayer p = Minecraft.getMinecraft().player;
      float f3 = (float)(banner.getPos().getX() * 7 + banner.getPos().getY() * 9 + banner.getPos().getZ() * 13)
         + p.ticksExisted
         + par8;
      float rx = 0.02F - MathHelper.sin(f3 / 11.0F) * 0.02F;
      this.model.Banner.rotateAngleX = rx;
      this.model.renderBanner();
      GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
      if (banner.getAspect() != null) {
         GL11.glPushMatrix();
         GL11.glTranslatef(0.0F, 0.0F, 0.05001F);
         GL11.glScaled(0.0375, 0.0375, 0.0375);
         GL11.glRotatef(180.0F, 0.0F, 1.0F, 0.0F);
         GL11.glRotatef(-rx * (180.0F / (float)Math.PI) * 2.0F, 1.0F, 0.0F, 0.0F);
         UtilsFX.drawTag(-8, 0, banner.getAspect(), 0.0F, 0, 0.0, 771, 0.75F, false);
         GL11.glPopMatrix();
      }

      GL11.glPopMatrix();
   }

   public void render(TileEntity te, double x, double y, double z, float partialTicks, int destroyStage, float alpha) {
      super.render(te, x, y, z, partialTicks, destroyStage, alpha);
      this.renderTileEntityAt((TileBanner)te, x, y, z, partialTicks);
   }
}
