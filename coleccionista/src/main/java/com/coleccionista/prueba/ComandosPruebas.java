package com.coleccionista.prueba;

import com.coleccionista.Coleccionista;
import com.coleccionista.evento.Evento;
import com.coleccionista.evento.Pieza;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;

import java.util.ArrayList;
import java.util.List;

/**
 * {@code /evento encadenados ...}: preparar (todos a la Sala de los grilletes), empezar (con el discurso o "ya"), soltar, cupo, tiempo,
 * gracia del gas, cerrar y terminar. {@code ir <marca> <pieza>} lleva a un equipo a un lugar del mapa (para probar sin jugar).
 */
public final class ComandosPruebas {
    private static final String MOD = Coleccionista.MODID;

    private ComandosPruebas() {
    }

    private static int ok(CommandContext<CommandSourceStack> c, Component m) {
        c.getSource().sendSuccess(() -> m, true);
        return 1;
    }

    private static int error(CommandContext<CommandSourceStack> c, Component m) {
        c.getSource().sendFailure(m);
        return 0;
    }

    private static Pieza pieza(CommandContext<CommandSourceStack> c) {
        String q = StringArgumentType.getString(c, "pieza");
        Pieza p = Evento.get(c.getSource().getServer()).buscar(q);
        if (p == null) {
            c.getSource().sendFailure(Component.translatable(MOD + ".cmd.no_existe", q));
        }
        return p;
    }

    public static LiteralArgumentBuilder<CommandSourceStack> encadenados(SuggestionProvider<CommandSourceStack> vivas) {
        SuggestionProvider<CommandSourceStack> marcas = (c, b) -> {
            List<String> r = new ArrayList<>(List.of("enc.placa.1.a", "enc.placa.1.b", "enc.placa.2.a", "enc.placa.2.b", "enc.salida",
                    "enc.centro", "enc.tubo.1", "enc.tubo.2", "enc.tubo.3"));
            return SharedSuggestionProvider.suggest(r, b);
        };
        return Commands.literal("encadenados")
                .then(Commands.literal("preparar").executes(c -> {
                    Component e = Encadenados.preparar(c.getSource().getServer());
                    return e == null ? ok(c, Component.translatable(MOD + ".enc.cmd.preparada", Encadenados.cupo())) : error(c, e);
                }))
                .then(Commands.literal("empezar").executes(c -> empezar(c, true))
                        .then(Commands.literal("ya").executes(c -> empezar(c, false))))
                .then(Commands.literal("soltar")
                        .then(Commands.literal("todos").executes(c -> ok(c, Component.translatable(MOD + ".enc.cmd.soltados",
                                Encadenados.soltarTodos(c.getSource().getServer())))))
                        .then(Commands.argument("pieza", StringArgumentType.word()).suggests(vivas).executes(c -> {
                            Pieza p = pieza(c);
                            if (p == null) {
                                return 0;
                            }
                            return Encadenados.soltar(c.getSource().getServer(), p.id) ? ok(c, Component.translatable(MOD + ".enc.cmd.soltados", 1))
                                    : error(c, Component.translatable(MOD + ".enc.ya_libre"));
                        })))
                .then(Commands.literal("ir").then(Commands.argument("marca", StringArgumentType.word()).suggests(marcas)
                        .then(Commands.argument("pieza", StringArgumentType.word()).suggests(vivas).executes(c -> {
                            Pieza p = pieza(c);
                            if (p == null) {
                                return 0;
                            }
                            String m = StringArgumentType.getString(c, "marca");
                            return Encadenados.ir(c.getSource().getServer(), p, m) ? ok(c, Component.translatable(MOD + ".enc.cmd.ir", p.nombre, m))
                                    : error(c, Component.translatable(MOD + ".enc.cmd.sin_marca", m));
                        }))))
                .then(Commands.literal("cupo").then(Commands.argument("n", IntegerArgumentType.integer(0, 24)).executes(c -> {
                    int n = IntegerArgumentType.getInteger(c, "n");
                    Encadenados.cupoFijo = n <= 0 ? -1 : n;
                    return ok(c, Component.translatable(MOD + ".enc.cmd.cupo", n <= 0 ? "auto" : String.valueOf(n)));
                })))
                .then(Commands.literal("tiempo").then(Commands.argument("segundos", IntegerArgumentType.integer(20, 3600)).executes(c -> {
                    Encadenados.segundos = IntegerArgumentType.getInteger(c, "segundos");
                    return ok(c, Component.translatable(MOD + ".enc.cmd.tiempo", Encadenados.segundos));
                })))
                .then(Commands.literal("gracia").then(Commands.argument("segundos", IntegerArgumentType.integer(0, 600)).executes(c -> {
                    Encadenados.gracia = IntegerArgumentType.getInteger(c, "segundos");
                    return ok(c, Component.translatable(MOD + ".enc.cmd.gracia", Encadenados.gracia));
                })))
                .then(Commands.literal("cerrar").executes(c -> {
                    MinecraftServer s = c.getSource().getServer();
                    if (Encadenados.fase() != Encadenados.Fase.JUEGO) {
                        return error(c, Component.translatable(MOD + ".enc.cmd.no_juego"));
                    }
                    Encadenados.cerrar(s, false);
                    return ok(c, Component.translatable(MOD + ".enc.cmd.cerrada"));
                }))
                .then(Commands.literal("terminar").executes(c -> {
                    Encadenados.terminar(c.getSource().getServer());
                    return ok(c, Component.translatable(MOD + ".enc.cmd.terminada"));
                }));
    }

