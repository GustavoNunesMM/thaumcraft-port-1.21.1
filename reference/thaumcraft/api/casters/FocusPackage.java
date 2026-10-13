package thaumcraft.api.casters;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.EntitySelectors;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;

public class FocusPackage implements IFocusElement {
   public World world;
   private EntityLivingBase caster;
   private UUID casterUUID;
   private float power = 1.0F;
   private int complexity = 0;
   int index;
   UUID uid;
   public List<IFocusElement> nodes = Collections.synchronizedList(new ArrayList<>());

   @Override
   public String getResearch() {
      return null;
   }

   public FocusPackage() {
   }

   public FocusPackage(EntityLivingBase caster) {
      this.world = caster.world;
      this.caster = caster;
      this.casterUUID = caster.getUniqueID();
   }

   @Override
   public String getKey() {
      return "thaumcraft.PACKAGE";
   }

   @Override
   public IFocusElement.EnumUnitType getType() {
      return IFocusElement.EnumUnitType.PACKAGE;
   }

   public int getComplexity() {
      return this.complexity;
   }

   public void setComplexity(int complexity) {
      this.complexity = complexity;
   }

   public UUID getUniqueID() {
      return this.uid;
   }

   public void setUniqueID(UUID id) {
      this.uid = id;
   }

   public int getExecutionIndex() {
      return this.index;
   }

   public void setExecutionIndex(int idx) {
      this.index = idx;
   }

   public void addNode(IFocusElement e) {
      this.nodes.add(e);
   }

   public UUID getCasterUUID() {
      if (this.caster != null) {
         this.casterUUID = this.caster.getUniqueID();
      }

      return this.casterUUID;
   }

   public void setCasterUUID(UUID casterUUID) {
      this.casterUUID = casterUUID;
   }

   public EntityLivingBase getCaster() {
      try {
         if (this.caster == null) {
            this.caster = this.world.getPlayerEntityByUUID(this.getCasterUUID());
         }

         if (this.caster == null) {
            for (EntityLivingBase e : this.world.getEntities(EntityLivingBase.class, EntitySelectors.IS_ALIVE)) {
               if (this.getCasterUUID().equals(e.getUniqueID())) {
                  this.caster = e;
                  break;
               }
            }
         }
      } catch (Exception var3) {
      }

      return this.caster;
   }

   public FocusEffect[] getFocusEffects() {
      return this.getFocusEffectsPackage(this);
   }

   private FocusEffect[] getFocusEffectsPackage(FocusPackage fp) {
      ArrayList<FocusEffect> out = new ArrayList<>();

      for (IFocusElement el : fp.nodes) {
         if (el instanceof FocusEffect) {
            out.add((FocusEffect)el);
         } else if (el instanceof FocusPackage) {
            for (FocusEffect fep : this.getFocusEffectsPackage((FocusPackage)el)) {
               out.add(fep);
            }
         } else if (el instanceof FocusModSplit) {
            for (FocusPackage fsp : ((FocusModSplit)el).getSplitPackages()) {
               for (FocusEffect fep : this.getFocusEffectsPackage(fsp)) {
                  out.add(fep);
               }
            }
         }
      }

      return out.toArray(new FocusEffect[0]);
   }

   public void deserialize(NBTTagCompound nbt) {
      this.uid = nbt.getUniqueId("uid");
      this.index = nbt.getInteger("index");
      int dim = nbt.getInteger("dim");
      this.world = DimensionManager.getWorld(dim);
      this.setCasterUUID(nbt.getUniqueId("casterUUID"));
      this.power = nbt.getFloat("power");
      this.complexity = nbt.getInteger("complexity");
      NBTTagList nodelist = nbt.getTagList("nodes", 10);
      this.nodes.clear();

      for (int x = 0; x < nodelist.tagCount(); x++) {
         NBTTagCompound nodenbt = nodelist.getCompoundTagAt(x);
         IFocusElement.EnumUnitType ut = IFocusElement.EnumUnitType.valueOf(nodenbt.getString("type"));
         if (ut != null) {
            if (ut == IFocusElement.EnumUnitType.PACKAGE) {
               FocusPackage fp = new FocusPackage();
               fp.deserialize(nodenbt.getCompoundTag("package"));
               this.nodes.add(fp);
               break;
            }

            IFocusElement fn = FocusEngine.getElement(nodenbt.getString("key"));
            if (fn != null) {
               if (fn instanceof FocusNode) {
                  ((FocusNode)fn).initialize();
                  if (((FocusNode)fn).getSettingList() != null) {
                     for (String ns : ((FocusNode)fn).getSettingList()) {
                        ((FocusNode)fn).getSetting(ns).setValue(nodenbt.getInteger("setting." + ns));
                     }
                  }

                  if (fn instanceof FocusModSplit) {
                     ((FocusModSplit)fn).deserialize(nodenbt.getCompoundTag("packages"));
                  }
               }

               this.addNode(fn);
            }
         }
      }
   }

   public NBTTagCompound serialize() {
      NBTTagCompound nbt = new NBTTagCompound();
      if (this.uid != null) {
         nbt.setUniqueId("uid", this.uid);
      }

      nbt.setInteger("index", this.index);
      if (this.getCasterUUID() != null) {
         nbt.setUniqueId("casterUUID", this.getCasterUUID());
      }

      if (this.world != null) {
         nbt.setInteger("dim", this.world.provider.getDimension());
      }

      nbt.setFloat("power", this.power);
      nbt.setInteger("complexity", this.complexity);
      NBTTagList nodelist = new NBTTagList();
      synchronized (this.nodes) {
         for (IFocusElement node : this.nodes) {
            if (node != null && node.getType() != null) {
               NBTTagCompound nodenbt = new NBTTagCompound();
               nodenbt.setString("type", node.getType().name());
               nodenbt.setString("key", node.getKey());
               if (node.getType() == IFocusElement.EnumUnitType.PACKAGE) {
                  nodenbt.setTag("package", ((FocusPackage)node).serialize());
                  nodelist.appendTag(nodenbt);
                  break;
               }

               if (node instanceof FocusNode && ((FocusNode)node).getSettingList() != null) {
                  for (String ns : ((FocusNode)node).getSettingList()) {
                     nodenbt.setInteger("setting." + ns, ((FocusNode)node).getSettingValue(ns));
                  }
               }

               if (node instanceof FocusModSplit) {
                  nodenbt.setTag("packages", ((FocusModSplit)node).serialize());
               }

               nodelist.appendTag(nodenbt);
            }
         }
      }

      nbt.setTag("nodes", nodelist);
      return nbt;
   }

   public float getPower() {
      return this.power;
   }

   public void multiplyPower(float pow) {
      this.power *= pow;
   }

   public FocusPackage copy(EntityLivingBase caster) {
      FocusPackage fp = new FocusPackage(caster);
      fp.deserialize(this.serialize());
      return fp;
   }

   public void initialize(EntityLivingBase caster) {
      this.world = caster.getEntityWorld();
      IFocusElement node = this.nodes.get(0);
      if (node instanceof FocusMediumRoot && ((FocusMediumRoot)node).supplyTargets() == null) {
         ((FocusMediumRoot)node).setupFromCaster(caster);
      }
   }

   public int getSortingHelper() {
      String s = "";

      for (IFocusElement k : this.nodes) {
         s = s + k.getKey();
         if (k instanceof FocusNode && ((FocusNode)k).getSettingList() != null) {
            for (String ns : ((FocusNode)k).getSettingList()) {
               s = s + "" + ((FocusNode)k).getSettingValue(ns);
            }
         }
      }

      return s.hashCode();
   }
}
