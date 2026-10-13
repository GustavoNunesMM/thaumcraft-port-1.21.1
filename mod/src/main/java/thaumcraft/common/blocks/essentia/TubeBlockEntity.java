package thaumcraft.common.blocks.essentia;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.IEssentiaTransport;
import thaumcraft.api.casters.IInteractWithCaster;
import thaumcraft.common.blocks.TCBlockEntity;
import thaumcraft.common.registers.ModBlockEntities;
import thaumcraft.common.registers.ModSounds;

/**
 * TileTube: every other tick takes the strongest suction of its neighbours minus one, and every 5 ticks pulls one
 * essentia from the neighbour with less suction. Neighbours fighting over different aspects make the tube vent.
 * Subclasses: valve (redstone stops it), restricted (halves suction), one-way, filtered.
 */
public class TubeBlockEntity extends TCBlockEntity implements IEssentiaTransport, IInteractWithCaster {
   public Direction facing = Direction.NORTH;
   public boolean[] openSides = {true, true, true, true, true, true};
   @Nullable
   Aspect essentiaType;
   int essentiaAmount;
   @Nullable
   Aspect suctionType;
   int suction;
   public int venting;
   int count;
   public int ventColor;

   public TubeBlockEntity(BlockPos pos, BlockState state) {
      this(ModBlockEntities.TUBE.get(), pos, state);
   }

