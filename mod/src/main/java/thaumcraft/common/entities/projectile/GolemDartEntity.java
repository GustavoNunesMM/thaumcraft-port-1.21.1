package thaumcraft.common.entities.projectile;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import thaumcraft.common.registers.ModMobs;

/** EntityGolemDart: a small arrow (fired by golems with a dart launcher; the golems belong to their own milestone). */
public class GolemDartEntity extends AbstractArrow {
   public GolemDartEntity(EntityType<? extends GolemDartEntity> type, Level level) {
      super(type, level);
   }

   public GolemDartEntity(Level level, double x, double y, double z) {
      super(ModMobs.GOLEM_DART.get(), x, y, z, level, new ItemStack(Items.ARROW), null);
   }

   public GolemDartEntity(Level level, LivingEntity shooter) {
      super(ModMobs.GOLEM_DART.get(), shooter, level, new ItemStack(Items.ARROW), null);
      this.pickup = Pickup.DISALLOWED;
   }

   @Override
   protected ItemStack getDefaultPickupItem() {
      return new ItemStack(Items.ARROW);
   }
}
