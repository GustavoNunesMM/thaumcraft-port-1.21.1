package thaumcraft.client.fx.beams;

import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.client.FMLClientHandler;
import org.lwjgl.opengl.GL11;
import thaumcraft.client.fx.ParticleEngine;

public class FXBeamWand extends Particle {
   public int particle = 16;
   EntityLivingBase sourceEntity = null;
   private double offset = 0.0;
   private float length = 0.0F;
   private float rotYaw = 0.0F;
   private float rotPitch = 0.0F;
   private float prevYaw = 0.0F;
   private float prevPitch = 0.0F;
   private Entity targetEntity = null;
   private double tX = 0.0;
   private double tY = 0.0;
   private double tZ = 0.0;
   private double ptX = 0.0;
   private double ptY = 0.0;
   private double ptZ = 0.0;
   private int type = 0;
   private float endMod = 1.0F;
   private boolean reverse = false;
   private boolean pulse = true;
   private int rotationspeed = 5;
   private float prevSize = 0.0F;
   public int impact;
   ResourceLocation beam = new ResourceLocation("thaumcraft", "textures/misc/beam.png");
   ResourceLocation beam1 = new ResourceLocation("thaumcraft", "textures/misc/beam1.png");
   ResourceLocation beam2 = new ResourceLocation("thaumcraft", "textures/misc/beam2.png");
   ResourceLocation beam3 = new ResourceLocation("thaumcraft", "textures/misc/beam3.png");

   public FXBeamWand(World par1World, EntityLivingBase p, double tx, double ty, double tz, float red, float green, float blue, int age) {
      super(par1World, p.posX, p.posY, p.posZ, 0.0, 0.0, 0.0);
      this.offset = p.height / 2.0F + 0.25;
      this.particleRed = red;
      this.particleGreen = green;
      this.particleBlue = blue;
      this.sourceEntity = p;
      this.setSize(0.02F, 0.02F);
      this.motionX = 0.0;
      this.motionY = 0.0;
      this.motionZ = 0.0;
      this.tX = tx;
      this.tY = ty;
      this.tZ = tz;
      float xd = (float)(p.posX - this.tX);
      float yd = (float)(p.posY + this.offset - this.tY);
      float zd = (float)(p.posZ - this.tZ);
      this.length = MathHelper.sqrt(xd * xd + yd * yd + zd * zd);
      double var7x = MathHelper.sqrt(xd * xd + zd * zd);
      this.rotYaw = (float)(Math.atan2(xd, zd) * 180.0 / Math.PI);
      this.rotPitch = (float)(Math.atan2(yd, var7x) * 180.0 / Math.PI);
      this.prevYaw = this.rotYaw;
      this.prevPitch = this.rotPitch;
      this.particleMaxAge = age;
      Entity renderentity = FMLClientHandler.instance().getClient().getRenderViewEntity();
      int visibleDistance = 50;
      if (!FMLClientHandler.instance().getClient().gameSettings.fancyGraphics) {
         visibleDistance = 25;
      }

      if (renderentity.getDistance(p.posX, p.posY, p.posZ) > visibleDistance) {
         this.particleMaxAge = 0;
      }
   }

   public void updateBeam(double x, double y, double z) {
      this.tX = x;
      this.tY = y;
      this.tZ = z;

      while (this.particleMaxAge - this.particleAge < 4) {
         this.particleMaxAge++;
      }
   }

   public void onUpdate() {
      this.prevPosX = this.sourceEntity.posX;
      this.prevPosY = this.sourceEntity.posY + this.offset;
      this.prevPosZ = this.sourceEntity.posZ;
      this.ptX = this.tX;
      this.ptY = this.tY;
      this.ptZ = this.tZ;
      this.prevYaw = this.rotYaw;
      this.prevPitch = this.rotPitch;
      float xd = (float)(this.sourceEntity.posX - this.tX);
      float yd = (float)(this.sourceEntity.posY + this.offset - this.tY);
      float zd = (float)(this.sourceEntity.posZ - this.tZ);
      this.length = MathHelper.sqrt(xd * xd + yd * yd + zd * zd);
      double var7 = MathHelper.sqrt(xd * xd + zd * zd);
      this.rotYaw = (float)(Math.atan2(xd, zd) * 180.0 / Math.PI);
      this.rotPitch = (float)(Math.atan2(yd, var7) * 180.0 / Math.PI);

      while (this.rotPitch - this.prevPitch < -180.0F) {
         this.prevPitch -= 360.0F;
      }

      while (this.rotPitch - this.prevPitch >= 180.0F) {
         this.prevPitch += 360.0F;
      }

      while (this.rotYaw - this.prevYaw < -180.0F) {
         this.prevYaw -= 360.0F;
      }

      while (this.rotYaw - this.prevYaw >= 180.0F) {
         this.prevYaw += 360.0F;
      }

      if (this.impact > 0) {
         this.impact--;
      }

      if (this.particleAge++ >= this.particleMaxAge) {
         this.setExpired();
      }
   }

