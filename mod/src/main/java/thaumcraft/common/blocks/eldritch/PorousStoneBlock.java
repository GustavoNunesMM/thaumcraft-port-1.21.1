package thaumcraft.common.blocks.eldritch;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.items.VisCrystalItem;
import thaumcraft.common.registers.ModItems;

/**
 * BlockStonePorous: crumbly stone left behind when tainted rock dies. Usually drops gravel; one time in fifteen (more
 * with fortune) it gives up something it absorbed: a compound vis crystal, amber, native clusters or a vanilla
 * mineral, weighted as in createDrops (tin/silver/lead clusters don't exist in this port).
 */
public class PorousStoneBlock extends Block {
   private record Loot(java.util.function.Supplier<ItemStack> stack, int weight) {
   }

   private static List<Loot> drops;

   public PorousStoneBlock(Properties properties) {
      super(properties);
   }

   private static List<Loot> drops() {
      if (drops == null) {
         List<Loot> l = new ArrayList<>();
         for (Aspect aspect : Aspect.getCompoundAspects()) l.add(new Loot(() -> VisCrystalItem.of(aspect, 1), 1));
         l.add(new Loot(() -> new ItemStack(ModItems.AMBER.get()), 20));
         l.add(new Loot(() -> new ItemStack(ModItems.CLUSTER_IRON.get()), 20));
         l.add(new Loot(() -> new ItemStack(ModItems.CLUSTER_GOLD.get()), 10));
         l.add(new Loot(() -> new ItemStack(ModItems.CLUSTER_CINNABAR.get()), 10));
         l.add(new Loot(() -> new ItemStack(ModItems.CLUSTER_COPPER.get()), 10));
         l.add(new Loot(() -> new ItemStack(Items.DIAMOND), 2));
         l.add(new Loot(() -> new ItemStack(Items.EMERALD), 4));
         l.add(new Loot(() -> new ItemStack(Items.REDSTONE), 8));
         l.add(new Loot(() -> new ItemStack(Items.PRISMARINE_CRYSTALS), 3));
         l.add(new Loot(() -> new ItemStack(Items.PRISMARINE_SHARD), 3));
         l.add(new Loot(() -> new ItemStack(Items.CLAY_BALL), 30));
         l.add(new Loot(() -> new ItemStack(Items.QUARTZ), 15));
         drops = l;
      }
      return drops;
   }

   /** One roll of the porous stone drop table (rr = nextInt(15) + fortune; rr > 13 gives the special drop). */
   public static ItemStack rollDrop(RandomSource r, int fortune) {
      if (r.nextInt(15) + fortune > 13) {
         List<Loot> l = drops();
         int total = l.stream().mapToInt(Loot::weight).sum();
         int pick = r.nextInt(total);
         for (Loot e : l) {
            pick -= e.weight();
            if (pick < 0) return e.stack().get();
         }
      }
      return new ItemStack(Items.GRAVEL);
   }

   @Override
   protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
      ItemStack tool = params.getOptionalParameter(LootContextParams.TOOL);
      int fortune = 0;
      if (tool != null && !tool.isEmpty()) {
         fortune = EnchantmentHelper.getItemEnchantmentLevel(
            params.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.FORTUNE), tool);
      }
      return List.of(rollDrop(params.getLevel().random, fortune));
   }
}
