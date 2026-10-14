package thaumcraft.common.blocks.world.taint;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import thaumcraft.common.entities.monster.ThaumicSlimeEntity;
import thaumcraft.common.registers.ModFlux;
import thaumcraft.common.registers.ModMobs;
import thaumcraft.common.registers.ModSounds;

import java.util.Optional;

/**
 * BlockFluxGoo: a finite fluid (Forge BlockFluidFinite, 8 quanta: LEVEL 0..7 holds LEVEL+1) that flows down and levels
 * out sideways, slows and exhausts whatever wades through it, and slowly evaporates back into the aura's flux (or
 * curdles into taint fibre). Big puddles may turn into thaumic slimes.
 *
 * <p>Port notes: the finite-fluid flow of BlockFluidFinite.updateTick is reimplemented here (scheduled ticks). Thaumic
 * slimes belong to the monster milestone and aren't ported yet: until then the puddle just pollutes the aura instead.
 * A full (LEVEL 7) puddle can be scooped with a bucket into a bucket of flux goo.
 */
public class FluxGooBlock extends Block implements BucketPickup {
   public static final IntegerProperty LEVEL = IntegerProperty.create("level", 0, 7);
   public static final int QUANTA = 8;
   /** BlockFluidBase.tickRate = viscosity / 200. */
   private static final int TICK_RATE = 6000 / 200;
   private static final VoxelShape[] SHAPES = new VoxelShape[QUANTA];

   static {
      for (int i = 0; i < QUANTA; i++) SHAPES[i] = Block.box(0, 0, 0, 16, 2 * (i + 1), 16);
   }

   public FluxGooBlock(Properties properties) {
      super(properties);
      registerDefaultState(stateDefinition.any().setValue(LEVEL, 7));
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
      builder.add(LEVEL);
   }

   public static float quantaPercentage(BlockState state) {
      return (state.getValue(LEVEL) + 1) / (float) QUANTA;
   }

