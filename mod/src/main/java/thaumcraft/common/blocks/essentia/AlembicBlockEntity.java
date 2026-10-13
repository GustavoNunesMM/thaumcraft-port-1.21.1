package thaumcraft.common.blocks.essentia;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IAspectContainer;
import thaumcraft.api.aspects.IEssentiaTransport;
import thaumcraft.common.blocks.TCBlockEntity;
import thaumcraft.common.registers.ModBlockEntities;

/** TileAlembic: collects up to 128 essentia of one aspect from the smeltery below; tubes take it from the sides and top. */
public class AlembicBlockEntity extends TCBlockEntity implements IAspectContainer, IEssentiaTransport {
   public static final int MAX_AMOUNT = 128;
   @Nullable
   public Aspect aspect;
   @Nullable
   public Aspect aspectFilter;
   public int amount;
   /** The labelled side (DOWN when unlabelled). */
   public Direction facing = Direction.DOWN;

   public AlembicBlockEntity(BlockPos pos, BlockState state) {
      super(ModBlockEntities.ALEMBIC.get(), pos, state);
   }

   @Override
   protected void readSync(CompoundTag tag, HolderLookup.Provider registries) {
      facing = Direction.from3DDataValue(tag.getByte("facing"));
      aspectFilter = getAspect(tag, "AspectFilter");
      aspect = getAspect(tag, "aspect");
      amount = tag.getShort("amount");
   }

   @Override
   protected void writeSync(CompoundTag tag, HolderLookup.Provider registries) {
      putAspect(tag, "aspect", aspect);
      putAspect(tag, "AspectFilter", aspectFilter);
      tag.putShort("amount", (short) amount);
      tag.putByte("facing", (byte) facing.get3DDataValue());
   }

   @Override
   public AspectList getAspects() {
      return aspect != null ? new AspectList().add(aspect, amount) : new AspectList();
   }

   @Override
   public void setAspects(AspectList aspects) {
   }

   @Override
   public int addToContainer(Aspect tt, int am) {
      if (aspectFilter != null && tt != aspectFilter) return am;
      if (amount < MAX_AMOUNT && tt == aspect || amount == 0) {
         aspect = tt;
         int added = Math.min(am, MAX_AMOUNT - amount);
         amount += added;
         am -= added;
      }
      sync();
      return am;
   }

   @Override
   public boolean takeFromContainer(Aspect tt, int am) {
      if (amount == 0 || aspect == null) {
         aspect = null;
         amount = 0;
      }
      if (aspect != null && amount >= am && tt == aspect) {
         amount -= am;
         if (amount <= 0) {
            aspect = null;
            amount = 0;
         }
         sync();
         return true;
      }
      return false;
   }

   @Override
   public boolean doesContainerContainAmount(Aspect tt, int am) {
      return amount >= am && tt == aspect;
   }

   @Override
   public int containerContains(Aspect tt) {
      return tt == aspect ? amount : 0;
   }

   @Override
   public boolean doesContainerAccept(Aspect tag) {
      return true;
   }

   @Override
   public boolean isConnectable(Direction face) {
      return face != facing && face != Direction.DOWN;
   }

   @Override
   public boolean canInputFrom(Direction face) {
      return false;
   }

   @Override
   public boolean canOutputTo(Direction face) {
      return face != facing && face != Direction.DOWN;
   }

   @Override
   public void setSuction(Aspect aspect, int amount) {
   }

   @Override
   public Aspect getSuctionType(Direction loc) {
      return null;
   }

   @Override
   public int getSuctionAmount(Direction loc) {
      return 0;
   }

   @Override
   public Aspect getEssentiaType(Direction loc) {
      return aspect;
   }

   @Override
   public int getEssentiaAmount(Direction loc) {
      return amount;
   }

   @Override
   public int takeEssentia(Aspect aspect, int amount, Direction face) {
      return canOutputTo(face) && takeFromContainer(aspect, amount) ? amount : 0;
   }

   @Override
   public int addEssentia(Aspect aspect, int amount, Direction loc) {
      return 0;
   }

   @Override
   public int getMinimumSuction() {
      return 0;
   }

   /** Puts one essentia into the alembics stacked above pos: first one already holding it, else the first that accepts it. */
   public static boolean processAlembics(Level level, BlockPos pos, Aspect aspect) {
      for (int deep = 1; level.getBlockEntity(pos.above(deep)) instanceof AlembicBlockEntity alembic; deep++) {
         if (alembic.amount > 0 && alembic.aspect == aspect && alembic.addToContainer(aspect, 1) == 0) return true;
      }
      for (int deep = 1; level.getBlockEntity(pos.above(deep)) instanceof AlembicBlockEntity alembic; deep++) {
         if ((alembic.aspectFilter == null || alembic.aspectFilter == aspect) && alembic.addToContainer(aspect, 1) == 0) return true;
      }
      return false;
   }

   public int comparatorSignal() {
      float r = (float) amount / MAX_AMOUNT;
      return (int) Math.floor(r * 14.0F) + (amount > 0 ? 1 : 0);
   }
}
