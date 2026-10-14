package thaumcraft.common.blocks.eldritch;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import thaumcraft.common.entities.monster.eldritch.EldritchGuardianEntity;
import thaumcraft.common.registers.ModEldritch;
import thaumcraft.common.registers.ModSounds;

/**
 * The eldritch altar (BlocksTC.eldritch, id {@code thaumcraft:eldritch}; ritualist cultist clerics gather around it).
 *
 * <p>Port note: Thaumcraft 6 kept the eldritch altar, obelisks, locks and portal only as names and textures (the
 * Outer Lands were never finished in 1.12). This follows Thaumcraft 4: the altar stands among four obelisks; every
 * eldritch eye set into it wakes one obelisk and calls eldritch guardians from the void, and with the fourth eye an
 * eldritch portal to the Outer Lands opens above it.
 */
public class EldritchAltarBlock extends Block {
   public static final IntegerProperty EYES = IntegerProperty.create("eyes", 0, 4);

   public EldritchAltarBlock(Properties properties) {
      super(properties);
      registerDefaultState(stateDefinition.any().setValue(EYES, 0));
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
      builder.add(EYES);
   }

   @Override
   protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
      BlockHitResult hit) {
      if (!stack.is(ModEldritch.ELDRITCH_EYE.get())) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
      int eyes = state.getValue(EYES);
      if (eyes >= 4) return ItemInteractionResult.FAIL;
      if (!level.isClientSide) {
         if (!player.getAbilities().instabuild) stack.shrink(1);
         insertEye((ServerLevel) level, pos, state, player);
      }
      return ItemInteractionResult.sidedSuccess(level.isClientSide);
   }

   /** Sets one more eye: lights an obelisk, summons guardians, and the fourth opens the portal. */
   public static void insertEye(ServerLevel level, BlockPos pos, BlockState state, @javax.annotation.Nullable Player player) {
      int eyes = state.getValue(EYES) + 1;
      level.setBlockAndUpdate(pos, state.setValue(EYES, eyes));
      level.playSound(null, pos, ModSounds.EVILPORTAL.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
      lightObelisk(level, pos, eyes - 1);
      RandomSource r = level.random;
      for (int a = 0; a < 1 + r.nextInt(2); a++) {
         EldritchGuardianEntity eg = ModEldritch.ELDRITCH_GUARDIAN.get().create(level);
         if (eg == null) continue;
         for (int t = 0; t < 20; t++) {
            BlockPos p = pos.offset(r.nextInt(17) - 8, r.nextInt(5) - 2, r.nextInt(17) - 8);
            if (Math.abs(p.getX() - pos.getX()) < 4 && Math.abs(p.getZ() - pos.getZ()) < 4) continue;
            eg.moveTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, r.nextFloat() * 360.0F, 0.0F);
            if (level.getBlockState(p.below()).isFaceSturdy(level, p.below(), net.minecraft.core.Direction.UP) && level.noCollision(eg)) {
               eg.finalizeSpawn(level, level.getCurrentDifficultyAt(p), MobSpawnType.EVENT, null);
               eg.restrictTo(pos, 16);
               if (player != null && !player.isCreative()) eg.setTarget(player);
               level.addFreshEntity(eg);
               break;
            }
         }
      }
      if (eyes >= 4) {
         BlockPos portal = pos.above();
         if (level.getBlockState(portal).canBeReplaced()) level.setBlockAndUpdate(portal, ModEldritch.ELDRITCH_PORTAL.get().defaultBlockState());
         level.playSound(null, pos, SoundEvents.END_PORTAL_SPAWN, SoundSource.BLOCKS, 1.0F, 1.0F);
         if (player != null) player.displayClientMessage(Component.translatable("tc.eldritch.portal_open"), true);
      }
   }

   /** The obelisks stand at the four corners two blocks away (the order the eyes light them). */
   static final int[][] CORNERS = {{-2, -2}, {2, -2}, {2, 2}, {-2, 2}};

   private static void lightObelisk(Level level, BlockPos altar, int index) {
      if (index < 0 || index >= 4) return;
      for (int y = -1; y <= 4; y++) {
         BlockPos p = altar.offset(CORNERS[index][0], y, CORNERS[index][1]);
         BlockState s = level.getBlockState(p);
         if (s.hasProperty(EldritchObeliskBlock.LIT)) level.setBlockAndUpdate(p, s.setValue(EldritchObeliskBlock.LIT, true));
      }
   }
}
