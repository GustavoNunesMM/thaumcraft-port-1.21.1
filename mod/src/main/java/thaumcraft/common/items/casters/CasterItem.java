package thaumcraft.common.items.casters;

import java.text.DecimalFormat;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import thaumcraft.api.casters.FocusEngine;
import thaumcraft.api.casters.FocusPackage;
import thaumcraft.api.casters.ICaster;
import thaumcraft.api.casters.IFocusBlockPicker;
import thaumcraft.api.casters.IFocusElement;
import thaumcraft.api.casters.IInteractWithCaster;
import thaumcraft.api.items.IArchitect;
import thaumcraft.common.network.ModNetwork;
import thaumcraft.common.network.SyncAuraPayload;
import thaumcraft.common.registers.ModDataComponents;
import thaumcraft.common.world.aura.AuraChunk;
import thaumcraft.common.world.aura.AuraHandler;

/**
 * ItemCaster: the caster's gauntlet. It draws vis from the aura (area 0: this chunk; 1: this chunk and the four 16
 * blocks away; 2: the 3x3 chunks around), works devices like tubes and casts the focus fitted to it.
 */
public class CasterItem extends Item implements ICaster, IArchitect {
   private static final DecimalFormat FORMAT = new DecimalFormat("#######.#");
   private final int area;

   public CasterItem(int area, Properties properties) {
      super(properties);
      this.area = area;
   }

   private float getAuraPool(Player player) {
      Level level = player.level();
      BlockPos pos = player.blockPosition();
      float tot;
      switch (area) {
         case 1 -> {
            tot = AuraHandler.getVis(level, pos);
            for (Direction face : Direction.Plane.HORIZONTAL) tot += AuraHandler.getVis(level, pos.relative(face, 16));
         }
         case 2 -> {
            tot = 0.0F;
            for (int xx = -1; xx <= 1; xx++) {
               for (int zz = -1; zz <= 1; zz++) tot += AuraHandler.getVis(level, pos.offset(xx * 16, 0, zz * 16));
            }
         }
         default -> tot = AuraHandler.getVis(level, pos);
      }
      return tot;
   }

   @Override
   public boolean consumeVis(ItemStack is, Player player, float amount, boolean crafting, boolean sim) {
      amount *= getConsumptionModifier(is, player, crafting);
      if (getAuraPool(player) < amount) return false;
      if (sim) return true;
      Level level = player.level();
      BlockPos pos = player.blockPosition();
      switch (area) {
         case 1 -> {
            float i = amount / 5.0F;
            while (amount > 0.0F) {
               i = Math.min(i, amount);
               amount -= AuraHandler.drainVis(level, pos, i, false);
               if (amount <= 0.0F) return true;
               i = Math.min(i, amount);
               for (Direction face : Direction.Plane.HORIZONTAL) {
                  amount -= AuraHandler.drainVis(level, pos.relative(face, 16), i, false);
                  if (amount <= 0.0F) return true;
               }
            }
         }
         case 2 -> {
            float i = amount / 9.0F;
            while (amount > 0.0F) {
               i = Math.min(i, amount);
               for (int xx = -1; xx <= 1; xx++) {
                  for (int zz = -1; zz <= 1; zz++) {
                     amount -= AuraHandler.drainVis(level, pos.offset(xx * 16, 0, zz * 16), i, false);
                     if (amount <= 0.0F) return true;
                  }
               }
            }
         }
         default -> amount -= AuraHandler.drainVis(level, pos, amount, false);
      }
      return amount <= 0.0F;
   }

   @Override
   public float getConsumptionModifier(ItemStack is, Player player, boolean crafting) {
      float mod = 1.0F;
      if (player != null) mod -= CasterManager.getTotalVisDiscount(player);
      return Math.max(mod, 0.1F);
   }

   // ------------------------------------------------------------------ focus

   @Nullable
   @Override
   public FocusItem getFocus(ItemStack stack) {
      ItemStack f = getFocusStack(stack);
      return f != null && f.getItem() instanceof FocusItem fi ? fi : null;
   }

   @Nullable
   @Override
   public ItemStack getFocusStack(ItemStack stack) {
      ItemContainerContents c = stack.get(ModDataComponents.CASTER_FOCUS.get());
      if (c == null) return null;
      ItemStack f = c.copyOne();
      return f.isEmpty() ? null : f;
   }

