package com.coleccionista.prueba;

import com.coleccionista.Coleccionista;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Los comandos de la final: {@code /evento veintiuno [llamar|llegar|saltar|terminar|limpiar|accion <lado 0-1> <pedir|plantarse|espiar|
 * regresar>|mazo <cartas...>|mazo azar|golpes <1-5>|turno <segundos>|comodines <si>|encadenar <si>|rapido <si>]}.
 */
public final class ComandosVeintiuno {
    private static final String MOD = Coleccionista.MODID;

    private ComandosVeintiuno() {
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
        return no == null ? ok(c, Component.literal(Veintiuno.resumen())) : error(c, no);
    }

    private static int si(CommandContext<CommandSourceStack> c, boolean hecho) {
        return hecho ? ok(c, Component.literal(Veintiuno.resumen())) : error(c, Component.translatable(MOD + ".v21.cmd.no"));
    }

    public static LiteralArgumentBuilder<CommandSourceStack> veintiuno() {
        return Commands.literal("veintiuno")
                .executes(c -> ok(c, Component.literal(Veintiuno.resumen())))
                .then(Commands.literal("llamar").executes(c -> resultado(c, Veintiuno.llamar(c.getSource().getServer()))))
                .then(Commands.literal("llegar").executes(c -> resultado(c, Veintiuno.llegar(c.getSource().getServer()))))
                .then(Commands.literal("saltar").executes(c -> si(c, Veintiuno.saltar(c.getSource().getServer()))))
                .then(Commands.literal("terminar").executes(c -> si(c, Veintiuno.terminar(c.getSource().getServer()))))
                .then(Commands.literal("limpiar").executes(c -> {
                    Veintiuno.reiniciar(c.getSource().getServer());
                    return ok(c, Component.literal(Veintiuno.resumen()));
                }))
                .then(Commands.literal("accion").then(Commands.argument("lado", IntegerArgumentType.integer(0, 1))
                        .then(Commands.argument("que", StringArgumentType.word())
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(List.of("pedir", "plantarse", "espiar", "regresar"), b))
                                .executes(c -> si(c, Veintiuno.accion(c.getSource().getServer(), IntegerArgumentType.getInteger(c, "lado"),
                                        StringArgumentType.getString(c, "que")))))))
                .then(Commands.literal("mazo").then(Commands.argument("cartas", StringArgumentType.greedyString()).executes(c -> {
                    String a = StringArgumentType.getString(c, "cartas").trim();
                    if (a.equals("azar")) {
                        Veintiuno.mazoFijo = null;
                        return ok(c, Component.literal("mazo=azar"));
                    }
                    List<Integer> l = new ArrayList<>();
                    for (String x : a.split("\\s+")) {
                        try {
                            int n = Integer.parseInt(x);
                            if (n < 1 || n > Veintiuno.CARTAS || l.contains(n)) {
                                return error(c, Component.literal("mazo: cartas del 1 al 11, sin repetir"));
                            }
                            l.add(n);
                        } catch (NumberFormatException e) {
                            return error(c, Component.literal("mazo <cartas...> | mazo azar"));
                        }
                    }
                    Veintiuno.mazoFijo = l;
                    return ok(c, Component.literal("mazo=" + l));
                })))
                .then(Commands.literal("golpes").then(Commands.argument("n", IntegerArgumentType.integer(1, 5)).executes(c -> {
                    Veintiuno.golpesMax = IntegerArgumentType.getInteger(c, "n");
                    return ok(c, Component.literal("golpes=" + Veintiuno.golpesMax));
                })))
                .then(Commands.literal("turno").then(Commands.argument("segundos", IntegerArgumentType.integer(5, 120)).executes(c -> {
                    Veintiuno.segundosTurno = IntegerArgumentType.getInteger(c, "segundos");
                    return ok(c, Component.literal("turno=" + Veintiuno.segundosTurno + "s"));
                })))
                .then(Commands.literal("comodines").then(Commands.argument("si", BoolArgumentType.bool()).executes(c -> {
                    Veintiuno.comodines = BoolArgumentType.getBool(c, "si");
                    return ok(c, Component.literal("comodines=" + Veintiuno.comodines));
                })))
                .then(Commands.literal("encadenar").then(Commands.argument("si", BoolArgumentType.bool()).executes(c -> {
                    Veintiuno.encadenar = BoolArgumentType.getBool(c, "si");
                    return ok(c, Component.literal("encadenar=" + Veintiuno.encadenar));
                })))
                .then(Commands.literal("rapido").then(Commands.argument("si", BoolArgumentType.bool()).executes(c -> {
                    Veintiuno.rapido = BoolArgumentType.getBool(c, "si");
                    return ok(c, Component.literal("rapido=" + Veintiuno.rapido));
                })));
    }
}
