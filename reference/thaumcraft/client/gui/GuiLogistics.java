package thaumcraft.client.gui;

import java.io.IOException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;
import thaumcraft.client.gui.plugins.GuiImageButton;
import thaumcraft.client.gui.plugins.GuiPlusMinusButton;
import thaumcraft.client.gui.plugins.GuiScrollButton;
import thaumcraft.client.gui.plugins.GuiSliderTC;
import thaumcraft.common.container.ContainerLogistics;
import thaumcraft.common.lib.SoundsTC;
import thaumcraft.common.lib.network.PacketHandler;
import thaumcraft.common.lib.network.misc.PacketLogisticsRequestToServer;
import thaumcraft.common.lib.network.misc.PacketMiscStringToServer;

@SideOnly(Side.CLIENT)
public class GuiLogistics extends GuiContainer {
   int selectedSlot = -1;
   ContainerLogistics con = null;
   World world;
   EntityPlayer player;
   BlockPos target;
   EnumFacing side;
   ResourceLocation tex = new ResourceLocation("thaumcraft", "textures/gui/gui_logistics.png");
   long lu = 0L;
   int lastStackSize = 1;
   int stackSize = 1;
   boolean stacksizeUpdated = false;
   ItemStack selectedStack = null;
   int lastScrollPos = 0;
   GuiSliderTC scrollbar;
   GuiSliderTC countbar;
   GuiPlusMinusButton countbutton1;
   GuiPlusMinusButton countbutton2;
   GuiImageButton requestbutton;
   GuiTextField searchField;

   public GuiLogistics(InventoryPlayer par1InventoryPlayer, World world, BlockPos pos, EnumFacing side) {
      super(new ContainerLogistics(par1InventoryPlayer, world));
      this.world = world;
      this.player = par1InventoryPlayer.player;
      this.xSize = 215;
      this.ySize = 215;
      this.con = (ContainerLogistics)this.inventorySlots;
      this.target = pos;
      this.side = side;
   }

   public void drawScreen(int mouseX, int mouseY, float partialTicks) {
      this.drawDefaultBackground();
      super.drawScreen(mouseX, mouseY, partialTicks);
      this.renderHoveredToolTip(mouseX, mouseY);
   }

   protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
      long ct = System.currentTimeMillis();
      if (ct > this.lu) {
         this.lu = ct + 1000L;
         this.mc.playerController.sendEnchantPacket(this.inventorySlots.windowId, 22);
      }

      this.mc.renderEngine.bindTexture(this.tex);
      GL11.glEnable(3042);
      if (this.scrollbar != null) {
         if (this.scrollbar.getMax() != this.con.end) {
            this.scrollbar.setMax(this.con.end);
         }

         int sv = Math.round(this.scrollbar.getSliderValue());
         if (sv != this.lastScrollPos) {
            this.lastScrollPos = sv;
            this.mc.playerController.sendEnchantPacket(this.inventorySlots.windowId, 100 + this.lastScrollPos);
         } else if (this.con.updated && sv != this.con.start) {
            this.scrollbar.setSliderValue(this.con.start, false);
            this.con.updated = false;
         }
      }

      this.countbar.visible = this.selectedSlot >= 0;
      this.countbutton1.visible = this.selectedSlot >= 0;
      this.countbutton2.visible = this.selectedSlot >= 0;
      this.requestbutton.visible = this.selectedSlot >= 0;
      if (this.selectedSlot >= 0
         && this.selectedStack != null
         && !this.selectedStack.isEmpty()
         && (
            !this.selectedStack.isItemEqual(this.inventorySlots.getSlot(this.selectedSlot).getStack())
               || !ItemStack.areItemStackTagsEqual(this.selectedStack, this.inventorySlots.getSlot(this.selectedSlot).getStack())
         )) {
         this.selectedSlot = -1;

         for (Slot slot : this.inventorySlots.inventorySlots) {
            if (this.selectedStack.isItemEqual(slot.getStack()) && ItemStack.areItemStackTagsEqual(this.selectedStack, slot.getStack())) {
               this.selectedSlot = slot.slotNumber;
               break;
            }
         }
      }

      if (this.selectedSlot >= 0 && !this.inventorySlots.getSlot(this.selectedSlot).getHasStack()) {
         this.selectedSlot = -1;
      }

      if (this.selectedSlot >= 0
         && this.inventorySlots.getSlot(this.selectedSlot) != null
         && this.inventorySlots.getSlot(this.selectedSlot).getHasStack()) {
         ItemStack stack = this.inventorySlots.getSlot(this.selectedSlot).getStack();
         if (this.countbar.getMax() != stack.getCount()) {
            this.countbar.setMax(stack.getCount());
         }

         int sv = Math.round(this.countbar.getSliderValue());
         if (sv != this.lastStackSize) {
            this.lastStackSize = sv;
            this.stackSize = this.lastStackSize;
         } else if (this.stacksizeUpdated && sv != this.stackSize) {
            this.countbar.setSliderValue(this.stackSize, false);
            this.stacksizeUpdated = false;
         }

         String s = "" + this.stackSize;
         this.fontRenderer.drawString(s, 83 - this.fontRenderer.getStringWidth(s) / 2, 196, 3355443);
      }

