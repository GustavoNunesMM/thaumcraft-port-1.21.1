package thaumcraft.common.items.mobs;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import thaumcraft.api.research.EnumKnowledgeType;
import thaumcraft.api.research.ResearchCategories;
import thaumcraft.api.research.ResearchCategory;
import thaumcraft.common.registers.ModSounds;
import thaumcraft.common.research.ResearchManager;

/** ItemPechWand: a pech mage's wand. Studying it (once auromancy is known) teaches the pech focus and grants some knowledge. */
public class PechWandItem extends Item {
   public PechWandItem(Properties properties) {
      super(properties);
   }

   @Override
   public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
      tooltip.add(Component.translatable("item.curio.text"));
   }

   @Override
   public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
      ItemStack stack = player.getItemInHand(hand);
      if (!ResearchManager.knowsResearch(player, "BASEAUROMANCY")) {
         if (!level.isClientSide) player.sendSystemMessage(Component.translatable("not.pechwand").withStyle(ChatFormatting.RED));
         return InteractionResultHolder.pass(stack);
      }
      if (!player.getAbilities().instabuild) stack.shrink(1);
      level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.LEARN.get(), SoundSource.NEUTRAL, 0.5F,
         0.4F / (level.random.nextFloat() * 0.4F + 0.8F));
      if (!level.isClientSide) {
         if (!ResearchManager.knowsResearch(player, "FOCUSPECH")) {
            ResearchManager.progressResearch(player, "FOCUSPECH");
            player.sendSystemMessage(Component.translatable("got.pechwand").withStyle(ChatFormatting.DARK_PURPLE));
         }
         ResearchCategory[] rc = ResearchCategories.CATEGORIES.values().toArray(new ResearchCategory[0]);
         int oProg = EnumKnowledgeType.OBSERVATION.getProgression();
         ResearchManager.addKnowledge(player, EnumKnowledgeType.OBSERVATION, rc[player.getRandom().nextInt(rc.length)],
            Mth.nextInt(player.getRandom(), oProg / 3, oProg / 2));
         int tProg = EnumKnowledgeType.THEORY.getProgression();
         ResearchManager.addKnowledge(player, EnumKnowledgeType.THEORY, rc[player.getRandom().nextInt(rc.length)],
            Mth.nextInt(player.getRandom(), tProg / 5, tProg / 4));
      }
      player.awardStat(Stats.ITEM_USED.get(this));
      return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
   }
}
