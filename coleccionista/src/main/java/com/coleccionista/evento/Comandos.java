package com.coleccionista.evento;

import com.coleccionista.Coleccionista;
import com.coleccionista.bot.PiezaBot;
import com.coleccionista.registry.ModRegistry;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

/**
 * {@code /evento ...}: todo lo que puede hacer el organizador (solo operadores). El panel del organizador usa estas mismas funciones.
 */
public final class Comandos {
    private static final String MOD = Coleccionista.MODID;

    private Comandos() {
    }

    private static SuggestionProvider<CommandSourceStack> piezas(Predicate<Pieza> filtro) {
        return (c, b) -> {
            List<String> r = new ArrayList<>();
            for (Pieza p : Evento.get(c.getSource().getServer()).piezas()) {
                if (filtro.test(p)) {
                    r.add(p.nombre);
                }
            }
            return SharedSuggestionProvider.suggest(r, b);
        };
    }

    public static void registrar(CommandDispatcher<CommandSourceStack> d) {
        SuggestionProvider<CommandSourceStack> todas = piezas(p -> true);
        SuggestionProvider<CommandSourceStack> vivas = piezas(Pieza::vivo);
        SuggestionProvider<CommandSourceStack> fantasmas = piezas(p -> !p.vivo());
        d.register(Commands.literal("evento").requires(s -> s.hasPermission(2))
                .executes(Comandos::estado)
                .then(Commands.literal("estado").executes(Comandos::estado))
                .then(Commands.literal("inscribir")
                        .then(Commands.literal("todos").executes(c -> inscribir(c, c.getSource().getServer().getPlayerList().getPlayers())))
                        .then(Commands.argument("jugadores", EntityArgument.players())
                                .executes(c -> inscribir(c, EntityArgument.getPlayers(c, "jugadores")))))
                .then(Commands.literal("quitar").then(Commands.argument("pieza", StringArgumentType.word()).suggests(todas)
                        .executes(Comandos::quitar)))
                .then(Commands.literal("organizador").then(Commands.argument("jugadores", EntityArgument.players())
                        .executes(c -> organizador(c, true))
                        .then(Commands.literal("no").executes(c -> organizador(c, false)))))
                .then(Commands.literal("eliminar")
                        .then(Commands.literal("equipo").then(Commands.argument("n", IntegerArgumentType.integer(1))
                                .executes(Comandos::eliminarEquipo)))
                        .then(Commands.argument("pieza", StringArgumentType.word()).suggests(vivas)
                                .executes(c -> eliminar(c, ""))
                                .then(Commands.argument("motivo", StringArgumentType.greedyString())
                                        .executes(c -> eliminar(c, StringArgumentType.getString(c, "motivo"))))))
                .then(Commands.literal("revivir").then(Commands.argument("pieza", StringArgumentType.word()).suggests(fantasmas)
                        .executes(Comandos::revivir)))
                .then(Commands.literal("equipos")
                        .then(Commands.literal("quitar").executes(c -> {
                            Evento.get(c.getSource().getServer()).sinEquipos(c.getSource().getServer());
                            return ok(c, Component.translatable(MOD + ".cmd.sin_equipos"));
                        }))
                        .then(Commands.argument("tam", IntegerArgumentType.integer(2, 10)).executes(c -> equipos(c,
                                IntegerArgumentType.getInteger(c, "tam")))))
                .then(Commands.literal("parejas").executes(c -> equipos(c, 2)))
                .then(Commands.literal("reloj")
                        .then(Commands.literal("parar").executes(c -> reloj(c, 0)))
                        .then(Commands.argument("segundos", IntegerArgumentType.integer(0, 36000))
                                .executes(c -> reloj(c, IntegerArgumentType.getInteger(c, "segundos")))))
                .then(Commands.literal("prueba").then(Commands.argument("nombre", StringArgumentType.greedyString()).executes(c -> {
                    Evento.get(c.getSource().getServer()).prueba(c.getSource().getServer(), StringArgumentType.getString(c, "nombre"));
                    return ok(c, Component.translatable(MOD + ".cmd.prueba", StringArgumentType.getString(c, "nombre")));
                })))
                .then(Commands.literal("pausar").executes(c -> {
                    boolean b = Evento.get(c.getSource().getServer()).pausar(c.getSource().getServer());
                    return b ? ok(c, Component.translatable(MOD + ".cmd.pausado")) : error(c, Component.translatable(MOD + ".cmd.ya_pausado"));
                }))
                .then(Commands.literal("reanudar").executes(c -> {
                    boolean b = Evento.get(c.getSource().getServer()).reanudar(c.getSource().getServer());
                    return b ? ok(c, Component.translatable(MOD + ".cmd.reanudado")) : error(c, Component.translatable(MOD + ".cmd.no_pausado"));
                }))
                .then(Commands.literal("anunciar").then(Commands.argument("texto", StringArgumentType.greedyString()).executes(c -> {
                    Anuncio.decir(c.getSource().getServer(), StringArgumentType.getString(c, "texto"));
                    return 1;
                })))
                .then(Commands.literal("reiniciar")
                        .executes(c -> error(c, Component.translatable(MOD + ".cmd.reiniciar_confirmar")))
                        .then(Commands.literal("confirmar").executes(c -> {
                            Evento.get(c.getSource().getServer()).reiniciar(c.getSource().getServer());
                            return ok(c, Component.translatable(MOD + ".cmd.reiniciado"));
                        })))
                .then(Commands.literal("bots").then(Commands.argument("cantidad", IntegerArgumentType.integer(1, 30))
                        .executes(c -> bots(c, IntegerArgumentType.getInteger(c, "cantidad")))))
                // ---- el disfraz del Coleccionista
                .then(Commands.literal("disfraz")
                        .then(Commands.literal("si").executes(c -> disfraz(c, null, true))
                                .then(Commands.argument("jugadores", EntityArgument.players()).executes(c -> disfraz(c,
                                        EntityArgument.getPlayers(c, "jugadores"), true))))
                        .then(Commands.literal("no").executes(c -> disfraz(c, null, false))
                                .then(Commands.argument("jugadores", EntityArgument.players()).executes(c -> disfraz(c,
                                        EntityArgument.getPlayers(c, "jugadores"), false)))))
                // ---- el mapa y el despertar
                .then(Commands.literal("construir").executes(Comandos::construir))
                .then(Commands.literal("sala").executes(c -> {
                    if (!Complejo.construido(c.getSource().getServer())) {
                        return error(c, Component.translatable(MOD + ".cmd.sin_mapa"));
                    }
                    Despertar.esperar(c.getSource().getServer());
                    return ok(c, Component.translatable(MOD + ".cmd.sala"));
                }))
                .then(Commands.literal("despertar").executes(c -> Despertar.despertar(c.getSource().getServer())
                        ? ok(c, Component.translatable(MOD + ".cmd.despertar")) : error(c, Component.translatable(MOD + ".cmd.no_sala"))))
                .then(Commands.literal("investigar").then(Commands.argument("segundos", IntegerArgumentType.integer(3, 3600)).executes(c -> {
                    Despertar.segundosInvestigar = IntegerArgumentType.getInteger(c, "segundos");
                    return ok(c, Component.translatable(MOD + ".cmd.investigar", Despertar.segundosInvestigar));
                })))
                .then(Commands.literal("discurso").executes(c -> {
                    Despertar.discurso(c.getSource().getServer());
                    return ok(c, Component.translatable(MOD + ".cmd.discurso"));
                }))
                .then(Commands.literal("montacargas").then(Commands.argument("zona", StringArgumentType.word())
                        .suggests((c, b) -> net.minecraft.commands.SharedSuggestionProvider.suggest(com.coleccionista.mapa.Antesala.ZONAS, b))
                        .executes(c -> {
                            String z = StringArgumentType.getString(c, "zona");
                            Component no = Montacargas.viajar(c.getSource().getServer(), z);
                            return no == null ? ok(c, Component.translatable(MOD + ".cmd.montacargas", Montacargas.nombre(z))) : error(c, no);
                        })))
                .then(Commands.literal("recorrido")
                        .executes(c -> ok(c, Component.translatable(MOD + ".cmd.recorrido", Recorrido.estado())))
                        .then(Commands.literal("iniciar").executes(c -> Recorrido.iniciar(c.getSource().getServer())
                                ? ok(c, Component.translatable(MOD + ".cmd.recorrido_inicia")) : error(c, Component.translatable(MOD + ".cmd.recorrido_no"))))
                        .then(Commands.literal("pausa").executes(c -> Recorrido.pausar(c.getSource().getServer(), true)
                                ? ok(c, Component.translatable(MOD + ".cmd.recorrido", Recorrido.estado())) : error(c, Component.translatable(MOD + ".cmd.recorrido_no"))))
                        .then(Commands.literal("seguir").executes(c -> Recorrido.pausar(c.getSource().getServer(), false)
                                ? ok(c, Component.translatable(MOD + ".cmd.recorrido", Recorrido.estado())) : error(c, Component.translatable(MOD + ".cmd.recorrido_no"))))
                        .then(Commands.literal("saltar").executes(c -> Recorrido.saltar(c.getSource().getServer())
                                ? ok(c, Component.translatable(MOD + ".cmd.recorrido", Recorrido.estado())) : error(c, Component.translatable(MOD + ".cmd.recorrido_no"))))
                        .then(Commands.literal("parar").executes(c -> {
                            Recorrido.parar(c.getSource().getServer());
                            return ok(c, Component.translatable(MOD + ".cmd.recorrido", Recorrido.estado()));
                        }))
                        .then(Commands.literal("ir").then(Commands.argument("prueba", StringArgumentType.word())
                                .suggests((c, b) -> net.minecraft.commands.SharedSuggestionProvider.suggest(Recorrido.pruebas(), b))
                                .executes(c -> Recorrido.ir(c.getSource().getServer(), StringArgumentType.getString(c, "prueba"))
                                        ? ok(c, Component.translatable(MOD + ".cmd.recorrido", Recorrido.estado())) : error(c, Component.translatable(MOD + ".cmd.recorrido_no")))))
                        .then(Commands.literal("auto").then(Commands.argument("si", com.mojang.brigadier.arguments.BoolArgumentType.bool()).executes(c -> {
                            Recorrido.auto = com.mojang.brigadier.arguments.BoolArgumentType.getBool(c, "si");
                            return ok(c, Component.translatable(MOD + ".cmd.recorrido_ajustes", Recorrido.auto, Recorrido.extra, Recorrido.formarSeg, Recorrido.elevadorSeg));
                        })))
                        .then(Commands.literal("extra").then(Commands.argument("si", com.mojang.brigadier.arguments.BoolArgumentType.bool()).executes(c -> {
                            Recorrido.extra = com.mojang.brigadier.arguments.BoolArgumentType.getBool(c, "si");
                            return ok(c, Component.translatable(MOD + ".cmd.recorrido_ajustes", Recorrido.auto, Recorrido.extra, Recorrido.formarSeg, Recorrido.elevadorSeg));
                        })))
                        .then(Commands.literal("tiempos").then(Commands.argument("formar", com.mojang.brigadier.arguments.IntegerArgumentType.integer(10, 600))
                                .then(Commands.argument("elevador", com.mojang.brigadier.arguments.IntegerArgumentType.integer(10, 600)).executes(c -> {
                                    Recorrido.formarSeg = com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(c, "formar");
                                    Recorrido.elevadorSeg = com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(c, "elevador");
                                    return ok(c, Component.translatable(MOD + ".cmd.recorrido_ajustes", Recorrido.auto, Recorrido.extra, Recorrido.formarSeg, Recorrido.elevadorSeg));
                                })))))
                // ---- capítulo 1: el portón, la sala grande, los ascensores y los circuitos de los sentidos
                .then(Commands.literal("porton")
                        .then(Commands.literal("abrir").executes(c -> {
                            Celdas.porton(c.getSource().getServer(), true);
                            Capitulo1.alAbrirPorton(c.getSource().getServer());
                            return ok(c, Component.translatable(MOD + ".cmd.porton", "↑"));
                        }))
                        .then(Commands.literal("cerrar").executes(c -> {
                            Celdas.porton(c.getSource().getServer(), false);
                            return ok(c, Component.translatable(MOD + ".cmd.porton", "↓"));
                        })))
                .then(Commands.literal("inscripcion").then(Commands.argument("auto", com.mojang.brigadier.arguments.BoolArgumentType.bool()).executes(c -> {
                    Evento ev = Evento.get(c.getSource().getServer());
                    ev.autoInscribir = com.mojang.brigadier.arguments.BoolArgumentType.getBool(c, "auto");
                    ev.cambio(c.getSource().getServer());
                    return ok(c, Component.translatable(MOD + ".cmd.inscripcion", ev.autoInscribir));
                })))
                .then(Commands.literal("capitulo1")
                        .executes(c -> ok(c, Component.literal("cap1=" + Capitulo1.resumen() + " sentidos=" + com.coleccionista.prueba.Sentidos.resumen())))
                        .then(Commands.literal("sala").executes(c -> ok(c, Component.translatable(MOD + ".cmd.cap1_sala",
                                Capitulo1.aLaSala(c.getSource().getServer())))))
                        .then(Commands.literal("equipos").executes(c -> {
                            Component no = Capitulo1.equipos(c.getSource().getServer());
                            return no == null ? ok(c, Component.translatable(MOD + ".cmd.cap1_equipos")) : error(c, no);
                        }))
                        .then(Commands.literal("formar").executes(c -> {
                            Capitulo1.formar(c.getSource().getServer());
                            return ok(c, Component.literal("cap1=" + Capitulo1.resumen()));
                        }))
                        .then(Commands.literal("parar").executes(c -> {
                            Capitulo1.reiniciar(c.getSource().getServer());
                            Pantallas.callar(c.getSource().getServer());
                            return ok(c, Component.literal("cap1=" + Capitulo1.resumen()));
                        }))
                        .then(Commands.literal("bajar").executes(c -> Capitulo1.bajar(c.getSource().getServer(), true)
                                ? ok(c, Component.translatable(MOD + ".cmd.cap1_bajan")) : error(c, Component.translatable(MOD + ".cap1.no_bajan")))))
                .then(Commands.literal("pocos").then(Commands.argument("si", com.mojang.brigadier.arguments.BoolArgumentType.bool())
                        .executes(c -> {
                            Evento ev = Evento.get(c.getSource().getServer());
                            ev.pocos = com.mojang.brigadier.arguments.BoolArgumentType.getBool(c, "si");
                            ev.cambio(c.getSource().getServer());
                            return ok(c, Component.translatable(MOD + ".cmd.pocos", ev.pocos));
                        })))
                .then(Commands.literal("sentidos")
                        .executes(c -> ok(c, Component.literal(com.coleccionista.prueba.Sentidos.resumen())))
                        .then(Commands.literal("saltar").executes(c -> com.coleccionista.prueba.Sentidos.saltar(c.getSource().getServer())
                                ? ok(c, Component.literal(com.coleccionista.prueba.Sentidos.resumen())) : error(c, Component.translatable(MOD + ".sen.no"))))
                        .then(Commands.literal("terminar").executes(c -> com.coleccionista.prueba.Sentidos.terminarYa(c.getSource().getServer())
                                ? ok(c, Component.literal(com.coleccionista.prueba.Sentidos.resumen())) : error(c, Component.translatable(MOD + ".sen.no"))))
                        .then(Commands.literal("limpiar").executes(c -> {
                            com.coleccionista.prueba.Sentidos.reiniciar(c.getSource().getServer());
                            return ok(c, Component.literal(com.coleccionista.prueba.Sentidos.resumen()));
                        }))
                        .then(Commands.literal("bots").then(Commands.argument("salen", com.mojang.brigadier.arguments.BoolArgumentType.bool())
                                .executes(c -> {
                                    com.coleccionista.prueba.Sentidos.botsSalen = com.mojang.brigadier.arguments.BoolArgumentType.getBool(c, "salen");
                                    return ok(c, Component.literal("botsSalen=" + com.coleccionista.prueba.Sentidos.botsSalen));
                                })
                                .then(Commands.argument("min", IntegerArgumentType.integer(1, 600)).then(Commands.argument("max", IntegerArgumentType.integer(2, 900))
                                        .executes(c -> {
                                            com.coleccionista.prueba.Sentidos.botsSalen = com.mojang.brigadier.arguments.BoolArgumentType.getBool(c, "salen");
                                            com.coleccionista.prueba.Sentidos.botMin = IntegerArgumentType.getInteger(c, "min");
                                            com.coleccionista.prueba.Sentidos.botMax = Math.max(com.coleccionista.prueba.Sentidos.botMin + 1,
                                                    IntegerArgumentType.getInteger(c, "max"));
                                            return ok(c, Component.literal("bots " + com.coleccionista.prueba.Sentidos.botMin + ".."
                                                    + com.coleccionista.prueba.Sentidos.botMax));
                                        })))))
                        .then(Commands.literal("mostrar").executes(c -> {
                            com.coleccionista.prueba.Sentidos.mostrar(c.getSource().getServer());
                            return ok(c, Component.literal(com.coleccionista.prueba.Sentidos.resumen()));
                        }))
                        .then(Commands.literal("toma").then(Commands.argument("pieza", StringArgumentType.word())
                                .then(Commands.argument("i", IntegerArgumentType.integer(0, 2)).executes(c -> {
                                    Pieza p = Evento.get(c.getSource().getServer()).buscar(StringArgumentType.getString(c, "pieza"));
                                    return p != null && com.coleccionista.prueba.Sentidos.tomarComo(c.getSource().getServer(), p,
                                            IntegerArgumentType.getInteger(c, "i")) ? ok(c, Component.literal(com.coleccionista.prueba.Sentidos.resumen()))
                                            : error(c, Component.translatable(MOD + ".sen.no"));
                                }))))
                        .then(Commands.literal("salida").then(Commands.argument("pieza", StringArgumentType.word()).executes(c -> {
                            Pieza p = Evento.get(c.getSource().getServer()).buscar(StringArgumentType.getString(c, "pieza"));
                            return p != null && com.coleccionista.prueba.Sentidos.aLaSalida(c.getSource().getServer(), p)
                                    ? ok(c, Component.literal(com.coleccionista.prueba.Sentidos.resumen())) : error(c, Component.translatable(MOD + ".sen.no"));
                        })))
                        .then(Commands.literal("tiempos").then(Commands.argument("elegir", IntegerArgumentType.integer(5, 600))
                                .then(Commands.argument("juego", IntegerArgumentType.integer(30, 3600)).executes(c -> {
                                    com.coleccionista.prueba.Sentidos.segundosElegir = IntegerArgumentType.getInteger(c, "elegir");
                                    com.coleccionista.prueba.Sentidos.segundos = IntegerArgumentType.getInteger(c, "juego");
                                    return ok(c, Component.literal("elegir=" + com.coleccionista.prueba.Sentidos.segundosElegir + " juego="
                                            + com.coleccionista.prueba.Sentidos.segundos));
                                })))))
                .then(Commands.literal("mascota")
                        .executes(c -> ok(c, Component.literal(com.coleccionista.prueba.Mascota.resumen())))
                        .then(Commands.literal("llegar").executes(c -> {
                            Component no = com.coleccionista.prueba.Mascota.llegar(c.getSource().getServer());
                            return no == null ? ok(c, Component.literal(com.coleccionista.prueba.Mascota.resumen())) : error(c, no);
                        }))
                        .then(Commands.literal("saltar").executes(c -> com.coleccionista.prueba.Mascota.saltar(c.getSource().getServer())
                                ? ok(c, Component.literal(com.coleccionista.prueba.Mascota.resumen())) : error(c, Component.translatable(MOD + ".man.no"))))
                        .then(Commands.literal("soltar").executes(c -> com.coleccionista.prueba.Mascota.soltar(c.getSource().getServer())
                                ? ok(c, Component.literal(com.coleccionista.prueba.Mascota.resumen())) : error(c, Component.translatable(MOD + ".man.no"))))
                        .then(Commands.literal("terminar").executes(c -> com.coleccionista.prueba.Mascota.terminar(c.getSource().getServer())
                                ? ok(c, Component.literal(com.coleccionista.prueba.Mascota.resumen())) : error(c, Component.translatable(MOD + ".man.no"))))
                        .then(Commands.literal("limpiar").executes(c -> {
                            com.coleccionista.prueba.Mascota.reiniciar(c.getSource().getServer());
                            return ok(c, Component.literal(com.coleccionista.prueba.Mascota.resumen()));
                        }))
                        .then(Commands.literal("cazador")
                                .then(Commands.literal("nadie").executes(c -> {
                                    com.coleccionista.prueba.Mascota.elegir(null);
                                    return ok(c, Component.translatable(MOD + ".cmd.cazador_nadie"));
                                }))
                                .then(Commands.argument("jugador", EntityArgument.player()).executes(c -> {
                                    ServerPlayer p = EntityArgument.getPlayer(c, "jugador");
                                    com.coleccionista.prueba.Mascota.elegir(p.getUUID());
                                    return ok(c, Component.translatable(MOD + ".cmd.cazador", p.getGameProfile().getName()));
                                })))
                        .then(Commands.literal("comer").executes(c -> {
                            // la primera pieza viva (para la prueba automática)
                            Pieza presa = null;
                            for (Pieza v : Evento.get(c.getSource().getServer()).vivos()) {
                                if (!v.id.equals(com.coleccionista.prueba.Mascota.cazador())) {
                                    presa = v;
                                    break;
                                }
                            }
                            return presa != null && com.coleccionista.prueba.Mascota.comerA(c.getSource().getServer(), presa) ? ok(c, Component.literal(com.coleccionista.prueba.Mascota.resumen()))
                                    : error(c, Component.translatable(MOD + ".man.no"));
                        }).then(Commands.argument("pieza", StringArgumentType.word()).executes(c -> {
                            Pieza p = Evento.get(c.getSource().getServer()).buscar(StringArgumentType.getString(c, "pieza"));
                            return p != null && com.coleccionista.prueba.Mascota.comerA(c.getSource().getServer(), p) ? ok(c, Component.literal(com.coleccionista.prueba.Mascota.resumen()))
                                    : error(c, Component.translatable(MOD + ".man.no"));
                        })))
                        .then(Commands.literal("auto").then(Commands.argument("si", com.mojang.brigadier.arguments.BoolArgumentType.bool())
                                .executes(c -> {
                                    com.coleccionista.prueba.Mascota.auto = com.mojang.brigadier.arguments.BoolArgumentType.getBool(c, "si");
                                    return ok(c, Component.literal("auto=" + com.coleccionista.prueba.Mascota.auto));
                                })))
                        .then(Commands.literal("tiempos").then(Commands.argument("esconder", IntegerArgumentType.integer(5, 600))
                                .then(Commands.argument("cazar", IntegerArgumentType.integer(20, 3600)).executes(c -> {
                                    com.coleccionista.prueba.Mascota.segundosEsconder = IntegerArgumentType.getInteger(c, "esconder");
                                    com.coleccionista.prueba.Mascota.segundosCazar = IntegerArgumentType.getInteger(c, "cazar");
                                    return ok(c, Component.literal("esconder=" + com.coleccionista.prueba.Mascota.segundosEsconder + " cazar=" + com.coleccionista.prueba.Mascota.segundosCazar));
                                })))))
                .then(Commands.literal("salida").executes(c -> Celdas.hablarSalida(c.getSource().getServer())
                        ? ok(c, Component.translatable(MOD + ".cmd.salida")) : ok(c, Component.translatable(MOD + ".cmd.salida_espera"))))
                .then(Commands.literal("pantalla")
                        .then(Commands.literal("apagada").executes(c -> pantalla(c, "apagada", "")))
                        .then(Commands.literal("estatica").executes(c -> pantalla(c, "estatica", "")))
                        .then(Commands.literal("coleccionista").executes(c -> pantalla(c, "coleccionista", "")))
                        .then(Commands.literal("texto").then(Commands.argument("texto", StringArgumentType.greedyString())
                                .executes(c -> pantalla(c, "texto", StringArgumentType.getString(c, "texto")))))
                        // que el Coleccionista diga uno de sus discursos (para repetirlo o probarlo)
                        .then(Commands.literal("hablar").then(Commands.argument("guion", StringArgumentType.word())
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(List.of("bienvenida", "equipos", "sentidos", "vitrina", "vitrina_fin",
                                        "antesala", "mansion", "mansion_suelta", "mansion_fin", "caja", "caja_revision", "caja_traidores", "caja_fin", "montacargas", "encadenados", "traidor", "compartir",
                                        "ciegas", "sacrificio", "puertas", "copas", "tres"), b))
                                .executes(c -> Pantallas.hablar(c.getSource().getServer(), StringArgumentType.getString(c, "guion"), null)
                                        ? ok(c, Component.literal("habla: " + StringArgumentType.getString(c, "guion")))
                                        : error(c, Component.literal("no existe ese discurso")))))
                        .then(Commands.literal("callar").executes(c -> {
                            Pantallas.callar(c.getSource().getServer());
                            return ok(c, Component.literal("callado"));
                        })))
                .then(Commands.literal("celdas")
                        .then(Commands.literal("abrir").executes(c -> celdas(c, 0, true))
                                .then(Commands.argument("n", IntegerArgumentType.integer(1, com.coleccionista.mapa.AlaCeldas.CELDAS))
                                        .executes(c -> celdas(c, IntegerArgumentType.getInteger(c, "n"), true))))
                        .then(Commands.literal("cerrar").executes(c -> celdas(c, 0, false))))
                .then(Commands.literal("uniforme")
                        .then(Commands.literal("si").executes(c -> {
                            MinecraftServer sv = c.getSource().getServer();
                            Uniforme.activar(sv, Evento.get(sv), true);
                            return 1;
                        }))
                        .then(Commands.literal("no").executes(c -> {
                            MinecraftServer sv = c.getSource().getServer();
                            Uniforme.activar(sv, Evento.get(sv), false);
                            return 1;
                        })))
                .then(Commands.literal("chat")
                        .then(Commands.literal("cerrar").executes(c -> {
                            Chat.cerrar(c.getSource().getServer(), true);
                            return 1;
                        }))
                        .then(Commands.literal("abrir").executes(c -> {
                            Chat.cerrar(c.getSource().getServer(), false);
                            return 1;
                        })))
                .then(Commands.literal("candados")
                        .then(Commands.literal("si").executes(c -> {
                            Celdas.candados(c.getSource().getServer(), true);
                            return ok(c, Component.translatable(MOD + ".cmd.candados_si"));
                        }))
                        .then(Commands.literal("no").executes(c -> {
                            Celdas.candados(c.getSource().getServer(), false);
                            return ok(c, Component.translatable(MOD + ".cmd.candados_no"));
                        })))
                .then(Commands.literal("luces")
                        .then(Commands.literal("si").executes(c -> {
                            Celdas.luces(c.getSource().getServer(), true);
                            return ok(c, Component.translatable(MOD + ".cmd.luces"));
                        }))
                        .then(Commands.literal("no").executes(c -> {
                            Celdas.luces(c.getSource().getServer(), false);
                            return ok(c, Component.translatable(MOD + ".cmd.luces"));
                        })))
                // ---- las pruebas
                .then(com.coleccionista.prueba.ComandosPruebas.encadenados(vivas))
                .then(com.coleccionista.prueba.ComandosPruebas.compartir(vivas))
                .then(com.coleccionista.prueba.ComandosPruebas.puertas())
                .then(com.coleccionista.prueba.ComandosPruebas.copas())
                .then(com.coleccionista.prueba.ComandosPruebas.sacrificio())
                .then(com.coleccionista.prueba.ComandosPruebas.traidor(vivas))
                .then(com.coleccionista.prueba.ComandosCaja.caja(vivas))
                .then(com.coleccionista.prueba.ComandosCaja.probar())
                .then(com.coleccionista.prueba.ComandosSorpresa.sorpresa(vivas))
                .then(com.coleccionista.prueba.ComandosTe.te())
                .then(com.coleccionista.prueba.ComandosPiano.piano(vivas))
                .then(com.coleccionista.prueba.ComandosVeintiuno.veintiuno())
                .then(com.coleccionista.prueba.ComandosPruebas.ciegas(vivas))
                .then(com.coleccionista.prueba.ComandosPruebas.tres(vivas))
                .then(com.coleccionista.voz.ComandosVoz.voz(piezas(p -> true)))
                .then(Commands.literal("cuerpos").then(Commands.literal("limpiar").executes(c -> {
                    int n = Cuerpos.limpiar(c.getSource().getServer());
                    return ok(c, Component.translatable(MOD + ".cmd.cuerpos", n));
                }))));
        com.coleccionista.prueba.ComandosCaja.registrarBala(d, vivas);
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

