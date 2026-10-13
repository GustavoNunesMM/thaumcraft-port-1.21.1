package thaumcraft.client.fx;

import java.awt.Color;
import java.util.Random;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleLava.Factory;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.client.FMLClientHandler;
import thaumcraft.api.items.ItemsTC;
import thaumcraft.client.fx.beams.FXArc;
import thaumcraft.client.fx.beams.FXBeamBore;
import thaumcraft.client.fx.beams.FXBeamWand;
import thaumcraft.client.fx.beams.FXBolt;
import thaumcraft.client.fx.other.FXBlockWard;
import thaumcraft.client.fx.other.FXBoreStream;
import thaumcraft.client.fx.other.FXEssentiaStream;
import thaumcraft.client.fx.other.FXShieldRunes;
import thaumcraft.client.fx.other.FXVoidStream;
import thaumcraft.client.fx.particles.FXBlockRunes;
import thaumcraft.client.fx.particles.FXBoreParticles;
import thaumcraft.client.fx.particles.FXBoreSparkle;
import thaumcraft.client.fx.particles.FXBreakingFade;
import thaumcraft.client.fx.particles.FXFireMote;
import thaumcraft.client.fx.particles.FXGeneric;
import thaumcraft.client.fx.particles.FXGenericGui;
import thaumcraft.client.fx.particles.FXGenericP2E;
import thaumcraft.client.fx.particles.FXPlane;
import thaumcraft.client.fx.particles.FXSmokeSpiral;
import thaumcraft.client.fx.particles.FXSwarm;
import thaumcraft.client.fx.particles.FXVent;
import thaumcraft.client.fx.particles.FXVent2;
import thaumcraft.client.fx.particles.FXVisSparkle;
import thaumcraft.client.fx.particles.FXWispEG;
import thaumcraft.common.lib.SoundsTC;
import thaumcraft.common.tiles.crafting.TileCrucible;

public class FXDispatcher {
   public static FXDispatcher INSTANCE = new FXDispatcher();
   static int q = 0;

   public World getWorld() {
      return FMLClientHandler.instance().getClient().world;
   }

   public void drawFireMote(float x, float y, float z, float vx, float vy, float vz, float r, float g, float b, float alpha, float scale) {
      boolean bb = this.getWorld().rand.nextBoolean();
      FXFireMote glow = new FXFireMote(this.getWorld(), x, y, z, vx, vy, vz, r, g, b, bb ? scale / 3.0F : scale, bb ? 1 : 0);
      glow.setAlphaF(alpha);
      ParticleEngine.addEffect(this.getWorld(), glow);
   }

   public void drawAlumentum(float x, float y, float z, float vx, float vy, float vz, float r, float g, float b, float alpha, float scale) {
      FXFireMote glow = new FXFireMote(this.getWorld(), x, y, z, vx, vy, vz, r, g, b, scale, 1);
      glow.setAlphaF(alpha);
      ParticleEngine.addEffect(this.getWorld(), glow);
   }

   public void drawTaintParticles(float x, float y, float z, float vx, float vy, float vz, float scale) {
      FXGeneric fb = new FXGeneric(this.getWorld(), x, y, z, vx, vy, vz);
      fb.setMaxAge(80 + this.getWorld().rand.nextInt(20));
      fb.setRBGColorF(
         0.4F + this.getWorld().rand.nextFloat() * 0.2F,
         0.1F + this.getWorld().rand.nextFloat() * 0.3F,
         0.5F + this.getWorld().rand.nextFloat() * 0.2F
      );
      fb.setAlphaF(0.75F, 0.0F);
      fb.setGridSize(16);
      fb.setParticles(57 + this.getWorld().rand.nextInt(3), 1, 1);
      fb.setScale(scale, scale / 4.0F);
      fb.setLayer(1);
      fb.setSlowDown(0.975F);
      fb.setGravity(0.2F);
      fb.setRotationSpeed(this.getWorld().rand.nextFloat(), this.getWorld().rand.nextBoolean() ? -1.0F : 1.0F);
      ParticleEngine.addEffect(this.getWorld(), fb);
   }

   public void drawLightningFlash(double x, double y, double z, float r, float g, float b, float alpha, float scale) {
      FXGeneric fb = new FXGeneric(this.getWorld(), x, y, z, 0.0, 0.0, 0.0);
      fb.setMaxAge(5 + this.getWorld().rand.nextInt(5));
      fb.setGridSize(16);
      fb.setRBGColorF(r, g, b);
      fb.setAlphaF(alpha, 0.0F);
      fb.setParticles(108 + this.getWorld().rand.nextInt(4), 1, 1);
      fb.setScale(scale);
      fb.setLayer(0);
      fb.setRotationSpeed(this.getWorld().rand.nextFloat(), 0.0F);
      ParticleEngine.addEffect(this.getWorld(), fb);
   }

   public void drawGenericParticles(double x, double y, double z, double mx, double my, double mz, FXDispatcher.GenPart part) {
      FXGeneric fb = new FXGeneric(this.getWorld(), x, y, z, mx, my, mz);
      fb.setMaxAge(part.age);
      fb.setRBGColorF(part.redStart, part.greenStart, part.blueStart, part.redEnd, part.greenEnd, part.blueEnd);
      fb.setAlphaF(part.alpha);
      fb.setLoop(part.loop);
      fb.setParticles(part.partStart, part.partNum, part.partInc);
      fb.setScale(part.scale);
      fb.setLayer(part.layer);
      fb.setRotationSpeed(part.rotstart, part.rot);
      fb.setSlowDown(part.slowDown);
      fb.setGravity(part.grav);
      fb.setGridSize(part.grid);
      ParticleEngine.addEffectWithDelay(this.getWorld(), fb, part.delay);
   }

   public void drawGenericParticles(
      double x,
      double y,
      double z,
      double x2,
      double y2,
      double z2,
      float r,
      float g,
      float b,
      float alpha,
      boolean loop,
      int start,
      int num,
      int inc,
      int age,
      int delay,
      float scale,
      float rot,
      int layer
   ) {
      FXGeneric fb = new FXGeneric(this.getWorld(), x, y, z, x2, y2, z2);
      fb.setMaxAge(age);
      fb.setRBGColorF(r, g, b);
      fb.setAlphaF(alpha);
      fb.setLoop(loop);
      fb.setParticles(start, num, inc);
      fb.setScale(scale);
      fb.setLayer(layer);
      fb.setRotationSpeed(rot);
      ParticleEngine.addEffectWithDelay(this.getWorld(), fb, delay);
   }

   public void drawGenericParticles16(
      double x,
      double y,
      double z,
      double x2,
      double y2,
      double z2,
      float r,
      float g,
      float b,
      float alpha,
      boolean loop,
      int start,
      int num,
      int inc,
      int age,
      int delay,
      float scale,
      float rot,
      int layer
   ) {
      FXGeneric fb = new FXGeneric(this.getWorld(), x, y, z, x2, y2, z2);
      fb.setGridSize(16);
      fb.setMaxAge(age);
      fb.setRBGColorF(r, g, b);
      fb.setAlphaF(alpha);
      fb.setLoop(loop);
      fb.setParticles(start, num, inc);
      fb.setScale(scale);
      fb.setLayer(layer);
      fb.setRotationSpeed(rot);
      ParticleEngine.addEffectWithDelay(this.getWorld(), fb, delay);
   }

   public void drawLevitatorParticles(double x, double y, double z, double x2, double y2, double z2) {
      FXGeneric fb = new FXGeneric(this.getWorld(), x, y, z, x2, y2, z2);
      fb.setMaxAge(200 + this.getWorld().rand.nextInt(100));
      fb.setRBGColorF(0.5F, 0.5F, 0.2F);
      fb.setAlphaF(0.3F, 0.0F);
      fb.setGridSize(16);
      fb.setParticles(56, 1, 1);
      fb.setScale(2.0F, 5.0F);
      fb.setLayer(0);
      fb.setSlowDown(1.0);
      fb.setRotationSpeed(this.getWorld().rand.nextFloat(), this.getWorld().rand.nextBoolean() ? -1.0F : 1.0F);
      ParticleEngine.addEffect(this.getWorld(), fb);
   }

