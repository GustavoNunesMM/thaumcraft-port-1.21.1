package thaumcraft.common.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import java.util.stream.StreamSupport;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.research.EnumKnowledgeType;
import thaumcraft.api.research.ResearchCategories;
import thaumcraft.api.research.ResearchCategory;
import thaumcraft.api.research.ResearchEntry;
import thaumcraft.common.aspects.AspectManager;
import thaumcraft.common.network.ModNetwork;
import thaumcraft.common.network.SyncWarpPayload;
import thaumcraft.common.research.PlayerKnowledge;
import thaumcraft.common.research.PlayerWarp;
import thaumcraft.common.research.ResearchManager;
import thaumcraft.common.world.aura.AuraChunk;
import thaumcraft.common.world.aura.AuraHandler;

/**
 * /thaumcraft (aliases /thaum, /tc), as CommandThaumcraft:
 * research list | research &lt;player&gt; list|all|reset|&lt;key&gt;|revoke &lt;key&gt; | warp &lt;player&gt; add|set &lt;n&gt; [PERM|TEMP],
 * plus the port's helpers: aspects | knowledge | aura [set vis|flux|base value].
 */
public final class ThaumcraftCommand {
   private static final SuggestionProvider<CommandSourceStack> RESEARCH = (c, b) -> SharedSuggestionProvider.suggest(
      StreamSupport.stream(ResearchCategories.allResearch().spliterator(), false).map(ResearchEntry::getKey), b);

   private ThaumcraftCommand() {
   }

