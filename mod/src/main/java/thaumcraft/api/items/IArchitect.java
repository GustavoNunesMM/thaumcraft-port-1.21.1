package thaumcraft.api.items;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;

/** Tools that preview the blocks they will affect (the plan focus, the primal crusher...). */
public interface IArchitect {
   HitResult getArchitectMOP(ItemStack stack, Level level, LivingEntity player);

   boolean useBlockHighlight(ItemStack stack);

   List<BlockPos> getArchitectBlocks(ItemStack stack, Level level, BlockPos pos, Direction side, Player player);

   boolean showAxis(ItemStack stack, Level level, Player player, Direction side, EnumAxis axis);

   enum EnumAxis {
      X, Y, Z
   }
}
