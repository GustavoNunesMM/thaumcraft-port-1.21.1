package thaumcraft.common.blocks.devices;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IAspectContainer;
import thaumcraft.api.aspects.IEssentiaTransport;
import thaumcraft.common.blocks.TCBlockEntity;
import thaumcraft.common.registers.ModDevices;

/** TileThaumatoriumTop: passes essentia (and its view of the aspects) to the thaumatorium under it. */
public class ThaumatoriumTopBlockEntity extends TCBlockEntity implements IAspectContainer, IEssentiaTransport {
   public ThaumatoriumTopBlockEntity(BlockPos pos, BlockState state) {
      super(ModDevices.THAUMATORIUM_TOP_BE.get(), pos, state);
   }

   @Nullable
   public ThaumatoriumBlockEntity thaumatorium() {
      return level != null && level.getBlockEntity(worldPosition.below()) instanceof ThaumatoriumBlockEntity t ? t : null;
   }

   @Override
   public int addToContainer(Aspect tt, int am) {
      ThaumatoriumBlockEntity t = thaumatorium();
      return t == null ? am : t.addToContainer(tt, am);
   }

   @Override
   public boolean takeFromContainer(Aspect tt, int am) {
      ThaumatoriumBlockEntity t = thaumatorium();
      return t != null && t.takeFromContainer(tt, am);
   }

   @Override
   public boolean doesContainerContainAmount(Aspect tt, int am) {
      ThaumatoriumBlockEntity t = thaumatorium();
      return t != null && t.doesContainerContainAmount(tt, am);
   }

   @Override
   public int containerContains(Aspect tt) {
      ThaumatoriumBlockEntity t = thaumatorium();
      return t == null ? 0 : t.containerContains(tt);
   }

   @Override
   public boolean doesContainerAccept(Aspect tag) {
      return true;
   }

   @Override
   public AspectList getAspects() {
      ThaumatoriumBlockEntity t = thaumatorium();
      return t == null ? null : t.essentia;
   }

   @Override
   public void setAspects(AspectList aspects) {
      ThaumatoriumBlockEntity t = thaumatorium();
      if (t != null) t.setAspects(aspects);
   }

   @Override
   public boolean isConnectable(Direction face) {
      ThaumatoriumBlockEntity t = thaumatorium();
      return t != null && t.isConnectable(face);
   }

   @Override
   public boolean canInputFrom(Direction face) {
      ThaumatoriumBlockEntity t = thaumatorium();
      return t != null && t.canInputFrom(face);
   }

   @Override
   public boolean canOutputTo(Direction face) {
      return false;
   }

   @Override
   public void setSuction(@Nullable Aspect aspect, int amount) {
      ThaumatoriumBlockEntity t = thaumatorium();
      if (t != null) t.setSuction(aspect, amount);
   }

   @Override
   public Aspect getSuctionType(@Nullable Direction face) {
      ThaumatoriumBlockEntity t = thaumatorium();
      return t == null ? null : t.getSuctionType(face);
   }

   @Override
   public int getSuctionAmount(@Nullable Direction face) {
      ThaumatoriumBlockEntity t = thaumatorium();
      return t == null ? 0 : t.getSuctionAmount(face);
   }

   @Override
   public Aspect getEssentiaType(@Nullable Direction face) {
      return null;
   }

   @Override
   public int getEssentiaAmount(@Nullable Direction face) {
      return 0;
   }

   @Override
   public int takeEssentia(Aspect aspect, int amount, @Nullable Direction face) {
      ThaumatoriumBlockEntity t = thaumatorium();
      return t == null ? 0 : t.takeEssentia(aspect, amount, face);
   }

   @Override
   public int addEssentia(Aspect aspect, int amount, @Nullable Direction face) {
      ThaumatoriumBlockEntity t = thaumatorium();
      return t == null ? 0 : t.addEssentia(aspect, amount, face);
   }

   @Override
   public int getMinimumSuction() {
      return 0;
   }
}
