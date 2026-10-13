package thaumcraft.common.registers;

import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredRegister;
import thaumcraft.Thaumcraft;
import thaumcraft.common.menu.ArcaneWorkbenchMenu;
import thaumcraft.common.menu.ResearchTableMenu;

public final class ModMenus {
   public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, Thaumcraft.MODID);

   public static final Supplier<MenuType<ResearchTableMenu>> RESEARCH_TABLE = MENUS.register("research_table",
      () -> IMenuTypeExtension.create(ResearchTableMenu::new));

   public static final Supplier<MenuType<ArcaneWorkbenchMenu>> ARCANE_WORKBENCH = MENUS.register("arcane_workbench",
      () -> IMenuTypeExtension.create(ArcaneWorkbenchMenu::new));

   public static final Supplier<MenuType<thaumcraft.common.menu.SmelterMenu>> SMELTER = MENUS.register("smelter",
      () -> IMenuTypeExtension.create(thaumcraft.common.menu.SmelterMenu::new));

   private ModMenus() {
   }
}
