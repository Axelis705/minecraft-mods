package com.autoscoleccion.server;

import com.autoscoleccion.AutosMod;
import com.autoscoleccion.game.Boxes;
import com.autoscoleccion.game.Cars;
import com.autoscoleccion.net.AutosNet;
import com.autoscoleccion.net.CollectionPacket;
import com.autoscoleccion.registry.ModItems;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Collection;
import java.util.List;

/**
 * Comandos (para operadores): /autos desbloquear [jugadores] llena todo el álbum, /autos reiniciar [jugadores] lo vacía y
 * /autos caja &lt;0-3&gt; [cantidad] da cajas (0 Ruta Urbana, 1 Pista Nocturna, 2 Leyendas, 3 Exclusivos).
 */
@Mod.EventBusSubscriber(modid = AutosMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class AutosCommands {
    private AutosCommands() {
    }

    @SubscribeEvent
    public static void register(RegisterCommandsEvent e) {
        CommandDispatcher<CommandSourceStack> d = e.getDispatcher();
        d.register(Commands.literal("autos").requires(s -> s.hasPermission(2))
                .then(Commands.literal("desbloquear")
                        .executes(c -> unlock(c.getSource(), List.of(c.getSource().getPlayerOrException()), true))
                        .then(Commands.argument("jugadores", EntityArgument.players())
                                .executes(c -> unlock(c.getSource(), EntityArgument.getPlayers(c, "jugadores"), true))))
                .then(Commands.literal("reiniciar")
                        .executes(c -> unlock(c.getSource(), List.of(c.getSource().getPlayerOrException()), false))
                        .then(Commands.argument("jugadores", EntityArgument.players())
                                .executes(c -> unlock(c.getSource(), EntityArgument.getPlayers(c, "jugadores"), false))))
                .then(Commands.literal("caja")
                        .then(Commands.argument("tipo", IntegerArgumentType.integer(0, Boxes.COUNT - 1))
                                .executes(c -> box(c.getSource(), IntegerArgumentType.getInteger(c, "tipo"), 1))
                                .then(Commands.argument("cantidad", IntegerArgumentType.integer(1, 64))
                                        .executes(c -> box(c.getSource(), IntegerArgumentType.getInteger(c, "tipo"),
                                                IntegerArgumentType.getInteger(c, "cantidad")))))));
    }

    private static int unlock(CommandSourceStack src, Collection<ServerPlayer> players, boolean all) {
        for (ServerPlayer sp : players) {
            int[] c = AutosData.counts(sp);
            for (int i = 0; i < Cars.COUNT; i++) {
                c[i] = all ? Math.max(1, c[i]) : 0;
            }
            AutosData.setCounts(sp, c);
            AutosNet.toPlayer(sp, new CollectionPacket(c, false));
            sp.sendSystemMessage(Component.literal(all ? "§a¡Álbum completo! Ya tienes los " + Cars.COUNT + " autos." : "§eTu álbum de autos quedó vacío."));
        }
        src.sendSuccess(() -> Component.literal((all ? "Álbum desbloqueado para " : "Álbum reiniciado para ") + players.size() + " jugador(es)."), true);
        return players.size();
    }

    private static int box(CommandSourceStack src, int type, int count) throws CommandSyntaxException {
        ServerPlayer sp = src.getPlayerOrException();
        ItemStack st = new ItemStack(ModItems.BOXES.get(type).get(), count);
        if (!sp.getInventory().add(st)) {
            sp.drop(st, false);
        }
        src.sendSuccess(() -> Component.literal("Te di " + count + " caja(s) " + Boxes.NAME[type] + "."), false);
        return count;
    }
}
