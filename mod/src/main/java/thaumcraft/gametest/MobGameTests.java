package thaumcraft.gametest;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import thaumcraft.Thaumcraft;
import thaumcraft.common.blocks.devices.SmeltingBonus;
import thaumcraft.common.crafting.TripleMeatTreatRecipe;
import thaumcraft.common.entities.SpecialItemEntity;
import thaumcraft.common.entities.monster.cult.CultistClericEntity;
import thaumcraft.common.entities.monster.cult.CultistEntity;
import thaumcraft.common.entities.monster.cult.CultistKnightEntity;
import thaumcraft.common.entities.monster.cult.CultistLeaderEntity;
import thaumcraft.common.entities.monster.cult.CultistPortalGreaterEntity;
import thaumcraft.common.entities.monster.cult.CultistPortalLesserEntity;
import thaumcraft.common.registers.ModBlocks;
import thaumcraft.common.registers.ModDevices;
import thaumcraft.common.registers.ModGear;
import thaumcraft.common.registers.ModItems;
import thaumcraft.common.registers.ModMobs;

/** Milestone 13: loot bags and crates, the primordial pearl, meat nuggets, the cult and its portals. */
@GameTestHolder(Thaumcraft.MODID)
@PrefixGameTestTemplate(false)
public class MobGameTests {
   private static <T extends net.minecraft.world.entity.Mob> T spawn(GameTestHelper h, net.minecraft.world.entity.EntityType<T> type, BlockPos rel) {
      T e = type.create(h.getLevel());
      BlockPos abs = h.absolutePos(rel);
      e.moveTo(abs.getX() + 0.5, abs.getY(), abs.getZ() + 0.5, 0, 0);
      e.finalizeSpawn(h.getLevel(), h.getLevel().getCurrentDifficultyAt(abs), MobSpawnType.SPAWN_EGG, null);
      h.getLevel().addFreshEntity(e);
      return e;
   }