      GL11.glDisable(3042);
   }

   protected boolean checkHotbarKeys(int par1) {
      return false;
   }

   protected void drawGuiContainerBackgroundLayer(float par1, int par2, int par3) {
      this.mc.renderEngine.bindTexture(this.tex);
      GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
      GL11.glEnable(3042);
      this.drawTexturedModalRect(this.guiLeft, this.guiTop, 0, 0, this.xSize, this.ySize);
      if (this.selectedSlot >= 0) {
         this.drawTexturedModalRect(this.guiLeft + 17 + this.selectedSlot % 9 * 19, this.guiTop + 17 + this.selectedSlot / 9 * 19, 222, 46, 20, 20);
      }

      GL11.glDisable(3042);
      this.searchField.drawTextBox();
      if (!this.searchField.isFocused() && this.searchField.getText().isEmpty()) {
         this.fontRenderer
            .drawString(
               new TextComponentTranslation("tc.logistics.search", new Object[0]).getFormattedText(),
               this.guiLeft + 146,
               this.guiTop + 197,
               2236962
            );
      }
   }

   public void handleMouseInput() throws IOException {
      super.handleMouseInput();
      int k = Mouse.getDWheel();
      if (k < 0) {
         this.mc.playerController.sendEnchantPacket(this.inventorySlots.windowId, 0);
      } else if (k > 0) {
         this.mc.playerController.sendEnchantPacket(this.inventorySlots.windowId, 1);
      }
   }

   protected void handleMouseClick(Slot slotIn, int slotId, int mouseButton, ClickType type) {
      super.handleMouseClick(slotIn, slotId, mouseButton, type);
      if (slotIn != null && slotId < 81 && slotIn.getHasStack()) {
         Minecraft.getMinecraft().player.playSound(SoundsTC.clack, 0.66F, 1.0F);
         this.selectedSlot = slotId;
         this.selectedStack = slotIn.getStack();
      }
   }

   protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
      super.mouseClicked(mouseX, mouseY, mouseButton);
      this.searchField.mouseClicked(mouseX, mouseY, mouseButton);
   }

   protected void actionPerformed(GuiButton button) throws IOException {
      if (button.id == 1) {
         this.mc.playerController.sendEnchantPacket(this.inventorySlots.windowId, 0);
      }

      if (button.id == 0) {
         this.mc.playerController.sendEnchantPacket(this.inventorySlots.windowId, 1);
      }

      if (this.selectedSlot >= 0
         && this.inventorySlots.getSlot(this.selectedSlot) != null
         && this.inventorySlots.getSlot(this.selectedSlot).getHasStack()) {
         ItemStack stack = this.inventorySlots.getSlot(this.selectedSlot).getStack();
         if (button.id == 2) {
            this.stackSize--;
            if (this.stackSize < 1) {
               this.stackSize = 1;
            }

            this.stacksizeUpdated = true;
         }

         if (button.id == 3) {
            this.stackSize++;
            if (this.stackSize > stack.getCount()) {
               this.stackSize = stack.getCount();
            }

            this.stacksizeUpdated = true;
         }

         if (button.id == 7) {
            ItemStack s2 = stack.copy();
            s2.setCount(1);
            PacketHandler.INSTANCE.sendToServer(new PacketLogisticsRequestToServer(this.target, this.side, s2, this.stackSize));
         }
      }
   }

   public void initGui() {
      super.initGui();
      this.buttonList.clear();
      this.buttonList.add(new GuiScrollButton(0, this.guiLeft + 195, this.guiTop + 16, 10, 10, true, true));
      this.buttonList.add(new GuiScrollButton(1, this.guiLeft + 195, this.guiTop + 180, 10, 10, false, true));
      this.countbutton1 = new GuiPlusMinusButton(2, this.guiLeft + 13, this.guiTop + 195, 10, 10, true);
      this.countbutton2 = new GuiPlusMinusButton(3, this.guiLeft + 57, this.guiTop + 195, 10, 10, false);
      this.buttonList.add(this.countbutton1);
      this.buttonList.add(this.countbutton2);
      this.scrollbar = new GuiSliderTC(
         5, this.guiLeft + 196, this.guiTop + 28, 8, 149, "logistics.scrollbar", 0.0F, this.con.end, this.con.start, true
      );
      this.countbar = new GuiSliderTC(6, this.guiLeft + 24, this.guiTop + 196, 32, 8, "logistics.countbar", 1.0F, 64.0F, 1.0F, false);
      this.buttonList.add(this.scrollbar);
      this.buttonList.add(this.countbar);
      this.requestbutton = new GuiImageButton(
         this,
         7,
         this.guiLeft + 116,
         this.guiTop + 200,
         40,
         13,
         "tc.logistics.request",
         "logistics.request",
         new ResourceLocation("thaumcraft", "textures/gui/gui_base.png"),
         37,
         82,
         40,
         13
      );
      this.buttonList.add(this.requestbutton);
      this.searchField = new GuiTextField(8, this.fontRenderer, this.guiLeft + 143, this.guiTop + 196, 55, this.fontRenderer.FONT_HEIGHT);
      this.searchField.setMaxStringLength(10);
      this.searchField.setEnableBackgroundDrawing(true);
      this.searchField.setVisible(false);
      this.searchField.setTextColor(16777215);
      this.searchField.setVisible(true);
      this.searchField.setCanLoseFocus(true);
      this.searchField.setText("");
      Keyboard.enableRepeatEvents(true);
   }

   public void onGuiClosed() {
      super.onGuiClosed();
      Keyboard.enableRepeatEvents(false);
   }

   protected void keyTyped(char typedChar, int keyCode) throws IOException {
      if (this.searchField.textboxKeyTyped(typedChar, keyCode)) {
         PacketHandler.INSTANCE.sendToServer(new PacketMiscStringToServer(0, this.searchField.getText()));
      } else {
         super.keyTyped(typedChar, keyCode);
      }
   }
}
