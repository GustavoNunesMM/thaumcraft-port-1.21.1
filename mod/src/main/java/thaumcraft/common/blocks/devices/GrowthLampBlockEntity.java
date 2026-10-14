package thaumcraft.common.blocks.devices;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.CactusBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.GrassBlock;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.SugarCaneBlock;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.IEssentiaTransport;
import thaumcraft.common.blocks.DeviceBlock;
import thaumcraft.common.blocks.TCBlockEntity;
import thaumcraft.common.registers.ModDevices;

/**
 * TileLampGrowth: each point of herba gives 20 charges; every tick (while lit) one random column within 6 blocks is
 * searched top-down for a plant that is not fully grown, which gets an extra growth tick.
 */
public class GrowthLampBlockEntity extends TCBlockEntity implements IEssentiaTransport {
   private boolean reserve = false;
   public int charges = -1;
   public int maxCharges = 20;
   private BlockPos last = BlockPos.ZERO;
   @Nullable
   private BlockState lastState = null;
   private final List<BlockPos> checklist = new ArrayList<>();
   private int drawDelay = 0;

   public GrowthLampBlockEntity(BlockPos pos, BlockState state) {
      super(ModDevices.LAMP_GROWTH_BE.get(), pos, state);
   }

   private Direction facing() {
      return DeviceBlock.getFacing(getBlockState());
   }

   public static void serverTick(Level level, BlockPos pos, BlockState state, GrowthLampBlockEntity be) {
      if (be.charges <= 0) {
         if (be.reserve) {
            be.charges = be.maxCharges;
            be.reserve = false;
            be.sync();
         } else if (be.drawEssentia()) {
            be.charges = be.maxCharges;
            be.sync();
         }
         state = be.getBlockState();
         if (be.charges <= 0) {
            if (DeviceBlock.isEnabled(state)) level.setBlock(pos, state.setValue(DeviceBlock.ENABLED, false), 3);
         } else if (!be.gettingPower() && !DeviceBlock.isEnabled(state)) {
            level.setBlock(pos, state.setValue(DeviceBlock.ENABLED, true), 3);
         }
      }
      if (!be.reserve && be.drawEssentia()) be.reserve = true;
      if (be.charges == 0) {
         be.charges = -1;
         be.sync();
      }
      if (!be.gettingPower() && be.charges > 0) be.updatePlant((ServerLevel) level);
   }

   /** isPlant: growables, plants and cacti, but not grass blocks. */
   private boolean isPlant(BlockState b) {
      return (b.getBlock() instanceof BonemealableBlock || b.getBlock() instanceof BushBlock || b.getBlock() instanceof CactusBlock
         || b.getBlock() instanceof SugarCaneBlock) && !(b.getBlock() instanceof GrassBlock);
   }

   /** CropUtils.isGrownCrop. */
   public static boolean isGrownCrop(Level level, BlockPos pos, BlockState bs) {
      if (bs.isAir()) return false;
      if (bs.getBlock() instanceof CropBlock crop && crop.isMaxAge(bs)) return true;
      return bs.getBlock() instanceof BonemealableBlock g && !g.isValidBonemealTarget(level, pos, bs) && !(bs.getBlock() instanceof StemBlock);
   }

   private void updatePlant(ServerLevel level) {
      BlockState bs = level.getBlockState(last);
      if (lastState != null && bs != lastState) {
         // PacketFXBlockMist on the plant that just grew.
         level.sendParticles(ParticleTypes.HAPPY_VILLAGER, last.getX() + 0.5, last.getY() + 0.5, last.getZ() + 0.5, 6, 0.3, 0.3, 0.3, 0.0);
         lastState = bs;
      }
      int distance = 6;
      if (checklist.isEmpty()) {
         for (int a = -distance; a <= distance; a++) {
            for (int b = -distance; b <= distance; b++) checklist.add(worldPosition.offset(a, distance, b));
         }
         Collections.shuffle(checklist, new java.util.Random(level.random.nextLong()));
      }
      BlockPos c = checklist.remove(0);
      for (int y = c.getY(); y >= worldPosition.getY() - distance; y--) {
         BlockPos bp = new BlockPos(c.getX(), y, c.getZ());
         BlockState s = level.getBlockState(bp);
         if (!s.isAir() && isPlant(s) && worldPosition.getCenter().distanceToSqr(bp.getCenter()) < distance * distance
            && !isGrownCrop(level, bp, s)) {
            charges--;
            last = bp;
            lastState = s;
            // scheduleUpdate(bp, block, 1): the plant's growth tick.
            s.randomTick(level, bp, level.random);
            return;
         }
      }
   }

   boolean drawEssentia() {
      if (++drawDelay % 5 != 0) return false;
      Direction f = facing();
      IEssentiaTransport ic = ThaumcraftApiHelper.getConnectableTile(level, worldPosition, f);
      if (ic != null) {
         if (!ic.canOutputTo(f.getOpposite())) return false;
         return ic.getSuctionAmount(f.getOpposite()) < getSuctionAmount(f) && ic.takeEssentia(Aspect.PLANT, 1, f.getOpposite()) == 1;
      }
      return false;
   }

   @Override
   protected void readSync(CompoundTag tag, HolderLookup.Provider registries) {
      reserve = tag.getBoolean("reserve");
      charges = tag.getInt("charges");
   }

   @Override
   protected void writeSync(CompoundTag tag, HolderLookup.Provider registries) {
      tag.putBoolean("reserve", reserve);
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
      return Aspect.PLANT;
   }

   @Override
   public int getSuctionAmount(@Nullable Direction face) {
      return face != facing() || reserve && charges > 0 ? 0 : 128;
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
