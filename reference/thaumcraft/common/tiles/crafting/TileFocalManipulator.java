package thaumcraft.common.tiles.crafting;

import java.util.HashMap;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.blocks.BlocksTC;
import thaumcraft.api.capabilities.ThaumcraftCapabilities;
import thaumcraft.api.casters.FocusModSplit;
import thaumcraft.api.casters.FocusPackage;
import thaumcraft.client.fx.FXDispatcher;
import thaumcraft.common.items.casters.ItemFocus;
import thaumcraft.common.lib.SoundsTC;
import thaumcraft.common.lib.utils.InventoryUtils;
import thaumcraft.common.tiles.TileThaumcraftInventory;
import thaumcraft.common.world.aura.AuraHandler;

public class TileFocalManipulator extends TileThaumcraftInventory {
   public float vis = 0.0F;
   public HashMap<Integer, FocusElementNode> data = new HashMap<>();
   public String focusName = "";
   int ticks = 0;
   public boolean doGather;
   public float visCost = 0.0F;
   public int xpCost = 0;
   private AspectList crystals = new AspectList();
   public AspectList crystalsSync = new AspectList();
   public boolean doGuiReset = false;

   public TileFocalManipulator() {
      super(1);
      this.syncedSlots = new int[]{0};
   }

   @Override
   public void readSyncNBT(NBTTagCompound nbt) {
      super.readSyncNBT(nbt);
      this.vis = nbt.getFloat("vis");
      this.focusName = nbt.getString("focusName");
      this.crystalsSync = new AspectList();
      this.crystalsSync.readFromNBT(nbt, "crystals");
      NBTTagList nodelist = nbt.getTagList("nodes", 10);
      this.data.clear();

      for (int x = 0; x < nodelist.tagCount(); x++) {
         NBTTagCompound nodenbt = nodelist.getCompoundTagAt(x);
         FocusElementNode node = new FocusElementNode();
         node.deserialize(nodenbt);
         this.data.put(node.id, node);
      }
   }

   @Override
   public NBTTagCompound writeSyncNBT(NBTTagCompound nbt) {
      super.writeSyncNBT(nbt);
      nbt.setFloat("vis", this.vis);
      nbt.setString("focusName", this.focusName);
      this.crystalsSync.writeToNBT(nbt, "crystals");
      NBTTagList nodelist = new NBTTagList();

      for (FocusElementNode node : this.data.values()) {
         nodelist.appendTag(node.serialize());
      }

      nbt.setTag("nodes", nodelist);
      return nbt;
   }

   @SideOnly(Side.CLIENT)
   public AxisAlignedBB getRenderBoundingBox() {
      return new AxisAlignedBB(
         this.pos.getX(),
         this.pos.getY(),
         this.pos.getZ(),
         this.pos.getX() + 1,
         this.pos.getY() + 1,
         this.pos.getZ() + 1
      );
   }

   @Override
   public void setInventorySlotContents(int par1, ItemStack stack) {
      ItemStack prev = this.getStackInSlot(par1);
      super.setInventorySlotContents(par1, stack);
      if (stack.isEmpty() || !ItemStack.areItemStacksEqual(stack, prev)) {
         if (this.world.isRemote) {
            this.data.clear();
            this.doGuiReset = true;
         } else {
            this.vis = 0.0F;
            this.crystalsSync = new AspectList();
            this.markDirty();
            this.syncSlots(null);
         }
      }
   }

   public float spendAura(float vis) {
      if (this.world.getBlockState(this.getPos().up()).getBlock() != BlocksTC.arcaneWorkbenchCharger) {
         return AuraHandler.drainVis(this.getWorld(), this.getPos(), vis, false);
      } else {
         float q = vis;
         float z = vis / 9.0F;

         for (int xx = -1; xx <= 1; xx++) {
            for (int zz = -1; zz <= 1; zz++) {
               if (z > q) {
                  z = q;
               }

               q -= AuraHandler.drainVis(this.getWorld(), this.getPos().add(xx * 16, 0, zz * 16), z, false);
               if (q <= 0.0F) {
                  return vis - q;
               }
            }
         }

         return vis - q;
      }
   }

