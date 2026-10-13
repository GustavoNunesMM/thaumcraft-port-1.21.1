package thaumcraft.client.renderers.tile;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;
import thaumcraft.client.lib.UtilsFX;
import thaumcraft.client.lib.obj.AdvancedModelLoader;
import thaumcraft.client.lib.obj.IModelCustom;
import thaumcraft.common.lib.utils.BlockStateUtils;
import thaumcraft.common.tiles.crafting.TileGolemBuilder;

@SideOnly(Side.CLIENT)
public class TileGolemBuilderRenderer extends TileEntitySpecialRenderer {
   private IModelCustom model;
   private static final ResourceLocation TM = new ResourceLocation("thaumcraft", "models/block/golembuilder.obj");
   private static final ResourceLocation TEX = new ResourceLocation("thaumcraft", "textures/blocks/golembuilder.png");
   EntityItem entityitem = null;

   public TileGolemBuilderRenderer() {
      this.model = AdvancedModelLoader.loadModel(TM);
   }

   // $VF: Unable to simplify switch-on-enum, as the enum class was not able to be found.
   // Please report this to the Vineflower issue tracker, at https://github.com/Vineflower/vineflower/issues with a copy of the class file (if you have the rights to distribute it!)
   public void renderTileEntityAt(TileGolemBuilder tile, double par2, double par4, double par6, float pt, int destroyStage) {
      GL11.glPushMatrix();
      GL11.glTranslatef((float)par2 + 0.5F, (float)par4, (float)par6 + 0.5F);
      GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
      this.bindTexture(TEX);
      if (destroyStage >= 0) {
         this.bindTexture(DESTROY_STAGES[destroyStage]);
         GlStateManager.matrixMode(5890);
         GlStateManager.pushMatrix();
         GlStateManager.scale(5.0F, 5.0F, 2.0F);
         GlStateManager.translate(0.0625F, 0.0625F, 0.0625F);
         GlStateManager.matrixMode(5888);
      } else {
         GL11.glMatrixMode(5890);
         GL11.glLoadIdentity();
         GL11.glScalef(1.0F, -1.0F, 1.0F);
         GL11.glMatrixMode(5888);
      }

      EnumFacing facing = BlockStateUtils.getFacing(tile.getBlockMetadata());
      if (tile.getWorld() != null) {
         switch (facing.ordinal()) {
            case 3:
               GL11.glRotatef(180.0F, 0.0F, 1.0F, 0.0F);
               break;
            case 4:
               GL11.glRotatef(90.0F, 0.0F, 1.0F, 0.0F);
               break;
            case 5:
               GL11.glRotatef(270.0F, 0.0F, 1.0F, 0.0F);
         }
      }

      this.model.renderAllExcept("press");
      GL11.glPushMatrix();
      float h = tile.press;
      double s = Math.sin(Math.toRadians(h)) * 0.625;
      GL11.glTranslated(0.0, -s, 0.0);
      this.model.renderPart("press");
      GL11.glPopMatrix();
      if (destroyStage >= 0) {
         GlStateManager.matrixMode(5890);
         GlStateManager.popMatrix();
         GlStateManager.matrixMode(5888);
      } else {
         GL11.glMatrixMode(5890);
         GL11.glLoadIdentity();
         GL11.glScalef(1.0F, 1.0F, 1.0F);
         GL11.glMatrixMode(5888);
      }

      GL11.glTranslatef(-0.3125F, 0.625F, 1.3125F);
      GL11.glRotatef(90.0F, -1.0F, 0.0F, 0.0F);
      TextureAtlasSprite icon = Minecraft.getMinecraft().getBlockRendererDispatcher().getBlockModelShapes().getTexture(Blocks.LAVA.getDefaultState());
      UtilsFX.renderQuadFromIcon(icon, 0.625F, 1.0F, 1.0F, 1.0F, 200, 771, 1.0F);
      GL11.glPopMatrix();
   }

   public void render(TileEntity te, double x, double y, double z, float partialTicks, int destroyStage, float alpha) {
      super.render(te, x, y, z, partialTicks, destroyStage, alpha);
      this.renderTileEntityAt((TileGolemBuilder)te, x, y, z, partialTicks, destroyStage);
   }
}
