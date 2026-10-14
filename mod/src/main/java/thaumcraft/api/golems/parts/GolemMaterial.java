package thaumcraft.api.golems.parts;

import java.util.Locale;
import java.util.function.Supplier;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import thaumcraft.api.golems.EnumGolemTrait;

/** A golem's material: its texture, item colour, health/armour/damage modifiers and the components it is built from. */
public class GolemMaterial {
   protected static GolemMaterial[] materials = new GolemMaterial[0];
   public byte id;
   public String key;
   public String[] research;
   public ResourceLocation texture;
   public int itemColor;
   public int healthMod;
   public int armor;
   public int damage;
   /** Suppliers: the items are registered after the parts are declared. */
   public Supplier<ItemStack> componentBase;
   public Supplier<ItemStack> componentMechanism;
   public EnumGolemTrait[] traits;

   public GolemMaterial(String key, String[] research, ResourceLocation texture, int itemColor, int hp, int armor, int damage,
                        Supplier<ItemStack> compb, Supplier<ItemStack> compm, EnumGolemTrait[] tags) {
      this.key = key;
      this.research = research;
      this.texture = texture;
      this.itemColor = itemColor;
      this.componentBase = compb;
      this.componentMechanism = compm;
      this.healthMod = hp;
      this.armor = armor;
      this.traits = tags;
      this.damage = damage;
   }

   public static void register(GolemMaterial thing) {
      thing.id = (byte) materials.length;
      GolemMaterial[] temp = new GolemMaterial[materials.length + 1];
      System.arraycopy(materials, 0, temp, 0, materials.length);
      temp[thing.id] = thing;
      materials = temp;
   }

   public Component getLocalizedName() {
      return Component.translatable("golem.material." + key.toLowerCase(Locale.ROOT));
   }

   public Component getLocalizedDescription() {
      return Component.translatable("golem.material.text." + key.toLowerCase(Locale.ROOT));
   }

   public static GolemMaterial[] getMaterials() {
      return materials;
   }
}
