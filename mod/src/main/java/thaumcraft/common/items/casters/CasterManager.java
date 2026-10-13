package thaumcraft.common.items.casters;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import javax.annotation.Nullable;
import net.minecraft.core.NonNullList;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import thaumcraft.api.casters.FocusEngine;
import thaumcraft.api.casters.FocusPackage;
import thaumcraft.api.casters.ICaster;
import thaumcraft.api.items.IVisDiscountGear;
import thaumcraft.common.registers.ModDataComponents;
import thaumcraft.common.registers.ModSounds;
import top.theillusivec4.curios.api.CuriosApi;

/** CasterManager: vis discounts, cooldowns, changing foci (from the inventory and pouches) and the plan focus area. */
public final class CasterManager {
   static final Map<Integer, Long> cooldownServer = new HashMap<>();
   static final Map<Integer, Long> cooldownClient = new HashMap<>();

   private CasterManager() {
   }

   /** Total vis discount (0..1) of the armor worn; baubles and vis exhaustion come with those features. */
   public static float getTotalVisDiscount(Player player) {
      if (player == null) return 0.0F;
      int total = 0;
      for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
         ItemStack s = player.getItemBySlot(slot);
         if (s.getItem() instanceof IVisDiscountGear g) total += g.getVisDiscount(s, player);
      }
      return total / 100.0F;
   }

   /** Crafting vis paid from the first caster in the inventory (last slot first, as in the original). */
   public static boolean consumeVisFromInventory(Player player, float cost) {
      NonNullList<ItemStack> inv = player.getInventory().items;
      for (int a = inv.size() - 1; a >= 0; a--) {
         ItemStack item = inv.get(a);
         if (item.getItem() instanceof ICaster c && c.consumeVis(item, player, cost, true, false)) return true;
      }
      return false;
   }

   // ------------------------------------------------------------------ focus pouches (inventory and curios belt)

   /** The player's curio slots (the belt slot holds a focus pouch), or null without Curios data. */
   @Nullable
   static IItemHandlerModifiable curios(Player player) {
      try {
         return CuriosApi.getCuriosInventory(player).map(h -> h.getEquippedCurios()).orElse(null);
      } catch (Throwable t) {
         return null;
      }
   }

   /** A pouch location: an inventory slot (>= 0) or a curio slot (encoded as -1 - index). */
   private static ItemStack pouchAt(Player player, int loc) {
      if (loc >= 0) return player.getInventory().items.get(loc);
      IItemHandlerModifiable c = curios(player);
      return c == null ? ItemStack.EMPTY : c.getStackInSlot(-1 - loc);
   }

   private static void setPouchAt(Player player, int loc, ItemStack pouch) {
      if (loc >= 0) {
         player.getInventory().items.set(loc, pouch);
         player.getInventory().setChanged();
      } else {
         IItemHandlerModifiable c = curios(player);
         if (c != null) c.setStackInSlot(-1 - loc, pouch);
      }
   }

   /** All foci the player carries, keyed by sorting helper: inventory slot, or pouch id * 1000 + pouch slot. */
   public static TreeMap<String, Integer> collectFoci(Player player, Map<Integer, Integer> pouches, @Nullable Map<String, ItemStack> items) {
      TreeMap<String, Integer> foci = new TreeMap<>();
      int pouchcount = 0;
      IItemHandlerModifiable baubles = curios(player);
      if (baubles != null) {
         for (int a = 0; a < baubles.getSlots(); a++) {
            ItemStack item = baubles.getStackInSlot(a);
            if (item.getItem() instanceof FocusPouchItem) {
               pouchcount++;
               pouches.put(pouchcount, -1 - a);
               NonNullList<ItemStack> inv = FocusPouchItem.getInventory(item);
               for (int q = 0; q < inv.size(); q++) {
                  ItemStack f = inv.get(q);
                  if (f.getItem() instanceof FocusItem) {
                     String sh = FocusItem.getSortingHelper(f);
                     if (sh != null) {
                        foci.put(sh, q + pouchcount * 1000);
                        if (items != null) items.put(sh, f.copy());
                     }
                  }
               }
            }
         }
      }
      for (int ax = 0; ax < 36; ax++) {
         ItemStack item = player.getInventory().items.get(ax);
         if (item.getItem() instanceof FocusItem) {
            String sh = FocusItem.getSortingHelper(item);
            if (sh == null) continue;
            foci.put(sh, ax);
            if (items != null) items.put(sh, item.copy());
         }
         if (item.getItem() instanceof FocusPouchItem) {
            pouches.put(++pouchcount, ax);
            NonNullList<ItemStack> inv = FocusPouchItem.getInventory(item);
            for (int qx = 0; qx < inv.size(); qx++) {
               ItemStack f = inv.get(qx);
               if (f.getItem() instanceof FocusItem) {
                  String sh = FocusItem.getSortingHelper(f);
                  if (sh != null) {
                     foci.put(sh, qx + pouchcount * 1000);
                     if (items != null) items.put(sh, f.copy());
                  }
               }
            }
         }
      }
      return foci;
   }

   /**
    * Swaps the caster's focus for the one with the given sorting key (or the next one after it); "REMOVE" takes the focus
    * out. Removed foci go into a pouch first, then the inventory.
    */
   public static void changeFocus(ItemStack is, Level w, Player player, String focus) {
      ICaster wand = (ICaster) is.getItem();
      Map<Integer, Integer> pouches = new HashMap<>();
      TreeMap<String, Integer> foci = collectFoci(player, pouches, null);
      if (!focus.equals("REMOVE") && !foci.isEmpty()) {
         String newkey = focus;
         if (foci.get(focus) == null) newkey = foci.higherKey(focus);
         if (newkey == null || foci.get(newkey) == null) newkey = foci.firstKey();
         int where = foci.get(newkey);
         ItemStack item = ItemStack.EMPTY;
         if (where < 1000 && where >= 0) {
            item = player.getInventory().items.get(where).copy();
         } else {
            int pid = where / 1000;
            if (pouches.containsKey(pid)) {
               int pouchslot = pouches.get(pid);
               item = fetchFocusFromPouch(player, where - pid * 1000, pouchAt(player, pouchslot).copy(), pouchslot);
            }
         }
         if (item.isEmpty()) return;
         if (where < 1000 && where >= 0) player.getInventory().items.set(where, ItemStack.EMPTY);
         w.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.TICKS.get(), SoundSource.PLAYERS, 0.3F, 1.0F);
         if (wand.getFocus(is) != null) {
            ItemStack old = wand.getFocusStack(is);
            if (addFocusToPouch(player, old.copy(), pouches) || player.getInventory().add(old.copy())) wand.setFocus(is, ItemStack.EMPTY);
         }
         if (wand.getFocus(is) == null) {
            wand.setFocus(is, item);
         } else if (!addFocusToPouch(player, item, pouches)) {
            player.getInventory().add(item);
         }
      } else if (wand.getFocus(is) != null) {
         ItemStack old = wand.getFocusStack(is);
         if (addFocusToPouch(player, old.copy(), pouches) || player.getInventory().add(old.copy())) {
            wand.setFocus(is, null);
            w.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.TICKS.get(), SoundSource.PLAYERS, 0.3F, 0.9F);
         }
      }
   }

   private static ItemStack fetchFocusFromPouch(Player player, int focusid, ItemStack pouch, int pouchslot) {
      ItemStack focus = ItemStack.EMPTY;
      NonNullList<ItemStack> inv = FocusPouchItem.getInventory(pouch);
      ItemStack contents = inv.get(focusid);
      if (contents.getItem() instanceof FocusItem) {
         focus = contents.copy();
         inv.set(focusid, ItemStack.EMPTY);
         FocusPouchItem.setInventory(pouch, inv);
         setPouchAt(player, pouchslot, pouch);
      }
      return focus;
   }

   private static boolean addFocusToPouch(Player player, ItemStack focus, Map<Integer, Integer> pouches) {
      for (int pouchslot : pouches.values()) {
         ItemStack pouch = pouchAt(player, pouchslot);
         if (!(pouch.getItem() instanceof FocusPouchItem)) continue;
         NonNullList<ItemStack> inv = FocusPouchItem.getInventory(pouch);
         for (int q = 0; q < inv.size(); q++) {
            if (inv.get(q).isEmpty()) {
               inv.set(q, focus.copy());
               ItemStack np = pouch.copy();
               FocusPouchItem.setInventory(np, inv);
               setPouchAt(player, pouchslot, np);
               return true;
            }
         }
      }
      return false;
   }

   // ------------------------------------------------------------------ plan focus area (G key)

   /** G: grows the plan area along the selected axis (mod 0); CTRL+G (mod 1) cycles the axis. */
   public static void toggleMisc(ItemStack itemstack, Level world, Player player, int mod) {
      if (!(itemstack.getItem() instanceof ICaster caster)) return;
      FocusPackage fp = FocusItem.getPackage(caster.getFocusStack(itemstack));
      if (fp != null && FocusEngine.doesPackageContainElement(fp, "thaumcraft.PLAN")) {
         int dim = getAreaDim(itemstack);
         if (mod == 0) {
            int areax = getAreaX(itemstack);
            int areay = getAreaY(itemstack);
            int areaz = getAreaZ(itemstack);
            int max = getAreaSize(itemstack);
            if (dim == 0) {
               areax++;
               areaz++;
               areay++;
            } else if (dim == 1) {
               areax++;
            } else if (dim == 2) {
               areaz++;
            } else if (dim == 3) {
               areay++;
            }
            if (areax > max) areax = 0;
            if (areaz > max) areaz = 0;
            if (areay > max) areay = 0;
            setArea(itemstack, areax, areay, areaz, dim);
         }
         if (mod == 1) {
            if (++dim > 3) dim = 0;
            setArea(itemstack, getAreaX(itemstack), getAreaY(itemstack), getAreaZ(itemstack), dim);
         }
      }
   }

   private static int getAreaSize(ItemStack itemstack) {
      return itemstack.getItem() instanceof ICaster ? 3 : 0;
   }

   private static ModDataComponents.CasterArea area(ItemStack stack) {
      ModDataComponents.CasterArea a = stack.get(ModDataComponents.CASTER_AREA.get());
      return a == null ? ModDataComponents.CasterArea.DEFAULT : a;
   }

   private static void setArea(ItemStack stack, int x, int y, int z, int dim) {
      stack.set(ModDataComponents.CASTER_AREA.get(), new ModDataComponents.CasterArea(x, y, z, dim));
   }

   public static int getAreaDim(ItemStack stack) {
      return area(stack).dim();
   }

   public static int getAreaX(ItemStack stack) {
      return Math.min(area(stack).x(), getAreaSize(stack));
   }

   public static int getAreaY(ItemStack stack) {
      return Math.min(area(stack).y(), getAreaSize(stack));
   }

   public static int getAreaZ(ItemStack stack) {
      return Math.min(area(stack).z(), getAreaSize(stack));
   }

   // ------------------------------------------------------------------ cooldowns

   public static boolean isOnCooldown(LivingEntity e) {
      Map<Integer, Long> m = e.level().isClientSide ? cooldownClient : cooldownServer;
      Long t = m.get(e.getId());
      return t != null && t > System.currentTimeMillis();
   }

   /** Seconds left (client only, for the HUD). */
   public static float getCooldown(LivingEntity e) {
      Long t = cooldownClient.get(e.getId());
      return e.level().isClientSide && t != null ? (t - System.currentTimeMillis()) / 1000.0F : 0.0F;
   }

   /** Cooldown in ticks (0 clears it on both sides). */
   public static void setCooldown(LivingEntity e, int cd) {
      if (cd == 0) {
         cooldownClient.remove(e.getId());
         cooldownServer.remove(e.getId());
      } else {
         (e.level().isClientSide ? cooldownClient : cooldownServer).put(e.getId(), System.currentTimeMillis() + cd * 50L);
      }
   }

   /** Every focus element key in the package (and its split packages); for tests and tooltips. */
   public static List<String> keys(FocusPackage p) {
      List<String> out = new ArrayList<>();
      p.nodes.forEach(n -> out.add(n.getKey()));
      return out;
   }
}
