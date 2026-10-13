package thaumcraft.common.tiles.essentia;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.codechicken.lib.raytracer.RayTracer;
import thaumcraft.common.lib.SoundsTC;

public class TileTubeValve extends TileTube {
   public boolean allowFlow = true;
   boolean wasPoweredLastTick = false;
   public float rotation = 0.0F;

   @Override
   public void update() {
      if (!this.world.isRemote && this.count % 5 == 0) {
         boolean gettingPower = this.gettingPower();
         if (this.wasPoweredLastTick && !gettingPower && !this.allowFlow) {
            this.allowFlow = true;
            this.world
               .playSound(
                  null, this.pos, SoundsTC.squeek, SoundCategory.BLOCKS, 0.7F, 0.9F + this.world.rand.nextFloat() * 0.2F
               );
            this.syncTile(true);
            this.markDirty();
         }

         if (!this.wasPoweredLastTick && gettingPower && this.allowFlow) {
            this.allowFlow = false;
            this.world
               .playSound(
                  null, this.pos, SoundsTC.squeek, SoundCategory.BLOCKS, 0.7F, 0.9F + this.world.rand.nextFloat() * 0.2F
               );
            this.syncTile(true);
            this.markDirty();
         }

         this.wasPoweredLastTick = gettingPower;
      }

      if (this.world.isRemote) {
         if (!this.allowFlow && this.rotation < 360.0F) {
            this.rotation += 20.0F;
         } else if (this.allowFlow && this.rotation > 0.0F) {
            this.rotation -= 20.0F;
         }
      }

      super.update();
   }

   @Override
   public boolean onCasterRightClick(World world, ItemStack wandstack, EntityPlayer player, BlockPos bp, EnumFacing side, EnumHand hand) {
      RayTraceResult hit = RayTracer.retraceBlock(world, player, this.pos);
      if (hit == null) {
         return false;
      } else if (hit.subHit >= 0 && hit.subHit < 6) {
         player.world
            .playSound(
               this.pos.getX(),
               this.pos.getY(),
               this.pos.getZ(),
               SoundsTC.tool,
               SoundCategory.BLOCKS,
               0.5F,
               0.9F + player.world.rand.nextFloat() * 0.2F,
               false
            );
         player.swingArm(hand);
         this.markDirty();
         this.syncTile(true);
         this.openSides[hit.subHit] = !this.openSides[hit.subHit];
         EnumFacing dir = EnumFacing.VALUES[hit.subHit];
         TileEntity tile = world.getTileEntity(this.pos.offset(dir));
         if (tile != null && tile instanceof TileTube) {
            ((TileTube)tile).openSides[dir.getOpposite().ordinal()] = this.openSides[hit.subHit];
            this.syncTile(true);
            tile.markDirty();
         }

         return true;
      } else if (hit.subHit != 6) {
         return false;
      } else {
         player.world
            .playSound(
               this.pos.getX(),
               this.pos.getY(),
               this.pos.getZ(),
               SoundsTC.tool,
               SoundCategory.BLOCKS,
               0.5F,
               0.9F + player.world.rand.nextFloat() * 0.2F,
               false
            );
         player.swingArm(hand);
         int a = this.facing.ordinal();
         this.markDirty();

         while (++a < 20) {
            if (!this.canConnectSide(EnumFacing.VALUES[a % 6])) {
               a %= 6;
               this.facing = EnumFacing.VALUES[a];
               this.syncTile(true);
               this.markDirty();
               break;
            }
         }

         return true;
      }
   }

   @Override
   public void readSyncNBT(NBTTagCompound nbttagcompound) {
      super.readSyncNBT(nbttagcompound);
      this.allowFlow = nbttagcompound.getBoolean("flow");
      this.wasPoweredLastTick = nbttagcompound.getBoolean("hadpower");
   }

   @Override
   public NBTTagCompound writeSyncNBT(NBTTagCompound nbttagcompound) {
      nbttagcompound = super.writeSyncNBT(nbttagcompound);
      nbttagcompound.setBoolean("flow", this.allowFlow);
      nbttagcompound.setBoolean("hadpower", this.wasPoweredLastTick);
      return nbttagcompound;
   }

   @Override
   public boolean isConnectable(EnumFacing face) {
      return face != this.facing && super.isConnectable(face);
   }

   @Override
   public void setSuction(Aspect aspect, int amount) {
      if (this.allowFlow) {
         super.setSuction(aspect, amount);
      }
   }

   @Override
   public boolean gettingPower() {
      return this.world.getRedstonePowerFromNeighbors(this.pos) > 0;
   }
}