    private static int estado(CommandContext<CommandSourceStack> c) {
        MinecraftServer s = c.getSource().getServer();
        Evento ev = Evento.get(s);
        int resta = ev.resta(s);
        c.getSource().sendSuccess(() -> Component.translatable(MOD + ".cmd.estado", ev.fase().name(),
                ev.prueba().isEmpty() ? "-" : ev.prueba(), ev.vivos().size(), ev.piezas().size(),
                resta < 0 ? "-" : String.format("%d:%02d", resta / 60, resta % 60)).withStyle(ChatFormatting.GOLD), false);
        for (Pieza p : ev.piezas()) {
            MutableComponent m = Component.literal(" " + p.etiqueta() + " " + p.nombre).withStyle(p.vivo() ? ChatFormatting.WHITE
                    : ChatFormatting.DARK_GRAY);
            if (p.equipo >= 0) {
                m.append(Component.literal("  eq " + (p.equipo + 1)).withStyle(ChatFormatting.AQUA));
            }
            if (!p.vivo()) {
                m.append(Component.literal("  ☠ #" + p.orden).withStyle(ChatFormatting.DARK_RED));
            }
            if (p.bot) {
                m.append(Component.literal("  (bot)").withStyle(ChatFormatting.DARK_GRAY));
            }
            c.getSource().sendSuccess(() -> m, false);
        }
        return 1;
    }

