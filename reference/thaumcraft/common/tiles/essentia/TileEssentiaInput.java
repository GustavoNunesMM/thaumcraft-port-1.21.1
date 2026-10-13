package thaumcraft.common.tiles.essentia;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.IEssentiaTransport;
import thaumcraft.common.lib.events.EssentiaHandler;
import thaumcraft.common.tiles.TileThaumcraft;

public class TileEssentiaInput extends TileThaumcraft implements IEssentiaTransport, ITickable {
   int count = 0;

   @Override
   public boolean isConnectable(EnumFacing face) {
      return face == this.getFacing().getOpposite();
   }

   @Override
   public boolean canInputFrom(EnumFacing face) {
      return face == this.getFacing().getOpposite();
   }

   @Override
   public boolean canOutputTo(EnumFacing face) {
      return false;
   }

   @Override
   public void setSuction(Aspect aspect, int amount) {
   }

   @Override
   public int getMinimumSuction() {
      return 0;
   }

   @Override
   public Aspect getSuctionType(EnumFacing loc) {
      return null;
   }

   @Override
   public int getSuctionAmount(EnumFacing loc) {
      return 128;
   }

   @Override
   public Aspect getEssentiaType(EnumFacing loc) {
      return null;
   }

   @Override
   public int getEssentiaAmount(EnumFacing loc) {
      return 0;
   }

   @Override
   public int takeEssentia(Aspect aspect, int amount, EnumFacing face) {
      return 0;
   }

   @Override
   public int addEssentia(Aspect aspect, int amount, EnumFacing face) {
      return amount;
   }

   public void update() {
      if (!this.world.isRemote && ++this.count % 5 == 0) {
         this.fillJar();
      }
   }

   void fillJar() {
      TileEntity te = ThaumcraftApiHelper.getConnectableTile(this.world, this.getPos(), this.getFacing().getOpposite());
      if (te != null) {
         IEssentiaTransport ic = (IEssentiaTransport)te;
         if (!ic.canOutputTo(this.getFacing())) {
            return;
         }

         if (ic.getEssentiaAmount(this.getFacing()) > 0
            && ic.getSuctionAmount(this.getFacing()) < this.getSuctionAmount(this.getFacing().getOpposite())
            && this.getSuctionAmount(this.getFacing().getOpposite()) >= ic.getMinimumSuction()) {
            Aspect ta = ic.getEssentiaType(this.getFacing());
            if (EssentiaHandler.addEssentia(this, ta, this.getFacing(), 16, false, 5)) {
               ic.takeEssentia(ta, 1, this.getFacing());
            }
         }
      }
   }
}
