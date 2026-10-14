package thaumcraft.client.golems;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import thaumcraft.Thaumcraft;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.golems.EnumGolemTrait;
import thaumcraft.api.golems.IGolemProperties;
import thaumcraft.api.golems.parts.GolemAddon;
import thaumcraft.api.golems.parts.GolemArm;
import thaumcraft.api.golems.parts.GolemHead;
import thaumcraft.api.golems.parts.GolemLeg;
import thaumcraft.api.golems.parts.GolemMaterial;
import thaumcraft.client.gui.GuiDraw;
import thaumcraft.common.golems.GolemBuilderBlockEntity;
import thaumcraft.common.golems.GolemInvHelper;
import thaumcraft.common.golems.GolemPayloads;
import thaumcraft.common.golems.GolemProperties;
import thaumcraft.common.golems.menu.GolemBuilderMenu;
import thaumcraft.common.research.ResearchManager;

/**
 * GuiGolemBuilder: pick a material, head, arms, legs and addon among the researched ones; the screen shows the traits,
 * stats and components of the design and asks the press to make it.
 */
public class GolemBuilderScreen extends AbstractContainerScreen<GolemBuilderMenu> {
   private static final ResourceLocation MAT_ICON = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/items/golem.png");
   static int headIndex = 0;
   static int matIndex = 0;
   static int armIndex = 0;
   static int legIndex = 0;
   static int addonIndex = 0;
   private final GolemBuilderBlockEntity builder;
   private final Player player;
   private final List<GolemHead> valHeads = new ArrayList<>();
   private final List<GolemMaterial> valMats = new ArrayList<>();
   private final List<GolemArm> valArms = new ArrayList<>();
   private final List<GolemLeg> valLegs = new ArrayList<>();
   private final List<GolemAddon> valAddons = new ArrayList<>();
   private final List<GolemWidgets.Btn> buttons = new ArrayList<>();
   private IGolemProperties props = GolemProperties.fromLong(0L);
   private float hearts;
   private float armor;
   private float damage;
   private GolemWidgets.Craft craftButton;
   private int cost;
   private boolean allfound;
   private ItemStack[] components;
   private boolean[] owns;
   private boolean disableAll;

   public GolemBuilderScreen(GolemBuilderMenu menu, Inventory inv, Component title) {
      super(menu, inv, title);
      this.player = inv.player;
      this.builder = menu.builder;
      this.imageWidth = 208;
      this.imageHeight = 224;
   }

   @Override
   protected void init() {
      super.init();
      valHeads.clear();
      for (GolemHead head : GolemHead.getHeads()) {
         if (head != null && ResearchManager.knowsResearchStrict(player, head.research)) valHeads.add(head);
      }
      valMats.clear();
      for (GolemMaterial mat : GolemMaterial.getMaterials()) {
         if (mat != null && ResearchManager.knowsResearchStrict(player, mat.research)) valMats.add(mat);
      }
      valArms.clear();
      for (GolemArm arm : GolemArm.getArms()) {
         if (arm != null && ResearchManager.knowsResearchStrict(player, arm.research)) valArms.add(arm);
      }
      valLegs.clear();
      for (GolemLeg leg : GolemLeg.getLegs()) {
         if (leg != null && ResearchManager.knowsResearchStrict(player, leg.research)) valLegs.add(leg);
      }
      valAddons.clear();
      for (GolemAddon addon : GolemAddon.getAddons()) {
         if (addon != null && ResearchManager.knowsResearchStrict(player, addon.research)) valAddons.add(addon);
      }
      if (headIndex >= valHeads.size()) headIndex = 0;
      if (matIndex >= valMats.size()) matIndex = 0;
      if (armIndex >= valArms.size()) armIndex = 0;
      if (legIndex >= valLegs.size()) legIndex = 0;
      if (addonIndex >= valAddons.size()) addonIndex = 0;
      gatherInfo();
   }

