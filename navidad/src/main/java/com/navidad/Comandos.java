package com.navidad;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.navidad.entidad.Pinata;
import com.navidad.objeto.Ropa;
import com.navidad.registry.ModRegistry;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/** {@code /navidad ...}: la temporada, el calendario, regalos, piñatas, ropa y lo demás (casi todo para operadores). */
public final class Comandos {
    private Comandos() {
    }

    public static void registrar(CommandDispatcher<CommandSourceStack> d) {
        List<String> ropa = new ArrayList<>(Ropa.CABEZA);
        ropa.addAll(Ropa.CUERPO);
        ropa.addAll(Ropa.PIES);
        ropa.addAll(Ropa.CUELLO);
        d.register(Commands.literal(Navidad.MODID)
                .executes(c -> {
                    c.getSource().sendSuccess(() -> Component.translatable("navidad.hola", com.axeliscore.AxelisCore.version(),
                            Component.translatable(Temporada.activa(c.getSource().getServer()) ? "navidad.temporada.si" : "navidad.temporada.no")), false);
                    Navidad.LOGGER.info("PRUEBA_NAVIDAD_OK {}", com.axeliscore.AxelisCore.version());
                    return 1;
                })
                .then(Commands.literal("temporada").requires(s -> s.hasPermission(2))
                        .then(Commands.argument("modo", StringArgumentType.word())
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(List.of("si", "no", "auto"), b))
                                .executes(c -> {
                                    String m = StringArgumentType.getString(c, "modo");
                                    if (!List.of("si", "no", "auto").contains(m)) {
                                        c.getSource().sendFailure(Component.literal("si | no | auto"));
                                        return 0;
                                    }
                                    Temporada.modo(c.getSource().getServer(), m);
                                    Fiestas.avisar(c.getSource().getServer());
                                    c.getSource().sendSuccess(() -> Component.translatable("navidad.temporada.modo", m), true);
                                    return 1;
                                })))
                .then(Commands.literal("calendario").requires(s -> s.hasPermission(2))
                        .then(Commands.literal("dia").then(Commands.argument("dia", IntegerArgumentType.integer(0, 31)).executes(c -> {
                            Temporada.diaPrueba = IntegerArgumentType.getInteger(c, "dia");
                            Fiestas.avisar(c.getSource().getServer());
                            c.getSource().sendSuccess(() -> Component.translatable("navidad.calendario.fingido", Temporada.diaPrueba), true);
                            return 1;
                        }))))
                .then(Commands.literal("regalo").requires(s -> s.hasPermission(2))
                        .executes(c -> regalo(c, List.of(c.getSource().getPlayerOrException())))
                        .then(Commands.argument("quien", EntityArgument.players())
                                .executes(c -> regalo(c, new ArrayList<>(EntityArgument.getPlayers(c, "quien"))))))
                .then(Commands.literal("pinata").requires(s -> s.hasPermission(2)).executes(Comandos::pinata))
                .then(Commands.literal("aldea").requires(s -> s.hasPermission(2))
                        .executes(c -> aldea(c, true))
                        .then(Commands.literal("sin_gente").executes(c -> aldea(c, false))))
                .then(Commands.literal("gente").requires(s -> s.hasPermission(2))
                        .then(Commands.argument("quien", StringArgumentType.word())
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(GENTE, b))
                                .executes(c -> {
                                    var def = com.navidad.gente.Gente.porNombre(StringArgumentType.getString(c, "quien"));
                                    if (def == null) {
                                        c.getSource().sendFailure(Component.literal(String.join(" | ", GENTE)));
                                        return 0;
                                    }
                                    com.navidad.gente.Gente.poner(c.getSource().getLevel(), def, net.minecraft.core.BlockPos.containing(c.getSource().getPosition()));
                                    return 1;
                                })))
                .then(Commands.literal("trineo").requires(s -> s.hasPermission(2)).executes(c -> {
                    Fiestas.trineo(c.getSource().getLevel(), c.getSource().getPlayerOrException());
                    return 1;
                }))
                .then(Commands.literal("posada").requires(s -> s.hasPermission(2)).executes(c -> {
                    Fiestas.posada(c.getSource().getLevel(), c.getSource().getPlayerOrException());
                    return 1;
                }))
                .then(Commands.literal("intercambio").requires(s -> s.hasPermission(2)).executes(c -> {
                    int n = Fiestas.intercambio(c.getSource().getServer());
                    if (n == 0) {
                        c.getSource().sendFailure(Component.translatable("navidad.fiesta.intercambio_pocos"));
                    } else {
                        c.getSource().sendSuccess(() -> Component.translatable("navidad.fiesta.intercambio_listo", n), true);
                    }
                    return n;
                }))
                .then(Commands.literal("anio_nuevo").requires(s -> s.hasPermission(2))
                        .executes(c -> {
                            AnioNuevo.empezar(c.getSource().getServer(), AnioNuevo.CUENTA);
                            return 1;
                        })
                        .then(Commands.argument("segundos", IntegerArgumentType.integer(0, 120)).executes(c -> {
                            AnioNuevo.empezar(c.getSource().getServer(), IntegerArgumentType.getInteger(c, "segundos"));
                            return 1;
                        })))
                .then(Commands.literal("galletas").requires(s -> s.hasPermission(2)).executes(c -> {
                    int n = GalletasSanta.revisar(c.getSource().getServer(), true);
                    c.getSource().sendSuccess(() -> Component.literal("Santa pasó por " + n + " platos de galletas"), true);
                    return n;
                }))
                .then(Commands.literal("arbol").requires(s -> s.hasPermission(2)).executes(c -> {
                    int n = ArbolSanta.manana(c.getSource().getServer(), true);
                    c.getSource().sendSuccess(() -> Component.literal("Santa pasó por " + n + " árboles"), true);
                    return n;
                }))
                .then(Commands.literal("viajero").requires(s -> s.hasPermission(2)).executes(c -> {
                    ServerPlayer sp = c.getSource().getPlayerOrException();
                    boolean ok = Viajero.llegar(sp.serverLevel(), sp);
                    c.getSource().sendSuccess(() -> Component.literal(ok ? "Llegó el duende viajero" : "No encontré dónde ponerlo"), true);
                    return ok ? 1 : 0;
                }))
                .then(Commands.literal("ladron").requires(s -> s.hasPermission(2)).executes(c -> {
                    boolean ok = ArbolSanta.ladronYa(c.getSource().getPlayerOrException());
                    c.getSource().sendSuccess(() -> Component.literal(ok ? "Ahí viene el duende ladrón" : "No hay regalos bajo tu árbol"), true);
                    return ok ? 1 : 0;
                }))
                .then(Commands.literal("carta").requires(s -> s.hasPermission(2))
                        .then(Commands.literal("entregar").executes(c -> {
                            ServerPlayer sp = c.getSource().getPlayerOrException();
                            boolean ok = Cartas.entregar(sp, true);
                            c.getSource().sendSuccess(() -> Component.literal(ok ? "carta entregada" : "no hay carta pendiente"), false);
                            return ok ? 1 : 0;
                        })))
                .then(Commands.literal("krampus").requires(s -> s.hasPermission(2)).executes(c -> {
                    Vec3 pos = c.getSource().getPosition();
                    Vec3 mira = Vec3.directionFromRotation(0, c.getSource().getRotation().y).scale(10);
                    com.navidad.entidad.Krampus.llamar(c.getSource().getLevel(), pos.add(mira), c.getSource().getEntity() instanceof
                            net.minecraft.world.entity.LivingEntity le ? le : null);
                    return 1;
                })
                        // la Noche del Krampus ya (para quien se diga, o para ti)
                        .then(Commands.literal("noche").executes(c -> nocheKrampus(c, java.util.List.of(c.getSource().getPlayerOrException())))
                                .then(Commands.argument("jugadores", net.minecraft.commands.arguments.EntityArgument.players())
                                        .executes(c -> nocheKrampus(c, net.minecraft.commands.arguments.EntityArgument.getPlayers(c, "jugadores")))))
                        .then(Commands.literal("visitas")
                                .then(Commands.literal("si").executes(c -> visitas(c, true)))
                                .then(Commands.literal("no").executes(c -> visitas(c, false))))
                        .then(Commands.literal("krampusillos").executes(c -> {
                            ServerPlayer sp = c.getSource().getPlayerOrException();
                            boolean arbol = com.navidad.krampus.NocheKrampus.diablitosAlArbol(sp);
                            if (!arbol) {
                                for (int i = 0; i < 2; i++) {
                                    Vec3 lado = Vec3.directionFromRotation(0, sp.getYRot() + (i == 0 ? 30 : -30)).scale(6);
                                    com.navidad.entidad.Krampusillo.llamar(sp.serverLevel(), sp.position().add(lado), sp, null);
                                }
                            }
                            return 1;
                        })))
                .then(Commands.literal("guarida").requires(s -> s.hasPermission(2)).executes(c -> {
                    net.minecraft.core.BlockPos p = net.minecraft.core.BlockPos.containing(c.getSource().getPosition());
                    boolean ok = com.navidad.mundo.Guarida.construir(c.getSource().getLevel(), p, c.getSource().getLevel().random, true);
                    c.getSource().sendSuccess(() -> Component.translatable("navidad.guarida.lista"), true);
                    return ok ? 1 : 0;
                }))
                .then(Commands.literal("reiniciar").requires(s -> s.hasPermission(2))
                        .then(reiniciar("libro", Reinicio::libro))
                        .then(reiniciar("santa", Reinicio::santa))
                        .then(reiniciar("todo", Reinicio::todo)))
                .then(Commands.literal("ropa").requires(s -> s.hasPermission(2))
                        .then(Commands.argument("prenda", StringArgumentType.word())
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(ropa, b))
                                .executes(c -> {
                                    String p = StringArgumentType.getString(c, "prenda");
                                    if (!ropa.contains(p)) {
                                        c.getSource().sendFailure(Component.literal(String.join(" | ", ropa)));
                                        return 0;
                                    }
                                    ServerPlayer sp = c.getSource().getPlayerOrException();
                                    Ropa.alternar(c.getSource().getServer(), sp, p);
                                    return 1;
                                }))));
    }

    /** {@code reiniciar <que> [jugadores]}: sin jugadores, a quien escribe el comando. */
    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> reiniciar(String que,
            java.util.function.Consumer<ServerPlayer> hacer) {
        return Commands.literal(que)
                .executes(c -> {
                    hacer.accept(c.getSource().getPlayerOrException());
                    return 1;
                })
                .then(Commands.argument("jugadores", EntityArgument.players()).executes(c -> {
                    java.util.Collection<ServerPlayer> ps = EntityArgument.getPlayers(c, "jugadores");
                    ps.forEach(hacer);
                    c.getSource().sendSuccess(() -> Component.translatable("navidad.reinicio.listo", que, ps.size()), true);
                    return ps.size();
                }));
    }

    static final List<String> GENTE = List.of("santa", "senora_claus", "duende_juguetero", "duende_sastre", "duende_renos");

    private static int nocheKrampus(CommandContext<CommandSourceStack> c, java.util.Collection<ServerPlayer> quienes) {
        int n = 0;
        for (ServerPlayer p : quienes) {
            if (com.navidad.krampus.NocheKrampus.empezar(p)) {
                n++;
            }
        }
        int k = n;
        c.getSource().sendSuccess(() -> Component.translatable("navidad.noche_krampus.comando", k), true);
        return n;
    }

    private static int visitas(CommandContext<CommandSourceStack> c, boolean si) {
        com.navidad.krampus.NocheKrampus.visitas(c.getSource().getServer(), si);
        c.getSource().sendSuccess(() -> Component.translatable(si ? "navidad.noche_krampus.visitas_si" : "navidad.noche_krampus.visitas_no"), true);
        return 1;
    }

    private static int aldea(CommandContext<CommandSourceStack> c, boolean gente) {
        net.minecraft.core.BlockPos p = net.minecraft.core.BlockPos.containing(c.getSource().getPosition());
        long t0 = System.currentTimeMillis();
        int n = com.navidad.aldea.Aldea.construir(c.getSource().getLevel(), p, gente);
        long ms = System.currentTimeMillis() - t0;
        c.getSource().sendSuccess(() -> Component.translatable("navidad.aldea.lista", n, ms), true);
        return 1;
    }

    private static int regalo(CommandContext<CommandSourceStack> c, List<ServerPlayer> ps) {
        for (ServerPlayer p : ps) {
            ItemStack st = com.navidad.objeto.Premios.caja(p.getRandom(), null, "Santa");
            if (!p.getInventory().add(st)) {
                p.drop(st, false);
            }
        }
        c.getSource().sendSuccess(() -> Component.translatable("navidad.regalo.dado", ps.size()), true);
        return ps.size();
    }

    private static int pinata(CommandContext<CommandSourceStack> c) {
        ServerLevel l = c.getSource().getLevel();
        Vec3 pos = c.getSource().getPosition();
        Vec3 mira = Vec3.directionFromRotation(0, c.getSource().getRotation().y);
        Pinata p = ModRegistry.PINATA_ENT.get().create(l);
        if (p == null) {
            return 0;
        }
        p.moveTo(pos.x + mira.x * 2.5, pos.y + 0.3, pos.z + mira.z * 2.5, c.getSource().getRotation().y, 0);
        l.addFreshEntity(p);
        l.playSound(null, p.blockPosition(), ModRegistry.CANCION_PINATA.get(), net.minecraft.sounds.SoundSource.RECORDS, 1.2F, 1F);
        return 1;
    }
}