   public void drawStabilizerParticles(double x, double y, double z, double x2, double y2, double z2, int life) {
      FXGeneric fb = new FXGeneric(this.getWorld(), x, y, z, x2, y2, z2);
      fb.setMaxAge(life + this.getWorld().rand.nextInt(life));
      fb.setRBGColorF(0.5F, 0.2F, 0.5F);
      fb.setAlphaF(0.3F, 0.0F);
      fb.setGridSize(16);
      fb.setParticles(72 + this.getWorld().rand.nextInt(4), 1, 1);
      fb.setScale(1.0F, 10.0F);
      fb.setLayer(0);
      fb.setSlowDown(1.01);
      fb.setRotationSpeed(this.getWorld().rand.nextFloat(), this.getWorld().rand.nextBoolean() ? -1.0F : 1.0F);
      ParticleEngine.addEffect(this.getWorld(), fb);
   }

   public void drawGolemFlyParticles(double x, double y, double z, double x2, double y2, double z2) {
      try {
         FXGeneric fb = new FXGeneric(this.getWorld(), x, y, z, x2, y2, z2);
         fb.setMaxAge(20 + this.getWorld().rand.nextInt(5));
         fb.setAlphaF(0.3F, 0.0F);
         fb.setGridSize(16);
         fb.setParticles(56, 1, 1);
         fb.setScale(1.5F, 3.0F, 8.0F);
         fb.setLayer(0);
         fb.setSlowDown(1.0);
         fb.setWind(0.001);
         fb.setRotationSpeed(this.getWorld().rand.nextFloat(), this.getWorld().rand.nextBoolean() ? -1.0F : 1.0F);
         ParticleEngine.addEffect(this.getWorld(), fb);
      } catch (Exception var14) {
      }
   }

   public void drawPollutionParticles(BlockPos p) {
      float x = p.getX() + 0.2F + this.getWorld().rand.nextFloat() * 0.6F;
      float y = p.getY() + 0.2F + this.getWorld().rand.nextFloat() * 0.6F;
      float z = p.getZ() + 0.2F + this.getWorld().rand.nextFloat() * 0.6F;
      FXGeneric fb = new FXGeneric(
         this.getWorld(),
         x,
         y,
         z,
         (this.getWorld().rand.nextFloat() - this.getWorld().rand.nextFloat()) * 0.005,
         0.02,
         (this.getWorld().rand.nextFloat() - this.getWorld().rand.nextFloat()) * 0.005
      );
      fb.setMaxAge(100 + this.getWorld().rand.nextInt(60));
      fb.setRBGColorF(1.0F, 0.3F, 0.9F);
      fb.setAlphaF(0.5F, 0.0F);
      fb.setGridSize(16);
      fb.setParticles(56, 1, 1);
      fb.setScale(2.0F, 5.0F);
      fb.setLayer(1);
      fb.setSlowDown(1.0);
      fb.setWind(0.001);
      fb.setRotationSpeed(this.getWorld().rand.nextFloat(), this.getWorld().rand.nextBoolean() ? -1.0F : 1.0F);
      ParticleEngine.addEffect(this.getWorld(), fb);
   }

   public void drawBlockSparkles(BlockPos p, Vec3d start) {
      AxisAlignedBB bs = this.getWorld().getBlockState(p).getBoundingBox(this.getWorld(), p);
      bs.grow(0.1, 0.1, 0.1);
      int num = (int)(bs.getAverageEdgeLength() * 20.0);

      for (EnumFacing face : EnumFacing.values()) {
         IBlockState state = this.getWorld().getBlockState(p.offset(face));
         if (!state.isOpaqueCube() && !state.isSideSolid(this.getWorld(), p.offset(face), face.getOpposite())) {
            boolean rx = face.getXOffset() == 0;
            boolean ry = face.getYOffset() == 0;
            boolean rz = face.getZOffset() == 0;
            double mx = 0.5 + face.getXOffset() * 0.51;
            double my = 0.5 + face.getYOffset() * 0.51;
            double mz = 0.5 + face.getZOffset() * 0.51;

            for (int a = 0; a < num * 2; a++) {
               double x = mx;
               double y = my;
               double z = mz;
               if (rx) {
                  x = mx + this.getWorld().rand.nextGaussian() * 0.6;
               }

               if (ry) {
                  y = my + this.getWorld().rand.nextGaussian() * 0.6;
               }

               if (rz) {
                  z = mz + this.getWorld().rand.nextGaussian() * 0.6;
               }

               x = MathHelper.clamp(x, bs.minX, bs.maxX);
               y = MathHelper.clamp(y, bs.minY, bs.maxY);
               z = MathHelper.clamp(z, bs.minZ, bs.maxZ);
               float r = MathHelper.getInt(this.getWorld().rand, 255, 255) / 255.0F;
               float g = MathHelper.getInt(this.getWorld().rand, 189, 255) / 255.0F;
               float b = MathHelper.getInt(this.getWorld().rand, 64, 255) / 255.0F;
               Vec3d v1 = new Vec3d(p.getX() + x, p.getY() + y, p.getZ() + z);
               double delay = this.getWorld().rand.nextInt(5) + v1.distanceTo(start) * 16.0;
               this.drawSimpleSparkle(
                  this.getWorld().rand,
                  p.getX() + x,
                  p.getY() + y,
                  p.getZ() + z,
                  0.0,
                  0.0025,
                  0.0,
                  0.4F + (float)this.getWorld().rand.nextGaussian() * 0.1F,
                  r,
                  g,
                  b,
                  (int)delay,
                  1.0F,
                  0.01F,
                  16
               );
            }
         }
      }
   }

   public void drawLineSparkle(
      Random rand,
      double x,
      double y,
      double z,
      double x2,
      double y2,
      double z2,
      float scale,
      float r,
      float g,
      float b,
      int delay,
      float decay,
      float grav,
      int baseAge
   ) {
      boolean sp = rand.nextFloat() < 0.2;
      FXGeneric fb = new FXGeneric(this.getWorld(), x, y, z, x2, y2, z2);
      int age = baseAge * 4 + this.getWorld().rand.nextInt(baseAge);
      fb.setMaxAge(age);
      fb.setRBGColorF(r, g, b);
      fb.setAlphaF(0.0F, 1.0F, 0.0F);
      fb.setParticles(sp ? 320 : 512, 16, 1);
      fb.setLoop(true);
      fb.setGravity(grav);
      fb.setScale(scale, scale * 2.0F, scale);
      fb.setLayer(0);
      fb.setSlowDown(decay);
      fb.setRandomMovementScale(5.0E-5F, 0.0F, 5.0E-5F);
      ParticleEngine.addEffectWithDelay(this.getWorld(), fb, delay);
   }

   public void drawSimpleSparkle(
      Random rand,
      double x,
      double y,
      double z,
      double x2,
      double y2,
      double z2,
      float scale,
      float r,
      float g,
      float b,
      int delay,
      float decay,
      float grav,
      int baseAge
   ) {
      boolean sp = rand.nextFloat() < 0.2;
      FXGeneric fb = new FXGeneric(this.getWorld(), x, y, z, x2, y2, z2);
      int age = baseAge * 4 + this.getWorld().rand.nextInt(baseAge);
      fb.setMaxAge(age);
      fb.setRBGColorF(r, g, b);
      float[] alphas = new float[6 + rand.nextInt(age / 3)];

      for (int a = 1; a < alphas.length - 1; a++) {
         alphas[a] = rand.nextFloat();
      }

      fb.setAlphaF(alphas);
      fb.setParticles(sp ? 320 : 512, 16, 1);
      fb.setLoop(true);
      fb.setGravity(grav);
      fb.setScale(scale, scale * 2.0F);
      fb.setLayer(0);
      fb.setSlowDown(decay);
      fb.setRandomMovementScale(5.0E-4F, 0.001F, 5.0E-4F);
      fb.setWind(5.0E-4);
      ParticleEngine.addEffectWithDelay(this.getWorld(), fb, delay);
   }

   public void drawSimpleSparkleGui(
      Random rand, double x, double y, double x2, double y2, float scale, float r, float g, float b, int delay, float decay, float grav
   ) {
      boolean sp = rand.nextFloat() < 0.2;
      FXGenericGui fb = new FXGenericGui(this.getWorld(), x, y, 0.0, x2, y2, 0.0);
      fb.setMaxAge(32 + this.getWorld().rand.nextInt(8));
      fb.setRBGColorF(r, g, b);
      fb.setAlphaF(new float[]{0.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 0.0F});
      fb.setParticles(sp ? 320 : 512, 16, 1);
      fb.setLoop(true);
      fb.setGravity(grav);
      fb.setScale(new float[]{scale, scale * 2.0F});
      fb.setNoClip(false);
      fb.setLayer(4);
      fb.setSlowDown(decay);
      fb.setRandomMovementScale(0.025F, 0.025F, 0.0F);
      ParticleEngine.addEffectWithDelay(this.getWorld(), fb, delay);
   }

