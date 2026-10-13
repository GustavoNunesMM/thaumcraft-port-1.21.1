package thaumcraft.client.fx.other;

import com.sasmaster.glelwjgl.java.CoreGLE;
import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import org.lwjgl.opengl.GL11;
import thaumcraft.client.fx.FXDispatcher;
import thaumcraft.client.fx.ParticleEngine;
import thaumcraft.codechicken.lib.vec.Quat;
import thaumcraft.common.blocks.essentia.BlockEssentiaTransport;
import thaumcraft.common.lib.utils.BlockStateUtils;

public class FXEssentiaStream extends Particle {
   private double targetX;
   private double targetY;
   private double targetZ;
   private double startX;
   private double startY;
   private double startZ;
   private int count = 0;
   public int length = 20;
   private String key = "";
   private BlockPos startPos = null;
   private BlockPos endPos = null;
   static HashMap<String, FXEssentiaStream> pt = new HashMap<>();
   CoreGLE gle = new CoreGLE();
   private static final ResourceLocation TEX0 = new ResourceLocation("thaumcraft", "textures/misc/essentia.png");
   int layer = 1;
   double[][] points;
   float[][] colours;
   double[] radii;
   int growing = -1;
   ArrayList<Quat> vecs = new ArrayList<>();

   public FXEssentiaStream(
      World w, double par2, double par4, double par6, double tx, double ty, double tz, int count, int color, float scale, int extend, double my
   ) {
      super(w, par2, par4, par6, 0.0, 0.0, 0.0);
      this.particleScale = (float)(scale * (1.0 + this.rand.nextGaussian() * 0.15F));
      this.length = Math.max(20, extend);
      this.count = count;
      this.targetX = tx;
      this.targetY = ty;
      this.targetZ = tz;
      BlockPos bp1 = new BlockPos(this.posX, this.posY, this.posZ);
      BlockPos bp2 = new BlockPos(this.targetX, this.targetY, this.targetZ);
      IBlockState bs = w.getBlockState(bp1);
      if (bs.getBlock() instanceof BlockEssentiaTransport) {
         EnumFacing f = BlockStateUtils.getFacing(bs);
         this.posX = this.posX + f.getXOffset() * 0.05F;
         this.posY = this.posY + f.getYOffset() * 0.05F;
         this.posZ = this.posZ + f.getZOffset() * 0.05F;
      }

      double dx = tx - this.posX;
      double dy = ty - this.posY;
      double dz = tz - this.posZ;
      int base = (int)(MathHelper.sqrt(dx * dx + dy * dy + dz * dz) * 21.0F);
      if (base < 1) {
         base = 1;
      }

      this.particleMaxAge = base;
      String k = bp1.toLong() + "" + bp2.toLong() + "" + color;
      if (pt.containsKey(k)) {
         FXEssentiaStream trail2 = pt.get(k);
         if (!trail2.isExpired && trail2.vecs.size() < trail2.length) {
            trail2.length = trail2.length + Math.max(extend, 5);
            trail2.particleMaxAge = trail2.particleMaxAge + Math.max(extend, 5);
            this.particleMaxAge = 0;
         }
      }

      if (this.particleMaxAge > 0) {
         pt.put(k, this);
         this.key = k;
      }

      this.motionX = MathHelper.sin(count / 4.0F) * 0.015F;
      this.motionY = my + MathHelper.sin(count / 3.0F) * 0.015F;
      this.motionZ = MathHelper.sin(count / 2.0F) * 0.015F;
      Color c = new Color(color);
      this.particleRed = c.getRed() / 255.0F;
      this.particleGreen = c.getGreen() / 255.0F;
      this.particleBlue = c.getBlue() / 255.0F;
      this.particleGravity = 0.2F;
      this.vecs.add(new Quat(0.0, 0.0, 0.0, 0.001));
      this.vecs.add(new Quat(0.0, 0.0, 0.0, 0.001));
      this.startX = this.posX;
      this.startY = this.posY;
      this.startZ = this.posZ;
      this.startPos = new BlockPos(this.startX, this.startY, this.startZ);
      this.endPos = bp2;
   }

   public void renderParticle(BufferBuilder wr, Entity entity, float f, float f1, float f2, float f3, float f4, float f5) {
      Tessellator.getInstance().draw();
      GL11.glPushMatrix();
      double ePX = this.startX - interpPosX;
      double ePY = this.startY - interpPosY;
      double ePZ = this.startZ - interpPosZ;
      GL11.glTranslated(ePX, ePY, ePZ);
      if (this.points != null && this.points.length > 2) {
         Minecraft.getMinecraft().renderEngine.bindTexture(TEX0);
         this.gle.set_POLYCYL_TESS(8);
         this.gle.set__ROUND_TESS_PIECES(1);
         this.gle.gleSetJoinStyle(1042);
         this.gle
            .glePolyCone(
               this.points.length, this.points, this.colours, this.radii, 0.075F, this.growing < 0 ? 0.0F : 0.075F * (this.particleAge - this.growing + f)
            );
      }

      GL11.glPopMatrix();
      Minecraft.getMinecraft().renderEngine.bindTexture(ParticleEngine.particleTexture);
      wr.begin(7, DefaultVertexFormats.PARTICLE_POSITION_TEX_COLOR_LMAP);
   }

