package thaumcraft.common.tiles.essentia;

import java.util.List;
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
import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IAspectContainer;
import thaumcraft.api.aspects.IEssentiaTransport;
import thaumcraft.codechicken.lib.raytracer.IndexedCuboid6;
import thaumcraft.codechicken.lib.raytracer.RayTracer;
import thaumcraft.codechicken.lib.vec.Cuboid6;
import thaumcraft.common.lib.SoundsTC;
import thaumcraft.common.tiles.devices.TileBellows;

public class TileTubeBuffer extends TileTube implements IAspectContainer {
   public AspectList aspects = new AspectList();
   public final int MAXAMOUNT = 10;
   public byte[] chokedSides = new byte[]{0, 0, 0, 0, 0, 0};
   int count = 0;
   int bellows = -1;

   @Override
   public void readSyncNBT(NBTTagCompound nbttagcompound) {
      this.aspects.readFromNBT(nbttagcompound);
      byte[] sides = nbttagcompound.getByteArray("open");
      if (sides != null && sides.length == 6) {
         for (int a = 0; a < 6; a++) {
            this.openSides[a] = sides[a] == 1;
         }
      }

      this.chokedSides = nbttagcompound.getByteArray("choke");
      if (this.chokedSides == null || this.chokedSides.length < 6) {
         this.chokedSides = new byte[]{0, 0, 0, 0, 0, 0};
      }

      this.facing = EnumFacing.VALUES[nbttagcompound.getInteger("side")];
   }

   @Override
   public NBTTagCompound writeSyncNBT(NBTTagCompound nbttagcompound) {
      this.aspects.writeToNBT(nbttagcompound);
      byte[] sides = new byte[6];

      for (int a = 0; a < 6; a++) {
         sides[a] = (byte)(this.openSides[a] ? 1 : 0);
      }

      nbttagcompound.setByteArray("open", sides);
      nbttagcompound.setByteArray("choke", this.chokedSides);
      nbttagcompound.setInteger("side", this.facing.ordinal());
      return nbttagcompound;
   }

   @Override
   public AspectList getAspects() {
      return this.aspects;
   }

   @Override
   public void setAspects(AspectList aspects) {
   }

   @Override
   public int addToContainer(Aspect tt, int am) {
      if (am != 1) {
         return am;
      } else if (this.aspects.visSize() < 10) {
         this.aspects.add(tt, am);
         this.markDirty();
         this.syncTile(false);
         return 0;
      } else {
         return am;
      }
   }

   @Override
   public boolean takeFromContainer(Aspect tt, int am) {
      if (this.aspects.getAmount(tt) >= am) {
         this.aspects.remove(tt, am);
         this.markDirty();
         this.syncTile(false);
         return true;
      } else {
         return false;
      }
   }

   @Override
   public boolean takeFromContainer(AspectList ot) {
      return false;
   }

   @Override
   public boolean doesContainerContainAmount(Aspect tag, int amt) {
      return this.aspects.getAmount(tag) >= amt;
   }

   @Override
   public boolean doesContainerContain(AspectList ot) {
      return false;
   }

   @Override
   public int containerContains(Aspect tag) {
      return this.aspects.getAmount(tag);
   }

   @Override
   public boolean doesContainerAccept(Aspect tag) {
      return true;
   }

   @Override
   public boolean isConnectable(EnumFacing face) {
      return this.openSides[face.ordinal()];
   }

   @Override
   public boolean canInputFrom(EnumFacing face) {
      return this.openSides[face.ordinal()];
   }

   @Override
   public boolean canOutputTo(EnumFacing face) {
      return this.openSides[face.ordinal()];
   }

   @Override
   public void setSuction(Aspect aspect, int amount) {
   }

   @Override
   public int getMinimumSuction() {
      return 0;
   }

   @Override
   public Aspect getSuctionType(EnumFacing loc) {
      return null;
   }

   @Override
   public int getSuctionAmount(EnumFacing loc) {
      return this.chokedSides[loc.ordinal()] == 2 ? 0 : (this.bellows > 0 && this.chokedSides[loc.ordinal()] != 1 ? this.bellows * 32 : 1);
   }

