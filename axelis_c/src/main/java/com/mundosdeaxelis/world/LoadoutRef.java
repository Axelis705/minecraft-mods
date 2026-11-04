package com.mundosdeaxelis.world;

import com.mundosdeaxelis.crate.CrateKind;
import com.mundosdeaxelis.crate.Tier;
import com.mundosdeaxelis.gear.ArmorDefs;
import com.mundosdeaxelis.gear.GearSet;
import com.mundosdeaxelis.gear.GearStats;
import net.minecraft.world.damagesource.CombatRules;

/**
 * El jugador de referencia de cada etapa: lo MEJOR que da la caja de esa etapa, con todo lo que suma. La dificultad de los
 * jefes y de la dungeon se calibra siempre contra este jugador (quien tenga menos, lo pasa peor; quien tenga más, mejor).
 *
 * <p>Qué incluye (todo sale de las mismas tablas que usa el juego, menos lo marcado como estimación):
 * <ul>
 * <li>Armadura: set B de la caja (puntos, dureza y reducción propia por puesto).</li>
 * <li>Mascotas: ranuras llenas con mascotas de la rareza más alta de la caja (armadura por caja, {@code CratePools.petArmor}).
 * Ranuras según el nivel de la Mesa que se espera en esa etapa (3 hasta la etapa 2, 4 hasta la 4, 5 después).</li>
 * <li>Bonus del set B: vida extra, daño extra, esquiva (Vacío estelar 12 %).</li>
 * <li>Espada B de la caja (daño medio) más el daño extra del set.</li>
 * <li>Reliquias (ESTIMACIÓN): ranuras 6/7/8 con reliquias de la rareza más alta; se cuentan 3 de combate que dan
 * {@link #RELIC_OFF} de daño y {@link #RELIC_DEF} de reducción cada una, multiplicado por la potencia de la rareza.</li>
 * <li>Encantamientos (ESTIMACIÓN, desde la etapa 2): Protección en la armadura ({@link #enchantDefense}) y Filo en la espada
 * ({@link #enchantDamage}). La etapa 1 no los cuenta (así se probó); a partir de ahí se supone que el jugador ya tiene mesa de
 * encantamientos.</li>
 * </ul>
 * No cuenta pociones ni comida (la comida de las dungeons está limitada aparte: ver {@code ForgeRoastItem}).
 */
public final class LoadoutRef {
    /** Reliquias de combate que se supone que lleva el jugador de referencia. */
    public static final int COMBAT_RELICS = 3;
    /** Daño extra por reliquia de combate y punto de potencia de la rareza (estimación). */
    public static final float RELIC_OFF = 0.04F;
    /** Reducción de daño por reliquia de combate y punto de potencia de la rareza (estimación). */
    public static final float RELIC_DEF = 0.03F;

    private LoadoutRef() {
    }

    /** Reducción de daño por encantamientos de la armadura que se espera en cada etapa (1..8): 0, 20, 30, 38, 44, 50, 55, 60 %. */
    public static float enchantDefense(int stage) {
        float[] v = {0.0F, 0.0F, 0.20F, 0.30F, 0.38F, 0.44F, 0.50F, 0.55F, 0.60F};
        return v[Math.max(1, Math.min(8, stage))];
    }

    /** Daño extra de la espada por Filo que se espera en cada etapa: 0, 1.5, 2, 2.5 y 3 (Filo V) desde la 5. */
    public static float enchantDamage(int stage) {
        float[] v = {0.0F, 0.0F, 1.5F, 2.0F, 2.5F, 3.0F, 3.0F, 3.0F, 3.0F};
        return v[Math.max(1, Math.min(8, stage))];
    }

    /** Rareza que corresponde a un nivel de calidad (1..9). */
    public static Tier tierOfLevel(int level) {
        return switch (Math.max(1, Math.min(9, level))) {
            case 1 -> Tier.COMMON;
            case 2 -> Tier.UNCOMMON;
            case 3, 4 -> Tier.RARE;
            case 5 -> Tier.EPIC;
            case 6 -> Tier.LEGENDARY;
            case 7 -> Tier.MYTHIC;
            case 8 -> Tier.STELLAR;
            default -> Tier.CELESTIAL;
        };
    }

    /** Rareza más alta que da la caja de la etapa (1..8). */
    public static Tier topTier(int stage) {
        return tierOfLevel(CrateKind.byIndex(Math.max(1, Math.min(8, stage))).highLevel());
    }

    public static int petSlots(int stage) {
        return stage <= 2 ? 3 : stage <= 4 ? 4 : 5;
    }

