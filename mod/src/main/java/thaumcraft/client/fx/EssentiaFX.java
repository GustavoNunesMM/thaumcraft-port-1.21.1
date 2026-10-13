package thaumcraft.client.fx;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.common.blocks.DeviceBlock;
import thaumcraft.common.blocks.essentia.EssentiaTransportBlock;
import thaumcraft.common.blocks.essentia.SmelterBlockEntity;
import thaumcraft.common.blocks.essentia.TubeBlockEntity;
import thaumcraft.common.network.EssentiaSourcePayload;

/** Essentia effects: tube venting, smeltery vents and the essentia streams of EssentiaHandler. */
public final class EssentiaFX {
   private EssentiaFX() {
   }

   private static float r(int color) {
      return (color >> 16 & 255) / 255.0F;
   }

   private static float g(int color) {
      return (color >> 8 & 255) / 255.0F;
   }

   private static float b(int color) {
      return (color & 255) / 255.0F;
   }

   /** drawVentParticles: a puff of coloured smoke. */
   public static void drawVentParticles(double x, double y, double z, double vx, double vy, double vz, int color) {
      ClientLevel level = Minecraft.getInstance().level;
      FXGeneric fb = new FXGeneric(level, x, y, z, vx, vy, vz);
      fb.setMaxAge(40);
      fb.setRBGColorF(r(color), g(color), b(color));
      fb.setAlphaF(0.4F, 0.0F);
      fb.setParticles(64, 8, 1);
      fb.setScale(2.0F, 5.0F);
      fb.setLayer(1);
      fb.setSlowDown(0.975);
      fb.setRandomMovementScale(0.0025F, 0.0F, 0.0025F);
      Minecraft.getInstance().particleEngine.add(fb);
   }

   public static void tubeVent(TubeBlockEntity t) {
      java.util.Random r = new java.util.Random(t.hashCode() * 4L);
      float rp = r.nextFloat() * 360.0F;
      float ry = r.nextFloat() * 360.0F;
      double fx = -Mth.sin(ry / 180.0F * (float) Math.PI) * Mth.cos(rp / 180.0F * (float) Math.PI);
      double fz = Mth.cos(ry / 180.0F * (float) Math.PI) * Mth.cos(rp / 180.0F * (float) Math.PI);
      double fy = -Mth.sin(rp / 180.0F * (float) Math.PI);
      BlockPos p = t.getBlockPos();
      drawVentParticles(p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, fx / 5.0, fy / 5.0, fz / 5.0, t.ventColor);
   }

   public static void smelterVent(SmelterBlockEntity s, Direction d) {
      ClientLevel level = Minecraft.getInstance().level;
      BlockPos p = s.getBlockPos();
      Direction o = d.getOpposite();
      level.playLocalSound(p.getX() + 0.5 + o.getStepX(), p.getY() + 0.5, p.getZ() + 0.5 + o.getStepZ(), SoundEvents.LAVA_EXTINGUISH,
         SoundSource.BLOCKS, 0.25F, 2.6F + (level.random.nextFloat() - level.random.nextFloat()) * 0.8F, true);
      for (int a = 0; a < 4; a++) {
         float fx = 0.1F - level.random.nextFloat() * 0.2F;
         float fz = 0.1F - level.random.nextFloat() * 0.2F;
         float fy = 0.1F - level.random.nextFloat() * 0.2F;
         float fx2 = 0.1F - level.random.nextFloat() * 0.2F;
         float fz2 = 0.1F - level.random.nextFloat() * 0.2F;
         float fy2 = 0.1F - level.random.nextFloat() * 0.2F;
         drawVentParticles(p.getX() + 0.5F + fx + o.getStepX(), p.getY() + 0.5F + fy, p.getZ() + 0.5F + fz + o.getStepZ(),
            o.getStepX() / 4.0F + fx2, o.getStepY() / 4.0F + fy2, o.getStepZ() / 4.0F + fz2, 0xAAAAAA);
      }
   }

   /**
    * FXEssentiaStream, simplified: glowing motes that leave the source and home in on the target
    * (the original drew a GLE tube along the motes' path).
    */
   public static void essentiaStream(EssentiaSourcePayload p) {
      ClientLevel level = Minecraft.getInstance().level;
      if (level == null) return;
      double sx = p.from().getX() + 0.5, sy = p.from().getY() + 0.5, sz = p.from().getZ() + 0.5;
      BlockState bs = level.getBlockState(p.from());
      if (bs.getBlock() instanceof EssentiaTransportBlock) {
         Direction f = DeviceBlock.getFacing(bs);
         sx += f.getStepX() * 0.05;
         sy += f.getStepY() * 0.05;
         sz += f.getStepZ() * 0.05;
      }
      int count = 2 + Math.min(p.ext(), 6) / 2;
      for (int i = 0; i < count; i++) {
         Minecraft.getInstance().particleEngine.add(new Stream(level, sx, sy, sz, p.to().getX() + 0.5, p.to().getY() + 0.5,
            p.to().getZ() + 0.5, p.color(), i * 2));
      }
   }

   public static final class Stream extends FXGeneric {
      private final double tx, ty, tz;
      private final int delay;

      public Stream(ClientLevel level, double x, double y, double z, double tx, double ty, double tz, int color, int delay) {
         super(level, x, y, z, 0, 0, 0);
         this.tx = tx;
         this.ty = ty;
         this.tz = tz;
         this.delay = delay;
         double d = Math.sqrt((tx - x) * (tx - x) + (ty - y) * (ty - y) + (tz - z) * (tz - z));
         setMaxAge(Math.max(10, (int) (d * 12)) + delay);
         setRBGColorF(r(color), g(color), b(color));
         setAlphaF(0.0F, 0.9F, 0.9F, 0.0F);
         setParticle(7);
         setScale(0.6F + random.nextFloat() * 0.3F);
         setLayer(0);
         setNoClip(true);
         xd = Mth.sin(delay / 4.0F) * 0.015F;
         yd = 0.02 + Mth.sin(delay / 3.0F) * 0.015F;
         zd = Mth.sin(delay / 2.0F) * 0.015F;
      }

      @Override
      public void tick() {
         if (age < delay) {
            age++;
            xo = x;
            yo = y;
            zo = z;
            return;
         }
         double dx = tx - x, dy = ty - y, dz = tz - z;
         double d = Math.sqrt(dx * dx + dy * dy + dz * dz);
         if (d < 0.2) {
            remove();
            return;
         }
         double pull = 0.01 / Math.min(1.0, d);
         xd = Mth.clamp((xd + dx / d * pull) * 0.985, -0.08, 0.08);
         yd = Mth.clamp((yd + dy / d * pull) * 0.985, -0.08, 0.08);
         zd = Mth.clamp((zd + dz / d * pull) * 0.985, -0.08, 0.08);
         xo = x;
         yo = y;
         zo = z;
         if (age++ >= lifetime) remove();
         setPos(x + xd, y + yd, z + zd);
      }
   }
}
