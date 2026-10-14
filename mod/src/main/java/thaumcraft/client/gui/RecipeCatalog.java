package thaumcraft.client.gui;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.ShapedRecipe;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.crafting.CrucibleRecipe;
import thaumcraft.api.crafting.IArcaneRecipe;
import thaumcraft.api.crafting.ShapedArcaneRecipe;
import thaumcraft.common.research.ResearchManager;
import thaumcraft.Thaumcraft;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.items.VisCrystalItem;
import thaumcraft.common.registers.ModItems;

/**
 * The recipe catalog the Thaumonomicon draws from (CommonInternals.craftingRecipeCatalog + fake recipes + groups).
 * A research stage names recipes by id; an id resolves to one or more {@link RecipeView}s: a real recipe, a recipe
 * group (all recipes whose "group" is that id), or a display-only "fake" recipe.
 */
public final class RecipeCatalog {
   /** How one recipe shows up in the book. */
   public interface RecipeView {
      ItemStack output();

      /** False if the player hasn't researched it (shown as unknown). */
      default boolean known(Player player) {
         return true;
      }

      void render(ResearchPageScreen screen, GuiGraphics g, int x, int y, int mx, int my);

      /** A click on the shown recipe page (x, y: the page centre); true if handled. */
      default boolean mouseClicked(ResearchPageScreen screen, int x, int y, int mx, int my) {
         return false;
      }
   }

   /** Converters from recipe types to views; later milestones add arcane, crucible, infusion... */
   private static final List<Function<Recipe<?>, RecipeView>> CONVERTERS = new ArrayList<>();
   private static final Map<ResourceLocation, List<RecipeView>> FAKE = new LinkedHashMap<>();
   /**
    * Ids the 1.12 research json names but no recipe ever had (the original book showed nothing for them), pointed at
    * the recipes they obviously meant.
    */
   private static final Map<ResourceLocation, ResourceLocation> ALIAS = Map.of(
      id("arcane_stone"), id("stonearcane"),
      id("arcane_brick"), id("brickarcane"),
      id("nitorcolor"), id("nitorgroup"),
      id("jarlabelessence"), id("jarlabelaspect"));

   static {
      CONVERTERS.add(r -> r instanceof IArcaneRecipe ar ? new ArcaneView(ar) : null);
      CONVERTERS.add(r -> r instanceof CrucibleRecipe cr ? new CrucibleView(cr) : null);
      CONVERTERS.add(r -> r instanceof CraftingRecipe cr ? new CraftingView(cr) : null);
      CONVERTERS.add(r -> r instanceof AbstractCookingRecipe cr ? new CookingView(cr) : null);
      // ConfigRecipes' fake salis mundus recipe, for display.
      FAKE.put(id("salismundusfake"), List.of(new ShapelessDisplay(() -> new ItemStack(ModItems.SALIS_MUNDUS.get()), () -> List.of(
         new ItemStack(Items.FLINT), new ItemStack(Items.BOWL), new ItemStack(Items.REDSTONE),
         VisCrystalItem.of(Aspect.AIR, 1), VisCrystalItem.of(Aspect.FIRE, 1), VisCrystalItem.of(Aspect.WATER, 1)))));
   }

   private RecipeCatalog() {
   }

