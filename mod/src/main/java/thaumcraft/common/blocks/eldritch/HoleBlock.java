package thaumcraft.common.blocks.eldritch;

import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import thaumcraft.common.registers.ModEldritch;

/**
 * BlockHole: a "dimensional tear" left by the rift focus. The block it replaced is shifted into another reality and
 * comes back when the tear's countdown ends (TileHole). Unbreakable, no collision, cannot be targeted.
 */
public class HoleBlock extends BaseEntityBlock {
   public static final MapCodec<HoleBlock> CODEC = simpleCodec(HoleBlock::new);

   /** BlockUtils.portableHoleBlackList (ConfigBlocks): beds, pistons, doors and the infernal furnace. */
   public static final java.util.List<String> BLACKLIST = new java.util.ArrayList<>(java.util.List.of("minecraft:piston", "minecraft:piston_head",
      "minecraft:sticky_piston", "minecraft:moving_piston", "thaumcraft:infernal_furnace"));

   public HoleBlock(Properties properties) {
      super(properties);
   }

   @Override
   protected MapCodec<? extends BaseEntityBlock> codec() {
      return CODEC;
   }

   public static boolean isBlacklisted(BlockState state) {
      if (state.is(net.minecraft.tags.BlockTags.BEDS) || state.is(net.minecraft.tags.BlockTags.DOORS)) return true;
      return BLACKLIST.contains(net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString());
   }

   /**
    * FocusEffectRift.createHole: turns the block at pos into a tear for {@code max} ticks. The tear spreads
    * {@code count - 1} more blocks against {@code side} (and a 3x3 ring of single tears around each step).
    */
   public static boolean createHole(Level level, BlockPos pos, @Nullable Direction side, byte count, int max) {
      BlockState bs = level.getBlockState(pos);
      if (!level.isClientSide && level.getBlockEntity(pos) == null && !isBlacklisted(bs) && !bs.is(Blocks.BEDROCK)
         && !bs.is(ModEldritch.HOLE.get()) && (bs.isAir() || !bs.canBeReplaced()) && bs.getDestroySpeed(level, pos) != -1.0F) {
         if (level.setBlock(pos, ModEldritch.HOLE.get().defaultBlockState(), 3) && level.getBlockEntity(pos) instanceof HoleBlockEntity ts) {
            ts.oldBlock = bs;
            ts.countdownMax = (short) max;
            ts.count = count;
            ts.direction = side;
            ts.setChanged();
         }
         return true;
      }
      return false;
   }

   @Override
   protected RenderShape getRenderShape(BlockState state) {
      return RenderShape.ENTITYBLOCK_ANIMATED;
   }

   @Override
   protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
      return Shapes.empty();
   }

   @Override
   protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
      return Shapes.empty();
   }

   @Override
   public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
      return ItemStack.EMPTY;
   }

   @Override
   protected float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
      return 1.0F;
   }

   @Override
   protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
      return true;
   }

   @Nullable
   @Override
   public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
      return new HoleBlockEntity(pos, state);
   }

   @Nullable
   @Override
   public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
      return createTickerHelper(type, ModEldritch.HOLE_BE.get(), level.isClientSide ? HoleBlockEntity::clientTick : HoleBlockEntity::serverTick);
   }
}