    public static LiteralArgumentBuilder<CommandSourceStack> compartir(SuggestionProvider<CommandSourceStack> vivas) {
        return Commands.literal("compartir")
                .then(Commands.literal("preparar").executes(c -> {
                    Component e = Compartir.preparar(c.getSource().getServer());
                    return e == null ? ok(c, Component.translatable(MOD + ".com.cmd.preparada")) : error(c, e);
                }))
                .then(Commands.literal("empezar").executes(c -> Compartir.empezar(c.getSource().getServer(), true) ? ok(c,
                        Component.translatable(MOD + ".com.cmd.empieza")) : error(c, Component.translatable(MOD + ".com.cmd.no_lista")))
                        .then(Commands.literal("ya").executes(c -> Compartir.empezar(c.getSource().getServer(), false) ? ok(c,
                                Component.translatable(MOD + ".com.cmd.empieza")) : error(c, Component.translatable(MOD + ".com.cmd.no_lista")))))
                .then(Commands.literal("elegir").executes(c -> Compartir.elegirYa(c.getSource().getServer()) ? ok(c,
                        Component.translatable(MOD + ".com.cmd.elegir")) : error(c, Component.translatable(MOD + ".com.cmd.no_toca"))))
                .then(Commands.literal("revelar").executes(c -> Compartir.revelarYa(c.getSource().getServer()) ? ok(c,
                        Component.translatable(MOD + ".com.cmd.revelar")) : error(c, Component.translatable(MOD + ".com.cmd.no_toca"))))
                .then(Commands.literal("elige").then(Commands.argument("pieza", StringArgumentType.word()).suggests(vivas)
                        .then(Commands.literal("compartir").executes(c -> forzar(c, false)))
                        .then(Commands.literal("robar").executes(c -> forzar(c, true)))))
                .then(Commands.literal("hablar").then(Commands.argument("segundos", IntegerArgumentType.integer(5, 600)).executes(c -> {
                    Compartir.segundosHablar = IntegerArgumentType.getInteger(c, "segundos");
                    return ok(c, Component.translatable(MOD + ".com.cmd.tiempos", Compartir.segundosHablar, Compartir.segundosElegir));
                })))
                .then(Commands.literal("tiempo_elegir").then(Commands.argument("segundos", IntegerArgumentType.integer(5, 300)).executes(c -> {
                    Compartir.segundosElegir = IntegerArgumentType.getInteger(c, "segundos");
                    return ok(c, Component.translatable(MOD + ".com.cmd.tiempos", Compartir.segundosHablar, Compartir.segundosElegir));
                })))
                .then(Commands.literal("terminar").executes(c -> {
                    Compartir.terminar(c.getSource().getServer());
                    return ok(c, Component.translatable(MOD + ".com.cmd.terminada"));
                }));
    }

