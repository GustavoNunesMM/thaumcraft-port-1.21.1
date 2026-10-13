package thaumcraft.common.crafting;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.util.RecipeMatcher;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.crafting.InfusionRecipe;
import thaumcraft.common.events.GearEvents;
import thaumcraft.common.registers.ModGear;
import thaumcraft.common.registers.ModItems;
import thaumcraft.common.research.ResearchManager;
import top.theillusivec4.curios.api.CuriosApi;

/**
 * InfusionRunicAugmentRecipe: one more point of runic shielding (thaumcraft:runic) on any armor piece or curio. Each
 * point already there asks for one more amber and doubles the essentia; instability grows with it too.
 */
public class InfusionRunicAugmentRecipe extends InfusionRecipe {
   public static final TagKey<Item> AMBER = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "gems/amber"));

   public InfusionRunicAugmentRecipe(String group, String research) {
      super(group, research, 0, new AspectList(), Ingredient.EMPTY, List.of(Ingredient.of(ModItems.SALIS_MUNDUS.get()), Ingredient.of(AMBER)),
         ItemStack.EMPTY);
   }

   /** Armor, or anything that fits a Curios slot (IBauble in 1.12). */
   public static boolean accepts(ItemStack central, Level level) {
      if (central.isEmpty()) return false;
      return central.getItem() instanceof ArmorItem || !CuriosApi.getItemStackSlots(central, level).isEmpty();
   }

   public NonNullList<Ingredient> getComponents(ItemStack input) {
      NonNullList<Ingredient> com = NonNullList.create();
      com.add(Ingredient.of(ModItems.SALIS_MUNDUS.get()));
      com.add(Ingredient.of(AMBER));
      int fc = GearEvents.getRunicCharge(input);
      for (int c = 0; c < fc; c++) com.add(Ingredient.of(AMBER));
      return com;
   }

   @Override
   public boolean matches(List<ItemStack> input, ItemStack central, Level level, Player player) {
      if (!ResearchManager.knowsResearch(player, research) || !accepts(central, level)) return false;
      return RecipeMatcher.findMatches(input, getComponents(central)) != null;
   }

   @Override
   public ItemStack getRecipeOutput(Player player, ItemStack input, List<ItemStack> comps) {
      if (input.isEmpty()) return ItemStack.EMPTY;
      ItemStack out = input.copy();
      out.set(ModGear.RUNIC.get(), GearEvents.getRunicCharge(input) + 1);
      return out;
   }

   @Override
   public AspectList getAspects(Player player, ItemStack input, List<ItemStack> comps) {
      AspectList out = new AspectList();
      int vis = 20 + (int) (20.0 * Math.pow(2.0, GearEvents.getRunicCharge(input)));
      out.add(Aspect.PROTECT, vis);
      out.add(Aspect.CRYSTAL, vis / 2);
      out.add(Aspect.ENERGY, vis / 2);
      return out;
   }

   @Override
   public int getInstability(Player player, ItemStack input, List<ItemStack> comps) {
      return 5 + GearEvents.getRunicCharge(input) / 2;
   }

   @Override
   public RecipeSerializer<?> getSerializer() {
      return ModGear.INFUSION_RUNIC_SERIALIZER.get();
   }

   public static class Serializer implements RecipeSerializer<InfusionRunicAugmentRecipe> {
      static final MapCodec<InfusionRunicAugmentRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
         Codec.STRING.optionalFieldOf("group", "").forGetter(InfusionRunicAugmentRecipe::getGroup),
         Codec.STRING.optionalFieldOf("research", "RUNICSHIELDING").forGetter(r -> r.research)
      ).apply(i, InfusionRunicAugmentRecipe::new));
      static final StreamCodec<RegistryFriendlyByteBuf, InfusionRunicAugmentRecipe> STREAM_CODEC = StreamCodec.composite(
         ByteBufCodecs.STRING_UTF8, InfusionRunicAugmentRecipe::getGroup, ByteBufCodecs.STRING_UTF8, r -> r.research,
         InfusionRunicAugmentRecipe::new);

      @Override
      public MapCodec<InfusionRunicAugmentRecipe> codec() {
         return CODEC;
      }

      @Override
      public StreamCodec<RegistryFriendlyByteBuf, InfusionRunicAugmentRecipe> streamCodec() {
         return STREAM_CODEC;
      }
   }
}
