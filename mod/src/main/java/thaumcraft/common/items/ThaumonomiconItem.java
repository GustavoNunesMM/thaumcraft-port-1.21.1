package thaumcraft.common.items;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import thaumcraft.api.research.ResearchEntry;
import thaumcraft.api.research.ResearchCategories;
import thaumcraft.common.config.ModConfig;
import thaumcraft.common.lib.ClientBridge;
import thaumcraft.common.registers.ModSounds;
import thaumcraft.common.research.ResearchManager;

/** ItemThaumonomicon. The cheat sheet (creative only, if enabled in the config) grants all research when opened. */
public class ThaumonomiconItem extends Item {
   private final boolean cheat;

   public ThaumonomiconItem(boolean cheat, Properties properties) {
      super(properties);
      this.cheat = cheat;
   }

   @Override
   public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
      if (player instanceof ServerPlayer sp) {
         if (cheat && ModConfig.allowCheatSheet()) {
            ResearchManager.giveAllResearch(sp);
         } else {
            // Catch up on siblings of known research (added by datapack updates, for example).
            for (ResearchEntry ri : ResearchCategories.allResearch()) {
               if (ResearchManager.knowsResearch(sp, ri.getKey())) {
                  for (String sib : ri.getSiblings()) {
                     if (!ResearchManager.knowsResearch(sp, sib)) ResearchManager.completeResearch(sp, sib);
                  }
               }
            }
         }
         ResearchManager.sync(sp);
      } else {
         level.playLocalSound(player.getX(), player.getY(), player.getZ(), ModSounds.PAGE.get(), SoundSource.PLAYERS, 1.0F, 1.0F, false);
         ClientBridge.openThaumonomicon.run();
      }
      return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
   }

   @Override
   public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
      if (cheat) tooltip.add(Component.literal("Creative only").withStyle(ChatFormatting.DARK_PURPLE));
   }
}
