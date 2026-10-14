package thaumcraft.common.blocks.devices;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import thaumcraft.common.registers.ModFlux;

/** The flux milestone's content used by the devices (the spa): the purifying fluid and the bath salts. */
public final class DeviceStubs {
   private DeviceStubs() {
   }

   public static Fluid purifyingFluid() {
      return ModFlux.PURIFYING_FLUID.get();
   }

   public static boolean isBathSalts(ItemStack stack) {
      return stack.is(ModFlux.BATH_SALTS.get());
   }
}
