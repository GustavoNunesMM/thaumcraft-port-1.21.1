package thaumcraft.common.items.tools;

import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import thaumcraft.api.ThaumcraftMaterials;

/**
 * ItemThaumiumAxe/Hoe/Pickaxe/Shovel/Sword: plain tools of the thaumium tier (repaired with thaumium ingots through the
 * tier). The 1.12 damage and speed values map one to one onto the vanilla attribute helpers.
 */
public final class ThaumiumTools {
   private ThaumiumTools() {
   }

   /** ItemHoe in 1.12: no damage bonus, attack speed = material damage - 3. */
   public static ItemAttributeModifiers hoeAttributes(Tier tier) {
      return DiggerItem.createAttributes(tier, -tier.getAttackDamageBonus(), tier.getAttackDamageBonus() - 3.0F);
   }

   public static SwordItem sword(Item.Properties p) {
      return new SwordItem(ThaumcraftMaterials.TOOLMAT_THAUMIUM, p.attributes(SwordItem.createAttributes(ThaumcraftMaterials.TOOLMAT_THAUMIUM, 3, -2.4F)));
   }

   public static PickaxeItem pickaxe(Item.Properties p) {
      return new PickaxeItem(ThaumcraftMaterials.TOOLMAT_THAUMIUM, p.attributes(DiggerItem.createAttributes(ThaumcraftMaterials.TOOLMAT_THAUMIUM, 1.0F, -2.8F)));
   }

   public static AxeItem axe(Item.Properties p) {
      return new AxeItem(ThaumcraftMaterials.TOOLMAT_THAUMIUM, p.attributes(DiggerItem.createAttributes(ThaumcraftMaterials.TOOLMAT_THAUMIUM, 8.0F, -3.0F)));
   }

   public static ShovelItem shovel(Item.Properties p) {
      return new ShovelItem(ThaumcraftMaterials.TOOLMAT_THAUMIUM, p.attributes(DiggerItem.createAttributes(ThaumcraftMaterials.TOOLMAT_THAUMIUM, 1.5F, -3.0F)));
   }

   public static HoeItem hoe(Item.Properties p) {
      return new HoeItem(ThaumcraftMaterials.TOOLMAT_THAUMIUM, p.attributes(hoeAttributes(ThaumcraftMaterials.TOOLMAT_THAUMIUM)));
   }
}
