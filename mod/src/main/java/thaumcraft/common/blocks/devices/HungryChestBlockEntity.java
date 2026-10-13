package thaumcraft.common.blocks.devices;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.common.registers.ModDevices;

/** TileHungryChest: a plain single chest. */
public class HungryChestBlockEntity extends ChestBlockEntity {
   public HungryChestBlockEntity(BlockPos pos, BlockState state) {
      super(ModDevices.HUNGRY_CHEST_BE.get(), pos, state);
   }

   @Override
   protected Component getDefaultName() {
      return Component.translatable("block.thaumcraft.hungry_chest");
   }
}
