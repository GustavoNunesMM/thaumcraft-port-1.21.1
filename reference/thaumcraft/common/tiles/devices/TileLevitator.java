package thaumcraft.common.tiles.devices;

import java.util.List;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.passive.EntityHorse;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.EnumFacing.Axis;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.World;
import thaumcraft.api.aura.AuraHelper;
import thaumcraft.client.fx.FXDispatcher;
import thaumcraft.codechicken.lib.raytracer.IndexedCuboid6;
import thaumcraft.codechicken.lib.vec.Cuboid6;
import thaumcraft.codechicken.lib.vec.Vector3;
import thaumcraft.common.lib.utils.BlockStateUtils;
import thaumcraft.common.tiles.TileThaumcraft;

public class TileLevitator extends TileThaumcraft implements ITickable {
   private int[] ranges = new int[]{4, 8, 16, 32};
   private int range = 1;
   private int rangeActual = 0;
   private int counter = 0;
   private int vis = 0;

   public void update() {
      EnumFacing facing = BlockStateUtils.getFacing(this.getBlockMetadata());
      if (this.rangeActual > this.ranges[this.range]) {
         this.rangeActual = 0;
      }

      int p = this.counter % this.ranges[this.range];
      if (this.world.getBlockState(this.pos.offset(facing, 1 + p)).isOpaqueCube()) {
         if (1 + p < this.rangeActual) {
            this.rangeActual = 1 + p;
         }

         this.counter = -1;
      } else if (1 + p > this.rangeActual) {
         this.rangeActual = 1 + p;
      }

      this.counter++;
      if (!this.world.isRemote && this.vis < 10) {
         this.vis = (int)(this.vis + AuraHelper.drainVis(this.world, this.getPos(), 1.0F, false) * 1200.0F);
         this.markDirty();
         this.syncTile(false);
      }

      if (this.rangeActual > 0 && this.vis > 0 && BlockStateUtils.isEnabled(this.getBlockMetadata())) {
         List<Entity> targets = this.world
            .getEntitiesWithinAABB(
               Entity.class,
               new AxisAlignedBB(
                  this.pos.getX() - (facing.getXOffset() < 0 ? this.rangeActual : 0),
                  this.pos.getY() - (facing.getYOffset() < 0 ? this.rangeActual : 0),
                  this.pos.getZ() - (facing.getZOffset() < 0 ? this.rangeActual : 0),
                  this.pos.getX() + 1 + (facing.getXOffset() > 0 ? this.rangeActual : 0),
                  this.pos.getY() + 1 + (facing.getYOffset() > 0 ? this.rangeActual : 0),
                  this.pos.getZ() + 1 + (facing.getZOffset() > 0 ? this.rangeActual : 0)
               )
            );
         boolean lifted = false;
         if (targets.size() > 0) {
            for (Entity e : targets) {
               if (e instanceof EntityItem || e.canBePushed() || e instanceof EntityHorse) {
                  lifted = true;
                  this.drawFXAt(e);
                  this.drawFX(facing, 0.6);
                  if (!e.isSneaking() || facing != EnumFacing.UP) {
                     e.motionX = e.motionX + 0.1F * facing.getXOffset();
                     e.motionY = e.motionY + 0.1F * facing.getYOffset();
                     e.motionZ = e.motionZ + 0.1F * facing.getZOffset();
                     if (facing.getAxis() != Axis.Y && !e.onGround) {
                        if (e.motionY < 0.0) {
                           e.motionY *= 0.9F;
                        }

                        e.motionY += 0.08F;
                     }

                     if (e.motionX > 0.35F) {
                        e.motionX = 0.35F;
                     }

                     if (e.motionY > 0.35F) {
                        e.motionY = 0.35F;
                     }

                     if (e.motionZ > 0.35F) {
                        e.motionZ = 0.35F;
                     }

                     if (e.motionX < -0.35F) {
                        e.motionX = -0.35F;
                     }

                     if (e.motionY < -0.35F) {
                        e.motionY = -0.35F;
                     }

                     if (e.motionZ < -0.35F) {
                        e.motionZ = -0.35F;
                     }
                  } else if (e.motionY < 0.0) {
                     e.motionY *= 0.9F;
                  }

                  e.fallDistance = 0.0F;
                  this.vis = this.vis - this.getCost();
                  if (this.vis <= 0) {
                     break;
                  }
               }
            }
         }

         this.drawFX(facing, 0.1);
         if (lifted && !this.world.isRemote && this.counter % 20 == 0) {
            this.markDirty();
         }
      }
   }

