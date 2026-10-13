package thaumcraft.common.crafting.dust;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.common.blocks.DeviceBlock;
import thaumcraft.common.lib.FXHelper;
import thaumcraft.common.lib.ServerTasks;
import thaumcraft.common.registers.ModBlocks;
import thaumcraft.common.registers.ModDevices;
import thaumcraft.common.research.ResearchManager;

/** The salis mundus multiblocks of the devices milestone: the infernal furnace and the thaumatorium. */
public final class DeviceDustTriggers {
   private DeviceDustTriggers() {
   }

   public static void register() {
      infernalFurnace();
      DustTrigger.register(new Thaumatorium());
   }

   /** 3x3x3 of nether bricks (corners/edges) and obsidian (faces) around lava, with iron bars in front. */
   private static void infernalFurnace() {
      DustTriggerMultiblock.Part nb = DustTriggerMultiblock.Part.of(s -> s.is(Blocks.NETHER_BRICKS),
         d -> ModDevices.PLACEHOLDER_BRICK.get().defaultBlockState());
      DustTriggerMultiblock.Part ob = DustTriggerMultiblock.Part.of(s -> s.is(Blocks.OBSIDIAN),
         d -> ModDevices.PLACEHOLDER_OBSIDIAN.get().defaultBlockState());
      DustTriggerMultiblock.Part ib = DustTriggerMultiblock.Part.air(s -> s.is(Blocks.IRON_BARS));
      DustTriggerMultiblock.Part la = DustTriggerMultiblock.Part.of(s -> s.is(Blocks.LAVA),
         d -> ModDevices.INFERNAL_FURNACE.get().defaultBlockState().setValue(DeviceBlock.HORIZONTAL_FACING, d.getOpposite()));
      DustTriggerMultiblock.Part[][][] bp = {
         {{nb, ob, nb}, {ob, null, ob}, {nb, ob, nb}},
         {{nb, ob, nb}, {ob, la, ob}, {nb, ib, nb}},
         {{nb, ob, nb}, {ob, ob, ob}, {nb, ob, nb}}};
      DustTrigger.register(new DustTriggerMultiblock("INFERNALFURNACE", bp, new ItemStack(ModDevices.INFERNAL_FURNACE_ITEM.get())));
   }

   /**
    * Two alchemical constructs stacked on a crucible become a thaumatorium, facing the player (the 1.12 blueprint's
    * setApplyPlayerFacing, which needs the player, hence a trigger of its own).
    */
   static final class Thaumatorium implements DustTrigger {
      private static boolean metal(BlockState s) {
         return s.is(ModDevices.METAL_ALCHEMICAL.get());
      }

      /** The top block of the structure the clicked block belongs to, or null. */
      @Nullable
      private static BlockPos top(Level level, BlockPos pos) {
         if (!metal(level.getBlockState(pos))) return null;
         if (metal(level.getBlockState(pos.below())) && level.getBlockState(pos.below(2)).is(ModBlocks.CRUCIBLE.get())) return pos;
         if (metal(level.getBlockState(pos.above())) && level.getBlockState(pos.below()).is(ModBlocks.CRUCIBLE.get())) return pos.above();
         return null;
      }

      @Nullable
      @Override
      public Placement getValidFace(Level level, Player player, BlockPos pos, Direction face) {
         if (!ResearchManager.knowledge(player).isResearchKnown("THAUMATORIUM")) return null;
         BlockPos top = top(level, pos);
         if (top == null) return null;
         return new Placement(0, top.getY() - pos.getY(), 0, player.getDirection().getOpposite());
      }

      @Override
      public List<BlockPos> sparkle(Level level, Player player, BlockPos pos, Placement placement) {
         BlockPos top = pos.above(placement.yOffset());
         return List.of(top, top.below(), top.below(2));
      }

      @Override
      public void execute(Level level, Player player, BlockPos pos, Placement placement, Direction side) {
         if (!(level instanceof ServerLevel sl) || !(player instanceof ServerPlayer sp)) return;
         BlockPos top = pos.above(placement.yOffset());
         Direction facing = placement.facing() != null ? placement.facing() : Direction.NORTH;
         swap(sl, sp, top, ModDevices.THAUMATORIUM_TOP.get().defaultBlockState().setValue(DeviceBlock.HORIZONTAL_FACING, facing));
         swap(sl, sp, top.below(), ModDevices.THAUMATORIUM.get().defaultBlockState().setValue(DeviceBlock.HORIZONTAL_FACING, facing));
      }

      private static void swap(ServerLevel level, ServerPlayer player, BlockPos p, BlockState target) {
         BlockState expected = level.getBlockState(p);
         ServerTasks.schedule(50, () -> {
            if (level.getBlockState(p) != expected || !level.mayInteract(player, p)) return;
            level.setBlockAndUpdate(p, target);
            FXHelper.bamf(level, p);
         });
      }
   }
}
