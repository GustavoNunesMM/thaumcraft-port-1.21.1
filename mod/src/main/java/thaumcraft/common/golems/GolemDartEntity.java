package thaumcraft.common.golems;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import thaumcraft.common.registers.ModGolems;

/** EntityGolemDart: the darts of the dart launcher arms (an arrow that can't be picked up). */
public class GolemDartEntity extends AbstractArrow {
   public GolemDartEntity(EntityType<? extends GolemDartEntity> type, Level level) {
      super(type, level);
   }

   public GolemDartEntity(Level level, LivingEntity shooter) {
      super(ModGolems.GOLEM_DART.get(), shooter, level, new ItemStack(Items.ARROW), null);
      this.pickup = Pickup.DISALLOWED;
   }

   @Override
   protected ItemStack getDefaultPickupItem() {
      return new ItemStack(Items.ARROW);
   }
}
