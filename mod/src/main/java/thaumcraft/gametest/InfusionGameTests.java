package thaumcraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import thaumcraft.Thaumcraft;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.blocks.essentia.JarBlockEntity;
import thaumcraft.common.blocks.infusion.InfusionMatrixBlockEntity;
import thaumcraft.common.blocks.infusion.InlayBlock;
import thaumcraft.common.blocks.infusion.PedestalBlockEntity;
import thaumcraft.common.crafting.dust.DustTrigger;
import thaumcraft.common.items.VisCrystalItem;
import thaumcraft.common.lib.enchantment.EnumInfusionEnchantment;
import thaumcraft.common.registers.ModBlocks;
import thaumcraft.common.registers.ModItems;
import thaumcraft.common.research.ResearchManager;

/** Milestone 8: the infusion altar. */
@GameTestHolder(Thaumcraft.MODID)
@PrefixGameTestTemplate(false)
public class InfusionGameTests {
   static final BlockPos MATRIX = new BlockPos(16, 4, 16);

   /** Arcane stone columns, a pedestal under the matrix, then salis mundus. */
   static void buildAltar(GameTestHelper h, ServerPlayer player) {
      for (int x = 10; x <= 22; x++) {
         for (int z = 10; z <= 22; z++) h.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
      }
      for (int dx : new int[]{-1, 1}) {
         for (int dz : new int[]{-1, 1}) {
            h.setBlock(MATRIX.offset(dx, -2, dz), ModBlocks.STONE_ARCANE.get());
            h.setBlock(MATRIX.offset(dx, -1, dz), ModBlocks.STONE_ARCANE.get());
         }
      }
      h.setBlock(MATRIX.below(2), ModBlocks.PEDESTAL_ARCANE.get());
      h.setBlock(MATRIX, ModBlocks.INFUSION_MATRIX.get());
      BlockPos pos = h.absolutePos(MATRIX.offset(1, -1, 1));
      DustTrigger t = DustTrigger.TRIGGERS.stream().filter(x -> x.getValidFace(h.getLevel(), player, pos, Direction.UP) != null).findFirst().orElseThrow();
      t.execute(h.getLevel(), player, pos, t.getValidFace(h.getLevel(), player, pos, Direction.UP), Direction.UP);
   }

   @GameTest(template = "trees", skyAccess = true, timeoutTicks = 200)
   public static void salisMundusMakesTheAltar(GameTestHelper h) {
      ServerPlayer player = h.makeMockServerPlayerInLevel();
      ResearchManager.giveRecursiveResearch(player, "INFUSION");
      buildAltar(h, player);
      h.runAfterDelay(70, () -> {
         for (int dx : new int[]{-1, 1}) {
            for (int dz : new int[]{-1, 1}) {
               h.assertBlockPresent(ModBlocks.PILLAR_ARCANE.get(), MATRIX.offset(dx, -2, dz));
               h.assertBlockPresent(Blocks.AIR, MATRIX.offset(dx, -1, dz));
            }
         }
         var m = (InfusionMatrixBlockEntity) h.getLevel().getBlockEntity(h.absolutePos(MATRIX));
         h.assertTrue(m.validLocation(), "the altar is complete");
         h.succeed();
      });
   }

