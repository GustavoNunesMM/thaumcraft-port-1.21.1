package thaumcraft.common.lib.network.misc;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.IThreadListener;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import thaumcraft.api.golems.GolemHelper;
import thaumcraft.common.lib.utils.Utils;

public class PacketLogisticsRequestToServer implements IMessage, IMessageHandler<PacketLogisticsRequestToServer, IMessage> {
   private BlockPos pos;
   private ItemStack stack;
   private EnumFacing side;
   private int stacksize;

   public PacketLogisticsRequestToServer() {
   }

   public PacketLogisticsRequestToServer(BlockPos pos, EnumFacing side, ItemStack stack, int size) {
      this.pos = pos;
      this.stack = stack;
      this.side = side;
      this.stacksize = size;
   }

   public void toBytes(ByteBuf buffer) {
      if (this.pos != null && this.side != null) {
         buffer.writeBoolean(true);
         buffer.writeLong(this.pos.toLong());
         buffer.writeByte(this.side.getIndex());
      } else {
         buffer.writeBoolean(false);
      }

      Utils.writeItemStackToBuffer(buffer, this.stack);
      buffer.writeInt(this.stacksize);
   }

   public void fromBytes(ByteBuf buffer) {
      if (buffer.readBoolean()) {
         this.pos = BlockPos.fromLong(buffer.readLong());
         this.side = EnumFacing.values()[buffer.readByte()];
      }

      this.stack = Utils.readItemStackFromBuffer(buffer);
      this.stacksize = buffer.readInt();
   }

   public IMessage onMessage(final PacketLogisticsRequestToServer message, final MessageContext ctx) {
      IThreadListener mainThread = ctx.getServerHandler().player.getServerWorld();
      mainThread.addScheduledTask(new Runnable() {
         @Override
         public void run() {
            World world = ctx.getServerHandler().player.getServerWorld();
            Entity player = ctx.getServerHandler().player;

            for (int ui = 0; message.stacksize > 0; ui++) {
               ItemStack s = message.stack.copy();
               s.setCount(Math.min(message.stacksize, s.getMaxStackSize()));
               PacketLogisticsRequestToServer var5 = message;
               var5.stacksize = var5.stacksize - s.getCount();
               if (message.pos != null) {
                  GolemHelper.requestProvisioning(world, message.pos, message.side, s, ui);
               } else {
                  GolemHelper.requestProvisioning(world, ctx.getServerHandler().player, s, ui);
               }
            }
         }
      });
      return null;
   }
}
