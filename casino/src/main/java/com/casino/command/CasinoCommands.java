package com.casino.command;

import com.casino.Casino;
import com.casino.chip.Chips;
import com.casino.data.CasinoData;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Comandos de administración (nivel 2):
 * <ul>
 *   <li>/casino resetwheel [jugadores] - el jugador puede volver a girar la ruleta hoy</li>
 *   <li>/casino chips &lt;jugadores&gt; &lt;cantidad&gt; - regala fichas</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = Casino.MODID)
public final class CasinoCommands {
    private CasinoCommands() {
    }

    @SubscribeEvent
    public static void register(RegisterCommandsEvent e) {
        CommandDispatcher<CommandSourceStack> d = e.getDispatcher();
        d.register(Commands.literal("casino").requires(s -> s.hasPermission(2))
                .then(Commands.literal("resetwheel")
                        .executes(c -> {
                            ServerPlayer p = c.getSource().getPlayerOrException();
                            CasinoData.get(c.getSource().getServer()).resetSpin(p.getUUID());
                            c.getSource().sendSuccess(() -> Component.translatable("casino.cmd.reset", p.getName()), true);
                            return 1;
                        })
                        .then(Commands.argument("players", EntityArgument.players()).executes(c -> {
                            int n = 0;
                            for (ServerPlayer p : EntityArgument.getPlayers(c, "players")) {
                                CasinoData.get(c.getSource().getServer()).resetSpin(p.getUUID());
                                n++;
                            }
                            int count = n;
                            c.getSource().sendSuccess(() -> Component.translatable("casino.cmd.reset_many", count), true);
                            return n;
                        })))
                .then(Commands.literal("chips")
                        .then(Commands.argument("players", EntityArgument.players())
                                .then(Commands.argument("amount", IntegerArgumentType.integer(1, 1_000_000)).executes(c -> {
                                    int amount = IntegerArgumentType.getInteger(c, "amount");
                                    int n = 0;
                                    for (ServerPlayer p : EntityArgument.getPlayers(c, "players")) {
                                        Chips.give(p, amount);
                                        n++;
                                    }
                                    int count = n;
                                    c.getSource().sendSuccess(() -> Component.translatable("casino.cmd.chips", amount, count), true);
                                    return n;
                                })))));
    }
}
