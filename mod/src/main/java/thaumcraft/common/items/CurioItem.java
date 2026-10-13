package thaumcraft.common.items;

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
import thaumcraft.common.research.PlayerWarp;
import thaumcraft.common.research.ResearchManager;

/** ItemCurio: a strange object; studying it (right-click) grants knowledge, mostly of one category. */
public class CurioItem extends Item {
   public final String variant;

   public CurioItem(String variant, Properties properties) {
      super(properties);
      this.variant = variant;
   }

   @Override
   public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
      tooltip.add(Component.translatable("item.curio.text"));
   }

   private static void knowledge(Player p, String cat) {
      int oProg = EnumKnowledgeType.OBSERVATION.getProgression();
      int tProg = EnumKnowledgeType.THEORY.getProgression();
      ResearchCategory rc = ResearchCategories.getResearchCategory(cat);
      ResearchManager.addKnowledge(p, EnumKnowledgeType.OBSERVATION, rc, Mth.nextInt(p.getRandom(), oProg / 2, oProg));
      ResearchManager.addKnowledge(p, EnumKnowledgeType.THEORY, rc, Mth.nextInt(p.getRandom(), tProg / 3, tProg / 2));
   }

   @Override
   public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
      ItemStack stack = player.getItemInHand(hand);
      level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.LEARN.get(), SoundSource.NEUTRAL, 0.5F,
         0.4F / (level.random.nextFloat() * 0.4F + 0.8F));
      if (!level.isClientSide) {
         switch (variant) {
            case "preserved" -> knowledge(player, "ALCHEMY");
            case "ancient" -> knowledge(player, "GOLEMANCY");
            case "eldritch" -> {
               knowledge(player, "ELDRITCH");
               ResearchManager.addWarp(player, 1, PlayerWarp.Type.NORMAL);
               ResearchManager.addWarp(player, 5, PlayerWarp.Type.TEMPORARY);
            }
            case "knowledge" -> knowledge(player, "INFUSION");
            case "twisted" -> knowledge(player, "ARTIFICE");
            case "rites" -> {
               PlayerWarp w = ResearchManager.warp(player);
               if (w.get(PlayerWarp.Type.PERMANENT) + w.get(PlayerWarp.Type.NORMAL) <= 20) {
                  player.sendSystemMessage(Component.translatable("fail.crimsonrites").withStyle(ChatFormatting.DARK_PURPLE));
                  return InteractionResultHolder.pass(stack);
               }
               if (!ResearchManager.knowledge(player).isResearchKnown("CrimsonRites")) ResearchManager.completeResearch(player, "CrimsonRites");
               knowledge(player, "ELDRITCH");
               ResearchManager.addWarp(player, 1, PlayerWarp.Type.NORMAL);
               ResearchManager.addWarp(player, 5, PlayerWarp.Type.TEMPORARY);
               if (player.getRandom().nextBoolean()) ResearchManager.addWarp(player, 1, PlayerWarp.Type.PERMANENT);
            }
            default -> knowledge(player, "AUROMANCY");
         }
         ResearchCategory[] rc = ResearchCategories.CATEGORIES.values().toArray(new ResearchCategory[0]);
         int oProg = EnumKnowledgeType.OBSERVATION.getProgression();
         int tProg = EnumKnowledgeType.THEORY.getProgression();
         ResearchManager.addKnowledge(player, EnumKnowledgeType.OBSERVATION, rc[player.getRandom().nextInt(rc.length)], Mth.nextInt(player.getRandom(), oProg / 2, oProg));
         ResearchManager.addKnowledge(player, EnumKnowledgeType.THEORY, rc[player.getRandom().nextInt(rc.length)], Mth.nextInt(player.getRandom(), tProg / 3, tProg / 2));
         if (!player.getAbilities().instabuild) stack.shrink(1);
         player.sendSystemMessage(Component.translatable("tc.knowledge.gained").withStyle(ChatFormatting.DARK_PURPLE));
      }
      player.awardStat(Stats.ITEM_USED.get(this));
      return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
   }
}
