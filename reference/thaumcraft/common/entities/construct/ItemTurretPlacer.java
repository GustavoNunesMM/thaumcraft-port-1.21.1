package thaumcraft.common.entities.construct;

import java.util.List;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import thaumcraft.common.items.ItemTCBase;

public class ItemTurretPlacer extends ItemTCBase {
   public ItemTurretPlacer() {
      super("turret", "basic", "advanced", "bore");
   }

   public EnumActionResult onItemUseFirst(EntityPlayer player, World world, BlockPos pos, EnumFacing side, float hitX, float hitY, float hitZ, EnumHand hand) {
      if (side == EnumFacing.DOWN) {
         return EnumActionResult.PASS;
      } else {
         boolean flag = world.getBlockState(pos).getBlock().isReplaceable(world, pos);
         BlockPos blockpos = flag ? pos : pos.offset(side);
         if (!player.canPlayerEdit(blockpos, side, player.getHeldItem(hand))) {
            return EnumActionResult.PASS;
         } else {
            BlockPos blockpos1 = blockpos.up();
            boolean flag1 = !world.isAirBlock(blockpos) && !world.getBlockState(blockpos).getBlock().isReplaceable(world, blockpos);
            flag1 |= !world.isAirBlock(blockpos1) && !world.getBlockState(blockpos1).getBlock().isReplaceable(world, blockpos1);
            if (flag1) {
               return EnumActionResult.PASS;
            } else {
               double d0 = blockpos.getX();
               double d1 = blockpos.getY();
               double d2 = blockpos.getZ();
               List<Entity> list = world.getEntitiesWithinAABBExcludingEntity((Entity)null, new AxisAlignedBB(d0, d1, d2, d0 + 1.0, d1 + 2.0, d2 + 1.0));
               if (!list.isEmpty()) {
                  return EnumActionResult.PASS;
               } else if (!world.isRemote) {
                  world.setBlockToAir(blockpos);
                  world.setBlockToAir(blockpos1);
                  EntityOwnedConstruct turret = null;
                  switch (player.getHeldItem(hand).getItemDamage()) {
                     case 0:
                        turret = new EntityTurretCrossbow(world, blockpos);
                        break;
                     case 1:
                        turret = new EntityTurretCrossbowAdvanced(world, blockpos);
                        break;
                     case 2:
                        turret = new EntityArcaneBore(world, blockpos, player.getHorizontalFacing());
                  }

                  if (turret != null) {
                     world.spawnEntity(turret);
                     turret.setOwned(true);
                     turret.setValidSpawn();
                     turret.setOwnerId(player.getUniqueID());
                     world.playSound(
                        (EntityPlayer)null,
                        turret.posX,
                        turret.posY,
                        turret.posZ,
                        SoundEvents.ENTITY_ARMORSTAND_PLACE,
                        SoundCategory.BLOCKS,
                        0.75F,
                        0.8F
                     );
                  }

                  player.getHeldItem(hand).shrink(1);
                  return EnumActionResult.SUCCESS;
               } else {
                  return EnumActionResult.PASS;
               }
            }
         }
      }
   }
}