   private boolean complete() {
      return !valHeads.isEmpty() && !valMats.isEmpty() && !valArms.isEmpty() && !valLegs.isEmpty() && !valAddons.isEmpty();
   }

   private void gatherInfo() {
      buttons.clear();
      craftButton = new GolemWidgets.Craft(99, leftPos + 120, topPos + 104);
      buttons.add(craftButton);
      if (valHeads.size() > 1) {
         buttons.add(new GolemWidgets.Scroll(0, leftPos + 112 - 5 - 6, topPos - 5 + 16 + 8, 10, 10, true, false));
         buttons.add(new GolemWidgets.Scroll(1, leftPos + 112 - 5 + 22, topPos - 5 + 16 + 8, 10, 10, false, false));
      }
      if (valMats.size() > 1) {
         buttons.add(new GolemWidgets.Scroll(2, leftPos + 16 - 5 - 6, topPos - 5 + 16 + 8, 10, 10, true, false));
         buttons.add(new GolemWidgets.Scroll(3, leftPos + 16 - 5 + 22, topPos - 5 + 16 + 8, 10, 10, false, false));
      }
      if (valArms.size() > 1) {
         buttons.add(new GolemWidgets.Scroll(4, leftPos + 112 - 5 - 6, topPos - 5 + 40 + 8, 10, 10, true, false));
         buttons.add(new GolemWidgets.Scroll(5, leftPos + 112 - 5 + 22, topPos - 5 + 40 + 8, 10, 10, false, false));
      }
      if (valLegs.size() > 1) {
         buttons.add(new GolemWidgets.Scroll(6, leftPos + 112 - 5 - 6, topPos - 5 + 64 + 8, 10, 10, true, false));
         buttons.add(new GolemWidgets.Scroll(7, leftPos + 112 - 5 + 22, topPos - 5 + 64 + 8, 10, 10, false, false));
      }
      if (valAddons.size() > 1) {
         buttons.add(new GolemWidgets.Scroll(8, leftPos + 16 - 5 - 6, topPos - 5 + 64 + 8, 10, 10, true, false));
         buttons.add(new GolemWidgets.Scroll(9, leftPos + 16 - 5 + 22, topPos - 5 + 64 + 8, 10, 10, false, false));
      }
      if (!valHeads.isEmpty()) {
         GolemHead h = valHeads.get(headIndex);
         buttons.add(new GolemWidgets.Hover(100, leftPos + 120, topPos + 24, 16, 16, h.getLocalizedName(), h.getLocalizedDescription(), h.icon));
      }
      if (!valMats.isEmpty()) {
         GolemMaterial m = valMats.get(matIndex);
         buttons.add(new GolemWidgets.Hover(101, leftPos + 24, topPos + 24, 16, 16, m.getLocalizedName(), m.getLocalizedDescription(), MAT_ICON,
            m.itemColor));
      }
      if (!valArms.isEmpty()) {
         GolemArm a = valArms.get(armIndex);
         buttons.add(new GolemWidgets.Hover(102, leftPos + 120, topPos + 48, 16, 16, a.getLocalizedName(), a.getLocalizedDescription(), a.icon));
      }
      if (!valLegs.isEmpty()) {
         GolemLeg l = valLegs.get(legIndex);
         buttons.add(new GolemWidgets.Hover(103, leftPos + 120, topPos + 72, 16, 16, l.getLocalizedName(), l.getLocalizedDescription(), l.icon));
      }
      if (!valAddons.isEmpty() && !valAddons.get(addonIndex).key.equalsIgnoreCase("none")) {
         GolemAddon a = valAddons.get(addonIndex);
         buttons.add(new GolemWidgets.Hover(103, leftPos + 24, topPos + 72, 16, 16, a.getLocalizedName(), a.getLocalizedDescription(), a.icon));
      }
      props = GolemProperties.fromLong(0L);
      if (!complete()) {
         components = null;
         craftButton.enabled = false;
         return;
      }
      props.setHead(valHeads.get(headIndex));
      props.setMaterial(valMats.get(matIndex));
      props.setArms(valArms.get(armIndex));
      props.setLegs(valLegs.get(legIndex));
      props.setAddon(valAddons.get(addonIndex));
      GolemPayloads.sendToServer(new GolemPayloads.BuilderCheck(builder.getBlockPos(), props.toLong()));
      redoComps();
      EnumGolemTrait[] tags = props.getTraits().toArray(new EnumGolemTrait[0]);
      if (tags.length > 0) {
         int yy = tags.length <= 4 ? (tags.length - 1) % 4 * 8 : 24;
         int xx = (tags.length - 1) / 4 % 4 * 8;
         int i = 0;
         int q = 0;
         int z = 0;
         for (EnumGolemTrait tag : tags) {
            buttons.add(new GolemWidgets.Hover(30 + z, leftPos + 72 + q * 16 - xx, topPos + 48 + 16 * i - yy, 16, 16, tag.getLocalizedName(),
               tag.getLocalizedDescription(), tag.icon));
            if (++i > 3) {
               i = 0;
               q++;
            }
            z++;
         }
      }
      int hh = 10 + props.getMaterial().healthMod;
      if (props.hasTrait(EnumGolemTrait.FRAGILE)) hh = (int) (hh * 0.75);
      hearts = hh / 2.0F;
      int aa = props.getMaterial().armor;
      if (props.hasTrait(EnumGolemTrait.ARMORED)) aa = (int) Math.max(aa * 1.5, aa + 1);
      if (props.hasTrait(EnumGolemTrait.FRAGILE)) aa = (int) (aa * 0.75);
      armor = aa / 2.0F;
      double dd = props.hasTrait(EnumGolemTrait.FIGHTER) ? props.getMaterial().damage : 0.0;
      if (props.hasTrait(EnumGolemTrait.BRUTAL)) dd = Math.max(dd * 1.5, dd + 1.0);
      damage = (float) (dd / 2.0);
   }

