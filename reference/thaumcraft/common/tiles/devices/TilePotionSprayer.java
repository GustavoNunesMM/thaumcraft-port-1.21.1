package thaumcraft.common.tiles.devices;

import java.util.List;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.potion.PotionType;
import net.minecraft.potion.PotionUtils;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectHelper;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IAspectContainer;
import thaumcraft.api.aspects.IEssentiaTransport;
import thaumcraft.client.fx.FXDispatcher;
import thaumcraft.common.config.ConfigAspects;
import thaumcraft.common.container.slot.SlotPotion;
import thaumcraft.common.lib.utils.BlockStateUtils;
import thaumcraft.common.tiles.TileThaumcraftInventory;

public class TilePotionSprayer extends TileThaumcraftInventory implements IAspectContainer, IEssentiaTransport {
   public AspectList recipe = new AspectList();
   public AspectList recipeProgress = new AspectList();
   public int charges = 0;
   public int color = 0;
   int counter = 0;
   boolean activated = false;
   int venting = 0;
   Aspect currentSuction = null;

   public TilePotionSprayer() {
      super(1);
   }

   @Override
   public void update() {
      super.update();
      this.counter++;
      EnumFacing facing = BlockStateUtils.getFacing(this.getBlockMetadata());
      if (this.world.isRemote) {
         if (this.venting > 0) {
            this.venting--;

            for (int a = 0; a < this.venting / 2; a++) {
               float fx = 0.1F - this.world.rand.nextFloat() * 0.2F;
               float fz = 0.1F - this.world.rand.nextFloat() * 0.2F;
               float fy = 0.1F - this.world.rand.nextFloat() * 0.2F;
               float fx2 = (float)(this.world.rand.nextGaussian() * 0.06);
               float fz2 = (float)(this.world.rand.nextGaussian() * 0.06);
               float fy2 = (float)(this.world.rand.nextGaussian() * 0.06);
               FXDispatcher.INSTANCE
                  .drawVentParticles2(
                     this.pos.getX() + 0.5F + fx + facing.getXOffset() / 2.0F,
                     this.pos.getY() + 0.5F + fy + facing.getYOffset() / 2.0F,
                     this.pos.getZ() + 0.5F + fz + facing.getZOffset() / 2.0F,
                     fx2 + facing.getXOffset() * 0.25,
                     fy2 + facing.getYOffset() * 0.25,
                     fz2 + facing.getZOffset() * 0.25,
                     this.color,
                     4.0F
                  );
            }
         }
      } else {
         if (this.counter % 5 == 0) {
            this.currentSuction = null;
            if (this.getStackInSlot(0).isEmpty() || this.charges >= 8) {
               return;
            }

            boolean done = true;

            for (Aspect aspect : this.recipe.getAspectsSortedByName()) {
               if (this.recipeProgress.getAmount(aspect) < this.recipe.getAmount(aspect)) {
                  this.currentSuction = aspect;
                  done = false;
                  break;
               }
            }

            if (done) {
               this.recipeProgress = new AspectList();
               this.charges++;
               this.syncTile(false);
               this.markDirty();
            } else if (this.currentSuction != null) {
               this.fill();
            }
         }

         if (!BlockStateUtils.isEnabled(this.getBlockMetadata())) {
            if (!this.activated && this.charges > 0) {
               this.charges--;
               List<PotionEffect> effects = PotionUtils.getEffectsFromStack(this.getStackInSlot(0));
               if (effects != null && !effects.isEmpty()) {
                  int area = 1;
                  BlockPos p = this.pos.offset(facing, 2);
                  List<EntityLivingBase> targets = this.world
                     .getEntitiesWithinAABB(
                        EntityLivingBase.class,
                        new AxisAlignedBB(
                           p.getX() - area,
                           p.getY() - area,
                           p.getZ() - area,
                           p.getX() + 1 + area,
                           p.getY() + 1 + area,
                           p.getZ() + 1 + area
                        )
                     );
                  boolean lifted = false;
                  if (targets.size() > 0) {
                     for (EntityLivingBase e : targets) {
                        if (!e.isDead && e.canBeHitWithPotion()) {
                           for (PotionEffect potioneffect1 : effects) {
                              Potion potion = potioneffect1.getPotion();
                              if (potion.isInstant()) {
                                 potion.affectEntity(null, null, e, potioneffect1.getAmplifier(), 1.0);
                              } else {
                                 e.addPotionEffect(new PotionEffect(potion, potioneffect1.getDuration(), potioneffect1.getAmplifier()));
                              }
                           }
                        }
                     }
                  }
               }

               this.world
                  .playSound(
                     null,
                     this.pos,
                     SoundEvents.BLOCK_LAVA_EXTINGUISH,
                     SoundCategory.BLOCKS,
                     0.25F,
                     2.6F + (this.world.rand.nextFloat() - this.world.rand.nextFloat()) * 0.8F
                  );
               this.world.addBlockEvent(this.getPos(), this.getBlockType(), 0, 0);
               this.syncTile(false);
               this.markDirty();
            }

            this.activated = true;
         } else if (this.activated) {
            this.activated = false;
         }
      }
   }

