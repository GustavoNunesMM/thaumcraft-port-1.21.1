package thaumcraft.common.golems.seals;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import thaumcraft.api.golems.seals.ISeal;
import thaumcraft.api.golems.seals.ISealConfigArea;
import thaumcraft.api.golems.seals.ISealConfigToggles;
import thaumcraft.api.golems.seals.ISealEntity;
import thaumcraft.api.golems.seals.SealPos;
import thaumcraft.api.golems.tasks.Task;
import thaumcraft.common.golems.tasks.TaskHandler;
import thaumcraft.common.lib.network.PacketHandler;
import thaumcraft.common.lib.network.misc.PacketSealToClient;

public class SealEntity implements ISealEntity {
   SealPos sealPos;
   ISeal seal;
   byte priority = 0;
   byte color = 0;
   boolean locked = false;
   boolean redstone = false;
   String owner = "";
   boolean stopped = false;
   private BlockPos area = new BlockPos(1, 1, 1);

   public SealEntity() {
   }

   public SealEntity(World world, SealPos sealPos, ISeal seal) {
      this.sealPos = sealPos;
      this.seal = seal;
      if (seal instanceof ISealConfigArea) {
         int x = sealPos.face.getXOffset() == 0 ? 3 : 1;
         int y = sealPos.face.getYOffset() == 0 ? 3 : 1;
         int z = sealPos.face.getZOffset() == 0 ? 3 : 1;
         this.area = new BlockPos(x, y, z);
      }
   }

   @Override
   public void tickSealEntity(World world) {
      if (this.seal != null) {
         if (this.isStoppedByRedstone(world)) {
            if (!this.stopped) {
               for (Task t : TaskHandler.getTasks(world.provider.getDimension()).values()) {
                  if (t.getSealPos() != null && t.getSealPos().equals(this.sealPos)) {
                     t.setSuspended(true);
                  }
               }
            }

            this.stopped = true;
            return;
         }

         this.stopped = false;
         this.seal.tickSeal(world, this);
      }
   }

   @Override
   public boolean isStoppedByRedstone(World world) {
      return this.isRedstoneSensitive()
         && (world.isBlockPowered(this.getSealPos().pos) || world.isBlockPowered(this.getSealPos().pos.offset(this.getSealPos().face)));
   }

   @Override
   public ISeal getSeal() {
      return this.seal;
   }

   @Override
   public SealPos getSealPos() {
      return this.sealPos;
   }

   @Override
   public byte getPriority() {
      return this.priority;
   }

   @Override
   public void setPriority(byte priority) {
      this.priority = priority;
   }

   @Override
   public byte getColor() {
      return this.color;
   }

   @Override
   public void setColor(byte color) {
      this.color = color;
   }

   @Override
   public String getOwner() {
      return this.owner;
   }

   @Override
   public void setOwner(String owner) {
      this.owner = owner;
   }

   @Override
   public boolean isLocked() {
      return this.locked;
   }

   @Override
   public void setLocked(boolean locked) {
      this.locked = locked;
   }

   @Override
   public boolean isRedstoneSensitive() {
      return this.redstone;
   }

   @Override
   public void setRedstoneSensitive(boolean redstone) {
      this.redstone = redstone;
   }

   @Override
   public void readNBT(NBTTagCompound nbt) {
      BlockPos p = BlockPos.fromLong(nbt.getLong("pos"));
      EnumFacing face = EnumFacing.VALUES[nbt.getByte("face")];
      this.sealPos = new SealPos(p, face);
      this.setPriority(nbt.getByte("priority"));
      this.setColor(nbt.getByte("color"));
      this.setLocked(nbt.getBoolean("locked"));
      this.setRedstoneSensitive(nbt.getBoolean("redstone"));
      this.setOwner(nbt.getString("owner"));

      try {
         this.seal = (ISeal)SealHandler.getSeal(nbt.getString("type")).getClass().newInstance();
      } catch (Exception var8) {
      }

      if (this.seal != null) {
         this.seal.readCustomNBT(nbt);
         if (this.seal instanceof ISealConfigArea) {
            this.area = BlockPos.fromLong(nbt.getLong("area"));
         }

         if (this.seal instanceof ISealConfigToggles) {
            for (ISealConfigToggles.SealToggle prop : ((ISealConfigToggles)this.seal).getToggles()) {
               if (nbt.hasKey(prop.getKey())) {
                  prop.setValue(nbt.getBoolean(prop.getKey()));
               }
            }
         }
      }
   }

   @Override
   public NBTTagCompound writeNBT() {
      NBTTagCompound nbt = new NBTTagCompound();
      nbt.setLong("pos", this.sealPos.pos.toLong());
      nbt.setByte("face", (byte)this.sealPos.face.ordinal());
      nbt.setString("type", this.seal.getKey());
      nbt.setByte("priority", this.getPriority());
      nbt.setByte("color", this.getColor());
      nbt.setBoolean("locked", this.isLocked());
      nbt.setBoolean("redstone", this.isRedstoneSensitive());
      nbt.setString("owner", this.getOwner());
      if (this.seal != null) {
         this.seal.writeCustomNBT(nbt);
         if (this.seal instanceof ISealConfigArea) {
            nbt.setLong("area", this.area.toLong());
         }

         if (this.seal instanceof ISealConfigToggles) {
            for (ISealConfigToggles.SealToggle prop : ((ISealConfigToggles)this.seal).getToggles()) {
               nbt.setBoolean(prop.getKey(), prop.getValue());
            }
         }
      }

      return nbt;
   }

   @Override
   public void syncToClient(World world) {
      if (!world.isRemote) {
         PacketHandler.INSTANCE.sendToDimension(new PacketSealToClient(this), world.provider.getDimension());
      }
   }

   @Override
   public BlockPos getArea() {
      return this.area;
   }

   @Override
   public void setArea(BlockPos v) {
      this.area = v;
   }
}
