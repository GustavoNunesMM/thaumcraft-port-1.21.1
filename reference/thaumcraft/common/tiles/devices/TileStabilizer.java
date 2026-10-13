package thaumcraft.common.tiles.devices;

import java.util.List;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import thaumcraft.api.aura.AuraHelper;
import thaumcraft.common.entities.EntityFluxRift;
import thaumcraft.common.lib.utils.BlockStateUtils;
import thaumcraft.common.tiles.TileThaumcraft;

public class TileStabilizer extends TileThaumcraft implements ITickable {
   private int ticks = 0;
   private int delay = 0;
   int lastEnergy = 0;
   protected int energy = 0;
   protected final int capacity = 15;

   @SideOnly(Side.CLIENT)
   public AxisAlignedBB getRenderBoundingBox() {
      return new AxisAlignedBB(
         this.getPos().getX(),
         this.getPos().getY(),
         this.getPos().getZ(),
         this.getPos().getX() + 1,
         this.getPos().getY() + 1.5,
         this.getPos().getZ() + 1
      );
   }

   public void update() {
      if (!this.world.isRemote) {
         this.ticks++;
         if (this.energy < 15 && this.ticks % 20 == 0) {
            this.energy++;
            AuraHelper.polluteAura(this.getWorld(), this.getPos(), 0.25F, true);
            this.markDirty();
            this.syncTile(false);
            this.world.notifyNeighborsOfStateChange(this.getPos(), this.world.getBlockState(this.pos).getBlock(), false);
         }

         if (this.energy > 0 && this.delay <= 0 && this.ticks % 5 == 0) {
            int q = this.energy;
            this.tryAddStability();
            if (q != this.energy) {
               this.markDirty();
               this.syncTile(false);
            }
         }

         if (this.delay > 0) {
            this.delay--;
         }
      }

      if (this.world.isRemote && this.energy != this.lastEnergy) {
         this.world.markBlockRangeForRenderUpdate(this.getPos(), this.getPos());
         this.lastEnergy = this.energy;
      }
   }

   private void tryAddStability() {
      EnumFacing facing = BlockStateUtils.getFacing(this.getBlockMetadata());
      List<EntityFluxRift> targets = this.world
         .getEntitiesWithinAABB(
            EntityFluxRift.class,
            new AxisAlignedBB(
                  this.pos.getX(),
                  this.pos.getY(),
                  this.pos.getZ(),
                  this.pos.getX() + 1,
                  this.pos.getY() + 1,
                  this.pos.getZ() + 1
               )
               .grow(8.0)
         );
      if (targets.size() > 0) {
         for (EntityFluxRift e : targets) {
            if (!e.isDead && e.getStability() != EntityFluxRift.EnumStability.VERY_STABLE && this.mitigate(1)) {
               e.addStability();
               this.delay += 5;
               if (this.energy <= 0) {
                  return;
               }
            }
         }
      }
   }

   @Override
   public void readSyncNBT(NBTTagCompound nbt) {
      this.energy = Math.min(nbt.getInteger("energy"), 15);
   }

   @Override
   public NBTTagCompound writeSyncNBT(NBTTagCompound nbt) {
      nbt.setInteger("energy", this.energy);
      return nbt;
   }

   public int getEnergy() {
      return this.energy;
   }

   public boolean mitigate(int e) {
      if (this.energy >= e) {
         this.energy -= e;
         this.world.notifyNeighborsOfStateChange(this.getPos(), this.world.getBlockState(this.pos).getBlock(), false);
         return true;
      } else {
         return false;
      }
   }
}
