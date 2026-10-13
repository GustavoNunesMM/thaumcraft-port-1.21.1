package thaumcraft.common.blocks.essentia;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IAspectContainer;
import thaumcraft.api.aspects.IEssentiaTransport;
import thaumcraft.common.blocks.devices.BellowsBlockEntity;
import thaumcraft.common.registers.ModBlockEntities;
import thaumcraft.common.registers.ModSounds;

/**
 * TileTubeBuffer: holds up to 10 essentia of any aspects. Suction 1 (or 32 per bellows) per side; a caster
 * while sneaking cycles a side between normal, choked (no bellows boost) and closed to suction.
 */
public class TubeBufferBlockEntity extends TubeBlockEntity implements IAspectContainer {
   public static final int MAXAMOUNT = 10;
   public AspectList aspects = new AspectList();
   public byte[] chokedSides = {0, 0, 0, 0, 0, 0};
   int bellows = -1;

   public TubeBufferBlockEntity(BlockPos pos, BlockState state) {
      super(ModBlockEntities.TUBE_BUFFER.get(), pos, state);
   }

   @Override
   protected void readSync(CompoundTag tag, HolderLookup.Provider registries) {
      aspects = AspectList.CODEC.parse(net.minecraft.nbt.NbtOps.INSTANCE, tag.get("Aspects")).result().orElseGet(AspectList::new);
      byte[] sides = tag.getByteArray("open");
      if (sides.length == 6) {
         for (int a = 0; a < 6; a++) openSides[a] = sides[a] == 1;
      }
      chokedSides = tag.getByteArray("choke");
      if (chokedSides.length < 6) chokedSides = new byte[]{0, 0, 0, 0, 0, 0};
      facing = Direction.from3DDataValue(tag.getInt("side"));
   }

   @Override
   protected void writeSync(CompoundTag tag, HolderLookup.Provider registries) {
      tag.put("Aspects", AspectList.CODEC.encodeStart(net.minecraft.nbt.NbtOps.INSTANCE, aspects).getOrThrow());
      byte[] sides = new byte[6];
      for (int a = 0; a < 6; a++) sides[a] = (byte) (openSides[a] ? 1 : 0);
      tag.putByteArray("open", sides);
      tag.putByteArray("choke", chokedSides);
      tag.putInt("side", facing.get3DDataValue());
   }

   @Override
   protected void readExtra(CompoundTag tag, HolderLookup.Provider registries) {
   }

   @Override
   protected void writeExtra(CompoundTag tag, HolderLookup.Provider registries) {
   }

   @Override
   public AspectList getAspects() {
      return aspects;
   }

   @Override
   public void setAspects(AspectList aspects) {
   }

   @Override
   public int addToContainer(Aspect tt, int am) {
      if (am != 1) return am;
      if (aspects.visSize() < MAXAMOUNT) {
         aspects.add(tt, am);
         sync();
         return 0;
      }
      return am;
   }

   @Override
   public boolean takeFromContainer(Aspect tt, int am) {
      if (aspects.getAmount(tt) >= am) {
         aspects.remove(tt, am);
         sync();
         return true;
      }
      return false;
   }

   @Override
   public boolean doesContainerContainAmount(Aspect tag, int amt) {
      return aspects.getAmount(tag) >= amt;
   }

   @Override
   public int containerContains(Aspect tag) {
      return aspects.getAmount(tag);
   }

   @Override
   public boolean doesContainerAccept(Aspect tag) {
      return true;
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
      if (loc == null) return 1;
      byte c = chokedSides[loc.get3DDataValue()];
      return c == 2 ? 0 : bellows > 0 && c != 1 ? bellows * 32 : 1;
   }

   @Override
   public Aspect getEssentiaType(Direction loc) {
      return aspects.size() > 0 ? aspects.getAspects()[level.random.nextInt(aspects.size())] : null;
   }

   @Override
   public int getEssentiaAmount(Direction loc) {
      return aspects.visSize();
   }

   @Override
   public int takeEssentia(Aspect aspect, int amount, Direction face) {
      if (!canOutputTo(face)) return 0;
      int suction = 0;
      IEssentiaTransport ic = ThaumcraftApiHelper.getConnectableTile(level, worldPosition, face);
      if (ic != null) suction = ic.getSuctionAmount(face.getOpposite());
      // Don't give it away if another side sucks harder for it.
      for (Direction dir : Direction.values()) {
         if (!canOutputTo(dir) || dir == face) continue;
         IEssentiaTransport o = ThaumcraftApiHelper.getConnectableTile(level, worldPosition, dir);
         if (o != null) {
            int sa = o.getSuctionAmount(dir.getOpposite());
            Aspect su = o.getSuctionType(dir.getOpposite());
            if ((su == aspect || su == null) && suction < sa && getSuctionAmount(dir) < sa) return 0;
         }
      }
      amount = Math.min(amount, aspects.getAmount(aspect));
      return takeFromContainer(aspect, amount) ? amount : 0;
   }

   @Override
   public int addEssentia(Aspect aspect, int amount, Direction face) {
      return canInputFrom(face) ? amount - addToContainer(aspect, amount) : 0;
   }

   @Override
   protected void update() {
      count++;
      if (bellows < 0 || count % 20 == 0) bellows = BellowsBlockEntity.getBellows(level, worldPosition, Direction.values());
      if (!level.isClientSide && count % 5 == 0 && aspects.visSize() < MAXAMOUNT) fillBuffer();
   }

   void fillBuffer() {
      for (Direction dir : Direction.values()) {
         if (!canInputFrom(dir)) continue;
         IEssentiaTransport ic = ThaumcraftApiHelper.getConnectableTile(level, worldPosition, dir);
         if (ic != null && ic.getEssentiaAmount(dir.getOpposite()) > 0 && ic.getSuctionAmount(dir.getOpposite()) < getSuctionAmount(dir)
            && getSuctionAmount(dir) >= ic.getMinimumSuction()) {
            Aspect ta = ic.getEssentiaType(dir.getOpposite());
            if (ta != null) addToContainer(ta, ic.takeEssentia(ta, 1, dir.getOpposite()));
            return;
         }
      }
   }

   @Override
   protected double armLength() {
      return 0.5;
   }

   @Override
   public boolean onCasterRightClick(Level level, ItemStack caster, Player player, BlockPos pos, Direction side, InteractionHand hand, BlockHitResult hit) {
      int sub = partHit(hit);
      if (sub < 0 || sub >= 6) return false;
      player.swing(hand);
      if (!level.isClientSide) {
         if (player.isShiftKeyDown()) {
            level.playSound(null, worldPosition, ModSounds.SQUEEK.get(), SoundSource.BLOCKS, 0.6F, 2.0F + level.random.nextFloat() * 0.2F);
            chokedSides[sub]++;
            if (chokedSides[sub] > 2) chokedSides[sub] = 0;
            sync();
         } else {
            playTool(player);
            toggleSide(sub);
         }
      }
      return true;
   }

   @Override
   public int breakSpill() {
      return aspects.visSize();
   }

   public int comparatorSignal() {
      float r = aspects.visSize() / 10.0F;
      return (int) Math.floor(r * 14.0F) + (aspects.visSize() > 0 ? 1 : 0);
   }
}
