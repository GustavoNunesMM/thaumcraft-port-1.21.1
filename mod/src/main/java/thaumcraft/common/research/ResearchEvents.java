package thaumcraft.common.research;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.Filterable;
import net.minecraft.stats.Stats;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile;
import net.minecraft.world.entity.projectile.LlamaSpit;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.WrittenBookContent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerWakeUpEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import thaumcraft.Thaumcraft;
import thaumcraft.common.config.ModConfig;
import thaumcraft.common.items.ThaumonomiconItem;
import thaumcraft.common.items.VisCrystalItem;
import thaumcraft.common.lib.InventoryHelper;

/** The research triggers of PlayerEvents, EntityEvents, CraftingEvents and ConfigResearch.checkPeriodicStuff. */
@EventBusSubscriber(modid = Thaumcraft.MODID)
public final class ResearchEvents {
   private ResearchEvents() {
   }

   private static void learn(ServerPlayer player, String key, String message) {
      PlayerKnowledge k = ResearchManager.knowledge(player);
      if (k.addResearch(key)) {
         ResearchManager.markDirty(player);
         if (message != null) player.displayClientMessage(Component.translatable(message).withStyle(ChatFormatting.DARK_PURPLE), true);
      }
   }

   @SubscribeEvent
   public static void pickup(ItemEntityPickupEvent.Post event) {
      if (!(event.getPlayer() instanceof ServerPlayer player)) return;
      ItemStack stack = event.getOriginalStack();
      PlayerKnowledge k = ResearchManager.knowledge(player);
      if (stack.getItem() instanceof VisCrystalItem && !k.isResearchKnown("!gotcrystals")) {
         k.addResearch("!gotcrystals");
         ResearchManager.markDirty(player);
         player.sendSystemMessage(Component.translatable("got.crystals").withStyle(ChatFormatting.DARK_PURPLE));
         if (ModConfig.noSleep() && !k.isResearchKnown("!gotdream")) giveDreamJournal(player);
      }
      if (stack.getItem() instanceof ThaumonomiconItem && !k.isResearchKnown("!gotthaumonomicon")) {
         k.addResearch("!gotthaumonomicon");
         ResearchManager.markDirty(player);
      }
   }

   @SubscribeEvent
   public static void wakeUp(PlayerWakeUpEvent event) {
      if (event.getEntity() instanceof ServerPlayer player) {
         PlayerKnowledge k = ResearchManager.knowledge(player);
         if (k.isResearchKnown("!gotcrystals") && !k.isResearchKnown("!gotdream")) giveDreamJournal(player);
      }
   }

   /** The dream journal: a written book pointing the player at salis mundus. */
   public static void giveDreamJournal(ServerPlayer player) {
      ResearchManager.knowledge(player).addResearch("!gotdream");
      ResearchManager.markDirty(player);
      ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
      java.util.List<Filterable<Component>> pages = new java.util.ArrayList<>();
      for (int i = 1; i <= 3; i++) pages.add(Filterable.passThrough(Component.translatable("book.start." + i)));
      book.set(DataComponents.WRITTEN_BOOK_CONTENT, new WrittenBookContent(
         Filterable.passThrough("Dream Journal"), player.getGameProfile().getName(), 3, pages, true));
      book.set(DataComponents.CUSTOM_NAME, Component.translatable("book.start.title").withStyle(s -> s.withItalic(false)));
      InventoryHelper.giveOrDrop(player, book);
      player.sendSystemMessage(Component.translatable("got.dream").withStyle(ChatFormatting.DARK_PURPLE));
   }

   @SubscribeEvent
   public static void crafted(PlayerEvent.ItemCraftedEvent event) {
      if (event.getEntity() instanceof ServerPlayer player) ResearchManager.onItemCrafted(player, event.getCrafting());
   }

   @SubscribeEvent
   public static void hurt(LivingIncomingDamageEvent event) {
      if (!(event.getEntity() instanceof ServerPlayer player)) return;
      var source = event.getSource();
      if (source.is(DamageTypeTags.IS_FIRE) && ResearchManager.knowsResearchStrict(player, "BASEAUROMANCY@2")) {
         learn(player, "f_onfire", "got.onfire");
      }
      if (source.getDirectEntity() != null && ResearchManager.knowsResearchStrict(player, "FOCUSPROJECTILE@2")) {
         if (source.getDirectEntity() instanceof AbstractArrow) learn(player, "f_arrow", "got.projectile");
         if (source.getDirectEntity() instanceof AbstractHurtingProjectile) learn(player, "f_fireball", "got.projectile");
         if (source.getDirectEntity() instanceof LlamaSpit) learn(player, "f_spit", "got.projectile");
      }
   }

   @SubscribeEvent
   public static void playerTick(PlayerTickEvent.Post event) {
      if (event.getEntity() instanceof ServerPlayer player && player.tickCount % 200 == 0) checkPeriodicStuff(player);
   }

   /** ConfigResearch.checkPeriodicStuff */
   public static void checkPeriodicStuff(ServerPlayer player) {
      PlayerKnowledge k = ResearchManager.knowledge(player);
      var biome = player.level().getBiome(player.blockPosition());
      if (biome.is(Tags.Biomes.IS_NETHER)) learn(player, "m_hellandback", "got.hellandback");
      if (biome.is(Tags.Biomes.IS_END)) learn(player, "m_endoftheworld", "got.endoftheworld");
      if (k.isResearchKnown("UNLOCKAUROMANCY@1") && !k.isResearchKnown("UNLOCKAUROMANCY@2")) {
         // 1.12: within 10 blocks of the bottom of the world, or above 40% of its height.
         int min = player.level().getMinBuildHeight();
         if (player.getY() < min + 10) learn(player, "m_deepdown", "got.deepdown");
         if (player.getY() > player.level().getMaxBuildHeight() * 0.4) learn(player, "m_uphigh", "got.uphigh");
      }
      var stats = player.getStats();
      if (stats.getValue(Stats.CUSTOM.get(Stats.WALK_ONE_CM)) > 160000) learn(player, "m_walker", null);
      if (stats.getValue(Stats.CUSTOM.get(Stats.SPRINT_ONE_CM)) > 80000) learn(player, "m_runner", null);
      if (stats.getValue(Stats.CUSTOM.get(Stats.JUMP)) > 500) learn(player, "m_jumper", null);
      if (stats.getValue(Stats.CUSTOM.get(Stats.SWIM_ONE_CM)) > 8000) learn(player, "m_swimmer", null);
   }
}