   public void drawBlockMistParticles(BlockPos p, int c) {
      AxisAlignedBB bs = this.getWorld().getBlockState(p).getBoundingBox(this.getWorld(), p);
      Color color = new Color(c);

      for (int a = 0; a < 8; a++) {
         double x = p.getX() + bs.minX + this.getWorld().rand.nextFloat() * (bs.maxX - bs.minX);
         double y = p.getY() + bs.minY + this.getWorld().rand.nextFloat() * (bs.maxY - bs.minY);
         double z = p.getZ() + bs.minZ + this.getWorld().rand.nextFloat() * (bs.maxZ - bs.minZ);
         FXGeneric fb = new FXGeneric(
            this.getWorld(),
            x,
            y,
            z,
            this.getWorld().rand.nextGaussian() * 0.01,
            this.getWorld().rand.nextFloat() * 0.075,
            this.getWorld().rand.nextGaussian() * 0.01
         );
         fb.setMaxAge(50 + this.getWorld().rand.nextInt(25));
         fb.setRBGColorF(color.getRed() / 255.0F, color.getGreen() / 255.0F, color.getBlue() / 255.0F);
         fb.setAlphaF(0.0F, 0.5F, 0.4F, 0.3F, 0.2F, 0.1F, 0.0F);
         fb.setGridSize(16);
         fb.setParticles(56, 1, 1);
         fb.setScale(5.0F, 1.0F);
         fb.setLayer(0);
         fb.setSlowDown(1.0);
         fb.setGravity(0.1F);
         fb.setWind(0.001);
         fb.setRotationSpeed(this.getWorld().rand.nextFloat(), this.getWorld().rand.nextBoolean() ? -1.0F : 1.0F);
         ParticleEngine.addEffect(this.getWorld(), fb);
      }
   }

   public void drawFocusCloudParticle(double x, double y, double z, double mx, double my, double mz, int c) {
      Color color = new Color(c);
      FXGeneric fb = new FXGeneric(this.getWorld(), x, y, z, mx, my, mz);
      fb.setMaxAge(20 + this.getWorld().rand.nextInt(10));
      fb.setRBGColorF(color.getRed() / 255.0F, color.getGreen() / 255.0F, color.getBlue() / 255.0F);
      fb.setAlphaF(0.0F, 0.66F, 0.0F);
      fb.setGridSize(16);
      fb.setParticles(56 + this.getWorld().rand.nextInt(4), 1, 1);
      fb.setScale(5.0F + this.getWorld().rand.nextFloat(), 10.0F + this.getWorld().rand.nextFloat());
      fb.setLayer(0);
      fb.setSlowDown(0.99);
      fb.setWind(0.001);
      fb.setRotationSpeed(this.getWorld().rand.nextFloat(), this.getWorld().rand.nextBoolean() ? -0.25F : 0.25F);
      ParticleEngine.addEffect(this.getWorld(), fb);
   }

   public void drawWispyMotesOnBlock(BlockPos pp, int age, float grav) {
      this.drawWispyMotes(
         pp.getX() + this.getWorld().rand.nextFloat(),
         pp.getY(),
         pp.getZ() + this.getWorld().rand.nextFloat(),
         0.0,
         0.0,
         0.0,
         age,
         0.4F + this.getWorld().rand.nextFloat() * 0.6F,
         0.6F + this.getWorld().rand.nextFloat() * 0.4F,
         0.6F + this.getWorld().rand.nextFloat() * 0.4F,
         grav
      );
   }

   public void drawWispyMotes(double d, double e, double f, double vx, double vy, double vz, int age, float grav) {
      this.drawWispyMotes(
         d,
         e,
         f,
         vx,
         vy,
         vz,
         age,
         0.25F + this.getWorld().rand.nextFloat() * 0.75F,
         0.25F + this.getWorld().rand.nextFloat() * 0.75F,
         0.25F + this.getWorld().rand.nextFloat() * 0.75F,
         grav
      );
   }

   public void drawWispyMotes(double d, double e, double f, double vx, double vy, double vz, int age, float r, float g, float b, float grav) {
      FXGeneric fb = new FXGeneric(this.getWorld(), d, e, f, vx, vy, vz);
      fb.setMaxAge((int)(age + age / 2 * this.getWorld().rand.nextFloat()));
      fb.setRBGColorF(r, g, b);
      fb.setAlphaF(0.0F, 0.6F, 0.6F, 0.0F);
      fb.setGridSize(64);
      fb.setParticles(512, 16, 1);
      fb.setScale(1.0F, 0.5F);
      fb.setLoop(true);
      fb.setWind(0.001);
      fb.setGravity(grav);
      fb.setRandomMovementScale(0.0025F, 0.0F, 0.0025F);
      ParticleEngine.addEffect(this.getWorld(), fb);
   }

   public void drawBlockMistParticlesFlat(BlockPos p, int c) {
      Block bs = this.getWorld().getBlockState(p).getBlock();
      Color color = new Color(c);

      for (int a = 0; a < 6; a++) {
         double x = p.getX() + this.getWorld().rand.nextFloat();
         double y = p.getY() + this.getWorld().rand.nextFloat() * 0.125F;
         double z = p.getZ() + this.getWorld().rand.nextFloat();
         FXGeneric fb = new FXGeneric(
            this.getWorld(),
            x,
            y,
            z,
            (this.getWorld().rand.nextFloat() - this.getWorld().rand.nextFloat()) * 0.005,
            0.005,
            (this.getWorld().rand.nextFloat() - this.getWorld().rand.nextFloat()) * 0.005
         );
         fb.setMaxAge(400 + this.getWorld().rand.nextInt(100));
         fb.setRBGColorF(color.getRed() / 255.0F, color.getGreen() / 255.0F, color.getBlue() / 255.0F);
         fb.setAlphaF(1.0F, 0.0F);
         fb.setGridSize(8);
         fb.setParticles(24, 1, 1);
         fb.setScale(2.0F, 5.0F);
         fb.setLayer(0);
         fb.setSlowDown(1.0);
         fb.setWind(0.001);
         fb.setRotationSpeed(this.getWorld().rand.nextFloat(), this.getWorld().rand.nextBoolean() ? -1.0F : 1.0F);
         ParticleEngine.addEffect(this.getWorld(), fb);
      }
   }

   public void crucibleBubble(float x, float y, float z, float cr, float cg, float cb) {
      FXGeneric fb = new FXGeneric(this.getWorld(), x, y, z, 0.0, 0.0, 0.0);
      fb.setMaxAge(15 + this.getWorld().rand.nextInt(10));
      fb.setScale(this.getWorld().rand.nextFloat() * 0.3F + 0.3F);
      fb.setRBGColorF(cr, cg, cb);
      fb.setRandomMovementScale(0.002F, 0.002F, 0.002F);
      fb.setGravity(-0.001F);
      fb.setParticle(64);
      fb.setFinalFrames(65, 66, 66);
      ParticleEngine.addEffect(this.getWorld(), fb);
   }

   public void crucibleBoil(BlockPos pos, TileCrucible tile, int j) {
      for (int a = 0; a < 2; a++) {
         FXGeneric fb = new FXGeneric(
            this.getWorld(),
            pos.getX() + 0.2F + this.getWorld().rand.nextFloat() * 0.6F,
            pos.getY() + 0.1F + tile.getFluidHeight(),
            pos.getZ() + 0.2F + this.getWorld().rand.nextFloat() * 0.6F,
            0.0,
            0.002,
            0.0
         );
         fb.setMaxAge((int)(7.0 + 8.0 / (Math.random() * 0.8 + 0.2)));
         fb.setScale(this.getWorld().rand.nextFloat() * 0.3F + 0.2F);
         if (tile.aspects.size() == 0) {
            fb.setRBGColorF(1.0F, 1.0F, 1.0F);
         } else {
            Color color = new Color(tile.aspects.getAspects()[this.getWorld().rand.nextInt(tile.aspects.getAspects().length)].getColor());
            fb.setRBGColorF(color.getRed() / 255.0F, color.getGreen() / 255.0F, color.getBlue() / 255.0F);
         }

         fb.setRandomMovementScale(0.001F, 0.001F, 0.001F);
         fb.setGravity(-0.025F * j);
         fb.setParticle(64);
         fb.setFinalFrames(65, 66);
         ParticleEngine.addEffect(this.getWorld(), fb);
      }
   }

