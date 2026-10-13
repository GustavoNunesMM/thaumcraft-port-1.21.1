package thaumcraft.api.items;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Gear that adds warp while held (main hand), worn or equipped as a curio. */
public interface IWarpingGear {
   int getWarp(ItemStack stack, Player player);
}
