package thaumcraft.common.entities.monster.mods;

import net.minecraft.client.renderer.entity.RenderLivingBase;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.DamageSource;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import thaumcraft.api.capabilities.IPlayerWarp;
import thaumcraft.api.capabilities.ThaumcraftCapabilities;
import thaumcraft.client.fx.FXDispatcher;

public class ChampionModWarp implements IChampionModifierEffect {
   @Override
   public float performEffect(EntityLivingBase boss, EntityLivingBase target, DamageSource source, float amount) {
      if (boss.world.rand.nextFloat() < 0.33F && target instanceof EntityPlayer) {
         ThaumcraftCapabilities.getWarp((EntityPlayer)target).add(IPlayerWarp.EnumWarpType.TEMPORARY, 1 + boss.world.rand.nextInt(3));
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
               0.0,
               0.0,
               0.8F + boss.world.rand.nextFloat() * 0.2F,
               0.0F,
               0.9F + boss.world.rand.nextFloat() * 0.1F,
               0.7F,
               true,
               264,
               8,
               1,
               10 + boss.world.rand.nextInt(4),
               0,
               0.6F + boss.world.rand.nextFloat() * 0.4F,
               0.0F,
               0
            );
      }
   }

   @Override
   public void preRender(EntityLivingBase boss, RenderLivingBase renderLivingBase) {
   }
}
