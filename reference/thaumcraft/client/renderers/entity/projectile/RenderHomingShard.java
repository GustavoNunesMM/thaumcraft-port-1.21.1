package thaumcraft.client.renderers.entity.projectile;

import java.util.Random;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;
import thaumcraft.client.fx.ParticleEngine;
import thaumcraft.client.lib.UtilsFX;
import thaumcraft.common.entities.projectile.EntityHomingShard;

public class RenderHomingShard extends Render {
   private Random random = new Random();
   private static final ResourceLocation beamTexture = new ResourceLocation("thaumcraft", "textures/misc/beaml.png");

   public RenderHomingShard(RenderManager rm) {
      super(rm);
      this.shadowSize = 0.0F;
   }

   public void renderEntityAt(EntityHomingShard entity, double x, double y, double z, float fq, float pticks) {
      Tessellator tessellator = Tessellator.getInstance();
      GL11.glPushMatrix();
      GL11.glDepthMask(false);
      GL11.glEnable(3042);
      GL11.glBlendFunc(770, 1);
      this.bindTexture(ParticleEngine.particleTexture);
      float f2 = (8 + entity.ticksExisted % 8) / 64.0F;
      float f3 = f2 + 0.015625F;
      float f4 = 0.0625F;
      float f5 = f4 + 0.015625F;
      float f6 = 1.0F;
      float f7 = 0.5F;
      float f8 = 0.5F;
      GL11.glColor4f(0.9F, 0.075F, 0.9525F, 1.0F);
      GL11.glPushMatrix();
      GL11.glTranslated(x, y, z);
      GL11.glRotatef(180.0F - this.renderManager.playerViewY, 0.0F, 1.0F, 0.0F);
      GL11.glRotatef(-this.renderManager.playerViewX, 1.0F, 0.0F, 0.0F);
      GL11.glScalef(0.4F + 0.1F * entity.getStrength(), 0.4F + 0.1F * entity.getStrength(), 0.4F + 0.1F * entity.getStrength());
      tessellator.getBuffer().begin(7, UtilsFX.VERTEXFORMAT_POS_TEX_CO_LM_NO);
      int i = 240;
      int j = i >> 16 & 65535;
      int k = i & 65535;
      tessellator.getBuffer()
         .pos(-f7, -f8, 0.0)
         .tex(f2, f5)
         .color(0.9F, 0.075F, 0.9525F, 1.0F)
         .lightmap(j, k)
         .normal(0.0F, 1.0F, 0.0F)
         .endVertex();
      tessellator.getBuffer()
         .pos(f6 - f7, -f8, 0.0)
         .tex(f3, f5)
         .color(0.9F, 0.075F, 0.9525F, 1.0F)
         .lightmap(j, k)
         .normal(0.0F, 1.0F, 0.0F)
         .endVertex();
      tessellator.getBuffer()
         .pos(f6 - f7, 1.0F - f8, 0.0)
         .tex(f3, f4)
         .color(0.9F, 0.075F, 0.9525F, 1.0F)
         .lightmap(j, k)
         .normal(0.0F, 1.0F, 0.0F)
         .endVertex();
      tessellator.getBuffer()
         .pos(-f7, 1.0F - f8, 0.0)
         .tex(f2, f4)
         .color(0.9F, 0.075F, 0.9525F, 1.0F)
         .lightmap(j, k)
         .normal(0.0F, 1.0F, 0.0F)
         .endVertex();
      tessellator.draw();
      GL11.glPopMatrix();
      GL11.glPushMatrix();
      GL11.glBlendFunc(770, 1);
      this.bindTexture(beamTexture);
      Minecraft mc = Minecraft.getMinecraft();
      EntityPlayerSP p = mc.player;
      double doubleX = p.lastTickPosX + (p.posX - p.lastTickPosX) * pticks;
      double doubleY = p.lastTickPosY + (p.posY - p.lastTickPosY) * pticks;
      double doubleZ = p.lastTickPosZ + (p.posZ - p.lastTickPosZ) * pticks;
      UtilsFX.Vector player = new UtilsFX.Vector((float)doubleX, (float)doubleY, (float)doubleZ);
      double dX = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * pticks;
      double dY = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * pticks;
      double dZ = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * pticks;
      UtilsFX.Vector start = new UtilsFX.Vector((float)dX, (float)dY, (float)dZ);
      if (entity.vl.size() == 0) {
         entity.vl.add(start);
      }

      GL11.glTranslated(-doubleX, -doubleY, -doubleZ);
      UtilsFX.Vector vs = new UtilsFX.Vector((float)dX, (float)dY, (float)dZ);
      tessellator.getBuffer().begin(7, DefaultVertexFormats.POSITION_TEX_LMAP_COLOR);
      int c = entity.vl.size();

      for (UtilsFX.Vector nv : entity.vl) {
         UtilsFX.drawBeam(vs, nv, player, 0.25F * ((float)c / entity.vl.size()), 240, 0.405F, 0.075F, 0.525F, 0.5F);
         vs = nv;
         c--;
      }

      tessellator.draw();
      GL11.glPopMatrix();
      GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
      GL11.glBlendFunc(770, 771);
      GL11.glDisable(3042);
      GL11.glDisable(32826);
      GL11.glDepthMask(true);
      GL11.glPopMatrix();
   }

   public void doRender(Entity entity, double d, double d1, double d2, float f, float f1) {
      this.renderEntityAt((EntityHomingShard)entity, d, d1, d2, f, f1);
   }

   protected ResourceLocation getEntityTexture(Entity entity) {
      return TextureMap.LOCATION_BLOCKS_TEXTURE;
   }
}
