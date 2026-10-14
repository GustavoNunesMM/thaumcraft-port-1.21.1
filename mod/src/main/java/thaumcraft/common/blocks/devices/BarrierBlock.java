package thaumcraft.common.blocks.devices;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import thaumcraft.common.registers.ModDevices;

/** BlockBarrier ("warding aura"): the invisible wall above a barrier stone. Solid only for mobs, and only while unpowered. */
public class BarrierBlock extends Block {
   public BarrierBlock(Properties properties) {
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
   protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
      if (ctx instanceof EntityCollisionContext ec) {
         Entity e = ec.getEntity();
         if (e instanceof LivingEntity && !(e instanceof Player) && e.getPassengersAndSelf().noneMatch(p -> p instanceof Player)) {
            int a = level.getBlockState(pos.below()).is(ModDevices.PAVING_STONE_BARRIER.get()) ? 1 : 2;
            if (!(level instanceof Level l) || l.getBestNeighborSignal(pos.below(a)) == 0) return Shapes.block();
         }
      }
      return Shapes.empty();
   }

   /** isPassable: open while the barrier stone under it is powered. */
   public static boolean isPassable(Level level, BlockPos pos) {
      for (int a = 1; a < 3; a++) {
         if (level.getBlockEntity(pos.below(a)) instanceof BarrierStoneBlockEntity) return level.getBestNeighborSignal(pos.below(a)) > 0;
      }
      return true;
   }

   @Override
   protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean moving) {
      BlockState below = level.getBlockState(pos.below());
      if (!below.is(ModDevices.PAVING_STONE_BARRIER.get()) && !below.is(this)) level.removeBlock(pos, false);
   }

   @Override
   protected boolean isPathfindable(BlockState state, PathComputationType type) {
      return false;
   }

   @Override
   public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
      return ItemStack.EMPTY;
   }
}