   public void crucibleFroth(float x, float y, float z) {
      FXGeneric fb = new FXGeneric(this.getWorld(), x, y, z, 0.0, 0.0, 0.0);
      fb.setMaxAge(4 + this.getWorld().rand.nextInt(3));
      fb.setScale(this.getWorld().rand.nextFloat() * 0.2F + 0.2F);
      fb.setRBGColorF(0.5F, 0.5F, 0.7F);
      fb.setRandomMovementScale(0.001F, 0.001F, 0.001F);
      fb.setGravity(0.1F);
      fb.setParticle(64);
      fb.setFinalFrames(65, 66);
      ParticleEngine.addEffect(this.getWorld(), fb);
   }

   public void crucibleFrothDown(float x, float y, float z) {
      FXGeneric fb = new FXGeneric(this.getWorld(), x, y, z, 0.0, 0.0, 0.0);
      fb.setMaxAge(12 + this.getWorld().rand.nextInt(12));
      fb.setScale(this.getWorld().rand.nextFloat() * 0.2F + 0.4F);
      fb.setRBGColorF(0.25F, 0.0F, 0.75F);
      fb.setAlphaF(0.8F);
      fb.setRandomMovementScale(0.001F, 0.001F, 0.001F);
      fb.setGravity(0.05F);
      fb.setNoClip(false);
      fb.setParticle(73);
      fb.setFinalFrames(65, 66);
      fb.setLayer(1);
      ParticleEngine.addEffect(this.getWorld(), fb);
   }

   public void drawBamf(BlockPos p, boolean sound, boolean flair, EnumFacing side) {
      this.drawBamf(p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, sound, flair, side);
   }

   public void drawPedestalShield(BlockPos pos) {
      FXShieldRunes fb = new FXShieldRunes(this.getWorld(), pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, null, 8, 0.0F, 90.0F);
      FMLClientHandler.instance().getClient().effectRenderer.addEffect(fb);
   }

   public void drawBamf(BlockPos p, float r, float g, float b, boolean sound, boolean flair, EnumFacing side) {
      this.drawBamf(p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, r, g, b, sound, flair, side);
   }

   public void drawBamf(BlockPos p, int color, boolean sound, boolean flair, EnumFacing side) {
      this.drawBamf(p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, color, sound, flair, side);
   }

   public void drawBamf(double x, double y, double z, int color, boolean sound, boolean flair, EnumFacing side) {
      Color c = new Color(color);
      float r = c.getRed() / 255.0F;
      float g = c.getGreen() / 255.0F;
      float b = c.getBlue() / 255.0F;
      this.drawBamf(x, y, z, r, g, b, sound, flair, side);
   }

   public void drawBamf(double x, double y, double z, boolean sound, boolean flair, EnumFacing side) {
      this.drawBamf(x, y, z, 0.5F, 0.1F, 0.6F, sound, flair, side);
   }

   public void drawBamf(double x, double y, double z, float r, float g, float b, boolean sound, boolean flair, EnumFacing side) {
      if (sound) {
         this.getWorld()
            .playSound(x, y, z, SoundsTC.poof, SoundCategory.BLOCKS, 0.4F, 1.0F + (float)this.getWorld().rand.nextGaussian() * 0.05F, false);
      }

      for (int a = 0; a < 6 + this.getWorld().rand.nextInt(3) + 2; a++) {
         double vx = (0.05F + this.getWorld().rand.nextFloat() * 0.05F) * (this.getWorld().rand.nextBoolean() ? -1 : 1);
         double vy = (0.05F + this.getWorld().rand.nextFloat() * 0.05F) * (this.getWorld().rand.nextBoolean() ? -1 : 1);
         double vz = (0.05F + this.getWorld().rand.nextFloat() * 0.05F) * (this.getWorld().rand.nextBoolean() ? -1 : 1);
         if (side != null) {
            vx += side.getXOffset() * 0.1F;
            vy += side.getYOffset() * 0.1F;
            vz += side.getZOffset() * 0.1F;
         }

         FXGeneric fb2 = new FXGeneric(this.getWorld(), x + vx * 2.0, y + vy * 2.0, z + vz * 2.0, vx / 2.0, vy / 2.0, vz / 2.0);
         fb2.setMaxAge(20 + this.getWorld().rand.nextInt(15));
         fb2.setRBGColorF(
            MathHelper.clamp(r * (1.0F + (float)this.getWorld().rand.nextGaussian() * 0.1F), 0.0F, 1.0F),
            MathHelper.clamp(g * (1.0F + (float)this.getWorld().rand.nextGaussian() * 0.1F), 0.0F, 1.0F),
            MathHelper.clamp(b * (1.0F + (float)this.getWorld().rand.nextGaussian() * 0.1F), 0.0F, 1.0F)
         );
         fb2.setAlphaF(1.0F, 0.1F);
         fb2.setGridSize(16);
         fb2.setParticles(123, 5, 1);
         fb2.setScale(3.0F, 4.0F + this.getWorld().rand.nextFloat() * 3.0F);
         fb2.setLayer(1);
         fb2.setSlowDown(0.7);
         fb2.setRotationSpeed(this.getWorld().rand.nextFloat(), this.getWorld().rand.nextBoolean() ? -1.0F : 1.0F);
         ParticleEngine.addEffect(this.getWorld(), fb2);
      }

      if (flair) {
         for (int a = 0; a < 2 + this.getWorld().rand.nextInt(3); a++) {
            double vx = (0.025F + this.getWorld().rand.nextFloat() * 0.025F) * (this.getWorld().rand.nextBoolean() ? -1 : 1);
            double vy = (0.025F + this.getWorld().rand.nextFloat() * 0.025F) * (this.getWorld().rand.nextBoolean() ? -1 : 1);
            double vz = (0.025F + this.getWorld().rand.nextFloat() * 0.025F) * (this.getWorld().rand.nextBoolean() ? -1 : 1);
            this.drawWispyMotes(x + vx * 2.0, y + vy * 2.0, z + vz * 2.0, vx, vy, vz, 15 + this.getWorld().rand.nextInt(10), -0.01F);
         }

         FXGeneric fb = new FXGeneric(this.getWorld(), x, y, z, 0.0, 0.0, 0.0);
         fb.setMaxAge(10 + this.getWorld().rand.nextInt(5));
         fb.setRBGColorF(1.0F, 0.9F, 1.0F);
         fb.setAlphaF(1.0F, 0.0F);
         fb.setGridSize(16);
         fb.setParticles(77, 1, 1);
         fb.setScale(10.0F + this.getWorld().rand.nextFloat() * 2.0F, 0.0F);
         fb.setLayer(0);
         fb.setRotationSpeed(this.getWorld().rand.nextFloat(), (float)this.getWorld().rand.nextGaussian());
         ParticleEngine.addEffect(this.getWorld(), fb);
      }

      for (int a = 0; a < (flair ? 2 : 0) + this.getWorld().rand.nextInt(3); a++) {
         this.drawCurlyWisp(
            x,
            y,
            z,
            0.0,
            0.0,
            0.0,
            1.0F,
            (0.9F + this.getWorld().rand.nextFloat() * 0.1F + r) / 2.0F,
            (0.1F + g) / 2.0F,
            (0.5F + this.getWorld().rand.nextFloat() * 0.1F + b) / 2.0F,
            0.75F,
            side,
            a,
            0,
            0
         );
      }
   }

   public void drawCurlyWisp(
      double x,
      double y,
      double z,
      double vx,
      double vy,
      double vz,
      float scale,
      float r,
      float g,
      float b,
      float a,
      EnumFacing side,
      int seed,
      int layer,
      int delay
   ) {
      if (this.getWorld() != null) {
         vx += (0.0025F + this.getWorld().rand.nextFloat() * 0.005F) * (this.getWorld().rand.nextBoolean() ? -1 : 1);
         vy += (0.0025F + this.getWorld().rand.nextFloat() * 0.005F) * (this.getWorld().rand.nextBoolean() ? -1 : 1);
         vz += (0.0025F + this.getWorld().rand.nextFloat() * 0.005F) * (this.getWorld().rand.nextBoolean() ? -1 : 1);
         if (side != null) {
            vx += side.getXOffset() * 0.025F;
            vy += side.getYOffset() * 0.025F;
            vz += side.getZOffset() * 0.025F;
         }

         FXGeneric fb2 = new FXGeneric(this.getWorld(), x + vx * 5.0, y + vy * 5.0, z + vz * 5.0, vx, vy, vz);
         if (seed > 0 && this.getWorld().rand.nextBoolean()) {
            fb2.setAngles(90.0F * (float)this.getWorld().rand.nextGaussian(), 90.0F * (float)this.getWorld().rand.nextGaussian());
         }

         fb2.setMaxAge(25 + this.getWorld().rand.nextInt(20 + 20 * seed));
         fb2.setRBGColorF(r, g, b, 0.1F, 0.0F, 0.1F);
         fb2.setAlphaF(a, 0.0F);
         fb2.setGridSize(16);
         fb2.setParticles(60 + this.getWorld().rand.nextInt(4), 1, 1);
         fb2.setScale(5.0F * scale, (10.0F + this.getWorld().rand.nextFloat() * 4.0F) * scale);
         fb2.setLayer(layer);
         fb2.setRotationSpeed(
            this.getWorld().rand.nextFloat(),
            this.getWorld().rand.nextBoolean()
               ? -2.0F - this.getWorld().rand.nextFloat() * 2.0F
               : 2.0F + this.getWorld().rand.nextFloat() * 2.0F
         );
         ParticleEngine.addEffectWithDelay(this.getWorld(), fb2, delay);
      }
   }

