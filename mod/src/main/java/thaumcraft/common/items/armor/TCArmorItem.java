package thaumcraft.common.items.armor;

import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import thaumcraft.Thaumcraft;

/**
 * Base of Thaumcraft's armor: getArmorTexture as in 1.12 (the textures stay in textures/entity/armor) and the per-item
 * repair material (getIsRepairable). For dyeable armor the dyeable layer gets the base texture and the other one the
 * overlay, like the 1.12 type == null / "overlay" passes.
 */
public class TCArmorItem extends ArmorItem {
   private final Function<EquipmentSlot, String> texture;
   private final Function<EquipmentSlot, String> overlay;
   private final Supplier<Ingredient> repair;

   public TCArmorItem(Holder<ArmorMaterial> material, Type type, Properties p, Function<EquipmentSlot, String> texture,
                      Function<EquipmentSlot, String> overlay, Supplier<Ingredient> repair) {
      super(material, type, p);
      this.texture = texture;
      this.overlay = overlay;
      this.repair = repair;
   }

   public TCArmorItem(Holder<ArmorMaterial> material, Type type, Properties p, String texture, Supplier<Ingredient> repair) {
      this(material, type, p, s -> texture, null, repair);
   }

   public static ResourceLocation tex(String name) {
      return ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/entity/armor/" + name + ".png");
   }

   @Override
   public ResourceLocation getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, ArmorMaterial.Layer layer, boolean innerModel) {
      if (overlay != null && !layer.dyeable()) return tex(overlay.apply(slot));
      return tex(texture.apply(slot));
   }

   @Override
   public boolean isValidRepairItem(ItemStack stack, ItemStack repairCandidate) {
      if (repair != null && repair.get().test(repairCandidate)) return true;
      return super.isValidRepairItem(stack, repairCandidate);
   }
}
