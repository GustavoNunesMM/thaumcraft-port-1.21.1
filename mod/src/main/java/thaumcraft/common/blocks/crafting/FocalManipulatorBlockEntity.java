package thaumcraft.common.blocks.crafting;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.casters.FocusModSplit;
import thaumcraft.api.casters.FocusPackage;
import thaumcraft.common.blocks.TCBlockEntity;
import thaumcraft.common.items.VisCrystalItem;
import thaumcraft.common.items.casters.FocusItem;
import thaumcraft.common.menu.FocalManipulatorMenu;
import thaumcraft.common.registers.ModBlocks;
import thaumcraft.common.registers.ModCasters;
import thaumcraft.common.registers.ModSounds;
import thaumcraft.common.research.ResearchManager;
import thaumcraft.common.world.aura.AuraHandler;

/**
 * TileFocalManipulator: holds a focus and the node tree designed in its screen. Starting the craft takes experience
 * and vis crystals; vis is then drawn from the aura (20 a second) and the package is imprinted into the focus.
 */
public class FocalManipulatorBlockEntity extends TCBlockEntity implements MenuProvider {
   public float vis;
   public Map<Integer, FocusElementNode> data = new HashMap<>();
   public String focusName = "";
   private int ticks;
   public boolean doGather;
   public int xpCost;
   private AspectList crystals = new AspectList();
   public AspectList crystalsSync = new AspectList();
   /** Client: the focus slot changed, so the screen rebuilds its tree. */
   public boolean doGuiReset;
   private boolean silent;

   public final SimpleContainer inventory = new SimpleContainer(1) {
      @Override
      public void setItem(int slot, ItemStack stack) {
         ItemStack prev = getItem(slot);
         super.setItem(slot, stack);
         if (!silent && (stack.isEmpty() || !ItemStack.matches(stack, prev))) slotChanged();
      }

      @Override
      public ItemStack removeItem(int slot, int amount) {
         ItemStack r = super.removeItem(slot, amount);
         if (!silent && !r.isEmpty()) slotChanged();
         return r;
      }

      @Override
      public ItemStack removeItemNoUpdate(int slot) {
         ItemStack r = super.removeItemNoUpdate(slot);
         if (!silent && !r.isEmpty()) slotChanged();
         return r;
      }

      @Override
      public boolean canPlaceItem(int slot, ItemStack stack) {
         return stack.getItem() instanceof FocusItem;
      }

      @Override
      public int getMaxStackSize() {
         return 1;
      }

      @Override
      public void setChanged() {
         super.setChanged();
         FocalManipulatorBlockEntity.this.setChanged();
      }

      @Override
      public boolean stillValid(Player player) {
         return level != null && level.getBlockEntity(worldPosition) == FocalManipulatorBlockEntity.this
            && player.distanceToSqr(worldPosition.getCenter()) <= 64.0;
      }
   };

   public FocalManipulatorBlockEntity(BlockPos pos, BlockState state) {
      super(ModCasters.FOCAL_MANIPULATOR_BE.get(), pos, state);
   }

   public ItemStack getFocus() {
      return inventory.getItem(0);
   }

   private void slotChanged() {
      if (level == null) return;
      if (level.isClientSide) {
         data.clear();
         doGuiReset = true;
      } else {
         vis = 0.0F;
         crystalsSync = new AspectList();
         data.clear();
         sync();
      }
   }

   // ------------------------------------------------------------------ sync

   @Override
   protected void readSync(CompoundTag nbt, HolderLookup.Provider registries) {
      vis = nbt.getFloat("vis");
      focusName = nbt.getString("focusName");
      crystalsSync = readAspects(nbt.getCompound("crystals"));
      data.clear();
      for (Tag t : nbt.getList("nodes", Tag.TAG_COMPOUND)) {
         FocusElementNode node = new FocusElementNode();
         node.deserialize((CompoundTag) t);
         data.put(node.id, node);
      }
      silent = true;
      inventory.setItem(0, nbt.contains("focus") ? ItemStack.parseOptional(registries, nbt.getCompound("focus")) : ItemStack.EMPTY);
      silent = false;
      // An empty tree from the server (new focus, finished craft): the screen starts a fresh one.
      if (level != null && level.isClientSide && data.isEmpty() && !getFocus().isEmpty()) doGuiReset = true;
   }

