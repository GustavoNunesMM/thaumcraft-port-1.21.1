package thaumcraft.common.entities.monster.mods;

import net.minecraft.client.renderer.entity.RenderLivingBase;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.DamageSource;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import thaumcraft.client.fx.FXDispatcher;

public class ChampionModSpined implements IChampionModifierEffect {
   @Override
   public float performEffect(EntityLivingBase boss, EntityLivingBase target, DamageSource source, float amount) {
      if (target != null && !source.damageType.equalsIgnoreCase("thorns")) {
         target.attackEntityFrom(DamageSource.causeThornsDamage(boss), 1 + boss.world.rand.nextInt(3));
         target.playSound(SoundEvents.ENCHANT_THORNS_HIT, 0.5F, 1.0F);
         return amount;
      } else {
         return amount;
      }
   }

   @SideOnly(Side.CLIENT)
   @Override
   public void showFX(EntityLivingBase boss) {
      if (!boss.world.rand.nextBoolean()) {
         float w = boss.world.rand.nextFloat() * boss.width;
         float d = boss.world.rand.nextFloat() * boss.width;
         float h = boss.world.rand.nextFloat() * boss.height;
         int p = 704 + boss.world.rand.nextInt(4) * 3;
         FXDispatcher.INSTANCE
            .drawGenericParticles(
               boss.getEntityBoundingBox().minX + w,
               boss.getEntityBoundingBox().minY + h,
               boss.getEntityBoundingBox().minZ + d,
               0.0,
               0.0,
               0.0,
               0.5F + boss.world.rand.nextFloat() * 0.2F,
               0.1F + boss.world.rand.nextFloat() * 0.2F,
               0.1F + boss.world.rand.nextFloat() * 0.2F,
               0.7F,
               false,
               p,
               3,
               1,
               3,
               0,
               1.2F + boss.world.rand.nextFloat() * 0.3F,
               0.0F,
               0
            );
      }
   }

   @Override
   public void preRender(EntityLivingBase boss, RenderLivingBase renderLivingBase) {
   }
}
