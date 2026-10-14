package thaumcraft.common.blocks.eldritch;

import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import thaumcraft.common.world.eldritch.OuterLands;

/**
 * Eldritch portal (tile.eldritch.teleporter): opens above an altar fed four eldritch eyes and stands in the entry
 * chamber of every Outer Lands maze. Players stepping into it cross over; from the Outer Lands it leads back to where
 * they came from. (Thaumcraft 4 design, see EldritchAltarBlock.)
 */
public class EldritchPortalBlock extends BaseEntityBlock {
   public static final MapCodec<EldritchPortalBlock> CODEC = simpleCodec(EldritchPortalBlock::new);
   private static final VoxelShape SHAPE = box(2.0, 2.0, 2.0, 14.0, 14.0, 14.0);

   public EldritchPortalBlock(Properties properties) {
      super(properties);
   }

   @Override
   protected MapCodec<? extends BaseEntityBlock> codec() {
      return CODEC;
   }

   @Override
   protected RenderShape getRenderShape(BlockState state) {
      return RenderShape.ENTITYBLOCK_ANIMATED;
   }

   @Override
   protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
      return SHAPE;
   }

   @Override
   protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
      return Shapes.empty();
   }

   @Override
   public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
      return ItemStack.EMPTY;
   }

   @Override
   protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
      if (!level.isClientSide && entity instanceof ServerPlayer player && !player.isPassenger() && !player.isVehicle()
         && !player.isOnPortalCooldown()) {
         OuterLands.travel(player, pos);
      }
   }

   @Override
   public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource r) {
      if (r.nextInt(3) == 0) {
         thaumcraft.client.fx.EldritchFX.sparkle(pos.getX() + 0.5F + (r.nextFloat() - r.nextFloat()), pos.getY() + 0.5F + (r.nextFloat() - r.nextFloat()),
            pos.getZ() + 0.5F + (r.nextFloat() - r.nextFloat()), 0.3F, 0.0F, 0.5F);
      }
   }

   @Nullable
   @Override
   public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
      return new EldritchPortalBlockEntity(pos, state);
   }
}
