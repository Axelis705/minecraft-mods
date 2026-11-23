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

    private static int empezar(CommandContext<CommandSourceStack> c, boolean discurso) {
        return Encadenados.empezar(c.getSource().getServer(), discurso) ? ok(c, Component.translatable(MOD + ".enc.cmd.empieza"))
                : error(c, Component.translatable(MOD + ".enc.cmd.no_lista"));
    }
}
