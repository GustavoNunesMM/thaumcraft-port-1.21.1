package thaumcraft.common.entities.monster.eldritch;

import javax.annotation.Nullable;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import thaumcraft.Thaumcraft;
import thaumcraft.api.entities.IEldritchMob;
import thaumcraft.common.entities.SpecialItemEntity;

/** Small shared helpers of the eldritch milestone. */
public final class EldritchHelper {
   /** Entity types that count as IEldritchMob without implementing it (the mind spider of the mobs milestone). */
   public static final TagKey<EntityType<?>> ELDRITCH = TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "eldritch"));

   private EldritchHelper() {
   }

   public static boolean isEldritch(@Nullable Entity e) {
      return e instanceof IEldritchMob || e != null && e.getType().is(ELDRITCH);
   }

   /** An entity type of another milestone, looked up by id (null while that milestone isn't merged). */
   @Nullable
   public static EntityType<?> entityType(String path) {
      ResourceLocation id = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, path);
      return BuiltInRegistries.ENTITY_TYPE.containsKey(id) ? BuiltInRegistries.ENTITY_TYPE.get(id) : null;
   }

   /** An item of another milestone, looked up by id (empty while that milestone isn't merged). */
   public static ItemStack item(String path) {
      ResourceLocation id = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, path);
      Item item = BuiltInRegistries.ITEM.containsKey(id) ? BuiltInRegistries.ITEM.get(id) : null;
      return item == null ? ItemStack.EMPTY : new ItemStack(item);
   }

   /** EntityUtils.entityDropSpecialItem: a floating, unburnable item (the primordial pearl) dropped at a height. */
   public static void dropSpecialItem(LivingEntity entity, ItemStack stack, float dropHeight) {
      if (stack.isEmpty() || entity.level().isClientSide) return;
      SpecialItemEntity e = new SpecialItemEntity(entity.level(), entity.getX(), entity.getY() + dropHeight, entity.getZ(), stack);
      e.setDefaultPickUpDelay();
      e.setDeltaMovement(0.0, 0.1, 0.0);
      entity.level().addFreshEntity(e);
   }
}