   @Override
   public void update() {
      super.update();
      boolean complete = false;
      this.ticks++;
      if (!this.world.isRemote) {
         if (this.ticks % 20 == 0) {
            if (this.vis > 0.0F
               && (this.getStackInSlot(0) == null || this.getStackInSlot(0).isEmpty() || !(this.getStackInSlot(0).getItem() instanceof ItemFocus))) {
               complete = true;
               this.vis = 0.0F;
               this.world.playSound(null, this.pos, SoundsTC.wandfail, SoundCategory.BLOCKS, 0.33F, 1.0F);
            }

            if (!complete && this.vis > 0.0F) {
               float amt = this.spendAura(Math.min(20.0F, this.vis));
               if (amt > 0.0F) {
                  this.world.addBlockEvent(this.pos, this.getBlockType(), 5, 1);
                  this.vis -= amt;
                  this.syncTile(false);
                  this.markDirty();
               }

               if (this.vis <= 0.0F
                  && this.getStackInSlot(0) != null
                  && !this.getStackInSlot(0).isEmpty()
                  && this.getStackInSlot(0).getItem() instanceof ItemFocus) {
                  complete = true;
                  this.endCraft();
               }
            }
         }
      } else if (this.vis > 0.0F) {
         FXDispatcher.INSTANCE
            .drawGenericParticles(
               this.pos.getX()
                  + 0.5
                  + (this.world.rand.nextFloat() - this.world.rand.nextFloat()) * 0.3F,
               this.pos.getY()
                  + 1.4
                  + (this.world.rand.nextFloat() - this.world.rand.nextFloat()) * 0.3F,
               this.pos.getZ()
                  + 0.5
                  + (this.world.rand.nextFloat() - this.world.rand.nextFloat()) * 0.3F,
               0.0,
               0.0,
               0.0,
               0.5F + this.world.rand.nextFloat() * 0.4F,
               1.0F - this.world.rand.nextFloat() * 0.4F,
               1.0F - this.world.rand.nextFloat() * 0.4F,
               0.8F,
               false,
               448,
               9,
               1,
               6 + this.world.rand.nextInt(5),
               0,
               0.3F + this.world.rand.nextFloat() * 0.3F,
               0.0F,
               0
            );
      }

      if (complete) {
         this.vis = 0.0F;
         this.syncTile(false);
         this.markDirty();
      }
   }

   private FocusPackage generateFocus() {
      if (this.data != null && !this.data.isEmpty()) {
         FocusPackage core = new FocusPackage();
         int totalComplexity = 0;
         HashMap<String, Integer> compCount = new HashMap<>();

         for (FocusElementNode node : this.data.values()) {
            if (node.node != null) {
               int a = 0;
               if (compCount.containsKey(node.node.getKey())) {
                  a = compCount.get(node.node.getKey());
               }

               node.complexityMultiplier = 0.5F * (++a + 1);
               compCount.put(node.node.getKey(), a);
               totalComplexity = (int)(totalComplexity + node.node.getComplexity() * node.complexityMultiplier);
            }
         }

         core.setComplexity(totalComplexity);
         FocusElementNode root = this.data.get(0);
         this.traverseChildren(core, root);
         return core;
      } else {
         return null;
      }
   }

   private void traverseChildren(FocusPackage currentPackage, FocusElementNode currentNode) {
      if (currentPackage != null && currentNode != null) {
         currentPackage.addNode(currentNode.node);
         if (currentNode.children != null && currentNode.children.length != 0) {
            if (currentNode.children.length == 1) {
               this.traverseChildren(currentPackage, this.data.get(currentNode.children[0]));
            } else {
               FocusModSplit splitNode = (FocusModSplit)currentNode.node;
               splitNode.getSplitPackages().clear();

               for (int c : currentNode.children) {
                  FocusPackage splitPackage = new FocusPackage();
                  this.traverseChildren(splitPackage, this.data.get(c));
                  splitNode.getSplitPackages().add(splitPackage);
               }
            }
         }
      }
   }

