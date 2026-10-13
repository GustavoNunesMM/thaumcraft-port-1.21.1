package thaumcraft.common.items.tools;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.IAspectContainer;
import thaumcraft.api.aspects.IEssentiaTransport;
import thaumcraft.common.blocks.essentia.TubeBufferBlockEntity;

/**
 * ItemResonator: right-click an essentia transport to read its essentia and suction on the clicked face. (The condenser
 * readout of 1.12 waits for the condenser.)
 */
public class ResonatorItem extends Item {
   public ResonatorItem(Properties p) {
      super(p);
   }

   @Override
   public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext ctx) {
      Level world = ctx.getLevel();
      BlockPos pos = ctx.getClickedPos();
      Player player = ctx.getPlayer();
      BlockEntity tile = world.getBlockEntity(pos);
      if (!(tile instanceof IEssentiaTransport et) || player == null) return InteractionResult.PASS;
      if (world.isClientSide) {
         player.swing(ctx.getHand());
         return InteractionResult.SUCCESS;
      }
      Direction side = ctx.getClickedFace();
      if (!(tile instanceof TubeBufferBlockEntity) && et.getEssentiaType(side) != null) {
         player.sendSystemMessage(Component.translatable("tc.resonator1", "" + et.getEssentiaAmount(side), et.getEssentiaType(side).getName()));
      } else if (tile instanceof TubeBufferBlockEntity && tile instanceof IAspectContainer ac && ac.getAspects().size() > 0) {
         for (Aspect aspect : ac.getAspects().getAspectsSortedByName()) {
            player.sendSystemMessage(Component.translatable("tc.resonator1", "" + ac.getAspects().getAmount(aspect), aspect.getName()));
         }
      }
      Component s = et.getSuctionType(side) != null ? Component.literal(et.getSuctionType(side).getName()) : Component.translatable("tc.resonator3");
      player.sendSystemMessage(Component.translatable("tc.resonator2", "" + et.getSuctionAmount(side), s));
      world.playSound(null, pos, SoundEvents.SHIELD_BLOCK, SoundSource.BLOCKS, 0.5F, 1.9F + world.random.nextFloat() * 0.1F);
      return InteractionResult.SUCCESS;
   }
}
