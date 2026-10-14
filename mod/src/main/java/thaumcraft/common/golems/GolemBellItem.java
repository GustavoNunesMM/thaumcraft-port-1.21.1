package thaumcraft.common.golems;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.golems.ISealDisplayer;
import thaumcraft.api.golems.seals.ISealEntity;
import thaumcraft.api.golems.seals.SealPos;
import thaumcraft.common.golems.seals.SealHandler;
import thaumcraft.common.registers.ModSounds;
import thaumcraft.common.research.ResearchManager;

/**
 * ItemGolemBell: rings to show seals; used on a seal it opens the seal's settings (sneaking removes the seal), sneak-used
 * elsewhere it opens the logistics request screen (GOLEMLOGISTICS).
 */
public class GolemBellItem extends Item implements ISealDisplayer {
   public GolemBellItem(Properties props) {
      super(props);
   }

   @Override
   public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
      player.swing(hand);
      if (level.isClientSide) {
         player.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 0.6F, 1.0F + level.random.nextFloat() * 0.1F);
      } else if (player instanceof ServerPlayer sp) {
         HitResult mop = player.pick(player.blockInteractionRange(), 1.0F, false);
         if (mop.getType() != HitResult.Type.MISS) {
            ISealEntity se = getSeal(player);
            if (se != null) {
               if (player.isShiftKeyDown()) {
                  SealHandler.removeSealEntity(level, se.getSealPos(), false);
                  level.playSound(null, se.getSealPos().pos, ModSounds.ZAP.get(), SoundSource.BLOCKS, 0.5F, 1.0F);
               } else {
                  GolemMenus.openSeal(sp, se);
               }
            }
            return InteractionResultHolder.fail(player.getItemInHand(hand));
         }
         if (player.isShiftKeyDown() && ResearchManager.knowsResearch(player, "GOLEMLOGISTICS")) {
            GolemMenus.openLogistics(sp, null, null);
            return InteractionResultHolder.fail(player.getItemInHand(hand));
         }
      }
      return super.use(level, player, hand);
   }

   @Override
   public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext ctx) {
      Player player = ctx.getPlayer();
      if (player == null) return InteractionResult.PASS;
      player.swing(ctx.getHand());
      Level level = ctx.getLevel();
      ISealEntity se = SealHandler.getSealEntity(level, new SealPos(ctx.getClickedPos(), ctx.getClickedFace()));
      if (level.isClientSide) {
         return se != null || player.isShiftKeyDown() && ResearchManager.knowsResearch(player, "GOLEMLOGISTICS") ? InteractionResult.SUCCESS
            : InteractionResult.PASS;
      }
      if (se != null) {
         if (player.isShiftKeyDown()) {
            SealHandler.removeSealEntity(level, se.getSealPos(), false);
            level.playSound(null, ctx.getClickedPos(), ModSounds.ZAP.get(), SoundSource.BLOCKS, 0.5F, 1.0F);
         } else if (player instanceof ServerPlayer sp) {
            GolemMenus.openSeal(sp, se);
         }
         return InteractionResult.SUCCESS;
      }
      if (player.isShiftKeyDown() && ResearchManager.knowsResearch(player, "GOLEMLOGISTICS") && player instanceof ServerPlayer sp) {
         GolemMenus.openLogistics(sp, ctx.getClickedPos(), ctx.getClickedFace());
         return InteractionResult.SUCCESS;
      }
      return InteractionResult.PASS;
   }

   /** The seal the player looks at within 5 blocks (seals are flat, so this checks each block face along the look). */
   @Nullable
   public static ISealEntity getSeal(Player player) {
      Vec3 start = player.getEyePosition();
      Vec3 look = player.getViewVector(1.0F).scale(5.0);
      Vec3 end = start.add(look);
      BlockPos last = null;
      List<AABB> unit = List.of(new AABB(0, 0, 0, 1, 1, 1));
      for (int a = 1; a <= 50; a++) {
         BlockPos pos = BlockPos.containing(start.add(look.scale(a / 50.0)));
         if (pos.equals(last)) continue;
         last = pos;
         BlockHitResult hit = AABB.clip(unit, start, end, pos);
         if (hit != null) {
            ISealEntity se = SealHandler.getSealEntity(player.level(), new SealPos(pos, hit.getDirection()));
            if (se != null) return se;
         }
         if (!player.level().getBlockState(pos).getCollisionShape(player.level(), pos).isEmpty()
            && player.level().clip(new ClipContext(start, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player)).getBlockPos().equals(pos)) {
            // A solid block stops the search past it (its own faces were checked above).
            break;
         }
      }
      return null;
   }
}
