package thaumcraft.common.tiles.crafting;

import java.util.List;
import java.util.Random;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.NonNullList;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import thaumcraft.api.ThaumcraftInvHelper;
import thaumcraft.api.aura.AuraHelper;
import thaumcraft.codechicken.lib.raytracer.IndexedCuboid6;
import thaumcraft.codechicken.lib.vec.Cuboid6;
import thaumcraft.common.lib.SoundsTC;
import thaumcraft.common.lib.utils.BlockStateUtils;
import thaumcraft.common.lib.utils.InventoryUtils;
import thaumcraft.common.tiles.TileThaumcraft;

public class TilePatternCrafter extends TileThaumcraft implements ITickable {
   public byte type = 0;
   public int count = new Random(System.currentTimeMillis()).nextInt(20);
   private final InventoryCrafting craftMatrix = new InventoryCrafting(new Container() {
      public boolean canInteractWith(EntityPlayer playerIn) {
         return false;
      }
   }, 3, 3);
   float power = 0.0F;
   public float rot;
   public float rp;
   public int rotTicks = 0;
   ItemStack outStack = null;

   @Override
   public void readSyncNBT(NBTTagCompound nbt) {
      this.type = nbt.getByte("type");
   }

   @Override
   public NBTTagCompound writeSyncNBT(NBTTagCompound nbt) {
      nbt.setByte("type", this.type);
      return nbt;
   }

   @Override
   public void readFromNBT(NBTTagCompound nbt) {
      this.power = nbt.getFloat("power");
      super.readFromNBT(nbt);
   }

