package thaumcraft.common.research;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.animal.Parrot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile;
import net.minecraft.world.entity.projectile.LlamaSpit;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.items.IItemHandler;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.research.EnumKnowledgeType;
import thaumcraft.api.research.ResearchCategories;
import thaumcraft.api.research.ResearchCategory;
import thaumcraft.common.aspects.AspectManager;
import thaumcraft.common.registers.ModBlocks;

/**
 * Port of ScanningManager and the scannables of ConfigResearch.initScannables. Scanning runs every scannable thing
 * over the target; each one that matches progresses its research key ("!minecraft:stone", "!terra", "ORE",
 * "f_SPIDER"...) and, if that taught something, gets its onSuccess (knowledge gain).
 */
public final class ScanningManager {
   private static final int MAX_INVENTORY_SCAN = 100;

   /** IScanThing */
   public interface ScanThing {
      boolean checkThing(ServerPlayer player, @Nullable Object obj);

      /** Research key to progress; null/"" = just run onSuccess (and say nothing). */
      @Nullable
      String getResearchKey(ServerPlayer player, @Nullable Object obj);

      default void onSuccess(ServerPlayer player, @Nullable Object obj) {
      }
   }

   private static List<ScanThing> things;

   private ScanningManager() {
   }

   public static void addScannableThing(ScanThing thing) {
      things().add(thing);
   }

   private static synchronized List<ScanThing> things() {
      if (things == null) {
         things = new ArrayList<>();
         registerDefaults(things);
      }
      return things;
   }

   // ------------------------------------------------------------------ scanning

