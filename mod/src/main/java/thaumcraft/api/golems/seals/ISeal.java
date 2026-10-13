package thaumcraft.api.golems.seals;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import thaumcraft.api.golems.EnumGolemTrait;
import thaumcraft.api.golems.IGolemAPI;
import thaumcraft.api.golems.tasks.Task;

/**
 * A kind of seal. Each placed seal gets its own instance (made with {@link #newInstance()}), which holds its
 * configuration and the tasks it handed out. In 1.12 returnContainer/returnGui picked the seal's GUI; every seal used
 * the same one, which the seal menu now opens directly.
 */
public interface ISeal {
   String getKey();

   /** A fresh instance of this seal type (Class.newInstance in 1.12). */
   ISeal newInstance();

   boolean canPlaceAt(Level level, BlockPos pos, Direction side);

   void tickSeal(Level level, ISealEntity seal);

   void onTaskStarted(Level level, IGolemAPI golem, Task task);

   boolean onTaskCompletion(Level level, IGolemAPI golem, Task task);

   void onTaskSuspension(Level level, Task task);

   boolean canGolemPerformTask(IGolemAPI golem, Task task);

   void readCustomNBT(CompoundTag nbt, HolderLookup.Provider registries);

   void writeCustomNBT(CompoundTag nbt, HolderLookup.Provider registries);

   /** The seal's texture (a block atlas sprite, "thaumcraft:items/seals/seal_x"). */
   ResourceLocation getSealIcon();

   void onRemoval(Level level, BlockPos pos, Direction side);

   @Nullable
   EnumGolemTrait[] getRequiredTags();

   @Nullable
   EnumGolemTrait[] getForbiddenTags();
}
