package thaumcraft.common.golems;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import thaumcraft.api.golems.IGolemAPI;

/** Golems clicking blocks through a fake player; whatever it ends up holding goes to the golem. */
public class GolemInteractionHelper {
   public static final GameProfile PROFILE = new GameProfile(UUID.fromString("3b5c1d9e-5a77-4e5b-9d5e-7f1a9a0c3e11"), "FakeThaumcraftGolem");

   public static FakePlayer fakePlayer(ServerLevel level, LivingEntity at) {
      FakePlayer fp = FakePlayerFactory.get(level, PROFILE);
      fp.moveTo(at.getX(), at.getY(), at.getZ(), at.getYRot(), at.getXRot());
      return fp;
   }

   public static void golemClick(Level world, IGolemAPI golem, BlockPos pos, Direction face, ItemStack clickStack, boolean sneaking, boolean rightClick) {
      if (!(world instanceof ServerLevel level)) return;
      LivingEntity g = golem.getGolemEntity();
      FakePlayer fp = fakePlayer(level, g);
      fp.setItemInHand(InteractionHand.MAIN_HAND, clickStack);
      fp.setShiftKeyDown(sneaking);
      if (!rightClick) {
         try {
            fp.gameMode.handleBlockBreakAction(pos, ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, face, level.getMaxBuildHeight(), 0);
         } catch (Exception ignored) {
         }
      } else {
         if (fp.getMainHandItem().getItem() instanceof BlockItem bi && !mayPlace(level, bi, pos)) {
            g.setPos(g.getX() + face.getStepX(), g.getY() + face.getStepY(), g.getZ() + face.getStepZ());
         }
         try {
            BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos), face, pos, false);
            fp.gameMode.useItemOn(fp, level, fp.getMainHandItem(), InteractionHand.MAIN_HAND, hit);
         } catch (Exception ignored) {
         }
      }
      golem.addRankXp(1);
      if (fp.getMainHandItem().isEmpty()) fp.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
      dropSomeItems(fp, golem);
      fp.setShiftKeyDown(false);
      golem.swingArm();
   }

   private static boolean mayPlace(ServerLevel level, BlockItem item, BlockPos pos) {
      VoxelShape shape = item.getBlock().defaultBlockState().getCollisionShape(level, pos);
      return shape.isEmpty() || level.isUnobstructed(null, shape.move(pos.getX(), pos.getY(), pos.getZ()));
   }

   /** Moves everything the fake player holds to the golem, dropping what it can't carry. */
   public static void dropSomeItems(FakePlayer fp, IGolemAPI golem) {
      var inv = fp.getInventory();
      for (var list : java.util.List.of(inv.items, inv.armor, inv.offhand)) {
         for (int i = 0; i < list.size(); i++) {
            ItemStack s = list.get(i);
            if (s.isEmpty()) continue;
            if (golem.canCarry(s, true)) s = golem.holdItem(s);
            if (!s.isEmpty()) GolemInvHelper.dropItemAtEntity(golem.getGolemWorld(), s, golem.getGolemEntity());
            list.set(i, ItemStack.EMPTY);
         }
      }
   }
}