   public void pechsCurseTick(double posX, double posY, double posZ) {
      FXGeneric fb2 = new FXGeneric(this.getWorld(), posX, posY, posZ, 0.0, 0.0, 0.0);
      fb2.setAngles(90.0F * (float)this.getWorld().rand.nextGaussian(), 90.0F * (float)this.getWorld().rand.nextGaussian());
      fb2.setMaxAge(50 + this.getWorld().rand.nextInt(50));
      fb2.setRBGColorF(0.9F, 0.1F, 0.5F, 0.1F + this.getWorld().rand.nextFloat() * 0.1F, 0.0F, 0.5F + this.getWorld().rand.nextFloat() * 0.1F);
      fb2.setAlphaF(0.75F, 0.0F);
      fb2.setGridSize(8);
      fb2.setParticles(28 + this.getWorld().rand.nextInt(4), 1, 1);
      fb2.setScale(3.0F, 5.0F + this.getWorld().rand.nextFloat() * 2.0F);
      fb2.setLayer(0);
      fb2.setRotationSpeed(
         this.getWorld().rand.nextFloat(),
         this.getWorld().rand.nextBoolean()
            ? -3.0F - this.getWorld().rand.nextFloat() * 3.0F
            : 3.0F + this.getWorld().rand.nextFloat() * 3.0F
      );
      ParticleEngine.addEffect(this.getWorld(), fb2);
      this.drawWispyMotes(posX, posY, posZ, 0.0, 0.0, 0.0, 10 + this.getWorld().rand.nextInt(10), -0.01F);
   }

   public void scanHighlight(BlockPos p) {
      AxisAlignedBB bb = this.getWorld().getBlockState(p).getBoundingBox(this.getWorld(), p);
      bb = bb.offset(p);
      this.scanHighlight(bb);
   }

   public void scanHighlight(Entity e) {
      AxisAlignedBB bb = e.getEntityBoundingBox();
      this.scanHighlight(bb);
   }

   public void scanHighlight(AxisAlignedBB bb) {
      int num = MathHelper.ceil(bb.getAverageEdgeLength() * 2.0);
      double ax = (bb.minX + bb.maxX) / 2.0;
      double ay = (bb.minY + bb.maxY) / 2.0;
      double az = (bb.minZ + bb.maxZ) / 2.0;

      for (EnumFacing face : EnumFacing.values()) {
         double mx = 0.5 + face.getXOffset() * 0.51;
         double my = 0.5 + face.getYOffset() * 0.51;
         double mz = 0.5 + face.getZOffset() * 0.51;

         for (int a = 0; a < num * 2; a++) {
            double x = mx + this.getWorld().rand.nextGaussian() * (bb.maxX - bb.minX);
            double y = my + this.getWorld().rand.nextGaussian() * (bb.maxY - bb.minY);
            double z = mz + this.getWorld().rand.nextGaussian() * (bb.maxZ - bb.minZ);
            x = MathHelper.clamp(x, bb.minX - ax, bb.maxX - ax);
            y = MathHelper.clamp(y, bb.minY - ay, bb.maxY - ay);
            z = MathHelper.clamp(z, bb.minZ - az, bb.maxZ - az);
            float r = MathHelper.getInt(this.getWorld().rand, 16, 32) / 255.0F;
            float g = MathHelper.getInt(this.getWorld().rand, 132, 165) / 255.0F;
            float b = MathHelper.getInt(this.getWorld().rand, 223, 239) / 255.0F;
            this.drawSimpleSparkle(
               this.getWorld().rand,
               ax + x,
               ay + y,
               az + z,
               0.0,
               0.0,
               0.0,
               0.4F + (float)this.getWorld().rand.nextGaussian() * 0.1F,
               r,
               g,
               b,
               this.getWorld().rand.nextInt(10),
               1.0F,
               0.0F,
               4
            );
         }
      }
   }

   public void sparkle(float x, float y, float z, float r, float g, float b) {
      if (this.getWorld().rand.nextInt(6) < 4) {
         this.drawGenericParticles(
            x,
            y,
            z,
            0.0,
            0.0,
            0.0,
            r,
            g,
            b,
            0.9F,
            true,
            320,
            16,
            1,
            6 + this.getWorld().rand.nextInt(4),
            0,
            0.6F + this.getWorld().rand.nextFloat() * 0.2F,
            0.0F,
            0
         );
      }
   }

   public void visSparkle(int x, int y, int z, int x2, int y2, int z2, int color) {
      FXVisSparkle fb = new FXVisSparkle(
         this.getWorld(),
         x + this.getWorld().rand.nextFloat(),
         y + this.getWorld().rand.nextFloat(),
         z + this.getWorld().rand.nextFloat(),
         x2 + 0.4 + this.getWorld().rand.nextFloat() * 0.2F,
         y2 + 0.4 + this.getWorld().rand.nextFloat() * 0.2F,
         z2 + 0.4 + this.getWorld().rand.nextFloat() * 0.2F
      );
      Color c = new Color(color);
      fb.setRBGColorF(c.getRed() / 255.0F, c.getGreen() / 255.0F, c.getBlue() / 255.0F);
      ParticleEngine.addEffect(this.getWorld(), fb);
   }

   public void voidStreak(double x, double y, double z, double x2, double y2, double z2, int seed, float scale) {
      FXVoidStream fb = new FXVoidStream(this.getWorld(), x, y, z, x2, y2, z2, seed, scale);
      ParticleEngine.addEffect(this.getWorld(), fb);
   }

   public void splooshFX(Entity e) {
      float f = this.getWorld().rand.nextFloat() * (float) Math.PI * 2.0F;
      float f1 = this.getWorld().rand.nextFloat() * 0.5F + 0.5F;
      float f2 = MathHelper.sin(f) * 2.0F * 0.5F * f1;
      float f3 = MathHelper.cos(f) * 2.0F * 0.5F * f1;
      FXBreakingFade fx = new FXBreakingFade(
         this.getWorld(),
         e.posX + f2,
         e.posY + this.getWorld().rand.nextFloat() * e.height,
         e.posZ + f3,
         Items.SLIME_BALL,
         0
      );
      if (this.getWorld().rand.nextBoolean()) {
         fx.setRBGColorF(0.6F, 0.0F, 0.3F);
         fx.setAlphaF(0.4F);
      } else {
         fx.setRBGColorF(0.3F, 0.0F, 0.3F);
         fx.setAlphaF(0.6F);
      }

      fx.setParticleMaxAge((int)(66.0F / (this.getWorld().rand.nextFloat() * 0.9F + 0.1F)));
      FMLClientHandler.instance().getClient().effectRenderer.addEffect(fx);
   }

   public void taintsplosionFX(Entity e) {
      FXBreakingFade fx = new FXBreakingFade(
         this.getWorld(),
         e.posX,
         e.posY + this.getWorld().rand.nextFloat() * e.height,
         e.posZ,
         Items.SLIME_BALL
      );
      if (this.getWorld().rand.nextBoolean()) {
         fx.setRBGColorF(0.6F, 0.0F, 0.3F);
         fx.setAlphaF(0.4F);
      } else {
         fx.setRBGColorF(0.3F, 0.0F, 0.3F);
         fx.setAlphaF(0.6F);
      }

      fx.setSpeed(Math.random() * 2.0 - 1.0, Math.random() * 2.0 - 1.0, Math.random() * 2.0 - 1.0);
      fx.boom();
      fx.setParticleMaxAge((int)(66.0F / (this.getWorld().rand.nextFloat() * 0.9F + 0.1F)));
      FMLClientHandler.instance().getClient().effectRenderer.addEffect(fx);
   }

