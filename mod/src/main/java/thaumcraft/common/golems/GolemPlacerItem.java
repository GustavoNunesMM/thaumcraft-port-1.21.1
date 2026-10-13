package thaumcraft.common.golems;

import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import thaumcraft.api.golems.EnumGolemTrait;
import thaumcraft.api.golems.IGolemProperties;
import thaumcraft.api.golems.ISealDisplayer;
import thaumcraft.api.golems.parts.GolemArm;
import thaumcraft.api.golems.parts.GolemHead;
import thaumcraft.api.golems.parts.GolemMaterial;
import thaumcraft.common.items.ICreativeVariants;
import thaumcraft.common.registers.ModGolems;

/** ItemGolemPlacer ("golem"): a packed golem; its make-up (props) and rank xp travel with it. */
public class GolemPlacerItem extends Item implements ISealDisplayer, ICreativeVariants {
   public GolemPlacerItem(Properties props) {
      super(props);
   }

   public static ItemStack of(IGolemProperties props) {
      ItemStack is = new ItemStack(ModGolems.GOLEM_PLACER.get());
      is.set(ModGolems.GOLEM_PROPS.get(), props.toLong());
      return is;
   }

   public static IGolemProperties getProperties(ItemStack stack) {
      return GolemProperties.fromLong(stack.getOrDefault(ModGolems.GOLEM_PROPS.get(), 0L));
   }

   @Override
   public void addCreativeStacks(Consumer<ItemStack> output) {
      output.accept(of(GolemProperties.fromLong(0L)));
      IGolemProperties props = GolemProperties.fromLong(0L);
      props.setHead(GolemHead.getHeads()[1]);
      props.setArms(GolemArm.getArms()[1]);
      output.accept(of(props));
      props = GolemProperties.fromLong(0L);
      props.setMaterial(GolemMaterial.getMaterials()[1]);
      props.setHead(GolemHead.getHeads()[1]);
      props.setArms(GolemArm.getArms()[2]);
      output.accept(of(props));
      props = GolemProperties.fromLong(0L);
      props.setMaterial(GolemMaterial.getMaterials()[4]);
      props.setHead(GolemHead.getHeads()[1]);
      props.setArms(GolemArm.getArms()[3]);
      output.accept(of(props));
   }

   @Override
   public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
      if (!stack.has(ModGolems.GOLEM_PROPS.get())) return;
      IGolemProperties props = getProperties(stack);
      if (props.hasTrait(EnumGolemTrait.SMART)) {
         if (props.getRank() >= 10) {
            tooltip.add(Component.translatable("golem.rank").append(" " + props.getRank()).withStyle(ChatFormatting.GOLD));
         } else {
            int rx = stack.getOrDefault(ModGolems.GOLEM_XP.get(), 0);
            int xn = (props.getRank() + 1) * (props.getRank() + 1) * GolemEntity.XPM;
            tooltip.add(Component.translatable("golem.rank").append(" " + props.getRank()).withStyle(ChatFormatting.GOLD)
               .append(Component.literal(" (" + rx + "/" + xn + ")").withStyle(ChatFormatting.DARK_GREEN)));
         }
      }
      tooltip.add(props.getMaterial().getLocalizedName().copy().withStyle(ChatFormatting.GREEN));
      for (EnumGolemTrait tag : props.getTraits()) tooltip.add(Component.literal("-").append(tag.getLocalizedName()).withStyle(ChatFormatting.BLUE));
   }

   @Override
   public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext ctx) {
      Level level = ctx.getLevel();
      Player player = ctx.getPlayer();
      if (!level.getBlockState(ctx.getClickedPos()).isSolid()) return InteractionResult.FAIL;
      if (level.isClientSide) return InteractionResult.PASS;
      BlockPos pos = ctx.getClickedPos().relative(ctx.getClickedFace());
      if (player != null && !player.mayUseItemAt(pos, ctx.getClickedFace(), stack)) return InteractionResult.FAIL;
      GolemEntity golem = spawn((ServerLevel) level, pos, stack, player);
      if (golem != null && (player == null || !player.getAbilities().instabuild)) stack.shrink(1);
      return InteractionResult.SUCCESS;
   }

   /** Puts down the golem this placer holds, owned by the player. */
   public static GolemEntity spawn(ServerLevel level, BlockPos pos, ItemStack stack, Player player) {
      GolemEntity golem = ModGolems.GOLEM.get().create(level);
      if (golem == null) return null;
      golem.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0.0F, 0.0F);
      golem.setOwned(true);
      golem.setValidSpawn();
      if (player != null) golem.setOwnerId(player.getUUID());
      if (stack.has(ModGolems.GOLEM_PROPS.get())) golem.setProperties(getProperties(stack));
      golem.setRankXp(stack.getOrDefault(ModGolems.GOLEM_XP.get(), 0));
      golem.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.SPAWN_EGG, null);
      golem.initialSpawn();
      golem.setHealth(golem.getMaxHealth());
      return level.addFreshEntity(golem) ? golem : null;
   }
}
