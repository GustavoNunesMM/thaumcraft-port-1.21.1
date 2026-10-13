package thaumcraft.common.tiles.crafting;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.stream.Collectors;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IAspectContainer;
import thaumcraft.api.aspects.IEssentiaTransport;
import thaumcraft.api.blocks.BlocksTC;
import thaumcraft.api.capabilities.ThaumcraftCapabilities;
import thaumcraft.api.crafting.CrucibleRecipe;
import thaumcraft.client.fx.FXDispatcher;
import thaumcraft.common.blocks.IBlockFacing;
import thaumcraft.common.container.InventoryFake;
import thaumcraft.common.lib.utils.BlockStateUtils;
import thaumcraft.common.lib.utils.InventoryUtils;
import thaumcraft.common.tiles.TileThaumcraftInventory;

public class TileThaumatorium extends TileThaumcraftInventory implements IAspectContainer, IEssentiaTransport, ITickable {
   public AspectList essentia = new AspectList();
   public ArrayList<Integer> recipeHash = new ArrayList<>();
   public ArrayList<AspectList> recipeEssentia = new ArrayList<>();
   public ArrayList<String> recipePlayer = new ArrayList<>();
   public int currentCraft = -1;
   public int maxRecipes = 1;
   public Aspect currentSuction = null;
   int venting = 0;
   int counter = 0;
   boolean heated = false;
   CrucibleRecipe currentRecipe = null;
   public Container eventHandler;
   public ArrayList<CrucibleRecipe> recipes = new ArrayList<>();

   public TileThaumatorium() {
      super(1);
   }

   @SideOnly(Side.CLIENT)
   public AxisAlignedBB getRenderBoundingBox() {
      return new AxisAlignedBB(
         this.getPos().getX() - 0.1,
         this.getPos().getY() - 0.1,
         this.getPos().getZ() - 0.1,
         this.getPos().getX() + 1.1,
         this.getPos().getY() + 2.1,
         this.getPos().getZ() + 1.1
      );
   }

   @Override
   public void readSyncNBT(NBTTagCompound nbttagcompound) {
      this.essentia.readFromNBT(nbttagcompound);
      this.maxRecipes = nbttagcompound.getByte("maxrec");
      this.recipeEssentia = new ArrayList<>();
      this.recipeHash = new ArrayList<>();
      this.recipePlayer = new ArrayList<>();
      int[] hashes = nbttagcompound.getIntArray("recipes");
      if (hashes != null) {
         for (int hash : hashes) {
            CrucibleRecipe recipe = ThaumcraftApi.getCrucibleRecipeFromHash(hash);
            if (recipe != null) {
               this.recipeEssentia.add(recipe.getAspects().copy());
               this.recipePlayer.add("");
               this.recipeHash.add(hash);
            }
         }
      }
   }

   @Override
   public NBTTagCompound writeSyncNBT(NBTTagCompound nbttagcompound) {
      nbttagcompound.setByte("maxrec", (byte)this.maxRecipes);
      this.essentia.writeToNBT(nbttagcompound);
      int[] hashes = new int[this.recipeHash.size()];
      int a = 0;

      for (Integer i : this.recipeHash) {
         hashes[a] = i;
         a++;
      }

      nbttagcompound.setIntArray("recipes", hashes);
      return nbttagcompound;
   }

   @Override
   public void readFromNBT(NBTTagCompound nbtCompound) {
      super.readFromNBT(nbtCompound);
      NBTTagList nbttaglist2 = nbtCompound.getTagList("OutputPlayer", 8);

      for (int a = 0; a < nbttaglist2.tagCount(); a++) {
         if (this.recipePlayer.size() > a) {
            this.recipePlayer.set(a, nbttaglist2.getStringTagAt(a));
         }
      }
   }

   @Override
   public NBTTagCompound writeToNBT(NBTTagCompound nbtCompound) {
      super.writeToNBT(nbtCompound);
      NBTTagList nbttaglist2 = new NBTTagList();
      if (this.recipePlayer.size() > 0) {
         for (int a = 0; a < this.recipePlayer.size(); a++) {
            if (this.recipePlayer.get(a) != null) {
               NBTTagString nbttagcompound1 = new NBTTagString(this.recipePlayer.get(a));
               nbttaglist2.appendTag(nbttagcompound1);
            }
         }
      }

      nbtCompound.setTag("OutputPlayer", nbttaglist2);
      return nbtCompound;
   }

