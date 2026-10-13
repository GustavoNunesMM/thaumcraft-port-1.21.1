package thaumcraft.common.container.slot;

import com.google.common.collect.Lists;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryCraftResult;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.NonNullList;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.fml.common.FMLCommonHandler;
import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.crafting.ContainerDummy;
import thaumcraft.api.crafting.IArcaneRecipe;
import thaumcraft.api.items.ItemsTC;
import thaumcraft.common.items.casters.CasterManager;
import thaumcraft.common.lib.crafting.ThaumcraftCraftingManager;
import thaumcraft.common.tiles.crafting.TileArcaneWorkbench;

public class SlotCraftingArcaneWorkbench extends Slot {
   private final InventoryCrafting craftMatrix;
   private EntityPlayer player;
   private int amountCrafted;
   private TileArcaneWorkbench tile;

   public SlotCraftingArcaneWorkbench(
      TileArcaneWorkbench te, EntityPlayer par1EntityPlayer, InventoryCrafting inventory, IInventory par3IInventory, int par4, int par5, int par6
   ) {
      super(par3IInventory, par4, par5, par6);
      this.player = par1EntityPlayer;
      this.craftMatrix = inventory;
      this.tile = te;
   }

   public boolean isItemValid(ItemStack stack) {
      return false;
   }

   public ItemStack decrStackSize(int amount) {
      if (this.getHasStack()) {
         this.amountCrafted = this.amountCrafted + Math.min(amount, this.getStack().getCount());
      }

      return super.decrStackSize(amount);
   }

   protected void onCrafting(ItemStack stack, int amount) {
      this.amountCrafted += amount;
      this.onCrafting(stack);
   }

   protected void onSwapCraft(int p_190900_1_) {
      this.amountCrafted += p_190900_1_;
   }

   protected void onCrafting(ItemStack stack) {
      if (this.amountCrafted > 0) {
         stack.onCrafting(this.player.world, this.player, this.amountCrafted);
         FMLCommonHandler.instance().firePlayerCraftingEvent(this.player, stack, this.craftMatrix);
      }

      this.amountCrafted = 0;
      InventoryCraftResult inventorycraftresult = (InventoryCraftResult)this.inventory;
      IRecipe irecipe = inventorycraftresult.getRecipeUsed();
      if (irecipe != null && !irecipe.isDynamic()) {
         this.player.unlockRecipes(Lists.newArrayList(new IRecipe[]{irecipe}));
         inventorycraftresult.setRecipeUsed((IRecipe)null);
      }
   }

   public ItemStack onTake(EntityPlayer thePlayer, ItemStack stack) {
      this.onCrafting(stack);
      IArcaneRecipe recipe = ThaumcraftCraftingManager.findMatchingArcaneRecipe(this.craftMatrix, thePlayer);
      InventoryCrafting ic = this.craftMatrix;
      ForgeHooks.setCraftingPlayer(thePlayer);
      NonNullList<ItemStack> nonnulllist;
      if (recipe != null) {
         nonnulllist = CraftingManager.getRemainingItems(this.craftMatrix, thePlayer.world);
      } else {
         ic = new InventoryCrafting(new ContainerDummy(), 3, 3);

         for (int a = 0; a < 9; a++) {
            ic.setInventorySlotContents(a, this.craftMatrix.getStackInSlot(a));
         }

         ic.eventHandler = this.craftMatrix.eventHandler;
         nonnulllist = CraftingManager.getRemainingItems(ic, thePlayer.world);
      }

      ForgeHooks.setCraftingPlayer(null);
      int vis = 0;
      AspectList crystals = null;
      if (recipe != null) {
         vis = recipe.getVis();
         vis = (int)(vis * (1.0F - CasterManager.getTotalVisDiscount(thePlayer)));
         crystals = recipe.getCrystals();
         if (vis > 0) {
            this.tile.getAura();
            this.tile.spendAura(vis);
         }
      }

      for (int i = 0; i < Math.min(9, nonnulllist.size()); i++) {
         ItemStack itemstack = ic.getStackInSlot(i);
         ItemStack itemstack1 = (ItemStack)nonnulllist.get(i);
         if (!itemstack.isEmpty()) {
            this.craftMatrix.decrStackSize(i, 1);
            itemstack = ic.getStackInSlot(i);
         }

         if (!itemstack1.isEmpty()) {
            if (itemstack.isEmpty()) {
               this.craftMatrix.setInventorySlotContents(i, itemstack1);
            } else if (ItemStack.areItemsEqual(itemstack, itemstack1) && ItemStack.areItemStackTagsEqual(itemstack, itemstack1)) {
               itemstack1.grow(itemstack.getCount());
               this.craftMatrix.setInventorySlotContents(i, itemstack1);
            } else if (!this.player.inventory.addItemStackToInventory(itemstack1)) {
               this.player.dropItem(itemstack1, false);
            }
         }
      }

      if (crystals != null) {
         for (Aspect aspect : crystals.getAspects()) {
            ItemStack cs = ThaumcraftApiHelper.makeCrystal(aspect, crystals.getAmount(aspect));

            for (int i = 0; i < 6; i++) {
               ItemStack itemstack1x = this.craftMatrix.getStackInSlot(9 + i);
               if (itemstack1x.getItem() == ItemsTC.crystalEssence && ItemStack.areItemStackTagsEqual(cs, itemstack1x)) {
                  this.craftMatrix.decrStackSize(9 + i, cs.getCount());
               }
            }
         }
      }

      return stack;
   }
}
