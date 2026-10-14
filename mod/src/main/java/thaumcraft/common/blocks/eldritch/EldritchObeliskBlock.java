package thaumcraft.common.blocks.eldritch;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/**
 * Eldritch obelisk segments (tile.eldritch.obelisk_base / obelisk_body) and, with {@code capstone}, the capstone.
 * An obelisk is three segments under a capstone; it glows (obelisk_side_2 / obelisk_cap_2) once the altar it guards
 * has taken its eye. Unbreakable, like the rest of the eldritch architecture.
 */
public class EldritchObeliskBlock extends Block {
   public enum Part implements StringRepresentable {
      BOTTOM, MIDDLE, TOP;

      @Override
      public String getSerializedName() {
         return name().toLowerCase(java.util.Locale.ROOT);
      }
   }

   public static final EnumProperty<Part> PART = EnumProperty.create("part", Part.class);
   public static final BooleanProperty LIT = BooleanProperty.create("lit");
   private final boolean capstone;

   public EldritchObeliskBlock(Properties properties, boolean capstone) {
      super(properties);
      this.capstone = capstone;
      BlockState def = stateDefinition.any().setValue(LIT, false);
      if (!capstone) def = def.setValue(PART, Part.BOTTOM);
      registerDefaultState(def);
   }

   public EldritchObeliskBlock(Properties properties) {
      this(properties, false);
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
      builder.add(LIT);
      // Capstones have no part; the property list is built before the constructor's field is set, so both get it.
      builder.add(PART);
   }

   public boolean isCapstone() {
      return capstone;
   }

   @Override
   public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource r) {
      if (state.getValue(LIT) && r.nextInt(4) == 0) {
         thaumcraft.client.fx.EldritchFX.sparkle(pos.getX() + r.nextFloat(), pos.getY() + r.nextFloat(), pos.getZ() + r.nextFloat(), 0.5F, 0.0F, 0.8F);
      }
   }
}
