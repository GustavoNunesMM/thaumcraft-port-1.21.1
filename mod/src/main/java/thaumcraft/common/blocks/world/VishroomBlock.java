package thaumcraft.common.blocks.world;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Vector3f;

/** Port of BlockPlantVishroom: a magical mushroom that makes whoever walks through it queasy. */
public class VishroomBlock extends BushBlock {
   public static final MapCodec<VishroomBlock> CODEC = simpleCodec(VishroomBlock::new);
   private static final VoxelShape SHAPE = box(4, 0, 4, 12, 10, 12);
   private static final Vector3f COLOR = new Vector3f(0.5F, 0.3F, 0.8F);

   public VishroomBlock(Properties properties) {
      super(properties);
   }

   @Override
   protected MapCodec<? extends BushBlock> codec() {
      return CODEC;
   }

   /** EnumPlantType.Cave in 1.12: any solid ground. */
   @Override
   protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
      return state.isFaceSturdy(level, pos, net.minecraft.core.Direction.UP);
   }

   @Override
   protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
      return SHAPE;
   }

   @Override
   protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
      if (!level.isClientSide && entity instanceof LivingEntity living && level.random.nextInt(5) == 0) {
         living.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 200, 0));
      }
   }

   @Override
   public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource rand) {
      if (rand.nextInt(3) == 0) {
         level.addParticle(new DustParticleOptions(COLOR, 0.6F),
            pos.getX() + 0.5 + (rand.nextFloat() - rand.nextFloat()) * 0.4, pos.getY() + 0.3,
            pos.getZ() + 0.5 + (rand.nextFloat() - rand.nextFloat()) * 0.4, 0, 0, 0);
      }
   }
}