   @Override
   public void setFocus(ItemStack stack, @Nullable ItemStack focus) {
      if (focus != null && !focus.isEmpty()) stack.set(ModDataComponents.CASTER_FOCUS.get(), ItemContainerContents.fromItems(List.of(focus.copy())));
      else stack.remove(ModDataComponents.CASTER_FOCUS.get());
   }

   @Nullable
   private FocusPackage focusPackage(ItemStack stack) {
      return FocusItem.getPackage(getFocusStack(stack));
   }

   @Override
   public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
      if (oldStack.getItem() == this && newStack.getItem() == this) {
         String s1 = FocusItem.getSortingHelper(getFocusStack(oldStack));
         String s2 = FocusItem.getSortingHelper(getFocusStack(newStack));
         return (s1 == null ? 0 : s1.hashCode()) != (s2 == null ? 0 : s2.hashCode());
      }
      return newStack.getItem() != oldStack.getItem();
   }

   @Override
   public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
      ItemStack focus = getFocusStack(stack);
      Component text = Component.empty();
      if (focus != null && focus.getItem() instanceof FocusItem fi) {
         float amt = fi.getVisCost(focus);
         if (amt > 0.0F) text = Component.literal(FORMAT.format(amt) + " ").append(Component.translatable("item.Focus.cost1"))
            .withStyle(ChatFormatting.RESET);
      }
      tooltip.add(Component.translatable("tc.vis.cost").append(" ").append(text).withStyle(ChatFormatting.ITALIC, ChatFormatting.AQUA));
      if (focus != null && focus.getItem() instanceof FocusItem fi) {
         tooltip.add(focus.getHoverName().copy().withStyle(ChatFormatting.BOLD, ChatFormatting.ITALIC, ChatFormatting.GREEN));
         fi.addFocusInformation(focus, tooltip);
      }
   }

   /** Keeps the client's aura gauge current while a caster is held (every half second). */
   @Override
   public void inventoryTick(ItemStack is, Level w, Entity e, int slot, boolean currentItem) {
      if (!w.isClientSide && e.tickCount % 10 == 0 && e instanceof ServerPlayer sp) {
         for (ItemStack h : sp.getHandSlots()) {
            if (!h.isEmpty() && h.getItem() instanceof ICaster) {
               updateAura(w, sp);
               break;
            }
         }
      }
   }

   private void updateAura(Level world, ServerPlayer player) {
      float cv = 0.0F, cf = 0.0F;
      int bv = 0;
      int cx = player.getBlockX() >> 4, cz = player.getBlockZ() >> 4;
      switch (area) {
         case 1 -> {
            AuraChunk ac = AuraHandler.getAuraChunk(world, cx, cz);
            if (ac != null) {
               cv = ac.getVis();
               cf = ac.getFlux();
               bv = ac.getBase();
               for (Direction face : Direction.Plane.HORIZONTAL) {
                  ac = AuraHandler.getAuraChunk(world, cx + face.getStepX(), cz + face.getStepZ());
                  if (ac != null) {
                     cv += ac.getVis();
                     cf += ac.getFlux();
                     bv += ac.getBase();
                  }
               }
            }
         }
         case 2 -> {
            for (int xx = -1; xx <= 1; xx++) {
               for (int zz = -1; zz <= 1; zz++) {
                  AuraChunk ac = AuraHandler.getAuraChunk(world, cx + xx, cz + zz);
                  if (ac != null) {
                     cv += ac.getVis();
                     cf += ac.getFlux();
                     bv += ac.getBase();
                  }
               }
            }
         }
         default -> {
            AuraChunk ac = AuraHandler.getAuraChunk(world, cx, cz);
            if (ac != null) {
               cv = ac.getVis();
               cf = ac.getFlux();
               bv = ac.getBase();
            }
         }
      }
      ModNetwork.sendTo(player, new SyncAuraPayload((short) bv, cv, cf));
   }

   /** Devices that react to a caster get the click before the block itself; a block picker focus picks with sneak. */
   @Override
   public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext ctx) {
      Level level = ctx.getLevel();
      BlockPos pos = ctx.getClickedPos();
      Player player = ctx.getPlayer();
      if (player == null) return InteractionResult.PASS;
      BlockHitResult hit = new BlockHitResult(ctx.getClickLocation(), ctx.getClickedFace(), pos, ctx.isInside());
      BlockState bs = level.getBlockState(pos);
      if (bs.getBlock() instanceof IInteractWithCaster b && b.onCasterRightClick(level, stack, player, pos, ctx.getClickedFace(), ctx.getHand(), hit)) {
         return InteractionResult.sidedSuccess(level.isClientSide);
      }
      if (level.getBlockEntity(pos) instanceof IInteractWithCaster t && t.onCasterRightClick(level, stack, player, pos, ctx.getClickedFace(), ctx.getHand(), hit)) {
         return InteractionResult.sidedSuccess(level.isClientSide);
      }
      FocusPackage core = focusPackage(stack);
      if (core != null && player.isShiftKeyDown() && level.getBlockEntity(pos) == null) {
         for (IFocusElement fe : core.nodes) {
            if (fe instanceof IFocusBlockPicker) {
               if (!level.isClientSide) {
                  ItemStack isout = new ItemStack(bs.getBlock().asItem());
                  if (isout.isEmpty() || isout.is(Items.AIR)) isout = ItemStack.EMPTY;
                  storePickedBlock(stack, isout);
                  return InteractionResult.SUCCESS;
               }
               player.swing(ctx.getHand());
               return InteractionResult.PASS;
            }
         }
      }
      return InteractionResult.PASS;
   }

   @Override
   public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
      ItemStack held = player.getItemInHand(hand);
      ItemStack focusStack = getFocusStack(held);
      FocusItem focus = getFocus(held);
      if (focus != null && !CasterManager.isOnCooldown(player)) {
         CasterManager.setCooldown(player, focus.getActivationTime(focusStack));
         FocusPackage core = FocusItem.getPackage(focusStack);
         if (core == null) return InteractionResultHolder.pass(held);
         if (player.isShiftKeyDown()) {
            for (IFocusElement fe : core.nodes) {
               if (fe instanceof IFocusBlockPicker) return InteractionResultHolder.pass(held);
            }
         }
         if (world.isClientSide) return InteractionResultHolder.success(held);
         if (consumeVis(held, player, focus.getVisCost(focusStack), false, false)) {
            FocusEngine.castFocusPackage(player, core);
            player.swing(hand, true);
            return InteractionResultHolder.success(held);
         }
         return InteractionResultHolder.fail(held);
      }
      return super.use(world, player, hand);
   }

   // ------------------------------------------------------------------ architect (plan focus)

   @Override
   public List<BlockPos> getArchitectBlocks(ItemStack stack, Level world, BlockPos pos, Direction side, Player player) {
      FocusPackage fp = focusPackage(stack);
      if (fp != null) {
         for (IFocusElement fe : fp.nodes) {
            if (fe instanceof IArchitect a) return a.getArchitectBlocks(stack, world, pos, side, player);
         }
      }
      return null;
   }

   @Override
   public boolean showAxis(ItemStack stack, Level world, Player player, Direction side, EnumAxis axis) {
      FocusPackage fp = focusPackage(stack);
      if (fp != null) {
         for (IFocusElement fe : fp.nodes) {
            if (fe instanceof IArchitect a) return a.showAxis(stack, world, player, side, axis);
         }
      }
      return false;
   }

   @Override
   public HitResult getArchitectMOP(ItemStack stack, Level world, LivingEntity player) {
      FocusPackage fp = focusPackage(stack);
      if (fp != null && FocusEngine.doesPackageContainElement(fp, "thaumcraft.PLAN")
         && FocusEngine.getElement("thaumcraft.PLAN") instanceof IArchitect a) {
         return a.getArchitectMOP(getFocusStack(stack), world, player);
      }
      return null;
   }

   @Override
   public boolean useBlockHighlight(ItemStack stack) {
      return false;
   }

   public void storePickedBlock(ItemStack stack, ItemStack stackout) {
      if (stackout.isEmpty()) stack.remove(ModDataComponents.PICKED_BLOCK.get());
      else stack.set(ModDataComponents.PICKED_BLOCK.get(), ItemContainerContents.fromItems(List.of(stackout.copyWithCount(1))));
   }

   @Nullable
   @Override
   public ItemStack getPickedBlock(ItemStack stack) {
      if (stack == null || stack.isEmpty()) return ItemStack.EMPTY;
      ItemContainerContents picked = stack.get(ModDataComponents.PICKED_BLOCK.get());
      FocusPackage fp = focusPackage(stack);
      if (picked != null && fp != null) {
         for (IFocusElement fe : fp.nodes) {
            if (fe instanceof IFocusBlockPicker) return picked.copyOne();
         }
      }
      return null;
   }
}
