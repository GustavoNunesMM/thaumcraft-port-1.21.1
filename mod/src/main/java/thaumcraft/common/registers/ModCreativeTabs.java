package thaumcraft.common.registers;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import thaumcraft.Thaumcraft;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.items.VisCrystalItem;

public final class ModCreativeTabs {
   public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Thaumcraft.MODID);

   public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN = TABS.register("thaumcraft", () -> CreativeModeTab.builder()
      .title(Component.translatable("itemGroup.thaumcraft"))
      .icon(() -> ModItems.THAUMOMETER.get().getDefaultInstance())
      .displayItems((params, output) -> ModItems.ITEMS.getEntries().forEach(item -> {
         if (item.get() == ModItems.CRYSTAL_ESSENCE.get()) {
            // One vis crystal per aspect, as ItemCrystalEssence.getSubItems did.
            Aspect.all().forEach(a -> output.accept(VisCrystalItem.of(a, 1)));
         } else if (item.get() instanceof thaumcraft.common.items.ICreativeVariants v) {
            v.addCreativeStacks(output::accept);
         } else {
            output.accept(item.get());
         }
      }))
      .build());

   private ModCreativeTabs() {
   }
}