   private void drawFX(EnumFacing facing, double c) {
   }

   public boolean receiveClientEvent(int i, int j) {
      if (i >= 0) {
         if (this.world.isRemote) {
            this.venting = 15;
         }

         return true;
      } else {
         return super.receiveClientEvent(i, j);
      }
   }

   @Override
   public void readSyncNBT(NBTTagCompound nbt) {
      this.recipe = new AspectList();
      this.recipe.readFromNBT(nbt, "recipe");
      this.recipeProgress = new AspectList();
      this.recipeProgress.readFromNBT(nbt, "progress");
      this.charges = nbt.getInteger("charges");
      this.color = nbt.getInteger("color");
   }

   @Override
   public NBTTagCompound writeSyncNBT(NBTTagCompound nbt) {
      this.recipe.writeToNBT(nbt, "recipe");
      this.recipeProgress.writeToNBT(nbt, "progress");
      nbt.setInteger("charges", this.charges);
      nbt.setInteger("color", this.color);
      return nbt;
   }

   @Override
   public boolean isItemValidForSlot(int par1, ItemStack stack) {
      return stack != null && !stack.isEmpty() && SlotPotion.isValidPotion(stack);
   }

   @Override
   public void setInventorySlotContents(int par1, ItemStack stack) {
      super.setInventorySlotContents(par1, stack);
      this.recalcAspects();
   }

   @Override
   public ItemStack decrStackSize(int par1, int par2) {
      ItemStack stack = super.decrStackSize(par1, par2);
      this.recalcAspects();
      return stack;
   }

   private void recalcAspects() {
      if (!this.world.isRemote) {
         ItemStack stack = this.getStackInSlot(0);
         this.color = 3355443;
         if (!this.world.isRemote) {
            if (stack != null && !stack.isEmpty()) {
               this.recipe = ConfigAspects.getPotionAspects(stack);
               this.color = this.getPotionColor(stack);
            } else {
               this.recipe = new AspectList();
            }

            this.charges = 0;
            this.recipe = AspectHelper.cullTags(this.recipe, 10);
            this.recipeProgress = new AspectList();
            this.syncTile(false);
            this.markDirty();
         }
      }
   }

   public int getPotionColor(ItemStack itemstack) {
      PotionType potion = PotionUtils.getPotionFromItem(itemstack);
      return potion != null ? PotionUtils.getPotionColor(potion) : 3355443;
   }

   void fill() {
      EnumFacing facing = BlockStateUtils.getFacing(this.getBlockMetadata());
      TileEntity te = null;
      IEssentiaTransport ic = null;

      for (int y = 0; y <= 1; y++) {
         for (EnumFacing dir : EnumFacing.VALUES) {
            if (dir != facing) {
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
      int ce = this.recipe.getAmount(tt) - this.recipeProgress.getAmount(tt);
      if (ce <= 0) {
         return am;
      } else {
         int add = Math.min(ce, am);
         this.recipeProgress.add(tt, add);
         this.syncTile(false);
         this.markDirty();
         return am - add;
      }
   }

   @Override
   public boolean takeFromContainer(Aspect tt, int am) {
      return false;
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
      return this.recipeProgress.getAmount(tt) >= am;
   }

   @Override
   public int containerContains(Aspect tt) {
      return this.recipeProgress.getAmount(tt);
   }

   @Override
   public boolean doesContainerAccept(Aspect tag) {
      return true;
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
      return this.recipeProgress;
   }

   @Override
   public void setAspects(AspectList aspects) {
      this.recipeProgress = aspects;
   }
}