   @Override
   public Aspect getEssentiaType(EnumFacing loc) {
      return this.aspects.size() > 0 ? this.aspects.getAspects()[this.world.rand.nextInt(this.aspects.getAspects().length)] : null;
   }

   @Override
   public int getEssentiaAmount(EnumFacing loc) {
      return this.aspects.visSize();
   }

   @Override
   public int takeEssentia(Aspect aspect, int amount, EnumFacing face) {
      if (!this.canOutputTo(face)) {
         return 0;
      } else {
         TileEntity te = null;
         IEssentiaTransport ic = null;
         int suction = 0;
         te = ThaumcraftApiHelper.getConnectableTile(this.world, this.pos, face);
         if (te != null) {
            ic = (IEssentiaTransport)te;
            suction = ic.getSuctionAmount(face.getOpposite());
         }

         for (EnumFacing dir : EnumFacing.VALUES) {
            if (this.canOutputTo(dir) && dir != face) {
               te = ThaumcraftApiHelper.getConnectableTile(this.world, this.pos, dir);
               if (te != null) {
                  ic = (IEssentiaTransport)te;
                  int sa = ic.getSuctionAmount(dir.getOpposite());
                  Aspect su = ic.getSuctionType(dir.getOpposite());
                  if ((su == aspect || su == null) && suction < sa && this.getSuctionAmount(dir) < sa) {
                     return 0;
                  }
               }
            }
         }

         if (amount > this.aspects.getAmount(aspect)) {
            amount = this.aspects.getAmount(aspect);
         }

         return this.takeFromContainer(aspect, amount) ? amount : 0;
      }
   }

   @Override
   public int addEssentia(Aspect aspect, int amount, EnumFacing face) {
      return this.canInputFrom(face) ? amount - this.addToContainer(aspect, amount) : 0;
   }

   @Override
   public void update() {
      this.count++;
      if (this.bellows < 0 || this.count % 20 == 0) {
         this.getBellows();
      }

      if (!this.world.isRemote && this.count % 5 == 0 && this.aspects.visSize() < 10) {
         this.fillBuffer();
      }
   }

   void fillBuffer() {
      TileEntity te = null;
      IEssentiaTransport ic = null;

      for (EnumFacing dir : EnumFacing.VALUES) {
         if (this.canInputFrom(dir)) {
            te = ThaumcraftApiHelper.getConnectableTile(this.world, this.pos, dir);
            if (te != null) {
               ic = (IEssentiaTransport)te;
               if (ic.getEssentiaAmount(dir.getOpposite()) > 0
                  && ic.getSuctionAmount(dir.getOpposite()) < this.getSuctionAmount(dir)
                  && this.getSuctionAmount(dir) >= ic.getMinimumSuction()) {
                  Aspect ta = ic.getEssentiaType(dir.getOpposite());
                  this.addToContainer(ta, ic.takeEssentia(ta, 1, dir.getOpposite()));
                  return;
               }
            }
         }
      }
   }

   public void getBellows() {
      this.bellows = TileBellows.getBellows(this.world, this.pos, EnumFacing.VALUES);
   }

   @Override
   public boolean onCasterRightClick(World world, ItemStack wandstack, EntityPlayer player, BlockPos bp, EnumFacing side, EnumHand hand) {
      RayTraceResult hit = RayTracer.retraceBlock(world, player, this.pos);
      if (hit == null) {
         return false;
      } else if (hit.subHit >= 0 && hit.subHit < 6) {
         player.swingArm(hand);
         if (player.isSneaking()) {
            player.world
               .playSound(
                  bp.getX() + 0.5,
                  bp.getY() + 0.5,
                  bp.getZ() + 0.5,
                  SoundsTC.squeek,
                  SoundCategory.BLOCKS,
                  0.6F,
                  2.0F + world.rand.nextFloat() * 0.2F,
                  false
               );
            if (!world.isRemote) {
               this.chokedSides[hit.subHit]++;
               if (this.chokedSides[hit.subHit] > 2) {
                  this.chokedSides[hit.subHit] = 0;
               }

               this.markDirty();
               this.syncTile(true);
            }
         } else {
            player.world
               .playSound(
                  bp.getX() + 0.5,
                  bp.getY() + 0.5,
                  bp.getZ() + 0.5,
                  SoundsTC.tool,
                  SoundCategory.BLOCKS,
                  0.5F,
                  0.9F + player.world.rand.nextFloat() * 0.2F,
                  false
               );
            this.openSides[hit.subHit] = !this.openSides[hit.subHit];
            EnumFacing dir = EnumFacing.VALUES[hit.subHit];
            TileEntity tile = world.getTileEntity(this.pos.offset(dir));
            if (tile != null && tile instanceof TileTube) {
               ((TileTube)tile).openSides[dir.getOpposite().ordinal()] = this.openSides[hit.subHit];
               ((TileTube)tile).syncTile(true);
               tile.markDirty();
            }

            this.markDirty();
            this.syncTile(true);
         }

         return true;
      } else {
         return false;
      }
   }

