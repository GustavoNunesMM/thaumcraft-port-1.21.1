package thaumcraft.api.casters;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Caster's gauntlets. Focus handling comes with the casters milestone. */
public interface ICaster {
   float getConsumptionModifier(ItemStack caster, Player player, boolean crafting);

   boolean consumeVis(ItemStack caster, Player player, float amount, boolean crafting, boolean simulate);
}
