package thaumcraft.common.entities.monster.mods;

import net.minecraft.client.renderer.entity.RenderLivingBase;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.MobEffects;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;
import thaumcraft.client.fx.FXDispatcher;

public class ChampionModPoison implements IChampionModifierEffect {
   @Override
   public float performEffect(EntityLivingBase boss, EntityLivingBase target, DamageSource source, float amount) {
      if (boss.world.rand.nextFloat() < 0.4F) {
         target.addPotionEffect(new PotionEffect(MobEffects.POISON, 100));
      }

      return amount;
   }

   @SideOnly(Side.CLIENT)
   @Override
   public void showFX(EntityLivingBase boss) {
      if (!boss.world.rand.nextBoolean()) {
         float w = boss.world.rand.nextFloat() * boss.width;
         float d = boss.world.rand.nextFloat() * boss.width;
         float h = boss.world.rand.nextFloat() * boss.height;
         FXDispatcher.INSTANCE
            .drawGenericParticles(
               boss.getEntityBoundingBox().minX + w,
               boss.getEntityBoundingBox().minY + h,
               boss.getEntityBoundingBox().minZ + d,
               0.0,
               0.02,
               0.0,
               0.2F,
               0.6F + boss.world.rand.nextFloat() * 0.1F,
               0.2F + boss.world.rand.nextFloat() * 0.1F,
               0.7F,
               false,
               579,
               4,
               1,
               8 + boss.world.rand.nextInt(4),
               0,
               0.5F + boss.world.rand.nextFloat() * 0.2F,
               0.5F,
               0
            );
      }
   }

   @Override
   public void preRender(EntityLivingBase boss, RenderLivingBase renderLivingBase) {
      GL11.glColor4f(0.6F, 1.0F, 0.7F, 1.0F);
   }
}