   public void setRGB(float r, float g, float b) {
      this.particleRed = r;
      this.particleGreen = g;
      this.particleBlue = b;
   }

   public void setType(int type) {
      this.type = type;
   }

   public void setEndMod(float endMod) {
      this.endMod = endMod;
   }

   public void setReverse(boolean reverse) {
      this.reverse = reverse;
   }

   public void setPulse(boolean pulse) {
      this.pulse = pulse;
   }

   public void setRotationspeed(int rotationspeed) {
      this.rotationspeed = rotationspeed;
   }

   public void renderParticle(BufferBuilder wr, Entity entityIn, float f, float f1, float f2, float f3, float f4, float f5) {
      Tessellator.getInstance().draw();
      GL11.glPushMatrix();
      float var9 = 1.0F;
      float slide = Minecraft.getMinecraft().player.ticksExisted;
      float rot = (float)(this.world.provider.getWorldTime() % (360 / this.rotationspeed) * this.rotationspeed) + this.rotationspeed * f;
      float size = 1.0F;
      if (this.pulse) {
         size = Math.min(this.particleAge / 4.0F, 1.0F);
         size = (float)(this.prevSize + (double)(size - this.prevSize) * f);
      }

      float op = 0.4F;
      if (this.pulse && this.particleMaxAge - this.particleAge <= 4) {
         op = 0.4F - (4 - (this.particleMaxAge - this.particleAge)) * 0.1F;
      }

      switch (this.type) {
         case 1:
            Minecraft.getMinecraft().renderEngine.bindTexture(this.beam1);
            break;
         case 2:
            Minecraft.getMinecraft().renderEngine.bindTexture(this.beam2);
            break;
         case 3:
            Minecraft.getMinecraft().renderEngine.bindTexture(this.beam3);
            break;
         default:
            Minecraft.getMinecraft().renderEngine.bindTexture(this.beam);
      }

      GL11.glTexParameterf(3553, 10242, 10497.0F);
      GL11.glTexParameterf(3553, 10243, 10497.0F);
      GL11.glDisable(2884);
      float var11 = slide + f;
      if (this.reverse) {
         var11 *= -1.0F;
      }

      float var12 = -var11 * 0.2F - MathHelper.floor(-var11 * 0.1F);
      GL11.glEnable(3042);
      GL11.glBlendFunc(770, 1);
      GL11.glDepthMask(false);
      double prex = this.sourceEntity.prevPosX;
      double prey = this.sourceEntity.prevPosY + this.offset;
      double prez = this.sourceEntity.prevPosZ;
      double px = this.sourceEntity.posX;
      double py = this.sourceEntity.posY + this.offset;
      double pz = this.sourceEntity.posZ;
      prex -= MathHelper.cos(this.sourceEntity.prevRotationYaw / 180.0F * 3.141593F) * 0.066F;
      prey -= 0.06;
      prez -= MathHelper.sin(this.sourceEntity.prevRotationYaw / 180.0F * 3.141593F) * 0.04F;
      Vec3d vec3d = this.sourceEntity.getLook(1.0F);
      prex += vec3d.x * 0.3;
      prey += vec3d.y * 0.3;
      prez += vec3d.z * 0.3;
      px -= MathHelper.cos(this.sourceEntity.rotationYaw / 180.0F * 3.141593F) * 0.066F;
      py -= 0.06;
      pz -= MathHelper.sin(this.sourceEntity.rotationYaw / 180.0F * 3.141593F) * 0.04F;
      vec3d = this.sourceEntity.getLook(1.0F);
      px += vec3d.x * 0.3;
      py += vec3d.y * 0.3;
      pz += vec3d.z * 0.3;
      float xx = (float)(prex + (px - prex) * f - interpPosX);
      float yy = (float)(prey + (py - prey) * f - interpPosY);
      float zz = (float)(prez + (pz - prez) * f - interpPosZ);
      GL11.glTranslated(xx, yy, zz);
      float ry = (float)(this.prevYaw + (double)(this.rotYaw - this.prevYaw) * f);
      float rp = (float)(this.prevPitch + (double)(this.rotPitch - this.prevPitch) * f);
      GL11.glRotatef(90.0F, 1.0F, 0.0F, 0.0F);
      GL11.glRotatef(180.0F + ry, 0.0F, 0.0F, -1.0F);
      GL11.glRotatef(rp, 1.0F, 0.0F, 0.0F);
      double var44 = -0.15 * size;
      double var17 = 0.15 * size;
      double var44b = -0.15 * size * this.endMod;
      double var17b = 0.15 * size * this.endMod;
      int i = 200;
      int j = i >> 16 & 65535;
      int k = i & 65535;
      GL11.glRotatef(rot, 0.0F, 1.0F, 0.0F);

      for (int t = 0; t < 3; t++) {
         double var29 = this.length * size * var9;
         double var31 = 0.0;
         double var33 = 1.0;
         double var35 = -1.0F + var12 + t / 3.0F;
         double var37 = this.length * size * var9 + var35;
         GL11.glRotatef(60.0F, 0.0F, 1.0F, 0.0F);
         wr.begin(7, DefaultVertexFormats.PARTICLE_POSITION_TEX_COLOR_LMAP);
         wr.pos(var44b, var29, 0.0)
            .tex(var33, var37)
            .color(this.particleRed, this.particleGreen, this.particleBlue, op)
            .lightmap(j, k)
            .endVertex();
         wr.pos(var44, 0.0, 0.0)
            .tex(var33, var35)
            .color(this.particleRed, this.particleGreen, this.particleBlue, op)
            .lightmap(j, k)
            .endVertex();
         wr.pos(var17, 0.0, 0.0)
            .tex(var31, var35)
            .color(this.particleRed, this.particleGreen, this.particleBlue, op)
            .lightmap(j, k)
            .endVertex();
         wr.pos(var17b, var29, 0.0)
            .tex(var31, var37)
            .color(this.particleRed, this.particleGreen, this.particleBlue, op)
            .lightmap(j, k)
            .endVertex();
         Tessellator.getInstance().draw();
      }

      GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
      GL11.glDepthMask(true);
      GL11.glBlendFunc(770, 771);
      GL11.glDisable(3042);
      GL11.glEnable(2884);
      GL11.glPopMatrix();
      if (this.impact > 0) {
         this.renderImpact(Tessellator.getInstance(), f, f1, f2, f3, f4, f5);
      }

      Minecraft.getMinecraft().renderEngine.bindTexture(ParticleManager.PARTICLE_TEXTURES);
      wr.begin(7, DefaultVertexFormats.PARTICLE_POSITION_TEX_COLOR_LMAP);
      this.prevSize = size;
   }

