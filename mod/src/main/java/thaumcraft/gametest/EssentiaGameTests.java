package thaumcraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import thaumcraft.Thaumcraft;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.blocks.essentia.AlembicBlockEntity;
import thaumcraft.common.blocks.essentia.CentrifugeBlockEntity;
import thaumcraft.common.blocks.essentia.JarBlockEntity;
import thaumcraft.common.blocks.essentia.SmelterBlockEntity;
import thaumcraft.common.blocks.essentia.TubeBlock;
import thaumcraft.common.crafting.JarLabelRecipe;
import thaumcraft.common.items.PhialItem;
import thaumcraft.common.registers.ModBlocks;
import thaumcraft.common.registers.ModDataComponents;
import thaumcraft.common.registers.ModItems;

/** Milestone 7: essentia (jars, tubes, smeltery, alembic, centrifuge, labels). */
@GameTestHolder(Thaumcraft.MODID)
@PrefixGameTestTemplate(false)
public class EssentiaGameTests {
   private static JarBlockEntity jar(GameTestHelper h, BlockPos rel) {
      h.setBlock(rel, ModBlocks.JAR_NORMAL.get());
      return (JarBlockEntity) h.getLevel().getBlockEntity(h.absolutePos(rel));
   }

   /** Two jars joined by tubes on top of them: a labelled jar pulls the essentia out of an unlabelled one. */
   @GameTest(template = "empty", timeoutTicks = 600)
   public static void tubesMoveEssentiaToLabelledJar(GameTestHelper h) {
      JarBlockEntity from = jar(h, new BlockPos(1, 1, 2));
      JarBlockEntity to = jar(h, new BlockPos(3, 1, 2));
      from.addToContainer(Aspect.FIRE, 20);
      to.aspect = Aspect.FIRE;
      to.aspectFilter = Aspect.FIRE;
      for (int x = 1; x <= 3; x++) h.setBlock(new BlockPos(x, 2, 2), ModBlocks.TUBE.get());
      h.succeedWhen(() -> {
         h.assertTrue(h.getBlockState(new BlockPos(2, 2, 2)).getValue(net.minecraft.world.level.block.PipeBlock.EAST), "tubes connect");
         h.assertTrue(to.amount >= 5, "essentia should flow to the labelled jar: " + to.amount + " (left " + from.amount + ")");
      });
   }

   @GameTest(template = "empty", timeoutTicks = 100)
   public static void jarHoldsOneAspectUpTo250(GameTestHelper h) {
      JarBlockEntity j = jar(h, new BlockPos(2, 1, 2));
      h.assertTrue(j.addToContainer(Aspect.WATER, 300) == 50, "a jar holds 250");
      h.assertTrue(j.addToContainer(Aspect.FIRE, 5) == 5, "a jar holds a single aspect");
      h.assertTrue(j.takeFromContainer(Aspect.WATER, 250) && j.aspect == null, "emptying clears the aspect");
      h.setBlock(new BlockPos(3, 1, 2), ModBlocks.JAR_VOID.get());
      JarBlockEntity v = (JarBlockEntity) h.getLevel().getBlockEntity(h.absolutePos(new BlockPos(3, 1, 2)));
      h.assertTrue(v.addToContainer(Aspect.WATER, 300) == 0 && v.amount == 250, "a void jar destroys the excess");
      h.succeed();
   }

   /** Smeltery with fuel melts an item into essentia, which goes up into the alembic. */
   @GameTest(template = "empty", timeoutTicks = 1200)
   public static void smelteryFillsAlembic(GameTestHelper h) {
      BlockPos rel = new BlockPos(2, 1, 2);
      h.setBlock(rel, ModBlocks.SMELTER_BASIC.get());
      h.setBlock(rel.above(), ModBlocks.ALEMBIC.get());
      SmelterBlockEntity s = (SmelterBlockEntity) h.getLevel().getBlockEntity(h.absolutePos(rel));
      AlembicBlockEntity a = (AlembicBlockEntity) h.getLevel().getBlockEntity(h.absolutePos(rel.above()));
      s.setItem(0, new ItemStack(Items.COBBLESTONE, 4));
      s.setItem(1, new ItemStack(Items.COAL, 4));
      h.succeedWhen(() -> h.assertTrue(a.amount > 0 && a.aspect == Aspect.EARTH, "the alembic gets terra: " + a.amount + " " + s.aspects));
   }