   @Override
   protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
      return SHAPES[state.getValue(LEVEL)];
   }

   @Override
   protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
      return Shapes.empty();
   }

   /** isReplaceable: shallow goo can be built over. */
   @Override
   protected boolean canBeReplaced(BlockState state, net.minecraft.world.item.context.BlockPlaceContext ctx) {
      return state.getValue(LEVEL) < 4 && !ctx.getItemInHand().is(asItem());
   }

   @Override
   protected boolean canBeReplaced(BlockState state, net.minecraft.world.level.material.Fluid fluid) {
      return state.getValue(LEVEL) < 4;
   }

   // ------------------------------------------------------------------ entities

   @Override
   protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
      int md = state.getValue(LEVEL);
      if (entity instanceof ThaumicSlimeEntity slime) {
         // thaumic slimes grow by eating the goo
         if (!level.isClientSide && slime.getSize() < md && level.random.nextBoolean()) {
            slime.setSize(slime.getSize() + 1, true);
            if (md > 1) level.setBlock(pos, state.setValue(LEVEL, md - 1), 2);
            else level.removeBlock(pos, false);
         }
         return;
      }
      float q = quantaPercentage(state);
      Vec3 m = entity.getDeltaMovement();
      entity.setDeltaMovement(m.x * (1.0F - q), m.y, m.z * (1.0F - q));
      if (!level.isClientSide && entity instanceof LivingEntity le) {
         MobEffectInstance pe = new MobEffectInstance(ModFlux.VIS_EXHAUST, 600, md / 3, true, true);
         pe.getCures().clear();
         le.addEffect(pe);
      }
   }

   // ------------------------------------------------------------------ ticking

   @Override
   protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean moving) {
      if (!level.isClientSide) level.scheduleTick(pos, this, TICK_RATE);
   }

   @Override
   protected BlockState updateShape(BlockState state, Direction dir, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
      level.scheduleTick(pos, this, TICK_RATE);
      return state;
   }

   @Override
   protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource rand) {
      tick(state, level, pos, rand);
   }

   /** BlockFluxGoo.updateTick (random and scheduled ticks alike, as in 1.12). */
   @Override
   protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource rand) {
      int meta = state.getValue(LEVEL);
      if (meta >= 2 && level.isEmptyBlock(pos.above()) && rand.nextInt(50) == 0) {
         // a thaumic slime rises from the goo (size 1 below level 6, else 2)
         level.removeBlock(pos, false);
         ThaumicSlimeEntity slime = new ThaumicSlimeEntity(ModMobs.THAUMIC_SLIME.get(), level);
         slime.moveTo(pos.getX() + 0.5F, pos.getY(), pos.getZ() + 0.5F, 0.0F, 0.0F);
         slime.setSize(meta >= 6 ? 2 : 1, true);
         level.addFreshEntity(slime);
         slime.playSound(ModSounds.GORE.get(), 1.0F, 1.0F);
      } else if (rand.nextInt(4) == 0) {
         if (meta == 0) {
            if (rand.nextBoolean()) {
               FluxEffects.pollute(level, pos, 1.0F, true);
               level.removeBlock(pos, false);
            } else {
               level.setBlockAndUpdate(pos, ModFlux.TAINT_FIBRE.get().defaultBlockState());
            }
         } else {
            level.setBlock(pos, state.setValue(LEVEL, meta - 1), 2);
            FluxEffects.pollute(level, pos, 1.0F, true);
         }
      } else {
         flow(state, level, pos, rand);
      }
   }

   // ------------------------------------------------------------------ BlockFluidFinite

   /** Quanta in a block: 0 for anything goo may flow into, -1 for anything else. */
   private int quantaValue(Level level, BlockPos pos) {
      BlockState bs = level.getBlockState(pos);
      if (bs.is(this)) return bs.getValue(LEVEL) + 1;
      return canDisplace(level, pos, bs) ? 0 : -1;
   }

   private int quantaValueBelow(Level level, BlockPos pos, int belowThis) {
      int q = quantaValue(level, pos);
      return q >= belowThis ? -1 : q;
   }

   /** BlockFluidBase.canDisplace (+ defaultDisplacements: taint fibre). */
   private boolean canDisplace(Level level, BlockPos pos, BlockState bs) {
      if (bs.isAir()) return true;
      if (bs.is(this)) return false;
      if (bs.getBlock() instanceof TaintFibreBlock) return true;
      return bs.canBeReplaced() && bs.getFluidState().isEmpty();
   }

   private void setQuanta(Level level, BlockPos pos, int quanta) {
      if (quanta <= 0) level.removeBlock(pos, false);
      else level.setBlock(pos, defaultBlockState().setValue(LEVEL, quanta - 1), 3);
   }

   private void flow(BlockState state, ServerLevel level, BlockPos pos, RandomSource rand) {
      boolean changed = false;
      int quantaRemaining = state.getValue(LEVEL) + 1;
      int prevRemaining = quantaRemaining;
      quantaRemaining = tryToFlowVerticallyInto(level, pos, quantaRemaining);
      if (quantaRemaining < 1) return;
      if (quantaRemaining != prevRemaining) {
         changed = true;
         if (quantaRemaining == 1) {
            level.setBlock(pos, state.setValue(LEVEL, 0), 2);
            return;
         }
      } else if (quantaRemaining == 1) {
         return;
      }

      int lowerThan = quantaRemaining - 1;
      int total = quantaRemaining;
      int count = 1;
      for (Direction side : Direction.Plane.HORIZONTAL) {
         BlockPos off = pos.relative(side);
         int quanta = quantaValueBelow(level, off, lowerThan);
         if (quanta >= 0) {
            count++;
            total += quanta;
         }
      }
      if (count == 1) {
         if (changed) level.setBlock(pos, state.setValue(LEVEL, quantaRemaining - 1), 2);
         return;
      }
      int each = total / count;
      int rem = total % count;
      for (Direction side : Direction.Plane.HORIZONTAL) {
         BlockPos off = pos.relative(side);
         int quanta = quantaValueBelow(level, off, lowerThan);
         if (quanta >= 0) {
            int newQuanta = each;
            if (rem == count || rem > 1 && rand.nextInt(count - rem) != 0) {
               newQuanta++;
               rem--;
            }
            if (newQuanta != quanta) {
               setQuanta(level, off, newQuanta);
               if (newQuanta > 0) level.scheduleTick(off, this, TICK_RATE);
            }
            count--;
         }
      }
      if (rem > 0) each++;
      setQuanta(level, pos, each);
   }

   private int tryToFlowVerticallyInto(ServerLevel level, BlockPos pos, int amtToInput) {
      BlockPos other = pos.below();
      if (other.getY() < level.getMinBuildHeight()) {
         level.removeBlock(pos, false);
         return 0;
      }
      int amt = quantaValueBelow(level, other, QUANTA);
      if (amt >= 0) {
         amt += amtToInput;
         if (amt > QUANTA) {
            setQuanta(level, other, QUANTA);
            level.scheduleTick(other, this, TICK_RATE);
            return amt - QUANTA;
         } else if (amt > 0) {
            setQuanta(level, other, amt);
            level.scheduleTick(other, this, TICK_RATE);
            level.removeBlock(pos, false);
            return 0;
         }
         return amtToInput;
      }
      return amtToInput;
   }

   @Override
   public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource rand) {
      int meta = state.getValue(LEVEL);
      if (rand.nextInt(44) <= meta) {
         thaumcraft.client.fx.TaintFX.gooBubble(pos.getX() + rand.nextFloat(), pos.getY() + 0.125F * meta, pos.getZ() + rand.nextFloat());
      }
   }

   // ------------------------------------------------------------------ bucket

   @Override
   public ItemStack pickupBlock(Player player, LevelAccessor level, BlockPos pos, BlockState state) {
      if (state.getValue(LEVEL) < QUANTA - 1) return ItemStack.EMPTY;
      level.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 11);
      return new ItemStack(ModFlux.BUCKET_GOO.get());
   }

   @Override
   public Optional<SoundEvent> getPickupSound() {
      return Optional.of(net.minecraft.sounds.SoundEvents.BUCKET_FILL);
   }
}
