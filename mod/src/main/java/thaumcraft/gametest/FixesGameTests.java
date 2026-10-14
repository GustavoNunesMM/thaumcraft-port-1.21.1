package thaumcraft.gametest;

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
}
