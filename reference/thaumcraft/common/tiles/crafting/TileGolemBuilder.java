package thaumcraft.common.tiles.crafting;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagLong;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.IEssentiaTransport;
import thaumcraft.api.golems.IGolemProperties;
import thaumcraft.api.items.ItemsTC;
import thaumcraft.client.fx.FXDispatcher;
import thaumcraft.common.container.ContainerGolemBuilder;
import thaumcraft.common.golems.GolemProperties;
import thaumcraft.common.golems.ItemGolemPlacer;
import thaumcraft.common.lib.SoundsTC;
import thaumcraft.common.lib.utils.InventoryUtils;
import thaumcraft.common.tiles.TileThaumcraftInventory;

public class TileGolemBuilder extends TileThaumcraftInventory implements IEssentiaTransport {
   public long golem = -1L;
   public int cost = 0;
   public int maxCost = 0;
   public boolean[] hasStuff = null;
   boolean bufferedEssentia = false;
   int ticks = 0;
   public int press = 0;
   IGolemProperties props = null;
   ItemStack[] components = null;

   public TileGolemBuilder() {
      super(1);
   }

   @Override
   public void messageFromClient(NBTTagCompound nbt, EntityPlayerMP player) {
      super.messageFromClient(nbt, player);
      if (nbt.hasKey("check")) {
         this.hasStuff = this.checkCraft(nbt.getLong("golem"));
         byte[] ba = new byte[this.hasStuff.length];

         for (int a = 0; a < ba.length; a++) {
            ba[a] = (byte)(this.hasStuff[a] ? 1 : 0);
         }

         NBTTagCompound nbt2 = new NBTTagCompound();
         nbt2.setByteArray("stuff", ba);
         this.sendMessageToClient(nbt2, player);
      } else if (nbt.hasKey("golem")) {
         this.startCraft(nbt.getLong("golem"), player);
      }
   }

   @Override
   public void messageFromServer(NBTTagCompound nbt) {
      super.messageFromServer(nbt);
      if (nbt.hasKey("stuff")) {
         this.hasStuff = null;
         byte[] ba = nbt.getByteArray("stuff");
         if (ba != null && ba.length > 0) {
            this.hasStuff = new boolean[ba.length];

            for (int a = 0; a < ba.length; a++) {
               this.hasStuff[a] = ba[a] == 1;
            }
         }

         ContainerGolemBuilder.redo = true;
      }
   }

   @Override
   public void readSyncNBT(NBTTagCompound nbttagcompound) {
      super.readSyncNBT(nbttagcompound);
      this.golem = nbttagcompound.getLong("golem");
      this.cost = nbttagcompound.getInteger("cost");
      this.maxCost = nbttagcompound.getInteger("mcost");
      if (this.golem >= 0L) {
         try {
            this.props = GolemProperties.fromLong(this.golem);
            this.components = this.props.generateComponents();
         } catch (Exception var3) {
            this.props = null;
            this.components = null;
            this.cost = 0;
            this.golem = -1L;
         }
      }
   }

   @Override
   public NBTTagCompound writeSyncNBT(NBTTagCompound nbttagcompound) {
      nbttagcompound.setLong("golem", this.golem);
      nbttagcompound.setInteger("cost", this.cost);
      nbttagcompound.setInteger("mcost", this.maxCost);
      return super.writeSyncNBT(nbttagcompound);
   }

   @SideOnly(Side.CLIENT)
   public AxisAlignedBB getRenderBoundingBox() {
      return new AxisAlignedBB(
         this.pos.getX() - 1,
         this.pos.getY(),
         this.pos.getZ() - 1,
         this.pos.getX() + 2,
         this.pos.getY() + 2,
         this.pos.getZ() + 2
      );
   }

