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
 * Los comandos del piano gigante: {@code /evento piano [llamar|llegar|saltar|terminar|limpiar|caen <0 auto, 1-10>|nota <segundos>|
 * inicio <notas>|fallar <pieza>|fallan <n por ronda...>|pisar <pieza> <1-8>|bien <pieza>|encadenar <si>|rapido <si>]}.
 */
public final class ComandosPiano {
    private static final String MOD = Coleccionista.MODID;

    private ComandosPiano() {
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
        return no == null ? ok(c, Component.literal(Piano.resumen())) : error(c, no);
    }

    private static int si(CommandContext<CommandSourceStack> c, boolean hecho) {
        return hecho ? ok(c, Component.literal(Piano.resumen())) : error(c, Component.translatable(MOD + ".piano.cmd.no"));
    }

    private static Pieza pieza(CommandContext<CommandSourceStack> c) {
        return Evento.get(c.getSource().getServer()).buscar(StringArgumentType.getString(c, "pieza"));
    }

    public static LiteralArgumentBuilder<CommandSourceStack> piano(SuggestionProvider<CommandSourceStack> vivas) {
        return Commands.literal("piano")
                .executes(c -> ok(c, Component.literal(Piano.resumen())))
                .then(Commands.literal("llamar").executes(c -> resultado(c, Piano.llamar(c.getSource().getServer()))))
                .then(Commands.literal("llegar").executes(c -> resultado(c, Piano.llegar(c.getSource().getServer()))))
                .then(Commands.literal("saltar").executes(c -> si(c, Piano.saltar(c.getSource().getServer()))))
                .then(Commands.literal("terminar").executes(c -> si(c, Piano.terminar(c.getSource().getServer()))))
                .then(Commands.literal("limpiar").executes(c -> {
                    Piano.reiniciar(c.getSource().getServer());
                    return ok(c, Component.literal(Piano.resumen()));
                }))
                .then(Commands.literal("caen").then(Commands.argument("n", IntegerArgumentType.integer(0, 10)).executes(c -> {
                    Piano.caer = IntegerArgumentType.getInteger(c, "n");
                    return ok(c, Component.literal("caen=" + (Piano.caer == 0 ? "auto" : String.valueOf(Piano.caer))));
                })))
                .then(Commands.literal("nota").then(Commands.argument("segundos", IntegerArgumentType.integer(2, 30)).executes(c -> {
                    Piano.segundosNota = IntegerArgumentType.getInteger(c, "segundos");
                    return ok(c, Component.literal("nota=" + Piano.segundosNota + "s"));
                })))
                .then(Commands.literal("inicio").then(Commands.argument("notas", IntegerArgumentType.integer(1, 12)).executes(c -> {
                    Piano.notasInicio = IntegerArgumentType.getInteger(c, "notas");
                    return ok(c, Component.literal("inicio=" + Piano.notasInicio));
                })))
                .then(Commands.literal("fallar").then(Commands.argument("pieza", StringArgumentType.word()).suggests(vivas).executes(c -> {
                    Pieza p = pieza(c);
                    if (p == null) {
                        return error(c, Component.translatable(MOD + ".caja.cmd.no_pieza"));
                    }
                    Piano.fallar(p.id);
                    return ok(c, Component.literal("fallar: " + p.nombre));
                })))
                .then(Commands.literal("fallan").then(Commands.argument("n", StringArgumentType.greedyString()).executes(c -> {
                    java.util.List<Integer> l = new java.util.ArrayList<>();
                    for (String x : StringArgumentType.getString(c, "n").trim().split("\\s+")) {
                        try {
                            l.add(Integer.parseInt(x));
                        } catch (NumberFormatException e) {
                            return error(c, Component.literal("fallan <n> [<n> ...]"));
                        }
                    }
                    Piano.fallanEnRondas(l);
                    return ok(c, Component.literal("fallan=" + l));
                })))
                .then(Commands.literal("pisar").then(Commands.argument("pieza", StringArgumentType.word()).suggests(vivas)
                        .then(Commands.argument("tecla", IntegerArgumentType.integer(1, 8)).executes(c -> {
                            Pieza p = pieza(c);
                            if (p == null) {
                                return error(c, Component.translatable(MOD + ".caja.cmd.no_pieza"));
                            }
                            return si(c, Piano.pisar(c.getSource().getServer(), p.id, IntegerArgumentType.getInteger(c, "tecla") - 1));
                        }))))
                .then(Commands.literal("bien").then(Commands.argument("pieza", StringArgumentType.word()).suggests(vivas).executes(c -> {
                    // pisa la tecla correcta que le sigue (para las pruebas)
                    Pieza p = pieza(c);
                    if (p == null) {
                        return error(c, Component.translatable(MOD + ".caja.cmd.no_pieza"));
                    }
                    int k = Piano.siguiente(p.id);
                    return si(c, k >= 0 && Piano.pisar(c.getSource().getServer(), p.id, k));
                })))
                .then(Commands.literal("encadenar").then(Commands.argument("si", BoolArgumentType.bool()).executes(c -> {
                    Piano.encadenar = BoolArgumentType.getBool(c, "si");
                    return ok(c, Component.literal("encadenar=" + Piano.encadenar));
                })))
                .then(Commands.literal("rapido").then(Commands.argument("si", BoolArgumentType.bool()).executes(c -> {
                    Piano.rapido = BoolArgumentType.getBool(c, "si");
                    return ok(c, Component.literal("rapido=" + Piano.rapido));
                })));
    }
}