    private static int inscribir(CommandContext<CommandSourceStack> c, Collection<ServerPlayer> ps) {
        MinecraftServer s = c.getSource().getServer();
        Evento ev = Evento.get(s);
        int n = 0;
        for (ServerPlayer p : ps) {
            if (ev.esOrganizador(p.getUUID()) || ev.pieza(p) != null) {
                continue;
            }
            ev.inscribir(s, p.getUUID(), p.getGameProfile().getName(), false);
            n++;
        }
        int fn = n;
        return ok(c, Component.translatable(MOD + ".cmd.inscritos", fn, ev.piezas().size()));
    }

    private static int quitar(CommandContext<CommandSourceStack> c) {
        Pieza p = pieza(c);
        if (p == null) {
            return 0;
        }
        Evento.get(c.getSource().getServer()).quitar(c.getSource().getServer(), p);
        return ok(c, Component.translatable(MOD + ".cmd.quitado", p.nombre));
    }

    private static int organizador(CommandContext<CommandSourceStack> c, boolean si) throws CommandSyntaxException {
        MinecraftServer s = c.getSource().getServer();
        Evento ev = Evento.get(s);
        for (ServerPlayer p : EntityArgument.getPlayers(c, "jugadores")) {
            Pieza x = ev.pieza(p);
            if (si && x != null) {
                ev.quitar(s, x);
            }
            ev.organizador(s, p.getUUID(), si);
        }
        return ok(c, Component.translatable(MOD + (si ? ".cmd.organizador" : ".cmd.no_organizador")));
    }