   @Override
   public void update() {
      super.update();
      boolean complete = false;
      if (!this.world.isRemote) {
         this.ticks++;
         if (this.ticks % 5 == 0 && !complete && this.cost > 0 && this.golem >= 0L) {
            if (this.bufferedEssentia || this.drawEssentia()) {
               this.bufferedEssentia = false;
               this.cost--;
               this.markDirty();
            }

            if (this.cost <= 0) {
               ItemStack placer = new ItemStack(ItemsTC.golemPlacer);
               placer.setTagInfo("props", new NBTTagLong(this.golem));
               if (this.getStackInSlot(0).isEmpty()
                  || this.getStackInSlot(0).getCount() < this.getStackInSlot(0).getMaxStackSize()
                     && this.getStackInSlot(0).isItemEqual(placer)
                     && ItemStack.areItemStackTagsEqual(this.getStackInSlot(0), placer)) {
                  if (this.getStackInSlot(0) != null && !this.getStackInSlot(0).isEmpty()) {
                     this.getStackInSlot(0).grow(1);
                  } else {
                     this.setInventorySlotContents(0, placer.copy());
                  }

                  complete = true;
                  this.world.playSound(null, this.pos, SoundsTC.wand, SoundCategory.BLOCKS, 1.0F, 1.0F);
               }
            }
         }
      } else {
         if (this.press < 90 && this.cost > 0 && this.golem > 0L) {
            this.press += 6;
            if (this.press >= 60) {
               this.world
                  .playSound(
                     this.pos.getX() + 0.5,
                     this.pos.getY() + 0.5,
                     this.pos.getZ() + 0.5,
                     SoundEvents.BLOCK_LAVA_EXTINGUISH,
                     SoundCategory.BLOCKS,
                     0.66F,
                     1.0F + this.world.rand.nextFloat() * 0.1F,
                     false
                  );

               for (int a = 0; a < 16; a++) {
                  FXDispatcher.INSTANCE
                     .drawVentParticles(
                        this.pos.getX() + 0.5,
                        this.pos.getY() + 1,
                        this.pos.getZ() + 0.5,
                        this.world.rand.nextGaussian() * 0.1,
                        0.0,
                        this.world.rand.nextGaussian() * 0.1,
                        11184810
                     );
               }
            }
         }

         if (this.press >= 90 && this.world.rand.nextInt(8) == 0) {
            FXDispatcher.INSTANCE
               .drawVentParticles(
                  this.pos.getX() + 0.5,
                  this.pos.getY() + 1,
                  this.pos.getZ() + 0.5,
                  this.world.rand.nextGaussian() * 0.1,
                  0.0,
                  this.world.rand.nextGaussian() * 0.1,
                  11184810
               );
            this.world
               .playSound(
                  this.pos.getX() + 0.5,
                  this.pos.getY() + 0.5,
                  this.pos.getZ() + 0.5,
                  SoundEvents.BLOCK_LAVA_EXTINGUISH,
                  SoundCategory.BLOCKS,
                  0.1F,
                  1.0F + this.world.rand.nextFloat() * 0.1F,
                  false
               );
         }

         if (this.press > 0 && (this.cost <= 0 || this.golem == -1L)) {
            if (this.press >= 90) {
               for (int a = 0; a < 10; a++) {
                  FXDispatcher.INSTANCE
                     .drawVentParticles(
                        this.pos.getX() + 0.5,
                        this.pos.getY() + 1,
                        this.pos.getZ() + 0.5,
                        this.world.rand.nextGaussian() * 0.1,
                        0.0,
                        this.world.rand.nextGaussian() * 0.1,
                        11184810
                     );
               }
            }

            this.press -= 3;
         }
      }

      if (complete) {
         this.cost = 0;
         this.golem = -1L;
         this.syncTile(false);
         this.markDirty();
      }
   }

   public boolean[] checkCraft(long id) {
      IGolemProperties props = GolemProperties.fromLong(id);
      ItemStack[] cc = props.generateComponents();
      boolean[] ret = new boolean[cc.length];
      int a = 0;

      for (ItemStack stack : props.generateComponents()) {
         ret[a] = InventoryUtils.checkAdjacentChests(this.world, this.pos, stack);
         a++;
      }

      return ret;
   }

