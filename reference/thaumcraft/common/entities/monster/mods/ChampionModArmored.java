package thaumcraft.common.entities.monster.mods;

import net.minecraft.client.renderer.entity.RenderLivingBase;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.DamageSource;
import thaumcraft.client.fx.FXDispatcher;

public class ChampionModArmored implements IChampionModifierEffect {
   @Override
   public float performEffect(EntityLivingBase mob, EntityLivingBase target, DamageSource source, float amount) {
      if (!source.isUnblockable()) {
         float f1 = amount * 19.0F;
         amount = f1 / 25.0F;
      }

      return amount;
   }

   @Override
   public void showFX(EntityLivingBase boss) {
      if (boss.world.rand.nextInt(4) == 0) {
         float w = boss.world.rand.nextFloat() * boss.width;
         float d = boss.world.rand.nextFloat() * boss.width;
         float h = boss.world.rand.nextFloat() * boss.height;
         FXDispatcher.INSTANCE
            .drawGenericParticles(
               boss.getEntityBoundingBox().minX + w,
               boss.getEntityBoundingBox().minY + h,
               boss.getEntityBoundingBox().minZ + d,
               0.0,
               0.0,
               0.0,
               0.9F,
               0.9F,
               0.9F + boss.world.rand.nextFloat() * 0.1F,
               0.7F,
               false,
               448,
               9,
               1,
               5 + boss.world.rand.nextInt(4),
               0,
               0.6F + boss.world.rand.nextFloat() * 0.2F,
               0.0F,
               0
            );
      }
   }

   @Override
   public void preRender(EntityLivingBase boss, RenderLivingBase renderLivingBase) {
   }
}
