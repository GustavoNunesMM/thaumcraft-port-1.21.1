package thaumcraft.common.blocks.devices;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * BlockLoot: the crates and urns found in dungeons. Breaking one gives 1-3 (+1 per rarity) random loot bag items
 * (the loot tables thaumcraft:blocks/loot_*), or the block itself with silk touch.
 */
public class LootBlock extends Block {
   private static final VoxelShape URN = box(2, 1, 2, 14, 13, 14);
   private static final VoxelShape CRATE = box(1, 0, 1, 15, 14, 15);
   public final boolean urn;

   public LootBlock(boolean urn, Properties properties) {
      super(properties);
      this.urn = urn;
   }

   @Override
   protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
      return urn ? URN : CRATE;
   }
}