   private void redoComps() {
      if (!complete()) return;
      allfound = true;
      cost = props.getTraits().size() * 2;
      components = props.generateComponents();
      buttons.removeIf(b -> b.id >= 10 && b.id < 30);
      if (components.length >= 1) {
         owns = new boolean[components.length];
         for (int a = 0; a < components.length; a++) {
            cost += components[a].getCount();
            owns[a] = builder.hasStuff != null && builder.hasStuff.length > a && builder.hasStuff[a];
            if (!owns[a]) owns[a] = GolemInvHelper.isPlayerCarryingAmount(player, components[a], true);
            if (!owns[a]) allfound = false;
         }
         buttons.add(new GolemWidgets.Hover(10, leftPos + 152, topPos + 24, 16, 16, Component.literal(Aspect.MECHANISM.getName()),
            Aspect.MECHANISM.getLocalizedDescription(), Aspect.MECHANISM));
         int i = 1;
         int q = 0;
         int z = 0;
         for (ItemStack stack : components) {
            buttons.add(new GolemWidgets.Hover(11 + z, leftPos + 152 + q * 16, topPos + 24 + 16 * i, 16, 16, stack.getHoverName(), null, stack));
            if (++i > 3) {
               i = 0;
               q++;
            }
            z++;
         }
      }
      for (GolemWidgets.Btn b : buttons) {
         b.enabled = !disableAll;
         if (!disableAll && b == craftButton) craftButton.enabled = allfound;
      }
   }

