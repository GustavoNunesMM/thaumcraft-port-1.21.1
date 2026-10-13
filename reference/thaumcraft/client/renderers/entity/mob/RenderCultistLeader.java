package thaumcraft.client.renderers.entity.mob;

import java.awt.Color;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderBiped;
import net.minecraft.client.renderer.entity.RenderLivingBase;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.layers.LayerBipedArmor;
import net.minecraft.client.renderer.entity.layers.LayerHeldItem;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;
import thaumcraft.common.entities.monster.boss.EntityCultistLeader;

@SideOnly(Side.CLIENT)
public class RenderCultistLeader extends RenderBiped<EntityCultistLeader> {
   private static final ResourceLocation skin = new ResourceLocation("thaumcraft", "textures/entity/cultist.png");
   private static final ResourceLocation fl = new ResourceLocation("thaumcraft", "textures/misc/wispy.png");

   public RenderCultistLeader(RenderManager p_i46127_1_) {
      super(p_i46127_1_, new ModelBiped(), 0.5F);
      this.addLayer(new LayerHeldItem(this));
      LayerBipedArmor layerbipedarmor = new LayerBipedArmor(this) {
         protected void initArmor() {
            this.modelLeggings = new ModelBiped();
            this.modelArmor = new ModelBiped();
         }
      };
      this.addLayer(layerbipedarmor);
   }

   protected ResourceLocation getEntityTexture(EntityCultistLeader entity) {
      return skin;
   }

   protected void preRenderCallback(EntityCultistLeader entitylivingbaseIn, float partialTickTime) {
      super.preRenderCallback(entitylivingbaseIn, partialTickTime);
      GL11.glScalef(1.15F, 1.15F, 1.15F);
   }

   private void drawFloatyLine(
      double x, double y, double z, double x2, double y2, double z2, float partialTicks, int color, float speed, float distance, float width
   ) {
      Entity player = Minecraft.getMinecraft().getRenderViewEntity();
      double iPX = player.prevPosX + (player.posX - player.prevPosX) * partialTicks;
      double iPY = player.prevPosY + (player.posY - player.prevPosY) * partialTicks;
      double iPZ = player.prevPosZ + (player.posZ - player.prevPosZ) * partialTicks;
      GL11.glTranslated(-iPX + x2, -iPY + y2, -iPZ + z2);
      float time = (float)(System.nanoTime() / 30000000L);
      Color co = new Color(color);
      float r = co.getRed() / 255.0F;
      float g = co.getGreen() / 255.0F;
      float b = co.getBlue() / 255.0F;
      GL11.glEnable(3042);
      GL11.glBlendFunc(770, 771);
      Tessellator tessellator = Tessellator.getInstance();
      double dc1x = (float)(x - x2);
      double dc1y = (float)(y - y2);
      double dc1z = (float)(z - z2);
      this.bindTexture(fl);
      tessellator.getBuffer().begin(5, DefaultVertexFormats.POSITION_TEX_COLOR);
      double dx2 = 0.0;
      double dy2 = 0.0;
      double dz2 = 0.0;
      double d3 = x - x2;
      double d4 = y - y2;
      double d5 = z - z2;
      float dist = MathHelper.sqrt(d3 * d3 + d4 * d4 + d5 * d5);
      float blocks = Math.round(dist);
      float length = blocks * 6.0F;
      float f9 = 0.0F;
      float f10 = 1.0F;

      for (int i = 0; i <= length * distance; i++) {
         float f2 = i / length;
         float f2a = i * 1.5F / length;
         f2a = Math.min(0.75F, f2a);
         float f3 = 1.0F - Math.abs(i - length / 2.0F) / (length / 2.0F);
         double dx = dc1x + MathHelper.sin((float)((z % 16.0 + dist * (1.0F - f2) * 6.0F - time % 32767.0F / 5.0F) / 4.0)) * 0.5F * f3;
         double dy = dc1y + MathHelper.sin((float)((x % 16.0 + dist * (1.0F - f2) * 6.0F - time % 32767.0F / 5.0F) / 3.0)) * 0.5F * f3;
         double dz = dc1z + MathHelper.sin((float)((y % 16.0 + dist * (1.0F - f2) * 6.0F - time % 32767.0F / 5.0F) / 2.0)) * 0.5F * f3;
         float f13 = (1.0F - f2) * dist - time * speed;
         tessellator.getBuffer().pos(dx * f2, dy * f2 - width, dz * f2).tex(f13, f10).color(r, g, b, 0.8F).endVertex();
         tessellator.getBuffer().pos(dx * f2, dy * f2 + width, dz * f2).tex(f13, f9).color(r, g, b, 0.8F).endVertex();
      }

      tessellator.draw();
      tessellator.getBuffer().begin(5, DefaultVertexFormats.POSITION_TEX_COLOR);

      for (int var84 = 0; var84 <= length * distance; var84++) {
         float f2 = var84 / length;
         float f2a = var84 * 1.5F / length;
         f2a = Math.min(0.75F, f2a);
         float f3 = 1.0F - Math.abs(var84 - length / 2.0F) / (length / 2.0F);
         double dx = dc1x + MathHelper.sin((float)((z % 16.0 + dist * (1.0F - f2) * 6.0F - time % 32767.0F / 5.0F) / 4.0)) * 0.5F * f3;
         double dy = dc1y + MathHelper.sin((float)((x % 16.0 + dist * (1.0F - f2) * 6.0F - time % 32767.0F / 5.0F) / 3.0)) * 0.5F * f3;
         double dz = dc1z + MathHelper.sin((float)((y % 16.0 + dist * (1.0F - f2) * 6.0F - time % 32767.0F / 5.0F) / 2.0)) * 0.5F * f3;
         float f13 = (1.0F - f2) * dist - time * speed;
         tessellator.getBuffer().pos(dx * f2 - width, dy * f2, dz * f2).tex(f13, f10).color(r, g, b, 0.8F).endVertex();
         tessellator.getBuffer().pos(dx * f2 + width, dy * f2, dz * f2).tex(f13, f9).color(r, g, b, 0.8F).endVertex();
      }

      tessellator.draw();
      GL11.glDisable(3042);
   }
}