   @Override
   protected void writeSync(CompoundTag nbt, HolderLookup.Provider registries) {
      nbt.putFloat("vis", vis);
      nbt.putString("focusName", focusName);
      nbt.put("crystals", writeAspects(crystalsSync));
      ListTag nodelist = new ListTag();
      for (FocusElementNode node : data.values()) nodelist.add(node.serialize());
      nbt.put("nodes", nodelist);
      if (!getFocus().isEmpty()) nbt.put("focus", getFocus().save(registries));
   }

   @Override
   protected void writeExtra(CompoundTag tag, HolderLookup.Provider registries) {
      tag.putInt("xpCost", xpCost);
   }

   @Override
   protected void readExtra(CompoundTag tag, HolderLookup.Provider registries) {
      xpCost = tag.getInt("xpCost");
   }

   private static CompoundTag writeAspects(AspectList al) {
      CompoundTag t = new CompoundTag();
      for (Aspect a : al.getAspects()) t.putInt(a.getTag(), al.getAmount(a));
      return t;
   }

   private static AspectList readAspects(CompoundTag t) {
      AspectList al = new AspectList();
      for (String k : t.getAllKeys()) {
         Aspect a = Aspect.getAspect(k);
         if (a != null) al.add(a, t.getInt(k));
      }
      return al;
   }

   // ------------------------------------------------------------------ crafting

   /** Vis from this chunk, or from the 3x3 chunks around with an arcane workbench charger on top. */
   public float spendAura(float amount) {
      if (!level.getBlockState(worldPosition.above()).is(ModBlocks.ARCANE_WORKBENCH_CHARGER.get())) {
         return AuraHandler.drainVis(level, worldPosition, amount, false);
      }
      float q = amount;
      float z = amount / 9.0F;
      for (int xx = -1; xx <= 1; xx++) {
         for (int zz = -1; zz <= 1; zz++) {
            if (z > q) z = q;
            q -= AuraHandler.drainVis(level, worldPosition.offset(xx * 16, 0, zz * 16), z, false);
            if (q <= 0.0F) return amount - q;
         }
      }
      return amount - q;
   }

   public static void tick(Level level, BlockPos pos, BlockState state, FocalManipulatorBlockEntity be) {
      be.tick();
   }

   private void tick() {
      boolean complete = false;
      ticks++;
      if (!level.isClientSide) {
         if (ticks % 20 == 0) {
            if (vis > 0.0F && !(getFocus().getItem() instanceof FocusItem)) {
               complete = true;
               vis = 0.0F;
               level.playSound(null, worldPosition, ModSounds.WANDFAIL.get(), SoundSource.BLOCKS, 0.33F, 1.0F);
            }
            if (!complete && vis > 0.0F) {
               float amt = spendAura(Math.min(20.0F, vis));
               if (amt > 0.0F) {
                  level.blockEvent(worldPosition, getBlockState().getBlock(), 5, 1);
                  vis -= amt;
                  sync();
               }
               if (vis <= 0.0F && getFocus().getItem() instanceof FocusItem) {
                  complete = true;
                  endCraft();
               }
            }
         }
      } else if (vis > 0.0F) {
         thaumcraft.client.fx.FocusEntityFX.manipulatorCrafting(level, worldPosition);
      }
      if (complete) {
         vis = 0.0F;
         sync();
      }
   }

   /** The package the node tree describes (root first; split nodes get one sub-package per branch). */
   @Nullable
   public FocusPackage generateFocus() {
      if (data.isEmpty()) return null;
      FocusPackage core = new FocusPackage();
      int totalComplexity = 0;
      Map<String, Integer> compCount = new HashMap<>();
      for (FocusElementNode node : data.values()) {
         if (node.node != null) {
            int a = compCount.getOrDefault(node.node.getKey(), 0);
            node.complexityMultiplier = 0.5F * (++a + 1);
            compCount.put(node.node.getKey(), a);
            totalComplexity = (int) (totalComplexity + node.node.getComplexity() * node.complexityMultiplier);
         }
      }
      core.setComplexity(totalComplexity);
      traverseChildren(core, data.get(0));
      return core;
   }

