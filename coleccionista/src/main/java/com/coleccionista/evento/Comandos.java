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
                                .executes(c -> eliminarEquipo(c, false))
                                .then(Commands.literal("forzar").executes(c -> eliminarEquipo(c, true)))))
                        .then(Commands.argument("pieza", StringArgumentType.word()).suggests(vivas)
                                .executes(c -> eliminar(c, "", false))
                                .then(Commands.literal("forzar").executes(c -> eliminar(c, "", true)))
                                .then(Commands.argument("motivo", StringArgumentType.greedyString())
                                        .executes(c -> eliminar(c, StringArgumentType.getString(c, "motivo"), false)))))
                .then(Commands.literal("revivir").then(Commands.argument("pieza", StringArgumentType.word()).suggests(fantasmas)
                        .executes(Comandos::revivir)))
                .then(Commands.literal("ficha").then(Commands.argument("pieza", StringArgumentType.word()).suggests(todas)
                        .then(Commands.argument("cantidad", IntegerArgumentType.integer(-10, 10)).executes(Comandos::ficha))))
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
                        .executes(c -> bots(c, IntegerArgumentType.getInteger(c, "cantidad"))))));
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
            m.append(Component.literal("  ♥" + p.fichas).withStyle(ChatFormatting.GOLD));
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

    private static int eliminar(CommandContext<CommandSourceStack> c, String motivo, boolean forzar) {
        Pieza p = pieza(c);
        if (p == null) {
            return 0;
        }
        Evento.Resultado r = Evento.get(c.getSource().getServer()).eliminar(c.getSource().getServer(), p, motivo, forzar);
        return switch (r) {
            case ELIMINADO -> ok(c, Component.translatable(MOD + ".cmd.eliminado", p.nombre));
            case SALVADO -> ok(c, Component.translatable(MOD + ".cmd.salvado", p.nombre));
            case YA_ESTABA -> error(c, Component.translatable(MOD + ".cmd.ya_eliminado", p.nombre));
        };
    }

    private static int eliminarEquipo(CommandContext<CommandSourceStack> c, boolean forzar) {
        MinecraftServer s = c.getSource().getServer();
        Evento ev = Evento.get(s);
        int n = IntegerArgumentType.getInteger(c, "n") - 1;
        List<Pieza> eq = ev.equipo(n);
        if (eq.isEmpty()) {
            return error(c, Component.translatable(MOD + ".cmd.equipo_vacio", n + 1));
        }
        for (Pieza p : eq) {
            ev.eliminar(s, p, "", forzar);
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

    private static int ficha(CommandContext<CommandSourceStack> c) {
        Pieza p = pieza(c);
        if (p == null) {
            return 0;
        }
        int n = IntegerArgumentType.getInteger(c, "cantidad");
        Evento.get(c.getSource().getServer()).fichas(c.getSource().getServer(), p, n);
        return ok(c, Component.translatable(MOD + ".cmd.fichas", p.nombre, p.fichas));
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
