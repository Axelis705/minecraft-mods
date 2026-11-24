package com.coleccionista.evento;

import com.coleccionista.Coleccionista;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Atajos para moderar con números: {@code /minijuego <número> <acción>}, por ejemplo {@code /minijuego 1 empezar} o
 * {@code /minijuego 3 terminar}. El 0 es el despertar; del 1 al 7 las pruebas en orden; el 8 la prueba extra. Cada atajo corre el comando
 * largo de {@code /evento} (mismos permisos y mismos mensajes). {@code /minijuego} solo, o con el número, dice qué hay.
 */
public final class ComandosMinijuego {
    private static final String MOD = Coleccionista.MODID;
    /** Nombre (para /evento) y zona del montacargas de cada número. */
    private static final String[] PRUEBA = {"", "encadenados", "traidor", "compartir", "ciegas", "sacrificio", "puertas", "copas", "tres"};
    private static final String[] ZONA = {"", "cloacas", "archivo", "salon", "laberintos", "capilla", "puertas", "final", "galeria"};

    private ComandosMinijuego() {
    }

    /** Las acciones de un número y el comando largo que corre cada una. */
    static Map<String, String> acciones(int n) {
        Map<String, String> m = new LinkedHashMap<>();
        if (n == 0) {
            m.put("sala", "evento sala");
            m.put("despertar", "evento despertar");
            m.put("discurso", "evento discurso");
            m.put("abrir", "evento celdas abrir");
            m.put("salida", "evento salida");
            m.put("recorrido", "evento recorrido iniciar");
            return m;
        }
        String p = PRUEBA[n];
        // llevarlas a la sala de llegada de la prueba (en el elevador) y seguir el recorrido desde ahí
        m.put("ir", "evento recorrido ir " + p);
        m.put("montacargas", "evento recorrido ir " + p);
        m.put("preparar", "evento " + p + " preparar");
        m.put("empezar", n == 6 ? "evento puertas discurso" : "evento " + p + " empezar");
        m.put("ya", n == 6 ? "evento puertas ronda" : "evento " + p + " empezar ya");
        switch (n) {
            case 1 -> {
                m.put("soltar", "evento encadenados soltar todos");
                m.put("cerrar", "evento encadenados cerrar");
            }
            case 2, 4 -> m.put("cortar", "evento " + p + " cortar");
            case 3 -> {
                m.put("elegir", "evento compartir elegir");
                m.put("revelar", "evento compartir revelar");
            }
            case 5 -> m.put("votar", "evento sacrificio votar");
            case 6 -> {
                m.put("ronda", "evento puertas ronda");
                m.put("cerrar", "evento puertas cerrar");
            }
            case 7 -> m.put("beber", "evento copas beber");
            case 8 -> m.put("siguiente", "evento tres arrancar");
            default -> {
            }
        }
        m.put("terminar", "evento " + p + " terminar");
        return m;
    }

    public static void registrar(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("minijuego").requires(s -> s.hasPermission(2))
                .executes(ComandosMinijuego::ayuda)
                .then(Commands.argument("numero", IntegerArgumentType.integer(0, 8))
                        .executes(c -> ayudaDe(c, IntegerArgumentType.getInteger(c, "numero")))
                        .then(Commands.argument("accion", StringArgumentType.word())
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(acciones(IntegerArgumentType.getInteger(c, "numero")).keySet(), b))
                                .executes(ComandosMinijuego::correr))));
    }

    private static int correr(CommandContext<CommandSourceStack> c) {
        int n = IntegerArgumentType.getInteger(c, "numero");
        String a = StringArgumentType.getString(c, "accion");
        String cmd = acciones(n).get(a);
        if (cmd == null) {
            c.getSource().sendFailure(Component.translatable(MOD + ".mini.no_accion", n, String.join(", ", acciones(n).keySet())));
            return 0;
        }
        Coleccionista.LOGGER.info("COLECCIONISTA_MINIJUEGO {} {} -> /{}", n, a, cmd);
        c.getSource().getServer().getCommands().performPrefixedCommand(c.getSource(), cmd);
        return 1;
    }

    private static int ayuda(CommandContext<CommandSourceStack> c) {
        c.getSource().sendSuccess(() -> Component.translatable(MOD + ".mini.ayuda").withStyle(ChatFormatting.GOLD), false);
        for (int n = 0; n <= 8; n++) {
            int k = n;
            c.getSource().sendSuccess(() -> Component.literal(" " + k + " · ").append(Component.translatable(MOD + ".mini.n" + k))
                    .withStyle(ChatFormatting.GRAY), false);
        }
        return 1;
    }

    private static int ayudaDe(CommandContext<CommandSourceStack> c, int n) {
        c.getSource().sendSuccess(() -> Component.literal(n + " · ").append(Component.translatable(MOD + ".mini.n" + n))
                .append(": " + String.join(" · ", acciones(n).keySet())).withStyle(ChatFormatting.GOLD), false);
        return 1;
    }
}