   public void tentacleAriseFX(Entity e) {
      for (int j = 0; j < 2.0F * e.height; j++) {
         float f = this.getWorld().rand.nextFloat() * (float) Math.PI * e.height;
         float f1 = this.getWorld().rand.nextFloat() * 0.5F + 0.5F;
         float f2 = MathHelper.sin(f) * e.height * 0.25F * f1;
         float f3 = MathHelper.cos(f) * e.height * 0.25F * f1;
         FXBreakingFade fx = new FXBreakingFade(this.getWorld(), e.posX + f2, e.posY, e.posZ + f3, Items.SLIME_BALL);
         fx.setRBGColorF(0.4F, 0.0F, 0.4F);
         fx.setAlphaF(0.5F);
         fx.setParticleMaxAge((int)(66.0F / (this.getWorld().rand.nextFloat() * 0.9F + 0.1F)));
         FMLClientHandler.instance().getClient().effectRenderer.addEffect(fx);
         if (!this.getWorld().isAirBlock(e.getPosition().down())) {
            f = this.getWorld().rand.nextFloat() * (float) Math.PI * e.height;
            f1 = this.getWorld().rand.nextFloat() * 0.5F + 0.5F;
            f2 = MathHelper.sin(f) * e.height * 0.25F * f1;
            f3 = MathHelper.cos(f) * e.height * 0.25F * f1;
            this.getWorld()
               .spawnParticle(
                  EnumParticleTypes.BLOCK_CRACK,
                  e.posX + f2,
                  e.posY,
                  e.posZ + f3,
                  0.0,
                  0.0,
                  0.0,
                  new int[]{Block.getStateId(this.getWorld().getBlockState(e.getPosition().down()))}
               );
         }
      }
   }

   public void slimeJumpFX(Entity e, int i) {
      float f = this.getWorld().rand.nextFloat() * (float) Math.PI * 2.0F;
      float f1 = this.getWorld().rand.nextFloat() * 0.5F + 0.5F;
      float f2 = MathHelper.sin(f) * i * 0.5F * f1;
      float f3 = MathHelper.cos(f) * i * 0.5F * f1;
      FXBreakingFade fx = new FXBreakingFade(
         this.getWorld(),
         e.posX + f2,
         (e.getEntityBoundingBox().minY + e.getEntityBoundingBox().maxY) / 2.0,
         e.posZ + f3,
         Items.SLIME_BALL,
         0
      );
      fx.setRBGColorF(0.7F, 0.0F, 1.0F);
      fx.setAlphaF(0.4F);
      fx.setParticleMaxAge((int)(66.0F / (this.getWorld().rand.nextFloat() * 0.9F + 0.1F)));
      FMLClientHandler.instance().getClient().effectRenderer.addEffect(fx);
   }

   public void taintLandFX(Entity e) {
      float f = this.getWorld().rand.nextFloat() * (float) Math.PI * 2.0F;
      float f1 = this.getWorld().rand.nextFloat() * 0.5F + 0.5F;
      float f2 = MathHelper.sin(f) * 2.0F * 0.5F * f1;
      float f3 = MathHelper.cos(f) * 2.0F * 0.5F * f1;
      if (this.getWorld().isRemote) {
         FXBreakingFade fx = new FXBreakingFade(
            this.getWorld(),
            e.posX + f2,
            (e.getEntityBoundingBox().minY + e.getEntityBoundingBox().maxY) / 2.0,
            e.posZ + f3,
            Items.SLIME_BALL
         );
         fx.setRBGColorF(0.1F, 0.0F, 0.1F);
         fx.setAlphaF(0.4F);
         fx.setParticleMaxAge((int)(66.0F / (this.getWorld().rand.nextFloat() * 0.9F + 0.1F)));
         FMLClientHandler.instance().getClient().effectRenderer.addEffect(fx);
      }
   }

   public void drawInfusionParticles1(double x, double y, double z, BlockPos pos, ItemStack stack) {
      FXBoreParticles fb = new FXBoreParticles(
            this.getWorld(),
            x,
            y,
            z,
            pos.getX() + 0.5,
            pos.getY() - 0.5,
            pos.getZ() + 0.5,
            (float)this.getWorld().rand.nextGaussian() * 0.03F,
            (float)this.getWorld().rand.nextGaussian() * 0.03F,
            (float)this.getWorld().rand.nextGaussian() * 0.03F,
            stack
         )
         .getObjectColor(pos);
      fb.setAlphaF(0.3F);
      FMLClientHandler.instance().getClient().effectRenderer.addEffect(fb);
   }

   public void drawInfusionParticles2(double x, double y, double z, BlockPos pos, IBlockState id, int md) {
      FXBoreParticles fb = new FXBoreParticles(
            this.getWorld(), x, y, z, pos.getX() + 0.5, pos.getY() - 0.5, pos.getZ() + 0.5, id, md
         )
         .getObjectColor(pos);
      fb.setAlphaF(0.3F);
      FMLClientHandler.instance().getClient().effectRenderer.addEffect(fb);
   }

   public void drawInfusionParticles3(double x, double y, double z, int x2, int y2, int z2) {
      FXBoreSparkle fb = new FXBoreSparkle(this.getWorld(), x, y, z, x2 + 0.5, y2 - 0.5, z2 + 0.5);
      fb.setRBGColorF(0.4F + this.getWorld().rand.nextFloat() * 0.2F, 0.2F, 0.6F + this.getWorld().rand.nextFloat() * 0.3F);
      ParticleEngine.addEffect(this.getWorld(), fb);
   }

   public void drawInfusionParticles4(double x, double y, double z, int x2, int y2, int z2) {
      FXBoreSparkle fb = new FXBoreSparkle(this.getWorld(), x, y, z, x2 + 0.5, y2 - 0.5, z2 + 0.5);
      fb.setRBGColorF(0.2F, 0.6F + this.getWorld().rand.nextFloat() * 0.3F, 0.3F);
      ParticleEngine.addEffect(this.getWorld(), fb);
   }

   public void drawVentParticles(double x, double y, double z, double x2, double y2, double z2, int color) {
      FXVent fb = new FXVent(this.getWorld(), x, y, z, x2, y2, z2, color);
      fb.setAlphaF(0.4F);
      ParticleEngine.addEffect(this.getWorld(), fb);
   }

   public void drawVentParticles(double x, double y, double z, double x2, double y2, double z2, int color, float scale) {
      FXVent fb = new FXVent(this.getWorld(), x, y, z, x2, y2, z2, color);
      fb.setAlphaF(0.4F);
      fb.setScale(scale);
      ParticleEngine.addEffect(this.getWorld(), fb);
   }

   public void drawVentParticles2(double x, double y, double z, double x2, double y2, double z2, int color, float scale) {
      FXVent2 fb = new FXVent2(this.getWorld(), x, y, z, x2, y2, z2, color);
      fb.setAlphaF(0.4F);
      fb.setScale(scale);
      ParticleEngine.addEffect(this.getWorld(), fb);
      if (this.getWorld().rand.nextInt(6) < 2) {
         this.drawGenericParticles(
            x,
            y,
            z,
            x2 / 2.0,
            y2 / 2.0,
            z2 / 2.0,
            1.0F,
            0.7F,
            0.2F,
            0.9F,
            true,
            320,
            16,
            1,
            10 + this.getWorld().rand.nextInt(4),
            0,
            0.25F + this.getWorld().rand.nextFloat() * 0.1F,
            0.0F,
            0
         );
      }
   }

   public void spark(double d, double e, double f, float size, float r, float g, float b, float a) {
      FXGeneric fb = new FXGeneric(this.getWorld(), d, e, f, 0.0, 0.0, 0.0);
      fb.setMaxAge(5 + this.getWorld().rand.nextInt(5));
      fb.setAlphaF(a);
      fb.setRBGColorF(r, g, b);
      fb.setGridSize(16);
      fb.setParticles(8 + this.getWorld().rand.nextInt(3) * 16, 8, 1);
      fb.setScale(size);
      fb.setFlipped(this.getWorld().rand.nextBoolean());
      ParticleEngine.addEffect(this.getWorld(), fb);
   }

   public void smokeSpiral(double x, double y, double z, float rad, int start, int miny, int color) {
      FXSmokeSpiral fx = new FXSmokeSpiral(this.getWorld(), x, y, z, rad, start, miny);
      Color c = new Color(color);
      fx.setRBGColorF(c.getRed() / 255.0F, c.getGreen() / 255.0F, c.getBlue() / 255.0F);
      ParticleEngine.addEffect(this.getWorld(), fx);
   }

