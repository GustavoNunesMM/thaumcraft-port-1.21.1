package thaumcraft.common.registers;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeModificationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import thaumcraft.Thaumcraft;

/**
 * The champion attributes: ThaumcraftApiHelper.CHAMPION_MOD ("tc.mobmod", synced to clients for the champion effects)
 * and ChampionModTainted.TAINTED_MOD. Every living entity type gets them (EntityEvents.entityConstuct gave them to all
 * creatures); types without an attack damage attribute get one at 0, so a tainted critter can bite.
 */
public final class ModChampions {
   public static final DeferredRegister<Attribute> ATTRIBUTES = DeferredRegister.create(Registries.ATTRIBUTE, Thaumcraft.MODID);

   public static final DeferredHolder<Attribute, Attribute> CHAMPION_MOD = ATTRIBUTES.register("champion_mod",
      () -> new RangedAttribute("attribute.name.thaumcraft.champion_mod", -2.0, -2.0, 100.0).setSyncable(true));
   public static final DeferredHolder<Attribute, Attribute> TAINTED_MOD = ATTRIBUTES.register("tainted_mod",
      () -> new RangedAttribute("attribute.name.thaumcraft.tainted_mod", 0.0, 0.0, 1.0));

   private ModChampions() {
   }

   public static void init(IEventBus modBus) {
      ATTRIBUTES.register(modBus);
      modBus.addListener(ModChampions::attributes);
   }

   private static void attributes(EntityAttributeModificationEvent event) {
      for (EntityType<? extends LivingEntity> type : event.getTypes()) {
         if (!event.has(type, CHAMPION_MOD)) event.add(type, CHAMPION_MOD);
         if (!event.has(type, TAINTED_MOD)) event.add(type, TAINTED_MOD);
         if (!event.has(type, Attributes.ATTACK_DAMAGE)) event.add(type, Attributes.ATTACK_DAMAGE, 0.0);
      }
   }
}
