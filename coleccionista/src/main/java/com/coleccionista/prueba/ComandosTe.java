package com.coleccionista.prueba;

import com.coleccionista.Coleccionista;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

/**
 * Los comandos del salón de té: {@code /evento te [llamar|llegar|saltar|terminar|limpiar|jugada <lado 0-1> <1 piedra, 2 papel, 3 tijera>|
 * caliz <1-5>|veneno <0 al azar, 1-5>|encadenar <si>|rapido <si>|tiempos <parejas> <jugada> <caliz>]}.
 */
public final class ComandosTe {
    private static final String MOD = Coleccionista.MODID;

    private ComandosTe() {
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
        return no == null ? ok(c, Component.literal(Te.resumen())) : error(c, no);
    }

    private static int si(CommandContext<CommandSourceStack> c, boolean hecho) {
        return hecho ? ok(c, Component.literal(Te.resumen())) : error(c, Component.translatable(MOD + ".te.cmd.no"));
    }

    public static LiteralArgumentBuilder<CommandSourceStack> te() {
        return Commands.literal("te")
                .executes(c -> ok(c, Component.literal(Te.resumen())))
                .then(Commands.literal("llamar").executes(c -> resultado(c, Te.llamar(c.getSource().getServer()))))
                .then(Commands.literal("llegar").executes(c -> resultado(c, Te.llegar(c.getSource().getServer()))))
                .then(Commands.literal("saltar").executes(c -> si(c, Te.saltar(c.getSource().getServer()))))
                .then(Commands.literal("terminar").executes(c -> si(c, Te.terminar(c.getSource().getServer()))))
                .then(Commands.literal("limpiar").executes(c -> {
                    Te.reiniciar(c.getSource().getServer());
                    return ok(c, Component.literal(Te.resumen()));
                }))
                .then(Commands.literal("jugada").then(Commands.argument("lado", IntegerArgumentType.integer(0, 1))
                        .then(Commands.argument("g", IntegerArgumentType.integer(1, 3)).executes(c -> si(c, Te.jugar(c.getSource().getServer(),
                                IntegerArgumentType.getInteger(c, "lado"), IntegerArgumentType.getInteger(c, "g")))))))
                .then(Commands.literal("caliz").then(Commands.argument("k", IntegerArgumentType.integer(1, Te.CALICES))
                        .executes(c -> si(c, Te.beber(c.getSource().getServer(), IntegerArgumentType.getInteger(c, "k") - 1)))))
                .then(Commands.literal("veneno").then(Commands.argument("k", IntegerArgumentType.integer(0, Te.CALICES)).executes(c -> {
                    Te.venenoFijo = IntegerArgumentType.getInteger(c, "k") - 1;
                    return ok(c, Component.literal("veneno=" + (Te.venenoFijo < 0 ? "azar" : String.valueOf(Te.venenoFijo + 1))));
                })))
                .then(Commands.literal("encadenar").then(Commands.argument("si", BoolArgumentType.bool()).executes(c -> {
                    Te.encadenar = BoolArgumentType.getBool(c, "si");
                    return ok(c, Component.literal("encadenar=" + Te.encadenar));
                })))
                .then(Commands.literal("rapido").then(Commands.argument("si", BoolArgumentType.bool()).executes(c -> {
                    Te.rapido = BoolArgumentType.getBool(c, "si");
                    return ok(c, Component.literal("rapido=" + Te.rapido));
                })))
                .then(Commands.literal("tiempos").then(Commands.argument("parejas", IntegerArgumentType.integer(5, 600))
                        .then(Commands.argument("jugada", IntegerArgumentType.integer(3, 120))
                                .then(Commands.argument("caliz", IntegerArgumentType.integer(3, 120)).executes(c -> {
                                    Te.segundosParejas = IntegerArgumentType.getInteger(c, "parejas");
                                    Te.segundosJugada = IntegerArgumentType.getInteger(c, "jugada");
                                    Te.segundosCaliz = IntegerArgumentType.getInteger(c, "caliz");
                                    return ok(c, Component.literal("parejas=" + Te.segundosParejas + " jugada=" + Te.segundosJugada + " caliz="
                                            + Te.segundosCaliz));
                                })))));
    }
}
