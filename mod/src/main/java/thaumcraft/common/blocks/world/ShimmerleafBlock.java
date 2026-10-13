package thaumcraft.common.blocks.world;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Vector3f;

/** Port of BlockPlantShimmerleaf: a glowing plant that grows around silverwood trees. */
public class ShimmerleafBlock extends BushBlock {
   public static final MapCodec<ShimmerleafBlock> CODEC = simpleCodec(ShimmerleafBlock::new);
   private static final VoxelShape SHAPE = box(3, 0, 3, 13, 13, 13);

   public ShimmerleafBlock(Properties properties) {
      super(properties);
   }

   @Override
   protected MapCodec<? extends BushBlock> codec() {
      return CODEC;
   }

   @Override
   protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
      return state.is(BlockTags.DIRT);
   }

   @Override
   protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
      Vec3 o = state.getOffset(level, pos);
      return SHAPE.move(o.x, o.y, o.z);
   }

   /** The original drew wispy motes (FXDispatcher, not ported yet); a pale cyan dust is the closest vanilla particle. */
   @Override
   public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource rand) {
      if (rand.nextInt(3) == 0) {
         Vector3f color = new Vector3f(0.3F + rand.nextFloat() * 0.3F, 0.7F + rand.nextFloat() * 0.3F, 0.7F + rand.nextFloat() * 0.3F);
         level.addParticle(new DustParticleOptions(color, 0.6F),
            pos.getX() + 0.5 + rand.nextGaussian() * 0.1, pos.getY() + 0.4 + rand.nextGaussian() * 0.1, pos.getZ() + 0.5 + rand.nextGaussian() * 0.1,
            rand.nextGaussian() * 0.01, rand.nextGaussian() * 0.01, rand.nextGaussian() * 0.01);
      }
   }
}
