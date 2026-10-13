package thaumcraft.common.golems.seals;

import javax.annotation.Nullable;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import thaumcraft.api.golems.ISealDisplayer;
import thaumcraft.api.golems.seals.ISeal;

/**
 * ItemSealPlacer: one item per seal type (the metadata variants of thaumcraft:seals in 1.12, seal_blank being
 * meta 0). Used on a block face it places the seal there.
 */
public class SealPlacerItem extends Item implements ISealDisplayer {
   @Nullable
   private final String sealKey;

   public SealPlacerItem(@Nullable String sealKey, Properties props) {
      super(props);
      this.sealKey = sealKey;
   }

   @Nullable
   public String getSealKey() {
      return sealKey;
   }

   @Override
   public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext ctx) {
      Level level = ctx.getLevel();
      Player player = ctx.getPlayer();
      if (sealKey == null || player == null || player.isShiftKeyDown()) return InteractionResult.PASS;
      // The server decides; the client must not open the clicked block's own screen meanwhile.
      if (level.isClientSide) return InteractionResult.SUCCESS;
      if (!player.mayUseItemAt(ctx.getClickedPos(), ctx.getClickedFace(), stack)) return InteractionResult.FAIL;
      ISeal type = SealHandler.getSeal(sealKey);
      ISeal seal = type == null ? null : type.newInstance();
      if (seal != null && seal.canPlaceAt(level, ctx.getClickedPos(), ctx.getClickedFace())) {
         if (SealHandler.addSealEntity(level, ctx.getClickedPos(), ctx.getClickedFace(), seal, player) && !player.getAbilities().instabuild) {
            stack.shrink(1);
         }
         return InteractionResult.SUCCESS;
      }
      return InteractionResult.FAIL;
   }
}