    public static LiteralArgumentBuilder<CommandSourceStack> puertas() {
        return Commands.literal("puertas")
                .then(Commands.literal("preparar").executes(c -> {
                    Component e = LasPuertas.preparar(c.getSource().getServer());
                    return e == null ? ok(c, Component.translatable(MOD + ".pue.cmd.preparada", LasPuertas.rondas)) : error(c, e);
                }))
                .then(Commands.literal("ronda").executes(c -> LasPuertas.ronda(c.getSource().getServer()) ? ok(c,
                        Component.translatable(MOD + ".pue.cmd.ronda")) : error(c, Component.translatable(MOD + ".pue.cmd.no_toca"))))
                .then(Commands.literal("cerrar").executes(c -> LasPuertas.cerrar(c.getSource().getServer()) ? ok(c,
                        Component.translatable(MOD + ".pue.cmd.cerrada")) : error(c, Component.translatable(MOD + ".pue.cmd.no_toca"))))
                .then(Commands.literal("rondas").then(Commands.argument("n", IntegerArgumentType.integer(1, 10)).executes(c -> {
                    LasPuertas.rondas = IntegerArgumentType.getInteger(c, "n");
                    return ok(c, Component.translatable(MOD + ".pue.cmd.ajustes", LasPuertas.rondas, LasPuertas.segundosElegir));
                })))
                .then(Commands.literal("tiempo").then(Commands.argument("segundos", IntegerArgumentType.integer(5, 300)).executes(c -> {
                    LasPuertas.segundosElegir = IntegerArgumentType.getInteger(c, "segundos");
                    return ok(c, Component.translatable(MOD + ".pue.cmd.ajustes", LasPuertas.rondas, LasPuertas.segundosElegir));
                })))
                .then(Commands.literal("terminar").executes(c -> {
                    LasPuertas.terminar(c.getSource().getServer());
                    return ok(c, Component.translatable(MOD + ".pue.cmd.terminada"));
                }));
    }

    public static LiteralArgumentBuilder<CommandSourceStack> copas() {
        return Commands.literal("copas")
                .then(Commands.literal("preparar").executes(c -> {
                    Component e = LasCopas.preparar(c.getSource().getServer());
                    return e == null ? ok(c, Component.translatable(MOD + ".cop.cmd.preparada")) : error(c, e);
                }))
                .then(Commands.literal("empezar").executes(c -> LasCopas.empezar(c.getSource().getServer(), true) ? ok(c,
                        Component.translatable(MOD + ".cop.cmd.empieza")) : error(c, Component.translatable(MOD + ".cop.cmd.no_toca")))
                        .then(Commands.literal("ya").executes(c -> LasCopas.empezar(c.getSource().getServer(), false) ? ok(c,
                                Component.translatable(MOD + ".cop.cmd.empieza")) : error(c, Component.translatable(MOD + ".cop.cmd.no_toca")))))
                .then(Commands.literal("beber").executes(c -> LasCopas.beberYa(c.getSource().getServer()) ? ok(c,
                        Component.translatable(MOD + ".cop.cmd.bebe")) : error(c, Component.translatable(MOD + ".cop.cmd.no_toca"))))
                .then(Commands.literal("tiempo").then(Commands.argument("segundos", IntegerArgumentType.integer(5, 300)).executes(c -> {
                    LasCopas.segundosTurno = IntegerArgumentType.getInteger(c, "segundos");
                    return ok(c, Component.translatable(MOD + ".cop.cmd.tiempo", LasCopas.segundosTurno));
                })))
                .then(Commands.literal("terminar").executes(c -> {
                    LasCopas.terminar(c.getSource().getServer());
                    return ok(c, Component.translatable(MOD + ".cop.cmd.terminada"));
                }));
    }

