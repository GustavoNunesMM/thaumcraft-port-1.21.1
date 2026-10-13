package thaumcraft.common.crafting.dust;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.Tags;
import thaumcraft.common.registers.ModBlocks;
import thaumcraft.common.registers.ModItems;

/** The dust triggers of ConfigRecipes.initializeCompoundRecipes, added as their results get ported. */
public final class ModDustTriggers {
   private ModDustTriggers() {
   }

   public static void register() {
      DustTrigger.TRIGGERS.clear();
      DustTrigger.register(new DustTriggerSimple("!gotdream",
         s -> s.is(Blocks.BOOKSHELF) || s.is(Tags.Blocks.BOOKSHELVES), () -> new ItemStack(ModItems.THAUMONOMICON.get())));
      DustTrigger.register(new DustTriggerSimple("FIRSTSTEPS@1",
         s -> s.is(Blocks.CRAFTING_TABLE) || s.is(Tags.Blocks.PLAYER_WORKSTATIONS_CRAFTING_TABLES),
         () -> new ItemStack(ModBlocks.ARCANE_WORKBENCH.get())));
      DustTrigger.register(new DustTriggerSimple("UNLOCKALCHEMY@1", s -> s.is(net.minecraft.tags.BlockTags.CAULDRONS),
         () -> new ItemStack(ModBlocks.CRUCIBLE.get())));
      altar("INFUSION", ModBlocks.STONE_ARCANE.get(), ModBlocks.PILLAR_ARCANE.get(), ModBlocks.PEDESTAL_ARCANE.get());
      altar("INFUSIONANCIENT", ModBlocks.STONE_ANCIENT.get(), ModBlocks.PILLAR_ANCIENT.get(), ModBlocks.PEDESTAL_ANCIENT.get());
      altar("INFUSIONELDRITCH", ModBlocks.STONE_ELDRITCH_TILE.get(), ModBlocks.PILLAR_ELDRITCH.get(), ModBlocks.PEDESTAL_ELDRITCH.get());
   }

   /** The infusion altars: matrix on top, pedestal two below it, two-high stone columns at the corners that become pillars. */
   private static void altar(String research, net.minecraft.world.level.block.Block stone, net.minecraft.world.level.block.Block pillar,
                             net.minecraft.world.level.block.Block pedestal) {
      DustTriggerMultiblock.Part im = DustTriggerMultiblock.Part.of(s -> s.is(ModBlocks.INFUSION_MATRIX.get()), null);
      DustTriggerMultiblock.Part top = DustTriggerMultiblock.Part.air(s -> s.is(stone));
      DustTriggerMultiblock.Part pn = DustTriggerMultiblock.Part.of(s -> s.is(pedestal), null);
      DustTriggerMultiblock.Part[] bottom = new DustTriggerMultiblock.Part[4];
      net.minecraft.core.Direction[] facings = {net.minecraft.core.Direction.EAST, net.minecraft.core.Direction.NORTH,
         net.minecraft.core.Direction.SOUTH, net.minecraft.core.Direction.WEST};
      for (int i = 0; i < 4; i++) {
         var f = facings[i];
         bottom[i] = DustTriggerMultiblock.Part.of(s -> s.is(stone),
            d -> pillar.defaultBlockState().setValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING, f));
      }
      DustTriggerMultiblock.Part[][][] bp = {
         {{null, null, null}, {null, im, null}, {null, null, null}},
         {{top, null, top}, {null, null, null}, {top, null, top}},
         {{bottom[0], null, bottom[1]}, {null, pn, null}, {bottom[2], null, bottom[3]}}};
      DustTrigger.register(new DustTriggerMultiblock(research, bp, new ItemStack(ModBlocks.INFUSION_MATRIX.get())));
   }
}
