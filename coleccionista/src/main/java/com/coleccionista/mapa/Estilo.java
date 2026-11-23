package com.coleccionista.mapa;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Pasa un plano a los bloques nuevos del complejo: lo que una zona todavía pone con bloques de Minecraft (faroles, velas, cadenas,
 * barrotes, tablones...) se cambia por el bloque propio que le toca, con su orientación. Cada zona da además su tabla de materiales (qué
 * piedra, piso o madera de Minecraft se vuelve qué material nuevo). No usa nada de Minecraft (se puede correr en el visor).
 */
public final class Estilo {
    private static final String C = "coleccionista:";
    /** Lo que se cambia igual en todas las zonas. */
    private static final Map<String, String> COMUN = new HashMap<>();
    /** Bloques que se quedan como están (no son "de construcción": agua, luz invisible, cabezas de jugador, la pantalla...). */
    public static final Set<String> SE_QUEDAN = Set.of("air", "water", "light", "player_head", "player_wall_head", "barrier");

    static {
        COMUN.put("iron_bars", C + "barrotes");
        COMUN.put("chain", C + "cadena");
        COMUN.put("lantern", C + "farol");
        COMUN.put("soul_lantern", C + "farol");
        COMUN.put("candle", C + "vela");
        COMUN.put("cobweb", C + "telarana");
        COMUN.put("glow_lichen", C + "musgo");
        COMUN.put("tripwire_hook", C + "argolla");
        COMUN.put("barrel", C + "barril");
        COMUN.put("red_carpet", C + "alfombra_roja");
        COMUN.put("black_carpet", C + "alfombra_negra");
        COMUN.put("heavy_weighted_pressure_plate", C + "placa_presion");
        COMUN.put("light_weighted_pressure_plate", C + "placa_presion");
        COMUN.put("iron_trapdoor", C + "trampilla");
        COMUN.put("glass", C + "vidrio_sucio");
        COMUN.put("spruce_planks", C + "tablon");
        COMUN.put("dark_oak_planks", C + "tablon");
        COMUN.put("spruce_slab", C + "losa_tablon");
        COMUN.put("spruce_stairs", C + "escalera_tablon");
        COMUN.put("spruce_fence", C + "barandal");
        COMUN.put("dark_oak_fence", C + "barandal");
        COMUN.put("stone_brick_stairs", C + "escalera_ladrillo");
        COMUN.put("mud", C + "lodo");
        COMUN.put("packed_mud", C + "lodo");
        COMUN.put("stone", C + "lodo");
        COMUN.put("waxed_copper_block", C + "cobre_viejo");
    }

    /** Las Cloacas (la roca, el piso, el techo y el cobre ya los pone con bloques nuevos el mapa). */
    public static final Map<String, String> CLOACAS = Map.of(
            "deepslate_tiles", C + "concreto_piso",
            "cracked_deepslate_tiles", C + "concreto_piso",
            "polished_deepslate", C + "concreto_piso",
            "polished_blackstone_bricks", C + "ladrillo_oscuro",
            "chiseled_polished_blackstone", C + "ladrillo_oscuro",
            "polished_blackstone", C + "metal_placa");

    private Estilo() {
    }

    /** Cambia todo el plano. {@code zona}: la tabla de materiales de la zona (puede ser vacía). */
    public static void aplicar(Plano p, Map<String, String> zona) {
        p.m.replaceAll((k, v) -> convertir(v, zona));
    }

    /** Lo que quedó sin cambiar (para revisar que todo sea nuevo): id → cuántos. */
    public static Map<String, Integer> pendientes(Plano p) {
        Map<String, Integer> out = new TreeMap<>();
        for (String v : p.m.values()) {
            String id = id(v);
            if (!v.startsWith(C) && !SE_QUEDAN.contains(id)) {
                out.merge(id, 1, Integer::sum);
            }
        }
        return out;
    }

    private static String id(String spec) {
        int i = spec.indexOf('[');
        String id = i < 0 ? spec : spec.substring(0, i);
        return id.startsWith("minecraft:") ? id.substring(10) : id;
    }

    static String convertir(String spec, Map<String, String> zona) {
        if (spec.startsWith(C)) {
            return spec;
        }
        String id = id(spec);
        String nuevo = zona.containsKey(id) ? zona.get(id) : COMUN.get(id);
        if (nuevo == null) {
            return spec;
        }
        int i = spec.indexOf('[');
        Map<String, String> pr = new HashMap<>();
        if (i >= 0) {
            for (String kv : spec.substring(i + 1, spec.length() - 1).split(",")) {
                int e = kv.indexOf('=');
                if (e > 0) {
                    pr.put(kv.substring(0, e).trim(), kv.substring(e + 1).trim());
                }
            }
        }
        // escaleras, losas, barrotes/cercas, cadenas y trampillas: las mismas propiedades
        if (id.endsWith("_stairs") || id.endsWith("_slab") || id.endsWith("_trapdoor") || id.equals("chain")) {
            return nuevo + (i >= 0 ? spec.substring(i) : "");
        }
        if (id.equals("iron_bars") || id.endsWith("_fence") || id.endsWith("_pane")) {
            pr.keySet().retainAll(Set.of("north", "east", "south", "west", "waterlogged"));
            return con(nuevo, pr);
        }
        switch (id) {
            case "tripwire_hook" -> {
                return nuevo + "[facing=" + pr.getOrDefault("facing", "north") + "]";
            }
            case "barrel" -> {
                String f = pr.getOrDefault("facing", "north");
                return nuevo + "[facing=" + (f.equals("up") || f.equals("down") ? "north" : f) + "]";
            }
            case "glow_lichen" -> {
                // el liquen se pega a una cara: la calcomania queda con la espalda en esa pared
                for (String[] c : new String[][] {{"north", "south"}, {"south", "north"}, {"east", "west"}, {"west", "east"}}) {
                    if ("true".equals(pr.get(c[0]))) {
                        return nuevo + "[facing=" + c[1] + "]";
                    }
                }
                return "air";
            }
            default -> {
                return nuevo;
            }
        }
    }

    private static String con(String id, Map<String, String> pr) {
        if (pr.isEmpty()) {
            return id;
        }
        StringBuilder sb = new StringBuilder(id).append('[');
        boolean first = true;
        for (Map.Entry<String, String> e : new TreeMap<>(pr).entrySet()) {
            sb.append(first ? "" : ",").append(e.getKey()).append('=').append(e.getValue());
            first = false;
        }
        return sb.append(']').toString();
    }
}
