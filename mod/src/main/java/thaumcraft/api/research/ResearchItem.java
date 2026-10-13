package thaumcraft.api.research;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import javax.annotation.Nullable;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.ItemEnchantments;

/**
 * An item a research stage asks for (or rewards): a stack, or any item of a tag (the 1.12 ore dictionary).
 * Written as "id", "id;count", "id;count;{components}" or "#tag" (see tools/convert_research.py).
 */
public record ResearchItem(ItemStack stack, @Nullable TagKey<Item> tag) {
   /** null for unknown items (not ported yet, or from a missing mod), which the original also skipped. */
   @Nullable
   public static ResearchItem parse(String text, HolderLookup.Provider registries) {
      if (text.startsWith("#")) {
         TagKey<Item> tag = TagKey.create(Registries.ITEM, ResourceLocation.parse(text.substring(1)));
         return new ResearchItem(ItemStack.EMPTY, tag);
      }
      String[] parts = text.split(";", 3);
      Item item = BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse(parts[0])).orElse(Items.AIR);
      if (item == Items.AIR) return null;
      int count = parts.length > 1 ? Integer.parseInt(parts[1]) : 1;
      ItemStack stack = new ItemStack(item, count);
      if (parts.length > 2) {
         DataComponentPatch patch = DataComponentPatch.CODEC.parse(registries.createSerializationContext(JsonOps.INSTANCE),
            JsonParser.parseString(parts[2])).getOrThrow();
         stack.applyComponents(patch);
      }
      return new ResearchItem(stack, null);
   }

   public boolean isTag() {
      return tag != null;
   }

   /**
    * InventoryUtils matching: tags by tag; an enchanted book stands for "anything with these enchantments" (the 1.12
    * enchanted placeholder); otherwise same item whose components include the required ones ("relaxed NBT").
    */
   public boolean matches(ItemStack other) {
      if (other.isEmpty()) return false;
      if (tag != null) return other.is(tag);
      ItemEnchantments wanted = stack.get(DataComponents.STORED_ENCHANTMENTS);
      if (stack.is(Items.ENCHANTED_BOOK) && wanted != null && !wanted.isEmpty()) {
         ItemEnchantments have = other.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY);
         ItemEnchantments applied = other.getTagEnchantments();
         for (var e : wanted.entrySet()) {
            if (Math.max(have.getLevel(e.getKey()), applied.getLevel(e.getKey())) < e.getIntValue()) return false;
         }
         return true;
      }
      if (!other.is(stack.getItem())) return false;
      for (var entry : stack.getComponentsPatch().entrySet()) {
         if (entry.getValue().isPresent() && !java.util.Objects.equals(other.get(entry.getKey()), entry.getValue().get())) return false;
      }
      return true;
   }

   public int count() {
      return tag != null ? 1 : stack.getCount();
   }

   /** Key used to remember that the player crafted this ("[#]" + key is added as research). */
   public String craftKey() {
      if (tag != null) return "#" + tag.location();
      return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
   }

   /** Something to show for it: the stack, or the first item of the tag. */
   public ItemStack display() {
      if (tag == null) return stack;
      return BuiltInRegistries.ITEM.getTag(tag).flatMap(t -> t.stream().findFirst()).map(h -> new ItemStack(h.value())).orElse(ItemStack.EMPTY);
   }
}
