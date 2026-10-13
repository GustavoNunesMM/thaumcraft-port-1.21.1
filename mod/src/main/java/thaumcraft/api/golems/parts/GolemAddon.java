package thaumcraft.api.golems.parts;

import java.util.Locale;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.api.golems.EnumGolemTrait;

public class GolemAddon {
   protected static GolemAddon[] addons = new GolemAddon[0];
   public byte id;
   public String key;
   public String[] research;
   public ResourceLocation icon;
   public Object[] components;
   public EnumGolemTrait[] traits;
   public IAddonFunction function;
   public PartModel model;

   public GolemAddon(String key, String[] research, ResourceLocation icon, PartModel model, Object[] comp, EnumGolemTrait[] tags) {
      this(key, research, icon, model, comp, null, tags);
   }

   public GolemAddon(String key, String[] research, ResourceLocation icon, PartModel model, Object[] comp, IAddonFunction function, EnumGolemTrait[] tags) {
      this.key = key;
      this.research = research;
      this.icon = icon;
      this.components = comp;
      this.traits = tags;
      this.model = model;
      this.function = function;
   }

   public static void register(GolemAddon thing) {
      thing.id = (byte) addons.length;
      GolemAddon[] temp = new GolemAddon[addons.length + 1];
      System.arraycopy(addons, 0, temp, 0, addons.length);
      temp[thing.id] = thing;
      addons = temp;
   }

   public Component getLocalizedName() {
      return Component.translatable("golem.addon." + key.toLowerCase(Locale.ROOT));
   }

   public Component getLocalizedDescription() {
      return Component.translatable("golem.addon.text." + key.toLowerCase(Locale.ROOT));
   }

   public static GolemAddon[] getAddons() {
      return addons;
   }

   public interface IAddonFunction extends IGenericFunction {
   }
}
