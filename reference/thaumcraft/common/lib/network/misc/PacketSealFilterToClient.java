package thaumcraft.common.lib.network.misc;

import io.netty.buffer.ByteBuf;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import thaumcraft.Thaumcraft;
import thaumcraft.api.golems.seals.ISealConfigFilter;
import thaumcraft.api.golems.seals.ISealEntity;
import thaumcraft.api.golems.seals.SealPos;
import thaumcraft.common.golems.seals.SealHandler;
import thaumcraft.common.lib.utils.Utils;

public class PacketSealFilterToClient implements IMessage, IMessageHandler<PacketSealFilterToClient, IMessage> {
   BlockPos pos;
   EnumFacing face;
   byte filtersize;
   NonNullList<ItemStack> filter;
   NonNullList<Integer> filterStackSize;

   public PacketSealFilterToClient() {
   }

   public PacketSealFilterToClient(ISealEntity se) {
      this.pos = se.getSealPos().pos;
      this.face = se.getSealPos().face;
      if (se.getSeal() != null && se.getSeal() instanceof ISealConfigFilter) {
         ISealConfigFilter cp = (ISealConfigFilter)se.getSeal();
         this.filtersize = (byte)cp.getFilterSize();
         this.filter = cp.getInv();
         this.filterStackSize = cp.getSizes();
      }
   }

   public void toBytes(ByteBuf dos) {
      dos.writeLong(this.pos.toLong());
      dos.writeByte(this.face.ordinal());
      dos.writeByte(this.filtersize);

      for (int a = 0; a < this.filtersize; a++) {
         Utils.writeItemStackToBuffer(dos, (ItemStack)this.filter.get(a));
         dos.writeShort((Integer)this.filterStackSize.get(a));
      }
   }

   public void fromBytes(ByteBuf dat) {
      this.pos = BlockPos.fromLong(dat.readLong());
      this.face = EnumFacing.VALUES[dat.readByte()];
      this.filtersize = dat.readByte();
      this.filter = NonNullList.withSize(this.filtersize, ItemStack.EMPTY);
      this.filterStackSize = NonNullList.withSize(this.filtersize, 0);

      for (int a = 0; a < this.filtersize; a++) {
         this.filter.set(a, Utils.readItemStackFromBuffer(dat));
         this.filterStackSize.set(a, Integer.valueOf(dat.readShort()));
      }
   }

   public IMessage onMessage(PacketSealFilterToClient message, MessageContext ctx) {
      try {
         ISealEntity seal = SealHandler.getSealEntity(Thaumcraft.proxy.getClientWorld().provider.getDimension(), new SealPos(message.pos, message.face));
         if (seal != null && seal.getSeal() instanceof ISealConfigFilter) {
            ISealConfigFilter cp = (ISealConfigFilter)seal.getSeal();

            for (int a = 0; a < message.filtersize; a++) {
               cp.setFilterSlot(a, (ItemStack)message.filter.get(a));
               cp.setFilterSlotSize(a, (Integer)message.filterStackSize.get(a));
            }
         }
      } catch (Exception var6) {
         var6.printStackTrace();
      }

      return null;
   }
}
