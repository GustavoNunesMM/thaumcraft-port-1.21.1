package thaumcraft.common.blocks.essentia;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IAspectContainer;
import thaumcraft.api.aspects.IEssentiaTransport;
import thaumcraft.common.blocks.TCBlockEntity;
import thaumcraft.common.registers.ModBlockEntities;
import thaumcraft.common.registers.ModSounds;

/** TileCentrifuge: takes compound essentia from below and splits each one into one of its two components, output on top. */
public class CentrifugeBlockEntity extends TCBlockEntity implements IAspectContainer, IEssentiaTransport {
   @Nullable
   public Aspect aspectOut;
   @Nullable
   public Aspect aspectIn;
   int count;
   int process;
   float rotationSpeed;
   public float rotation;

   public CentrifugeBlockEntity(BlockPos pos, BlockState state) {
      super(ModBlockEntities.CENTRIFUGE.get(), pos, state);
   }

   @Override
   protected void readSync(CompoundTag tag, HolderLookup.Provider registries) {
      aspectIn = getAspect(tag, "aspectIn");
      aspectOut = getAspect(tag, "aspectOut");
   }

   @Override
   protected void writeSync(CompoundTag tag, HolderLookup.Provider registries) {
      putAspect(tag, "aspectIn", aspectIn);
      putAspect(tag, "aspectOut", aspectOut);
   }

   @Override
   public AspectList getAspects() {
      AspectList al = new AspectList();
      if (aspectOut != null) al.add(aspectOut, 1);
      return al;
   }

   @Override
   public void setAspects(AspectList aspects) {
   }

   @Override
   public int addToContainer(Aspect tt, int am) {
      if (am > 0 && aspectOut == null) {
         aspectOut = tt;
         sync();
         am--;
      }
      return am;
   }

   @Override
   public boolean takeFromContainer(Aspect tt, int am) {
      if (aspectOut != null && tt == aspectOut) {
         aspectOut = null;
         sync();
         return true;
      }
      return false;
   }

   @Override
   public boolean doesContainerContainAmount(Aspect tag, int amt) {
      return amt == 1 && tag == aspectOut;
   }

   @Override
   public int containerContains(Aspect tag) {
      return tag == aspectOut ? 1 : 0;
   }

   @Override
   public boolean doesContainerAccept(Aspect tag) {
      return true;
   }

   @Override
   public boolean isConnectable(Direction face) {
      return face == Direction.UP || face == Direction.DOWN;
   }

   @Override
   public boolean canInputFrom(Direction face) {
      return face == Direction.DOWN;
   }

   @Override
   public boolean canOutputTo(Direction face) {
      return face == Direction.UP;
   }

   @Override
   public void setSuction(Aspect aspect, int amount) {
   }

   @Override
   public int getMinimumSuction() {
      return 0;
   }

   @Override
   public Aspect getSuctionType(Direction face) {
      return null;
   }

   @Override
   public int getSuctionAmount(Direction face) {
      return face == Direction.DOWN ? (gettingPower() ? 0 : aspectIn == null ? 128 : 64) : 0;
   }

   @Override
   public Aspect getEssentiaType(Direction loc) {
      return aspectOut;
   }

   @Override
   public int getEssentiaAmount(Direction loc) {
      return aspectOut != null ? 1 : 0;
   }

   @Override
   public int takeEssentia(Aspect aspect, int amount, Direction face) {
      return canOutputTo(face) && takeFromContainer(aspect, amount) ? amount : 0;
   }

   @Override
   public int addEssentia(Aspect aspect, int amount, Direction face) {
      if (aspectIn == null && !aspect.isPrimal()) {
         aspectIn = aspect;
         process = 39;
         sync();
         return 1;
      }
      return 0;
   }

   public static void tick(Level level, BlockPos pos, BlockState state, CentrifugeBlockEntity be) {
      if (!level.isClientSide) {
         if (!be.gettingPower()) {
            if (be.aspectOut == null && be.aspectIn == null && ++be.count % 5 == 0) be.drawEssentia();
            if (be.process > 0) be.process--;
            if (be.aspectOut == null && be.aspectIn != null && be.process == 0) be.processEssentia();
         }
      } else {
         if (be.aspectIn != null && !be.gettingPower() && be.rotationSpeed < 20.0F) be.rotationSpeed += 2.0F;
         if ((be.aspectIn == null || be.gettingPower()) && be.rotationSpeed > 0.0F) be.rotationSpeed -= 0.5F;
         int pr = (int) be.rotation;
         be.rotation += be.rotationSpeed;
         if (be.rotation % 180.0F <= 20.0F && pr % 180 >= 160 && be.rotationSpeed > 0.0F) {
            level.playLocalSound(pos, ModSounds.PUMP.get(), SoundSource.BLOCKS, 1.0F, 1.0F, false);
         }
      }
   }

   void processEssentia() {
      Aspect[] comps = aspectIn.getComponents();
      aspectOut = comps[level.random.nextInt(2)];
      aspectIn = null;
      sync();
   }

   void drawEssentia() {
      IEssentiaTransport ic = ThaumcraftApiHelper.getConnectableTile(level, worldPosition, Direction.DOWN);
      if (ic == null || !ic.canOutputTo(Direction.UP)) return;
      Aspect ta = null;
      if (ic.getEssentiaAmount(Direction.UP) > 0 && ic.getSuctionAmount(Direction.UP) < getSuctionAmount(Direction.DOWN)
         && getSuctionAmount(Direction.DOWN) >= ic.getMinimumSuction()) {
         ta = ic.getEssentiaType(Direction.UP);
      }
      if (ta != null && !ta.isPrimal() && ic.getSuctionAmount(Direction.UP) < getSuctionAmount(Direction.DOWN) && ic.takeEssentia(ta, 1, Direction.UP) == 1) {
         aspectIn = ta;
         process = 39;
         sync();
      }
   }
}