   public void wispFXEG(double posX, double posY, double posZ, Entity target) {
      for (int a = 0; a < 2; a++) {
         FXWispEG ef = new FXWispEG(this.getWorld(), posX, posY, posZ, target);
         ParticleEngine.addEffect(this.getWorld(), ef);
      }
   }

   public void burst(double sx, double sy, double sz, float size) {
      FXGeneric fb = new FXGeneric(this.getWorld(), sx, sy, sz, 0.0, 0.0, 0.0);
      fb.setMaxAge(31);
      fb.setGridSize(16);
      fb.setParticles(208, 31, 1);
      fb.setScale(size);
      ParticleEngine.addEffect(this.getWorld(), fb);
   }

   public void excavateFX(BlockPos pos, EntityLivingBase p, int progress) {
      RenderGlobal rg = Minecraft.getMinecraft().renderGlobal;
      rg.sendBlockBreakProgress(p.getEntityId(), pos, progress);
   }

   public Object beamCont(EntityLivingBase p, double tx, double ty, double tz, int type, int color, boolean reverse, float endmod, Object input, int impact) {
      FXBeamWand beamcon = null;
      Color c = new Color(color);
      if (input instanceof FXBeamWand) {
         beamcon = (FXBeamWand)input;
      }

      if (beamcon != null && beamcon.isAlive()) {
         beamcon.updateBeam(tx, ty, tz);
         beamcon.setEndMod(endmod);
         beamcon.impact = impact;
      } else {
         beamcon = new FXBeamWand(this.getWorld(), p, tx, ty, tz, c.getRed() / 255.0F, c.getGreen() / 255.0F, c.getBlue() / 255.0F, 8);
         beamcon.setType(type);
         beamcon.setEndMod(endmod);
         beamcon.setReverse(reverse);
         FMLClientHandler.instance().getClient().effectRenderer.addEffect(beamcon);
      }

      return beamcon;
   }

   public Object beamBore(
      double px, double py, double pz, double tx, double ty, double tz, int type, int color, boolean reverse, float endmod, Object input, int impact
   ) {
      FXBeamBore beamcon = null;
      Color c = new Color(color);
      if (input instanceof FXBeamBore) {
         beamcon = (FXBeamBore)input;
      }

      if (beamcon != null && beamcon.isAlive()) {
         beamcon.updateBeam(px, py, pz, tx, ty, tz);
         beamcon.setEndMod(endmod);
         beamcon.impact = impact;
      } else {
         beamcon = new FXBeamBore(this.getWorld(), px, py, pz, tx, ty, tz, c.getRed() / 255.0F, c.getGreen() / 255.0F, c.getBlue() / 255.0F, 8);
         beamcon.setType(type);
         beamcon.setEndMod(endmod);
         beamcon.setReverse(reverse);
         FMLClientHandler.instance().getClient().effectRenderer.addEffect(beamcon);
      }

      return beamcon;
   }

   public void boreDigFx(int x, int y, int z, Entity e, IBlockState bi, int md, int delay) {
      float p = 50.0F;

      for (int a = 0; a < p / delay; a++) {
         if (this.getWorld().rand.nextInt(4) == 0) {
            FXBoreSparkle fb = new FXBoreSparkle(
               this.getWorld(),
               x + this.getWorld().rand.nextFloat(),
               y + this.getWorld().rand.nextFloat(),
               z + this.getWorld().rand.nextFloat(),
               e
            );
            ParticleEngine.addEffect(this.getWorld(), fb);
         } else {
            FXBoreParticles fb = new FXBoreParticles(
               this.getWorld(),
               x + this.getWorld().rand.nextFloat(),
               y + this.getWorld().rand.nextFloat(),
               z + this.getWorld().rand.nextFloat(),
               e.posX,
               e.posY,
               e.posZ,
               bi,
               md
            );
            fb.setTarget(e);
            FMLClientHandler.instance().getClient().effectRenderer.addEffect(fb);
         }
      }
   }

   public void essentiaTrailFx(BlockPos p1, BlockPos p2, int count, int color, float scale, int ext) {
      FXEssentiaStream fb = new FXEssentiaStream(
         this.getWorld(),
         p1.getX() + 0.5,
         p1.getY() + 0.5,
         p1.getZ() + 0.5,
         p2.getX() + 0.5,
         p2.getY() + 0.5,
         p2.getZ() + 0.5,
         count,
         color,
         scale,
         ext,
         0.0
      );
      ParticleEngine.addEffect(this.getWorld(), fb);
   }

   public void boreTrailFx(BlockPos p1, Entity e, int count, int color, float scale, int ext) {
      FXBoreStream fb = new FXBoreStream(
         this.getWorld(), p1.getX() + 0.5, p1.getY() + 0.5, p1.getZ() + 0.5, e, count, color, scale, ext, 0.0
      );
      ParticleEngine.addEffect(this.getWorld(), fb);
   }

   public void essentiaDropFx(double x, double y, double z, float r, float g, float b, float alpha) {
      FXGeneric fb = new FXGeneric(
         this.getWorld(),
         x,
         y,
         z,
         this.getWorld().rand.nextGaussian() * 0.005F,
         this.getWorld().rand.nextGaussian() * 0.005F,
         this.getWorld().rand.nextGaussian() * 0.005F
      );
      fb.setMaxAge(20 + this.getWorld().rand.nextInt(10));
      fb.setRBGColorF(r, g, b);
      fb.setAlphaF(alpha);
      fb.setLoop(false);
      fb.setParticles(25, 1, 1);
      fb.setScale(0.4F + this.getWorld().rand.nextFloat() * 0.2F, 0.2F);
      fb.setLayer(1);
      fb.setGravity(0.01F);
      fb.setRotationSpeed(0.0F);
      ParticleEngine.addEffect(this.getWorld(), fb);
   }

   public void jarSplashFx(double x, double y, double z) {
      FXGeneric fb = new FXGeneric(
         this.getWorld(),
         x + this.getWorld().rand.nextGaussian() * 0.075F,
         y,
         z + this.getWorld().rand.nextGaussian() * 0.075F,
         this.getWorld().rand.nextGaussian() * 0.015F,
         0.075F + this.getWorld().rand.nextFloat() * 0.05F,
         this.getWorld().rand.nextGaussian() * 0.015F
      );
      fb.setMaxAge(20 + this.getWorld().rand.nextInt(10));
      Color c = new Color(2650102);
      fb.setRBGColorF(c.getRed() / 255.0F, c.getGreen() / 255.0F, c.getBlue() / 255.0F);
      fb.setAlphaF(0.5F);
      fb.setLoop(false);
      fb.setParticles(73, 1, 1);
      fb.setScale(0.4F + this.getWorld().rand.nextFloat() * 0.3F, 0.0F);
      fb.setLayer(1);
      fb.setGravity(0.3F);
      fb.setRotationSpeed(0.0F);
      ParticleEngine.addEffect(this.getWorld(), fb);
   }

   public void waterTrailFx(BlockPos p1, BlockPos p2, int count, int color, float scale) {
      FXEssentiaStream fb = new FXEssentiaStream(
         this.getWorld(),
         p1.getX() + 0.5,
         p1.getY() + 0.66,
         p1.getZ() + 0.5,
         p2.getX() + 0.5,
         p2.getY() + 0.5,
         p2.getZ() + 0.5,
         count,
         color,
         scale,
         0,
         0.2
      );
      ParticleEngine.addEffect(this.getWorld(), fb);
   }

   public void furnaceLavaFx(int x, int y, int z, int facingX, int facingZ) {
      float qx = facingX == 0
         ? (this.getWorld().rand.nextFloat() - this.getWorld().rand.nextFloat()) * 0.5F
         : facingX * this.getWorld().rand.nextFloat();
      float qz = facingZ == 0
         ? (this.getWorld().rand.nextFloat() - this.getWorld().rand.nextFloat()) * 0.5F
         : facingZ * this.getWorld().rand.nextFloat();
      Particle fb = new Factory()
         .createParticle(
            0,
            this.getWorld(),
            x + 0.5F + (this.getWorld().rand.nextFloat() - this.getWorld().rand.nextFloat()) * 0.3F + facingX * 1.0F,
            y + 0.3F,
            z + 0.5F + (this.getWorld().rand.nextFloat() - this.getWorld().rand.nextFloat()) * 0.3F + facingZ * 1.0F,
            0.15F * qx,
            0.2F * this.getWorld().rand.nextFloat(),
            0.15F * qz,
            new int[0]
         );
      FMLClientHandler.instance().getClient().effectRenderer.addEffect(fb);
   }

