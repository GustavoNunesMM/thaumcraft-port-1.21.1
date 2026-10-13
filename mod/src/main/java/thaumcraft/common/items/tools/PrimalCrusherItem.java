package thaumcraft.common.items.tools;

import java.util.function.Consumer;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;
import thaumcraft.Thaumcraft;
import thaumcraft.api.ThaumcraftMaterials;
import thaumcraft.api.items.IWarpingGear;
import thaumcraft.common.items.ICreativeVariants;
import thaumcraft.common.lib.enchantment.EnumInfusionEnchantment;

/**
 * ItemPrimalCrusher: a pickaxe and shovel in one (the thaumcraft:mineable/primal_crusher tag joins both), self-repairing,
 * two warp; it comes with Destructive and Refining. It can't harvest wood, leaves or plants.
 */
public class PrimalCrusherItem extends DiggerItem implements IWarpingGear, ICreativeVariants {
   public static final TagKey<Block> MINEABLE = TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "mineable/primal_crusher"));

   public PrimalCrusherItem(Properties p) {
      super(ThaumcraftMaterials.TOOLMAT_PRIMALVOID, MINEABLE, p.attributes(DiggerItem.createAttributes(ThaumcraftMaterials.TOOLMAT_PRIMALVOID, 3.5F, -2.8F)));
   }

   @Override
   public boolean canPerformAction(ItemStack stack, ItemAbility ability) {
      return ability == ItemAbilities.PICKAXE_DIG || ability == ItemAbilities.SHOVEL_DIG;
   }

   @Override
   public int getWarp(ItemStack stack, Player player) {
      return 2;
   }

   @Override
   public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
      super.inventoryTick(stack, level, entity, slot, selected);
      VoidTools.selfRepair(stack, level, entity);
   }

   @Override
   public void addCreativeStacks(Consumer<ItemStack> output) {
      ItemStack w1 = new ItemStack(this);
      EnumInfusionEnchantment.addInfusionEnchantment(w1, EnumInfusionEnchantment.DESTRUCTIVE, 1);
      EnumInfusionEnchantment.addInfusionEnchantment(w1, EnumInfusionEnchantment.REFINING, 1);
      output.accept(w1);
   }
}
