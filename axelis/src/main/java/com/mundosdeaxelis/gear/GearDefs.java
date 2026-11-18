package com.mundosdeaxelis.gear;


import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * La tabla de herramientas de los sets. Cada set tiene una herramienta por familia (espada, pico, hacha y pala); las capacidades
 * son acumulativas: un set mejor siempre tiene todo lo del anterior y algo más.
 */
public final class GearDefs {
    private GearDefs() {
    }

    public static final List<GearDef> ALL = new ArrayList<>();
    private static final Map<String, GearDef> BY_ID = new HashMap<>();

    private static final Map<Perk, Integer> MAX = new EnumMap<>(Perk.class);

    static {
        MAX.put(Perk.XP, 3);
        MAX.put(Perk.AREA, 3);
        MAX.put(Perk.VEIN, 3);
        MAX.put(Perk.HASTE, 3);
        MAX.put(Perk.FELL, 4);
        MAX.put(Perk.TREASURE, 3);
        MAX.put(Perk.SOULS, 4);
        MAX.put(Perk.LIFE, 3);
        MAX.put(Perk.FIRE, 3);
        MAX.put(Perk.FROST, 3);
        MAX.put(Perk.ZAP, 3);
        MAX.put(Perk.SKILL, 6);
        MAX.put(Perk.LOOT, 3);
    }

    /**
     * Arma la herramienta de una familia para un set. Hay 16 sets (puesto 1..16, el orden de las cajas: dos por caja), así que
     * cada familia tiene 16 piezas. Cada capacidad se escribe como "NOMBRE@a,b,c": sube de nivel en las piezas número a, b y c
     * (la primera es la 0). Los números están pensados para 24 piezas (la escala antigua) y aquí se pasan a 16 con
     * {@link #scale(int)}. Como el orden es el de las cajas, cada pieza tiene todo lo de las anteriores y los sets mejores
     * siempre añaden algo nuevo.
     */
    private static void ladder(GearSet set, Family fam, String spec) {
        int n = set.rank - 1;
        EnumMap<Perk, Integer> m = new EnumMap<>(Perk.class);
        for (String tok : spec.trim().split("\\s+")) {
            if (tok.isEmpty()) {
                continue;
            }
            String[] kv = tok.split("@");
            Perk p = Perk.valueOf(kv[0]);
            int lvl = 0;
            for (String th : kv[1].split(",")) {
                if (n >= scale(Integer.parseInt(th.trim()))) {
                    lvl++;
                }
            }
            if (lvl > 0) {
                m.put(p, Math.min(lvl, MAX.getOrDefault(p, 1)));
            }
        }
        // capacidades que crecen set por set (de menos a más)
        switch (fam) {
            case SWORD -> {
                // la habilidad de almas sube con el puesto: ninguna en la 1.ª espada, una cada pocos puestos después
                int sk = Skill.levelForRank(set.rank);
                if (sk > 0) {
                    m.put(Perk.SKILL, sk);
                }
            }
            case AXE -> m.put(Perk.FELL, set.rank);
            case PICK -> {
                m.put(Perk.VEIN, set.rank);
                if (set.rank >= GearEvents.PICK_AREA_FROM) {
                    m.put(Perk.AREA, set.rank - GearEvents.PICK_AREA_FROM + 1);
                }
            }
            case SHOVEL -> {
                if (set.rank >= GearEvents.SHOVEL_AREA_FROM) {
                    m.put(Perk.AREA, set.rank - GearEvents.SHOVEL_AREA_FROM + 1);
                }
            }
            default -> {
            }
        }
        GearDef d = new GearDef(set, fam, set.tier, (set.rank - 1) % 2, Collections.unmodifiableMap(m));
        ALL.add(d);
        BY_ID.put(d.id(), d);
    }

    /** Pasa un número de pieza de la escala antigua (0..23) a la nueva (0..15). */
    private static int scale(int old) {
        return (int) Math.ceil(old * 15.0 / 23.0);
    }

    /** Capacidades de cada familia (números en la escala antigua de 24 piezas; ver {@link #scale(int)}). */
    // El fuego (prender al enemigo) empieza en la caja de Fuego (puesto 7): las espadas de las cajas 1 a 3 ya no queman.
    private static final String SWORD = "SOULS@0,6,12,21 LIFE@2,8,14 FIRE@9,15,21 FROST@6,11,19 ZAP@12,18,20 LOOT@3,11,19";
    // La tala (FELL), la veta (VEIN) y el área (AREA) ya no suben por niveles sueltos: crecen set por set (ver GearEvents.FELL_LOGS,
    // VEIN_CAP, AREA_R y AREA_D) y su "nivel" es el puesto del set (hacha/veta) o el puesto menos el que empieza (área).
    private static final String PICK = "XP@0,5,12 HASTE@1,8,16 SMELT@3 RADAR@6 MAGNET@12";
    private static final String AXE = "REPLANT@2 LEAVES@6 SMELT@10 MAGNET@12 HASTE@15,19,21";
    private static final String SHOVEL = "HASTE@1,9,16 TREASURE@4,10,17 MAGNET@11 SMELT@19";

    static {
        for (GearSet set : GearSet.values()) {
            ladder(set, Family.SWORD, SWORD);
        }
        for (GearSet set : GearSet.values()) {
            ladder(set, Family.PICK, PICK);
        }
        for (GearSet set : GearSet.values()) {
            ladder(set, Family.AXE, AXE);
        }
        for (GearSet set : GearSet.values()) {
            ladder(set, Family.SHOVEL, SHOVEL);
        }
    }

    public static GearDef get(String id) {
        return BY_ID.get(id);
    }

    public static List<GearDef> of(GearSet set) {
        List<GearDef> out = new ArrayList<>();
        for (GearDef d : ALL) {
            if (d.set() == set) {
                out.add(d);
            }
        }
        return out;
    }
}
