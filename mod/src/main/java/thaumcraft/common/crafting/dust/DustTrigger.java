package thaumcraft.common.crafting.dust;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/** IDustTrigger: something that happens when salis mundus is used on a block (or a multiblock structure). */
public interface DustTrigger {
   List<DustTrigger> TRIGGERS = new ArrayList<>();

   /** Where the structure sits relative to the clicked block; null if this trigger doesn't apply. */
   record Placement(int xOffset, int yOffset, int zOffset, @Nullable Direction facing) {}

   @Nullable
   Placement getValidFace(Level level, Player player, BlockPos pos, Direction face);

   void execute(Level level, Player player, BlockPos pos, Placement placement, Direction face);

   /** Blocks that sparkle on the client when it triggers. */
   default List<BlockPos> sparkle(Level level, Player player, BlockPos pos, Placement placement) {
      return List.of(pos);
   }

   static void register(DustTrigger trigger) {
      TRIGGERS.add(trigger);
   }
}
