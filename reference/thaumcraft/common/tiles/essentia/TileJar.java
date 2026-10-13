package thaumcraft.common.tiles.essentia;

import java.util.Random;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import thaumcraft.common.tiles.TileThaumcraft;

public class TileJar extends TileThaumcraft implements ITickable {
   protected static Random rand = new Random();

   @SideOnly(Side.CLIENT)
   public AxisAlignedBB getRenderBoundingBox() {
      return new AxisAlignedBB(
         this.getPos().getX(),
         this.getPos().getY(),
         this.getPos().getZ(),
         this.getPos().getX() + 1,
         this.getPos().getY() + 1,
         this.getPos().getZ() + 1
      );
   }

   public void update() {
   }
}
