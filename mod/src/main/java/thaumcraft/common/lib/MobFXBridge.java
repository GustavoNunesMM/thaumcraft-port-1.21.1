package thaumcraft.common.lib;

import net.minecraft.world.entity.Entity;

/**
 * Client effects used by the mobs milestone (FXDispatcher.burst, drawWispParticles, cultistSpawn, slimeJumpFX...). The
 * common code calls these hooks only on the client; the client installs the real implementations at setup.
 */
public final class MobFXBridge {
   public interface Impl {
      void burst(double x, double y, double z, float size);

      void wispParticles(double x, double y, double z, int color);

      void cultistSpawn(double x, double y, double z, double vx, double vy, double vz);

      void slimeJump(Entity e, int size);

      void wispyMotesOnBlock(net.minecraft.core.BlockPos pos, int age, float grav);
   }

   public static Impl INSTANCE = new Impl() {
      @Override
      public void burst(double x, double y, double z, float size) {
      }

      @Override
      public void wispParticles(double x, double y, double z, int color) {
      }

      @Override
      public void cultistSpawn(double x, double y, double z, double vx, double vy, double vz) {
      }

      @Override
      public void slimeJump(Entity e, int size) {
      }

      @Override
      public void wispyMotesOnBlock(net.minecraft.core.BlockPos pos, int age, float grav) {
      }
   };

   private MobFXBridge() {
   }
}