   @Override
   public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
      nbt.setFloat("power", this.power);
      return super.writeToNBT(nbt);
   }

   public void update() {
      if (this.world.isRemote) {
         if (this.rotTicks > 0) {
            this.rotTicks--;
            if (this.rotTicks % Math.floor(Math.max(1.0F, this.rp)) == 0.0) {
               this.world
                  .playSound(
                     this.pos.getX() + 0.5,
                     this.pos.getY() + 0.5,
                     this.pos.getZ() + 0.5,
                     SoundsTC.clack,
                     SoundCategory.BLOCKS,
                     0.2F,
                     1.7F,
                     false
                  );
            }

            this.rp++;
         } else {
            this.rp *= 0.8F;
         }

         this.rot = this.rot + this.rp;
      }

      if (!this.world.isRemote && this.count++ % 20 == 0 && BlockStateUtils.isEnabled(this.getBlockMetadata())) {
         if (this.power <= 0.0F) {
            this.power = this.power + AuraHelper.drainVis(this.getWorld(), this.getPos(), 5.0F, false);
         }

         int amt = 9;
         switch (this.type) {
            case 0:
               amt = 9;
               break;
            case 1:
               amt = 1;
               break;
            case 2:
            case 3:
               amt = 2;
               break;
            case 4:
               amt = 4;
               break;
            case 5:
            case 6:
               amt = 3;
               break;
            case 7:
            case 8:
               amt = 6;
               break;
            case 9:
               amt = 8;
         }

         IItemHandler above = ThaumcraftInvHelper.getItemHandlerAt(this.getWorld(), this.getPos().up(), EnumFacing.DOWN);
         IItemHandler below = ThaumcraftInvHelper.getItemHandlerAt(this.getWorld(), this.getPos().down(), EnumFacing.UP);
         if (above != null && below != null) {
            for (int a = 0; a < above.getSlots(); a++) {
               ItemStack testStack = above.getStackInSlot(a).copy();
               if (!testStack.isEmpty()) {
                  testStack.setCount(amt);
                  if (InventoryUtils.removeStackFrom(
                              this.getWorld(),
                              this.getPos().up(),
                              EnumFacing.DOWN,
                              testStack.copy(),
                              ThaumcraftInvHelper.InvFilter.BASEORE,
                              true
                           )
                           .getCount()
                        == amt
                     && this.craft(testStack)
                     && this.power >= 1.0F
                     && ItemHandlerHelper.insertItem(below, this.outStack.copy(), true).isEmpty()) {
                     boolean b = true;

                     for (int i = 0; i < 9; i++) {
                        if (this.craftMatrix.getStackInSlot(i) != null
                           && !ItemHandlerHelper.insertItem(below, this.craftMatrix.getStackInSlot(i).copy(), true).isEmpty()) {
                           b = false;
                           break;
                        }
                     }

                     if (b) {
                        ItemHandlerHelper.insertItem(below, this.outStack.copy(), false);

                        for (int ix = 0; ix < 9; ix++) {
                           if (this.craftMatrix.getStackInSlot(ix) != null) {
                              ItemHandlerHelper.insertItem(below, this.craftMatrix.getStackInSlot(ix).copy(), false);
                           }
                        }

                        InventoryUtils.removeStackFrom(
                           this.getWorld(), this.getPos().up(), EnumFacing.DOWN, testStack, ThaumcraftInvHelper.InvFilter.BASEORE, false
                        );
                        this.world.addBlockEvent(this.getPos(), this.getBlockType(), 1, 0);
                        this.power--;
                        break;
                     }
                  }
               }
            }
         }
      }
   }

   private boolean craft(ItemStack inStack) {
      this.outStack = ItemStack.EMPTY;
      this.craftMatrix.clear();
      switch (this.type) {
         case 0:
            for (int ax = 0; ax < 9; ax++) {
               this.craftMatrix.setInventorySlotContents(ax, ItemHandlerHelper.copyStackWithSize(inStack, 1));
            }
            break;
         case 1:
            this.craftMatrix.setInventorySlotContents(0, ItemHandlerHelper.copyStackWithSize(inStack, 1));
            break;
         case 2:
            for (int ax = 0; ax < 2; ax++) {
               this.craftMatrix.setInventorySlotContents(ax, ItemHandlerHelper.copyStackWithSize(inStack, 1));
            }
            break;
         case 3:
            for (int ax = 0; ax < 2; ax++) {
               this.craftMatrix.setInventorySlotContents(ax * 3, ItemHandlerHelper.copyStackWithSize(inStack, 1));
            }
            break;
         case 4:
            for (int ax = 0; ax < 2; ax++) {
               for (int b = 0; b < 2; b++) {
                  this.craftMatrix.setInventorySlotContents(ax + b * 3, ItemHandlerHelper.copyStackWithSize(inStack, 1));
               }
            }
            break;
         case 5:
            for (int ax = 0; ax < 3; ax++) {
               this.craftMatrix.setInventorySlotContents(ax, ItemHandlerHelper.copyStackWithSize(inStack, 1));
            }
            break;
         case 6:
            for (int ax = 0; ax < 3; ax++) {
               this.craftMatrix.setInventorySlotContents(ax * 3, ItemHandlerHelper.copyStackWithSize(inStack, 1));
            }
            break;
         case 7:
            for (int ax = 0; ax < 6; ax++) {
               this.craftMatrix.setInventorySlotContents(ax, ItemHandlerHelper.copyStackWithSize(inStack, 1));
            }
            break;
         case 8:
            for (int ax = 0; ax < 2; ax++) {
               for (int b = 0; b < 3; b++) {
                  this.craftMatrix.setInventorySlotContents(ax + b * 3, ItemHandlerHelper.copyStackWithSize(inStack, 1));
               }
            }
            break;
         case 9:
            for (int a = 0; a < 9; a++) {
               if (a != 4) {
                  this.craftMatrix.setInventorySlotContents(a, ItemHandlerHelper.copyStackWithSize(inStack, 1));
               }
            }
      }

      IRecipe ir = CraftingManager.findMatchingRecipe(this.craftMatrix, this.world);
      if (ir == null) {
         return false;
      } else {
         this.outStack = ir.getCraftingResult(this.craftMatrix);
         NonNullList<ItemStack> aitemstack = CraftingManager.getRemainingItems(this.craftMatrix, this.world);

         for (int i = 0; i < aitemstack.size(); i++) {
            ItemStack itemstack1 = this.craftMatrix.getStackInSlot(i);
            ItemStack itemstack2 = (ItemStack)aitemstack.get(i);
            if (!itemstack1.isEmpty()) {
               this.craftMatrix.setInventorySlotContents(i, ItemStack.EMPTY);
            }

            if (!itemstack1.isEmpty() && this.craftMatrix.getStackInSlot(i).isEmpty()) {
               this.craftMatrix.setInventorySlotContents(i, itemstack2);
            }
         }

         return !this.outStack.isEmpty();
      }
   }

   public void cycle() {
      this.type++;
      if (this.type > 9) {
         this.type = 0;
      }

      this.syncTile(false);
      this.markDirty();
   }

   public boolean receiveClientEvent(int i, int j) {
      if (i == 1) {
         if (this.world.isRemote) {
            this.rotTicks = 10;
         }

         return true;
      } else {
         return super.receiveClientEvent(i, j);
      }
   }

   public RayTraceResult rayTrace(World world, Vec3d vec3d, Vec3d vec3d1, RayTraceResult fullblock) {
      return fullblock;
   }

   public void addTraceableCuboids(List<IndexedCuboid6> cuboids) {
      EnumFacing facing = BlockStateUtils.getFacing(this.getBlockMetadata());
      cuboids.add(new IndexedCuboid6(0, this.getCuboidByFacing(facing)));
   }

   public Cuboid6 getCuboidByFacing(EnumFacing facing) {
      switch (facing) {
         case EAST:
            return new Cuboid6(
               this.getPos().getX() + 0.125,
               this.getPos().getY() + 0.125,
               this.getPos().getZ() + 0.375,
               this.getPos().getX() + 0.25,
               this.getPos().getY() + 0.375,
               this.getPos().getZ() + 0.625
            );
         case NORTH:
            return new Cuboid6(
               this.getPos().getX() + 0.375,
               this.getPos().getY() + 0.125,
               this.getPos().getZ() + 0.75,
               this.getPos().getX() + 0.625,
               this.getPos().getY() + 0.375,
               this.getPos().getZ() + 0.875
            );
         case SOUTH:
            return new Cuboid6(
               this.getPos().getX() + 0.375,
               this.getPos().getY() + 0.125,
               this.getPos().getZ() + 0.125,
               this.getPos().getX() + 0.625,
               this.getPos().getY() + 0.375,
               this.getPos().getZ() + 0.25
            );
         default:
            return new Cuboid6(
               this.getPos().getX() + 0.75,
               this.getPos().getY() + 0.125,
               this.getPos().getZ() + 0.375,
               this.getPos().getX() + 0.875,
               this.getPos().getY() + 0.375,
               this.getPos().getZ() + 0.625
            );
      }
   }
}
