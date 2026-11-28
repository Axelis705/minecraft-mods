package com.coleccionista.hotel;

import com.coleccionista.Coleccionista;
import com.coleccionista.evento.Evento;
import com.coleccionista.evento.Pieza;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.Entity;

/**
 * Los comandos del hotel: {@code /evento hotel [empezar|ascensores|bajar|terminar|limpiar|usar <si>|ultimo <si>|tiempos <juego> <entrar>
 * <viaje>|trampas <n>|abrir <salida|trampa|n> <pieza>|salir <pieza>|frente <n> <pieza>]}.
 */
public final class ComandosHotel {
    private static final String MOD = Coleccionista.MODID;

    private ComandosHotel() {
    }

    private static int ok(CommandContext<CommandSourceStack> c, Component m) {
        c.getSource().sendSuccess(() -> m, true);
        return 1;
    }

    private static int resumen(CommandContext<CommandSourceStack> c) {
        return ok(c, Component.literal("hotel: " + JuegoHotel.resumen()));
    }

    private static int resultado(CommandContext<CommandSourceStack> c, Component no) {
        if (no != null) {
            c.getSource().sendFailure(no);
            return 0;
        }
        return resumen(c);
    }

    private static int si(CommandContext<CommandSourceStack> c, boolean hecho) {
        if (!hecho) {
            c.getSource().sendFailure(Component.translatable(MOD + ".hotel.cmd.no"));
            return 0;
        }
        return resumen(c);
    }

    private static Entity entidad(CommandContext<CommandSourceStack> c) {
        MinecraftServer s = c.getSource().getServer();
        Pieza p = Evento.get(s).buscar(StringArgumentType.getString(c, "pieza"));
        return p == null ? null : Evento.entidad(s, p);
    }

    public static LiteralArgumentBuilder<CommandSourceStack> hotel(SuggestionProvider<CommandSourceStack> vivas) {
        return Commands.literal("hotel")
                .executes(ComandosHotel::resumen)
                .then(Commands.literal("empezar").executes(c -> resultado(c, JuegoHotel.empezar(c.getSource().getServer()))))
                .then(Commands.literal("ascensores").executes(c -> {
                    JuegoHotel.abrir(c.getSource().getServer());
                    return resumen(c);
                }))
                .then(Commands.literal("bajar").executes(c -> {
                    JuegoHotel.bajar(c.getSource().getServer());
                    return resumen(c);
                }))
                .then(Commands.literal("terminar").executes(c -> si(c, JuegoHotel.fin(c.getSource().getServer()))))
                .then(Commands.literal("limpiar").executes(c -> {
                    JuegoHotel.reiniciar(c.getSource().getServer());
                    return resumen(c);
                }))
                .then(Commands.literal("usar").then(Commands.argument("si", BoolArgumentType.bool()).executes(c -> {
                    JuegoHotel.usar = BoolArgumentType.getBool(c, "si");
                    return ok(c, Component.literal("hotel usar=" + JuegoHotel.usar));
                })))
                .then(Commands.literal("ultimo").then(Commands.argument("si", BoolArgumentType.bool()).executes(c -> {
                    JuegoHotel.ultimoCae = BoolArgumentType.getBool(c, "si");
                    return ok(c, Component.literal("hotel ultimoCae=" + JuegoHotel.ultimoCae));
                })))
                .then(Commands.literal("tiempos").then(Commands.argument("juego", IntegerArgumentType.integer(30, 3600))
                        .then(Commands.argument("entrar", IntegerArgumentType.integer(5, 600))
                                .then(Commands.argument("viaje", IntegerArgumentType.integer(60, 2400)).executes(c -> {
                                    JuegoHotel.segundos = IntegerArgumentType.getInteger(c, "juego");
                                    JuegoHotel.entrarSeg = IntegerArgumentType.getInteger(c, "entrar");
                                    JuegoHotel.viajeTicks = IntegerArgumentType.getInteger(c, "viaje");
                                    return ok(c, Component.literal("hotel juego=" + JuegoHotel.segundos + "s entrar=" + JuegoHotel.entrarSeg
                                            + "s viaje=" + JuegoHotel.viajeTicks + "t"));
                                })))))
                .then(Commands.literal("trampas").then(Commands.argument("n", IntegerArgumentType.integer(0, 8)).executes(c -> {
                    JuegoHotel.trampas = IntegerArgumentType.getInteger(c, "n");
                    return ok(c, Component.literal("hotel trampas=" + JuegoHotel.trampas));
                })))
                // para probar: alguien abre una puerta (la de la salida, una con trampa o la N), cae por el tobogán o se para frente a una
                .then(Commands.literal("abrir").then(Commands.argument("cual", StringArgumentType.word())
                        .then(Commands.argument("pieza", StringArgumentType.word()).suggests(vivas).executes(c -> {
                            Entity e = entidad(c);
                            String cual = StringArgumentType.getString(c, "cual");
                            int k = cual.equals("salida") ? JuegoHotel.puertaDe(false) : cual.equals("trampa") ? JuegoHotel.puertaDe(true)
                                    : parse(cual);
                            if (e == null || k < 0) {
                                return si(c, false);
                            }
                            JuegoHotel.frente(c.getSource().getServer(), e, k);
                            JuegoHotel.abrir(c.getSource().getServer(), e, k);
                            return resumen(c);
                        }))))
                .then(Commands.literal("salir").then(Commands.argument("pieza", StringArgumentType.word()).suggests(vivas).executes(c -> {
                    Entity e = entidad(c);
                    if (e == null) {
                        return si(c, false);
                    }
                    JuegoHotel.salir(c.getSource().getServer(), e);
                    return resumen(c);
                })))
                .then(Commands.literal("frente").then(Commands.argument("n", IntegerArgumentType.integer(0, 63))
                        .then(Commands.argument("pieza", StringArgumentType.word()).suggests(vivas).executes(c -> {
                            Entity e = entidad(c);
                            return si(c, e != null && JuegoHotel.frente(c.getSource().getServer(), e, IntegerArgumentType.getInteger(c, "n")));
                        }))));
    }

    private static int parse(String s) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
