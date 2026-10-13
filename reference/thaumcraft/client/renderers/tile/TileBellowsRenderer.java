package thaumcraft.client.renderers.tile;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.opengl.GL11;
import thaumcraft.client.renderers.models.block.ModelBellows;
import thaumcraft.client.renderers.models.block.ModelBoreBase;
import thaumcraft.common.lib.utils.BlockStateUtils;
import thaumcraft.common.tiles.devices.TileBellows;
import thaumcraft.common.tiles.essentia.TileTubeBuffer;

public class TileBellowsRenderer extends TileEntitySpecialRenderer<TileBellows> {
   private ModelBellows model = new ModelBellows();
   private ModelBoreBase model2 = new ModelBoreBase();
   private static final ResourceLocation TEX = new ResourceLocation("thaumcraft", "textures/blocks/bellows.png");
   private static final ResourceLocation TEX_BORE = new ResourceLocation("thaumcraft", "textures/models/bore.png");

   // $VF: Unable to simplify switch-on-enum, as the enum class was not able to be found.
   // Please report this to the Vineflower issue tracker, at https://github.com/Vineflower/vineflower/issues with a copy of the class file (if you have the rights to distribute it!)
   public void render(TileBellows bellows, double x, double y, double z, float partialTicks, int destroyStage, float alpha) {
      super.render(bellows, x, y, z, partialTicks, destroyStage, alpha);
      float scale = 0.0F;
      EnumFacing dir = EnumFacing.WEST;
      boolean extension = false;
      if (bellows == null) {
         EntityPlayer p = Minecraft.getMinecraft().player;
         scale = MathHelper.sin(p.ticksExisted / 8.0F) * 0.3F + 0.7F;
      } else {
         scale = bellows.inflation;
         dir = BlockStateUtils.getFacing(bellows.getBlockMetadata());
         TileEntity te = bellows.getWorld().getTileEntity(bellows.getPos().offset(BlockStateUtils.getFacing(bellows.getBlockMetadata())));
         if (te != null && te instanceof TileTubeBuffer) {
            extension = true;
         }
      }

      float tscale = 0.125F + scale * 0.875F;
      if (extension) {
         this.bindTexture(TEX_BORE);
         GL11.glPushMatrix();
         GL11.glTranslatef((float)x + 0.5F + dir.getXOffset(), (float)y + dir.getYOffset(), (float)z + 0.5F + dir.getZOffset());
         switch (dir.getOpposite().ordinal()) {
            case 0:
               GL11.glTranslatef(-0.5F, 0.5F, 0.0F);
               GL11.glRotatef(90.0F, 0.0F, 0.0F, -1.0F);
               break;
            case 1:
               GL11.glTranslatef(0.5F, 0.5F, 0.0F);
               GL11.glRotatef(90.0F, 0.0F, 0.0F, 1.0F);
               break;
            case 2:
               GL11.glRotatef(90.0F, 0.0F, 1.0F, 0.0F);
               break;
            case 3:
               GL11.glRotatef(270.0F, 0.0F, 1.0F, 0.0F);
               break;
            case 4:
               GL11.glRotatef(180.0F, 0.0F, 1.0F, 0.0F);
               break;
            case 5:
               GL11.glRotatef(0.0F, 0.0F, 1.0F, 0.0F);
         }

         this.model2.renderNozzle();
         GL11.glPopMatrix();
      }

      this.bindTexture(TEX);
      GL11.glPushMatrix();
      GL11.glEnable(2977);
      GL11.glEnable(32826);
      GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
      this.translateFromOrientation((float)x, (float)y, (float)z, dir.ordinal());
      if (destroyStage >= 0) {
         this.bindTexture(DESTROY_STAGES[destroyStage]);
         GlStateManager.matrixMode(5890);
         GlStateManager.pushMatrix();
         GlStateManager.scale(4.0F, 4.0F, 1.0F);
         GlStateManager.translate(0.0625F, 0.0625F, 0.0625F);
         GlStateManager.matrixMode(5888);
      }

      GL11.glTranslatef(0.0F, 1.0F, 0.0F);
      GL11.glPushMatrix();
      GL11.glScalef(0.5F, (scale + 0.1F) / 2.0F, 0.5F);
      this.model.Bag.setRotationPoint(0.0F, 0.5F, 0.0F);
      this.model.Bag.render(0.0625F);
      GL11.glScalef(1.0F, 1.0F, 1.0F);
      GL11.glPopMatrix();
      GL11.glTranslatef(0.0F, -1.0F, 0.0F);
      GL11.glPushMatrix();
      GL11.glTranslatef(0.0F, -tscale / 2.0F + 0.5F, 0.0F);
      this.model.TopPlank.render(0.0625F);
      GL11.glTranslatef(0.0F, tscale / 2.0F - 0.5F, 0.0F);
      GL11.glPopMatrix();
      GL11.glPushMatrix();
      GL11.glTranslatef(0.0F, tscale / 2.0F - 0.5F, 0.0F);
      this.model.BottomPlank.render(0.0625F);
      GL11.glTranslatef(0.0F, -tscale / 2.0F + 0.5F, 0.0F);
      GL11.glPopMatrix();
      this.model.render();
      GL11.glDisable(32826);
      GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
      GL11.glPopMatrix();
      if (destroyStage >= 0) {
         GlStateManager.matrixMode(5890);
         GlStateManager.popMatrix();
         GlStateManager.matrixMode(5888);
      }
   }

   private void translateFromOrientation(double x, double y, double z, int orientation) {
      GL11.glTranslatef((float)x + 0.5F, (float)y - 0.5F, (float)z + 0.5F);
      if (orientation == 0) {
         GL11.glTranslatef(0.0F, 1.0F, -1.0F);
         GL11.glRotatef(90.0F, 1.0F, 0.0F, 0.0F);
      } else if (orientation == 1) {
         GL11.glTranslatef(0.0F, 1.0F, 1.0F);
         GL11.glRotatef(270.0F, 1.0F, 0.0F, 0.0F);
      } else if (orientation == 2) {
         GL11.glRotatef(180.0F, 0.0F, 1.0F, 0.0F);
      } else if (orientation == 4) {
         GL11.glRotatef(270.0F, 0.0F, 1.0F, 0.0F);
      } else if (orientation == 5) {
         GL11.glRotatef(90.0F, 0.0F, 1.0F, 0.0F);
      }
   }
}
