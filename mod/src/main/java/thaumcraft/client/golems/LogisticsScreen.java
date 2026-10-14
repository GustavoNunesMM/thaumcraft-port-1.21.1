package thaumcraft.client.golems;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;
import thaumcraft.Thaumcraft;
import thaumcraft.client.gui.GuiDraw;
import thaumcraft.common.golems.GolemPayloads;
import thaumcraft.common.golems.menu.LogisticsMenu;
import thaumcraft.common.registers.ModSounds;

/**
 * GuiLogistics: browse what the provider seals nearby hold, pick a stack and an amount, and ask golems to bring it to
 * the player (or to the inventory the bell was used on).
 */
public class LogisticsScreen extends AbstractContainerScreen<LogisticsMenu> {
   private static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/gui/gui_logistics.png");
   private final List<GolemWidgets.Btn> buttons = new ArrayList<>();
   private int selectedSlot = -1;
   private long lu = 0L;
   private int lastStackSize = 1;
   private int stackSize = 1;
   private boolean stacksizeUpdated = false;
   private ItemStack selectedStack = ItemStack.EMPTY;
   private int lastScrollPos = 0;
   private GolemWidgets.Slider scrollbar;
   private GolemWidgets.Slider countbar;
   private GolemWidgets.PlusMinus countbutton1;
   private GolemWidgets.PlusMinus countbutton2;
   private GolemWidgets.Image requestbutton;
   private EditBox searchField;

   public LogisticsScreen(LogisticsMenu menu, Inventory inv, Component title) {
      super(menu, inv, title);
      this.imageWidth = 215;
      this.imageHeight = 215;
   }

   @Override
   protected void init() {
      super.init();
      buttons.clear();
      buttons.add(new GolemWidgets.Scroll(0, leftPos + 195, topPos + 16, 10, 10, true, true));
      buttons.add(new GolemWidgets.Scroll(1, leftPos + 195, topPos + 180, 10, 10, false, true));
      countbutton1 = new GolemWidgets.PlusMinus(2, leftPos + 13, topPos + 195, 10, 10, true);
      countbutton2 = new GolemWidgets.PlusMinus(3, leftPos + 57, topPos + 195, 10, 10, false);
      buttons.add(countbutton1);
      buttons.add(countbutton2);
      scrollbar = new GolemWidgets.Slider(5, leftPos + 196, topPos + 28, 8, 149, 0.0F, menu.end, menu.start, true);
      countbar = new GolemWidgets.Slider(6, leftPos + 24, topPos + 196, 32, 8, 1.0F, 64.0F, 1.0F, false);
      buttons.add(scrollbar);
      buttons.add(countbar);
      requestbutton = new GolemWidgets.Image(7, leftPos + 116, topPos + 200, 40, 13, Component.translatable("tc.logistics.request"), null,
         GolemWidgets.BASE, 37, 82, 40, 13);
      buttons.add(requestbutton);
      searchField = new EditBox(font, leftPos + 143, topPos + 196, 55, font.lineHeight, Component.translatable("tc.logistics.search"));
      searchField.setMaxLength(10);
      searchField.setBordered(true);
      searchField.setTextColor(0xFFFFFF);
      searchField.setCanLoseFocus(true);
      searchField.setValue("");
      searchField.setResponder(text -> GolemPayloads.sendToServer(new GolemPayloads.LogisticsSearch(text)));
      addWidget(searchField);
   }

   private void send(int id) {
      minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
   }

   @Override
   protected boolean checkHotbarKeyPressed(int keyCode, int scanCode) {
      return false;
   }

   @Override
   protected void renderBg(GuiGraphics g, float pt, int mouseX, int mouseY) {
      GuiDraw.resetColor();
      GolemWidgets.rect(g, TEX, leftPos, topPos, 0, 0, imageWidth, imageHeight);
      if (selectedSlot >= 0) {
         GolemWidgets.rect(g, TEX, leftPos + 17 + selectedSlot % 9 * 19, topPos + 17 + selectedSlot / 9 * 19, 222, 46, 20, 20);
      }
      for (GolemWidgets.Btn b : buttons) b.render(g, font, mouseX, mouseY);
      GuiDraw.resetColor();
      searchField.render(g, mouseX, mouseY, pt);
      if (!searchField.isFocused() && searchField.getValue().isEmpty()) {
         g.drawString(font, Component.translatable("tc.logistics.search"), leftPos + 146, topPos + 197, 0x222222, false);
      }
   }

