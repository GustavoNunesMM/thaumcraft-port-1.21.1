package thaumcraft.common.blocks.devices;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.IEssentiaTransport;
import thaumcraft.common.blocks.DeviceBlock;
import thaumcraft.common.blocks.TCBlockEntity;
import thaumcraft.common.registers.ModDevices;

/**
 * TileLampFertility: stores up to 10 desiderium; every 300 ticks (with more than 1 stored) two adult animals of the
 * same kind within 7 blocks fall in love, as long as there are no more than 9 of them, for 5 charges.
 */
public class FertilityLampBlockEntity extends TCBlockEntity implements IEssentiaTransport {
   public int charges = 0;
   private int count = 0;
   private int drawDelay = 0;

   public FertilityLampBlockEntity(BlockPos pos, BlockState state) {
      super(ModDevices.LAMP_FERTILITY_BE.get(), pos, state);
   }

   private Direction facing() {
      return DeviceBlock.getFacing(getBlockState());
   }

   public static void serverTick(Level level, BlockPos pos, BlockState state, FertilityLampBlockEntity be) {
      if (be.charges < 10) {
         if (be.drawEssentia()) {
            be.charges++;
            be.sync();
         }
         state = be.getBlockState();
         if (be.charges <= 1) {
            if (DeviceBlock.isEnabled(state)) level.setBlock(pos, state.setValue(DeviceBlock.ENABLED, false), 3);
         } else if (!be.gettingPower() && !DeviceBlock.isEnabled(state)) {
            level.setBlock(pos, state.setValue(DeviceBlock.ENABLED, true), 3);
         }
      }
      if (!be.gettingPower() && be.charges > 1 && be.count++ % 300 == 0) be.updateAnimals();
   }

   private void updateAnimals() {
      int distance = 7;
      List<Animal> animals = level.getEntitiesOfClass(Animal.class, new AABB(worldPosition).inflate(distance));
      for (Animal a : animals) {
         if (a.getAge() != 0 || a.isInLove()) continue;
         List<Animal> same = new ArrayList<>();
         for (Animal o : animals) {
            if (o.getClass().equals(a.getClass())) same.add(o);
         }
         if (same.size() <= 9) {
            Animal partner = null;
            for (Animal o : same) {
               if (o.getAge() == 0 && !o.isInLove()) {
                  if (partner != null) {
                     charges -= 5;
                     o.setInLove(null);
                     partner.setInLove(null);
                     sync();
                     return;
                  }
                  partner = o;
               }
            }
         }
      }
   }

   boolean drawEssentia() {
      if (++drawDelay % 5 != 0) return false;
      Direction f = facing();
      IEssentiaTransport ic = ThaumcraftApiHelper.getConnectableTile(level, worldPosition, f);
      if (ic != null) {
         if (!ic.canOutputTo(f.getOpposite())) return false;
         return ic.getSuctionAmount(f.getOpposite()) < getSuctionAmount(f) && ic.takeEssentia(Aspect.DESIRE, 1, f.getOpposite()) == 1;
      }
      return false;
   }

   @Override
   protected void readSync(CompoundTag tag, HolderLookup.Provider registries) {
      charges = tag.getInt("charges");
   }

   @Override
   protected void writeSync(CompoundTag tag, HolderLookup.Provider registries) {
      tag.putInt("charges", charges);
   }

   @Override
   public boolean isConnectable(Direction face) {
      return face == facing();
   }

   @Override
   public boolean canInputFrom(Direction face) {
      return face == facing();
   }

   @Override
   public boolean canOutputTo(Direction face) {
      return false;
   }

   @Override
   public void setSuction(@Nullable Aspect aspect, int amount) {
   }

   @Override
   public int getMinimumSuction() {
      return 0;
   }

   @Override
   public Aspect getSuctionType(@Nullable Direction face) {
      return Aspect.DESIRE;
   }

   @Override
   public int getSuctionAmount(@Nullable Direction face) {
      return face == facing() ? 128 - charges * 10 : 0;
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
      return 0;
   }

   @Override
   public int addEssentia(Aspect aspect, int amount, @Nullable Direction face) {
      return 0;
   }
}
