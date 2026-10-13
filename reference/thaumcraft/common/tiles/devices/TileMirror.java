package thaumcraft.common.tiles.devices;

import java.util.ArrayList;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.fml.common.FMLCommonHandler;
import thaumcraft.api.aura.AuraHelper;
import thaumcraft.client.fx.FXDispatcher;
import thaumcraft.common.lib.utils.BlockStateUtils;
import thaumcraft.common.lib.utils.Utils;
import thaumcraft.common.tiles.TileThaumcraft;

public class TileMirror extends TileThaumcraft implements IInventory, ITickable {
   public boolean linked = false;
   public int linkX;
   public int linkY;
   public int linkZ;
   public int linkDim;
   public int instability;
   int count = 0;
   int inc = 40;
   private ArrayList<ItemStack> outputStacks = new ArrayList<>();

   public void restoreLink() {
      if (this.isDestinationValid()) {
         World targetWorld = FMLCommonHandler.instance().getMinecraftServerInstance().getWorld(this.linkDim);
         if (targetWorld == null) {
            return;
         }

         TileEntity te = targetWorld.getTileEntity(new BlockPos(this.linkX, this.linkY, this.linkZ));
         if (te != null && te instanceof TileMirror) {
            TileMirror tm = (TileMirror)te;
            tm.linked = true;
            tm.linkX = this.getPos().getX();
            tm.linkY = this.getPos().getY();
            tm.linkZ = this.getPos().getZ();
            tm.linkDim = this.world.provider.getDimension();
            tm.syncTile(false);
            this.linked = true;
            this.markDirty();
            tm.markDirty();
            this.syncTile(false);
         }
      }
   }

   public void invalidateLink() {
      World targetWorld = DimensionManager.getWorld(this.linkDim);
      if (targetWorld != null) {
         if (Utils.isChunkLoaded(targetWorld, this.linkX, this.linkZ)) {
            TileEntity te = targetWorld.getTileEntity(new BlockPos(this.linkX, this.linkY, this.linkZ));
            if (te != null && te instanceof TileMirror) {
               TileMirror tm = (TileMirror)te;
               tm.linked = false;
               this.markDirty();
               tm.markDirty();
               tm.syncTile(false);
            }
         }
      }
   }

   public boolean isLinkValid() {
      if (!this.linked) {
         return false;
      } else {
         World targetWorld = DimensionManager.getWorld(this.linkDim);
         if (targetWorld == null) {
            return false;
         } else {
            TileEntity te = targetWorld.getTileEntity(new BlockPos(this.linkX, this.linkY, this.linkZ));
            if (te != null && te instanceof TileMirror) {
               TileMirror tm = (TileMirror)te;
               if (!tm.linked) {
                  this.linked = false;
                  this.markDirty();
                  this.syncTile(false);
                  return false;
               } else if (tm.linkX == this.getPos().getX()
                  && tm.linkY == this.getPos().getY()
                  && tm.linkZ == this.getPos().getZ()
                  && tm.linkDim == this.world.provider.getDimension()) {
                  return true;
               } else {
                  this.linked = false;
                  this.markDirty();
                  this.syncTile(false);
                  return false;
               }
            } else {
               this.linked = false;
               this.markDirty();
               this.syncTile(false);
               return false;
            }
         }
      }
   }

   public boolean isLinkValidSimple() {
      if (!this.linked) {
         return false;
      } else {
         World targetWorld = DimensionManager.getWorld(this.linkDim);
         if (targetWorld == null) {
            return false;
         } else {
            TileEntity te = targetWorld.getTileEntity(new BlockPos(this.linkX, this.linkY, this.linkZ));
            if (te != null && te instanceof TileMirror) {
               TileMirror tm = (TileMirror)te;
               return !tm.linked
                  ? false
                  : tm.linkX == this.getPos().getX()
                     && tm.linkY == this.getPos().getY()
                     && tm.linkZ == this.getPos().getZ()
                     && tm.linkDim == this.world.provider.getDimension();
            } else {
               return false;
            }
         }
      }
   }

   public boolean isDestinationValid() {
      World targetWorld = DimensionManager.getWorld(this.linkDim);
      if (targetWorld == null) {
         return false;
      } else {
         TileEntity te = targetWorld.getTileEntity(new BlockPos(this.linkX, this.linkY, this.linkZ));
         if (te != null && te instanceof TileMirror) {
            TileMirror tm = (TileMirror)te;
            return !tm.isLinkValid();
         } else {
            this.linked = false;
            this.markDirty();
            this.syncTile(false);
            return false;
         }
      }
   }

