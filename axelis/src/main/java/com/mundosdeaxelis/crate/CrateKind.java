package com.mundosdeaxelis.crate;

import com.mundosdeaxelis.gear.GearSet;

/**
 * Las cajas permanentes: ocho de colección (de la más sencilla a la mejor: Naturaleza, Forja, Hielo, Fuego, Magia, Dragón,
 * Cielo y Abismo) y la de Mascotas, que solo existe en el modo creativo. Cada caja de colección da un set de equipo (cuatro
 * herramientas, tres rarezas como máximo) y se abre con su propia llave.
 * <p>
 * Los ids internos de las cajas originales se conservan (forest = Naturaleza, magma = Fuego, ruby = Dragón) para no romper
 * bloques y objetos que ya existen en los mundos.
 */
public enum CrateKind {
    PETS("pets", 0xFF7FC8, 0, new GearSet[0], new int[]{1, 2, 3, 4, 5, 6, 7, 8, 9}, new int[]{12, 12, 12, 12, 12, 12, 12, 8, 8}),
    FOREST("forest", 0x7BD66B, 2, new GearSet[]{GearSet.FOREST, GearSet.BLOOM}, new int[]{1}, new int[]{100}),
    FORGE("forge", 0xC9D4DE, 3, new GearSet[]{GearSet.FORGE, GearSet.BRONZE}, new int[]{1, 2}, new int[]{62, 38}),
    ICE("ice", 0x8FDFFF, 1, new GearSet[]{GearSet.ICE, GearSet.AURORA}, new int[]{2, 3}, new int[]{62, 38}),
    MAGMA("magma", 0xFF7A2A, 1, new GearSet[]{GearSet.MAGMA, GearSet.OBSIDIAN}, new int[]{3, 4, 5}, new int[]{45, 38, 17}),
    MAGIC("magic", 0xC070FF, 0, new GearSet[]{GearSet.MAGIC, GearSet.MOON}, new int[]{5, 6}, new int[]{62, 38}),
    RUBY("ruby", 0xFF3A5E, 0, new GearSet[]{GearSet.RUBY, GearSet.GOLDEN}, new int[]{6, 7}, new int[]{62, 38}),
    SKY("sky", 0x7AB8FF, 2, new GearSet[]{GearSet.STORM, GearSet.PEGASUS}, new int[]{7, 8}, new int[]{64, 36}),
    ABYSS("abyss", 0x9A5CFF, 3, new GearSet[]{GearSet.SHADOW, GearSet.COSMIC}, new int[]{8, 9}, new int[]{70, 30});

    /** Garantía: hasta tres rarezas por caja (las más altas, de Épica para arriba); el mínimo de aperturas es el primer número y el valor real sale de {@link #pity(Tier)}. */
    public static final int[] PITY = {10, 10, 10};

    public final String id;
    public final int color;
    /** Forma del cristal 3D (0 brillante, 1 punta, 2 prisma, 3 esmeralda). */
    public final int gem;
    /** Set de equipo que da esta caja (vacío en Mascotas). */
    public final GearSet[] sets;
    /** Niveles de calidad (1..9) que puede dar la caja y el porcentaje de cada uno (la suma es 100). Mascotas usa los nueve. */
    private final int[] levels;
    private final int[] levelPct;

    CrateKind(String id, int color, int gem, GearSet[] sets, int[] levels, int[] levelPct) {
        this.id = id;
        this.color = color;
        this.gem = gem;
        this.sets = sets;
        this.levels = levels;
        this.levelPct = levelPct;
    }

    /** Niveles de calidad que da la caja (de menor a mayor). */
    public int[] levels() {
        return levels;
    }

    /** Porcentaje de cada nivel de {@link #levels()}. */
    public int[] levelPct() {
        return levelPct;
    }

    public int lowLevel() {
        return levels[0];
    }

    public int highLevel() {
        return levels[levels.length - 1];
    }

    /** Peso de la rareza en milésimas (520 = 52 %), calculado con el contenido real de la caja. */
    public int weight(Tier t) {
        return (int) Math.round(CratePools.tierChances(this)[t.ordinal()] * 10.0);
    }

    /** Esencia que cuesta la llave en la Mesa del Artífice (0 = no se vende: solo creativo). */
    public int keyCost() {
        return switch (this) {
            case PETS -> 0;
            case FOREST -> 30;
            case FORGE -> 50;
            case ICE -> 80;
            case MAGMA -> 120;
            case MAGIC -> 160;
            case RUBY -> 220;
            case SKY -> 280;
            case ABYSS -> 400;
        };
    }

    /** Puesto de la caja de colección entre las ocho (1 = la más sencilla, 8 = la mejor); 0 en Mascotas. */
    public int rank() {
        return this == PETS ? 0 : ordinal();
    }

    /** Rarezas con garantía: las (hasta tres) más altas que da la caja, de Épica para arriba. */
    public Tier[] pityTiers() {
        java.util.List<Tier> l = new java.util.ArrayList<>();
        for (Tier t : Tier.values()) {
            if (t.ordinal() >= Tier.EPIC.ordinal() && weight(t) > 0) {
                l.add(t);
            }
        }
        int from = Math.max(0, l.size() - PITY.length);
        return l.subList(from, l.size()).toArray(new Tier[0]);
    }

    /** Posición de la rareza entre las que tienen garantía en esta caja (-1 si no tiene). */
    public int pityIndex(Tier t) {
        Tier[] a = pityTiers();
        for (int i = 0; i < a.length; i++) {
            if (a[i] == t) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Cuántas aperturas sin esa rareza hacen falta para que salga seguro. Se calcula con la probabilidad real de la caja (más o
     * menos el doble de lo que tardaría en salir de forma natural), así la garantía solo cubre la mala suerte y nunca convierte
     * en habitual algo que la caja casi no da. Si la rareza sale menos de 1,5 de cada 100 aperturas, no hay garantía (0). La
     * garantía cubre las (hasta tres) rarezas más altas de cada caja, de Épica para arriba.
     */
    public int pity(Tier t) {
        int w = weight(t);
        if (pityIndex(t) < 0 || w < 15) {
            return 0;
        }
        return Math.max(PITY[0], Math.min(200, Math.round(2000.0F / w)));
    }

    public String blockId() {
        return "crate_" + id;
    }

    public String keyId() {
        return "key_" + id;
    }

    public String titleKey() {
        return "crate.mundos_de_axelis." + id;
    }

    public static CrateKind byIndex(int i) {
        CrateKind[] v = values();
        return v[Math.max(0, Math.min(v.length - 1, i))];
    }

    public static CrateKind byId(String id) {
        for (CrateKind k : values()) {
            if (k.id.equals(id)) {
                return k;
            }
        }
        return null;
    }
}
