package thaumcraft.common.items;

import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.blocks.essentia.AlembicBlockEntity;
import thaumcraft.common.blocks.essentia.JarBlockEntity;
import thaumcraft.common.registers.ModDataComponents;
import thaumcraft.common.registers.ModItems;

/**
 * ItemPhial: an empty phial takes 10 essentia from a jar or alembic; a filled one (phial_filled, the essentia in the
 * thaumcraft:essentia component) pours its 10 essentia into a jar.
 */
public class PhialItem extends Item implements ICreativeVariants {
   public static final int AMOUNT = 10;
   private final boolean filled;

   public PhialItem(boolean filled, Properties properties) {
      super(properties);
      this.filled = filled;
   }

   public static ItemStack makePhial(Aspect aspect, int amt) {
      ItemStack i = new ItemStack(ModItems.PHIAL_FILLED.get());
      i.set(ModDataComponents.ESSENTIA.get(), new AspectList().add(aspect, amt));
      return i;
   }

   public static ItemStack makeFilledPhial(Aspect aspect) {
      return makePhial(aspect, AMOUNT);
   }

   @Override
   public void addCreativeStacks(Consumer<ItemStack> output) {
      if (!filled) output.accept(new ItemStack(this));
      else Aspect.all().forEach(a -> output.accept(makeFilledPhial(a)));
   }

   @Override
   public Component getName(ItemStack stack) {
      AspectList al = stack.get(ModDataComponents.ESSENTIA.get());
      return filled && al != null && !al.isEmpty() ? Component.translatable(getDescriptionId(stack), al.getAspects()[0].getName())
         : Component.translatable(getDescriptionId(stack), "?");
   }

   private static void give(Player player, ItemStack held, ItemStack result) {
      held.consume(1, player);
      if (!player.getInventory().add(result)) player.drop(result, false);
      player.playSound(SoundEvents.BOTTLE_FILL, 0.25F, 1.0F);
   }

   @Override
   public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext ctx) {
      Level level = ctx.getLevel();
      BlockPos pos = ctx.getClickedPos();
      Player player = ctx.getPlayer();
      if (player == null) return InteractionResult.PASS;
      BlockEntity te = level.getBlockEntity(pos);
      if (!filled) {
         if (te instanceof AlembicBlockEntity tile && tile.amount >= AMOUNT) {
            if (level.isClientSide) return InteractionResult.SUCCESS;
            Aspect a = tile.aspect;
            if (tile.takeFromContainer(a, AMOUNT)) give(player, stack, makePhial(a, AMOUNT));
            return InteractionResult.SUCCESS;
         }
         if (te instanceof JarBlockEntity tile && tile.amount >= AMOUNT) {
            if (level.isClientSide) return InteractionResult.SUCCESS;
            Aspect a = tile.aspect;
            if (tile.takeFromContainer(a, AMOUNT)) give(player, stack, makePhial(a, AMOUNT));
            return InteractionResult.SUCCESS;
         }
         return InteractionResult.PASS;
      }
      AspectList al = stack.get(ModDataComponents.ESSENTIA.get());
      if (al != null && al.size() == 1 && te instanceof JarBlockEntity tile) {
         Aspect aspect = al.getAspects()[0];
         int amt = al.getAmount(aspect);
         if (tile.amount <= JarBlockEntity.CAPACITY - amt && tile.doesContainerAccept(aspect)) {
            if (level.isClientSide) return InteractionResult.SUCCESS;
            if (tile.addToContainer(aspect, amt) == 0) give(player, stack, new ItemStack(ModItems.PHIAL.get()));
            return InteractionResult.SUCCESS;
         }
      }
      return InteractionResult.PASS;
   }
}