   public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext context) {
      var root = dispatcher.register(build("thaumcraft"));
      dispatcher.register(Commands.literal("thaum").redirect(root));
      dispatcher.register(Commands.literal("tc").redirect(root));
   }

   private static LiteralArgumentBuilder<CommandSourceStack> build(String name) {
      return Commands.literal(name)
         .then(Commands.literal("aspects").executes(ThaumcraftCommand::aspects))
         .then(Commands.literal("knowledge").executes(ThaumcraftCommand::knowledge))
         .then(Commands.literal("aura")
            .executes(ThaumcraftCommand::aura)
            .then(Commands.literal("set").requires(s -> s.hasPermission(2))
               .then(Commands.argument("what", StringArgumentType.word())
                  .suggests((c, b) -> SharedSuggestionProvider.suggest(new String[]{"vis", "flux", "base"}, b))
                  .then(Commands.argument("value", FloatArgumentType.floatArg(0, 32766)).executes(ThaumcraftCommand::setAura)))))
         .then(Commands.literal("research").requires(s -> s.hasPermission(2))
            .then(Commands.literal("list").executes(ThaumcraftCommand::listResearch))
            .then(Commands.argument("player", EntityArgument.player())
               .then(Commands.literal("list").executes(ThaumcraftCommand::listPlayerResearch))
               .then(Commands.literal("all").executes(ThaumcraftCommand::giveAll))
               .then(Commands.literal("reset").executes(ThaumcraftCommand::reset))
               .then(Commands.literal("revoke").then(Commands.argument("key", StringArgumentType.string()).suggests(RESEARCH)
                  .executes(ThaumcraftCommand::revoke)))
               .then(Commands.argument("key", StringArgumentType.string()).suggests(RESEARCH).executes(ThaumcraftCommand::give))))
         .then(Commands.literal("warp").requires(s -> s.hasPermission(2))
            .then(Commands.argument("player", EntityArgument.player())
               .then(Commands.literal("add").then(warpAmount(true)))
               .then(Commands.literal("set").then(warpAmount(false)))));
   }

   private static com.mojang.brigadier.builder.RequiredArgumentBuilder<CommandSourceStack, Integer> warpAmount(boolean add) {
      return Commands.argument("amount", IntegerArgumentType.integer(add ? -100 : 0, add ? 100 : 10000))
         .executes(c -> warp(c, add, ""))
         .then(Commands.argument("type", StringArgumentType.word())
            .suggests((c, b) -> SharedSuggestionProvider.suggest(new String[]{"PERM", "TEMP"}, b))
            .executes(c -> warp(c, add, StringArgumentType.getString(c, "type"))));
   }

   private static void success(CommandContext<CommandSourceStack> ctx, String text) {
      ctx.getSource().sendSuccess(() -> Component.literal(text).withStyle(ChatFormatting.DARK_PURPLE), true);
   }

   // ------------------------------------------------------------------ research

   private static int listResearch(CommandContext<CommandSourceStack> ctx) {
      int n = 0;
      for (ResearchEntry ri : ResearchCategories.allResearch()) {
         ctx.getSource().sendSuccess(() -> Component.literal(ri.getKey()).withStyle(ChatFormatting.DARK_PURPLE), false);
         n++;
      }
      return n;
   }

   private static int listPlayerResearch(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
      ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
      var list = ResearchManager.knowledge(player).getResearchList();
      ctx.getSource().sendSuccess(() -> Component.literal("Research for " + player.getGameProfile().getName()).withStyle(ChatFormatting.DARK_PURPLE), false);
      ctx.getSource().sendSuccess(() -> Component.literal(String.join(", ", list)).withStyle(ChatFormatting.DARK_PURPLE), false);
      return list.size();
   }

   private static int give(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
      ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
      String key = StringArgumentType.getString(ctx, "key");
      if (ResearchCategories.getResearch(key) == null) {
         ctx.getSource().sendFailure(Component.literal("Research does not exist."));
         return 0;
      }
      ResearchManager.giveRecursiveResearch(player, key);
      ResearchManager.sync(player);
      player.sendSystemMessage(Component.literal(ctx.getSource().getTextName() + " gave you " + key + " research and its requisites.")
         .withStyle(ChatFormatting.DARK_PURPLE));
      success(ctx, "Success!");
      return 1;
   }

   private static int revoke(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
      ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
      String key = StringArgumentType.getString(ctx, "key");
      if (ResearchCategories.getResearch(key) == null) {
         ctx.getSource().sendFailure(Component.literal("Research does not exist."));
         return 0;
      }
      ResearchManager.revokeRecursiveResearch(player, key);
      ResearchManager.sync(player);
      player.sendSystemMessage(Component.literal(ctx.getSource().getTextName() + " removed " + key + " research and its children.")
         .withStyle(ChatFormatting.DARK_PURPLE));
      success(ctx, "Success!");
      return 1;
   }

   private static int giveAll(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
      ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
      ResearchManager.giveAllResearch(player);
      ResearchManager.sync(player);
      player.sendSystemMessage(Component.literal(ctx.getSource().getTextName() + " has given you all research.").withStyle(ChatFormatting.DARK_PURPLE));
      success(ctx, "Success!");
      return 1;
   }

   private static int reset(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
      ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
      ResearchManager.resetResearch(player);
      ResearchManager.sync(player);
      player.sendSystemMessage(Component.literal(ctx.getSource().getTextName() + " has reset all your research.").withStyle(ChatFormatting.DARK_PURPLE));
      success(ctx, "Success!");
      return 1;
   }

   private static int warp(CommandContext<CommandSourceStack> ctx, boolean add, String type) throws CommandSyntaxException {
      ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
      int amount = IntegerArgumentType.getInteger(ctx, "amount");
      PlayerWarp.Type t = type.equalsIgnoreCase("PERM") ? PlayerWarp.Type.PERMANENT
         : type.equalsIgnoreCase("TEMP") ? PlayerWarp.Type.TEMPORARY : PlayerWarp.Type.NORMAL;
      if (add) {
         ResearchManager.addWarp(player, amount, t);
         player.sendSystemMessage(Component.literal(ctx.getSource().getTextName() + " added " + amount + " warp to your total.")
            .withStyle(ChatFormatting.DARK_PURPLE));
      } else {
         ResearchManager.warp(player).set(t, amount);
         ModNetwork.sendTo(player, new SyncWarpPayload(ResearchManager.warp(player).copy()));
         player.sendSystemMessage(Component.literal(ctx.getSource().getTextName() + " set your warp to " + amount).withStyle(ChatFormatting.DARK_PURPLE));
      }
      success(ctx, "Success!");
      return 1;
   }

   // ------------------------------------------------------------------ port helpers

   private static int aspects(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
      ServerPlayer player = ctx.getSource().getPlayerOrException();
      ItemStack stack = player.getMainHandItem();
      AspectList list = AspectManager.SERVER.getObjectAspects(player.registryAccess(), stack);
      if (list == null) {
         ctx.getSource().sendSuccess(() -> Component.translatable("tc.port.cmd.noaspects", stack.getHoverName()), false);
         return 0;
      }
      MutableComponent line = Component.empty().append(stack.getHoverName()).append(": ");
      for (Aspect a : list.getAspectsSortedByAmount()) {
         line.append(a.getLocalizedName()).append(" " + list.getAmount(a) + "  ");
      }
      ctx.getSource().sendSuccess(() -> line, false);
      return list.size();
   }

   private static int knowledge(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
      ServerPlayer player = ctx.getSource().getPlayerOrException();
      PlayerKnowledge k = ResearchManager.knowledge(player);
      long aspects = Aspect.all().stream().filter(a -> k.isResearchKnown("!" + a.getTag())).count();
      long objects = k.getResearchList().stream().filter(r -> r.startsWith("!")).count() - aspects;
      ctx.getSource().sendSuccess(() -> Component.translatable("tc.port.cmd.knowledge", objects, aspects, Aspect.all().size()), false);
      for (ResearchCategory cat : ResearchCategories.CATEGORIES.values()) {
         String line = String.format("  %s: theory %d (%d), observation %d (%d)", cat.key,
            k.getKnowledge(EnumKnowledgeType.THEORY, cat), k.getKnowledgeRaw(EnumKnowledgeType.THEORY, cat),
            k.getKnowledge(EnumKnowledgeType.OBSERVATION, cat), k.getKnowledgeRaw(EnumKnowledgeType.OBSERVATION, cat));
         ctx.getSource().sendSuccess(() -> Component.literal(line), false);
      }
      return (int) objects;
   }

   private static int aura(CommandContext<CommandSourceStack> ctx) {
      BlockPos pos = BlockPos.containing(ctx.getSource().getPosition());
      AuraChunk ac = AuraHandler.getAuraChunk(ctx.getSource().getLevel(), pos);
      if (ac == null) {
         ctx.getSource().sendFailure(Component.translatable("tc.port.cmd.noaura"));
         return 0;
      }
      ctx.getSource().sendSuccess(() -> Component.translatable("tc.port.cmd.aura",
         ac.getBase(), String.format("%.1f", ac.getVis()), String.format("%.1f", ac.getFlux())), false);
      return (int) ac.getVis();
   }

   private static int setAura(CommandContext<CommandSourceStack> ctx) {
      BlockPos pos = BlockPos.containing(ctx.getSource().getPosition());
      AuraChunk ac = AuraHandler.getAuraChunk(ctx.getSource().getLevel(), pos);
      if (ac == null) {
         ctx.getSource().sendFailure(Component.translatable("tc.port.cmd.noaura"));
         return 0;
      }
      float value = FloatArgumentType.getFloat(ctx, "value");
      switch (StringArgumentType.getString(ctx, "what")) {
         case "vis" -> ac.setVis(value);
         case "flux" -> ac.setFlux(value);
         case "base" -> ac.setBase((short) Math.min(value, Short.MAX_VALUE));
         default -> {
            ctx.getSource().sendFailure(Component.literal("vis | flux | base"));
            return 0;
         }
      }
      ctx.getSource().getLevel().getChunkAt(pos).setUnsaved(true);
      return aura(ctx);
   }
}
