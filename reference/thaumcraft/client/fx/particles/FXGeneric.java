package thaumcraft.client.fx.particles;

import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.lwjgl.opengl.GL11;
import thaumcraft.common.lib.utils.Utils;

public class FXGeneric extends Particle {
   boolean doneFrames = false;
   boolean flipped = false;
   double windX;
   double windZ;
   int layer = 0;
   float dr = 0.0F;
   float dg = 0.0F;
   float db = 0.0F;
   boolean loop = false;
   float rotationSpeed = 0.0F;
   int startParticle = 0;
   int numParticles = 1;
   int particleInc = 1;
   float[] scaleKeys = new float[]{1.0F};
   float[] scaleFrames = new float[]{0.0F};
   float[] alphaKeys = new float[]{1.0F};
   float[] alphaFrames = new float[]{0.0F};
   double slowDown = 0.98F;
   float randomX;
   float randomY;
   float randomZ;
   int[] finalFrames = null;
   boolean angled = false;
   float angleYaw;
   float anglePitch;
   int gridSize = 64;

   public FXGeneric(World world, double x, double y, double z, double xx, double yy, double zz) {
      super(world, x, y, z, xx, yy, zz);
      this.setSize(0.1F, 0.1F);
      this.setPosition(x, y, z);
      this.prevPosX = this.posX;
      this.prevPosY = this.posY;
      this.prevPosZ = this.posZ;
      this.prevPosX = x;
      this.prevPosY = y;
      this.prevPosZ = z;
      this.particleTextureJitterX = 0.0F;
      this.particleTextureJitterY = 0.0F;
      this.motionX = xx;
      this.motionY = yy;
      this.motionZ = zz;
   }

   public FXGeneric(World world, double x, double y, double z) {
      super(world, x, y, z, 0.0, 0.0, 0.0);
      this.setSize(0.1F, 0.1F);
      this.setPosition(x, y, z);
      this.prevPosX = this.posX;
      this.prevPosY = this.posY;
      this.prevPosZ = this.posZ;
      this.prevPosX = x;
      this.prevPosY = y;
      this.prevPosZ = z;
      this.particleTextureJitterX = 0.0F;
      this.particleTextureJitterY = 0.0F;
   }

   void calculateFrames() {
      this.doneFrames = true;
      if (this.alphaKeys == null) {
         this.setAlphaF(1.0F);
      }

      this.alphaFrames = new float[this.particleMaxAge + 1];
      float inc = (float)(this.alphaKeys.length - 1) / this.particleMaxAge;
      float is = 0.0F;

      for (int a = 0; a <= this.particleMaxAge; a++) {
         int isF = MathHelper.floor(is);
         float diff = isF < this.alphaKeys.length - 1 ? this.alphaKeys[isF + 1] - this.alphaKeys[isF] : 0.0F;
         float pa = is - isF;
         this.alphaFrames[a] = this.alphaKeys[isF] + diff * pa;
         is += inc;
      }

      if (this.scaleKeys == null) {
         this.setScale(1.0F);
      }

      this.scaleFrames = new float[this.particleMaxAge + 1];
      inc = (float)(this.scaleKeys.length - 1) / this.particleMaxAge;
      is = 0.0F;

      for (int a = 0; a <= this.particleMaxAge; a++) {
         int isF = MathHelper.floor(is);
         float diff = isF < this.scaleKeys.length - 1 ? this.scaleKeys[isF + 1] - this.scaleKeys[isF] : 0.0F;
         float pa = is - isF;
         this.scaleFrames[a] = this.scaleKeys[isF] + diff * pa;
         is += inc;
      }
   }

   public void onUpdate() {
      if (!this.doneFrames) {
         this.calculateFrames();
      }

      this.prevPosX = this.posX;
      this.prevPosY = this.posY;
      this.prevPosZ = this.posZ;
      if (this.particleAge++ >= this.particleMaxAge) {
         this.setExpired();
      }

      this.prevParticleAngle = this.particleAngle;
      this.particleAngle = this.particleAngle + (float) Math.PI * this.rotationSpeed * 2.0F;
      this.motionY = this.motionY - 0.04 * this.particleGravity;
      this.move(this.motionX, this.motionY, this.motionZ);
      this.motionX = this.motionX * this.slowDown;
      this.motionY = this.motionY * this.slowDown;
      this.motionZ = this.motionZ * this.slowDown;
      this.motionX = this.motionX + this.world.rand.nextGaussian() * this.randomX;
      this.motionY = this.motionY + this.world.rand.nextGaussian() * this.randomY;
      this.motionZ = this.motionZ + this.world.rand.nextGaussian() * this.randomZ;
      this.motionX = this.motionX + this.windX;
      this.motionZ = this.motionZ + this.windZ;
      if (this.onGround && this.slowDown != 1.0) {
         this.motionX *= 0.7F;
         this.motionZ *= 0.7F;
      }
   }

