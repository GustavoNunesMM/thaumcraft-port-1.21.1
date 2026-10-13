package thaumcraft.common.aspects;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.item.crafting.SmithingTransformRecipe;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.Tags;
import thaumcraft.Thaumcraft;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectHelper;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.network.SyncAspectsPayload;
import thaumcraft.common.registers.ModDataComponents;

/**
 * Computes and holds the aspects of every item, block, entity and potion.
 * Port of ThaumcraftCraftingManager.getObjectTags/generateTags + ConfigAspects from 1.12.
 *
 * <p>The server computes everything after datapacks load (explicit JSON entries, then recipe-derived
 * aspects for everything else) and syncs the result to clients, so both sides answer lookups the same way.
 */
public class AspectManager {
   public static final AspectManager SERVER = new AspectManager();
   public static final AspectManager CLIENT = new AspectManager();

   private static final int CAP = 500;
   private static final int MAX_DEPTH = 100;

   private Map<Item, AspectList> items = Map.of();
   private Map<Block, AspectList> blocks = Map.of();
   private Map<EntityType<?>, AspectList> entities = Map.of();
   private Map<ResourceKey<Potion>, AspectList> potions = Map.of();
   private boolean dirty = true;

   public static AspectManager get(Level level) {
      return level.isClientSide ? CLIENT : SERVER;
   }

   public void clear() {
      items = Map.of();
      blocks = Map.of();
      entities = Map.of();
      potions = Map.of();
      dirty = true;
   }

   public void invalidate() {
      dirty = true;
   }

   public boolean isDirty() {
      return dirty;
   }

   // ------------------------------------------------------------------ lookups

   /** Aspects of a stack, including potion contents and enchantments. Null when it has none. */
   public AspectList getObjectAspects(HolderLookup.Provider registries, ItemStack stack) {
      if (stack.isEmpty()) return null;
      // Essentia containers (vis crystals...) are worth exactly what they hold, as in the original.
      AspectList held = stack.get(ModDataComponents.ESSENTIA.get());
      if (held != null && !held.isEmpty()) return held.copy();
      AspectList base = null;
      PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
      if (contents != null && contents.potion().isPresent()) {
         AspectList pa = contents.potion().get().unwrapKey().map(potions::get).orElse(null);
         Aspect bonus = potionItemBonus(stack.getItem());
         if (pa != null && bonus != null) base = pa.copy().add(bonus, 5);
      }
      if (base == null) {
         AspectList ia = items.get(stack.getItem());
         base = ia == null ? null : ia.copy();
      }
      AspectList ench = enchantmentAspects(stack);
      if (ench != null) {
         base = base == null ? ench : base.merge(ench);
      }
      if (base == null || base.isEmpty()) return null;
      return AspectHelper.cullTags(base);
   }

   public AspectList getItemAspects(Item item) {
      AspectList a = items.get(item);
      return a == null ? null : a.copy();
   }

   /** Aspects of a block in the world. Blocks without an item form (fire, portals) have their own entries. */
   public AspectList getBlockAspects(Level level, BlockPos pos) {
      AspectList b = blocks.get(level.getBlockState(pos).getBlock());
      if (b != null) return b.copy();
      return getObjectAspects(level.registryAccess(), getItemFromBlock(level, pos));
   }

   public static ItemStack getItemFromBlock(Level level, BlockPos pos) {
      BlockState state = level.getBlockState(pos);
      ItemStack is = new ItemStack(state.getBlock().asItem());
      if (is.isEmpty()) {
         if (state.getFluidState().is(FluidTags.WATER)) is = new ItemStack(Items.WATER_BUCKET);
         else if (state.getFluidState().is(FluidTags.LAVA)) is = new ItemStack(Items.LAVA_BUCKET);
      }
      return is;
   }

   public boolean hasBlockEntry(Block block) {
      return blocks.containsKey(block);
   }

   public AspectList getEntityAspects(Entity entity) {
      if (entity instanceof Player player) {
         // As in the original: humanus plus three aspects picked from the player's name.
         AspectList tags = new AspectList().add(Aspect.MAN, 4);
         Random rand = new Random(player.getGameProfile().getName().hashCode());
         Aspect[] all = Aspect.all().toArray(new Aspect[0]);
         for (int i = 0; i < 3; i++) tags.add(all[rand.nextInt(all.length)], 15);
         return tags;
      }
      AspectList a = entities.get(entity.getType());
      return a == null ? null : a.copy();
   }