   @Override
   public boolean canConnectSide(EnumFacing side) {
      TileEntity tile = this.world.getTileEntity(this.pos.offset(side));
      return tile != null && tile instanceof IEssentiaTransport;
   }

   @Override
   public void addTraceableCuboids(List<IndexedCuboid6> cuboids) {
      float min = 0.375F;
      float max = 0.625F;
      if (this.canConnectSide(EnumFacing.DOWN)) {
         cuboids.add(
            new IndexedCuboid6(
               0,
               new Cuboid6(
                  this.pos.getX() + min,
                  this.pos.getY(),
                  this.pos.getZ() + min,
                  this.pos.getX() + max,
                  this.pos.getY() + 0.5,
                  this.pos.getZ() + max
               )
            )
         );
      }

      if (this.canConnectSide(EnumFacing.UP)) {
         cuboids.add(
            new IndexedCuboid6(
               1,
               new Cuboid6(
                  this.pos.getX() + min,
                  this.pos.getY() + 0.5,
                  this.pos.getZ() + min,
                  this.pos.getX() + max,
                  this.pos.getY() + 1,
                  this.pos.getZ() + max
               )
            )
         );
      }

      if (this.canConnectSide(EnumFacing.NORTH)) {
         cuboids.add(
            new IndexedCuboid6(
               2,
               new Cuboid6(
                  this.pos.getX() + min,
                  this.pos.getY() + min,
                  this.pos.getZ(),
                  this.pos.getX() + max,
                  this.pos.getY() + max,
                  this.pos.getZ() + 0.5
               )
            )
         );
      }

      if (this.canConnectSide(EnumFacing.SOUTH)) {
         cuboids.add(
            new IndexedCuboid6(
               3,
               new Cuboid6(
                  this.pos.getX() + min,
                  this.pos.getY() + min,
                  this.pos.getZ() + 0.5,
                  this.pos.getX() + max,
                  this.pos.getY() + max,
                  this.pos.getZ() + 1
               )
            )
         );
      }

      if (this.canConnectSide(EnumFacing.WEST)) {
         cuboids.add(
            new IndexedCuboid6(
               4,
               new Cuboid6(
                  this.pos.getX(),
                  this.pos.getY() + min,
                  this.pos.getZ() + min,
                  this.pos.getX() + 0.5,
                  this.pos.getY() + max,
                  this.pos.getZ() + max
               )
            )
         );
      }

      if (this.canConnectSide(EnumFacing.EAST)) {
         cuboids.add(
            new IndexedCuboid6(
               5,
               new Cuboid6(
                  this.pos.getX() + 0.5,
                  this.pos.getY() + min,
                  this.pos.getZ() + min,
                  this.pos.getX() + 1,
                  this.pos.getY() + max,
                  this.pos.getZ() + max
               )
            )
         );
      }

      cuboids.add(
         new IndexedCuboid6(
            6,
            new Cuboid6(
               this.pos.getX() + 0.25F,
               this.pos.getY() + 0.25F,
               this.pos.getZ() + 0.25F,
               this.pos.getX() + 0.75F,
               this.pos.getY() + 0.75F,
               this.pos.getZ() + 0.75F
            )
         )
      );
   }
}
