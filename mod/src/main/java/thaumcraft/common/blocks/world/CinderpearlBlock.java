package thaumcraft.common.blocks.world;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Port of BlockPlantCinderpearl: a smouldering desert plant. */
public class CinderpearlBlock extends BushBlock {
   public static final MapCodec<CinderpearlBlock> CODEC = simpleCodec(CinderpearlBlock::new);
   private static final VoxelShape SHAPE = box(3, 0, 3, 13, 13, 13);

   public CinderpearlBlock(Properties properties) {
      super(properties);
   }

   @Override
   protected MapCodec<? extends BushBlock> codec() {
      return CODEC;
   }

   /** Sand, dirt and (stained) terracotta, as in 1.12 (red sand was a kind of sand back then). */
   @Override
   protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
      return state.is(BlockTags.SAND) || state.is(BlockTags.DIRT) || state.is(BlockTags.TERRACOTTA);
   }

   @Override
   protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
      Vec3 o = state.getOffset(level, pos);
      return SHAPE.move(o.x, o.y, o.z);
   }

   @Override
   public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource rand) {
      if (rand.nextBoolean()) {
         double x = pos.getX() + 0.5 + (rand.nextFloat() - rand.nextFloat()) * 0.1;
         double y = pos.getY() + 0.6 + (rand.nextFloat() - rand.nextFloat()) * 0.1;
         double z = pos.getZ() + 0.5 + (rand.nextFloat() - rand.nextFloat()) * 0.1;
         level.addParticle(ParticleTypes.SMOKE, x, y, z, 0, 0, 0);
         level.addParticle(ParticleTypes.FLAME, x, y, z, 0, 0, 0);
      }
   }
}
