package thaumcraft.common.tiles.misc;

import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import thaumcraft.api.blocks.BlocksTC;
import thaumcraft.client.fx.FXDispatcher;
import thaumcraft.common.items.casters.foci.FocusEffectRift;

public class TileHole extends TileMemory implements ITickable {
   public short countdown = 0;
   public short countdownmax = 120;
   public byte count = 0;
   public EnumFacing direction = null;

   public TileHole() {
   }

   public TileHole(IBlockState bi, short max, byte count, EnumFacing direction) {
      super(bi);
      this.count = count;
      this.countdownmax = max;
      this.direction = direction;
   }

   public TileHole(byte count) {
      this.count = count;
   }

   public void update() {
      if (this.world.isRemote) {
         for (int a = 0; a < 2; a++) {
            this.surroundwithsparkles();
         }
      } else {
         if (this.countdown == 0 && this.count > 1 && this.direction != null) {
            switch (this.direction.getAxis()) {
               case Y:
                  for (int axx = 0; axx < 9; axx++) {
                     if (axx / 3 != 1 || axx % 3 != 1) {
                        FocusEffectRift.createHole(
                           this.world, this.getPos().add(-1 + axx / 3, 0, -1 + axx % 3), null, (byte)1, this.countdownmax
                        );
                     }
                  }
                  break;
               case Z:
                  for (int ax = 0; ax < 9; ax++) {
                     if (ax / 3 != 1 || ax % 3 != 1) {
                        FocusEffectRift.createHole(
                           this.world, this.getPos().add(-1 + ax / 3, -1 + ax % 3, 0), null, (byte)1, this.countdownmax
                        );
                     }
                  }
                  break;
               case X:
                  for (int a = 0; a < 9; a++) {
                     if (a / 3 != 1 || a % 3 != 1) {
                        FocusEffectRift.createHole(
                           this.world, this.getPos().add(0, -1 + a / 3, -1 + a % 3), null, (byte)1, this.countdownmax
                        );
                     }
                  }
            }

            if (!FocusEffectRift.createHole(
               this.world,
               this.getPos().offset(this.direction.getOpposite()),
               this.direction,
               (byte)(this.count - 1),
               this.countdownmax
            )) {
               this.count = 0;
            }
         }

         this.countdown++;
         if (this.countdown % 20 == 0) {
            this.markDirty();
         }

         if (this.countdown >= this.countdownmax) {
            this.world.setBlockState(this.getPos(), this.oldblock, 3);
         }
      }
   }

   private void surroundwithsparkles() {
      for (EnumFacing d1 : EnumFacing.values()) {
         IBlockState b1 = this.world.getBlockState(this.getPos().offset(d1));
         if (b1.getBlock() != BlocksTC.hole && !b1.isOpaqueCube()) {
            for (EnumFacing d2 : EnumFacing.values()) {
               if (d1.getAxis() != d2.getAxis()
                  && (
                     this.world.getBlockState(this.getPos().offset(d2)).isOpaqueCube()
                        || this.world.getBlockState(this.getPos().offset(d1).offset(d2)).isOpaqueCube()
                  )) {
                  float sx = 0.5F * d1.getXOffset();
                  float sy = 0.5F * d1.getYOffset();
                  float sz = 0.5F * d1.getZOffset();
                  if (sx == 0.0F) {
                     sx = 0.5F * d2.getXOffset();
                  }

                  if (sy == 0.0F) {
                     sy = 0.5F * d2.getYOffset();
                  }

                  if (sz == 0.0F) {
                     sz = 0.5F * d2.getZOffset();
                  }

                  if (sx == 0.0F) {
                     sx = this.world.rand.nextFloat();
                  } else {
                     sx += 0.5F;
                  }

                  if (sy == 0.0F) {
                     sy = this.world.rand.nextFloat();
                  } else {
                     sy += 0.5F;
                  }

                  if (sz == 0.0F) {
                     sz = this.world.rand.nextFloat();
                  } else {
                     sz += 0.5F;
                  }

                  FXDispatcher.INSTANCE
                     .sparkle(
                        this.getPos().getX() + sx,
                        this.getPos().getY() + sy,
                        this.getPos().getZ() + sz,
                        0.25F,
                        0.25F,
                        1.0F
                     );
               }
            }
         }
      }
   }

   @Override
   public void readFromNBT(NBTTagCompound nbttagcompound) {
      super.readFromNBT(nbttagcompound);
      this.countdown = nbttagcompound.getShort("countdown");
      this.countdownmax = nbttagcompound.getShort("countdownmax");
      this.count = nbttagcompound.getByte("count");
      byte db = nbttagcompound.getByte("direction");
      this.direction = db >= 0 ? EnumFacing.values()[db] : null;
   }

   @Override
   public NBTTagCompound writeToNBT(NBTTagCompound nbttagcompound) {
      super.writeToNBT(nbttagcompound);
      nbttagcompound.setShort("countdown", this.countdown);
      nbttagcompound.setShort("countdownmax", this.countdownmax);
      nbttagcompound.setByte("count", this.count);
      nbttagcompound.setByte("direction", this.direction == null ? -1 : (byte)this.direction.ordinal());
      return nbttagcompound;
   }
}
