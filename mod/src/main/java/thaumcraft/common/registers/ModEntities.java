package thaumcraft.common.registers;

import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredRegister;
import thaumcraft.Thaumcraft;
import thaumcraft.common.entities.SpecialItemEntity;
import thaumcraft.common.entities.projectile.AlumentumEntity;

public final class ModEntities {
   public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, Thaumcraft.MODID);

   public static final Supplier<EntityType<SpecialItemEntity>> SPECIAL_ITEM = ENTITIES.register("special_item",
      () -> EntityType.Builder.<SpecialItemEntity>of(SpecialItemEntity::new, MobCategory.MISC).sized(0.25F, 0.25F)
         .clientTrackingRange(6).updateInterval(20).build("special_item"));

   public static final Supplier<EntityType<AlumentumEntity>> ALUMENTUM = ENTITIES.register("alumentum",
      () -> EntityType.Builder.<AlumentumEntity>of(AlumentumEntity::new, MobCategory.MISC).sized(0.25F, 0.25F)
         .clientTrackingRange(4).updateInterval(10).build("alumentum"));

   private ModEntities() {
   }
}
