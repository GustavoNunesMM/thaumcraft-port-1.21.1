package thaumcraft.common.items.casters;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import thaumcraft.api.casters.ICaster;
import thaumcraft.api.casters.IInteractWithCaster;
import thaumcraft.common.world.aura.AuraHandler;

/**
 * ItemCaster: the caster's gauntlet. It draws vis from the aura (area 0: this chunk; 1: this chunk and the four 16
 * blocks away; 2: the 3x3 chunks around) and works devices like tubes. Foci come with the casters milestone.
 */
public class CasterItem extends Item implements ICaster {
   private final int area;

   public CasterItem(int area, Properties properties) {
      super(properties);
      this.area = area;
   }

   private float getAuraPool(Player player) {
      Level level = player.level();
      BlockPos pos = player.blockPosition();
      float tot;
      switch (area) {
         case 1 -> {
            tot = AuraHandler.getVis(level, pos);
            for (Direction face : Direction.Plane.HORIZONTAL) tot += AuraHandler.getVis(level, pos.relative(face, 16));
         }
         case 2 -> {
            tot = 0.0F;
            for (int xx = -1; xx <= 1; xx++) {
               for (int zz = -1; zz <= 1; zz++) tot += AuraHandler.getVis(level, pos.offset(xx * 16, 0, zz * 16));
            }
         }
         default -> tot = AuraHandler.getVis(level, pos);
      }
      return tot;
   }

   @Override
   public boolean consumeVis(ItemStack is, Player player, float amount, boolean crafting, boolean sim) {
      amount *= getConsumptionModifier(is, player, crafting);
      if (getAuraPool(player) < amount) return false;
      if (sim) return true;
      Level level = player.level();
      BlockPos pos = player.blockPosition();
      switch (area) {
         case 1 -> {
            float i = amount / 5.0F;
            while (amount > 0.0F) {
               i = Math.min(i, amount);
               amount -= AuraHandler.drainVis(level, pos, i, false);
               if (amount <= 0.0F) return true;
               i = Math.min(i, amount);
               for (Direction face : Direction.Plane.HORIZONTAL) {
                  amount -= AuraHandler.drainVis(level, pos.relative(face, 16), i, false);
                  if (amount <= 0.0F) return true;
               }
            }
         }
         case 2 -> {
            float i = amount / 9.0F;
            while (amount > 0.0F) {
               i = Math.min(i, amount);
               for (int xx = -1; xx <= 1; xx++) {
                  for (int zz = -1; zz <= 1; zz++) {
                     amount -= AuraHandler.drainVis(level, pos.offset(xx * 16, 0, zz * 16), i, false);
                     if (amount <= 0.0F) return true;
                  }
               }
            }
         }
         default -> amount -= AuraHandler.drainVis(level, pos, amount, false);
      }
      return amount <= 0.0F;
   }

   @Override
   public float getConsumptionModifier(ItemStack is, Player player, boolean crafting) {
      float mod = 1.0F;
      if (player != null) mod -= CasterManager.getTotalVisDiscount(player);
      return Math.max(mod, 0.1F);
   }

   /** Devices that react to a caster get the click before the block itself. */
   @Override
   public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext ctx) {
      Level level = ctx.getLevel();
      BlockPos pos = ctx.getClickedPos();
      Player player = ctx.getPlayer();
      if (player == null) return InteractionResult.PASS;
      BlockHitResult hit = new BlockHitResult(ctx.getClickLocation(), ctx.getClickedFace(), pos, ctx.isInside());
      if (level.getBlockState(pos).getBlock() instanceof IInteractWithCaster b
         && b.onCasterRightClick(level, stack, player, pos, ctx.getClickedFace(), ctx.getHand(), hit)) {
         return InteractionResult.sidedSuccess(level.isClientSide);
      }
      if (level.getBlockEntity(pos) instanceof IInteractWithCaster t && t.onCasterRightClick(level, stack, player, pos, ctx.getClickedFace(), ctx.getHand(), hit)) {
         return InteractionResult.sidedSuccess(level.isClientSide);
      }
      return InteractionResult.PASS;
   }
}
