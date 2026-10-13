package thaumcraft.api.casters;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;

/** Blocks or block entities that react to a right click with a caster's gauntlet (tubes, for instance). */
public interface IInteractWithCaster {
   boolean onCasterRightClick(Level level, ItemStack caster, Player player, BlockPos pos, Direction side, InteractionHand hand, BlockHitResult hit);
}