   public void blockRunes(double x, double y, double z, float r, float g, float b, int dur, float grav) {
      FXBlockRunes fb = new FXBlockRunes(this.getWorld(), x + 0.5, y + 0.5, z + 0.5, r, g, b, dur);
      fb.setGravity(grav);
      ParticleEngine.addEffect(this.getWorld(), fb);
   }

   public void blockRunes2(double x, double y, double z, float r, float g, float b, int dur, float grav) {
      FXBlockRunes fb = new FXBlockRunes(this.getWorld(), x + 0.5, y + 0.5, z + 0.5, r, g, b, dur);
      fb.setGravity(grav);
      fb.setScale((float)(0.5 + this.getWorld().rand.nextGaussian() * 0.1F));
      fb.setOffsetX(0.0);
      ParticleEngine.addEffect(this.getWorld(), fb);
   }

   public void drawSlash(double x, double y, double z, double x2, double y2, double z2, int dur) {
      FXPlane fb = new FXPlane(this.getWorld(), x, y, z, x2, y2, z2, dur);
      ParticleEngine.addEffect(this.getWorld(), fb);
   }

   public void blockWard(double x, double y, double z, EnumFacing side, float f, float f1, float f2) {
      FXBlockWard fb = new FXBlockWard(this.getWorld(), x + 0.5, y + 0.5, z + 0.5, side, f, f1, f2);
      FMLClientHandler.instance().getClient().effectRenderer.addEffect(fb);
   }

   public Object swarmParticleFX(Entity targetedEntity, float f1, float f2, float pg) {
      FXSwarm fx = new FXSwarm(
         this.getWorld(),
         targetedEntity.posX + (this.getWorld().rand.nextFloat() - this.getWorld().rand.nextFloat()) * 2.0F,
         targetedEntity.posY + (this.getWorld().rand.nextFloat() - this.getWorld().rand.nextFloat()) * 2.0F,
         targetedEntity.posZ + (this.getWorld().rand.nextFloat() - this.getWorld().rand.nextFloat()) * 2.0F,
         targetedEntity,
         0.8F + this.getWorld().rand.nextFloat() * 0.2F,
         this.getWorld().rand.nextFloat() * 0.4F,
         1.0F - this.getWorld().rand.nextFloat() * 0.2F,
         f1,
         f2,
         pg
      );
      ParticleEngine.addEffect(this.getWorld(), fx);
      return fx;
   }

   public void bottleTaintBreak(double x, double y, double z) {
      for (int k1 = 0; k1 < 8; k1++) {
         this.getWorld()
            .spawnParticle(
               EnumParticleTypes.ITEM_CRACK,
               x,
               y,
               z,
               this.getWorld().rand.nextGaussian() * 0.15,
               this.getWorld().rand.nextDouble() * 0.2,
               this.getWorld().rand.nextGaussian() * 0.15,
               new int[]{Item.getIdFromItem(ItemsTC.bottleTaint)}
            );
      }

      this.getWorld()
         .playSound(x, y, z, SoundEvents.ENTITY_SPLASH_POTION_BREAK, SoundCategory.NEUTRAL, 1.0F, this.getWorld().rand.nextFloat() * 0.1F + 0.9F, false);
   }

   public void arcLightning(double x, double y, double z, double tx, double ty, double tz, float r, float g, float b, float h) {
      if (h <= 0.0F) {
         h = 0.1F;
      }

      FXArc efa = new FXArc(this.getWorld(), x, y, z, tx, ty, tz, r, g, b, h);
      FMLClientHandler.instance().getClient().effectRenderer.addEffect(efa);
   }

   public void arcBolt(double x, double y, double z, double tx, double ty, double tz, float r, float g, float b, float width) {
      FXBolt efa = new FXBolt(this.getWorld(), x, y, z, tx, ty, tz, r, g, b, width);
      FMLClientHandler.instance().getClient().effectRenderer.addEffect(efa);
   }

   public void cultistSpawn(double x, double y, double z, double a, double b, double c) {
      FXGeneric fb = new FXGeneric(this.getWorld(), x, y, z, a, b, c);
      fb.setMaxAge(10 + this.getWorld().rand.nextInt(10));
      fb.setRBGColorF(1.0F, 1.0F, 1.0F, 0.6F, 0.0F, 0.0F);
      fb.setAlphaF(0.8F);
      fb.setGridSize(16);
      fb.setParticles(160, 6, 1);
      fb.setScale(3.0F + this.getWorld().rand.nextFloat() * 2.0F);
      fb.setLayer(1);
      ParticleEngine.addEffect(this.getWorld(), fb);
   }

   public void drawWispyMotesEntity(double x, double y, double z, Entity e, float r, float g, float b) {
      FXGenericP2E fb = new FXGenericP2E(this.getWorld(), x, y, z, e);
      fb.setRBGColorF(r, g, b);
      fb.setAlphaF(0.6F);
      fb.setParticles(512, 16, 1);
      fb.setLoop(true);
      fb.setWind(0.001);
      fb.setRandomMovementScale(0.0025F, 0.0F, 0.0025F);
      ParticleEngine.addEffect(this.getWorld(), fb);
   }

   public void drawWispParticles(double x, double y, double z, double x2, double y2, double z2, int color, int a) {
      FXGeneric fb = new FXGeneric(this.getWorld(), x, y, z, x2, y2, z2);
      fb.setMaxAge(10 + this.getWorld().rand.nextInt(5));
      Color c = new Color(color);
      fb.setRBGColorF(c.getRed() / 255.0F, c.getGreen() / 255.0F, c.getBlue() / 255.0F);
      fb.setAlphaF(0.5F);
      fb.setLoop(true);
      fb.setGridSize(64);
      fb.setParticles(264, 8, 1);
      fb.setScale(1.0F + this.getWorld().rand.nextFloat() * 0.25F, 0.05F);
      fb.setWind(2.5E-4);
      fb.setRandomMovementScale(0.0025F, 0.0F, 0.0025F);
      ParticleEngine.addEffectWithDelay(this.getWorld(), fb, a);
   }

   public void drawNitorCore(double x, double y, double z, double x2, double y2, double z2) {
      FXGeneric fb = new FXGeneric(this.getWorld(), x, y, z, x2, y2, z2);
      fb.setMaxAge(10);
      fb.setRBGColorF(1.0F, 1.0F, 1.0F);
      fb.setAlphaF(1.0F);
      fb.setParticles(457, 1, 1);
      fb.setScale(1.0F, 1.0F + (float)this.getWorld().rand.nextGaussian() * 0.1F, 1.0F);
      fb.setLayer(1);
      fb.setRandomMovementScale(2.0E-4F, 2.0E-4F, 2.0E-4F);
      ParticleEngine.addEffect(this.getWorld(), fb);
   }

   public void drawNitorFlames(double x, double y, double z, double x2, double y2, double z2, int color, int a) {
      FXGeneric fb = new FXGeneric(this.getWorld(), x, y, z, x2, y2, z2);
      fb.setMaxAge(10 + this.getWorld().rand.nextInt(5));
      Color c = new Color(color);
      fb.setRBGColorF(c.getRed() / 255.0F, c.getGreen() / 255.0F, c.getBlue() / 255.0F);
      fb.setAlphaF(0.66F);
      fb.setLoop(true);
      fb.setGridSize(64);
      fb.setParticles(264, 8, 1);
      fb.setScale(3.0F + this.getWorld().rand.nextFloat(), 0.05F);
      fb.setRandomMovementScale(0.0025F, 0.0F, 0.0025F);
      ParticleEngine.addEffectWithDelay(this.getWorld(), fb, a);
   }

   public static class GenPart {
      public int grid = 64;
      public int age = 0;
      public float redStart = 1.0F;
      public float greenStart = 1.0F;
      public float blueStart = 1.0F;
      public float redEnd = 1.0F;
      public float greenEnd = 1.0F;
      public float blueEnd = 1.0F;
      public float[] alpha = new float[]{1.0F};
      public float[] scale = new float[]{1.0F};
      public float rot;
      public float rotstart = 0.0F;
      public boolean loop = false;
      public int partStart = 0;
      public int partNum = 1;
      public int partInc = 1;
      public int layer = 0;
      public double slowDown = 0.98F;
      public float grav = 0.0F;
      public int delay = 0;
   }
}
