package thaumcraft.common.items.casters;

import java.text.DecimalFormat;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import thaumcraft.api.casters.FocusEffect;
import thaumcraft.api.casters.FocusEngine;
import thaumcraft.api.casters.FocusMediumRoot;
import thaumcraft.api.casters.FocusModSplit;
import thaumcraft.api.casters.FocusNode;
import thaumcraft.api.casters.FocusPackage;
import thaumcraft.api.casters.IFocusElement;
import thaumcraft.api.casters.NodeSetting;
import thaumcraft.common.registers.ModDataComponents;

/** ItemFocus: a crystal focus with a maximum complexity; the focal manipulator imprints a focus package into it. */
public class FocusItem extends Item {
   public static final DecimalFormat FORMAT = new DecimalFormat("#######.##");
   private final int maxComplexity;

   public FocusItem(int complexity, Properties properties) {
      super(properties);
      this.maxComplexity = complexity;
   }

   /** The average colour of the package's effects (white for a blank focus). */
   public static int getFocusColor(ItemStack focusstack) {
      FocusPackage core = getPackage(focusstack);
      if (core == null) return 0xFFFFFF;
      FocusEffect[] fe = core.getFocusEffects();
      int r = 0, g = 0, b = 0;
      for (FocusEffect ef : fe) {
         int c = FocusEngine.getElementColor(ef.getKey());
         r += c >> 16 & 255;
         g += c >> 8 & 255;
         b += c & 255;
      }
      if (fe.length > 0) {
         r /= fe.length;
         g /= fe.length;
         b /= fe.length;
      }
      return r << 16 | g << 8 | b;
   }

   /** Name + package hash: sorts and identifies foci for the focus selector. Null for a blank focus. */
   @Nullable
   public static String getSortingHelper(ItemStack focusstack) {
      FocusPackage p = getPackage(focusstack);
      if (p == null) return null;
      return focusstack.getHoverName().getString() + p.getSortingHelper();
   }

   public static void setPackage(ItemStack focusstack, FocusPackage core) {
      focusstack.set(ModDataComponents.FOCUS_PACKAGE.get(), core.serialize());
   }

   @Nullable
   public static FocusPackage getPackage(@Nullable ItemStack focusstack) {
      if (focusstack == null || focusstack.isEmpty()) return null;
      CompoundTag tag = focusstack.get(ModDataComponents.FOCUS_PACKAGE.get());
      if (tag == null) return null;
      FocusPackage p = new FocusPackage();
      p.deserialize(tag);
      return p;
   }

   @Override
   public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
      addFocusInformation(stack, tooltip);
   }

   public void addFocusInformation(ItemStack stack, List<Component> tooltip) {
      FocusPackage p = getPackage(stack);
      if (p != null) {
         tooltip.add(Component.literal(FORMAT.format(getVisCost(stack)) + " ").append(Component.translatable("item.Focus.cost1")));
         for (IFocusElement fe : p.nodes) {
            if (fe instanceof FocusNode fn && !(fe instanceof FocusMediumRoot)) buildInfo(tooltip, fn, 0);
         }
      }
   }

   private static void buildInfo(List<Component> list, FocusNode node, int depth) {
      if (node instanceof FocusMediumRoot) return;
      MutableComponent t0 = Component.literal("  ".repeat(depth))
         .append(Component.translatable(node.getUnlocalizedName()).withStyle(ChatFormatting.DARK_PURPLE));
      if (!node.getSettingList().isEmpty()) {
         MutableComponent s = Component.literal(" [");
         boolean q = false;
         for (String st : node.getSettingList()) {
            NodeSetting ns = node.getSetting(st);
            if (q) s.append(", ");
            s.append(ns.getLocalizedName()).append(" ").append(ns.getValueText());
            q = true;
         }
         t0.append(s.append("]").withStyle(ChatFormatting.DARK_AQUA));
      }
      list.add(t0);
      if (node instanceof FocusModSplit split) {
         for (FocusPackage p : split.getSplitPackages()) {
            for (IFocusElement fe : p.nodes) {
               if (fe instanceof FocusNode fn && !(fe instanceof FocusMediumRoot)) buildInfo(list, fn, depth + 1);
            }
         }
      }
   }

   public float getVisCost(ItemStack focusstack) {
      FocusPackage p = getPackage(focusstack);
      return p == null ? 0.0F : p.getComplexity() / 5.0F;
   }

   /** Cooldown in ticks between casts. */
   public int getActivationTime(ItemStack focusstack) {
      FocusPackage p = getPackage(focusstack);
      return p == null ? 0 : Math.max(5, p.getComplexity() / 5 * (p.getComplexity() / 4));
   }

   public int getMaxComplexity() {
      return maxComplexity;
   }
}
