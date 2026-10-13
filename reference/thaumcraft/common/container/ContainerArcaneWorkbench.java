package thaumcraft.common.container;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IContainerListener;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryCraftResult;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.network.play.server.SPacketSetSlot;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.api.ThaumcraftInvHelper;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.capabilities.ThaumcraftCapabilities;
import thaumcraft.api.crafting.ContainerDummy;
import thaumcraft.api.crafting.IArcaneRecipe;
import thaumcraft.common.blocks.world.ore.ShardType;
import thaumcraft.common.container.slot.SlotCraftingArcaneWorkbench;
import thaumcraft.common.container.slot.SlotCrystal;
import thaumcraft.common.items.casters.CasterManager;
import thaumcraft.common.lib.crafting.ThaumcraftCraftingManager;
import thaumcraft.common.tiles.crafting.TileArcaneWorkbench;

public class ContainerArcaneWorkbench extends Container {
   private TileArcaneWorkbench tileEntity;
   private InventoryPlayer ip;
   public InventoryCraftResult craftResult = new InventoryCraftResult();
   public static int[] xx = new int[]{64, 17, 112, 17, 112, 64};
   public static int[] yy = new int[]{13, 35, 35, 93, 93, 115};
   private int lastVis = -1;
   private long lastCheck = 0L;

   public ContainerArcaneWorkbench(InventoryPlayer par1InventoryPlayer, TileArcaneWorkbench e) {
      this.tileEntity = e;
      this.tileEntity.inventoryCraft.eventHandler = this;
      this.ip = par1InventoryPlayer;
      e.getAura();
      this.addSlotToContainer(
         new SlotCraftingArcaneWorkbench(this.tileEntity, par1InventoryPlayer.player, this.tileEntity.inventoryCraft, this.craftResult, 15, 160, 64)
      );

      for (int var6 = 0; var6 < 3; var6++) {
         for (int var7 = 0; var7 < 3; var7++) {
            this.addSlotToContainer(new Slot(this.tileEntity.inventoryCraft, var7 + var6 * 3, 40 + var7 * 24, 40 + var6 * 24));
         }
      }

      for (ShardType st : ShardType.values()) {
         if (st.getMetadata() < 6) {
            this.addSlotToContainer(new SlotCrystal(st.getAspect(), this.tileEntity.inventoryCraft, st.getMetadata() + 9, xx[st.getMetadata()], yy[st.getMetadata()]));
         }
      }

      for (int var9 = 0; var9 < 3; var9++) {
         for (int var7 = 0; var7 < 9; var7++) {
            this.addSlotToContainer(new Slot(par1InventoryPlayer, var7 + var9 * 9 + 9, 16 + var7 * 18, 151 + var9 * 18));
         }
      }

      for (int var10 = 0; var10 < 9; var10++) {
         this.addSlotToContainer(new Slot(par1InventoryPlayer, var10, 16 + var10 * 18, 209));
      }

      this.onCraftMatrixChanged(this.tileEntity.inventoryCraft);
   }

   public void addListener(IContainerListener par1ICrafting) {
      super.addListener(par1ICrafting);
      this.tileEntity.getAura();
      par1ICrafting.sendWindowProperty(this, 0, this.tileEntity.auraVisServer);
   }

   public void detectAndSendChanges() {
      super.detectAndSendChanges();
      long t = System.currentTimeMillis();
      if (t > this.lastCheck) {
         this.lastCheck = t + 500L;
         this.tileEntity.getAura();
      }

      if (this.lastVis != this.tileEntity.auraVisServer) {
         this.onCraftMatrixChanged(this.tileEntity.inventoryCraft);
      }

      for (int i = 0; i < this.listeners.size(); i++) {
         IContainerListener icrafting = (IContainerListener)this.listeners.get(i);
         if (this.lastVis != this.tileEntity.auraVisServer) {
            icrafting.sendWindowProperty(this, 0, this.tileEntity.auraVisServer);
         }
      }

      this.lastVis = this.tileEntity.auraVisServer;
   }

   @SideOnly(Side.CLIENT)
   public void updateProgressBar(int par1, int par2) {
      if (par1 == 0) {
         this.tileEntity.auraVisClient = par2;
      }
   }

   public void onCraftMatrixChanged(IInventory par1IInventory) {
      IArcaneRecipe recipe = ThaumcraftCraftingManager.findMatchingArcaneRecipe(this.tileEntity.inventoryCraft, this.ip.player);
      boolean hasVis = true;
      boolean hasCrystals = true;
      if (recipe != null) {
         int vis = 0;
         AspectList crystals = null;
         vis = recipe.getVis();
         vis = (int)(vis * (1.0F - CasterManager.getTotalVisDiscount(this.ip.player)));
         crystals = recipe.getCrystals();
         this.tileEntity.getAura();
         hasVis = this.tileEntity.getWorld().isRemote ? this.tileEntity.auraVisClient >= vis : this.tileEntity.auraVisServer >= vis;
         if (crystals != null && crystals.size() > 0) {
            for (Aspect aspect : crystals.getAspects()) {
               if (ThaumcraftInvHelper.countTotalItemsIn(
                     ThaumcraftInvHelper.wrapInventory(this.tileEntity.inventoryCraft, EnumFacing.UP),
                     ThaumcraftApiHelper.makeCrystal(aspect, crystals.getAmount(aspect)),
                     ThaumcraftInvHelper.InvFilter.STRICT
                  )
                  < crystals.getAmount(aspect)) {
                  hasCrystals = false;
                  break;
               }
            }
         }
      }

      if (hasVis && hasCrystals) {
         this.slotChangedCraftingGrid(this.tileEntity.getWorld(), this.ip.player, this.tileEntity.inventoryCraft, this.craftResult);
      }

      super.detectAndSendChanges();
   }

