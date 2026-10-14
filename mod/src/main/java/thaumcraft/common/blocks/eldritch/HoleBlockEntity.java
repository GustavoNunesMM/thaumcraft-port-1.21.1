package thaumcraft.common.blocks.eldritch;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.common.blocks.TCBlockEntity;
import thaumcraft.common.registers.ModEldritch;

/**
 * TileHole (a TileMemory): remembers the block it replaced and puts it back after {@code countdownMax} ticks. On its
 * first tick a tear with {@code count > 1} opens a 3x3 ring around itself in the plane of its direction and the next
 * tear one block deeper, so a cast bores a 3x3 tunnel.
 */
public class HoleBlockEntity extends TCBlockEntity {
   public BlockState oldBlock = Blocks.AIR.defaultBlockState();
   public short countdown = 0;
   public short countdownMax = 120;
   public byte count = 0;
   @Nullable
   public Direction direction = null;

   public HoleBlockEntity(BlockPos pos, BlockState state) {
      super(ModEldritch.HOLE_BE.get(), pos, state);
   }

   public static void serverTick(Level level, BlockPos pos, BlockState state, HoleBlockEntity te) {
      if (te.countdown == 0 && te.count > 1 && te.direction != null) {
         for (int a = 0; a < 9; a++) {
            if (a / 3 == 1 && a % 3 == 1) continue;
            BlockPos p = switch (te.direction.getAxis()) {
               case Y -> pos.offset(-1 + a / 3, 0, -1 + a % 3);
               case Z -> pos.offset(-1 + a / 3, -1 + a % 3, 0);
               case X -> pos.offset(0, -1 + a / 3, -1 + a % 3);
            };
            HoleBlock.createHole(level, p, null, (byte) 1, te.countdownMax);
         }
         if (!HoleBlock.createHole(level, pos.relative(te.direction.getOpposite()), te.direction, (byte) (te.count - 1), te.countdownMax)) {
            te.count = 0;
         }
      }
      te.countdown++;
      if (te.countdown % 20 == 0) te.setChanged();
      if (te.countdown >= te.countdownMax) level.setBlock(pos, te.oldBlock, 3);
   }

   public static void clientTick(Level level, BlockPos pos, BlockState state, HoleBlockEntity te) {
      for (int a = 0; a < 2; a++) te.surroundWithSparkles(level, pos);
   }

   private void surroundWithSparkles(Level level, BlockPos pos) {
      for (Direction d1 : Direction.values()) {
         BlockState b1 = level.getBlockState(pos.relative(d1));
         if (b1.is(ModEldritch.HOLE.get()) || b1.isSolidRender(level, pos.relative(d1))) continue;
         for (Direction d2 : Direction.values()) {
            if (d1.getAxis() == d2.getAxis()) continue;
            if (!level.getBlockState(pos.relative(d2)).isSolidRender(level, pos.relative(d2))
               && !level.getBlockState(pos.relative(d1).relative(d2)).isSolidRender(level, pos.relative(d1).relative(d2))) continue;
            float sx = 0.5F * d1.getStepX();
            float sy = 0.5F * d1.getStepY();
            float sz = 0.5F * d1.getStepZ();
            if (sx == 0.0F) sx = 0.5F * d2.getStepX();
            if (sy == 0.0F) sy = 0.5F * d2.getStepY();
            if (sz == 0.0F) sz = 0.5F * d2.getStepZ();
            sx = sx == 0.0F ? level.random.nextFloat() : sx + 0.5F;
            sy = sy == 0.0F ? level.random.nextFloat() : sy + 0.5F;
            sz = sz == 0.0F ? level.random.nextFloat() : sz + 0.5F;
            thaumcraft.client.fx.EldritchFX.sparkle(pos.getX() + sx, pos.getY() + sy, pos.getZ() + sz, 0.25F, 0.25F, 1.0F);
         }
      }
   }

   @Override
   protected void writeExtra(CompoundTag tag, HolderLookup.Provider registries) {
      tag.put("oldblock", NbtUtils.writeBlockState(oldBlock));
      tag.putShort("countdown", countdown);
      tag.putShort("countdownmax", countdownMax);
      tag.putByte("count", count);
      tag.putByte("direction", direction == null ? -1 : (byte) direction.ordinal());
   }

   @Override
   protected void readExtra(CompoundTag tag, HolderLookup.Provider registries) {
      if (tag.contains("oldblock")) oldBlock = NbtUtils.readBlockState(registries.lookupOrThrow(Registries.BLOCK), tag.getCompound("oldblock"));
      countdown = tag.getShort("countdown");
      countdownMax = tag.getShort("countdownmax");
      count = tag.getByte("count");
      byte db = tag.getByte("direction");
      direction = db >= 0 ? Direction.values()[db] : null;
   }
}