   private static Aspect potionItemBonus(Item item) {
      if (item == Items.POTION) return Aspect.WATER;
      if (item == Items.TIPPED_ARROW) return Aspect.AVERSION;
      if (item == Items.SPLASH_POTION) return Aspect.ENERGY;
      if (item == Items.LINGERING_POTION) return Aspect.TRAP;
      return null;
   }

   private static AspectList enchantmentAspects(ItemStack stack) {
      ItemEnchantments ench = stack.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY);
      if (ench.isEmpty()) ench = stack.getTagEnchantments();
      if (ench.isEmpty()) return null;
      AspectList tmp = new AspectList();
      int magic = 0;
      for (Object2IntMap.Entry<Holder<Enchantment>> e : ench.entrySet()) {
         Holder<Enchantment> h = e.getKey();
         int lvl = e.getIntValue() * 3;
         EnchantmentAspects.apply(h, lvl, tmp);
         int weight = h.value().getWeight();
         if (weight <= 1) magic += 6;
         else if (weight <= 2) magic += 4;
         else if (weight <= 5) magic += 2;
         magic += lvl;
      }
      if (magic > 0) tmp.merge(Aspect.MAGIC, magic);
      return tmp;
   }

   // ------------------------------------------------------------------ sync

   public SyncAspectsPayload toPayload() {
      return new SyncAspectsPayload(items, blocks, entities, potions);
   }

   public void acceptSync(SyncAspectsPayload p) {
      items = p.items();
      blocks = p.blocks();
      entities = p.entities();
      potions = p.potions();
      dirty = false;
   }

   // ------------------------------------------------------------------ computation (server)

   public void compute(MinecraftServer server) {
      long start = System.currentTimeMillis();
      new Computation(server).run();
      dirty = false;
      if (Boolean.getBoolean("thaumcraft.dumpAspects")) dump(server);
      Thaumcraft.LOGGER.info(
         "Computed aspects for {} items, {} blocks, {} entities, {} potions in {} ms",
         items.size(), blocks.size(), entities.size(), potions.size(), System.currentTimeMillis() - start
      );
   }

   /** Debug: -Dthaumcraft.dumpAspects=true writes every item's aspects to thaumcraft-aspects.txt in the game dir. */
   private void dump(MinecraftServer server) {
      java.nio.file.Path out = server.getServerDirectory().resolve("thaumcraft-aspects.txt");
      List<String> lines = new ArrayList<>();
      BuiltInRegistries.ITEM.entrySet().stream()
         .sorted(java.util.Comparator.comparing(e -> e.getKey().location().toString()))
         .forEach(e -> lines.add(e.getKey().location() + " = " + items.getOrDefault(e.getValue(), new AspectList())));
      entities.forEach((t, a) -> lines.add("entity " + EntityType.getKey(t) + " = " + a));
      potions.forEach((k, a) -> lines.add("potion " + k.location() + " = " + a));
      try {
         java.nio.file.Files.write(out, lines);
         Thaumcraft.LOGGER.info("Wrote aspect dump to {}", out);
      } catch (java.io.IOException ex) {
         Thaumcraft.LOGGER.error("Failed to write aspect dump", ex);
      }
   }

   private record RecipeInfo(List<Ingredient> inputs, int count) {}

   private class Computation {
      private final MinecraftServer server;
      private final HolderLookup.Provider registries;
      /** "Registered" aspects before item bonuses, like CommonInternals.objectTags. */
      private final Map<Item, AspectList> base = new HashMap<>();
      private final Set<Item> failed = new HashSet<>();
      private final Map<Item, List<RecipeInfo>> recipes = new HashMap<>();
      private final Map<Item, List<RecipeInfo>> smelting = new HashMap<>();
      private final Map<Item, thaumcraft.api.crafting.CrucibleRecipe> crucible = new HashMap<>();
      private final Map<Item, thaumcraft.api.crafting.InfusionRecipe> infusion = new HashMap<>();
      private final Map<Item, AspectList> finals = new HashMap<>();

      Computation(MinecraftServer server) {
         this.server = server;
         this.registries = server.registryAccess();
      }

      void run() {
         indexRecipes();
         Map<Block, AspectList> newBlocks = new HashMap<>();
         for (AspectData.ItemEntry e : AspectData.INSTANCE.items()) {
            try {
               applyEntry(e, newBlocks);
            } catch (Exception ex) {
               Thaumcraft.LOGGER.warn("Failed to apply aspect entry {}: {}", e, ex.toString());
            }
         }
         for (Item item : BuiltInRegistries.ITEM) {
            if (item == Items.AIR) continue;
            AspectList a = lookup(item);
            if (a != null && !a.isEmpty()) finals.put(item, a);
         }
         Map<EntityType<?>, AspectList> newEntities = new HashMap<>();
         for (AspectData.EntityEntry e : AspectData.INSTANCE.entities()) {
            BuiltInRegistries.ENTITY_TYPE.getOptional(e.entity()).ifPresentOrElse(
               t -> newEntities.put(t, e.aspects().copy()),
               () -> Thaumcraft.LOGGER.debug("Unknown entity in aspect data: {}", e.entity())
            );
         }
         items = Map.copyOf(finals);
         blocks = Map.copyOf(newBlocks);
         entities = Map.copyOf(newEntities);
         potions = Map.copyOf(computePotions());
      }

      private void indexRecipes() {
         for (RecipeHolder<?> holder : server.getRecipeManager().getRecipes()) {
            Recipe<?> r = holder.value();
            if (r instanceof thaumcraft.api.crafting.CrucibleRecipe cr) {
               ItemStack out = cr.getResultItem(registries);
               // Essentia holders are worth their contents; hedge recipes are defined by the aspects themselves.
               if (!out.isEmpty() && !out.has(ModDataComponents.ESSENTIA.get()) && !cr.hasRuntimeAspects()) crucible.putIfAbsent(out.getItem(), cr);
               continue;
            }
            if (r instanceof thaumcraft.api.crafting.InfusionRecipe ir) {
               ItemStack out = ir.getResultItem(registries);
               if (!out.isEmpty() && !out.has(ModDataComponents.ESSENTIA.get())) infusion.putIfAbsent(out.getItem(), ir);
               continue;
            }
            List<Ingredient> inputs;
            Map<Item, List<RecipeInfo>> index = recipes;
            if (r instanceof SmeltingRecipe sr) {
               inputs = sr.getIngredients();
               index = smelting;
            } else if (r instanceof CraftingRecipe cr && !cr.isSpecial()) {
               inputs = cr.getIngredients();
            } else if (r instanceof thaumcraft.api.crafting.IArcaneRecipe ar) {
               inputs = ar.getIngredients(); // arcane recipes were crafting recipes in 1.12 (crystals not counted)
            } else if (r instanceof SmithingTransformRecipe st) {
               inputs = List.of(st.template, st.base, st.addition);
            } else {
               continue;
            }
            ItemStack out = r.getResultItem(registries);
            if (out.isEmpty()) continue;
            index.computeIfAbsent(out.getItem(), k -> new ArrayList<>()).add(new RecipeInfo(inputs, out.getCount()));
         }
      }

      private void applyEntry(AspectData.ItemEntry e, Map<Block, AspectList> newBlocks) {
         AspectList given = e.aspects().copy();
         if (e.copy().isPresent()) {
            Item src = BuiltInRegistries.ITEM.get(e.copy().get());
            AspectList c = lookup(src);
            if (c != null) given = c.copy().add(given);
         }
         if (e.block().isPresent()) {
            BuiltInRegistries.BLOCK.getOptional(e.block().get()).ifPresent(b -> newBlocks.put(b, e.aspects().copy()));
            return;
         }
         List<Item> targets = new ArrayList<>();
         if (e.item().isPresent()) {
            BuiltInRegistries.ITEM.getOptional(e.item().get()).ifPresentOrElse(
               targets::add, () -> Thaumcraft.LOGGER.debug("Unknown item in aspect data: {}", e.item().get())
            );
         } else if (e.tag().isPresent()) {
            TagKey<Item> tag = TagKey.create(Registries.ITEM, e.tag().get());
            BuiltInRegistries.ITEM.getTagOrEmpty(tag).forEach(h -> targets.add(h.value()));
         }
         for (Item item : targets) {
            if (e.complex()) {
               AspectList existing = base.containsKey(item) ? base.get(item) : generate(item, new LinkedHashSet<>());
               AspectList result = existing == null ? new AspectList() : existing.copy();
               base.put(item, result.merge(given));
            } else {
               base.put(item, given.copy());
            }
            finals.remove(item);
         }
      }

      /** Equivalent of getObjectTags: registered/generated aspects plus item bonuses, culled and capped. */
      private AspectList lookup(Item item) {
         return lookup(item, new LinkedHashSet<>());
      }

      private AspectList lookup(Item item, LinkedHashSet<Item> history) {
         AspectList cached = finals.get(item);
         if (cached != null) return cached;
         AspectList tags = base.containsKey(item) ? base.get(item) : generate(item, history);
         AspectList out = capAspects(applyItemBonus(item, tags), CAP);
         if (out != null && (base.containsKey(item) || history.isEmpty())) finals.put(item, out);
         return out;
      }

      private AspectList generate(Item item, LinkedHashSet<Item> history) {
         if (base.containsKey(item)) return base.get(item);
         if (failed.contains(item) || history.contains(item) || history.size() >= MAX_DEPTH) return null;
         history.add(item);
         AspectList ret = generateFromRecipes(item, history);
         history.remove(item);
         ret = capAspects(ret, CAP);
         if (ret != null) base.put(item, ret);
         else if (history.isEmpty()) failed.add(item);
         return ret;
      }

      /** generateTagsFromCrucibleRecipes: the catalyst's aspects plus the square root of the recipe's, per item made. */
      private AspectList generateFromCrucible(Item item, LinkedHashSet<Item> history) {
         thaumcraft.api.crafting.CrucibleRecipe cr = crucible.get(item);
         if (cr == null) return null;
         ItemStack[] cats = cr.getCatalyst().getItems();
         if (cats.length == 0 || cats[0].isEmpty()) return null;
         AspectList ot = cr.getBaseAspects();
         int ss = cr.getResultItem(registries).getCount();
         AspectList out = new AspectList();
         AspectList ot2 = lookup(cats[0].getItem(), history);
         if (ot2 != null) out.add(ot2);
         for (Aspect tt : ot.getAspects()) out.add(tt, (int) (Math.sqrt(ot.getAmount(tt)) / ss));
         out.aspects.values().removeIf(v -> v <= 0);
         return out;
      }

      /** generateTagsFromInfusionRecipes: the central item's and components' aspects plus the square root of the recipe's. */
      private AspectList generateFromInfusion(Item item, LinkedHashSet<Item> history) {
         thaumcraft.api.crafting.InfusionRecipe ir = infusion.get(item);
         if (ir == null) return null;
         List<Ingredient> ingredients = new ArrayList<>();
         ingredients.add(ir.central);
         ingredients.addAll(ir.getComponents());
         int ss = ir.getResultItem(registries).getCount();
         AspectList out = aspectsFromIngredients(new RecipeInfo(ingredients, ss), history);
         for (Aspect tt : ir.aspects.getAspects()) out.add(tt, (int) (Math.sqrt(ir.aspects.getAmount(tt)) / ss));
         out.aspects.values().removeIf(v -> v <= 0);
         return out;
      }

      private AspectList generateFromRecipes(Item item, LinkedHashSet<Item> history) {
         AspectList fromCrucible = generateFromCrucible(item, history);
         if (fromCrucible != null) return fromCrucible;
         AspectList fromInfusion = generateFromInfusion(item, history);
         if (fromInfusion != null) return fromInfusion;
         AspectList ret = null;
         int value = Integer.MAX_VALUE;
         for (RecipeInfo r : recipes.getOrDefault(item, List.of())) {
            AspectList ph = aspectsFromIngredients(r, history);
            ph.aspects.values().removeIf(v -> v <= 0);
            if (ph.visSize() < value && ph.visSize() > 0) {
               ret = ph;
               value = ph.visSize();
            }
         }
         return ret != null ? ret : generateFromSmelting(item, history);
      }

      /**
       * Port addition: the original only derived aspects from crafting recipes. Smelting-only items
       * (smooth stone, glass-like blocks added after 1.12...) get their input's aspects plus ignis,
       * the same pattern ConfigAspects used by hand for brick and terracotta.
       */
      private AspectList generateFromSmelting(Item item, LinkedHashSet<Item> history) {
         for (RecipeInfo r : smelting.getOrDefault(item, List.of())) {
            ItemStack[] stacks = r.inputs().get(0).getItems();
            if (stacks.length == 0) continue;
            AspectList in = lookup(stacks[0].getItem(), history);
            if (in == null || in.isEmpty()) continue;
            AspectList out = new AspectList();
            in.aspects.forEach((a, v) -> {
               if (v / r.count() > 0) out.add(a, v / r.count());
            });
            return out.add(Aspect.FIRE, 1);
         }
         return null;
      }

      private AspectList aspectsFromIngredients(RecipeInfo recipe, LinkedHashSet<Item> history) {
         AspectList mid = new AspectList();
         for (Ingredient ing : recipe.inputs()) {
            ItemStack[] stacks = ing.getItems();
            if (stacks.length == 0) continue;
            // The original always took the first option; take the first one that has aspects instead,
            // so e.g. quartz stairs don't come out empty because chiseled quartz is listed first.
            ItemStack is = stacks[0];
            AspectList obj = null;
            for (ItemStack option : stacks) {
               obj = lookup(option.getItem(), history);
               if (obj != null && !obj.isEmpty()) {
                  is = option;
                  break;
               }
            }
            if (obj != null) mid.add(obj);
            if (is.hasCraftingRemainingItem()) {
               AspectList rem = lookup(is.getCraftingRemainingItem().getItem(), history);
               if (rem != null) rem.aspects.forEach(mid::reduce);
            }
         }
         AspectList out = new AspectList();
         for (Aspect as : mid.getAspects()) {
            float v = mid.getAmount(as) * 0.75F / recipe.count();
            if (v < 1.0F && v > 0.75F) v = 1.0F;
            if ((int) v > 0) out.add(as, (int) v);
         }
         return out;
      }

      /** Port of getBonusTags (minus enchantments, which depend on the stack and are added at lookup). */
      private AspectList applyItemBonus(Item item, AspectList source) {
         AspectList tmp = source == null ? new AspectList() : source.copy();
         if (item instanceof ArmorItem armor) {
            tmp.merge(Aspect.PROTECT, armor.getDefense() * 4);
         } else if (item instanceof SwordItem sword) {
            float dmg = 3.0F + sword.getTier().getAttackDamageBonus();
            if (dmg + 1.0F > 0.0F) tmp.merge(Aspect.AVERSION, (int) (dmg + 1.0F) * 4);
         } else if (item instanceof BowItem || item instanceof CrossbowItem) {
            tmp.merge(Aspect.AVERSION, 10).merge(Aspect.FLIGHT, 5);
         } else if (item instanceof ShearsItem || item instanceof HoeItem) {
            int dur = new ItemStack(item).getMaxDamage();
            if (dur <= Tiers.WOOD.getUses()) tmp.merge(Aspect.TOOL, 4);
            else if (dur <= Tiers.STONE.getUses() || dur <= Tiers.GOLD.getUses()) tmp.merge(Aspect.TOOL, 8);
            else if (dur <= Tiers.IRON.getUses()) tmp.merge(Aspect.TOOL, 12);
            else tmp.merge(Aspect.TOOL, 16);
         } else if (item instanceof DiggerItem tool) {
            tmp.merge(Aspect.TOOL, (harvestLevel(tool.getTier()) + 1) * 4);
         }
         if (item.builtInRegistryHolder().is(Tags.Items.DYES)) {
            tmp.merge(Aspect.SENSES, 5);
         }
         return tmp.isEmpty() ? null : AspectHelper.cullTags(tmp);
      }

      private static int harvestLevel(Tier tier) {
         if (tier instanceof Tiers t) {
            return switch (t) {
               case WOOD, GOLD -> 0;
               case STONE -> 1;
               case IRON -> 2;
               case DIAMOND -> 3;
               case NETHERITE -> 4;
            };
         }
         return tier.getUses() >= Tiers.DIAMOND.getUses() ? 3 : tier.getUses() >= Tiers.IRON.getUses() ? 2 : 1;
      }

      private static AspectList capAspects(AspectList source, int amount) {
         if (source == null) return null;
         AspectList out = new AspectList();
         source.aspects.forEach((a, v) -> out.merge(a, Math.min(amount, v)));
         return out;
      }

      // -------------------------------------------------------------- potions

      private Map<ResourceKey<Potion>, AspectList> computePotions() {
         Map<ResourceKey<Potion>, AspectList> out = new HashMap<>();
         PotionBrewing brewing = server.potionBrewing();
         BuiltInRegistries.POTION.holders().forEach(h -> out.put(h.key(), potionAspects(brewing, h)));
         return out;
      }

      /** Port of ConfigAspects.getPotionAspects (without the per-item bonus, applied at lookup). */
      private AspectList potionAspects(PotionBrewing brewing, Holder<Potion> potion) {
         AspectList tmp = new AspectList();
         if (potion.is(Potions.WATER)) {
            return tmp.add(Aspect.WATER, 5);
         }
         List<ItemStack> reagents = new ArrayList<>();
         potionReagents(brewing, potion, reagents, 0);
         for (ItemStack r : reagents) {
            AspectList ra = lookup(r.getItem());
            if (ra != null) tmp.add(ra);
            tmp.add(Aspect.ALCHEMY, 3);
         }
         for (Aspect a : tmp.getAspects()) {
            tmp.remove(a, (int) (tmp.getAmount(a) * 0.66));
         }
         if (reagents.isEmpty()) {
            tmp.add(Aspect.MAGIC, 5).add(Aspect.ALCHEMY, 5);
         }
         return tmp;
      }

      private void potionReagents(PotionBrewing brewing, Holder<Potion> potion, List<ItemStack> out, int depth) {
         if (depth > 16) return;
         for (PotionBrewing.Mix<Potion> mix : brewing.potionMixes) {
            if (mix.to().is(potion)) {
               ItemStack[] stacks = mix.ingredient().getItems();
               if (stacks.length > 0) out.add(stacks[0]);
               if (!mix.from().is(Potions.WATER)) potionReagents(brewing, mix.from(), out, depth + 1);
               return;
            }
         }
      }
   }

   /** Enchantment -> aspect mapping from getBonusTags; entries marked "port" cover enchantments added after 1.12. */
   private static final class EnchantmentAspects {
      static void apply(Holder<Enchantment> e, int lvl, AspectList tmp) {
         if (is(e, Enchantments.AQUA_AFFINITY)) tmp.merge(Aspect.WATER, lvl);
         else if (is(e, Enchantments.BANE_OF_ARTHROPODS)) tmp.merge(Aspect.BEAST, lvl / 2).merge(Aspect.AVERSION, lvl / 2);
         else if (is(e, Enchantments.BLAST_PROTECTION)) tmp.merge(Aspect.PROTECT, lvl / 2).merge(Aspect.ENTROPY, lvl / 2);
         else if (is(e, Enchantments.EFFICIENCY)) tmp.merge(Aspect.TOOL, lvl);
         else if (is(e, Enchantments.FEATHER_FALLING)) tmp.merge(Aspect.FLIGHT, lvl);
         else if (is(e, Enchantments.FIRE_ASPECT)) tmp.merge(Aspect.FIRE, lvl / 2).merge(Aspect.AVERSION, lvl / 2);
         else if (is(e, Enchantments.FIRE_PROTECTION)) tmp.merge(Aspect.PROTECT, lvl / 2).merge(Aspect.FIRE, lvl / 2);
         else if (is(e, Enchantments.FLAME)) tmp.merge(Aspect.FIRE, lvl);
         else if (is(e, Enchantments.FORTUNE)) tmp.merge(Aspect.DESIRE, lvl);
         else if (is(e, Enchantments.INFINITY)) tmp.merge(Aspect.CRAFT, lvl);
         else if (is(e, Enchantments.KNOCKBACK)) tmp.merge(Aspect.AIR, lvl);
         else if (is(e, Enchantments.LOOTING)) tmp.merge(Aspect.DESIRE, lvl);
         else if (is(e, Enchantments.POWER)) tmp.merge(Aspect.AVERSION, lvl);
         else if (is(e, Enchantments.PROJECTILE_PROTECTION)) tmp.merge(Aspect.PROTECT, lvl);
         else if (is(e, Enchantments.PROTECTION)) tmp.merge(Aspect.PROTECT, lvl);
         else if (is(e, Enchantments.PUNCH)) tmp.merge(Aspect.AIR, lvl);
         else if (is(e, Enchantments.RESPIRATION)) tmp.merge(Aspect.AIR, lvl);
         else if (is(e, Enchantments.SHARPNESS)) tmp.merge(Aspect.AVERSION, lvl);
         else if (is(e, Enchantments.SILK_TOUCH)) tmp.merge(Aspect.EXCHANGE, lvl);
         else if (is(e, Enchantments.THORNS)) tmp.merge(Aspect.AVERSION, lvl);
         else if (is(e, Enchantments.SMITE)) tmp.merge(Aspect.UNDEAD, lvl / 2).merge(Aspect.AVERSION, lvl / 2);
         else if (is(e, Enchantments.UNBREAKING)) tmp.merge(Aspect.EARTH, lvl);
         else if (is(e, Enchantments.DEPTH_STRIDER)) tmp.merge(Aspect.WATER, lvl);
         else if (is(e, Enchantments.LUCK_OF_THE_SEA)) tmp.merge(Aspect.DESIRE, lvl);
         else if (is(e, Enchantments.LURE)) tmp.merge(Aspect.BEAST, lvl);
         else if (is(e, Enchantments.FROST_WALKER)) tmp.merge(Aspect.COLD, lvl);
         else if (is(e, Enchantments.MENDING)) tmp.merge(Aspect.CRAFT, lvl);
         // port: enchantments that did not exist in 1.12
         else if (is(e, Enchantments.SWEEPING_EDGE)) tmp.merge(Aspect.AVERSION, lvl);
         else if (is(e, Enchantments.IMPALING)) tmp.merge(Aspect.AVERSION, lvl / 2).merge(Aspect.WATER, lvl / 2);
         else if (is(e, Enchantments.RIPTIDE)) tmp.merge(Aspect.WATER, lvl / 2).merge(Aspect.MOTION, lvl / 2);
         else if (is(e, Enchantments.LOYALTY)) tmp.merge(Aspect.MOTION, lvl);
         else if (is(e, Enchantments.CHANNELING)) tmp.merge(Aspect.ENERGY, lvl);
         else if (is(e, Enchantments.MULTISHOT)) tmp.merge(Aspect.AVERSION, lvl);
         else if (is(e, Enchantments.PIERCING)) tmp.merge(Aspect.AVERSION, lvl);
         else if (is(e, Enchantments.QUICK_CHARGE)) tmp.merge(Aspect.MOTION, lvl);
         else if (is(e, Enchantments.SOUL_SPEED)) tmp.merge(Aspect.SOUL, lvl / 2).merge(Aspect.MOTION, lvl / 2);
         else if (is(e, Enchantments.SWIFT_SNEAK)) tmp.merge(Aspect.MOTION, lvl / 2).merge(Aspect.SENSES, lvl / 2);
         else if (is(e, Enchantments.DENSITY)) tmp.merge(Aspect.EARTH, lvl);
         else if (is(e, Enchantments.BREACH)) tmp.merge(Aspect.AVERSION, lvl / 2).merge(Aspect.ENTROPY, lvl / 2);
         else if (is(e, Enchantments.WIND_BURST)) tmp.merge(Aspect.AIR, lvl / 2).merge(Aspect.FLIGHT, lvl / 2);
         else if (is(e, Enchantments.BINDING_CURSE)) tmp.merge(Aspect.TRAP, lvl);
         else if (is(e, Enchantments.VANISHING_CURSE)) tmp.merge(Aspect.VOID, lvl);
      }

      private static boolean is(Holder<Enchantment> h, ResourceKey<Enchantment> key) {
         return h.is(key);
      }
   }
}
