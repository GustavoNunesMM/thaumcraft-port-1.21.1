package thaumcraft.common.lib.network.playerdata;

import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.translation.I18n;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import thaumcraft.common.lib.SoundsTC;

public class PacketWarpMessage implements IMessage, IMessageHandler<PacketWarpMessage, IMessage> {
   protected int data = 0;
   protected byte type = 0;

   public PacketWarpMessage() {
   }

   public PacketWarpMessage(EntityPlayer player, byte type, int change) {
      this.data = change;
      this.type = type;
   }

   public void toBytes(ByteBuf buffer) {
      buffer.writeInt(this.data);
      buffer.writeByte(this.type);
   }

   public void fromBytes(ByteBuf buffer) {
      this.data = buffer.readInt();
      this.type = buffer.readByte();
   }

   @SideOnly(Side.CLIENT)
   public IMessage onMessage(final PacketWarpMessage message, MessageContext ctx) {
      if (message.data != 0) {
         Minecraft.getMinecraft().addScheduledTask(new Runnable() {
            @Override
            public void run() {
               PacketWarpMessage.this.processMessage(message);
            }
         });
      }

      return null;
   }

   @SideOnly(Side.CLIENT)
   void processMessage(PacketWarpMessage message) {
      if (message.type == 0 && message.data > 0) {
         String text = I18n.translateToLocal("tc.addwarp");
         if (message.data < 0) {
            text = I18n.translateToLocal("tc.removewarp");
         } else {
            Minecraft.getMinecraft().player.playSound(SoundsTC.whispers, 0.5F, 1.0F);
         }
      } else if (message.type == 1) {
         String text = I18n.translateToLocal("tc.addwarpsticky");
         if (message.data < 0) {
            text = I18n.translateToLocal("tc.removewarpsticky");
         } else {
            Minecraft.getMinecraft().player.playSound(SoundsTC.whispers, 0.5F, 1.0F);
         }

         Minecraft.getMinecraft().player.sendStatusMessage(new TextComponentString(text), true);
      } else if (message.data > 0) {
         String text = I18n.translateToLocal("tc.addwarptemp");
         if (message.data < 0) {
            text = I18n.translateToLocal("tc.removewarptemp");
         }

         Minecraft.getMinecraft().player.sendStatusMessage(new TextComponentString(text), true);
      }
   }
}
