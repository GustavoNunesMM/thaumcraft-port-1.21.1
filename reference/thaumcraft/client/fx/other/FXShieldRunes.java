package thaumcraft.client.fx.other;

import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import org.lwjgl.opengl.GL11;
import thaumcraft.client.lib.obj.AdvancedModelLoader;
import thaumcraft.client.lib.obj.IModelCustom;
import thaumcraft.common.entities.monster.cult.EntityCultist;

public class FXShieldRunes extends Particle {
   ResourceLocation[] tex1 = new ResourceLocation[15];
   ResourceLocation[] tex2 = new ResourceLocation[15];
   Entity target = null;
   float yaw = 0.0F;
   float pitch = 0.0F;
   private IModelCustom model;
   private static final ResourceLocation MODEL = new ResourceLocation("thaumcraft", "models/obj/hemis.obj");

   public FXShieldRunes(World world, double d, double d1, double d2, Entity target, int age, float yaw, float pitch) {
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
      this.yaw = yaw;
      this.pitch = pitch;
      if (target != null) {
         this.prevPosX = this.posX = target.posX;
         this.prevPosY = this.posY = (target.getEntityBoundingBox().minY + target.getEntityBoundingBox().maxY) / 2.0;
         this.prevPosZ = this.posZ = target.posZ;
      }

      for (int a = 0; a < 15; a++) {
         this.tex1[a] = new ResourceLocation("thaumcraft", "textures/models/ripple" + (a + 1) + ".png");
         this.tex2[a] = new ResourceLocation("thaumcraft", "textures/models/hemis" + (a + 1) + ".png");
      }
   }

   public void renderParticle(BufferBuilder wr, Entity entityIn, float f, float f1, float f2, float f3, float f4, float f5) {
      Tessellator.getInstance().draw();
      GL11.glPushMatrix();
      GL11.glDisable(2884);
      GL11.glDepthMask(false);
      GL11.glEnable(3042);
      GL11.glBlendFunc(770, 1);
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
      if (this.target != null && this.target instanceof EntityMob && !(this.target instanceof EntityCultist)) {
         Minecraft.getMinecraft().renderEngine.bindTexture(this.tex1[frame - 1]);
         b = 0.5F;
      } else {
         Minecraft.getMinecraft().renderEngine.bindTexture(this.tex2[frame - 1]);
      }

      int i = 220;
      int j = i % 65536;
      int k = i / 65536;
      OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, j / 1.0F, k / 1.0F);
      GL11.glRotatef(180.0F - this.yaw, 0.0F, 1.0F, 0.0F);
      GL11.glRotatef(-this.pitch, 1.0F, 0.0F, 0.0F);
      float th = this.target == null ? 2.0F : this.target.height;
      GL11.glScaled(0.2 * th, 0.2 * th, 0.2 * th);
      if (this.target == null) {
         GL11.glColor4f(0.65F, 0.1F, 0.5F, Math.min(1.0F, (1.0F - fade) * 3.0F));
      } else {
         GL11.glColor4f(b, b, b, Math.min(1.0F, (1.0F - fade) * 3.0F));
      }

      this.model.renderAll();
      GL11.glBlendFunc(770, 771);
      GL11.glDisable(3042);
      GL11.glDepthMask(true);
      GL11.glEnable(2884);
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

      if (this.target != null) {
         this.posX = this.target.posX;
         this.posY = (this.target.getEntityBoundingBox().minY + this.target.getEntityBoundingBox().maxY) / 2.0;
         this.posZ = this.target.posZ;
      }
   }
}