   boolean checkHeat() {
      Material mat = this.world.getBlockState(this.pos.down(2)).getMaterial();
      Block bi = this.world.getBlockState(this.pos.down(2)).getBlock();
      return mat == Material.LAVA || mat == Material.FIRE || BlocksTC.nitor.containsValue(bi) || bi == Blocks.MAGMA;
   }

   public ItemStack getCurrentOutputRecipe() {
      ItemStack out = ItemStack.EMPTY;
      if (this.currentCraft >= 0 && this.recipeHash != null && this.recipeHash.size() > 0) {
         CrucibleRecipe recipe = ThaumcraftApi.getCrucibleRecipeFromHash(this.recipeHash.get(this.currentCraft));
         if (recipe != null) {
            out = recipe.getRecipeOutput().copy();
         }
      }

      return out;
   }

   @Override
   public void update() {
      if (!this.world.isRemote) {
         if (this.counter == 0 || this.counter % 40 == 0) {
            this.heated = this.checkHeat();
            this.getUpgrades();
         }

         this.counter++;
         if (this.heated && !this.gettingPower() && this.counter % 5 == 0 && this.recipeHash != null && this.recipeHash.size() > 0) {
            if (this.getStackInSlot(0).isEmpty()) {
               this.currentSuction = null;
               return;
            }

            if (this.currentCraft < 0
               || this.currentCraft >= this.recipeHash.size()
               || this.currentRecipe == null
               || !this.currentRecipe.catalystMatches(this.getStackInSlot(0))) {
               for (int a = 0; a < this.recipeHash.size(); a++) {
                  CrucibleRecipe recipe = ThaumcraftApi.getCrucibleRecipeFromHash(this.recipeHash.get(a));
                  if (recipe.catalystMatches(this.getStackInSlot(0))) {
                     this.currentCraft = a;
                     this.currentRecipe = recipe;
                     break;
                  }
               }
            }

            if (this.currentCraft < 0 || this.currentCraft >= this.recipeHash.size()) {
               return;
            }

            boolean done = true;
            this.currentSuction = null;

            for (Aspect aspect : this.recipeEssentia.get(this.currentCraft).getAspectsSortedByName()) {
               if (this.essentia.getAmount(aspect) < this.recipeEssentia.get(this.currentCraft).getAmount(aspect)) {
                  this.currentSuction = aspect;
                  done = false;
                  break;
               }
            }

            if (done) {
               this.completeRecipe();
            } else if (this.currentSuction != null) {
               this.fill();
            }
         }
      } else if (this.venting > 0) {
         this.venting--;
         float fx = 0.1F - this.world.rand.nextFloat() * 0.2F;
         float fz = 0.1F - this.world.rand.nextFloat() * 0.2F;
         float fy = 0.1F - this.world.rand.nextFloat() * 0.2F;
         float fx2 = 0.1F - this.world.rand.nextFloat() * 0.2F;
         float fz2 = 0.1F - this.world.rand.nextFloat() * 0.2F;
         float fy2 = 0.1F - this.world.rand.nextFloat() * 0.2F;
         int color = 16777215;
         EnumFacing facing = BlockStateUtils.getFacing(this.getBlockMetadata());
         FXDispatcher.INSTANCE
            .drawVentParticles(
               this.pos.getX() + 0.5F + fx + facing.getXOffset() / 2.0F,
               this.pos.getY() + 0.5F + fy,
               this.pos.getZ() + 0.5F + fz + facing.getZOffset() / 2.0F,
               facing.getXOffset() / 4.0F + fx2,
               fy2,
               facing.getZOffset() / 4.0F + fz2,
               color
            );
      }
   }

