package thaumcraft.common.blocks.crafting;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.event.EventHooks;
import thaumcraft.common.items.ScribingToolsItem;
import thaumcraft.common.registers.ModBlocks;

/** BlockTable: a wooden or stone table. Using scribing tools on the wooden one turns it into a research table. */
public class TableBlock extends Block {
   private static final VoxelShape SHAPE = Shapes.or(box(0, 12, 0, 16, 16, 16), box(1, 0, 1, 5, 12, 5), box(11, 0, 1, 15, 12, 5),
      box(1, 0, 11, 5, 12, 15), box(11, 0, 11, 15, 12, 15));
   private final boolean wood;

   public TableBlock(boolean wood, Properties properties) {
      super(properties);
      this.wood = wood;
   }

   @Override
   protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
      return SHAPE;
   }

   @Override
   protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                             BlockHitResult hit) {
      if (!wood || !(stack.getItem() instanceof ScribingToolsItem)) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
      if (!level.isClientSide) {
         BlockState table = ModBlocks.RESEARCH_TABLE.get().defaultBlockState()
            .setValue(HorizontalDirectionalBlock.FACING, player.getDirection());
         level.setBlockAndUpdate(pos, table);
         if (level.getBlockEntity(pos) instanceof ResearchTableBlockEntity be) {
            be.getItems().setStackInSlot(0, stack.copy());
            player.setItemInHand(hand, ItemStack.EMPTY);
            be.setChanged();
         }
         if (player instanceof ServerPlayer sp) {
            EventHooks.firePlayerCraftingEvent(sp, new ItemStack(ModBlocks.RESEARCH_TABLE.get()), new SimpleContainer(1));
         }
      }
      return ItemInteractionResult.sidedSuccess(level.isClientSide);
   }

   @Override
   protected boolean isPathfindable(BlockState state, net.minecraft.world.level.pathfinder.PathComputationType type) {
      return false;
   }

   public static boolean isTopSolid(Direction side) {
      return side == Direction.UP;
   }
}
