package thaumcraft.client.fx.other;

import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import org.lwjgl.opengl.GL11;

public class FXBlockWard extends Particle {
   ResourceLocation[] tex1 = new ResourceLocation[15];
   EnumFacing side;
   int rotation = 0;
   float sx = 0.0F;
   float sy = 0.0F;
   float sz = 0.0F;

   public FXBlockWard(World world, double d, double d1, double d2, EnumFacing side, float f, float f1, float f2) {
      super(world, d, d1, d2, 0.0, 0.0, 0.0);
      this.side = side;
      this.particleGravity = 0.0F;
      this.motionX = this.motionY = this.motionZ = 0.0;
      this.particleMaxAge = 12 + this.rand.nextInt(5);
      this.setSize(0.01F, 0.01F);
      this.prevPosX = this.posX;
      this.prevPosY = this.posY;
      this.prevPosZ = this.posZ;
      this.particleScale = (float)(1.4 + this.rand.nextGaussian() * 0.3F);
      this.rotation = this.rand.nextInt(360);
      this.sx = MathHelper.clamp(f - 0.6F + this.rand.nextFloat() * 0.2F, -0.4F, 0.4F);
      this.sy = MathHelper.clamp(f1 - 0.6F + this.rand.nextFloat() * 0.2F, -0.4F, 0.4F);
      this.sz = MathHelper.clamp(f2 - 0.6F + this.rand.nextFloat() * 0.2F, -0.4F, 0.4F);
      if (side.getXOffset() != 0) {
         this.sx = 0.0F;
      }

      if (side.getYOffset() != 0) {
         this.sy = 0.0F;
      }

      if (side.getZOffset() != 0) {
         this.sz = 0.0F;
      }

      for (int a = 0; a < 15; a++) {
         this.tex1[a] = new ResourceLocation("thaumcraft", "textures/models/hemis" + (a + 1) + ".png");
      }
   }

   public void renderParticle(BufferBuilder wr, Entity entityIn, float f, float f1, float f2, float f3, float f4, float f5) {
      Tessellator.getInstance().draw();
      GL11.glPushMatrix();
      float fade = (this.particleAge + f) / this.particleMaxAge;
      int frame = Math.min(15, (int)(15.0F * fade));
      Minecraft.getMinecraft().renderEngine.bindTexture(this.tex1[frame - 1]);
      GL11.glDepthMask(false);
      GL11.glEnable(3042);
      GL11.glBlendFunc(770, 1);
      GL11.glColor4f(1.0F, 1.0F, 1.0F, this.particleAlpha / 2.0F);
      float var13 = (float)(this.prevPosX + (this.posX - this.prevPosX) * f - interpPosX);
      float var14 = (float)(this.prevPosY + (this.posY - this.prevPosY) * f - interpPosY);
      float var15 = (float)(this.prevPosZ + (this.posZ - this.prevPosZ) * f - interpPosZ);
      GL11.glTranslated(var13 + this.sx, var14 + this.sy, var15 + this.sz);
      GL11.glRotatef(90.0F, this.side.getYOffset(), -this.side.getXOffset(), this.side.getZOffset());
      GL11.glRotatef(this.rotation, 0.0F, 0.0F, 1.0F);
      if (this.side.getZOffset() > 0) {
         GL11.glTranslated(0.0, 0.0, 0.505F);
         GL11.glRotatef(180.0F, 0.0F, -1.0F, 0.0F);
      } else {
         GL11.glTranslated(0.0, 0.0, -0.505F);
      }

      float var12 = this.particleScale;
      float var16 = 1.0F;
      wr.begin(7, DefaultVertexFormats.PARTICLE_POSITION_TEX_COLOR_LMAP);
      int i = 240;
      int j = i >> 16 & 65535;
      int k = i & 65535;
      wr.pos(-0.5 * var12, 0.5 * var12, 0.0)
         .tex(0.0, 1.0)
         .color(this.particleRed * var16, this.particleGreen * var16, this.particleBlue * var16, this.particleAlpha / 2.0F)
         .lightmap(j, k)
         .endVertex();
      wr.pos(0.5 * var12, 0.5 * var12, 0.0)
         .tex(1.0, 1.0)
         .color(this.particleRed * var16, this.particleGreen * var16, this.particleBlue * var16, this.particleAlpha / 2.0F)
         .lightmap(j, k)
         .endVertex();
      wr.pos(0.5 * var12, -0.5 * var12, 0.0)
         .tex(1.0, 0.0)
         .color(this.particleRed * var16, this.particleGreen * var16, this.particleBlue * var16, this.particleAlpha / 2.0F)
         .lightmap(j, k)
         .endVertex();
      wr.pos(-0.5 * var12, -0.5 * var12, 0.0)
         .tex(0.0, 0.0)
         .color(this.particleRed * var16, this.particleGreen * var16, this.particleBlue * var16, this.particleAlpha / 2.0F)
         .lightmap(j, k)
         .endVertex();
      Tessellator.getInstance().draw();
      GL11.glDisable(3042);
      GL11.glDepthMask(true);
      GL11.glPopMatrix();
      Minecraft.getMinecraft().renderEngine.bindTexture(ParticleManager.PARTICLE_TEXTURES);
      wr.begin(7, DefaultVertexFormats.PARTICLE_POSITION_TEX_COLOR_LMAP);
   }

   public void onUpdate() {
      this.prevPosX = this.posX;
      this.prevPosY = this.posY;
      this.prevPosZ = this.posZ;
      float threshold = this.particleMaxAge / 5.0F;
      if (this.particleAge <= threshold) {
         this.particleAlpha = this.particleAge / threshold;
      } else {
         this.particleAlpha = (float)(this.particleMaxAge - this.particleAge) / this.particleMaxAge;
      }

      if (this.particleAge++ >= this.particleMaxAge) {
         this.setExpired();
      }

      this.motionY = this.motionY - 0.04 * this.particleGravity;
      this.posX = this.posX + this.motionX;
      this.posY = this.posY + this.motionY;
      this.posZ = this.posZ + this.motionZ;
   }

   public void setGravity(float value) {
      this.particleGravity = value;
   }
}
