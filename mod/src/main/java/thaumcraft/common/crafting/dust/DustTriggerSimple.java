package thaumcraft.common.crafting.dust;

import java.util.function.Predicate;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.EventHooks;
import thaumcraft.common.lib.FXHelper;
import thaumcraft.common.lib.ServerTasks;
import thaumcraft.common.research.ResearchManager;

/**
 * DustTriggerSimple/DustTriggerOre: dusting a block (matched by block or tag) turns it, after a short delay, into the
 * result: a block (crafting table -> arcane workbench) or an item that pops out (bookshelf -> Thaumonomicon).
 */
public class DustTriggerSimple implements DustTrigger {
   private final String research;
   private final Predicate<BlockState> target;
   private final Supplier<ItemStack> result;

   public DustTriggerSimple(@Nullable String research, Predicate<BlockState> target, Supplier<ItemStack> result) {
      this.research = research;
      this.target = target;
      this.result = result;
   }

   @Nullable
   @Override
   public Placement getValidFace(Level level, Player player, BlockPos pos, Direction face) {
      if (!target.test(level.getBlockState(pos))) return null;
      if (research != null && !ResearchManager.knowledge(player).isResearchKnown(research)) return null;
      return new Placement(0, 0, 0, null);
   }

   @Override
   public void execute(Level level, Player player, BlockPos pos, Placement placement, Direction face) {
      if (!(level instanceof ServerLevel sl) || !(player instanceof ServerPlayer sp)) return;
      ItemStack out = result.get();
      EventHooks.firePlayerCraftingEvent(sp, out.copy(), new SimpleContainer(1));
      BlockState state = level.getBlockState(pos);
      ServerTasks.schedule(50, () -> swap(sl, sp, pos, state, out));
   }

   /** The ServerEvents swapper, for a single block: only if nothing changed meanwhile. */
   static void swap(ServerLevel level, ServerPlayer player, BlockPos pos, BlockState expected, ItemStack out) {
      if (level.getBlockState(pos) != expected || !level.mayInteract(player, pos)) return;
      if (out.getItem() instanceof BlockItem bi) {
         level.setBlockAndUpdate(pos, bi.getBlock().defaultBlockState());
      } else {
         level.removeBlock(pos, false);
         ItemEntity item = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.1, pos.getZ() + 0.5, out.copy(), 0, 0, 0);
         item.setNoGravity(true);
         level.addFreshEntity(item);
      }
      FXHelper.bamf(level, pos);
   }
}