   @GameTest(template = "empty", timeoutTicks = 400)
   public static void centrifugeSplitsCompounds(GameTestHelper h) {
      JarBlockEntity j = jar(h, new BlockPos(2, 1, 2));
      j.addToContainer(Aspect.LIFE, 10);
      h.setBlock(new BlockPos(2, 2, 2), ModBlocks.CENTRIFUGE.get());
      CentrifugeBlockEntity c = (CentrifugeBlockEntity) h.getLevel().getBlockEntity(h.absolutePos(new BlockPos(2, 2, 2)));
      h.succeedWhen(() -> h.assertTrue(c.aspectOut == Aspect.EARTH || c.aspectOut == Aspect.WATER,
         "victus splits into terra or aqua: " + c.aspectOut));
   }

   @GameTest(template = "empty")
   public static void labelRecipeTakesThePhialsAspect(GameTestHelper h) {
      JarLabelRecipe r = new JarLabelRecipe(net.minecraft.world.item.crafting.CraftingBookCategory.MISC);
      CraftingInput in = CraftingInput.of(2, 1, java.util.List.of(new ItemStack(ModItems.LABEL.get()), PhialItem.makeFilledPhial(Aspect.MIND)));
      h.assertTrue(r.matches(in, h.getLevel()), "label + phial");
      ItemStack out = r.assemble(in, h.getLevel().registryAccess());
      h.assertTrue(out.is(ModItems.LABEL_FILLED.get()) && out.get(ModDataComponents.ASPECT_FILTER.get()) == Aspect.MIND, "marked label");
      h.succeed();
   }

   @GameTest(template = "empty")
   public static void jarDropKeepsEssentia(GameTestHelper h) {
      JarBlockEntity j = jar(h, new BlockPos(2, 1, 2));
      j.addToContainer(Aspect.AIR, 42);
      j.aspectFilter = Aspect.AIR;
      var drops = net.minecraft.world.level.block.Block.getDrops(j.getBlockState(), h.getLevel(), j.getBlockPos(), j);
      h.assertTrue(drops.size() == 1, "one jar drops: " + drops);
      AspectList al = drops.get(0).get(ModDataComponents.ESSENTIA.get());
      h.assertTrue(al != null && al.getAmount(Aspect.AIR) == 42, "the dropped jar keeps its essentia: " + al);
      h.assertTrue(drops.get(0).get(ModDataComponents.ASPECT_FILTER.get()) == Aspect.AIR, "and its label");
      h.succeed();
   }

   @GameTest(template = "empty")
   public static void tubeConnectsOnlyToTransports(GameTestHelper h) {
      h.setBlock(new BlockPos(2, 1, 2), ModBlocks.TUBE.get());
      h.setBlock(new BlockPos(3, 1, 2), Blocks.STONE);
      h.setBlock(new BlockPos(1, 1, 2), ModBlocks.TUBE.get());
      var s = h.getBlockState(new BlockPos(2, 1, 2));
      h.assertTrue(s.getBlock() instanceof TubeBlock, "tube");
      h.assertTrue(s.getValue(net.minecraft.world.level.block.PipeBlock.WEST) && !s.getValue(net.minecraft.world.level.block.PipeBlock.EAST),
         "arm towards the other tube only: " + s);
      h.assertTrue(Direction.WEST.getStepX() < 0, "");
      h.succeed();
   }
}
