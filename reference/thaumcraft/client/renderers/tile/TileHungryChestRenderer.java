package thaumcraft.client.renderers.tile;

import net.minecraft.client.model.ModelChest;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;
import thaumcraft.common.tiles.devices.TileHungryChest;

@SideOnly(Side.CLIENT)
public class TileHungryChestRenderer extends TileEntitySpecialRenderer {
   private ModelChest chestModel = new ModelChest();
   private static final ResourceLocation textureNormal = new ResourceLocation("thaumcraft", "textures/models/chesthungry.png");

   public void renderTileEntityChestAt(TileHungryChest chest, double par2, double par4, double par6, float par8, int bp) {
      int var9x = 0;
      if (!chest.hasWorld()) {
         var9x = 0;
      } else {
         var9x = chest.getBlockMetadata();
      }

      ModelChest var14 = this.chestModel;
      if (bp >= 0) {
         this.bindTexture(DESTROY_STAGES[bp]);
         GlStateManager.matrixMode(5890);
         GlStateManager.pushMatrix();
         GlStateManager.scale(4.0F, 4.0F, 1.0F);
         GlStateManager.translate(0.0625F, 0.0625F, 0.0625F);
         GlStateManager.matrixMode(5888);
      } else {
         this.bindTexture(textureNormal);
      }

      GL11.glPushMatrix();
      GL11.glEnable(32826);
      if (bp < 0) {
         GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
      }

      GL11.glTranslatef((float)par2, (float)par4 + 1.0F, (float)par6 + 1.0F);
      GL11.glScalef(1.0F, -1.0F, -1.0F);
      GL11.glTranslatef(0.5F, 0.5F, 0.5F);
      short var11 = 0;
      if (var9x == 2) {
         var11 = 180;
      }

      if (var9x == 3) {
         var11 = 0;
      }

      if (var9x == 4) {
         var11 = 90;
      }

      if (var9x == 5) {
         var11 = -90;
      }

      GL11.glRotatef(var11, 0.0F, 1.0F, 0.0F);
      GL11.glTranslatef(-0.5F, -0.5F, -0.5F);
      float var12 = chest.prevLidAngle + (chest.lidAngle - chest.prevLidAngle) * par8;
      var12 = 1.0F - var12;
      var12 = 1.0F - var12 * var12 * var12;
      var14.chestLid.rotateAngleX = -(var12 * (float) Math.PI / 2.0F);
      var14.renderAll();
      GL11.glDisable(32826);
      GL11.glPopMatrix();
      GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
      if (bp >= 0) {
         GlStateManager.matrixMode(5890);
         GlStateManager.popMatrix();
         GlStateManager.matrixMode(5888);
      }
   }

   public void render(TileEntity te, double x, double y, double z, float partialTicks, int destroyStage, float alpha) {
      this.renderTileEntityChestAt((TileHungryChest)te, x, y, z, partialTicks, destroyStage);
   }
}
