package thaumcraft.client.renderers.entity.projectile;

import java.util.Random;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;
import thaumcraft.client.fx.ParticleEngine;

public class RenderEldritchOrb extends Render {
   private Random random = new Random();

   public RenderEldritchOrb(RenderManager renderManager) {
      super(renderManager);
      this.shadowSize = 0.0F;
   }

   public void renderEntityAt(Entity entity, double x, double y, double z, float fq, float pticks) {
      Tessellator tessellator = Tessellator.getInstance();
      this.random.setSeed(187L);
      GL11.glPushMatrix();
      RenderHelper.disableStandardItemLighting();
      float f1 = entity.ticksExisted / 80.0F;
      float f3 = 0.9F;
      float f2 = 0.0F;
      GL11.glTranslatef((float)x, (float)y, (float)z);
      GL11.glDisable(3553);
      GL11.glShadeModel(7425);
      GL11.glEnable(3042);
      GL11.glBlendFunc(770, 1);
      GL11.glDisable(3008);
      GL11.glEnable(2884);
      GL11.glDepthMask(false);
      GL11.glPushMatrix();

      for (int i = 0; i < 12; i++) {
         GL11.glRotatef(this.random.nextFloat() * 360.0F, 1.0F, 0.0F, 0.0F);
         GL11.glRotatef(this.random.nextFloat() * 360.0F, 0.0F, 1.0F, 0.0F);
         GL11.glRotatef(this.random.nextFloat() * 360.0F, 0.0F, 0.0F, 1.0F);
         GL11.glRotatef(this.random.nextFloat() * 360.0F, 1.0F, 0.0F, 0.0F);
         GL11.glRotatef(this.random.nextFloat() * 360.0F, 0.0F, 1.0F, 0.0F);
         GL11.glRotatef(this.random.nextFloat() * 360.0F + f1 * 360.0F, 0.0F, 0.0F, 1.0F);
         tessellator.getBuffer().begin(6, DefaultVertexFormats.POSITION_COLOR);
         float fa = this.random.nextFloat() * 20.0F + 5.0F + f2 * 10.0F;
         float f4 = this.random.nextFloat() * 2.0F + 1.0F + f2 * 2.0F;
         fa /= 30.0F / (Math.min(entity.ticksExisted, 10) / 10.0F);
         f4 /= 30.0F / (Math.min(entity.ticksExisted, 10) / 10.0F);
         tessellator.getBuffer().pos(0.0, 0.0, 0.0).color(1.0F, 1.0F, 1.0F, 1.0F - f2).endVertex();
         tessellator.getBuffer().pos(-0.866 * f4, fa, -0.5F * f4).color(64.0F, 64.0F, 64.0F, 255.0F * (1.0F - f2)).endVertex();
         tessellator.getBuffer().pos(0.866 * f4, fa, -0.5F * f4).color(64.0F, 64.0F, 64.0F, 255.0F * (1.0F - f2)).endVertex();
         tessellator.getBuffer().pos(0.0, fa, 1.0F * f4).color(64.0F, 64.0F, 64.0F, 255.0F * (1.0F - f2)).endVertex();
         tessellator.getBuffer().pos(-0.866 * f4, fa, -0.5F * f4).color(64.0F, 64.0F, 64.0F, 255.0F * (1.0F - f2)).endVertex();
         tessellator.draw();
      }

      GL11.glPopMatrix();
      GL11.glDepthMask(true);
      GL11.glDisable(2884);
      GL11.glDisable(3042);
      GL11.glShadeModel(7424);
      GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
      GL11.glEnable(3553);
      GL11.glEnable(3008);
      RenderHelper.enableStandardItemLighting();
      GL11.glPopMatrix();
      GL11.glPushMatrix();
      GL11.glTranslated(x, y, z);
      GL11.glEnable(3042);
      GL11.glBlendFunc(770, 771);
      GL11.glDepthMask(false);
      this.bindTexture(ParticleEngine.particleTexture);
      f2 = entity.ticksExisted % 13 / 64.0F;
      f3 = f2 + 0.015625F;
      float f4 = 0.046875F;
      float f5 = f4 + 0.015625F;
      float f6 = 1.0F;
      float f7 = 0.5F;
      float f8 = 0.5F;
      GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
      GL11.glRotatef(180.0F - this.renderManager.playerViewY, 0.0F, 1.0F, 0.0F);
      GL11.glRotatef(-this.renderManager.playerViewX, 1.0F, 0.0F, 0.0F);
      GL11.glScaled(0.75, 0.75, 0.75);
      tessellator.getBuffer().begin(7, DefaultVertexFormats.POSITION_TEX_NORMAL);
      tessellator.getBuffer();
      tessellator.getBuffer().pos(0.0F - f7, 0.0F - f8, 0.0).tex(f2, f5).normal(0.0F, 1.0F, 0.0F).endVertex();
      tessellator.getBuffer().pos(f6 - f7, 0.0F - f8, 0.0).tex(f3, f5).normal(0.0F, 1.0F, 0.0F).endVertex();
      tessellator.getBuffer().pos(f6 - f7, 1.0F - f8, 0.0).tex(f3, f4).normal(0.0F, 1.0F, 0.0F).endVertex();
      tessellator.getBuffer().pos(0.0F - f7, 1.0F - f8, 0.0).tex(f2, f4).normal(0.0F, 1.0F, 0.0F).endVertex();
      tessellator.draw();
      GL11.glDepthMask(true);
      GL11.glDisable(3042);
      GL11.glDisable(32826);
      GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
      GL11.glPopMatrix();
   }

   public void doRender(Entity entity, double d, double d1, double d2, float f, float f1) {
      this.renderEntityAt(entity, d, d1, d2, f, f1);
   }

   protected ResourceLocation getEntityTexture(Entity entity) {
      return ParticleEngine.particleTexture;
   }
}
