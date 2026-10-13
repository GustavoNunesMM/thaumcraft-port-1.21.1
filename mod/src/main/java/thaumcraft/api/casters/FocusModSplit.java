package thaumcraft.api.casters;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

/** A modifier that runs several sub-packages from the same point. */
public abstract class FocusModSplit extends FocusMod {
   private final List<FocusPackage> packages = new ArrayList<>();

   public final List<FocusPackage> getSplitPackages() {
      return packages;
   }

   public void deserialize(CompoundTag nbt) {
      packages.clear();
      for (Tag t : nbt.getList("packages", Tag.TAG_COMPOUND)) {
         FocusPackage fp = new FocusPackage();
         fp.deserialize((CompoundTag) t);
         packages.add(fp);
      }
   }

   public CompoundTag serialize() {
      CompoundTag nbt = new CompoundTag();
      ListTag list = new ListTag();
      for (FocusPackage node : packages) list.add(node.serialize());
      nbt.put("packages", list);
      return nbt;
   }

   @Override
   public float getPowerMultiplier() {
      return 0.75F;
   }
}
