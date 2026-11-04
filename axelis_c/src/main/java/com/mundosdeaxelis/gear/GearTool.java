package com.mundosdeaxelis.gear;

import com.mundosdeaxelis.crate.Tiered;

import java.util.Map;

/**
 * Arma o herramienta del mod con rareza. Las especiales tienen una {@link Ability} propia; las de la escalera de un set
 * tienen una {@link GearDef} con sus capacidades. Una de las dos es siempre {@code null}.
 */
public interface GearTool extends Tiered {
    Ability ability();

    default GearDef def() {
        return null;
    }

    /** Capacidades de la herramienta (las de la ficha, o las equivalentes de una especial). */
    default Map<Perk, Integer> perks() {
        GearDef d = def();
        return d != null ? d.perks() : Ability.perksOf(ability());
    }

    default int lvl(Perk p) {
        return perks().getOrDefault(p, 0);
    }
}
