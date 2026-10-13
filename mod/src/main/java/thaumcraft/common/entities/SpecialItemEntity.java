package thaumcraft.common.entities;

import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import thaumcraft.common.registers.ModEntities;

/** EntitySpecialItem: what the crucible spits out; floats up gently and ignores explosions (and the crucible). */
public class SpecialItemEntity extends ItemEntity {
   public SpecialItemEntity(EntityType<? extends ItemEntity> type, Level level) {
      super(type, level);
   }

   public SpecialItemEntity(Level level, double x, double y, double z, ItemStack stack) {
      this(ModEntities.SPECIAL_ITEM.get(), level);
      setPos(x, y, z);
      setItem(stack);
      setYRot((float) (Math.random() * 360.0));
      setDeltaMovement(Math.random() * 0.2F - 0.1F, 0.2F, Math.random() * 0.2F - 0.1F);
   }

   @Override
   public void tick() {
      if (tickCount > 1) {
         Vec3 m = getDeltaMovement();
         double my = m.y > 0.0 ? m.y * 0.9F : m.y;
         setDeltaMovement(m.x, my + 0.04F, m.z);
         super.tick();
      }
   }

   @Override
   public boolean hurt(DamageSource source, float amount) {
      return !source.is(DamageTypeTags.IS_EXPLOSION) && super.hurt(source, amount);
   }
}
