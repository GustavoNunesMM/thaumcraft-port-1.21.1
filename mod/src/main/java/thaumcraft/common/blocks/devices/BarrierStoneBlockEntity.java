package thaumcraft.common.blocks.devices;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import thaumcraft.common.registers.ModDevices;

/** TileBarrierStone: keeps the two barrier blocks above it, and knocks back airborne mobs over it, while unpowered. */
public class BarrierStoneBlockEntity extends BlockEntity {
   private int count = 0;

   public BarrierStoneBlockEntity(BlockPos pos, BlockState state) {
      super(ModDevices.BARRIER_STONE_BE.get(), pos, state);
   }

   public boolean gettingPower() {
      return level.getBestNeighborSignal(worldPosition) > 0;
   }

   public static void serverTick(Level level, BlockPos pos, BlockState state, BarrierStoneBlockEntity be) {
      if (be.count == 0) be.count = level.random.nextInt(100);
      if (be.count % 5 == 0 && !be.gettingPower()) {
         List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class,
            new AABB(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 3, pos.getZ() + 1).inflate(0.1));
         for (LivingEntity e : targets) {
            if (!e.onGround() && !(e instanceof Player)) {
               e.push(-Mth.sin((e.getYRot() + 180.0F) * (float) Math.PI / 180.0F) * 0.2F, -0.1,
                  Mth.cos((e.getYRot() + 180.0F) * (float) Math.PI / 180.0F) * 0.2F);
               e.hurtMarked = true;
            }
         }
      }
      if (++be.count % 100 == 0) {
         BlockState barrier = ModDevices.BARRIER.get().defaultBlockState();
         for (int i = 1; i <= 2; i++) {
            if (!level.getBlockState(pos.above(i)).is(ModDevices.BARRIER.get()) && level.isEmptyBlock(pos.above(i))) {
               level.setBlock(pos.above(i), barrier, 3);
            }
         }
      }
   }
}
