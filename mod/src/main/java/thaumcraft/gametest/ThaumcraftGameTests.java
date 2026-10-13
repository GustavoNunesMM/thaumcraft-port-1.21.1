package thaumcraft.gametest;

import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import thaumcraft.Thaumcraft;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.research.EnumKnowledgeType;
import thaumcraft.api.research.ResearchCategories;
import thaumcraft.common.aspects.AspectManager;
import thaumcraft.common.network.SyncAspectsPayload;
import thaumcraft.common.network.SyncKnowledgePayload;
import thaumcraft.common.registers.ModAttachments;
import thaumcraft.common.registers.ModItems;
import thaumcraft.common.research.PlayerKnowledge;
import thaumcraft.common.research.ScanningManager;

/** End-to-end checks of the aspect + scanning milestone. Run with ./gradlew runGameTestServer. */
@GameTestHolder(Thaumcraft.MODID)
@PrefixGameTestTemplate(false)
public class ThaumcraftGameTests {
   private static AspectList aspects(GameTestHelper h, ItemStack stack) {
      return AspectManager.SERVER.getObjectAspects(h.getLevel().registryAccess(), stack);
   }

   private static void expect(GameTestHelper h, ItemStack stack, Aspect aspect, int amount) {
      AspectList a = aspects(h, stack);
      int got = a == null ? 0 : a.getAmount(aspect);
      h.assertTrue(got == amount, stack.getItem() + " " + aspect.getTag() + ": expected " + amount + ", got " + got + " in " + a);
   }

   private static PlayerKnowledge knowledge(ServerPlayer p) {
      return p.getData(ModAttachments.KNOWLEDGE);
   }

   @GameTest(template = "empty")
   public static void explicitAspectsMatchOriginal(GameTestHelper h) {
      h.assertFalse(AspectManager.SERVER.isDirty(), "aspects were never computed");
      expect(h, new ItemStack(Items.STONE), Aspect.EARTH, 5);
      expect(h, new ItemStack(Items.IRON_INGOT), Aspect.METAL, 15);
      expect(h, new ItemStack(Items.DEEPSLATE_IRON_ORE), Aspect.METAL, 15); // via c:ores/iron tag
      expect(h, new ItemStack(Items.MUSIC_DISC_CAT), Aspect.SENSES, 15);
      expect(h, new ItemStack(Items.CHARCOAL), Aspect.FIRE, 10);
      h.succeed();
   }

   @GameTest(template = "empty")
   public static void derivedAspects(GameTestHelper h) {
      // Crafting: bucket = 3 iron ingots * 15 * 0.75 = 33 metallum (+ vacuos from complex tag)
      expect(h, new ItemStack(Items.BUCKET), Aspect.METAL, 33);
      // Copy: water bucket = bucket + 20 aqua
      expect(h, new ItemStack(Items.WATER_BUCKET), Aspect.WATER, 20);
      // Item bonus: diamond sword aversio = (3 + 3 + 1) * 4
      expect(h, new ItemStack(Items.DIAMOND_SWORD), Aspect.AVERSION, 28);
      // Smithing (port addition): netherite sword inherits from the diamond sword
      h.assertTrue(aspects(h, new ItemStack(Items.NETHERITE_SWORD)) != null, "netherite sword has no aspects");
      // Smelting (port addition): smooth stone = stone + ignis
      expect(h, new ItemStack(Items.SMOOTH_STONE), Aspect.FIRE, 1);
      h.succeed();
   }

