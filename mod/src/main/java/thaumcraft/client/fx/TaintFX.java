package thaumcraft.client.fx;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import thaumcraft.common.network.FluxFXPayload;
import thaumcraft.common.registers.ModFlux;

/**
 * The FXDispatcher effects of milestone 12 (flux and taint), on top of FXGeneric. FXBreakingFade (slime-ball crumbs)
 * becomes tinted goo motes; FXSwarm and FXVoidStream are small FXGeneric subclasses.
 */
public final class TaintFX {
   private TaintFX() {
   }

   private static ClientLevel level() {
      return Minecraft.getInstance().level;
   }

   private static void add(Particle fx) {
      Minecraft.getInstance().particleEngine.add(fx);
   }

   public static void handle(FluxFXPayload p) {
      if (p.kind() == 0) {
         for (int a = 0; a < Math.min(40, p.amount()); a++) pollution(p.pos());
      }
   }

   /** drawPollutionParticles. */
   public static void pollution(BlockPos p) {
      ClientLevel level = level();
      if (level == null) return;
      var r = level.random;
      FXGeneric fb = new FXGeneric(level, p.getX() + 0.2F + r.nextFloat() * 0.6F, p.getY() + 0.2F + r.nextFloat() * 0.6F,
         p.getZ() + 0.2F + r.nextFloat() * 0.6F, (r.nextFloat() - r.nextFloat()) * 0.005, 0.02, (r.nextFloat() - r.nextFloat()) * 0.005);
      fb.setMaxAge(100 + r.nextInt(60));
      fb.setRBGColorF(1.0F, 0.3F, 0.9F);
      fb.setAlphaF(0.5F, 0.0F);
      fb.setGridSize(16);
      fb.setParticles(56, 1, 1);
      fb.setScale(2.0F, 5.0F);
      fb.setLayer(1);
      fb.setSlowDown(1.0);
      fb.setWind(0.001);
      fb.setRotationSpeed(r.nextFloat(), r.nextBoolean() ? -1.0F : 1.0F);
      add(fb);
   }

   public static void taintParticles(double x, double y, double z, float vx, float vy, float vz, float scale) {
      ClientLevel level = level();
      var r = level.random;
      FXGeneric fb = new FXGeneric(level, x, y, z, vx, vy, vz);
      fb.setMaxAge(80 + r.nextInt(20));
      fb.setRBGColorF(0.4F + r.nextFloat() * 0.2F, 0.1F + r.nextFloat() * 0.3F, 0.5F + r.nextFloat() * 0.2F);
      fb.setAlphaF(0.75F, 0.0F);
      fb.setGridSize(16);
      fb.setParticles(57 + r.nextInt(3), 1, 1);
      fb.setScale(scale, scale / 4.0F);
      fb.setLayer(1);
      fb.setSlowDown(0.975F);
      fb.setGravity(0.2F);
      fb.setRotationSpeed(r.nextFloat(), r.nextBoolean() ? -1.0F : 1.0F);
      add(fb);
   }

   public static void lightningFlash(double x, double y, double z, float red, float green, float blue, float alpha, float scale) {
      ClientLevel level = level();
      FXGeneric fb = new FXGeneric(level, x, y, z, 0, 0, 0);
      fb.setMaxAge(5 + level.random.nextInt(5));
      fb.setGridSize(16);
      fb.setRBGColorF(red, green, blue);
      fb.setAlphaF(alpha, 0.0F);
      fb.setParticles(108 + level.random.nextInt(4), 1, 1);
      fb.setScale(scale);
      fb.setLayer(0);
      fb.setRotationSpeed(level.random.nextFloat(), 0.0F);
      add(fb);
   }

   /** drawCurlyWisp (no side, seed 1, layer 0). */
   public static void curlyWisp(double x, double y, double z, float scale, float red, float green, float blue, float alpha) {
      ClientLevel level = level();
      var r = level.random;
      double vx = (0.0025F + r.nextFloat() * 0.005F) * (r.nextBoolean() ? -1 : 1);
      double vy = (0.0025F + r.nextFloat() * 0.005F) * (r.nextBoolean() ? -1 : 1);
      double vz = (0.0025F + r.nextFloat() * 0.005F) * (r.nextBoolean() ? -1 : 1);
      FXGeneric fb = new FXGeneric(level, x + vx * 5.0, y + vy * 5.0, z + vz * 5.0, vx, vy, vz);
      if (r.nextBoolean()) fb.setAngles(90.0F * (float) r.nextGaussian(), 90.0F * (float) r.nextGaussian());
      fb.setMaxAge(25 + r.nextInt(40));
      fb.setRBGColorF(red, green, blue);
      fb.setAlphaF(0.0F, alpha, 0.0F);
      fb.setGridSize(16);
      fb.setParticles(72 + r.nextInt(4), 1, 1);
      fb.setScale(scale, scale * 2.0F);
      fb.setLayer(0);
      fb.setSlowDown(1.0);
      fb.setRotationSpeed(r.nextFloat(), r.nextBoolean() ? -1.0F : 1.0F);
      add(fb);
   }

