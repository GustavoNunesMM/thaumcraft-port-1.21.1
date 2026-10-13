package thaumcraft.api.golems.parts;

import java.util.Locale;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.api.golems.EnumGolemTrait;

public class GolemHead {
   protected static GolemHead[] heads = new GolemHead[0];
   public byte id;
   public String key;
   public String[] research;
   public ResourceLocation icon;
   /** ItemStack, Supplier&lt;ItemStack&gt;, or "base"/"mech" for the material's components. */
   public Object[] components;
   public EnumGolemTrait[] traits;
   public IHeadFunction function;
   public PartModel model;

   public GolemHead(String key, String[] research, ResourceLocation icon, PartModel model, Object[] comp, EnumGolemTrait[] tags) {
      this(key, research, icon, model, comp, null, tags);
   }

   public GolemHead(String key, String[] research, ResourceLocation icon, PartModel model, Object[] comp, IHeadFunction function, EnumGolemTrait[] tags) {
      this.key = key;
      this.research = research;
      this.icon = icon;
      this.components = comp;
      this.traits = tags;
      this.model = model;
      this.function = function;
   }

   public static void register(GolemHead thing) {
      thing.id = (byte) heads.length;
      GolemHead[] temp = new GolemHead[heads.length + 1];
      System.arraycopy(heads, 0, temp, 0, heads.length);
      temp[thing.id] = thing;
      heads = temp;
   }

   public Component getLocalizedName() {
      return Component.translatable("golem.head." + key.toLowerCase(Locale.ROOT));
   }

   public Component getLocalizedDescription() {
      return Component.translatable("golem.head.text." + key.toLowerCase(Locale.ROOT));
   }

   public static GolemHead[] getHeads() {
      return heads;
   }

   public interface IHeadFunction extends IGenericFunction {
   }
}