   public void setFXLayer(int l) {
      this.layer = l;
   }

   public int getFXLayer() {
      return this.layer;
   }

   public void onUpdate() {
      this.prevPosX = this.posX;
      this.prevPosY = this.posY;
      this.prevPosZ = this.posZ;
      if (this.particleAge++ < this.particleMaxAge && this.length >= 1) {
         this.motionY = this.motionY + 0.01 * this.particleGravity;
         this.move(this.motionX, this.motionY, this.motionZ);
         this.motionX *= 0.985;
         this.motionY *= 0.985;
         this.motionZ *= 0.985;
         this.motionX = MathHelper.clamp((float)this.motionX, -0.05F, 0.05F);
         this.motionY = MathHelper.clamp((float)this.motionY, -0.05F, 0.05F);
         this.motionZ = MathHelper.clamp((float)this.motionZ, -0.05F, 0.05F);
         double dx = this.targetX - this.posX;
         double dy = this.targetY - this.posY;
         double dz = this.targetZ - this.posZ;
         double d13 = 0.01;
         double d11 = MathHelper.sqrt(dx * dx + dy * dy + dz * dz);
         dx /= d11;
         dy /= d11;
         dz /= d11;
         this.motionX = this.motionX + dx * (d13 / Math.min(1.0, d11));
         this.motionY = this.motionY + dy * (d13 / Math.min(1.0, d11));
         this.motionZ = this.motionZ + dz * (d13 / Math.min(1.0, d11));
         float scale = this.particleScale * (0.75F + MathHelper.sin((this.count + this.particleAge) / 2.0F) * 0.25F);
         if (d11 < 1.0) {
            float f = MathHelper.sin((float)(d11 * (Math.PI / 2)));
            scale *= f;
            this.particleScale *= f;
         }

         if (this.particleScale > 0.001) {
            this.vecs.add(new Quat(scale, this.posX - this.startX, this.posY - this.startY, this.posZ - this.startZ));
         } else {
            if (this.growing < 0) {
               this.growing = this.particleAge;
            }

            this.length--;
            FXDispatcher.INSTANCE
               .essentiaDropFx(
                  this.targetX + this.rand.nextGaussian() * 0.075F,
                  this.targetY + this.rand.nextGaussian() * 0.075F,
                  this.targetZ + this.rand.nextGaussian() * 0.075F,
                  this.particleRed,
                  this.particleGreen,
                  this.particleBlue,
                  0.5F
               );
         }

         if (this.vecs.size() > this.length) {
            this.vecs.remove(0);
         }

         this.points = new double[this.vecs.size()][3];
         this.colours = new float[this.vecs.size()][4];
         this.radii = new double[this.vecs.size()];
         int c = this.vecs.size();

         for (Quat v : this.vecs) {
            c--;
            float variance = 1.0F + MathHelper.sin((c + this.particleAge) / 3.0F) * 0.2F;
            float xx = MathHelper.sin((c + this.particleAge) / 6.0F) * 0.03F;
            float yy = MathHelper.sin((c + this.particleAge) / 7.0F) * 0.03F;
            float zz = MathHelper.sin((c + this.particleAge) / 8.0F) * 0.03F;
            this.points[c][0] = v.x + xx;
            this.points[c][1] = v.y + yy;
            this.points[c][2] = v.z + zz;
            this.radii[c] = v.s * variance;
            if (c > this.vecs.size() - 10) {
               this.radii[c] = this.radii[c] * MathHelper.cos((float)((c - (this.vecs.size() - 12)) / 10.0F * (Math.PI / 2)));
            }

            if (c == 0) {
               this.radii[c] = 0.0;
            } else if (c == 1) {
               this.radii[c] = 0.0;
            } else if (c == 2) {
               this.radii[c] = (this.particleScale * 0.5 + this.radii[c]) / 2.0;
            } else if (c == 3) {
               this.radii[c] = (this.particleScale + this.radii[c]) / 2.0;
            } else if (c == 4) {
               this.radii[c] = (this.particleScale + this.radii[c] * 2.0) / 3.0;
            }

            float v2 = 1.0F - MathHelper.sin((c + this.particleAge) / 2.0F) * 0.1F;
            this.colours[c][0] = this.particleRed * v2;
            this.colours[c][1] = this.particleGreen * v2;
            this.colours[c][2] = this.particleBlue * v2;
            this.colours[c][3] = 1.0F;
         }

         if (this.vecs.size() > 2 && this.rand.nextBoolean()) {
            int q = this.rand.nextInt(3);
            if (this.rand.nextBoolean()) {
               q = this.vecs.size() - 2;
            }

            FXDispatcher.INSTANCE
               .essentiaDropFx(
                  this.vecs.get(q).x + this.startX,
                  this.vecs.get(q).y + this.startY,
                  this.vecs.get(q).z + this.startZ,
                  this.particleRed,
                  this.particleGreen,
                  this.particleBlue,
                  0.5F
               );
         }
      } else {
         this.setExpired();
         if (pt.containsKey(this.key) && pt.get(this.key).isExpired) {
            pt.remove(this.key);
         }
      }
   }

   public void setGravity(float value) {
      this.particleGravity = value;
   }
}
