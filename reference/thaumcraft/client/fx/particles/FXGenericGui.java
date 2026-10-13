package thaumcraft.client.fx.particles;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import org.lwjgl.opengl.GL11;

public class FXGenericGui extends FXGeneric {
   boolean inGame = false;

   public FXGenericGui(World world, double x, double y, double z) {
      super(world, x, y, z);
      this.inGame = Minecraft.getMinecraft().inGameHasFocus;
   }

   public FXGenericGui(World world, double x, double y, double z, double xx, double yy, double zz) {
      super(world, x, y, z, xx, yy, zz);
      this.inGame = Minecraft.getMinecraft().inGameHasFocus;
   }

   @Override
   public void onUpdate() {
      super.onUpdate();
      if (!this.inGame && Minecraft.getMinecraft().inGameHasFocus) {
         this.setExpired();
      }
   }

   @Override
   public void draw(
      BufferBuilder wr, Entity entityIn, float partialTicks, float rotationX, float rotationZ, float rotationYZ, float rotationXY, float rotationXZ
   ) {
      float tx1 = (float)this.particleTextureIndexX / this.gridSize;
      float tx2 = tx1 + 1.0F / this.gridSize;
      float ty1 = (float)this.particleTextureIndexY / this.gridSize;
      float ty2 = ty1 + 1.0F / this.gridSize;
      float ts = 0.1F * this.particleScale;
      if (this.particleTexture != null) {
         tx1 = this.particleTexture.getMinU();
         tx2 = this.particleTexture.getMaxU();
         ty1 = this.particleTexture.getMinV();
         ty2 = this.particleTexture.getMaxV();
      }

      if (this.flipped) {
         float t = tx1;
         tx1 = tx2;
         tx2 = t;
      }

      float fs = MathHelper.clamp((this.particleAge + partialTicks) / this.particleMaxAge, 0.0F, 1.0F);
      float pr = this.particleRed + (this.dr - this.particleRed) * fs;
      float pg = this.particleGreen + (this.dg - this.particleGreen) * fs;
      float pb = this.particleBlue + (this.db - this.particleBlue) * fs;
      int i = this.getBrightnessForRender(partialTicks);
      int j = i >> 16 & 65535;
      int k = i & 65535;
      float f5 = (float)(this.prevPosX + (this.posX - this.prevPosX) * partialTicks);
      float f6 = (float)(this.prevPosY + (this.posY - this.prevPosY) * partialTicks);
      float f7 = (float)(this.prevPosZ + (this.posZ - this.prevPosZ) * partialTicks);
      GL11.glPushMatrix();
      GL11.glTranslatef(f5, f6, -90.0F + f7);
      if (this.angled) {
         GL11.glRotatef(-this.angleYaw + 90.0F, 0.0F, 1.0F, 0.0F);
         GL11.glRotatef(this.anglePitch + 90.0F, 1.0F, 0.0F, 0.0F);
      }

      if (this.particleAngle != 0.0F) {
         float f8 = this.particleAngle + (this.particleAngle - this.prevParticleAngle) * partialTicks;
         GL11.glRotated(f8 * (180.0 / Math.PI), 0.0, 0.0, 1.0);
      }

      wr.begin(7, DefaultVertexFormats.PARTICLE_POSITION_TEX_COLOR_LMAP);
      wr.pos(-ts, -ts, 0.0).tex(tx2, ty2).color(pr, pg, pb, this.particleAlpha).lightmap(j, k).endVertex();
      wr.pos(-ts, ts, 0.0).tex(tx2, ty1).color(pr, pg, pb, this.particleAlpha).lightmap(j, k).endVertex();
      wr.pos(ts, ts, 0.0).tex(tx1, ty1).color(pr, pg, pb, this.particleAlpha).lightmap(j, k).endVertex();
      wr.pos(ts, -ts, 0.0).tex(tx1, ty2).color(pr, pg, pb, this.particleAlpha).lightmap(j, k).endVertex();
      Tessellator.getInstance().draw();
      GL11.glPopMatrix();
   }
}