   public boolean transport(EntityItem ie) {
      ItemStack items = ie.getItem();
      if (this.linked && this.isLinkValid()) {
         World world = FMLCommonHandler.instance().getMinecraftServerInstance().getWorld(this.linkDim);
         TileEntity target = world.getTileEntity(new BlockPos(this.linkX, this.linkY, this.linkZ));
         if (target != null && target instanceof TileMirror) {
            ((TileMirror)target).addStack(items);
            this.addInstability(null, items.getCount());
            ie.setDead();
            this.markDirty();
            target.markDirty();
            world.addBlockEvent(this.getPos(), this.blockType, 1, 0);
            return true;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   public boolean transportDirect(ItemStack items) {
      if (items != null && !items.isEmpty() && items.getCount() > 0) {
         this.addStack(items.copy());
         this.markDirty();
         return true;
      } else {
         return false;
      }
   }

   public void eject() {
      if (this.outputStacks.size() > 0 && this.count > 20) {
         int i = this.world.rand.nextInt(this.outputStacks.size());
         if (this.outputStacks.get(i) != null && !this.outputStacks.get(i).isEmpty()) {
            ItemStack outItem = this.outputStacks.get(i).copy();
            outItem.setCount(1);
            if (this.spawnItem(outItem)) {
               this.outputStacks.get(i).shrink(1);
               this.addInstability(null, 1);
               this.world.addBlockEvent(this.getPos(), this.blockType, 1, 0);
               if (this.outputStacks.get(i).getCount() <= 0) {
                  this.outputStacks.remove(i);
               }
            }
         } else {
            this.outputStacks.remove(i);
         }

         this.markDirty();
      }
   }

   public boolean spawnItem(ItemStack stack) {
      try {
         EnumFacing face = BlockStateUtils.getFacing(this.getBlockMetadata());
         EntityItem ie2 = new EntityItem(
            this.world,
            this.getPos().getX() + 0.5,
            this.getPos().getY() + 0.25,
            this.getPos().getZ() + 0.5,
            stack
         );
         ie2.motionX = face.getXOffset() * 0.15F;
         ie2.motionY = face.getYOffset() * 0.15F;
         ie2.motionZ = face.getZOffset() * 0.15F;
         ie2.timeUntilPortal = 20;
         this.world.spawnEntity(ie2);
         return true;
      } catch (Exception var4) {
         return false;
      }
   }

   protected void addInstability(World targetWorld, int amt) {
      this.instability += amt;
      this.markDirty();
      if (targetWorld != null) {
         TileEntity te = targetWorld.getTileEntity(new BlockPos(this.linkX, this.linkY, this.linkZ));
         if (te != null && te instanceof TileMirror) {
            ((TileMirror)te).instability += amt;
            if (((TileMirror)te).instability < 0) {
               ((TileMirror)te).instability = 0;
            }

            te.markDirty();
         }
      }
   }

   @Override
   public void readSyncNBT(NBTTagCompound nbttagcompound) {
      super.readSyncNBT(nbttagcompound);
      this.linked = nbttagcompound.getBoolean("linked");
      this.linkX = nbttagcompound.getInteger("linkX");
      this.linkY = nbttagcompound.getInteger("linkY");
      this.linkZ = nbttagcompound.getInteger("linkZ");
      this.linkDim = nbttagcompound.getInteger("linkDim");
      this.instability = nbttagcompound.getInteger("instability");
   }

   @Override
   public NBTTagCompound writeSyncNBT(NBTTagCompound nbttagcompound) {
      super.writeSyncNBT(nbttagcompound);
      nbttagcompound.setBoolean("linked", this.linked);
      nbttagcompound.setInteger("linkX", this.linkX);
      nbttagcompound.setInteger("linkY", this.linkY);
      nbttagcompound.setInteger("linkZ", this.linkZ);
      nbttagcompound.setInteger("linkDim", this.linkDim);
      nbttagcompound.setInteger("instability", this.instability);
      return nbttagcompound;
   }

   public boolean receiveClientEvent(int i, int j) {
      if (i == 1) {
         if (this.world.isRemote) {
            EnumFacing face = BlockStateUtils.getFacing(this.getBlockMetadata());
            double xx = this.getPos().getX() + 0.33 + this.world.rand.nextFloat() * 0.33F - face.getXOffset() / 2.0;
            double yy = this.getPos().getY() + 0.33 + this.world.rand.nextFloat() * 0.33F - face.getYOffset() / 2.0;
            double zz = this.getPos().getZ() + 0.33 + this.world.rand.nextFloat() * 0.33F - face.getZOffset() / 2.0;
            FXDispatcher.INSTANCE
               .drawWispyMotes(
                  xx,
                  yy,
                  zz,
                  face.getXOffset() / 50.0 + this.world.rand.nextGaussian() * 0.01,
                  face.getYOffset() / 50.0 + this.world.rand.nextGaussian() * 0.01,
                  face.getZOffset() / 50.0 + this.world.rand.nextGaussian() * 0.01,
                  MathHelper.getInt(this.world.rand, 10, 30),
                  this.world.rand.nextFloat() / 3.0F,
                  0.0F,
                  this.world.rand.nextFloat() / 2.0F,
                  (float)(this.world.rand.nextGaussian() * 0.01)
               );
         }

         return true;
      } else {
         return super.receiveClientEvent(i, j);
      }
   }

   public void update() {
      if (!this.world.isRemote) {
         this.eject();
         this.checkInstability();
         if (this.count++ % this.inc == 0) {
            if (!this.isLinkValidSimple()) {
               if (this.inc < 600) {
                  this.inc += 20;
               }

               this.restoreLink();
            } else {
               this.inc = 40;
            }
         }
      }
   }

   public void checkInstability() {
      if (this.instability > 128) {
         AuraHelper.polluteAura(this.world, this.pos, 1.0F, true);
         this.instability -= 128;
         this.markDirty();
      }

      if (this.instability > 0 && this.count % 100 == 0) {
         this.instability--;
      }
   }

   @Override
   public void readFromNBT(NBTTagCompound nbtCompound) {
      super.readFromNBT(nbtCompound);
      NBTTagList nbttaglist = nbtCompound.getTagList("Items", 10);
      this.outputStacks = new ArrayList<>();

      for (int i = 0; i < nbttaglist.tagCount(); i++) {
         NBTTagCompound nbttagcompound1 = nbttaglist.getCompoundTagAt(i);
         byte b0 = nbttagcompound1.getByte("Slot");
         this.outputStacks.add(new ItemStack(nbttagcompound1));
      }
   }

   @Override
   public NBTTagCompound writeToNBT(NBTTagCompound nbtCompound) {
      super.writeToNBT(nbtCompound);
      NBTTagList nbttaglist = new NBTTagList();

      for (int i = 0; i < this.outputStacks.size(); i++) {
         if (this.outputStacks.get(i) != null && this.outputStacks.get(i).getCount() > 0) {
            NBTTagCompound nbttagcompound1 = new NBTTagCompound();
            nbttagcompound1.setByte("Slot", (byte)i);
            this.outputStacks.get(i).writeToNBT(nbttagcompound1);
            nbttaglist.appendTag(nbttagcompound1);
         }
      }

      nbtCompound.setTag("Items", nbttaglist);
      return nbtCompound;
   }

   public int getSizeInventory() {
      return 1;
   }

   public ItemStack getStackInSlot(int par1) {
      return ItemStack.EMPTY;
   }

   public ItemStack decrStackSize(int par1, int par2) {
      return ItemStack.EMPTY;
   }

   public ItemStack removeStackFromSlot(int par1) {
      return ItemStack.EMPTY;
   }

   public void addStack(ItemStack stack) {
      this.outputStacks.add(stack);
      this.markDirty();
   }

   public void setInventorySlotContents(int par1, ItemStack stack2) {
      World world = FMLCommonHandler.instance().getMinecraftServerInstance().getWorld(this.linkDim);
      TileEntity target = world.getTileEntity(new BlockPos(this.linkX, this.linkY, this.linkZ));
      if (target != null && target instanceof TileMirror) {
         ((TileMirror)target).addStack(stack2.copy());
         this.addInstability(null, stack2.getCount());
         world.addBlockEvent(this.getPos(), this.blockType, 1, 0);
      } else {
         this.spawnItem(stack2.copy());
      }
   }

   public int getInventoryStackLimit() {
      return 64;
   }

   public boolean isUsableByPlayer(EntityPlayer var1) {
      return false;
   }

   public boolean isItemValidForSlot(int var1, ItemStack var2) {
      World world = FMLCommonHandler.instance().getMinecraftServerInstance().getWorld(this.linkDim);
      TileEntity target = world.getTileEntity(new BlockPos(this.linkX, this.linkY, this.linkZ));
      return target != null && target instanceof TileMirror;
   }

   public String getName() {
      return null;
   }

   public boolean hasCustomName() {
      return false;
   }

   public ITextComponent getDisplayName() {
      return null;
   }

   public void openInventory(EntityPlayer player) {
   }

   public void closeInventory(EntityPlayer player) {
   }

   public int getField(int id) {
      return 0;
   }

   public void setField(int id, int value) {
   }

   public int getFieldCount() {
      return 0;
   }

   public void clear() {
   }

   public boolean isEmpty() {
      return false;
   }
}