   private void completeRecipe() {
      if (this.currentRecipe != null
         && this.currentCraft < this.recipeHash.size()
         && this.currentRecipe.matches(this.essentia, this.getStackInSlot(0))
         && this.decrStackSize(0, 1) != null) {
         this.essentia = new AspectList();
         ItemStack dropped = this.getCurrentOutputRecipe();
         EntityPlayer p = this.world.getPlayerEntityByName(this.recipePlayer.get(this.currentCraft));
         if (p != null) {
            FMLCommonHandler.instance().firePlayerCraftingEvent(p, dropped, new InventoryFake(this.getStackInSlot(0)));
         }

         EnumFacing facing = BlockStateUtils.getFacing(this.getBlockMetadata());
         InventoryUtils.ejectStackAt(this.getWorld(), this.getPos(), facing, dropped);
         this.world
            .playSound(
               null,
               this.pos,
               SoundEvents.BLOCK_LAVA_EXTINGUISH,
               SoundCategory.BLOCKS,
               0.25F,
               2.6F + (this.world.rand.nextFloat() - this.world.rand.nextFloat()) * 0.8F
            );
         this.currentCraft = -1;
         this.syncTile(false);
         this.markDirty();
      }
   }

   void fill() {
      EnumFacing facing = BlockStateUtils.getFacing(this.getBlockMetadata());
      TileEntity te = null;
      IEssentiaTransport ic = null;

      for (int y = 0; y <= 1; y++) {
         for (EnumFacing dir : EnumFacing.VALUES) {
            if (dir != facing && dir != EnumFacing.DOWN && (y != 0 || dir != EnumFacing.UP)) {
               te = ThaumcraftApiHelper.getConnectableTile(this.world, this.pos.up(y), dir);
               if (te != null) {
                  ic = (IEssentiaTransport)te;
                  if (ic.getEssentiaAmount(dir.getOpposite()) > 0
                     && ic.getSuctionAmount(dir.getOpposite()) < this.getSuctionAmount(null)
                     && this.getSuctionAmount(null) >= ic.getMinimumSuction()) {
                     int ess = ic.takeEssentia(this.currentSuction, 1, dir.getOpposite());
                     if (ess > 0) {
                        this.addToContainer(this.currentSuction, ess);
                        return;
                     }
                  }
               }
            }
         }
      }
   }

   @Override
   public int addToContainer(Aspect tt, int am) {
      int ce = this.currentRecipe.getAspects().getAmount(tt) - this.essentia.getAmount(tt);
      if (this.currentRecipe != null && ce > 0) {
         int add = Math.min(ce, am);
         this.essentia.add(tt, add);
         this.syncTile(false);
         this.markDirty();
         return am - add;
      } else {
         return am;
      }
   }

