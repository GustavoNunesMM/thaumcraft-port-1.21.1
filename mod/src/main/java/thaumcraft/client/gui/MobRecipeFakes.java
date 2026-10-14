package thaumcraft.client.gui;

import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import thaumcraft.Thaumcraft;
import thaumcraft.common.registers.ModMobs;

/** ConfigRecipes' fake triple meat treat recipe (three meat nuggets and sugar), shown in the Thaumonomicon. */
@EventBusSubscriber(modid = Thaumcraft.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class MobRecipeFakes {
   private MobRecipeFakes() {
   }

   @SubscribeEvent
   public static void setup(FMLClientSetupEvent e) {
      RecipeCatalog.addFake(ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "triplemeattreatfake"),
         new RecipeCatalog.ShapelessDisplay(() -> new ItemStack(ModMobs.TRIPLE_MEAT_TREAT.get()), () -> List.of(
            new ItemStack(ModMobs.CHUNKS.get(0).get()), new ItemStack(ModMobs.CHUNKS.get(1).get()), new ItemStack(ModMobs.CHUNKS.get(2).get()),
            new ItemStack(Items.SUGAR))));
   }
}
