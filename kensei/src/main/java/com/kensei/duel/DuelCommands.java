package com.kensei.duel;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * /kensei duel bot [1v1|2v2] (duelo de práctica contra rivales) · /kensei duel challenge &lt;jugador&gt; · /kensei duel 2v2 &lt;aliado&gt; &lt;rival1&gt; &lt;rival2&gt; · accept · decline · leave · watch &lt;jugador&gt; · end (admin).
 * Casi todo se puede hacer también con la tecla de duelo y con los botones del chat.
 */
public final class DuelCommands {
    private DuelCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("kensei").then(Commands.literal("duel")
                .then(Commands.literal("challenge").then(Commands.argument("player", EntityArgument.player()).executes(c -> {
                    DuelManager.challenge(c.getSource().getPlayerOrException(), EntityArgument.getPlayer(c, "player"));
                    return 1;
                })))
                .then(Commands.literal("2v2").then(Commands.argument("ally", EntityArgument.player())
                        .then(Commands.argument("foe1", EntityArgument.player()).then(Commands.argument("foe2", EntityArgument.player()).executes(c -> {
                            DuelManager.challenge2v2(c.getSource().getPlayerOrException(), EntityArgument.getPlayer(c, "ally"),
                                    EntityArgument.getPlayer(c, "foe1"), EntityArgument.getPlayer(c, "foe2"));
                            return 1;
                        })))))
                .then(Commands.literal("bot")
                        .executes(c -> {
                            DuelManager.startBotDuel(c.getSource().getPlayerOrException(), false);
                            return 1;
                        })
                        .then(Commands.literal("1v1").executes(c -> {
                            DuelManager.startBotDuel(c.getSource().getPlayerOrException(), false);
                            return 1;
                        }))
                        .then(Commands.literal("2v2").executes(c -> {
                            DuelManager.startBotDuel(c.getSource().getPlayerOrException(), true);
                            return 1;
                        })))
                .then(Commands.literal("accept").executes(c -> {
                    DuelManager.accept(c.getSource().getPlayerOrException());
                    return 1;
                }))
                .then(Commands.literal("decline").executes(c -> {
                    DuelManager.decline(c.getSource().getPlayerOrException());
                    return 1;
                }))
                .then(Commands.literal("leave").executes(c -> {
                    DuelManager.leave(c.getSource().getPlayerOrException());
                    return 1;
                }))
                .then(Commands.literal("watch").then(Commands.argument("player", EntityArgument.player()).executes(c -> {
                    DuelManager.watch(c.getSource().getPlayerOrException(), EntityArgument.getPlayer(c, "player"));
                    return 1;
                })))
                .then(Commands.literal("end").requires(s -> s.hasPermission(2)).executes(c -> {
                    int n = DuelManager.endAll();
                    c.getSource().sendSuccess(() -> Component.translatable("kensei.duel.ended_all", n), true);
                    return n;
                }))));
    }
}