   public static void scanTheThing(ServerPlayer player, @Nullable Object object) {
      boolean found = false;
      boolean suppress = false;
      for (ScanThing thing : things()) {
         if (!thing.checkThing(player, object)) continue;
         String key = thing.getResearchKey(player, object);
         if (key == null || key.isEmpty() || ResearchManager.progressResearch(player, key)) {
            if (key == null || key.isEmpty()) suppress = true;
            found = true;
            thing.onSuccess(player, object);
         }
      }
      if (!suppress) {
         player.displayClientMessage(Component.translatable(found ? "tc.knownobject" : "tc.unknownobject")
            .withStyle(found ? ChatFormatting.GREEN : ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC), true);
      }
      if (found) ResearchManager.markDirty(player);

      if (object instanceof BlockPos pos) {
         IItemHandler handler = player.level().getCapability(Capabilities.ItemHandler.BLOCK, pos, Direction.UP);
         if (handler != null) {
            int scanned = 0;
            for (int slot = 0; slot < handler.getSlots(); slot++) {
               ItemStack stack = handler.getStackInSlot(slot);
               if (!stack.isEmpty()) {
                  scanTheThing(player, stack);
                  scanned++;
               }
               if (scanned >= MAX_INVENTORY_SCAN) {
                  player.displayClientMessage(Component.translatable("tc.invtoolarge").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC), true);
                  break;
               }
            }
         }
      }
   }

   /** Whether scanning this would teach the player anything (the scan highlight). */
   public static boolean isThingStillScannable(ServerPlayer player, @Nullable Object object) {
      for (ScanThing thing : things()) {
         if (thing.checkThing(player, object)) {
            String key = thing.getResearchKey(player, object);
            if (key != null && !ResearchManager.knowledge(player).isResearchKnown(key)) return true;
         }
      }
      return false;
   }

   /** getItemFromParms: the item form of a scanned stack, item entity or block. */
   public static ItemStack getItemFromParms(ServerPlayer player, @Nullable Object obj) {
      if (obj instanceof ItemStack s) return s;
      if (obj instanceof ItemEntity e) return e.getItem();
      if (obj instanceof BlockPos pos) return AspectManager.getItemFromBlock(player.level(), pos);
      return ItemStack.EMPTY;
   }

   @Nullable
   private static AspectList aspectsOf(ServerPlayer player, @Nullable Object obj) {
      if (obj == null) return null;
      AspectManager am = AspectManager.get(player.level());
      if (obj instanceof Entity e && !(obj instanceof ItemEntity)) return am.getEntityAspects(e);
      if (obj instanceof BlockPos pos && am.hasBlockEntry(player.level().getBlockState(pos).getBlock())) return am.getBlockAspects(player.level(), pos);
      ItemStack is = getItemFromParms(player, obj);
      return is.isEmpty() ? null : am.getObjectAspects(player.registryAccess(), is);
   }

   // ------------------------------------------------------------------ scan things

   /** ScanGeneric: anything with aspects; teaches the object and observation knowledge from its aspects. */
   public static final class ScanGeneric implements ScanThing {
      @Override
      public boolean checkThing(ServerPlayer player, Object obj) {
         AspectList al = aspectsOf(player, obj);
         return al != null && !al.isEmpty();
      }

      @Override
      public void onSuccess(ServerPlayer player, Object obj) {
         AspectList al = aspectsOf(player, obj);
         if (al == null) return;
         for (ResearchCategory category : ResearchCategories.CATEGORIES.values()) {
            ResearchManager.addKnowledge(player, EnumKnowledgeType.OBSERVATION, category, category.applyFormula(al));
         }
      }

      @Override
      public String getResearchKey(ServerPlayer player, Object obj) {
         if (obj instanceof Entity e && !(obj instanceof ItemEntity)) return "!" + EntityType.getKey(e.getType());
         if (obj instanceof BlockPos pos) {
            Block block = player.level().getBlockState(pos).getBlock();
            if (AspectManager.get(player.level()).hasBlockEntry(block)) return "!" + BuiltInRegistries.BLOCK.getKey(block);
         }
         ItemStack is = getItemFromParms(player, obj);
         return is.isEmpty() ? null : "!" + BuiltInRegistries.ITEM.getKey(is.getItem());
      }
   }

   /** ScanAspect: the first thing seen with an aspect teaches that aspect. */
   public record ScanAspect(String research, Aspect aspect) implements ScanThing {
      @Override
      public boolean checkThing(ServerPlayer player, Object obj) {
         AspectList al = aspectsOf(player, obj);
         return al != null && al.getAmount(aspect) > 0;
      }

      @Override
      public void onSuccess(ServerPlayer player, Object obj) {
         for (String cat : new String[]{"AUROMANCY", "BASICS", "ALCHEMY"}) {
            ResearchManager.addKnowledge(player, EnumKnowledgeType.OBSERVATION, ResearchCategories.getResearchCategory(cat), 1);
         }
         player.sendSystemMessage(Component.translatable("tc.port.newaspect", aspect.getLocalizedName(), aspect.getLocalizedDescription()));
      }

      @Override
      public String getResearchKey(ServerPlayer player, Object obj) {
         return research;
      }
   }

   /** ScanEntity */
   public record ScanEntity(String research, Predicate<Entity> test) implements ScanThing {
      public static ScanEntity of(String research, Class<? extends Entity> type) {
         return new ScanEntity(research, type::isInstance);
      }

      @Override
      public boolean checkThing(ServerPlayer player, Object obj) {
         return obj instanceof Entity e && !(obj instanceof ItemEntity) && test.test(e);
      }

      @Override
      public String getResearchKey(ServerPlayer player, Object obj) {
         return research;
      }
   }

   /** ScanBlock: a block in the world. */
   public record ScanBlock(String research, Supplier<Set<Block>> blocks) implements ScanThing {
      @SafeVarargs
      public static ScanBlock of(String research, Supplier<? extends Block>... blocks) {
         return new ScanBlock(research, () -> {
            java.util.HashSet<Block> set = new java.util.HashSet<>();
            for (Supplier<? extends Block> b : blocks) set.add(b.get());
            return set;
         });
      }

      @Override
      public boolean checkThing(ServerPlayer player, Object obj) {
         return obj instanceof BlockPos pos && blocks.get().contains(player.level().getBlockState(pos).getBlock());
      }

      @Override
      public String getResearchKey(ServerPlayer player, Object obj) {
         return research;
      }
   }

   /** ScanBlock by tag (stands in for ScanMaterial, whose block materials no longer exist). */
   public record ScanBlockTag(String research, TagKey<Block> tag) implements ScanThing {
      @Override
      public boolean checkThing(ServerPlayer player, Object obj) {
         return obj instanceof BlockPos pos && player.level().getBlockState(pos).is(tag);
      }

      @Override
      public String getResearchKey(ServerPlayer player, Object obj) {
         return research;
      }
   }

   /** ScanItem: an item stack or item entity. */
   public record ScanItem(String research, Predicate<ItemStack> test) implements ScanThing {
      public static ScanItem of(String research, Supplier<? extends Item> item) {
         return new ScanItem(research, s -> s.is(item.get()));
      }

      @Override
      public boolean checkThing(ServerPlayer player, Object obj) {
         ItemStack is = obj instanceof ItemStack s ? s : obj instanceof ItemEntity e ? e.getItem() : ItemStack.EMPTY;
         return !is.isEmpty() && test.test(is);
      }

      @Override
      public String getResearchKey(ServerPlayer player, Object obj) {
         return research;
      }
   }

   /** ScanOreDictionary: an item (or block's item) in any of these tags. */
   public record ScanItemTag(String research, List<TagKey<Item>> tags) implements ScanThing {
      @Override
      public boolean checkThing(ServerPlayer player, Object obj) {
         ItemStack is = getItemFromParms(player, obj);
         return !is.isEmpty() && tags.stream().anyMatch(is::is);
      }

      @Override
      public String getResearchKey(ServerPlayer player, Object obj) {
         return research;
      }
   }

   /** ScanEnchantment, for every enchantment on the scanned item. */
   public static final class ScanEnchantments implements ScanThing {
      @Override
      public boolean checkThing(ServerPlayer player, Object obj) {
         return first(player, obj) != null;
      }

      @Nullable
      private static String first(ServerPlayer player, Object obj) {
         ItemStack is = getItemFromParms(player, obj);
         if (is.isEmpty()) return null;
         var ench = is.getOrDefault(DataComponents.STORED_ENCHANTMENTS, is.getTagEnchantments());
         if (ench.isEmpty()) ench = is.getTagEnchantments();
         for (Object2IntMap.Entry<Holder<Enchantment>> e : ench.entrySet()) {
            String key = "!" + e.getKey().unwrapKey().map(k -> k.location().toString()).orElse("?");
            if (!ResearchManager.knowledge(player).isResearchKnown(key)) return key;
         }
         return ench.isEmpty() ? null : "!" + ench.entrySet().iterator().next().getKey().unwrapKey().map(k -> k.location().toString()).orElse("?");
      }

      @Override
      public String getResearchKey(ServerPlayer player, Object obj) {
         return first(player, obj);
      }
   }

   /** ScanPotion: an effect on a scanned creature or in a scanned potion. */
   public static final class ScanEffects implements ScanThing {
      @Nullable
      private static String first(ServerPlayer player, Object obj) {
         List<Holder<MobEffect>> effects = new ArrayList<>();
         if (obj instanceof LivingEntity e) {
            for (MobEffectInstance i : e.getActiveEffects()) effects.add(i.getEffect());
         } else {
            ItemStack is = getItemFromParms(player, obj);
            PotionContents pc = is.get(DataComponents.POTION_CONTENTS);
            if (pc != null) pc.getAllEffects().forEach(i -> effects.add(i.getEffect()));
         }
         String any = null;
         for (Holder<MobEffect> h : effects) {
            String key = "!" + h.unwrapKey().map(k -> k.location().toString()).orElse("?");
            if (!ResearchManager.knowledge(player).isResearchKnown(key)) return key;
            any = key;
         }
         return any;
      }

      @Override
      public boolean checkThing(ServerPlayer player, Object obj) {
         return obj != null && first(player, obj) != null;
      }

      @Override
      public String getResearchKey(ServerPlayer player, Object obj) {
         return first(player, obj);
      }
   }

   private static void registerDefaults(List<ScanThing> list) {
      list.add(new ScanGeneric());
      for (Aspect a : Aspect.all()) list.add(new ScanAspect("!" + a.getTag(), a));
      list.add(new ScanEnchantments());
      list.add(new ScanEffects());
      // Thaumcraft's own creatures are added as they are ported.
      list.add(ScanEntity.of("f_SPIDER", Spider.class));
      list.add(ScanEntity.of("f_BAT", Bat.class));
      list.add(ScanEntity.of("f_FLY", Bat.class));
      list.add(ScanEntity.of("f_FLY", Parrot.class));
      list.add(ScanEntity.of("f_FLY", Ghast.class));
      list.add(ScanEntity.of("f_FLY", Blaze.class));
      list.add(ScanBlock.of("ORE", ModBlocks.ORE_AMBER, ModBlocks.ORE_CINNABAR, ModBlocks.DEEPSLATE_ORE_CINNABAR, ModBlocks.CRYSTAL_AER,
         ModBlocks.CRYSTAL_IGNIS, ModBlocks.CRYSTAL_AQUA, ModBlocks.CRYSTAL_TERRA, ModBlocks.CRYSTAL_ORDO, ModBlocks.CRYSTAL_PERDITIO,
         ModBlocks.CRYSTAL_VITIUM));
      list.add(ScanBlock.of("!OREAMBER", ModBlocks.ORE_AMBER));
      list.add(ScanBlock.of("!ORECINNABAR", ModBlocks.ORE_CINNABAR, ModBlocks.DEEPSLATE_ORE_CINNABAR));
      list.add(ScanBlock.of("!ORECRYSTAL", ModBlocks.CRYSTAL_AER, ModBlocks.CRYSTAL_IGNIS, ModBlocks.CRYSTAL_AQUA, ModBlocks.CRYSTAL_TERRA,
         ModBlocks.CRYSTAL_ORDO, ModBlocks.CRYSTAL_PERDITIO, ModBlocks.CRYSTAL_VITIUM));
      list.add(ScanBlock.of("PLANTS", ModBlocks.LOG_GREATWOOD, ModBlocks.LOG_SILVERWOOD, ModBlocks.SAPLING_GREATWOOD, ModBlocks.SAPLING_SILVERWOOD,
         ModBlocks.CINDERPEARL, ModBlocks.SHIMMERLEAF, ModBlocks.VISHROOM));
      list.add(ScanBlock.of("!PLANTWOOD", ModBlocks.LOG_GREATWOOD, ModBlocks.LOG_SILVERWOOD, ModBlocks.SAPLING_GREATWOOD, ModBlocks.SAPLING_SILVERWOOD));
      list.add(ScanBlock.of("!PLANTCINDERPEARL", ModBlocks.CINDERPEARL));
      list.add(ScanBlock.of("!PLANTSHIMMERLEAF", ModBlocks.SHIMMERLEAF));
      list.add(ScanBlock.of("!PLANTVISHROOM", ModBlocks.VISHROOM));
      list.add(ScanItem.of("!DRAGONBREATH", () -> Items.DRAGON_BREATH));
      list.add(ScanItem.of("!TOTEMUNDYING", () -> Items.TOTEM_OF_UNDYING));
      list.add(ScanBlock.of("f_TELEPORT", () -> Blocks.NETHER_PORTAL, () -> Blocks.END_PORTAL, () -> Blocks.END_PORTAL_FRAME));
      list.add(ScanItem.of("f_TELEPORT", () -> Items.ENDER_PEARL));
      list.add(ScanEntity.of("f_TELEPORT", EnderMan.class));
      list.add(ScanBlock.of("f_DISPENSER", () -> Blocks.DISPENSER));
      list.add(ScanItem.of("f_DISPENSER", () -> Items.DISPENSER));
      list.add(ScanItem.of("f_MATCLAY", () -> Items.CLAY_BALL));
      list.add(new ScanBlockTag("f_MATCLAY", BlockTags.TERRACOTTA));
      list.add(ScanBlock.of("f_MATCLAY", () -> Blocks.CLAY));
      list.add(new ScanItemTag("f_MATIRON", List.of(Tags.Items.ORES_IRON, Tags.Items.INGOTS_IRON, Tags.Items.STORAGE_BLOCKS_IRON, tag("c:plates/iron"))));
      list.add(new ScanItemTag("f_MATBRASS", List.of(tag("c:ingots/brass"), tag("c:storage_blocks/brass"), tag("c:plates/brass"))));
      list.add(new ScanItemTag("f_MATTHAUMIUM", List.of(tag("c:ingots/thaumium"), tag("c:storage_blocks/thaumium"), tag("c:plates/thaumium"))));
      list.add(new ScanItemTag("f_MATVOID", List.of(tag("c:ingots/void"), tag("c:storage_blocks/void"), tag("c:plates/void"))));
      list.add(ScanEntity.of("f_arrow", AbstractArrow.class));
      list.add(new ScanItem("f_arrow", s -> s.is(ItemTags.ARROWS)));
      list.add(ScanEntity.of("f_fireball", AbstractHurtingProjectile.class));
      list.add(ScanEntity.of("f_spit", LlamaSpit.class));
      list.add(new ScanSky());
   }

   private static TagKey<Item> tag(String id) {
      return TagKey.create(net.minecraft.core.registries.Registries.ITEM, net.minecraft.resources.ResourceLocation.parse(id));
   }
}