   public void renderImpact(Tessellator tessellator, float f, float f1, float f2, float f3, float f4, float f5) {
      GL11.glPushMatrix();
      GL11.glDepthMask(false);
      GL11.glEnable(3042);
      GL11.glBlendFunc(770, 1);
      Minecraft.getMinecraft().renderEngine.bindTexture(ParticleEngine.particleTexture);
      GL11.glColor4f(1.0F, 1.0F, 1.0F, 0.66F);
      int part = this.particleAge % 16;
      float var8 = part / 16.0F;
      float var9 = var8 + 0.0624375F;
      float var10 = 0.3125F;
      float var11 = var10 + 0.0624375F;
      float var12 = this.endMod / 2.0F / (6 - this.impact);
      float var13 = (float)(this.ptX + (this.tX - this.ptX) * f - interpPosX);
      float var14 = (float)(this.ptY + (this.tY - this.ptY) * f - interpPosY);
      float var15 = (float)(this.ptZ + (this.tZ - this.ptZ) * f - interpPosZ);
      float var16 = 1.0F;
      int i = 200;
      int j = i >> 16 & 65535;
      int k = i & 65535;
      tessellator.getBuffer().begin(7, DefaultVertexFormats.PARTICLE_POSITION_TEX_COLOR_LMAP);
      tessellator.getBuffer()
         .pos(var13 - f1 * var12 - f4 * var12, var14 - f2 * var12, var15 - f3 * var12 - f5 * var12)
         .tex(var9, var11)
         .color(this.particleRed, this.particleGreen, this.particleBlue, 0.66F)
         .lightmap(j, k)
         .endVertex();
      tessellator.getBuffer()
         .pos(var13 - f1 * var12 + f4 * var12, var14 + f2 * var12, var15 - f3 * var12 + f5 * var12)
         .tex(var9, var10)
         .color(this.particleRed, this.particleGreen, this.particleBlue, 0.66F)
         .lightmap(j, k)
         .endVertex();
      tessellator.getBuffer()
         .pos(var13 + f1 * var12 + f4 * var12, var14 + f2 * var12, var15 + f3 * var12 + f5 * var12)
         .tex(var8, var10)
         .color(this.particleRed, this.particleGreen, this.particleBlue, 0.66F)
         .lightmap(j, k)
         .endVertex();
      tessellator.getBuffer()
         .pos(var13 + f1 * var12 - f4 * var12, var14 - f2 * var12, var15 + f3 * var12 - f5 * var12)
         .tex(var8, var11)
         .color(this.particleRed, this.particleGreen, this.particleBlue, 0.66F)
         .lightmap(j, k)
         .endVertex();
      tessellator.draw();
      GL11.glBlendFunc(770, 771);
      GL11.glDisable(3042);
      GL11.glDepthMask(true);
      GL11.glPopMatrix();
   }
}
