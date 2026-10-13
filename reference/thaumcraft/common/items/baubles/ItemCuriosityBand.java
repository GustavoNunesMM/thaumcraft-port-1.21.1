package thaumcraft.common.items.baubles;

import baubles.api.BaubleType;
import baubles.api.IBauble;
import baubles.api.render.IRenderBauble;
import baubles.api.render.IRenderBauble.Helper;
import baubles.api.render.IRenderBauble.RenderType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import thaumcraft.client.lib.UtilsFX;
import thaumcraft.common.items.ItemTCBase;

public class ItemCuriosityBand extends ItemTCBase implements IBauble, IRenderBauble {
   ResourceLocation tex = new ResourceLocation("thaumcraft", "textures/items/curiosity_band_worn.png");

   public ItemCuriosityBand() {
      super("curiosity_band");
      this.maxStackSize = 1;
      this.canRepair = false;
      this.setMaxDamage(0);
   }

   public EnumRarity getRarity(ItemStack itemstack) {
      return EnumRarity.RARE;
   }

   public BaubleType getBaubleType(ItemStack itemstack) {
      return BaubleType.HEAD;
   }

   @SideOnly(Side.CLIENT)
   public void onPlayerBaubleRender(ItemStack stack, EntityPlayer player, RenderType type, float ticks) {
      if (type == RenderType.HEAD) {
         boolean armor = !player.getItemStackFromSlot(EntityEquipmentSlot.HEAD).isEmpty();
         Minecraft.getMinecraft().renderEngine.bindTexture(this.tex);
         Helper.translateToHeadLevel(player);
         Helper.translateToFace();
         Helper.defaultTransforms();
         GlStateManager.rotate(180.0F, 0.0F, 1.0F, 0.0F);
         GlStateManager.translate(-0.5, -0.5, armor ? 0.12F : 0.0);
         UtilsFX.renderTextureIn3D(0.0F, 0.0F, 1.0F, 1.0F, 16, 26, 0.1F);
      }
   }
}
