package thaumcraft.common.crafting.dust;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.EventHooks;
import thaumcraft.common.lib.FXHelper;
import thaumcraft.common.lib.ServerTasks;
import thaumcraft.common.research.ResearchManager;

/**
 * DustTriggerMultiblock: salis mundus on any block of a structure (blueprint layers from the top down, any of the
 * four horizontal rotations) swaps its parts into their targets, after a short delay.
 */
public class DustTriggerMultiblock implements DustTrigger {
   /**
    * A blueprint cell. source: what must be there (null = anything); target: what it becomes, given the structure's
    * facing (null = left alone, AIR = removed).
    */
   public record Part(@Nullable Predicate<BlockState> source, @Nullable Function<Direction, BlockState> target) {
      public static Part of(Predicate<BlockState> source, @Nullable Function<Direction, BlockState> target) {
         return new Part(source, target);
      }

      public static Part air(Predicate<BlockState> source) {
         return new Part(source, d -> Blocks.AIR.defaultBlockState());
      }
   }

   private final String research;
   private final Part[][][] blueprint;
   private final ItemStack craftResult;
   private final int ySize, xSize, zSize;

   public DustTriggerMultiblock(String research, Part[][][] blueprint, ItemStack craftResult) {
      this.research = research;
      this.blueprint = blueprint;
      this.craftResult = craftResult;
      this.ySize = blueprint.length;
      this.xSize = blueprint[0].length;
      this.zSize = blueprint[0][0].length;
   }

   /** Matrix.Rotate90DegRight applied (3 - horizontal index) times: the layer as seen when facing that way. */
   private static Part[][] rotate(Part[][] layer, Direction facing) {
      Part[][] m = layer;
      int times = (3 - facing.get2DDataValue() + 4) % 4;
      for (int t = 0; t < times; t++) {
         int rows = m.length, cols = m[0].length;
         Part[][] r = new Part[cols][rows];
         for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) r[j][rows - 1 - i] = m[i][j];
         }
         m = r;
      }
      return m;
   }

   @Nullable
   @Override
   public Placement getValidFace(Level level, Player player, BlockPos pos, Direction face) {
      if (research != null && !ResearchManager.knowledge(player).isResearchKnown(research)) return null;
      for (int yy = -ySize; yy <= 0; yy++) {
         for (int xx = -xSize; xx <= 0; xx++) {
            for (int zz = -zSize; zz <= 0; zz++) {
               Direction f = fit(level, pos.offset(xx, yy, zz));
               if (f != null) return new Placement(xx, yy, zz, f);
            }
         }
      }
      return null;
   }

   @Nullable
   private Direction fit(Level level, BlockPos pos) {
      outer:
      // 1.12 EnumFacing.HORIZONTALS order, so symmetric structures end up rotated (and their parts facing) as in the original.
      for (Direction face : new Direction[]{Direction.SOUTH, Direction.WEST, Direction.NORTH, Direction.EAST}) {
         for (int y = 0; y < ySize; y++) {
            Part[][] m = rotate(blueprint[y], face);
            for (int x = 0; x < m.length; x++) {
               for (int z = 0; z < m[x].length; z++) {
                  Part p = m[x][z];
                  if (p != null && p.source() != null && !p.source().test(level.getBlockState(pos.offset(x, -y + ySize - 1, z)))) continue outer;
               }
            }
         }
         return face;
      }
      return null;
   }

   @Override
   public List<BlockPos> sparkle(Level level, Player player, BlockPos pos, Placement placement) {
      BlockPos p2 = pos.offset(placement.xOffset(), placement.yOffset(), placement.zOffset());
      List<BlockPos> list = new ArrayList<>();
      for (int y = 0; y < ySize; y++) {
         Part[][] m = rotate(blueprint[y], placement.facing());
         for (int x = 0; x < m.length; x++) {
            for (int z = 0; z < m[x].length; z++) {
               if (m[x][z] != null && m[x][z].source() != null) list.add(p2.offset(x, -y + ySize - 1, z));
            }
         }
      }
      return list;
   }

   @Override
   public void execute(Level level, Player player, BlockPos pos, Placement placement, Direction side) {
      if (!(level instanceof ServerLevel sl) || !(player instanceof ServerPlayer sp)) return;
      EventHooks.firePlayerCraftingEvent(sp, craftResult.copy(), new SimpleContainer(1));
      BlockPos p2 = pos.offset(placement.xOffset(), placement.yOffset(), placement.zOffset());
      for (int y = 0; y < ySize; y++) {
         Part[][] m = rotate(blueprint[y], placement.facing());
         for (int x = 0; x < m.length; x++) {
            for (int z = 0; z < m[x].length; z++) {
               Part part = m[x][z];
               if (part == null || part.target() == null) continue;
               BlockPos p3 = p2.offset(x, -y + ySize - 1, z);
               BlockState expected = level.getBlockState(p3);
               BlockState target = part.target().apply(placement.facing());
               ServerTasks.schedule(50, () -> {
                  if (sl.getBlockState(p3) != expected || !sl.mayInteract(sp, p3)) return;
                  sl.setBlockAndUpdate(p3, target);
                  FXHelper.bamf(sl, p3);
               });
            }
         }
      }
   }
}
