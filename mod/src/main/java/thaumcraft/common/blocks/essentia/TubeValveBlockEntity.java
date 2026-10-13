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
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.registers.ModBlockEntities;
import thaumcraft.common.registers.ModSounds;

/** TileTubeValve: closed by a redstone signal or a right click; its facing side never connects (that's the handle). */
public class TubeValveBlockEntity extends TubeBlockEntity {
   public boolean allowFlow = true;
   boolean wasPoweredLastTick;
   public float rotation;

   public TubeValveBlockEntity(BlockPos pos, BlockState state) {
      super(ModBlockEntities.TUBE_VALVE.get(), pos, state);
   }

   @Override
   protected void update() {
      if (!level.isClientSide && count % 5 == 0) {
         boolean power = gettingPower();
         if (wasPoweredLastTick && !power && !allowFlow) setFlow(true);
         if (!wasPoweredLastTick && power && allowFlow) setFlow(false);
         wasPoweredLastTick = power;
      }
      if (level.isClientSide) {
         if (!allowFlow && rotation < 360.0F) rotation += 20.0F;
         else if (allowFlow && rotation > 0.0F) rotation -= 20.0F;
      }
      super.update();
   }

   public void setFlow(boolean flow) {
      allowFlow = flow;
      level.playSound(null, worldPosition, ModSounds.SQUEEK.get(), SoundSource.BLOCKS, 0.7F, 0.9F + level.random.nextFloat() * 0.2F);
      sync();
   }

   @Override
   protected void readSync(CompoundTag tag, HolderLookup.Provider registries) {
      super.readSync(tag, registries);
      allowFlow = tag.getBoolean("flow");
      wasPoweredLastTick = tag.getBoolean("hadpower");
   }

   @Override
   protected void writeSync(CompoundTag tag, HolderLookup.Provider registries) {
      super.writeSync(tag, registries);
      tag.putBoolean("flow", allowFlow);
      tag.putBoolean("hadpower", wasPoweredLastTick);
   }

   @Override
   public boolean isConnectable(Direction face) {
      return face != facing && super.isConnectable(face);
   }

   @Override
   public void setSuction(Aspect aspect, int amount) {
      if (allowFlow) super.setSuction(aspect, amount);
   }

   /** The core turns the handle to the next side without a neighbour. */
   @Override
   protected void rotateCore() {
      int a = facing.get3DDataValue();
      while (++a < 20) {
         Direction d = Direction.from3DDataValue(a % 6);
         if (!canConnectSide(d)) {
            facing = d;
            sync();
            TubeBlock.refresh(level, worldPosition);
            break;
         }
      }
   }

   @Override
   public boolean onCasterRightClick(Level level, ItemStack caster, Player player, BlockPos pos, Direction side, InteractionHand hand, BlockHitResult hit) {
      return super.onCasterRightClick(level, caster, player, pos, side, hand, hit);
   }
}
