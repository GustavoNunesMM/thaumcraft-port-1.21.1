package thaumcraft.common.golems.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms.TransformType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import thaumcraft.api.golems.IGolemAPI;
import thaumcraft.api.golems.parts.PartModel;

public class PartModelHauler extends PartModel {
   public PartModelHauler(ResourceLocation objModel, ResourceLocation objTexture, PartModel.EnumAttachPoint attachPoint) {
      super(objModel, objTexture, attachPoint);
   }

   @Override
   public void postRenderObjectPart(String partName, IGolemAPI golem, float partialTicks, PartModel.EnumLimbSide side) {
      if (golem.getCarrying().size() > 1 && golem.getCarrying().get(1) != null) {
         ItemStack itemstack = (ItemStack)golem.getCarrying().get(1);
         if (itemstack != null && !itemstack.isEmpty()) {
            GlStateManager.pushMatrix();
            Item item = itemstack.getItem();
            Minecraft minecraft = Minecraft.getMinecraft();
            GlStateManager.scale(0.375, 0.375, 0.375);
            GlStateManager.translate(0.0F, 0.33F, 0.825F);
            if (!(item instanceof ItemBlock)) {
               GlStateManager.translate(0.0F, 0.0F, -0.25F);
            }

            minecraft.getItemRenderer().renderItem(golem.getGolemEntity(), itemstack, TransformType.HEAD);
            GlStateManager.popMatrix();
         }
      }
   }
}