   private void traverseChildren(FocusPackage currentPackage, @Nullable FocusElementNode currentNode) {
      if (currentPackage == null || currentNode == null) return;
      currentPackage.addNode(currentNode.node);
      if (currentNode.children == null || currentNode.children.length == 0) return;
      if (currentNode.children.length == 1) {
         traverseChildren(currentPackage, data.get(currentNode.children[0]));
      } else if (currentNode.node instanceof FocusModSplit splitNode) {
         splitNode.getSplitPackages().clear();
         for (int c : currentNode.children) {
            FocusPackage splitPackage = new FocusPackage();
            traverseChildren(splitPackage, data.get(c));
            splitNode.getSplitPackages().add(splitPackage);
         }
      }
   }

   public void endCraft() {
      vis = 0.0F;
      if (getFocus().getItem() instanceof FocusItem) {
         FocusPackage core = generateFocus();
         if (core != null) {
            level.playSound(null, worldPosition, ModSounds.WAND.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
            ItemStack focus = getFocus().copy();
            if (focusName != null && !focusName.isEmpty()) focus.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, Component.literal(focusName));
            FocusItem.setPackage(focus, core);
            inventory.setItem(0, focus);
            crystalsSync = new AspectList();
            data.clear();
            sync();
         }
      }
   }

   /** Checks the tree, the research, experience and crystals; takes them and starts drawing vis. */
   public boolean startCraft(Player p) {
      if (data.isEmpty() || vis > 0.0F || !(getFocus().getItem() instanceof FocusItem fi)) return false;
      int maxComplexity = fi.getMaxComplexity();
      int totalComplexity = 0;
      crystals = new AspectList();
      Map<String, Integer> compCount = new HashMap<>();
      for (FocusElementNode node : data.values()) {
         if (node.node == null) return false;
         if (node.node.getResearch() != null && !ResearchManager.knowsResearchStrict(p, node.node.getResearch())) return false;
         int a = compCount.getOrDefault(node.node.getKey(), 0);
         node.complexityMultiplier = 0.5F * (++a + 1);
         compCount.put(node.node.getKey(), a);
         totalComplexity = (int) (totalComplexity + node.node.getComplexity() * node.complexityMultiplier);
         if (node.node.getAspect() != null) crystals.add(node.node.getAspect(), 1);
      }
      vis = totalComplexity * 10 + maxComplexity / 5;
      xpCost = (int) Math.max(1L, Math.round(Math.sqrt(totalComplexity)));
      if (!p.getAbilities().instabuild && p.experienceLevel < xpCost) {
         vis = 0.0F;
         return false;
      }
      if (crystals.getAspects().length <= 0) {
         vis = 0.0F;
         return false;
      }
      for (Aspect as : crystals.getAspects()) {
         if (countCrystals(p, as) < crystals.getAmount(as)) {
            vis = 0.0F;
            return false;
         }
      }
      if (!p.getAbilities().instabuild) p.giveExperienceLevels(-xpCost);
      for (Aspect as : crystals.getAspects()) consumeCrystals(p, as, crystals.getAmount(as));
      crystalsSync = crystals.copy();
      sync();
      level.playSound(null, worldPosition, ModSounds.CRAFTSTART.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
      return true;
   }

   /** The vis crystals of an aspect the player carries. */
   public static int countCrystals(Player p, Aspect as) {
      int n = 0;
      for (ItemStack s : p.getInventory().items) {
         if (s.getItem() instanceof VisCrystalItem && VisCrystalItem.getAspect(s) == as) n += s.getCount();
      }
      return n;
   }

   private static void consumeCrystals(Player p, Aspect as, int amount) {
      List<ItemStack> items = p.getInventory().items;
      for (ItemStack s : items) {
         if (amount <= 0) break;
         if (s.getItem() instanceof VisCrystalItem && VisCrystalItem.getAspect(s) == as) {
            int t = Math.min(amount, s.getCount());
            s.shrink(t);
            amount -= t;
         }
      }
      p.getInventory().setChanged();
   }

   @Override
   public boolean triggerEvent(int i, int j) {
      if (i == 1) {
         doGuiReset = true;
         return true;
      }
      if (i == 5) {
         if (level != null && level.isClientSide) thaumcraft.client.fx.FocusEntityFX.manipulatorSparkle(level, worldPosition);
         return true;
      }
      return super.triggerEvent(i, j);
   }

   /** The crystals the current tree needs (for the screen). */
   public static ItemStack crystal(Aspect a, int n) {
      return VisCrystalItem.of(a, n);
   }

   @Override
   public Component getDisplayName() {
      return Component.translatable("block.thaumcraft.wand_workbench");
   }

   @Override
   public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
      return new FocalManipulatorMenu(id, inv, this);
   }
}
