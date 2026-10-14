package thaumcraft.common.blocks.devices;

import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.registers.ModDataComponents;
import thaumcraft.common.registers.ModItems;

/**
 * BlockBannerTC: the thaumcraft banners, one per dye colour plus the crimson cult one (dye == null). A dyed banner
 * takes the aspect of a phial it is clicked with (sneak-click clears it) and shows its symbol.
 */
public class BannerBlock extends BaseEntityBlock {
   private static final VoxelShape STANDING = box(5.28, 0, 5.28, 10.56, 16, 10.56);
   private static final VoxelShape[] WALL = {box(0, 0, 0, 16, 16, 4), box(12, 0, 0, 16, 16, 16), box(0, 0, 12, 16, 16, 16),
      box(0, 0, 0, 4, 16, 16)};
   @Nullable
   public final DyeColor dye;

   public BannerBlock(@Nullable DyeColor dye, Properties properties) {
      super(properties);
      this.dye = dye;
   }

   @Override
   protected MapCodec<? extends BaseEntityBlock> codec() {
      return simpleCodec(p -> new BannerBlock(dye, p));
   }

   @Override
   protected RenderShape getRenderShape(BlockState state) {
      return RenderShape.ENTITYBLOCK_ANIMATED;
   }

   @Override
   protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
      if (!(level.getBlockEntity(pos) instanceof BannerBlockEntity tile)) return Shapes.block();
      if (!tile.getWall()) return STANDING;
      return switch (tile.getBannerFacing()) {
         case 0 -> WALL[0];
         case 4 -> WALL[1];
         case 8 -> WALL[2];
         case 12 -> WALL[3];
         default -> Shapes.block();
      };
   }

   @Override
   protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
      return Shapes.empty();
   }

   /** The banner's colour (-1 for the crimson cult banner). */
   public int getColor() {
      return dye == null ? -1 : dye.getTextureDiffuseColor();
   }

   @Override
   protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                             BlockHitResult hit) {
      if (level.isClientSide || dye == null || !(level.getBlockEntity(pos) instanceof BannerBlockEntity te)) return ItemInteractionResult.SUCCESS;
      if (player.isShiftKeyDown()) {
         te.setAspect(null);
      } else if (stack.is(ModItems.PHIAL_FILLED.get())) {
         AspectList al = stack.get(ModDataComponents.ESSENTIA.get());
         if (al == null || al.isEmpty()) return ItemInteractionResult.SUCCESS;
         te.setAspect(al.getAspects()[0]);
         stack.shrink(1);
      } else {
         return ItemInteractionResult.SUCCESS;
      }
      te.sync();
      level.playSound(null, pos, SoundEvents.WOOL_HIT, SoundSource.BLOCKS, 1.0F, 1.0F);
      return ItemInteractionResult.SUCCESS;
   }

   @Override
   public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
      ItemStack drop = new ItemStack(this);
      if (level.getBlockEntity(pos) instanceof BannerBlockEntity te && te.getAspect() != null) drop.set(thaumcraft.common.registers.ModDevices.BANNER_ASPECT.get(), te.getAspect());
      return drop;
   }

   @Nullable
   @Override
   public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
      return new BannerBlockEntity(pos, state);
   }
}
