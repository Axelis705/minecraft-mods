package com.mundosdeaxelis.gear;

import com.mundosdeaxelis.crate.Tier;

import java.util.Map;

/** Ficha de una herramienta de la escalera de un set: qué es, qué rareza tiene y qué capacidades trae. */
public record GearDef(GearSet set, Family family, Tier tier, int rung, Map<Perk, Integer> perks) {
    public String id() {
        return set.id + "_" + family.id + "_" + set.artSuffix;
    }

    public int lvl(Perk p) {
        return perks.getOrDefault(p, 0);
    }

    public boolean has(Perk p) {
        return perks.containsKey(p);
    }

    /** Daño total de la espada de este set (6 en el primero, a nivel hierro, y 50 en el último; ver {@link GearStats}). */
    public float swordDamage() {
        return GearStats.sword(set.rank);
    }

    /** Velocidad al picar/talar/excavar (6 en el primer set, como el hierro, y 28 en el último). */
    public float miningSpeed() {
        return GearStats.speed(set.rank);
    }

    public int ord() {
        return tier.ordinal();
    }

    public float attackSpeed() {
        return switch (family) {
            case SWORD -> -2.4F;
            case PICK -> -2.8F;
            case AXE -> -3.0F;
            case SHOVEL -> -3.0F;
        };
    }
}
