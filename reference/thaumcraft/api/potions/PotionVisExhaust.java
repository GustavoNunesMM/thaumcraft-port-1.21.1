package thaumcraft.api.potions;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.potion.Potion;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class PotionVisExhaust extends Potion {
   public static Potion instance = null;
   private int statusIconIndex = -1;
   static final ResourceLocation rl = new ResourceLocation("thaumcraft", "textures/misc/potions.png");

   public PotionVisExhaust(boolean par2, int par3) {
      super(par2, par3);
      this.setIconIndex(5, 1);
      this.setPotionName("potion.vis_exhaust");
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
   }
}
