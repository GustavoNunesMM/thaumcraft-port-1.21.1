package thaumcraft.client.renderers;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import thaumcraft.client.fx.FXDispatcher;
import thaumcraft.common.blocks.crafting.CrucibleBlockEntity;
import thaumcraft.common.registers.ModSounds;

/** The client half of TileCrucible: drawEffects and receiveClientEvent. */
public final class CrucibleEffects {
   private CrucibleEffects() {
   }

   public static void draw(CrucibleBlockEntity be) {
      Level level = be.getLevel();
      BlockPos pos = be.getBlockPos();
      RandomSource rand = level.random;
      if (be.heat > 150) {
         FXDispatcher.crucibleFroth(pos.getX() + 0.2F + rand.nextFloat() * 0.6F, pos.getY() + be.getFluidHeight(),
            pos.getZ() + 0.2F + rand.nextFloat() * 0.6F);
         if (be.aspects.visSize() > CrucibleBlockEntity.MAX_TAGS) {
            for (int a = 0; a < 2; a++) {
               FXDispatcher.crucibleFrothDown(pos.getX(), pos.getY() + 1, pos.getZ() + rand.nextFloat());
               FXDispatcher.crucibleFrothDown(pos.getX() + 1, pos.getY() + 1, pos.getZ() + rand.nextFloat());
               FXDispatcher.crucibleFrothDown(pos.getX() + rand.nextFloat(), pos.getY() + 1, pos.getZ());
               FXDispatcher.crucibleFrothDown(pos.getX() + rand.nextFloat(), pos.getY() + 1, pos.getZ() + 1);
            }
         }
      }
      if (rand.nextInt(6) == 0 && !be.aspects.isEmpty()) {
         int color = be.aspects.getAspects()[rand.nextInt(be.aspects.size())].getColor();
         int x = 5 + rand.nextInt(22);
         int y = 5 + rand.nextInt(22);
         FXDispatcher.crucibleBubble(pos.getX() + x / 32.0F + 0.015625F, pos.getY() + 0.05F + be.getFluidHeight(),
            pos.getZ() + y / 32.0F + 0.015625F, (color >> 16 & 255) / 255.0F, (color >> 8 & 255) / 255.0F, (color & 255) / 255.0F);
      }
   }

   public static void event(CrucibleBlockEntity be, int id, int type) {
      Level level = be.getLevel();
      BlockPos pos = be.getBlockPos();
      if (id == 99) {
         FXDispatcher.drawBamf(pos.getX() + 0.5, pos.getY() + 1.25F, pos.getZ() + 0.5, true, true, Direction.UP);
         level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, ModSounds.SPILL.get(), SoundSource.BLOCKS, 0.2F, 1.0F, false);
      } else if (id == 1) {
         FXDispatcher.drawBamf(pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5, true, true, Direction.UP);
      } else if (id == 2) {
         level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, ModSounds.SPILL.get(), SoundSource.BLOCKS, 0.2F, 1.0F, false);
         for (int q = 0; q < 10; q++) FXDispatcher.crucibleBoil(pos, be.getFluidHeight(), be.aspects, type);
      }
   }
}