   @Override
   protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
      long ct = System.currentTimeMillis();
      if (ct > lu) {
         lu = ct + 1000L;
         send(22);
      }
      if (scrollbar != null) {
         if (scrollbar.getMax() != menu.end) scrollbar.setMax(menu.end);
         int sv = Math.round(scrollbar.getSliderValue());
         if (sv != lastScrollPos) {
            lastScrollPos = sv;
            send(100 + lastScrollPos);
         } else if (menu.updated && sv != menu.start) {
            scrollbar.setSliderValue(menu.start);
            menu.updated = false;
         }
      }
      boolean sel = selectedSlot >= 0;
      countbar.visible = sel;
      countbutton1.visible = sel;
      countbutton2.visible = sel;
      requestbutton.visible = sel;
      if (selectedSlot >= 0 && !selectedStack.isEmpty() && !ItemStack.isSameItemSameComponents(selectedStack, menu.getSlot(selectedSlot).getItem())) {
         selectedSlot = -1;
         for (Slot slot : menu.slots) {
            if (slot.index < 81 && ItemStack.isSameItemSameComponents(selectedStack, slot.getItem())) {
               selectedSlot = slot.index;
               break;
            }
         }
      }
      if (selectedSlot >= 0 && !menu.getSlot(selectedSlot).hasItem()) selectedSlot = -1;
      if (selectedSlot >= 0) {
         ItemStack stack = menu.getSlot(selectedSlot).getItem();
         if (countbar.getMax() != stack.getCount()) countbar.setMax(stack.getCount());
         int sv = Math.round(countbar.getSliderValue());
         if (sv != lastStackSize) {
            lastStackSize = sv;
            stackSize = lastStackSize;
         } else if (stacksizeUpdated && sv != stackSize) {
            countbar.setSliderValue(stackSize);
            stacksizeUpdated = false;
         }
         String s = String.valueOf(stackSize);
         g.drawString(font, s, 83 - font.width(s) / 2, 196, 0x333333, false);
      }
   }

   @Override
   public void render(GuiGraphics g, int mouseX, int mouseY, float pt) {
      super.render(g, mouseX, mouseY, pt);
      for (GolemWidgets.Btn b : buttons) {
         if (b == requestbutton && b.isOver(mouseX, mouseY)) {
            g.pose().pushPose();
            g.pose().translate(0, 0, 400);
            b.foreground(g, font, mouseX, mouseY);
            g.pose().popPose();
         }
      }
      renderTooltip(g, mouseX, mouseY);
   }

   @Override
   protected void slotClicked(Slot slot, int slotId, int button, ClickType type) {
      super.slotClicked(slot, slotId, button, type);
      if (slot != null && slotId >= 0 && slotId < 81 && slot.hasItem()) {
         minecraft.player.playSound(ModSounds.CLACK.get(), 0.66F, 1.0F);
         selectedSlot = slotId;
         selectedStack = slot.getItem().copy();
      }
   }

   @Override
   public boolean mouseClicked(double mx, double my, int button) {
      if (button == 0) {
         for (GolemWidgets.Btn b : new ArrayList<>(buttons)) {
            if (b.clicks(mx, my)) {
               if (!(b instanceof GolemWidgets.Slider)) {
                  minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
               }
               actionPerformed(b.id);
               searchField.setFocused(false);
               return true;
            }
         }
      }
      boolean inField = searchField.mouseClicked(mx, my, button);
      searchField.setFocused(inField);
      if (inField) return true;
      return super.mouseClicked(mx, my, button);
   }

   @Override
   public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
      for (GolemWidgets.Btn b : buttons) b.mouseDragged(mx, my);
      return super.mouseDragged(mx, my, button, dx, dy);
   }

   @Override
   public boolean mouseReleased(double mx, double my, int button) {
      for (GolemWidgets.Btn b : buttons) b.mouseReleased();
      return super.mouseReleased(mx, my, button);
   }

   @Override
   public boolean mouseScrolled(double mx, double my, double sx, double sy) {
      if (sy < 0) send(0);
      else if (sy > 0) send(1);
      return true;
   }

   @Override
   public boolean keyPressed(int key, int scan, int mods) {
      if (searchField.isFocused() && key != GLFW.GLFW_KEY_ESCAPE) {
         searchField.keyPressed(key, scan, mods);
         return true;
      }
      return super.keyPressed(key, scan, mods);
   }

   @Override
   public boolean charTyped(char c, int mods) {
      if (searchField.isFocused()) return searchField.charTyped(c, mods);
      return super.charTyped(c, mods);
   }

   private void actionPerformed(int id) {
      if (id == 1) send(0);
      if (id == 0) send(1);
      if (selectedSlot >= 0 && menu.getSlot(selectedSlot).hasItem()) {
         ItemStack stack = menu.getSlot(selectedSlot).getItem();
         if (id == 2) {
            stackSize = Math.max(1, stackSize - 1);
            stacksizeUpdated = true;
         }
         if (id == 3) {
            stackSize = Math.min(stack.getCount(), stackSize + 1);
            stacksizeUpdated = true;
         }
         if (id == 7) {
            ItemStack s2 = stack.copyWithCount(1);
            boolean hasTarget = menu.target != null && menu.side != null;
            GolemPayloads.sendToServer(new GolemPayloads.LogisticsRequest(hasTarget ? Optional.of(menu.target) : Optional.empty(),
               hasTarget ? menu.side.get3DDataValue() : 0, s2, stackSize));
         }
      }
   }
}
