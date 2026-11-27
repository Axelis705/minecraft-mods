package com.navidad.libro;

import java.util.ArrayList;
import java.util.List;

/**
 * Lo que trae el Libro de Navidad: cuatro capítulos con seis tareas de survival cada uno. Cada tarea dice qué la hace avanzar (un evento
 * del juego, un logro o el nivel de tu árbol), cuántas veces, y qué adornos te da. Al terminar las seis de un capítulo, su premio grande
 * (la estrella, el ángel y la estrella polar para la punta). Lo usan el servidor (para contar y dar premios) y la pantalla del libro.
 */
public final class Tareas {
    /** Cómo avanza una tarea: sumando cada vez que pasa algo, o con el número más alto que llegue (nivel o variedad del árbol). */
    public enum Modo {
        SUMA, MAXIMO
    }

    /** Un premio: un objeto ("navidad:adorno_copo", "minecraft:diamond") y cuántos. */
    public record Premio(String id, int n) {
    }

    /** Una tarea: id, capítulo (0..3), qué la mueve ("adorno", "logro:pinguino", "nivel_arbol"...), meta, ícono y premios. */
    public record Tarea(String id, int cap, String evento, Modo modo, int meta, String icono, List<Premio> premios) {
    }

    public record Capitulo(String id, String icono, int color, List<Premio> premios) {
    }

    public static final List<Capitulo> CAPITULOS = List.of(
            new Capitulo("preparativos", "navidad:pino_navidad", 0xC81E2A, List.of(p("navidad:punta_estrella", 1), p("navidad:adorno_luces", 1))),
            new Capitulo("invierno", "minecraft:snowball", 0x3A7AE8, List.of(p("navidad:adorno_cristal", 2), p("navidad:adorno_luces", 1),
                    p("navidad:lanzanieves", 1))),
            new Capitulo("santa", "navidad:caja_regalo", 0x2E8A48, List.of(p("navidad:punta_angel", 1), p("navidad:adorno_guirnalda", 1),
                    p("navidad:adorno_cristal", 2))),
            new Capitulo("nochebuena", "navidad:punta_polar", 0xB8860B, List.of(p("navidad:punta_polar", 1), p("minecraft:diamond", 5))));

    public static final List<Tarea> TODAS = new ArrayList<>();

    private static Premio p(String id, int n) {
        return new Premio(id, n);
    }

    private static void t(String id, int cap, String evento, int meta, String icono, Premio... premios) {
        TODAS.add(new Tarea(id, cap, evento, Modo.SUMA, meta, icono, List.of(premios)));
    }

    private static void m(String id, int cap, String evento, int meta, String icono, Premio... premios) {
        TODAS.add(new Tarea(id, cap, evento, Modo.MAXIMO, meta, icono, List.of(premios)));
    }

    static {
        // 1. Preparativos: tu árbol y tu casa
        t("poner_arbol", 0, "arbol_puesto", 1, "navidad:pino_navidad", p("navidad:adorno_esfera_roja", 3));
        t("colgar", 0, "adorno", 5, "navidad:adorno_esfera_roja", p("navidad:adorno_baston", 2), p("navidad:adorno_esfera_dorada", 2));
        t("troncos", 0, "tronco_abeto", 16, "minecraft:spruce_log", p("navidad:adorno_guirnalda", 1), p("navidad:adorno_mono", 2));
        t("casa", 0, "deco_puesta", 10, "navidad:luces_colgantes", p("navidad:adorno_esfera_azul", 2), p("navidad:adorno_campanita", 1));
        t("bebida", 0, "bebida", 1, "navidad:chocolate_caliente", p("navidad:adorno_galleta", 2));
        t("abeto", 0, "logro:puesto", 1, "navidad:letrero_pinos", p("navidad:adorno_luces", 1));
        // 2. Espíritu de invierno: la nieve y sus animalitos
        t("pinguino", 1, "logro:pinguino", 1, "minecraft:cod", p("navidad:adorno_pinguino", 1));
        t("golem", 1, "golem_nieve", 1, "minecraft:carved_pumpkin", p("navidad:adorno_copo", 2));
        t("pescar", 1, "pescar", 8, "minecraft:fishing_rod", p("navidad:adorno_pina", 2));
        t("angel", 1, "logro:angel", 1, "navidad:angel_nieve", p("navidad:adorno_copo", 1), p("navidad:adorno_esfera_azul", 1));
        t("bolas", 1, "bola_nieve", 50, "minecraft:snowball", p("navidad:adorno_guirnalda", 1), p("navidad:adorno_campanita", 1));
        t("renito", 1, "logro:renito", 1, "minecraft:golden_carrot", p("navidad:adorno_renito", 1));
        // 3. La magia de Santa
        t("regalo_aire", 2, "logro:regalo_cielo", 1, "navidad:caja_regalo", p("navidad:adorno_pajarito", 1));
        t("abrir", 2, "regalo_abierto", 10, "navidad:caja_regalo", p("navidad:adorno_campanita", 2));
        t("galletas", 2, "logro:galletas", 1, "navidad:galletas_leche", p("navidad:adorno_galleta", 2));
        t("carta", 2, "logro:carta", 1, "navidad:buzon_santa", p("navidad:adorno_mono", 2), p("navidad:adorno_esfera_roja", 2));
        t("calcetin", 2, "logro:calcetin", 1, "navidad:calcetin_rojo", p("navidad:adorno_baston", 3));
        t("ladron", 2, "logro:ladron", 1, "navidad:gorro_duende", p("navidad:adorno_cristal", 1));
        // 4. Nochebuena: lo más difícil
        m("nivel_3", 3, "nivel_arbol", 3, "navidad:adorno_campanita", p("navidad:adorno_luces", 1));
        m("nivel_5", 3, "nivel_arbol", 5, "navidad:punta_estrella", p("navidad:adorno_cristal", 2));
        m("variedad", 3, "variedad", 12, "navidad:adorno_pajarito", p("navidad:adorno_luces", 1), p("navidad:adorno_pajarito", 1));
        t("munecos", 3, "muneco", 10, "navidad:huevo_muneco_malvado", p("navidad:adorno_pinguino", 1));
        t("viajero", 3, "logro:viajero", 1, "navidad:gorro_santa", p("navidad:adorno_renito", 1), p("navidad:adorno_pajarito", 1));
        t("krampus", 3, "logro:krampus", 1, "navidad:cuerno_krampus", p("navidad:adorno_cristal", 2), p("minecraft:diamond", 3));
    }

    private Tareas() {
    }

    public static Tarea tarea(String id) {
        for (Tarea t : TODAS) {
            if (t.id().equals(id)) {
                return t;
            }
        }
        return null;
    }

    public static List<Tarea> delCapitulo(int cap) {
        List<Tarea> l = new ArrayList<>();
        for (Tarea t : TODAS) {
            if (t.cap() == cap) {
                l.add(t);
            }
        }
        return l;
    }
}
