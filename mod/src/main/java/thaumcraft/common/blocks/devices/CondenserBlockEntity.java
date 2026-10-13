package thaumcraft.common.blocks.devices;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.IEssentiaTransport;
import thaumcraft.common.blocks.DeviceBlock;
import thaumcraft.common.blocks.TCBlockEntity;
import thaumcraft.common.registers.ModFlux;
import thaumcraft.common.world.aura.AuraHandler;

/**
 * TileCondenser: pulls essentia (any kind but vitium) from its sides and spends it to pull flux out of the aura through
 * the lattice above it, turning each point into vitium essentia that can be drawn from the bottom. More (unclogged)
 * lattice makes it faster; every lattice piece adds to the cost; now and then a lattice piece clogs.
 */
public class CondenserBlockEntity extends TCBlockEntity implements IEssentiaTransport {
   public static final int MAX = 100;
   private int essentia;
   private int flux;
   private int count;
   private final List<Long> history = new ArrayList<>();
   private final List<Long> blockList = new ArrayList<>();
   private final List<Long> uncloggedList = new ArrayList<>();
   public float latticeCount = -1.0F;
   public int interval;
   public int cost;

   public CondenserBlockEntity(BlockPos pos, BlockState state) {
      super(ModFlux.CONDENSER_BE.get(), pos, state);
   }

   public int getEssentia() {
      return essentia;
   }

   public int getFlux() {
      return flux;
   }

   /** For tests and the resonator: sets the stored (non-vitium) essentia. */
   public void setEssentia(int amount) {
      essentia = amount;
      sync();
   }

   @Override
   protected void readSync(CompoundTag tag, HolderLookup.Provider registries) {
      essentia = tag.getShort("essentia");
      flux = tag.getShort("flux");
   }

   @Override
   protected void writeSync(CompoundTag tag, HolderLookup.Provider registries) {
      tag.putShort("essentia", (short) essentia);
      tag.putShort("flux", (short) flux);
   }

   public static void tick(Level level, BlockPos pos, BlockState state, CondenserBlockEntity be) {
      if (be.latticeCount < 0.0F) be.triggerCheck();
      be.count++;
      if (!DeviceBlock.isEnabled(state) || be.latticeCount <= 0.0F) return;
      if (level.isClientSide) {
         if (be.essentia > 0 && !be.uncloggedList.isEmpty() && be.count % Math.max(3, be.interval / 50) == 0) {
            BlockPos p = BlockPos.of(be.uncloggedList.get(level.random.nextInt(be.uncloggedList.size())));
            thaumcraft.client.fx.TaintFX.spark(p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, 4.5F + level.random.nextFloat(),
               0.33F + level.random.nextFloat() * 0.66F, 0.33F + level.random.nextFloat() * 0.66F, 0.33F + level.random.nextFloat() * 0.66F, 0.8F);
         }
         return;
      }
      if (be.count % 5 == 0 && be.essentia < MAX) be.fill();
      if (be.interval > 0 && be.essentia >= be.cost && be.flux < MAX && be.count % be.interval == 0 && AuraHandler.getFlux(level, pos) >= 1.0F) {
         AuraHandler.drainFlux(level, pos, 1.0F, false);
         be.essentia -= be.cost;
         be.flux++;
         if (level.random.nextInt(50) == 0) be.makeLatticeDirty();
         be.sync();
      }
   }

   private void makeLatticeDirty() {
      if (uncloggedList.isEmpty()) return;
      int q = level.random.nextInt(uncloggedList.size());
      if (q == 0) q = level.random.nextInt(uncloggedList.size());
      BlockPos p = BlockPos.of(uncloggedList.get(q));
      if (level.getBlockState(p).is(ModFlux.CONDENSER_LATTICE.get())) {
         level.setBlock(p, ModFlux.CONDENSER_LATTICE_DIRTY.get().defaultBlockState(), 3);
         ModFlux.CONDENSER_LATTICE_DIRTY.get().triggerUpdate(level, p);
      }
   }

