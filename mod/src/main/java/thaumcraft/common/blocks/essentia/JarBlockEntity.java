package thaumcraft.common.blocks.essentia;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IAspectSource;
import thaumcraft.api.aspects.IEssentiaTransport;
import thaumcraft.common.blocks.TCBlockEntity;
import thaumcraft.common.registers.ModBlockEntities;
import thaumcraft.common.registers.ModBlocks;
import thaumcraft.common.registers.ModDataComponents;
import thaumcraft.common.world.aura.AuraHandler;

/**
 * TileJarFillable / TileJarFillableVoid: holds up to 250 essentia of one aspect, filled from a tube on top.
 * A label locks it to one aspect; a void jar keeps sucking when full and destroys the excess (as flux, rarely).
 */
public class JarBlockEntity extends TCBlockEntity implements IAspectSource, IEssentiaTransport {
   public static final int CAPACITY = 250;
   @Nullable
   public Aspect aspect;
   @Nullable
   public Aspect aspectFilter;
   public int amount;
   /** The side the label faces. */
   public Direction facing = Direction.NORTH;
   /** Brass lid brace: stops machines from taking essentia wirelessly. */
   public boolean blocked;
   private int count;

   public JarBlockEntity(BlockPos pos, BlockState state) {
      super(ModBlockEntities.JAR.get(), pos, state);
   }

   public boolean isVoid() {
      return getBlockState().is(ModBlocks.JAR_VOID.get());
   }

   @Override
   protected void readSync(CompoundTag tag, HolderLookup.Provider registries) {
      aspect = getAspect(tag, "Aspect");
      aspectFilter = getAspect(tag, "AspectFilter");
      amount = tag.getShort("Amount");
      facing = Direction.from3DDataValue(tag.getByte("facing"));
      if (facing.getAxis() == Direction.Axis.Y) facing = Direction.NORTH;
      blocked = tag.getBoolean("blocked");
   }

   @Override
   protected void writeSync(CompoundTag tag, HolderLookup.Provider registries) {
      putAspect(tag, "Aspect", aspect);
      putAspect(tag, "AspectFilter", aspectFilter);
      tag.putShort("Amount", (short) amount);
      tag.putByte("facing", (byte) facing.get3DDataValue());
      tag.putBoolean("blocked", blocked);
   }

   // The jar item keeps the essentia and the label (copy_components in the loot table, and placing the item back).
   @Override
   protected void collectImplicitComponents(DataComponentMap.Builder components) {
      super.collectImplicitComponents(components);
      if (aspect != null && amount > 0) components.set(ModDataComponents.ESSENTIA.get(), new AspectList().add(aspect, amount));
      if (aspectFilter != null) components.set(ModDataComponents.ASPECT_FILTER.get(), aspectFilter);
   }

   @Override
   protected void applyImplicitComponents(DataComponentInput input) {
      super.applyImplicitComponents(input);
      setAspects(input.getOrDefault(ModDataComponents.ESSENTIA.get(), new AspectList()));
      aspectFilter = input.get(ModDataComponents.ASPECT_FILTER.get());
   }

   @Override
   public void removeComponentsFromTag(CompoundTag tag) {
      tag.remove("Aspect");
      tag.remove("AspectFilter");
      tag.remove("Amount");
   }

   @Override
   public AspectList getAspects() {
      AspectList al = new AspectList();
      if (aspect != null && amount > 0) al.add(aspect, amount);
      return al;
   }

   @Override
   public void setAspects(AspectList aspects) {
      if (aspects != null && aspects.size() > 0) {
         aspect = aspects.getAspectsSortedByAmount()[0];
         amount = aspects.getAmount(aspect);
      }
   }

   @Override
   public int addToContainer(Aspect tt, int am) {
      if (am == 0) return am;
      if (isVoid()) {
         boolean up = amount < CAPACITY;
         if (tt == aspect || amount == 0) {
            aspect = tt;
            amount += am;
            am = 0;
            if (amount > CAPACITY) {
               if (level.random.nextInt(250) == 0) AuraHandler.addFlux(level, worldPosition, 1.0F);
               amount = CAPACITY;
            }
         }
         if (up) sync();
         return am;
      }
      if (amount < CAPACITY && tt == aspect || amount == 0) {
         aspect = tt;
         int added = Math.min(am, CAPACITY - amount);
         amount += added;
         am -= added;
      }
      sync();
      return am;
   }

   @Override
   public boolean takeFromContainer(Aspect tt, int am) {
      if (amount >= am && tt == aspect) {
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
   public boolean doesContainerContainAmount(Aspect tag, int amt) {
      return amount >= amt && tag == aspect;
   }

   @Override
   public int containerContains(Aspect tag) {
      return tag == aspect ? amount : 0;
   }

   @Override
   public boolean doesContainerAccept(Aspect tag) {
      return aspectFilter == null || tag.equals(aspectFilter);
   }

   @Override
   public boolean isConnectable(Direction face) {
      return face == Direction.UP;
   }

   @Override
   public boolean canInputFrom(Direction face) {
      return face == Direction.UP;
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
      if (isVoid()) return aspectFilter != null ? 48 : 32;
      return aspectFilter != null ? 64 : 32;
   }

   @Override
   public Aspect getSuctionType(Direction loc) {
      return aspectFilter != null ? aspectFilter : aspect;
   }

   @Override
   public int getSuctionAmount(Direction loc) {
      if (isVoid()) return aspectFilter != null && amount < CAPACITY ? 48 : 32;
      if (amount < CAPACITY) return aspectFilter != null ? 64 : 32;
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
   public int addEssentia(Aspect aspect, int amount, Direction face) {
      return canInputFrom(face) ? amount - addToContainer(aspect, amount) : 0;
   }

   @Override
   public boolean isBlocked() {
      return blocked;
   }

   public static void serverTick(Level level, BlockPos pos, BlockState state, JarBlockEntity be) {
      if (++be.count % 5 == 0 && (be.amount < CAPACITY || be.isVoid())) be.fillJar();
   }

   void fillJar() {
      IEssentiaTransport ic = ThaumcraftApiHelper.getConnectableTile(level, worldPosition, Direction.UP);
      if (ic == null || !ic.canOutputTo(Direction.DOWN)) return;
      Aspect ta = null;
      if (aspectFilter != null) {
         ta = aspectFilter;
      } else if (aspect != null && amount > 0) {
         ta = aspect;
      } else if (ic.getEssentiaAmount(Direction.DOWN) > 0 && ic.getSuctionAmount(Direction.DOWN) < getSuctionAmount(Direction.UP)
         && getSuctionAmount(Direction.UP) >= ic.getMinimumSuction()) {
         ta = ic.getEssentiaType(Direction.DOWN);
      }
      if (ta != null && ic.getSuctionAmount(Direction.DOWN) < getSuctionAmount(Direction.UP)) {
         addToContainer(ta, ic.takeEssentia(ta, 1, Direction.DOWN));
      }
   }

   public int comparatorSignal() {
      float r = amount / (float) CAPACITY;
      return (int) Math.floor(r * 14.0F) + (amount > 0 ? 1 : 0);
   }
}
