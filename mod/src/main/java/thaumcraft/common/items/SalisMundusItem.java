package thaumcraft.common.items;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import thaumcraft.common.crafting.dust.DustTrigger;
import thaumcraft.common.registers.ModSounds;

/** ItemMagicDust: use on a block to fire the first matching dust trigger. */
public class SalisMundusItem extends Item {
   public SalisMundusItem(Properties properties) {
      super(properties);
   }

   @Override
   public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext ctx) {
      Player player = ctx.getPlayer();
      Level level = ctx.getLevel();
      BlockPos pos = ctx.getClickedPos();
      if (player == null || !player.mayUseItemAt(pos, ctx.getClickedFace(), stack)) return InteractionResult.FAIL;
      if (player.isShiftKeyDown()) return InteractionResult.PASS;
      for (DustTrigger trigger : DustTrigger.TRIGGERS) {
         DustTrigger.Placement place = trigger.getValidFace(level, player, pos, ctx.getClickedFace());
         if (place == null) continue;
         player.swing(ctx.getHand());
         if (!player.getAbilities().instabuild) stack.shrink(1);
         trigger.execute(level, player, pos, place, ctx.getClickedFace());
         if (level.isClientSide) sparkles(level, player, pos, ctx.getClickLocation(), trigger, place);
         return InteractionResult.sidedSuccess(level.isClientSide);
      }
      return InteractionResult.PASS;
   }

   /** doSparkles: golden dust flies from the hand to the block, then the trigger's blocks glitter. */
   private static void sparkles(Level level, Player player, BlockPos pos, Vec3 hit, DustTrigger trigger, DustTrigger.Placement place) {
      Vec3 from = player.getEyePosition().add(player.getLookAngle().scale(0.4)).add(0, -0.3, 0);
      Vec3 dir = Vec3.atCenterOf(pos).subtract(from);
      for (int a = 0; a < 50; a++) {
         boolean floaty = a < 50 / 3;
         Vector3f color = new Vector3f(1.0F, (189 + level.random.nextInt(67)) / 255.0F, (64 + level.random.nextInt(192)) / 255.0F);
         level.addParticle(new DustParticleOptions(color, 0.8F), from.x, from.y, from.z,
            dir.x / 6.0 + level.random.nextGaussian() * 0.05,
            dir.y / 6.0 + level.random.nextGaussian() * 0.05 + (floaty ? 0.05 : 0.15),
            dir.z / 6.0 + level.random.nextGaussian() * 0.05);
      }
      level.playLocalSound(pos.getX(), pos.getY(), pos.getZ(), ModSounds.DUST.get(), SoundSource.PLAYERS, 0.33F,
         1.0F + (float) level.random.nextGaussian() * 0.05F, false);
      for (BlockPos p : trigger.sparkle(level, player, pos, place)) {
         for (int i = 0; i < 6; i++) {
            level.addParticle(net.minecraft.core.particles.ParticleTypes.ENCHANT, p.getX() + level.random.nextFloat(), p.getY() + level.random.nextFloat(),
               p.getZ() + level.random.nextFloat(), 0, 0.05, 0);
         }
      }
   }
}
