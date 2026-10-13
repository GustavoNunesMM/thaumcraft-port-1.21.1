package thaumcraft.api.casters;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

/** FocusPackage: the chain of focus elements stored in a focus, plus the caster and power while it runs. */
public class FocusPackage implements IFocusElement {
   public Level world;
   private LivingEntity caster;
   private UUID casterUUID;
   private float power = 1.0F;
   private int complexity;
   int index;
   UUID uid;
   public List<IFocusElement> nodes = Collections.synchronizedList(new ArrayList<>());

   public FocusPackage() {
   }

   public FocusPackage(LivingEntity caster) {
      this.world = caster.level();
      this.caster = caster;
      this.casterUUID = caster.getUUID();
   }

   @Override
   public String getResearch() {
      return null;
   }

   @Override
   public String getKey() {
      return "thaumcraft.PACKAGE";
   }

   @Override
   public EnumUnitType getType() {
      return EnumUnitType.PACKAGE;
   }

   public int getComplexity() {
      return complexity;
   }

   public void setComplexity(int complexity) {
      this.complexity = complexity;
   }

   public UUID getUniqueID() {
      return uid;
   }

   public void setUniqueID(UUID id) {
      uid = id;
   }

   public int getExecutionIndex() {
      return index;
   }

   public void setExecutionIndex(int idx) {
      index = idx;
   }

   public void addNode(IFocusElement e) {
      nodes.add(e);
   }

   public UUID getCasterUUID() {
      if (caster != null) casterUUID = caster.getUUID();
      return casterUUID;
   }

   public void setCasterUUID(UUID casterUUID) {
      this.casterUUID = casterUUID;
   }

   @Nullable
   public LivingEntity getCaster() {
      if (caster == null && world != null && getCasterUUID() != null) {
         caster = world.getPlayerByUUID(getCasterUUID());
         if (caster == null && world instanceof ServerLevel sl && sl.getEntity(getCasterUUID()) instanceof LivingEntity le) caster = le;
      }
      return caster;
   }

   public FocusEffect[] getFocusEffects() {
      return effectsOf(this).toArray(new FocusEffect[0]);
   }

   private static List<FocusEffect> effectsOf(FocusPackage fp) {
      List<FocusEffect> out = new ArrayList<>();
      for (IFocusElement el : fp.nodes) {
         if (el instanceof FocusEffect fe) out.add(fe);
         else if (el instanceof FocusPackage p) out.addAll(effectsOf(p));
         else if (el instanceof FocusModSplit split) {
            for (FocusPackage sp : split.getSplitPackages()) out.addAll(effectsOf(sp));
         }
      }
      return out;
   }

   public void deserialize(CompoundTag nbt) {
      uid = nbt.hasUUID("uid") ? nbt.getUUID("uid") : null;
      index = nbt.getInt("index");
      if (nbt.contains("dim")) {
         MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
         if (server != null) world = server.getLevel(ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(nbt.getString("dim"))));
      }
      if (nbt.hasUUID("casterUUID")) setCasterUUID(nbt.getUUID("casterUUID"));
      power = nbt.contains("power") ? nbt.getFloat("power") : 1.0F;
      complexity = nbt.getInt("complexity");
      nodes.clear();
      for (Tag t : nbt.getList("nodes", Tag.TAG_COMPOUND)) {
         CompoundTag nodenbt = (CompoundTag) t;
         EnumUnitType ut;
         try {
            ut = EnumUnitType.valueOf(nodenbt.getString("type"));
         } catch (IllegalArgumentException e) {
            continue;
         }
         if (ut == EnumUnitType.PACKAGE) {
            FocusPackage fp = new FocusPackage();
            fp.deserialize(nodenbt.getCompound("package"));
            nodes.add(fp);
            break;
         }
         IFocusElement fn = FocusEngine.getElement(nodenbt.getString("key"));
         if (fn == null) continue;
         if (fn instanceof FocusNode node) {
            node.initialize();
            for (String ns : node.getSettingList()) node.getSetting(ns).setValue(nodenbt.getInt("setting." + ns));
            if (fn instanceof FocusModSplit split) split.deserialize(nodenbt.getCompound("packages"));
         }
         addNode(fn);
      }
   }

   public CompoundTag serialize() {
      CompoundTag nbt = new CompoundTag();
      if (uid != null) nbt.putUUID("uid", uid);
      nbt.putInt("index", index);
      if (getCasterUUID() != null) nbt.putUUID("casterUUID", getCasterUUID());
      if (world != null) nbt.putString("dim", world.dimension().location().toString());
      nbt.putFloat("power", power);
      nbt.putInt("complexity", complexity);
      ListTag list = new ListTag();
      synchronized (nodes) {
         for (IFocusElement node : nodes) {
            if (node == null || node.getType() == null) continue;
            CompoundTag nodenbt = new CompoundTag();
            nodenbt.putString("type", node.getType().name());
            nodenbt.putString("key", node.getKey());
            if (node.getType() == EnumUnitType.PACKAGE) {
               nodenbt.put("package", ((FocusPackage) node).serialize());
               list.add(nodenbt);
               break;
            }
            if (node instanceof FocusNode fn) {
               for (String ns : fn.getSettingList()) nodenbt.putInt("setting." + ns, fn.getSettingValue(ns));
            }
            if (node instanceof FocusModSplit split) nodenbt.put("packages", split.serialize());
            list.add(nodenbt);
         }
      }
      nbt.put("nodes", list);
      return nbt;
   }

   public float getPower() {
      return power;
   }

   public void multiplyPower(float pow) {
      power *= pow;
   }

   public FocusPackage copy(LivingEntity caster) {
      FocusPackage fp = new FocusPackage(caster);
      fp.deserialize(serialize());
      fp.world = caster.level();
      return fp;
   }

   public void initialize(LivingEntity caster) {
      world = caster.level();
      IFocusElement node = nodes.get(0);
      if (node instanceof FocusMediumRoot root && root.supplyTargets() == null) root.setupFromCaster(caster);
   }

   public int getSortingHelper() {
      StringBuilder s = new StringBuilder();
      for (IFocusElement k : nodes) {
         s.append(k.getKey());
         if (k instanceof FocusNode fn) {
            for (String ns : fn.getSettingList()) s.append(fn.getSettingValue(ns));
         }
      }
      return s.toString().hashCode();
   }
}
