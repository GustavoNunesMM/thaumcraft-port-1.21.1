package thaumcraft.client.renderers.tile;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.ARBShaderObjects;
import org.lwjgl.opengl.GL11;
import thaumcraft.api.blocks.BlocksTC;
import thaumcraft.client.lib.UtilsFX;
import thaumcraft.client.lib.ender.ShaderCallback;
import thaumcraft.client.lib.ender.ShaderHelper;

public class TileHoleRenderer extends TileEntitySpecialRenderer {
   private final ShaderCallback shaderCallback = new ShaderCallback() {
      @Override
      public void call(int shader) {
         Minecraft mc = Minecraft.getMinecraft();
         int x = ARBShaderObjects.glGetUniformLocationARB(shader, "yaw");
         ARBShaderObjects.glUniform1fARB(x, (float)(mc.player.rotationYaw * 2.0F * Math.PI / 360.0));
         int z = ARBShaderObjects.glGetUniformLocationARB(shader, "pitch");
         ARBShaderObjects.glUniform1fARB(z, -((float)(mc.player.rotationPitch * 2.0F * Math.PI / 360.0)));
      }
   };
   private static final ResourceLocation starsTexture = new ResourceLocation("textures/entity/end_portal.png");

   public void render(TileEntity te, double x, double y, double z, float partialTicks, int destroyStage, float alpha) {
      super.render(te, x, y, z, partialTicks, destroyStage, alpha);
      GL11.glPushMatrix();
      this.bindTexture(starsTexture);
      ShaderHelper.useShader(ShaderHelper.endShader, this.shaderCallback);
      GL11.glTranslated(x + 0.5, y + 0.5, z + 0.5);

      for (EnumFacing face : EnumFacing.values()) {
         IBlockState bs = te.getWorld().getBlockState(te.getPos().offset(face));
         if (bs.isOpaqueCube() && bs.getBlock() != BlocksTC.hole) {
            GL11.glPushMatrix();
            GL11.glRotatef(90.0F, -face.getYOffset(), face.getXOffset(), -face.getZOffset());
            if (face.getZOffset() < 0) {
               GL11.glTranslated(0.0, 0.0, -0.499F);
               GL11.glRotatef(180.0F, 0.0F, 1.0F, 0.0F);
            } else {
               GL11.glTranslated(0.0, 0.0, 0.499F);
            }

            GL11.glRotatef(90.0F, 0.0F, 0.0F, -1.0F);
            UtilsFX.renderQuadCentered();
            GL11.glPopMatrix();
         }
      }

      ShaderHelper.releaseShader();
      GL11.glPopMatrix();
   }
}
