package thaumcraft.api.casters;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.world.phys.HitResult;
import thaumcraft.api.aspects.Aspect;

/** A medium, effect or modifier in a focus package. Nodes pass trajectories and targets down the chain. */
public abstract class FocusNode implements IFocusElement {
   FocusPackage pack;
   private FocusNode parent;
   final Map<String, NodeSetting> settings = new LinkedHashMap<>();

   protected FocusNode() {
      initialize();
   }

   public String getUnlocalizedName() {
      return getKey() + ".name";
   }

   public String getUnlocalizedText() {
      return getKey() + ".text";
   }

   public abstract int getComplexity();

   @Nullable
   public abstract Aspect getAspect();

   @Nullable
   public abstract EnumSupplyType[] mustBeSupplied();

   @Nullable
   public abstract EnumSupplyType[] willSupply();

   public boolean canSupply(EnumSupplyType type) {
      EnumSupplyType[] ws = willSupply();
      if (ws != null) {
         for (EnumSupplyType st : ws) {
            if (st == type) return true;
         }
      }
      return false;
   }

   @Nullable
   public HitResult[] supplyTargets() {
      return null;
   }

   @Nullable
   public Trajectory[] supplyTrajectories() {
      return null;
   }

   public final void setPackage(FocusPackage pack) {
      this.pack = pack;
   }

   public final FocusPackage getPackage() {
      return pack;
   }

   /** The rest of the package after this node (what a projectile, cloud or mine carries). */
   @Nullable
   public final FocusPackage getRemainingPackage() {
      FocusPackage p = getPackage();
      List<IFocusElement> l2 = new ArrayList<>(p.nodes.subList(p.index + 1, p.nodes.size()));
      if (l2.isEmpty()) return null;
      FocusPackage p2 = new FocusPackage();
      p2.setUniqueID(p.getUniqueID());
      p2.world = p.world;
      p2.multiplyPower(p.getPower());
      p2.nodes = java.util.Collections.synchronizedList(l2);
      p2.setCasterUUID(p.getCasterUUID());
      return p2;
   }

   @Nullable
   public final FocusNode getParent() {
      return parent;
   }

   public final Set<String> getSettingList() {
      return settings.keySet();
   }

   public final NodeSetting getSetting(String key) {
      return settings.get(key);
   }

   public final int getSettingValue(String key) {
      return settings.containsKey(key) ? settings.get(key).getValue() : 0;
   }

   @Nullable
   public NodeSetting[] createSettings() {
      return null;
   }

   public final void initialize() {
      settings.clear();
      NodeSetting[] set = createSettings();
      if (set != null) {
         for (NodeSetting setting : set) settings.put(setting.key, setting);
      }
   }

   public void setParent(FocusNode parent) {
      this.parent = parent;
   }

   public float getPowerMultiplier() {
      return 1.0F;
   }

   public boolean isExclusive() {
      return false;
   }

   public enum EnumSupplyType {
      TARGET, TRAJECTORY
   }
}
