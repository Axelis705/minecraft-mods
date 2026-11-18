package com.mundosdeaxelis.cape;

import com.mundosdeaxelis.crate.CrateKind;
import com.mundosdeaxelis.crate.Tier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Las capas del mod: objetos aparte (no van en la armadura) que salen de las cajas y se ponen en la ranura "Capa" del menú de
 * equipo. Cada caja de colección da la capa de su propio set. Para añadir una capa nueva basta con una línea aquí, su textura
 * {@code textures/cape/<id>.png} (y {@code <id>_glow.png}), su icono y sus textos.
 */
public final class CapeDefs {
    private CapeDefs() {
    }

    /** Una capa: id del objeto (cape_xxx), caja que la da y rareza. */
    public record Def(String id, CrateKind crate, Tier tier) {
        /** Nombre corto de la textura (sin el prefijo cape_). */
        public String texName() {
            return id;
        }

        /** Armadura que da la capa (puntos enteros): su puesto en la lista, de 1 (la primera) a 16 (la última). */
        public int armor() {
            return ALL.indexOf(this) + 1;
        }
    }

    public static final List<Def> ALL;

    static {
        List<Def> l = new ArrayList<>();
        l.add(new Def("cape_forest", CrateKind.FOREST, Tier.COMMON));
        l.add(new Def("cape_bloom", CrateKind.FOREST, Tier.COMMON));
        l.add(new Def("cape_forge", CrateKind.FORGE, Tier.COMMON));
        l.add(new Def("cape_bronze", CrateKind.FORGE, Tier.UNCOMMON));
        l.add(new Def("cape_ice", CrateKind.ICE, Tier.UNCOMMON));
        l.add(new Def("cape_aurora", CrateKind.ICE, Tier.RARE));
        l.add(new Def("cape_magma", CrateKind.MAGMA, Tier.RARE));
        l.add(new Def("cape_obsidian", CrateKind.MAGMA, Tier.EPIC));
        l.add(new Def("cape_magic", CrateKind.MAGIC, Tier.EPIC));
        l.add(new Def("cape_moon", CrateKind.MAGIC, Tier.LEGENDARY));
        l.add(new Def("cape_ruby", CrateKind.RUBY, Tier.LEGENDARY));
        l.add(new Def("cape_golden", CrateKind.RUBY, Tier.MYTHIC));
        l.add(new Def("cape_storm", CrateKind.SKY, Tier.MYTHIC));
        l.add(new Def("cape_pegasus", CrateKind.SKY, Tier.STELLAR));
        l.add(new Def("cape_shadow", CrateKind.ABYSS, Tier.STELLAR));
        l.add(new Def("cape_cosmic", CrateKind.ABYSS, Tier.CELESTIAL));
        ALL = Collections.unmodifiableList(l);
    }

    public static Def get(String id) {
        for (Def d : ALL) {
            if (d.id().equals(id)) {
                return d;
            }
        }
        return null;
    }

    /** Capas que da esa caja. */
    public static List<Def> of(CrateKind kind) {
        List<Def> out = new ArrayList<>();
        for (Def d : ALL) {
            if (d.crate == kind) {
                out.add(d);
            }
        }
        return out;
    }
}
