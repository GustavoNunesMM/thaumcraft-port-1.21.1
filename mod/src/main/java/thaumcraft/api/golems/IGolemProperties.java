package thaumcraft.api.golems;

import java.util.Set;
import net.minecraft.world.item.ItemStack;
import thaumcraft.api.golems.parts.GolemAddon;
import thaumcraft.api.golems.parts.GolemArm;
import thaumcraft.api.golems.parts.GolemHead;
import thaumcraft.api.golems.parts.GolemLeg;
import thaumcraft.api.golems.parts.GolemMaterial;

public interface IGolemProperties {
   Set<EnumGolemTrait> getTraits();

   boolean hasTrait(EnumGolemTrait trait);

   long toLong();

   ItemStack[] generateComponents();

   void setMaterial(GolemMaterial mat);

   GolemMaterial getMaterial();

   void setHead(GolemHead head);

   GolemHead getHead();

   void setArms(GolemArm arms);

   GolemArm getArms();

   void setLegs(GolemLeg legs);

   GolemLeg getLegs();

   void setAddon(GolemAddon addon);

   GolemAddon getAddon();

   void setRank(int rank);

   int getRank();
}
