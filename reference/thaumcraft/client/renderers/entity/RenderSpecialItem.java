package thaumcraft.client.renderers.entity;

import java.util.Random;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.RenderItem;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderEntityItem;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.client.FMLClientHandler;
import org.lwjgl.opengl.GL11;

public class RenderSpecialItem extends RenderEntityItem {
   public RenderSpecialItem(RenderManager p_i46167_1_, RenderItem p_i46167_2_) {
      super(p_i46167_1_, p_i46167_2_);
   }

   public void doRender(EntityItem e, double x, double y, double z, float p_177075_8_, float pt) {
      Random random = new Random(187L);
      float var11 = MathHelper.sin((e.getAge() + pt) / 10.0F + e.hoverStart) * 0.1F + 0.1F;
      GL11.glPushMatrix();
      GL11.glTranslatef((float)x, (float)y + var11 + 0.25F, (float)z);
      int q = !FMLClientHandler.instance().getClient().gameSettings.fancyGraphics ? 5 : 10;
      Tessellator tessellator = Tessellator.getInstance();
      BufferBuilder wr = tessellator.getBuffer();
      RenderHelper.disableStandardItemLighting();
      float f1 = e.getAge() / 500.0F;
      float f3 = 0.9F;
      float f2 = 0.0F;
      GL11.glDisable(3553);
      GL11.glShadeModel(7425);
      GL11.glEnable(3042);
      GL11.glBlendFunc(770, 1);
      GL11.glDisable(3008);
      GL11.glEnable(2884);
      GL11.glDepthMask(false);
      GL11.glPushMatrix();

      for (int i = 0; i < q; i++) {
         GL11.glRotatef(random.nextFloat() * 360.0F, 1.0F, 0.0F, 0.0F);
         GL11.glRotatef(random.nextFloat() * 360.0F, 0.0F, 1.0F, 0.0F);
         GL11.glRotatef(random.nextFloat() * 360.0F, 0.0F, 0.0F, 1.0F);
         GL11.glRotatef(random.nextFloat() * 360.0F, 1.0F, 0.0F, 0.0F);
         GL11.glRotatef(random.nextFloat() * 360.0F, 0.0F, 1.0F, 0.0F);
         GL11.glRotatef(random.nextFloat() * 360.0F + f1 * 360.0F, 0.0F, 0.0F, 1.0F);
         wr.begin(6, DefaultVertexFormats.POSITION_COLOR);
         float fa = random.nextFloat() * 20.0F + 5.0F + f2 * 10.0F;
         float f4 = random.nextFloat() * 2.0F + 1.0F + f2 * 2.0F;
         fa /= 30.0F / (Math.min(e.getAge(), 10) / 10.0F);
         f4 /= 30.0F / (Math.min(e.getAge(), 10) / 10.0F);
         wr.pos(0.0, 0.0, 0.0).color(1.0F, 1.0F, 1.0F, 1.0F - f2).endVertex();
         wr.pos(-0.866 * f4, fa, -0.5F * f4).color(1.0F, 0.0F, 1.0F, 0.0F).endVertex();
         wr.pos(0.866 * f4, fa, -0.5F * f4).color(1.0F, 0.0F, 1.0F, 0.0F).endVertex();
         wr.pos(0.0, fa, 1.0F * f4).color(1.0F, 0.0F, 1.0F, 0.0F).endVertex();
         wr.pos(-0.866 * f4, fa, -0.5F * f4).color(1.0F, 0.0F, 1.0F, 0.0F).endVertex();
         tessellator.draw();
      }

      GL11.glPopMatrix();
      GL11.glDepthMask(true);
      GL11.glDisable(2884);
      GL11.glBlendFunc(770, 771);
      GL11.glDisable(3042);
      GL11.glShadeModel(7424);
      GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
      GL11.glEnable(3553);
      GL11.glEnable(3008);
      RenderHelper.enableStandardItemLighting();
      GL11.glPopMatrix();
      super.doRender(e, x, y, z, p_177075_8_, pt);
   }
}