    private static int eliminar(CommandContext<CommandSourceStack> c, String motivo) {
        Pieza p = pieza(c);
        if (p == null) {
            return 0;
        }
        Evento.Resultado r = Evento.get(c.getSource().getServer()).eliminar(c.getSource().getServer(), p, motivo);
        return r == Evento.Resultado.ELIMINADO ? ok(c, Component.translatable(MOD + ".cmd.eliminado", p.nombre))
                : error(c, Component.translatable(MOD + ".cmd.ya_eliminado", p.nombre));
    }

    private static int eliminarEquipo(CommandContext<CommandSourceStack> c) {
        MinecraftServer s = c.getSource().getServer();
        Evento ev = Evento.get(s);
        int n = IntegerArgumentType.getInteger(c, "n") - 1;
        List<Pieza> eq = ev.equipo(n);
        if (eq.isEmpty()) {
            return error(c, Component.translatable(MOD + ".cmd.equipo_vacio", n + 1));
        }
        for (Pieza p : eq) {
            ev.eliminar(s, p, "");
        }
        return ok(c, Component.translatable(MOD + ".cmd.equipo_eliminado", n + 1));
    }

    private static int revivir(CommandContext<CommandSourceStack> c) {
        Pieza p = pieza(c);
        if (p == null) {
            return 0;
        }
        boolean b = Evento.get(c.getSource().getServer()).revivir(c.getSource().getServer(), p);
        return b ? ok(c, Component.translatable(MOD + ".cmd.revivido", p.nombre)) : error(c, Component.translatable(MOD + ".cmd.ya_vivo",
                p.nombre));
    }