   private void drawFX(EnumFacing facing, double c) {
      if (this.world.isRemote && this.world.rand.nextFloat() < c) {
         float x = this.pos.getX() + 0.25F + this.world.rand.nextFloat() * 0.5F;
         float y = this.pos.getY() + 0.25F + this.world.rand.nextFloat() * 0.5F;
         float z = this.pos.getZ() + 0.25F + this.world.rand.nextFloat() * 0.5F;
         FXDispatcher.INSTANCE.drawLevitatorParticles(x, y, z, facing.getXOffset() / 50.0, facing.getYOffset() / 50.0, facing.getZOffset() / 50.0);
      }
   }

   private void drawFXAt(Entity e) {
      if (this.world.isRemote && this.world.rand.nextFloat() < 0.1F) {
         float x = (float)(e.posX + (this.world.rand.nextFloat() - this.world.rand.nextFloat()) * e.width);
         float y = (float)(e.posY + this.world.rand.nextFloat() * e.height);
         float z = (float)(e.posZ + (this.world.rand.nextFloat() - this.world.rand.nextFloat()) * e.width);
         FXDispatcher.INSTANCE
            .drawLevitatorParticles(
               x,
               y,
               z,
               (this.world.rand.nextFloat() - this.world.rand.nextFloat()) * 0.01,
               (this.world.rand.nextFloat() - this.world.rand.nextFloat()) * 0.01,
               (this.world.rand.nextFloat() - this.world.rand.nextFloat()) * 0.01
            );
      }
   }

   @Override
   public void readSyncNBT(NBTTagCompound nbt) {
      this.range = nbt.getByte("range");
      this.vis = nbt.getInteger("vis");
   }

   @Override
   public NBTTagCompound writeSyncNBT(NBTTagCompound nbt) {
      nbt.setByte("range", (byte)this.range);
      nbt.setInteger("vis", this.vis);
      return nbt;
   }

   public int getCost() {
      return this.ranges[this.range] * 2;
   }

   public void increaseRange(EntityPlayer playerIn) {
      this.rangeActual = 0;
      if (!this.world.isRemote) {
         this.range++;
         if (this.range >= this.ranges.length) {
            this.range = 0;
         }

         this.markDirty();
         this.syncTile(false);
         playerIn.sendMessage(new TextComponentString(String.format(I18n.translateToLocal("tc.levitator"), this.ranges[this.range], this.getCost())));
      }
   }

   public RayTraceResult rayTrace(World world, Vec3d vec3d, Vec3d vec3d1, RayTraceResult fullblock) {
      return fullblock;
   }

   public void addTraceableCuboids(List<IndexedCuboid6> cuboids) {
      EnumFacing facing = BlockStateUtils.getFacing(this.getBlockMetadata());
      cuboids.add(
         new IndexedCuboid6(
            0,
            this.getCuboidByFacing(facing)
               .add(new Vector3(this.getPos().getX(), this.getPos().getY(), this.getPos().getZ()))
         )
      );
   }

   public Cuboid6 getCuboidByFacing(EnumFacing facing) {
      switch (facing) {
         case DOWN:
            return new Cuboid6(0.375, 0.875, 0.375, 0.625, 0.9375, 0.625);
         case EAST:
            return new Cuboid6(0.0625, 0.375, 0.375, 0.125, 0.625, 0.625);
         case WEST:
            return new Cuboid6(0.875, 0.375, 0.375, 0.9375, 0.625, 0.625);
         case SOUTH:
            return new Cuboid6(0.375, 0.375, 0.0625, 0.625, 0.625, 0.125);
         case NORTH:
            return new Cuboid6(0.375, 0.375, 0.875, 0.625, 0.625, 0.9375);
         default:
            return new Cuboid6(0.375, 0.0625, 0.375, 0.625, 0.125, 0.625);
      }
   }
}