   public void renderParticle(BufferBuilder wr, Entity entity, float f, float f1, float f2, float f3, float f4, float f5) {
      if (this.loop) {
         this.setParticleTextureIndex(this.startParticle + this.particleAge / this.particleInc % this.numParticles);
      } else {
         float fs = (float)this.particleAge / this.particleMaxAge;
         this.setParticleTextureIndex((int)(this.startParticle + Math.min(this.numParticles * fs, (float)(this.numParticles - 1))));
      }

      if (this.finalFrames != null && this.finalFrames.length > 0 && this.particleAge > this.particleMaxAge - this.finalFrames.length) {
         int frame = this.particleMaxAge - this.particleAge;
         if (frame < 0) {
            frame = 0;
         }

         this.setParticleTextureIndex(this.finalFrames[frame]);
      }

      this.particleAlpha = this.alphaFrames.length <= 0 ? 0.0F : this.alphaFrames[Math.min(this.particleAge, this.alphaFrames.length - 1)];
      this.particleScale = this.scaleFrames.length <= 0 ? 0.0F : this.scaleFrames[Math.min(this.particleAge, this.scaleFrames.length - 1)];
      this.draw(wr, entity, f, f1, f2, f3, f4, f5);
   }

   public boolean isFlipped() {
      return this.flipped;
   }

