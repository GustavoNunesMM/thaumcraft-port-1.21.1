package thaumcraft.common.blocks.misc;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import thaumcraft.common.lib.ServerTasks;
import thaumcraft.common.registers.ModSounds;

/** BlockEffect "sap": the withering field a curse focus leaves; withers what walks through and fades on a random tick. */
public class SapEffectBlock extends Block {
   public SapEffectBlock(Properties properties) {
      super(properties);
   }

   @Override
   protected RenderShape getRenderShape(BlockState state) {
      return RenderShape.INVISIBLE;
   }

   @Override
   protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
      return Shapes.empty();
   }

   @Override
   protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
      if (!level.isClientSide && !thaumcraft.common.entities.monster.eldritch.EldritchHelper.isEldritch(entity) && entity instanceof LivingEntity le && !le.hasEffect(MobEffects.WITHER)) {
         ServerTasks.schedule(0, () -> {
            le.addEffect(new MobEffectInstance(MobEffects.WITHER, 40, 0, true, true));
            le.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1, true, true));
            le.addEffect(new MobEffectInstance(MobEffects.HUNGER, 40, 1, true, true));
         });
      }
   }

   @Override
   protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
      level.removeBlock(pos, false);
   }

   @Override
   public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource r) {
      thaumcraft.client.fx.FociParticles.sap(pos.getX(), pos.getY(), pos.getZ());
      if (r.nextInt(50) == 0) {
         level.playLocalSound(pos.getX(), pos.getY(), pos.getZ(), ModSounds.JACOBS.get(), SoundSource.AMBIENT, 0.25F,
            1.0F + (r.nextFloat() - r.nextFloat()) * 0.2F, false);
      }
   }
}
