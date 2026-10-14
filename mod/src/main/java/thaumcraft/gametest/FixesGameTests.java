package thaumcraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import thaumcraft.Thaumcraft;
import thaumcraft.common.entities.monster.ThaumicSlimeEntity;
import thaumcraft.common.entities.monster.mods.ChampionModifier;
import thaumcraft.common.registers.ModDevices;
import thaumcraft.common.registers.ModExtras;
import thaumcraft.common.registers.ModFlux;
import thaumcraft.common.registers.ModItems;
import thaumcraft.common.registers.ModMobs;
import thaumcraft.common.research.PlayerKnowledge;
import thaumcraft.common.research.ResearchManager;
import thaumcraft.common.research.ScanningManager;

/** The gaps found by the post-milestone audit: scan triggers, missing recipes and groups, late items. */
@GameTestHolder(Thaumcraft.MODID)
@PrefixGameTestTemplate(false)
public class FixesGameTests {
   private static ResourceLocation tc(String path) {
      return ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, path);
   }

   @GameTest(template = "empty")
   public static void scanTriggersForItems(GameTestHelper h) {
      ServerPlayer player = h.makeMockServerPlayerInLevel();
      ScanningManager.scanTheThing(player, new ItemStack(ModItems.VOID_SEED.get()));
      ScanningManager.scanTheThing(player, new ItemStack(ModDevices.BRAIN.get()));
      ScanningManager.scanTheThing(player, new ItemStack(ModMobs.PRIMORDIAL_PEARL.get()));
      ScanningManager.scanTheThing(player, new ItemStack(ModMobs.PECH_WAND.get()));
      PlayerKnowledge k = ResearchManager.knowledge(player);
      for (String key : new String[]{"f_VOIDSEED", "f_BRAIN", "PRIMPEARL", "!Pechwand"}) {
         h.assertTrue(k.isResearchKnown(key), "scanning should teach " + key + ": " + k.getResearchList());
      }
      h.succeed();
   }

   @GameTest(template = "empty")
   public static void scanTriggersForCreatures(GameTestHelper h) {
      ServerPlayer player = h.makeMockServerPlayerInLevel();
      EntityType<?>[] types = {ModFlux.TAINT_CRAWLER.get(), ModFlux.TAINTACLE.get(), ModFlux.TAINT_SEED.get(), ModFlux.TAINT_SWARM.get(),
         ModFlux.FLUX_RIFT.get()};
      for (EntityType<?> t : types) {
         Entity e = t.create(h.getLevel());
         e.moveTo(h.absoluteVec(new net.minecraft.world.phys.Vec3(2, 2, 2)));
         ScanningManager.scanTheThing(player, e);
         e.discard();
      }
      PlayerKnowledge k = ResearchManager.knowledge(player);
      for (String key : new String[]{"!TaintCrawler", "!Taintacle", "!TaintSeed", "!TaintSwarm", "!FluxRift", "f_toomuchflux"}) {
         h.assertTrue(k.isResearchKnown(key), "scanning should teach " + key + ": " + k.getResearchList());
      }
      h.succeed();
   }

   @GameTest(template = "empty")
   public static void missingRecipesPresent(GameTestHelper h) {
      var rm = h.getLevel().getRecipeManager();
      String[] ids = {"nitordyered", "nitordyeblack", "metal_purification_copper", "arcaneeartoggle", "fleshtoblock", "blocktoflesh"};
      for (String id : ids) h.assertTrue(rm.byKey(tc(id)).isPresent(), "missing recipe " + id);
      String[][] groups = {{"brassplate", "thaumcraft:brass_stuff"}, {"brassingotstoblock", "thaumcraft:brass_stuff"},
         {"tallowcandle", "thaumcraft:tallowcandles"}, {"tallowcandlered", "thaumcraft:tallowcandles"}, {"nitordyeblue", "thaumcraft:nitorgroup"},
         {"thaumiumplate", "thaumcraft:thaumium_stuff"}};
      for (String[] g : groups) {
         RecipeHolder<?> r = rm.byKey(tc(g[0])).orElse(null);
         h.assertTrue(r != null && g[1].equals(r.value().getGroup()), g[0] + " should be in group " + g[1]);
      }
      h.succeed();
   }

   @GameTest(template = "empty")
   public static void lateItemsRegistered(GameTestHelper h) {
      for (String id : new String[]{"grapple_gun", "grapple_gun_tip", "grapple_gun_spool", "creative_flux_sponge", "activator_rail", "thaumatorium"}) {
         h.assertTrue(BuiltInRegistries.ITEM.containsKey(tc(id)), "item " + id + " missing");
      }
      h.assertTrue(h.getLevel().getRecipeManager().byKey(tc("grapplegun")).isPresent(), "grapple gun recipe should be enabled");
      h.assertTrue(h.getLevel().getRecipeManager().byKey(tc("activatorrail")).isPresent(), "activator rail recipe should be enabled");
      // OwnedConstructEntity looks the rail up by id and reads its POWERED state
      h.assertTrue(BuiltInRegistries.BLOCK.getKey(ModExtras.ACTIVATOR_RAIL.get()).equals(tc("activator_rail"))
         && ModExtras.ACTIVATOR_RAIL.get().defaultBlockState().hasProperty(BlockStateProperties.POWERED), "activator rail block");
      h.succeed();
   }

   @GameTest(template = "empty")
   public static void fluxGooSpawnsAndFeedsThaumicSlimes(GameTestHelper h) {
      BlockPos rel = new BlockPos(2, 2, 2);
      BlockPos pos = h.absolutePos(rel);
      var goo = ModFlux.FLUX_GOO.get().defaultBlockState().setValue(thaumcraft.common.blocks.world.taint.FluxGooBlock.LEVEL, 6);
      h.setBlock(rel, goo);
      // the 1-in-50 roll comes up: a size 2 thaumic slime rises from the deep goo
      net.minecraft.util.RandomSource zero = new net.minecraft.world.level.levelgen.LegacyRandomSource(0) {
         @Override
         public int nextInt(int bound) {
            return 0;
         }
      };
      h.getBlockState(rel).tick(h.getLevel(), pos, zero);
      var slimes = h.getLevel().getEntitiesOfClass(ThaumicSlimeEntity.class, new net.minecraft.world.phys.AABB(pos).inflate(2));
      h.assertTrue(slimes.size() == 1 && slimes.get(0).getSize() == 2, "goo should spawn one size 2 slime: " + slimes);
      h.assertTrue(h.getBlockState(rel).isAir(), "the goo is used up");
      ThaumicSlimeEntity slime = slimes.get(0);
      h.assertTrue(slime instanceof thaumcraft.api.entities.ITaintedMob, "thaumic slimes are tainted mobs");
      // a slime in goo deeper than its size grows by eating it
      slime.setSize(1, true);
      h.setBlock(rel, goo.setValue(thaumcraft.common.blocks.world.taint.FluxGooBlock.LEVEL, 5));
      for (int i = 0; i < 40 && h.getBlockState(rel).is(ModFlux.FLUX_GOO.get()); i++) h.getBlockState(rel).entityInside(h.getLevel(), pos, slime);
      h.assertTrue(slime.getSize() > 1, "slime should have grown, size " + slime.getSize());
      slime.discard();
      h.succeed();
   }

   @GameTest(template = "empty")
   public static void championCreationAndDrops(GameTestHelper h) {
      var zombie = h.spawn(net.minecraft.world.entity.EntityType.ZOMBIE, new BlockPos(2, 2, 2));
      h.assertTrue(ChampionModifier.getChampionValue(zombie) >= -1, "the spawn roll should have run");
      // force a fresh roll into a specific modifier
      var z2 = new net.minecraft.world.entity.monster.Zombie(h.getLevel());
      z2.moveTo(h.absoluteVec(new net.minecraft.world.phys.Vec3(3, 2, 3)));
      double hp = z2.getMaxHealth();
      ChampionModifier.makeChampion(z2, false, 11);
      h.assertTrue(ChampionModifier.getChampionValue(z2) == 11 && ChampionModifier.get(z2).name.equals("vampiric"), "vampiric champion");
      h.assertTrue(z2.getMaxHealth() == hp + 100.0, "champion health +100: " + z2.getMaxHealth());
      h.assertTrue(z2.hasCustomName(), "champions are named");
      ChampionModifier.makeChampion(z2, false, 3);
      h.assertTrue(ChampionModifier.getChampionValue(z2) == 11, "a champion is never re-rolled");
      h.getLevel().addFreshEntity(z2);
      h.assertTrue(ChampionModifier.getChampionValue(z2) == 11, "joining the world keeps the modifier");
      // slain by a player: a loot bag
      ServerPlayer player = h.makeMockServerPlayerInLevel();
      z2.setLastHurtByPlayer(player);
      z2.hurt(h.getLevel().damageSources().playerAttack(player), 10000.0F);
      var bags = h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, z2.getBoundingBox().inflate(3),
         e -> e.getItem().getItem() instanceof thaumcraft.common.items.mobs.LootBagItem);
      h.assertTrue(!bags.isEmpty(), "a slain champion drops a loot bag");
      bags.forEach(Entity::discard);
      zombie.discard();
      h.succeed();
   }

   @GameTest(template = "empty")
   public static void taintedChampions(GameTestHelper h) {
      var cow = h.spawn(net.minecraft.world.entity.EntityType.COW, new BlockPos(2, 2, 2));
      ChampionModifier.makeTainted(cow);
      h.assertTrue(ChampionModifier.getChampionValue(cow) == ChampionModifier.TAINTED, "tainted champion: " + ChampionModifier.getChampionValue(cow));
      h.assertTrue(cow.getMaxHealth() == 35.0F, "tainted health buff: " + cow.getMaxHealth());
      // its AI turns hostile on the next tick (type 0 modifier)
      h.runAfterDelay(2, () -> {
         boolean melee = cow.goalSelector.getAvailableGoals().stream().anyMatch(g -> g.getGoal() instanceof net.minecraft.world.entity.ai.goal.MeleeAttackGoal);
         h.assertTrue(melee, "a tainted cow attacks");
         h.assertTrue(cow.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE) > 0, "and bites");
         cow.discard();
         h.succeed();
      });
   }
}