   /** FXSpark (condenser lattice). */
   public static void spark(double x, double y, double z, float size, float red, float green, float blue, float alpha) {
      ClientLevel level = level();
      FXGeneric fb = new FXGeneric(level, x, y, z);
      fb.setMaxAge(5 + level.random.nextInt(5));
      fb.setGridSize(16);
      fb.setRBGColorF(red, green, blue);
      fb.setAlphaF(alpha, 0.0F);
      fb.setParticles(108 + level.random.nextInt(4), 1, 1);
      fb.setScale(size / 4.0F);
      fb.setLayer(0);
      add(fb);
   }

   /** FXSlimyBubble (liquid death). */
   public static void slimyBubble(double x, double y, double z, float size, float red, float green, float blue) {
      ClientLevel level = level();
      FXGeneric fb = new FXGeneric(level, x, y, z);
      fb.setMaxAge(15 + level.random.nextInt(10));
      fb.setRBGColorF(red, green, blue);
      fb.setAlphaF(0.8F);
      fb.setParticle(64);
      fb.setFinalFrames(65, 66);
      fb.setScale(size * 8.0F);
      fb.setLayer(1);
      add(fb);
   }

   /** BlockFluidPure.randomDisplayTick. */
   public static void pureMote(double x, double y, double z) {
      ClientLevel level = level();
      FXGeneric fb = new FXGeneric(level, x, y, z);
      fb.setMaxAge(10 + level.random.nextInt(10));
      fb.setScale(level.random.nextFloat() * 0.3F + 0.3F);
      fb.setRBGColorF(1.0F, 1.0F, 1.0F);
      fb.setRandomMovementScale(0.001F, 0.001F, 0.001F);
      fb.setGravity(-0.01F);
      fb.setAlphaF(0.25F);
      fb.setParticle(64);
      fb.setFinalFrames(65, 66);
      add(fb);
   }

   /** BlockFluxGoo.randomDisplayTick. */
   public static void gooBubble(double x, double y, double z) {
      ClientLevel level = level();
      FXGeneric fb = new FXGeneric(level, x, y, z);
      fb.setMaxAge(2 + level.random.nextInt(3));
      fb.setScale(level.random.nextFloat() * 0.3F + 0.2F);
      fb.setRBGColorF(1.0F, 0.0F, 0.5F);
      fb.setRandomMovementScale(0.001F, 0.001F, 0.001F);
      fb.setGravity(-0.01F);
      fb.setAlphaF(0.25F);
      fb.setParticle(64);
      fb.setFinalFrames(65, 66);
      add(fb);
   }

   private static void goo(double x, double y, double z, double vx, double vy, double vz, float red, float green, float blue, float alpha) {
      ClientLevel level = level();
      FXGeneric fb = new FXGeneric(level, x, y, z, vx, vy, vz);
      fb.setMaxAge((int) (66.0F / (level.random.nextFloat() * 0.9F + 0.1F)) / 3);
      fb.setRBGColorF(red, green, blue);
      fb.setAlphaF(alpha, 0.0F);
      fb.setGridSize(16);
      fb.setParticles(57 + level.random.nextInt(3), 1, 1);
      fb.setScale(0.6F + level.random.nextFloat() * 0.4F);
      fb.setGravity(0.6F);
      fb.setLayer(1);
      add(fb);
   }

   /** tentacleAriseFX: goo and crumbs of the ground under a rising tentacle. */
   public static void tentacleArise(Entity e) {
      ClientLevel level = level();
      for (int j = 0; j < 2.0F * e.getBbHeight(); j++) {
         float f = level.random.nextFloat() * (float) Math.PI * e.getBbHeight();
         float f1 = level.random.nextFloat() * 0.5F + 0.5F;
         float f2 = Mth.sin(f) * e.getBbHeight() * 0.25F * f1;
         float f3 = Mth.cos(f) * e.getBbHeight() * 0.25F * f1;
         goo(e.getX() + f2, e.getY(), e.getZ() + f3, 0, 0.05, 0, 0.4F, 0.0F, 0.4F, 0.5F);
         BlockPos below = e.blockPosition().below();
         if (!level.isEmptyBlock(below)) {
            level.addParticle(new BlockParticleOption(ParticleTypes.BLOCK, level.getBlockState(below)), e.getX() + f3, e.getY(), e.getZ() + f2, 0, 0, 0);
         }
      }
   }

