package thaumcraft.client.fx.other;

import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import org.lwjgl.opengl.GL11;
import thaumcraft.client.lib.obj.AdvancedModelLoader;
import thaumcraft.client.lib.obj.IModelCustom;

public class FXSonic extends Particle {
   Entity target = null;
   float yaw = 0.0F;
   float pitch = 0.0F;
   private IModelCustom model;
   private static final ResourceLocation MODEL = new ResourceLocation("thaumcraft", "models/obj/hemis.obj");

   public FXSonic(World world, double d, double d1, double d2, Entity target, int age) {
      super(world, d, d1, d2, 0.0, 0.0, 0.0);
      this.particleRed = 1.0F;
      this.particleGreen = 1.0F;
      this.particleBlue = 1.0F;
      this.particleGravity = 0.0F;
      this.motionX = this.motionY = this.motionZ = 0.0;
      this.particleMaxAge = age + this.rand.nextInt(age / 2);
      this.setSize(0.01F, 0.01F);
      this.particleScale = 1.0F;
      this.target = target;
      this.yaw = target.getRotationYawHead();
      this.pitch = target.rotationPitch;
      this.prevPosX = this.posX = target.posX;
      this.prevPosY = this.posY = target.posY + target.getEyeHeight();
      this.prevPosZ = this.posZ = target.posZ;
   }

   public void renderParticle(BufferBuilder wr, Entity entityIn, float f, float f1, float f2, float f3, float f4, float f5) {
      Tessellator.getInstance().draw();
      GL11.glPushMatrix();
      GL11.glDepthMask(false);
      GL11.glEnable(3042);
      GL11.glBlendFunc(770, 771);
      if (this.model == null) {
         this.model = AdvancedModelLoader.loadModel(MODEL);
      }

      float fade = (this.particleAge + f) / this.particleMaxAge;
      float xx = (float)(this.prevPosX + (this.posX - this.prevPosX) * f - interpPosX);
      float yy = (float)(this.prevPosY + (this.posY - this.prevPosY) * f - interpPosY);
      float zz = (float)(this.prevPosZ + (this.posZ - this.prevPosZ) * f - interpPosZ);
      GL11.glTranslated(xx, yy, zz);
      float b = 1.0F;
      int frame = Math.min(15, (int)(14.0F * fade) + 1);
      Minecraft.getMinecraft().renderEngine.bindTexture(new ResourceLocation("thaumcraft", "textures/models/ripple" + frame + ".png"));
      b = 0.5F;
      int i = 220;
      int j = i % 65536;
      int k = i / 65536;
      OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, j / 1.0F, k / 1.0F);
      GL11.glRotatef(-this.yaw, 0.0F, 1.0F, 0.0F);
      GL11.glRotatef(this.pitch, 1.0F, 0.0F, 0.0F);
      GL11.glTranslated(0.0, 0.0, 2.0F * this.target.height + this.target.width / 2.0F);
      GL11.glScaled(0.25 * this.target.height, 0.25 * this.target.height, -1.0F * this.target.height);
      GL11.glColor4f(b, b, b, 1.0F);
      this.model.renderAll();
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
      if (this.particleAge++ >= this.particleMaxAge) {
         this.setExpired();
      }

      this.posX = this.target.posX;
      this.posY = this.target.posY + this.target.getEyeHeight();
      this.posZ = this.target.posZ;
   }
}
