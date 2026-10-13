package thaumcraft.common.tiles.crafting;

import java.util.HashMap;
import net.minecraft.nbt.NBTTagCompound;
import thaumcraft.api.casters.FocusEngine;
import thaumcraft.api.casters.FocusNode;
import thaumcraft.api.casters.IFocusElement;

public class FocusElementNode {
   public int x;
   public int y;
   public int id;
   public boolean target = false;
   public boolean trajectory = false;
   public int parent = -1;
   public int[] children = new int[0];
   public float complexityMultiplier = 1.0F;
   public FocusNode node = null;

   public float getPower(HashMap<Integer, FocusElementNode> data) {
      if (this.node == null) {
         return 1.0F;
      } else {
         float pow = this.node.getPowerMultiplier();
         FocusElementNode p = data.get(this.parent);
         if (p != null && p.node != null) {
            pow *= p.getPower(data);
         }

         return pow;
      }
   }

   public void deserialize(NBTTagCompound nbt) {
      this.x = nbt.getInteger("x");
      this.y = nbt.getInteger("y");
      this.id = nbt.getInteger("id");
      this.target = nbt.getBoolean("target");
      this.trajectory = nbt.getBoolean("trajectory");
      this.parent = nbt.getInteger("parent");
      this.children = nbt.getIntArray("children");
      this.complexityMultiplier = nbt.getFloat("complexity");
      IFocusElement fe = FocusEngine.getElement(nbt.getString("key"));
      if (fe != null) {
         this.node = (FocusNode)fe;
         ((FocusNode)fe).initialize();
         if (((FocusNode)fe).getSettingList() != null) {
            for (String ns : ((FocusNode)fe).getSettingList()) {
               ((FocusNode)fe).getSetting(ns).setValue(nbt.getInteger("setting." + ns));
            }
         }
      }
   }

   public NBTTagCompound serialize() {
      NBTTagCompound nbt = new NBTTagCompound();
      nbt.setInteger("x", this.x);
      nbt.setInteger("y", this.y);
      nbt.setInteger("id", this.id);
      nbt.setBoolean("target", this.target);
      nbt.setBoolean("trajectory", this.trajectory);
      nbt.setInteger("parent", this.parent);
      nbt.setIntArray("children", this.children);
      nbt.setFloat("complexity", this.complexityMultiplier);
      if (this.node != null) {
         nbt.setString("key", this.node.getKey());
         if (this.node.getSettingList() != null) {
            for (String ns : this.node.getSettingList()) {
               nbt.setInteger("setting." + ns, this.node.getSettingValue(ns));
            }
         }
      }

      return nbt;
   }
}
