package thaumcraft.common.items.consumables;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import thaumcraft.common.registers.ModFlux;

/**
 * Bucket of flux goo (the Forge universal bucket filled with FluidFluxGoo in 1.12): pours a full puddle of goo. Flux goo
 * is a finite block rather than a flowing fluid, so this is not a vanilla BucketItem.
 */
public class FluxGooBucketItem extends Item {
   public FluxGooBucketItem(Properties properties) {
      super(properties);
   }

   @Override
   public InteractionResult useOn(UseOnContext ctx) {
      Level level = ctx.getLevel();
      BlockPos pos = ctx.getClickedPos();
      if (!level.getBlockState(pos).canBeReplaced(new BlockPlaceContext(ctx))) pos = pos.relative(ctx.getClickedFace());
      if (!level.getBlockState(pos).canBeReplaced()) return InteractionResult.FAIL;
      Player player = ctx.getPlayer();
      if (!level.isClientSide) {
         level.setBlockAndUpdate(pos, ModFlux.FLUX_GOO.get().defaultBlockState());
         level.playSound(null, pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
         if (player != null) {
            ItemStack result = ItemUtils.createFilledResult(ctx.getItemInHand(), player, new ItemStack(Items.BUCKET));
            player.setItemInHand(ctx.getHand(), result);
         }
      }
      return InteractionResult.sidedSuccess(level.isClientSide);
   }
}
