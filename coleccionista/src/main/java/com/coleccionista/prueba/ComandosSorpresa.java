package com.coleccionista.prueba;

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

/**
 * Los comandos de la caja sorpresa: {@code /evento sorpresa [llamar|llegar|saltar|vueltas <1-3>|terminar|limpiar|comer <n>|secreto <n>|
 * primero <pieza>|encadenar <si>|rapido <si>|tiempos <ir> <volver>]}.
 */
public final class ComandosSorpresa {
    private static final String MOD = Coleccionista.MODID;

    private ComandosSorpresa() {
    }

    private static int ok(CommandContext<CommandSourceStack> c, Component m) {
        c.getSource().sendSuccess(() -> m, true);
        return 1;
    }

    private static int error(CommandContext<CommandSourceStack> c, Component m) {
        c.getSource().sendFailure(m);
        return 0;
    }

    private static int resultado(CommandContext<CommandSourceStack> c, Component no) {
        return no == null ? ok(c, Component.literal(Sorpresa.resumen())) : error(c, no);
    }

    private static int si(CommandContext<CommandSourceStack> c, boolean hecho) {
        return hecho ? ok(c, Component.literal(Sorpresa.resumen())) : error(c, Component.translatable(MOD + ".sor.cmd.no"));
    }

    public static LiteralArgumentBuilder<CommandSourceStack> sorpresa(SuggestionProvider<CommandSourceStack> vivas) {
        return Commands.literal("sorpresa")
                .executes(c -> ok(c, Component.literal(Sorpresa.resumen())))
                .then(Commands.literal("llamar").executes(c -> resultado(c, Sorpresa.llamar(c.getSource().getServer()))))
                .then(Commands.literal("llegar").executes(c -> resultado(c, Sorpresa.llegar(c.getSource().getServer()))))
                .then(Commands.literal("saltar").executes(c -> si(c, Sorpresa.saltar(c.getSource().getServer()))))
                .then(Commands.literal("vueltas").then(Commands.argument("n", IntegerArgumentType.integer(1, 3))
                        .executes(c -> si(c, Sorpresa.elegir(c.getSource().getServer(), IntegerArgumentType.getInteger(c, "n"))))))
                .then(Commands.literal("terminar").executes(c -> si(c, Sorpresa.terminar(c.getSource().getServer()))))
                .then(Commands.literal("limpiar").executes(c -> {
                    Sorpresa.reiniciar(c.getSource().getServer());
                    return ok(c, Component.literal(Sorpresa.resumen()));
                }))
                .then(Commands.literal("comer").then(Commands.argument("n", IntegerArgumentType.integer(0, 30)).executes(c -> {
                    Sorpresa.comer = IntegerArgumentType.getInteger(c, "n");
                    return ok(c, Component.literal("comer=" + Sorpresa.comer));
                })))
                .then(Commands.literal("secreto").then(Commands.argument("n", IntegerArgumentType.integer(1, 30)).executes(c -> {
                    Sorpresa.secreto(IntegerArgumentType.getInteger(c, "n"));
                    return ok(c, Component.literal(Sorpresa.resumen()));
                })))
                .then(Commands.literal("primero").then(Commands.argument("pieza", StringArgumentType.word()).suggests(vivas).executes(c -> {
                    Pieza p = Evento.get(c.getSource().getServer()).buscar(StringArgumentType.getString(c, "pieza"));
                    if (p == null) {
                        return error(c, Component.translatable(MOD + ".caja.cmd.no_pieza"));
                    }
                    Sorpresa.primero = p.id;
                    return ok(c, Component.literal("primero: " + p.nombre));
                })))
                .then(Commands.literal("encadenar").then(Commands.argument("si", BoolArgumentType.bool()).executes(c -> {
                    Sorpresa.encadenar = BoolArgumentType.getBool(c, "si");
                    return ok(c, Component.literal("encadenar=" + Sorpresa.encadenar));
                })))
                .then(Commands.literal("rapido").then(Commands.argument("si", BoolArgumentType.bool()).executes(c -> {
                    Sorpresa.rapido = BoolArgumentType.getBool(c, "si");
                    return ok(c, Component.literal("rapido=" + Sorpresa.rapido));
                })))
                .then(Commands.literal("tiempos").then(Commands.argument("ir", IntegerArgumentType.integer(5, 300))
                        .then(Commands.argument("volver", IntegerArgumentType.integer(5, 300)).executes(c -> {
                            Sorpresa.segundosIr = IntegerArgumentType.getInteger(c, "ir");
                            Sorpresa.segundosVolver = IntegerArgumentType.getInteger(c, "volver");
                            return ok(c, Component.literal("ir=" + Sorpresa.segundosIr + " volver=" + Sorpresa.segundosVolver));
                        }))))
                // tiempo máximo de la caja (0 = sin límite): al acabarse, termina el turno que va y se acaba con los que ya se comió
                .then(Commands.literal("maximo").executes(c -> ok(c, Component.literal("maximo=" + Sorpresa.segundosMax + " s")))
                        .then(Commands.argument("segundos", IntegerArgumentType.integer(0, 3600)).executes(c -> {
                            Sorpresa.segundosMax = IntegerArgumentType.getInteger(c, "segundos");
                            return ok(c, Component.literal("maximo=" + Sorpresa.segundosMax + " s"));
                        })));
    }
}
