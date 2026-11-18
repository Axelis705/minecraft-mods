package com.mundosdeaxelis.gear;

import java.util.EnumMap;
import java.util.Map;

/** Capacidades que se acumulan en las herramientas por set y rareza. El número que las acompaña es su nivel. */
public enum Perk {
    /** Experiencia extra al romper menas. */
    XP,
    /** Mina en área (1: 3x3, 2: 3x3x3, 3: 5x5x2). */
    AREA,
    /** Rompe toda la veta conectada (tope según nivel). */
    VEIN,
    /** Funde lo que sueltan los bloques. */
    SMELT,
    /** Habilidad: revela los minerales cercanos. */
    RADAR,
    /** Los objetos caen directo a tu inventario. */
    MAGNET,
    /** Prisa mientras la sostienes. */
    HASTE,
    /** Tala árboles (1: poco, 2: normal, 3: grandes, 4: gigantes). */
    FELL,
    /** Quita las hojas del árbol talado. */
    LEAVES,
    /** Vuelve a plantar el retoño. */
    REPLANT,
    /** Posibilidad de encontrar tesoros al excavar. */
    TREASURE,
    /** Más almas al matar. */
    SOULS,
    /** Roba vida al golpear. */
    LIFE,
    /** Prende fuego. */
    FIRE,
    /** Ralentiza y congela. */
    FROST,
    /** Descargas eléctricas entre enemigos. */
    ZAP,
    /** Botín: suma niveles de Botín al matar (más objetos y más probabilidad de materiales). */
    LOOT,
    /** Habilidad activa de espada (clic derecho, gasta almas). */
    SKILL;

    public final String id = name().toLowerCase();

    /** "XP2 HASTE1 SMELT" -> {XP=2, HASTE=1, SMELT=1}. */
    public static Map<Perk, Integer> parse(String spec) {
        EnumMap<Perk, Integer> m = new EnumMap<>(Perk.class);
        for (String tok : spec.trim().split("\\s+")) {
            if (tok.isEmpty()) {
                continue;
            }
            int i = tok.length();
            while (i > 0 && Character.isDigit(tok.charAt(i - 1))) {
                i--;
            }
            Perk p = Perk.valueOf(tok.substring(0, i));
            m.put(p, i == tok.length() ? 1 : Integer.parseInt(tok.substring(i)));
        }
        return m;
    }
}
