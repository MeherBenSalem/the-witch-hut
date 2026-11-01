package tn.nightbeam.betterwitchhuts.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import java.util.Arrays;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import tn.nightbeam.betterwitchhuts.core.Errand;
import tn.nightbeam.betterwitchhuts.core.Hex;
import tn.nightbeam.betterwitchhuts.core.Objectives;
import tn.nightbeam.betterwitchhuts.core.Pact;
import tn.nightbeam.betterwitchhuts.core.PlayerRecord;
import tn.nightbeam.betterwitchhuts.core.Standing;
import tn.nightbeam.betterwitchhuts.core.WitchConfig;
import tn.nightbeam.betterwitchhuts.game.Coven;
import tn.nightbeam.betterwitchhuts.game.Effects;
import tn.nightbeam.betterwitchhuts.game.HexRules;
import tn.nightbeam.betterwitchhuts.game.Records;

/**
 * {@code /witchbound} shows your own standing, pacts and hexes. Operators: {@code /witchbound info|standing|pact|hex|
 * errand <player> ...} to read and change any player's record (for servers, modpack quests and testing).
 */
public final class WitchboundCommand {
    public static final String NAME = "witchbound";
    private static final SuggestionProvider<CommandSourceStack> PACTS = (c, b) ->
            SharedSuggestionProvider.suggest(Arrays.stream(Pact.values()).map(Pact::id), b);
    private static final SuggestionProvider<CommandSourceStack> HEXES = (c, b) ->
            SharedSuggestionProvider.suggest(Arrays.stream(Hex.values()).map(Hex::id), b);

    private WitchboundCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal(NAME)
                .executes(c -> info(c.getSource(), c.getSource().getPlayerOrException()))
                .then(Commands.literal("info").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(c -> info(c.getSource(), EntityArgument.getPlayer(c, "player")))))
                .then(Commands.literal("standing").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.literal("set").then(Commands.argument("value", IntegerArgumentType.integer(Standing.MIN, Standing.MAX))
                                        .executes(c -> standing(c, true))))
                                .then(Commands.literal("add").then(Commands.argument("value", IntegerArgumentType.integer(-200, 200))
                                        .executes(c -> standing(c, false))))))
                .then(Commands.literal("pact").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.literal("grant").then(Commands.argument("pact", StringArgumentType.word()).suggests(PACTS)
                                        .executes(c -> pact(c, true))))
                                .then(Commands.literal("remove").then(Commands.argument("pact", StringArgumentType.word()).suggests(PACTS)
                                        .executes(c -> pact(c, false))))
                                .then(Commands.literal("clear").executes(WitchboundCommand::clearPacts))))
                .then(Commands.literal("hex").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.literal("lay").executes(c -> lay(c, null))
                                        .then(Commands.argument("hex", StringArgumentType.word()).suggests(HEXES)
                                                .executes(c -> lay(c, StringArgumentType.getString(c, "hex")))))
                                .then(Commands.literal("clear").executes(WitchboundCommand::clearHexes))))
                .then(Commands.literal("errand").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.literal("clear").executes(WitchboundCommand::clearErrand)))));
    }

    private static int info(CommandSourceStack source, ServerPlayer player) {
        PlayerRecord record = Records.get(player);
        Standing.Tier tier = Standing.tier(record.standing);
        source.sendSuccess(() -> Component.translatable("command.better_witch_huts.info.standing", player.getDisplayName(),
                record.standing, Coven.tierName(tier)).withStyle(ChatFormatting.DARK_PURPLE), false);
        MutableComponent pacts = Component.translatable("command.better_witch_huts.info.pacts", record.pacts.size(),
                WitchConfig.get().pactSlots(tier));
        for (Pact pact : record.pactList()) {
            pacts.append(Component.literal(" ")).append(Coven.pactName(pact).withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        source.sendSuccess(() -> pacts, false);
        for (PlayerRecord.HexState state : record.hexes) {
            Hex hex = Hex.byId(state.hex);
            var task = Objectives.byId(state.task);
            if (hex != null && task != null) {
                int done = task.display(HexRules.progress(player, state));
                source.sendSuccess(() -> Component.translatable("command.better_witch_huts.info.hex", Coven.hexName(hex).withStyle(ChatFormatting.RED),
                        Coven.objectiveText(task), done, task.displayTarget()), false);
            }
        }
        if (record.errand != null) {
            Errand errand = Errand.byId(record.errand.errand);
            if (errand != null) {
                int done = errand.objective().display(Coven.errandProgress(player, record));
                source.sendSuccess(() -> Component.translatable("command.better_witch_huts.info.errand", Coven.objectiveText(errand.objective()),
                        done, errand.objective().displayTarget()), false);
            }
        }
        return record.standing;
    }

    private static int standing(CommandContext<CommandSourceStack> c, boolean set) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(c, "player");
        int value = IntegerArgumentType.getInteger(c, "value");
        PlayerRecord record = Records.get(player);
        Coven.changeStanding(player, set ? value - record.standing : value);
        c.getSource().sendSuccess(() -> Component.literal(player.getScoreboardName() + ": " + record.standing), true);
        return record.standing;
    }

    private static int pact(CommandContext<CommandSourceStack> c, boolean grant) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(c, "player");
        Pact pact = Pact.byId(StringArgumentType.getString(c, "pact"));
        if (pact == null) {
            c.getSource().sendFailure(Component.literal("Unknown pact"));
            return 0;
        }
        PlayerRecord record = Records.get(player);
        boolean changed = grant ? !record.hasPact(pact) && record.pacts.add(pact.id()) : record.pacts.remove(pact.id());
        record.dirty |= changed;
        Effects.sync(player, record);
        if (changed && grant) {
            Coven.award(player, "first_pact");
        }
        c.getSource().sendSuccess(() -> Component.literal((grant ? "Granted " : "Removed ") + pact.id() + " (" + player.getScoreboardName() + ")"), true);
        return changed ? 1 : 0;
    }

    private static int clearPacts(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(c, "player");
        PlayerRecord record = Records.get(player);
        int n = record.pacts.size();
        record.pacts.clear();
        record.dirty = true;
        Effects.sync(player, record);
        c.getSource().sendSuccess(() -> Component.literal("Cleared " + n + " pacts"), true);
        return n;
    }

    private static int lay(CommandContext<CommandSourceStack> c, String id) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(c, "player");
        Hex hex = id == null ? null : Hex.byId(id);
        if (id != null && hex == null) {
            c.getSource().sendFailure(Component.literal("Unknown hex"));
            return 0;
        }
        boolean landed = HexRules.lay(player, hex, c.getSource().getDisplayName(), false);
        c.getSource().sendSuccess(() -> Component.literal(landed ? "Hex laid" : "No hex laid (already carried, or at the limit)"), true);
        return landed ? 1 : 0;
    }

    private static int clearHexes(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(c, "player");
        PlayerRecord record = Records.get(player);
        int n = record.hexes.size();
        record.hexes.clear();
        record.dirty = true;
        Effects.sync(player, record);
        c.getSource().sendSuccess(() -> Component.literal("Cleared " + n + " hexes"), true);
        return n;
    }

    private static int clearErrand(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(c, "player");
        PlayerRecord record = Records.get(player);
        boolean had = record.errand != null;
        record.errand = null;
        record.dirty = true;
        c.getSource().sendSuccess(() -> Component.literal(had ? "Errand cleared" : "No errand"), true);
        return had ? 1 : 0;
    }
}
