package thaumcraft.common.golems.seals;

import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import thaumcraft.api.golems.EnumGolemTrait;
import thaumcraft.api.golems.IGolemAPI;
import thaumcraft.api.golems.seals.ISeal;
import thaumcraft.api.golems.seals.ISealGui;
import thaumcraft.api.golems.tasks.Task;

/** The empty defaults most seals share (the 1.12 seals each spelled them out). */
public abstract class SealBase implements ISeal, ISealGui {
   protected static final Random RAND = new Random(System.nanoTime());
   private final ResourceLocation icon;

   protected SealBase(String iconName) {
      this.icon = ResourceLocation.fromNamespaceAndPath("thaumcraft", "items/seals/" + iconName);
   }

   @Override
   public ResourceLocation getSealIcon() {
      return icon;
   }

   @Override
   public boolean canPlaceAt(Level level, BlockPos pos, Direction side) {
      return !level.isEmptyBlock(pos);
   }

   @Override
   public void onTaskStarted(Level level, IGolemAPI golem, Task task) {
   }

   @Override
   public void onTaskSuspension(Level level, Task task) {
   }

   @Override
   public void onRemoval(Level level, BlockPos pos, Direction side) {
   }

   @Override
   public void readCustomNBT(CompoundTag nbt, HolderLookup.Provider registries) {
   }

   @Override
   public void writeCustomNBT(CompoundTag nbt, HolderLookup.Provider registries) {
   }

   @Override
   public int[] getGuiCategories() {
      return new int[]{CAT_PRIORITY};
   }

   @Override
   public EnumGolemTrait[] getRequiredTags() {
      return null;
   }

   @Override
   public EnumGolemTrait[] getForbiddenTags() {
      return null;
   }
}
