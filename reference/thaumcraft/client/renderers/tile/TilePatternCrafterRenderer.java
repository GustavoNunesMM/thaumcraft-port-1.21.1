package thaumcraft.client.renderers.tile;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.client.FMLClientHandler;
import org.lwjgl.opengl.GL11;
import thaumcraft.client.lib.UtilsFX;
import thaumcraft.client.renderers.models.block.ModelBoreBase;
import thaumcraft.common.lib.utils.BlockStateUtils;
import thaumcraft.common.tiles.crafting.TilePatternCrafter;

public class TilePatternCrafterRenderer extends TileEntitySpecialRenderer {
   private ModelBoreBase model = new ModelBoreBase();
   private static final ResourceLocation TEX = new ResourceLocation("thaumcraft", "textures/blocks/pattern_crafter_modes.png");
   private static final ResourceLocation ICON = new ResourceLocation("thaumcraft", "textures/misc/gear_brass.png");

   public void renderTileEntityAt(TilePatternCrafter pc, double x, double y, double z, float fq) {
      Minecraft mc = FMLClientHandler.instance().getClient();
      int f = 3;
      if (pc.getWorld() != null) {
         f = BlockStateUtils.getFacing(pc.getBlockMetadata()).ordinal();
      }

      GL11.glPushMatrix();
      GL11.glTranslatef((float)x + 0.5F, (float)y + 0.75F, (float)z + 0.5F);
      switch (f) {
         case 2:
            GL11.glRotatef(180.0F, 0.0F, 1.0F, 0.0F);
         case 3:
         default:
            break;
         case 4:
            GL11.glRotatef(270.0F, 0.0F, 1.0F, 0.0F);
            break;
         case 5:
            GL11.glRotatef(90.0F, 0.0F, 1.0F, 0.0F);
      }

      GL11.glPushMatrix();
      GL11.glRotatef(90.0F, 0.0F, 0.0F, 1.0F);
      GL11.glTranslatef(0.0F, 0.0F, -0.5F);
      UtilsFX.renderQuadCentered(
         TEX,
         10,
         1,
         pc.type,
         0.5F,
         1.0F,
         1.0F,
         1.0F,
         pc.getBlockType().getPackedLightmapCoords(pc.getWorld().getBlockState(pc.getPos()), pc.getWorld(), pc.getPos()),
         771,
         1.0F
      );
      GL11.glPopMatrix();
      mc.renderEngine.bindTexture(ICON);
      GL11.glPushMatrix();
      GL11.glTranslatef(-0.2F, -0.40625F, 0.05F);
      GL11.glRotatef(-pc.rot % 360.0F, 0.0F, 0.0F, 1.0F);
      GL11.glScaled(0.5, 0.5, 1.0);
      GL11.glTranslatef(-0.5F, -0.5F, 0.0F);
      UtilsFX.renderTextureIn3D(1.0F, 0.0F, 0.0F, 1.0F, 16, 16, 0.1F);
      GL11.glPopMatrix();
      GL11.glPushMatrix();
      GL11.glTranslatef(0.2F, -0.40625F, 0.05F);
      GL11.glRotatef(pc.rot % 360.0F, 0.0F, 0.0F, 1.0F);
      GL11.glScaled(0.5, 0.5, 1.0);
      GL11.glTranslatef(-0.5F, -0.5F, 0.0F);
      UtilsFX.renderTextureIn3D(1.0F, 0.0F, 0.0F, 1.0F, 16, 16, 0.1F);
      GL11.glPopMatrix();
      GL11.glPopMatrix();
   }

   public void render(TileEntity te, double x, double y, double z, float partialTicks, int destroyStage, float alpha) {
      super.render(te, x, y, z, partialTicks, destroyStage, alpha);
      this.renderTileEntityAt((TilePatternCrafter)te, x, y, z, partialTicks);
   }
}