   private static List<ItemEntity> items(GameTestHelper h) {
      return h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(h.absolutePos(BlockPos.ZERO)).inflate(16));
   }

   /** Opening a loot bag scatters 8-12 rolls of loot and uses the bag up. */
   @GameTest(template = "empty")
   public static void lootBagOpensIntoLoot(GameTestHelper h) {
      ServerPlayer player = h.makeMockServerPlayerInLevel();
      player.moveTo(h.absoluteVec(new Vec3(2.5, 2, 2.5)));
      player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModMobs.LOOT_BAG_RARE.get(), 2));
      player.getMainHandItem().use(h.getLevel(), player, InteractionHand.MAIN_HAND);
      h.assertTrue(player.getMainHandItem().getCount() == 1, "the bag is used up");
      h.assertTrue(items(h).size() >= 8, "8-12 rolls of loot, got " + items(h).size());
      h.succeed();
   }

   /** Breaking a loot crate (no silk touch) gives 1-3 (+rarity) rolls of the loot bag table. */
   @GameTest(template = "empty")
   public static void lootCrateDropsLoot(GameTestHelper h) {
      BlockPos pos = new BlockPos(2, 2, 2);
      h.setBlock(pos, ModDevices.LOOT_URN_UNCOMMON.get());
      h.getLevel().destroyBlock(h.absolutePos(pos), true);
      List<ItemEntity> drops = items(h);
      h.assertTrue(drops.size() >= 2 && drops.size() <= 4, "2-4 rolls from an uncommon urn, got " + drops.size());
      h.assertTrue(drops.stream().noneMatch(i -> i.getItem().is(ModDevices.LOOT_URN_UNCOMMON.get().asItem())), "no silk touch, no urn");
      h.succeed();
   }

   private static List<ItemStack> roll(GameTestHelper h, String table) {
      ServerPlayer player = h.makeMockServerPlayerInLevel();
      LootTable t = h.getLevel().getServer().reloadableRegistries()
         .getLootTable(ResourceKey.create(Registries.LOOT_TABLE, ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, table)));
      return t.getRandomItems(new LootParams.Builder(h.getLevel()).withParameter(LootContextParams.ORIGIN, player.position())
         .withParameter(LootContextParams.THIS_ENTITY, player).create(LootContextParamSets.GIFT));
   }

   /** The rare bag's table holds primordial pearls (some worn down) and the gear roll swaps in armor and weapons. */
   @GameTest(template = "empty", timeoutTicks = 200)
   public static void rareLootHasPearlsAndGear(GameTestHelper h) {
      boolean pearl = false, gear = false;
      for (int i = 0; i < 3000 && !(pearl && gear); i++) {
         for (ItemStack s : roll(h, "loot_bag/rare")) {
            if (s.is(ModMobs.PRIMORDIAL_PEARL.get())) pearl = true;
            if (s.getItem() instanceof net.minecraft.world.item.ArmorItem || s.getItem() instanceof net.minecraft.world.item.TieredItem) gear = true;
         }
      }
      h.assertTrue(pearl, "a primordial pearl in 3000 rare rolls");
      h.assertTrue(gear, "a piece of gear in 3000 rare rolls");
      h.succeed();
   }

   /** The pearl is used up one step per craft: pearl, nodule, mote, then gone. */
   @GameTest(template = "empty")
   public static void primordialPearlWearsDown(GameTestHelper h) {
      ItemStack pearl = new ItemStack(ModMobs.PRIMORDIAL_PEARL.get());
      h.assertTrue(pearl.getDescriptionId().endsWith(".pearl"), "a fresh pearl");
      ItemStack next = pearl.getCraftingRemainingItem();
      h.assertTrue(next.is(ModMobs.PRIMORDIAL_PEARL.get()) && next.getDamageValue() == 1, "one step more worn");
      next.setDamageValue(4);
      h.assertTrue(next.getDescriptionId().endsWith(".nodule"), "a nodule at 4");
      next.setDamageValue(7);
      h.assertTrue(next.getDescriptionId().endsWith(".mote"), "a mote at 7");
      h.assertTrue(next.getCraftingRemainingItem().isEmpty(), "a mote is used up");
      h.succeed();
   }

   /** Three different meat nuggets and sugar make the treat; duplicates do not. */
   @GameTest(template = "empty")
   public static void tripleMeatTreatRecipe(GameTestHelper h) {
      TripleMeatTreatRecipe r = new TripleMeatTreatRecipe(CraftingBookCategory.MISC);
      ItemStack beef = new ItemStack(ModMobs.CHUNKS.get(0).get()), chicken = new ItemStack(ModMobs.CHUNKS.get(1).get());
      ItemStack pork = new ItemStack(ModMobs.CHUNKS.get(2).get()), sugar = new ItemStack(Items.SUGAR);
      h.assertTrue(r.matches(CraftingInput.of(2, 2, List.of(beef, chicken, pork, sugar)), h.getLevel()), "three nuggets and sugar");
      h.assertFalse(r.matches(CraftingInput.of(2, 2, List.of(beef, beef.copy(), pork, sugar)), h.getLevel()), "two of the same nugget");
      h.assertFalse(r.matches(CraftingInput.of(2, 2, List.of(beef, chicken, pork, ItemStack.EMPTY)), h.getLevel()), "no sugar");
      h.assertTrue(r.assemble(CraftingInput.of(2, 2, List.of(beef, chicken, pork, sugar)), h.getLevel().registryAccess())
         .is(ModMobs.TRIPLE_MEAT_TREAT.get()), "the treat");
      h.succeed();
   }

   /** The infernal furnace's bonus table now gives meat nuggets for raw meat. */
   @GameTest(template = "empty")
   public static void infernalFurnaceGivesMeatNuggets(GameTestHelper h) {
      boolean got = false;
      for (int i = 0; i < 300 && !got; i++) {
         got = SmeltingBonus.roll(new ItemStack(Items.BEEF), h.getLevel().random).stream().anyMatch(s -> s.is(ModMobs.CHUNKS.get(0).get()));
      }
      h.assertTrue(got, "a beef nugget");
      h.succeed();
   }

   /** Amber ore sometimes gives a preserved curiosity instead of amber. */
   @GameTest(template = "empty", timeoutTicks = 200)
   public static void amberOreDropsCuriosities(GameTestHelper h) {
      LootTable t = h.getLevel().getServer().reloadableRegistries()
         .getLootTable(ResourceKey.create(Registries.LOOT_TABLE, ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "blocks/ore_amber")));
      boolean curio = false, amber = false;
      for (int i = 0; i < 1000 && !(curio && amber); i++) {
         for (ItemStack s : t.getRandomItems(new LootParams.Builder(h.getLevel()).withParameter(LootContextParams.ORIGIN, h.absoluteVec(Vec3.ZERO))
            .withParameter(LootContextParams.BLOCK_STATE, ModBlocks.ORE_AMBER.get().defaultBlockState())
            .withParameter(LootContextParams.TOOL, new ItemStack(Items.IRON_PICKAXE)).create(LootContextParamSets.BLOCK))) {
            if (s.is(ModItems.CURIOS.get(1).get())) curio = true;
            if (s.is(ModItems.AMBER.get())) amber = true;
         }
      }
      h.assertTrue(curio && amber, "both amber and preserved curiosities");
      h.succeed();
   }

   /** Cultists come dressed: knights in crimson plate, clerics in robes; they are allies of each other. */
   @GameTest(template = "empty")
   public static void cultistsSpawnInUniform(GameTestHelper h) {
      CultistKnightEntity knight = spawn(h, ModMobs.CULTIST_KNIGHT.get(), new BlockPos(1, 2, 1));
      CultistClericEntity cleric = spawn(h, ModMobs.CULTIST_CLERIC.get(), new BlockPos(3, 2, 3));
      h.assertTrue(knight.getItemBySlot(EquipmentSlot.CHEST).is(ModGear.CRIMSON_PLATE_CHEST.get()), "knight plate");
      h.assertTrue(cleric.getItemBySlot(EquipmentSlot.CHEST).is(ModGear.CRIMSON_ROBE_CHEST.get()), "cleric robe");
      h.assertTrue(knight.isAlliedTo(cleric) && !knight.canAttackType(ModMobs.CULTIST_CLERIC.get()), "cultists are allies");
      h.succeed();
   }

   /** The praetor wears the praetor armor, has a title, and buffs nearby cultists with regeneration. */
   @GameTest(template = "empty", timeoutTicks = 100)
   public static void praetorLeadsTheCult(GameTestHelper h) {
      CultistLeaderEntity leader = spawn(h, ModMobs.CULTIST_LEADER.get(), new BlockPos(2, 2, 2));
      CultistKnightEntity knight = spawn(h, ModMobs.CULTIST_KNIGHT.get(), new BlockPos(3, 2, 3));
      h.assertTrue(leader.getItemBySlot(EquipmentSlot.HEAD).is(ModGear.CRIMSON_PRAETOR_HELM.get()), "praetor helm");
      h.assertTrue(leader.hasCustomName(), "a title");
      h.succeedWhen(() -> h.assertTrue(knight.hasEffect(net.minecraft.world.effect.MobEffects.REGENERATION), "the knight regenerates"));
   }

   /** A lesser portal wakes when a player comes close and summons cultists, hurting itself. */
   @GameTest(template = "trees", timeoutTicks = 600)
   public static void lesserPortalSummonsCultists(GameTestHelper h) {
      CultistPortalLesserEntity portal = spawn(h, ModMobs.CULTIST_PORTAL_LESSER.get(), new BlockPos(16, 2, 16));
      ServerPlayer player = h.makeMockServerPlayerInLevel();
      player.moveTo(h.absoluteVec(new Vec3(16.5, 2, 22.5)));
      h.succeedWhen(() -> {
         h.assertTrue(portal.isActive(), "the portal woke up");
         h.assertTrue(!h.getLevel().getEntitiesOfClass(CultistEntity.class, portal.getBoundingBox().inflate(32)).isEmpty(), "cultists summoned");
         h.assertTrue(portal.getHealth() < portal.getMaxHealth(), "summoning hurts the portal");
      });
   }

   /** The greater portal leaves a primordial pearl behind. */
   @GameTest(template = "empty")
   public static void greaterPortalDropsPearl(GameTestHelper h) {
      CultistPortalGreaterEntity portal = spawn(h, ModMobs.CULTIST_PORTAL_GREATER.get(), new BlockPos(2, 2, 2));
      portal.kill();
      h.assertTrue(h.getLevel().getEntitiesOfClass(SpecialItemEntity.class, new AABB(h.absolutePos(BlockPos.ZERO)).inflate(16))
         .stream().anyMatch(e -> e.getItem().is(ModMobs.PRIMORDIAL_PEARL.get())), "a primordial pearl");
      h.succeed();
   }

   /** Every creature of the milestone can be created and has its attributes. */
   @GameTest(template = "empty")
   public static void creaturesSpawn(GameTestHelper h) {
      for (var type : List.of(ModMobs.WISP.get(), ModMobs.FIREBAT.get(), ModMobs.PECH.get(), ModMobs.MIND_SPIDER.get(), ModMobs.BRAINY_ZOMBIE.get(),
         ModMobs.GIANT_BRAINY_ZOMBIE.get(), ModMobs.THAUMIC_SLIME.get())) {
         var e = spawn(h, type, new BlockPos(2, 2, 2));
         h.assertTrue(e.isAlive() && e.getMaxHealth() > 0, type.toShortString() + " spawned");
         e.discard();
      }
      h.succeed();
   }

   /** WorldGenMound: the barrow is built on solid ground, with loot crates/urns, a chest and a skeleton spawner inside. */
   @GameTest(template = "trees", timeoutTicks = 100)
   public static void moundGenerates(GameTestHelper h) {
      for (int x = 1; x <= 21; x++) {
         for (int z = 1; z <= 21; z++) h.setBlock(new BlockPos(x, 11, z), net.minecraft.world.level.block.Blocks.DIRT);
      }
      BlockPos base = new BlockPos(2, 2, 2);
      h.assertTrue(thaumcraft.common.world.mobs.MoundFeature.generate(h.getLevel(), h.getLevel().random, h.absolutePos(base)), "the mound was placed");
      var loot = h.getBlockState(base.offset(9, 1, 7)).getBlock();
      h.assertTrue(loot instanceof thaumcraft.common.blocks.devices.LootBlock, "a crate or urn, got " + loot);
      h.assertBlockPresent(net.minecraft.world.level.block.Blocks.SPAWNER, base.offset(4, 5, 4));
      h.assertTrue(h.getBlockState(base.offset(10, 1, 9)).getBlock() instanceof net.minecraft.world.level.block.ChestBlock, "the chest");
      h.succeed();
   }

   /** generateSurface's lesser portal stands on natural ground. */
   @GameTest(template = "empty")
   public static void cultistPortalFeatureSpawnsPortal(GameTestHelper h) {
      h.setBlock(new BlockPos(2, 1, 2), net.minecraft.world.level.block.Blocks.DIRT);
      h.assertTrue(thaumcraft.common.world.mobs.CultistPortalFeature.spawn(h.getLevel(), h.absolutePos(new BlockPos(2, 1, 2))), "spawned");
      h.assertEntityPresent(ModMobs.CULTIST_PORTAL_LESSER.get());
      h.succeed();
   }

   /** The magical forest and eerie biomes are registered. */
   @GameTest(template = "empty")
   public static void biomesRegistered(GameTestHelper h) {
      var reg = h.getLevel().registryAccess().registryOrThrow(Registries.BIOME);
      h.assertTrue(reg.containsKey(thaumcraft.common.world.MobWorldgen.MAGICAL_FOREST), "magical forest");
      h.assertTrue(reg.containsKey(thaumcraft.common.world.MobWorldgen.EERIE), "eerie");
      h.succeed();
   }

   /** A powered arcane bore with a pickaxe and vis digs out the stone in front of it. */
   @GameTest(template = "trees", timeoutTicks = 1200)
   public static void arcaneBoreDigs(GameTestHelper h) {
      for (int x = 10; x <= 22; x++) {
         for (int y = 2; y <= 10; y++) {
            for (int z = 4; z <= 14; z++) h.setBlock(new BlockPos(x, y, z), net.minecraft.world.level.block.Blocks.STONE);
         }
      }
      BlockPos at = new BlockPos(16, 2, 16); // the "trees" floor is at y=1
      h.setBlock(at.offset(1, -1, 0), net.minecraft.world.level.block.Blocks.REDSTONE_BLOCK);
      thaumcraft.common.world.aura.AuraHandler.getAuraChunk(h.getLevel(), h.absolutePos(at)).setVis(500);
      var bore = ModMobs.ARCANE_BORE.get().create(h.getLevel());
      bore.placeAt(h.absolutePos(at), h.getTestRotation().rotate(net.minecraft.core.Direction.NORTH));
      bore.setValidSpawn();
      bore.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.DIAMOND_PICKAXE));
      h.getLevel().addFreshEntity(bore);
      h.succeedWhen(() -> {
         int air = 0;
         for (int x = 10; x <= 22; x++) {
            for (int y = 2; y <= 10; y++) {
               for (int z = 4; z <= 14; z++) if (h.getBlockState(new BlockPos(x, y, z)).isAir()) air++;
            }
         }
         h.assertTrue(air >= 5, "the bore dug " + air + " blocks; active=" + bore.isActive() + " charge=" + bore.getCharge() + " valid=" + bore.validInventory() + " pos=" + bore.blockPosition() + " want=" + h.absolutePos(at) + " below=" + h.getLevel().getBlockState(bore.blockPosition().below()) + " facing=" + bore.getFacing() + " rot=" + h.getTestRotation());
      });
   }

   /** An automated crossbow loaded with arrows shoots at monsters. */
   @GameTest(template = "trees", timeoutTicks = 600)
   public static void turretShootsMonsters(GameTestHelper h) {
      var turret = ModMobs.TURRET_CROSSBOW.get().create(h.getLevel());
      turret.placeAt(h.absolutePos(new BlockPos(16, 2, 10)));
      turret.setValidSpawn();
      turret.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.ARROW, 64));
      h.getLevel().addFreshEntity(turret);
      var husk = h.spawnWithNoFreeWill(net.minecraft.world.entity.EntityType.HUSK, new BlockPos(16, 2, 20));
      h.succeedWhen(() -> h.assertTrue(husk.isDeadOrDying() || husk.getHealth() < husk.getMaxHealth(), "the husk was shot"));
   }
}
