package com.mundosdeaxelis.arma;

import com.mundosdeaxelis.crate.CrateKind;
import com.mundosdeaxelis.crate.Tier;
import com.mundosdeaxelis.gear.GearSet;
import com.mundosdeaxelis.gear.GearStats;

/**
 * Los ocho tiers de armas, uno por caja de colección (etapa 281). Cada tier tiene su elemento, que se aplica en todos los
 * golpes y en la especial. El daño de referencia es el de la espada del set B de la caja ({@link GearStats#sword}), así el
 * equilibrio de jefes y dungeons ({@code LoadoutRef}) sigue igual.
 */
public enum TierArma {
    BOSQUE("bosque", CrateKind.FOREST, 0x7BD66B, Elemento.VENENO),
    FORJA("forja", CrateKind.FORGE, 0xFF9A3C, Elemento.SANGRADO),
    HIELO("hielo", CrateKind.ICE, 0x8FDFFF, Elemento.ESCARCHA),
    MAGMA("magma", CrateKind.MAGMA, 0xFF6A1A, Elemento.FUEGO),
    MAGIA("magia", CrateKind.MAGIC, 0xC070FF, Elemento.ARCANO),
    DRAGON("dragon", CrateKind.RUBY, 0xFF3A5E, Elemento.VAMPIRO),
    CIELO("cielo", CrateKind.SKY, 0xFFE866, Elemento.RAYO),
    ABISMO("abismo", CrateKind.ABYSS, 0x8CF6FF, Elemento.VACIO);

    /** Lo que hace el elemento del tier al golpear. */
    public enum Elemento {
        VENENO, SANGRADO, ESCARCHA, FUEGO, ARCANO, VAMPIRO, RAYO, VACIO;

        public final String id = name().toLowerCase();
    }

    public final String id;
    public final CrateKind caja;
    public final int color;
    public final Elemento elemento;

    TierArma(String id, CrateKind caja, int color, Elemento elemento) {
        this.id = id;
        this.caja = caja;
        this.color = color;
        this.elemento = elemento;
    }

    /** 1..8 (el orden de las cajas). */
    public int nivel() {
        return ordinal() + 1;
    }

    /** Puesto del set B de la caja (2, 4 ... 16): de ahí salen el daño y la durabilidad. */
    public int puesto() {
        return 2 * nivel();
    }

    /** Daño de referencia (el de la espada): 8, 14, 20, 26, 32, 38, 44, 50. */
    public float danoBase() {
        return GearStats.sword(puesto());
    }

    public GearSet setA() {
        return caja.sets[0];
    }

    public GearSet setB() {
        return caja.sets[1];
    }

    /** Rareza interna: la del set A de su caja (la que se ve sale de lo seguido que toca en la caja). */
    public Tier rareza() {
        return setA().tier;
    }

    public boolean ignifuga() {
        return nivel() >= 4;
    }

    /** Nivel de "más almas" (0..4) y de botín (0..3) que trae el arma, que crece con el tier. */
    public int nivelAlmas() {
        return new int[]{0, 0, 2, 2, 3, 3, 4, 4}[ordinal()];
    }

    public int nivelBotin() {
        return new int[]{0, 0, 1, 1, 1, 2, 2, 3}[ordinal()];
    }

    public static TierArma deCaja(CrateKind k) {
        for (TierArma t : values()) {
            if (t.caja == k) {
                return t;
            }
        }
        return null;
    }
}
