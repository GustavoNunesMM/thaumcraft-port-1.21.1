package thaumcraft.common.items.baubles;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.CommonHooks;
import thaumcraft.client.fx.FXDispatcher;
import thaumcraft.common.lib.GearClientBridge;
import thaumcraft.common.network.CloudJumpPayload;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

/**
 * ItemCloudRing: a second jump in mid-air (client side, like 1.12; the server is told to forget the fall). Falls hurt less
 * while it is worn (GearEvents).
 */
public class CloudRingItem extends Item implements ICurioItem {
   public static final Map<String, Boolean> JUMP_LIST = new HashMap<>();

   public CloudRingItem(Properties p) {
      super(p.stacksTo(1));
   }

   @Override
   public void curioTick(SlotContext ctx, ItemStack stack) {
      LivingEntity player = ctx.entity();
      if (!player.level().isClientSide || !(player instanceof Player p) || !GearClientBridge.isLocalPlayer.test(p)) return;
      boolean spacePressed = GearClientBridge.jumpPressed.getAsBoolean();
      String name = player.getName().getString();
      if (spacePressed && !JUMP_LIST.containsKey(name)) JUMP_LIST.put(name, true);
      // (1.12 also waited for jumpTicks == 0; the jump delay is private now and a fresh key press is needed anyway.)
      if (spacePressed && !player.onGround() && !player.isInWater() && Boolean.TRUE.equals(JUMP_LIST.get(name))) {
         FXDispatcher.drawBamf(player.getX(), player.getY() + 0.5, player.getZ(), 1.0F, 1.0F, 1.0F, false, false, Direction.UP);
         player.level().playLocalSound(player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.1F,
            1.0F + (float) player.getRandom().nextGaussian() * 0.05F, false);
         JUMP_LIST.put(name, false);
         Vec3 m = player.getDeltaMovement();
         double my = 0.75;
         if (player.hasEffect(MobEffects.JUMP)) my += (player.getEffect(MobEffects.JUMP).getAmplifier() + 1) * 0.1F;
         double mx = m.x, mz = m.z;
         if (player.isSprinting()) {
            float f = player.getYRot() * (float) (Math.PI / 180.0);
            mx -= Mth.sin(f) * 0.2F;
            mz += Mth.cos(f) * 0.2F;
         }
         player.setDeltaMovement(mx, my, mz);
         player.fallDistance = 0.0F;
         CloudJumpPayload.sendToServer();
         CommonHooks.onLivingJump(player);
      }
      if (player.onGround()) JUMP_LIST.remove(name);
   }
}
