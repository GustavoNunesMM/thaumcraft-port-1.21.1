package thaumcraft.api.golems.parts;

import java.util.Locale;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.api.golems.EnumGolemTrait;

public class GolemLeg {
   protected static GolemLeg[] legs = new GolemLeg[0];
   public byte id;
   public String key;
   public String[] research;
   public ResourceLocation icon;
   public Object[] components;
   public EnumGolemTrait[] traits;
   public ILegFunction function;
   public PartModel model;

   public GolemLeg(String key, String[] research, ResourceLocation icon, PartModel model, Object[] comp, EnumGolemTrait[] tags) {
      this(key, research, icon, model, comp, null, tags);
   }

   public GolemLeg(String key, String[] research, ResourceLocation icon, PartModel model, Object[] comp, ILegFunction function, EnumGolemTrait[] tags) {
      this.key = key;
      this.research = research;
      this.icon = icon;
      this.components = comp;
      this.traits = tags;
      this.model = model;
      this.function = function;
   }

   public static void register(GolemLeg thing) {
      thing.id = (byte) legs.length;
      GolemLeg[] temp = new GolemLeg[legs.length + 1];
      System.arraycopy(legs, 0, temp, 0, legs.length);
      temp[thing.id] = thing;
      legs = temp;
   }

   public Component getLocalizedName() {
      return Component.translatable("golem.leg." + key.toLowerCase(Locale.ROOT));
   }

   public Component getLocalizedDescription() {
      return Component.translatable("golem.leg.text." + key.toLowerCase(Locale.ROOT));
   }

   public static GolemLeg[] getLegs() {
      return legs;
   }

   public interface ILegFunction extends IGenericFunction {
   }
}