   protected void slotChangedCraftingGrid(World world, EntityPlayer player, InventoryCrafting craftMat, InventoryCraftResult craftRes) {
      if (!world.isRemote) {
         EntityPlayerMP entityplayermp = (EntityPlayerMP)player;
         ItemStack itemstack = ItemStack.EMPTY;
         IArcaneRecipe arecipe = ThaumcraftCraftingManager.findMatchingArcaneRecipe(craftMat, entityplayermp);
         if (arecipe != null
            && (arecipe.isDynamic() || !world.getGameRules().getBoolean("doLimitedCrafting") || entityplayermp.getRecipeBook().isUnlocked(arecipe))
            && ThaumcraftCapabilities.getKnowledge(player).isResearchKnown(arecipe.getResearch())) {
            craftRes.setRecipeUsed(arecipe);
            itemstack = arecipe.getCraftingResult(craftMat);
         } else {
            InventoryCrafting craftInv = new InventoryCrafting(new ContainerDummy(), 3, 3);

            for (int a = 0; a < 9; a++) {
               craftInv.setInventorySlotContents(a, craftMat.getStackInSlot(a));
            }

            IRecipe irecipe = CraftingManager.findMatchingRecipe(craftInv, world);
            if (irecipe != null
               && (irecipe.isDynamic() || !world.getGameRules().getBoolean("doLimitedCrafting") || entityplayermp.getRecipeBook().isUnlocked(irecipe))
               )
             {
               craftRes.setRecipeUsed(irecipe);
               itemstack = irecipe.getCraftingResult(craftMat);
            }
         }

         craftRes.setInventorySlotContents(0, itemstack);
         entityplayermp.connection.sendPacket(new SPacketSetSlot(this.windowId, 0, itemstack));
      }
   }

   public void onContainerClosed(EntityPlayer par1EntityPlayer) {
      super.onContainerClosed(par1EntityPlayer);
      if (!this.tileEntity.getWorld().isRemote) {
         this.tileEntity.inventoryCraft.eventHandler = new ContainerDummy();
      }
   }

   public boolean canInteractWith(EntityPlayer par1EntityPlayer) {
      return this.tileEntity.getWorld().getTileEntity(this.tileEntity.getPos()) != this.tileEntity
         ? false
         : par1EntityPlayer.getDistanceSqToCenter(this.tileEntity.getPos()) <= 64.0;
   }

   public ItemStack transferStackInSlot(EntityPlayer par1EntityPlayer, int par1) {
      ItemStack var2x = ItemStack.EMPTY;
      Slot var3 = (Slot)this.inventorySlots.get(par1);
      if (var3 != null && var3.getHasStack()) {
         ItemStack var4 = var3.getStack();
         var2x = var4.copy();
         if (par1 == 0) {
            if (!this.mergeItemStack(var4, 16, 52, true)) {
               return ItemStack.EMPTY;
            }

            var3.onSlotChange(var4, var2x);
         } else if (par1 >= 16 && par1 < 52) {
            for (ShardType st : ShardType.values()) {
               if (st.getMetadata() < 6 && SlotCrystal.isValidCrystal(var4, st.getAspect())) {
                  if (!this.mergeItemStack(var4, 10 + st.getMetadata(), 11 + st.getMetadata(), false)) {
                     return ItemStack.EMPTY;
                  }

                  if (var4.getCount() == 0) {
                     break;
                  }
               }
            }

            if (var4.getCount() != 0) {
               if (par1 >= 16 && par1 < 43) {
                  if (!this.mergeItemStack(var4, 43, 52, false)) {
                     return ItemStack.EMPTY;
                  }
               } else if (par1 >= 43 && par1 < 52 && !this.mergeItemStack(var4, 16, 43, false)) {
                  return ItemStack.EMPTY;
               }
            }
         } else if (!this.mergeItemStack(var4, 16, 52, false)) {
            return ItemStack.EMPTY;
         }

         if (var4.getCount() == 0) {
            var3.putStack(ItemStack.EMPTY);
         } else {
            var3.onSlotChanged();
         }

         if (var4.getCount() == var2x.getCount()) {
            return ItemStack.EMPTY;
         }

         var3.onTake(this.ip.player, var4);
      }

      return var2x;
   }

   public boolean canMergeSlot(ItemStack stack, Slot slot) {
      return slot.inventory != this.craftResult && super.canMergeSlot(stack, slot);
   }
}
