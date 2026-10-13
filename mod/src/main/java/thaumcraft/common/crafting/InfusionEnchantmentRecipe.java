package thaumcraft.common.crafting;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Random;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.util.RecipeMatcher;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.crafting.InfusionRecipe;
import thaumcraft.common.lib.enchantment.EnumInfusionEnchantment;
import thaumcraft.common.registers.ModDataComponents;
import thaumcraft.common.registers.ModRecipes;
import thaumcraft.common.research.ResearchManager;

/**
 * InfusionEnchantmentRecipe: adds one level of an infusion enchantment to a fitting tool. The essentia cost grows with
 * the level and with the other infusion enchantments already on it, and each one there may add a point of warp.
 */
public class InfusionEnchantmentRecipe extends InfusionRecipe {
   public final EnumInfusionEnchantment enchantment;

   public InfusionEnchantmentRecipe(String group, EnumInfusionEnchantment ench, AspectList aspects, List<Ingredient> components) {
      super(group, ench.research, 4, aspects, Ingredient.EMPTY, components, ItemStack.EMPTY);
      this.enchantment = ench;
   }

   /** Whether the item belongs to one of the enchantment's tool classes. */
   public static boolean fits(EnumInfusionEnchantment ench, ItemStack central) {
      if (ench.toolClasses.contains("all")) return true;
      var mods = central.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, net.minecraft.world.item.component.ItemAttributeModifiers.EMPTY);
      // As in 1.12, anything with an attack damage modifier counts as a weapon (tools included).
      if (ench.toolClasses.contains("weapon") && mods.modifiers().stream().anyMatch(e -> e.attribute().is(Attributes.ATTACK_DAMAGE))) return true;
      if (ench.toolClasses.contains("pickaxe") && central.canPerformAction(ItemAbilities.PICKAXE_DIG)) return true;
      if (ench.toolClasses.contains("axe") && central.canPerformAction(ItemAbilities.AXE_DIG)) return true;
      if (ench.toolClasses.contains("shovel") && central.canPerformAction(ItemAbilities.SHOVEL_DIG)) return true;
      if (ench.toolClasses.contains("hoe") && central.canPerformAction(ItemAbilities.HOE_DIG)) return true;
      if (central.getItem() instanceof ArmorItem armor) {
         String at = switch (armor.getEquipmentSlot()) {
            case HEAD -> "helm";
            case CHEST -> "chest";
            case LEGS -> "legs";
            case FEET -> "boots";
            default -> "none";
         };
         return ench.toolClasses.contains("armor") || ench.toolClasses.contains(at);
      }
      return false;
   }

   @Override
   public boolean matches(List<ItemStack> input, ItemStack central, Level level, Player player) {
      if (central.isEmpty() || !ResearchManager.knowsResearch(player, research)) return false;
      if (EnumInfusionEnchantment.getInfusionEnchantmentLevel(central, enchantment) >= enchantment.maxLevel) return false;
      return fits(enchantment, central) && RecipeMatcher.findMatches(input, components) != null;
   }

   @Override
   public ItemStack getRecipeOutput(Player player, ItemStack input, List<ItemStack> comps) {
      ItemStack out = input.copy();
      int cl = EnumInfusionEnchantment.getInfusionEnchantmentLevel(out, enchantment);
      if (cl >= enchantment.maxLevel) return ItemStack.EMPTY;
      List<EnumInfusionEnchantment> el = EnumInfusionEnchantment.getInfusionEnchantments(input);
      if (new Random(System.nanoTime()).nextInt(10) < el.size()) {
         out.set(ModDataComponents.WARP.get(), input.getOrDefault(ModDataComponents.WARP.get(), 0) + 1);
      }
      EnumInfusionEnchantment.addInfusionEnchantment(out, enchantment, cl + 1);
      return out;
   }

   @Override
   public AspectList getAspects(Player player, ItemStack input, List<ItemStack> comps) {
      AspectList out = new AspectList();
      if (input.isEmpty()) return out;
      int cl = EnumInfusionEnchantment.getInfusionEnchantmentLevel(input, enchantment) + 1;
      if (cl > enchantment.maxLevel) return out;
      List<EnumInfusionEnchantment> el = EnumInfusionEnchantment.getInfusionEnchantments(input);
      int others = el.size() - (el.contains(enchantment) ? 1 : 0);
      float modifier = cl + others * 0.33F;
      for (Aspect a : aspects.getAspects()) out.add(a, (int) (aspects.getAmount(a) * modifier));
      return out;
   }

   @Override
   public RecipeSerializer<?> getSerializer() {
      return ModRecipes.INFUSION_ENCHANTMENT_SERIALIZER.get();
   }

   public static class Serializer implements RecipeSerializer<InfusionEnchantmentRecipe> {
      static final Codec<EnumInfusionEnchantment> ENCH = Codec.STRING.xmap(EnumInfusionEnchantment::byId, EnumInfusionEnchantment::id);
      static final MapCodec<InfusionEnchantmentRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
         Codec.STRING.optionalFieldOf("group", "").forGetter(r -> r.getGroup()),
         ENCH.fieldOf("enchantment").forGetter(r -> r.enchantment),
         AspectList.CODEC.fieldOf("aspects").forGetter(r -> r.aspects),
         Ingredient.CODEC_NONEMPTY.listOf().fieldOf("components").forGetter(r -> r.components)
      ).apply(i, InfusionEnchantmentRecipe::new));
      static final StreamCodec<RegistryFriendlyByteBuf, InfusionEnchantmentRecipe> STREAM_CODEC = StreamCodec.of(
         (buf, r) -> {
            buf.writeUtf(r.getGroup());
            buf.writeUtf(r.enchantment.id());
            AspectList.STREAM_CODEC.encode(buf, r.aspects);
            Ingredient.CONTENTS_STREAM_CODEC.apply(ByteBufCodecs.list()).encode(buf, r.components);
         },
         buf -> new InfusionEnchantmentRecipe(buf.readUtf(), EnumInfusionEnchantment.byId(buf.readUtf()), AspectList.STREAM_CODEC.decode(buf),
            Ingredient.CONTENTS_STREAM_CODEC.apply(ByteBufCodecs.list()).decode(buf)));

      @Override
      public MapCodec<InfusionEnchantmentRecipe> codec() {
         return CODEC;
      }

      @Override
      public StreamCodec<RegistryFriendlyByteBuf, InfusionEnchantmentRecipe> streamCodec() {
         return STREAM_CODEC;
      }
   }
}
