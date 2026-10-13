package thaumcraft.common.items.armor;

import net.minecraft.core.Holder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import thaumcraft.api.items.IWarpingGear;
import thaumcraft.common.items.tools.VoidTools;

/** ItemVoidArmor: void metal armor; mends itself (in the inventory and worn) and gives one warp. */
public class VoidArmorItem extends TCArmorItem implements IWarpingGear {
   public VoidArmorItem(Holder<ArmorMaterial> material, Type type, Properties p) {
      super(material, type, p, s -> s == EquipmentSlot.LEGS ? "void_2" : "void_1", null, null);
   }

   /** Covers onUpdate and onArmorTick: the player's inventory tick includes the armor slots. */
   @Override
   public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
      super.inventoryTick(stack, level, entity, slot, selected);
      VoidTools.selfRepair(stack, level, entity);
   }

   @Override
   public int getWarp(ItemStack stack, Player player) {
      return 1;
   }
}
