package thaumcraft.common.lib.network.fx;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import thaumcraft.client.fx.FXDispatcher;

public class PacketFXBlockArc implements IMessage, IMessageHandler<PacketFXBlockArc, IMessage> {
   private int x;
   private int y;
   private int z;
   private float tx;
   private float ty;
   private float tz;
   private float r;
   private float g;
   private float b;

   public PacketFXBlockArc() {
   }

   public PacketFXBlockArc(BlockPos pos, Entity source, float r, float g, float b) {
      this.x = pos.getX();
      this.y = pos.getY();
      this.z = pos.getZ();
      this.tx = (float)source.posX;
      this.ty = (float)(source.getEntityBoundingBox().minY + source.height / 2.0F);
      this.tz = (float)source.posZ;
      this.r = r;
      this.g = g;
      this.b = b;
   }

   public PacketFXBlockArc(BlockPos pos, BlockPos pos2, float r, float g, float b) {
      this.x = pos.getX();
      this.y = pos.getY();
      this.z = pos.getZ();
      this.tx = pos2.getX() + 0.5F;
      this.ty = pos2.getY() + 0.5F;
      this.tz = pos2.getZ() + 0.5F;
      this.r = r;
      this.g = g;
      this.b = b;
   }

   public void toBytes(ByteBuf buffer) {
      buffer.writeInt(this.x);
      buffer.writeInt(this.y);
      buffer.writeInt(this.z);
      buffer.writeFloat(this.tx);
      buffer.writeFloat(this.ty);
      buffer.writeFloat(this.tz);
      buffer.writeFloat(this.r);
      buffer.writeFloat(this.g);
      buffer.writeFloat(this.b);
   }

   public void fromBytes(ByteBuf buffer) {
      this.x = buffer.readInt();
      this.y = buffer.readInt();
      this.z = buffer.readInt();
      this.tx = buffer.readFloat();
      this.ty = buffer.readFloat();
      this.tz = buffer.readFloat();
      this.r = buffer.readFloat();
      this.g = buffer.readFloat();
      this.b = buffer.readFloat();
   }

   public IMessage onMessage(PacketFXBlockArc message, MessageContext ctx) {
      FXDispatcher.INSTANCE
         .arcLightning(message.tx, message.ty, message.tz, message.x + 0.5, message.y + 0.5, message.z + 0.5, message.r, message.g, message.b, 0.5F);
      return null;
   }
}
