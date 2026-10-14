package thaumcraft.common.blocks.devices;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import thaumcraft.Thaumcraft;

/**
 * STUB: things owned by other milestones that are not ported yet (the Flux/Taint milestone owns the purifying fluid
 * and the bath salts). They are looked up by registry id, so they start working once registered.
 */
public final class DeviceStubs {
   private static final ResourceLocation PURIFYING_FLUID = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "purifying_fluid");
   private static final ResourceLocation BATH_SALTS = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "bath_salts");

   private DeviceStubs() {
   }

   /** The purifying fluid, or EMPTY while it is not registered. */
   public static Fluid purifyingFluid() {
      return BuiltInRegistries.FLUID.containsKey(PURIFYING_FLUID) ? BuiltInRegistries.FLUID.get(PURIFYING_FLUID) : Fluids.EMPTY;
   }

   public static boolean isBathSalts(ItemStack stack) {
      if (stack.isEmpty() || !BuiltInRegistries.ITEM.containsKey(BATH_SALTS)) return false;
      Item salts = BuiltInRegistries.ITEM.get(BATH_SALTS);
      return salts != Items.AIR && stack.is(salts);
   }
}