   private void fill() {
      for (Direction face : Direction.Plane.HORIZONTAL) {
         IEssentiaTransport ic = ThaumcraftApiHelper.getConnectableTile(level, worldPosition, face);
         if (ic == null) continue;
         if (!ic.canOutputTo(face.getOpposite())) return;
         Aspect ta = null;
         if (ic.getEssentiaAmount(face.getOpposite()) > 0 && ic.getSuctionAmount(face.getOpposite()) < getSuctionAmount(face)
            && getSuctionAmount(face) >= ic.getMinimumSuction()) {
            ta = ic.getEssentiaType(face.getOpposite());
         }
         if (ta == null) continue;
         if (ta != Aspect.FLUX) essentia += ic.takeEssentia(ta, 1, face.getOpposite());
         else makeLatticeDirty();
         sync();
         if (essentia >= MAX) return;
      }
   }

   /** Recounts the lattice above (also called when a lattice piece changes). */
   public void triggerCheck() {
      history.clear();
      blockList.clear();
      uncloggedList.clear();
      latticeCount = 0.0F;
      interval = 0;
      performCheck(worldPosition, true, false);
      history.clear();
      if (latticeCount <= 0.0F) {
         latticeCount = 0.0F;
      } else {
         if (latticeCount > 40.0F) latticeCount = 40.0F;
         interval = Math.max(5, Math.round(600.0F - latticeCount * 15.0F));
         cost = (int) (4.0 + Math.sqrt(blockList.size()));
      }
   }

   private void performCheck(BlockPos pos, boolean skip, boolean clogged) {
      if (latticeCount < 0.0F) return;
      history.add(pos.asLong());
      boolean found = false;
      int sides = 0;
      for (Direction face : Direction.values()) {
         if (skip && face != Direction.UP) continue;
         BlockPos p2 = pos.relative(face);
         BlockState bs = level.getBlockState(p2);
         boolean lattice = bs.is(ModFlux.CONDENSER_LATTICE.get());
         boolean latticeDirty = bs.is(ModFlux.CONDENSER_LATTICE_DIRTY.get());
         if (skip && latticeDirty) clogged = true;
         if (lattice || latticeDirty) sides++;
         if (history.contains(p2.asLong())) continue;
         if (face == Direction.DOWN && bs.is(ModFlux.CONDENSER.get())) {
            latticeCount = -99.0F;
            return;
         }
         if (worldPosition.getY() < p2.getY() && !(worldPosition.distSqr(p2) > 74.0) && (lattice || latticeDirty)) {
            blockList.add(p2.asLong());
            if (lattice) uncloggedList.add(p2.asLong());
            found = true;
            performCheck(p2, false, clogged || latticeDirty);
            if (skip) break;
         }
      }
      if (found && !clogged) latticeCount += 1.0F - 0.15F * sides;
   }

   // ------------------------------------------------------------------ IEssentiaTransport

   @Override
   public boolean isConnectable(Direction face) {
      return face != Direction.UP;
   }

   @Override
   public boolean canInputFrom(Direction face) {
      return face != Direction.UP && face != Direction.DOWN;
   }

   @Override
   public boolean canOutputTo(Direction face) {
      return face == Direction.DOWN;
   }

   @Override
   public void setSuction(@Nullable Aspect aspect, int amount) {
   }

   @Nullable
   @Override
   public Aspect getSuctionType(@Nullable Direction face) {
      return null;
   }

   @Override
   public int getSuctionAmount(@Nullable Direction face) {
      return face != Direction.DOWN && essentia < MAX ? 128 : 0;
   }

   @Override
   public int takeEssentia(Aspect aspect, int amount, @Nullable Direction face) {
      int amt = !canOutputTo(face) || aspect != null && aspect != Aspect.FLUX ? 0 : Math.min(amount, flux);
      if (amt > 0) {
         flux -= amt;
         sync();
      }
      return amt;
   }

   /** As in 1.12 the condenser only takes essentia by pulling it (fill); pushes are acknowledged but not stored. */
   @Override
   public int addEssentia(Aspect aspect, int amount, @Nullable Direction face) {
      int amt = canInputFrom(face) ? Math.min(amount, MAX - essentia) : 0;
      if (amt > 0) sync();
      return amt;
   }

   @Override
   public Aspect getEssentiaType(@Nullable Direction face) {
      return Aspect.FLUX;
   }

   @Override
   public int getEssentiaAmount(@Nullable Direction face) {
      return flux;
   }

   @Override
   public int getMinimumSuction() {
      return 0;
   }
}
