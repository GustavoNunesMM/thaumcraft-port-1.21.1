package thaumcraft.api.golems.seals;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

public class SealPos {
   public final BlockPos pos;
   public final Direction face;

   public SealPos(BlockPos pos, Direction face) {
      this.pos = pos.immutable();
      this.face = face;
   }

   @Override
   public int hashCode() {
      byte b0 = (byte) (face.ordinal() + 1);
      int i = 31 * b0 + pos.getX();
      i = 31 * i + pos.getY();
      return 31 * i + pos.getZ();
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) return true;
      return o instanceof SealPos sp && pos.equals(sp.pos) && face == sp.face;
   }

   @Override
   public String toString() {
      return pos.toShortString() + "/" + face.getName();
   }
}
