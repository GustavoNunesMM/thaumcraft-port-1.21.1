package thaumcraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import thaumcraft.Thaumcraft;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.crafting.CrucibleRecipe;
import thaumcraft.common.aspects.AspectManager;
import thaumcraft.common.blocks.crafting.CrucibleBlockEntity;
import thaumcraft.common.crafting.dust.DustTrigger;
import thaumcraft.common.entities.SpecialItemEntity;
import thaumcraft.common.registers.ModBlocks;
import thaumcraft.common.registers.ModEntities;
import thaumcraft.common.registers.ModItems;
import thaumcraft.common.research.ResearchManager;
import thaumcraft.common.world.aura.AuraHandler;

/** Milestone 6: crucible and alchemy. */
@GameTestHolder(Thaumcraft.MODID)
@PrefixGameTestTemplate(false)
public class AlchemyGameTests {
   private static final BlockPos REL = new BlockPos(2, 2, 2);

   private static CrucibleBlockEntity crucible(GameTestHelper h, boolean boiling) {
      h.setBlock(REL, ModBlocks.CRUCIBLE.get());
      CrucibleBlockEntity be = (CrucibleBlockEntity) h.getLevel().getBlockEntity(h.absolutePos(REL));
      be.tank.fill(new FluidStack(Fluids.WATER, 1000), net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
      if (boiling) be.heat = 200;
      return be;
   }

   @GameTest(template = "empty", timeoutTicks = 120)
   public static void dustedCauldronBecomesCrucible(GameTestHelper h) {
      ServerPlayer player = h.makeMockServerPlayerInLevel();
      ResearchManager.giveRecursiveResearch(player, "UNLOCKALCHEMY");
      h.setBlock(REL, Blocks.CAULDRON);
      BlockPos pos = h.absolutePos(REL);
      DustTrigger t = DustTrigger.TRIGGERS.stream().filter(x -> x.getValidFace(h.getLevel(), player, pos, Direction.UP) != null)
         .findFirst().orElseThrow();
      t.execute(h.getLevel(), player, pos, t.getValidFace(h.getLevel(), player, pos, Direction.UP), Direction.UP);
      h.runAfterDelay(60, () -> {
         h.assertBlockPresent(ModBlocks.CRUCIBLE.get(), REL);
         h.succeed();
      });
   }

   @GameTest(template = "empty", timeoutTicks = 400)
   public static void heatsOverLavaAndDissolvesItems(GameTestHelper h) {
      h.setBlock(REL.below(), Blocks.LAVA);
      CrucibleBlockEntity be = crucible(h, false);
      h.runAfterDelay(160, () -> {
         h.assertTrue(be.heat > 150, "the crucible should boil over lava, heat " + be.heat);
         ItemEntity item = new ItemEntity(h.getLevel(), h.absolutePos(REL).getX() + 0.5, h.absolutePos(REL).getY() + 0.5,
            h.absolutePos(REL).getZ() + 0.5, new ItemStack(Items.COBBLESTONE));
         item.setDeltaMovement(0, 0, 0);
         h.getLevel().addFreshEntity(item);
         h.succeedWhen(() -> {
            h.assertTrue(be.aspects.getAmount(Aspect.EARTH) > 0, "the cobblestone should dissolve: " + be.aspects);
            h.assertTrue(item.isRemoved(), "the item is used up");
         });
      });
   }

   @GameTest(template = "empty", timeoutTicks = 300)
   public static void nitorIsAHeatSource(GameTestHelper h) {
      h.setBlock(REL.below(), ModBlocks.NITOR.get(DyeColor.YELLOW).get());
      CrucibleBlockEntity be = crucible(h, false);
      h.runAfterDelay(160, () -> {
         h.assertTrue(be.heat > 150, "nitor should heat the crucible, heat " + be.heat);
         h.succeed();
      });
   }

   @GameTest(template = "empty")
   public static void makesNitor(GameTestHelper h) {
      ServerPlayer player = h.makeMockServerPlayerInLevel();
      CrucibleBlockEntity be = crucible(h, true);
      be.aspects = new AspectList().add(Aspect.ENERGY, 12).add(Aspect.FIRE, 10).add(Aspect.LIGHT, 10).add(Aspect.WATER, 3);
      be.attemptSmelt(new ItemStack(Items.GLOWSTONE_DUST), player);
      h.assertTrue(be.aspects.getAmount(Aspect.LIGHT) > 10 && h.getEntities(ModEntities.SPECIAL_ITEM.get()).isEmpty(),
         "without the research the dust only dissolves: " + be.aspects);
      be.aspects = new AspectList().add(Aspect.ENERGY, 12).add(Aspect.FIRE, 10).add(Aspect.LIGHT, 10).add(Aspect.WATER, 3);
      ResearchManager.giveRecursiveResearch(player, "UNLOCKALCHEMY");
      ItemStack left = be.attemptSmelt(new ItemStack(Items.GLOWSTONE_DUST), player);
      h.assertTrue(left.isEmpty(), "the catalyst is used");
      h.assertTrue(be.aspects.getAmount(Aspect.ENERGY) == 2 && be.aspects.getAmount(Aspect.FIRE) == 0 && be.aspects.getAmount(Aspect.WATER) == 3,
         "the recipe's essentia is used, the rest stays: " + be.aspects);
      h.assertTrue(be.tank.getFluidAmount() == 950, "50 mB of water used");
      var found = h.getEntities(ModEntities.SPECIAL_ITEM.get());
      h.assertTrue(found.stream().anyMatch(e -> e.getItem().is(ModBlocks.NITOR.get(DyeColor.YELLOW).get().asItem())), "a nitor pops out: " + found);
      h.succeed();
   }

   @GameTest(template = "empty")
   public static void spillingMakesFlux(GameTestHelper h) {
      CrucibleBlockEntity be = crucible(h, true);
      be.aspects = new AspectList().add(Aspect.FIRE, 40);
      float before = AuraHandler.getFlux(h.getLevel(), h.absolutePos(REL));
      be.spillRemnants();
      float after = AuraHandler.getFlux(h.getLevel(), h.absolutePos(REL));
      h.assertTrue(be.aspects.isEmpty() && be.tank.isEmpty(), "spilled");
      h.assertTrue(Math.abs(after - before - 10.0F) < 0.01F, "40 essentia spill 10 flux: " + (after - before));
      h.succeed();
   }

   @GameTest(template = "empty")
   public static void ingotAspectsComeFromCrucibleRecipes(GameTestHelper h) {
      AspectList thaumium = AspectManager.SERVER.getItemAspects(ModItems.INGOT_THAUMIUM.get());
      h.assertTrue(thaumium != null && thaumium.getAmount(Aspect.MAGIC) > 0 && thaumium.getAmount(Aspect.METAL) > 0,
         "thaumium = iron + sqrt of the recipe's essentia: " + thaumium);
      var hedge = h.getLevel().getRecipeManager().byKey(ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "hedge_gunpowder"));
      h.assertTrue(hedge.isPresent(), "hedge alchemy recipe loaded");
      AspectList need = ((CrucibleRecipe) hedge.get().value()).getAspects(h.getLevel());
      h.assertTrue(need.equals(AspectManager.SERVER.getItemAspects(Items.GUNPOWDER)), "hedge alchemy costs the gunpowder's own aspects: " + need);
      h.succeed();
   }
}