   private static ResourceLocation id(String path) {
      return ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, path);
   }

   public static void addConverter(Function<Recipe<?>, RecipeView> converter) {
      CONVERTERS.add(0, converter);
   }

   public static void addFake(ResourceLocation id, RecipeView view) {
      FAKE.computeIfAbsent(id, k -> new ArrayList<>()).add(view);
   }

   @Nullable
   private static RecipeView convert(Recipe<?> r) {
      for (Function<Recipe<?>, RecipeView> c : CONVERTERS) {
         RecipeView v = c.apply(r);
         if (v != null) return v;
      }
      return null;
   }

   /** Every view an id stands for (empty if unknown). */
   public static List<RecipeView> lookup(ResourceLocation id) {
      id = ALIAS.getOrDefault(id, id);
      List<RecipeView> out = new ArrayList<>();
      if (FAKE.containsKey(id)) out.addAll(FAKE.get(id));
      var level = Minecraft.getInstance().level;
      if (level == null) return out;
      var rm = level.getRecipeManager();
      rm.byKey(id).ifPresent(h -> {
         RecipeView v = convert(h.value());
         if (v != null) out.add(v);
      });
      if (out.isEmpty()) {
         String group = id.toString();
         for (RecipeHolder<?> h : rm.getRecipes()) {
            if (group.equals(h.value().getGroup()) || id.getPath().equals(h.value().getGroup())) {
               RecipeView v = convert(h.value());
               if (v != null) out.add(v);
            }
         }
      }
      return out;
   }

   /** The first output an id makes, for searching. */
   public static ItemStack output(ResourceLocation id) {
      List<RecipeView> v = lookup(id);
      return v.isEmpty() ? ItemStack.EMPTY : v.get(0).output();
   }

   // ------------------------------------------------------------------ views

   static ItemStack cycle(Ingredient ing, int index) {
      ItemStack[] items = ing.getItems();
      if (items.length == 0) return ItemStack.EMPTY;
      int i = (int) ((System.currentTimeMillis() / 1000L + index) % items.length);
      return items[i];
   }

   /** drawCraftingPage */
   record CraftingView(CraftingRecipe recipe) implements RecipeView {
      @Override
      public ItemStack output() {
         var level = Minecraft.getInstance().level;
         return level == null ? ItemStack.EMPTY : recipe.getResultItem(level.registryAccess());
      }

      @Override
      public void render(ResearchPageScreen s, GuiGraphics g, int x, int y, int mx, int my) {
         s.drawCraftingGrid(g, x, y);
         s.drawStackAt(g, output(), x - 8, y - 84, mx, my, false);
         NonNullList<Ingredient> items = recipe.getIngredients();
         if (recipe instanceof ShapedRecipe sr) {
            s.drawCentered(g, Component.translatable("recipe.type.workbench"), x, y - 104);
            int rw = sr.getWidth(), rh = sr.getHeight();
            for (int i = 0; i < rw && i < 3; i++) {
               for (int j = 0; j < rh && j < 3; j++) {
                  s.drawStackAt(g, cycle(items.get(i + j * rw), i + j * rw), x - 40 + i * 32, y - 40 + j * 32, mx, my, true);
               }
            }
         } else {
            s.drawCentered(g, Component.translatable("recipe.type.workbenchshapeless"), x, y - 104);
            for (int i = 0; i < items.size() && i < 9; i++) {
               s.drawStackAt(g, cycle(items.get(i), i), x - 40 + i % 3 * 32, y - 40 + i / 3 * 32, mx, my, true);
            }
         }
      }
   }

   /** drawArcaneCraftingPage */
   record ArcaneView(IArcaneRecipe recipe) implements RecipeView {
      @Override
      public ItemStack output() {
         var level = Minecraft.getInstance().level;
         return level == null ? ItemStack.EMPTY : recipe.getResultItem(level.registryAccess());
      }

      @Override
      public boolean known(Player player) {
         return ResearchManager.knowsResearch(player, recipe.getResearch());
      }

      @Override
      public void render(ResearchPageScreen s, GuiGraphics g, int x, int y, int mx, int my) {
         s.drawArcaneGrid(g, x, y, recipe.getVis(), mx, my);
         s.drawStackAt(g, output(), x - 8, y - 84, mx, my, false);
         AspectList crystals = recipe.getCrystals();
         int a = 0, sz = crystals.size();
         for (Aspect aspect : crystals.getAspects()) {
            s.drawStackAt(g, VisCrystalItem.of(aspect, crystals.getAmount(aspect)), x + 4 - sz * 10 + a * 20, y + 59, mx, my, true);
            a++;
         }
         NonNullList<Ingredient> items = recipe.getIngredients();
         if (recipe instanceof ShapedArcaneRecipe sr) {
            s.drawCentered(g, Component.translatable("recipe.type.arcane"), x, y - 104);
            int rw = sr.getWidth(), rh = sr.getHeight();
            for (int i = 0; i < rw && i < 3; i++) {
               for (int j = 0; j < rh && j < 3; j++) {
                  s.drawStackAt(g, cycle(items.get(i + j * rw), i + j * rw), x - 40 + i * 32, y - 40 + j * 32, mx, my, true);
               }
            }
         } else {
            s.drawCentered(g, Component.translatable("recipe.type.arcane.shapeless"), x, y - 104);
            for (int i = 0; i < items.size() && i < 9; i++) {
               s.drawStackAt(g, cycle(items.get(i), i), x - 40 + i % 3 * 32, y - 40 + i / 3 * 32, mx, my, true);
            }
         }
      }
   }

   /** drawCruciblePage */
   record CrucibleView(CrucibleRecipe recipe) implements RecipeView {
      @Override
      public ItemStack output() {
         var level = Minecraft.getInstance().level;
         return level == null ? ItemStack.EMPTY : recipe.getResultItem(level.registryAccess());
      }

      @Override
      public boolean known(Player player) {
         return ResearchManager.knowsResearch(player, recipe.getResearch());
      }

      @Override
      public void render(ResearchPageScreen s, GuiGraphics g, int x, int y, int mx, int my) {
         s.drawCentered(g, Component.translatable("recipe.type.crucible"), x, y - 104);
         s.drawCrucibleBackdrop(g, x, y);
         AspectList aspects = recipe.getAspects(Minecraft.getInstance().level);
         int size = aspects.size();
         int rows = (size - 1) / 3;
         int shift = (3 - size % 3) * 10;
         int sx = x - 28;
         int sy = y + 8 - 10 * rows;
         int total = 0;
         for (Aspect tag : aspects.getAspectsSortedByName()) {
            int m = total / 3 >= rows && (rows > 1 || size < 3) ? 1 : 0;
            s.drawAspectTag(g, tag, aspects.getAmount(tag), sx + total % 3 * 20 + shift * m, sy + total / 3 * 20, mx, my);
            total++;
         }
         s.drawStackAt(g, output(), x - 8, y - 50, mx, my, false);
         s.drawStackAt(g, cycle(recipe.getCatalyst(), 0), x - 64, y - 56, mx, my, true);
      }
   }

   /** Smelting and friends (the original showed RecipeMisc entries the same way). */
   record CookingView(AbstractCookingRecipe recipe) implements RecipeView {
      @Override
      public ItemStack output() {
         var level = Minecraft.getInstance().level;
         return level == null ? ItemStack.EMPTY : recipe.getResultItem(level.registryAccess());
      }

      @Override
      public void render(ResearchPageScreen s, GuiGraphics g, int x, int y, int mx, int my) {
         s.drawCentered(g, Component.translatable("recipe.type.smelting"), x, y - 104);
         s.drawStackAt(g, output(), x - 8, y - 84, mx, my, false);
         s.drawStackAt(g, cycle(recipe.getIngredients().get(0), 0), x - 8, y - 20, mx, my, true);
         g.renderItem(new ItemStack(Items.FURNACE), x - 8, y + 12);
      }
   }

   /** A shapeless recipe shown but not registered (the 1.12 "fake" recipes). */
   record ShapelessDisplay(java.util.function.Supplier<ItemStack> out, java.util.function.Supplier<List<ItemStack>> inputs) implements RecipeView {
      @Override
      public ItemStack output() {
         return out.get();
      }

      @Override
      public void render(ResearchPageScreen s, GuiGraphics g, int x, int y, int mx, int my) {
         s.drawCraftingGrid(g, x, y);
         s.drawStackAt(g, output(), x - 8, y - 84, mx, my, false);
         s.drawCentered(g, Component.translatable("recipe.type.workbenchshapeless"), x, y - 104);
         List<ItemStack> in = inputs.get();
         for (int i = 0; i < in.size() && i < 9; i++) s.drawStackAt(g, in.get(i), x - 40 + i % 3 * 32, y - 40 + i / 3 * 32, mx, my, true);
      }
   }
}