   @GameTest(template = "empty")
   public static void stackDependentAspects(GameTestHelper h) {
      ItemStack water = PotionContents.createItemStack(Items.POTION, Potions.WATER);
      expect(h, water, Aspect.WATER, 10); // 5 for water + 5 bottle bonus
      ItemStack healing = PotionContents.createItemStack(Items.SPLASH_POTION, Potions.HEALING);
      AspectList ha = aspects(h, healing);
      h.assertTrue(ha != null && ha.getAmount(Aspect.ALCHEMY) > 0 && ha.getAmount(Aspect.ENERGY) >= 5, "splash healing: " + ha);

      ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
      sword.enchant(h.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SHARPNESS), 3);
      AspectList sa = aspects(h, sword);
      h.assertTrue(sa != null && sa.getAmount(Aspect.MAGIC) == 9, "sharpness III magic should be 9: " + sa);
      h.succeed();
   }

   @GameTest(template = "empty")
   public static void entityAspects(GameTestHelper h) {
      Cow cow = h.spawn(EntityType.COW, new BlockPos(2, 1, 2));
      AspectList a = AspectManager.SERVER.getEntityAspects(cow);
      h.assertTrue(a != null && a.getAmount(Aspect.BEAST) == 15, "cow: " + a);
      h.succeed();
   }

   @GameTest(template = "empty")
   public static void thaumometerScansBlock(GameTestHelper h) {
      ServerPlayer player = h.makeMockServerPlayerInLevel();
      h.setBlock(new BlockPos(2, 1, 2), Blocks.GOLD_BLOCK);
      BlockPos feet = h.absolutePos(new BlockPos(2, 2, 2));
      player.moveTo(feet.getX() + 0.5, feet.getY(), feet.getZ() + 0.5, 0.0F, 90.0F); // looking straight down
      player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.THAUMOMETER.get()));

      ModItems.THAUMOMETER.get().use(h.getLevel(), player, InteractionHand.MAIN_HAND);

      PlayerKnowledge k = knowledge(player);
      h.assertTrue(k.isResearchKnown("!minecraft:gold_block"), "gold block not learned: " + k.getResearchList());
      h.assertTrue(k.isResearchKnown("!metallum"), "metallum not learned: " + k.getResearchList());
      h.assertTrue(k.getKnowledgeRaw(EnumKnowledgeType.OBSERVATION, ResearchCategories.getResearchCategory("BASICS")) > 0, "no observation knowledge gained");
      h.succeed();
   }

   @GameTest(template = "empty")
   public static void rescanTeachesNothing(GameTestHelper h) {
      ServerPlayer player = h.makeMockServerPlayerInLevel();
      h.setBlock(new BlockPos(2, 1, 2), Blocks.STONE);
      BlockPos pos = h.absolutePos(new BlockPos(2, 1, 2));
      ScanningManager.scanTheThing(player, pos);
      int known = knowledge(player).getResearchList().size();
      int obs = knowledge(player).getKnowledgeRaw(EnumKnowledgeType.OBSERVATION, ResearchCategories.getResearchCategory("BASICS"));
      h.assertTrue(known == 2, "stone should teach !minecraft:stone and !terra, got " + knowledge(player).getResearchList());
      ScanningManager.scanTheThing(player, pos);
      h.assertTrue(knowledge(player).getResearchList().size() == known, "rescan added research");
      h.assertTrue(knowledge(player).getKnowledgeRaw(EnumKnowledgeType.OBSERVATION, ResearchCategories.getResearchCategory("BASICS")) == obs, "rescan added knowledge");
      h.assertFalse(ScanningManager.isThingStillScannable(player, pos), "stone still scannable after scan");
      h.succeed();
   }

   @GameTest(template = "empty")
   public static void scanEntityAndDroppedItem(GameTestHelper h) {
      ServerPlayer player = h.makeMockServerPlayerInLevel();
      Cow cow = h.spawn(EntityType.COW, new BlockPos(1, 1, 1));
      ItemEntity apple = h.spawnItem(Items.APPLE, 3, 1, 3);
      ScanningManager.scanTheThing(player, cow);
      ScanningManager.scanTheThing(player, apple);
      PlayerKnowledge k = knowledge(player);
      h.assertTrue(k.isResearchKnown("!minecraft:cow") && k.isResearchKnown("!bestia"), "cow: " + k.getResearchList());
      h.assertTrue(k.isResearchKnown("!minecraft:apple"), "apple: " + k.getResearchList());
      h.succeed();
   }

   @GameTest(template = "empty")
   public static void scanChestContents(GameTestHelper h) {
      ServerPlayer player = h.makeMockServerPlayerInLevel();
      BlockPos rel = new BlockPos(1, 1, 1);
      h.setBlock(rel, Blocks.CHEST);
      ChestBlockEntity chest = h.getBlockEntity(rel);
      chest.setItem(0, new ItemStack(Items.DIAMOND));
      chest.setItem(5, new ItemStack(Items.BLAZE_ROD));
      ScanningManager.scanTheThing(player, h.absolutePos(rel));
      PlayerKnowledge k = knowledge(player);
      h.assertTrue(k.isResearchKnown("!minecraft:chest"), "chest: " + k.getResearchList());
      h.assertTrue(k.isResearchKnown("!minecraft:diamond") && k.isResearchKnown("!minecraft:blaze_rod"), "contents: " + k.getResearchList());
      h.succeed();
   }

   @GameTest(template = "empty")
   public static void knowledgePersistsAndSyncs(GameTestHelper h) {
      PlayerKnowledge k = new PlayerKnowledge();
      k.addResearch("!minecraft:stone");
      k.addKnowledge(EnumKnowledgeType.OBSERVATION, ResearchCategories.getResearchCategory("BASICS"), 7);
      Tag saved = PlayerKnowledge.CODEC.encodeStart(NbtOps.INSTANCE, k).getOrThrow();
      PlayerKnowledge loaded = PlayerKnowledge.CODEC.parse(NbtOps.INSTANCE, saved).getOrThrow();
      h.assertTrue(loaded.isResearchKnown("!minecraft:stone") && loaded.getKnowledgeRaw(EnumKnowledgeType.OBSERVATION, ResearchCategories.getResearchCategory("BASICS")) == 7, "NBT round trip failed");

      RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), h.getLevel().registryAccess());
      SyncKnowledgePayload.STREAM_CODEC.encode(buf, new SyncKnowledgePayload(k));
      PlayerKnowledge synced = SyncKnowledgePayload.STREAM_CODEC.decode(buf).knowledge();
      h.assertTrue(synced.getResearchList().equals(k.getResearchList()) && synced.getKnowledgeRaw(EnumKnowledgeType.OBSERVATION, ResearchCategories.getResearchCategory("BASICS")) == 7, "knowledge packet round trip failed");
      h.succeed();
   }

   @GameTest(template = "empty")
   public static void aspectTablesSurviveNetwork(GameTestHelper h) {
      SyncAspectsPayload sent = AspectManager.SERVER.toPayload();
      RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), h.getLevel().registryAccess());
      SyncAspectsPayload.STREAM_CODEC.encode(buf, sent);
      int bytes = buf.readableBytes();
      SyncAspectsPayload got = SyncAspectsPayload.STREAM_CODEC.decode(buf);
      h.assertTrue(got.items().equals(sent.items()), "item table differs after decode");
      h.assertTrue(got.entities().equals(sent.entities()) && got.potions().equals(sent.potions()) && got.blocks().equals(sent.blocks()), "tables differ");
      h.assertTrue(bytes < 1_000_000, "aspect packet too large: " + bytes);
      Thaumcraft.LOGGER.info("Aspect sync packet: {} bytes, {} items", bytes, got.items().size());
      h.succeed();
   }
}
