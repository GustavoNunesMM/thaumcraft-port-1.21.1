package thaumcraft.common.golems;

import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import thaumcraft.common.blocks.DeviceBlock;
import thaumcraft.common.golems.menu.GolemBuilderMenu;
import thaumcraft.common.registers.ModBlocks;
import thaumcraft.common.registers.ModGolems;

/**
 * BlockGolemBuilder: the golem press, made from a piston by the MINDCLOCKWORK multiblock. Breaking it (or any of its
 * placeholders) turns the structure back into its parts.
 */
public class GolemBuilderBlock extends DeviceBlock {
   public static final MapCodec<GolemBuilderBlock> CODEC = simpleCodec(GolemBuilderBlock::new);
   public static boolean ignore = false;

   public GolemBuilderBlock(Properties props) {
      super(props);
   }

   @Override
   protected MapCodec<? extends BaseEntityBlock> codec() {
      return CODEC;
   }

   @Override
   protected Facing facingType() {
      return Facing.HORIZONTAL;
   }

   @Override
   protected RenderShape getRenderShape(BlockState state) {
      return RenderShape.ENTITYBLOCK_ANIMATED;
   }

   @Nullable
   @Override
   public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
      return new GolemBuilderBlockEntity(pos, state);
   }

   @Nullable
   @Override
   public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
      return createTickerHelper(type, ModGolems.GOLEM_BUILDER_BE.get(),
         level.isClientSide ? GolemBuilderBlockEntity::clientTick : GolemBuilderBlockEntity::serverTick);
   }

   @Override
   protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
      if (!level.isClientSide && player instanceof ServerPlayer sp) openMenu(sp, pos);
      return InteractionResult.sidedSuccess(level.isClientSide);
   }

   public static void openMenu(ServerPlayer player, BlockPos pos) {
      if (!(player.level().getBlockEntity(pos) instanceof GolemBuilderBlockEntity be)) return;
      player.openMenu(new SimpleMenuProvider((id, inv, p) -> new GolemBuilderMenu(id, inv, be), Component.translatable("block.thaumcraft.golem_builder")),
         buf -> buf.writeBlockPos(pos));
   }

   @Override
   protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
      if (!state.is(newState.getBlock())) {
         if (level.getBlockEntity(pos) instanceof GolemBuilderBlockEntity be && !level.isClientSide) {
            net.minecraft.world.Containers.dropContents(level, pos, be);
         }
         destroy(level, pos, pos);
      }
      super.onRemove(state, level, pos, newState, moving);
   }

   /** Turns the placeholders around the press back into their blocks, and the press itself into a piston. */
   public static void destroy(Level level, BlockPos pos, BlockPos startpos) {
      if (ignore || level.isClientSide) return;
      ignore = true;
      try {
         for (int a = -1; a <= 1; a++) {
            for (int b = 0; b <= 1; b++) {
               for (int c = -1; c <= 1; c++) {
                  BlockPos q = pos.offset(a, b, c);
                  if (q.equals(startpos)) continue;
                  BlockState bs = level.getBlockState(q);
                  if (bs.is(ModGolems.PLACEHOLDER_BARS.get())) level.setBlockAndUpdate(q, Blocks.IRON_BARS.defaultBlockState());
                  if (bs.is(ModGolems.PLACEHOLDER_ANVIL.get())) level.setBlockAndUpdate(q, Blocks.ANVIL.defaultBlockState());
                  if (bs.is(ModGolems.PLACEHOLDER_CAULDRON.get())) level.setBlockAndUpdate(q, Blocks.CAULDRON.defaultBlockState());
                  if (bs.is(ModGolems.PLACEHOLDER_TABLE.get())) level.setBlockAndUpdate(q, ModBlocks.TABLE_STONE.get().defaultBlockState());
               }
            }
         }
         if (!pos.equals(startpos)) level.setBlockAndUpdate(pos, Blocks.PISTON.defaultBlockState().setValue(PistonBaseBlock.FACING, Direction.UP));
      } finally {
         ignore = false;
      }
   }
}
