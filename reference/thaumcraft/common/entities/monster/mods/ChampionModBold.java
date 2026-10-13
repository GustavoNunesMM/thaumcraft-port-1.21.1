package thaumcraft.common.entities.monster.mods;

import net.minecraft.client.renderer.entity.RenderLivingBase;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.DamageSource;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import thaumcraft.client.fx.FXDispatcher;

public class ChampionModBold implements IChampionModifierEffect {
   @Override
   public float performEffect(EntityLivingBase boss, EntityLivingBase target, DamageSource source, float ammount) {
      return 0.0F;
   }

   @SideOnly(Side.CLIENT)
   @Override
   public void showFX(EntityLivingBase boss) {
      if (!boss.world.rand.nextBoolean()) {
         float w = boss.world.rand.nextFloat() * boss.width;
         float d = boss.world.rand.nextFloat() * boss.width;
         float h = boss.world.rand.nextFloat() * boss.height / 3.0F;
         FXDispatcher.INSTANCE
            .spark(
               boss.getEntityBoundingBox().minX + w,
               boss.getEntityBoundingBox().minY + h,
               boss.getEntityBoundingBox().minZ + d,
               0.2F,
               0.3F - boss.world.rand.nextFloat() * 0.1F,
               0.0F,
               0.8F + boss.world.rand.nextFloat() * 0.2F,
               1.0F
            );
      }
   }

   @Override
   public void preRender(EntityLivingBase boss, RenderLivingBase renderLivingBase) {
   }
}
