package thaumcraft.common.tiles.devices;

import java.util.ArrayList;
import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.oredict.OreDictionary;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.ThaumcraftInvHelper;
import thaumcraft.api.aura.AuraHelper;
import thaumcraft.api.blocks.BlocksTC;
import thaumcraft.api.internal.CommonInternals;
import thaumcraft.client.fx.FXDispatcher;
import thaumcraft.common.lib.utils.BlockStateUtils;
import thaumcraft.common.lib.utils.InventoryUtils;
import thaumcraft.common.tiles.TileThaumcraftInventory;

public class TileInfernalFurnace extends TileThaumcraftInventory {
   public int furnaceCookTime;
   public int furnaceMaxCookTime;
   public int speedyTime;
   public int facingX = -5;
   public int facingZ = -5;

   @SideOnly(Side.CLIENT)
   public AxisAlignedBB getRenderBoundingBox() {
      return new AxisAlignedBB(
         this.getPos().getX() - 1.3,
         this.getPos().getY() - 1.3,
         this.getPos().getZ() - 1.3,
         this.getPos().getX() + 2.3,
         this.getPos().getY() + 2.3,
         this.getPos().getZ() + 2.3
      );
   }

   public TileInfernalFurnace() {
      super(32);
      this.furnaceCookTime = 0;
      this.furnaceMaxCookTime = 0;
      this.speedyTime = 0;
   }

   @Override
   public int[] getSlotsForFace(EnumFacing par1) {
      return par1 == EnumFacing.UP ? super.getSlotsForFace(par1) : new int[0];
   }

   @Override
   public boolean canExtractItem(int par1, ItemStack stack2, EnumFacing par3) {
      return false;
   }

   @Override
   public void readFromNBT(NBTTagCompound nbttagcompound) {
      super.readFromNBT(nbttagcompound);
      this.furnaceCookTime = nbttagcompound.getShort("CookTime");
      this.speedyTime = nbttagcompound.getShort("SpeedyTime");
   }

   @Override
   public NBTTagCompound writeToNBT(NBTTagCompound nbttagcompound) {
      super.writeToNBT(nbttagcompound);
      nbttagcompound.setShort("CookTime", (short)this.furnaceCookTime);
      nbttagcompound.setShort("SpeedyTime", (short)this.speedyTime);
      return nbttagcompound;
   }

   @Override
   public void update() {
      super.update();
      if (this.facingX == -5) {
         this.setFacing();
      }

      if (!this.world.isRemote) {
         boolean cookedflag = false;
         if (this.furnaceCookTime > 0) {
            this.furnaceCookTime--;
            cookedflag = true;
         }

         if (this.furnaceMaxCookTime <= 0) {
            this.furnaceMaxCookTime = this.calcCookTime();
         }

         if (this.furnaceCookTime > this.furnaceMaxCookTime) {
            this.furnaceCookTime = this.furnaceMaxCookTime;
         }

         if (this.furnaceCookTime <= 0 && cookedflag) {
            for (int a = 0; a < this.getSizeInventory(); a++) {
               if (this.getStackInSlot(a) != null && !this.getStackInSlot(a).isEmpty()) {
                  ItemStack itemstack = FurnaceRecipes.instance().getSmeltingResult(this.getStackInSlot(a));
                  if (itemstack != null && !itemstack.isEmpty()) {
                     if (this.speedyTime > 0) {
                        this.speedyTime--;
                     }

                     this.ejectItem(itemstack.copy(), this.getStackInSlot(a));
                     this.world.addBlockEvent(this.getPos(), BlocksTC.infernalFurnace, 3, 0);
                     if (this.getWorld().rand.nextInt(20) == 0) {
                        AuraHelper.polluteAura(this.getWorld(), this.getPos().offset(this.getFacing().getOpposite()), 1.0F, true);
                     }

                     this.decrStackSize(a, 1);
                     break;
                  }

                  this.setInventorySlotContents(a, ItemStack.EMPTY);
               }
            }
         }

         if (this.speedyTime <= 0) {
            this.speedyTime = (int)AuraHelper.drainVis(this.getWorld(), this.getPos(), 20.0F, false);
         }

         if (this.furnaceCookTime == 0 && !cookedflag) {
            for (int ax = 0; ax < this.getSizeInventory(); ax++) {
               if (this.canSmelt(this.getStackInSlot(ax))) {
                  this.furnaceMaxCookTime = this.calcCookTime();
                  this.furnaceCookTime = this.furnaceMaxCookTime;
                  break;
               }
            }
         }
      }
   }

   private int getBellows() {
      int bellows = 0;

      for (EnumFacing dir : EnumFacing.VALUES) {
         if (dir != EnumFacing.UP) {
            BlockPos p2 = this.pos.offset(dir, 2);
            TileEntity tile = this.world.getTileEntity(p2);
            if (tile != null
               && tile instanceof TileBellows
               && BlockStateUtils.getFacing(this.world.getBlockState(p2)) == dir.getOpposite()
               && this.world.getRedstonePowerFromNeighbors(p2) == 0) {
               bellows++;
            }
         }
      }

      return Math.min(4, bellows);
   }

   private int calcCookTime() {
      int b = this.getBellows();
      if (b > 0) {
         b = (20 - (b - 1)) * b;
      }

      return Math.max(10, (this.speedyTime > 0 ? 80 : 140) - b);
   }

   public ItemStack addItemsToInventory(ItemStack items) {
      if (this.canSmelt(items)) {
         items = ThaumcraftInvHelper.insertStackAt(this.getWorld(), this.getPos(), EnumFacing.UP, items, false);
      } else {
         this.destroyItem();
         items = ItemStack.EMPTY;
      }

      return items;
   }