    private static int equipos(CommandContext<CommandSourceStack> c, int tam) {
        MinecraftServer s = c.getSource().getServer();
        Evento ev = Evento.get(s);
        if (ev.vivos().size() < 2) {
            return error(c, Component.translatable(MOD + ".cmd.pocos"));
        }
        int n = ev.equiposAlAzar(s, tam).size();
        return ok(c, Component.translatable(MOD + ".cmd.equipos", n));
    }

    private static int reloj(CommandContext<CommandSourceStack> c, int seg) {
        Evento.get(c.getSource().getServer()).reloj(c.getSource().getServer(), seg);
        return ok(c, Component.translatable(MOD + ".cmd.reloj", seg / 60, String.format("%02d", seg % 60)));
    }

    private static int construir(CommandContext<CommandSourceStack> c) {
        ServerLevel l = c.getSource().getLevel();
        if (l != c.getSource().getServer().overworld()) {
            return error(c, Component.translatable(MOD + ".cmd.solo_overworld"));
        }
        if (Complejo.construyendo()) {
            return error(c, Complejo.avance());
        }
        net.minecraft.core.BlockPos o = net.minecraft.core.BlockPos.containing(c.getSource().getPosition()).below();
        CommandSourceStack src = c.getSource();
        Complejo.construir(l, o, () -> src.sendSuccess(() -> Component.translatable(MOD + ".cmd.construido", o.getX(), o.getY(), o.getZ()),
                true));
        return ok(c, Component.translatable(MOD + ".cmd.construyendo", 0));
    }