   public boolean startCraft(long id, EntityPlayer p) {
      ItemStack placer = new ItemStack(ItemsTC.golemPlacer);
      placer.setTagInfo("props", new NBTTagLong(id));
      if (this.getStackInSlot(0) == null
         || this.getStackInSlot(0).isEmpty()
         || this.getStackInSlot(0).getCount() < this.getStackInSlot(0).getMaxStackSize()
            && this.getStackInSlot(0).isItemEqual(placer)
            && ItemStack.areItemStackTagsEqual(this.getStackInSlot(0), placer)) {
         this.golem = id;
         this.props = GolemProperties.fromLong(this.golem);
         this.components = this.props.generateComponents();
         if (!InventoryUtils.consumeItemsFromAdjacentInventoryOrPlayer(this.getWorld(), this.getPos(), p, true, this.components)) {
            this.cost = 0;
            this.props = null;
            this.components = null;
            this.golem = -1L;
            return false;
         } else {
            this.cost = this.props.getTraits().size() * 2;

            for (ItemStack stack : this.components) {
               this.cost = this.cost + stack.getCount();
            }

            InventoryUtils.consumeItemsFromAdjacentInventoryOrPlayer(this.getWorld(), this.getPos(), p, false, this.components);
            this.maxCost = this.cost;
            this.markDirty();
            this.syncTile(false);
            this.world.playSound(null, this.pos, SoundsTC.wand, SoundCategory.BLOCKS, 0.25F, 1.0F);
            return true;
         }
      } else {
         this.cost = 0;
         this.props = null;
         this.components = null;
         this.golem = -1L;
         return false;
      }
   }

   @Override
   public boolean isItemValidForSlot(int par1, ItemStack stack2) {
      return stack2 != null && !stack2.isEmpty() && stack2.getItem() instanceof ItemGolemPlacer;
   }

   boolean drawEssentia() {
      for (EnumFacing face : EnumFacing.VALUES) {
         TileEntity te = ThaumcraftApiHelper.getConnectableTile(this.world, this.getPos(), face);
         if (te != null) {
            IEssentiaTransport ic = (IEssentiaTransport)te;
            if (!ic.canOutputTo(face.getOpposite())) {
               return false;
            }

            if (ic.getSuctionAmount(face.getOpposite()) < this.getSuctionAmount(face) && ic.takeEssentia(Aspect.MECHANISM, 1, face.getOpposite()) == 1) {
               return true;
            }
         }
      }

      return false;
   }

   @Override
   public boolean isConnectable(EnumFacing face) {
      return face.getHorizontalIndex() >= 0 || face == EnumFacing.DOWN;
   }

   @Override
   public boolean canInputFrom(EnumFacing face) {
      return this.isConnectable(face);
   }

   @Override
   public boolean canOutputTo(EnumFacing face) {
      return false;
   }

   @Override
   public void setSuction(Aspect aspect, int amount) {
   }

   @Override
   public int getMinimumSuction() {
      return 0;
   }

   @Override
   public Aspect getSuctionType(EnumFacing face) {
      return Aspect.MECHANISM;
   }

   @Override
   public int getSuctionAmount(EnumFacing face) {
      return this.cost > 0 && this.golem >= 0L ? 128 : 0;
   }

   @Override
   public Aspect getEssentiaType(EnumFacing loc) {
      return null;
   }

   @Override
   public int getEssentiaAmount(EnumFacing loc) {
      return 0;
   }

   @Override
   public int takeEssentia(Aspect aspect, int amount, EnumFacing facing) {
      return 0;
   }

   @Override
   public int addEssentia(Aspect aspect, int amount, EnumFacing facing) {
      if (!this.bufferedEssentia && this.cost > 0 && this.golem >= 0L && aspect == Aspect.MECHANISM) {
         this.bufferedEssentia = true;
         return 1;
      } else {
         return 0;
      }
   }

   public boolean canRenderBreaking() {
      return true;
   }
}