    public static int relicSlots(int stage) {
        return stage <= 2 ? 6 : stage <= 4 ? 7 : 8;
    }

    /** Armadura de las mascotas de referencia: las mejores mascotas de las cajas hasta esa etapa (2N-1 cada una, 2N la estrella de la caja N). */
    public static float petArmor(int stage) {
        int st = Math.max(1, Math.min(8, stage));
        return com.mundosdeaxelis.crate.CratePools.bestPetArmor(st, petSlots(st));
    }

    /** Armadura de la capa de referencia: la capa B de la caja de la etapa (puesto 2·etapa en la lista de capas). */
    public static float capeArmor(int stage) {
        return 2 * Math.max(1, Math.min(8, stage));
    }

    private static GearSet byRank(int rank) {
        for (GearSet s : GearSet.values()) {
            if (s.rank == rank) {
                return s;
            }
        }
        return GearSet.FOREST;
    }

    /** Vida extra (puntos) que da el bonus del set de ese puesto. */
    public static float bonusHp(int rank) {
        GearSet s = byRank(rank);
        return switch (s) {
            case FOREST -> 2.0F;
            case BLOOM -> 3.0F;
            case RUBY -> 4.0F;
            case GOLDEN -> 6.0F;
            case COSMIC -> 8.0F;
            default -> 0.0F;
        };
    }

    /** Daño de ataque extra del bonus del set. */
    public static float bonusDamage(int rank) {
        GearSet s = byRank(rank);
        return s == GearSet.GOLDEN ? 2.5F : s == GearSet.RUBY ? 1.5F : 0.0F;
    }

    /** Probabilidad de esquivar un golpe (bonus del set). */
    public static float dodge(int rank) {
        GearSet s = byRank(rank);
        return s == GearSet.COSMIC ? 0.12F : s == GearSet.SHADOW ? 0.10F : 0.0F;
    }

    /** Vida máxima del jugador de referencia con el set de ese puesto. */
    public static float maxHp(int rank) {
        return 20.0F + bonusHp(rank);
    }

    public static float relicDamageMul(int stage) {
        return 1.0F + RELIC_OFF * COMBAT_RELICS * topTier(stage).power;
    }

    public static float relicDefenseMul(int stage) {
        return Math.max(0.5F, 1.0F - RELIC_DEF * COMBAT_RELICS * topTier(stage).power);
    }

    /** Daño por golpe del jugador de referencia (espada B + bonus del set, con reliquias). */
    public static float hit(int stage) {
        int rank = Math.max(2, Math.min(16, 2 * stage));
        return (GearStats.sword(rank) + bonusDamage(rank) + enchantDamage(stage)) * relicDamageMul(stage);
    }

    /** Daño que recibe tras todo (armadura, dureza, reducción del set, mascotas, reliquias, esquiva esperada) por un golpe de {@code raw}. */
    public static float taken(int stage, int rank, float raw) {
        return taken(stage, rank, raw, true);
    }

    /** Igual, pero con {@code full = false} solo cuenta el set (sin mascotas, reliquias ni esquiva): es la base histórica de la etapa 1. */
    public static float taken(int stage, int rank, float raw, boolean full) {
        float armor = ArmorDefs.setTotal(rank) + (full ? petArmor(stage) + capeArmor(stage) : 0.0F);
        float toughness = 4.0F * (0.5F + 0.25F * (rank - 1.0F));
        float dmg = CombatRules.getDamageAfterAbsorb(raw, armor, toughness) * (1.0F - ArmorDefs.damageReduction(rank));
        if (armor > 30.0F) {
            dmg *= 1.0F - Math.min(0.15F, (armor - 30.0F) * 0.005F);
        }
        if (full) {
            dmg *= 1.0F - enchantDefense(stage);
            dmg *= relicDefenseMul(stage);
            dmg *= 1.0F - dodge(rank);
        }
        return dmg;
    }

    /** Resumen legible para el comando de pruebas. */
    public static String describe(int stage) {
        int rank = Math.max(2, Math.min(16, 2 * stage));
        return String.format("etapa %d: set B puesto %d (armadura %d + mascotas %.0f [%d x %s] + capa %.0f, vida %.0f, esquiva %.0f%%), golpe %.1f, reliquias x%.2f daño / x%.2f defensa",
                stage, rank, ArmorDefs.setTotal(rank), petArmor(stage), petSlots(stage), topTier(stage).id, capeArmor(stage), maxHp(rank), dodge(rank) * 100.0F,
                hit(stage), relicDamageMul(stage), relicDefenseMul(stage));
    }
}