   public static void taintLand(Entity e) {
      ClientLevel level = level();
      float f = level.random.nextFloat() * (float) Math.PI * 2.0F;
      float f1 = level.random.nextFloat() * 0.5F + 0.5F;
      goo(e.getX() + Mth.sin(f) * f1, (e.getBoundingBox().minY + e.getBoundingBox().maxY) / 2.0, e.getZ() + Mth.cos(f) * f1, 0, 0, 0, 0.1F, 0.0F, 0.1F,
         0.4F);
   }

   public static void taintsplosion(Entity e) {
      ClientLevel level = level();
      boolean b = level.random.nextBoolean();
      goo(e.getX(), e.getY() + level.random.nextFloat() * e.getBbHeight(), e.getZ(), (Math.random() * 2.0 - 1.0) * 0.2, (Math.random() * 2.0 - 1.0) * 0.2,
         (Math.random() * 2.0 - 1.0) * 0.2, b ? 0.6F : 0.3F, 0.0F, b ? 0.3F : 0.3F, b ? 0.4F : 0.6F);
   }

   public static void bottleTaintBreak(double x, double y, double z) {
      ClientLevel level = level();
      for (int k1 = 0; k1 < 8; k1++) {
         level.addParticle(new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(ModFlux.BOTTLE_TAINT.get())), x, y, z, level.random.nextGaussian() * 0.15,
            level.random.nextDouble() * 0.2, level.random.nextGaussian() * 0.15);
      }
      level.playLocalSound(x, y, z, SoundEvents.SPLASH_POTION_BREAK, SoundSource.NEUTRAL, 1.0F, level.random.nextFloat() * 0.1F + 0.9F, false);
   }

   // ------------------------------------------------------------------ FXSwarm

   public static Object swarmParticle(Entity target, float scale, float turnSpeed, float speed) {
      ClientLevel level = level();
      var r = level.random;
      Swarm fx = new Swarm(level, target.getX() + (r.nextFloat() - r.nextFloat()) * 2.0F, target.getY() + (r.nextFloat() - r.nextFloat()) * 2.0F,
         target.getZ() + (r.nextFloat() - r.nextFloat()) * 2.0F, target, speed, turnSpeed);
      fx.setRBGColorF(0.8F + r.nextFloat() * 0.2F, r.nextFloat() * 0.4F, 1.0F - r.nextFloat() * 0.2F);
      fx.setScale(scale * 4.0F);
      add(fx);
      return fx;
   }

   public static boolean isAlive(Object particle) {
      return particle instanceof Particle p && p.isAlive();
   }

   /** FXSwarm: a mote buzzing around its swarm entity. */
   static final class Swarm extends FXGeneric {
      private final Entity target;
      private final float speed;
      private final float turn;

      Swarm(ClientLevel level, double x, double y, double z, Entity target, float speed, float turn) {
         super(level, x, y, z);
         this.target = target;
         this.speed = speed;
         this.turn = turn;
         setMaxAge(1000);
         setGridSize(64);
         setParticle(7);
         setLayer(1);
         setSlowDown(1.0);
      }

      @Override
      public void tick() {
         if (!doneFrames) calculateFrames();
         xo = x;
         yo = y;
         zo = z;
         if (!target.isAlive() || age++ >= lifetime) {
            remove();
            return;
         }
         Vec3 to = new Vec3(target.getX() - x, target.getY() + target.getBbHeight() / 2.0 - y, target.getZ() - z);
         double d = to.length();
         if (d > 0.01) {
            to = to.scale(1.0 / d);
            double k = turn / 100.0;
            xd += (to.x * speed - xd) * k + random.nextGaussian() * speed * 0.3;
            yd += (to.y * speed - yd) * k + random.nextGaussian() * speed * 0.3;
            zd += (to.z * speed - zd) * k + random.nextGaussian() * speed * 0.3;
         }
         move(xd, yd, zd);
      }
   }

   // ------------------------------------------------------------------ FXVoidStream

   /** voidStreak: a dark mote travelling from a rift to the void siphon. */
   public static void voidStreak(double x, double y, double z, double x2, double y2, double z2, int seed, float scale) {
      ClientLevel level = level();
      int steps = 20;
      for (int i = 0; i < 4; i++) {
         FXGeneric fb = new FXGeneric(level, x, y, z, (x2 - x) / steps, (y2 - y) / steps, (z2 - z) / steps);
         fb.setMaxAge(steps);
         fb.setRBGColorF(0.1F, 0.0F, 0.15F);
         fb.setAlphaF(0.8F, 0.4F);
         fb.setGridSize(16);
         fb.setParticles(57 + level.random.nextInt(3), 1, 1);
         fb.setScale(scale * 40.0F, scale * 10.0F);
         fb.setSlowDown(1.0);
         fb.setNoClip(true);
         fb.setLayer(1);
         add(fb);
         x += (x2 - x) * 0.05;
         y += (y2 - y) * 0.05;
         z += (z2 - z) * 0.05;
      }
   }
}