   @Override
   protected void renderBg(GuiGraphics g, float pt, int mouseX, int mouseY) {
      GuiDraw.resetColor();
      GolemWidgets.rect(g, GolemWidgets.BUILDER, leftPos, topPos, 0, 0, imageWidth, imageHeight);
      if (components != null && components.length > 0 && owns != null) {
         int i = 1;
         int q = 0;
         GuiDraw.color(1.0F, 1.0F, 1.0F, 0.5F);
         for (int a = 0; a < components.length; a++) {
            if (!owns[a]) GolemWidgets.rect(g, GolemWidgets.BUILDER, leftPos + 144 + q * 16, topPos + 16 + 16 * i, 240, 0, 16, 16);
            if (++i > 3) {
               i = 0;
               q++;
            }
         }
         GuiDraw.resetColor();
      }
      if (builder.cost > 0) {
         int w = builder.maxCost <= 0 ? 0 : (int) (46.0F * (1.0F - (float) builder.cost / builder.maxCost));
         GolemWidgets.rect(g, GolemWidgets.BUILDER, leftPos + 145, topPos + 89, 209, 89, w, 6);
         if (!disableAll) {
            disableAll = true;
            redoComps();
         }
      } else if (disableAll) {
         disableAll = false;
         redoComps();
      }
      g.drawCenteredString(font, String.valueOf(hearts), leftPos + 48, topPos + 108, 0xFFFFFF);
      g.drawCenteredString(font, String.valueOf(armor), leftPos + 72, topPos + 108, 0xFFFFFF);
      g.drawCenteredString(font, String.valueOf(damage), leftPos + 97, topPos + 108, 0xFFFFFF);
      for (GolemWidgets.Btn b : buttons) b.render(g, font, mouseX, mouseY);
      GuiDraw.resetColor();
   }

   @Override
   protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
      if (components != null && components.length > 0) {
         String s = String.valueOf(cost);
         g.drawString(font, s, 162 - font.width(s), 24, 0xFFFFFF);
      }
      for (int[] f : new int[][]{{12, 12}, {12, 60}, {108, 12}, {108, 36}, {108, 60}}) {
         GolemWidgets.rect(g, GolemWidgets.BUILDER, f[0], f[1], 228, 124, 24, 24);
      }
      if (GolemBuilderMenu.redo) {
         redoComps();
         GolemBuilderMenu.redo = false;
      }
   }

   @Override
   public void render(GuiGraphics g, int mouseX, int mouseY, float pt) {
      super.render(g, mouseX, mouseY, pt);
      for (GolemWidgets.Btn b : buttons) {
         if (b.isOver(mouseX, mouseY)) {
            g.pose().pushPose();
            g.pose().translate(0, 0, 400);
            b.foreground(g, font, mouseX, mouseY);
            g.pose().popPose();
            break;
         }
      }
      renderTooltip(g, mouseX, mouseY);
   }

   @Override
   public boolean mouseClicked(double mx, double my, int button) {
      if (button == 0) {
         for (GolemWidgets.Btn b : new ArrayList<>(buttons)) {
            if (b.clicks(mx, my)) {
               minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
               actionPerformed(b.id);
               return true;
            }
         }
      }
      return super.mouseClicked(mx, my, button);
   }

   private static int cycle(int index, int delta, int size) {
      int i = index + delta;
      if (i < 0) i = size - 1;
      if (i >= size) i = 0;
      return i;
   }

   private void actionPerformed(int id) {
      switch (id) {
         case 0, 1 -> headIndex = cycle(headIndex, id == 0 ? -1 : 1, valHeads.size());
         case 2, 3 -> matIndex = cycle(matIndex, id == 2 ? -1 : 1, valMats.size());
         case 4, 5 -> armIndex = cycle(armIndex, id == 4 ? -1 : 1, valArms.size());
         case 6, 7 -> legIndex = cycle(legIndex, id == 6 ? -1 : 1, valLegs.size());
         case 8, 9 -> addonIndex = cycle(addonIndex, id == 8 ? -1 : 1, valAddons.size());
         case 99 -> {
            if (allfound && complete()) {
               GolemPayloads.sendToServer(new GolemPayloads.BuilderCraft(builder.getBlockPos(), props.toLong()));
               minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 99);
               disableAll = true;
            }
            return;
         }
         default -> {
            return;
         }
      }
      gatherInfo();
   }
}
