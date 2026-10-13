package thaumcraft.common.tiles.crafting;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.text.ITextComponent;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IAspectContainer;
import thaumcraft.api.aspects.IEssentiaTransport;
import thaumcraft.common.tiles.TileThaumcraft;

public class TileThaumatoriumTop extends TileThaumcraft implements IAspectContainer, IEssentiaTransport, ISidedInventory, ITickable {
   public TileThaumatorium thaumatorium = null;

   public void update() {
      if (this.thaumatorium == null) {
         TileEntity tile = this.world.getTileEntity(this.pos.down());
         if (tile != null && tile instanceof TileThaumatorium) {
            this.thaumatorium = (TileThaumatorium)tile;
         }
      }
   }

   @Override
   public int addToContainer(Aspect tt, int am) {
      return this.thaumatorium == null ? am : this.thaumatorium.addToContainer(tt, am);
   }

   @Override
   public boolean takeFromContainer(Aspect tt, int am) {
      return this.thaumatorium == null ? false : this.thaumatorium.takeFromContainer(tt, am);
   }

   @Override
   public boolean takeFromContainer(AspectList ot) {
      return false;
   }

   @Override
   public boolean doesContainerContain(AspectList ot) {
      return false;
   }

   @Override
   public boolean doesContainerContainAmount(Aspect tt, int am) {
      return this.thaumatorium == null ? false : this.thaumatorium.doesContainerContainAmount(tt, am);
   }

   @Override
   public int containerContains(Aspect tt) {
      return this.thaumatorium == null ? 0 : this.thaumatorium.containerContains(tt);
   }

   @Override
   public boolean doesContainerAccept(Aspect tag) {
      return true;
   }

   @Override
   public boolean isConnectable(EnumFacing face) {
      return this.thaumatorium == null ? false : this.thaumatorium.isConnectable(face);
   }

   @Override
   public boolean canInputFrom(EnumFacing face) {
      return this.thaumatorium == null ? false : this.thaumatorium.canInputFrom(face);
   }

   @Override
   public boolean canOutputTo(EnumFacing face) {
      return false;
   }

   @Override
   public void setSuction(Aspect aspect, int amount) {
      if (this.thaumatorium != null) {
         this.thaumatorium.setSuction(aspect, amount);
      }
   }

   @Override
   public Aspect getSuctionType(EnumFacing loc) {
      return this.thaumatorium == null ? null : this.thaumatorium.getSuctionType(loc);
   }

   @Override
   public int getSuctionAmount(EnumFacing loc) {
      return this.thaumatorium == null ? 0 : this.thaumatorium.getSuctionAmount(loc);
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
   public int takeEssentia(Aspect aspect, int amount, EnumFacing face) {
      return this.thaumatorium == null ? 0 : this.thaumatorium.takeEssentia(aspect, amount, face);
   }

   @Override
   public int addEssentia(Aspect aspect, int amount, EnumFacing face) {
      return this.thaumatorium == null ? 0 : this.thaumatorium.addEssentia(aspect, amount, face);
   }

   @Override
   public int getMinimumSuction() {
      return 0;
   }

   @Override
   public AspectList getAspects() {
      return this.thaumatorium == null ? null : this.thaumatorium.essentia;
   }

   @Override
   public void setAspects(AspectList aspects) {
      if (this.thaumatorium != null) {
         this.thaumatorium.setAspects(aspects);
      }
   }

   public int getSizeInventory() {
      return 1;
   }

   public ItemStack getStackInSlot(int par1) {
      return this.thaumatorium == null ? ItemStack.EMPTY : this.thaumatorium.getStackInSlot(par1);
   }

   public ItemStack decrStackSize(int par1, int par2) {
      return this.thaumatorium == null ? ItemStack.EMPTY : this.thaumatorium.decrStackSize(par1, par2);
   }

   public ItemStack removeStackFromSlot(int par1) {
      return this.thaumatorium == null ? ItemStack.EMPTY : this.thaumatorium.removeStackFromSlot(par1);
   }

   public void setInventorySlotContents(int par1, ItemStack stack2) {
      if (this.thaumatorium != null) {
         this.thaumatorium.setInventorySlotContents(par1, stack2);
      }
   }

   public int getInventoryStackLimit() {
      return 64;
   }

   public boolean isUsableByPlayer(EntityPlayer par1EntityPlayer) {
      return this.world.getTileEntity(this.pos) != this ? false : par1EntityPlayer.getDistanceSqToCenter(this.pos) <= 64.0;
   }

   public boolean isItemValidForSlot(int par1, ItemStack stack2) {
      return true;
   }

   public int[] getSlotsForFace(EnumFacing side) {
      return new int[]{0};
   }

   public boolean canInsertItem(int index, ItemStack itemStackIn, EnumFacing direction) {
      return true;
   }

   public boolean canExtractItem(int index, ItemStack stack, EnumFacing direction) {
      return true;
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
      this.thaumatorium.clear();
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

   public boolean isEmpty() {
      return this.thaumatorium.isEmpty();
   }
}