   private void destroyItem() {
      this.world
         .playSound(
            this.pos.getX() + 0.5F,
            this.pos.getY() + 0.5F,
            this.pos.getZ() + 0.5F,
            SoundEvents.BLOCK_LAVA_EXTINGUISH,
            SoundCategory.BLOCKS,
            0.3F,
            2.6F + (this.world.rand.nextFloat() - this.world.rand.nextFloat()) * 0.8F,
            false
         );
      double var21 = this.pos.getX() + this.world.rand.nextFloat();
      double var22 = this.pos.getY() + 1;
      double var23 = this.pos.getZ() + this.world.rand.nextFloat();
      this.world.spawnParticle(EnumParticleTypes.LAVA, var21, var22, var23, 0.0, 0.0, 0.0, new int[0]);
   }

   public void ejectItem(ItemStack items, ItemStack furnaceItemStack) {
      if (items != null && !items.isEmpty()) {
         ArrayList<ItemStack> ejecti = new ArrayList<>();
         ejecti.add(items.copy());
         int bellows = this.getBellows() + 1;
         float lx = 0.5F;
         lx += this.facingX * 1.2F;
         float lz = 0.5F;
         lz += this.facingZ * 1.2F;
         float mx = 0.0F;
         float mz = 0.0F;

         for (int a = 0; a < bellows; a++) {
            ItemStack[] boni = this.getSmeltingBonus(furnaceItemStack);
            if (boni != null) {
               for (ItemStack bonus : boni) {
                  if (!bonus.isEmpty() && bonus.getCount() > 0) {
                     ejecti.add(bonus);
                  }
               }
            }
         }

         for (ItemStack outItem : ejecti) {
            if (!outItem.isEmpty()) {
               EnumFacing facing = BlockStateUtils.getFacing(this.getBlockMetadata()).getOpposite();
               InventoryUtils.ejectStackAt(this.getWorld(), this.getPos(), facing, outItem);
            }
         }

         int cnt = items.getCount();
         float xpf = FurnaceRecipes.instance().getSmeltingExperience(items);
         if (xpf == 0.0F) {
            cnt = 0;
         } else if (xpf < 1.0F) {
            int var4 = MathHelper.floor(cnt * xpf);
            if (var4 < MathHelper.ceil(cnt * xpf) && (float)Math.random() < cnt * xpf - var4) {
               var4++;
            }

            cnt = var4;
         }

         while (cnt > 0) {
            int var4 = EntityXPOrb.getXPSplit(cnt);
            cnt -= var4;
            EntityXPOrb xp = new EntityXPOrb(
               this.world,
               this.pos.getX() + lx,
               this.pos.getY() + 0.4F,
               this.pos.getZ() + lz,
               var4
            );
            mx = this.facingX == 0
               ? (this.world.rand.nextFloat() - this.world.rand.nextFloat()) * 0.025F
               : this.facingX * 0.13F;
            mz = this.facingZ == 0
               ? (this.world.rand.nextFloat() - this.world.rand.nextFloat()) * 0.025F
               : this.facingZ * 0.13F;
            xp.motionX = mx;
            xp.motionZ = mz;
            xp.motionY = 0.0;
            this.world.spawnEntity(xp);
         }
      }
   }

   private ItemStack[] getSmeltingBonus(ItemStack in) {
      ArrayList<ItemStack> out = new ArrayList<>();

      for (ThaumcraftApi.SmeltBonus bonus : CommonInternals.smeltingBonus) {
         if (!(bonus.in instanceof ItemStack)) {
            for (int id : OreDictionary.getOreIDs(in)) {
               String od = OreDictionary.getOreName(id);
               if (((String)bonus.in).equals(od)) {
                  if (this.world.rand.nextFloat() <= bonus.chance) {
                     ItemStack is = bonus.out.copy();
                     if (is.getCount() < 1) {
                        is.setCount(1);
                     }

                     out.add(is);
                  }
                  break;
               }
            }
         } else if (in.getItem() == ((ItemStack)bonus.in).getItem()
            && (in.getItemDamage() == ((ItemStack)bonus.in).getItemDamage() || ((ItemStack)bonus.in).getItemDamage() == 32767)
            && this.world.rand.nextFloat() <= bonus.chance) {
            ItemStack is = bonus.out.copy();
            if (is.getCount() < 1) {
               is.setCount(1);
            }

            out.add(is);
         }
      }

      return out.toArray(new ItemStack[0]);
   }

   private boolean canSmelt(ItemStack stack) {
      return !FurnaceRecipes.instance().getSmeltingResult(stack).isEmpty();
   }

   private void setFacing() {
      this.facingX = 0;
      this.facingZ = 0;
      EnumFacing face = this.getFacing().getOpposite();
      this.facingX = face.getXOffset();
      this.facingZ = face.getZOffset();
   }

   public boolean receiveClientEvent(int i, int j) {
      if (i != 3) {
         return super.receiveClientEvent(i, j);
      } else {
         if (this.world.isRemote) {
            for (int a = 0; a < 5; a++) {
               FXDispatcher.INSTANCE
                  .furnaceLavaFx(
                     this.pos.getX(), this.pos.getY(), this.pos.getZ(), this.facingX, this.facingZ
                  );
               this.world
                  .playSound(
                     this.pos.getX() + 0.5F,
                     this.pos.getY() + 0.5F,
                     this.pos.getZ() + 0.5F,
                     SoundEvents.BLOCK_LAVA_POP,
                     SoundCategory.BLOCKS,
                     0.1F + this.world.rand.nextFloat() * 0.1F,
                     0.9F + this.world.rand.nextFloat() * 0.15F,
                     false
                  );
            }
         }

         return true;
      }
   }
}