   public void setFlipped(boolean flip) {
      this.flipped = flip;
   }

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
      float f5 = (float)(this.prevPosX + (this.posX - this.prevPosX) * partialTicks - interpPosX);
      float f6 = (float)(this.prevPosY + (this.posY - this.prevPosY) * partialTicks - interpPosY);
      float f7 = (float)(this.prevPosZ + (this.posZ - this.prevPosZ) * partialTicks - interpPosZ);
      if (this.angled) {
         Tessellator.getInstance().draw();
         GL11.glPushMatrix();
         GL11.glTranslated(f5, f6, f7);
         GL11.glRotatef(-this.angleYaw + 90.0F, 0.0F, 1.0F, 0.0F);
         GL11.glRotatef(this.anglePitch + 90.0F, 1.0F, 0.0F, 0.0F);
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
         wr.begin(7, DefaultVertexFormats.PARTICLE_POSITION_TEX_COLOR_LMAP);
      } else {
         Vec3d[] avec3d = new Vec3d[]{
            new Vec3d(-rotationX * ts - rotationXY * ts, -rotationZ * ts, -rotationYZ * ts - rotationXZ * ts),
            new Vec3d(-rotationX * ts + rotationXY * ts, rotationZ * ts, -rotationYZ * ts + rotationXZ * ts),
            new Vec3d(rotationX * ts + rotationXY * ts, rotationZ * ts, rotationYZ * ts + rotationXZ * ts),
            new Vec3d(rotationX * ts - rotationXY * ts, -rotationZ * ts, rotationYZ * ts - rotationXZ * ts)
         };
         if (this.particleAngle != 0.0F) {
            float f8 = this.particleAngle + (this.particleAngle - this.prevParticleAngle) * partialTicks;
            float f9 = MathHelper.cos(f8 * 0.5F);
            float f10 = MathHelper.sin(f8 * 0.5F) * (float)cameraViewDir.x;
            float f11 = MathHelper.sin(f8 * 0.5F) * (float)cameraViewDir.y;
            float f12 = MathHelper.sin(f8 * 0.5F) * (float)cameraViewDir.z;
            Vec3d vec3d = new Vec3d(f10, f11, f12);

            for (int l = 0; l < 4; l++) {
               avec3d[l] = vec3d.scale(2.0 * avec3d[l].dotProduct(vec3d))
                  .add(avec3d[l].scale(f9 * f9 - vec3d.dotProduct(vec3d)))
                  .add(vec3d.crossProduct(avec3d[l]).scale(2.0F * f9));
            }
         }

         wr.pos(f5 + avec3d[0].x, f6 + avec3d[0].y, f7 + avec3d[0].z)
            .tex(tx2, ty2)
            .color(pr, pg, pb, this.particleAlpha)
            .lightmap(j, k)
            .endVertex();
         wr.pos(f5 + avec3d[1].x, f6 + avec3d[1].y, f7 + avec3d[1].z)
            .tex(tx2, ty1)
            .color(pr, pg, pb, this.particleAlpha)
            .lightmap(j, k)
            .endVertex();
         wr.pos(f5 + avec3d[2].x, f6 + avec3d[2].y, f7 + avec3d[2].z)
            .tex(tx1, ty1)
            .color(pr, pg, pb, this.particleAlpha)
            .lightmap(j, k)
            .endVertex();
         wr.pos(f5 + avec3d[3].x, f6 + avec3d[3].y, f7 + avec3d[3].z)
            .tex(tx1, ty2)
            .color(pr, pg, pb, this.particleAlpha)
            .lightmap(j, k)
            .endVertex();
      }
   }

   public void setWind(double d) {
      int m = this.world.getMoonPhase();
      Vec3d vsource = new Vec3d(0.0, 0.0, 0.0);
      Vec3d vtar = new Vec3d(0.1, 0.0, 0.0);
      vtar = Utils.rotateAroundY(vtar, m * (40 + this.world.rand.nextInt(10)) / 180.0F * (float) Math.PI);
      Vec3d vres = vsource.add(vtar.x, vtar.y, vtar.z);
      this.windX = vres.x * d;
      this.windZ = vres.z * d;
   }

   public void setLayer(int layer) {
      this.layer = layer;
   }

   public void setRBGColorF(float particleRedIn, float particleGreenIn, float particleBlueIn) {
      super.setRBGColorF(particleRedIn, particleGreenIn, particleBlueIn);
      this.dr = particleRedIn;
      this.dg = particleGreenIn;
      this.db = particleBlueIn;
   }

   public void setRBGColorF(float particleRedIn, float particleGreenIn, float particleBlueIn, float r2, float g2, float b2) {
      super.setRBGColorF(particleRedIn, particleGreenIn, particleBlueIn);
      this.dr = r2;
      this.dg = g2;
      this.db = b2;
   }

   public int getFXLayer() {
      return this.layer;
   }

   public void setLoop(boolean loop) {
      this.loop = loop;
   }

   public void setRotationSpeed(float rot) {
      this.rotationSpeed = (float)(rot * 0.017453292519943);
   }

   public void setRotationSpeed(float start, float rot) {
      this.particleAngle = (float)(start * Math.PI * 2.0);
      this.rotationSpeed = (float)(rot * 0.017453292519943);
   }

   public void setMaxAge(int max) {
      this.particleMaxAge = max;
   }

   public void setParticles(int startParticle, int numParticles, int particleInc) {
      this.numParticles = numParticles;
      this.particleInc = particleInc;
      this.startParticle = startParticle;
      this.setParticleTextureIndex(startParticle);
   }

   public void setParticle(int startParticle) {
      this.numParticles = 1;
      this.particleInc = 1;
      this.startParticle = startParticle;
      this.setParticleTextureIndex(startParticle);
   }

   public void setScale(float... scale) {
      this.particleScale = scale[0];
      this.scaleKeys = scale;
   }

   public void setAlphaF(float... a1) {
      super.setAlphaF(a1[0]);
      this.alphaKeys = a1;
   }

   public void setAlphaF(float a1) {
      super.setAlphaF(a1);
      this.alphaKeys = new float[1];
      this.alphaKeys[0] = a1;
   }

   public void setSlowDown(double slowDown) {
      this.slowDown = slowDown;
   }

   public void setRandomMovementScale(float x, float y, float z) {
      this.randomX = x;
      this.randomY = y;
      this.randomZ = z;
   }

   public void setFinalFrames(int... frames) {
      this.finalFrames = frames;
   }

   public void setAngles(float yaw, float pitch) {
      this.angleYaw = yaw;
      this.anglePitch = pitch;
      this.angled = true;
   }

   public void setGravity(float g) {
      this.particleGravity = g;
   }

   public void setParticleTextureIndex(int index) {
      if (index < 0) {
         index = 0;
      }

      this.particleTextureIndexX = index % this.gridSize;
      this.particleTextureIndexY = index / this.gridSize;
   }

   public void setGridSize(int gridSize) {
      this.gridSize = gridSize;
   }

   public void setNoClip(boolean clip) {
      this.canCollide = clip;
   }
}
