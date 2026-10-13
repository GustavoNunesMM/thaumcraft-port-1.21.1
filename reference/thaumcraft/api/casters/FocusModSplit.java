package thaumcraft.api.casters;

import java.util.ArrayList;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

public abstract class FocusModSplit extends FocusMod {
   private ArrayList<FocusPackage> packages = new ArrayList<>();

   public final ArrayList<FocusPackage> getSplitPackages() {
      return this.packages;
   }

   public void deserialize(NBTTagCompound nbt) {
      NBTTagList nodelist = nbt.getTagList("packages", 10);
      this.packages.clear();

      for (int x = 0; x < nodelist.tagCount(); x++) {
         NBTTagCompound nodenbt = nodelist.getCompoundTagAt(x);
         FocusPackage fp = new FocusPackage();
         fp.deserialize(nodenbt);
         this.packages.add(fp);
      }
   }

   public NBTTagCompound serialize() {
      NBTTagCompound nbt = new NBTTagCompound();
      NBTTagList nodelist = new NBTTagList();

      for (FocusPackage node : this.packages) {
         nodelist.appendTag(node.serialize());
      }

      nbt.setTag("packages", nodelist);
      return nbt;
   }

   @Override
   public float getPowerMultiplier() {
      return 0.75F;
   }
}
