package thaumcraft.common.golems;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import thaumcraft.common.registers.ModGolems;

/**
 * BlockPlaceholder: an invisible part of the golem press (the press model draws them). Using one opens the press,
 * breaking one takes the press apart; it drops the block it stands for (loot table).
 */
public class PlaceholderBlock extends Block {
   public PlaceholderBlock(Properties props) {
      super(props);
   }

   @Override
   protected RenderShape getRenderShape(BlockState state) {
      return RenderShape.INVISIBLE;
   }

   @Override
   protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
      return Shapes.block();
   }

   @Override
   protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
      if (level.isClientSide) return InteractionResult.SUCCESS;
      for (BlockPos q : BlockPos.betweenClosed(pos.offset(-1, -1, -1), pos.offset(1, 1, 1))) {
         if (level.getBlockState(q).is(ModGolems.GOLEM_BUILDER.get()) && player instanceof ServerPlayer sp) {
            GolemBuilderBlock.openMenu(sp, q.immutable());
            return InteractionResult.SUCCESS;
         }
      }
      return InteractionResult.PASS;
   }

   @Override
   protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
      if (!state.is(newState.getBlock()) && !GolemBuilderBlock.ignore && !level.isClientSide) {
         for (BlockPos q : BlockPos.betweenClosed(pos.offset(-1, -1, -1), pos.offset(1, 1, 1))) {
            if (level.getBlockState(q).is(ModGolems.GOLEM_BUILDER.get())) {
               GolemBuilderBlock.destroy(level, q.immutable(), pos);
               break;
            }
         }
      }
      super.onRemove(state, level, pos, newState, moving);
   }
}
