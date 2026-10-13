package thaumcraft;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import thaumcraft.common.config.ModConfig;
import thaumcraft.common.network.ModNetwork;
import thaumcraft.common.registers.ModAttachments;
import thaumcraft.common.registers.ModBlockEntities;
import thaumcraft.common.registers.ModMenus;
import thaumcraft.common.research.theorycraft.Cards;
import thaumcraft.common.registers.ModBlocks;
import thaumcraft.common.registers.ModCreativeTabs;
import thaumcraft.common.registers.ModDataComponents;
import thaumcraft.common.crafting.dust.ModDustTriggers;
import thaumcraft.common.registers.ModFeatures;
import thaumcraft.common.registers.ModRecipes;
import thaumcraft.common.registers.ModItems;
import thaumcraft.common.registers.ModSounds;

@Mod(Thaumcraft.MODID)
public class Thaumcraft {
   public static final String MODID = "thaumcraft";
   public static final Logger LOGGER = LogUtils.getLogger();

   public Thaumcraft(IEventBus modBus, ModContainer container) {
      thaumcraft.common.registers.ModCasters.init();
      thaumcraft.common.registers.ModGear.init(modBus);
      ModBlocks.BLOCKS.register(modBus);
      ModItems.ITEMS.register(modBus);
      ModDataComponents.COMPONENTS.register(modBus);
      ModFeatures.FEATURES.register(modBus);
      ModRecipes.SERIALIZERS.register(modBus);
      ModRecipes.TYPES.register(modBus);
      ModBlockEntities.BLOCK_ENTITIES.register(modBus);
      thaumcraft.common.registers.ModEntities.ENTITIES.register(modBus);
      modBus.addListener((net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent e) ->
         e.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK, ModBlockEntities.CRUCIBLE.get(),
            (be, side) -> be.getTank()));
      ModMenus.MENUS.register(modBus);
      ModSounds.SOUNDS.register(modBus);
      ModAttachments.ATTACHMENT_TYPES.register(modBus);
      ModCreativeTabs.TABS.register(modBus);
      modBus.addListener(ModNetwork::register);
      modBus.addListener((FMLCommonSetupEvent e) -> e.enqueueWork(() -> {
         ModBlocks.registerFlammables();
         ModDustTriggers.register();
         Cards.register();
      }));
      container.registerConfig(net.neoforged.fml.config.ModConfig.Type.COMMON, ModConfig.SPEC);
   }
}
