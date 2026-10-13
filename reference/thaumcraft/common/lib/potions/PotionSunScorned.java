package thaumcraft.common.lib.potions;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.potion.Potion;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class PotionSunScorned extends Potion {
   public static Potion instance = null;
   private int statusIconIndex = -1;
   static final ResourceLocation rl = new ResourceLocation("thaumcraft", "textures/misc/potions.png");

   public PotionSunScorned(boolean par2, int par3) {
      super(par2, par3);
      this.setIconIndex(0, 0);
      this.setPotionName("potion.sunscorned");
      this.setIconIndex(6, 2);
      this.setEffectiveness(0.25);
   }

   public boolean isBadEffect() {
      return true;
   }

   @SideOnly(Side.CLIENT)
   public int getStatusIconIndex() {
      Minecraft.getMinecraft().renderEngine.bindTexture(rl);
      return super.getStatusIconIndex();
   }

   public void performEffect(EntityLivingBase target, int par2) {
      if (!target.world.isRemote) {
         float f = target.getBrightness();
         if (f > 0.5F
            && target.world.rand.nextFloat() * 30.0F < (f - 0.4F) * 2.0F
            && target.world
               .canBlockSeeSky(
                  new BlockPos(
                     MathHelper.floor(target.posX),
                     MathHelper.floor(target.posY),
                     MathHelper.floor(target.posZ)
                  )
               )) {
            target.setFire(4);
         } else if (f < 0.25F && target.world.rand.nextFloat() > f * 2.0F) {
            target.heal(1.0F);
         }
      }
   }

   public boolean isReady(int par1, int par2) {
      return par1 % 40 == 0;
   }
}
