package thaumcraft.common.entities.monster.mods;

import net.minecraft.client.renderer.entity.RenderLivingBase;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.DamageSource;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;
import thaumcraft.client.fx.FXDispatcher;

public class ChampionModFire implements IChampionModifierEffect {
   @Override
   public float performEffect(EntityLivingBase boss, EntityLivingBase target, DamageSource source, float amount) {
      if (boss.world.rand.nextFloat() < 0.4F) {
         target.setFire(4);
      }

      return amount;
   }

   @SideOnly(Side.CLIENT)
   @Override
   public void showFX(EntityLivingBase boss) {
      float w = boss.world.rand.nextFloat() * boss.width;
      float d = boss.world.rand.nextFloat() * boss.width;
      float h = boss.world.rand.nextFloat() * boss.height;
      FXDispatcher.INSTANCE
         .drawGenericParticles(
            boss.getEntityBoundingBox().minX + w,
            boss.getEntityBoundingBox().minY + h,
            boss.getEntityBoundingBox().minZ + d,
            0.0,
            0.03,
            0.0,
            0.9F + boss.world.rand.nextFloat() * 0.1F,
            1.0F,
            1.0F,
            0.7F,
            false,
            640,
            10,
            1,
            8 + boss.world.rand.nextInt(4),
            0,
            0.7F + boss.world.rand.nextFloat() * 0.2F,
            0.0F,
            0
         );
   }

   @Override
   public void preRender(EntityLivingBase boss, RenderLivingBase renderLivingBase) {
      GL11.glColor4f(1.0F, 0.75F, 5.0F, 1.0F);
   }
}
