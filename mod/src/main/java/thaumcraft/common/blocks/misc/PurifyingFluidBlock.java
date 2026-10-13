package thaumcraft.common.blocks.misc;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.phys.Vec3;
import thaumcraft.common.research.PlayerWarp;
import thaumcraft.common.research.ResearchManager;
import thaumcraft.common.registers.ModFlux;

/**
 * BlockFluidPure: a player bathing in a source block gets a Warp Ward (shorter the more permanent warp they have) and
 * uses the source up.
 */
public class PurifyingFluidBlock extends LiquidBlock {
   public PurifyingFluidBlock(FlowingFluid fluid, Properties properties) {
      super(fluid, properties);
   }

   @Override
   protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
      float q = (8 - state.getValue(LEVEL) % 8) / 8.0F;
      Vec3 m = entity.getDeltaMovement();
      entity.setDeltaMovement(m.x * (1.0F - q / 2.0F), m.y, m.z * (1.0F - q / 2.0F));
      if (!level.isClientSide && state.getValue(LEVEL) == 0 && entity instanceof Player player && !player.hasEffect(ModFlux.WARP_WARD)) {
         int warp = ResearchManager.warp(player).get(PlayerWarp.Type.PERMANENT);
         int div = 1;
         if (warp > 0) div = Math.max(1, (int) Math.sqrt(warp));
         player.addEffect(new MobEffectInstance(ModFlux.WARP_WARD, Math.min(32000, 200000 / div), 0, true, true));
         level.removeBlock(pos, false);
      }
   }

   @Override
   public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource rand) {
      int meta = state.getValue(LEVEL) % 8;
      if (rand.nextInt(10) == 0) {
         thaumcraft.client.fx.TaintFX.pureMote(pos.getX() + rand.nextFloat(), pos.getY() + 0.125F * (8 - meta), pos.getZ() + rand.nextFloat());
      }
      if (rand.nextInt(50) == 0) {
         level.playLocalSound(pos.getX() + rand.nextFloat(), pos.getY() + 0.5, pos.getZ() + rand.nextFloat(), SoundEvents.LAVA_POP, SoundSource.BLOCKS,
            0.1F + rand.nextFloat() * 0.1F, 0.9F + rand.nextFloat() * 0.15F, false);
      }
   }
}
