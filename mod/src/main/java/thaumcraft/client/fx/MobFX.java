package thaumcraft.client.fx;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import thaumcraft.common.lib.MobFXBridge;

/** The client side of MobFXBridge: FXDispatcher.burst, drawWispParticles, cultistSpawn, slimeJumpFX and drawWispyMotesOnBlock. */
public final class MobFX implements MobFXBridge.Impl {
   public static final MobFX INSTANCE = new MobFX();

   private MobFX() {
   }

   private static ClientLevel level() {
      return Minecraft.getInstance().level;
   }

   private static void add(Particle p) {
      Minecraft.getInstance().particleEngine.add(p);
   }

   @Override
   public void burst(double x, double y, double z, float size) {
      ClientLevel level = level();
      if (level == null) return;
      FXGeneric fb = new FXGeneric(level, x, y, z, 0.0, 0.0, 0.0);
      fb.setMaxAge(31);
      fb.setGridSize(16);
      fb.setParticles(208, 31, 1);
      fb.setScale(size);
      add(fb);
   }

   @Override
   public void wispParticles(double x, double y, double z, int color) {
      ClientLevel level = level();
      if (level == null) return;
      FXGeneric fb = new FXGeneric(level, x, y, z, 0.0, 0.0, 0.0);
      fb.setMaxAge(10 + level.random.nextInt(5));
      fb.setRBGColorF((color >> 16 & 255) / 255.0F, (color >> 8 & 255) / 255.0F, (color & 255) / 255.0F);
      fb.setAlphaF(0.5F);
      fb.setLoop(true);
      fb.setGridSize(64);
      fb.setParticles(264, 8, 1);
      fb.setScale(1.0F + level.random.nextFloat() * 0.25F, 0.05F);
      fb.setWind(2.5E-4);
      fb.setRandomMovementScale(0.0025F, 0.0F, 0.0025F);
      add(fb);
   }

   @Override
   public void cultistSpawn(double x, double y, double z, double vx, double vy, double vz) {
      ClientLevel level = level();
      if (level == null) return;
      FXGeneric fb = new FXGeneric(level, x, y, z, vx, vy, vz);
      fb.setMaxAge(10 + level.random.nextInt(10));
      fb.setRBGColorF(1.0F, 1.0F, 1.0F, 0.6F, 0.0F, 0.0F);
      fb.setAlphaF(0.8F);
      fb.setGridSize(16);
      fb.setParticles(160, 6, 1);
      fb.setScale(3.0F + level.random.nextFloat() * 2.0F);
      fb.setLayer(1);
      add(fb);
   }

   /** slimeJumpFX: purple slime ball fragments (FXBreakingFade; the vanilla item particle, tinted). */
   @Override
   public void slimeJump(Entity e, int size) {
      ClientLevel level = level();
      if (level == null) return;
      float f = level.random.nextFloat() * (float) Math.PI * 2.0F;
      float f1 = level.random.nextFloat() * 0.5F + 0.5F;
      float f2 = Mth.sin(f) * size * 0.5F * f1;
      float f3 = Mth.cos(f) * size * 0.5F * f1;
      Particle p = Minecraft.getInstance().particleEngine.createParticle(new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.SLIME_BALL)),
         e.getX() + f2, (e.getBoundingBox().minY + e.getBoundingBox().maxY) / 2.0, e.getZ() + f3, 0.0, 0.0, 0.0);
      if (p != null) {
         p.setColor(0.7F, 0.0F, 1.0F);
         p.setLifetime((int) (66.0F / (level.random.nextFloat() * 0.9F + 0.1F)));
      }
   }

   @Override
   public void wispyMotesOnBlock(BlockPos pp, int age, float grav) {
      ClientLevel level = level();
      if (level == null) return;
      FXDispatcher.drawWispyMotes(pp.getX() + level.random.nextFloat(), pp.getY(), pp.getZ() + level.random.nextFloat(), 0.0, 0.0, 0.0, age,
         0.4F + level.random.nextFloat() * 0.6F, 0.6F + level.random.nextFloat() * 0.4F, 0.6F + level.random.nextFloat() * 0.4F, grav);
   }
}
