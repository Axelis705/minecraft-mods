package com.coleccionista.prueba;

import com.coleccionista.Coleccionista;
import com.coleccionista.evento.Evento;
import com.coleccionista.evento.Pieza;
import com.mojang.brigadier.CommandDispatcher;
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

/**
 * Los comandos de la caja de música: {@code /evento caja [llamar|llegar|saltar|roles|bien|bala [pieza]|terminar|limpiar|auto <si>|
 * encadenar <si>|tiempos <turno> <elegir>]} y el atajo del organizador {@code /bala [pieza]} (dispara a la pieza que apunta el arma).
 */
public final class ComandosCaja {
    private static final String MOD = Coleccionista.MODID;

    private ComandosCaja() {
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
        return no == null ? ok(c, Component.literal(Caja.resumen())) : error(c, no);
    }

    private static int si(CommandContext<CommandSourceStack> c, boolean hecho) {
        return hecho ? ok(c, Component.literal(Caja.resumen())) : error(c, Component.translatable(MOD + ".caja.cmd.no"));
    }

    private static Component bala(CommandContext<CommandSourceStack> c, boolean conPieza) {
        MinecraftServer s = c.getSource().getServer();
        Pieza p = null;
        if (conPieza) {
            p = Evento.get(s).buscar(StringArgumentType.getString(c, "pieza"));
            if (p == null) {
                return Component.translatable(MOD + ".caja.cmd.no_pieza");
            }
        }
        return Caja.bala(s, p);
    }

    public static LiteralArgumentBuilder<CommandSourceStack> caja(SuggestionProvider<CommandSourceStack> vivas) {
        return Commands.literal("caja")
                .executes(c -> ok(c, Component.literal(Caja.resumen())))
                .then(Commands.literal("llamar").executes(c -> resultado(c, Caja.llamar(c.getSource().getServer()))))
                .then(Commands.literal("llegar").executes(c -> resultado(c, Caja.llegar(c.getSource().getServer()))))
                .then(Commands.literal("saltar").executes(c -> si(c, Caja.saltar(c.getSource().getServer()))))
                .then(Commands.literal("bien").executes(c -> si(c, Caja.decidir(c.getSource().getServer(), false))))
                .then(Commands.literal("bala").executes(c -> resultado(c, bala(c, false)))
                        .then(Commands.argument("pieza", StringArgumentType.word()).suggests(vivas).executes(c -> resultado(c, bala(c, true)))))
                .then(Commands.literal("terminar").executes(c -> si(c, Caja.terminar(c.getSource().getServer()))))
                .then(Commands.literal("limpiar").executes(c -> {
                    Caja.reiniciar(c.getSource().getServer());
                    return ok(c, Component.literal(Caja.resumen()));
                }))
                .then(Commands.literal("auto").then(Commands.argument("si", BoolArgumentType.bool()).executes(c -> {
                    Caja.auto = BoolArgumentType.getBool(c, "si");
                    return ok(c, Component.literal("auto=" + Caja.auto));
                })))
                .then(Commands.literal("esperar").then(Commands.argument("si", BoolArgumentType.bool()).executes(c -> {
                    Caja.esperar = BoolArgumentType.getBool(c, "si");
                    return ok(c, Component.literal("esperar=" + Caja.esperar));
                })))
                .then(Commands.literal("primero").then(Commands.argument("pieza", StringArgumentType.word()).suggests(vivas).executes(c -> {
                    Pieza p = Evento.get(c.getSource().getServer()).buscar(StringArgumentType.getString(c, "pieza"));
                    if (p == null) {
                        return error(c, Component.translatable(MOD + ".caja.cmd.no_pieza"));
                    }
                    Caja.primero.add(p.id);
                    return ok(c, Component.literal("primero: " + p.nombre));
                })))
                .then(Commands.literal("roles").executes(c -> {
                    MinecraftServer s = c.getSource().getServer();
                    if (Caja.fase() == Caja.Fase.PASILLO) {
                        Caja.llegar(s);
                    }
                    Caja.roles(s);
                    return si(c, Caja.fase() == Caja.Fase.ROLES || Caja.fase() == Caja.Fase.APAGON);
                }))
                .then(Commands.literal("encadenar").then(Commands.argument("si", BoolArgumentType.bool()).executes(c -> {
                    Caja.encadenar = BoolArgumentType.getBool(c, "si");
                    return ok(c, Component.literal("encadenar=" + Caja.encadenar));
                })))
                .then(Commands.literal("sentarse").then(Commands.argument("segundos", IntegerArgumentType.integer(10, 600)).executes(c -> {
                    Caja.segundosSentarse = IntegerArgumentType.getInteger(c, "segundos");
                    return ok(c, Component.literal("sentarse=" + Caja.segundosSentarse));
                })))
                .then(Commands.literal("tiempos").then(Commands.argument("turno", IntegerArgumentType.integer(5, 600))
                        .then(Commands.argument("elegir", IntegerArgumentType.integer(5, 300)).executes(c -> {
                            Caja.segundosTurno = IntegerArgumentType.getInteger(c, "turno");
                            Caja.segundosElegir = IntegerArgumentType.getInteger(c, "elegir");
                            return ok(c, Component.literal("turno=" + Caja.segundosTurno + " elegir=" + Caja.segundosElegir));
                        }))));
    }

    /** {@code /evento probar venda|cinta|esposas|nada [jugadores]}: los papeles de los sentidos, para probarlos. */
    public static LiteralArgumentBuilder<CommandSourceStack> probar() {
        LiteralArgumentBuilder<CommandSourceStack> b = Commands.literal("probar");
        for (String r : new String[] {"venda", "cinta", "esposas", "nada"}) {
            b.then(Commands.literal(r).executes(c -> {
                RolesPrueba.poner(c.getSource().getServer(), c.getSource().getPlayerOrException(), r.toUpperCase(java.util.Locale.ROOT));
                return ok(c, Component.literal(r));
            }).then(Commands.argument("jugadores", net.minecraft.commands.arguments.EntityArgument.players()).executes(c -> {
                for (net.minecraft.server.level.ServerPlayer p : net.minecraft.commands.arguments.EntityArgument.getPlayers(c, "jugadores")) {
                    RolesPrueba.poner(c.getSource().getServer(), p, r.toUpperCase(java.util.Locale.ROOT));
                }
                return ok(c, Component.literal(r));
            })));
        }
        return b;
    }

    /** {@code /bala [pieza]}: para el organizador, sin escribir /evento. */
    public static void registrarBala(CommandDispatcher<CommandSourceStack> d, SuggestionProvider<CommandSourceStack> vivas) {
        d.register(Commands.literal("bala").requires(s -> s.hasPermission(2))
                .executes(c -> resultado(c, bala(c, false)))
                .then(Commands.argument("pieza", StringArgumentType.word()).suggests(vivas).executes(c -> resultado(c, bala(c, true)))));
    }
}