    private static int disfraz(CommandContext<CommandSourceStack> c, Collection<ServerPlayer> ps, boolean si) throws CommandSyntaxException {
        MinecraftServer s = c.getSource().getServer();
        Collection<ServerPlayer> lista = ps != null ? ps : java.util.List.of(c.getSource().getPlayerOrException());
        for (ServerPlayer p : lista) {
            Evento.get(s).disfraz(s, p.getUUID(), si);
            if (si && p.isSpectator()) {
                p.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
            }
        }
        return ok(c, Component.translatable(MOD + (si ? ".cmd.disfraz_si" : ".cmd.disfraz_no")));
    }

    private static int pantalla(CommandContext<CommandSourceStack> c, String modo, String texto) {
        Pantallas.callar(c.getSource().getServer());
        Pantallas.modo(c.getSource().getServer(), modo, texto);
        return ok(c, Component.translatable(MOD + ".cmd.pantalla", modo));
    }

    private static int celdas(CommandContext<CommandSourceStack> c, int n, boolean abrir) {
        Celdas.puertas(c.getSource().getServer(), n, abrir);
        return ok(c, Component.translatable(MOD + (abrir ? ".cmd.celdas_abiertas" : ".cmd.celdas_cerradas")));
    }

    /** Pone jugadores de prueba en círculo alrededor de quien manda el comando y los inscribe. */
    private static int bots(CommandContext<CommandSourceStack> c, int n) {
        MinecraftServer s = c.getSource().getServer();
        Evento ev = Evento.get(s);
        ServerLevel l = c.getSource().getLevel();
        Vec3 o = c.getSource().getPosition();
        int hechos = 0;
        for (int i = 0; i < n; i++) {
            PiezaBot b = ModRegistry.BOT.get().create(l);
            if (b == null) {
                continue;
            }
            double a = Math.PI * 2 * i / n;
            double r = 2.5 + n * 0.15;
            b.moveTo(o.x + Math.cos(a) * r, o.y, o.z + Math.sin(a) * r, (float) Math.toDegrees(a) + 90F, 0F);
            b.finalizeSpawn(l, l.getCurrentDifficultyAt(b.blockPosition()), MobSpawnType.COMMAND, null, null);
            String nombre = String.format("Bot_%02d", ev.siguienteNumero);
            b.setCustomName(Component.literal(nombre));
            b.setCustomNameVisible(true);
            l.addFreshEntity(b);
            ev.inscribir(s, b.getUUID(), nombre, true);
            hechos++;
        }
        int h = hechos;
        return ok(c, Component.translatable(MOD + ".cmd.bots", h));
    }
}
