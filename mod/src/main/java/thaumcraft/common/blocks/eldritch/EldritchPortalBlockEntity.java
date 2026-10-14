package thaumcraft.common.blocks.eldritch;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.common.registers.ModEldritch;

/** Only there so the portal can be drawn (the swirling eldritch_portal.png billboard). */
public class EldritchPortalBlockEntity extends BlockEntity {
   public EldritchPortalBlockEntity(BlockPos pos, BlockState state) {
      super(ModEldritch.ELDRITCH_PORTAL_BE.get(), pos, state);
   }
}
