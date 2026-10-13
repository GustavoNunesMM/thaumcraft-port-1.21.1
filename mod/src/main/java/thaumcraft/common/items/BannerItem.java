package thaumcraft.common.items;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.blocks.devices.BannerBlockEntity;
import thaumcraft.common.registers.ModDevices;

/** BlockBannerTCItem: placed standing on a floor (facing the player) or hanging from the side of a block. */
public class BannerItem extends BlockItem {
   public BannerItem(Block block, Properties properties) {
      super(block, properties);
   }

   @Override
   public InteractionResult useOn(UseOnContext ctx) {
      Direction side = ctx.getClickedFace();
      Level level = ctx.getLevel();
      Player player = ctx.getPlayer();
      if (side == Direction.DOWN || !level.getBlockState(ctx.getClickedPos()).isSolid()) return InteractionResult.FAIL;
      BlockPos pos = ctx.getClickedPos().relative(side);
      ItemStack stack = ctx.getItemInHand();
      if (player != null && !player.mayUseItemAt(pos, side, stack)) return InteractionResult.FAIL;
      if (!level.getBlockState(pos).canBeReplaced()) return InteractionResult.FAIL;
      if (level.isClientSide) return InteractionResult.SUCCESS;
      BlockState state = getBlock().defaultBlockState();
      level.setBlock(pos, state, 3);
      if (level.getBlockEntity(pos) instanceof BannerBlockEntity tile) {
         if (side == Direction.UP) {
            float yaw = player != null ? player.getYRot() : 0.0F;
            tile.setBannerFacing((byte) (Mth.floor((yaw + 180.0F) * 16.0F / 360.0F + 0.5) & 15));
         } else {
            tile.setWall(true);
            int i = switch (side) {
               case NORTH -> 8;
               case WEST -> 4;
               case EAST -> 12;
               default -> 0;
            };
            tile.setBannerFacing((byte) i);
         }
         Aspect a = stack.get(ModDevices.BANNER_ASPECT.get());
         if (a != null) tile.setAspect(a);
         tile.sync();
      }
      level.playSound(null, pos, state.getSoundType(level, pos, player).getPlaceSound(), net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 0.8F);
      stack.shrink(1);
      return InteractionResult.CONSUME;
   }

   @Override
   public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
      Aspect a = stack.get(ModDevices.BANNER_ASPECT.get());
      if (a != null) tooltip.add(a.getLocalizedName());
   }
}
