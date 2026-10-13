package thaumcraft.api.casters;

import javax.annotation.Nullable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Caster's gauntlets: they draw vis from the aura and cast the focus fitted to them. */
public interface ICaster {
   float getConsumptionModifier(ItemStack caster, Player player, boolean crafting);

   boolean consumeVis(ItemStack caster, Player player, float amount, boolean crafting, boolean simulate);

   /** The focus item fitted, or null. */
   @Nullable
   default Item getFocus(ItemStack caster) {
      ItemStack f = getFocusStack(caster);
      return f == null || f.isEmpty() ? null : f.getItem();
   }

   /** The fitted focus stack (a copy), or null if there is none. */
   @Nullable
   default ItemStack getFocusStack(ItemStack caster) {
      return null;
   }

   /** Fits a focus (an empty stack or null removes it). */
   default void setFocus(ItemStack caster, @Nullable ItemStack focus) {
   }

   /** The block an exchange focus will place (null if the focus has no block picker). */
   @Nullable
   default ItemStack getPickedBlock(ItemStack caster) {
      return null;
   }
}
