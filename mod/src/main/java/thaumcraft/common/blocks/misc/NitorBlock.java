package thaumcraft.common.blocks.misc;

import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import thaumcraft.common.registers.ModBlockEntities;

/** BlockNitor: a floating, ever-burning flame (drawn only with particles), in the 16 dye colors. */
public class NitorBlock extends BaseEntityBlock {
   private static final VoxelShape SHAPE = box(5.28, 5.28, 5.28, 10.56, 10.56, 10.56);
   public final DyeColor dye;

   public NitorBlock(DyeColor dye, Properties properties) {
      super(properties);
      this.dye = dye;
   }

   @Override
   protected MapCodec<? extends BaseEntityBlock> codec() {
      return simpleCodec(p -> new NitorBlock(dye, p));
   }

   @Override
   protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
      return SHAPE;
   }

   @Override
   protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
      return Shapes.empty();
   }

   @Override
   protected RenderShape getRenderShape(BlockState state) {
      return RenderShape.INVISIBLE;
   }

   @Nullable
   @Override
   public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
      return new NitorBlockEntity(pos, state);
   }

   @Nullable
   @Override
   public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
      return level.isClientSide ? createTickerHelper(type, ModBlockEntities.NITOR.get(), NitorBlockEntity::clientTick) : null;
   }
}
