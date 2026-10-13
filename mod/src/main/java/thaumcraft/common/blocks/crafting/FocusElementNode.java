package thaumcraft.common.blocks.crafting;

import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import thaumcraft.api.casters.FocusEngine;
import thaumcraft.api.casters.FocusNode;
import thaumcraft.api.casters.IFocusElement;

/** A node of the focal manipulator's tree: a focus element (or an empty socket), its place and links. */
public class FocusElementNode {
   public int x;
   public int y;
   public int id;
   public boolean target;
   public boolean trajectory;
   public int parent = -1;
   public int[] children = new int[0];
   public float complexityMultiplier = 1.0F;
   @Nullable
   public FocusNode node;

   public float getPower(Map<Integer, FocusElementNode> data) {
      if (node == null) return 1.0F;
      float pow = node.getPowerMultiplier();
      FocusElementNode p = data.get(parent);
      if (p != null && p.node != null) pow *= p.getPower(data);
      return pow;
   }

   public void deserialize(CompoundTag nbt) {
      x = nbt.getInt("x");
      y = nbt.getInt("y");
      id = nbt.getInt("id");
      target = nbt.getBoolean("target");
      trajectory = nbt.getBoolean("trajectory");
      parent = nbt.getInt("parent");
      children = nbt.getIntArray("children");
      complexityMultiplier = nbt.getFloat("complexity");
      IFocusElement fe = nbt.contains("key") ? FocusEngine.getElement(nbt.getString("key")) : null;
      if (fe instanceof FocusNode fn) {
         node = fn;
         fn.initialize();
         for (String ns : fn.getSettingList()) fn.getSetting(ns).setValue(nbt.getInt("setting." + ns));
      }
   }

   public CompoundTag serialize() {
      CompoundTag nbt = new CompoundTag();
      nbt.putInt("x", x);
      nbt.putInt("y", y);
      nbt.putInt("id", id);
      nbt.putBoolean("target", target);
      nbt.putBoolean("trajectory", trajectory);
      nbt.putInt("parent", parent);
      nbt.putIntArray("children", children);
      nbt.putFloat("complexity", complexityMultiplier);
      if (node != null) {
         nbt.putString("key", node.getKey());
         for (String ns : node.getSettingList()) nbt.putInt("setting." + ns, node.getSettingValue(ns));
      }
      return nbt;
   }
}
