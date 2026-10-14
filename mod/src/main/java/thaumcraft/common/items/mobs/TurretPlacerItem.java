package thaumcraft.common.items.mobs;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import thaumcraft.common.entities.construct.ArcaneBoreEntity;
import thaumcraft.common.entities.construct.OwnedConstructEntity;
import thaumcraft.common.entities.construct.TurretCrossbowAdvancedEntity;
import thaumcraft.common.entities.construct.TurretCrossbowEntity;
import thaumcraft.common.registers.ModMobs;

/** ItemTurretPlacer: places an automated crossbow (0), an advanced one (1) or an arcane bore (2) owned by the player. */
public class TurretPlacerItem extends Item {
   public final int type;

   public TurretPlacerItem(int type, Item.Properties p) {
      super(p);
      this.type = type;
   }

   @Override
   public InteractionResult useOn(UseOnContext ctx) {
      Direction side = ctx.getClickedFace();
      if (side == Direction.DOWN) return InteractionResult.PASS;
      Level world = ctx.getLevel();
      Player player = ctx.getPlayer();
      BlockPos pos = ctx.getClickedPos();
      boolean flag = world.getBlockState(pos).canBeReplaced(new BlockPlaceContext(ctx));
      BlockPos blockpos = flag ? pos : pos.relative(side);
      if (player != null && !player.mayUseItemAt(blockpos, side, ctx.getItemInHand())) return InteractionResult.PASS;
      BlockPos blockpos1 = blockpos.above();
      boolean blocked = !world.isEmptyBlock(blockpos) && !world.getBlockState(blockpos).canBeReplaced();
      blocked |= !world.isEmptyBlock(blockpos1) && !world.getBlockState(blockpos1).canBeReplaced();
      if (blocked) return InteractionResult.PASS;
      double d0 = blockpos.getX(), d1 = blockpos.getY(), d2 = blockpos.getZ();
      if (!world.getEntities(null, new AABB(d0, d1, d2, d0 + 1.0, d1 + 2.0, d2 + 1.0)).isEmpty()) return InteractionResult.PASS;
      if (!world.isClientSide) {
         world.removeBlock(blockpos, false);
         world.removeBlock(blockpos1, false);
         OwnedConstructEntity turret = switch (type) {
            case 0 -> {
               TurretCrossbowEntity t = new TurretCrossbowEntity(ModMobs.TURRET_CROSSBOW.get(), world);
               t.placeAt(blockpos);
               yield t;
            }
            case 1 -> {
               TurretCrossbowAdvancedEntity t = new TurretCrossbowAdvancedEntity(ModMobs.TURRET_CROSSBOW_ADVANCED.get(), world);
               t.placeAt(blockpos);
               yield t;
            }
            case 2 -> {
               ArcaneBoreEntity t = new ArcaneBoreEntity(ModMobs.ARCANE_BORE.get(), world);
               t.placeAt(blockpos, player != null ? player.getDirection() : Direction.NORTH);
               yield t;
            }
            default -> null;
         };
         if (turret != null) {
            turret.setOwned(true);
            turret.setValidSpawn();
            if (player != null) turret.setOwnerId(player.getUUID());
            world.addFreshEntity(turret);
            world.playSound(null, turret.getX(), turret.getY(), turret.getZ(), SoundEvents.ARMOR_STAND_PLACE, SoundSource.BLOCKS, 0.75F, 0.8F);
         }
         ctx.getItemInHand().shrink(1);
      }
      return InteractionResult.sidedSuccess(world.isClientSide);
   }
}