   public void endCraft() {
      this.vis = 0.0F;
      if (this.getStackInSlot(0) != null && !this.getStackInSlot(0).isEmpty() && this.getStackInSlot(0).getItem() instanceof ItemFocus) {
         FocusPackage core = this.generateFocus();
         if (core != null) {
            this.world.playSound(null, this.pos, SoundsTC.wand, SoundCategory.BLOCKS, 1.0F, 1.0F);
            ItemStack focus = this.getStackInSlot(0);
            if (focus.getTagCompound() != null) {
               focus.getTagCompound().removeTag("color");
            }

            focus.setStackDisplayName(this.focusName);
            ItemFocus.setPackage(focus, core);
            this.setInventorySlotContents(0, focus);
            this.crystalsSync = new AspectList();
            this.data.clear();
            this.syncTile(false);
            this.markDirty();
         }
      }
   }

   public boolean startCraft(int id, EntityPlayer p) {
      if (this.data != null
         && !this.data.isEmpty()
         && !(this.vis > 0.0F)
         && this.getStackInSlot(0) != null
         && !this.getStackInSlot(0).isEmpty()
         && this.getStackInSlot(0).getItem() instanceof ItemFocus) {
         int maxComplexity = ((ItemFocus)this.getStackInSlot(0).getItem()).getMaxComplexity();
         int totalComplexity = 0;
         this.crystals = new AspectList();
         HashMap<String, Integer> compCount = new HashMap<>();

         for (FocusElementNode node : this.data.values()) {
            if (node.node == null) {
               return false;
            }

            if (!ThaumcraftCapabilities.knowsResearchStrict(p, node.node.getResearch())) {
               return false;
            }

            int a = 0;
            if (compCount.containsKey(node.node.getKey())) {
               a = compCount.get(node.node.getKey());
            }

            node.complexityMultiplier = 0.5F * (++a + 1);
            compCount.put(node.node.getKey(), a);
            totalComplexity = (int)(totalComplexity + node.node.getComplexity() * node.complexityMultiplier);
            if (node.node.getAspect() != null) {
               this.crystals.add(node.node.getAspect(), 1);
            }
         }

         this.vis = totalComplexity * 10 + maxComplexity / 5;
         this.xpCost = (int)Math.max(1L, Math.round(Math.sqrt(totalComplexity)));
         if (!p.capabilities.isCreativeMode && p.experienceLevel < this.xpCost) {
            this.vis = 0.0F;
            return false;
         } else {
            if (!p.capabilities.isCreativeMode) {
               p.addExperienceLevel(-this.xpCost);
            }

            if (this.crystals.getAspects().length <= 0) {
               this.vis = 0.0F;
               return false;
            } else {
               ItemStack[] components = new ItemStack[this.crystals.getAspects().length];
               int r = 0;

               for (Aspect as : this.crystals.getAspects()) {
                  components[r] = ThaumcraftApiHelper.makeCrystal(as, this.crystals.getAmount(as));
                  r++;
               }

               if (components.length >= 0) {
                  for (int ax = 0; ax < components.length; ax++) {
                     if (!InventoryUtils.isPlayerCarryingAmount(p, components[ax], false)) {
                        this.vis = 0.0F;
                        return false;
                     }
                  }

                  for (int axx = 0; axx < components.length; axx++) {
                     InventoryUtils.consumePlayerItem(p, components[axx], true, false);
                  }

                  this.crystalsSync = this.crystals.copy();
               }

               this.markDirty();
               this.syncTile(false);
               this.world.playSound(null, this.pos, SoundsTC.craftstart, SoundCategory.BLOCKS, 1.0F, 1.0F);
               return true;
            }
         }
      } else {
         return false;
      }
   }

   @Override
   public boolean isItemValidForSlot(int par1, ItemStack stack) {
      return stack.getItem() instanceof ItemFocus;
   }

   public boolean receiveClientEvent(int i, int j) {
      if (i == 1) {
         this.doGuiReset = true;
      }

      if (i == 5) {
         if (this.world.isRemote) {
            FXDispatcher.INSTANCE
               .visSparkle(
                  this.pos.getX() + this.getWorld().rand.nextInt(3) - this.getWorld().rand.nextInt(3),
                  this.pos.getY() + this.getWorld().rand.nextInt(3),
                  this.pos.getZ() + this.getWorld().rand.nextInt(3) - this.getWorld().rand.nextInt(3),
                  this.pos.getX(),
                  this.pos.getY() + 1,
                  this.pos.getZ(),
                  j
               );
         }

         return true;
      } else {
         return super.receiveClientEvent(i, j);
      }
   }
}