   @Override
   public boolean takeFromContainer(Aspect tt, int am) {
      if (this.essentia.getAmount(tt) >= am) {
         this.essentia.remove(tt, am);
         this.syncTile(false);
         this.markDirty();
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
   public boolean doesContainerContain(AspectList ot) {
      return false;
   }

   @Override
   public boolean doesContainerContainAmount(Aspect tt, int am) {
      return this.essentia.getAmount(tt) >= am;
   }

   @Override
   public int containerContains(Aspect tt) {
      return this.essentia.getAmount(tt);
   }

   @Override
   public boolean doesContainerAccept(Aspect tag) {
      return true;
   }

   public boolean receiveClientEvent(int i, int j) {
      if (i >= 0) {
         if (this.world.isRemote) {
            this.venting = 7;
         }

         return true;
      } else {
         return super.receiveClientEvent(i, j);
      }
   }

   @Override
   public boolean isConnectable(EnumFacing face) {
      return face != BlockStateUtils.getFacing(this.getBlockMetadata());
   }

   @Override
   public boolean canInputFrom(EnumFacing face) {
      return face != BlockStateUtils.getFacing(this.getBlockMetadata());
   }

   @Override
   public boolean canOutputTo(EnumFacing face) {
      return false;
   }

   @Override
   public void setSuction(Aspect aspect, int amount) {
      this.currentSuction = aspect;
   }

   @Override
   public Aspect getSuctionType(EnumFacing loc) {
      return this.currentSuction;
   }

   @Override
   public int getSuctionAmount(EnumFacing loc) {
      return this.currentSuction != null ? 128 : 0;
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
      return this.canOutputTo(face) && this.takeFromContainer(aspect, amount) ? amount : 0;
   }

   @Override
   public int addEssentia(Aspect aspect, int amount, EnumFacing face) {
      return this.canInputFrom(face) ? amount - this.addToContainer(aspect, amount) : 0;
   }

   @Override
   public int getMinimumSuction() {
      return 0;
   }

   @Override
   public AspectList getAspects() {
      return this.essentia;
   }

   @Override
   public void setAspects(AspectList aspects) {
      this.essentia = aspects;
   }

   public void markDirty() {
      super.markDirty();
      if (this.eventHandler != null) {
         this.eventHandler.onCraftMatrixChanged(this);
      }
   }

   @Override
   public int[] getSlotsForFace(EnumFacing side) {
      return new int[]{0};
   }

   @Override
   public boolean gettingPower() {
      return this.world.getRedstonePowerFromNeighbors(this.pos) > 0
         || this.world.getRedstonePowerFromNeighbors(this.pos.down()) > 0
         || this.world.getRedstonePowerFromNeighbors(this.pos.up()) > 0;
   }

   public void getUpgrades() {
      EnumFacing facing = BlockStateUtils.getFacing(this.getBlockMetadata());
      int mr = 1;

      for (int yy = 0; yy <= 1; yy++) {
         for (EnumFacing dir : EnumFacing.VALUES) {
            if (dir != EnumFacing.DOWN && dir != facing) {
               int xx = this.pos.getX() + dir.getXOffset();
               int zz = this.pos.getZ() + dir.getZOffset();
               BlockPos bp = new BlockPos(xx, this.pos.getY() + yy + dir.getYOffset(), zz);
               IBlockState bs = this.world.getBlockState(bp);
               if (bs == BlocksTC.brainBox.getDefaultState().withProperty(IBlockFacing.FACING, dir.getOpposite())) {
                  mr += 2;
               }
            }
         }
      }

      if (mr != this.maxRecipes) {
         this.maxRecipes = mr;

         while (this.recipeHash.size() > this.maxRecipes) {
            this.recipeHash.remove(this.recipeHash.size() - 1);
         }

         this.syncTile(false);
         this.markDirty();
      }
   }

   @Override
   public String getName() {
      return "container.alchemyfurnace";
   }

   @Override
   public boolean hasCustomName() {
      return false;
   }

   @Override
   public ITextComponent getDisplayName() {
      return null;
   }

   public void updateRecipes(EntityPlayer player) {
      this.recipes.clear();
      ArrayList<CrucibleRecipe> recipesTemp = new ArrayList<>();
      if (this.getStackInSlot(0) != null && !this.getStackInSlot(0).isEmpty() && this.recipeHash != null) {
         for (Object r : ThaumcraftApi.getCraftingRecipes().values()) {
            if (r instanceof CrucibleRecipe) {
               CrucibleRecipe creps = (CrucibleRecipe)r;
               if (ThaumcraftCapabilities.knowsResearchStrict(player, creps.getResearch()) && creps.catalystMatches(this.getStackInSlot(0))) {
                  recipesTemp.add(creps);
               } else if (this.recipeHash != null && this.recipeHash.size() > 0) {
                  for (Integer hash : this.recipeHash) {
                     if (creps.hash == hash) {
                        recipesTemp.add(creps);
                        break;
                     }
                  }
               }
            }
         }
      }

      this.recipes = recipesTemp.stream().sorted(new TileThaumatorium.RecipeOutputComparator()).collect(Collectors.toList());
   }

   public ArrayList<Integer> generateRecipeHashlist() {
      ArrayList<Integer> hashList = new ArrayList<>();

      label32:
      for (int hash : this.recipeHash) {
         for (CrucibleRecipe cr : this.recipes) {
            if (cr.hash == hash) {
               continue label32;
            }
         }

         hashList.add(hash);
      }

      for (CrucibleRecipe crx : this.recipes) {
         hashList.add(crx.hash);
      }

      return hashList;
   }

   private class RecipeOutputComparator implements Comparator<CrucibleRecipe> {
      public RecipeOutputComparator() {
      }

      public int compare(CrucibleRecipe a, CrucibleRecipe b) {
         return a.equals(b) ? 0 : a.getRecipeOutput().getDisplayName().compareTo(b.getRecipeOutput().getDisplayName());
      }
   }
}
