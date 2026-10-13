package thaumcraft.client.fx;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import thaumcraft.Thaumcraft;

/**
 * FXGeneric: the all-purpose particle of the original, drawn from textures/misc/particles.png (a grid of
 * gridSize x gridSize frames). Alpha and scale are keyframed over the particle's life; layer 0 is additive,
 * layer 1 alpha-blended (2/3 were the same without depth test; here they only skip depth writes).
 */
public class FXGeneric extends Particle {
   public static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/misc/particles.png");
   public static final ParticleRenderType[] LAYERS = {layer("additive", true), layer("blend", false), layer("additive_top", true),
      layer("blend_top", false)};

   private static ParticleRenderType layer(String name, boolean additive) {
      return new ParticleRenderType() {
         @Override
         public BufferBuilder begin(Tesselator tesselator, TextureManager textureManager) {
            RenderSystem.setShader(GameRenderer::getParticleShader);
            RenderSystem.setShaderTexture(0, TEXTURE);
            RenderSystem.enableBlend();
            if (additive) RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
            else RenderSystem.defaultBlendFunc();
            RenderSystem.depthMask(false);
            return tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);
         }

         @Override
         public String toString() {
            return "thaumcraft:" + name;
         }
      };
   }

   boolean doneFrames;
   boolean flipped;
   double windX;
   double windZ;
   int layer;
   float dr, dg, db;
   boolean loop;
   float rotationSpeed;
   int startParticle;
   int numParticles = 1;
   int particleInc = 1;
   float[] scaleKeys = {1.0F};
   float[] scaleFrames = {0.0F};
   float[] alphaKeys = {1.0F};
   float[] alphaFrames = {0.0F};
   double slowDown = 0.98F;
   float randomX, randomY, randomZ;
   int[] finalFrames;
   boolean angled;
   float angleYaw, anglePitch;
   int gridSize = 64;
   int textureIndex;
   float particleScale = 1.0F;

   public FXGeneric(ClientLevel level, double x, double y, double z, double xx, double yy, double zz) {
      super(level, x, y, z);
      setSize(0.1F, 0.1F);
      this.xd = xx;
      this.yd = yy;
      this.zd = zz;
   }

   public FXGeneric(ClientLevel level, double x, double y, double z) {
      this(level, x, y, z, 0, 0, 0);
   }

   private static float[] frames(float[] keys, int maxAge) {
      float[] out = new float[maxAge + 1];
      float inc = (float) (keys.length - 1) / maxAge;
      float is = 0.0F;
      for (int a = 0; a <= maxAge; a++) {
         int isF = Mth.floor(is);
         float diff = isF < keys.length - 1 ? keys[isF + 1] - keys[isF] : 0.0F;
         out[a] = keys[Math.min(isF, keys.length - 1)] + diff * (is - isF);
         is += inc;
      }
      return out;
   }

   void calculateFrames() {
      doneFrames = true;
      alphaFrames = frames(alphaKeys, lifetime);
      scaleFrames = frames(scaleKeys, lifetime);
   }

   @Override
   public void tick() {
      if (!doneFrames) calculateFrames();
      xo = x;
      yo = y;
      zo = z;
      if (age++ >= lifetime) remove();
      oRoll = roll;
      roll += (float) Math.PI * rotationSpeed * 2.0F;
      yd -= 0.04 * gravity;
      move(xd, yd, zd);
      xd *= slowDown;
      yd *= slowDown;
      zd *= slowDown;
      xd += random.nextGaussian() * randomX;
      yd += random.nextGaussian() * randomY;
      zd += random.nextGaussian() * randomZ;
      xd += windX;
      zd += windZ;
      if (onGround && slowDown != 1.0) {
         xd *= 0.7F;
         zd *= 0.7F;
      }
   }

   @Override
   public void render(VertexConsumer buffer, Camera camera, float partialTicks) {
      if (!doneFrames) calculateFrames();
      if (loop) {
         textureIndex = startParticle + age / particleInc % numParticles;
      } else {
         float fs = (float) age / lifetime;
         textureIndex = (int) (startParticle + Math.min(numParticles * fs, (float) (numParticles - 1)));
      }
      if (finalFrames != null && finalFrames.length > 0 && age > lifetime - finalFrames.length) {
         textureIndex = finalFrames[Math.max(0, lifetime - age)];
      }
      alpha = alphaFrames.length == 0 ? 0.0F : alphaFrames[Math.min(age, alphaFrames.length - 1)];
      particleScale = scaleFrames.length == 0 ? 0.0F : scaleFrames[Math.min(age, scaleFrames.length - 1)];

      int idx = Math.max(0, textureIndex);
      float tx1 = (float) (idx % gridSize) / gridSize;
      float tx2 = tx1 + 1.0F / gridSize;
      float ty1 = (float) (idx / gridSize) / gridSize;
      float ty2 = ty1 + 1.0F / gridSize;
      if (flipped) {
         float t = tx1;
         tx1 = tx2;
         tx2 = t;
      }
      float ts = 0.1F * particleScale;
      float fs = Mth.clamp((age + partialTicks) / lifetime, 0.0F, 1.0F);
      float pr = rCol + (dr - rCol) * fs;
      float pg = gCol + (dg - gCol) * fs;
      float pb = bCol + (db - bCol) * fs;
      int light = getLightColor(partialTicks);
      Vec3 cam = camera.getPosition();
      float px = (float) (Mth.lerp(partialTicks, xo, x) - cam.x);
      float py = (float) (Mth.lerp(partialTicks, yo, y) - cam.y);
      float pz = (float) (Mth.lerp(partialTicks, zo, z) - cam.z);
      Quaternionf q = new Quaternionf();
      if (angled) {
         q.rotateY((float) Math.toRadians(-angleYaw + 90.0F)).rotateX((float) Math.toRadians(anglePitch + 90.0F));
      } else {
         q.set(camera.rotation());
      }
      float r = Mth.lerp(partialTicks, oRoll, roll);
      if (r != 0.0F) q.rotateZ(r);
      vertex(buffer, q, px, py, pz, 1.0F, -1.0F, ts, tx2, ty2, pr, pg, pb, light);
      vertex(buffer, q, px, py, pz, 1.0F, 1.0F, ts, tx2, ty1, pr, pg, pb, light);
      vertex(buffer, q, px, py, pz, -1.0F, 1.0F, ts, tx1, ty1, pr, pg, pb, light);
      vertex(buffer, q, px, py, pz, -1.0F, -1.0F, ts, tx1, ty2, pr, pg, pb, light);
   }

   private void vertex(VertexConsumer b, Quaternionf q, float x, float y, float z, float ox, float oy, float size, float u, float v,
                       float r, float g, float bl, int light) {
      Vector3f p = new Vector3f(ox, oy, 0.0F).rotate(q).mul(size).add(x, y, z);
      b.addVertex(p.x(), p.y(), p.z()).setUv(u, v).setColor(r, g, bl, alpha).setLight(light);
   }

   @Override
   public ParticleRenderType getRenderType() {
      return LAYERS[Math.max(0, Math.min(3, layer))];
   }

   public void setWind(double d) {
      long m = level.getMoonPhase();
      Vec3 v = new Vec3(0.1, 0.0, 0.0).yRot(m * (40 + random.nextInt(10)) / 180.0F * (float) Math.PI);
      windX = v.x * d;
      windZ = v.z * d;
   }

   public void setLayer(int layer) {
      this.layer = layer;
   }

   public void setRBGColorF(float r, float g, float b) {
      setColor(r, g, b);
      dr = r;
      dg = g;
      db = b;
   }

   public void setRBGColorF(float r, float g, float b, float r2, float g2, float b2) {
      setColor(r, g, b);
      dr = r2;
      dg = g2;
      db = b2;
   }

   public void setLoop(boolean loop) {
      this.loop = loop;
   }

   public void setRotationSpeed(float rot) {
      rotationSpeed = (float) (rot * 0.017453292519943);
   }

   public void setRotationSpeed(float start, float rot) {
      roll = oRoll = (float) (start * Math.PI * 2.0);
      rotationSpeed = (float) (rot * 0.017453292519943);
   }

   public void setMaxAge(int max) {
      lifetime = max;
   }

   public void setParticles(int start, int num, int inc) {
      numParticles = num;
      particleInc = inc;
      startParticle = start;
      textureIndex = start;
   }

   public void setParticle(int start) {
      setParticles(start, 1, 1);
   }

   public void setScale(float... scale) {
      particleScale = scale[0];
      scaleKeys = scale;
   }

   public void setAlphaF(float... a) {
      alpha = a[0];
      alphaKeys = a;
   }

   public void setSlowDown(double slowDown) {
      this.slowDown = slowDown;
   }

   public void setRandomMovementScale(float x, float y, float z) {
      randomX = x;
      randomY = y;
      randomZ = z;
   }

   public void setFinalFrames(int... frames) {
      finalFrames = frames;
   }

   public void setAngles(float yaw, float pitch) {
      angleYaw = yaw;
      anglePitch = pitch;
      angled = true;
   }

   public void setGravity(float g) {
      gravity = g;
   }

   public void setGridSize(int gridSize) {
      this.gridSize = gridSize;
   }

   public void setNoClip(boolean collide) {
      hasPhysics = collide;
   }

   public void setFlipped(boolean flip) {
      flipped = flip;
   }
}