   /** A whole infusion: aer crystal + wheat seeds + salis mundus and essentia from jars make an aer crystal cluster. */
   @GameTest(template = "trees", skyAccess = true, timeoutTicks = 1600)
   public static void infusionCraftsTheResult(GameTestHelper h) {
      ServerPlayer player = h.makeMockServerPlayerInLevel();
      ResearchManager.giveRecursiveResearch(player, "INFUSION");
      ResearchManager.giveRecursiveResearch(player, "CRYSTALFARMER");
      buildAltar(h, player);
      h.runAfterDelay(70, () -> {
         BlockPos center = MATRIX.below(2);
         ((PedestalBlockEntity) h.getLevel().getBlockEntity(h.absolutePos(center))).setItem(0, VisCrystalItem.of(Aspect.AIR, 1));
         BlockPos p1 = center.offset(3, 0, 0), p2 = center.offset(-3, 0, 0);
         h.setBlock(p1, ModBlocks.PEDESTAL_ARCANE.get());
         h.setBlock(p2, ModBlocks.PEDESTAL_ARCANE.get());
         ((PedestalBlockEntity) h.getLevel().getBlockEntity(h.absolutePos(p1))).setItem(0, new ItemStack(Items.WHEAT_SEEDS));
         ((PedestalBlockEntity) h.getLevel().getBlockEntity(h.absolutePos(p2))).setItem(0, new ItemStack(ModItems.SALIS_MUNDUS.get()));
         Aspect[] needs = {Aspect.AIR, Aspect.CRYSTAL, Aspect.TRAP};
         for (int i = 0; i < needs.length; i++) {
            BlockPos jp = center.offset(-1 + i, 0, 4);
            h.setBlock(jp, ModBlocks.JAR_NORMAL.get());
            ((JarBlockEntity) h.getLevel().getBlockEntity(h.absolutePos(jp))).addToContainer(needs[i], 50);
         }
         var m = (InfusionMatrixBlockEntity) h.getLevel().getBlockEntity(h.absolutePos(MATRIX));
         BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(h.absolutePos(MATRIX)), Direction.UP, h.absolutePos(MATRIX), false);
         m.onCasterRightClick(h.getLevel(), ItemStack.EMPTY, player, h.absolutePos(MATRIX), Direction.UP, InteractionHand.MAIN_HAND, hit);
         h.assertTrue(m.active, "the caster activates the matrix");
         m.onCasterRightClick(h.getLevel(), ItemStack.EMPTY, player, h.absolutePos(MATRIX), Direction.UP, InteractionHand.MAIN_HAND, hit);
         h.assertTrue(m.crafting, "the second click starts the infusion");
         h.succeedWhen(() -> {
            ItemStack out = ((PedestalBlockEntity) h.getLevel().getBlockEntity(h.absolutePos(center))).getItem();
            h.assertTrue(out.is(ModBlocks.CRYSTAL_AER.get().asItem()), "infused into an air crystal cluster: " + out + " " + m.getAspects());
            h.assertTrue(((PedestalBlockEntity) h.getLevel().getBlockEntity(h.absolutePos(p1))).getItem().isEmpty(), "components used");
         });
      });
   }

   @GameTest(template = "trees", skyAccess = true, timeoutTicks = 200)
   public static void infusionEnchantmentRecipeMatches(GameTestHelper h) {
      ServerPlayer player = h.makeMockServerPlayerInLevel();
      ResearchManager.giveRecursiveResearch(player, "INFUSIONENCHANTMENT");
      ItemStack pick = new ItemStack(Items.IRON_PICKAXE);
      var recipe = InfusionMatrixBlockEntity.findMatchingInfusionRecipe(h.getLevel(),
         java.util.List.of(new ItemStack(Items.ENCHANTED_BOOK), new ItemStack(Items.MAP)), pick, player);
      h.assertTrue(recipe != null, "sounding matches a pickaxe");
      ItemStack out = recipe.getRecipeOutput(player, pick, java.util.List.of());
      h.assertTrue(EnumInfusionEnchantment.getInfusionEnchantmentLevel(out, EnumInfusionEnchantment.SOUNDING) == 1, "sounding I");
      h.assertTrue(InfusionMatrixBlockEntity.findMatchingInfusionRecipe(h.getLevel(),
         java.util.List.of(new ItemStack(Items.ENCHANTED_BOOK), new ItemStack(Items.MAP)), new ItemStack(Items.IRON_SWORD), player) == null,
         "no sounding on a sword");
      h.succeed();
   }

   @GameTest(template = "trees", skyAccess = true, timeoutTicks = 300)
   public static void stabilizerChargesInlayAndPedestal(GameTestHelper h) {
      for (int x = 10; x <= 14; x++) h.setBlock(new BlockPos(x, 0, 16), Blocks.STONE);
      h.setBlock(new BlockPos(10, 1, 16), ModBlocks.STABILIZER.get());
      h.setBlock(new BlockPos(11, 1, 16), ModBlocks.INLAY.get());
      h.setBlock(new BlockPos(12, 1, 16), ModBlocks.INLAY.get());
      h.setBlock(new BlockPos(13, 1, 16), ModBlocks.PEDESTAL_ARCANE.get());
      h.succeedWhen(() -> {
         int c = h.getBlockState(new BlockPos(13, 1, 16)).getValue(InlayBlock.CHARGE);
         h.assertTrue(c >= 3, "the pedestal gets charge through the inlay: " + c);
      });
   }
}
