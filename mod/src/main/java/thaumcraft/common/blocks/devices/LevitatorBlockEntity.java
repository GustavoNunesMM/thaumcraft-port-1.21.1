package thaumcraft.common.blocks.devices;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import thaumcraft.common.blocks.DeviceBlock;
import thaumcraft.common.blocks.TCBlockEntity;
import thaumcraft.common.registers.ModDevices;
import thaumcraft.common.world.aura.AuraHandler;

/**
 * TileLevitator: pushes items and pushable entities along its facing, up to 4/8/16/32 blocks (stopping at the first
 * opaque block). Runs on both sides (so players are moved by their own client), paid with vis from the aura.
 */
public class LevitatorBlockEntity extends TCBlockEntity {
   private static final int[] RANGES = {4, 8, 16, 32};
   private int range = 1;
   private int rangeActual = 0;
   private int counter = 0;
   private int vis = 0;

   public LevitatorBlockEntity(BlockPos pos, BlockState state) {
      super(ModDevices.LEVITATOR_BE.get(), pos, state);
   }

   public static void tick(Level level, BlockPos pos, BlockState state, LevitatorBlockEntity be) {
      Direction facing = DeviceBlock.getFacing(state);
      if (be.rangeActual > RANGES[be.range]) be.rangeActual = 0;
      int p = be.counter % RANGES[be.range];
      BlockPos check = pos.relative(facing, 1 + p);
      if (level.getBlockState(check).isSolidRender(level, check)) {
         if (1 + p < be.rangeActual) be.rangeActual = 1 + p;
         be.counter = -1;
      } else if (1 + p > be.rangeActual) {
         be.rangeActual = 1 + p;
      }
      be.counter++;
      if (!level.isClientSide && be.vis < 10) {
         int got = (int) (AuraHandler.drainVis(level, pos, 1.0F, false) * 1200.0F);
         if (got > 0) {
            be.vis += got;
            be.sync();
         }
      }
      if (be.rangeActual > 0 && be.vis > 0 && DeviceBlock.isEnabled(state)) {
         int ra = be.rangeActual;
         AABB box = new AABB(pos.getX() - (facing.getStepX() < 0 ? ra : 0), pos.getY() - (facing.getStepY() < 0 ? ra : 0),
            pos.getZ() - (facing.getStepZ() < 0 ? ra : 0), pos.getX() + 1 + (facing.getStepX() > 0 ? ra : 0),
            pos.getY() + 1 + (facing.getStepY() > 0 ? ra : 0), pos.getZ() + 1 + (facing.getStepZ() > 0 ? ra : 0));
         List<Entity> targets = level.getEntitiesOfClass(Entity.class, box);
         boolean lifted = false;
         for (Entity e : targets) {
            if (e instanceof ItemEntity || e.isPushable() || e instanceof AbstractHorse) {
               lifted = true;
               be.drawFXAt(e);
               be.drawFX(facing, 0.6);
               Vec3 m = e.getDeltaMovement();
               double mx = m.x, my = m.y, mz = m.z;
               if (!e.isShiftKeyDown() || facing != Direction.UP) {
                  mx += 0.1F * facing.getStepX();
                  my += 0.1F * facing.getStepY();
                  mz += 0.1F * facing.getStepZ();
                  if (facing.getAxis() != Direction.Axis.Y && !e.onGround()) {
                     if (my < 0.0) my *= 0.9F;
                     my += 0.08F;
                  }
                  mx = Math.max(-0.35F, Math.min(0.35F, mx));
                  my = Math.max(-0.35F, Math.min(0.35F, my));
                  mz = Math.max(-0.35F, Math.min(0.35F, mz));
               } else if (my < 0.0) {
                  my *= 0.9F;
               }
               e.setDeltaMovement(mx, my, mz);
               if (!level.isClientSide && !(e instanceof Player)) e.hurtMarked = true;
               e.fallDistance = 0.0F;
               be.vis -= be.getCost();
               if (be.vis <= 0) break;
            }
         }
         be.drawFX(facing, 0.1);
         if (lifted && !level.isClientSide && be.counter % 20 == 0) be.setChanged();
      }
   }

   private void drawFX(Direction facing, double c) {
      if (level.isClientSide && level.random.nextFloat() < c) {
         float x = worldPosition.getX() + 0.25F + level.random.nextFloat() * 0.5F;
         float y = worldPosition.getY() + 0.25F + level.random.nextFloat() * 0.5F;
         float z = worldPosition.getZ() + 0.25F + level.random.nextFloat() * 0.5F;
         thaumcraft.client.fx.DeviceFX.drawLevitatorParticles(x, y, z, facing.getStepX() / 50.0, facing.getStepY() / 50.0, facing.getStepZ() / 50.0);
      }
   }

   private void drawFXAt(Entity e) {
      if (level.isClientSide && level.random.nextFloat() < 0.1F) {
         var r = level.random;
         float x = (float) (e.getX() + (r.nextFloat() - r.nextFloat()) * e.getBbWidth());
         float y = (float) (e.getY() + r.nextFloat() * e.getBbHeight());
         float z = (float) (e.getZ() + (r.nextFloat() - r.nextFloat()) * e.getBbWidth());
         thaumcraft.client.fx.DeviceFX.drawLevitatorParticles(x, y, z, (r.nextFloat() - r.nextFloat()) * 0.01, (r.nextFloat() - r.nextFloat()) * 0.01,
            (r.nextFloat() - r.nextFloat()) * 0.01);
      }
   }

   public int getCost() {
      return RANGES[range] * 2;
   }

   public int getRange() {
      return RANGES[range];
   }

   public int getVis() {
      return vis;
   }

   public void increaseRange(Player player) {
      rangeActual = 0;
      if (!level.isClientSide) {
         range++;
         if (range >= RANGES.length) range = 0;
         sync();
         player.sendSystemMessage(Component.translatable("thaumcraft.levitator.range", RANGES[range], getCost()));
      }
   }

   @Override
   protected void readSync(CompoundTag tag, HolderLookup.Provider registries) {
      range = Math.max(0, Math.min(RANGES.length - 1, tag.getByte("range")));
      vis = tag.getInt("vis");
   }

   @Override
   protected void writeSync(CompoundTag tag, HolderLookup.Provider registries) {
      tag.putByte("range", (byte) range);
      tag.putInt("vis", vis);
   }
}