   protected TubeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
      super(type, pos, state);
   }

   @Override
   protected void readSync(CompoundTag tag, HolderLookup.Provider registries) {
      essentiaType = getAspect(tag, "type");
      essentiaAmount = tag.getInt("amount");
      facing = Direction.from3DDataValue(tag.getInt("side"));
      byte[] sides = tag.getByteArray("open");
      if (sides.length == 6) {
         for (int a = 0; a < 6; a++) openSides[a] = sides[a] == 1;
      }
   }

   @Override
   protected void writeSync(CompoundTag tag, HolderLookup.Provider registries) {
      putAspect(tag, "type", essentiaType);
      tag.putInt("amount", essentiaAmount);
      byte[] sides = new byte[6];
      for (int a = 0; a < 6; a++) sides[a] = (byte) (openSides[a] ? 1 : 0);
      tag.putInt("side", facing.get3DDataValue());
      tag.putByteArray("open", sides);
   }

   @Override
   protected void readExtra(CompoundTag tag, HolderLookup.Provider registries) {
      suctionType = getAspect(tag, "stype");
      suction = tag.getInt("samount");
   }

   @Override
   protected void writeExtra(CompoundTag tag, HolderLookup.Provider registries) {
      putAspect(tag, "stype", suctionType);
      tag.putInt("samount", suction);
   }

   public static void tick(Level level, BlockPos pos, BlockState state, TubeBlockEntity be) {
      be.update();
   }

   protected void update() {
      if (venting > 0) venting--;
      if (count == 0) count = level.random.nextInt(10);
      if (!level.isClientSide) {
         if (venting <= 0) {
            if (++count % 2 == 0) {
               calculateSuction(null, false, false);
               checkVenting();
               if (essentiaType != null && essentiaAmount == 0) essentiaType = null;
            }
            if (count % 5 == 0 && suction > 0) equalizeWithNeighbours(false);
         }
      } else if (venting > 0) {
         TubeEffects.vent(this);
      }
   }

   void calculateSuction(@Nullable Aspect filter, boolean restrict, boolean directional) {
      suction = 0;
      suctionType = null;
      for (Direction loc : Direction.values()) {
         if (directional && facing != loc.getOpposite() || !isConnectable(loc)) continue;
         IEssentiaTransport ic = ThaumcraftApiHelper.getConnectableTile(level, worldPosition, loc);
         if (ic == null) continue;
         Aspect st = ic.getSuctionType(loc.getOpposite());
         if ((filter == null || st == null || st == filter)
            && (filter != null || getEssentiaAmount(loc) <= 0 || st == null || getEssentiaType(loc) == st)
            && (filter == null || getEssentiaAmount(loc) <= 0 || getEssentiaType(loc) == null || st == null || getEssentiaType(loc) == st)) {
            int suck = ic.getSuctionAmount(loc.getOpposite());
            if (suck > 0 && suck > suction + 1) setSuction(st == null ? filter : st, restrict ? suck / 2 : suck - 1);
         }
      }
   }

   void checkVenting() {
      for (Direction loc : Direction.values()) {
         if (!isConnectable(loc)) continue;
         IEssentiaTransport ic = ThaumcraftApiHelper.getConnectableTile(level, worldPosition, loc);
         if (ic == null) continue;
         int suck = ic.getSuctionAmount(loc.getOpposite());
         if (suction > 0 && (suck == suction || suck == suction - 1) && suctionType != ic.getSuctionType(loc.getOpposite())
            && !(ic instanceof TubeFilterBlockEntity)) {
            level.blockEvent(worldPosition, getBlockState().getBlock(), 1, suctionType == null ? -1 : suctionType.getColor());
            venting = 40;
         }
      }
   }

   void equalizeWithNeighbours(boolean directional) {
      if (essentiaAmount > 0) return;
      for (Direction loc : Direction.values()) {
         if (directional && facing == loc.getOpposite() || !isConnectable(loc)) continue;
         IEssentiaTransport ic = ThaumcraftApiHelper.getConnectableTile(level, worldPosition, loc);
         if (ic == null || !ic.canOutputTo(loc.getOpposite())) continue;
         if ((getSuctionType(null) == null || getSuctionType(null) == ic.getEssentiaType(loc.getOpposite())
            || ic.getEssentiaType(loc.getOpposite()) == null)
            && getSuctionAmount(null) > ic.getSuctionAmount(loc.getOpposite()) && getSuctionAmount(null) >= ic.getMinimumSuction()) {
            Aspect a = getSuctionType(null);
            if (a == null) {
               a = ic.getEssentiaType(loc.getOpposite());
               if (a == null) a = ic.getEssentiaType(null);
            }
            if (a == null) continue;
            int am = addEssentia(a, ic.takeEssentia(a, 1, loc.getOpposite()), loc);
            if (am > 0) {
               if (level.random.nextInt(100) == 0) level.blockEvent(worldPosition, getBlockState().getBlock(), 0, 0);
               return;
            }
         }
      }
   }

   @Override
   public boolean isConnectable(@Nullable Direction face) {
      return face != null && openSides[face.get3DDataValue()];
   }

   @Override
   public boolean canInputFrom(@Nullable Direction face) {
      return face != null && openSides[face.get3DDataValue()];
   }

   @Override
   public boolean canOutputTo(@Nullable Direction face) {
      return face != null && openSides[face.get3DDataValue()];
   }

   @Override
   public void setSuction(@Nullable Aspect aspect, int amount) {
      suctionType = aspect;
      suction = amount;
   }

   @Override
   public Aspect getSuctionType(@Nullable Direction loc) {
      return suctionType;
   }

   @Override
   public int getSuctionAmount(@Nullable Direction loc) {
      return suction;
   }

   @Override
   public Aspect getEssentiaType(@Nullable Direction loc) {
      return essentiaType;
   }

   @Override
   public int getEssentiaAmount(@Nullable Direction loc) {
      return essentiaAmount;
   }

   @Override
   public int takeEssentia(Aspect aspect, int amount, @Nullable Direction face) {
      if (canOutputTo(face) && essentiaType == aspect && essentiaAmount > 0 && amount > 0) {
         essentiaAmount--;
         if (essentiaAmount <= 0) essentiaType = null;
         setChanged();
         return 1;
      }
      return 0;
   }

   @Override
   public int addEssentia(Aspect aspect, int amount, @Nullable Direction face) {
      if (canInputFrom(face) && essentiaAmount == 0 && amount > 0) {
         essentiaType = aspect;
         essentiaAmount++;
         setChanged();
         return 1;
      }
      return 0;
   }

   @Override
   public int getMinimumSuction() {
      return 0;
   }

   @Override
   public boolean triggerEvent(int id, int param) {
      if (id == 0) {
         if (level.isClientSide) {
            level.playLocalSound(worldPosition, ModSounds.CREAK.get(), SoundSource.AMBIENT, 1.0F, 1.3F + level.random.nextFloat() * 0.2F, false);
         }
         return true;
      }
      if (id == 1) {
         if (level.isClientSide) {
            if (venting <= 0) {
               level.playLocalSound(worldPosition, SoundEvents.LAVA_EXTINGUISH, SoundSource.BLOCKS, 0.1F, 1.0F + level.random.nextFloat() * 0.1F, false);
            }
            venting = 50;
            ventColor = param == -1 ? 0xAAAAAA : param;
         }
         return true;
      }
      return super.triggerEvent(id, param);
   }

   /** The neighbour is something tubes can connect to (drawn as an arm, clickable with a caster). */
   public boolean canConnectSide(Direction side) {
      return level.getBlockEntity(worldPosition.relative(side)) instanceof IEssentiaTransport;
   }

   /** Arm boxes (index = Direction 3D value) and the core (index 6), in block coordinates; armLength is 0.375 for tubes. */
   public AABB part(int index, double armLength) {
      double min = 0.375, max = 0.625;
      return switch (index) {
         case 0 -> new AABB(min, 0, min, max, armLength, max);
         case 1 -> new AABB(min, 1 - armLength, min, max, 1, max);
         case 2 -> new AABB(min, min, 0, max, max, armLength);
         case 3 -> new AABB(min, min, 1 - armLength, max, max, 1);
         case 4 -> new AABB(0, min, min, armLength, max, max);
         case 5 -> new AABB(1 - armLength, min, min, 1, max, max);
         default -> new AABB(0.375, 0.375, 0.375, 0.625, 0.625, 0.625);
      };
   }

   protected double armLength() {
      return 0.375;
   }

   /** RayTracer.retraceBlock: which part of the tube was clicked, or -1. Arms win over the core. */
   public int partHit(BlockHitResult hit) {
      Vec3 p = hit.getLocation().subtract(Vec3.atLowerCornerWithOffset(worldPosition, 0, 0, 0));
      for (int i = 0; i < 6; i++) {
         if (canConnectSide(Direction.from3DDataValue(i)) && part(i, armLength()).inflate(0.01).contains(p)) return i;
      }
      return part(6, armLength()).inflate(0.01).contains(p) ? 6 : -1;
   }

   protected void playTool(Player player) {
      level.playSound(null, worldPosition, ModSounds.TOOL.get(), SoundSource.BLOCKS, 0.5F, 0.9F + level.random.nextFloat() * 0.2F);
   }

   /** Opens/closes the clicked side (and the matching side of a tube next to it). */
   protected void toggleSide(int side) {
      openSides[side] = !openSides[side];
      Direction dir = Direction.from3DDataValue(side);
      BlockEntity tile = level.getBlockEntity(worldPosition.relative(dir));
      if (tile instanceof TubeBlockEntity t) {
         t.openSides[dir.getOpposite().get3DDataValue()] = openSides[side];
         t.sync();
         TubeBlock.refresh(level, t.getBlockPos());
      }
      sync();
      TubeBlock.refresh(level, worldPosition);
   }

   @Override
   public boolean onCasterRightClick(Level level, ItemStack caster, Player player, BlockPos pos, Direction side, InteractionHand hand, BlockHitResult hit) {
      int sub = partHit(hit);
      if (sub >= 0 && sub < 6) {
         if (!level.isClientSide) {
            playTool(player);
            toggleSide(sub);
         }
         player.swing(hand);
         return true;
      }
      if (sub == 6) {
         if (!level.isClientSide) {
            playTool(player);
            rotateCore();
         }
         player.swing(hand);
         return true;
      }
      return false;
   }

   /** Clicking the core turns the tube's facing (used by one-way tubes) to the next side it can connect to. */
   protected void rotateCore() {
      int a = facing.get3DDataValue();
      while (++a < 20) {
         Direction d = Direction.from3DDataValue(a % 6).getOpposite();
         if (canConnectSide(d) && isConnectable(d)) {
            facing = Direction.from3DDataValue(a % 6);
            sync();
            break;
         }
      }
   }

   public int breakSpill() {
      return essentiaAmount;
   }

   /** Client-side helpers kept apart so the server never loads particle classes. */
   static final class TubeEffects {
      static void vent(TubeBlockEntity t) {
         thaumcraft.client.fx.EssentiaFX.tubeVent(t);
      }
   }
}
