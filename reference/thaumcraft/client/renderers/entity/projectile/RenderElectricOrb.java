package thaumcraft.client.renderers.entity.projectile;

import java.util.Random;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.opengl.GL11;
import thaumcraft.client.fx.ParticleEngine;
import thaumcraft.client.lib.UtilsFX;
import thaumcraft.common.entities.projectile.EntityGolemOrb;

public class RenderElectricOrb extends Render {
   private Random random = new Random();

   public RenderElectricOrb(RenderManager rm) {
      super(rm);
      this.shadowSize = 0.0F;
   }

   public void renderEntityAt(Entity entity, double x, double y, double z, float fq, float pticks) {
      Tessellator tessellator = Tessellator.getInstance();
      GL11.glPushMatrix();
      GL11.glTranslated(x, y, z);
      GL11.glEnable(3042);
      GL11.glBlendFunc(770, 1);
      GL11.glDepthMask(false);
      this.bindTexture(ParticleEngine.particleTexture);
      float f2 = (1 + entity.ticksExisted % 6) / 32.0F;
      float f3 = f2 + 0.03125F;
      float f4 = 0.21875F;
      if (entity instanceof EntityGolemOrb && ((EntityGolemOrb)entity).red) {
         f4 = 0.1875F;
      }

      float f5 = f4 + 0.03125F;
      float f6 = 1.0F;
      float f7 = 0.5F;
      float f8 = 0.5F;
      GL11.glColor4f(1.0F, 1.0F, 1.0F, 0.8F);
      GL11.glRotatef(180.0F - this.renderManager.playerViewY, 0.0F, 1.0F, 0.0F);
      GL11.glRotatef(-this.renderManager.playerViewX, 1.0F, 0.0F, 0.0F);
      float bob = MathHelper.sin(entity.ticksExisted / 5.0F) * 0.2F + 0.2F;
      GL11.glScalef(1.0F + bob, 1.0F + bob, 1.0F + bob);
      tessellator.getBuffer().begin(7, UtilsFX.VERTEXFORMAT_POS_TEX_CO_LM_NO);
      int i = 220;
      int j = i >> 16 & 65535;
      int k = i & 65535;
      tessellator.getBuffer()
         .pos(-f7, -f8, 0.0)
         .tex(f2, f5)
         .color(1.0F, 1.0F, 1.0F, 1.0F)
         .lightmap(j, k)
         .normal(0.0F, 1.0F, 0.0F)
         .endVertex();
      tessellator.getBuffer()
         .pos(f6 - f7, -f8, 0.0)
         .tex(f3, f5)
         .color(1.0F, 1.0F, 1.0F, 1.0F)
         .lightmap(j, k)
         .normal(0.0F, 1.0F, 0.0F)
         .endVertex();
      tessellator.getBuffer()
         .pos(f6 - f7, 1.0F - f8, 0.0)
         .tex(f3, f4)
         .color(1.0F, 1.0F, 1.0F, 1.0F)
         .lightmap(j, k)
         .normal(0.0F, 1.0F, 0.0F)
         .endVertex();
      tessellator.getBuffer()
         .pos(-f7, 1.0F - f8, 0.0)
         .tex(f2, f4)
         .color(1.0F, 1.0F, 1.0F, 1.0F)
         .lightmap(j, k)
         .normal(0.0F, 1.0F, 0.0F)
         .endVertex();
      tessellator.draw();
      GL11.glDepthMask(true);
      GL11.glBlendFunc(770, 771);
      GL11.glDisable(3042);
      GL11.glDisable(32826);
      GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
      GL11.glPopMatrix();
   }

   public void doRender(Entity entity, double d, double d1, double d2, float f, float f1) {
      this.renderEntityAt(entity, d, d1, d2, f, f1);
   }

   protected ResourceLocation getEntityTexture(Entity entity) {
      return TextureMap.LOCATION_BLOCKS_TEXTURE;
   }
}
