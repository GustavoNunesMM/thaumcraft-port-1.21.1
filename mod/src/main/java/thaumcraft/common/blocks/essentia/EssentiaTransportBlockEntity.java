package thaumcraft.common.blocks.essentia;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.IEssentiaTransport;
import thaumcraft.common.blocks.DeviceBlock;
import thaumcraft.common.blocks.TCBlockEntity;
import thaumcraft.common.lib.EssentiaHandler;
import thaumcraft.common.registers.ModBlockEntities;

/**
 * TileEssentiaInput / TileEssentiaOutput (essentia transfusers): the filling one takes essentia from its tube and
 * sends it to jars within 16 blocks; the emptying one takes it from those jars and feeds the tube.
 */
public class EssentiaTransportBlockEntity extends TCBlockEntity implements IEssentiaTransport {
   private final boolean input;
   int count;

   public EssentiaTransportBlockEntity(BlockPos pos, BlockState state) {
      super(ModBlockEntities.ESSENTIA_TRANSPORT.get(), pos, state);
      this.input = state.getBlock() instanceof EssentiaTransportBlock b && b.input;
   }

   private Direction facing() {
      return DeviceBlock.getFacing(getBlockState());
   }

   @Override
   public boolean isConnectable(Direction face) {
      return face == facing().getOpposite();
   }

   @Override
   public boolean canInputFrom(Direction face) {
      return input && face == facing().getOpposite();
   }

   @Override
   public boolean canOutputTo(Direction face) {
      return !input && face == facing().getOpposite();
   }

   @Override
   public void setSuction(Aspect aspect, int amount) {
   }

   @Override
   public int getMinimumSuction() {
      return 0;
   }

   @Override
   public Aspect getSuctionType(Direction loc) {
      return null;
   }

   @Override
   public int getSuctionAmount(Direction loc) {
      return input ? 128 : 0;
   }

   @Override
   public Aspect getEssentiaType(Direction loc) {
      return null;
   }

   @Override
   public int getEssentiaAmount(Direction loc) {
      return 0;
   }

   @Override
   public int takeEssentia(Aspect aspect, int amount, Direction face) {
      return 0;
   }

   @Override
   public int addEssentia(Aspect aspect, int amount, Direction face) {
      return amount;
   }

   public static void tick(Level level, BlockPos pos, BlockState state, EssentiaTransportBlockEntity be) {
      if (++be.count % 5 == 0) {
         if (be.input) be.fillJar();
         else be.fillBuffer();
      }
   }

   void fillJar() {
      IEssentiaTransport ic = ThaumcraftApiHelper.getConnectableTile(level, worldPosition, facing().getOpposite());
      if (ic == null || !ic.canOutputTo(facing())) return;
      if (ic.getEssentiaAmount(facing()) > 0 && ic.getSuctionAmount(facing()) < getSuctionAmount(facing().getOpposite())
         && getSuctionAmount(facing().getOpposite()) >= ic.getMinimumSuction()) {
         Aspect ta = ic.getEssentiaType(facing());
         if (ta != null && EssentiaHandler.addEssentia(this, ta, facing(), 16, false, 5)) ic.takeEssentia(ta, 1, facing());
      }
   }

   void fillBuffer() {
      IEssentiaTransport ic = ThaumcraftApiHelper.getConnectableTile(level, worldPosition, facing().getOpposite());
      if (ic == null || !ic.canInputFrom(facing())) return;
      if (ic.getSuctionAmount(facing()) > 0 && ic.getSuctionType(facing()) != null) {
         Aspect ta = ic.getSuctionType(facing());
         if (EssentiaHandler.drainEssentiaWithConfirmation(this, ta, facing(), 16, false, 5) && ic.addEssentia(ta, 1, facing()) > 0) {
            EssentiaHandler.confirmDrain();
         }
      }
   }
}