    public static LiteralArgumentBuilder<CommandSourceStack> sacrificio() {
        return Commands.literal("sacrificio")
                .then(Commands.literal("preparar").executes(c -> {
                    Component e = Sacrificio.preparar(c.getSource().getServer());
                    return e == null ? ok(c, Component.translatable(MOD + ".sac.cmd.preparada")) : error(c, e);
                }))
                .then(Commands.literal("empezar").executes(c -> Sacrificio.empezar(c.getSource().getServer(), true) ? ok(c,
                        Component.translatable(MOD + ".sac.cmd.empieza")) : error(c, Component.translatable(MOD + ".sac.cmd.no_toca")))
                        .then(Commands.literal("ya").executes(c -> Sacrificio.empezar(c.getSource().getServer(), false) ? ok(c,
                                Component.translatable(MOD + ".sac.cmd.empieza")) : error(c, Component.translatable(MOD + ".sac.cmd.no_toca")))))
                .then(Commands.literal("votar").executes(c -> Sacrificio.votarYa(c.getSource().getServer()) ? ok(c,
                        Component.translatable(MOD + ".sac.cmd.votar")) : error(c, Component.translatable(MOD + ".sac.cmd.no_toca"))))
                .then(Commands.literal("bots_ofrecen")
                        .then(Commands.literal("si").executes(c -> {
                            Sacrificio.botsSeOfrecen = true;
                            return ok(c, Component.literal("bots_ofrecen = si"));
                        }))
                        .then(Commands.literal("no").executes(c -> {
                            Sacrificio.botsSeOfrecen = false;
                            return ok(c, Component.literal("bots_ofrecen = no"));
                        })))
                .then(Commands.literal("tamano").then(Commands.argument("n", IntegerArgumentType.integer(2, 6)).executes(c -> {
                    Sacrificio.tamano = IntegerArgumentType.getInteger(c, "n");
                    return ok(c, Component.translatable(MOD + ".sac.cmd.ajustes", Sacrificio.tamano, Sacrificio.segundosOfrecer, Sacrificio.segundosVotar));
                })))
                .then(Commands.literal("tiempo").then(Commands.argument("ofrecer", IntegerArgumentType.integer(5, 600))
                        .then(Commands.argument("votar", IntegerArgumentType.integer(5, 300)).executes(c -> {
                            Sacrificio.segundosOfrecer = IntegerArgumentType.getInteger(c, "ofrecer");
                            Sacrificio.segundosVotar = IntegerArgumentType.getInteger(c, "votar");
                            return ok(c, Component.translatable(MOD + ".sac.cmd.ajustes", Sacrificio.tamano, Sacrificio.segundosOfrecer,
                                    Sacrificio.segundosVotar));
                        }))))
                .then(Commands.literal("terminar").executes(c -> {
                    Sacrificio.terminar(c.getSource().getServer());
                    return ok(c, Component.translatable(MOD + ".sac.cmd.terminada"));
                }));
    }

    private static int forzar(CommandContext<CommandSourceStack> c, boolean robar) {
        Pieza p = pieza(c);
        if (p == null) {
            return 0;
        }
        return Compartir.forzar(c.getSource().getServer(), p, robar) ? ok(c, Component.translatable(MOD + ".com.cmd.forzada", p.nombre))
                : error(c, Component.translatable(MOD + ".com.cmd.no_toca"));
    }

    private static int empezar(CommandContext<CommandSourceStack> c, boolean discurso) {
        return Encadenados.empezar(c.getSource().getServer(), discurso) ? ok(c, Component.translatable(MOD + ".enc.cmd.empieza"))
                : error(c, Component.translatable(MOD + ".enc.cmd.no_lista"));
    }
}
