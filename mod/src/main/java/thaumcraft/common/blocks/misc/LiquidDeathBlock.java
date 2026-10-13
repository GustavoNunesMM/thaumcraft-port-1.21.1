package thaumcraft.common.blocks.misc;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.phys.Vec3;
import thaumcraft.common.lib.damage.DamageSourceThaumcraft;

/** BlockFluidDeath: dissolves anything alive that wades through it (more damage the deeper it is). */
public class LiquidDeathBlock extends LiquidBlock {
   public LiquidDeathBlock(FlowingFluid fluid, Properties properties) {
      super(fluid, properties);
   }

   /** 1.12 meta of a 4-quanta classic fluid: 0 for the source, up to 3 at the thin edges. */
   private static int meta(BlockState state) {
      return Math.min(3, state.getValue(LEVEL) / 2);
   }

   @Override
   protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
      float q = (4 - meta(state)) / 4.0F;
      Vec3 m = entity.getDeltaMovement();
      entity.setDeltaMovement(m.x * (1.0F - q / 2.0F), m.y, m.z * (1.0F - q / 2.0F));
      if (!level.isClientSide && entity instanceof LivingEntity) {
         entity.hurt(DamageSourceThaumcraft.dissolve(level), 4 - meta(state) + 1);
      }
   }

   @Override
   public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource rand) {
      if (rand.nextInt(20) == 0) {
         float h = rand.nextFloat() * 0.075F;
         thaumcraft.client.fx.TaintFX.slimyBubble(pos.getX() + rand.nextFloat(), pos.getY() + 0.1F + 0.225F * (4 - meta(state)),
            pos.getZ() + rand.nextFloat(), 0.075F + h, 0.3F - rand.nextFloat() * 0.1F, 0.0F, 0.4F + rand.nextFloat() * 0.1F);
      }
      if (rand.nextInt(50) == 0) {
         level.playLocalSound(pos.getX() + rand.nextFloat(), pos.getY() + 1.0, pos.getZ() + rand.nextFloat(), SoundEvents.LAVA_POP, SoundSource.BLOCKS,
            0.1F + rand.nextFloat() * 0.1F, 0.9F + rand.nextFloat() * 0.15F, false);
      }
   }
}
