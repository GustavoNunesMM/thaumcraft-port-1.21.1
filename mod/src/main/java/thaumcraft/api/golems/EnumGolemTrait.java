package thaumcraft.api.golems;

import java.util.Locale;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public enum EnumGolemTrait {
   SMART, DEFT, CLUMSY, FIGHTER, WHEELED, FLYER, CLIMBER, HEAVY, LIGHT, FRAGILE, REPAIR, SCOUT, ARMORED, BRUTAL, FIREPROOF, BREAKER, HAULER,
   RANGED, BLASTPROOF;

   public final ResourceLocation icon;
   public EnumGolemTrait opposite;

   EnumGolemTrait() {
      this.icon = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/misc/golem/tag_" + name().toLowerCase(Locale.ROOT) + ".png");
   }

   public Component getLocalizedName() {
      return Component.translatable("golem.trait." + name().toLowerCase(Locale.ROOT));
   }

   public Component getLocalizedDescription() {
      return Component.translatable("golem.trait.text." + name().toLowerCase(Locale.ROOT));
   }

   static {
      CLUMSY.opposite = DEFT;
      DEFT.opposite = CLUMSY;
      HEAVY.opposite = LIGHT;
      LIGHT.opposite = HEAVY;
      FRAGILE.opposite = ARMORED;
      ARMORED.opposite = FRAGILE;
   }
}
