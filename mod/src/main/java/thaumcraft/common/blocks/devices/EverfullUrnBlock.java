package thaumcraft.common.blocks.devices;

import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import thaumcraft.common.blocks.DeviceBlock;
import thaumcraft.common.registers.ModDevices;

/** BlockWaterJug (everfull urn): makes water out of vis and pours it into the tanks and cauldrons around it. */
public class EverfullUrnBlock extends DeviceBlock {
   public static final MapCodec<EverfullUrnBlock> CODEC = simpleCodec(EverfullUrnBlock::new);
   private static final VoxelShape SHAPE = box(3, 0, 3, 13, 16, 13);

   public EverfullUrnBlock(Properties properties) {
      super(properties);
   }

   @Override
   protected MapCodec<? extends BaseEntityBlock> codec() {
      return CODEC;
   }

   @Override
   protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
      return SHAPE;
   }

   @Override
   protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                             BlockHitResult hit) {
      if (!(level.getBlockEntity(pos) instanceof EverfullUrnBlockEntity tile)) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
      if (level.isClientSide) return ItemInteractionResult.SUCCESS;
      if (FluidUtil.interactWithFluidHandler(player, hand, tile.tank)) {
         tile.sync();
         level.playSound(null, pos, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 0.33F, 1.0F + (level.random.nextFloat() - level.random.nextFloat()) * 0.3F);
      } else if (stack.is(Items.GLASS_BOTTLE) && tile.tank.getFluidAmount() >= 333) {
         ItemStack water = PotionContents.createItemStack(Items.POTION, Potions.WATER);
         if (!player.getAbilities().instabuild) stack.shrink(1);
         if (stack.isEmpty()) player.setItemInHand(hand, water);
         else if (!player.getInventory().add(water)) player.drop(water, false);
         tile.drain(333, IFluidHandler.FluidAction.EXECUTE);
         level.playSound(null, pos, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 0.33F, 1.0F + (level.random.nextFloat() - level.random.nextFloat()) * 0.3F);
      }
      return ItemInteractionResult.SUCCESS;
   }

   @Override
   public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource rand) {
      if (level.getBlockEntity(pos) instanceof EverfullUrnBlockEntity tile && tile.tank.getFluidAmount() >= tile.tank.getCapacity()) {
         thaumcraft.client.fx.DeviceFX.jarSplash(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5);
      }
   }

   @Nullable
   @Override
   public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
      return new EverfullUrnBlockEntity(pos, state);
   }

   @Nullable
   @Override
   public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
      return createTickerHelper(type, ModDevices.EVERFULL_URN_BE.get(), EverfullUrnBlockEntity::tick);
   }
}
